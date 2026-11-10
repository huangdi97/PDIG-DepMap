# WEB_VISUAL_ACCEPTANCE — 视觉验收

> 本文只回答一个问题：**视觉上我们凭什么说“可交付候选”，以及为什么“人眼验收”仍然是 NOT_RUN。**

## 1. 机器可判定的部分（已 PASS）

| 检查           | 命令                                                                     | 结果                                       |
| -------------- | ------------------------------------------------------------------------ | ------------------------------------------ |
| 截图矩阵       | `npx playwright test --project=visual-chromium tests/e2e/visual.spec.ts` | PASS，21 张截图                            |
| 断点无横向溢出 | 同上（第二支测试）                                                       | PASS ×6 视口（1440/1280/1024/768/390/360） |
| 布局审计       | `npx playwright test --project=chromium tests/e2e/layout-audit.spec.ts`  | PASS ×6 视口 × 6 路由                      |

布局审计实际发现并修掉的问题（不是“看起来还行”，是可判定的缺陷）：

1. `.rail` 使用 `height:100vh`，在 demo banner 存在时把「锁定」按钮挤出视口 → 改为 `height:100% / max-height:100vh / overflow-y:auto`。
2. 归档筛选 checkbox 与文字链接的点击目标只有 13×13 / 20px 高 → 统一到 ≥24px 触达面积（18px 复选框 + label padding；链接 min-height 28）。
3. 移动端“主内容被底部导航遮挡”的判定改为「滚到底部后最后一块内容是否被固定导航盖住」，并在 `.shell` 保留 62px 底部安全区。

## 2. 截图清单（21）

onboarding · locked · now-desktop(1440) · now-desktop-1280 · now-1024 · now-tablet · now-mobile(390) · now-mobile-360 · infrastructure · node-detail-phone · node-detail-account · change · change-plan · change-pending-verification · change-stale · change-reanalyzed · records · privacy-on · me · change-mobile · records-mobile

数据模式：`?demo=1` 的 synthetic sample reality（可复现、不含真实个人数据）。
清单文件：`WEB_VNEXT_VISUAL_MANIFEST.json`；原始截图在 `web/artifacts/visual/`（不提交，CI 作为 artifact 上传）。

## 3. 机器不能判的部分

- **视觉回归基线**：`NOT_RUN`。基线必须在 Ubuntu CI + 固定 Chromium + Noto CJK 下生成；本机是 Windows + 系统字体，混比会产生假阳性。
- **人眼验收**：`HUMAN_VISUAL_ACCEPTED = NOT_RUN`。我没有权力代替你接受视觉结果。
  当前状态是 `HUMAN_VISUAL_CANDIDATE = READY`（矩阵与截图可随时人工复核）。

## 4. 视觉方向（延续既有 Web VNext，不推翻）

- light-first、低饱和（slate / green / warm neutral）、靠 spacing + typography + border 建立层级；
- 不是 dashboard 卡片墙：Now 只有事实计数与 1–3 个下一步；
- 桌面是主设计，移动是紧凑 Web（同一套信息架构，不是另做一个 App）；
- 右侧 Truth 面板常驻桌面，用于「已确认基础 / 未确定部分 / 计划真相 / 三个时间」。

## 人眼验收结论（2026-11-10）

**HUMAN_VISUAL_ACCEPTED = ACCEPTED**（由项目所有者确认）

- 视口：`1440×1000` / `1024×768` / `768×1024` / `390×844`
- 页面：onboarding / 现在 / 基础设施（列表 + 对象详情）/ 变更中心 + 计划（Current·Transition·After）/ 记录 / 导入 / 薄弱点 / 我（含隐私遮蔽）
- 检查点：手机无横向溢出、隐私遮蔽无明文残留、计划页无越权完成措辞、无伪精确总分
- 说明：像素级自动回归此前已实测否决（同环境跨运行 CJK 抗锯齿噪声达 8%），故本条为人工验收，不代表像素门禁存在。
