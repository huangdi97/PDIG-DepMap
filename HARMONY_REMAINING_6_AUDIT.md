# Harmony 剩余 6 条 canonical 用例 — 逐条审计

日期：2026-09-18
执行面：hvigor 本地单元测试（ArkTS，无设备、无模拟器）
门禁：`node tools/harmony/run-conformance-host.mjs`
结论口径：**91 = 85 已执行 + 2 环境缺失 + 4 设备运行时 + 0 未移植**

---

## 1. 为什么这份文件存在

`ConformanceRunner.ets` 与 `run-conformance-host.mjs` 都曾引用 `HARMONY_REMAINING_24_AUDIT.md`，
而该文件在仓库中并不存在 —— 一个指向不存在文档的引用，等于把"逐条论证"换成了一句
无法核查的承诺。本文件补齐它，并把 24 → 6 的账目变化交代清楚。

**账目迁移链（每一步都有对应的可执行证据，不是叙事）：**

| 阶段 | 已执行 | 未移植 | 环境缺失 | 设备运行时 |
|---|---|---|---|---|
| N3 基线（按类一刀切） | 57 | — | — | 28（整类） |
| 逐条重新定性后 | 63 | 20 | 2 | 4 |
| state-machine + timeline 落地后 | 65 | 20 | 2 | 4 |
| **ArkTS parser adapter 落地后（本轮）** | **85** | **0** | **2** | **4** |

分母 91 全程未变。变的只是"哪些被真实执行了"——这正是这个门禁存在的意义。

---

## 2. 未移植桶（HOST_IMPLEMENTATION_MISSING）：0 条

上一版的 20 条 parser 用例本轮全部迁入"已执行"，依据是它们的性质：

- 22 个导入文件中 20 个经实测是**合法 UTF-8**（只有 `csv-gb18030.csv` 与 `gbk.csv` 不是）；
- 解析逻辑为 CSV 词法 + 日期/金额解析 + OFX SGML 扫描 + 微信账单列规则，**零平台 API**；
- 缺的一直是 ArkTS 侧 adapter 移植（`sources/Parsers.ets`），不是设备能力。

把它们记成"设备才能测"会同时犯两个错：把没做的事伪装成外部阻塞，并让 91 的分母
少掉 20。现在 `HOST_IMPL_MISSING_CASES` 保留为空数组而非删掉整个分支 —— 下次有人
往回塞一条，必须像下面这两桶一样写出逐条理由。

### 本轮移植暴露的一个真实缺陷（记录在案，防回归）

`crypto/DepmapBounds.ets` 的 `decodeBase64` 原按 **完整 4 字符组** 分配输出长度：

```
outLength = (digits.length / 4 | 0) * 3     // 错：忽略尾部残缺组
```

Base64 末尾 2 个有效字符 + `==` 仍编码 1 个真实字节，3 个有效字符 + `=` 编码 2 个。
旧公式把这部分静默丢弃，于是**长度 mod 3 ≠ 0 的文件会丢失最后 1–2 个字节**。

表现极隐蔽：只错最后一行，且金额列解析正常 —— `EUR` 变 `EU`、`JPY` 变 `JP`。
两条用例（parser-csv-eu-semicolon / parser-csv-multi-currency）因此失败。

修法：

```
const rem = digits.length % 4;
if (rem === 1) throw ...('truncated base64 group');       // 非法 base64
outLength = (digits.length >> 2) * 3 + (rem === 0 ? 0 : rem - 1);
```

已用 28 个导入文件逐一 round-trip 校验通过。此前不暴露，是因为只有 depmap 的
salt / tag / ciphertext 走过这条路径，而那些字段长度恰好整除。

**教训（写进门禁注释）**：导入文件以 Base64 内嵌、解码留在被测代码内，是刻意的 ——
若在读取侧先 decode，"字节 → 文本"就被移出被测范围，这类编码缺陷将永远测不到。

---

