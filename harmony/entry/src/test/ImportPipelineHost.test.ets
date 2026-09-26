/*
 * Import 管线（ImportPipeline / MerchantResolver / RecurrenceDetector）的**主机执行面**。
 *
 * 被测代码 = 真实 ArkTS 模块；期望值来自冻结基准（import-coordinator.ts /
 * resolver.ts / recurring.ts 的语义口径，GOAL §14/§15/§19）。fixture 以 base64
 * 内嵌（与 FixtureBundle 同款机制），经 decodeBase64 → WechatParser 真实解析。
 *
 * 诚实边界：
 *   - 指纹哈希经 `FingerprintHasher` 缝注入确定性 mock（设备层为 cryptoFramework）；
 *   - 路由默认 merchant_agreement(accountNodeId→service)，adapter 级 enrich 属设备层；
 *   - 未 resolution / 非 recurring 商户不产生 Proposal。
 */

import { describe, it, expect } from '@ohos/hypium';
import { GraphStore } from '../main/ets/data/GraphStore';
import { GraphRepository } from '../main/ets/data/GraphRepository';
import {
  FingerprintHasher,
  ImportPipeline,
  MerchantCandidate,
} from '../main/ets/data/ImportPipeline';
import {
  ResolvableEntity,
  normalizeMerchantName,
  resolveMerchant,
  similarity,
} from '../main/ets/data/MerchantResolver';
import { decodeBase64 } from '../main/ets/crypto/DepmapBounds';

/** 确定性 mock 哈希（secret 长度敏感；设备层为 cryptoFramework 实现）。 */
class MockHasher implements FingerprintHasher {
  hash(secret: string, scope: string): string {
    return 'mock-'.concat(String(secret.length)).concat(':').concat(scope);
  }
}

const FIXTURE_RECURRING = '5Lqk5piT5pe26Ze0LOS6pOaYk+exu+WeiyzkuqTmmJPlr7nmlrks5ZWG5ZOBLOaUti/mlK8s6YeR6aKdKOWFgyks5pSv5LuY5pa55byPLOW9k+WJjeeKtuaAgSzkuqTmmJPljZXlj7cs5ZWG5oi35Y2V5Y+3LOWkh+azqAoyMDI2LTAxLTE1IDEwOjIzOjAwLOWVhuaIt+a2iOi0uSzohb7orq/op4bpopFWSVAs5Lya5ZGYLOaUr+WHuiwyNS4wMCzmi5vllYbpk7booYzkv6HnlKjljaEoNDQxNyks5pSv5LuY5oiQ5YqfLDIwMjYwMTE1MTAyMzAwMSwxMDAwMSwKMjAyNi0wMi0xNSAxMDoyMzowMCzllYbmiLfmtojotLks6IW+6K6v6KeG6aKRVklQLOS8muWRmCzmlK/lh7osMjUuMDAs5oub5ZWG6ZO26KGM5L+h55So5Y2hKDQ0MTcpLOaUr+S7mOaIkOWKnywyMDI2MDIxNTEwMjMwMDEsMTAwMDIsCjIwMjYtMDMtMTUgMTA6MjM6MDAs5ZWG5oi35raI6LS5LOiFvuiur+inhumikVZJUCzkvJrlkZgs5pSv5Ye6LDI1LjAwLOmbtumSsSzmlK/ku5jmiJDlip8sMjAyNjAzMTUxMDIzMDAxLDEwMDAzLAoyMDI2LTAxLTIwIDA5OjAwOjAwLOi9rOi0pizlvKDkuIksLyzmlLblhaUsMjAwLjAwLOmbtumSsSzlt7LlrZjlhaXpm7bpkrEsMjAyNjAxMjAwOTAwMDAxLDIwMDAxLAo=';
const FIXTURE_UNRESOLVED = '5Lqk5piT5pe26Ze0LOS6pOaYk+exu+WeiyzkuqTmmJPlr7nmlrks5ZWG5ZOBLOaUti/mlK8s6YeR6aKdKOWFgyks5pSv5LuY5pa55byPLOW9k+WJjeeKtuaAgSzkuqTmmJPljZXlj7cs5ZWG5oi35Y2V5Y+3LOWkh+azqAoyMDI2LTAxLTA1IDEyOjAwOjAwLOWVhuaIt+a2iOi0uSzmnKrnn6XllYbmiLcsLyzmlK/lh7osMzAuMDAs6Zu26ZKxLOaUr+S7mOaIkOWKnywyMDI2MDEwNTEyMDAwMDEsMjAwMDIsCjIwMjYtMDItMDUgMTI6MDA6MDAs5ZWG5oi35raI6LS5LOacquefpeWVhuaItywvLOaUr+WHuiwzMC4wMCzpm7bpkrEs5pSv5LuY5oiQ5YqfLDIwMjYwMjA1MTIwMDAwMSwyMDAwMywK';
const FIXTURE_SINGLE = '5Lqk5piT5pe26Ze0LOS6pOaYk+exu+WeiyzkuqTmmJPlr7nmlrks5ZWG5ZOBLOaUti/mlK8s6YeR6aKdKOWFgyks5pSv5LuY5pa55byPLOW9k+WJjeeKtuaAgSzkuqTmmJPljZXlj7cs5ZWG5oi35Y2V5Y+3LOWkh+azqAoyMDI2LTA1LTAxIDA4OjAwOjAwLOWVhuaIt+a2iOi0uSxTSU5HTEUgTUVSQ0hBTlQsLyzmlK/lh7osMTIuMDAs6Zu26ZKxLOaUr+S7mOaIkOWKnywyMDI2MDUwMTA4MDAwMDEsMzAwMDEsCg==';

