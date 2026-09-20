package com.emohappy.pulse.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emohappy.pulse.model.DashboardSummary
import com.emohappy.pulse.model.UserSettings
import com.emohappy.pulse.ui.theme.*
import kotlin.math.max

@Composable
fun ProgressSection(
    summary: DashboardSummary,
    settings: UserSettings,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenGuruDetail: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 达标与额度池跟踪",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "上个月",
                            tint = TextMuted
                        )
                    }
                    Text(
                        text = summary.monthStr.ifEmpty { "本月统计" },
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "下个月",
                            tint = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. 迎新礼
            if (settings.welcome) {
                val welProgress = if (summary.welcomeThreshold > 0) {
                    (summary.welcomeSpend / summary.welcomeThreshold).toFloat().coerceIn(0f, 1f)
                } else 0f
                val welReward = settings.totalWelcomeReward.toInt()

                ProgressItem(
                    title = "🎁 迎新礼 (满 ¥${summary.welcomeThreshold.toInt()} 返 $welReward RC)",
                    valueText = "¥${summary.welcomeSpend.toInt()} / ¥${summary.welcomeThreshold.toInt()}",
                    progress = welProgress,
                    progressColor = if (summary.welcomeAchieved) SuccessGreen else PurpleWelcome,
                    hint = if (summary.welcomeAchieved) {
                        "🎉 迎新已达标！已单独入账一条 +$welReward RC 迎新大礼包！"
                    } else {
                        val needed = max(0.0, summary.welcomeThreshold - summary.welcomeSpend)
                        "⏳ 迎新期内已刷 ¥${summary.welcomeSpend.toInt()}，还差 ¥${needed.toInt()} 解锁 $welReward RC 礼包"
                    },
                    hintColor = if (summary.welcomeAchieved) SuccessGreen else PurpleWelcome
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2. 最红中国内地签账奖赏 (RH CN Spend - 1~6月季度全类别 / 7~12月月度餐饮加码)
            if (settings.rhCnSpend || settings.chinaDining) {
                val isH2 = (summary.monthStr.length >= 7 && (summary.monthStr.substring(5, 7).toIntOrNull() ?: 1) > 6)
                if (!isH2) {
                    val qStatus = summary.rhCnStatus
                    val isUnlocked = qStatus.isUnlocked
                    val cap = qStatus.capRC.toInt()
                    val threshold = qStatus.thresholdSpend.toInt()
                    val rate = settings.rhCnH1Config.ratePercent.toInt()
                    val isRegistered = qStatus.isRegistered

                    val valueText = if (!isUnlocked) {
                        "本季已刷 ¥${qStatus.currentSpend.toInt()} / 门槛 ¥$threshold"
                    } else {
                        "${String.format("%.1f", qStatus.earnedRC)} / $cap RC (已达标)"
                    }

                    val hint = if (!isRegistered) {
                        "⚠️ 未达登记日或未在Reward+登记，请确认完成登记以激活本期加赠！"
                    } else if (!isUnlocked) {
                        "🔒 本季有效签账已刷 ¥${qStatus.currentSpend.toInt()}，还差 ¥${qStatus.remainingSpendToUnlock.toInt()} 激活额外 ${rate}% (封顶 $cap RC)！"
                    } else {
                        if (qStatus.earnedRC >= qStatus.capRC) {
                            "✅ 季度已达标！本季 $cap RC 奖励已全额封顶 (${qStatus.payoutDesc})"
                        } else {
                            "✅ 季度已达标！已获 +${String.format("%.1f", qStatus.earnedRC)} RC (还可刷 ¥${qStatus.remainingSpendToCap.toInt()} 拿满 $cap RC · ${qStatus.payoutDesc})"
                        }
                    }

                    ProgressItem(
                        title = "🇨🇳 最红内地 (${qStatus.quarterName})",
                        valueText = valueText,
                        progress = qStatus.progress,
                        progressColor = if (isUnlocked) SuccessGreen else WarningOrange,
                        hint = hint,
                        hintColor = if (isUnlocked) SuccessGreen else WarningOrange
                    )
                } else {
                    // 下半年 (7~12月)：规则调整为月度餐饮额外 3%
                    val isUnlocked = summary.isMonthDiningUnlocked
                    val dRate = settings.diningRate.toInt()
                    val cap = settings.diningMonthlyCap.toInt()
                    val minSpend = settings.diningMinSpend.toInt()

                    val progress = if (!isUnlocked) {
                        if (minSpend > 0) (summary.monthTotalSpend / minSpend).toFloat().coerceIn(0f, 1f) else 0f
                    } else {
                        if (cap > 0) (summary.monthDiningRC / cap).toFloat().coerceIn(0f, 1f) else 0f
                    }

                    val valueText = if (!isUnlocked) {
                        "当月内地总签账 ¥${summary.monthTotalSpend.toInt()} / 门槛 ¥$minSpend"
                    } else {
                        "${String.format("%.1f", summary.monthDiningRC)} / $cap RC (已达标)"
                    }

                    val hint = if (!isUnlocked) {
                        val needed = max(0.0, summary.diningMinSpend - summary.monthTotalSpend)
                        "🔒 当月内地有效消费已刷 ¥${summary.monthTotalSpend.toInt()}，还差 ¥${needed.toInt()} 激活餐饮额外 ${dRate}% 回赠！"
                    } else {
                        if (summary.monthDiningRC >= summary.diningMonthlyCap) {
                            "✅ 已达标！本月 $cap RC 餐饮加赠已全额封顶！"
                        } else {
                            val capSpend = summary.diningMonthlyCap / (settings.diningRate / 100.0)
                            val neededSpend = max(0.0, capSpend - summary.monthDiningSpend)
                            "✅ 已达标！已到手餐饮加赠 ${String.format("%.1f", summary.monthDiningRC)} RC (还差餐饮 ¥${neededSpend.toInt()} 拿满 $cap RC)"
                        }
                    }

                    ProgressItem(
                        title = "🇨🇳 最红内地餐饮 (${summary.monthStr} · ${dRate}% / 月封顶 $cap RC)",
                        valueText = valueText,
                        progress = progress,
                        progressColor = if (isUnlocked) SuccessGreen else WarningOrange,
                        hint = hint,
                        hintColor = if (isUnlocked) SuccessGreen else WarningOrange
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. Pulse 2% 特别奖赏 (含年中重置标识)
            val pulseProgress = (summary.pulseUsedRC / summary.pulseCapRC).toFloat().coerceIn(0f, 1f)
            val pulseTitle = if (summary.pulseResetMidYear) {
                val halfName = if (summary.rhCnStatus.halfYearIndex == 1) "上半年" else "下半年 · 年中重置"
                "⚡ Pulse 2% 特别奖赏 ($halfName 封顶 1,600 RC)"
            } else {
                "⚡ Pulse 2% 特别奖赏 (年上限 1,600 RC)"
            }
            ProgressItem(
                title = pulseTitle,
                valueText = "${String.format("%.1f", summary.pulseUsedRC)} / 1,600 RC (${(pulseProgress * 100).toInt()}%)",
                progress = pulseProgress,
                progressColor = HsbcRed
            )

            // 5. 最红自主 2%
            if (settings.redReward) {
                Spacer(modifier = Modifier.height(12.dp))
                val redProgress = (summary.redUsedRC / summary.redCapRC).toFloat().coerceIn(0f, 1f)
                ProgressItem(
                    title = "🌍 最红自主 2% (年上限 2,000 RC)",
                    valueText = "${String.format("%.1f", summary.redUsedRC)} / 2,000 RC (${(redProgress * 100).toInt()}%)",
                    progress = redProgress,
                    progressColor = BlueSky
                )
            }

            // 6. Travel Guru 旅人狂赏
            Spacer(modifier = Modifier.height(14.dp))
            val activeTier = summary.guruState.activeTier
            if (summary.guruState.hasEnabledGuru && activeTier != null) {
                val rateStr = if (activeTier.ratePercent % 1.0 == 0.0) "${activeTier.ratePercent.toInt()}%" else "${activeTier.ratePercent}%"
                val earnedStr = String.format("%.1f", activeTier.earnedRC)
                val capStr = activeTier.capRC.toInt()
                val pct = (activeTier.progress * 100).toInt()

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenGuruDetail() }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✈️", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Travel Guru 旅人狂赏",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(20.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFF59E0B))
                                ) {
                                    Text(
                                        text = "${activeTier.cycleName} · ${activeTier.levelName} (${rateStr}) ›",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = earnedStr,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    val histText = if (summary.guruState.totalHistoricalGuruRC > activeTier.earnedRC) {
                                        " / $capStr RC (历程总赚 ${summary.guruState.totalHistoricalGuruRC.toInt()} RC)"
                                    } else {
                                        " / $capStr RC"
                                    }
                                    Text(
                                        text = histText,
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                                Text(
                                    text = "已拿 $pct%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { activeTier.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                color = Color(0xFFD97706),
                                trackColor = Color(0xFFFDE68A)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "剩余额度: ${String.format("%.1f", activeTier.remainingRC)} RC",
                                    fontSize = 10.sp,
                                    color = Color(0xFF0284C7),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (activeTier.remainingRC <= 0) "🎉 本期已封顶！" else "还可刷 ¥${activeTier.remainingSpendNeeded.toInt()} 封顶",
                                    fontSize = 10.sp,
                                    color = if (activeTier.remainingRC <= 0) SuccessGreen else HsbcRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            val upTask = summary.guruState.upgradeTask
                            val upHint = when {
                                upTask.isMaxLevel -> "👑 已达成最高荣誉 GURU 级旅人！独享 6% 最高外币回赠"
                                upTask.isReadyToUpgrade -> "🎉 升级已达成！将于 ${upTask.effectiveUpgradeMonth} 晋升至 ${upTask.targetLevelName}"
                                else -> {
                                    val targetShort = if (upTask.targetLevel == 2) "GING" else "GURU"
                                    val spendTargetWan = (upTask.task1TargetSpend / 10000).toInt()
                                    "升至 ${targetShort}: 外币 ¥${upTask.task1CurrentSpend.toInt()}/${spendTargetWan}万 · 旅行预订 ${upTask.task2CurrentCount}/${upTask.task2TargetCount} 次"
                                }
                            }
                            Text(
                                text = upHint,
                                fontSize = 9.sp,
                                color = if (upTask.isReadyToUpgrade || upTask.isMaxLevel) SuccessGreen else Color(0xFFB45309),
                                fontWeight = FontWeight.Medium
                            )

                            if (activeTier.endDate.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "⏳ 至 ${activeTier.endDate} (剩余 ${activeTier.daysRemaining} 天)",
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "查看完整天梯与流水 ➔",
                                        fontSize = 9.sp,
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenGuruDetail() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✈️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Travel Guru 旅人计划",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "尚未激活 · 点击查看天梯路线与设置",
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
                                text = "去配置 ›",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressItem(
    title: String,
    valueText: String,
    progress: Float,
    progressColor: Color,
    hint: String? = null,
    hintColor: Color = TextMuted
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = valueText,
                fontSize = 11.sp,
                color = TextMuted,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(10.dp)),
            color = progressColor,
            trackColor = BorderLight
        )

        if (hint != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = hintColor
            )
        }
    }
}
