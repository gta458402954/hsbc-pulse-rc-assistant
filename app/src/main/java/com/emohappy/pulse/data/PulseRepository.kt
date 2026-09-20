package com.emohappy.pulse.data

import com.emohappy.pulse.model.PulseBackupDto
import com.emohappy.pulse.model.TransactionBackupDto
import com.emohappy.pulse.model.TransactionEntity
import com.emohappy.pulse.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PulseRepository(
    private val transactionDao: TransactionDao,
    private val settingsDataStore: SettingsDataStore,
    private val autoBackupManager: AutoBackupManager? = null
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val settings: Flow<UserSettings> = settingsDataStore.settingsFlow

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private suspend fun triggerAutoBackup() {
        runCatching {
            val jsonContent = exportJson()
            autoBackupManager?.saveAutoBackup(jsonContent)
        }
    }

    suspend fun getAvailableAutoBackup(): String? = autoBackupManager?.getAvailableBackup()
    suspend fun getAutoBackupCount(): Int = autoBackupManager?.getBackupTransactionCount() ?: 0

    suspend fun addTransaction(tx: TransactionEntity) {
        transactionDao.insert(tx)
        triggerAutoBackup()
    }

    suspend fun updateTransaction(tx: TransactionEntity) {
        transactionDao.update(tx)
        triggerAutoBackup()
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.deleteById(id)
        triggerAutoBackup()
    }

    suspend fun clearAll() {
        transactionDao.deleteAll()
        triggerAutoBackup()
    }

    suspend fun saveSettings(userSettings: UserSettings) {
        settingsDataStore.saveSettings(userSettings)
        triggerAutoBackup()
    }

    suspend fun exportJson(): String {
        val txList = transactionDao.getAllTransactions().first()
        val currentSettings = settingsDataStore.settingsFlow.first()
        val backupDto = PulseBackupDto(
            transactions = txList.map { TransactionBackupDto.fromEntity(it) },
            settings = currentSettings
        )
        return json.encodeToString(backupDto)
    }

    suspend fun importJson(jsonContent: String): Result<Int> {
        return runCatching {
            val backupDto = json.decodeFromString<PulseBackupDto>(jsonContent)
            if (backupDto.transactions.isNotEmpty()) {
                val entities = backupDto.transactions.map { it.toEntity() }
                transactionDao.insertAll(entities)
            }
            settingsDataStore.saveSettings(backupDto.settings)
            triggerAutoBackup()
            backupDto.transactions.size
        }
    }
}
