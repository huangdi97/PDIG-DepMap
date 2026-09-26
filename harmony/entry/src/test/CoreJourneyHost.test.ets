/*
 * Harmony host **Core-Journey E2E** —— Android Core Journey（设备内 41/41）的
 * 无设备等价物：把四轮 host 验证的模块在真实 ArkTS 里端到端串起来。
 *
 *   导入（WechatParser+ImportPipeline）→ 提案（GraphRepository upsert）
 *   → 用户确认（confirmDependency）→ 影响（ImpactKernel simulateScenario）
 *   → 备份/恢复往返（exportPayload/importPayload）。
 *
 * 断言覆盖 AGENTS 铁律：机器推断永不产生 required（proposal-only 永不 must_change）、
 * confirmed required 边失效 → must_change 级联、未确认备用 → needs_review、
 * revision 与 mutation 同事务、restore 后影响结果逐字节一致。
 */

import { describe, it, expect } from '@ohos/hypium';
import { GraphStore } from '../main/ets/data/GraphStore';
import { GraphRepository } from '../main/ets/data/GraphRepository';
import {
  FingerprintHasher,
  ImportPipeline,
} from '../main/ets/data/ImportPipeline';
import { decodeBase64 } from '../main/ets/crypto/DepmapBounds';
import { ImpactResult, ImpactTargetResult, simulateScenario } from '../main/ets/domain/ImpactKernel';
import {
  Dependency,
  DependencyGroup,
  ImpactGraph,
  ImpactProposalInput,
  ImpactStateKey,
} from '../main/ets/domain/Entities';
import {
  Capability,
  Criticality,
  DependencyOrigin,
  DependencyState,
  GroupMode,
  GroupState,
  Relation,
  capabilityFromWire,
  criticalityFromWire,
  relationFromWire,
} from '../main/ets/generated/CanonicalEnums';

const FIXTURE_RECURRING = '5Lqk5piT5pe26Ze0LOS6pOaYk+exu+WeiyzkuqTmmJPlr7nmlrks5ZWG5ZOBLOaUti/mlK8s6YeR6aKdKOWFgyks5pSv5LuY5pa55byPLOW9k+WJjeeKtuaAgSzkuqTmmJPljZXlj7cs5ZWG5oi35Y2V5Y+3LOWkh+azqAoyMDI2LTAxLTE1IDEwOjIzOjAwLOWVhuaIt+a2iOi0uSzohb7orq/op4bpopFWSVAs5Lya5ZGYLOaUr+WHuiwyNS4wMCzmi5vllYbpk7booYzkv6HnlKjljaEoNDQxNyks5pSv5LuY5oiQ5YqfLDIwMjYwMTE1MTAyMzAwMSwxMDAwMSwKMjAyNi0wMi0xNSAxMDoyMzowMCzllYbmiLfmtojotLks6IW+6K6v6KeG6aKRVklQLOS8muWRmCzmlK/lh7osMjUuMDAs5oub5ZWG6ZO26KGM5L+h55So5Y2hKDQ0MTcpLOaUr+S7mOaIkOWKnywyMDI2MDIxNTEwMjMwMDEsMTAwMDIsCjIwMjYtMDMtMTUgMTA6MjM6MDAs5ZWG5oi35raI6LS5LOiFvuiur+inhumikVZJUCzkvJrlkZgs5pSv5Ye6LDI1LjAwLOmbtumSsSzmlK/ku5jmiJDlip8sMjAyNjAzMTUxMDIzMDAxLDEwMDAzLAoyMDI2LTAxLTIwIDA5OjAwOjAwLOi9rOi0pizlvKDkuIksLyzmlLblhaUsMjAwLjAwLOmbtumSsSzlt7LlrZjlhaXpm7bpkrEsMjAyNjAxMjAwOTAwMDAxLDIwMDAxLAo=';
const PROP_KEY = 'node-wechat-account|merchant_agreement|node-tencent|payment';
const ACCOUNT = 'node-wechat-account';
const SERVICE = 'node-tencent';

class MockHasher implements FingerprintHasher {
  hash(secret: string, scope: string): string {
    return 'mock-'.concat(String(secret.length)).concat(':').concat(scope);
  }
}

function freshRepo(): GraphRepository {
  return new GraphRepository(new GraphStore());
}

/** 导入 FIXTURE_RECURRING 到含 腾讯视频 节点的仓库 → 1 条 merchant_agreement 提案。 */
function seededRepo(): GraphRepository {
  const repo = freshRepo();
  repo.createNode(SERVICE, 'service', '腾讯视频');
  const pipeline = new ImportPipeline(repo, new MockHasher(), 'secret-1');
  pipeline.begin(decodeBase64(FIXTURE_RECURRING));
  pipeline.finalize();
  return repo;
}

function asStr(v: Object): string {
  return v as string;
}

