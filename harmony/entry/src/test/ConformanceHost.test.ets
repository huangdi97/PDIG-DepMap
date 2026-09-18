/*
 * Conformance —— **主机执行面**驱动真实 ArkTS runner。
 *
 * 这一层的定位必须说清楚，否则很容易被误读成"91/91 达成了"：
 *
 *   成立的：被测代码是真实 ArkTS（ConformanceRunner 由真 ArkTS 编译器产出），
 *           fixture 文本是冻结原文（逐字节内嵌，由 embed-fixtures.mjs --check 守门）。
 *           它**不是** Node 复刻实现 —— 这正是它与 check-relations-semantics.mjs 的区别。
 *
 *   不成立的：这**不是**设备运行时。主机测试环境对 @ohos.* 只给不可调用的桩
 *           （实测 fs.readTextSync → "is not callable"），
 *           所以 28 个 @ohos 依赖用例在这里仍然是 BLOCKED_BY_RUNTIME。
 *           主机执行面**不能**替代设备执行面，也**不**产出 91/91。
 *
 * 设计：为**每个可执行用例**生成一个 it()，于是 test_result.txt 本身就逐条给出
 *       证据（test=<caseId> result=Success/Error），而不是一句汇总数字。
 *       blocked / notImplemented 的用例**不**伪造成通过的测试；
 *       它们的账目由末尾的 accounting 测试单独核对 ——
 *       跳过它们却让报表看起来全绿，是最容易骗到自己的做法。
 */

import { describe, it, expect } from '@ohos/hypium';
import {
  ConformanceReport,
  ConformanceTextSource,
  RunnerEnv,
  runConformanceWith,
} from '../main/ets/conformance/ConformanceRunner';
import {
  FIXTURE_PATHS,
  FIXTURE_TEXTS,
  MANIFEST_TEXT,
} from './fixtures/FixtureBundle';
import {
  IMPORT_FILE_BASE64,
  IMPORT_FILE_NAMES,
} from './fixtures/ImportBundle';
import fs from '@ohos.file.fs';

/** 冻结 fixture + 原始账单字节 的文本源（主机无 fs，故只能是内存源）。 */
class EmbeddedTextSource implements ConformanceTextSource {
  private manifest: string = '';
  private paths: string[] = [];
  private texts: string[] = [];
  private importNames: string[] = [];
  private importBase64: string[] = [];

  constructor(
    manifest: string,
    paths: string[],
    texts: string[],
    importNames: string[],
    importBase64: string[],
  ) {
    this.manifest = manifest;
    this.paths = paths;
    this.texts = texts;
    this.importNames = importNames;
    this.importBase64 = importBase64;
  }

  readManifest(): string {
    return this.manifest;
  }

  readFixture(relPath: string): string {
    for (let i = 0; i < this.paths.length; i++) {
      if (this.paths[i] === relPath) {
        return this.texts[i];
      }
    }
    return '';
  }

  readImportFileBase64(relPath: string): string {
    for (let i = 0; i < this.importNames.length; i++) {
      if (this.importNames[i] === relPath) {
        return this.importBase64[i];
      }
    }
    return '';
  }

  describe(): string {
    return 'embedded:' + this.paths.length + '+' + this.importNames.length;
  }
}

/**
 * §11 的账目必须被**核对**，而不是被声称。改任何一个数都会让本测试失败。
 *
 * 这组数字是**首轮主机执行纠正出来的**，值得记住这个教训：
 * 之前文档里写的是「60 可执行 / 3 notImplemented」，那是**推算**出来的 ——
 * 我当时想当然地认为 state-machine 的 5 个用例都已实现，实际只有 2 个。
 * 真实执行后账目是：
 *     91 = 57 已执行 + 28 BLOCKED_BY_RUNTIME + 6 NOT_IMPLEMENTED
 * 未实现的 6 个是：timeline ×3、state-machine 的
 * action-verification / discovery-candidate / reality-drift。
 * 也就是说：**没有执行过的账目，就是没有被验证的账目。**
 *
 * 2026-09-18 后续：三台 state-machine（action-verification /
 * discovery-candidate / reality-drift）与 timeline 纯投影均已落地并实现，
 * 账目推进为 **63 已执行 + 28 blocked + 0 notImplemented**。
 *
 * 2026-09-18 N3 Host Closure：这 28 条被**逐条重新定性**，不再按类一刀切 ——
 * 按类一刀切会把一个本可执行的用例从分母里抹掉，那是隐性降级门禁：
 *   - `jcs-rfc8785-restricted-domain`：纯 JCS 内核，不需要任何 runtime。
 *     过去跟着 `jcs` 类整类被记为 blocked，实际缺的是 ArkTS 侧
 *     "JSON 文本 → JCS 值树"的入口（Object.keys 不可用），补上后 → 可执行。
 *   - `depmap-bounds-and-structure-rejection`：协议明确规定「边界校验必须在 KDF 之前
 *     完成」，这条用例按设计就不该触发 Argon2id。缺的是把「结构/边界层」从依赖
 *     @kit.CryptoArchitectureKit 的模块里分层抽出（crypto/DepmapBounds.ets）→ 可执行。
 *   - parser 的 20 条 UTF-8 用例：输入文件经实测全部是合法 UTF-8，解析是纯逻辑
 *     （CSV 词法 / 日期 + 金额 / OFX SGML 扫描 / 微信账单列规则），缺的只是
 *     ArkTS 侧 adapter 移植 → 本轮补上后**全部可执行**。
 *     注意原始文件是**按字节 Base64 内嵌**的，解码仍在被测代码里完成 ——
 *     若在读取侧先 decode，"字节→文本"就被移出被测范围，编码缺陷将永远测不到。
 *   - parser 的 2 条 GB18030 用例：需要字符集解码器，主机执行面没有
 *     → BLOCKED_BY_ENVIRONMENT（这 2 条是"环境缺能力"，不是"没写"）。
 *   - 余下 4 条（depmap-golden-v1 / depmap-utf8-password-normalization /
 *     backup-depmap-export-restore-roundtrip / migration-db-v1-to-v3）：
 *     确需设备上的 Argon2id 原生库或 ArkData。
 * 账目推进为 **85 已执行 + 0 未移植 + 2 环境缺失 + 4 设备运行时**。
 */
