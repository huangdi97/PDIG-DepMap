// SourceInstance 域模型（MVP02 Global Source Abstraction）—— 纯 ArkTS 部分。
//
// 移植自 core/src/domain/source.ts（SourceKind / CoverageMode）与
// core/src/fingerprint/fingerprint.ts 的**作用域**部分（SRC-01）。
//
// 作用域定界（SRC-01：不同 SourceInstance 不得共享 fingerprint 命名空间）：
//   fingerprint 的命名空间 = (adapterId, sourceInstanceId, txnId) 三元组，
//   唯一性维度 = UNIQUE(source_instance_id, fingerprint_version, fingerprint)
//   （persistence-contract.md §1）。
//   本模块实现并测试的是**作用域与规范化契约**（纯字符串语义）；
//   HMAC-SHA256 / SHA-256 哈希步骤依赖平台 crypto（设备层 FingerprintAdapter），
//   主机面无 @ohos，因此哈希不在此实现 —— 不把“未绑定哈希”伪装成完整指纹。
//
// 分层约束：本模块**零依赖**（不 import 任何其他模块）；legacy 实例工厂在
// data/PayloadMigration.ets（payload 形状属于持久化契约层，不属于 domain）。

/** SourceKind（core/src/domain/source.ts 同序）。 */
export const SOURCE_KINDS: string[] = [
  'statement_file',
  'platform_export',
  'open_banking',
  'manual',
  'discovery',
];

/** CoverageMode —— absence 语义声明的四种模式。 */
export const COVERAGE_MODES: string[] = [
  'event_stream',
  'partial_snapshot',
  'complete_snapshot',
  'user_selected',
];

/**
 * SRC-01 作用域契约（纯字符串，无哈希）。
 *
 * 稳定交易号指纹的命名空间：`adapterId:sourceInstanceId:sourceTxnId`。
 * 同一 sourceTxnId 在不同 SourceInstance 下必然得到不同 scope ——
 * 这是「不同 SourceInstance 不共享 fingerprint 命名空间」的最小可测语义。
 */
export class FingerprintScope {
  /** HMAC 输入的命名空间串（SRC-01）。 */
  static scopeKey(adapterId: string, sourceInstanceId: string, sourceTxnId: string): string {
    return `${adapterId}:${sourceInstanceId}:${sourceTxnId}`;
  }

  /** canonical-row 回退的前缀作用域（同文件完全重复行按 ordinal 区分前）。 */
  static canonicalRowScope(adapterId: string, sourceInstanceId: string, canonicalRowCore: string): string {
    return `${adapterId}:${sourceInstanceId}:${canonicalRowCore}`;
  }

  /**
   * normalizeDescription（fingerprint.ts 同语义，无正则实现）：
   * trim → 连续空白折叠为一个空格 → （/）→ (/) → lowercase。
   */
  static normalizeDescription(description: string): string {
    const s = description.trim();
    let out = '';
    let pendingSpace = false;
    for (const ch of s) {
      const ws = ch === ' ' || ch === '\t' || ch === '\n' || ch === '\r';
      if (ws) {
        pendingSpace = out.length > 0;
        continue;
      }
      if (pendingSpace) {
        out += ' ';
        pendingSpace = false;
      }
      out += ch;
    }
    let res = '';
    for (const ch of out) {
      if (ch === '（') {
        res += '(';
      } else if (ch === '）') {
        res += ')';
      } else {
        res += ch;
      }
    }
    return res.toLowerCase();
  }

  /**
   * canonical row 的原始核心串（哈希前）：金额按有符号两位小数文本参与，
   * 其余字段经 normalizeDescription。与 fingerprint.ts canonicalRowOf 同构。
   */
  static canonicalRowCore(
    occurredAt: string,
    signedAmountText: string,
    currency: string,
    description: string,
    counterparty: string,
    transactionType: string,
  ): string {
    return [occurredAt, signedAmountText, currency,
      FingerprintScope.normalizeDescription(description),
      FingerprintScope.normalizeDescription(counterparty), transactionType]
      .join('|');
  }
}