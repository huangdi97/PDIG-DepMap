/*
 * 持久化逻辑层（data/ 纯 ArkTS）的**主机执行面**。
 *
 * 被测代码 = 真实 ArkTS 模块（SchemaV3 / PayloadCodec / PayloadMigration /
 * SourceInstance），由 hvigor test 在主机执行面上真跑；期望值来自冻结契约：
 *   - fixtures/migration/migration-version-contract.json（schema 契约常量）
 *   - spec/migration/migration-spec.md §2（迁移 / 拒绝规则）
 *   - core/tests/integration/graph-payload-v2.test.ts（行为口径，TS 冻结基准）
 *
 * 判定方式：每个 it() 直接断言模块输出；schema 契约用例**独立断言**冻结值
 * （不与 SchemaV3 同源派生，避免"自己和比自己"）。
 *
 * ArkTS 约束：所有 JSON 输入一律用**字符串字面量/模板字符串**构造
 * （不写未声明类型的对象字面量）；null 篡改用字符串替换（不写字面量 null）。
 *
 * 仍不是设备运行时结论：DB 原子导入 / ArkData / crypto 哈希在设备层
 * （data/Repository.ets 后续），本文件只验证平台中立逻辑。
 */

import { describe, it, expect } from '@ohos/hypium';
import {
  GRAPH_PAYLOAD_VERSION,
  LEGACY_WECHAT_SOURCE_INSTANCE_ID,
  MIGRATION_CHAIN,
  NEVER_BUMPS_ON,
  PAYLOAD_TABLES,
  REJECTED_PAYLOAD_VERSIONS,
  REJECTED_SCHEMA_VERSIONS,
  SCHEMA_VERSION,
  getColumns,
} from '../main/ets/data/SchemaV3';
import {
  buildPayloadJson,
  checkGraphIntegrity,
  parsePayloadObject,
  validatePayload,
} from '../main/ets/data/PayloadCodec';
import {
  legacyWechatInstance,
  migrateIfNeeded,
  migratePayloadV1toV2,
  migratePayloadV2toV3,
} from '../main/ets/data/PayloadMigration';
import { FingerprintScope, SOURCE_KINDS, COVERAGE_MODES } from '../main/ets/domain/SourceInstance';

/** 最小 v1 payload（10 张 v1 表全空）。 */
function minimalV1(): string {
  return '{"payloadKind":"depmap-logical-graph","payloadVersion":1,' +
    '"meta":[],"nodes":[],"dependencies":[],"dependency_groups":[],' +
    '"dependency_proposals":[],"dependency_group_proposals":[],"evidence":[],' +
    '"observation_fingerprints":[],"import_sessions":[]}';
}

/** 带内联 evidence_id / fingerprint / session 的 v1 payload（含 1 节点 + 1 边）。 */
function v1WithEvidence(): string {
  return `{
  "payloadKind": "depmap-logical-graph",
  "payloadVersion": 1,
  "meta": [{"key": "schema_version", "value": "1"}],
  "nodes": [{
    "id": "node-1", "kind": "payment_instrument", "template_id": null, "name": "招商 4417",
    "issuer": null, "last4": null, "owner": "self", "archived": 0, "fields_json": "{}",
    "vault_ref": null, "wallet_ref": null, "created_at": "2026-01-01T00:00:00.000Z",
    "updated_at": "2026-01-01T00:00:00.000Z"
  }],
  "dependencies": [{
    "id": "dep-1", "from_node": "node-1", "relation": "funding_source", "to_node": "node-2",
    "capability": "payment", "criticality": "required", "group_id": null, "state": "active",
    "origin": "manual", "confirmed_at": "2026-01-02T00:00:00.000Z",
    "last_verified_at": "2026-01-02T00:00:00.000Z", "retired_at": null,
    "evidence_refs_json": "[]", "created_at": "2026-01-02T00:00:00.000Z",
    "updated_at": "2026-01-02T00:00:00.000Z"
  }],
  "dependency_groups": [],
  "dependency_proposals": [{
    "id": "prop-1", "key": "node-1|funding_source|node-2|payment", "from_node": "node-1",
    "relation": "funding_source", "to_node": "node-2", "capability": "payment",
    "proposal_type": "transaction_stream", "source": "wechat_statement", "parser_id": "wechat",
    "parser_version": 1, "confidence_score": 0.9, "path_json": "[]", "decision": "pending",
    "decided_at": null, "criticality_decision": null, "observation_count": 2,
    "rejected_at": null, "evidence_id": "ev-1", "created_at": "2026-01-02T00:00:00.000Z",
    "updated_at": "2026-01-02T00:00:00.000Z"
  }],
  "dependency_group_proposals": [],
  "evidence": [{
    "id": "ev-1", "proposal_key": "node-1|funding_source|node-2|payment", "parser_version": 1,
    "source_type": "statement_file", "parser_id": "wechat", "last_import_session_id": "sess-1",
    "first_observed_at": "2026-01-01T00:00:00.000Z", "last_observed_at": "2026-01-01T00:00:00.000Z",
    "observation_count": 2, "created_at": "2026-01-02T00:00:00.000Z",
    "updated_at": "2026-01-02T00:00:00.000Z"
  }],
  "observation_fingerprints": [{
    "fingerprint": "fp-abc", "source": "txn:1", "fingerprint_version": 1,
    "import_session_id": "sess-1", "first_seen_at": "2026-01-01T00:00:00.000Z"
  }],
  "import_sessions": [{
    "id": "sess-1", "source_type": "statement_file", "parser_id": "wechat", "parser_version": 1,
    "started_at": "2026-01-01T00:00:00.000Z", "completed_at": "2026-01-01T00:00:00.001Z",
    "raw_count": 3, "new_unique_count": 2, "duplicate_count": 1, "proposal_count": 1, "error_count": 0
  }]
}`;
}

