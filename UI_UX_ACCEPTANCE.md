# UI_UX_ACCEPTANCE.md — PDIG UI / UX 验收 Gate

> 判定：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`
> 关键约束：本项目当前**无 uni-app x 编译器（B10）**，所有 UI 判定为**源码级**；
> 编译/真机/截图类判定一律 `BLOCKED`，不虚报。

---

## 1. 最终体验标准（§161）

一个第一次使用的普通用户，**不需要理解 Graph / Dependency / Evidence / Revision**，应能完成：

1. 打开 App → 知道它是做什么的
2. 导入一个数据源
3. 看懂系统发现了什么
4. 确认或拒绝
5. 点击"更换银行卡"
6. 看懂哪些事项必须处理
7. 逐步完成
8. 知道什么时候需要重新检查
9. 做加密备份

> **若该完整流程不顺畅，`UI_UX_READY` 不得 PASS。**

## 2. 首页 3 秒标准（§162）

打开 App 后 3 秒内应知道：① 现在有没有需要处理的事？② 近期有没有重要变化？③ 可以从哪个场景开始？

## 3. ChangePlan 标准（§163）

进入计划后应知道：我要做什么？为什么？哪些必须做？哪些只是建议确认？现在能不能继续？判断基于哪些数据？还有没有未确认信息？

## 4. 隐私标准（§164）

导入文件前应知道：文件是否上传？哪些内容会保存？哪些不会保存？备份如何加密？

---

## Gate 清单

### A. 视觉一致性

- [ ] 颜色来自 token，无散落硬编码
- [ ] radius / padding / gap 统一
- [ ] 卡片样式收敛为组件
- [ ] 图标语言统一（无 emoji/线性/彩色混用）
- [ ] 标题长度受控，中文断行正常

### B. 层级与排版

- [ ] Typography 层级（Display/H1/H2/Section/Body/Secondary/Caption/Button）
- [ ] 系统字体，无远程字体加载
- [ ] Spacing scale 统一

### C. 状态完备

- [ ] 每页 Empty State
- [ ] 关键流程 Error State（非 console.error）
- [ ] 异步 Loading / disabled / progress
- [ ] 重复点击防护
- [ ] 破坏性操作确认（不可恢复者二次确认）

### D. 导航

- [ ] 一级导航冻结（tabBar）
- [ ] 无 dead-end 页面
- [ ] 返回 / 深链 / 刷新行为明确

### E. 适配

- [ ] Safe Area（iPhone 刘海 + Home Indicator）
- [ ] Android insets（status/nav bar，edge-to-edge）
- [ ] Harmony insets
- [ ] 小屏 / 大屏 / 平板基本布局
- [ ] 键盘遮挡 / Bottom Tab 遮挡
- [ ] 触控区 ≥ 44×44

### F. 可访问性

- [ ] 文本对比度达标
- [ ] 不依赖颜色单独表达状态
- [ ] Screen reader label
- [ ] 按钮语义
- [ ] 焦点顺序（Web）
- [ ] 长文本 / 大字号不截断

### G. 文案

- [ ] 中文 First
- [ ] 无 `ChangePlan` / `RealityDrift` / `GraphRevision` / `ScenarioCoverage` 等工程词
- [ ] 中文映射正确（见 `docs/DESIGN_SYSTEM.md` §文案映射）
- [ ] 标题短、副标题 1–2 行、技术解释入详情页
- [ ] 无恐吓式文案（无"危险/高危/严重风险"）
- [ ] 无虚假隐私承诺（不写"绝对不会泄露""100% 安全"）

### H. 工程 Demo 痕迹清除（§165）

- [ ] 无内部 ID / UUID / raw enum / JSON / SQL error / stack trace / debug counter 暴露于默认 UI
- [ ] 无 test wording / mock / fixture 名 / localhost
- [ ] 高级信息收敛至"高级详情 / 诊断 / 关于"

### I. 参考 App 隔离（§169）

- [ ] 无参考产品同款图标 / banner / 文案 / 完整布局
- [ ] 仅吸收：场景化入口 / 卡片信息层级 / Upcoming 模式

### J. 前端代码质量（§124）

- [ ] UI 代码通过 format / lint / typecheck（Core 仓内可执行部分）
- [ ] UI 静态 Gate（`check-ui.mjs`）PASS
- [ ] 不以"只是样式"绕过 Engineering Baseline

### K. 截图证据（§168）

- [ ] Home / Scenario Center / Plan Detail / Timeline / Drift / Import / Settings 截图
      → **BLOCKED**（无 HBuilderX / Web-H5 运行环境）

---

## 判定

| 项                | 状态                                                                  |
| ----------------- | --------------------------------------------------------------------- |
| UI_UX_READY       | 见 `PRODUCTION_RC_V1_REPORT.md`（源码级可 PASS；编译/截图类 BLOCKED） |
| Visual Regression | BLOCKED（无运行环境）                                                 |
| Device Visual QA  | BLOCKED（B1/B2/B3）                                                   |
