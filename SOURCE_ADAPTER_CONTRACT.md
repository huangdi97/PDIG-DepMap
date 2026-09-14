# SOURCE_ADAPTER_CONTRACT.md

```ts
interface EvidenceSourceAdapter {
  readonly id: string
  readonly version: number
  readonly sourceKind: SourceKind
  readonly coverageMode: CoverageMode
  readonly authoritativeFor: readonly string[]
  canHandle(input: SourceInput): Promise<number>
  parse(input: SourceInput, context: SourceContext): Promise<Observation[]>
  normalize(observations: readonly Observation[], context: SourceContext): Promise<NormalizedObservation[]>
}
```

## SourceKind

`statement_file | platform_export | open_banking | manual | discovery`

MVP02 使用 `statement_file`。

## CoverageMode

`event_stream | partial_snapshot | complete_snapshot | user_selected`

MVP02 三个 Adapter 全部 `event_stream`。

## authoritativeFor

MVP02 全部 `[]`。

因此 Adapter：

- 不自动创建 Reality
- 不根据 absence retire Dependency
- 不自动确认 backup
- 不产生 required

## Adapter 禁止

不得：写 Graph、写 Dependency/Group、保存 raw statement、输出秘密、调用业务网络、触发 destructive action。

## NormalizedPaymentObservation

至少：sourceInstanceId、adapterId、sourceTxnId?(memory only)、occurredAt、amount、currency?、direction、description?、counterparty?、balance?、transactionType?、paymentMethodHint?。

Unknown 保持 unknown。

## Determinism

相同 adapter version + input + mapping profile 必须 deterministic。

## Versioning

Parsing/Fingerprint 语义发生变化必须升级 adapterVersion，禁止静默改语义。
