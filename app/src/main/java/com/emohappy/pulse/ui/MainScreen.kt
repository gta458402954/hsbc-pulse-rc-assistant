package com.emohappy.pulse.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emohappy.pulse.ui.components.*
import com.emohappy.pulse.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("额度看板", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    ENTRY("快速记账", Icons.Filled.EditNote, Icons.Outlined.EditNote),
    TRANSACTIONS("消费明细", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong)
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 使用 HorizontalPagerState 保持页面视图常驻，实现满帧丝滑切换与左右滑动手势
    val pagerState = rememberPagerState(initialPage = 0) { MainTab.entries.size }

    // Travel Guru 详情抽屉状态
    var showGuruDetailSheet by remember { mutableStateOf(false) }

    // 导出文件 Launcher
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingExportJson != null) {
            writeJsonToUri(context, uri, pendingExportJson!!)
            Toast.makeText(context, "✅ 备份导出成功！", Toast.LENGTH_SHORT).show()
            pendingExportJson = null
        }
    }

    // 导入文件 Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = readJsonFromUri(context, uri)
            if (content != null) {
                viewModel.importJson(content) { result ->
                    result.onSuccess { count ->
                        Toast.makeText(context, "✅ 成功导入 $count 条消费记录！", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(context, "❌ 导入失败：JSON 格式不匹配", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // 方案 B：极简优雅纯粹底部栏，去除了笨拙的胶囊背景块，带微投影与精致小红条指示器
            NavigationBar(
                containerColor = CardBg,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .shadow(elevation = 6.dp, spotColor = Color(0x18000000))
                    .border(width = 0.5.dp, color = BorderLight)
            ) {
                MainTab.entries.forEachIndexed { index, tab ->
                    val isSelected = pagerState.currentPage == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // 精致状态指示小条
                                Box(
                                    modifier = Modifier
                                        .size(width = 10.dp, height = 2.5.dp)
                                        .background(
                                            color = if (isSelected) HsbcRed else Color.Transparent,
                                            shape = RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HsbcRed,
                            selectedTextColor = HsbcRed,
                            indicatorColor = Color.Transparent, // 关键：彻底去除笨重的椭圆胶囊块
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        },
        containerColor = BgLight,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        // 核心性能优化：HorizontalPager 页面常驻，预热两侧页面，0 重组白屏，120Hz 满帧
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 2,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (MainTab.entries[page]) {
                // ================= Tab 1: 纯粹额度看板 (Dashboard) =================
                MainTab.DASHBOARD -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        HeroCard(
                            summary = state.calculationResult.summary,
                            onOpenSettings = { viewModel.openSettings() }
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            item(key = "progress_section") {
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    ProgressSection(
                                        summary = state.calculationResult.summary,
                                        settings = state.settings,
                                        onPreviousMonth = { viewModel.previousDashboardMonth() },
                                        onNextMonth = { viewModel.nextDashboardMonth() },
                                        onOpenGuruDetail = { showGuruDetailSheet = true }
                                    )
                                }
                            }

                            item(key = "footer_copyright") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 20.dp, bottom = 28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "HSBC Pulse RC 奖赏助手 · 本地离线安全存储",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // ================= Tab 2: 快速记账 (Entry) =================
                MainTab.ENTRY -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💳 快速记账",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "实时测算 RC",
                                fontSize = 12.sp,
                                color = HsbcRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        QuickEntryCard(
                            amount = state.inputAmount,
                            onAmountChange = { viewModel.onAmountChanged(it) },
                            dateTime = state.inputDateTime,
                            onDateTimeChange = { viewModel.onDateTimeChanged(it) },
                            selectedChannel = state.selectedChannel,
                            onChannelSelect = { viewModel.onChannelSelected(it) },
                            selectedCategory = state.selectedCategory,
                            onCategorySelect = { viewModel.onCategorySelected(it) },
                            preview = state.livePreview,
                            onSubmit = {
                                viewModel.addTransaction()
                                Toast.makeText(context, "✅ 记账成功！已计入流水明细", Toast.LENGTH_SHORT).show()
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // ================= Tab 3: 消费明细 (Transactions) =================
                MainTab.TRANSACTIONS -> {
                    TransactionsDetailScreen(
                        calculationResult = state.calculationResult,
                        onPreviousMonth = { viewModel.previousDashboardMonth() },
                        onNextMonth = { viewModel.nextDashboardMonth() },
                        onImportBackup = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        onExportBackup = {
                            viewModel.exportJson { jsonStr ->
                                pendingExportJson = jsonStr
                                val today = LocalDate.now().toString()
                                exportLauncher.launch("hsbc_pulse_records_$today.json")
                            }
                        },
                        onEdit = { pt -> viewModel.startEdit(pt.entity) },
                        onDelete = { id -> viewModel.deleteTransaction(id) }
                    )
                }
            }
        }
    }

    // 编辑单笔交易弹窗
    state.editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            onDismiss = { viewModel.dismissEdit() },
            onSave = { updated -> viewModel.saveEdit(updated) }
        )
    }

    // 设置弹窗
    if (state.isSettingsOpen) {
        SettingsDialog(
            settings = state.settings,
            onDismiss = { viewModel.closeSettings() },
            onSave = { newSettings -> viewModel.saveSettings(newSettings) },
            onClearAll = { viewModel.clearAllData() }
        )
    }

    // Travel Guru 达标与额度中心详情抽屉
    if (showGuruDetailSheet) {
        GuruDetailSheet(
            summary = state.calculationResult.summary,
            settings = state.settings,
            transactions = state.calculationResult.transactions,
            onDismiss = { showGuruDetailSheet = false }
        )
    }
}

private fun writeJsonToUri(context: Context, uri: Uri, content: String) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(content.toByteArray(Charsets.UTF_8))
        }
    }
}

private fun readJsonFromUri(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }.getOrNull()
}
