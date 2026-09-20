package com.emohappy.pulse.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.emohappy.pulse.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "pulse_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val RED_REWARD = booleanPreferencesKey("red_reward")
        val RED_REG_DATE = stringPreferencesKey("red_reg_date")
        val CHINA_DINING = booleanPreferencesKey("china_dining")
        val CHINA_DINING_DATE = stringPreferencesKey("china_dining_date")
        val DINING_RATE = doublePreferencesKey("dining_rate")
        val DINING_MONTHLY_CAP = doublePreferencesKey("dining_monthly_cap")
        val DINING_MIN_SPEND = doublePreferencesKey("dining_min_spend")
        val WELCOME = booleanPreferencesKey("welcome")
        val CARD_ISSUE_DATE = stringPreferencesKey("card_issue_date")
        val WELCOME_SPEND_THRESHOLD = doublePreferencesKey("welcome_spend_threshold")
        val HAS_REFERRAL_CODE = booleanPreferencesKey("has_referral_code")
        val GURU_LEVEL = intPreferencesKey("guru_level")
        val GURU_REG_DATE = stringPreferencesKey("guru_reg_date")
        val GURU_CONFIGS_JSON = stringPreferencesKey("guru_configs_json")
        val GURU_STAGES_JSON = stringPreferencesKey("guru_stages_json")
        val CHINA_DINING_END_DATE = stringPreferencesKey("china_dining_end_date")
        val RH_CN_SPEND = booleanPreferencesKey("rh_cn_spend")
        val RH_CN_H1_JSON = stringPreferencesKey("rh_cn_h1_json")
        val RH_CN_H2_JSON = stringPreferencesKey("rh_cn_h2_json")
        val PULSE_RESET_MID_YEAR = booleanPreferencesKey("pulse_reset_mid_year")
    }

    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val savedGuruConfigs = prefs[Keys.GURU_CONFIGS_JSON]?.let {
            runCatching {
                json.decodeFromString<List<com.emohappy.pulse.model.GuruLevelConfig>>(it)
            }.getOrNull()
        } ?: listOf(
            com.emohappy.pulse.model.GuruLevelConfig(level = 1, enabled = true, startDate = "2026-01-01", endDate = "", ratePercent = 3.0, capRC = 500.0),
            com.emohappy.pulse.model.GuruLevelConfig(level = 2, enabled = false, startDate = "2026-04-01", endDate = "", ratePercent = 4.0, capRC = 1200.0),
            com.emohappy.pulse.model.GuruLevelConfig(level = 3, enabled = false, startDate = "2026-07-01", endDate = "", ratePercent = 6.0, capRC = 2200.0)
        )

        val defaultStages = UserSettings.DEFAULT_GURU_STAGES
        val savedGuruStages = prefs[Keys.GURU_STAGES_JSON]?.let {
            runCatching {
                json.decodeFromString<List<com.emohappy.pulse.model.GuruStagePeriod>>(it)
            }.getOrNull()
        } ?: defaultStages

        val savedH1Config = prefs[Keys.RH_CN_H1_JSON]?.let {
            runCatching {
                json.decodeFromString<com.emohappy.pulse.model.RhCnHalfYearConfig>(it)
            }.getOrNull()
        } ?: com.emohappy.pulse.model.RhCnHalfYearConfig(
            halfYearName = "上半年 (H1)",
            enabled = true,
            regDate = "2026-01-01",
            ratePercent = 3.0,
            quarterCapRC = 300.0,
            quarterSpendThreshold = 10000.0
        )

        val savedH2Config = prefs[Keys.RH_CN_H2_JSON]?.let {
            runCatching {
                json.decodeFromString<com.emohappy.pulse.model.RhCnHalfYearConfig>(it)
            }.getOrNull()
        } ?: com.emohappy.pulse.model.RhCnHalfYearConfig(
            halfYearName = "下半年 (H2 · 年中重置)",
            enabled = true,
            regDate = "2026-07-01",
            ratePercent = 3.0,
            quarterCapRC = 500.0,
            quarterSpendThreshold = 10000.0
        )

        UserSettings(
            redReward = prefs[Keys.RED_REWARD] ?: true,
            redRegDate = prefs[Keys.RED_REG_DATE] ?: "2026-01-01",
            rhCnSpend = prefs[Keys.RH_CN_SPEND] ?: true,
            rhCnH1Config = savedH1Config,
            rhCnH2Config = savedH2Config,
            pulseResetMidYear = prefs[Keys.PULSE_RESET_MID_YEAR] ?: true,
            chinaDining = prefs[Keys.CHINA_DINING] ?: true,
            chinaDiningDate = prefs[Keys.CHINA_DINING_DATE] ?: "2026-07-01",
            chinaDiningEndDate = prefs[Keys.CHINA_DINING_END_DATE] ?: "2026-12-31",
            diningRate = prefs[Keys.DINING_RATE] ?: 3.0,
            diningMonthlyCap = prefs[Keys.DINING_MONTHLY_CAP] ?: 80.0,
            diningMinSpend = prefs[Keys.DINING_MIN_SPEND] ?: 1200.0,
            welcome = prefs[Keys.WELCOME] ?: true,
            cardIssueDate = prefs[Keys.CARD_ISSUE_DATE] ?: LocalDate.now().toString(),
            welcomeSpendThreshold = prefs[Keys.WELCOME_SPEND_THRESHOLD] ?: 8000.0,
            hasReferralCode = prefs[Keys.HAS_REFERRAL_CODE] ?: true,
            guruLevel = prefs[Keys.GURU_LEVEL] ?: 1,
            guruRegDate = prefs[Keys.GURU_REG_DATE] ?: "2026-01-01",
            guruConfigs = savedGuruConfigs,
            guruStages = savedGuruStages
        )
    }

    suspend fun saveSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RED_REWARD] = settings.redReward
            prefs[Keys.RED_REG_DATE] = settings.redRegDate
            prefs[Keys.RH_CN_SPEND] = settings.rhCnSpend
            prefs[Keys.RH_CN_H1_JSON] = json.encodeToString(settings.rhCnH1Config)
            prefs[Keys.RH_CN_H2_JSON] = json.encodeToString(settings.rhCnH2Config)
            prefs[Keys.PULSE_RESET_MID_YEAR] = settings.pulseResetMidYear
            prefs[Keys.CHINA_DINING] = settings.chinaDining
            prefs[Keys.CHINA_DINING_DATE] = settings.chinaDiningDate
            prefs[Keys.CHINA_DINING_END_DATE] = settings.chinaDiningEndDate
            prefs[Keys.DINING_RATE] = settings.diningRate
            prefs[Keys.DINING_MONTHLY_CAP] = settings.diningMonthlyCap
            prefs[Keys.DINING_MIN_SPEND] = settings.diningMinSpend
            prefs[Keys.WELCOME] = settings.welcome
            prefs[Keys.CARD_ISSUE_DATE] = settings.cardIssueDate
            prefs[Keys.WELCOME_SPEND_THRESHOLD] = settings.welcomeSpendThreshold
            prefs[Keys.HAS_REFERRAL_CODE] = settings.hasReferralCode
            prefs[Keys.GURU_LEVEL] = settings.guruLevel
            prefs[Keys.GURU_REG_DATE] = settings.guruRegDate
            prefs[Keys.GURU_CONFIGS_JSON] = json.encodeToString(settings.guruConfigs)
            prefs[Keys.GURU_STAGES_JSON] = json.encodeToString(settings.guruStages)
        }
    }
}
