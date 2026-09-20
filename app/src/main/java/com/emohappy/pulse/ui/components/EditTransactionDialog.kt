package com.emohappy.pulse.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.emohappy.pulse.model.ExpenseCategory
import com.emohappy.pulse.model.PaymentChannel
import com.emohappy.pulse.model.TransactionEntity
import com.emohappy.pulse.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var dateTimeText by remember { mutableStateOf(transaction.dateTime) }
    var selectedChannel by remember { mutableStateOf(PaymentChannel.fromCode(transaction.channel)) }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.fromCode(transaction.category)) }

    fun openDateTimePicker() {
        val current = runCatching {
            LocalDate.parse(dateTimeText.substring(0, 10))
        }.getOrElse { LocalDate.now() }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                dateTimeText = selected.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    val displayDateFormatted = runCatching {
        val dt = LocalDate.parse(dateTimeText.substring(0, 10))
        dt.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
    }.getOrElse { dateTimeText }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✏️ 修改消费记录",
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

                Spacer(modifier = Modifier.height(14.dp))

                // 时间选择
                Text(text = "消费时间", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Outlined.DateRange, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            Text(text = displayDateFormatted, fontSize = 13.sp, color = TextPrimary)
                        }
                        Text(text = "修改 >", fontSize = 11.sp, color = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 金额
                Text(text = "消费金额 (CNY / HKD)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    singleLine = true,
                    leadingIcon = { Text("¥", color = HsbcRed, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 通道
                Text(text = "支付通道", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentChannel.entries.take(2).forEach { ch ->
                        SelectChip(
                            text = "${ch.icon} ${ch.displayName}",
                            isSelected = selectedChannel == ch,
                            onClick = { selectedChannel = ch },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentChannel.entries.drop(2).forEach { ch ->
                        SelectChip(
                            text = "${ch.icon} ${ch.displayName}",
                            isSelected = selectedChannel == ch,
                            onClick = { selectedChannel = ch },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 场景
                Text(text = "消费场景", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseCategory.entries.forEach { cat ->
                        SelectChip(
                            text = "${cat.icon} ${cat.displayName}",
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 操作按钮
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: return@Button
                        if (amt > 0.0) {
                            onSave(
                                transaction.copy(
                                    dateTime = dateTimeText,
                                    amount = amt,
                                    channel = selectedChannel.code,
                                    category = selectedCategory.code
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HsbcRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("保存修改", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("取消", fontSize = 14.sp, color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun SelectChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) HsbcPrimaryLight else Color.White,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) HsbcRed else BorderLight,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) HsbcRed else TextMuted
        )
    }
}
