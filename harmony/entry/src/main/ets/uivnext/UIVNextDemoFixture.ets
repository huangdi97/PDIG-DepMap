/**
 * PDIG UI vNext Demo Fixture — HarmonyOS 移植（all synthetic）。
 * 单一真源：spec/ui-vnext/UIVNextDemoFixture.json（人工 port；禁止真实数据）。
 * 冻结：同一 fixture + 同一时间 + 同一 locale + 同一动画状态 + 同一 scale = 截图可复现。
 * 铁律：region 是 UI 层派生属性；不触碰 canonical node schema。
 */
import {
  AttentionItem, ActiveChange, ChangeMigration, ChangeStage, RegionPresentation, UiVNextCard,
  UiVNextNumber, UiVNextRelation, UiVNextService, UpcomingItem, ArcPair,
} from './UIVNextModels';

export class UIVNextDemoFixture {
  static readonly regions: RegionPresentation[] = [
    new RegionPresentation('CN', '中国大陆', 35.86, 104.19),
    new RegionPresentation('HK', '香港', 22.32, 114.17),
    new RegionPresentation('MO', '澳门', 22.2, 113.55),
    new RegionPresentation('GB', '英国', 54.0, -2.5),
    new RegionPresentation('US', '美国', 39.0, -98.0),
    new RegionPresentation('SG', '新加坡', 1.35, 103.82),
  ];

  static readonly cards: UiVNextCard[] = [
    new UiVNextCard('card-cn-1', '招行储蓄卡', '招商银行', '4417', '****4417', 'CN', 'CNY', 'debit', 'physical', '银联', '2028-05', 'active', false, ['日常消费', '工资入账', '自动扣款'], 'deep-space'),
    new UiVNextCard('card-cn-2', '工行信用卡', '中国工商银行', '9023', '****9023', 'CN', 'CNY', 'credit', 'physical', '银联', '2026-11', 'expiring_soon', true, ['订阅扣款', '旅行'], 'region'),
    new UiVNextCard('card-cn-3', '中行数字卡', '中国银行', '6048', '****6048', 'CN', 'CNY', 'debit', 'virtual', '银联', '2030-01', 'active', false, ['数字支付'], 'minimal'),
    new UiVNextCard('card-hk-1', '汇丰卓越理財', 'HSBC 汇丰', '7351', '****7351', 'HK', 'HKD', 'credit', 'physical', 'Visa', '2027-03', 'active', false, ['国际消费', '自动扣款'], 'city'),
    new UiVNextCard('card-hk-2', '中银香港储蓄', '中银香港', '1188', '****1188', 'HK', 'HKD', 'debit', 'physical', '银通', '2030-09', 'active', false, ['本地日常'], 'glass'),
    new UiVNextCard('card-gb-1', 'Monzo 账户卡', 'Monzo', '4602', '****4602', 'GB', 'GBP', 'debit', 'virtual', 'Mastercard', '2029-01', 'active', false, ['日常消费'], 'metal'),
    new UiVNextCard('card-gb-2', 'Revolut 多币种', 'Revolut', '0047', '****0047', 'GB', 'GBP', 'credit', 'virtual', 'Mastercard', '2026-07', 'expiring_soon', true, ['多币种', '订阅'], 'abstract'),
    new UiVNextCard('card-us-1', 'Chase Sapphire', 'Chase', '8220', '****8220', 'US', 'USD', 'credit', 'physical', 'Visa', '2028-12', 'active', false, ['旅行', '订阅'], 'city'),
    new UiVNextCard('card-us-2', '虚拟卡 0109', 'Capital One', '0109', '****0109', 'US', 'USD', 'credit', 'virtual', 'Visa', '2027-06', 'active', false, ['网络订阅'], 'minimal'),
    new UiVNextCard('card-sg-1', 'DBS 银行卡', 'DBS', '3391', '****3391', 'SG', 'SGD', 'credit', 'physical', 'Visa', '2027-08', 'active', false, ['本地消费'], 'glass'),
  ];

  static readonly numbers: UiVNextNumber[] = [
    new UiVNextNumber('num-cn-1', '主号 中国移动', '+86 138****8823', 'CN', '+86', '中国移动', 'SIM', 'primary', ['银行验证', '注册', '2FA'], 'active', true, true),
    new UiVNextNumber('num-cn-2', '工作副号', '+86 137****5510', 'CN', '+86', '中国联通', 'eSIM', 'secondary', ['工作'], 'active', false, false),
    new UiVNextNumber('num-cn-3', '保号副号', '+86 139****2204', 'CN', '+86', '中国移动', 'SIM', 'secondary', ['保号'], 'active', false, false),
    new UiVNextNumber('num-hk-1', '香港主号', '+852 9***4321', 'HK', '+852', '3HK', 'SIM', 'primary', ['银行验证', '2FA'], 'active', true, true),
    new UiVNextNumber('num-gb-1', '英国主号', '+44 7911 182***', 'GB', '+44', 'Vodafone', 'eSIM', 'primary', ['注册', '旅行', '2FA'], 'active', false, false),
    new UiVNextNumber('num-us-1', '美国保号', '+1 415 887 ****', 'US', '+1', 'T-Mobile', 'SIM', 'secondary', ['恢复'], 'active', true, false),
    new UiVNextNumber('num-sg-1', '新加坡主号', '+65 9***2214', 'SG', '+65', 'Singtel', 'eSIM', 'primary', ['银行验证', '工作'], 'active', false, false),
  ];

