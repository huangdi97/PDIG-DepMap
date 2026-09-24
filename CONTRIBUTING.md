# CONTRIBUTING.md

> 本仓库当前为**单人开发 + Agent 辅助**模式。本文件的目的不是引入大量流程，
> 而是把「不可让渡的工程纪律」写清楚，避免后来者（人或 Agent）无意破坏它。

---

## 0. 先读这四份

| 顺序 | 文件                                 | 为什么                            |
| ---- | ------------------------------------ | --------------------------------- |
| 1    | `CANONICAL_DESIGN.md`                | 唯一产品 / Schema / Impact 母版   |
| 2    | `WORK_STATUS.md`                     | 当前阶段、当前 Gate、当前 blocker |
| 3    | `docs/ENGINEERING_STANDARDS.md`      | 工程基线                          |
| 4    | `docs/AGENT_DEVELOPMENT_PROTOCOL.md` | 变更纪律与取证口径                |

---

## 1. 变更顺序（不可颠倒）

```
spec/  →  tools/codegen  →  三端实现  →  fixtures/  →  conformance
```

1. **先改 `spec/`**（Canonical Spec 是唯一真源）
2. 跑 `node tools/codegen/generate.mjs`（**不要手改 generated 文件**，头部有 `DO NOT EDIT`）
3. 改平台实现
4. 补 / 改 `fixtures/` 的平台中立用例
5. 跑 `node tools/conformance/run.mjs`

**违反顺序的 PR 会被拒绝**——例如先改 Kotlin 再补 spec，会让三端漂移。

---

## 2. 绝对禁止

| 禁止项                                               | 原因                                    |
| ---------------------------------------------------- | --------------------------------------- |
| 删除或跳过测试以让门禁变绿                           | 这是本项目最严重的违规                  |
| 降低 strict / lint 等级                              | 类型安全基线不可回退                    |
| 用 synthetic 数据冒充真实数据取证                    | Real Data Gate 必须真实，否则标 NOT_RUN |
| 自研密码学原语（Argon2 / AES）                       | 必须链接经过审计的实现                  |
| 为图方便把 Argon2id 换成 PBKDF2                      | 容器格式是跨平台契约                    |
| 机器推断设置 `criticality=required`                  | 第一原则：required 只能由用户设置       |
| 让不同 SourceInstance 共享 fingerprint 命名空间      | 会造成跨源误合并                        |
| 用 `git reset --hard` / `git clean -fd` / force push | 会破坏冻结历史                          |
| 提交真实个人金融数据                                 | 见 `SECURITY.md`                        |

⚠️ **额外的仓库特有风险**：本工作区的 `git commit` 可能出现
「对象创建成功但 HEAD 不推进」（loose ref 被外部进程回收）。
**每次提交后必须复查 `git rev-parse HEAD` 与 `git status --short -uall`，不只看返回码。**

---

## 3. 提交前检查

```bash
# Canonical（必须）
node tools/codegen/generate.mjs --check
node tools/conformance/run.mjs

# Legacy Behavior Oracle（改动 core/ 时）
cd core && npm test && npm run check

# Android（改动 android/ 时）
cd android && ./gradlew --no-daemon :core:test :conformance:run

# Harmony（改动 harmony/ 时）
node tools/harmony/build-ascii-mirror.mjs
```

提交信息约定见 `docs/COMMIT_CONVENTION.md`（Conventional Commits）。

---

## 4. 状态声明口径（必须诚实）

| 等级               | 含义       | 何时可用              |
| ------------------ | ---------- | --------------------- |
| `SOURCE_READY`     | 源码完成   | 任何时候              |
| `BUILD_READY`      | 可编译     | 有编译证据            |
| `TESTED`           | 测试通过   | **有实跑输出**        |
| `RUNTIME_VERIFIED` | 运行时验证 | **有真机/模拟器证据** |
| `NOT_RUN`          | 未执行     | 无环境时必须如实标    |
| `BLOCKED_BY_*`     | 外部阻塞   | 必须写清阻塞源        |

**不允许**：因为"代码看起来对"就写 PASS；因为"没有设备"就写 RUNTIME_VERIFIED。

---

## 5. 文档更新义务

改动涉及以下内容时，**必须同步更新**：

- `WORK_STATUS.md`（当前状态）
- 受影响平台的状态矩阵（`NATIVE_MIGRATION_STATUS.md` / `NATIVE_PARITY_MATRIX.md` /
  `CROSS_PLATFORM_CONFORMANCE_MATRIX.md`）
- `README.md`（若命令或结构变化）
- `BLOCKERS.md`（若新增/解除外部 blocker）

---

## 6. Issue / PR 模板

- Bug：`.github/ISSUE_TEMPLATE/bug_report.yml`
- 需求：`.github/ISSUE_TEMPLATE/feature_request.yml`
- PR：`.github/pull_request_template.md`

**Bug 报告强制要求**：平台 + commit + Gate + 复现步骤 + 期望/实际 + 日志是否已脱敏。
**禁止在公开 issue 中贴真实个人数据**——详见 `SECURITY.md`。
