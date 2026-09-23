package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.NoticeStrip
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

/** 关于：版本、构建信息与发布声明（不做任何生产宣称）。 */
@Composable
fun AboutScreen(ui: UiState) {
    PdigPage(
        title = "关于",
        subtitle = "PDIG 0.1.0 Developer Preview",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            SectionDivider("项目")
            InfoRow("名称", "个人数字基础设施图谱（PDIG）")
            InfoRow("版本", "0.1.0 Developer Preview")
            InfoRow("一句话", "换卡、换号、注销账户之前，先看清哪些账户和支付路径会被影响。")
            SectionDivider("构建信息")
            InfoRow("界面框架", "Compose Desktop")
            InfoRow("Kotlin", "2.0.0")
            InfoRow("JVM", "JDK 21")
            InfoRow("容器协议", "DEPMAP_CONTAINER_V1")
            InfoRow("逻辑 Schema", "v3")
            SectionDivider("发布说明")
            NoticeStrip(
                "本版本为开发者预览：未使用商业签名证书，Windows 可能提示「未知发布者」（SmartScreen）。" +
                    "本软件不作任何「生产发布」的宣称；影响分析仅供决策参考，请勿替代你真实账户的官方渠道。",
            )
            NoticeStrip("源码仓库见项目 GitHub（README）；欢迎报告问题。数据不上传：本应用无后端、无遥测。")
        }
    }
}