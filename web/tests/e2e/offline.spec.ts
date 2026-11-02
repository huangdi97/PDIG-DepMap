import { expect, test } from '@playwright/test'
import { PASSWORD, addNode, createVault, lockFromRail, openNav, unlock } from './helpers.ts'

test.describe.configure({ mode: 'serial' })

test('E2E-15 离线：reload 后仍可解锁并读写', async ({ page, context }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'account', name: '离线测试账户' })

  // 等待 Service Worker 接管（第二次访问后 controller 才存在）
  await page.waitForFunction(() => navigator.serviceWorker?.controller !== null, undefined, {
    timeout: 30_000,
  })
  await page.reload()
  await expect(page.getByLabel('本地密码')).toBeVisible({ timeout: 30_000 })
  await unlock(page)

  await context.setOffline(true)
  await page.reload()
  await expect(
    page.getByRole('heading', { name: '个人数字依赖图' }).or(page.getByLabel('本地密码')),
  ).toBeVisible({
    timeout: 60_000,
  })
  await unlock(page)
  await openNav(page, '基础设施')
  await expect(page.getByTestId('infrastructure-view')).toContainText('离线测试账户')

  await addNode(page, { kind: 'service', name: '离线新增服务' })
  await expect(page.getByTestId('infrastructure-view')).toContainText('离线新增服务')

  await context.setOffline(false)
})

test('E2E-15b 离线状态下锁定/解锁保持数据一致', async ({ page, context }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'membership', name: '离线会员' })
  await page.waitForFunction(() => navigator.serviceWorker?.controller !== null, undefined, {
    timeout: 30_000,
  })
  await page.reload()
  await expect(page.getByLabel('本地密码')).toBeVisible({ timeout: 30_000 })
  await unlock(page)

  await context.setOffline(true)
  await lockFromRail(page)
  await unlock(page)
  await openNav(page, '基础设施')
  await expect(page.getByTestId('infrastructure-view')).toContainText('离线会员')
  await context.setOffline(false)
})
