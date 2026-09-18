// DO NOT EDIT 之外的语义 — 语义必须与 TS 冻结基准一致
// 端口自 core/src/scenarios/registry.ts（MVP03 §35–§41）
//
// 分层约束：只依赖 `../generated/CanonicalEnums`，**不依赖 DB / driver / repository**。
//
// 与本 TS 基准的一处**有意差异**（必须显式记录，不得被当作遗漏）：
//   原实现的 ScenarioTemplate 带 `scenarioFactory: (driver, inputs) => ChangePlan`，
//   直接依赖 `SqliteDriver` 与 `ChangePlanService` —— 那是 application/data 层的职责。
//   在 Harmony 侧，domain 层**不得**依赖 relationalStore（§9 约束）。
//   因此这里只保留**描述与校验**部分：
//     availability / requiredInputs / supportedCapabilities / planned gate。
//   真正的"用模板造计划"属于 application 层（尚未落地），
//   本模块以 `validateScenarioInstantiation` 形式提供它必须先通过的那道闸。
//
// 铁律（docs/SCENARIO_TEMPLATE_POLICY.md，原文照录）：
//   - 模板是静态产品配置，不是 Graph Reality；不得直接修改 Dependency；
//   - 只有涉及数字身份/访问/支付/恢复/控制/数据/数字服务连续性的事件才是 PDIG 场景；
//   - planned 模板没有 factory，不可执行、不进入可执行注册（ST gate）；
//   - MVP03 active 模板 = payment-domain（Impact 仅 payment 的现实约束）。

import { Capability, ScenarioAvailability, ScenarioCategory } from '../generated/CanonicalEnums';

export class ScenarioTemplateInputSpec {
  key: string = '';
  required: boolean = false;
  description: string = '';
}

export class ScenarioTemplate {
  id: string = '';
  category: ScenarioCategory = ScenarioCategory.PAYMENT;
  title: string = '';
  description: string = '';
  supportedCapabilities: Capability[] = [];
  requiredInputs: ScenarioTemplateInputSpec[] = [];
  optionalInputs: ScenarioTemplateInputSpec[] = [];
  /** 产品建议提前量（天）；非法律/金融保证，用户可调整。 */
  recommendedLeadTimeDays: number | null = null;
  availability: ScenarioAvailability = ScenarioAvailability.PLANNED;
}

function input(key: string, required: boolean, description: string): ScenarioTemplateInputSpec {
  const s = new ScenarioTemplateInputSpec();
  s.key = key;
  s.required = required;
  s.description = description;
  return s;
}

/**
 * 造一个 payment-domain 模板。
 *
 * 注意 `availability` 恒为 ACTIVE —— 与原文一致：
 * 只有 planned 展示样例才是非 active，且它们**不经过**本构造函数
 * （原文里 planned 模板是手写字面量，没有 factory）。这个不对称是刻意的：
 * 让"能被本函数造出来"本身就成为"可执行"的必要条件之一。
 */
function makePaymentTemplate(
  id: string,
  title: string,
  description: string,
  leadTimeDays: number | null,
): ScenarioTemplate {
  const t = new ScenarioTemplate();
  t.id = id;
  t.category = ScenarioCategory.PAYMENT;
  t.title = title;
  t.description = description;
  t.supportedCapabilities = [Capability.PAYMENT];
  t.requiredInputs = [
    input('targetPaymentInstrumentId', true, '要变更的支付工具节点 id'),
  ];
  t.optionalInputs = [
    input('replacementPaymentInstrumentId', false, '替代支付工具节点 id（可选）'),
    input('effectiveDate', false, '计划生效日期（ISO 8601）'),
  ];
  t.recommendedLeadTimeDays = leadTimeDays;
  t.availability = ScenarioAvailability.ACTIVE;
  return t;
}

/** 可执行注册表：仅 availability='active' 的模板。 */
export const SCENARIO_TEMPLATES: ScenarioTemplate[] = [
  makePaymentTemplate(
    'replace_payment_card',
    '更换银行卡',
    '换卡前检查支付钱包、自动扣款和订阅关系。',
    null,
  ),
  makePaymentTemplate(
    'expiring_payment_card',
    '银行卡即将到期',
    '到期前检查仍依赖这张卡的支付路径。',
    30,
  ),
  makePaymentTemplate(
    'close_payment_instrument',
    '注销银行卡',
    '注销前确认哪些支付关系需要迁移。',
    14,
  ),
];

