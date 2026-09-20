package com.emohappy.pulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emohappy.pulse.model.DashboardSummary
import com.emohappy.pulse.model.GuruTierStatus
import com.emohappy.pulse.model.ProcessedTransaction
import com.emohappy.pulse.model.UserSettings
import com.emohappy.pulse.ui.theme.*
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuruDetailSheet(
    summary: DashboardSummary,
    settings: UserSettings,
    transactions: List<ProcessedTransaction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val guruState = summary.guruState
    val activeTier = guruState.activeTier

    var selectedCycle by remember(guruState.currentCycleName) { mutableStateOf(guruState.currentCycleName) }
    val displayedTiers = if (guruState.allCycles.contains(selectedCycle)) {
        guruState.allTiers.filter { it.cycleName == selectedCycle }
    } else {
        guruState.activeCycleTiers.ifEmpty { guruState.allTiers }
    }

    // 过滤出享受了 Guru 奖励的交易记录
    val guruTxs = transactions.filter { it.breakdownDetail.rcGuru > 0 }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BgLight,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
        ) {
            // 顶栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✈️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Travel Guru 旅人权益与额度中心",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = TextMuted
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 0. 会籍周期切换与历史累计总览
                item(key = "cycle_switcher") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎯 会籍周期与历史",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "历史全部累计 ${String.format("%.1f", guruState.totalHistoricalGuruRC)} RC",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HsbcRed
                                )
                            }

                            if (guruState.allCycles.size > 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    guruState.allCycles.forEach { cycle ->
                                        val isSelected = cycle == selectedCycle
                                        val isCurActive = cycle == guruState.currentCycleName
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedCycle = cycle },
                                            label = {
                                                Text(
                                                    text = if (isCurActive) "$cycle (当前生效)" else cycle,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = HsbcRed.copy(alpha = 0.1f),
                                                selectedLabelColor = HsbcRed
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. 晋级天梯路线卡片 (按选中的周期动态展示 Lv1~Lv3 状态)
                item(key = "roadmap") {
                    val cycleLevels = (1..3).map { lvl ->
                        displayedTiers.find { it.level == lvl } ?: GuruTierStatus(
                            cycleName = selectedCycle,
                            level = lvl,
                            levelName = when (lvl) { 1 -> "Lv.1 GO 旅人"; 2 -> "Lv.2 GING 旅人"; else -> "Lv.3 GURU 旅人" },
                            enabled = false,
                            capRC = when (lvl) { 1 -> 500.0; 2 -> 1200.0; else -> 2200.0 },
                            ratePercent = when (lvl) { 1 -> 3.0; 2 -> 4.0; else -> 6.0 }
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏆 $selectedCycle · 晋级路线",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                val cycleTotalEarned = displayedTiers.sumOf { it.earnedRC }
                                Text(
                                    text = "本轮已赚 ${String.format("%.1f", cycleTotalEarned)} RC",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                cycleLevels.forEachIndexed { index, tier ->
                                    val isCurrent = tier.isCurrentActive || (activeTier?.level == tier.level && tier.cycleName == guruState.currentCycleName)
                                    val isDone = tier.isCompleted || (tier.endDate.isNotEmpty() && tier.daysRemaining <= 0 && tier.earnedRC > 0)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isCurrent -> HsbcRed
                                                        isDone -> SuccessGreen.copy(alpha = 0.15f)
                                                        else -> BorderLight
                                                    }
                                                )
                                                .border(
                                                    width = if (isCurrent) 2.dp else 1.dp,
                                                    color = when {
                                                        isCurrent -> HsbcRed
                                                        isDone -> SuccessGreen
                                                        else -> TextMuted
                                                    },
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when {
                                                isDone -> Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "已达成",
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                isCurrent -> Text(
                                                    text = "${tier.level}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp
                                                )
                                                else -> Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "待开启",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = tier.levelName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) HsbcRed else TextPrimary
                                        )
                                        Text(
                                            text = "${tier.earnedRC.toInt()} / ${tier.capRC.toInt()} RC",
                                            fontSize = 9.sp,
                                            color = TextMuted
                                        )
                                    }

                                    if (index < cycleLevels.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .height(2.dp)
                                                .padding(bottom = 18.dp)
                                                .background(BorderLight)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 1.1 升级成功庆祝礼盒横幅 (若双任务达成)
                val upgradeTask = guruState.upgradeTask
                if (upgradeTask.isReadyToUpgrade) {
                    item(key = "upgrade_congrats") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("🎁", fontSize = 32.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "升级啦！",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "恭喜您！你已完成有关任务并将于 ${upgradeTask.effectiveUpgradeMonth} 升级至 ${upgradeTask.targetLevelName}！",
                                        fontSize = 12.sp,
                                        color = Color(0xFF4B5563),
                                        lineHeight = 17.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "查看解锁奖赏 ➔",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HsbcRed
                                    )
                                }
                            }
                        }
                    }
                }

                // 1.2 升级任务卡片 (1:1 还原汇丰官方 App 样式，支持 Lv.2 GING 与 Lv.3 GURU 多级目标)
                if (upgradeTask.isMaxLevel) {
                    item(key = "max_level_card") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("👑", fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "最高荣誉 · GURU 级旅人",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFB45309)
                                            )
                                            Text(
                                                text = "会籍生效中 · 尊享最高阶回赠与礼遇",
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(20.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFF59E0B))
                                    ) {
                                        Text(
                                            text = "已登顶",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFFFFFBEB),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "✨ GURU 级旅人特权：",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "• 外币签账额外 6%「奖赏钱」顶格回赠 (阶段上限 2,200 RC)\n• 尊享全球机场贵宾室礼遇与专属旅行兑换特惠",
                                            fontSize = 10.sp,
                                            color = Color(0xFF92400E),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item(key = "upgrade_tasks") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "升级至 ${upgradeTask.targetLevelName}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = buildString {
                                                if (upgradeTask.deadlineDate.isNotEmpty()) append("${upgradeTask.deadlineDate} 前完成以下任务") else append("会籍期内完成以下任务")
                                                if (upgradeTask.cycleStartDate.isNotEmpty()) append(" · ${upgradeTask.currentCycleName}起点 ${upgradeTask.cycleStartDate}")
                                            },
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                    Text("📷", fontSize = 22.sp)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 任务一：外币累计 (3万 或 7万)
                                val targetWan = (upgradeTask.task1TargetSpend / 10000).toInt()
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = BgLight),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "任务一：累积相等于港币 ${String.format("%,d", upgradeTask.task1TargetSpend.toInt())} 元外币签账",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            if (upgradeTask.task1IsCompleted) {
                                                Text(
                                                    text = "✓ 已达成",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SuccessGreen
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        LinearProgressIndicator(
                                            progress = { upgradeTask.task1Progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            color = if (upgradeTask.task1IsCompleted) SuccessGreen else Color(0xFF2563EB),
                                            trackColor = BorderLight
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "HKD 0", fontSize = 9.sp, color = TextMuted)
                                            Text(text = "HKD ${String.format("%,d", upgradeTask.task1TargetSpend.toInt())}.00", fontSize = 9.sp, color = TextMuted)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (upgradeTask.task1IsCompleted) {
                                                "合资格签账: HKD ${String.format("%,d", upgradeTask.task1TargetSpend.toInt())}.00+ (已达标)"
                                            } else {
                                                val needed = max(0.0, upgradeTask.task1TargetSpend - upgradeTask.task1CurrentSpend)
                                                "合资格签账: HKD ${String.format("%.1f", upgradeTask.task1CurrentSpend)} (还差 HKD ${String.format("%.1f", needed)})"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (upgradeTask.task1IsCompleted) SuccessGreen else TextPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // 任务二：预订机票/邮轮/酒店达指定笔数 (3 笔或 6 笔，单笔 >= 800)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = BgLight),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "任务二：预订机票/邮轮/酒店达 ${upgradeTask.task2TargetCount} 次或以上",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "需单笔满 ¥800",
                                                fontSize = 9.sp,
                                                color = TextMuted
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // 行李箱网格 (每行最多 3 个)
                                        val totalBookings = upgradeTask.task2TargetCount
                                        val rows = (1..totalBookings).chunked(3)
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rows.forEach { rowItems ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceAround
                                                ) {
                                                    rowItems.forEach { i ->
                                                        val isChecked = upgradeTask.task2CurrentCount >= i
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(44.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isChecked) Color(0xFFF0FDF4) else Color.White)
                                                                    .border(
                                                                        width = 1.5.dp,
                                                                        color = if (isChecked) SuccessGreen else BorderLight,
                                                                        shape = CircleShape
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text("🧳", fontSize = 17.sp)
                                                                if (isChecked) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(15.dp)
                                                                            .align(Alignment.BottomEnd)
                                                                            .clip(CircleShape)
                                                                            .background(SuccessGreen),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Check,
                                                                            contentDescription = "达成",
                                                                            tint = Color.White,
                                                                            modifier = Modifier.size(9.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(3.dp))
                                                            Text(
                                                                text = "第 $i 笔",
                                                                fontSize = 9.sp,
                                                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isChecked) SuccessGreen else TextMuted
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (upgradeTask.task2IsCompleted) {
                                                "已达成 ${totalBookings} / ${totalBookings} 笔合资格旅游签账 (已达标)"
                                            } else {
                                                "已达成 ${upgradeTask.task2CurrentCount} / ${totalBookings} 笔 (还差 ${totalBookings - upgradeTask.task2CurrentCount} 笔 ¥800+ 旅游消费)"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (upgradeTask.task2IsCompleted) SuccessGreen else Color(0xFFD97706),
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 奖励预览
                                Surface(
                                    color = Color(0xFFFFFBEB),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "🎁 ${upgradeTask.targetLevelName} 专属升级奖赏：",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val rewardDetails = if (upgradeTask.targetLevel == 2) {
                                            "• 外币签账额外 4%「奖赏钱」回赠 (上限 1,200 RC)\n• Klook 港币 200 元电子礼券 / 香港航空优惠码"
                                        } else {
                                            "• 外币签账额外 6%「奖赏钱」顶格回赠 (上限 2,200 RC)\n• 免费全球机场贵宾室礼遇 / 豪华酒店升级特权"
                                        }
                                        Text(
                                            text = rewardDetails,
                                            fontSize = 9.sp,
                                            color = Color(0xFF92400E),
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. 当前生效等级作战室看板
                if (activeTier != null) {
                    item(key = "war_room") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFFFEF3C7).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                                    ) {
                                        Text(
                                            text = "👑 当前生效中 · ${activeTier.levelName}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFCD34D),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (activeTier.endDate.isNotEmpty()) {
                                        Text(
                                            text = "还剩 ${activeTier.daysRemaining} 天",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }

                                if (activeTier.startDate.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val endText = if (activeTier.endDate.isNotEmpty()) activeTier.endDate else "无固定结束"
                                    Text(
                                        text = "有效期：${activeTier.startDate} 至 $endText",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 数据对比格
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "本阶段已赚 RC", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = String.format("%.1f", activeTier.earnedRC),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(text = "上限 ${activeTier.capRC.toInt()} RC", fontSize = 9.sp, color = Color(0xFF64748B))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(36.dp)
                                            .background(Color(0xFF334155))
                                    )

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "专属额外加赠", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val rStr = if (activeTier.ratePercent % 1.0 == 0.0) "${activeTier.ratePercent.toInt()}%" else "${activeTier.ratePercent}%"
                                        Text(
                                            text = "+$rStr",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF38BDF8)
                                        )
                                        Text(text = "外币/指定场景", fontSize = 9.sp, color = Color(0xFF64748B))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "奖励上限消耗进度",
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "${(activeTier.progress * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { activeTier.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    color = Color(0xFFF59E0B),
                                    trackColor = Color(0xFF334155)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // 智能消费建议反算卡
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "💡 智能消费与防反撸反算：",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFBAE6FD)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        if (activeTier.remainingRC <= 0.0) {
                                            Text(
                                                text = "🎉 本等级奖励已全额封顶！请切换至其他更优返现卡，避免浪费额度。",
                                                fontSize = 11.sp,
                                                color = Color(0xFF86EFAC),
                                                lineHeight = 16.sp
                                            )
                                        } else {
                                            Text(
                                                text = "本阶段还差 ${String.format("%.1f", activeTier.remainingRC)} RC 达封顶，建议继续消费 ¥${activeTier.remainingSpendNeeded.toInt()} 即可拿满本等级全部加赠！",
                                                fontSize = 11.sp,
                                                color = Color(0xFFE0F2FE),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. 各等级全周期一览列表
                item(key = "all_tiers_list") {
                    val groupedByCycle = guruState.allTiers.groupBy { it.cycleName }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📋 各周期与等级额度一览",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "独立封顶统计",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            groupedByCycle.forEach { (cycleName, cycleTiers) ->
                                val cycleTotalEarned = cycleTiers.sumOf { it.earnedRC }
                                Surface(
                                    color = BgLight,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cycleName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HsbcRed
                                        )
                                        Text(
                                            text = "该轮累计 ${String.format("%.1f", cycleTotalEarned)} RC",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextMuted
                                        )
                                    }
                                }

                                cycleTiers.forEach { tier ->
                                    val rStr = if (tier.ratePercent % 1.0 == 0.0) "${tier.ratePercent.toInt()}%" else "${tier.ratePercent}%"
                                    val isCur = tier.isCurrentActive
                                    val isDone = tier.isCompleted

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${tier.levelName} (${rStr})",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCur) HsbcRed else TextPrimary
                                                )
                                                if (isCur) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = Color(0xFFFEF3C7),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "生效中",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFB45309),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                if (tier.isDowngradeReset) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        color = Color(0xFFEFF6FF),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "降级起点",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF1D4ED8),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            val dateRange = when {
                                                tier.startDate.isNotEmpty() && tier.endDate.isNotEmpty() -> "${tier.startDate} ~ ${tier.endDate}"
                                                tier.startDate.isNotEmpty() -> "从 ${tier.startDate} 起生效"
                                                else -> "未设置日期"
                                            }
                                            Text(text = dateRange, fontSize = 10.sp, color = TextMuted)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${String.format("%.1f", tier.earnedRC)} / ${tier.capRC.toInt()} RC",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = when {
                                                    isDone -> "🎉 已封顶"
                                                    tier.remainingRC <= 0 -> "已满额"
                                                    isCur -> "还剩 ${String.format("%.1f", tier.remainingRC)} RC"
                                                    else -> "待开启"
                                                },
                                                fontSize = 10.sp,
                                                color = when {
                                                    isDone -> SuccessGreen
                                                    isCur -> Color(0xFF0284C7)
                                                    else -> TextMuted
                                                },
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                // 4. Guru 加赠交易记录列表
                item(key = "guru_txs_header") {
                    Text(
                        text = "🧾 Guru 加赠交易流水 (${guruTxs.size} 笔)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (guruTxs.isEmpty()) {
                    item(key = "empty_txs") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无命中 Travel Guru 额外加赠的消费记录",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    items(guruTxs, key = { it.itemId }) { tx ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.entity.note.ifEmpty { tx.category.displayName },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${tx.itemDateTime} · ${tx.channel.displayName}",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "¥${String.format("%.2f", tx.entity.amount)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Guru +${String.format("%.2f", tx.breakdownDetail.rcGuru)} RC",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "bottom_space") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
