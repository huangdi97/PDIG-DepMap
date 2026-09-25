// payload 编解码与校验（纯 ArkTS，零 @ohos 依赖；主机单测可执行）。
//
// 移植自 core/src/services/graph-serialize.ts 的冻结基准：
//   - 导出形状：{ payloadKind, payloadVersion, schemaVersion } + 11 张表（固定顺序）
//   - 导入前全量校验：kind / payloadVersion / schemaVersion / 表集合 / 每行列存在性
//   - 孤儿完整性检查（checkGraphIntegrity 的 5 项）
//
// ArkTS 约束下的实现说明：
//   - `JSON.parse` 的结果统一经 `as Object` → `as Record<string, Object>` 访问；
//   - 不做 `Object.keys` / `delete`（ArkTS 不支持），全部操作由 SchemaV3 的
//     列清单驱动，缺失列 = key 不存在 = `undefined`。
//   - DB 原子替换 / 事务属于设备层（data/Repository.ets 后续），本模块只保证
//     「校验在写入前完成、失败不产生任何副作用」的纯函数语义。
//
// 分层约束：只依赖 ../data/SchemaV3。

import {
  GRAPH_PAYLOAD_KIND,
  GRAPH_PAYLOAD_VERSION,
  PAYLOAD_TABLES,
  SCHEMA_VERSION,
  getColumns,
} from '../data/SchemaV3';

/** payload 层错误：code 为 canonical 错误码（migration-spec §2.6）。 */
export class PayloadError extends Error {
  /** payload_invalid | payload_kind_mismatch | payload_version_unsupported | unsupported_schema | payload_table_missing | payload_column_missing */
  code: string = 'payload_invalid';

  constructor(code: string, message: string) {
    super(message);
    this.code = code;
  }
}

/** 解析 payload JSON 为对象；失败抛 PayloadError('payload_invalid', ...)。 */
export function parsePayloadObject(payloadJson: string): Record<string, Object> {
  let parsed: Object;
  try {
    parsed = JSON.parse(payloadJson) as Object;
  } catch (e) {
    throw new PayloadError('payload_invalid', 'payload is not valid JSON');
  }
  if (parsed === null || typeof parsed !== 'object' || parsed instanceof Array) {
    throw new PayloadError('payload_invalid', 'payload must be an object');
  }
  return parsed as Record<string, Object>;
}

function isSafeInt(v: Object): boolean {
  if (typeof v !== 'number') {
    return false;
  }
  const n: number = v as number;
  return Math.floor(n) === n && Math.abs(n) < 9007199254740991;
}

/** 取行内字段；不存在返回 undefined（列缺失的判据）。 */
export function fieldOf(row: Record<string, Object>, col: string): Object {
  return row[col];
}

/** 校验字符串字段。 */
export function requireString(row: Record<string, Object>, col: string): string {
  const v = row[col];
  if (typeof v !== 'string') {
    throw new PayloadError('payload_invalid', `payload.${col} must be a string`);
  }
  return v as string;
}

/** 全量校验（fail closed）：任何一项不满足即抛 PayloadError，不触碰任何外部状态。 */
export function validatePayload(payloadJson: string): void {
  const rec = parsePayloadObject(payloadJson);

  const kind = rec['payloadKind'];
  if (typeof kind !== 'string' || (kind as string) !== GRAPH_PAYLOAD_KIND) {
    throw new PayloadError('payload_kind_mismatch', `payloadKind must be ${GRAPH_PAYLOAD_KIND}`);
  }

  const pv = rec['payloadVersion'];
  if (typeof pv !== 'number' || (pv as number) !== GRAPH_PAYLOAD_VERSION) {
    throw new PayloadError(
      'payload_version_unsupported',
      `unsupported payloadVersion: ${String(pv)} (expected ${GRAPH_PAYLOAD_VERSION})`,
    );
  }

  const sv = rec['schemaVersion'];
  if (!isSafeInt(sv)) {
    throw new PayloadError('payload_invalid', 'schemaVersion must be an integer');
  }
  if ((sv as number) > SCHEMA_VERSION) {
    throw new PayloadError(
      'unsupported_schema',
      `payload schemaVersion (${String(sv)}) newer than supported (${SCHEMA_VERSION})`,
    );
  }

  for (const t of PAYLOAD_TABLES) {
    const value: Object = rec[t];
    if (value === undefined || !(value instanceof Array)) {
      throw new PayloadError('payload_table_missing', `payload.${t} must be an array`);
    }
    const rows = value as Array<Object>;
    const columns = getColumns(t);
    for (const row of rows) {
      if (row === null || typeof row !== 'object' || row instanceof Array) {
        throw new PayloadError('payload_invalid', `payload.${t} rows must be objects`);
      }
      const rowRec = row as Record<string, Object>;
      for (const col of columns) {
        if (rowRec[col] === undefined) {
          throw new PayloadError('payload_column_missing', `payload.${t} row missing column ${col}`);
        }
      }
    }
  }
}

/**
 * 从表数据构建 payload JSON（导出侧）。
 * 表顺序 = PAYLOAD_TABLES 固定顺序；缺少的表输出空数组（形状契约优先）。
 * 纯函数：不触碰 DB。
 */
