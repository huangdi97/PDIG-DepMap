package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState

/**
 * R23 Establish / Import reference.
 *
 * The current Preview flavor never opens a user file. Production cutover must
 * reuse the hardened FileWorkflowCoordinator + AppContainer import pipeline.
 */
@Composable
internal fun R23ImportReferenceScreen(
    app: VAppState,
    breakpoint: MediaBreakpoint,
) {
    val maxWidth = when (breakpoint) {
        MediaBreakpoint.COMPACT -> 640.dp
        MediaBreakpoint.MEDIUM -> 820.dp
        MediaBreakpoint.EXPANDED -> 940.dp
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (breakpoint == MediaBreakpoint.COMPACT) 13.dp else 22.dp,
                    vertical = if (breakpoint == MediaBreakpoint.COMPACT) 12.dp else 20.dp,
                )
                .testTag("pdig.r23.import-reference"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "建立基础设施",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = if (breakpoint == MediaBreakpoint.COMPACT) 22.sp else 27.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "通过本机导入发现对象和关系建议；确认导入也不会绕过后续 Human Review。",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth().testTag("pdig.r23.import.truth-boundary"),
                color = PdigV2Colors.PrimarySoft.copy(alpha = 0.64f),
                shape = RoundedCornerShape(VRadius.Xl),
                border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.18f)),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("文件留在本机 · 发现不等于依赖", color = PdigV2Colors.PrimaryText,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "正式导入先解析为 Observation，再确认要记录的对象并生成待复核关系。" +
                            "Proposal 在人工确认前不会参与 Impact。",
                        color = PdigV2Colors.TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            ImportReferenceStep(
                number = "1",
                title = "选择数据来源",
                detail = "使用已有 SourceInstance，或为新的账单/导出文件命名来源。",
                state = "正式工作区",
                tint = PdigV2Colors.PrimaryBright,
            )
            ImportReferenceStep(
                number = "2",
                title = "选择文件并在本机解析",
                detail = "文件选择器只授予读取权限；解析结果与原始 Observation 保持在受控工作流内。",
                state = "Preview 不读取文件",
                tint = PdigV2Colors.Warning,
            )
            ImportReferenceStep(
                number = "3",
                title = "确认要记录的对象",
                detail = "确认后可记录 Source / Evidence / Node，并生成待复核 Proposal；不会直接生成 Dependency。",
                state = "之后进入待复核",
                tint = PdigV2Colors.Positive,
            )

            Surface(
                modifier = Modifier.fillMaxWidth().testTag("pdig.r23.import.preview-disabled"),
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Lg),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("当前是隔离预览", color = PdigV2Colors.TextPrimary,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "为了不把设计演示伪装成真实导入，这里不拉起文件选择器、不读取文件、" +
                            "不创建 SourceInstance，也不提交任何 Proposal。",
                        color = PdigV2Colors.TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            ImportReferenceLink(
                title = "没有文件？手工记录",
                modifier = Modifier.fillMaxWidth(),
            ) { app.navigate(VScreen.MANUAL_ADD) }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ImportReferenceLink(
                    title = "数据与来源",
                    modifier = Modifier.weight(1f),
                ) { app.navigate(VScreen.SOURCES) }
                ImportReferenceLink(
                    title = "待复核",
                    modifier = Modifier.weight(1f),
                ) { app.navigate(VScreen.REVIEW) }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ImportReferenceStep(
    number: String,
    title: String,
    detail: String,
    state: String,
    tint: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                color = tint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(VRadius.Md),
            ) {
                Text(
                    number,
                    Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    color = tint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, color = PdigV2Colors.TextPrimary,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(state, color = tint, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(detail, color = PdigV2Colors.TextSecondary, fontSize = 10.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun ImportReferenceLink(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickable(onClick = onClick),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center) {
            Text(title + " →", color = PdigV2Colors.PrimaryText, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold)
        }
    }
}
