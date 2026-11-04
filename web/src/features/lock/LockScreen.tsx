import { useState, type ReactNode } from 'react'
import { useRuntime } from '../../app/store.tsx'
import { Button, Field } from '../../components/primitives/index.tsx'

const LIFECYCLE_MESSAGE: Record<string, string> = {
  locked: '本地 vault 已锁定。输入本地密码以解密。',
  unlocking: '正在派生密钥…',
  auth_failed: '口令错误或文件已损坏。数据保持锁定，未做任何修改。',
  corrupt_vault: '本地数据损坏，无法解密。你可以从加密备份恢复。',
  unsupported_vault: '这份数据的格式版本比当前应用更新，已拒绝读取（fail closed）。',
  storage_error: '浏览器存储不可用，无法读取本地 vault。',
}

export function LockScreen(): ReactNode {
  const { status, runCommand, notify } = useRuntime()
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [showRestore, setShowRestore] = useState(false)
  const [backupText, setBackupText] = useState('')
  const [backupPassword, setBackupPassword] = useState('')

  const unlock = async (): Promise<void> => {
    setBusy(true)
    const result = await runCommand({ type: 'unlock', password })
    setBusy(false)
    setPassword('')
    if (result && result.lifecycle === 'unlocked') notify('已解锁', 'success')
  }

  const restore = async (): Promise<void> => {
    if (backupText.trim() === '' || backupPassword === '') {
      notify('请选择备份文件并输入备份口令', 'error')
      return
    }
    const result = await runCommand({
      type: 'restore_backup',
      fileText: backupText,
      password: backupPassword,
      newVaultPassword: null,
    })
    if (result) notify('已从备份恢复', 'success')
  }

  const lifecycle = status?.lifecycle ?? 'locked'

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <p className="auth-kicker">PDIG / DepMap</p>
        <h1>{lifecycle === 'auth_failed' ? '无法解锁' : '本地 vault 已锁定'}</h1>
        <p className="auth-lede">{LIFECYCLE_MESSAGE[lifecycle] ?? '输入本地密码以继续。'}</p>
        {lifecycle === 'auth_failed' ||
        lifecycle === 'corrupt_vault' ||
        lifecycle === 'unsupported_vault' ? (
          <p className="auth-error" role="alert" data-testid="unlock-error">
            {LIFECYCLE_MESSAGE[lifecycle]}
          </p>
        ) : null}
        <Field
          label="本地密码"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') void unlock()
          }}
        />
        <div className="auth-actions">
          <Button
            variant="primary"
            onClick={() => void unlock()}
            disabled={busy || password === ''}
          >
            {busy ? '正在解锁…' : '解锁'}
          </Button>
          <Button variant="ghost" onClick={() => setShowRestore((value) => !value)}>
            {showRestore ? '收起恢复' : '从备份恢复'}
          </Button>
        </div>

        {showRestore ? (
          <div className="auth-restore">
            <label className="field">
              <span className="field-label">备份文件（.depmap）</span>
              <input
                className="input"
                type="file"
                accept=".depmap,application/json"
                onChange={(event) => {
                  const file = event.target.files?.[0]
                  if (!file) return
                  void file.text().then((text) => setBackupText(text))
                }}
              />
            </label>
            <Field
              label="备份口令"
              type="password"
              value={backupPassword}
              onChange={(event) => setBackupPassword(event.target.value)}
              hint="恢复会沿用当前本地密码重新加密；旧数据只在验证通过后被替换。"
            />
            <Button onClick={() => void restore()}>验证并恢复</Button>
          </div>
        ) : null}

        <p className="auth-foot">
          Web 端没有系统级截图保护；锁定后内存中的明文图与密钥会被丢弃，Worker 会被终止。
        </p>
      </div>
    </div>
  )
}