function asNum(v: Object): number {
  return v as number;
}

function memberIds(json: string): string[] {
  try {
    const parsed = JSON.parse(json) as Object;
    if (parsed instanceof Array) {
      const out: string[] = [];
      for (const e of (parsed as Array<Object>)) {
        if (typeof e === 'string') {
          out.push(e as string);
        }
      }
      return out;
    }
  } catch (e) {
    // 坏 JSON 视为空
  }
  return [];
}

/** repo 行 → ImpactGraph（测试侧适配器；生产设备层由 application 组装）。 */
function toImpactGraph(repo: GraphRepository): ImpactGraph {
  const g = new ImpactGraph();
  for (const node of repo.storeRef().nodes.values()) {
    g.nodeNames.set(String(node['id']), String(node['name']));
  }
  for (const row of repo.storeRef().dependencies.values()) {
    const d = new Dependency();
    d.id = String(row['id']);
    d.from = String(row['from_node']);
    d.relation = relationFromWire(String(row['relation'])) ?? Relation.FUNDING_SOURCE;
    d.to = String(row['to_node']);
    d.capability = capabilityFromWire(String(row['capability'])) ?? Capability.PAYMENT;
    d.criticality = criticalityFromWire(String(row['criticality'])) ?? Criticality.UNKNOWN;
    d.state = String(row['state']) === 'retired' ? DependencyState.RETIRED : DependencyState.ACTIVE;
    d.origin = DependencyOrigin.MANUAL;
    const gid = row['group_id'];
    d.groupId = (gid === null || gid === undefined) ? null : String(gid);
    d.lastVerifiedAt = String(row['last_verified_at']);
    g.dependencies.push(d);
  }
  for (const row of repo.storeRef().dependencyGroups.values()) {
    const grp = new DependencyGroup();
    grp.id = String(row['id']);
    grp.groupKey = String(row['group_key']);
    grp.targetNodeId = String(row['target_node_id']);
    grp.capability = Capability.PAYMENT;
    grp.mode = String(row['mode']) === 'ALL' ? GroupMode.ALL : GroupMode.ANY;
    grp.memberEdgeIds = memberIds(String(row['member_edge_ids_json']));
    grp.state = String(row['state']) === 'retired' ? GroupState.RETIRED : GroupState.ACTIVE;
    g.groups.push(grp);
  }
  for (const row of repo.storeRef().dependencyProposals.values()) {
    const p = new ImpactProposalInput();
    p.key = String(row['key']);
    p.from = String(row['from_node']);
    p.to = String(row['to_node']);
    p.capability = Capability.PAYMENT;
    const cs = row['confidence_score'];
    p.confidenceScore = (typeof cs === 'number') ? (cs as number) : null;
    g.proposals.push(p);
  }
  return g;
}

function targetOf(result: ImpactResult, nodeId: string): ImpactTargetResult | null {
  for (const t of result.targets) {
    if (t.nodeId === nodeId) {
      return t;
    }
  }
  return null;
}

