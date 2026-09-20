package com.emohappy.pulse.model

enum class PaymentChannel(val code: String, val displayName: String, val icon: String) {
    UNIONPAY_APP("unionpay_app", "你好中国 / 云闪付", "🟢"),
    APPLE_PAY("apple_pay", "Apple Pay (美团/京东)", "⚫"),
    WECHAT_ALIPAY("wechat_alipay", "微信 / 支付宝", "🔵"),
    PHYSICAL_POS("physical_pos", "实体卡刷卡", "⚪");

    val isMobileUnionPay: Boolean
        get() = this == UNIONPAY_APP || this == APPLE_PAY

    val isMicroPay: Boolean
        get() = this == WECHAT_ALIPAY

    companion object {
        fun fromCode(code: String): PaymentChannel {
            return entries.firstOrNull { it.code == code } ?: UNIONPAY_APP
        }
    }
}
