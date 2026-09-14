# SOURCE_ARCHITECTURE.md（MVP02 Global Source Abstraction）

> 状态：IMPLEMENTED + TESTED（Core/Node）。对应 GOAL_MVP02_GLOBAL_SOURCE §6/§19、
> MVP02_ARCHITECTURE_FREEZE、SOURCE_ADAPTER_CONTRACT.md。

## 1. 目标

把「微信账单解析」从 Domain 特例降级为**普通数据源 Adapter**，使同一套
Domain 语义（Observation → Evidence → Proposal → 人工确认 → Dependency/Group
→ Impact → ChangePlan）可以服务任意文件账单来源，且未来新增来源不需要改 Domain。

## 2. 分层

```text
SourceInstance（哪个来源，持久化）
      ↓
EvidenceSourceAdapter（如何解析，无状态策略对象）
   parse → normalize → (可选 suggestRoutes)
      ↓
ImportCoordinator（source-neutral 管线，唯一业务入口）
   begin → resolveMerchant → finalize
      ↓
fingerprint(source-scoped) → resolver → Evidence(按流) → DependencyProposal
      ↓
人工确认（confirmation-service，唯一写 Reality 的入口）
```

- Adapter 只产出 Observation / 建议，**永不**写 Dependency / Group /
  required / Graph；不持久化 raw statement；不调用网络。
- source-specific 逻辑（如微信 funding_source 推断）唯一合法位置是
  `Adapter.suggestRoutes`；Domain 层禁止 `if source == wechat` 分支。
- 三个 MVP02 Adapter：`wechat_statement`（`src/sources/wechat/adapter.ts`）、
  `generic_csv`（`src/sources/generic-csv/adapter.ts`）、`ofx_qfx`
  （`src/sources/ofx/adapter.ts`）。

## 3. Adapter 契约（src/sources/types.ts）

```ts
interface EvidenceSourceAdapter {
  id: string; version: number
  sourceKind: 'statement_file' | ...
  coverageMode: 'event_stream' | ...
  authoritativeFor: readonly string[]
  canHandle(input): Promise<number>            // deterministic 0..1
  parse(input, ctx): Promise<Observation[]>
  normalize(obs, ctx): Promise<NormalizedPaymentObservation[]>
  suggestRoutes?(norm, ctx, resolved): RouteSuggestion[]  // 可选，仅提议
  lastParseErrors?(): {line,reason}[]           // 可选，不进观测流
}
```

不变量：

- 相同 adapter version + input + mapping ⇒ deterministic 输出（×50 测试）。
- Parsing/Fingerprint 语义变化必须升级 `adapterVersion`。
- MVP02 文件 Adapter 强制 `coverageMode='event_stream'`、
  `authoritativeFor=[]`、`sourceKind='statement_file'`
  （`assertFileAdapterContract` 抛错强制）。

## 4. Coverage 语义

`event_stream` 意味着：**导入里没出现 ≠ 现实中不存在**。absence 不 retire
Dependency、不 reject Proposal、不 fail Group、不产生 must_change
（见 docs/MULTISOURCE_EVIDENCE.md §4 与 tests/integration/coverage-semantics.test.ts）。

## 5. 测试证据

- 契约：H0/H0b（assertFileAdapterContract 对三 Adapter）
- WeChat 普通化回归：MVP01 parser 18 fixtures + pipeline 全 PASS
- Generic CSV：20 用例 / OFX+QFX：15 用例 / multi-source E2E：9 用例
- 总量：259/259 PASS（0 skip）；quality gates 全绿（2026-09-13 实跑）
