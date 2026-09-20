package com.emohappy.pulse.domain

import com.emohappy.pulse.model.*
import org.junit.Assert.*
import org.junit.Test

class PulseCalculatorEngineTest {

    @Test
    fun testStandardMobileUnionPayDiningWithThresholdUnlocked() {
        val settings = UserSettings(
            redReward = true,
            rhCnSpend = false,
            chinaDining = true,
            diningRate = 3.0,
            diningMonthlyCap = 80.0,
            diningMinSpend = 1200.0,
            welcome = false
        )

        // 交易1：¥1200 日常消费 (解锁当月门槛)
        val tx1 = TransactionEntity(
            id = "1",
            dateTime = "2026-09-01T10:00",
            amount = 1200.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.DAILY.code
        )

        // 交易2：¥200 内地餐饮
        val tx2 = TransactionEntity(
            id = "2",
            dateTime = "2026-09-02T12:00",
            amount = 200.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.DINING.code
        )

        val result = PulseCalculatorEngine.recalculate(listOf(tx1, tx2), settings)

        // 交易2 返现率应为 0.4% + 2% + 2% + 3% = 7.4%
        // 200 * 7.4% = 14.8 RC
        val diningTx = result.transactions.first { it.entity.id == "2" }
        assertEquals(14.8, diningTx.unlockedRC, 0.001)
        assertEquals(0.0, diningTx.pendingRC, 0.001)
    }

    @Test
    fun testDiningPendingWhenMonthThresholdNotMet() {
        val settings = UserSettings(
            redReward = true,
            rhCnSpend = false,
            chinaDining = true,
            diningMinSpend = 1200.0
        )

        // 仅一笔 ¥500 内地餐饮，未满 1200
        val tx = TransactionEntity(
            id = "1",
            dateTime = "2026-09-01T10:00",
            amount = 500.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.DINING.code
        )

        val result = PulseCalculatorEngine.recalculate(listOf(tx), settings)
        val processed = result.transactions.first()

        // 基础 0.4% + Pulse 2% + 最红 2% = 4.4% = 22.0 RC 已到手
        // 餐饮 3% = 15.0 RC 待达标
        assertEquals(22.0, processed.unlockedRC, 0.001)
        assertEquals(15.0, processed.pendingRC, 0.001)
    }

    @Test
    fun testWechatAlipayExclusion() {
        val settings = UserSettings(
            redReward = true,
            rhCnSpend = false,
            chinaDining = true
        )

        val tx = TransactionEntity(
            id = "1",
            dateTime = "2026-09-01T10:00",
            amount = 1000.0,
            channel = PaymentChannel.WECHAT_ALIPAY.code,
            category = ExpenseCategory.DINING.code
        )

        val result = PulseCalculatorEngine.recalculate(listOf(tx), settings)
        val processed = result.transactions.first()

        // 微信支付宝：仅享 基础 0.4% (4.0) + 最红 2% (20.0) = 24.0 RC
        // 无法享受餐饮 3% 以及 Pulse 2%
        assertEquals(24.0, processed.unlockedRC, 0.001)
        assertEquals(0.0, processed.pendingRC, 0.001)
    }

    @Test
    fun testWelcomeOfferMilestone() {
        val settings = UserSettings(
            welcome = true,
            cardIssueDate = "2026-09-01",
            welcomeSpendThreshold = 8000.0,
            hasReferralCode = true // 1800 RC
        )

        val tx1 = TransactionEntity(
            id = "1",
            dateTime = "2026-09-05T10:00",
            amount = 5000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.DAILY.code
        )

        val tx2 = TransactionEntity(
            id = "2",
            dateTime = "2026-09-10T10:00",
            amount = 3500.0, // 累积满 8500
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.DAILY.code
        )

        val result = PulseCalculatorEngine.recalculate(listOf(tx1, tx2), settings)
        assertEquals(1, result.systemAwards.size)
        val award = result.systemAwards.first()
        assertEquals(1800.0, award.totalRC, 0.001)
        assertTrue(award.title.contains("迎新礼"))
        assertTrue(result.summary.welcomeAchieved)
    }

