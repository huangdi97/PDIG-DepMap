/**
 * Privacy Mask —— 诚实的能力边界（§24–§26）。
 *
 * Web 没有 Android FLAG_SECURE 等价能力，所以这里只叫「隐私遮蔽」：
 * - 敏感值不进入可见文本
 * - 敏感值也不进入 aria-label / title / data-*
 * - 遮蔽是渲染层的“不呈现”，不是加密；真正的保护来自锁定与加密 vault
 */
import type { ReactNode } from 'react'
import { useRuntime } from '../app/store.tsx'

export const MASK_TEXT = '••••••'

export function useMasked(): boolean {
  return useRuntime().prefs.privacyMask
}

export function Sensitive({
  value,
  className,
}: {
  value: string | null | undefined
  className?: string
}): ReactNode {
  const masked = useMasked()
  if (value === null || value === undefined || value === '')
    return <span className={className}>未记录</span>
  if (masked) {
    return (
      <span className={`sensitive masked ${className ?? ''}`} aria-label="敏感信息已遮蔽">
        {MASK_TEXT}
      </span>
    )
  }
  return <span className={`sensitive ${className ?? ''}`}>{value}</span>
}

/** 复合标签（例如「银行卡 · 0000」）整段遮蔽，避免 last4 泄漏。 */
export function SensitiveLabel({
  parts,
  className,
}: {
  parts: Array<string | null | undefined>
  className?: string
}): ReactNode {
  const masked = useMasked()
  const text = parts.filter((part): part is string => Boolean(part && part !== '')).join(' · ')
  if (text === '') return <span className={className}>未记录</span>
  if (masked) {
    return (
      <span className={`sensitive masked ${className ?? ''}`} aria-label="敏感信息已遮蔽">
        {MASK_TEXT}
      </span>
    )
  }
  return <span className={`sensitive ${className ?? ''}`}>{text}</span>
}

/** 结构化字段的值展示：隐私遮蔽开启时返回遮蔽文本（不返回原值）。 */
export function maskedOrValue(value: string, masked: boolean): string {
  return masked ? MASK_TEXT : value
}
