# HSBC Pulse RC 奖赏助手 (Android)

> 专门针对 **香港汇丰 Pulse 银联双币双币信用卡（HSBC Pulse UnionPay Diamond）** 打造的本土化、纯本地离线「奖赏钱」（RewardCash, RC）智能计算与额度追踪助手。

---

## ✨ 核心特性

- **💳 最红自主奖赏 (2.0% / 5X 额外)**
  - 追踪选择类别（如赏世界/赏中华/赏滋味等）年上限 **$2,000 RC** 额度池。
  - 自动检测是否超出上限并精准计算超出后的基础回赠。

- **⚡ Pulse 2% 移动支付特别奖赏 (额外 2.0%)**
  - 支持官方每半年度（上半年 / 7月1日年中重置）**$1,600 RC** 额度池追踪。
  - 自动过滤 Apple Pay / 云闪付 / 微信支付等移动支付渠道。

- **🇨🇳 最红中国内地签账 (RH CN Spend)**
  - 完整适配汇丰官方年中规则调整：
    - **上半年 (1 ~ 6 月)**：季度全品类消费满 **¥10,000** 享额外 **3%**，每季封顶 **$300 RC**（Q1/Q2 合计最高 $600 RC）。
    - **下半年 (7 ~ 12 月)**：每月内地累计总签账满 **¥1,200**，**餐饮品类**享额外 **3%**，每月封顶 **$80 RC**（半年最高 $480 RC）。
  - 记账或账单导入时自动依据消费月份无缝切换规则，杜绝重复计奖。

- **✈️ Travel Guru 旅人狂赏 (多轮次管理)**
  - 独家支持降级重新起跑的**多轮次狂赏管理**（第 1 轮、第 2 轮等）。
  - 支持 **Lv.1 GO 旅人 (3%)**、**Lv.2 GING 旅人 (4%)**、**Lv.3 GURU 旅人 (6%)** 等级追踪。
  - 外币消费门槛（如 3 笔 ≥ HK$1,500 机票/住宿预订、累计 HK$30,000 外币总签账）直观进度条呈现。

- **🔒 纯本地离线与隐私安全**
  - 无需注册、无需联网，个人财务与消费明细保存在本地 Android Room SQLite 数据库中，安全无忧。
  - 支持一键导出 JSON 备份与跨设备快速恢复。

- **📊 智能看板与落袋统计**
  - 区分「已落袋（已达标）」与「待达标预估」，实时计算综合实际到手返现率。
  - 进度条按月份/季度动态汇总剩余可用额度与离达标差距。

---

## 🛠️ 技术栈与架构

- **语言与构建**：Kotlin 2.0+ / Gradle (Kotlin DSL)
- **UI 框架**：Jetpack Compose + Material Design 3 (M3)
- **架构模式**：MVVM + Unidirectional Data Flow (UDF) + StateFlow
- **持久化**：AndroidX Room (SQLite ORM) + DataStore Preferences
- **测试覆盖**：JUnit 4 / 全套针对复杂达标门槛与边界封顶的单元测试集

---

## 🚀 编译与运行

### 前置要求
- Android Studio Ladybug (2024.2+) 或更高版本
- JDK 17+
- Android SDK 35 (最低支持 Android 8.0 / API 26)

### 本地编译与安装
```bash
# 克隆仓库
git clone https://github.com/gta458402954/hsbc-pulse-rc-assistant.git
cd hsbc-pulse-rc-assistant

# 运行单元测试
./gradlew testDebugUnitTest

# 编译并安装至连接的 Android 设备
./gradlew installDebug
```

---

## 📄 开源许可

本项目遵循 [MIT License](LICENSE) 许可开源。
