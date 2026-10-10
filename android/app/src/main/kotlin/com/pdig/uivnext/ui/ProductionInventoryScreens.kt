package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.production.ProductionVNextSession
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.VNextProductionSnapshot
import com.pdig.uivnext.production.VNextProductionSurfaceKind
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

@Composable
internal fun ProductionInventoryCategoryScreen(
    session: ProductionVNextSession,
    screen: VScreen,
    modifier: Modifier = Modifier,
) {
    val snapshot = session.dataSource.productionSnapshot()
    if (snapshot == null) {
        ProductionObjectUnavailable("生产基础设施快照不可读取", modifier)
        return
    }

    if (screen == VScreen.WEAKNESSES) {
        ProductionWeaknessesHoldScreen(modifier)
        return
    }

    val surfaceKind = productionSurfaceForScreen(screen)
    if (surfaceKind == null) {
        ProductionObjectUnavailable(
            when (screen) {
                VScreen.NUMBERS -> "号码类型尚未完成底层确认，因此暂不把通用身份对象当作手机号"
                VScreen.EMAILS -> "邮箱类型尚未完成底层确认，因此暂不把通用身份对象当作邮箱"
                else -> "该分类尚未完成正式数据映射"
            },
            modifier,
        )
        return
    }

    val objects = snapshot.objects.filter { it.surfaceKind == surfaceKind }
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("pdig.production-vnext.inventory-category"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(screen.titleZh, color = PdigV2Colors.TextPrimary, fontSize = 23.sp,
                    fontWeight = FontWeight.Bold)
                Text(
                    "只显示当前已确认数据中能够明确分类的对象。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        if (objects.isEmpty()) {
            item {
                ProductionObjectBoundary(
                    "当前没有该类型的已确认对象。空列表不代表外部不存在，只表示这里尚未记录。"
                )
            }
        } else {
            items(objects, key = { it.id }) { item ->
                ProductionObjectRow(
                    item = item,
                    dependencyCount = dependencyCount(snapshot, item.id),
                ) {
                    when (screen) {
                        VScreen.CARDS -> session.appState.openCard(item.id)
                        VScreen.ACCOUNTS ->
                            session.appState.openSecondaryObject(VScreen.ACCOUNT_DETAIL, item.id)
                        VScreen.DEVICES ->
                            session.appState.openSecondaryObject(VScreen.DEVICE_DETAIL, item.id)
                        VScreen.SERVICES ->
                            session.appState.openSecondaryObject(VScreen.SERVICE_DETAIL, item.id)
                        else -> Unit
                    }
                }
            }
        }

        item {
            when (screen) {
                VScreen.CARDS -> ProductionObjectBoundary(
                    "年费、账单日、分期等生命周期字段尚未进入正式数据模型；当前只展示已确认的支付工具身份。"
                )
                VScreen.DEVICES -> ProductionObjectBoundary(
                    "设备的认证/恢复能力尚未进入正式数据模型；不会根据设备名称猜测它具备哪些能力。"
                )
                else -> ProductionObjectBoundary(
                    "关系数只是已确认关系数量，不代表独立恢复路径数量，也不产生安全分数。"
                )
            }
        }
    }
}

