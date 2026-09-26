package com.sami.tradingchallengetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sami.tradingchallengetracker.data.AppDatabase
import com.sami.tradingchallengetracker.data.ChallengeEntity
import com.sami.tradingchallengetracker.data.MilestoneEntity
import com.sami.tradingchallengetracker.data.TradeEntity
import com.sami.tradingchallengetracker.data.TradingRepository
import com.sami.tradingchallengetracker.util.PdfExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TradingRepository(AppDatabase.getDatabase(application))

    val challenge: StateFlow<ChallengeEntity?> = repository.activeChallengeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trades: StateFlow<List<TradeEntity>> = challenge.flatMapLatest { ch ->
        if (ch == null) flowOf(emptyList())
        else repository.getTradesFlow(ch.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val milestones: StateFlow<List<MilestoneEntity>> = challenge.flatMapLatest { ch ->
        if (ch == null) flowOf(emptyList())
        else repository.getMilestonesFlow(ch.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureActiveChallenge()
        }
    }

    fun saveSettings(userName: String, initialCapitalCents: Long, targetBalanceCents: Long) {
        viewModelScope.launch {
            repository.saveSettings(userName, initialCapitalCents, targetBalanceCents)
        }
    }

    fun startChallenge() {
        viewModelScope.launch {
            repository.startChallenge()
        }
    }

    fun recordTrade(resultCents: Long, attachmentPath: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.recordTrade(resultCents, attachmentPath)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء حفظ الصفقة")
            }
        }
    }

    fun resetChallenge(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.resetChallenge()
            onDone()
        }
    }

    fun startNewChallenge(newCapitalCents: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.startNewChallenge(newCapitalCents)
            onDone()
        }
    }

    fun sharePdfReport() {
        val currentChallenge = challenge.value ?: return
        viewModelScope.launch {
            val allTrades = repository.getTradesForPdf(currentChallenge.id)
            PdfExporter.shareReport(getApplication(), currentChallenge, allTrades)
        }
    }
}
