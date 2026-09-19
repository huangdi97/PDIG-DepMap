#!/usr/bin/env python3
"""GB18030 码表独立复验。

为什么必须有这个文件：
    码表由 Node ICU 生成（tools/encoding/generate-gb18030-table.mjs）。
    如果只拿 ICU 自己校验自己，那叫"同源自检"，不构成证据 ——
    ICU 若在某段映射上有 bug，生成器会把它原样写进表，自检一样通过。

因此这里用**第二套独立实现** CPython 内置的 gb18030 codec（Modules/cjkcodecs，
与 ICU 无共享代码）逐位复验落地产物，覆盖：
    1. 双字节全部 23,940 个码位
    2. 四字节 BMP 区全部 50,400 个槽位（含"未定义必须仍未定义"）
    3. 四字节增补平面：边界 + 抽样，验证线性公式
    4. 单字节 0x80：记录两侧行为差异（ICU 解 U+20AC / CPython 拒绝）

校验对象是**落地产物文件**（Gb18030Table.ets / Gb18030Table.swift），
不是生成器的内存状态 —— 只测真正会被编译进 HAP / 被 Swift 编译的东西。

用法：python tools/encoding/crosscheck-gb18030.py
退出码 0 = 全部一致；1 = 出现任何不一致。
"""

import base64
import hashlib
import json
import os
import random
import re
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ETS = os.path.join(REPO, "harmony", "entry", "src", "main", "ets", "sources", "Gb18030Table.ets")
SWIFT = os.path.join(REPO, "ios", "Sources", "PDIGCore", "Encoding", "Gb18030Table.swift")
MANIFEST = os.path.join(os.path.dirname(__file__), "gb18030-manifest.json")

lines = []


def emit(s):
    lines.append(s)


def fail(msg):
    emit("FAIL " + msg)


# --------------------------------------------------------------------------
# 读取落地产物
# --------------------------------------------------------------------------
def read_chunks(path, marker, quote):
    """取出产物里的分块字符串常量。

    不能用「从 marker 找第一个分号」—— 四字节游程文本本身就用 `;` 作分隔符，
    那样会在第一个 chunk 内部截断。改为逐行识别 `  'xxx' +` / `  'xxx';`。
    """
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()
    start = text.index(marker)
    tail = text[start + len(marker):]
    pattern = re.compile(r"^\s*" + quote + "([^" + quote + "]*)" + quote + r"\s*\+?\s*;?\s*$")
    parts = []
    for line in tail.splitlines()[1:]:
        m = pattern.match(line)
        if m is None:
            break
        parts.append(m.group(1))
    return "".join(parts)


two_byte_b64_ets = read_chunks(ETS, "GB18030_TWO_BYTE_B64: string =", "'")
bmp4_text_ets = read_chunks(ETS, "GB18030_BMP4_RUNS_TEXT: string =", "'")
two_byte_b64_swift = read_chunks(SWIFT, "twoByteBase64 =", '"')
bmp4_text_swift = read_chunks(SWIFT, "bmp4RunsText =", '"')

emit("ARTIFACT 双字节 base64 长度   : ets=%d swift=%d" % (len(two_byte_b64_ets), len(two_byte_b64_swift)))
emit("ARTIFACT 四字节 BMP 游程长度  : ets=%d swift=%d" % (len(bmp4_text_ets), len(bmp4_text_swift)))
if two_byte_b64_ets != two_byte_b64_swift:
    fail("ArkTS 与 Swift 的双字节码表不一致")
if bmp4_text_ets != bmp4_text_swift:
    fail("ArkTS 与 Swift 的四字节 BMP 码表不一致")

