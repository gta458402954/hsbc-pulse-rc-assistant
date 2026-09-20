package com.emohappy.pulse.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emohappy.pulse.model.GuruLevelConfig
import com.emohappy.pulse.model.GuruStagePeriod
import com.emohappy.pulse.model.UserSettings
import com.emohappy.pulse.ui.theme.*
import java.time.LocalDate

@Composable
fun SettingsDialog(
    settings: UserSettings,
    onDismiss: () -> Unit,
    onSave: (UserSettings) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current

    var welcomeEnabled by remember { mutableStateOf(settings.welcome) }
    var cardIssueDate by remember { mutableStateOf(settings.cardIssueDate) }
    var welcomeThresholdText by remember { mutableStateOf(settings.welcomeSpendThreshold.toInt().toString()) }
    var hasReferralCode by remember { mutableStateOf(settings.hasReferralCode) }

    var rhCnSpendEnabled by remember { mutableStateOf(settings.rhCnSpend) }
    var rhCnSpendStartDate by remember { mutableStateOf(settings.rhCnSpendStartDate) }
    var rhCnSpendEndDate by remember { mutableStateOf(settings.rhCnSpendEndDate) }
    var rhCnSpendRateText by remember { mutableStateOf(if (settings.rhCnSpendRate % 1.0 == 0.0) settings.rhCnSpendRate.toInt().toString() else settings.rhCnSpendRate.toString()) }
    var rhCnSpendQuarterCapText by remember { mutableStateOf(settings.rhCnSpendQuarterCap.toInt().toString()) }
    var rhCnSpendThresholdText by remember { mutableStateOf(settings.rhCnSpendThreshold.toInt().toString()) }

    var rhCnH1RegDate by remember { mutableStateOf(settings.rhCnH1Config.regDate) }
    var rhCnH1CapText by remember { mutableStateOf(settings.rhCnH1Config.quarterCapRC.toInt().toString()) }
    var rhCnH1ThresholdText by remember { mutableStateOf(settings.rhCnH1Config.quarterSpendThreshold.toInt().toString()) }

    var rhCnH2RegDate by remember { mutableStateOf(settings.rhCnH2Config.regDate) }
    var rhCnH2CapText by remember { mutableStateOf(settings.rhCnH2Config.quarterCapRC.toInt().toString()) }
    var rhCnH2ThresholdText by remember { mutableStateOf(settings.rhCnH2Config.quarterSpendThreshold.toInt().toString()) }

    var pulseResetMidYear by remember { mutableStateOf(settings.pulseResetMidYear) }

    var chinaDiningEnabled by remember { mutableStateOf(settings.chinaDining) }
    var chinaDiningDate by remember { mutableStateOf(settings.chinaDiningDate) }
    var diningRateText by remember { mutableStateOf(settings.diningRate.toString()) }
    var diningCapText by remember { mutableStateOf(settings.diningMonthlyCap.toInt().toString()) }
    var diningMinSpendText by remember { mutableStateOf(settings.diningMinSpend.toInt().toString()) }

    var redRewardEnabled by remember { mutableStateOf(settings.redReward) }
    var redRegDate by remember { mutableStateOf(settings.redRegDate) }

    var selectedGuruTab by remember { mutableStateOf(1) }
    var guruConfigsState by remember { mutableStateOf(settings.guruConfigs) }
    var guruStagesState by remember { mutableStateOf(settings.guruStages.ifEmpty { UserSettings.DEFAULT_GURU_STAGES }) }

    var showClearConfirm by remember { mutableStateOf(false) }

    fun pickDate(initial: String, onDatePicked: (String) -> Unit) {
        val cur = runCatching { LocalDate.parse(initial) }.getOrElse { LocalDate.now() }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = LocalDate.of(year, month + 1, dayOfMonth)
                onDatePicked(chosen.toString())
            },
            cur.year,
            cur.monthValue - 1,
            cur.dayOfMonth
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ 活动报名与达标门槛配置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "✕",
                        fontSize = 18.sp,
                        color = TextMuted,
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. 迎新礼设置
                    SettingGroup(title = "开卡迎新礼活动", desc = "发卡60天内消费满额单独发放一条奖励") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "参与迎新活动", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = welcomeEnabled,
                                onCheckedChange = { welcomeEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = HsbcRed)
                            )
                        }

                        if (welcomeEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DatePickField(
                                    label = "发卡日期:",
                                    dateStr = cardIssueDate,
                                    onClick = { pickDate(cardIssueDate) { cardIssueDate = it } },
                                    modifier = Modifier.weight(1f)
                                )
                                MiniTextField(
                                    label = "达标门槛 (¥):",
                                    value = welcomeThresholdText,
                                    onValueChange = { welcomeThresholdText = it },
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "填了推荐码 (+1,000 RC)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(text = "开启享 1,800 RC / 关闭享标准 800 RC", fontSize = 10.sp, color = TextMuted)
                                }
                                Switch(
                                    checked = hasReferralCode,
                                    onCheckedChange = { hasReferralCode = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = HsbcRed)
                                )
                            }
                        }
                    }

                    // 2. 最红中国内地签账奖赏 (RH CN Spend - 季度活动，含年中重置)
                    // 2. 最红中国内地签账 (RH CN Spend - 年中规则调整)
                    SettingGroup(title = "最红中国内地签账 (RH CN Spend)", desc = "汇丰每年7月1日年中调整规则，上半年与下半年自动无缝衔接") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "参与最红中国内地签账", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = rhCnSpendEnabled,
                                onCheckedChange = { rhCnSpendEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = HsbcRed)
                            )
                        }

                        if (rhCnSpendEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            // 上半年 H1 卡片
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("📅 上半年 (H1: 1~6月 · 季度全类别消费)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DatePickField(
                                            label = "H1报名生效日:",
                                            dateStr = rhCnH1RegDate,
                                            onClick = { pickDate(rhCnH1RegDate) { rhCnH1RegDate = it } },
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        MiniTextField(
                                            label = "单季门槛(¥):",
                                            value = rhCnH1ThresholdText,
                                            onValueChange = { rhCnH1ThresholdText = it },
                                            keyboardType = KeyboardType.Number,
                                            modifier = Modifier.weight(1f)
                                        )
                                        MiniTextField(
                                            label = "单季封顶(RC):",
                                            value = rhCnH1CapText,
                                            onValueChange = { rhCnH1CapText = it },
                                            keyboardType = KeyboardType.Number,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // 下半年 H2 卡片 (年中规则调整为餐饮加赠)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🍽️ 下半年 (H2: 7~12月 · 餐饮额外加赠)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                        Text("7/1起仅限餐饮", fontSize = 10.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DatePickField(
                                            label = "H2报名生效日:",
                                            dateStr = chinaDiningDate,
                                            onClick = { pickDate(chinaDiningDate) { chinaDiningDate = it } },
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        MiniTextField(
                                            label = "月内地门槛(¥):",
                                            value = diningMinSpendText,
                                            onValueChange = { diningMinSpendText = it },
                                            keyboardType = KeyboardType.Number,
                                            modifier = Modifier.weight(1f)
                                        )
                                        MiniTextField(
                                            label = "月餐饮封顶(RC):",
                                            value = diningCapText,
                                            onValueChange = { diningCapText = it },
                                            keyboardType = KeyboardType.Number,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // Pulse 2% 年中重置联动开关
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("Pulse 2% 手机支付年中重置", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("1~6月上限1600 RC，7~12月重置并享新1600 RC", fontSize = 10.sp, color = TextMuted)
                                }
                                Switch(
                                    checked = pulseResetMidYear,
                                    onCheckedChange = { pulseResetMidYear = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = HsbcRed)
                                )
                            }
                        }
                    }

                    // 4. 最红自主
                    SettingGroup(title = "最红自主奖赏 (赏世界 5X)", desc = "额外 2% RC (含微信/支付宝)") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "开启最红自主 2%", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = redRewardEnabled,
                                onCheckedChange = { redRewardEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = HsbcRed)
                            )
                        }
                        if (redRewardEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            DatePickField(
                                label = "报名生效日期:",
                                dateStr = redRegDate,
                                onClick = { pickDate(redRegDate) { redRegDate = it } },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 5. Travel Guru (支持多周期与降级重刷)
                    SettingGroup(
                        title = "Travel Guru 旅人会籍与周期管理",
                        desc = "支持多周期与降级重刷（刷满3,900 RC后可重置为Lv.1开启新一轮，各周期额度与升级任务独立计算）"
                    ) {
                        val distinctCycles = guruStagesState.map { it.cycleName }.distinct()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "已配置 ${distinctCycles.size} 个周期 · 共 ${guruStagesState.size} 个阶段",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Text(
                                text = "↺ 恢复两轮默认配置",
                                fontSize = 11.sp,
                                color = HsbcRed,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable {
                                        guruStagesState = UserSettings.DEFAULT_GURU_STAGES
                                    }
                                    .padding(4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        guruStagesState.forEachIndexed { index, stage ->
                            var isExpanded by remember { mutableStateOf(index == guruStagesState.lastIndex) }
                            val lvlName = when (stage.level) {
                                1 -> "Lv.1 GO 旅人"
                                2 -> "Lv.2 GING 旅人"
                                3 -> "Lv.3 GURU 旅人"
                                else -> "Lv.${stage.level} 旅人"
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (stage.enabled) BgLight else BgLight.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (stage.isDowngradeReset) Color(0xFFF59E0B) else BorderLight
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { isExpanded = !isExpanded }
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${stage.cycleName} · $lvlName",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (stage.enabled) TextPrimary else TextMuted
                                                )
                                                if (stage.isDowngradeReset) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        color = Color(0xFFFEF3C7),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "🔄 降级重刷起点",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFB45309),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            val dateDesc = when {
                                                stage.startDate.isNotEmpty() && stage.endDate.isNotEmpty() -> "${stage.startDate} ~ ${stage.endDate}"
                                                stage.startDate.isNotEmpty() -> "从 ${stage.startDate} 起生效"
                                                else -> "未设日期"
                                            }
                                            val rStr = if (stage.ratePercent % 1.0 == 0.0) "${stage.ratePercent.toInt()}%" else "${stage.ratePercent}%"
                                            Text(
                                                text = "$dateDesc · $rStr · 封顶 ${stage.capRC.toInt()} RC",
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }

                                        Switch(
                                            checked = stage.enabled,
                                            onCheckedChange = { chk ->
                                                guruStagesState = guruStagesState.mapIndexed { i, s ->
                                                    if (i == index) s.copy(enabled = chk) else s
                                                }
                                            }
                                        )
                                    }

                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = stage.cycleName,
                                            onValueChange = { newName ->
                                                guruStagesState = guruStagesState.mapIndexed { i, s ->
                                                    if (i == index) s.copy(cycleName = newName) else s
                                                }
                                            },
                                            label = { Text("周期名称 (如: 第 1 轮 / 第 2 轮)", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("等级设定:", fontSize = 11.sp, color = TextPrimary)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf(
                                                    1 to ("Lv.1 GO" to (3.0 to 500.0)),
                                                    2 to ("Lv.2 GING" to (4.0 to 1200.0)),
                                                    3 to ("Lv.3 GURU" to (6.0 to 2200.0))
                                                ).forEach { (lvl, pair) ->
                                                    val (label, defaults) = pair
                                                    FilterChip(
                                                        selected = stage.level == lvl,
                                                        onClick = {
                                                            guruStagesState = guruStagesState.mapIndexed { i, s ->
                                                                if (i == index) s.copy(
                                                                    level = lvl,
                                                                    ratePercent = defaults.first,
                                                                    capRC = defaults.second
                                                                ) else s
                                                            }
                                                        },
                                                        label = { Text(label, fontSize = 10.sp) }
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            DatePickField(
                                                label = "生效起始日:",
                                                dateStr = if (stage.startDate.isNotEmpty()) stage.startDate else "点击选择",
                                                onClick = {
                                                    pickDate(if (stage.startDate.isNotEmpty()) stage.startDate else "2026-01-01") { chosen ->
                                                        guruStagesState = guruStagesState.mapIndexed { i, s ->
                                                            if (i == index) s.copy(startDate = chosen) else s
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )

                                            DatePickField(
                                                label = "结束日(选填):",
                                                dateStr = if (stage.endDate.isNotEmpty()) stage.endDate else "无期限",
                                                onClick = {
                                                    pickDate(if (stage.endDate.isNotEmpty()) stage.endDate else "2026-12-31") { chosen ->
                                                        guruStagesState = guruStagesState.mapIndexed { i, s ->
                                                            if (i == index) s.copy(endDate = chosen) else s
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        if (stage.endDate.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    text = "清除结束日（设为无期限）",
                                                    fontSize = 11.sp,
                                                    color = HsbcRed,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier
                                                        .clickable {
                                                            guruStagesState = guruStagesState.mapIndexed { i, s ->
                                                                if (i == index) s.copy(endDate = "") else s
                                                            }
                                                        }
                                                        .padding(top = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = if (stage.ratePercent % 1.0 == 0.0) stage.ratePercent.toInt().toString() else stage.ratePercent.toString(),
                                                onValueChange = { input ->
                                                    val num = input.toDoubleOrNull() ?: stage.ratePercent
                                                    guruStagesState = guruStagesState.mapIndexed { i, s ->
                                                        if (i == index) s.copy(ratePercent = num) else s
                                                    }
                                                },
                                                label = { Text("返现比例(%)", fontSize = 11.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )

                                            OutlinedTextField(
                                                value = stage.capRC.toInt().toString(),
                                                onValueChange = { input ->
                                                    val num = input.toDoubleOrNull() ?: stage.capRC
                                                    guruStagesState = guruStagesState.mapIndexed { i, s ->
                                                        if (i == index) s.copy(capRC = num) else s
                                                    }
                                                },
                                                label = { Text("本阶段封顶(RC)", fontSize = 11.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    guruStagesState = guruStagesState.mapIndexed { i, s ->
                                                        if (i == index) s.copy(isDowngradeReset = !s.isDowngradeReset) else s
                                                    }
                                                }
                                            ) {
                                                Checkbox(
                                                    checked = stage.isDowngradeReset,
                                                    onCheckedChange = { checked ->
                                                        guruStagesState = guruStagesState.mapIndexed { i, s ->
                                                            if (i == index) s.copy(isDowngradeReset = checked) else s
                                                        }
                                                    }
                                                )
                                                Text(
                                                    text = "降级重刷起点 (重置升级任务)",
                                                    fontSize = 11.sp,
                                                    color = TextPrimary
                                                )
                                            }

                                            if (guruStagesState.size > 1) {
                                                Text(
                                                    text = "删除此阶段",
                                                    fontSize = 11.sp,
                                                    color = HsbcRed,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clickable {
                                                            guruStagesState = guruStagesState.filterIndexed { i, _ -> i != index }
                                                        }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val lastStage = guruStagesState.lastOrNull()
                                    val nextLevel = when (lastStage?.level) {
                                        1 -> 2
                                        2 -> 3
                                        else -> 3
                                    }
                                    val defaultRates = mapOf(1 to 3.0, 2 to 4.0, 3 to 6.0)
                                    val defaultCaps = mapOf(1 to 500.0, 2 to 1200.0, 3 to 2200.0)
                                    val curCycleName = lastStage?.cycleName ?: "第 1 轮"
                                    val newStage = GuruStagePeriod(
                                        id = "stage_${System.currentTimeMillis()}",
                                        cycleName = curCycleName,
                                        level = nextLevel,
                                        enabled = true,
                                        startDate = LocalDate.now().toString(),
                                        endDate = "",
                                        ratePercent = defaultRates[nextLevel] ?: 4.0,
                                        capRC = defaultCaps[nextLevel] ?: 1200.0,
                                        isDowngradeReset = false
                                    )
                                    guruStagesState = guruStagesState + newStage
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("➕ 追加升级阶段", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val cycleCount = guruStagesState.map { it.cycleName }.distinct().size
                                    val nextCycleName = "第 ${cycleCount + 1} 轮 (${LocalDate.now().year}~至今)"
                                    val newStage = GuruStagePeriod(
                                        id = "stage_c${cycleCount + 1}_lv1",
                                        cycleName = nextCycleName,
                                        level = 1,
                                        enabled = true,
                                        startDate = LocalDate.now().toString(),
                                        endDate = "",
                                        ratePercent = 3.0,
                                        capRC = 500.0,
                                        isDowngradeReset = true
                                    )
                                    guruStagesState = guruStagesState + newStage
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("🔄 开启新一轮降级", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "HSBC Pulse RC 奖赏助手 · 本地离线安全存储",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 清空所有数据按钮
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🗑️ 清空所有消费数据与进度", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 完成按钮
                Button(
                    onClick = {
                        val newSettings = settings.copy(
                            welcome = welcomeEnabled,
                            cardIssueDate = cardIssueDate,
                            welcomeSpendThreshold = welcomeThresholdText.toDoubleOrNull() ?: 8000.0,
                            hasReferralCode = hasReferralCode,
                            rhCnSpend = rhCnSpendEnabled,
                            rhCnSpendStartDate = rhCnSpendStartDate,
                            rhCnSpendEndDate = rhCnSpendEndDate,
                            rhCnSpendRate = rhCnSpendRateText.toDoubleOrNull() ?: 3.0,
                            rhCnSpendQuarterCap = rhCnSpendQuarterCapText.toDoubleOrNull() ?: 300.0,
                            rhCnSpendThreshold = rhCnSpendThresholdText.toDoubleOrNull() ?: 10000.0,
                            rhCnH1Config = settings.rhCnH1Config.copy(
                                regDate = rhCnH1RegDate,
                                quarterCapRC = rhCnH1CapText.toDoubleOrNull() ?: 300.0,
                                quarterSpendThreshold = rhCnH1ThresholdText.toDoubleOrNull() ?: 10000.0
                            ),
                            rhCnH2Config = settings.rhCnH2Config.copy(
                                regDate = rhCnH2RegDate,
                                quarterCapRC = rhCnH2CapText.toDoubleOrNull() ?: 500.0,
                                quarterSpendThreshold = rhCnH2ThresholdText.toDoubleOrNull() ?: 10000.0
                            ),
                            pulseResetMidYear = pulseResetMidYear,
                            chinaDining = rhCnSpendEnabled,
                            chinaDiningDate = chinaDiningDate,
                            diningRate = 3.0,
                            diningMonthlyCap = diningCapText.toDoubleOrNull() ?: 80.0,
                            diningMinSpend = diningMinSpendText.toDoubleOrNull() ?: 1200.0,
                            redReward = redRewardEnabled,
                            redRegDate = redRegDate,
                            guruLevel = guruStagesState.filter { it.enabled }.maxOfOrNull { it.level } ?: 0,
                            guruConfigs = guruConfigsState,
                            guruStages = guruStagesState
                        )
                        onSave(newSettings)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HsbcRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("完成并保存规则", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("确认清空所有数据？", fontWeight = FontWeight.Bold) },
            text = { Text("此操作将删除全部消费记录并将进度重置，无法撤销。请确保已提前导出备份。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearAll()
                    }
                ) {
                    Text("确定清空", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingGroup(
    title: String,
    desc: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = desc, fontSize = 11.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(8.dp))
        content()
        HorizontalDivider(color = BorderLight, thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun DatePickField(
    label: String,
    dateStr: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 11.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
            color = Color(0xFFFAFAFA),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = dateStr, fontSize = 12.sp, color = TextPrimary)
                Icon(imageVector = Icons.Outlined.DateRange, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun MiniTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 11.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
