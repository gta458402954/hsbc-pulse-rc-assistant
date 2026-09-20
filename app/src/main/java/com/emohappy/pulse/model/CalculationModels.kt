package com.emohappy.pulse.model

data class BreakdownDetail(
    val rcBase: Double = 0.0,
    val rcPulse: Double = 0.0,
    val rcRed: Double = 0.0,
    val rcRhCn: Double = 0.0,
    val rcRhCnPending: Double = 0.0,
    val rcDining: Double = 0.0,
    val rcDiningPending: Double = 0.0,
    val rcGuru: Double = 0.0
)

sealed interface TimelineItem {
    val itemId: String
    val itemDateTime: String
    val isSystemAward: Boolean
}

data class ProcessedTransaction(
    val entity: TransactionEntity,
    val unlockedRC: Double,
    val pendingRC: Double,
    val totalRC: Double,
    val breakdown: List<String>,
    val breakdownDetail: BreakdownDetail
) : TimelineItem {
    override val itemId: String get() = entity.id
    override val itemDateTime: String get() = entity.dateTime
    override val isSystemAward: Boolean get() = false

    val channel: PaymentChannel get() = PaymentChannel.fromCode(entity.channel)
    val category: ExpenseCategory get() = ExpenseCategory.fromCode(entity.category)
}

data class SystemAward(
    val id: String,
    val dateTime: String,
    val amount: Double = 0.0,
    val totalRC: Double,
    val unlockedRC: Double,
    val pendingRC: Double = 0.0,
    val title: String,
    val detail: String
) : TimelineItem {
    override val itemId: String get() = id
    override val itemDateTime: String get() = dateTime
    override val isSystemAward: Boolean get() = true
}

data class RhCnQuarterStatus(
    val quarterIndex: Int = 1,
    val halfYearIndex: Int = 1,
    val phaseIndex: Int = 1,
    val quarterName: String = "2026 Q1 (H1 P1)",
    val periodStr: String = "01/01 ~ 03/31",
    val payoutDesc: String = "次季度6月入账",
    val isMidYearReset: Boolean = false,
    val isRegistered: Boolean = true,
    val currentSpend: Double = 0.0,
    val thresholdSpend: Double = 10000.0,
    val earnedRC: Double = 0.0,
    val capRC: Double = 300.0,
    val isUnlocked: Boolean = false,
    val progress: Float = 0f,
    val remainingSpendToUnlock: Double = 10000.0,
    val remainingSpendToCap: Double = 10000.0
)

data class DashboardSummary(
    val totalUnlockedRC: Double = 0.0,
    val totalPendingRC: Double = 0.0,
    val totalSpend: Double = 0.0,
    val avgRate: Double = 0.0,

    // 迎新礼
    val welcomeSpend: Double = 0.0,
    val welcomeThreshold: Double = 8000.0,
    val welcomeRewardRC: Double = 1800.0,
    val welcomeAchieved: Boolean = false,
    val welcomeDaysLeft: Long = 0,

    // Pulse 2%
    val pulseUsedRC: Double = 0.0,
    val pulseCapRC: Double = 1600.0,
    val pulseH1UsedRC: Double = 0.0,
    val pulseH2UsedRC: Double = 0.0,
    val pulseResetMidYear: Boolean = true,

    // 最红 2%
    val redUsedRC: Double = 0.0,
    val redCapRC: Double = 2000.0,

    // 最红中国内地签账奖赏 (RH CN Spend - 季度活动)
    val rhCnStatus: RhCnQuarterStatus = RhCnQuarterStatus(),
    val allRhCnQuarters: List<RhCnQuarterStatus> = emptyList(),

    // 当月内地餐饮 (可选旧项)
    val monthStr: String = "",
    val monthTotalSpend: Double = 0.0,
    val monthDiningSpend: Double = 0.0,
    val monthDiningRC: Double = 0.0,
    val diningMonthlyCap: Double = 80.0,
    val diningMinSpend: Double = 1200.0,
    val diningRate: Double = 3.0,
    val isMonthDiningUnlocked: Boolean = false,

    // Travel Guru 达标与额度状态
    val guruState: GuruDashboardState = GuruDashboardState()
)

data class GuruTierStatus(
    val stageId: String = "",
    val cycleName: String = "",
    val level: Int = 1,
    val levelName: String = "Lv.1 GO 旅人",
    val enabled: Boolean = false,
    val isCurrentActive: Boolean = false,
    val isCompleted: Boolean = false,
    val startDate: String = "",
    val endDate: String = "",
    val daysRemaining: Long = 0,
    val ratePercent: Double = 3.0,
    val capRC: Double = 500.0,
    val earnedRC: Double = 0.0,
    val remainingRC: Double = 0.0,
    val remainingSpendNeeded: Double = 0.0,
    val progress: Float = 0f,
    val isDowngradeReset: Boolean = false
)

data class GuruUpgradeTaskStatus(
    val currentCycleName: String = "第 2 轮",
    val cycleStartDate: String = "",
    val currentLevel: Int = 1,
    val targetLevel: Int = 2,
    val targetLevelName: String = "GING 级旅人 (第二级)",
    val deadlineDate: String = "",
    val task1TargetSpend: Double = 30000.0,
    val task1CurrentSpend: Double = 0.0,
    val task1IsCompleted: Boolean = false,
    val task1Progress: Float = 0f,
    val task2TargetCount: Int = 3,
    val task2CurrentCount: Int = 0,
    val task2MinAmountPerTx: Double = 800.0,
    val task2IsCompleted: Boolean = false,
    val isReadyToUpgrade: Boolean = false,
    val effectiveUpgradeMonth: String = "",
    val isMaxLevel: Boolean = false
)

data class GuruDashboardState(
    val hasEnabledGuru: Boolean = false,
    val currentCycleName: String = "第 2 轮",
    val allCycles: List<String> = emptyList(),
    val activeTier: GuruTierStatus? = null,
    val activeCycleTiers: List<GuruTierStatus> = emptyList(),
    val allTiers: List<GuruTierStatus> = emptyList(),
    val upgradeTask: GuruUpgradeTaskStatus = GuruUpgradeTaskStatus(),
    val totalHistoricalGuruRC: Double = 0.0
)

data class PreviewResult(
    val totalRC: Double,
    val pendingRC: Double,
    val rate: Double,
    val breakdownText: String
)

data class CalculationResult(
    val transactions: List<ProcessedTransaction>,
    val systemAwards: List<SystemAward>,
    val timeline: List<TimelineItem>, // reverse chronological order for display
    val summary: DashboardSummary
)
