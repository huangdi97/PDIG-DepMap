/**
 * VNextAssets —— 资产面：AssetCard（卡面）、NumberFace（号码身份面）、ContinuityRail（6 阶段轨）。
 * 卡面背景 = bundled procedural（preset → token 渐变；禁远程图）；last4 按 privacyMask 蒙版；
 * ContinuityRail：current→prerequisite→transition→verification→completion，blocked 必带明文原因。
 */
import { ChangeStage, UiVNextCard, UiVNextNumber, VTestIds } from './UIVNextModels';
import { VColors, VRadius, VSpacing, VType, statusColor, statusLabelZh } from './PdigV2Theme';
import { VLabelChip, VStatusBadge } from './VNextKit';

/** preset → token 渐变（只有 token 色值；procedural 本地素材）。 */
export function cardFaceStops(preset: string): string[] {
  if (preset === 'deep-space') {
    return [VColors.Canvas, VColors.SurfaceRaised, VColors.CanvasDeep];
  }
  if (preset === 'region') {
    return [VColors.PrimarySoft, VColors.Surface, VColors.CanvasDeep];
  }
  if (preset === 'city') {
    return [VColors.PrimarySoft, VColors.SurfaceRaised];
  }
  if (preset === 'glass') {
    return [VColors.SurfaceRaised, VColors.CanvasDeep];
  }
  if (preset === 'metal') {
    return [VColors.SurfaceRaised, VColors.Canvas, VColors.CanvasDeep];
  }
  if (preset === 'abstract') {
    return [VColors.Primary, VColors.PrimarySoft, VColors.CanvasDeep];
  }
  return [VColors.SurfaceRaised, VColors.CanvasDeep];
}

