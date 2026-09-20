package com.emohappy.pulse.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emohappy.pulse.model.ProcessedTransaction
import com.emohappy.pulse.model.TimelineItem
import com.emohappy.pulse.ui.theme.*

@Composable
fun RecentTransactionsCard(
    timeline: List<TimelineItem>,
    onViewAll: () -> Unit,
    onEdit: (ProcessedTransaction) -> Unit,
    onDelete: (String) -> Unit,
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ 最近动态",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.clickable { onViewAll() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "查看完整明细 ›",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HsbcRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (timeline.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无消费记录，立即在上方记一笔吧",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                val previewList = timeline.take(3)
                previewList.forEach { item ->
                    TimelineItemCard(
                        item = item,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }

                if (timeline.size > 3) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = HsbcRed.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewAll() }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "查看更多（还有 ${timeline.size - 3} 条记录） ›",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HsbcRed
                            )
                        }
                    }
                }
            }
        }
    }
}