    @Test
    fun testGuruIndependentIntervalsAndCustomRewards() {
        val settings = UserSettings(
            welcome = false,
            redReward = false,
            chinaDining = false,
            guruConfigs = listOf(
                // Lv1: 1月~2月生效，返现 3%，上限 500 RC
                GuruLevelConfig(level = 1, enabled = true, startDate = "2026-01-01", endDate = "2026-02-28", ratePercent = 3.0, capRC = 500.0),
                // Lv2: 延迟到 4月15日才升级生效，自定义 5% 返现，上限 1500 RC
                GuruLevelConfig(level = 2, enabled = true, startDate = "2026-04-15", endDate = "2026-12-31", ratePercent = 5.0, capRC = 1500.0)
            )
        )

        // 交易 1：2026-01-15 消费 ¥10,000 (命中 Lv1 3%，应获 300 RC)
        val tx1 = TransactionEntity(
            id = "1",
            dateTime = "2026-01-15T10:00",
            amount = 10000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.TRAVEL.code
        )

        // 交易 2：2026-03-20 消费 ¥10,000 (未达标空窗期，Lv1 已结束且 Lv2 尚未生效，Guru 返现应为 0)
        val tx2 = TransactionEntity(
            id = "2",
            dateTime = "2026-03-20T10:00",
            amount = 10000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.TRAVEL.code
        )

        // 交易 3：2026-04-20 消费 ¥10,000 (成功升级生效 Lv2，自定义 5%，应获 500 RC)
        val tx3 = TransactionEntity(
            id = "3",
            dateTime = "2026-04-20T10:00",
            amount = 10000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.TRAVEL.code
        )

        val result = PulseCalculatorEngine.recalculate(listOf(tx1, tx2, tx3), settings)

        val p1 = result.transactions.first { it.entity.id == "1" }
        assertEquals(300.0, p1.breakdownDetail.rcGuru, 0.001)

        val p2 = result.transactions.first { it.entity.id == "2" }
        assertEquals(0.0, p2.breakdownDetail.rcGuru, 0.001)

        val p3 = result.transactions.first { it.entity.id == "3" }
        assertEquals(500.0, p3.breakdownDetail.rcGuru, 0.001)
    }

    @Test
    fun testMultiCycleTravelGuruWithDowngradeReset() {
        val settings = UserSettings(
            welcome = false,
            redReward = false,
            chinaDining = false,
            guruStages = listOf(
                GuruStagePeriod(
                    id = "c1_lv1",
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
                    id = "c1_lv2",
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
                    id = "c2_lv1",
                    cycleName = "第 2 轮",
                    level = 1,
                    enabled = true,
                    startDate = "2026-02-01",
                    endDate = "",
                    ratePercent = 3.0,
                    capRC = 500.0,
                    isDowngradeReset = true
                )
            )
        )

        // 历史第1轮消费 ¥20,000 (3% 应拿满 500 RC 封顶)
        val txOld = TransactionEntity(
            id = "c1_tx",
            dateTime = "2024-10-15T10:00",
            amount = 20000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.TRAVEL.code
        )

        // 第2轮降级重刷后消费 ¥10,000 (全新 500 RC 上限，应正常拿到 300 RC)
        val txNew = TransactionEntity(
            id = "c2_tx",
            dateTime = "2026-02-15T10:00",
            amount = 10000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.TRAVEL.code
        )

        val result = PulseCalculatorEngine.recalculate(listOf(txOld, txNew), settings)

        val pOld = result.transactions.first { it.entity.id == "c1_tx" }
        assertEquals(500.0, pOld.breakdownDetail.rcGuru, 0.001)

        val pNew = result.transactions.first { it.entity.id == "c2_tx" }
        assertEquals(300.0, pNew.breakdownDetail.rcGuru, 0.001)

        // 升级任务验证：旧消费不应计入第2轮升级任务
        val upgradeTask = result.summary.guruState.upgradeTask
        assertEquals("第 2 轮", upgradeTask.currentCycleName)
        assertEquals("2026-02-01", upgradeTask.cycleStartDate)
        assertEquals(10000.0, upgradeTask.task1CurrentSpend, 0.001)
        assertEquals(1, upgradeTask.task2CurrentCount)
        assertFalse(upgradeTask.task1IsCompleted)
    }

