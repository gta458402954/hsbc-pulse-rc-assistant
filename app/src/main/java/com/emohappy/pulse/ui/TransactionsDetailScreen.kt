package com.emohappy.pulse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emohappy.pulse.model.CalculationResult
import com.emohappy.pulse.model.ProcessedTransaction
import com.emohappy.pulse.model.SystemAward
import com.emohappy.pulse.ui.components.TimelineItemCard
import com.emohappy.pulse.ui.theme.*

@Composable
fun TransactionsDetailScreen(
    calculationResult: CalculationResult,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onImportBackup: () -> Unit,
    onExportBackup: () -> Unit,
    onEdit: (ProcessedTransaction) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = calculationResult.summary
    val timeline = calculationResult.timeline

    val monthUnlockedRC = remember(timeline) {
        timeline.sumOf { item ->
            when (item) {
                is ProcessedTransaction -> item.unlockedRC
                is SystemAward -> item.unlockedRC
            }
        }
    }
    val monthPendingRC = remember(timeline) {
        timeline.sumOf { item ->
            when (item) {
                is ProcessedTransaction -> item.pendingRC
                is SystemAward -> item.pendingRC
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. 月份切换与备份工具栏
        item(key = "header_controls") {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month selector
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onPreviousMonth,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "上个月",
                                    tint = TextMuted
                                )
                            }
                            Text(
                                text = "📅 ${summary.monthStr}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = onNextMonth,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "下个月",
                                    tint = TextMuted
                                )
                            }
                        }

                        // Backup actions
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "📥 导入",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HsbcRed,
                                modifier = Modifier.clickable { onImportBackup() }
                            )
                            Text(
                                text = "📤 导出",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HsbcRed,
                                modifier = Modifier.clickable { onExportBackup() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Month Stat Summary Box
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "有效消费", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = "¥${String.format("%.2f", summary.monthTotalSpend)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "当月到手", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = "+${String.format("%.2f", monthUnlockedRC)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HsbcRed
                                )
                            }

                            if (monthPendingRC > 0) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "待达标", fontSize = 10.sp, color = WarningOrange)
                                    Text(
                                        text = "+${String.format("%.2f", monthPendingRC)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WarningOrange
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "记录笔数", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = "${timeline.size} 笔",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "section_title") {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 交易列表",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Text(
                    text = "共 ${timeline.size} 项",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // 2. 纯粹高效的虚拟化列表渲染 (解决掉帧核心)
        if (timeline.isEmpty()) {
            item(key = "empty_placeholder") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📭", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "该月份暂无消费记录",
                                color = TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            items(
                items = timeline,
                key = { it.itemId }
            ) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        TimelineItemCard(
                            item = item,
                            onEdit = onEdit,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }

        item(key = "bottom_spacing") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
