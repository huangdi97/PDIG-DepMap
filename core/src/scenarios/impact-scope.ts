import type { Capability } from '../domain/types.ts'

export const REPLACE_PHONE_IMPACT_CAPABILITIES = [
  'access',
  'authentication',
  'recovery',
  'communication',
] as const satisfies readonly Capability[]

/**
 * Canonical v0.3 ChangePlan impact scope.
 *
 * Unknown/legacy scenarios retain the frozen payment-domain default.
 */
export function scenarioImpactCapabilities(scenarioId: string): Capability[] {
  if (scenarioId === 'replace_phone_number') {
    return [...REPLACE_PHONE_IMPACT_CAPABILITIES]
  }
  return ['payment']
}
