package com.emohappy.pulse.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emohappy.pulse.model.ExpenseCategory
import com.emohappy.pulse.model.PaymentChannel
import com.emohappy.pulse.model.PreviewResult
import com.emohappy.pulse.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun QuickEntryCard(
    amount: String,
    onAmountChange: (String) -> Unit,
    dateTime: String,
    onDateTimeChange: (String) -> Unit,
    selectedChannel: PaymentChannel,
    onChannelSelect: (PaymentChannel) -> Unit,
    selectedCategory: ExpenseCategory,
    onCategorySelect: (ExpenseCategory) -> Unit,
    preview: PreviewResult,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 打开原生日期与时间选择器
    fun openDateTimePicker() {
        val current = runCatching {
            LocalDate.parse(dateTime.substring(0, 10))
        }.getOrElse { LocalDate.now() }

        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                onDateTimeChange(selected.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        )
        datePicker.show()
    }

    val displayDateTimeFormatted = runCatching {
        val dt = LocalDate.parse(dateTime.substring(0, 10))
        dt.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
    }.getOrElse { dateTime }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "✏️ 添加消费记录",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 消费时间选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "消费时间 (支持补录)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                Text(
                    text = "设为现在",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HsbcRed,
                    modifier = Modifier
                        .clickable {
                            onDateTimeChange(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // 可点击的时间选择条
            Surface(
                color = Color(0xFFFAFAFA),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openDateTimePicker() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = "选择日期",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = displayDateTimeFormatted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "修改 >",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 消费金额
            Text(
                text = "消费金额 (CNY / HKD)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                singleLine = true,
                placeholder = {
                    Text("0.00", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMuted.copy(alpha = 0.4f))
                },
                leadingIcon = {
                    Text(
                        text = "¥",
                        color = HsbcRed,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                    )
                },
                trailingIcon = {
                    if (amount.isNotEmpty()) {
                        IconButton(onClick = { onAmountChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "清除", tint = TextMuted)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HsbcRed,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFFAFAFA)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 支付通道
            Text(
                text = "支付通道",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipItem(
                    text = "${PaymentChannel.UNIONPAY_APP.icon} ${PaymentChannel.UNIONPAY_APP.displayName}",
                    isSelected = selectedChannel == PaymentChannel.UNIONPAY_APP,
                    onClick = { onChannelSelect(PaymentChannel.UNIONPAY_APP) },
                    modifier = Modifier.weight(1f)
                )
                ChipItem(
                    text = "${PaymentChannel.APPLE_PAY.icon} Apple Pay",
                    isSelected = selectedChannel == PaymentChannel.APPLE_PAY,
                    onClick = { onChannelSelect(PaymentChannel.APPLE_PAY) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipItem(
                    text = "${PaymentChannel.WECHAT_ALIPAY.icon} 微信 / 支付宝",
                    isSelected = selectedChannel == PaymentChannel.WECHAT_ALIPAY,
                    onClick = { onChannelSelect(PaymentChannel.WECHAT_ALIPAY) },
                    modifier = Modifier.weight(1f)
                )
                ChipItem(
                    text = "${PaymentChannel.PHYSICAL_POS.icon} 实体卡刷卡",
                    isSelected = selectedChannel == PaymentChannel.PHYSICAL_POS,
                    onClick = { onChannelSelect(PaymentChannel.PHYSICAL_POS) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 消费场景
            Text(
                text = "消费场景",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpenseCategory.entries.forEach { cat ->
                    ChipItem(
                        text = "${cat.icon} ${cat.displayName}",
                        isSelected = selectedCategory == cat,
                        onClick = { onCategorySelect(cat) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 实时测算预览 Box (黄橙色预警底色)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
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
                            text = "预计本笔获得奖赏钱",
                            fontSize = 12.sp,
                            color = Color(0xFF9A3412),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preview.breakdownText,
                            fontSize = 11.sp,
                            color = Color(0xFFC2410C)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+${String.format("%.2f", preview.totalRC)} RC",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HsbcRed
                        )
                        val rateLabel = if (preview.pendingRC > 0) {
                            val amtVal = amount.toDoubleOrNull() ?: 1.0
                            val unlockedPct = (preview.totalRC / amtVal) * 100
                            "实得 ${String.format("%.1f", unlockedPct)}% (另+${String.format("%.2f", preview.pendingRC)}待达标)"
                        } else {
                            "返现率 ${String.format("%.1f", preview.rate)}%"
                        }
                        Text(
                            text = rateLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 保存入账按钮
            Button(
                onClick = onSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = HsbcRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "确认保存入账",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ChipItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) HsbcPrimaryLight else Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (isSelected) HsbcRed else BorderLight,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) HsbcRed else TextMuted
        )
    }
}
