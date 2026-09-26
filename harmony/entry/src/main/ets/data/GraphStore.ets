// GraphStore —— 内存图存储（纯 ArkTS，零 @ohos；主机单测可执行）。
//
// 对应 core/src 的 SqliteDriver + repositories 的**逻辑持久化语义**：
//   - 每表 = Map<id, row>（插入顺序 = 确定性导出顺序）
//   - 行对象**只换不改**（replace-discipline）：任何更新都构造新行再 map.set，
//     因此 `clone()` 的浅拷贝即可作为快照 —— withTransaction 失败时整体回滚。
//   - schema_version / graph_revision 存 meta。
//   - 导出形状与 SchemaV3.PAYLOAD_TABLES 完全一致（11 表）；change_plans /
//     reality_drifts / discovery_candidates 属 schema v3 的 DB 层（设备），
//     不参与 payload（migration-spec §2.2），本层只记录 schemaVersion。
//
// ArkTS 约束：不用数组解构（arkts-no-destruct-decls），Map 迭代一律
// `.values()` 或 `entry[0]/entry[1]` 索引。
//
// 分层约束：只依赖 ../data/SchemaV3 与 ../data/PayloadCodec。

import {
  PAYLOAD_TABLES,
  SCHEMA_VERSION,
} from '../data/SchemaV3';
import { newRecord } from '../data/PayloadCodec';

/** 逻辑键常量（persistence-contract.md §1）。 */
export const META_KEY_SCHEMA_VERSION: string = 'schema_version';
export const META_KEY_GRAPH_REVISION: string = 'graph_revision';

export class GraphStore {
  nodes: Map<string, Record<string, Object>> = new Map();
  dependencies: Map<string, Record<string, Object>> = new Map();
  dependencyGroups: Map<string, Record<string, Object>> = new Map();
  dependencyProposals: Map<string, Record<string, Object>> = new Map();
  dependencyGroupProposals: Map<string, Record<string, Object>> = new Map();
  proposalEvidenceRefs: Array<Record<string, Object>> = [];
  evidence: Map<string, Record<string, Object>> = new Map();
  observationFingerprints: Map<string, Record<string, Object>> = new Map();
  importSessions: Map<string, Record<string, Object>> = new Map();
  sourceInstances: Map<string, Record<string, Object>> = new Map();
  /** meta：schema_version / graph_revision 等键值（value 为字符串形态）。 */
  meta: Map<string, string> = new Map();
  /** 当前 schema 版本（迁移链维护；与 meta.schema_version 一致）。 */
  schemaVersion: number = SCHEMA_VERSION;

  /** 浅拷贝快照（行对象不原地修改，浅拷贝即安全快照）。 */
  clone(): GraphStore {
    const s = new GraphStore();
    copyMap(this.nodes, s.nodes);
    copyMap(this.dependencies, s.dependencies);
    copyMap(this.dependencyGroups, s.dependencyGroups);
    copyMap(this.dependencyProposals, s.dependencyProposals);
    copyMap(this.dependencyGroupProposals, s.dependencyGroupProposals);
    s.proposalEvidenceRefs = this.proposalEvidenceRefs.slice();
    copyMap(this.evidence, s.evidence);
    copyMap(this.observationFingerprints, s.observationFingerprints);
    copyMap(this.importSessions, s.importSessions);
    copyMap(this.sourceInstances, s.sourceInstances);
    for (const entry of this.meta) {
      s.meta.set(entry[0], entry[1]);
    }
    s.schemaVersion = this.schemaVersion;
    return s;
  }

  /** 整体恢复（withTransaction 失败路径）。 */
  restoreFrom(other: GraphStore): void {
    this.nodes = copyToNew(other.nodes);
    this.dependencies = copyToNew(other.dependencies);
    this.dependencyGroups = copyToNew(other.dependencyGroups);
    this.dependencyProposals = copyToNew(other.dependencyProposals);
    this.dependencyGroupProposals = copyToNew(other.dependencyGroupProposals);
    this.proposalEvidenceRefs = other.proposalEvidenceRefs.slice();
    this.evidence = copyToNew(other.evidence);
    this.observationFingerprints = copyToNew(other.observationFingerprints);
    this.importSessions = copyToNew(other.importSessions);
    this.sourceInstances = copyToNew(other.sourceInstances);
    this.meta = new Map();
    for (const entry of other.meta) {
      this.meta.set(entry[0], entry[1]);
    }
    this.schemaVersion = other.schemaVersion;
  }

  /** 导出侧：按 PAYLOAD_TABLES 顺序生成表数据（确定性）。 */
  exportTables(): Map<string, Array<Record<string, Object>>> {
    const out = new Map<string, Array<Record<string, Object>>>();
    for (const t of PAYLOAD_TABLES) {
      out.set(t, this.tableRows(t));
    }
    return out;
  }

  tableRows(table: string): Array<Record<string, Object>> {
    const rows: Array<Record<string, Object>> = [];
    if (table === 'meta') {
      for (const entry of this.meta) {
        const row = newRecord();
        row['key'] = entry[0];
        row['value'] = entry[1];
        rows.push(row);
      }
      return rows;
    }
    if (table === 'nodes') {
      return valuesOf(this.nodes);
    }
    if (table === 'dependencies') {
      return valuesOf(this.dependencies);
    }
    if (table === 'dependency_groups') {
      return valuesOf(this.dependencyGroups);
    }
    if (table === 'dependency_proposals') {
      return valuesOf(this.dependencyProposals);
    }
    if (table === 'dependency_group_proposals') {
      return valuesOf(this.dependencyGroupProposals);
    }
    if (table === 'proposal_evidence_refs') {
      return this.proposalEvidenceRefs.slice();
    }
    if (table === 'evidence') {
      return valuesOf(this.evidence);
    }
    if (table === 'observation_fingerprints') {
      return valuesOf(this.observationFingerprints);
    }
    if (table === 'import_sessions') {
      return valuesOf(this.importSessions);
    }
    if (table === 'source_instances') {
      return valuesOf(this.sourceInstances);
    }
    return rows;
  }
}

function copyMap(src: Map<string, Record<string, Object>>, dst: Map<string, Record<string, Object>>): void {
  for (const entry of src) {
    dst.set(entry[0], entry[1]);
  }
}

function copyToNew(src: Map<string, Record<string, Object>>): Map<string, Record<string, Object>> {
  const dst = new Map<string, Record<string, Object>>();
  copyMap(src, dst);
  return dst;
}

function valuesOf(src: Map<string, Record<string, Object>>): Array<Record<string, Object>> {
  const rows: Array<Record<string, Object>> = [];
  for (const v of src.values()) {
    rows.push(v);
  }
  return rows;
}