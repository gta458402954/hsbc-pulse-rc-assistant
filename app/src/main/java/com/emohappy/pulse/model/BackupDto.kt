package com.emohappy.pulse.model

import kotlinx.serialization.Serializable

@Serializable
data class PulseBackupDto(
    val transactions: List<TransactionBackupDto> = emptyList(),
    val settings: UserSettings = UserSettings()
)

@Serializable
data class TransactionBackupDto(
    val id: String,
    val dateTime: String,
    val amount: Double,
    val channel: String,
    val category: String,
    val note: String = ""
) {
    fun toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            dateTime = dateTime,
            amount = amount,
            channel = channel,
            category = category,
            note = note
        )
    }

    companion object {
        fun fromEntity(entity: TransactionEntity): TransactionBackupDto {
            return TransactionBackupDto(
                id = entity.id,
                dateTime = entity.dateTime,
                amount = entity.amount,
                channel = entity.channel,
                category = entity.category,
                note = entity.note
            )
        }
    }
}
