package com.emohappy.pulse.model

enum class ExpenseCategory(val code: String, val displayName: String, val icon: String) {
    DINING("dining", "内地餐饮", "🍲"),
    DAILY("daily", "日常消费", "🛍️"),
    TRAVEL("travel", "旅行", "✈️");

    companion object {
        fun fromCode(code: String): ExpenseCategory {
            return entries.firstOrNull { it.code == code || (it == TRAVEL && code == "travel_booking") } ?: DINING
        }
    }
}
