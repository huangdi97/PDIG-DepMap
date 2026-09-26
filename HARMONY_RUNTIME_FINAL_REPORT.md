# HARMONY_RUNTIME_FINAL_REPORT.md

> 轮次：2026-09-26（spec §49-§71）；平台：HarmonyOS（ArkTS Stage Model）
> 环境：DevEco Studio `D:\Code\Harmony\DevEco Studio`（PDIG_DEVECO_HOME），ASCII mirror `C:\Users\Kaiser\pdig-harmony-build`
> 证据 SHA：73b0216（收口后刷新）

## 1. 最终 Gate
```text
HARMONY_HOST            = 142/142 PASS（新鲜：run=142 pass=142 fail=0 error=0）
HARMONY_CANONICAL       = 87/91 host 执行 PASS（fail=0）；4 条 device-blocked（详 §3）
HARMONY_RUNTIME         = BLOCKED（E-9：emulator 系统镜像缺失 -> hdc list targets=[Empty]）
HARMONY_ARKDATA         = BLOCKED（需 runtime）
HARMONY_HUKS            = BLOCKED（需 runtime）
HARMONY_NAPI_ARGON2     = IMPLEMENTED（libpdiargon2.so 双 ABI 已入 HAP）/ ON_DEVICE_NOT_RUN
HARMONY_PAGE_TOTAL      = 1（pages/Index 占位，产品页未实现）
HARMONY_PAGE_SCREENSHOTTED = 0（无 runtime）
HARMONY_FUNCTION_TOTAL  = 142（host checks）+ 91（canonical 口径）
HARMONY_FUNCTION_PASS   = 142
HARMONY_CORE_JOURNEY    = PASS（host：CoreJourneyHost 8 用例；设备面未跑）
HARMONY_THREE_SCENARIOS = host 覆盖（ImpactKernel/readiness/coverage 于 87 canonical 内）；设备面未跑
N3_HARMONY_FULL_PARITY  = 未达（唯一外部阻断 = E-9 模拟器镜像；host 面保持全绿）
```

## 2. Fresh 实跑证据（本轮）
1. `node tools/harmony/run-conformance-host.mjs` → **142/142 host checks**（87 canonical + 3 元测试 + 1 domain 自检 + 14 persistence + 17 repository + 12 import-pipeline + 8 core-journey），`HARMONY_CONFORMANCE_HOST=PASS`（BUILD SUCCESSFUL in ~1min）。
2. Clean `assembleHap`（build-ascii-mirror 机制）→ `BUILD SUCCESSFUL in 13s`；`entry-default-unsigned.hap` 3.4MB，**sha256=80beb459d8e8eeaf2928389610ea06bb6e3825c407e4478b5758c9425369f286**（2026-09-26 16:49）。
3. `scripts/harmony/harmony_emulator_preflight.ps1` 运行输出（精确证据）：
   - DevEco root / SDK hms+openharmony / hdc.exe(3.1.0b) / Emulator.exe：**OK**
   - `hdc list targets` → `[Empty]`；Emulator 系统镜像：`deploy dir present=False; no .img found` → **BLOCKER×2**
   - `PREFLIGHT_RESULT=BLOCKED`；磁盘 G:\ 215.8GB free（环境无磁盘问题）。

## 3. 4 条 canonical DEVICE-BLOCKED —— 逐条实际尝试与记录（§54）
统一按 §54 记录：fixture / runtime / command / result / log。执行面 = 设备 runtime（唯一）→ Emulator；结果：**无法启动 → BLOCKED**（精确原因均为 E-9）。

| # | fixture id | runtime | command | result | 阻断根因 |
|---|---|---|---|---|---|
| H1 | depmap-golden-v1 | Harmony 设备 | `hvigor test` 已执行该用例 → runner 报 no-result → BLOCKED | 未执行 | Argon2id native 需设备（E-9） |
| H2 | depmap-utf8-password-normalization | Harmony 设备 | 同上 | 未执行 | Argon2id native 需设备 |
| H3 | migration-db-v1-to-v3 | Harmony 设备 | 同上 | 未执行 | ArkData 物理 DB 需设备 |
| H4 | backup-depmap-export-restore-roundtrip | Harmony 设备 | 同上 | 未执行 | Argon2 + ArkData 需设备 |
> 不沿用历史 blocker：本轮以 preflight 实际执行 + hdc 实查 + 镜像目录实查为据（见 §2.3），结论与 E-9 外部阻断一致。

## 4. ArdData/Migration/Transaction 设备面（§56-§58）
架构已绑定（data/ 层 GraphStore/GraphRepository/MigrationChain 真实 ArkTS 编译入 HAP；关键约束同事务在 host 面已断言：MigrationChain ×50 幂等 / failAtStep 回滚 / confirmed+revision 同事务双回滚 / 幂等重导不 bump）。**物理 relationalStore 执行面 = BLOCKED（E-9）**，不冒充。

## 5. UI（§63-§69）
`pages/Index` 单占位页；产品级页面 NOT_IMPLEMENTED（matrix §5 Harmony 列如实记录）。运行时可用后按 §63/§68 补全页面截图。