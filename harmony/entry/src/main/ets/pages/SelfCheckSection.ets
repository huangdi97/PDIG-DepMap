/**
 * 系统自检 section —— 工程编译可达性证明的展示面板。
 *
 * 背景：hvigor 只编译从 ability / page 可达的模块；为使 domain / crypto /
 * conformance / data 层真正进入编译图并可在 HAP 内反查字符串，
 * Index.ets 的 aboutToAppear 会对每条探针计算一行输出（逻辑在本组件之外，
 * 由 Index 以只读 @Prop 传入，本组件不做任何判断）。
 *
 * 本组件只负责把这些探针值以「label + 值」的紧凑行展示在页面底部
 * 「系统自检」section。探针输出反映工程编译状态，不代表用户数据状态。
 */
import {
  ColorBorder,
  ColorTextPrimary,
  ColorTextSecondary,
  ColorTextTertiary,
  SpaceSm,
  FontMeta,
  FontSection,
  WeightMedium,
  WeightSemibold,
} from '../theme/PdigTokens';

@Component
export struct SelfCheckSection {
  @Prop relationProbe: string = '';
  @Prop domainProbe: string = '';
  @Prop containerProbe: string = '';
  @Prop argon2Probe: string = '';
  @Prop conformanceProbe: string = '';
  @Prop persistenceProbe: string = '';

  @Builder
  probeRow(label: string, value: string) {
    Row({ space: SpaceSm }) {
      Text(label)
        .fontSize(FontMeta)
        .fontWeight(WeightMedium)
        .fontColor(ColorTextSecondary)
        .width(88)
      Text(value)
        .fontSize(FontMeta)
        .fontColor(ColorTextTertiary)
        .layoutWeight(1)
        .maxLines(2)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
    }
    .width('100%')
    .alignItems(VerticalAlign.Top)
    .padding({ top: SpaceSm, bottom: SpaceSm })
  }

  build() {
    Column({ space: SpaceSm }) {
      Text('系统自检')
        .fontSize(FontSection)
        .fontWeight(WeightSemibold)
        .fontColor(ColorTextPrimary)
      Divider()
        .color(ColorBorder)
        .strokeWidth(1)
        .margin({ top: SpaceSm })

      Text('以下为工程自检输出（构建可达性证明），不反映你的数据状态。')
        .fontSize(FontMeta)
        .fontColor(ColorTextSecondary)
        .lineHeight(16)
        .width('100%')

      this.probeRow('关系规则', this.relationProbe)
      this.probeRow('领域语义', this.domainProbe)
      this.probeRow('容器规范', this.containerProbe)
      this.probeRow('Argon2 桥接', this.argon2Probe)
      this.probeRow('一致性套件', this.conformanceProbe)
      this.probeRow('持久化数据', this.persistenceProbe)
    }
    .width('100%')
  }
}
