# TYPE_SAFETY_AUDIT.md — 类型逃逸审计（RC PHASE F）

> 扫描：`grep -rnE ": any|as any|@ts-ignore|@ts-expect-error" core/src` + non-null 断言 + `as` casts
> 日期：2026-09-12

## 结论：关键路径 0 unsafe escape

| 逃逸类型 | core/src 数量 | 评估 |
|---|---|---|
| `: any` 显式注解 | **0** | — |
| `as any` | **0** | — |
| `@ts-ignore` / `@ts-expect-error` | **0** | — |
| non-null 断言 `!.` | **0**（RC 前为 4，已重构为显式 guard + throw） | eslint `no-non-null-assertion: error` 强制 |
| `as X` 断言 | 76 处 | 见下方分类审计 |
| eslint typed-lint unsafe-* 规则 | 全部 error，0 违规 | no-unsafe-assignment/call/member-access/return/argument |

## `as` 断言分类审计（76 处）

1. **SQLite 行映射（≈65 处，repositories/*）**：`row.capability as Capability` 等。
   性质：SQL 返回行是 `Record<string, unknown>`；列值来自本包写入的受 CHECK 约束的枚举
   （v1 DDL 已加固 capability/relation/criticality/state/origin/mode CHECK）。
   断言紧跟 SELECT 后逐列映射，且 `String()`/`Number()` 先行规范化。**可接受**。
2. **node-driver（2 处）**：`params as never[]` —— node:sqlite 参数类型与 SqlValue 的桥接，
   集中在唯一驱动文件。**可接受**（平台差异隔离点）。
3. **JSON payload 收窄（graph-serialize 3 处、jcs 1 处）**：全部跟在显式运行时 guard
   （isPlainObject / typeof 检查 / Array.isArray）之后。**可接受**。
4. **Evidence/ImportSession getById 后断言非空（4 处）**：INSERT/UPDATE 后立即按 id 重读，
   行必然存在；若并发删除则 `as Evidence` 掩盖 undefined —— 已在 AE 事务化后不可达
   （同事务内重读）。**可接受，已标注**。

## 关键路径扫描（GOAL §F 要求）

impact / crypto / parser / fingerprint / proposal / repository / migration：
- 无 `any`、无 `@ts-ignore`、无非空断言（修复后）
- tsc strict + noUncheckedIndexedAccess + noImplicitReturns + noImplicitAny + noImplicitOverride 全开
- 无文件级 eslint 关闭（本仓库无生成代码）

复现命令：

```bash
cd core
grep -rnE ": any|as any|@ts-ignore|@ts-expect-error" src   # 0 hits
grep -rnE "\w!\." src                                       # 0 hits
npm run lint                                                # 0 errors
npm run typecheck                                           # 0 errors
```
