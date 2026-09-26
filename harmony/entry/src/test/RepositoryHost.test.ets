/*
 * 持久化执行层（GraphStore / GraphRepository / MigrationChain）的**主机执行面**。
 *
 * 被测代码 = 真实 ArkTS 模块（data/ 层），由 hvigor test 在主机执行面上真跑；
 * 期望值来自冻结契约与 TS 参考实现（core/src/repositories/* 的语义口径）：
 *   - dependency-repository.ts：confirm INSERT / verify / re-activate 同 id +
 *     revision 同事务 bump + retire 幂等
 *   - graph-revision.ts：meta.graph_revision 单调递增；仅 Reality mutation bump
 *   - persistence-contract.md §1：evidence/fingerprint 唯一键
 *   - migration-spec.md §1.4/§2.7：迁移链幂等、失败回滚、payload 原子导入
 *
 * 仍不是设备运行时结论：ArkData 物理 DB、crypto 哈希在设备层；本文件只验证
 * 平台中立的逻辑持久化语义。
 */

import { describe, it, expect } from '@ohos/hypium';
import { GraphStore } from '../main/ets/data/GraphStore';
import { ConfirmResult, GraphRepository } from '../main/ets/data/GraphRepository';
import { MigrationChainError, applySchemaMigrations } from '../main/ets/data/MigrationChain';

function freshRepo(): GraphRepository {
  return new GraphRepository(new GraphStore());
}

/** 以 schemaVersion=1 起步的仓库（迁移链用例）。 */
function v1Repo(): GraphRepository {
  const store = new GraphStore();
  store.schemaVersion = 1;
  const repo = new GraphRepository(store);
  return repo;
}

function asNum(v: Object): number {
  return v as number;
}

function asStr(v: Object): string {
  return v as string;
}

function expectThrows(fn: () => void, fragment: string): void {
  let threw = false;
  let msg = '';
  try {
    fn();
  } catch (e) {
    threw = true;
    if (e instanceof Error) {
      msg = (e as Error).message;
    }
  }
  expect(threw).assertTrue();
  expect(msg.indexOf(fragment) >= 0).assertTrue();
}

