import { useState, type ReactNode } from 'react'
import { useRuntime } from '../../app/store.tsx'
import { navigate } from '../../app/routes.ts'
import type { DiagnosticsProjection } from '../../runtime/protocol.ts'
import { Badge, Button, Field, Select, SectionHeader } from '../../components/primitives/index.tsx'

export function MeView(): ReactNode {
  const { status, prefs, setPrefs, lock, runCommand, runQuery, notify } = useRuntime()
  const [backupPassword, setBackupPassword] = useState('')
  const [useSeparatePassword, setUseSeparatePassword] = useState(false)
  const [restoreText, setRestoreText] = useState('')
  const [restorePassword, setRestorePassword] = useState('')
  const [diagnostics, setDiagnostics] = useState<DiagnosticsProjection | null>(null)
  const [resetArmed, setResetArmed] = useState(false)
  const [busy, setBusy] = useState<string | null>(null)

  if (!status) return null

  const exportBackup = async (): Promise<void> => {
    setBusy('export')
    const password = useSeparatePassword ? backupPassword : null
    if (useSeparatePassword && backupPassword.length < 8) {
      setBusy(null)
      notify('独立导出密码至少 8 个字符', 'error')
      return
    }
    const result = await runCommand({ type: 'export_backup', password })
    setBusy(null)
    if (!result) return
    const blob = new Blob([result.containerJson], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = result.fileName
    link.click()
    URL.revokeObjectURL(url)
    notify('已导出加密备份（.depmap）', 'success')
  }

  const restore = async (): Promise<void> => {
    if (restoreText === '') {
      notify('请选择备份文件', 'error')
      return
    }
    setBusy('restore')
    const result = await runCommand({
      type: 'restore_backup',
      fileText: restoreText,
      password: restorePassword,
      newVaultPassword: null,
    })
    setBusy(null)
    if (result) notify(`已恢复：graphRevision ${result.graphRevision}`, 'success')
  }

  const reset = async (): Promise<void> => {
    if (!resetArmed) {
      setResetArmed(true)
      return
    }
    setBusy('reset')
    const result = await runCommand({ type: 'reset_local_data' })
    setBusy(null)
    setResetArmed(false)
    if (result) notify('本地数据已删除', 'success')
  }

  return (
    <div className="stack" data-testid="me-view">
      <header className="page-head">
        <div>
          <p className="page-kicker">我</p>
          <h1>本地 vault 与边界</h1>
          <p className="page-lede">
            这里没有账号、订阅或云同步。所有数据都在这个浏览器里，并且需要一个只有你知道的密码。
          </p>
        </div>
      </header>

      <section className="panel">
        <SectionHeader
          title="Vault"
          detail="锁定后内存中的明文图与密钥会被丢弃，Worker 会被终止。"
        />
        <dl className="facts facts-grid">
          <div>
            <dt>状态</dt>
            <dd>
              <Badge tone={status.demoMode ? 'warn' : 'ok'}>
                {status.demoMode ? 'DEMO / SAMPLE REALITY' : status.lifecycle}
              </Badge>
            </dd>
          </div>
          <div>
            <dt>graphRevision</dt>
            <dd>{status.graphRevision}</dd>
          </div>
          <div>
            <dt>schema</dt>
            <dd>{status.schemaVersion}</dd>
          </div>
          <div>
            <dt>最近保存</dt>
            <dd>{status.lastSavedAt ?? '尚未保存'}</dd>
          </div>
        </dl>
        <div className="inline-actions">
          <Button onClick={() => void lock()}>立即锁定</Button>
          <Button variant="ghost" onClick={() => navigate('/import')}>
            数据来源
          </Button>
        </div>
      </section>

      <section className="panel">
        <SectionHeader title="锁定与隐私" />
        <div className="inline-fields">
          <Select
            label="自动锁定"
            value={String(prefs.autoLockMinutes)}
            onChange={(value) => setPrefs({ autoLockMinutes: Number(value) })}
            options={[
              { value: '5', label: '5 分钟' },
              { value: '15', label: '15 分钟（建议）' },
              { value: '30', label: '30 分钟' },
              { value: '0', label: '标签页打开期间不自动锁定' },
            ]}
            hint="这是产品配置，不是安全保证。"
          />
          <label className="check">
            <input
              type="checkbox"
              checked={prefs.privacyMask}
              onChange={(event) => setPrefs({ privacyMask: event.target.checked })}
            />
            隐私遮蔽（名称、机构、账号标签）
          </label>
        </div>
        <p className="form-note">
          Web
          端没有系统级截图保护能力，所以这里只提供「隐私遮蔽」：敏感值不会渲染到页面文本、aria-label
          或 title 中。真正的保护来自锁定与加密 vault。
        </p>
      </section>

      <section className="panel">
        <SectionHeader title="浏览器存储" detail="诚实边界：浏览器的持久化存储仍可能被清理。" />
        <p className="form-note">
          持久化授权：<strong>{status.storage.persisted ? '已授予' : '未授予'}</strong> · 用量：
          {formatBytes(status.storage.usageBytes)} / 配额 {formatBytes(status.storage.quotaBytes)}
        </p>
        <p className="form-note">
          {status.storage.persisted
            ? '浏览器已授予持久化存储，但在存储压力下仍可能清理本地数据。建议保留加密备份。'
            : '浏览器未授予持久化存储，本地数据可能被清理。请创建加密备份。'}
        </p>
      </section>

      <section className="panel">
        <SectionHeader
          title="备份与恢复"
          detail="备份是 DEPMAP_CONTAINER_V1：Argon2id v19 + AES-256-GCM，与三端原生实现互操作。"
        />
        <label className="check">
          <input
            type="checkbox"
            checked={useSeparatePassword}
            onChange={(event) => setUseSeparatePassword(event.target.checked)}
          />
          为这次导出使用独立口令（否则使用本地 vault 口令）
        </label>
        {useSeparatePassword ? (
          <Field
            label="导出口令"
            type="password"
            value={backupPassword}
            onChange={(event) => setBackupPassword(event.target.value)}
            hint="请自行保存：PDIG 无法找回这个口令。"
          />
        ) : null}
        <div className="inline-actions">
          <Button
            variant="primary"
            onClick={() => void exportBackup()}
            disabled={busy !== null || status.demoMode}
          >
            {busy === 'export' ? '正在加密导出…' : '导出 .depmap'}
          </Button>
        </div>
        {status.demoMode ? <p className="form-note">示例数据不能导出为备份。</p> : null}

        <div className="restore-block">
          <label className="field">
            <span className="field-label">恢复：选择 .depmap 文件</span>
            <input
              className="input"
              type="file"
              accept=".depmap,application/json"
              onChange={(event) => {
                const file = event.target.files?.[0]
                if (!file) return
                void file.text().then((text) => setRestoreText(text))
              }}
            />
          </label>
          <Field
            label="备份口令"
            type="password"
            value={restorePassword}
            onChange={(event) => setRestorePassword(event.target.value)}
            hint="恢复会先验证、再原子替换；失败时当前 Reality 保持不变。"
          />
          <Button onClick={() => void restore()} disabled={busy !== null}>
            {busy === 'restore' ? '正在验证…' : '验证并恢复'}
          </Button>
        </div>
      </section>

      <section className="panel">
        <SectionHeader title="诊断" detail="只做本地自检：不联网、不发送。" />
        <div className="inline-actions">
          <Button
            onClick={() => {
              void runQuery({ type: 'get_diagnostics' }).then((result) => setDiagnostics(result))
            }}
          >
            运行本地诊断
          </Button>
        </div>
        {diagnostics ? (
          <dl className="facts facts-grid">
            <div>
              <dt>graphRevision</dt>
              <dd>{diagnostics.graphRevision}</dd>
            </div>
            <div>
              <dt>schema</dt>
              <dd>{diagnostics.schemaVersion}</dd>
            </div>
            <div>
              <dt>表计数</dt>
              <dd>
                {Object.entries(diagnostics.counts)
                  .map(([key, value]) => `${key}:${value}`)
                  .join(' · ')}
              </dd>
            </div>
            <div>
              <dt>孤立引用</dt>
              <dd>
                {Object.entries(diagnostics.integrityOrphans)
                  .map(([key, value]) => `${key}:${value}`)
                  .join(' · ') || '无'}
              </dd>
            </div>
            <div>
              <dt>可用模板</dt>
              <dd>{diagnostics.templates.join('、')}</dd>
            </div>
          </dl>
        ) : null}
      </section>

      <section className="panel panel-danger">
        <SectionHeader
          title="删除本地数据"
          detail="危险操作：删除本地加密 vault 与内存中的权威实例，并回到首次使用状态。"
        />
        <p className="form-note">已经下载到其它位置的 .depmap 备份不会被删除。</p>
        <div className="inline-actions">
          <Button variant="danger" onClick={() => void reset()} disabled={busy !== null}>
            {resetArmed ? '再次点击确认删除' : '删除本地数据'}
          </Button>
          {resetArmed ? (
            <Button variant="ghost" onClick={() => setResetArmed(false)}>
              取消
            </Button>
          ) : null}
        </div>
      </section>

      <section className="panel panel-quiet">
        <SectionHeader title="关于" />
        <ul className="truth-list">
          <li>PDIG 不提供密码管理、记账或支付功能。</li>
          <li>没有后端、没有账号、没有遥测；本页面不会发起任何业务网络请求。</li>
          <li>Observe ≠ Reality：导入的文件只会生成提案，不会自动改写你确认的关系。</li>
        </ul>
        <div className="inline-actions">
          <a className="link" href="../privacy/">
            隐私说明
          </a>
          <a className="link" href="../support/">
            支持
          </a>
        </div>
      </section>
    </div>
  )
}

function formatBytes(value: number | null): string {
  if (value === null) return '未知'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}
