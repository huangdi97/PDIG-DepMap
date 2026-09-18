// DO NOT EDIT 之外的语义 — 语义必须与 TS 冻结基准一致
// 端口自 core/src/repositories/graph-revision.ts（MVP03 §9–§11）
//
// 分层约束：本模块只保留**语义判定**（什么该提升 revision、什么不该），
// **不依赖 SqliteDriver / relationalStore**。读写持久值属于 data 层。
//
// 为什么值得单独成模块：
//   "哪些操作提升 graphRevision"是一条**正确性铁律**，而不是实现细节。
//   它决定了 ChangePlan 何时被判为 stale、Timeline 何时提示需重校验。
//   把它集中在纯函数里，才能在不接触数据库的情况下逐条验证 ——
//   而在 DB 层里散落 `bump()` 调用时，没有任何测试能穷举"哪些路径漏了/多了"。
//
// 铁律（原文照录）：
//   GraphRevision 是 Confirmed Reality Graph 的**语义版本**。
//   仅 Reality mutation 提升（在同一次 DB 事务内）：
//     Dependency created / retired / reactivated / criticality 被用户修改
//     DependencyGroup confirmed / retired / membership 确认变化
//   以下一律不提升：
//     Observation / ImportSession / Fingerprint / Evidence /
//     Proposal（创建与 confidence）/ RealityDrift / DiscoveryCandidate /
//     Timeline projection / UI 状态。

/**
 * 会被提升 graphRevision 的 Reality mutation 种类 —— **契约用词，不是内部发明**。
 *
 * ⚠ 2026-09-18 修正（由真实执行发现，不是代码审阅发现的）：
 *   本函数原先用一套自造的字符串（`dependency_created` / `group_confirmed` …），
 *   理由是"这属于 Harmony 端内部实现细节，不是跨端数据格式的一部分"。
 *   **那个理由是错的。** mutation 种类名就是**跨端共享的冻结词汇表**：
 *     * `spec/state-machines/state-machines.json` → machines[].bumpsOn
 *     * `fixtures/state-machine/state-machine-graph-revision.json` → expected.bumpsOn
 *     * `android/core/.../StateMachines.kt` → GraphRevisionMachine.bumpsOn
 *   三处都用 `node_create` / `dependency_confirm_create` / `drift_resolve_*` 这套词。
 *   自造词汇的后果是：契约里每一个真实 mutation 都被判为"不提升"，
 *   而 `drift_resolve_*` 等条目连对应常量都没有。
 *
 *   为什么代码审阅没发现：`DomainSelfCheck` 用的是**同一套自造词**去断言同一套自造词，
 *   自洽地全绿。这正是本项目反复强调的那条 —— **自比自的检查不构成证据**。
 *   只有拿冻结 fixture 真跑一遍才暴露出来。
 *
 * 为什么不放进 `CanonicalEnums.ets`：
 *   那是 tools/codegen/generate.mjs 从 spec/domain/domain.json 生成的三端共享枚举，
 *   为一个判别而新增 canonical enum 会改动冻结契约并要求 iOS/Android 重新生成，
 *   超出 N3 范围。这里用**普通字符串常量承载契约原文**即可 ——
 *   常量值必须与上面三处逐字一致。
 */
export const MUT_NODE_CREATE: string = 'node_create';
export const MUT_NODE_UPDATE: string = 'node_update';
export const MUT_NODE_ARCHIVE: string = 'node_archive';
export const MUT_DEPENDENCY_CONFIRM_CREATE: string = 'dependency_confirm_create';
export const MUT_DEPENDENCY_CONFIRM_UPDATE: string = 'dependency_confirm_update';
export const MUT_DEPENDENCY_RETIRE: string = 'dependency_retire';
export const MUT_DEPENDENCY_REACTIVATE: string = 'dependency_reactivate';
export const MUT_GROUP_CONFIRM: string = 'group_confirm';
export const MUT_GROUP_RETIRE: string = 'group_retire';
export const MUT_DRIFT_RESOLVE_REPLACEMENT: string = 'drift_resolve_replacement';
export const MUT_DRIFT_RESOLVE_ADDITIONAL_PATH: string = 'drift_resolve_additional_path';

/** 持久化键名（与 TS 基准一致：复用 meta 表，初始 0，不做 event sourcing）。 */
export const GRAPH_REVISION_KEY: string = 'graph_revision';

/**
 * 判定一次操作是否提升 graphRevision。
 *
 * 这是一个**白名单**而非黑名单：只有被显式列为 Reality mutation 的操作才提升。
 * 用白名单的理由是失效方向 —— 未来新增一种实体/事件时，
 * 黑名单会默认"提升"（把非 Reality 变化误记成 Reality 变更，
 * 让所有计划无端变 stale），白名单则默认"不提升"（最多漏一次提示，不会误报）。
 * 本项目的取向一贯是宁可漏报，不可把不确定伪装成必须处理。
 *
 * 白名单内容 = `spec/state-machines/state-machines.json` 的 GraphRevision.bumpsOn，
 * 逐条对应，不多不少。
 */
export function doesMutationBumpRevision(kind: string): boolean {
  if (kind === MUT_NODE_CREATE) {
    return true;
  }
  if (kind === MUT_NODE_UPDATE) {
    return true;
  }
  if (kind === MUT_NODE_ARCHIVE) {
    return true;
  }
  if (kind === MUT_DEPENDENCY_CONFIRM_CREATE) {
    return true;
  }
  if (kind === MUT_DEPENDENCY_CONFIRM_UPDATE) {
    return true;
  }
  if (kind === MUT_DEPENDENCY_RETIRE) {
    return true;
  }
  if (kind === MUT_DEPENDENCY_REACTIVATE) {
    return true;
  }
  if (kind === MUT_GROUP_CONFIRM) {
    return true;
  }
  if (kind === MUT_GROUP_RETIRE) {
    return true;
  }
  if (kind === MUT_DRIFT_RESOLVE_REPLACEMENT) {
    return true;
  }
  if (kind === MUT_DRIFT_RESOLVE_ADDITIONAL_PATH) {
    return true;
  }
  return false;
}

/**
 * 解析持久化的 revision 值。
 *
 * 与 TS 基准的容错一致：缺失 → 0；非有限数 → 0。
 * 注意**不**把 `null`/`NaN`/负值当成有效版本 ——
 * revision 是单调递增的计数器，负数会让所有 stale 判定反转。
 */
export function parseStoredRevision(raw: string | null): number {
  if (raw === null || raw.length === 0) {
    return 0;
  }
  const v = Number(raw);
  if (!Number.isFinite(v)) {
    return 0;
  }
  if (v < 0) {
    return 0;
  }
  return v;
}

/** 下一次 revision（+1）。恢复/回滚语义由调用方的事务保证，不在纯函数里处理。 */
export function nextRevision(current: number): number {
  return current + 1;
}

/**
 * 判定"计划相对于当前图版本是否需要重校验"。
 *
 * 与 PlanReadiness 的 isPlanStale 是同一口径的两处表达：
 * 这里以纯数字给出，便于在数据层与 UI 层复用而不引入 ChangePlan 结构依赖。
 */
export function revisionRequiresRevalidation(
  lastAnalyzedGraphRevision: number,
  currentGraphRevision: number,
): boolean {
  return currentGraphRevision > lastAnalyzedGraphRevision;
}
