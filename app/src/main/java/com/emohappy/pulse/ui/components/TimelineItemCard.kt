package com.emohappy.pulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emohappy.pulse.model.ProcessedTransaction
import com.emohappy.pulse.model.SystemAward
import com.emohappy.pulse.model.TimelineItem
import com.emohappy.pulse.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TimelineItemCard(
    item: TimelineItem,
    onEdit: (ProcessedTransaction) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (item) {
        is ProcessedTransaction -> RegularTransactionItem(
            tx = item,
            onEdit = { onEdit(item) },
            onDelete = { onDelete(item.entity.id) },
            modifier = modifier
        )
        is SystemAward -> WelcomeAwardItem(
            award = item,
            modifier = modifier
        )
    }
}

@Composable
private fun RegularTransactionItem(
    tx: ProcessedTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPending = tx.pendingRC > 0
    val timeFormatted = formatTime(tx.entity.dateTime)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${tx.category.displayName} · ¥${String.format("%.2f", tx.entity.amount)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (isPending) {
                        Surface(
                            color = PendingBg,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PendingBorder)
                        ) {
                            Text(
                                text = "待当月满1200",
                                color = PendingText,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = tx.channel.displayName,
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = tx.breakdown.joinToString(" · "),
                    fontSize = 10.sp,
                    color = Color(0xFFEA580C)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right RC & Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${String.format("%.2f", tx.unlockedRC)} RC",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HsbcRed
                    )
                    val rateText = if (isPending) {
                        "(+${String.format("%.2f", tx.pendingRC)}待激活)"
                    } else {
                        val pct = if (tx.entity.amount > 0) (tx.unlockedRC / tx.entity.amount) * 100 else 0.0
                        "${String.format("%.1f", pct)}% 到手"
                    }
                    Text(
                        text = rateText,
                        fontSize = 11.sp,
                        color = if (isPending) WarningOrange else TextMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "修改",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "删除",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = Color(0xFFF1F5F9),
            thickness = 1.dp,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun WelcomeAwardItem(
    award: SystemAward,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PurpleWelcomeBg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PurpleWelcomeBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
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
                    text = award.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9333EA)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFFF3E8FF),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "系统自动发放",
                            color = Color(0xFF7E22CE),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = formatTime(award.dateTime),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = award.detail,
                    fontSize = 10.sp,
                    color = Color(0xFFA855F7)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${String.format("%.2f", award.totalRC)} RC",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF9333EA)
                )
                Text(
                    text = "迎新大礼包",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA855F7)
                )
            }
        }
    }
}

private fun formatTime(dateTimeStr: String): String {
    return runCatching {
        val dt = if (dateTimeStr.length >= 10) {
            LocalDate.parse(dateTimeStr.substring(0, 10))
        } else {
            LocalDate.now()
        }
        dt.format(DateTimeFormatter.ofPattern("MM/dd"))
    }.getOrElse { dateTimeStr }
}
