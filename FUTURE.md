# FUTURE.md — v0.5+ Backlog

这些想法允许记录，但 MVP01 禁止实现。

- access / recovery / identity 跨 capability Impact
- N-of-M DependencyGroup
- SPOF / dominator / SCC 高级分析
- 更多银行 Parser
- 支付宝 Parser
- 通用 CSV mapper
- Clock mechanics / semantics 重构
- 活动追踪 / 持卡净值
- membership 深化
- 时间轴
- Web 无状态工作台
- WebDAV / BYOC 加密同步
- 家庭成员 / 多主体空间
- 桌面小组件
- 小程序只读端
- 更多可视化 Graph
- 商户 alias 社区贡献
- 更完善的 Entity Resolution

明确不默认进入：

- LLM
- Agent
- GraphRAG
- Neo4j
- embedding/vector DB
- GNN
- CRDT
- event sourcing

只有真实需求证明必要时再评审。

---

## 已识别但本轮不实现（登记原因：不扩大 RC 范围 / 依赖 B10 工具链）

- **真删除（crypto-erase）**：删除数据库文件 + 销毁 Keystore/Keychain/HUKS 中的数据库密钥。
  现状：`resetLocalData()` 只清空表数据（`DELETE FROM`），不删文件与密钥，界面文案已如实对齐。
  不实现原因：需要为 `SecureDb` 接口新增 `destroy()` 并在三端原生实现；在当前无编译器（B10）条件下
  属"未经验证的原生代码"，且只删密钥不删文件会让应用无法再打开既有数据库（比不删更糟）。
  另注：SQLite 删除行后 free page 可能残留，但数据库整体由设备绑定密钥加密，残留内容不可读。

- **离开后自动锁定 / 自动锁定超时可配置**：需要前台计时 + 重新鉴权流程。
  现状：设置页原先的开关**无实际效果**，已移除（禁止假开关），仅保留「启动验证始终开启」的只读说明。

- **手动声明 access / recovery / identity capability 的关系**：
  现状：MVP 影响分析只支持 `payment`（AGENTS §13），手动声明固定 capability = payment。

- **Core → UTS 代码生成**：见 `docs/FRONTEND_ARCHITECTURE_AUDIT.md` 与 BLOCKERS B22。
