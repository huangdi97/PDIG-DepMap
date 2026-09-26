// 迁移链（schema v1→v2→v3 的 in-memory 应用语义）—— 纯 ArkTS，零 @ohos。
//
// 对应 core/src/schema/migrations.ts 的迁移链 + migration-spec.md §1.4 强制要求：
//   - 每个 version 一个事务；失败回滚（停在旧版本，**不留半迁移状态**）
//   - 幂等：已是最新版本 → 严格 no-op（重复执行 ×50 零漂移）
//   - IDs / decisions / groups / sources 保留（本层不触碰数据行，只推进版本；
//     legacy 数据归属由 PayloadMigration 的 payload 迁移负责）
//   - legacy SourceInstance **只创建一次**（条件插入）
//   - **迁移不得 bump graph_revision**
//
// 诚实边界：change_plans / reality_drifts / discovery_candidates 是 schema v3
// 的 DB 层表（设备），本内存层以 schemaVersion 记录其存在性，不伪造行数据。
//
// 分层约束：依赖 ../data/{SchemaV3,GraphStore,GraphRepository}。

import { SCHEMA_VERSION } from '../data/SchemaV3';
import { GraphRepository } from '../data/GraphRepository';

export class MigrationChainError extends Error {
  code: string = 'migration_failed';

  constructor(code: string, message: string) {
    super(message);
    this.code = code;
  }
}

/**
 * 应用 schema 迁移链 1 → 2 → 3（全有或全无）。
 * `failAtStep` 仅测试用：1 = v1→v2 步失败注入；2 = v2→v3 步失败注入；
 * 0 = 正常路径。
 */
export function applySchemaMigrations(repo: GraphRepository, failAtStep: number): void {
  repo.withTransaction(() => {
    const from = repo.getSchemaVersion();
    if (from >= SCHEMA_VERSION) {
      return; // 幂等：已最新 → no-op
    }

    // v1 → v2：注入 legacy SourceInstance（条件插入一次）并推进版本
    if (failAtStep === 1) {
      throw new MigrationChainError('migration_injected_failure', 'injected v1->v2 failure');
    }
    repo.ensureLegacySourceInstance();
    repo.setSchemaVersion(2);

    // v2 → v3：推进版本（v3 新增三表在内存层仅以 schemaVersion 记录）
    if (failAtStep === 2) {
      throw new MigrationChainError('migration_injected_failure', 'injected v2->v3 failure');
    }
    repo.setSchemaVersion(SCHEMA_VERSION);
    // 迁移全程不 bump graph_revision（migration-spec §1.4 revision correct）
  });
}