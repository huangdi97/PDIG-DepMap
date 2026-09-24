# DECISION_LOG.md

## D-001 — Product canonical baseline

Status: ACCEPTED

`CANONICAL_DESIGN.md` is the only product/domain/schema/impact baseline.
Older v0.4.1/R1/R2 patch files are superseded.

## D-002 — Three-platform application stack

Status: ACCEPTED

Use:

- uni-app x Vapor + Vue 3 + TypeScript/UTS
- Android Kotlin adapter
- iOS Swift adapter
- HarmonyOS ArkTS adapter

Reason:
The product must be published on Android, iOS, and HarmonyOS while retaining a shared business core.

## D-003 — Platform storage

Status: ACCEPTED

- Android/iOS: SQLCipher + OS secure key storage
- HarmonyOS: ArkData encrypted relational storage + HUKS
- One logical Schema, platform-specific secure database adapters

## D-004 — MVP Impact scope

Status: ACCEPTED

Impact Kernel is payment-domain only.
Cross-capability inference is deferred.

## D-005 — Harmony 未执行用例必须按性质分桶，不得按类一刀切

Status: ACCEPTED

Harmony 的 91 条 canonical 用例中未执行的部分，必须逐条 caseId 归入
**三个性质不同的桶**，禁止按 category 整类记为 `BLOCKED_BY_RUNTIME`：

- `HOST_IMPLEMENTATION_MISSING` — 纯逻辑未移植，不需要任何设备能力
- `BLOCKED_BY_ENVIRONMENT` — 逻辑可行，但主机执行面缺能力（字符集转换）
- `BLOCKED_BY_RUNTIME` — 必须真实设备 / KDF / ArkData 才能判定

Reason:
按类一刀切会把本可执行的用例从分母里抹掉，那是**隐性降级门禁**：
把"没写"记成"设备才能测"，等于把没做的事伪装成外部阻塞。
实证：整类记账时 28 条被记为 blocked，逐条重新定性后 20 条是纯逻辑
（合法 UTF-8 输入 + 零平台 API），移植 `sources/Parsers.ets` 后全部可执行 ——
账目 65 → 85，分母 91 不变。

Consequence:
`HOST_IMPL_MISSING_CASES` 即使清空也保留在代码里，使"必须有逐条证据"
这件事继续成立；下次往回塞一条必须写明理由。

## D-006 — 导入文件以 Base64 内嵌，解码必须留在被测代码内

Status: ACCEPTED

conformance 的导入文件（CSV / OFX / QFX）以 **原始字节的 Base64** 内嵌进
测试 bundle，字节 → 文本的解码由**被测代码**完成，不得在读取侧预先 decode。

Reason:
若在读取侧先解码，"字节 → 文本"这一层就被移出被测范围，编码缺陷将永远测不到。
该设计的价值已被一次真实缺陷证实：`decodeBase64` 曾按完整 4 字符组分配输出
长度，长度 mod 3 ≠ 0 的文件尾部字节被静默截断（`EUR` → `EU`、`JPY` → `JP`，
且只错最后一行）。该缺陷只有在"解码属于被测代码"时才会表现为测试失败。

Consequence:
`decodeBase64` 现按 `rem = digits.length % 4` 补齐（`rem === 1` 判非法），
并已用 28 个导入文件逐一 round-trip 校验。