const PROP_KEY = 'node-wechat-account|merchant_agreement|node-tencent|payment';

function freshRepo(): GraphRepository {
  return new GraphRepository(new GraphStore());
}

function entitiesOf(repo: GraphRepository): ResolvableEntity[] {
  const out: ResolvableEntity[] = [];
  for (const node of repo.storeRef().nodes.values()) {
    const e = new ResolvableEntity();
    e.nodeId = String(node['id']);
    e.name = String(node['name']);
    out.push(e);
  }
  return out;
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

export default function importPipelineHostTest() {
  describe('importPipelineHost', () => {
    it('pipeline-begin-parses-and-auto-resolves-via-builtin-alias', 0, () => {
      const repo = freshRepo();
      repo.createNode('node-tencent', 'service', '腾讯视频');
      const pipeline = new ImportPipeline(repo, new MockHasher(), 'secret-1');
      const begin = pipeline.begin(decodeBase64(FIXTURE_RECURRING));
      expect(begin.rawCount).assertEqual(4);
      expect(begin.errors.length).assertEqual(0);
      // 腾讯视频VIP → BUILTIN_ALIASES alias_exact → node-tencent（auto）
      let tencent: Object | null = null;
      let zhangsan: Object | null = null;
      for (const c of begin.candidates) {
        if (c.merchantRaw === '腾讯视频VIP') {
          tencent = c;
        }
        if (c.merchantRaw === '张三') {
          zhangsan = c;
        }
      }
      expect(tencent === null).assertFalse();
      expect((tencent as MerchantCandidate).observationCount).assertEqual(3);
      expect(asStr((tencent as MerchantCandidate).resolvedNodeId)).assertEqual('node-tencent');
      expect(asStr((tencent as MerchantCandidate).resolution)).assertEqual('auto');
      expect(zhangsan === null).assertFalse();
      expect(asStr((zhangsan as MerchantCandidate).resolution)).assertEqual('pending');
    });

    it('pipeline-finalize-proposal-evidence-fingerprints-session', 0, () => {
      const repo = freshRepo();
      repo.createNode('node-tencent', 'service', '腾讯视频');
      const pipeline = new ImportPipeline(repo, new MockHasher(), 'secret-1');
      pipeline.begin(decodeBase64(FIXTURE_RECURRING));
      const out = pipeline.finalize();
      expect(out.newUniqueCount).assertEqual(4);
      expect(out.duplicateCount).assertEqual(0);
      expect(out.proposalKeys.length).assertEqual(1);
      expect(out.proposalKeys[0]).assertEqual(PROP_KEY);
      // 张三（收入行 + 未解析）→ unresolved
      expect(out.unresolvedMerchants.length).assertEqual(1);
      expect(out.unresolvedMerchants[0]).assertEqual('张三');
      const store = repo.storeRef();
      expect(store.dependencyProposals.has(PROP_KEY)).assertTrue();
      expect(store.evidence.size).assertEqual(1);
      expect(store.observationFingerprints.size).assertEqual(4);
      expect(store.importSessions.has('sess-import-1')).assertTrue();
      const sess = store.importSessions.get('sess-import-1') as Record<string, Object>;
      expect(asNum(sess['raw_count'])).assertEqual(4);
      expect(asNum(sess['proposal_count'])).assertEqual(1);
    });

    it('pipeline-revision-not-bumped-by-finalize', 0, () => {
      const repo = freshRepo();
      repo.createNode('node-tencent', 'service', '腾讯视频'); // rev 1
      const pipeline = new ImportPipeline(repo, new MockHasher(), 'secret-1'); // 微信账户节点 rev 2
      expect(repo.getRevision()).assertEqual(2);
      pipeline.begin(decodeBase64(FIXTURE_RECURRING));
      pipeline.finalize();
      // proposal/evidence/fingerprint/session 均不 bump revision
      expect(repo.getRevision()).assertEqual(2);
    });

    it('pipeline-recurring-unresolved-produces-no-proposal', 0, () => {
      const repo = freshRepo();
      const pipeline = new ImportPipeline(repo, new MockHasher(), 'secret-1');
      pipeline.begin(decodeBase64(FIXTURE_UNRESOLVED));
      const out = pipeline.finalize();
      expect(out.proposalKeys.length).assertEqual(0);
      expect(out.unresolvedMerchants.length).assertEqual(1);
      expect(out.unresolvedMerchants[0]).assertEqual('未知商户');
      expect(repo.storeRef().dependencyProposals.size).assertEqual(0);
      expect(repo.storeRef().observationFingerprints.size).assertEqual(2);
    });

    it('pipeline-manual-resolution-enables-recurring-proposal', 0, () => {
      const repo = freshRepo();
      repo.createNode('node-x', 'service', '未知商户');
      const pipeline = new ImportPipeline(repo, new MockHasher(), 'secret-1');
      pipeline.begin(decodeBase64(FIXTURE_UNRESOLVED));
      pipeline.resolveMerchant('未知商户', 'node-x');
      const out = pipeline.finalize();
      expect(out.proposalKeys.length).assertEqual(1);
      expect(out.proposalKeys[0]).assertEqual('node-wechat-account|merchant_agreement|node-x|payment');
      expect(out.unresolvedMerchants.length).assertEqual(0);
    });

    it('pipeline-reimport-idempotent-all-duplicates', 0, () => {
      const repo = freshRepo();
      repo.createNode('node-tencent', 'service', '腾讯视频');
      const p1 = new ImportPipeline(repo, new MockHasher(), 'secret-1');
      p1.begin(decodeBase64(FIXTURE_RECURRING));
      const first = p1.finalize();
      expect(first.proposalKeys.length).assertEqual(1);
      // 同一账单再导入：全部指纹重复 → 0 新提案（幂等）
      const p2 = new ImportPipeline(repo, new MockHasher(), 'secret-1');
      p2.begin(decodeBase64(FIXTURE_RECURRING));
      const second = p2.finalize();
      expect(second.newUniqueCount).assertEqual(0);
      expect(second.duplicateCount).assertEqual(4);
      expect(second.proposalKeys.length).assertEqual(0);
      expect(repo.storeRef().dependencyProposals.size).assertEqual(1); // 不重复问
      expect(repo.storeRef().observationFingerprints.size).assertEqual(4);
    });

    it('pipeline-fingerprint-deterministic-and-secret-sensitive', 0, () => {
      const repoA = freshRepo();
      const pa = new ImportPipeline(repoA, new MockHasher(), 'a');
      pa.begin(decodeBase64(FIXTURE_RECURRING));
      pa.finalize();
      const fpsA: string[] = [];
      for (const row of repoA.storeRef().observationFingerprints.values()) {
        fpsA.push(asStr(row['fingerprint']));
      }
      // 同 secret 重跑 → 同指纹集
      const repoB = freshRepo();
      const pb = new ImportPipeline(repoB, new MockHasher(), 'a');
      pb.begin(decodeBase64(FIXTURE_RECURRING));
      pb.finalize();
      const fpsB: string[] = [];
      for (const row of repoB.storeRef().observationFingerprints.values()) {
        fpsB.push(asStr(row['fingerprint']));
      }
      expect(fpsA.length).assertEqual(fpsB.length);
      let sameCount = 0;
      for (const f of fpsA) {
        if (fpsB.indexOf(f) >= 0) {
          sameCount++;
        }
      }
      expect(sameCount).assertEqual(fpsA.length);
      // 不同 secret（长度不同）→ 指纹集不同
      const repoC = freshRepo();
      const pc = new ImportPipeline(repoC, new MockHasher(), 'abcdefgh');
      pc.begin(decodeBase64(FIXTURE_RECURRING));
      let fpC = '';
      for (const row of repoC.storeRef().observationFingerprints.values()) {
        fpC = asStr(row['fingerprint']);
        break;
      }
      expect(fpC === fpsA[0]).assertFalse();
    });

    it('pipeline-single-occurrence-no-recurrence-no-proposal', 0, () => {
      const repo = freshRepo();
      const pipeline = new ImportPipeline(repo, new MockHasher(), 'secret-1');
      const begin = pipeline.begin(decodeBase64(FIXTURE_SINGLE));
      expect(begin.rawCount).assertEqual(1);
      const out = pipeline.finalize();
      expect(out.proposalKeys.length).assertEqual(0); // 单次 → recurrence null
      expect(out.newUniqueCount).assertEqual(1);
      expect(repo.storeRef().observationFingerprints.size).assertEqual(1);
    });

    it('pipeline-state-guards', 0, () => {
      const repo = freshRepo();
      const pipeline = new ImportPipeline(repo, new MockHasher(), 'secret-1');
      expectThrows(() => pipeline.finalize(), 'begin() must be called first');
      pipeline.begin(decodeBase64(FIXTURE_RECURRING));
      expectThrows(() => pipeline.begin(decodeBase64(FIXTURE_RECURRING)), 'pipeline already begun');
      expectThrows(() => pipeline.resolveMerchant('不存在', 'node-wechat-account'), 'merchant not in this import');
      expectThrows(() => pipeline.resolveMerchant('腾讯视频VIP', 'node-missing'), 'node not found');
    });

    it('resolver-normalize-and-similarity', 0, () => {
      expect(normalizeMerchantName(' 腾讯视频 VIP ')).assertEqual('腾讯视频vip');
      expect(normalizeMerchantName('腾讯·视频（VIP）')).assertEqual('腾讯视频(vip)');
      expect(normalizeMerchantName('APPLE.COM/BILL')).assertEqual('apple.com/bill');
      expect(similarity('abc', 'abc')).assertEqual(1);
      expect(similarity('', 'x')).assertEqual(0);
      const s = similarity('kitten', 'sitting');
      expect(Math.abs(s - 0.5714285714285714) < 0.001).assertTrue();
    });

    it('resolver-alias-exact-and-normalized-exact', 0, () => {
      const e1 = new ResolvableEntity();
      e1.nodeId = 'node-a';
      e1.name = '腾讯视频';
      const e2 = new ResolvableEntity();
      e2.nodeId = 'node-b';
      e2.name = '中国移动';
      const entities: ResolvableEntity[] = [e1, e2];
      const r1 = resolveMerchant('腾讯视频VIP', entities);
      expect(r1.status).assertEqual('resolved');
      expect(r1.nodeId).assertEqual('node-a');
      expect(r1.matchType).assertEqual('alias_exact');
      const r2 = resolveMerchant('腾讯视频 VIP', entities); // 规范化精确
      expect(r2.status).assertEqual('resolved');
      expect(r2.nodeId).assertEqual('node-a');
      const r3 = resolveMerchant('腾讯科技(深圳)有限公司', entities);
      expect(r3.status).assertEqual('resolved');
      expect(r3.nodeId).assertEqual('node-a');
      const r4 = resolveMerchant('NO SUCH SHOP', entities);
      expect(r4.status).assertEqual('unresolved');
    });

    it('resolver-fuzzy-and-ambiguous', 0, () => {
      const e1 = new ResolvableEntity();
      e1.nodeId = 'node-s';
      e1.name = 'SINGLE MERCHANT';
      const fuzzy = resolveMerchant('SINGLE MERCHANTX', [e1]);
      expect(fuzzy.status).assertEqual('resolved');
      expect(fuzzy.nodeId).assertEqual('node-s');
      expect(fuzzy.matchType).assertEqual('fuzzy');
      // 两个同名候选 → ambiguous
      const e2 = new ResolvableEntity();
      e2.nodeId = 'node-s2';
      e2.name = 'SINGLE MERCHANT';
      const amb = resolveMerchant('SINGLE MERCHANT', [e1, e2]);
      expect(amb.status).assertEqual('ambiguous');
    });
  });
}