/**
 * planned 展示样例：不可执行。
 *
 * 保留它的**唯一**目的是让 planned gate 可被真实测试 ——
 * 若移除，`validateScenarioInstantiation` 的拒绝分支将永远得不到执行，
 * 那道闸也就无从证明有效（absence ≠ 该分支是死的）。
 */
export const PLANNED_TEMPLATES: ScenarioTemplate[] = [makePlannedPhoneTemplate()];

function makePlannedPhoneTemplate(): ScenarioTemplate {
  const t = new ScenarioTemplate();
  t.id = 'replace_phone_number';
  t.category = ScenarioCategory.IDENTITY;
  t.title = '更换手机号';
  t.description = '设计稿（未实现）：手机号关联的验证与恢复路径检查。';
  t.supportedCapabilities = [Capability.ACCESS, Capability.RECOVERY, Capability.IDENTITY];
  t.requiredInputs = [input('targetPhoneAnchorId', true, '手机号锚点节点 id')];
  t.optionalInputs = [];
  t.recommendedLeadTimeDays = 30;
  t.availability = ScenarioAvailability.PLANNED;
  return t;
}

export function listActiveTemplates(): ScenarioTemplate[] {
  const out: ScenarioTemplate[] = [];
  for (let i = 0; i < SCENARIO_TEMPLATES.length; i++) {
    const t = SCENARIO_TEMPLATES[i];
    if (t.availability === ScenarioAvailability.ACTIVE) {
      out.push(t);
    }
  }
  return out;
}

export function getScenarioTemplate(id: string): ScenarioTemplate | undefined {
  for (let i = 0; i < SCENARIO_TEMPLATES.length; i++) {
    if (SCENARIO_TEMPLATES[i].id === id) {
      return SCENARIO_TEMPLATES[i];
    }
  }
  for (let i = 0; i < PLANNED_TEMPLATES.length; i++) {
    if (PLANNED_TEMPLATES[i].id === id) {
      return PLANNED_TEMPLATES[i];
    }
  }
  return undefined;
}

/** 校验结果：ok=false 时 reason 说明拒绝原因（对应 ErrorCode）。 */
export class ScenarioValidationResult {
  ok: boolean = false;
  reason: string = '';
  /** 对应 CanonicalEnums.ErrorCode 的 wire 值，便于 UI 映射。 */
  errorCode: string = '';
}

function reject(errorCode: string, reason: string): ScenarioValidationResult {
  const r = new ScenarioValidationResult();
  r.errorCode = errorCode;
  r.reason = reason;
  return r;
}

function accept(): ScenarioValidationResult {
  const r = new ScenarioValidationResult();
  r.ok = true;
  return r;
}

/**
 * ST gate：模板是否可被实例化。
 *
 * 拒绝顺序与原文一致：先"找不到"，再"不可执行"，最后"缺必填输入"。
 * 顺序有意义：它决定了用户看到的第一条错误，
 * 而"模板不存在"和"模板存在但未实现"是两种完全不同的产品反馈。
 *
 * `inputs` 用 Map 而非 Record<string,string>：ArkTS 不支持索引签名，
 * Map 是等价且类型安全的替代表达。
 */
export function validateScenarioInstantiation(
  templateId: string,
  inputs: Map<string, string>,
): ScenarioValidationResult {
  const template = getScenarioTemplate(templateId);
  if (template === undefined) {
    return reject('scenario_missing_input', `scenario template not found: ${templateId}`);
  }
  if (template.availability !== ScenarioAvailability.ACTIVE) {
    return reject(
      'scenario_not_executable',
      `scenario template ${templateId} is not executable (availability=${template.availability})`,
    );
  }
  for (let i = 0; i < template.requiredInputs.length; i++) {
    const req = template.requiredInputs[i];
    const v = inputs.get(req.key);
    if (v === undefined || v.length === 0) {
      return reject('scenario_missing_input', `scenario ${templateId} missing required input: ${req.key}`);
    }
  }
  return accept();
}