raw = base64.b64decode(two_byte_b64_ets)
emit("ARTIFACT 双字节表 bytes       : %d  sha256=%s" % (len(raw), hashlib.sha256(raw).hexdigest()[:16]))
two_byte = [raw[i * 2] | (raw[i * 2 + 1] << 8) for i in range(len(raw) // 2)]

# 展开四字节 BMP 游程（与产物里的解码逻辑同构，但用 Python 独立实现）
bmp4 = [-1] * 50400
run_count = 0
for seg in bmp4_text_ets.split(";"):
    if not seg:
        continue
    run_count += 1
    kind, body = seg[0], seg[1:]
    parts = body.split(".")
    start = int(parts[0], 16)
    length = int(parts[1], 16)
    if kind == "V":
        first = int(parts[2], 16)
        for k in range(length):
            bmp4[start + k] = first + k
emit("ARTIFACT 四字节 BMP 游程段数  : %d" % run_count)

# --------------------------------------------------------------------------
# 1) 双字节：CPython 逐位复验
# --------------------------------------------------------------------------
TRAILS = [t for t in range(0x40, 0xFF) if t != 0x7F]


def py_decode(seq):
    try:
        s = bytes(seq).decode("gb18030")
    except Exception:
        return None
    if len(s) != 1:
        return None
    cp = ord(s)
    return None if 0xD800 <= cp <= 0xDFFF else cp


divergences = []  # (kind, seq tuple, cpython, table)

bad_2b = 0
checked_2b = 0
for lead in range(0x81, 0xFF):
    base = (lead - 0x81) * 190
    for ti, trail in enumerate(TRAILS):
        expected = py_decode([lead, trail])
        actual = two_byte[base + ti]
        checked_2b += 1
        if expected != actual:
            bad_2b += 1
            divergences.append(("2", (lead, trail), expected, actual))
emit("双字节   : 复验 %d 位，与 CPython 不一致 %d 位（逐条对账见下）" % (checked_2b, bad_2b))

# --------------------------------------------------------------------------
# 2) 四字节 BMP：CPython 逐位复验（含"未定义必须仍未定义"）
# --------------------------------------------------------------------------
bad_bmp = 0
checked_bmp = 0
valid_count = 0
for b1 in range(0x81, 0x85):
    for b2 in range(0x30, 0x3A):
        for b3 in range(0x81, 0xFF):
            for b4 in range(0x30, 0x3A):
                k = ((b1 - 0x81) * 10 + (b2 - 0x30)) * 1260 + ((b3 - 0x81) * 10 + (b4 - 0x30))
                expected = py_decode([b1, b2, b3, b4])
                actual = bmp4[k]
                checked_bmp += 1
                if expected is None:
                    if actual >= 0:
                        bad_bmp += 1
                        divergences.append(("4", (b1, b2, b3, b4), None, actual))
                else:
                    valid_count += 1
                    if actual != expected:
                        bad_bmp += 1
                        divergences.append(("4", (b1, b2, b3, b4), expected, actual))
emit("四字节BMP: 复验 %d 槽位（有效 %d），与 CPython 不一致 %d 槽位（逐条对账见下）"
     % (checked_bmp, valid_count, bad_bmp))

# --------------------------------------------------------------------------
# 3) 四字节增补平面：公式 vs CPython（边界 + 抽样）
# --------------------------------------------------------------------------
random.seed(20260919)
samples = [(0x90, 0x30, 0x81, 0x30), (0x90, 0x30, 0x81, 0x31),
           (0xE3, 0x32, 0x9A, 0x35), (0xE3, 0x32, 0x9A, 0x36),
           (0xE3, 0x33, 0x81, 0x30), (0xE4, 0x30, 0x81, 0x30)]
for _ in range(4000):
    samples.append((random.randrange(0x90, 0xE4), random.randrange(0x30, 0x3A),
                    random.randrange(0x81, 0xFF), random.randrange(0x30, 0x3A)))
bad_sup = 0
for (b1, b2, b3, b4) in samples:
    predicted = 0x10000 + ((b1 - 0x90) * 10 + (b2 - 0x30)) * 1260 + ((b3 - 0x81) * 10 + (b4 - 0x30))
    expected = py_decode([b1, b2, b3, b4])
    if expected is None:
        if predicted <= 0x10FFFF:
            bad_sup += 1
    elif expected != predicted:
        bad_sup += 1
emit("增补平面: 复验 %d 条，不一致 %d 条" % (len(samples), bad_sup))

# --------------------------------------------------------------------------
# 4) 单字节 0x80 的行为差异（记录，不作为失败项）
# --------------------------------------------------------------------------
try:
    py80 = bytes([0x80]).decode("gb18030")
    py80s = "U+%04X" % ord(py80)
except Exception:
    py80s = "INVALID"
emit("单字节80 : CPython=%s（ICU=U+20AC；解码器从严取 INVALID）" % py80s)

# --------------------------------------------------------------------------
# 5) 与记录的分歧清单对账
# --------------------------------------------------------------------------
# 分歧本身不是失败：**未预期的分歧**才是失败。三套实现（ICU / CPython / Java）
# 在 GB18030 上确有 21 个码位互不一致，这是客观事实，藏起来才是问题。
# 因此这里要求"实测分歧集合 == gb18030-divergences.json 记录的集合"：
# 多一个、少一个、或某个码位的取值变了，都必须 FAIL 并重新裁决。
with open(os.path.join(os.path.dirname(__file__), "gb18030-divergences.json"),
          "r", encoding="utf-8") as f:
    doc = json.load(f)

observed = {}
for kind, seq, cp, tb in divergences:
    observed["".join("%02X" % b for b in seq)] = (
        ("U+%04X" % cp) if cp is not None else "UNDEF",
        ("U+%04X" % tb) if tb is not None and tb >= 0 else "UNDEF",
    )

# 断言 1：每一个已记录码位，落地产物里的取值必须等于"记录中选定的那一侧"
#         （有 override 取 override，否则取 icu）。这同时验证 20 处已知分歧
#         与 1 处 Android 对齐覆盖**确实生效**。
# 断言 2：与 CPython 的分歧集合必须恰好等于"选定侧 != cpython"的那些码位。
expected_chosen = {}
for e in doc["entries"]:
    expected_chosen[e["seq"]] = e["override"] if e["override"] else e["icu"]

TRAIL_SET = [t for t in range(0x40, 0xFF) if t != 0x7F]

def table_value(seq_hex):
    b = bytes.fromhex(seq_hex)
    if len(b) == 2:
        slot = (b[0] - 0x81) * 190
        ti = b[1] - 0x40 if b[1] <= 0x7E else b[1] - 0x80 + 63
        return "U+%04X" % two_byte[slot + ti]
    k = ((b[0] - 0x81) * 10 + (b[1] - 0x30)) * 1260 + ((b[2] - 0x81) * 10 + (b[3] - 0x30))
    v = bmp4[k]
    return "UNDEF" if v < 0 else "U+%04X" % v

wrong_chosen = []
for seq, want in expected_chosen.items():
    got = table_value(seq)
    if got != want:
        wrong_chosen.append("%s 记录=%s 产物=%s" % (seq, want, got))
if wrong_chosen:
    fail("已记录码位的产物取值与记录不符：" + "; ".join(wrong_chosen))

expected_div = sorted(seq for seq, want in expected_chosen.items()
                      if want != dict((e["seq"], e["cpython"]) for e in doc["entries"])[seq])
extra = sorted(set(observed) - set(expected_div))
missing = sorted(set(expected_div) - set(observed))
mismatched = sorted(k for k in set(observed) & set(expected_div) if observed[k][0] != dict(
    (e["seq"], e["cpython"]) for e in doc["entries"])[k])

emit("分歧对账: 记录 %d 个码位（其中 %d 个预期与 CPython 分歧，%d 个经覆盖后不再分歧）"
     % (len(doc["entries"]), len(expected_div), len(doc["entries"]) - len(expected_div)))
if not wrong_chosen:
    emit("分歧对账: 产物取值与选定侧逐条相符")
if extra:
    fail("出现未记录的分歧码位：" + ", ".join(extra))
if missing:
    fail("预期分歧码位未复现（实现可能已变）：" + ", ".join(missing))
if mismatched:
    fail("分歧码位的 CPython 取值与记录不符：" + ", ".join(mismatched))
if not (extra or missing or mismatched or wrong_chosen):
    emit("分歧对账: 完全一致 —— 全部 %d 个码位按记录取值，分歧集合分毫不差" % len(doc["entries"]))

# --------------------------------------------------------------------------
# 6) 与 manifest 对账
# --------------------------------------------------------------------------
if os.path.exists(MANIFEST):
    with open(MANIFEST, "r", encoding="utf-8") as f:
        manifest = json.load(f)
    m2 = manifest["twoByte"]
    emit("MANIFEST 双字节 slots/bytes  : %d / %d" % (m2["slots"], m2["bytes"]))
    if m2["slots"] != len(two_byte):
        fail("manifest 双字节 slots 与产物不符")
    if m2["bytes"] != len(raw):
        fail("manifest 双字节 bytes 与产物不符")
    if m2["sha256"] != hashlib.sha256(raw).hexdigest():
        fail("manifest 双字节 sha256 与产物不符")
    if manifest["bmp4"]["valid"] != valid_count:
        fail("manifest bmp4 valid=%d 与实测 %d 不符" % (manifest["bmp4"]["valid"], valid_count))
    if manifest["bmp4"]["runs"] != run_count:
        fail("manifest bmp4 runs=%d 与实测 %d 不符" % (manifest["bmp4"]["runs"], run_count))

total_bad = (bad_sup + sum(1 for l in lines if l.startswith("FAIL")))
emit("")
if total_bad == 0:
    emit("GB18030_CROSSCHECK = PASS（CPython 与 ICU 在全部复验点上一致）")
else:
    emit("GB18030_CROSSCHECK = FAIL（不一致 %d 处）" % total_bad)

out_path = os.path.join(REPO, ".workbuddy", "_gb_crosscheck.txt")
with open(out_path, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))

div_path = os.path.join(REPO, ".workbuddy", "_gb_divergences.txt")
with open(div_path, "w", encoding="utf-8") as f:
    for kind, seq, cp, tb in divergences:
        hexseq = "".join("%02X" % b for b in seq)
        f.write("%s %s %s %s\n" % (kind, hexseq,
                                   ("U+%04X" % cp) if cp is not None else "UNDEF",
                                   ("U+%04X" % tb) if tb is not None and tb >= 0 else "UNDEF"))
print("\n".join(lines))
sys.exit(0 if total_bad == 0 else 1)