@Composable
internal fun ProductionGenericObjectDetailScreen(
    session: ProductionVNextSession,
    detailScreen: VScreen,
    objectId: String?,
    modifier: Modifier = Modifier,
) {
    if (objectId.isNullOrBlank()) {
        ProductionObjectUnavailable("没有选择生产对象", modifier)
        return
    }
    val snapshot = session.dataSource.productionSnapshot()
    val item = snapshot?.objects?.firstOrNull { it.id == objectId }
    if (snapshot == null || item == null) {
        ProductionObjectUnavailable("对象不存在、已归档或当前无法读取", modifier)
        return
    }

    val expected = when (detailScreen) {
        VScreen.ACCOUNT_DETAIL -> VNextProductionSurfaceKind.ACCOUNT
        VScreen.DEVICE_DETAIL -> VNextProductionSurfaceKind.DEVICE
        VScreen.SERVICE_DETAIL -> VNextProductionSurfaceKind.SERVICE
        else -> null
    }
    if (expected == null || item.surfaceKind != expected) {
        ProductionObjectUnavailable("对象类型与详情页不匹配；不会强制转换类型", modifier)
        return
    }

    val impact = session.dataSource.productionImpact(item.id)
    val related = snapshot.confirmedDependencies.filter {
        it.fromId == item.id || it.toId == item.id
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("pdig.production-vnext.object-detail"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, color = PdigV2Colors.TextPrimary, fontSize = 23.sp,
                    fontWeight = FontWeight.Bold)
                Text(
                    productionObjectKindLabel(item.kind),
                    color = PdigV2Colors.TextMuted,
                    fontSize = 10.sp,
                )
            }
        }

        item {
            ProductionObjectBoundary(
                "${related.size} 条已确认关系 · 未确认关系仍可能存在"
            )
        }

        if (related.isNotEmpty()) {
            item {
                Text("已确认关系", color = PdigV2Colors.TextPrimary, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
            }
            items(related, key = { it.id }) { dep ->
                val outward = dep.fromId == item.id
                ProductionRelationCard(
                    title = if (outward) dep.toName else dep.fromName,
                    relation = dep.relation,
                    capability = dep.capability,
                    criticality = dep.criticality,
                    direction = if (outward) "从此对象指向" else "指向此对象",
                )
            }
        }

        item {
            Text("如果它发生变化？", color = PdigV2Colors.TextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.Bold)
        }

        if (impact == null) {
            item {
                ProductionObjectBoundary(
                    "当前无法读取权威影响分析；不会用关系数量代替连续性判断。"
                )
            }
        } else {
            val statusCounts = impact.targets.groupingBy { it.status }.eachCount()
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        (statusCounts["must_change"] ?: 0) to "必须处理",
                        (statusCounts["needs_review"] ?: 0) to "需要核对",
                        (statusCounts["unknown"] ?: 0) to "未知",
                    ).forEach { (count, label) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = PdigV2Colors.Surface,
                            shape = RoundedCornerShape(VRadius.Md),
                            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(count.toString(), color = PdigV2Colors.TextPrimary,
                                    fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text(label, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            if (impact.checklist.isNotEmpty()) {
                item {
                    Text("核对清单", color = PdigV2Colors.TextPrimary, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                }
                items(impact.checklist.take(10)) { check ->
                    ProductionObjectBoundary(
                        check.title + if (check.detail.isBlank()) "" else " · " + check.detail
                    )
                }
            }
        }

        item {
            when (detailScreen) {
                VScreen.DEVICE_DETAIL -> ProductionObjectBoundary(
                    "当前设备详情不会猜测通行密钥、动态验证码、短信验证、恢复因子或秘密位置；这些能力必须来自未来正式的数据语义。"
                )
                else -> ProductionObjectBoundary(
                    "详情只使用正式数据中的对象、已确认关系和权威影响分析；不会混入演示数据。"
                )
            }
        }
    }
}

@Composable
private fun ProductionWeaknessesHoldScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdig.production-vnext.weaknesses-hold"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "薄弱点",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "不把“部分可分析”冒充“完整连续性分析”。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            ProductionObjectBoundary(
                "现有 Android 底层已经能从已确认关系分析：单一路径、共享故障点、恢复循环三类问题；但旧实现仍在旧界面层，尚不是新版可直接消费的完整权威投影。"
            )
        }

        item {
            Text(
                "完整薄弱点模型还要求",
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        listOf(
            "未确认的备用路径" to "备用可能存在，但还没有足够证据确认可用。",
            "过期的恢复信息" to "恢复/认证信息需要重新确认，不能默认仍然有效。",
            "关键路径影响未知" to "关键关系存在，但失效后的真实影响还没有确认。",
            "待验证的变更" to "动作已记录完成，但结果还没有验证。",
        ).forEach { (title, body) ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PdigV2Colors.Surface,
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Column(
                        Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            title,
                            color = PdigV2Colors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(body, color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            ProductionObjectBoundary(
                "在七类结果都由可复用的底层连续性分析统一产出之前，新版正式界面不会显示一个缩减版“安全清单”，也不会用关系数量生成健康分。"
            )
        }
    }
}

@Composable
private fun ProductionObjectRow(
    item: VNextProductionObject,
    dependencyCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.name, color = PdigV2Colors.TextPrimary, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold)
                Text(productionObjectKindLabel(item.kind), color = PdigV2Colors.TextMuted, fontSize = 9.sp)
            }
            Text(
                "$dependencyCount 关系",
                color = PdigV2Colors.TextSecondary,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun ProductionRelationCard(
    title: String,
    relation: String,
    capability: String,
    criticality: String,
    direction: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold)
            Text(
                "$direction · ${productionRelationLabel(relation)} · ${productionCapabilityLabel(capability)}",
                color = PdigV2Colors.TextSecondary,
                fontSize = 10.sp,
            )
            Text(
                productionCriticalityLabel(criticality),
                color = PdigV2Colors.TextMuted,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun ProductionObjectBoundary(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.58f),
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Text(text, Modifier.padding(12.dp), color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun ProductionObjectUnavailable(text: String, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("当前生产绑定不可用", color = PdigV2Colors.TextPrimary,
            fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(4.dp))
        Text(text, color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
    }
}

private fun productionSurfaceForScreen(screen: VScreen): VNextProductionSurfaceKind? = when (screen) {
    VScreen.CARDS -> VNextProductionSurfaceKind.PAYMENT_ASSET
    VScreen.ACCOUNTS -> VNextProductionSurfaceKind.ACCOUNT
    VScreen.DEVICES -> VNextProductionSurfaceKind.DEVICE
    VScreen.SERVICES -> VNextProductionSurfaceKind.SERVICE
    else -> null
}

private fun dependencyCount(snapshot: VNextProductionSnapshot, objectId: String): Int =
    snapshot.confirmedDependencies.count { it.fromId == objectId || it.toId == objectId }
