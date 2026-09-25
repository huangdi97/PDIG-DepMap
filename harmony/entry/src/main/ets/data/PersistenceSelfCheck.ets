// data 层自检入口 —— 让 data/ 下的纯 ArkTS 模块**进入编译图**（与 domain 层同理）。
//
// 为什么需要它：hvigor 只编译从 ability / page 可达的模块（本项目真实教训，
// 见 DomainSelfCheck.ets 头部）。data/ 层每落地一组就必须有一个真实入边；
// 本模块就是这个入边，只依赖 data/ 内部模块，不引入任何平台依赖。
//
// 本模块**不做**“通过/失败”的健全性声明：它只是把关键路径（build → validate →
// migrate）的输出格式化成一行，供页面上肉眼核对与 HAP 内字符串反查。
// 真正的判定在主机单测 `PersistenceHost.test.ets`（hypium，hvigor test 执行）。

import {
  GRAPH_PAYLOAD_VERSION,
  PAYLOAD_TABLES,
  SCHEMA_VERSION,
} from '../data/SchemaV3';
import {
  buildPayloadJson,
  PayloadError,
  validatePayload,
} from '../data/PayloadCodec';
import { migrateIfNeeded } from '../data/PayloadMigration';
import { FingerprintScope } from '../domain/SourceInstance';

/** 最小 v1 payload（schema 契约要求的 10 张 v1 表全空）。 */
function minimalV1Payload(): string {
  return '{"payloadKind":"depmap-logical-graph","payloadVersion":1,' +
    '"meta":[],"nodes":[],"dependencies":[],"dependency_groups":[],' +
    '"dependency_proposals":[],"dependency_group_proposals":[],"evidence":[],' +
    '"observation_fingerprints":[],"import_sessions":[]}';
}

/** 自检行：payload 契约关键路径（migrate → build → validate）一次跑通。 */
export function persistenceSelfCheckLine(): string {
  try {
    const migrated = migrateIfNeeded(minimalV1Payload());
    validatePayload(migrated);
    const tables = new Map<string, Array<Record<string, Object>>>();
    const json = buildPayloadJson(tables);
    validatePayload(json);
    const scope = FingerprintScope.scopeKey('wechat_statement', 'si-a', 'txn-1');
    return 'payload: kind=ok version='.concat(String(GRAPH_PAYLOAD_VERSION))
      .concat(' schema=').concat(String(SCHEMA_VERSION))
      .concat(' tables=').concat(String(PAYLOAD_TABLES.length))
      .concat(' migrate=v3 build=roundtrip srcscope=').concat(scope);
  } catch (e) {
    const code = (e instanceof PayloadError) ? e.code : 'unknown';
    return 'payload: FAIL('.concat(code).concat(')');
  }
}