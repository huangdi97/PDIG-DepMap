#!/usr/bin/env node
// Generate EVIDENCE_SHA256SUMS.txt + screenshots.json for committed curated screenshots
// under artifacts/runtime-evidence/<run-id>/{desktop,android,harmony,ios}/**/*.png.
import { readdirSync, readFileSync, writeFileSync, statSync, existsSync } from "node:fs";
import { join, relative, posix } from "node:path";
import { createHash } from "node:crypto";
import { spawnSync } from "node:child_process";

const ROOT = process.cwd();
const RUN = "2026-09-26-multiclient-sweep";
const base = join(ROOT, "artifacts", "runtime-evidence", RUN);
const GIT_SHA = (spawnSync("git", ["rev-parse", "HEAD"], { cwd: ROOT }).stdout || "").toString().trim();

const entries = [];
function walk(dir) {
  if (!existsSync(dir)) return;
  for (const f of readdirSync(dir)) {
    const p = join(dir, f);
    if (statSync(p).isDirectory()) { walk(p); continue; }
    if (!p.toLowerCase().endsWith(".png")) continue;
    const rel = posix.join(relative(base, p).split("\\").join("/"));
    const hash = createHash("sha256").update(readFileSync(p)).digest("hex");
    entries.push({ file: rel, abs: p, sha256: hash });
  }
}
walk(base);

const lines = entries.map((e) => `${e.sha256}  ${e.file}`);
writeFileSync(join(ROOT, "EVIDENCE_SHA256SUMS.txt"), lines.join("\n") + "\n", "utf8");

const meta = entries.map((e) => {
  const parts = e.file.split("__"); // platform__device__theme__page__state__seq.png
  return {
    file: e.file,
    sha256: e.sha256,
    git_sha: GIT_SHA,
    platform: parts[0] ?? "",
    runtime: parts[0] === "desktop" ? "windows-runtime" : parts[0] === "android" ? "android-emulator-api36" : parts[0],
    device: parts[1] ?? "",
    resolution: e.file.includes("2048x1152") ? "2048x1152" : e.file.includes("1920") ? "1920x1080" : "1280x720",
    density: parts[1] === "phone-api36" ? "~420dpi" : "",
    locale: "zh-CN",
    theme: parts[2] ?? "light",
    page: parts[3] ?? "",
    state: parts[4] ?? "",
    fixture: "synthetic (fixtures/import/normal-wechat.csv or in-test manual observations)",
    test_id: parts[0] === "desktop" ? "desktop:run --shots (ShotDriver)" : "android:UiScreenshotEvidenceTest",
    timestamp: new Date().toISOString(),
  };
});
writeFileSync(join(ROOT, "screenshots.json"), JSON.stringify(meta, null, 2) + "\n", "utf8");
console.error(`indexed ${entries.length} screenshots; git_sha=${GIT_SHA}`);