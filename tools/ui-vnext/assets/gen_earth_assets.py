#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generate bundled Earth texture assets for PHASE 1C OFFLINE_TEXTURE_EARTH.

Deterministic generation from the repo's OWN bundled datasets
(desktop WorldCoastlines.kt / WorldCityLights.kt), plus procedural
value-noise clouds. No network, no third-party textures, license-safe
(repo-generated asset). Outputs PNGs + ASSET_MANIFEST.json (sha256).

Outputs (equirectangular 2048x1024):
  earth_albedo_2048.png      - ocean/land material + terrain shading + coast highlight
  earth_night_lights_2048.png- city illumination points with glow
  cloud_2048.png             - deterministic low-frequency cloud alpha
"""
import hashlib
import json
import math
import os
import re
import struct
import zlib

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
KOTLIN_DIR = os.path.join(REPO, "desktop", "app", "src", "main", "kotlin", "com", "pdig", "uivnext", "globe")
OUT_DIR = os.path.join(REPO, "spec", "ui-vnext", "assets")
W, H = 2048, 1024


def parse_coastlines(path):
    text = open(path, encoding="utf-8").read()
    polys = []
    for block in re.finditer(r"Coastline\(\s*\"([^\"]+)\",\s*listOf\((.*?)\n\s*\),", text, re.S):
        name = block.group(1)
        pts = [(float(a), float(b)) for a, b in re.findall(r"([-\d.]+)\s+to\s+([-\d.]+)", block.group(2))]
        if len(pts) >= 3:
            polys.append((name, pts))
    return polys


def parse_cities(path):
    text = open(path, encoding="utf-8").read()
    return [(float(lat), float(lon)) for lat, lon in re.findall(r"CityLight\(\"[^\"]+\",\s*([-\d.]+),\s*([-\d.]+)\)", text)]


def png_write(width, height, rows):
    """rows: list of bytearray rows (width*3 RGB). Returns PNG bytes."""
    def chunk(typ, data):
        c = struct.pack(">I", len(data)) + typ + data
        c += struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF)
        return c
    raw = b"".join(b"\x00" + bytes(r) for r in rows)
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


def lonlat_to_xy(lon, lat):
    u = ((lon + 180.0) % 360.0) / 360.0
    v = (90.0 - lat) / 180.0
    return int(u * (W - 1)), int(v * (H - 1))


def in_poly(x, y, pts):
    n = len(pts)
    inside = False
    for i in range(n):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % n]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1 + 1e-12) + x1:
            inside = not inside
    return inside


def land_mask():
    coast = parse_coastlines(os.path.join(KOTLIN_DIR, "WorldCoastlines.kt"))
    mask = [[False] * W for _ in range(H)]
    polys_xy = [[(float(lon), float(lat)) for lat, lon in pts] for _, pts in coast]
    for y in range(H):
        lat = 90.0 - y / H * 180.0
        for x in range(W):
            lon = x / W * 360.0 - 180.0
            for poly in polys_xy:
                if in_poly(lon, lat, poly):
                    mask[y][x] = True
                    break
    return mask


def random_gen(seed):
    state = seed
    def nxt():
        nonlocal state
        state = (state * 1103515245 + 12345) & 0x7FFFFFFF
        return state / 0x7FFFFFFF
    return nxt


def set_px(rows, x, y, color):
    r, g, b, a = color
    row = rows[y]
    row[x * 3] = min(255, row[x * 3] + ((r - row[x * 3]) * a // 255))
    row[x * 3 + 1] = min(255, row[x * 3 + 1] + ((g - row[x * 3 + 1]) * a // 255))
    row[x * 3 + 2] = min(255, row[x * 3 + 2] + ((b - row[x * 3 + 2]) * a // 255))


def line(rows, a, b, color):
    x0, y0 = a
    x1, y1 = b
    dx = abs(x1 - x0); dy = abs(y1 - y0)
    sx = 1 if x0 < x1 else -1; sy = 1 if y0 < y1 else -1
    err = dx - dy
    while True:
        if 0 <= x0 < W and 0 <= y0 < H:
            set_px(rows, x0, y0, color)
            if 0 <= x0 + 1 < W:
                set_px(rows, x0 + 1, y0, color)
            if 0 <= y0 + 1 < H:
                set_px(rows, x0, y0 + 1, color)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 > -dy:
            err -= dy; x0 += sx
        if e2 < dx:
            err += dx; y0 += sy


def gen_albedo():
    mask = land_mask()
    coast = parse_coastlines(os.path.join(KOTLIN_DIR, "WorldCoastlines.kt"))
    rows = []
    for y in range(H):
        lat = 90.0 - y / H * 180.0
        row = bytearray(W * 3)
        for x in range(W):
            if mask[y][x]:
                shade = 1.0 - abs(lat) / 90.0 * 0.35
                band = ((x * 31 + y * 17) % 97) / 97.0
                row[x * 3] = int(46 + 26 * band * shade)
                row[x * 3 + 1] = int(62 + 30 * band * shade)
                row[x * 3 + 2] = int(58 + 26 * band * shade)
            else:
                band = ((x * 13 + y * 7) % 71) / 71.0
                depth = 1.0 - abs(lat) / 90.0 * 0.5
                row[x * 3] = int(8 + 4 * band * depth)
                row[x * 3 + 1] = int(26 + 14 * band * depth)
                row[x * 3 + 2] = int(52 + 30 * band * depth)
        rows.append(row)
    for _, pts in coast:
        prev = None
        for lat, lon in pts:
            x, y = lonlat_to_xy(lon, lat)
            if prev is not None:
                line(rows, prev, (x, y), (150, 178, 220, 255))
            prev = (x, y)
    return png_write(W, H, rows)


def gen_night_lights():
    cities = parse_cities(os.path.join(KOTLIN_DIR, "WorldCityLights.kt"))
    rows = [bytearray(W * 3) for _ in range(H)]
    glow = []
    for lat, lon in cities:
        x, y = lonlat_to_xy(lon, lat)
        glow.append((x, y, 2.0, 1.0))
        glow.append((x, y, 5.0, 0.45))
        glow.append((x, y, 9.0, 0.18))
    for x, y, rad, amp in glow:
        for dy in range(-int(rad), int(rad) + 1):
            for dx in range(-int(rad), int(rad) + 1):
                d = math.hypot(dx, dy)
                if d <= rad:
                    f = (1.0 - d / rad) * amp
                    px, py = x + dx, y + dy
                    if 0 <= px < W and 0 <= py < H:
                        row = rows[py]
                        base = px * 3
                        row[base] = max(row[base], int(255 * f))
                        row[base + 1] = max(row[base + 1], int(217 * f))
                        row[base + 2] = max(row[base + 2], int(138 * f))
    return png_write(W, H, rows)


def value_noise(seed, grid):
    rnd = random_gen(seed)
    vals = [[rnd() for _ in range(grid + 1)] for _ in range(grid + 1)]
    def sample(px, py):
        gx = px * grid / W
        gy = py * grid / H
        x0 = int(gx) % grid; y0 = int(gy) % grid
        fx = gx - int(gx); fy = gy - int(gy)
        def v(x, y):
            return vals[y % grid][x % grid]
        a = v(x0, y0) + (v(x0 + 1, y0) - v(x0, y0)) * fx
        b = v(x0, y0 + 1) + (v(x0 + 1, y0 + 1) - v(x0, y0 + 1)) * fx
        return a + (b - a) * fy
    return sample


def gen_clouds():
    n1 = value_noise(777, 48)
    n2 = value_noise(999, 16)
    rows = []
    for y in range(H):
        row = bytearray(W * 3)
        for x in range(W):
            v = n1(x, y) * 0.6 + n2(x, y) * 0.4
            a = max(0.0, v - 0.42) / 0.58
            a = a * a
            g = int(235 * a)
            row[x * 3] = g
            row[x * 3 + 1] = g
            row[x * 3 + 2] = min(255, g + 15)
        rows.append(row)
    return png_write(W, H, rows)


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    manifest_path = os.path.join(OUT_DIR, "ASSET_MANIFEST.json")
    # SAFETY: PHASE 1D replaced procedural generation with real NASA public-domain
    # textures. Refuse to clobber them if the manifest records real (non-generated) assets.
    if os.path.exists(manifest_path):
        try:
            existing = json.load(open(manifest_path, encoding="utf-8"))
            sources = " ".join(a.get("source", "") for a in existing.get("assets", []))
            if "NASA" in sources or "Visible Earth" in sources:
                print("REFUSE: ASSET_MANIFEST.json records real NASA assets; procedural generator must not overwrite them.")
                return
        except Exception as e:
            print("REFUSE: cannot read existing manifest; not overwriting.", e)
            return
    os.makedirs(OUT_DIR, exist_ok=True)
    files = {
        "earth_albedo_2048.png": gen_albedo(),
        "earth_night_lights_2048.png": gen_night_lights(),
        "cloud_2048.png": gen_clouds(),
    }
    usage = {
        "earth_albedo_2048": "TextureEarthRenderer day-side earth material (ocean/land/terrain/coast)",
        "earth_night_lights_2048": "TextureEarthRenderer night-side city illumination",
        "cloud_2048": "TextureEarthRenderer subtle cloud layer",
    }
    manifest = []
    for name, data in files.items():
        path = os.path.join(OUT_DIR, name)
        with open(path, "wb") as f:
            f.write(data)
        manifest.append({
            "id": name.replace(".png", ""),
            "file": "spec/ui-vnext/assets/" + name,
            "type": "texture-equirect-rgb",
            "source": "generated deterministically from repo-bundled desktop WorldCoastlines.kt / WorldCityLights.kt + procedural value noise (repo own asset; no third-party textures)",
            "license": "PDIG-repo-generated (own work; offline; no redistribution of third-party content)",
            "sha256": sha256_file(path),
            "resolution": "%dx%d" % (W, H),
            "usage": usage[name.replace(".png", "")],
            "fallback": "VectorEarthFallbackRenderer (bundled coastlines procedural)",
        })
    manifest_path = os.path.join(OUT_DIR, "ASSET_MANIFEST.json")
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump({"specVersion": "1.0.0", "assets": manifest}, f, ensure_ascii=False, indent=2)
    print("wrote", len(manifest), "assets ->", OUT_DIR)
    for m in manifest:
        print(" ", m["file"], m["sha256"][:16], os.path.getsize(os.path.join(REPO, m["file"])))


if __name__ == "__main__":
    main()
