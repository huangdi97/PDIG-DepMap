# DEV_RELEASE_PROTOCOL.md — 未来版本发布协议（v0.3.0 closure 固化）

> 来源：v0.3.0 Final Contract & Evidence Closure（2026-09-27）。
> 背景：v0.3.0 实际发布为 `--no-ff` merge（`9e114d2`），与 Goal 要求的 ff-only 不符；
> 核验无制品/二进制/tag/规范影响（`RELEASE_FLOW_PROTOCOL_DEVIATION = DOCUMENTED`，Impact = PROCESS_ONLY），
> 为避免延续，自下一版本起固化如下协议。

## 1. 发布流程（强制）

```
1. release branch（如 release/vX.Y.Z）从 main 切出，冻结特性
2. 最终全量回归（见 §2 门禁清单）必须在 release 分支上全绿
3. 合入 main 必须为 ff-only：`git merge --ff-only <release-branch>`（禁止 --no-ff merge）
   - 若 ff-only 失败 → 说明 main 有新提交 → 先 rebase release 分支到 main 再 ff-only
4. 打 tag：`product-vX.Y.Z`（annotated），指向 main 最新 commit
5. 发布 GitHub Release：
   - 创建 draft release（assets + digest 齐备）
   - 发布前再跑一遍 §2 清单
   - 原位发布：`gh api -X PATCH repos/<owner>/<repo>/releases/<id> -f draft=false`（不移动 tag，不重建 release）
6. 发布后：更新 WORK_STATUS / BLOCKERS / RUNTIME_EVIDENCE_INDEX / FINAL_REPORT（如实，不复制旧结论）
```

## 2. 发布前门禁清单（全绿才允许 tag + 发布）

```bash
cd core
npm run check                 # format:check 必须包含 format:docs:check（防止文档 commit 漂移）
npm run check:full            # db-integrity / coverage / perf / deps
node tools/conformance/run.mjs  # codegen --check + fixture integrity + oracle + 平台报告 diff
```

- `format:docs:check` 是 hard gate：任何 docs commit 必须经 `npm run format:docs` 后再提交。
- iOS/Harmony 的 runtime 证据：iOS 走 macOS CI（workflow_dispatch 于发布分支 exact SHA）；Harmony host 走
  `PDIG_DEVECO_HOME=<DevEco> node tools/harmony/run-conformance-host.mjs`。
- Release draft 的每个 asset 必须带上 SHA256 digest（git 中 `EVIDENCE_SHA256SUMS.txt` 同步）。

## 3. 禁止（红线）

- 禁止 tag move / history rewrite / force push / 删除已发布 release。
- 禁止 `<PASS>` 声明未执行的门禁；`NOT_RUN`/`EXTERNAL_GATE`/`DEFERRED_HUMAN_VALIDATION` 必须如实标注。
- 禁止把工程缺口伪装成 external gate。
- 发布后不自动进入下一 MVP；由用户决定。

## 4. 本轮遗留（已知 open item，不属于历史改写）

- iOS XCUITest/iPad UI 测试最终绿态：分支 `closure/ios-xcuitest`（工程+根因修复已在分支），
  待一次全绿运行后并入 main（并按 §1 ff-only），随后更新 Final Gate Matrix。
- 下一打包轮次把 Windows `pdig.ico`（本轮已入树并接入 `nativeDistributions.windows.iconFile`）接入 NSIS 手工打包脚本。