/** v2 形状 payload（meta 可注入，用于 graph_revision 用例）。 */
function v2Payload(metaJson: string): string {
  return '{"payloadKind":"depmap-logical-graph","payloadVersion":2,"schemaVersion":2,' +
    '"meta":' + metaJson + ',' +
    '"nodes":[],"dependencies":[],"dependency_groups":[],"dependency_proposals":[],' +
    '"dependency_group_proposals":[],"proposal_evidence_refs":[],"evidence":[],' +
    '"observation_fingerprints":[],"import_sessions":[],"source_instances":[]}';
}

function asNum(v: Object): number {
  return v as number;
}

function asStr(v: Object): string {
  return v as string;
}

function expectThrowsPayloadError(fn: () => void, messageFragment: string): void {
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
  expect(msg.indexOf(messageFragment) >= 0).assertTrue();
}

export default function persistenceHostTest() {
  describe('persistenceHost', () => {
    it('persistence-schema-contract-matches-frozen-fixture', 0, () => {
      // 冻结值独立断言（fixtures/migration/migration-version-contract.json），
      // 不与 SchemaV3 同源派生 —— 平台侧必须逐字同意这份契约。
      expect(SCHEMA_VERSION).assertEqual(3);
      expect(MIGRATION_CHAIN.join(',')).assertEqual('1,2,3');
      expect(GRAPH_PAYLOAD_VERSION).assertEqual(3);
      expect(REJECTED_SCHEMA_VERSIONS.join(',')).assertEqual('0,4,99,100');
      expect(REJECTED_PAYLOAD_VERSIONS.join(',')).assertEqual('0,4,99');
      expect(PAYLOAD_TABLES.join(',')).assertEqual(
        'meta,nodes,dependencies,dependency_groups,dependency_proposals,' +
        'dependency_group_proposals,proposal_evidence_refs,evidence,' +
        'observation_fingerprints,import_sessions,source_instances');
      expect(NEVER_BUMPS_ON.length).assertEqual(15);
      expect(NEVER_BUMPS_ON[0]).assertEqual('evidence_record');
      expect(NEVER_BUMPS_ON[NEVER_BUMPS_ON.length - 1]).assertEqual('action_verify');
      for (let i = 0; i < PAYLOAD_TABLES.length; i++) {
        expect(getColumns(PAYLOAD_TABLES[i]).length > 0).assertTrue();
      }
      expect(getColumns('not-a-table').length).assertEqual(0);
    });

    it('persistence-v1-minimal-migrates-to-v3-headers-and-legacy-instance', 0, () => {
      const out = migrateIfNeeded(minimalV1());
      const rec = parsePayloadObject(out);
      expect(asNum(rec['payloadVersion'])).assertEqual(3);
      expect(asNum(rec['schemaVersion'])).assertEqual(3);
      const srcs = rec['source_instances'] as Array<Object>;
      expect(srcs.length).assertEqual(1);
      expect(asStr((srcs[0] as Record<string, Object>)['id']))
        .assertEqual(LEGACY_WECHAT_SOURCE_INSTANCE_ID);
      const meta = rec['meta'] as Array<Object>;
      let hasRevision = false;
      for (let i = 0; i < meta.length; i++) {
        const m = meta[i] as Record<string, Object>;
        if (asStr(m['key']) === 'graph_revision') {
          hasRevision = true;
          expect(asStr(m['value'])).assertEqual('0');
        }
      }
      expect(hasRevision).assertTrue();
    });

    it('persistence-v1-evidence-moves-to-refs-with-provenance', 0, () => {
      const out = migrateIfNeeded(v1WithEvidence());
      const rec = parsePayloadObject(out);
      const refs = rec['proposal_evidence_refs'] as Array<Object>;
      expect(refs.length).assertEqual(1);
      const ref0 = refs[0] as Record<string, Object>;
      expect(asStr(ref0['proposal_key'])).assertEqual('node-1|funding_source|node-2|payment');
      expect(asStr(ref0['evidence_id'])).assertEqual('ev-1');
      expect(asNum(ref0['position'])).assertEqual(0);
      const prop = (rec['dependency_proposals'] as Array<Object>)[0] as Record<string, Object>;
      expect(prop['evidence_id'] === undefined).assertTrue();
      expect(prop['rejected_at_stream_counts_json'] === null).assertTrue();
      const ev = (rec['evidence'] as Array<Object>)[0] as Record<string, Object>;
      expect(asStr(ev['source_instance_id'])).assertEqual('legacy-wechat-statement');
      expect(asStr(ev['evidence_kind'])).assertEqual('transaction_stream');
      expect(asStr(ev['adapter_id'])).assertEqual('wechat_statement');
      expect(asNum(ev['adapter_version'])).assertEqual(1);
      const fp = (rec['observation_fingerprints'] as Array<Object>)[0] as Record<string, Object>;
      expect(asStr(fp['source_instance_id'])).assertEqual('legacy-wechat-statement');
      const sess = (rec['import_sessions'] as Array<Object>)[0] as Record<string, Object>;
      expect(asStr(sess['source_instance_id'])).assertEqual('legacy-wechat-statement');
    });

    it('persistence-v1-empty-graph-deterministic-and-input-unchanged', 0, () => {
      const input = minimalV1();
      const out1 = migratePayloadV1toV2(input);
      const out2 = migratePayloadV1toV2(input);
      expect(out1).assertEqual(out2);
      expect(input).assertEqual(minimalV1());
      const rec = parsePayloadObject(out1);
      expect((rec['source_instances'] as Array<Object>).length).assertEqual(1);
      expect((rec['dependencies'] as Array<Object>).length).assertEqual(0);
      expect((rec['dependency_groups'] as Array<Object>).length).assertEqual(0);
    });

    it('persistence-v2-to-v3-graph-revision-add-or-keep', 0, () => {
      const out = migratePayloadV2toV3(v2Payload('[{"key":"schema_version","value":"2"}]'));
      const rec = parsePayloadObject(out);
      expect(asNum(rec['payloadVersion'])).assertEqual(3);
      expect(asNum(rec['schemaVersion'])).assertEqual(3);
      const meta = rec['meta'] as Array<Object>;
      expect(meta.length).assertEqual(2);
      let found = false;
      for (let i = 0; i < meta.length; i++) {
        const m = meta[i] as Record<string, Object>;
        if (asStr(m['key']) === 'graph_revision') {
          found = true;
          expect(asStr(m['value'])).assertEqual('0');
        }
      }
      expect(found).assertTrue();

      const out2 = migrateIfNeeded(v2Payload('[{"key":"graph_revision","value":"5"}]'));
      const rec2 = parsePayloadObject(out2);
      const meta2 = rec2['meta'] as Array<Object>;
      expect(meta2.length).assertEqual(1);
      expect(asStr((meta2[0] as Record<string, Object>)['value'])).assertEqual('5');
    });

    it('persistence-entry-version-mismatch-rejected', 0, () => {
      expectThrowsPayloadError(
        () => migratePayloadV1toV2('{"payloadKind":"depmap-logical-graph","payloadVersion":2}'),
        'expects payloadVersion 1');
      expectThrowsPayloadError(
        () => migratePayloadV2toV3('{"payloadKind":"depmap-logical-graph","payloadVersion":3}'),
        'expects payloadVersion 2');
    });

    it('persistence-json-shape-rejections', 0, () => {
      expectThrowsPayloadError(() => migratePayloadV1toV2('{'), 'not valid JSON');
      expectThrowsPayloadError(() => migratePayloadV1toV2('[]'), 'must be an object');
      expectThrowsPayloadError(() => migratePayloadV1toV2('null'), 'must be an object');
    });

    it('persistence-validate-rejects-kind-version-schema', 0, () => {
      const good = buildPayloadJson(new Map<string, Array<Record<string, Object>>>());
      const withKind = (k: string): string => {
        const rec = parsePayloadObject(good);
        rec['payloadKind'] = k;
        return JSON.stringify(rec);
      };
      expectThrowsPayloadError(() => validatePayload(withKind('some-other-app')),
        'payloadKind must be depmap-logical-graph');

      const withPv = (v: number): string => {
        const rec = parsePayloadObject(good);
        rec['payloadVersion'] = v;
        return JSON.stringify(rec);
      };
      expectThrowsPayloadError(() => validatePayload(withPv(0)), 'unsupported payloadVersion');
      expectThrowsPayloadError(() => validatePayload(withPv(4)), 'unsupported payloadVersion');
      expectThrowsPayloadError(() => validatePayload(withPv(99)), 'unsupported payloadVersion');

      const withSvRaw = (payloadJson: string): string => {
        const rec = parsePayloadObject(payloadJson);
        return JSON.stringify(rec);
      };
      expectThrowsPayloadError(() => validatePayload(withSvRaw(good.replace('"schemaVersion":3', '"schemaVersion":4'))),
        'newer than supported');
      expectThrowsPayloadError(() => validatePayload(withSvRaw(good.replace('"schemaVersion":3', '"schemaVersion":99'))),
        'newer than supported');
      expectThrowsPayloadError(() => validatePayload(withSvRaw(good.replace('"schemaVersion":3', '"schemaVersion":"2"'))),
        'schemaVersion must be an integer');
      expectThrowsPayloadError(() => validatePayload(withSvRaw(good.replace('"schemaVersion":3', '"schemaVersion":2.5'))),
        'schemaVersion must be an integer');
      expectThrowsPayloadError(() => validatePayload(withSvRaw(good.replace('"schemaVersion":3', '"schemaVersion":null'))),
        'schemaVersion must be an integer');

      const withSv = (v: Object): string => {
        const rec = parsePayloadObject(good);
        rec['schemaVersion'] = v;
        return JSON.stringify(rec);
      };
      expectThrowsPayloadError(() => validatePayload(withSv('2')), 'schemaVersion must be an integer');
      expectThrowsPayloadError(() => validatePayload(withSv(2.5)), 'schemaVersion must be an integer');
    });

    it('persistence-validate-rejects-missing-table-and-column', 0, () => {
      const good = buildPayloadJson(new Map<string, Array<Record<string, Object>>>());
      const noDeps = good.replace(',"dependencies":[]', '');
      expect(noDeps.indexOf('"dependencies"') < 0).assertTrue();
      expectThrowsPayloadError(() => validatePayload(noDeps),
        'payload.dependencies must be an array');

      const tables = new Map<string, Array<Record<string, Object>>>();
      tables.set('nodes', [JSON.parse('{"id":"n1"}') as Record<string, Object>]);
      const bad = buildPayloadJson(tables);
      expectThrowsPayloadError(() => validatePayload(bad), 'missing column kind');
    });

    it('persistence-build-validate-roundtrip', 0, () => {
      const tables = new Map<string, Array<Record<string, Object>>>();
      tables.set('nodes', [JSON.parse(
        '{"id":"n1","kind":"account","template_id":null,"name":"A","issuer":null,' +
        '"last4":null,"owner":"self","archived":0,"fields_json":"{}","vault_ref":null,' +
        '"wallet_ref":null,"created_at":"x","updated_at":"x"}') as Record<string, Object>]);
      const json = buildPayloadJson(tables);
      validatePayload(json);
      const rec = parsePayloadObject(json);
      expect(asNum(rec['payloadVersion'])).assertEqual(3);
      expect((rec['nodes'] as Array<Object>).length).assertEqual(1);
      expect((rec['source_instances'] as Array<Object>).length).assertEqual(0);
    });

    it('persistence-integrity-detects-orphans-and-clean-is-empty', 0, () => {
      const broken = new Map<string, Array<Record<string, Object>>>();
      broken.set('dependencies', [JSON.parse(
        '{"id":"dep-orphan","from_node":"ghost-api","to_node":"ghost-node"}') as Record<string, Object>]);
      const grpRow = JSON.parse('{"id":"grp-orphan","target_node_id":"ghost-node"}') as Record<string, Object>;
      grpRow['member_edge_ids_json'] = '["dep-ghost"]';
      broken.set('dependency_groups', [grpRow]);
      broken.set('evidence', [JSON.parse(
        '{"id":"ev-orphan","source_instance_id":"ghost-source"}') as Record<string, Object>]);
      broken.set('observation_fingerprints', [JSON.parse(
        '{"fingerprint":"f1","source_instance_id":"ghost-source"}') as Record<string, Object>]);
      const payload = parsePayloadObject(buildPayloadJson(broken));
      const rep = checkGraphIntegrity(payload);
      expect(rep.orphanDependencies.length).assertEqual(1);
      expect(rep.orphanDependencies[0]).assertEqual('dep-orphan');
      expect(rep.orphanGroups.length).assertEqual(1);
      expect(rep.orphanGroups[0]).assertEqual('grp-orphan');
      expect(rep.danglingGroupMembers.length).assertEqual(1);
      expect(rep.danglingGroupMembers[0]).assertEqual('grp-orphan');
      expect(rep.orphanEvidence.length).assertEqual(1);
      expect(rep.orphanEvidence[0]).assertEqual('ev-orphan');
      expect(rep.orphanFingerprints.length).assertEqual(1);

      const clean = parsePayloadObject(buildPayloadJson(new Map<string, Array<Record<string, Object>>>()));
      const cleanRep = checkGraphIntegrity(clean);
      expect(cleanRep.orphanDependencies.length).assertEqual(0);
      expect(cleanRep.orphanGroups.length).assertEqual(0);
      expect(cleanRep.danglingGroupMembers.length).assertEqual(0);
      expect(cleanRep.orphanEvidence.length).assertEqual(0);
      expect(cleanRep.orphanFingerprints.length).assertEqual(0);
    });

    it('persistence-src01-fingerprint-scope-isolation', 0, () => {
      const a = FingerprintScope.scopeKey('wechat_statement', 'si-a', 'txn-1');
      const b = FingerprintScope.scopeKey('wechat_statement', 'si-b', 'txn-1');
      expect(a === b).assertFalse();
      expect(a).assertEqual('wechat_statement:si-a:txn-1');
      expect(b).assertEqual('wechat_statement:si-b:txn-1');
      expect(FingerprintScope.canonicalRowScope('a', 'b', 'core')).assertEqual('a:b:core');
      // 无正则规范化：空白折叠 + 全角括号 + lowercase（单空格保留）
      expect(FingerprintScope.normalizeDescription('  腾讯 （视频）   VIP  '))
        .assertEqual('腾讯 (视频) vip');
      // SourceKind / CoverageMode 域枚举在场
      expect(SOURCE_KINDS.indexOf('statement_file') >= 0).assertTrue();
      expect(COVERAGE_MODES.indexOf('event_stream') >= 0).assertTrue();
    });
    it('persistence-legacy-instance-factory-deterministic', 0, () => {
      const i1 = legacyWechatInstance();
      const i2 = legacyWechatInstance();
      expect(JSON.stringify(i1)).assertEqual(JSON.stringify(i2));
      expect(asStr(i1['id'])).assertEqual('legacy-wechat-statement');
      expect(asStr(i1['source_kind'])).assertEqual('statement_file');
      expect(asStr(i1['currencies_json'])).assertEqual('["CNY"]');
      expect(asStr(i1['created_at'])).assertEqual('1970-01-01T00:00:00.000Z');
    });

    it('persistence-migrated-v1-payload-validates-clean', 0, () => {
      const migrated = migrateIfNeeded(v1WithEvidence());
      validatePayload(migrated);
      const rec = parsePayloadObject(migrated);
      const rep = checkGraphIntegrity(rec);
      expect(rep.orphanEvidence.length).assertEqual(0);
      expect(rep.orphanFingerprints.length).assertEqual(0);
    });
  });
}