export function buildPayloadJson(tables: Map<string, Array<Record<string, Object>>>): string {
  const payload: Record<string, Object> = newRecord();
  payload['payloadKind'] = GRAPH_PAYLOAD_KIND;
  payload['payloadVersion'] = GRAPH_PAYLOAD_VERSION;
  payload['schemaVersion'] = SCHEMA_VERSION;
  for (const t of PAYLOAD_TABLES) {
    const rows = tables.get(t);
    payload[t] = (rows === undefined) ? [] : rows;
  }
  return JSON.stringify(payload);
}

// ---------------------------------------------------------------------------
// 孤儿完整性检查（persistence-contract.md §2；5 项全部必须为 []）
// ---------------------------------------------------------------------------

export class OrphanReport {
  orphanDependencies: string[] = [];
  orphanGroups: string[] = [];
  danglingGroupMembers: string[] = [];
  orphanEvidence: string[] = [];
  orphanFingerprints: string[] = [];
}

function stringField(row: Record<string, Object>, col: string): string {
  const v = row[col];
  return (typeof v === 'string') ? (v as string) : '';
}

function tableRowsOf(payload: Record<string, Object>, table: string): Array<Record<string, Object>> {
  const value: Object = payload[table];
  if (value === undefined || !(value instanceof Array)) {
    return [];
  }
  const out: Array<Record<string, Object>> = [];
  for (const row of (value as Array<Object>)) {
    if (row !== null && typeof row === 'object' && !(row instanceof Array)) {
      out.push(row as Record<string, Object>);
    }
  }
  return out;
}

function idSet(rows: Array<Record<string, Object>>): Set<string> {
  const s = new Set<string>();
  for (const r of rows) {
    const id = stringField(r, 'id');
    if (id.length > 0) {
      s.add(id);
    }
  }
  return s;
}

/**
 * 5 项孤儿检查（与 core graph-serialize.ts checkGraphIntegrity 语义一致，
 * 但作用于内存 payload 而非 DB）：
 *   orphanDependencies   —— from_node / to_node 不在 nodes
 *   orphanGroups         —— target_node_id 不在 nodes
 *   danglingGroupMembers —— group.member_edge_ids_json 成员不在 dependencies
 *   orphanEvidence       —— evidence.source_instance_id 不在 source_instances
 *   orphanFingerprints   —— fingerprint.source_instance_id 不在 source_instances
 */
export function checkGraphIntegrity(payload: Record<string, Object>): OrphanReport {
  const report = new OrphanReport();
  const nodes = tableRowsOf(payload, 'nodes');
  const nodeIds = idSet(nodes);
  const deps = tableRowsOf(payload, 'dependencies');
  const depIds = idSet(deps);
  const groups = tableRowsOf(payload, 'dependency_groups');
  const evidence = tableRowsOf(payload, 'evidence');
  const fingerprints = tableRowsOf(payload, 'observation_fingerprints');
  const sources = tableRowsOf(payload, 'source_instances');
  const sourceIds = idSet(sources);

  for (const d of deps) {
    if (nodeIds.has(stringField(d, 'from_node')) && nodeIds.has(stringField(d, 'to_node'))) {
      continue;
    }
    report.orphanDependencies.push(stringField(d, 'id'));
  }
  for (const g of groups) {
    if (!nodeIds.has(stringField(g, 'target_node_id'))) {
      report.orphanGroups.push(stringField(g, 'id'));
    }
  }
  for (const g of groups) {
    const memberJson: string = stringField(g, 'member_edge_ids_json');
    let members: Array<Object> = [];
    if (memberJson.length > 0) {
      try {
        const parsed = JSON.parse(memberJson) as Object;
        if (parsed instanceof Array) {
          members = parsed as Array<Object>;
        }
      } catch (e) {
        members = [];
      }
    }
    for (const m of members) {
      if (typeof m === 'string' && !depIds.has(m as string)) {
        report.danglingGroupMembers.push(stringField(g, 'id'));
        break;
      }
    }
  }
  for (const e of evidence) {
    if (!sourceIds.has(stringField(e, 'source_instance_id'))) {
      report.orphanEvidence.push(stringField(e, 'id'));
    }
  }
  let fpIndex = 0;
  for (const f of fingerprints) {
    if (!sourceIds.has(stringField(f, 'source_instance_id'))) {
      report.orphanFingerprints.push('#'.concat(String(fpIndex)));
    }
    fpIndex++;
  }
  return report;
}

/**
 * 构造一个空动态行对象。
 * ArkTS 禁止未声明类型的对象字面量（arkts-no-untyped-obj-literals），
 * 用 `JSON.parse('{}')` 起底 + 索引赋值是平台既有的通行做法。
 */
export function newRecord(): Record<string, Object> {
  return JSON.parse('{}') as Record<string, Object>;
}

/** 可安全写入 Record 的 null（ArkTS 不允许字面量 null 直接赋给 Object）。 */
export const NULL_VALUE: Object = JSON.parse('null') as Object;