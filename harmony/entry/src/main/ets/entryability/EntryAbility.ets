import { UIAbility, AbilityConstant, Want } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { window } from '@kit.ArkUI';

/**
 * Harmony N3 主 Ability（Stage Model）。
 *
 * 设计约束（对齐 Android D-16 教训）：
 * 外部文件选择器会在 **独立任务 / 独立 UIAbility** 中运行，从而触发本 Ability 的
 * onBackground —— 等价于 Android 的 Activity.onStop → LockGate.lockNow()。
 * 因此 N3 **不得**把 Import / Restore 的待投递结果只挂在页面级组件状态里；
 * 该协调职责由 application 层的 FileWorkflowCoordinator 承担（本轮随后实现）。
 */
export default class EntryAbility extends UIAbility {
  private static readonly DOMAIN: number = 0x0D16;
  private static readonly TAG: string = 'PDIG';

  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    hilog.info(EntryAbility.DOMAIN, EntryAbility.TAG, 'EntryAbility onCreate');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    hilog.info(EntryAbility.DOMAIN, EntryAbility.TAG, 'EntryAbility onWindowStageCreate');
    windowStage.loadContent('pages/Index', (err, data) => {
      if (err.code) {
        hilog.error(EntryAbility.DOMAIN, EntryAbility.TAG,
          `loadContent failed, code=${err.code}, message=${err.message}`);
        return;
      }
      hilog.info(EntryAbility.DOMAIN, EntryAbility.TAG, 'loadContent succeeded');
    });
  }

  onBackground(): void {
    hilog.info(EntryAbility.DOMAIN, EntryAbility.TAG, 'EntryAbility onBackground');
  }

  onForeground(): void {
    hilog.info(EntryAbility.DOMAIN, EntryAbility.TAG, 'EntryAbility onForeground');
  }

  onDestroy(): void {
    hilog.info(EntryAbility.DOMAIN, EntryAbility.TAG, 'EntryAbility onDestroy');
  }
}
