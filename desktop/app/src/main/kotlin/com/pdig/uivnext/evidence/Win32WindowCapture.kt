package com.pdig.uivnext.evidence

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinGDI
import com.sun.jna.win32.StdCallLibrary
import java.awt.image.BufferedImage

/**
 * PHASE 1F-HF §3 —— Win32 定向窗口捕获（Windows，JDK 自带 + 既有 JNA 依赖；
 * 无新增下载）。用途：
 *   - FindWindowW 按标题绑定 PDIG 窗口 → 真实 HWND；
 *   - SetWindowPos(HWND_TOPMOST) + ShowWindow(SW_RESTORE) + SetForegroundWindow：
 *     把窗口带到可捕获的可见状态（Windows 前台锁下 AWT toFront 会被忽略）；
 *   - GetWindowRect：设备像素窗口矩形（避免 DPI 逻辑/物理错位）；
 *   - PrintWindow(PW_RENDERFULLCONTENT)：即使被遮挡也能拿到窗口自身渲染内容
 *     （GDI 重绘，非桌面截图；调用方仍做暗色主题像素校验后才写证据）。
 */
object Win32WindowCapture {

    private interface User32Ex : StdCallLibrary {
        fun FindWindowW(className: Pointer?, windowName: String?): WinDef.HWND
        fun GetWindowRect(hWnd: WinDef.HWND, rect: WinDef.RECT): Boolean
        fun SetWindowPos(hWnd: WinDef.HWND, after: WinDef.HWND, x: Int, y: Int, cx: Int, cy: Int, flags: Int): Boolean
        fun ShowWindow(hWnd: WinDef.HWND, nCmdShow: Int): Boolean
        fun SetForegroundWindow(hWnd: WinDef.HWND): Boolean
        fun BringWindowToTop(hWnd: WinDef.HWND): Boolean
        fun PrintWindow(hWnd: WinDef.HWND, hdc: WinDef.HDC, flags: Int): Boolean
        fun InvalidateRect(hWnd: WinDef.HWND, rect: Pointer?, erase: Boolean): Boolean
        fun UpdateWindow(hWnd: WinDef.HWND): Boolean
    }

    private val user32: User32Ex = Native.load("user32", User32Ex::class.java)
    private val gdi: GDI32 = GDI32.INSTANCE

    private const val HWND_TOPMOST = -1L
    private const val SWP_NOMOVE = 0x0002
    private const val SWP_NOSIZE = 0x0001
    private const val SWP_SHOWWINDOW = 0x0040
    private const val SW_RESTORE = 9
    private const val PW_RENDERFULLCONTENT = 0x00000002

    /** 强制 WM_PAINT（无效化 + 更新），让当前帧落到 GDI 可捕获表面。 */
    fun invalidateAndUpdate(hwnd: WinDef.HWND): Boolean {
        user32.InvalidateRect(hwnd, null, true)
        user32.UpdateWindow(hwnd)
        return true
    }

    /** 按窗口标题前缀查找 HWND（FindWindowW 精确匹配完整标题，标题来自 AWT 枚举）。 */
    fun findHwndByTitle(prefix: String): WinDef.HWND? {
        val awt = java.awt.Window.getWindows().filterIsInstance<java.awt.Frame>()
            .firstOrNull { it.isShowing && it.title.startsWith(prefix) } ?: return null
        return user32.FindWindowW(null, awt.title) // 找不到时 JNA 返回 null
    }

    /** 把窗口带到最顶层并恢复显示（SetWindowPos TOPMOST 不受前台锁限制）。 */
    fun bringToFront(hwnd: WinDef.HWND): Boolean {
        user32.ShowWindow(hwnd, SW_RESTORE)
        user32.SetWindowPos(hwnd, WinDef.HWND(Pointer.createConstant(HWND_TOPMOST)), 0, 0, 0, 0, SWP_NOMOVE or SWP_NOSIZE or SWP_SHOWWINDOW)
        user32.SetForegroundWindow(hwnd)
        user32.BringWindowToTop(hwnd)
        return true
    }

    /** 设备像素窗口矩形（GetWindowRect）。 */
    fun getWindowRect(hwnd: WinDef.HWND): WinDef.RECT? {
        val r = WinDef.RECT()
        return if (user32.GetWindowRect(hwnd, r)) r else null
    }

    /** PrintWindow 捕获窗口自身内容（即使被遮挡）。失败/空白返回 null。 */
    fun captureViaPrintWindow(hwnd: WinDef.HWND): BufferedImage? {
        val rect = getWindowRect(hwnd) ?: return null
        val w = rect.right - rect.left
        val h = rect.bottom - rect.top
        if (w <= 0 || h <= 0 || w > 8192 || h > 8192) return null
        val memDc = gdi.CreateCompatibleDC(null) ?: return null
        try {
            val bmp = gdi.CreateCompatibleBitmap(memDc, w, h) ?: return null
            val oldBmp = gdi.SelectObject(memDc, bmp)
            if (!user32.PrintWindow(hwnd, memDc, PW_RENDERFULLCONTENT)) {
                gdi.SelectObject(memDc, oldBmp)
                gdi.DeleteObject(bmp)
                return null
            }
            val bmi = WinGDI.BITMAPINFO()
            bmi.bmiHeader.biWidth = w
            bmi.bmiHeader.biHeight = -h
            bmi.bmiHeader.biPlanes = 1
            bmi.bmiHeader.biBitCount = 32
            bmi.bmiHeader.biCompression = WinGDI.BI_RGB
            val bits = ByteArray(w * h * 4)
            val mem = com.sun.jna.Memory(bits.size.toLong())
            gdi.GetDIBits(memDc, bmp, 0, h, mem, bmi, WinGDI.DIB_RGB_COLORS)
            mem.read(0, bits, 0, bits.size)
            gdi.SelectObject(memDc, oldBmp)
            gdi.DeleteObject(bmp)
            return bgraToImage(bits, w, h)
        } finally {
            gdi.DeleteDC(memDc)
        }
    }

    /** BGRA 字节 → ARGB BufferedImage。 */
    private fun bgraToImage(bits: ByteArray, w: Int, h: Int): BufferedImage {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = (y * w + x) * 4
                val b = bits[i].toInt() and 0xFF
                val g = bits[i + 1].toInt() and 0xFF
                val r = bits[i + 2].toInt() and 0xFF
                img.setRGB(x, y, (0xFF shl 24) or (r shl 16) or (g shl 8) or b)
            }
        }
        return img
    }
    /** HWND 数值（用于证据记录）。 */
    fun hwndValue(hwnd: WinDef.HWND): Long = Pointer.nativeValue(hwnd.pointer)
}
