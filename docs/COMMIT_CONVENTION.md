# COMMIT_CONVENTION.md — 提交规范（Engineering Baseline V1）

## 格式

```
<type>(<scope>): <subject>
```

type ∈：

| type | 用途 |
|---|---|
| feat | 新功能（如 `feat(source):`） |
| fix | 缺陷修复（如 `fix(impact):`） |
| test | 测试（如 `test(crypto):`） |
| refactor | 重构不改行为（如 `refactor(core):`） |
| docs | 文档（如 `docs(schema):`） |
| chore | 工程/构建/依赖（如 `chore(release):`） |
| perf | 性能 |
| security | 安全/隐私加固 |
| chore(engineering) | 工程基线类收口 |

scope 建议值：core / impact / crypto / parser / source / repository / migration / schema /
docs / engineering / release。

## 禁止

- `fix`、`update`、`123`、`修改一下` 等无信息 subject。
- 一次 commit 混合不相关改动（保持小步）。
- 提交：`.env` secrets / keystore / p12 / p8 / provisioning / 真实账单 / local user DB / 解密产物。

## 历史

不强制重写既有 history（历史 commit 保留原样）；本规范只约束「以后」。
