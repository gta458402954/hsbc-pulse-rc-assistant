package com.emohappy.pulse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.emohappy.pulse.data.PulseRepository
import com.emohappy.pulse.domain.PulseCalculatorEngine
import com.emohappy.pulse.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

data class MainUiState(
    val calculationResult: CalculationResult = CalculationResult(
        transactions = emptyList(),
        systemAwards = emptyList(),
        timeline = emptyList(),
        summary = DashboardSummary()
    ),
    val settings: UserSettings = UserSettings(),
    val inputAmount: String = "",
    val inputDateTime: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
    val selectedChannel: PaymentChannel = PaymentChannel.UNIONPAY_APP,
    val selectedCategory: ExpenseCategory = ExpenseCategory.DINING,
    val livePreview: PreviewResult = PreviewResult(0.0, 0.0, 0.0, "输入金额与时间查看返现测算"),
    val editingTransaction: TransactionEntity? = null,
    val isSettingsOpen: Boolean = false,
    val infoMessage: String? = null,
    val selectedDashboardMonth: YearMonth = YearMonth.now()
)

class MainViewModel(
    private val repository: PulseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var currentRawTransactions: List<TransactionEntity> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                repository.allTransactions,
                repository.settings,
                _uiState.map { it.selectedDashboardMonth }.distinctUntilChanged()
            ) { txs, settings, targetMonth ->
                currentRawTransactions = txs
                val calcResult = PulseCalculatorEngine.recalculate(txs, settings, targetMonth)
                Triple(calcResult, settings, targetMonth)
            }.collect { (calcResult, settings, _) ->
                _uiState.update { current ->
                    current.copy(
                        calculationResult = calcResult,
                        settings = settings
                    )
                }
                updateLivePreview()
            }
        }
    }

    fun onAmountChanged(amount: String) {
        _uiState.update { it.copy(inputAmount = amount) }
        updateLivePreview()
    }

    fun onDateTimeChanged(dateTime: String) {
        _uiState.update { it.copy(inputDateTime = dateTime) }
        updateLivePreview()
    }

    fun onChannelSelected(channel: PaymentChannel) {
        _uiState.update { it.copy(selectedChannel = channel) }
        updateLivePreview()
    }

    fun onCategorySelected(category: ExpenseCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
        updateLivePreview()
    }

    private fun updateLivePreview() {
        val state = _uiState.value
        val amt = state.inputAmount.toDoubleOrNull() ?: 0.0
        val tempTx = TransactionEntity(
            id = "temp_preview",
            dateTime = state.inputDateTime,
            amount = amt,
            channel = state.selectedChannel.code,
            category = state.selectedCategory.code
        )
        val preview = PulseCalculatorEngine.calculatePreview(
            currentTransactions = currentRawTransactions,
            tempTx = tempTx,
            settings = state.settings
        )
        _uiState.update { it.copy(livePreview = preview) }
    }

    fun addTransaction() {
        val state = _uiState.value
        val amt = state.inputAmount.toDoubleOrNull() ?: return
        if (amt <= 0.0) return

        val newTx = TransactionEntity(
            id = System.currentTimeMillis().toString() + "_" + UUID.randomUUID().toString().take(4),
            dateTime = state.inputDateTime,
            amount = amt,
            channel = state.selectedChannel.code,
            category = state.selectedCategory.code
        )

        viewModelScope.launch {
            repository.addTransaction(newTx)
            _uiState.update {
                it.copy(
                    inputAmount = ""
                    // 不再重置 inputDateTime 为当前时间，保留用户上次选择的时间，方便连续补录同一天的记录
                )
            }
            updateLivePreview()
        }
    }

    fun startEdit(tx: TransactionEntity) {
        _uiState.update { it.copy(editingTransaction = tx) }
    }

    fun dismissEdit() {
        _uiState.update { it.copy(editingTransaction = null) }
    }

    fun saveEdit(edited: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(edited)
            _uiState.update { it.copy(editingTransaction = null) }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun saveSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            closeSettings()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
            closeSettings()
        }
    }

    fun importJson(content: String, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = repository.importJson(content)
            onComplete(result)
        }
    }

    fun exportJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val jsonStr = repository.exportJson()
            onResult(jsonStr)
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun showInfoMessage(msg: String) {
        _uiState.update { it.copy(infoMessage = msg) }
    }

    fun previousDashboardMonth() {
        _uiState.update { it.copy(selectedDashboardMonth = it.selectedDashboardMonth.minusMonths(1)) }
    }

    fun nextDashboardMonth() {
        _uiState.update { it.copy(selectedDashboardMonth = it.selectedDashboardMonth.plusMonths(1)) }
    }

    companion object {
        fun provideFactory(repository: PulseRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
    }
}