    @Test
    fun testRhCnMidYearResetAndPhaseNaming() {
        val settings = UserSettings(
            redReward = false,
            rhCnSpend = true,
            welcome = false,
            rhCnH1Config = RhCnHalfYearConfig(
                halfYearName = "上半年 (H1)",
                enabled = true,
                regDate = "2026-01-01",
                ratePercent = 3.0,
                quarterCapRC = 300.0,
                quarterSpendThreshold = 10000.0
            ),
            diningMonthlyCap = 80.0,
            diningMinSpend = 1200.0,
            diningRate = 3.0
        )

        // Q1 (H1 P1): ¥15,000 日常消费 -> 3% = 450, 封顶 300 RC
        val txQ1 = TransactionEntity(
            id = "q1_tx",
            dateTime = "2026-02-10T10:00",
            amount = 15000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.DAILY.code
        )

        // Q2 (H1 P2): ¥12,000 日常消费 -> 3% = 360, 封顶 300 RC
        val txQ2 = TransactionEntity(
            id = "q2_tx",
            dateTime = "2026-05-10T10:00",
            amount = 12000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.DAILY.code
        )

        // Q3 (H2): 8月日常消费 ¥2,000 (下半年规则调整：非餐饮不享受最红内地额外 3%)
        val txQ3Daily = TransactionEntity(
            id = "q3_daily",
            dateTime = "2026-08-10T10:00",
            amount = 2000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.DAILY.code
        )

        // Q3 (H2): 8月餐饮消费 ¥3,000 (当月内地总签账满 1200，餐饮享 3%，月封顶 80 RC)
        val txQ3Dining = TransactionEntity(
            id = "q3_dining",
            dateTime = "2026-08-15T12:00",
            amount = 3000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.DINING.code
        )

        val result = PulseCalculatorEngine.recalculate(listOf(txQ1, txQ2, txQ3Daily, txQ3Dining), settings)

        val pQ1 = result.transactions.first { it.entity.id == "q1_tx" }
        assertEquals(300.0, pQ1.breakdownDetail.rcRhCn, 0.001)

        val pQ2 = result.transactions.first { it.entity.id == "q2_tx" }
        assertEquals(300.0, pQ2.breakdownDetail.rcRhCn, 0.001)

        val pQ3Daily = result.transactions.first { it.entity.id == "q3_daily" }
        assertEquals(0.0, pQ3Daily.breakdownDetail.rcRhCn, 0.001)

        val pQ3Dining = result.transactions.first { it.entity.id == "q3_dining" }
        assertEquals(80.0, pQ3Dining.breakdownDetail.rcRhCn, 0.001)
        assertEquals(80.0, pQ3Dining.breakdownDetail.rcDining, 0.001)

        // 验证季度命名与阶段
        val qList = result.summary.allRhCnQuarters
        assertEquals(4, qList.size)
        assertTrue(qList[0].quarterName.contains("H1 P1"))
        assertTrue(qList[1].quarterName.contains("H1 P2"))
        assertTrue(qList[2].quarterName.contains("H2"))
    }

