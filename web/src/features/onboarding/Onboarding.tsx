import { useState, type ReactNode } from 'react'
import { useRuntime } from '../../app/store.tsx'
import { Button, Field } from '../../components/primitives/index.tsx'

export function Onboarding(): ReactNode {
  const { runCommand, notify, status } = useRuntime()
  const [mode, setMode] = useState<'welcome' | 'create' | 'restore'>('welcome')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [backupText, setBackupText] = useState('')
  const [backupPassword, setBackupPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const create = async (): Promise<void> => {
    setError(null)
    if (password.length < 8) {
      setError('本地密码至少 8 个字符')
      return
    }
    if (password !== confirm) {
      setError('两次输入的密码不一致')
      return
    }
    setBusy(true)
    const result = await runCommand({ type: 'create_vault', password })
    setBusy(false)
    if (!result) setError('无法创建本地 vault，请检查浏览器存储权限')
    else notify('本地加密空间已创建', 'success')
  }

  const restore = async (): Promise<void> => {
    setError(null)
    if (backupText.trim() === '') {
      setError('请选择 .depmap 备份文件')
      return
    }
    if (newPassword.length < 8) {
      setError('本地密码至少 8 个字符')
      return
    }
    setBusy(true)
    const result = await runCommand({
      type: 'restore_backup',
      fileText: backupText,
      password: backupPassword,
      newVaultPassword: newPassword,
    })
    setBusy(false)
    if (!result) setError('恢复失败：口令错误、文件已损坏或版本不受支持。旧数据未被修改。')
    else notify('已从备份恢复', 'success')
  }

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <p className="auth-kicker">PDIG / DepMap</p>
        <h1>个人数字依赖图</h1>
        {mode === 'welcome' ? (
          <>
            <p className="auth-lede">
              在换卡、换号、换邮箱、注销账户之前，看清哪些账户、支付路径与恢复方式会受影响，
              以及哪些关系还没有被确认。
            </p>
            <ul className="auth-points">
              <li>完全本地：没有账号、没有后端、没有遥测。</li>
              <li>加密存储：本地 vault 使用 Argon2id + AES-256-GCM。</li>
              <li>诚实边界：没记录不等于不存在；未验证不等于已验证。</li>
            </ul>
            <div className="auth-actions">
              <Button variant="primary" onClick={() => setMode('create')}>
                创建本地加密空间
              </Button>
              <Button onClick={() => setMode('restore')}>从 .depmap 备份恢复</Button>
            </div>
            {status?.lifecycle === 'storage_error' ? (
              <p className="auth-error">浏览器存储不可用，无法创建本地空间。</p>
            ) : null}
          </>
        ) : null}

        {mode === 'create' ? (
          <>
            <p className="auth-lede">
              设置一个本地密码。它只用于在本机派生加密密钥，<strong>无法找回</strong>，也不会上传。
            </p>
            <Field
              label="本地密码"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
            <Field
              label="确认密码"
              type="password"
              autoComplete="new-password"
              value={confirm}
              onChange={(event) => setConfirm(event.target.value)}
            />
            {error ? (
              <p className="auth-error" role="alert">
                {error}
              </p>
            ) : null}
            <div className="auth-actions">
              <Button variant="primary" onClick={() => void create()} disabled={busy}>
                {busy ? '正在派生密钥…' : '创建'}
              </Button>
              <Button variant="ghost" onClick={() => setMode('welcome')} disabled={busy}>
                返回
              </Button>
            </div>
          </>
        ) : null}

        {mode === 'restore' ? (
          <>
            <p className="auth-lede">
              选择 `.depmap`
              备份文件并输入它的口令。恢复会在验证通过后才写入；失败时不会留下半恢复状态。
            </p>
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
            />
            <Field
              label="为这份数据设置本地密码"
              type="password"
              autoComplete="new-password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              hint="恢复后会立即用这个密码重新加密本地 vault。"
            />
            {error ? (
              <p className="auth-error" role="alert">
                {error}
              </p>
            ) : null}
            <div className="auth-actions">
              <Button variant="primary" onClick={() => void restore()} disabled={busy}>
                {busy ? '正在验证并恢复…' : '恢复'}
              </Button>
              <Button variant="ghost" onClick={() => setMode('welcome')} disabled={busy}>
                返回
              </Button>
            </div>
          </>
        ) : null}

        <p className="auth-foot">
          PDIG
          没有云端账户：如果你忘记本地密码，唯一的恢复方式是此前的加密备份（口令同样无法找回）。
        </p>
      </div>
    </div>
  )
}