  static readonly services: UiVNextService[] = [
    new UiVNextService('svc-wxpay', '微信支付', 'CN', 'payment'),
    new UiVNextService('svc-alipay', '支付宝', 'CN', 'payment'),
    new UiVNextService('svc-tencent', '腾讯视频', 'CN', 'subscription'),
    new UiVNextService('svc-hsbc', 'HSBC 网银', 'HK', 'banking'),
    new UiVNextService('svc-apple', 'Apple 订阅', 'US', 'subscription'),
    new UiVNextService('svc-netflix', 'Netflix', 'US', 'subscription'),
    new UiVNextService('svc-amazon', '亚马逊', 'GB', 'subscription'),
    new UiVNextService('svc-microsoft', 'Microsoft 365', 'US', 'subscription'),
    new UiVNextService('svc-dbs', 'DBS 网银', 'SG', 'banking'),
    new UiVNextService('svc-notion', 'Notion', 'GB', 'subscription'),
  ];

  static readonly relations: UiVNextRelation[] = [
    new UiVNextRelation('card-cn-1', 'svc-wxpay', 'funding'),
    new UiVNextRelation('card-cn-1', 'svc-tencent', 'funding'),
    new UiVNextRelation('card-cn-2', 'svc-tencent', 'funding'),
    new UiVNextRelation('card-hk-1', 'svc-hsbc', 'funding'),
    new UiVNextRelation('card-hk-1', 'svc-apple', 'funding'),
    new UiVNextRelation('card-us-1', 'svc-netflix', 'funding'),
    new UiVNextRelation('card-us-1', 'svc-microsoft', 'funding'),
    new UiVNextRelation('card-us-2', 'svc-apple', 'funding'),
    new UiVNextRelation('card-gb-2', 'svc-amazon', 'funding'),
    new UiVNextRelation('num-cn-1', 'svc-wxpay', 'authenticates'),
    new UiVNextRelation('num-cn-1', 'svc-alipay', 'authenticates'),
    new UiVNextRelation('num-hk-1', 'svc-hsbc', 'twoFA'),
    new UiVNextRelation('num-gb-1', 'svc-amazon', 'authenticates'),
    new UiVNextRelation('num-us-1', 'svc-apple', 'twoFA'),
    new UiVNextRelation('num-sg-1', 'svc-hsbc', 'authenticates'),
    new UiVNextRelation('num-sg-1', 'svc-dbs', 'twoFA'),
  ];

  static readonly attentionItems: AttentionItem[] = [
    new AttentionItem('att-1', 'critical', '工行信用卡 11 月到期，绑定 2 项自动扣款', 'card-cn-2'),
    new AttentionItem('att-2', 'warning', 'Revolut 多币种卡 7 月到期，建议更新订阅支付方式', 'card-gb-2'),
    new AttentionItem('att-3', 'warning', '+86 138****8823 是 2 个账户的唯一恢复路径', 'num-cn-1'),
  ];

  static readonly activeChanges: ActiveChange[] = [
    new ActiveChange('chg-1', '更换手机号（进行中）', 'verify_new_phone', 'change-phone'),
  ];

  static readonly upcoming: UpcomingItem[] = [
    new UpcomingItem('upc-1', '工行信用卡 2026-11-15 到期', 47),
    new UpcomingItem('upc-2', 'Revolut 卡 2026-07-20 到期', 20),
  ];

  static readonly changeStages: ChangeStage[] = [
    new ChangeStage(1, 'impact-analysis', 'completed', ''),
    new ChangeStage(2, 'establish-new-number', 'completed', ''),
    new ChangeStage(3, 'verify-new-number', 'verifying', ''),
    new ChangeStage(4, 'migrate-key-accounts', 'not_started', ''),
    new ChangeStage(5, 'check-recovery-paths', 'not_started', ''),
    new ChangeStage(6, 'retire-old-number', 'blocked', '新手机号验证通过后才能停用旧手机号'),
  ];

