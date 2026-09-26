// data 持久化执行层自检入口 —— 让 GraphStore / GraphRepository / MigrationChain
// **进入编译图**（与 DomainSelfCheck / PersistenceSelfCheck 同理）。
//
// 本模块不做“通过/失败”健全性声明，只把关键路径（迁移 → 建点 → 建边 →
// revision → 导出）的输出格式化成一行；真正的判定在主机单测
// `RepositoryHost.test.ets`（hypium，hvigor test 执行）。

import { GraphStore } from '../data/GraphStore';
import { GraphRepository } from '../data/GraphRepository';
import { applySchemaMigrations } from '../data/MigrationChain';
import { FingerprintHasher, ImportPipeline } from '../data/ImportPipeline';

/** 探针哈希（确定性；真实哈希为设备层 cryptoFramework）。 */
class ProbeHasher implements FingerprintHasher {
  hash(secret: string, scope: string): string {
    return 'probe:'.concat(scope);
  }
}

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
    // 管线探针：构造即验证 account 节点 + legacy 实例的幂等接线
    const probeRepo = new GraphRepository(new GraphStore());
    const pipeline = new ImportPipeline(probeRepo, new ProbeHasher(), 'probe-secret');
    const importOk = probeRepo.storeRef().nodes.size >= 2;
    return 'repo: schema='.concat(String(schema))
      .concat(' nodes=').concat(String(store.nodes.size))
      .concat(' deps=').concat(String(store.dependencies.size))
      .concat(' rev=').concat(String(rev))
      .concat(' export=').concat(ok ? 'ok' : 'BROKEN')
      .concat(' | import=ctor-').concat(importOk ? 'ok' : 'BROKEN');
  } catch (e) {
    const msg = (e instanceof Error) ? (e as Error).message : 'unknown';
    return 'repo: FAIL('.concat(msg).concat(')');
  }
}