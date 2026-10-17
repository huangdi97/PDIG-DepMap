/**
 * 常用场景目录（首页 2 列 action grid 的静态占位展示数据）。
 *
 * 场景清单语义以 domain/ScenarioTemplate 的可执行注册表为准
 * （active = replace_payment_card「更换银行卡」/ expiring_payment_card「银行卡即将到期」/
 *   close_payment_instrument「注销银行卡」）。
 * 本文件只承载首页入口的展示数据，不含任何场景执行逻辑（不改逻辑层）。
 */
export interface ScenarioCell {
  title: string;
  desc: string;
  /** 该 cell 是否为主行动入口（indigo 强调，非状态色）。 */
  accent: boolean;
}

export const ScenarioCatalog: ScenarioCell[][] = [
  [
    { title: '更换银行卡', desc: '换卡前查看支付路径影响与处理顺序', accent: false },
    { title: '银行卡即将到期', desc: '到期前确认自动扣款与备用路径', accent: false },
  ],
  [
    { title: '注销银行卡', desc: '注销前检查依赖与替代方案', accent: false },
    { title: '前往场景中心', desc: '可以主动准备：检查更多场景', accent: true },
  ],
];