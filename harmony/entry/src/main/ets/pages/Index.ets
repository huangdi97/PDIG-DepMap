/**
 * PDIG Harmony 首页 —— Personal Infrastructure Briefing（Quiet Infrastructure 方向，v0.3.1 冻结版）。
 *
 * §L 的明确要求：**Graph 仍不是首页**，首页是「回答式首页」。
 * 本页按 docs/uiux/PDIG_UIUX_DIRECTION_FREEZE.md §4 的信息顺序呈现 Briefing 骨架：
 *   需要你处理 / 基础设施薄弱点 / 可能发生了变化 / 即将到来 / 常用场景 / 我的基础设施
 * 所有表现层值一律引用 theme/PdigTokens（禁止页面散用颜色/字号字面量）。
 *
 * 底部「系统自检」由 SelfCheckSection 展示 6 条 domain/crypto/conformance/data 探针
 * （工程编译可达性证明，见 tools/harmony/check-compiled-reachability.mjs）；
 * 探针逻辑在 aboutToAppear 原样保留，仅展示形式从裸文本升级为「label + 值」紧凑行。
 * 职责拆分：场景目录数据见 pages/ScenarioCatalog.ets。
 */
import { validateRelationUse } from '../domain/Relations';
import { domainSelfCheckLine } from '../domain/DomainSelfCheck';
import { aadSelfCheckLine } from '../crypto/ContainerSelfCheck';
import { argon2SelfCheckLine } from '../crypto/Argon2idNative';
import { conformanceSummaryLine } from '../conformance/ConformanceSelfCheck';
import { persistenceSelfCheckLine } from '../data/PersistenceSelfCheck';
import { ScenarioCatalog, ScenarioCell } from './ScenarioCatalog';
import { SelfCheckSection } from './SelfCheckSection';
import {
  ColorPrimary,
  ColorBackground,
  ColorSurface,
  ColorBorder,
  ColorTextPrimary,
  ColorTextSecondary,
  ColorTextTertiary,
  ColorSuccess,
  ColorInfo,
  SpaceXs,
  SpaceSm,
  SpaceMd,
  SpaceLg,
  SpaceXl,
  SpaceXxl,
  RadiusMd,
  FontMeta,
  FontBody,
  FontSection,
  FontTitle,
  WeightMedium,
  WeightSemibold,
  WeightBold,
} from '../theme/PdigTokens';

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

  /** 品牌区：PDIG 标识 + 副标题 + 「本地数据文件已就绪」状态行。 */
  @Builder
  brandBlock() {
    Column({ space: SpaceXxl }) {
      Column({ space: SpaceXs }) {
        Row({ space: SpaceSm }) {
          Row()
            .width(10)
            .height(10)
            .borderRadius(2)
            .backgroundColor(ColorPrimary)
          Text(this.message)
            .fontSize(FontTitle)
            .fontWeight(WeightBold)
            .fontColor(ColorTextPrimary)
        }
        .alignItems(VerticalAlign.Center)

        Text('个人数字基础设施')
          .fontSize(FontBody)
          .fontColor(ColorTextSecondary)
      }
      .width('100%')
      .alignItems(HorizontalAlign.Start)

      Row({ space: SpaceSm }) {
        Text('✓')
          .fontSize(FontBody)
          .fontWeight(WeightMedium)
          .fontColor(ColorSuccess)
        Text('本地数据文件已就绪')
          .fontSize(FontBody)
          .fontColor(ColorTextPrimary)
      }
      .width('100%')
      .alignItems(VerticalAlign.Center)
      .padding({ top: SpaceMd, bottom: SpaceMd })
      .backgroundColor(ColorSurface)
      .borderRadius(RadiusMd)
      .border({ width: 1, color: ColorBorder })
    }
    .width('100%')
  }

  /** section 标题 + 底部 hairline（结构即信息，不做卡片包装）。 */
  @Builder
  sectionHeader(title: string) {
    Column() {
      Text(title)
        .fontSize(FontSection)
        .fontWeight(WeightSemibold)
        .fontColor(ColorTextPrimary)
      Divider()
        .color(ColorBorder)
        .strokeWidth(1)
        .margin({ top: SpaceSm })
    }
    .width('100%')
  }

  /** 状态行：icon + label + color 三通道（状态永不只靠颜色，P0 Gate）。 */
  @Builder
  statusRow(icon: string, label: string, color: string) {
    Row({ space: SpaceSm }) {
      Text(icon)
        .fontSize(FontBody)
        .fontWeight(WeightMedium)
        .fontColor(color)
      Text(label)
        .fontSize(FontBody)
        .fontColor(ColorTextPrimary)
        .layoutWeight(1)
    }
    .width('100%')
    .alignItems(VerticalAlign.Center)
    .padding({ top: SpaceSm, bottom: SpaceSm })
  }

  /** 次要说明行（13/400 语义 → 本页用 FontMeta 层级，避免与正文抢层级）。 */
  @Builder
  metaLine(text: string) {
    Text(text)
      .fontSize(FontMeta)
      .fontColor(ColorTextSecondary)
      .lineHeight(SpaceLg)
      .width('100%')
  }

  /** 常用场景 action cell（2 列网格单元，accent 为行动入口）。 */
  @Builder
  scenarioCell(cell: ScenarioCell) {
    Column({ space: SpaceXs }) {
      Row({ space: SpaceSm }) {
        Text(cell.title)
          .fontSize(FontBody)
          .fontWeight(WeightMedium)
          .fontColor(cell.accent ? ColorPrimary : ColorTextPrimary)
          .layoutWeight(1)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Text('›')
          .fontSize(FontBody)
          .fontColor(ColorTextTertiary)
      }
      .width('100%')
      .alignItems(VerticalAlign.Center)

      Text(cell.desc)
        .fontSize(FontMeta)
        .fontColor(ColorTextSecondary)
        .maxLines(2)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
    }
    .width('100%')
    .layoutWeight(1)
    .padding(SpaceMd)
    .backgroundColor(ColorSurface)
    .borderRadius(RadiusMd)
    .border({ width: 1, color: cell.accent ? ColorPrimary : ColorBorder })
  }

  build() {
    List({ space: SpaceXxl }) {
      ListItem() {
        this.brandBlock()
      }

      ListItem() {
        Column({ space: SpaceSm }) {
          this.sectionHeader('需要你处理')
          this.statusRow('✓', '当前没有需要立即处理的事项', ColorSuccess)
        }
        .width('100%')
      }

      ListItem() {
        Column({ space: SpaceSm }) {
          this.sectionHeader('基础设施薄弱点')
          this.statusRow('?', '未发现不等于一切安全', ColorInfo)
          this.metaLine('薄弱点只来自已确认的依赖关系；没有数据时这里保持未知，不会伪装成安全。')
        }
        .width('100%')
      }

      ListItem() {
        Column({ space: SpaceSm }) {
          this.sectionHeader('可能发生了变化')
          this.statusRow('✓', '暂无待确认的变化', ColorTextSecondary)
          this.metaLine('导入数据源后，这里会提示与记录不一致的地方。')
        }
        .width('100%')
      }

      ListItem() {
        Column({ space: SpaceSm }) {
          this.sectionHeader('即将到来')
          this.metaLine('未来 90 天内没有已计划的变更。')
          this.metaLine('开始一个场景（如更换银行卡）后，步骤与时间线会出现在这里。')
        }
        .width('100%')
      }

      ListItem() {
        Column({ space: SpaceSm }) {
          this.sectionHeader('常用场景')
          ForEach(ScenarioCatalog, (row: ScenarioCell[]) => {
            Row({ space: SpaceMd }) {
              ForEach(row, (cell: ScenarioCell) => {
                this.scenarioCell(cell)
              }, (cell: ScenarioCell): string => cell.title)
            }
            .width('100%')
          }, (row: ScenarioCell[]): string => row[0].title)
        }
        .width('100%')
      }

      ListItem() {
        Column({ space: SpaceSm }) {
          this.sectionHeader('我的基础设施')
          this.statusRow('?', '还没有确认的基础设施对象', ColorInfo)
          this.metaLine('导入数据源并确认关系后，这里会列出账户、支付路径与服务。')
        }
        .width('100%')
      }

      ListItem() {
        SelfCheckSection({
          relationProbe: this.relationProbe,
          domainProbe: this.domainProbe,
          containerProbe: this.containerProbe,
          argon2Probe: this.argon2Probe,
          conformanceProbe: this.conformanceProbe,
          persistenceProbe: this.persistenceProbe,
        })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(ColorBackground)
    .scrollBar(BarState.Off)
    .edgeEffect(EdgeEffect.None)
    .padding({ left: SpaceLg, right: SpaceLg, top: SpaceXl, bottom: SpaceXxl })
  }
}
