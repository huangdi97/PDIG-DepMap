# CHANGE_RISK_POLICY.md — 变更风险分级（Engineering Baseline V1）

> 修改前先定级；级别决定必须通过的 Gate（与 docs/DEFINITION_OF_DONE.md 联动）。

## LOW

docs / 注释 / UI 文案 / 格式化。
Gate：format + lint；无需全量回归。

## MEDIUM

Parser / Source Adapter / Repository / services（导入编排、确认服务）/ resolver / fixtures。
Gate：LOW 全部 + focused tests + `npm run check`（全量测试回归）+ 若新增 Adapter：contract 注册。

## HIGH

Impact kernel / Crypto / Migration / Fingerprint / Dependency-Group 语义 / RelationRegistry /
Schema / 安全存储 / proposal 生命周期。
Gate：MEDIUM 全部 + invariant & property 套件 + 对应 FAIL_CLOSED_MATRIX 行不回归 +
PERFORMANCE_BASELINE 对照（impact/crypto 路径）+ 人工 review 证据（commit message 或
WORK_STATUS 记录语义论证）。

## 变更落位速查

| 要动的东西                        | 级别   | 必读                                                                    |
| --------------------------------- | ------ | ----------------------------------------------------------------------- |
| `src/impact/kernel.ts`            | HIGH   | CANONICAL §7、TEST_STRATEGY（P1–P6/T1–T12）、FAIL_CLOSED F-11~F-15      |
| `src/crypto/depmap.ts`            | HIGH   | CRYPTO_PROTOCOL、golden 冻结、FAIL_CLOSED F-01~F-04                     |
| `src/schema/migrations.ts`        | HIGH   | SCHEMA_V1/V2、MIGRATION_V1_V2、长期规则（TEST_STRATEGY Migration Gate） |
| `src/sources/*/adapter.ts`        | MEDIUM | SOURCE_ADAPTER_CONTRACT、adapter-contract C0–C6                         |
| `src/domain/relation-registry.ts` | HIGH   | CANONICAL 语义铁律、INV5/INV6                                           |
| `vitest.config.ts` / lint 配置    | MEDIUM | COVERAGE_POLICY（exclude 白名单）、ENGINEERING_STANDARDS                |
