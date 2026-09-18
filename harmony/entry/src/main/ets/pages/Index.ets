/**
 * Harmony N3 起始页（占位骨架）。
 *
 * §L 的明确要求：**Graph 仍不是首页**，首页是「回答式首页」。
 * 本页当前只作为「工程可编译 / 可安装 / 可启动」的最小证明，
 * 真实的首页语义将在 domain + application 层就绪后替换此处的 placeholder。
 */
import { validateRelationUse } from '../domain/Relations';
import { aadSelfCheckLine } from '../crypto/ContainerSelfCheck';
import { argon2SelfCheckLine } from '../crypto/Argon2idNative';

@Entry
@Component
struct Index {
  @State message: string = 'PDIG — Harmony N3 Native Core';
  @State relationProbe: string = '';
  @State containerProbe: string = '';
  @State argon2Probe: string = '';

  aboutToAppear(): void {
    // 探针：确认 Domain 层被真正打进 HAP（可被反查字符串），而不是被 tree-shaking 丢弃。
    const ok: boolean = validateRelationUse('payment_instrument', 'funding_source', 'account', 'payment').ok;
    const bad: boolean = validateRelationUse('device', 'bound_to', 'account', 'payment').ok;
    this.relationProbe = `relations: funding_source=${ok ? 'allow' : 'reject'}, bound_to=${bad ? 'allow' : 'reject'}`;
    // 探针：DEPMAP_CONTAINER_V1 的 JCS / AAD 规范化自检。
    // 这一行同时承担第二个作用 —— 让 crypto/ 下的模块进入 CompileArkTS 的编译图。
    // hvigor 只编译从 ability / page 可达的模块，未被 import 的 .ets 不会被编译，
    // 那种情况下 assembleHap 的 "BUILD SUCCESSFUL" 不构成任何证据。
    this.containerProbe = aadSelfCheckLine();
    // 探针：NAPI 桥接自检。链路 ArkTS → NAPI(libpdiargon2.so) → libargon2。
    //
    // 这一行是 Argon2idNative 进入编译图的**唯一**入口。此前该模块因依赖方向反转
    // 而成为孤儿（源文件存在、从未被编译、构建仍成功），由
    // tools/harmony/check-compiled-reachability.mjs 正向 + 负向 probe 测出。
    //
    // 无设备时它会如实返回 "argon2: FAIL(native load/exec: ...)"，不伪装成通过。
    this.argon2Probe = argon2SelfCheckLine();
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

        Text(this.containerProbe)
          .fontSize(12)
          .fontColor('#999999')
          .margin({ top: 4 })

        Text(this.argon2Probe)
          .fontSize(12)
          .fontColor('#999999')
          .margin({ top: 4 })
      }
      .width('100%')
      .padding(16)
    }
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }
}