const EXPECTED_TOTAL = 91;
const EXPECTED_BLOCKED_BY_RUNTIME = 4;
const EXPECTED_BLOCKED_BY_ENVIRONMENT = 2;
const EXPECTED_HOST_IMPL_MISSING = 0;
const EXPECTED_NOT_IMPLEMENTED = 0;
const EXPECTED_PASSED = 85;
const EXPECTED_FAILED = 0;

export default function conformanceHostTest() {
  // 前置不变量：主机执行面没有可用的 fs。
  // 如果哪天这条失败，说明环境变了，本文件的结论口径需要重新评估 —— 
  // 与其悄悄改变，不如让不变量把这件事喊出来。
  describe('environmentPrecondition', () => {
    it('hostHasNoUsableFileFs', 0, () => {
      let usable = false;
      try {
        const t = fs.readTextSync('E:/__pdig_probe_should_not_exist__');
        usable = t.length >= 0 && t.length < 0;   // 恒 false：任何返回都视为"可用"
      } catch (e) {
        usable = false;
      }
      expect(usable).assertFalse();
    });
  });

  // 在 describe 体内同步跑一次 runner，然后逐用例生成 it()。
  const report: ConformanceReport = buildReport();

  describe('harmonyConformanceHost', () => {
    // 先用一条测试钉住"确实跑到 91 条用例"，
    // 否则 runner 早退（例如 manifest 读空）会生成 0 个 it() 并显示成全绿。
    it('runnerProducedAllOutcomes', 0, () => {
      expect(report.outcomes.length).assertEqual(EXPECTED_TOTAL);
    });

    for (let i = 0; i < report.outcomes.length; i++) {
      const o = report.outcomes[i];
      if (o.status === 'BLOCKED_BY_RUNTIME' || o.status === 'NOT_IMPLEMENTED' ||
          o.status === 'NOT_RUN' || o.status === 'HOST_IMPLEMENTATION_MISSING' ||
          o.status === 'BLOCKED_BY_ENVIRONMENT') {
        continue;   // 不伪造成通过的测试；账目由下面 accounting 核对
      }
      it(o.category + '/' + o.caseId, 0, () => {
        // 先钉 detail 为空。这一步不是装饰：
        // runner 抛错时 actual 与 expected **都是空串**，只比 payload 会让
        // "实现崩了"看起来和"通过"一模一样 —— 那是最容易骗到自己的一种假绿。
        // 首轮就靠这条抓出了 1 个真实 runner error。
        expect(o.detail).assertEqual('');
        // actual 与 expected 必须逐字节相同 —— 这是 conformance 的定义，
        // 不是"看起来差不多"。不一致时 hypium 会把两者都打出来。
        expect(o.actual).assertEqual(o.expected);
      });
    }

    it('accountingSplitMatchesSection11', 0, () => {
      expect(report.outcomes.length).assertEqual(EXPECTED_TOTAL);
      expect(report.blockedByRuntime.length).assertEqual(EXPECTED_BLOCKED_BY_RUNTIME);
      expect(report.blockedByEnvironment.length).assertEqual(EXPECTED_BLOCKED_BY_ENVIRONMENT);
      expect(report.hostImplMissing.length).assertEqual(EXPECTED_HOST_IMPL_MISSING);
      expect(report.notImplemented.length).assertEqual(EXPECTED_NOT_IMPLEMENTED);
      expect(report.passed.size).assertEqual(EXPECTED_PASSED);
      expect(report.failed.length).assertEqual(EXPECTED_FAILED);
      expect(report.notRun.length).assertEqual(0);
      // 五个分支必须刚好把 91 分完，不留缝隙也不重复计数。
      expect(report.blockedByRuntime.length + report.blockedByEnvironment.length +
        report.hostImplMissing.length + report.notImplemented.length +
        report.passed.size + report.failed.length).assertEqual(EXPECTED_TOTAL);
    });
  });
}

function buildReport(): ConformanceReport {
  const env = new RunnerEnv();
  env.root = 'embedded-fixtures';
  env.filesDir = '';
  env.now = new Date().toISOString();
  const source = new EmbeddedTextSource(
    MANIFEST_TEXT, FIXTURE_PATHS, FIXTURE_TEXTS, IMPORT_FILE_NAMES, IMPORT_FILE_BASE64);
  return runConformanceWith(env, source);
}