    @Test
    fun testPulse2PercentMidYearReset() {
        val settings = UserSettings(
            redReward = false,
            rhCnSpend = false,
            chinaDining = false,
            welcome = false,
            pulseResetMidYear = true
        )

        // 上半年 3月：刷 ¥90,000 Apple Pay (2% = 1800 -> 封顶 1600 RC)
        val txH1 = TransactionEntity(
            id = "pulse_h1",
            dateTime = "2026-03-20T10:00",
            amount = 90000.0,
            channel = PaymentChannel.APPLE_PAY.code,
            category = ExpenseCategory.DAILY.code
        )

        // 下半年 8月：刷 ¥50,000 Apple Pay (年中已重置，再享 2% = 1000 RC)
        val txH2 = TransactionEntity(
            id = "pulse_h2",
            dateTime = "2026-08-20T10:00",
            amount = 50000.0,
            channel = PaymentChannel.APPLE_PAY.code,
            category = ExpenseCategory.DAILY.code
        )

        val result = PulseCalculatorEngine.recalculate(listOf(txH1, txH2), settings)

        val pH1 = result.transactions.first { it.entity.id == "pulse_h1" }
        assertEquals(1600.0, pH1.breakdownDetail.rcPulse, 0.001)

        val pH2 = result.transactions.first { it.entity.id == "pulse_h2" }
        assertEquals(1000.0, pH2.breakdownDetail.rcPulse, 0.001)

        assertEquals(1600.0, result.summary.pulseH1UsedRC, 0.001)
        assertEquals(1000.0, result.summary.pulseH2UsedRC, 0.001)
    }

    @Test
    fun testMultiCycleGuruStagesMatchingAndCap() {
        val settings = UserSettings(
            redReward = false,
            rhCnSpend = false,
            chinaDining = false,
            welcome = false,
            guruStages = UserSettings.DEFAULT_GURU_STAGES
        )

        // 1. 第 1 轮 Lv.1: 2024-10-15 订机票 ¥10,000 (3% = 300 RC, cap 500)
        val txR1Lv1 = TransactionEntity(
            id = "tx_r1_lv1",
            dateTime = "2024-10-15T10:00",
            amount = 10000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.TRAVEL.code
        )

        // 2. 第 1 轮 Lv.2: 2025-03-15 酒店 ¥20,000 (4% = 800 RC, cap 1200)
        val txR1Lv2 = TransactionEntity(
            id = "tx_r1_lv2",
            dateTime = "2025-03-15T10:00",
            amount = 20000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.TRAVEL.code
        )

        // 3. 第 1 轮 Lv.3: 2025-10-15 机票 ¥30,000 (6% = 1800 RC, cap 2200)
        val txR1Lv3 = TransactionEntity(
            id = "tx_r1_lv3",
            dateTime = "2025-10-15T10:00",
            amount = 30000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.TRAVEL.code
        )

        // 4. 第 2 轮 Lv.1 (降级重刷): 2026-08-10 机票 ¥20,000 (3% = 600 -> 封顶 500 RC)
        val txR2Lv1 = TransactionEntity(
            id = "tx_r2_lv1",
            dateTime = "2026-08-10T10:00",
            amount = 20000.0,
            channel = PaymentChannel.UNIONPAY_APP.code,
            category = ExpenseCategory.TRAVEL.code
        )

        val result = PulseCalculatorEngine.recalculate(
            listOf(txR1Lv1, txR1Lv2, txR1Lv3, txR2Lv1),
            settings
        )

        val pR1Lv1 = result.transactions.first { it.entity.id == "tx_r1_lv1" }
        assertEquals(300.0, pR1Lv1.breakdownDetail.rcGuru, 0.001)

        val pR1Lv2 = result.transactions.first { it.entity.id == "tx_r1_lv2" }
        assertEquals(800.0, pR1Lv2.breakdownDetail.rcGuru, 0.001)

        val pR1Lv3 = result.transactions.first { it.entity.id == "tx_r1_lv3" }
        assertEquals(1800.0, pR1Lv3.breakdownDetail.rcGuru, 0.001)

        val pR2Lv1 = result.transactions.first { it.entity.id == "tx_r2_lv1" }
        assertEquals(500.0, pR2Lv1.breakdownDetail.rcGuru, 0.001) // 满额封顶 500 RC

        // 验证多周期总览
        val guruState = result.summary.guruState
        assertEquals(2, guruState.allCycles.size)
        assertTrue(guruState.allCycles.contains("第 1 轮"))
        assertTrue(guruState.allCycles.contains("第 2 轮"))
        assertEquals(3400.0, guruState.totalHistoricalGuruRC, 0.001) // 300 + 800 + 1800 + 500 = 3400
    }
}
