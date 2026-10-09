# Card Image — 更换卡面图片（/infrastructure/cards/{id}/customize）

> Android R10/R19 Product Contract · 2026-10-09  
> Status: **LIGHTWEIGHT_CONSUMER_FEATURE / SOURCE_IMPLEMENTED**  
> Presentation Layer only; Canonical / PersonalReality / .depmap unchanged.

## 1. Product decision

Android 卡片个性化不是“设计工作室”，而是一个**小功能**：

```text
选择一张图片
→ 立即预览
→ 保存并返回
```

用户不需要理解 material / layout / accent hex / preset id，也不应看到工程配置器。

这条决策覆盖旧版 Android Card Studio 的三栏 / Inspector 设计。

## 2. Allowed controls

Android card image surface only exposes:

- 当前卡面预览；
- 从 Android 系统相册选择图片；
- 少量内置图片：原卡面 / 海洋 / 云蓝 / 霞光 / 星夜；
- 保存并返回。

不展示：

- material；
- layout；
- hex color；
- internal preset id；
- logo engineering flags；
- 复杂素材树 / Inspector。

Number appearance remains a separate communication-identity feature and is not
collapsed by this Card decision.

## 3. Storage / privacy boundary

保存对象仍是 PresentationProfile，本机 preference only。

Allowed Android background projection:

```text
backgroundKind = preset      // 原卡面
backgroundKind = r10-art     // 内置消费者图片
backgroundKind = local-image // app-private sanitized JPEG filename
```

Local image rules:

- 必须来自 Android system content picker；
- 最大输入 12 MiB；
- decode 后最长边不超过 2048px；
- 转换为 app-private JPEG；
- PresentationProfile 只存安全 filename；
- 不保存任意 `content://` URI；
- 不保存外部 filesystem path；
- 不上传网络；
- 不写入 .depmap / Canonical / PersonalReality。

## 4. Privacy

- fresh install 默认 **不遮蔽**；
- 是否遮蔽由用户在「我」中决定；
- 卡面本身不得强制改变该全局选择；
- PresentationProfile 的 `maskSensitive` 默认 false；
- 已有用户明确保存的 preference 继续尊重。

## 5. Responsive translation

### Compact
- 填满可用内容宽；
- Preview first；
- 图片选择 / 内置图位于其下；
- 保存按钮始终在上部可达。

### Medium
- 轻量工作区居中；
- 最大内容宽约 680dp；
- 不扩展成多栏工程工具。

### Expanded
- 最大内容宽约 760dp；
- 只是增加留白与更大的 Preview；
- **不得**恢复 Desktop 三栏 Inspector。

## 6. Propagation contract

保存后，同一图片必须复用于：

- Cards list/grid；
- Expanded selected-card inspector；
- Card Detail；
- Card Image preview。

默认卡面仍可使用既有 issuer identity renderer；一旦 profile 为
`r10-art` / `local-image`，所有 Android adaptive surfaces 必须使用同一图片。

## 7. Required test IDs

```text
pdig.r19.card-image.workspace
pdig.customization.preview
pdig.customization.library
pdig.r10.card-art.choose-photo
pdig.r10.card-art.choice.original
pdig.r10.card-art.choice.ocean
pdig.r10.card-art.choice.sky
pdig.r10.card-art.choice.coral
pdig.r10.card-art.choice.night
pdig.r10.card-art.save
```

## 8. Acceptance

- [ ] 手机 / Medium / Expanded 均没有 material/layout/hex engineering controls。
- [ ] Ocean 与 Night runtime pixels 可区分。
- [ ] 相册入口触控目标 ≥48dp。
- [ ] 保存后重建 ViewModel 仍恢复 background kind/value。
- [ ] 保存图片后 List / Inspector / Detail / Preview 同图。
- [ ] 默认 privacy mask = false。
- [ ] local image never leaves app-private Presentation storage。
