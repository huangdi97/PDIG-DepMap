# COMMIT_CONVENTION.md — 提交规范（Engineering Baseline V1）

## 格式

```
<type>(<scope>): <subject>
```

type ∈：

| type               | 用途                                   |
| ------------------ | -------------------------------------- |
| feat               | 新功能（如 `feat(source):`）           |
| fix                | 缺陷修复（如 `fix(impact):`）          |
| test               | 测试（如 `test(crypto):`）             |
| refactor           | 重构不改行为（如 `refactor(core):`）   |
| docs               | 文档（如 `docs(schema):`）             |
| chore              | 工程/构建/依赖（如 `chore(release):`） |
| perf               | 性能                                   |
| security           | 安全/隐私加固                          |
| chore(engineering) | 工程基线类收口                         |

scope 建议值：core / impact / crypto / parser / source / repository / migration / schema /
docs / engineering / release。

## 禁止

- `fix`、`update`、`123`、`修改一下` 等无信息 subject。
- 一次 commit 混合不相关改动（保持小步）。
- 提交：`.env` secrets / keystore / p12 / p8 / provisioning / 真实账单 / local user DB / 解密产物。
- **在本环境用 `rebase` 处理分叉**（`git rebase` / `rebase --onto` / 交互式 rebase）。
  它是长事务，被超时杀死后触发的 gc 会剪掉"因 ref 混乱显得不可达"的对象，
  造成不可恢复的丢失 —— 2026-09-18 已真实发生一次。
  分叉改用「remote tip + 当前 worktree 树 → `commit-tree` 显式指定父」或 fresh clone，
  详见 `GIT_OPS_INCIDENT_AND_RULES.md` §2–§3。

## 历史

不强制重写既有 history（历史 commit 保留原样）；本规范只约束「以后」。