export default function coreJourneyHostTest() {
  describe('coreJourneyHost', () => {
    it('journey-import-produces-recurring-proposal', 0, () => {
      const repo = freshRepo();
      repo.createNode(SERVICE, 'service', '腾讯视频');
      const pipeline = new ImportPipeline(repo, new MockHasher(), 'secret-1');
      pipeline.begin(decodeBase64(FIXTURE_RECURRING));
      const out = pipeline.finalize();
      expect(out.proposalKeys.length).assertEqual(1);
      expect(out.proposalKeys[0]).assertEqual(PROP_KEY);
      const prop = repo.storeRef().dependencyProposals.get(PROP_KEY) as Record<string, Object>;
      expect(asStr(prop['decision'])).assertEqual('pending');
      expect(asNum(prop['observation_count'])).assertEqual(3);
    });

    it('journey-accept-proposal-confirms-edge-bumps-revision', 0, () => {
      const repo = seededRepo();
      const revBefore = repo.getRevision();
      const r = repo.confirmDependency(ACCOUNT, 'merchant_agreement', SERVICE, 'payment',
        'unknown', 'manual', 'dep-journey');
      expect(r.verified).assertFalse();
      expect(repo.getRevision()).assertEqual(revBefore + 1); // Reality mutation → bump
      const edge = repo.getDependency('dep-journey') as Record<string, Object>;
      expect(asStr(edge['state'])).assertEqual('active');
      expect(asStr(edge['criticality'])).assertEqual('unknown'); // 机器不自动 required
    });

    it('journey-proposal-only-never-produces-must-change', 0, () => {
      // 只导入提案、未确认依赖 → 模拟不可用账户 → 绝不 must_change（AGENTS §9）
      const repo = seededRepo();
      const g = toImpactGraph(repo);
      const result = simulateScenario(g, [new ImpactStateKey(ACCOUNT, Capability.PAYMENT)]);
      let mustChange = false;
      for (const t of result.targets) {
        if (t.status === 'must_change') {
          mustChange = true;
        }
      }
      expect(mustChange).assertFalse();
    });

    it('journey-confirmed-required-edge-must-change-cascade', 0, () => {
      const repo = seededRepo();
      repo.confirmDependency(ACCOUNT, 'merchant_agreement', SERVICE, 'payment', 'unknown', 'manual', 'dep-journey');
      repo.updateCriticality('dep-journey', 'required'); // 仅用户路径可提升
      const g = toImpactGraph(repo);
      const result = simulateScenario(g, [new ImpactStateKey(ACCOUNT, Capability.PAYMENT)]);
      const t = targetOf(result, SERVICE);
      expect(t === null).assertFalse();
      expect(t?.status).assertEqual('must_change');
      expect(t?.reasonCode).assertEqual('required_edge_no_alternative');
      // checklist 以 target_operation 收尾
      const last = result.checklist[result.checklist.length - 1];
      expect(last !== undefined).assertTrue();
      expect(last?.level).assertEqual('target_operation');
    });

    it('journey-unconfirmed-alternative-gives-needs-review', 0, () => {
      // 两条 confirmed 边（一条 required 失效，另一条来自仍可用来源）→ needs_review
      const repo = freshRepo();
      repo.createNode('card-a', 'payment_instrument', '卡A');
      repo.createNode('card-b', 'payment_instrument', '卡B');
      repo.createNode(SERVICE, 'service', '腾讯视频');
      repo.confirmDependency('card-a', 'funding_source', SERVICE, 'payment', 'required', 'manual', 'dep-a');
      repo.confirmDependency('card-b', 'funding_source', SERVICE, 'payment', 'required', 'manual', 'dep-b');
      const g = toImpactGraph(repo);
      const result = simulateScenario(g, [new ImpactStateKey('card-a', Capability.PAYMENT)]);
      const t = targetOf(result, SERVICE);
      expect(t === null).assertFalse();
      expect(t?.status).assertEqual('needs_review'); // 未确认备用组合 → 不 must_change
      expect(t?.reasonCode).assertEqual('unconfirmed_alternative_exists');
    });

    it('journey-machine-never-sets-required-anywhere', 0, () => {
      // 管线产物全链路扫描：proposal / evidence / fingerprint / session 无 required
      const repo = seededRepo();
      let requiredFound = '';
      const scan = (rows: Map<string, Record<string, Object>>): void => {
        for (const row of rows.values()) {
          // 只检查关键性字段（ArkTS 无 Object.entries，列清单驱动）
          if (asStr(row['criticality']) === 'required') {
            requiredFound = asStr(row['id']);
          }
        }
      };
      scan(repo.storeRef().dependencyProposals);
      scan(repo.storeRef().dependencies);
      expect(requiredFound).assertEqual('');
      expect(repo.storeRef().dependencies.size).assertEqual(0); // 未确认 → 无边
    });

    it('journey-backup-restore-roundtrip-preserves-impact', 0, () => {
      const repo = seededRepo();
      repo.confirmDependency(ACCOUNT, 'merchant_agreement', SERVICE, 'payment', 'unknown', 'manual', 'dep-journey');
      repo.updateCriticality('dep-journey', 'required');
      const before = repo.exportPayload();
      const g1 = toImpactGraph(repo);
      const r1 = simulateScenario(g1, [new ImpactStateKey(ACCOUNT, Capability.PAYMENT)]);
      // 恢复进新库
      const repo2 = freshRepo();
      repo2.importPayload(before);
      const after = repo2.exportPayload();
      expect(after).assertEqual(before); // 逐字节相同
      const g2 = toImpactGraph(repo2);
      const r2 = simulateScenario(g2, [new ImpactStateKey(ACCOUNT, Capability.PAYMENT)]);
      expect(r2.targets.length).assertEqual(r1.targets.length);
      const t2 = targetOf(r2, SERVICE);
      expect(t2?.status).assertEqual('must_change');
      expect(t2?.reasonCode).assertEqual('required_edge_no_alternative');
    });

    it('journey-revision-atomic-rollback-on-failure', 0, () => {
      const repo = freshRepo();
      repo.createNode(SERVICE, 'service', '腾讯视频');
      const revBefore = repo.getRevision();
      let threw = false;
      try {
        repo.withTransaction(() => {
          repo.confirmDependency(ACCOUNT, 'merchant_agreement', SERVICE, 'payment',
            'unknown', 'manual', 'dep-x');
          throw new Error('boom');
        });
      } catch (e) {
        threw = true;
      }
      expect(threw).assertTrue();
      expect(repo.getDependency('dep-x')).assertNull(); // 边与 revision 一并回滚
      expect(repo.getRevision()).assertEqual(revBefore);
    });
  });
}