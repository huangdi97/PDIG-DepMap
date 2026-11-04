/**
 * Demo / Sample Reality —— 完全 synthetic，绝不写入生产 vault（§21）。
 *
 * 全部通过真实 Core command 建立（不是伪造 projection），但数据本身是示例：
 * 手机号/邮箱使用 reserved 示例域，卡号只保留 last4=0000。
 */
import type { WebCommand } from '../runtime/protocol.ts'

type CommandRunner = (command: WebCommand) => Promise<unknown>

async function run(runner: CommandRunner, command: WebCommand): Promise<string> {
  const result = (await runner(command)) as { id: string }
  return result.id
}

export const DEMO_REALITY = {
  async seed(runner: CommandRunner): Promise<void> {
    const phone = await run(runner, {
      type: 'create_node',
      payload: {
        kind: 'identity_anchor',
        name: '示例主手机号',
        templateId: 'identity_anchor',
        fields: { subtype: 'phone', sample: true },
      },
    })
    const email = await run(runner, {
      type: 'create_node',
      payload: {
        kind: 'identity_anchor',
        name: '示例备用邮箱',
        templateId: 'identity_anchor',
        fields: { subtype: 'email', sample: true },
      },
    })
    const card = await run(runner, {
      type: 'create_node',
      payload: {
        kind: 'payment_instrument',
        name: '示例银行卡',
        issuer: '示例银行',
        last4: '0000',
        fields: { sample: true },
      },
    })
    const account = await run(runner, {
      type: 'create_node',
      payload: {
        kind: 'account',
        name: '示例云账户',
        fields: { sample: true },
      },
    })
    const service = await run(runner, {
      type: 'create_node',
      payload: { kind: 'service', name: '示例视频服务', fields: { sample: true } },
    })
    const membership = await run(runner, {
      type: 'create_node',
      payload: { kind: 'membership', name: '示例会员', fields: { sample: true } },
    })

    await runner({
      type: 'confirm_dependency',
      from: phone,
      relation: 'authenticates',
      to: account,
      capability: 'authentication',
      criticality: 'required',
    })
    await runner({
      type: 'confirm_dependency',
      from: email,
      relation: 'recovers',
      to: account,
      capability: 'recovery',
      criticality: 'unknown',
    })
    await runner({
      type: 'confirm_dependency',
      from: card,
      relation: 'funding_source',
      to: account,
      capability: 'payment',
      criticality: 'required',
    })
    await runner({
      type: 'confirm_dependency',
      from: account,
      relation: 'merchant_agreement',
      to: service,
      capability: 'payment',
      criticality: 'unknown',
    })
    await runner({
      type: 'confirm_dependency',
      from: account,
      relation: 'merchant_agreement',
      to: membership,
      capability: 'payment',
      criticality: 'unknown',
    })

    await runner({
      type: 'create_plan',
      templateId: 'replace_phone_number',
      inputs: { targetPhoneAnchorId: phone },
    })
  },
}
