/*
 * Domain 层自检的**主机执行面**。
 *
 * 为什么值得单独跑一次：
 *   `domainSelfCheckLine()` 的 15 项检查此前只是"被编译进 modules.abc、可被字符串反查" ——
 *   那证明的是**编译通过**，不是**断言通过**。两者是独立的事：
 *   一个逻辑写错的自检函数同样能编译并打包。
 *   DomainSelfCheck 不依赖任何 @ohos 模块，因此可以在主机执行面上真跑一遍，
 *   把 `HARMONY_DOMAIN` 从 COMPILED 抬到"真的执行过且全绿"。
 *
 * 判定方式：`domainSelfCheckLine()` 的每一项在**失败**时会内联一个标记 ——
 *   MISMATCH(...) / DUP(...) / ok(false/...)
 *   所以"全绿"等价于整行里不出现这三个东西。
 *   这比去解析每一段的格式更稳：格式可能演进，失败标记是约定的一部分。
 *
 * 仍然不是设备运行时结论 —— 它跑在同一套主机测试框架上。
 */

import { describe, it, expect } from '@ohos/hypium';
import { domainSelfCheckLine } from '../main/ets/domain/DomainSelfCheck';

export default function domainHostTest() {
  describe('domainSelfCheckHost', () => {
    it('domainSelfCheckLineIsAllGreen', 0, () => {
      const line = domainSelfCheckLine();

      // 前缀确认拿到的是汇总行而不是某一段的片段。
      expect(line.startsWith('domain: ')).assertTrue();

      // 三种失败标记一个都不许出现。
      expect(line.indexOf('MISMATCH(')).assertEqual(-1);
      expect(line.indexOf('DUP(')).assertEqual(-1);
      expect(line.indexOf('false')).assertEqual(-1);

      // 15 项自检必须全部在场 —— 少一项会让上面的"没有失败标记"变得廉价。
      const expectedSegments = [
        'required->must_change=',
        'unknown->needs_review=',
        'group_covered->backup_path=',
        'proposal_only',
        'cycle_terminates=',
        'statekey_is_pair=',
        'multi_initial_dedup_sorted=',
        'checklist_target_last=',
        'logicalkey_invariants=',
        'readiness=ok(',
        'coverage=ok(',
        'statemachine=ok(',
        'graphrevision=ok(',
        'timeline=ok(',
        'scenariogate=ok(',
      ];
      for (let i = 0; i < expectedSegments.length; i++) {
        expect(line.indexOf(expectedSegments[i]) >= 0).assertTrue();
      }
    });
  });
}