export default function repositoryHostTest() {
  describe('repositoryHost', () => {
    it('repository-node-create-bumps-revision', 0, () => {
      const repo = freshRepo();
      expect(repo.getRevision()).assertEqual(0);
      repo.createNode('n1', 'account', 'A');
      expect(repo.getRevision()).assertEqual(1);
      const store = repo.storeRef();
      expect(store.nodes.size).assertEqual(1);
      const row = store.nodes.get('n1') as Record<string, Object>;
      expect(asStr(row['kind'])).assertEqual('account');
      expect(asNum(row['archived'])).assertEqual(0);
    });

    it('repository-dependency-confirm-insert-verify-no-duplicate', 0, () => {
      const repo = freshRepo();
      repo.createNode('n1', 'account', 'A');
      const r1 = repo.confirmDependency('n1', 'funding_source', 'n2', 'payment', 'unknown', 'manual', 'dep-1');
      expect(r1.id).assertEqual('dep-1');
      expect(r1.reactivated).assertFalse();
      expect(r1.verified).assertFalse();
      const revAfterInsert = repo.getRevision();
      expect(revAfterInsert).assertEqual(2);
      // 同 logical key 再确认 → verify，不产生第二条边、不 bump
      const r2 = repo.confirmDependency('n1', 'funding_source', 'n2', 'payment', 'unknown', 'manual', '');
      expect(r2.verified).assertTrue();
      expect(r2.id).assertEqual('dep-1');
      expect(repo.activeDependencyIds().length).assertEqual(1);
      expect(repo.getRevision()).assertEqual(revAfterInsert);
    });

    it('repository-dependency-retire-reactivate-same-id', 0, () => {
      const repo = freshRepo();
      repo.createNode('n1', 'account', 'A');
      repo.confirmDependency('n1', 'funding_source', 'n2', 'payment', 'unknown', 'manual', 'dep-9');
      const revBeforeRetire = repo.getRevision();
      repo.retireDependency('dep-9');
      expect(repo.getRevision()).assertEqual(revBeforeRetire + 1);
      expect(repo.activeDependencyIds().length).assertEqual(0);
      const r = repo.confirmDependency('n1', 'funding_source', 'n2', 'payment', 'unknown', 'manual', '');
      expect(r.reactivated).assertTrue();
      expect(r.id).assertEqual('dep-9'); // 同 id 复活，不新建
      expect(repo.activeDependencyIds().length).assertEqual(1);
      expect(repo.activeDependencyIds()[0]).assertEqual('dep-9');
    });

    it('repository-dependency-retire-idempotent-no-double-bump', 0, () => {
      const repo = freshRepo();
      repo.createNode('n1', 'account', 'A');
      repo.confirmDependency('n1', 'funding_source', 'n2', 'payment', 'unknown', 'manual', 'dep-2');
      const before = repo.getRevision();
      repo.retireDependency('dep-2');
      const afterFirst = repo.getRevision();
      expect(afterFirst).assertEqual(before + 1);
      repo.retireDependency('dep-2'); // 已 retired → 幂等，不重复 bump
      expect(repo.getRevision()).assertEqual(afterFirst);
      expectThrows(() => repo.retireDependency('dep-missing'), 'dependency not found');
    });

    it('repository-criticality-default-unknown-and-no-auto-required', 0, () => {
      const repo = freshRepo();
      repo.createNode('n1', 'account', 'A');
      repo.confirmDependency('n1', 'funding_source', 'n2', 'payment', '', 'manual', 'dep-3');
      const row = repo.getDependency('dep-3') as Record<string, Object>;
      expect(asStr(row['criticality'])).assertEqual('unknown'); // 机器不得自动 required
      repo.updateCriticality('dep-3', 'required');
      expect(asStr((repo.getDependency('dep-3') as Record<string, Object>)['criticality']))
        .assertEqual('required');
      const rev = repo.getRevision();
      repo.updateCriticality('dep-3', 'required'); // 无变化 → 不 bump
      expect(repo.getRevision()).assertEqual(rev);
    });

    it('repository-node-archive-bumps-revision', 0, () => {
      const repo = freshRepo();
      repo.createNode('n1', 'account', 'A');
      const before = repo.getRevision();
      repo.archiveNode('n1');
      expect(repo.getRevision()).assertEqual(before + 1);
      const row = repo.storeRef().nodes.get('n1') as Record<string, Object>;
      expect(asNum(row['archived'])).assertEqual(1);
    });

    it('repository-group-confirm-retire-backfill-members', 0, () => {
      const repo = freshRepo();
      repo.createNode('n1', 'account', 'A');
      repo.confirmDependency('n1', 'funding_source', 'n2', 'payment', 'unknown', 'manual', 'dep-a');
      repo.confirmDependency('n1', 'funding_source', 'n3', 'payment', 'unknown', 'manual', 'dep-b');
      const before = repo.getRevision();
      const gid = repo.confirmGroup('wechat|payment|ANY|dep-a|dep-b', 'n2', 'ANY', ['dep-a', 'dep-b'], 'grp-1');
      expect(gid).assertEqual('grp-1');
      expect(repo.getRevision()).assertEqual(before + 1);
      // 成员边回填 group_id
      const edgeA = repo.getDependency('dep-a') as Record<string, Object>;
      expect(asStr(edgeA['group_id'])).assertEqual('grp-1');
      const edgeB = repo.getDependency('dep-b') as Record<string, Object>;
      expect(asStr(edgeB['group_id'])).assertEqual('grp-1');
      // 重复 confirm 同 group_key → 幂等，不 bump
      const gid2 = repo.confirmGroup('wechat|payment|ANY|dep-a|dep-b', 'n2', 'ANY', ['dep-a', 'dep-b'], '');
      expect(gid2).assertEqual('grp-1');
      expect(repo.getRevision()).assertEqual(before + 1);
      repo.retireGroup('grp-1');
      expect(repo.getRevision()).assertEqual(before + 2);
    });

    it('repository-proposal-upsert-accumulates-without-bump', 0, () => {
      const repo = freshRepo();
      repo.createNode('n1', 'account', 'A');
      const rev = repo.getRevision();
      repo.upsertProposal('n1|funding_source|n2|payment', 'prop-1', 'n1', 'funding_source', 'n2',
        'payment', 'pending', 2);
      repo.upsertProposal('n1|funding_source|n2|payment', 'prop-1', 'n1', 'funding_source', 'n2',
        'payment', 'pending', 3);
      expect(repo.getRevision()).assertEqual(rev); // proposal upsert 不 bump
      const store = repo.storeRef();
      expect(store.dependencyProposals.size).assertEqual(1); // 同 key 一条
      const row = store.dependencyProposals.get('n1|funding_source|n2|payment') as Record<string, Object>;
      expect(asNum(row['observation_count'])).assertEqual(5);
    });

    it('repository-proposal-accepted-not-reasked-rejected-needs-evidence', 0, () => {
      const repo = freshRepo();
      repo.createNode('n1', 'account', 'A');
      repo.upsertProposal('k1', 'prop-1', 'n1', 'funding_source', 'n2', 'payment', 'pending', 1);
      repo.setProposalDecision('k1', 'accepted');
      repo.upsertProposal('k1', 'prop-1', 'n1', 'funding_source', 'n2', 'payment', 'pending', 1);
      const row = repo.storeRef().dependencyProposals.get('k1') as Record<string, Object>;
      expect(asStr(row['decision'])).assertEqual('accepted'); // 不重复问
      // rejected → 需要新 evidence 才可重提（SRC-04 简化：新计数 ≥3）
      repo.setProposalDecision('k1', 'rejected');
      expect(repo.canRePropose('k1', new Map<string, number>())).assertFalse();
      const counts = new Map<string, number>();
      counts.set('si-a', 3);
      expect(repo.canRePropose('k1', counts)).assertTrue();
      expect(repo.canRePropose('missing-key', counts)).assertFalse();
    });

    it('repository-evidence-unique-per-source-accumulates', 0, () => {
      const repo = freshRepo();
      repo.createNode('n1', 'account', 'A');
      repo.recordEvidence('k1', 'si-a', 'ev-1', 2);
      repo.recordEvidence('k1', 'si-a', 'ev-1', 3);
      repo.recordEvidence('k1', 'si-b', 'ev-2', 1);
      const store = repo.storeRef();
      expect(store.evidence.size).assertEqual(2); // (proposal_key, source_instance_id) 唯一
      let total = 0;
      for (const row of store.evidence.values()) {
        total += asNum(row['observation_count']);
      }
      expect(total).assertEqual(6);
    });

    it('repository-fingerprint-unique-dims', 0, () => {
      const repo = freshRepo();
      repo.createNode('n1', 'account', 'A');
      repo.recordFingerprint('si-a', 1, 'fp-1');
      repo.recordFingerprint('si-a', 1, 'fp-1'); // 幂等
      repo.recordFingerprint('si-b', 1, 'fp-1'); // 不同源 → 另一条
      repo.recordFingerprint('si-a', 2, 'fp-1'); // 不同 version → 另一条
      expect(repo.storeRef().observationFingerprints.size).assertEqual(3);
    });

    it('repository-export-import-roundtrip-byte-identical', 0, () => {
      const repo = freshRepo();
      repo.createNode('n1', 'account', 'A');
      repo.createNode('n2', 'service', 'S');
      repo.confirmDependency('n1', 'funding_source', 'n2', 'payment', 'unknown', 'manual', 'dep-1');
      repo.upsertProposal('n1|funding_source|n2|payment', 'prop-1', 'n1', 'funding_source', 'n2',
        'payment', 'pending', 4);
      repo.recordEvidence('n1|funding_source|n2|payment', 'legacy-wechat-statement', 'ev-1', 4);
      repo.ensureLegacySourceInstance();
      const e1 = repo.exportPayload();
      const repo2 = freshRepo();
      repo2.importPayload(e1);
      const e2 = repo2.exportPayload();
      expect(e2).assertEqual(e1); // 逐字节相同
      expect(repo2.getRevision()).assertEqual(repo.getRevision()); // revision 往返保持
    });

    it('repository-import-invalid-payload-atomic-rollback', 0, () => {
      const repo = freshRepo();
      repo.createNode('n1', 'account', 'A');
      const before = repo.exportPayload();
      // 缺表 → validate 失败 → 存储逐字节保持原样
      expectThrows(
        () => repo.importPayload('{"payloadKind":"depmap-logical-graph","payloadVersion":3,"schemaVersion":3}'),
        'must be an array');
      expect(repo.exportPayload()).assertEqual(before);
      expect(repo.storeRef().nodes.size).assertEqual(1);
    });

    it('repository-transaction-rollback-on-injected-failure', 0, () => {
      const repo = freshRepo();
      const before = repo.exportPayload();
      let threw = false;
      try {
        repo.withTransaction(() => {
          repo.createNode('x', 'account', 'X');
          throw new Error('boom');
        });
      } catch (e) {
        threw = true;
      }
      expect(threw).assertTrue();
      expect(repo.exportPayload()).assertEqual(before); // 整体回滚
      expect(repo.storeRef().nodes.size).assertEqual(0);
      expect(repo.getRevision()).assertEqual(0);
    });

    it('repository-migration-chain-1-to-3-idempotent-legacy-once', 0, () => {
      const repo = v1Repo();
      for (let i = 0; i < 50; i++) {
        applySchemaMigrations(repo, 0);
      }
      expect(repo.getSchemaVersion()).assertEqual(3);
      expect(repo.storeRef().sourceInstances.size).assertEqual(1); // legacy 只建一次
      expect(repo.getRevision()).assertEqual(0); // 迁移不 bump revision
      const legacy = repo.storeRef().sourceInstances.get('legacy-wechat-statement') as Record<string, Object>;
      expect(asStr(legacy['adapter_id'])).assertEqual('wechat_statement');
    });

    it('repository-migration-failure-rollback-stays-at-old-version', 0, () => {
      const repo = v1Repo();
      let threw = false;
      let code = '';
      try {
        applySchemaMigrations(repo, 1);
      } catch (e) {
        threw = true;
        if (e instanceof MigrationChainError) {
          code = e.code;
        }
      }
      expect(threw).assertTrue();
      expect(code).assertEqual('migration_injected_failure');
      expect(repo.getSchemaVersion()).assertEqual(1); // 停在旧版本，无半迁移状态
      expect(repo.storeRef().sourceInstances.size).assertEqual(0); // 无副作用残留
    });

    it('repository-migration-step2-failure-rolls-back-step1', 0, () => {
      const repo = v1Repo();
      let threw = false;
      try {
        applySchemaMigrations(repo, 2);
      } catch (e) {
        threw = true;
      }
      expect(threw).assertTrue();
      // v1→v2 的推进与 legacy 注入必须一并回滚（同事务）
      expect(repo.getSchemaVersion()).assertEqual(1);
      expect(repo.storeRef().sourceInstances.size).assertEqual(0);
    });
  });
}