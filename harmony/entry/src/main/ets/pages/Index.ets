/**
 * Harmony N3 起始页（占位骨架）。
 *
 * §L 的明确要求：**Graph 仍不是首页**，首页是「回答式首页」。
 * 本页当前只作为「工程可编译 / 可安装 / 可启动」的最小证明，
 * 真实的首页语义将在 domain + application 层就绪后替换此处的 placeholder。
 */
import { validateRelationUse } from '../domain/Relations';
import { domainSelfCheckLine } from '../domain/DomainSelfCheck';
import { aadSelfCheckLine } from '../crypto/ContainerSelfCheck';
import { argon2SelfCheckLine } from '../crypto/Argon2idNative';
import { conformanceSummaryLine } from '../conformance/ConformanceSelfCheck';
import { persistenceSelfCheckLine } from '../data/PersistenceSelfCheck';

@Entry
@Component
struct Index {
  @State message: string = 'PDIG — Harmony N3 Native Core';
  @State relationProbe: string = '';
  @State domainProbe: string = '';
  @State containerProbe: string = '';
  @State argon2Probe: string = '';
  @State conformanceProbe: string = '';
  @State persistenceProbe: string = '';

  aboutToAppear(): void {
    // 探针：确认 Domain 层被真正打进 HAP（可被反查字符串），而不是被 tree-shaking 丢弃。
    const ok: boolean = validateRelationUse('payment_instrument', 'funding_source', 'account', 'payment').ok;
    const bad: boolean = validateRelationUse('device', 'bound_to', 'account', 'payment').ok;
    this.relationProbe = `relations: funding_source=${ok ? 'allow' : 'reject'}, bound_to=${bad ? 'allow' : 'reject'}`;
    // 探针：Domain 层（Entities / ImpactKernel）的确定性语义自检。
    //
    // 这一行是 domain/ 下 ImpactKernel 与 Entities 进入编译图的**唯一**入口。
    // hvigor 只编译从 ability / page 可达的模块；若无此 import，
    // 这两个文件会成为孤儿（源文件存在、从未被编译、assembleHap 仍成功），
    // 由 tools/harmony/check-compiled-reachability.mjs 的正/负向 probe 测出。
    this.domainProbe = domainSelfCheckLine();
    // 探针：DEPMAP_CONTAINER_V1 的 JCS / AAD 规范化自检。
    this.containerProbe = aadSelfCheckLine();
    // 探针：NAPI 桥接自检。链路 ArkTS → NAPI(libpdiargon2.so) → libargon2。
    //
    // 无设备时它会如实返回 "argon2: FAIL(native load/exec: ...)"，不伪装成通过。
    this.argon2Probe = argon2SelfCheckLine();
    // 探针：Conformance runner —— 在**设备上**执行平台中立 fixture。
    //
    // 这一行是 conformance/ 整层进入编译图的**唯一**入口，
    // 链路：Index → ConformanceSelfCheck → ConformanceRunner
    //       → { JsonText, HarnessFs, ImpactKernel, PlanReadiness, ScenarioCoverage,
    //           Relations, ScenarioTemplate, StateMachines, GraphRevision, Timeline }。
    //
    // 两件必须看清的事：
    //   1. 它**不是** verdict。fixture 根找不到时它返回 "NOT_RUN (0 executed)"，
    //      并给出投放命令 —— 不会把"环境未就绪"伪装成"用例全失败"；
    //   2. 它**不**把 runner 包装成一句"已实现"。91 这个分母是否满足，
    //      以设备上真实执行到的计数为准。
    this.conformanceProbe = conformanceSummaryLine();
    // 探针：data 持久化逻辑层（payload 编解码 / 迁移 / 版本契约）关键路径自检。
    // 链路：Index → PersistenceSelfCheck → { SchemaV3, PayloadCodec, PayloadMigration }。
    this.persistenceProbe = persistenceSelfCheckLine();
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

        Text(this.domainProbe)
          .fontSize(12)
          .fontColor('#999999')
          .margin({ top: 4 })

        Text(this.containerProbe)
          .fontSize(12)
          .fontColor('#999999')
          .margin({ top: 4 })

        Text(this.argon2Probe)
          .fontSize(12)
          .fontColor('#999999')
          .margin({ top: 4 })

        Text(this.conformanceProbe)
          .fontSize(12)
          .fontColor('#999999')
        Text(this.conformanceProbe)
          .fontSize(12)
          .fontColor('#999999')
          .margin({ top: 4 })

        Text(this.persistenceProbe)
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
