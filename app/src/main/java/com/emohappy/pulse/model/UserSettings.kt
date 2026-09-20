package com.emohappy.pulse.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class GuruLevelConfig(
    val level: Int = 1,
    val enabled: Boolean = false,
    val startDate: String = "2026-01-01",
    val endDate: String = "",
    val ratePercent: Double = 3.0,
    val capRC: Double = 500.0
)

@Serializable
data class GuruStagePeriod(
    val id: String = "stage_${System.currentTimeMillis()}",
    val cycleName: String = "第 1 轮",
    val level: Int = 1,
    val enabled: Boolean = true,
    val startDate: String = "2024-09-01",
    val endDate: String = "",
    val ratePercent: Double = 3.0,
    val capRC: Double = 500.0,
    val isDowngradeReset: Boolean = false
)

@Serializable
data class RhCnHalfYearConfig(
    val halfYearName: String = "上半年 (H1)",
    val enabled: Boolean = true,
    val regDate: String = "2026-01-01",
    val ratePercent: Double = 3.0,
    val quarterCapRC: Double = 300.0,
    val quarterSpendThreshold: Double = 10000.0
)

@Serializable
data class UserSettings(
    val redReward: Boolean = true,
    val redRegDate: String = "2026-01-01",
    val rhCnSpend: Boolean = true,
    val rhCnSpendStartDate: String = "2026-01-01",
    val rhCnSpendEndDate: String = "2026-12-31",
    val rhCnSpendRate: Double = 3.0,
    val rhCnSpendQuarterCap: Double = 300.0,
    val rhCnSpendThreshold: Double = 10000.0,
    val rhCnH1Config: RhCnHalfYearConfig = RhCnHalfYearConfig(
        halfYearName = "上半年 (H1)",
        enabled = true,
        regDate = "2026-01-01",
        ratePercent = 3.0,
        quarterCapRC = 300.0,
        quarterSpendThreshold = 10000.0
    ),
    val rhCnH2Config: RhCnHalfYearConfig = RhCnHalfYearConfig(
        halfYearName = "下半年 (H2 · 年中重置)",
        enabled = true,
        regDate = "2026-07-01",
        ratePercent = 3.0,
        quarterCapRC = 500.0,
        quarterSpendThreshold = 10000.0
    ),
    val pulseResetMidYear: Boolean = true,
    val chinaDining: Boolean = true,
    val chinaDiningDate: String = "2026-07-01",
    val chinaDiningEndDate: String = "2026-12-31",
    val diningRate: Double = 3.0,
    val diningMonthlyCap: Double = 80.0,
    val diningMinSpend: Double = 1200.0,
    val welcome: Boolean = true,
    val cardIssueDate: String = LocalDate.now().toString(),
    val welcomeSpendThreshold: Double = 8000.0,
    val hasReferralCode: Boolean = true,
    val guruLevel: Int = 0,
    val guruRegDate: String = "2026-01-01",
    val guruConfigs: List<GuruLevelConfig> = listOf(
        GuruLevelConfig(level = 1, enabled = false, startDate = "2026-01-01", endDate = "", ratePercent = 3.0, capRC = 500.0),
        GuruLevelConfig(level = 2, enabled = false, startDate = "2026-04-01", endDate = "", ratePercent = 4.0, capRC = 1200.0),
        GuruLevelConfig(level = 3, enabled = false, startDate = "2026-07-01", endDate = "", ratePercent = 6.0, capRC = 2200.0)
    ),
    val guruStages: List<GuruStagePeriod> = emptyList()
) {
    companion object {
        val DEFAULT_GURU_STAGES: List<GuruStagePeriod> = listOf(
            // 第 1 轮（2024年9月 ~ 2026年7月，已顺利完成全拿满 3,900 RC）
            GuruStagePeriod(
                id = "stage_c1_lv1",
                cycleName = "第 1 轮",
                level = 1,
                enabled = true,
                startDate = "2024-09-01",
                endDate = "2025-01-31",
                ratePercent = 3.0,
                capRC = 500.0,
                isDowngradeReset = false
            ),
            GuruStagePeriod(
                id = "stage_c1_lv2",
                cycleName = "第 1 轮",
                level = 2,
                enabled = true,
                startDate = "2025-02-01",
                endDate = "2025-08-31",
                ratePercent = 4.0,
                capRC = 1200.0,
                isDowngradeReset = false
            ),
            GuruStagePeriod(
                id = "stage_c1_lv3",
                cycleName = "第 1 轮",
                level = 3,
                enabled = true,
                startDate = "2025-09-01",
                endDate = "2026-07-31",
                ratePercent = 6.0,
                capRC = 2200.0,
                isDowngradeReset = false
            ),
            // 第 2 轮（2026年8月 ~ 至今，降级重刷中）
            GuruStagePeriod(
                id = "stage_c2_lv1",
                cycleName = "第 2 轮",
                level = 1,
                enabled = true,
                startDate = "2026-08-01",
                endDate = "2026-09-30",
                ratePercent = 3.0,
                capRC = 500.0,
                isDowngradeReset = true
            ),
            GuruStagePeriod(
                id = "stage_c2_lv2",
                cycleName = "第 2 轮",
                level = 2,
                enabled = true,
                startDate = "2026-10-01",
                endDate = "2027-07-31",
                ratePercent = 4.0,
                capRC = 1200.0,
                isDowngradeReset = false
            )
        )
    }

    val totalWelcomeReward: Double
        get() = if (hasReferralCode) 1800.0 else 800.0

    /**
     * 根据消费日期查找当前命中的有效 Guru 阶段配置（支持多周期与降级重刷）。
     */
    fun getActiveGuruStage(txDateStr: String): GuruStagePeriod? {
        if (guruStages.isNotEmpty()) {
            val matching = guruStages.filter { stage ->
                stage.enabled &&
                stage.startDate.isNotEmpty() &&
                txDateStr >= stage.startDate &&
                (stage.endDate.isEmpty() || txDateStr <= stage.endDate)
            }
            if (matching.isNotEmpty()) {
                return matching.maxByOrNull { it.startDate } ?: matching.last()
            }
        }
        val legacy = getActiveGuruConfig(txDateStr) ?: return null
        return GuruStagePeriod(
            id = "legacy_${legacy.level}",
            cycleName = "当前会籍",
            level = legacy.level,
            enabled = legacy.enabled,
            startDate = legacy.startDate,
            endDate = legacy.endDate,
            ratePercent = legacy.ratePercent,
            capRC = legacy.capRC
        )
    }

    /**
     * 向后兼容历史旧逻辑
     */
    fun getActiveGuruConfig(txDateStr: String): GuruLevelConfig? {
        if (guruConfigs.isNotEmpty()) {
            val matching = guruConfigs.filter { cfg ->
                cfg.enabled &&
                cfg.startDate.isNotEmpty() &&
                txDateStr >= cfg.startDate &&
                (cfg.endDate.isEmpty() || txDateStr <= cfg.endDate)
            }
            if (matching.isNotEmpty()) {
                return matching.maxByOrNull { it.level }
            }
        }
        if (guruLevel > 0 && (guruRegDate.isEmpty() || txDateStr >= guruRegDate)) {
            val defaultRates = mapOf(1 to 3.0, 2 to 4.0, 3 to 6.0)
            val defaultCaps = mapOf(1 to 500.0, 2 to 1200.0, 3 to 2200.0)
            return GuruLevelConfig(
                level = guruLevel,
                enabled = true,
                startDate = guruRegDate,
                endDate = "",
                ratePercent = defaultRates[guruLevel] ?: 3.0,
                capRC = defaultCaps[guruLevel] ?: 500.0
            )
        }
        return null
    }
}
