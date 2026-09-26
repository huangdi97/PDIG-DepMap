// data 持久化执行层自检入口 —— 让 GraphStore / GraphRepository / MigrationChain
// **进入编译图**（与 DomainSelfCheck / PersistenceSelfCheck 同理）。
//
// 本模块不做“通过/失败”健全性声明，只把关键路径（迁移 → 建点 → 建边 →
// revision → 导出）的输出格式化成一行；真正的判定在主机单测
// `RepositoryHost.test.ets`（hypium，hvigor test 执行）。

import { GraphStore } from '../data/GraphStore';
import { GraphRepository } from '../data/GraphRepository';
import { applySchemaMigrations } from '../data/MigrationChain';

/** 自检行：迁移链 → node → dependency → revision → export 一次跑通。 */
export function repositorySelfCheckLine(): string {
  try {
    const store = new GraphStore();
    store.schemaVersion = 1;
    const repo = new GraphRepository(store);
    applySchemaMigrations(repo, 0);
    repo.createNode('n-probe', 'account', 'Probe');
    repo.confirmDependency('n-probe', 'funding_source', 'n-probe', 'payment', 'unknown', 'manual', 'dep-probe');
    const rev = repo.getRevision();
    const schema = repo.getSchemaVersion();
    const payload = repo.exportPayload();
    const ok = payload.indexOf('"payloadKind":"depmap-logical-graph"') >= 0;
    return 'repo: schema='.concat(String(schema))
      .concat(' nodes=').concat(String(store.nodes.size))
      .concat(' deps=').concat(String(store.dependencies.size))
      .concat(' rev=').concat(String(rev))
      .concat(' export=').concat(ok ? 'ok' : 'BROKEN');
  } catch (e) {
    const msg = (e instanceof Error) ? (e as Error).message : 'unknown';
    return 'repo: FAIL('.concat(msg).concat(')');
  }
}