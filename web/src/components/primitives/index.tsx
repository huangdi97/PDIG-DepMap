import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode } from 'react'
import { useEffect, useId, useRef } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'

export function Button({
  variant = 'secondary',
  children,
  className,
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }): ReactNode {
  return (
    <button type="button" className={`btn btn-${variant} ${className ?? ''}`} {...rest}>
      {children}
    </button>
  )
}

export function Field({
  label,
  hint,
  error,
  ...rest
}: InputHTMLAttributes<HTMLInputElement> & {
  label: string
  hint?: string | undefined
  error?: string | undefined
}): ReactNode {
  const id = useId()
  return (
    <label className="field" htmlFor={id}>
      <span className="field-label">{label}</span>
      <input
        id={id}
        aria-label={label}
        className={`input ${error ? 'input-error' : ''}`}
        {...rest}
      />
      {hint ? <span className="field-hint">{hint}</span> : null}
      {error ? <span className="field-error">{error}</span> : null}
    </label>
  )
}

export function Select({
  label,
  value,
  onChange,
  options,
  hint,
  disabled,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  options: Array<{ value: string; label: string }>
  hint?: string | undefined
  disabled?: boolean
}): ReactNode {
  const id = useId()
  return (
    <label className="field" htmlFor={id}>
      <span className="field-label">{label}</span>
      <select
        id={id}
        aria-label={label}
        className="input"
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {hint ? <span className="field-hint">{hint}</span> : null}
    </label>
  )
}

export function Badge({
  tone = 'neutral',
  children,
}: {
  tone?: 'neutral' | 'accent' | 'warn' | 'danger' | 'ok'
  children: ReactNode
}): ReactNode {
  return <span className={`badge badge-${tone}`}>{children}</span>
}

export function EmptyState({
  title,
  detail,
  action,
}: {
  title: string
  detail?: string | undefined
  action?: ReactNode
}): ReactNode {
  return (
    <div className="empty">
      <p className="empty-title">{title}</p>
      {detail ? <p className="empty-detail">{detail}</p> : null}
      {action ? <div className="empty-action">{action}</div> : null}
    </div>
  )
}

export function Loading({ label = '正在加载' }: { label?: string }): ReactNode {
  return (
    <div className="loading" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  )
}

export function ErrorState({ message, retry }: { message: string; retry?: () => void }): ReactNode {
  return (
    <div className="error-state" role="alert">
      <p>{message}</p>
      {retry ? <Button onClick={retry}>重试</Button> : null}
    </div>
  )
}

export function SectionHeader({
  title,
  detail,
  action,
}: {
  title: string
  detail?: string | undefined
  action?: ReactNode
}): ReactNode {
  return (
    <div className="section-header">
      <div>
        <h2>{title}</h2>
        {detail ? <p className="section-detail">{detail}</p> : null}
      </div>
      {action ? <div className="section-action">{action}</div> : null}
    </div>
  )
}

export function Dialog({
  title,
  children,
  onClose,
  footer,
  wide,
}: {
  title: string
  children: ReactNode
  onClose: () => void
  footer?: ReactNode
  wide?: boolean
}): ReactNode {
  const ref = useRef<HTMLDialogElement | null>(null)
  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (!dialog.open) dialog.showModal()
  }, [])
  return (
    <dialog
      ref={ref}
      className={`dialog ${wide ? 'dialog-wide' : ''}`}
      onCancel={(event) => {
        event.preventDefault()
        onClose()
      }}
    >
      <div className="dialog-head">
        <h2>{title}</h2>
        <Button variant="ghost" onClick={onClose} aria-label="关闭">
          ✕
        </Button>
      </div>
      <div className="dialog-body">{children}</div>
      {footer ? <div className="dialog-foot">{footer}</div> : null}
    </dialog>
  )
}

/** 事实计数（明确不是安全分）。 */
export function Fact({ label, value }: { label: string; value: number | string }): ReactNode {
  return (
    <div className="fact">
      <span className="fact-value">{value}</span>
      <span className="fact-label">{label}</span>
    </div>
  )
}