## 3. 环境缺失（BLOCKED_BY_ENVIRONMENT）：2 条

| caseId | 阻塞原因 | 解除条件 |
|---|---|---|
| `parser-csv-gb18030` | 输入字节非合法 UTF-8，必须先做 GB18030 解码 | 主机执行面可用的字符集解码器 |
| `parser-wechat-gb18030` | 同上 | 同上 |

这两条的性质是"环境缺能力"，不是"没写"：

- 主机执行面无 `util.TextDecoder`（`@kit.ArkTS` 在本地单元测试中不加载）；
- 自造一张 GB18030 → UTF-16 码表既无必要，也无从验证其正确性 —— 一张没人能验证的
  码表比"明确记为环境缺失"更危险，它会让门禁看起来是绿的而实际是猜的。

**不做的理由必须写明**：误把它们归入"未移植"会诱导有人去写一个无法验证的解码器，
从而把"BLOCKED_BY_ENVIRONMENT"悄悄变成"假绿 PASS"。

---

## 4. 设备运行时（BLOCKED_BY_RUNTIME）：4 条

| caseId | 必需的真实能力 | 为什么主机原理上不可执行 |
|---|---|---|
| `depmap-golden-v1` | Argon2id（NAPI 原生 `.so`）+ AES-256-GCM | expected 含 `derivedKeyHex` / `ciphertextBase64` / `tagBase64`，必须真实 KDF 产出；主机无法加载 OHOS ABI 的 native 库 |
| `depmap-utf8-password-normalization` | 同上 + 口令 UTF-8 规范化 | 同上 |
| `backup-depmap-export-restore-roundtrip` | ArkData 真实 DB + 加密 + 往返 | 需要"导出 → 加密容器 → 恢复到新库 → 二次导出逐字节相同"的完整链路 |
| `migration-db-v1-to-v3` | relationalStore + v1 schema 种子数据 | 需要真实数据库实例与迁移执行 |

注意 `depmap-bounds-and-structure-rejection` **不在**这张表里：协议明确规定
「边界校验必须发生在 KDF 之前」，该用例按设计就不该触发 Argon2id。
把结构/边界层从依赖 `@kit.CryptoArchitectureKit` 的模块中分层抽出
（`crypto/DepmapBounds.ets`）后，它已作为真实执行用例入账 —— 这条正是
"按类一刀切会抹掉可执行用例"的实证。

---

## 5. 复核方式

```bash
PDIG_DEVECO_HOME="D:/Code/Harmony/DevEco Studio" node tools/harmony/run-conformance-host.mjs
```

预期输出：

```
[conformance-host] 汇总   : run=89 pass=89 fail=0 error=0
[ canonical ] HARMONY_TOTAL_CANONICAL    = 91
[ canonical ] HARMONY_HOST_EXECUTED      = 85   (fail=0)
[ canonical ] HARMONY_HOST_IMPL_MISSING  = 0
[ canonical ] HARMONY_ENV_BLOCKED        = 2
[ canonical ] HARMONY_DEVICE_BLOCKED     = 4
HARMONY_CONFORMANCE_HOST=PASS
HARMONY_HOST_PASS=85/91
```

89 = 85 canonical + 3 条 conformance 元测试 + 1 条 domain 自检。
三个桶的计数由测试内 `accountingSplitMatchesSection11` **独立**核对一遍，
不由 89 这个检查数反推 —— 检查数是执行面口径，canonical 是规范口径，两者不可互换。

---

## 6. 已知非确定性（不影响结论）

hvigor 本地单元测试在本机存在**间歇性挂起**：同样的命令有时 40s 完成，有时在
`BuildUnitTestHook` 之后无进展并被 `timeout 300` 杀掉（EXIT=124）。重跑即可通过。
这不是被测代码的问题（挂起发生在测试执行之前的构建阶段），但会浪费一次 5 分钟等待，
排查时不要误判为编译失败。
