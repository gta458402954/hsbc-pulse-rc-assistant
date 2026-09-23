package com.emohappy.pulse.domain

import com.emohappy.pulse.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

object PulseCalculatorEngine {

    private const val PULSE_CAP = 1600.0
    private const val RED_CAP = 2000.0
    private val GURU_CAPS = mapOf(1 to 500.0, 2 to 1200.0, 3 to 2200.0)
    private val GURU_RATES = mapOf(1 to 0.03, 2 to 0.04, 3 to 0.06)

    fun recalculate(
        transactions: List<TransactionEntity>,
        settings: UserSettings,
        targetMonth: YearMonth = YearMonth.now()
    ): CalculationResult {
        // 1. 严格按时间升序排序计算累积额度
        val sorted = transactions.sortedBy { it.dateTime }
        val systemAwards = mutableListOf<SystemAward>()

        val pulseH1ByYear = mutableMapOf<Int, Double>()
        val pulseH2ByYear = mutableMapOf<Int, Double>()
        val pulseYearlyByYear = mutableMapOf<Int, Double>()
        val redByYear = mutableMapOf<Int, Double>()
        val cumGuruRCByStageId = mutableMapOf<String, Double>()

        val dRatePercent = settings.diningRate
        val dRateDecimal = dRatePercent / 100.0
        val diningCap = settings.diningMonthlyCap
        val diningMinSpend = settings.diningMinSpend

        // 2. 预计算：当月有效消费（用于月度餐饮门槛）与季度内地有效签账（用于最红中国内地签账门槛）
        val monthlyTotalStats = mutableMapOf<String, Double>()
        val quarterlyCnSpendStats = mutableMapOf<String, Double>()

        sorted.forEach { t ->
            val amt = t.amount
            val txDateStr = if (t.dateTime.length >= 10) t.dateTime.substring(0, 10) else ""
            val monthKey = if (t.dateTime.length >= 7) t.dateTime.substring(0, 7) else ""
            val isMicroPay = (t.channel == PaymentChannel.WECHAT_ALIPAY.code)
            val isCnSpend = (t.category != ExpenseCategory.TRAVEL.code && t.category != "travel_booking")

            if (!isMicroPay && monthKey.isNotEmpty()) {
                monthlyTotalStats[monthKey] = (monthlyTotalStats[monthKey] ?: 0.0) + amt
            }

            if (isCnSpend && t.dateTime.length >= 10) {
                val year = runCatching { t.dateTime.substring(0, 4).toInt() }.getOrDefault(targetMonth.year)
                val month = runCatching { t.dateTime.substring(5, 7).toInt() }.getOrDefault(1)
                val qIndex = (month - 1) / 3 + 1
                val qKey = "$year-Q$qIndex"

                // 2025 全年以及 2026 上半年 (1~6月) 采用季度全类别内地签账门槛模式
                val isQuarterMode = (year < 2026) || (year == 2026 && month <= 6)
                if (isQuarterMode) {
                    val isEligible = if (year == 2026) {
                        val hConfig = settings.rhCnH1Config
                        hConfig.enabled && (hConfig.regDate.isEmpty() || txDateStr >= hConfig.regDate)
                    } else {
                        true // 2025年及历史年份按活动正常计入
                    }
                    if (isEligible) {
                        quarterlyCnSpendStats[qKey] = (quarterlyCnSpendStats[qKey] ?: 0.0) + amt
                    }
                }
            }
        }

        val monthlyUnlocked = monthlyTotalStats.mapValues { (_, spend) ->
            spend >= diningMinSpend
        }
        val quarterlyUnlocked = quarterlyCnSpendStats.mapValues { (qKey, spend) ->
            val year = runCatching { qKey.substring(0, 4).toInt() }.getOrDefault(2026)
            val threshold = if (year == 2026) settings.rhCnH1Config.quarterSpendThreshold else 10000.0
            spend >= threshold
        }

        // 3. 迎新礼周期与阈值计算 (发卡 60 天内)
        var welcomeCumSpend = 0.0
        var welcomeMilestoneTriggered = false
        val welcomeThreshold = settings.welcomeSpendThreshold
        val totalWelcomeReward = settings.totalWelcomeReward

        val issueDate = runCatching { LocalDate.parse(settings.cardIssueDate) }.getOrElse { LocalDate.now() }
        val expireDate = issueDate.plusDays(60)

        val monthlyAwardedRC = mutableMapOf<String, Double>()
        val quarterlyAwardedRC = mutableMapOf<String, Double>()
        val processedTxs = mutableListOf<ProcessedTransaction>()

        sorted.forEach { t ->
            val amt = t.amount
            val txDateStr = if (t.dateTime.length >= 10) t.dateTime.substring(0, 10) else ""
            val monthKey = if (t.dateTime.length >= 7) t.dateTime.substring(0, 7) else ""
            val txDate = runCatching { LocalDate.parse(txDateStr) }.getOrNull()

            val txYear = runCatching { t.dateTime.substring(0, 4).toInt() }.getOrDefault(targetMonth.year)
            val txMonth = runCatching { t.dateTime.substring(5, 7).toInt() }.getOrDefault(1)
            val qIndex = (txMonth - 1) / 3 + 1
            val qKey = "$txYear-Q$qIndex"

            val is2026H2Dining = (txYear == 2026 && txMonth > 6) || (txYear > 2026 && txMonth > 6)
            val phaseIndex = if (is2026H2Dining) qIndex - 2 else qIndex
            val phaseName = if (is2026H2Dining) "H2 P$phaseIndex (年中重置)" else "H1 P$phaseIndex"
            val qName = if (txYear == 2025) {
                when (qIndex) {
                    1 -> "2025 Q1 (最红内地及澳门 P1 · 季上限 360 RC)"
                    2 -> "2025 Q2 (最红内地及澳门 P2 · 季上限 300 RC)"
                    3 -> "2025 Q3 (最红内地签账 P1 · 季上限 500 RC)"
                    else -> "2025 Q4 (最红内地签账 P2 · 季上限 500 RC)"
                }
            } else if (txYear == 2026) {
                "${txYear} Q$qIndex ($phaseName)"
            } else {
                "${txYear} Q$qIndex"
            }

            val isMicroPay = (t.channel == PaymentChannel.WECHAT_ALIPAY.code)
            val isMobileUnionPay = (t.channel == PaymentChannel.UNIONPAY_APP.code || t.channel == PaymentChannel.APPLE_PAY.code)
            val isCnSpend = (t.category != ExpenseCategory.TRAVEL.code && t.category != "travel_booking")

            val rcBase = amt * 0.004
            var rcPulse = 0.0
            var rcRed = 0.0
            var rcRhCn = 0.0
            var rcRhCnPending = 0.0
            var rcDining = 0.0
            var rcDiningPending = 0.0
            var rcGuru = 0.0
            val breakdown = mutableListOf<String>()

            breakdown.add("基础 0.4% (+${String.format("%.2f", rcBase)})")

            // Pulse 2% 移动银联特别奖赏 (仅限 Apple Pay / 云闪付扫码 - 按年份及半年度隔离额度池)
            if (isMobileUnionPay) {
                val rawPulse = amt * 0.02
                if (settings.pulseResetMidYear) {
                    if (txMonth <= 6) {
                        val curH1 = pulseH1ByYear[txYear] ?: 0.0
                        val avail = max(0.0, PULSE_CAP - curH1)
                        rcPulse = min(rawPulse, avail)
                        pulseH1ByYear[txYear] = curH1 + rcPulse
                    } else {
                        val curH2 = pulseH2ByYear[txYear] ?: 0.0
                        val avail = max(0.0, PULSE_CAP - curH2)
                        rcPulse = min(rawPulse, avail)
                        pulseH2ByYear[txYear] = curH2 + rcPulse
                    }
                } else {
                    val curYearPulse = pulseYearlyByYear[txYear] ?: 0.0
                    val avail = max(0.0, PULSE_CAP - curYearPulse)
                    rcPulse = min(rawPulse, avail)
                    pulseYearlyByYear[txYear] = curYearPulse + rcPulse
                }
                if (rcPulse > 0) {
                    breakdown.add("Pulse 2% (+${String.format("%.2f", rcPulse)})")
                } else if (rawPulse > 0) {
                    breakdown.add("Pulse已封顶 (+0.00)")
                }
            }

            // 最红自主 2% (赏世界 5X - 按自然年隔离额度池，年上限 2,000 RC)
            if (settings.redReward) {
                val isRedEligible = settings.redRegDate.isEmpty() || txYear < 2026 || txDateStr >= settings.redRegDate
                if (isRedEligible) {
                    val curYearRed = redByYear[txYear] ?: 0.0
                    val rawRed = amt * 0.02
                    rcRed = min(rawRed, max(0.0, RED_CAP - curYearRed))
                    redByYear[txYear] = curYearRed + rcRed
                    if (rcRed > 0) {
                        breakdown.add("最红 2% (+${String.format("%.2f", rcRed)})")
                    } else if (rawRed > 0) {
                        breakdown.add("最红已封顶 (+0.00)")
                    }
                }
            }

            // 最红中国内地签账奖赏 (RH CN Spend) - 2025 全年与 2026 H1 季度全类别 / 2026 H2 月度餐饮
            val isRhCnActive = settings.rhCnSpend || settings.chinaDining
            if (isRhCnActive && isCnSpend) {
                if (!is2026H2Dining) {
                    // 2025 全年，或 2026 上半年 (1~6月)：季度全类别内地消费达标模式
                    val isReg = if (txYear < 2026) true else (settings.rhCnH1Config.regDate.isEmpty() || txDateStr >= settings.rhCnH1Config.regDate)
                    val inDateRange = if (txYear == 2026) {
                        isReg && (settings.rhCnSpendEndDate.isEmpty() || txDateStr <= settings.rhCnSpendEndDate)
                    } else {
                        isReg
                    }
                    if (inDateRange) {
                        val qCap = if (txYear == 2026) {
                            settings.rhCnH1Config.quarterCapRC
                        } else if (txYear == 2025) {
                            when (qIndex) {
                                1 -> 360.0
                                2 -> 300.0
                                else -> 500.0
                            }
                        } else {
                            300.0
                        }
                        val qThreshold = if (txYear == 2026) settings.rhCnH1Config.quarterSpendThreshold else 10000.0
                        val qRatePercent = if (txYear == 2026) settings.rhCnH1Config.ratePercent else 3.0

                        val curQuarterAwarded = quarterlyAwardedRC[qKey] ?: 0.0
                        val rawRhCn = amt * (qRatePercent / 100.0)
                        val potentialRhCn = min(rawRhCn, max(0.0, qCap - curQuarterAwarded))

                        val isQUnlocked = quarterlyUnlocked[qKey] == true
                        if (isQUnlocked) {
                            rcRhCn = potentialRhCn
                            quarterlyAwardedRC[qKey] = curQuarterAwarded + rcRhCn
                            if (rcRhCn > 0) {
                                breakdown.add("最红内地 ${qRatePercent.toInt()}% [${qName}已达标] (+${String.format("%.2f", rcRhCn)})")
                            } else if (rawRhCn > 0) {
                                breakdown.add("最红内地已达季封顶 (+0.00)")
                            }
                        } else {
                            rcRhCnPending = potentialRhCn
                            if (rcRhCnPending > 0) {
                                breakdown.add("最红内地 ${qRatePercent.toInt()}% [待本季满¥${qThreshold.toInt()}] (待+${String.format("%.2f", rcRhCnPending)})")
                            }
                        }
                    } else if (!isReg) {
                        breakdown.add("最红内地未登记 (${settings.rhCnH1Config.halfYearName}需于${settings.rhCnH1Config.regDate}后生效)")
                    }
                } else {
                    // 下半年 (2026 H2: 7~12月 · 年中规则调整)：仅限合资格餐饮签账享受额外 3%
                    if (t.category == ExpenseCategory.DINING.code && !isMicroPay) {
                        val inDateRange = (settings.chinaDiningDate.isEmpty() || txDateStr >= settings.chinaDiningDate) &&
                                (settings.chinaDiningEndDate.isEmpty() || txDateStr <= settings.chinaDiningEndDate)
                        if (inDateRange) {
                            val currentAwarded = monthlyAwardedRC[monthKey] ?: 0.0
                            val rawDining = amt * dRateDecimal
                            val potentialGrant = min(rawDining, max(0.0, diningCap - currentAwarded))

                            val isMonthUnlocked = monthlyUnlocked[monthKey] == true
                            if (isMonthUnlocked) {
                                rcDining = potentialGrant
                                rcRhCn = potentialGrant
                                monthlyAwardedRC[monthKey] = currentAwarded + rcDining
                                if (rcDining > 0) {
                                    breakdown.add("最红内地(餐饮) ${dRatePercent}% [已达标] (+${String.format("%.2f", rcDining)})")
                                } else if (rawDining > 0) {
                                    breakdown.add("最红内地(餐饮)已达月封顶 (+0.00)")
                                }
                            } else {
                                rcDiningPending = potentialGrant
                                rcRhCnPending = potentialGrant
                                if (rcDiningPending > 0) {
                                    breakdown.add("最红内地(餐饮) ${dRatePercent}% [待当月满¥${diningMinSpend.toInt()}] (待+${String.format("%.2f", rcDiningPending)})")
                                }
                            }
                        }
                    }
                }
            }

            // 迎新礼累计
            if (settings.welcome && !isMicroPay && txDate != null) {
                if (!txDate.isBefore(issueDate) && !txDate.isAfter(expireDate)) {
                    welcomeCumSpend += amt
                    if (!welcomeMilestoneTriggered && welcomeCumSpend >= welcomeThreshold) {
                        welcomeMilestoneTriggered = true
                        val detailText = if (settings.hasReferralCode) {
                            "消费满 ¥${welcomeThreshold.toInt()} · 基础 800 + 推荐码加赠 1000 RC"
                        } else {
                            "消费满 ¥${welcomeThreshold.toInt()} · 获得基础迎新 800 RC"
                        }
                        systemAwards.add(
                            SystemAward(
                                id = "welcome_award_${t.id}",
                                dateTime = t.dateTime,
                                totalRC = totalWelcomeReward,
                                unlockedRC = totalWelcomeReward,
                                title = "🎁 迎新礼达标奖励",
                                detail = detailText
                            )
                        )
                    }
                }
            }

            // Travel Guru 签账阶段加赠
            val activeGuru = settings.getActiveGuruStage(txDateStr)
            if (activeGuru != null && activeGuru.enabled && !isMicroPay && txDate != null) {
                val rawGuru = amt * (activeGuru.ratePercent / 100.0)
                val currentGuruAwarded = cumGuruRCByStageId[activeGuru.id] ?: 0.0
                rcGuru = min(rawGuru, max(0.0, activeGuru.capRC - currentGuruAwarded))
                cumGuruRCByStageId[activeGuru.id] = currentGuruAwarded + rcGuru

                val rateStr = if (activeGuru.ratePercent % 1.0 == 0.0) "${activeGuru.ratePercent.toInt()}%" else "${activeGuru.ratePercent}%"
                if (rcGuru > 0) {
                    breakdown.add("Guru Lv${activeGuru.level} $rateStr (+${String.format("%.2f", rcGuru)})")
                } else if (rawGuru > 0) {
                    breakdown.add("Guru Lv${activeGuru.level}已封顶 (+0.00)")
                }
            }

            val unlockedRC = rcBase + rcPulse + rcRed + (if (is2026H2Dining) rcDining else rcRhCn) + rcGuru
            val pendingRC = if (is2026H2Dining) rcDiningPending else rcRhCnPending

            processedTxs.add(
                ProcessedTransaction(
                    entity = t,
                    unlockedRC = unlockedRC,
                    pendingRC = pendingRC,
                    totalRC = unlockedRC,
                    breakdown = breakdown,
                    breakdownDetail = BreakdownDetail(
                        rcBase = rcBase,
                        rcPulse = rcPulse,
                        rcRed = rcRed,
                        rcRhCn = rcRhCn,
                        rcRhCnPending = rcRhCnPending,
                        rcDining = rcDining,
                        rcDiningPending = rcDiningPending,
                        rcGuru = rcGuru
                    )
                )
            )
        }

        // 4. 构建仪表盘与统计概要
        val now = LocalDate.now()
        val curMonthStr = "${targetMonth.year}年${targetMonth.monthValue}月"
        val curMonthKey = String.format("%04d-%02d", targetMonth.year, targetMonth.monthValue)

        val txUnlockedRC = processedTxs.sumOf { it.unlockedRC }
        val sysAwardRC = systemAwards.sumOf { it.totalRC }
        val totalUnlockedRC = txUnlockedRC + sysAwardRC
        val totalPendingRC = processedTxs.sumOf { it.pendingRC }
        val totalSpend = processedTxs.sumOf { it.entity.amount }
        val avgRate = if (totalSpend > 0) (totalUnlockedRC / totalSpend) * 100 else 0.0

        val curMonthTxs = processedTxs.filter {
            it.entity.dateTime.startsWith(curMonthKey) && it.entity.channel != PaymentChannel.WECHAT_ALIPAY.code
        }
        val monthTotalSpend = curMonthTxs.sumOf { it.entity.amount }
        val monthDiningSpend = curMonthTxs.filter { it.entity.category == ExpenseCategory.DINING.code }.sumOf { it.entity.amount }
        val monthDiningRC = curMonthTxs.sumOf { it.breakdownDetail.rcDining }
        val isMonthDiningUnlocked = monthlyUnlocked[curMonthKey] == true

        // 迎新礼天数与进度
        val daysLeft = max(0L, java.time.temporal.ChronoUnit.DAYS.between(now, expireDate))

        // 最红中国内地签账 (RH CN Spend) 4 季度汇总构建 (含 2025 全年季度模式与 2026 H2 年中重置餐饮逻辑)
        val curYear = targetMonth.year
        val isCurrentYearDiningMode = (curYear >= 2026)
        val allRhCnQuarters = (1..4).map { q ->
            val qKey = "$curYear-Q$q"
            val isQMonthlyDining = isCurrentYearDiningMode && (q >= 3)
            val pIndex = if (isQMonthlyDining) q - 2 else q
            val period = when (q) {
                1 -> "01/01 ~ 03/31"
                2 -> "04/01 ~ 06/30"
                3 -> "07/01 ~ 09/30"
                else -> "10/01 ~ 12/31"
            }
            val payout = when (q) {
                1 -> "次季度6月入账"
                2 -> "次季度9月入账"
                3 -> if (isQMonthlyDining) "次月入账" else "次季度12月入账"
                else -> if (isQMonthlyDining) "次月入账" else "次季度次年3月入账"
            }
            val qName = if (curYear == 2025) {
                when (q) {
                    1 -> "2025 Q1 (最红内地及澳门 P1 · 季上限 360 RC)"
                    2 -> "2025 Q2 (最红内地及澳门 P2 · 季上限 300 RC)"
                    3 -> "2025 Q3 (最红内地签账 P1 · 季上限 500 RC)"
                    else -> "2025 Q4 (最红内地签账 P2 · 季上限 500 RC)"
                }
            } else if (curYear == 2026) {
                when (q) {
                    1 -> "2026 Q1 (H1 P1 · 季度全类别)"
                    2 -> "2026 Q2 (H1 P2 · 季度全类别)"
                    3 -> "2026 Q3 (H2 餐饮加码 · 年中调整)"
                    else -> "2026 Q4 (H2 餐饮加码)"
                }
            } else {
                "$curYear Q$q"
            }

            if (!isQMonthlyDining) {
                val qThreshold = if (curYear == 2026) settings.rhCnH1Config.quarterSpendThreshold else 10000.0
                val qCap = if (curYear == 2026) {
                    settings.rhCnH1Config.quarterCapRC
                } else if (curYear == 2025) {
                    when (q) {
                        1 -> 360.0
                        2 -> 300.0
                        else -> 500.0
                    }
                } else {
                    300.0
                }
                val qRatePercent = if (curYear == 2026) settings.rhCnH1Config.ratePercent else 3.0
                val qSpend = quarterlyCnSpendStats[qKey] ?: 0.0
                val qEarned = quarterlyAwardedRC[qKey] ?: 0.0
                val isUnlocked = qSpend >= qThreshold
                val prog = if (!isUnlocked) {
                    if (qThreshold > 0) (qSpend / qThreshold).toFloat().coerceIn(0f, 1f) else 0f
                } else {
                    if (qCap > 0) (qEarned / qCap).toFloat().coerceIn(0f, 1f) else 0f
                }
                val remainingToUnlock = max(0.0, qThreshold - qSpend)
                val rateDec = qRatePercent / 100.0
                val remainingToCap = if (rateDec > 0) max(0.0, (qCap - qEarned) / rateDec) else 0.0

                RhCnQuarterStatus(
                    quarterIndex = q,
                    halfYearIndex = if (q <= 2) 1 else 2,
                    phaseIndex = pIndex,
                    quarterName = qName,
                    periodStr = period,
                    payoutDesc = payout,
                    isMidYearReset = false,
                    isRegistered = if (curYear < 2026) true else settings.rhCnH1Config.enabled,
                    currentSpend = qSpend,
                    thresholdSpend = qThreshold,
                    earnedRC = qEarned,
                    capRC = qCap,
                    isUnlocked = isUnlocked,
                    progress = prog,
                    remainingSpendToUnlock = remainingToUnlock,
                    remainingSpendToCap = remainingToCap
                )
            } else {
                val qMonths = if (q == 3) listOf("07", "08", "09") else listOf("10", "11", "12")
                val qTxs = processedTxs.filter { tx ->
                    val m = if (tx.entity.dateTime.length >= 7) tx.entity.dateTime.substring(5, 7) else ""
                    qMonths.contains(m) && tx.entity.dateTime.startsWith(curYear.toString())
                }
                val qSpend = qTxs.sumOf { it.entity.amount }
                val qEarned = qTxs.sumOf { it.breakdownDetail.rcDining }
                val qCap = settings.diningMonthlyCap * 3
                val prog = if (qCap > 0) (qEarned / qCap).toFloat().coerceIn(0f, 1f) else 0f
                val dRateDec = (settings.diningRate / 100.0).coerceAtLeast(0.001)
                val remainingSpendToCap = if (dRateDec > 0) max(0.0, (qCap - qEarned) / dRateDec) else 0.0

                RhCnQuarterStatus(
                    quarterIndex = q,
                    halfYearIndex = 2,
                    phaseIndex = pIndex,
                    quarterName = qName,
                    periodStr = period,
                    payoutDesc = payout,
                    isMidYearReset = (q == 3),
                    isRegistered = settings.rhCnSpend || settings.chinaDining,
                    currentSpend = qSpend,
                    thresholdSpend = settings.diningMinSpend * 3,
                    earnedRC = qEarned,
                    capRC = qCap,
                    isUnlocked = qEarned > 0,
                    progress = prog,
                    remainingSpendToUnlock = 0.0,
                    remainingSpendToCap = remainingSpendToCap
                )
            }
        }

        val curQuarterIndex = (targetMonth.monthValue - 1) / 3 + 1
        val rhCnStatus = allRhCnQuarters.getOrElse(curQuarterIndex - 1) { RhCnQuarterStatus() }

        // Travel Guru 状态统计 (支持多周期与降级重刷)
        val todayStr = now.toString()
        val levelNames = mapOf(1 to "Lv.1 GO 旅人", 2 to "Lv.2 GING 旅人", 3 to "Lv.3 GURU 旅人")
        val stages = if (settings.guruStages.isNotEmpty()) {
            settings.guruStages
        } else if (settings.guruConfigs.isNotEmpty()) {
            settings.guruConfigs.map { cfg ->
                GuruStagePeriod(
                    id = "cfg_${cfg.level}",
                    cycleName = "当前会籍",
                    level = cfg.level,
                    enabled = cfg.enabled,
                    startDate = cfg.startDate,
                    endDate = cfg.endDate,
                    ratePercent = cfg.ratePercent,
                    capRC = cfg.capRC
                )
            }
        } else {
            emptyList()
        }
        val hasEnabledGuru = stages.any { it.enabled } || settings.guruLevel > 0

        val allTierStatuses = stages.map { stage ->
            val earned = cumGuruRCByStageId[stage.id] ?: 0.0
            val cap = stage.capRC
            val remaining = max(0.0, cap - earned)
            val rateDec = stage.ratePercent / 100.0
            val spendNeeded = if (rateDec > 0) remaining / rateDec else 0.0
            val progress = if (cap > 0) (earned / cap).toFloat().coerceIn(0f, 1f) else 0f

            val daysRemaining = if (stage.endDate.isNotEmpty()) {
                val end = runCatching { LocalDate.parse(stage.endDate) }.getOrNull()
                if (end != null) max(0L, java.time.temporal.ChronoUnit.DAYS.between(now, end)) else 0L
            } else 0L

            val isCurrentActive = stage.enabled &&
                    stage.startDate.isNotEmpty() &&
                    todayStr >= stage.startDate &&
                    (stage.endDate.isEmpty() || todayStr <= stage.endDate)

            GuruTierStatus(
                stageId = stage.id,
                cycleName = stage.cycleName,
                level = stage.level,
                levelName = levelNames[stage.level] ?: "Lv.${stage.level}",
                enabled = stage.enabled,
                isCurrentActive = isCurrentActive,
                isCompleted = cap > 0 && earned >= cap,
                startDate = stage.startDate,
                endDate = stage.endDate,
                daysRemaining = daysRemaining,
                ratePercent = stage.ratePercent,
                capRC = cap,
                earnedRC = earned,
                remainingRC = remaining,
                remainingSpendNeeded = spendNeeded,
                progress = progress,
                isDowngradeReset = stage.isDowngradeReset
            )
        }

        val activeTier = allTierStatuses.firstOrNull { it.isCurrentActive }
            ?: allTierStatuses.lastOrNull { it.enabled }
            ?: allTierStatuses.lastOrNull()

        val currentCycleName = activeTier?.cycleName ?: stages.lastOrNull()?.cycleName ?: "第 1 轮"
        val activeCycleTiers = allTierStatuses.filter { it.cycleName == currentCycleName }
        val allCycles = allTierStatuses.map { it.cycleName }.distinct()
        val totalHistoricalGuruRC = cumGuruRCByStageId.values.sum()

        // 升级任务计算：仅统计【当前活跃周期】内（降级重刷起点之后）的达标消费与交易笔数
        val currentGuruLevel = activeTier?.level ?: 1
        val isMaxLevel = currentGuruLevel >= 3

        val targetLevel = if (currentGuruLevel == 1) 2 else 3
        val targetLevelName = if (targetLevel == 2) "GING 级旅人 (第二级)" else "GURU 级旅人 (第三级)"
        val task1TargetSpend = if (targetLevel == 2) 30000.0 else 70000.0
        val task2TargetCount = if (targetLevel == 2) 3 else 6

        // 获取当前周期的起始日期（取当前周期各阶段最早的 startDate）
        val currentCycleStages = stages.filter { it.cycleName == currentCycleName }
        val cycleStartDate = currentCycleStages.minOfOrNull { it.startDate } ?: ""

        // 是否已在规则中预定了次月升级阶段（例如已配置 2026-10-01 起生效的 Lv.2，说明官方已达标锁定升级）
        val scheduledNextStage = currentCycleStages.firstOrNull {
            it.enabled && it.level > currentGuruLevel && it.startDate.isNotEmpty() && it.startDate > (activeTier?.startDate ?: "")
        }
        val isScheduledUpgrade = scheduledNextStage != null

        // 任务一：合资格外币签账（汇丰规定：所有外币签账均计入，不含微信/支付宝等电子钱包，包含内地日常、餐饮、境外机酒等）
        val eligibleForeignTxs = processedTxs.filter {
            it.entity.channel != PaymentChannel.WECHAT_ALIPAY.code &&
            (cycleStartDate.isEmpty() || it.entity.dateTime >= cycleStartDate)
        }
        val actualForeignSpend = eligibleForeignTxs.sumOf { it.entity.amount }
        val task1Spend = if (isScheduledUpgrade && actualForeignSpend < task1TargetSpend) task1TargetSpend else actualForeignSpend
        val task1Done = isScheduledUpgrade || task1Spend >= task1TargetSpend
        val task1Prog = if (isScheduledUpgrade) 1f else (task1Spend / task1TargetSpend).toFloat().coerceIn(0f, 1f)

        // 任务二：预订机票/邮轮/酒店达 3 次或以上（需单笔满 ¥800 / HKD 800）
        val task2Bookings = eligibleForeignTxs.filter {
            (it.entity.category == ExpenseCategory.TRAVEL.code || it.entity.category == "travel_booking") &&
            it.entity.amount >= 800.0
        }
        val actualTask2Count = task2Bookings.size
        val task2Count = if (isScheduledUpgrade && actualTask2Count < task2TargetCount) task2TargetCount else actualTask2Count
        val task2Done = isScheduledUpgrade || task2Count >= task2TargetCount

        val isReadyToUpgrade = if (isMaxLevel) false else (task1Done && task2Done)

        val effectiveUpgradeMonth = if (scheduledNextStage != null && scheduledNextStage.startDate.isNotEmpty()) {
            val sDate = runCatching { LocalDate.parse(scheduledNextStage.startDate) }.getOrNull()
            if (sDate != null) "${sDate.year}年${sDate.monthValue}月" else "次月"
        } else if (isReadyToUpgrade) {
            val lastQualifyingTx = eligibleForeignTxs.lastOrNull()
            val txDate = lastQualifyingTx?.let { runCatching { LocalDate.parse(it.entity.dateTime.substring(0, 10)) }.getOrNull() } ?: now
            val upMonth = txDate.plusMonths(1)
            "${upMonth.year}年${upMonth.monthValue}月"
        } else {
            val nextMonth = now.plusMonths(1)
            "${nextMonth.year}年${nextMonth.monthValue}月"
        }

        val deadlineDate = if (activeTier != null && activeTier.endDate.isNotEmpty()) {
            activeTier.endDate
        } else {
            val start = activeTier?.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: now
            start.plusYears(1).toString()
        }

        val upgradeTask = GuruUpgradeTaskStatus(
            currentCycleName = currentCycleName,
            cycleStartDate = cycleStartDate,
            currentLevel = currentGuruLevel,
            targetLevel = targetLevel,
            targetLevelName = targetLevelName,
            deadlineDate = deadlineDate,
            task1TargetSpend = task1TargetSpend,
            task1CurrentSpend = task1Spend,
            task1IsCompleted = task1Done,
            task1Progress = task1Prog,
            task2TargetCount = task2TargetCount,
            task2CurrentCount = task2Count,
            task2MinAmountPerTx = 800.0,
            task2IsCompleted = task2Done,
            isReadyToUpgrade = isReadyToUpgrade,
            effectiveUpgradeMonth = effectiveUpgradeMonth,
            isMaxLevel = isMaxLevel
        )

        val guruDashboardState = GuruDashboardState(
            hasEnabledGuru = hasEnabledGuru,
            currentCycleName = currentCycleName,
            allCycles = allCycles,
            activeTier = activeTier,
            activeCycleTiers = activeCycleTiers,
            allTiers = allTierStatuses,
            upgradeTask = upgradeTask,
            totalHistoricalGuruRC = totalHistoricalGuruRC
        )

        val curYearPulseH1 = pulseH1ByYear[curYear] ?: 0.0
        val curYearPulseH2 = pulseH2ByYear[curYear] ?: 0.0
        val curYearPulseTotal = if (settings.pulseResetMidYear) {
            if (targetMonth.monthValue <= 6) curYearPulseH1 else curYearPulseH2
        } else {
            pulseYearlyByYear[curYear] ?: 0.0
        }
        val curYearRed = redByYear[curYear] ?: 0.0
        val isH2DiningActive = (curYear == 2026 && targetMonth.monthValue >= 7) || (curYear > 2026 && targetMonth.monthValue >= 7)

        val summary = DashboardSummary(
            totalUnlockedRC = totalUnlockedRC,
            totalPendingRC = totalPendingRC,
            totalSpend = totalSpend,
            avgRate = avgRate,
            welcomeSpend = welcomeCumSpend,
            welcomeThreshold = welcomeThreshold,
            welcomeRewardRC = totalWelcomeReward,
            welcomeAchieved = welcomeMilestoneTriggered || welcomeCumSpend >= welcomeThreshold,
            welcomeDaysLeft = daysLeft,
            targetYear = curYear,
            isH2DiningActive = isH2DiningActive,
            pulseUsedRC = curYearPulseTotal,
            pulseCapRC = PULSE_CAP,
            pulseH1UsedRC = curYearPulseH1,
            pulseH2UsedRC = curYearPulseH2,
            pulseResetMidYear = settings.pulseResetMidYear,
            redUsedRC = curYearRed,
            redCapRC = RED_CAP,
            rhCnStatus = rhCnStatus,
            allRhCnQuarters = allRhCnQuarters,
            monthStr = curMonthStr,
            monthTotalSpend = monthTotalSpend,
            monthDiningSpend = monthDiningSpend,
            monthDiningRC = monthDiningRC,
            diningMonthlyCap = diningCap,
            diningMinSpend = diningMinSpend,
            diningRate = dRatePercent,
            isMonthDiningUnlocked = isMonthDiningUnlocked,
            guruState = guruDashboardState
        )

        // 5. 倒序时间轴用于 UI 展示 (仅展示当前选中月份)
        val allItems = mutableListOf<TimelineItem>().apply {
            val combined = processedTxs + systemAwards
            addAll(combined.filter { it.itemDateTime.startsWith(curMonthKey) })
            sortBy { it.itemDateTime }
            reverse()
        }

        return CalculationResult(
            transactions = processedTxs,
            systemAwards = systemAwards,
            timeline = allItems,
            summary = summary
        )
    }

    fun calculatePreview(
        currentTransactions: List<TransactionEntity>,
        tempTx: TransactionEntity,
        settings: UserSettings
    ): PreviewResult {
        if (tempTx.amount <= 0.0) {
            return PreviewResult(0.0, 0.0, 0.0, "输入金额与时间查看返现测算")
        }

        val testList = currentTransactions + tempTx
        val txMonth = runCatching { YearMonth.parse(tempTx.dateTime.substring(0, 7)) }.getOrDefault(YearMonth.now())
        val result = recalculate(testList, settings, txMonth)
        val simulated = result.transactions.firstOrNull { it.entity.id == tempTx.id }
            ?: return PreviewResult(0.0, 0.0, 0.0, "")

        val totalRC = simulated.unlockedRC
        val pendingRC = simulated.pendingRC
        val rate = ((totalRC + pendingRC) / tempTx.amount) * 100

        return PreviewResult(
            totalRC = totalRC,
            pendingRC = pendingRC,
            rate = rate,
            breakdownText = simulated.breakdown.joinToString(" · ")
        )
    }
}
