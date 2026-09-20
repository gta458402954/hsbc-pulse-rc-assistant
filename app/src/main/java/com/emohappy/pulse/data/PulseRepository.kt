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
    private val settingsDataStore: SettingsDataStore
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val settings: Flow<UserSettings> = settingsDataStore.settingsFlow

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    suspend fun addTransaction(tx: TransactionEntity) {
        transactionDao.insert(tx)
    }

    suspend fun updateTransaction(tx: TransactionEntity) {
        transactionDao.update(tx)
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.deleteById(id)
    }

    suspend fun clearAll() {
        transactionDao.deleteAll()
    }

    suspend fun saveSettings(userSettings: UserSettings) {
        settingsDataStore.saveSettings(userSettings)
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
            backupDto.transactions.size
        }
    }
}
