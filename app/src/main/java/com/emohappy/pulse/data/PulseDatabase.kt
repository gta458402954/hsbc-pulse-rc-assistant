package com.emohappy.pulse.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.emohappy.pulse.model.TransactionEntity

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: PulseDatabase? = null

        fun getInstance(context: Context): PulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PulseDatabase::class.java,
                    "pulse_tracker.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