/** AssetCard：可独立操作对象卡面（grid / list / detail 共用；缺失字段 = 未设置/未知）。 */
@Component
export struct VAssetCard {
  @Prop card: UiVNextCard;
  @Prop masked: boolean;
  onTap: () => void = (): void => { };
  build() {
    Column({ space: VSpacing.Md }) {
      Row() {
        Column() {
          Text(this.card.nickname)
            .fontSize(VType.Body)
            .fontWeight(600)
            .fontColor(VColors.TextPrimary)
            .maxLines(1)
          Text(this.card.issuer)
            .fontSize(VType.Meta)
            .fontColor(VColors.TextSecondary)
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
        Text(this.card.form === 'virtual' ? '虚拟卡' : '实体卡')
          .fontSize(VType.Meta)
          .fontColor(VColors.TextMuted)
      }
      .width('100%')

      Blank()
        .height(1)
        .layoutWeight(1)

      Text(`•••• •••• •••• ${this.masked ? '••••' : this.card.last4}`)
        .fontSize(16)
        .fontColor(VColors.TextPrimary)
        .fontFamily('monospace')

      Row() {
        Column() {
          Text(`${this.card.type === 'credit' ? '信用卡' : '储蓄卡'} · ${this.card.network}`)
            .fontSize(VType.Meta)
            .fontColor(VColors.TextSecondary)
          Text(`地区 ${this.card.region} · ${this.card.currency} · 到期 ${this.card.expiry}`)
            .fontSize(VType.Meta)
            .fontColor(VColors.TextMuted)
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
        VStatusBadge({ status: this.card.status })
      }
      .width('100%')
      .alignItems(VerticalAlign.Center)
    }
    .width('100%')
    .aspectRatio(1.586)
    .padding(VSpacing.Xl)
    .linearGradient({
      angle: 135,
      colors: cardFaceStops(this.card.preset),
    })
    .borderRadius(VRadius.Lg)
    .border({ width: 1, color: VColors.BorderSubtle })
    .id(VTestIds.CARD_CARD)
    .onClick((): void => {
      this.onTap();
    })
    .accessibilityText(`${this.card.nickname}，${this.card.issuer}，${this.masked ? '号码已隐藏' : '尾号 ' + this.card.last4}`)
  }
}

/** NumberFace：号码身份面（detail 顶部 / 号码卡片形态）。 */
@Component
export struct VNumberFace {
  @Prop number: UiVNextNumber;
  onTap: () => void = (): void => { };
  build() {
    Column({ space: VSpacing.Md }) {
      Row() {
        Text(this.number.nickname)
          .fontSize(VType.Body)
          .fontWeight(600)
          .fontColor(VColors.TextPrimary)
          .layoutWeight(1)
        VStatusBadge({ status: this.number.status })
      }
      .width('100%')
      .alignItems(VerticalAlign.Center)

      Text(this.number.maskedNumber)
        .fontSize(20)
        .fontWeight(700)
        .fontColor(VColors.TextPrimary)
        .fontFamily('monospace')

      Row({ space: VSpacing.Sm }) {
        VLabelChip({ text: this.number.simKind === 'eSIM' ? 'eSIM' : '实体 SIM', highlight: false })
        VLabelChip({ text: this.roleLabel(this.number.role), highlight: false })
        VLabelChip({ text: `${this.number.countryCode} · ${this.number.region}`, highlight: false })
        if (this.number.recoveryOnly) {
          VLabelChip({ text: '唯一恢复路径', highlight: true })
        }
      }
      .width('100%')

      Text(this.number.usages.join(' · '))
        .fontSize(VType.Meta)
        .fontColor(VColors.TextSecondary)
    }
    .width('100%')
    .padding(VSpacing.Xl)
    .backgroundColor(VColors.SurfaceRaised)
    .borderRadius(VRadius.Lg)
    .border({ width: 1, color: VColors.BorderSubtle })
    .id(VTestIds.PHONE_ROW)
    .onClick((): void => {
      this.onTap();
    })
    .accessibilityText(`${this.number.nickname}，${this.number.maskedNumber}，号码已隐藏`)
  }

  private roleLabel(role: string): string {
    if (role === 'primary') {
      return '主号';
    }
    if (role === 'secondary') {
      return '副号';
    }
    if (role === 'keep') {
      return '保号';
    }
    return role;
  }
}

/** 阶段中文名（continuity-view.md §2）。 */
export function stageLabelZh(key: string): string {
  if (key === 'impact-analysis') {
    return '影响分析';
  }
  if (key === 'establish-new-number') {
    return '建立新号码';
  }
  if (key === 'verify-new-number') {
    return '验证新号码';
  }
  if (key === 'migrate-key-accounts') {
    return '迁移关键账户';
  }
  if (key === 'check-recovery-paths') {
    return '检查恢复路径';
  }
  if (key === 'retire-old-number') {
    return '停用旧号码';
  }
  return key;
}

/** ContinuityRail：6 阶段（completed/current/verifying/blocked/upcoming）+ make-before-break 闸门。 */
@Component
export struct VContinuityRail {
  @Prop stages: ChangeStage[];
  build() {
    Column({ space: VSpacing.Md }) {
      Scroll() {
        Row({ space: VSpacing.Sm }) {
          ForEach(this.stages, (stage: ChangeStage, index: number) => {
            this.railNode(stage);
            if (index < this.stages.length - 1) {
              Column() {
                Blank()
              }
              .width(24)
              .height(2)
              .margin({ top: 22 })
              .backgroundColor(stage.status === 'completed' ? VColors.Positive : VColors.BorderSubtle)
              .borderRadius(1)
            }
          }, (stage: ChangeStage): string => `stage-${stage.stage}`)
        }
        .alignItems(VerticalAlign.Top)
      }
      .scrollable(ScrollDirection.Horizontal)
      .scrollBar(BarState.Off)
      .width('100%')

      // make-before-break 闸门：blocked 阶段明文原因（绝不只禁用）
      if (this.blockedStage() !== undefined) {
        Row({ space: VSpacing.Sm }) {
          Text('!')
            .fontSize(VType.Body)
            .fontWeight(700)
            .fontColor(VColors.Critical)
          Text(this.blockedStage()!.blockReason === '' ? '需要先完成前置步骤' : this.blockedStage()!.blockReason)
            .fontSize(VType.Meta)
            .fontColor(VColors.TextPrimary)
            .layoutWeight(1)
        }
        .width('100%')
        .padding({ left: VSpacing.Lg, right: VSpacing.Lg, top: VSpacing.Sm, bottom: VSpacing.Sm })
        .alignItems(VerticalAlign.Center)
        .backgroundColor('rgba(250, 98, 107, 0.12)')
        .borderRadius(VRadius.Md)
        .id(VTestIds.CHANGE_PHONE_GATE)
        .accessibilityText(`阻止原因：${this.blockedStage()!.blockReason}`)
      }
    }
    .width('100%')
    .padding(VSpacing.Lg)
    .backgroundColor(VColors.Surface)
    .borderRadius(VRadius.Lg)
    .border({ width: 1, color: VColors.BorderSubtle })
    .id(VTestIds.CHANGE_PHONE_RAIL)
  }

  private blockedStage(): ChangeStage | undefined {
    return this.stages.find((s: ChangeStage): boolean => s.status === 'blocked');
  }

  @Builder
  railNode(stage: ChangeStage) {
    Column({ space: 4 }) {
      Row() {
        Text(this.nodeGlyph(stage))
          .fontSize(14)
          .fontWeight(700)
          .fontColor(stage.status === 'completed' ? VColors.CanvasDeep : this.nodeColor(stage))
          .textAlign(TextAlign.Center)
          .width('100%')
      }
      .width(48)
      .height(48)
      .borderRadius(24)
      .backgroundColor(stage.status === 'completed' ? VColors.Positive : VColors.SurfaceRaised)
      .border({
        width: 2,
        color: this.nodeColor(stage),
        style: stage.status === 'upcoming' ? BorderStyle.Dashed : BorderStyle.Solid,
      })
      .alignItems(VerticalAlign.Center)
      .id(`pdig.change.phone.stage${stage.stage}`)

      Text(stageLabelZh(stage.key))
        .fontSize(10)
        .fontColor(stage.status === 'not_started' ? VColors.TextMuted : VColors.TextPrimary)
        .maxLines(2)
        .textAlign(TextAlign.Center)
      if (stage.status === 'upcoming') {
        Text('计划')
          .fontSize(9)
          .fontColor(VColors.TextMuted)
      } else {
        Text(statusLabelZh(stage.status))
          .fontSize(9)
          .fontColor(this.nodeColor(stage))
      }
    }
    .width(64)
    .alignItems(HorizontalAlign.Center)
  }

  private nodeColor(stage: ChangeStage): string {
    return statusColor(stage.status);
  }

  private nodeGlyph(stage: ChangeStage): string {
    if (stage.status === 'completed') {
      return '✓';
    }
    if (stage.status === 'blocked') {
      return '!';
    }
    if (stage.status === 'verifying') {
      return '◷';
    }
    return `${stage.stage}`;
  }
}