  static readonly changeMigrations: ChangeMigration[] = [
    new ChangeMigration('微信支付', 'waiting'),
    new ChangeMigration('支付宝', 'waiting'),
    new ChangeMigration('招商银行网银', 'not_started'),
    new ChangeMigration('腾讯视频', 'waiting'),
  ];

  /** 地区聚合（由真实数据计算；无数据 = 0；禁止伪造）。 */
  static regionSummaries(): RegionPresentation[] {
    const result: RegionPresentation[] = [];
    const items = UIVNextDemoFixture.regions;
    for (let i = 0; i < items.length; i++) {
      const r = items[i];
      const cardsHere = UIVNextDemoFixture.cards.filter((c: UiVNextCard): boolean => c.region === r.regionCode).length;
      const phonesHere = UIVNextDemoFixture.numbers.filter((n: UiVNextNumber): boolean => n.region === r.regionCode).length;
      const servicesHere = UIVNextDemoFixture.services.filter((s: UiVNextService): boolean => s.region === r.regionCode).length;
      let attentionHere = 0;
      for (let j = 0; j < UIVNextDemoFixture.attentionItems.length; j++) {
        const item = UIVNextDemoFixture.attentionItems[j];
        const isCard = UIVNextDemoFixture.cards.some((c: UiVNextCard): boolean => c.id === item.target && c.region === r.regionCode);
        const isNumber = UIVNextDemoFixture.numbers.some((n: UiVNextNumber): boolean => n.id === item.target && n.region === r.regionCode);
        if (isCard || isNumber) {
          attentionHere++;
        }
      }
      const accountsHere: number = (r.regionCode === 'CN' || r.regionCode === 'HK' || r.regionCode === 'GB') ? 1 : 0;
      const copy = new RegionPresentation(r.regionCode, r.displayName, r.latitude, r.longitude);
      copy.cardCount = cardsHere;
      copy.phoneCount = phonesHere;
      copy.serviceCount = servicesHere;
      copy.accountCount = accountsHere;
      copy.attentionCount = attentionHere;
      result.push(copy);
    }
    return result;
  }

  static cardById(id: string): UiVNextCard | undefined {
    return UIVNextDemoFixture.cards.find((c: UiVNextCard): boolean => c.id === id);
  }

  static numberById(id: string): UiVNextNumber | undefined {
    return UIVNextDemoFixture.numbers.find((n: UiVNextNumber): boolean => n.id === id);
  }

  static servicesForCard(cardId: string): UiVNextService[] {
    const result: UiVNextService[] = [];
    const rels = UIVNextDemoFixture.relations.filter((rel: UiVNextRelation): boolean => rel.from === cardId);
    for (let i = 0; i < rels.length; i++) {
      const svc = UIVNextDemoFixture.services.find((s: UiVNextService): boolean => s.id === rels[i].to);
      if (svc !== undefined) {
        result.push(svc);
      }
    }
    return result;
  }

  static servicesForNumber(numberId: string): UiVNextService[] {
    const result: UiVNextService[] = [];
    const rels = UIVNextDemoFixture.relations.filter((rel: UiVNextRelation): boolean => rel.from === numberId);
    for (let i = 0; i < rels.length; i++) {
      const svc = UIVNextDemoFixture.services.find((s: UiVNextService): boolean => s.id === rels[i].to);
      if (svc !== undefined) {
        result.push(svc);
      }
    }
    return result;
  }

  /** 跨地区真实关系弧线（来源 fixture relations；禁止装饰性弧线）。 */
  static crossRegionPairs(): ArcPair[] {
    const result: ArcPair[] = [];
    const rels = UIVNextDemoFixture.relations;
    for (let i = 0; i < rels.length; i++) {
      const rel = rels[i];
      const fromRegion = UIVNextDemoFixture.regionOf(rel.from);
      const toRegion = UIVNextDemoFixture.regionOf(rel.to);
      if (fromRegion !== '' && toRegion !== '' && fromRegion !== toRegion) {
        const pair = new ArcPair(fromRegion, toRegion);
        const dup = result.some((p: ArcPair): boolean => (p.a === pair.a && p.b === pair.b) || (p.a === pair.b && p.b === pair.a));
        if (!dup) {
          result.push(pair);
        }
      }
    }
    return result;
  }

  private static regionOf(id: string): string {
    const card = UIVNextDemoFixture.cards.find((c: UiVNextCard): boolean => c.id === id);
    if (card !== undefined) {
      return card.region;
    }
    const num = UIVNextDemoFixture.numbers.find((n: UiVNextNumber): boolean => n.id === id);
    if (num !== undefined) {
      return num.region;
    }
    const svc = UIVNextDemoFixture.services.find((s: UiVNextService): boolean => s.id === id);
    if (svc !== undefined) {
      return svc.region;
    }
    return '';
  }
}
