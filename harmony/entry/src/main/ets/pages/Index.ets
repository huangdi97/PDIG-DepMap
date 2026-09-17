/**
 * Harmony N3 起始页（占位骨架）。
 *
 * §L 的明确要求：**Graph 仍不是首页**，首页是「回答式首页」。
 * 本页当前只作为「工程可编译 / 可安装 / 可启动」的最小证明，
 * 真实的首页语义将在 domain + application 层就绪后替换此处的 placeholder。
 */
import { validateRelationUse } from '../domain/Relations';

@Entry
@Component
struct Index {
  @State message: string = 'PDIG — Harmony N3 Native Core';
  @State relationProbe: string = '';

  aboutToAppear(): void {
    // 探针：确认 Domain 层被真正打进 HAP（可被反查字符串），而不是被 tree-shaking 丢弃。
    const ok: boolean = validateRelationUse('payment_instrument', 'funding_source', 'account', 'payment').ok;
    const bad: boolean = validateRelationUse('device', 'bound_to', 'account', 'payment').ok;
    this.relationProbe = `relations: funding_source=${ok ? 'allow' : 'reject'}, bound_to=${bad ? 'allow' : 'reject'}`;
  }

  build() {
    Row() {
      Column() {
        Text(this.message)
          .fontSize(20)
          .fontWeight(FontWeight.Medium)
          .textAlign(TextAlign.Center)

        Text('ArkTS / ArkUI / Stage Model skeleton')
          .fontSize(14)
          .fontColor('#666666')
          .margin({ top: 8 })

        Text(this.relationProbe)
          .fontSize(12)
          .fontColor('#999999')
          .margin({ top: 12 })
      }
      .width('100%')
      .padding(16)
    }
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }
}
