package com.sami.tradingchallengetracker.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TradingRepository(private val db: AppDatabase) {

    private val challengeDao = db.challengeDao()
    private val tradeDao = db.tradeDao()
    private val milestoneDao = db.milestoneDao()

    val activeChallengeFlow: Flow<ChallengeEntity?> = challengeDao.getActiveChallengeFlow()

    fun getTradesFlow(challengeId: String): Flow<List<TradeEntity>> =
        tradeDao.getTradesFlow(challengeId)

    fun getMilestonesFlow(challengeId: String): Flow<List<MilestoneEntity>> =
        milestoneDao.getMilestonesFlow(challengeId)

    suspend fun ensureActiveChallenge(): ChallengeEntity {
        var challenge = challengeDao.getActiveChallenge()
        if (challenge == null) {
            val newId = UUID.randomUUID().toString()
            challenge = ChallengeEntity(
                id = newId,
                userName = "",
                initialCapitalCents = 10000L,
                currentBalanceCents = 10000L,
                targetBalanceCents = 300000L,
                tradeCount = 0,
                challengeStarted = false
            )
            db.withTransaction {
                challengeDao.insertOrUpdate(challenge)
                initMilestonesForChallenge(newId)
            }
        }
        return challenge
    }

    private suspend fun initMilestonesForChallenge(challengeId: String) {
        val list = mutableListOf<MilestoneEntity>()
        for (i in 1..150) {
            list.add(
                MilestoneEntity(
                    id = "${challengeId}_milestone_$i",
                    challengeId = challengeId,
                    milestoneNumber = i,
                    profitCents = null,
                    balanceCents = null,
                    status = MilestoneStatus.PENDING,
                    tradeId = null,
                    timestamp = null,
                    attachmentPath = null
                )
            )
        }
        milestoneDao.insertAll(list)
    }

    suspend fun saveSettings(userName: String, initialCapitalCents: Long, targetBalanceCents: Long) {
        val current = ensureActiveChallenge()
        val updated = current.copy(
            userName = userName.trim(),
            targetBalanceCents = targetBalanceCents,
            initialCapitalCents = if (!current.challengeStarted) initialCapitalCents else current.initialCapitalCents,
            currentBalanceCents = if (!current.challengeStarted) initialCapitalCents else current.currentBalanceCents,
            updatedAt = System.currentTimeMillis()
        )
        challengeDao.insertOrUpdate(updated)
    }

    suspend fun startChallenge(): ChallengeEntity {
        val current = ensureActiveChallenge()
        val started = current.copy(
            challengeStarted = true,
            currentBalanceCents = current.initialCapitalCents,
            tradeCount = 0,
            updatedAt = System.currentTimeMillis()
        )
        db.withTransaction {
            tradeDao.deleteForChallenge(current.id)
            milestoneDao.deleteForChallenge(current.id)
            initMilestonesForChallenge(current.id)
            challengeDao.insertOrUpdate(started)
        }
        return started
    }

    suspend fun recordTrade(
        resultCents: Long,
        attachmentPath: String?
    ): TradeEntity = db.withTransaction {
        val challenge = challengeDao.getActiveChallenge()
            ?: throw IllegalStateException("يجب بدء التحدي أولاً.")

        if (!challenge.challengeStarted) {
            throw IllegalStateException("يجب بدء التحدي أولاً.")
        }
        if (challenge.tradeCount >= 150) {
            throw IllegalStateException("تم إكمال جميع محطات التحدي الـ150.")
        }
        if (resultCents == 0L) {
            throw IllegalArgumentException("لا يمكن أن تكون نتيجة الصفقة صفراً.")
        }

        val oldBalance = challenge.currentBalanceCents
        val newBalance = oldBalance + resultCents
        val tradeNumber = challenge.tradeCount + 1
        val type = if (resultCents > 0) TradeType.WIN else TradeType.LOSS
        val now = System.currentTimeMillis()
        val tradeId = UUID.randomUUID().toString()

        val trade = TradeEntity(
            id = tradeId,
            challengeId = challenge.id,
            tradeNumber = tradeNumber,
            resultCents = resultCents,
            oldBalanceCents = oldBalance,
            newBalanceCents = newBalance,
            type = type,
            attachmentPath = attachmentPath,
            timestamp = now
        )

        val milestone = MilestoneEntity(
            id = "${challenge.id}_milestone_$tradeNumber",
            challengeId = challenge.id,
            milestoneNumber = tradeNumber,
            profitCents = resultCents,
            balanceCents = newBalance,
            status = if (type == TradeType.WIN) MilestoneStatus.WIN else MilestoneStatus.LOSS,
            tradeId = tradeId,
            timestamp = now,
            attachmentPath = attachmentPath
        )

        tradeDao.insert(trade)
        milestoneDao.update(milestone)
        challengeDao.updateBalanceAndCount(challenge.id, newBalance, tradeNumber, now)

        trade
    }

    suspend fun resetChallenge() = db.withTransaction {
        val current = ensureActiveChallenge()
        tradeDao.deleteForChallenge(current.id)
        milestoneDao.deleteForChallenge(current.id)
        initMilestonesForChallenge(current.id)

        val reset = current.copy(
            currentBalanceCents = current.initialCapitalCents,
            tradeCount = 0,
            updatedAt = System.currentTimeMillis()
        )
        challengeDao.insertOrUpdate(reset)
    }

    suspend fun startNewChallenge(newInitialCapitalCents: Long) = db.withTransaction {
        val current = ensureActiveChallenge()
        val newId = UUID.randomUUID().toString()
        val newChallenge = ChallengeEntity(
            id = newId,
            userName = current.userName,
            initialCapitalCents = newInitialCapitalCents,
            currentBalanceCents = newInitialCapitalCents,
            targetBalanceCents = current.targetBalanceCents,
            tradeCount = 0,
            challengeStarted = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        challengeDao.insertOrUpdate(newChallenge)
        initMilestonesForChallenge(newId)
    }

    suspend fun getTradesForPdf(challengeId: String): List<TradeEntity> =
        tradeDao.getTradesAsc(challengeId)
}
