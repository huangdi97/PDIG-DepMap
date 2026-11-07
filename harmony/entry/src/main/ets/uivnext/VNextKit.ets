/**
 * VNextKit —— vNext 共享 UI 元件（StatusBadge / SectionHeader / LabelChip / AttentionRow /
 * RegionListItem / MaskIndicator）。全部 token 颜色来自 PdigV2Theme；testId 四端语义一致。
 * 状态行一律 icon + label + color 三通道（INTERACTION_CONTRACT.md §9）。
 */
import { AttentionItem, RegionPresentation, VTestIds } from './UIVNextModels';
import { VColors, VRadius, VSpacing, VType, statusColor, statusLabelZh } from './PdigV2Theme';

/** StatusBadge：icon（字形）+ label + color 三通道。 */
@Component
export struct VStatusBadge {
  @Prop status: string;
  build() {
    Row({ space: 4 }) {
      Text(this.statusGlyph(this.status))
        .fontSize(VType.Meta)
        .fontWeight(600)
        .fontColor(statusColor(this.status))
      Text(statusLabelZh(this.status))
        .fontSize(VType.Meta)
        .fontWeight(600)
        .fontColor(statusColor(this.status))
    }
    .padding({ left: VSpacing.Sm, right: VSpacing.Sm, top: 3, bottom: 3 })
    .backgroundColor(VColors.SurfaceRaised)
    .borderRadius(VRadius.Sm)
    .border({ width: 1, color: VColors.BorderSubtle })
    .alignItems(VerticalAlign.Center)
  }

  private statusGlyph(status: string): string {
    if (status === 'critical' || status === 'blocked' || status === 'expiring_soon') {
      return '!';
    }
    if (status === 'verifying' || status === 'waiting' || status === 'not_started') {
      return '◷';
    }
    if (status === 'completed' || status === 'active' || status === 'ok' || status === 'migrated') {
      return '✓';
    }
    return '?';
  }
}

/** SectionHeader：小节标题 + hairline。 */
@Component
export struct VSectionHeader {
  @Prop title: string;
  build() {
    Column() {
      Text(this.title)
        .fontSize(VType.SectionTitle)
        .fontWeight(600)
        .fontColor(VColors.TextPrimary)
      Divider()
        .color(VColors.BorderSubtle)
        .strokeWidth(1)
        .margin({ top: VSpacing.Sm })
    }
    .width('100%')
  }
}

/** LabelChip：小标签（用途 / 角色 / SIM 徽标）。 */
@Component
export struct VLabelChip {
  @Prop text: string;
  @Prop highlight: boolean;
  build() {
    Text(this.text)
      .fontSize(VType.Meta)
      .fontWeight(500)
      .fontColor(this.highlight ? VColors.Warning : VColors.TextSecondary)
      .padding({ left: VSpacing.Sm, right: VSpacing.Sm, top: 3, bottom: 3 })
      .backgroundColor(this.highlight ? 'rgba(244, 182, 75, 0.16)' : VColors.PrimarySoft)
      .borderRadius(VRadius.Sm)
      .maxLines(1)
  }
}

/** AttentionRow：一条 must-change / needs-review；critical 最突出；点击走真路由。 */
@Component
export struct VAttentionRow {
  @Prop item: AttentionItem;
  @Prop testIndex: number;
  onTap: () => void = (): void => { };
  build() {
    Row({ space: VSpacing.Lg }) {
      Text(this.item.severity === 'critical' ? '!' : '⚠')
        .fontSize(VType.Body)
        .fontWeight(700)
        .fontColor(statusColor(this.item.severity))
      Column() {
        Text(this.item.title)
          .fontSize(VType.Secondary)
          .fontColor(VColors.TextPrimary)
          .maxLines(2)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Text(statusLabelZh(this.item.severity))
          .fontSize(VType.Meta)
          .fontColor(statusColor(this.item.severity))
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)
      Text('›')
        .fontSize(VType.Body)
        .fontColor(VColors.TextMuted)
    }
    .width('100%')
    .minHeight(48)
    .padding({ left: VSpacing.Lg, right: VSpacing.Lg, top: 8, bottom: 8 })
    .alignItems(VerticalAlign.Center)
    .backgroundColor(this.item.severity === 'critical' ? 'rgba(250, 98, 107, 0.12)' : VColors.Surface)
    .borderRadius(VRadius.Md)
    .border({
      width: 1,
      color: this.item.severity === 'critical' ? 'rgba(250, 98, 107, 0.4)' : VColors.BorderSubtle,
    })
    .id(`pdig.now.attention.item${this.testIndex}`)
    .onClick((): void => {
      this.onTap();
    })
    .accessibilityText(`${statusLabelZh(this.item.severity)}：${this.item.title}`)
  }
}

/** RegionListItem：Globe 的非视觉替代（Region List 行；等价驱动 filter/drawer）。 */
@Component
export struct VRegionListItem {
  @Prop region: RegionPresentation;
  @Prop selected: boolean;
  onTap: () => void = (): void => { };
  build() {
    Row({ space: VSpacing.Lg }) {
      Circle({ width: 8, height: 8 })
        .fill(this.region.attentionCount > 0 ? VColors.Warning : VColors.PrimaryBright)
      Column() {
        Text(this.region.displayName)
          .fontSize(VType.Body)
          .fontColor(VColors.TextPrimary)
        Text(`${this.region.cardCount} 张卡 · ${this.region.phoneCount} 个号码 · ${this.region.serviceCount} 项服务`)
          .fontSize(VType.Meta)
          .fontColor(VColors.TextSecondary)
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)
      if (this.region.attentionCount > 0) {
        Text(`${this.region.attentionCount}`)
          .fontSize(VType.Meta)
          .fontWeight(600)
          .fontColor(VColors.Warning)
          .padding({ left: 8, right: 8, top: 2, bottom: 2 })
          .backgroundColor('rgba(244, 182, 75, 0.18)')
          .borderRadius(VRadius.Sm)
      }
    }
    .width('100%')
    .minHeight(48)
    .padding({ left: VSpacing.Lg, right: VSpacing.Lg, top: 8, bottom: 8 })
    .alignItems(VerticalAlign.Center)
    .backgroundColor(this.selected ? 'rgba(77, 116, 255, 0.18)' : VColors.Surface)
    .borderRadius(VRadius.Md)
    .border({ width: 1, color: this.selected ? VColors.PrimaryBright : VColors.BorderSubtle })
    .id(VTestIds.REGION_ITEM)
    .onClick((): void => {
      this.onTap();
    })
    .accessibilityText(`${this.region.displayName}，${this.region.cardCount} 张卡，${this.region.phoneCount} 个号码`)
  }
}

/** MaskIndicator：全局隐私遮蔽指示（top bar 状态位）。 */
@Component
export struct VMaskIndicator {
  @Prop enabled: boolean;
  build() {
    Row({ space: 4 }) {
      Text('🔒')
        .fontSize(VType.Meta)
        .fontColor(VColors.TextMuted)
      Text(this.enabled ? '隐私遮蔽已开启' : '隐私遮蔽已关闭')
        .fontSize(VType.Meta)
        .fontColor(VColors.TextSecondary)
    }
    .alignItems(VerticalAlign.Center)
  }
}
