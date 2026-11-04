import { expect, test } from '@playwright/test'
import {
  PASSWORD,
  addNode,
  addRelation,
  createVault,
  lockFromRail,
  openNav,
  openNode,
  unlock,
} from './helpers.ts'

test.describe.configure({ mode: 'serial' })

test('E2E-10 隐私遮蔽：可见文本 / aria-label / title / 搜索都不泄漏', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'identity_anchor', name: '隐私测试手机号', subtype: 'phone' })

  await openNav(page, '我')
  await page.getByLabel('隐私遮蔽（名称、机构、账号标签）').check()

  await openNav(page, '基础设施')
  const view = page.getByTestId('infrastructure-view')
  await expect(view).not.toContainText('隐私测试手机号')
  await expect(view).toContainText('••••••')

  // DOM 属性不得泄漏
  const html = await page.locator('body').innerHTML()
  expect(html).not.toContain('隐私测试手机号')

  // 搜索也遮蔽
  await page.keyboard.press('Control+k')
  const overlay = page.getByRole('dialog', { name: '全局搜索' })
  await expect(overlay).toBeVisible()
  await overlay.getByRole('textbox', { name: '搜索' }).fill('隐私测试')
  await expect(page.getByTestId('search-masked')).toBeVisible()
  const overlayHtml = await overlay.innerHTML()
  expect(overlayHtml).not.toContain('隐私测试手机号')
})

test('E2E-11 备份导出 → 删除本地数据 → 恢复（深等价）', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'payment_instrument', name: '备份银行卡', last4: '5555' })
  await addNode(page, { kind: 'account', name: '备份账户' })
  await openNode(page, '备份账户')
  await addRelation(page, {
    relation: 'funding_source',
    direction: 'incoming',
    peer: '备份银行卡',
    criticality: 'required',
  })

  await openNav(page, '我')
  const downloadPromise = page.waitForEvent('download')
  await page.getByRole('button', { name: '导出 .depmap' }).click()
  const download = await downloadPromise
  const path = await download.path()
  expect(path).toBeTruthy()
  const { readFileSync } = await import('node:fs')
  const containerJson = readFileSync(path as string, 'utf8')
  const parsed = JSON.parse(containerJson) as Record<string, unknown>
  expect(parsed['format']).toBe('depmap')
  expect(parsed['formatVersion']).toBe(1)

  // 删除本地数据 → 回到 onboarding
  await page.getByRole('button', { name: '删除本地数据' }).click()
  await page.getByRole('button', { name: '再次点击确认删除' }).click()
  await expect(page.getByRole('button', { name: '创建本地加密空间' })).toBeVisible({
    timeout: 30_000,
  })

  // 从备份恢复
  await page.getByRole('button', { name: '从 .depmap 备份恢复' }).click()
  await page.setInputFiles('input[type=file]', {
    name: 'backup.depmap',
    mimeType: 'application/json',
    buffer: Buffer.from(containerJson, 'utf8'),
  })
  await page.getByLabel('备份口令').fill(PASSWORD)
  await page.getByLabel('为这份数据设置本地密码').fill(PASSWORD)
  await page.getByRole('button', { name: '恢复' }).click()
  // 恢复后进入工作区（保留原 hash 路由，因此断言主区域而不是某个具体视图）
  await expect(page.locator('#main')).toBeVisible({ timeout: 60_000 })
  await expect(page.getByLabel('本地密码')).toHaveCount(0)

  await openNav(page, '基础设施')
  await expect(page.getByTestId('infrastructure-view')).toContainText('备份银行卡')
  await openNode(page, '备份账户')
  await expect(page.getByTestId('node-detail')).toContainText('criticality: required')

  // 恢复后的 vault 可用同一口令重新解锁
  await lockFromRail(page)
  await unlock(page)
})

test('E2E-13 篡改本地容器：认证失败且旧 vault 不被破坏', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'account', name: '篡改测试账户' })
  await lockFromRail(page)

  await page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open('pdig-web', 1)
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
    const record = await new Promise<Record<string, unknown>>((resolve, reject) => {
      const tx = db.transaction('vault', 'readwrite')
      const store = tx.objectStore('vault')
      const getRequest = store.get('active')
      getRequest.onsuccess = () => {
        const value = getRequest.result as Record<string, unknown>
        const container = JSON.parse(String(value['containerJson'])) as Record<string, unknown>
        const cipher = atob(String(container['ciphertext']))
        const tampered = `A${cipher.slice(1)}`
        container['ciphertext'] = btoa(tampered)
        value['containerJson'] = JSON.stringify(container)
        store.put(value)
        tx.oncomplete = () => resolve(value)
        tx.onerror = () => reject(tx.error)
      }
      getRequest.onerror = () => reject(getRequest.error)
    })
    if (!record) throw new Error('vault record missing')
    db.close()
  })

  await page.getByLabel('本地密码').fill(PASSWORD)
  await page.getByRole('button', { name: '解锁' }).click()
  await expect(page.getByTestId('unlock-error')).toBeVisible()
  await expect(page.getByTestId('now-view')).toHaveCount(0)
})

test('E2E-14 多标签页单写者：第二个标签页被阻断', async ({ page, context }) => {
  await createVault(page)
  const second = await context.newPage()
  await second.goto('/')
  await expect(second.getByTestId('lease-blocked')).toBeVisible({ timeout: 30_000 })
  await expect(second.getByText('PDIG 已在另一个标签页中打开')).toBeVisible()
  await second.close()
})
