package com.emohappy.pulse.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "transactions")
@Serializable
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val dateTime: String,
    val amount: Double,
    val channel: String,
    val category: String,
    val note: String = ""
)
