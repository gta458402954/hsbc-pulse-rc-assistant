package com.emohappy.pulse

import android.app.Application
import com.emohappy.pulse.data.PulseDatabase
import com.emohappy.pulse.data.PulseRepository
import com.emohappy.pulse.data.SettingsDataStore

class PulseApplication : Application() {

    lateinit var repository: PulseRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = PulseDatabase.getInstance(this)
        val dataStore = SettingsDataStore(this)
        val autoBackupManager = com.emohappy.pulse.data.AutoBackupManager(this)
        repository = PulseRepository(database.transactionDao(), dataStore, autoBackupManager)
    }
}
