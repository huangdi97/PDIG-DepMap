import { expect, test } from '@playwright/test'

/**
 * 布局审计（客观可判定，不替代人眼验收）：
 * - 交互控件必须完整落在视口内，且点击目标不小于 24×24
 * - 文本容器不得出现被裁剪的溢出（scrollHeight 明显大于 clientHeight 且 overflow:hidden）
 * - 主内容不得被底部导航遮挡
 */
const VIEWPORTS = [
  { width: 1440, height: 1000 },
  { width: 1280, height: 900 },
  { width: 1024, height: 768 },
  { width: 768, height: 1024 },
  { width: 390, height: 844 },
  { width: 360, height: 800 },
] as const

const ROUTES = ['/', '/infrastructure', '/change', '/records', '/me', '/findings'] as const

for (const viewport of VIEWPORTS) {
  test(`layout audit ${viewport.width}x${viewport.height}`, async ({ page }) => {
    await page.setViewportSize({ width: viewport.width, height: viewport.height })
    for (const route of ROUTES) {
      await page.goto(`/?demo=1#${route}`)
      await expect(page.locator('#main')).toBeVisible({ timeout: 60_000 })
      await page.waitForTimeout(600)
      // 滚到页面底部，检查最后一块内容是否被固定底部导航遮挡
      await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
      await page.waitForTimeout(200)

      const report = await page.evaluate(() => {
        const issues: string[] = []
        const vw = window.innerWidth
        const vh = window.innerHeight

        for (const el of Array.from(
          document.querySelectorAll<HTMLElement>('button, a, input, select'),
        )) {
          if (el.classList.contains('skip-link')) continue
          const target =
            el instanceof HTMLInputElement || el instanceof HTMLSelectElement
              ? (el.closest('label') ?? el)
              : el
          const rect = target.getBoundingClientRect()
          if (rect.width === 0 || rect.height === 0) continue
          if (rect.right > vw + 1 || rect.left < -1) {
            issues.push(
              `控件溢出视口: ${el.tagName} "${(el.textContent ?? '').trim().slice(0, 12)}"`,
            )
          }
          if (rect.height < 24 || rect.width < 24) {
            const style = getComputedStyle(el)
            if (style.visibility !== 'hidden' && style.display !== 'none') {
              issues.push(
                `点击目标过小: ${el.tagName} ${Math.round(rect.width)}x${Math.round(rect.height)}`,
              )
            }
          }
          // 首屏之外的控件属于正常滚动内容，不算布局缺陷。
        }

        for (const el of Array.from(
          document.querySelectorAll<HTMLElement>('.panel, .context-card, .fact'),
        )) {
          const style = getComputedStyle(el)
          const clippedY = style.overflowY === 'hidden' && el.scrollHeight > el.clientHeight + 2
          const clippedX = style.overflowX === 'hidden' && el.scrollWidth > el.clientWidth + 2
          if (clippedX) issues.push(`内容被横向裁剪: .${el.className.split(' ')[0]}`)
          if (clippedY && el.clientHeight > 0 && el.scrollHeight > el.clientHeight * 1.5) {
            issues.push(`内容被纵向裁剪: .${el.className.split(' ')[0]}`)
          }
        }

        const bottomNav = document.querySelector<HTMLElement>('.bottom-nav')
        if (bottomNav && getComputedStyle(bottomNav).display !== 'none') {
          const navRect = bottomNav.getBoundingClientRect()
          const blocks = Array.from(
            document.querySelectorAll<HTMLElement>('.panel, .empty, .table'),
          )
          const last = blocks[blocks.length - 1]
          if (last && last.getBoundingClientRect().bottom > navRect.top + 1) {
            issues.push('页面底部内容被固定底部导航遮挡')
          }
        }

        return issues.slice(0, 12)
      })

      expect(report, `route ${route} @ ${viewport.width}x${viewport.height}`).toEqual([])
    }
  })
}
