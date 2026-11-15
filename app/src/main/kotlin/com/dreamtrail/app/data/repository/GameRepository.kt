package com.dreamtrail.app.data.repository

import androidx.room.withTransaction
import com.dreamtrail.app.data.local.*
import com.dreamtrail.app.time.ClockProvider
import com.dreamtrail.app.time.DurationResolver
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.*
import com.dreamtrail.core.model.*
import com.dreamtrail.core.rng.DeterministicRng
import com.dreamtrail.core.save.SaveBackupCodec
import com.dreamtrail.core.save.SaveBackupPayload
import com.dreamtrail.core.sleep.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

data class SaveSnapshotSummary(
    val snapshotId: Long,
    val createdAtUtcMs: Long,
    val reason: String
)

data class AppUiDataState(
    val initialized: Boolean = false,
    val playerState: PlayerState = PlayerState(),
    val budgetState: RewardBudgetState = RewardBudgetState(),
    val discoveryState: DiscoveryState = DiscoveryState(),
    val inventory: List<EquipmentInstance> = emptyList(),
    val runningSession: ExpeditionSession? = null,
    val recentSettlements: List<SettlementResult> = emptyList(),
    val snapshots: List<SaveSnapshotSummary> = emptyList(),
    val settings: AppSettingsEntity = AppSettingsEntity()
)

class GameRepository(
    private val db: DreamTrailDatabase,
    private val clock: ClockProvider,
    val sleepProvider: SleepDataProvider = ManualOnlySleepDataProvider()
) {
    private val dao = db.dao()
    private val actionMutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val stateFlow: Flow<AppUiDataState> = combine(
        dao.observePlayerState(),
        dao.observeRewardBudget(),
        dao.observeDiscoveryState(),
        dao.observeInventory(),
        dao.observeRunningSession(),
        dao.observeRecentSettlements(20),
        dao.observeLatestSnapshots(),
        dao.observeSettings()
    ) { values ->
        val pEnt = values[0] as PlayerStateEntity?
        val bEnt = values[1] as RewardBudgetEntity?
        val dEnt = values[2] as DiscoveryStateEntity?
        @Suppress("UNCHECKED_CAST")
        val invEnts = values[3] as List<InventoryItemEntity>
        val runEnt = values[4] as ExpeditionSessionEntity?
        @Suppress("UNCHECKED_CAST")
        val settEnts = values[5] as List<SettlementEntity>
        @Suppress("UNCHECKED_CAST")
        val snapEnts = values[6] as List<SaveSnapshotEntity>
        val settingsEnt = values[7] as AppSettingsEntity?

        AppUiDataState(
            initialized = pEnt != null,
            playerState = pEnt?.let { decodePlayer(it.payloadJson) } ?: PlayerState(),
            budgetState = bEnt?.let { decodeBudget(it.payloadJson) } ?: RewardBudgetState(),
            discoveryState = dEnt?.let { decodeDiscovery(it.payloadJson) } ?: DiscoveryState(),
            inventory = invEnts.map { it.toModel() },
            runningSession = runEnt?.toModel(),
            recentSettlements = settEnts.mapNotNull { decodeSettlement(it.payloadJson) },
            snapshots = snapEnts.map { SaveSnapshotSummary(it.snapshotId, it.createdAtUtcMs, it.reason) },
            settings = settingsEnt ?: AppSettingsEntity()
        )
    }.flowOn(Dispatchers.IO)

    suspend fun ensureInitializedAndRecover(): ActionOutcome<SettlementResult?> = withContext(Dispatchers.IO) {
        actionMutex.withLock {
            db.withTransaction {
                if (dao.getPlayerState() == null) {
                    val defaultPlayer = PlayerState()
                    val defaultBudget = RewardBudgetState()
                    val defaultDiscovery = ProgressionEngine.reconcileUnlocks(defaultPlayer, DiscoveryState())
                    dao.upsertPlayerState(PlayerStateEntity(1, PlayerState.CURRENT_VERSION, encodePlayer(defaultPlayer)))
                    dao.upsertRewardBudget(RewardBudgetEntity(1, encodeBudget(defaultBudget)))
                    dao.upsertDiscoveryState(DiscoveryStateEntity(1, encodeDiscovery(defaultDiscovery)))
                    dao.upsertSettings(AppSettingsEntity())
                }
                val bundle = loadBundleInTx()
                val savedDiscovery = dao.getDiscoveryState()?.let { decodeDiscovery(it.payloadJson) }
                if (savedDiscovery != bundle.discoveryState) {
                    dao.upsertDiscoveryState(DiscoveryStateEntity(1, encodeDiscovery(bundle.discoveryState)))
                }
            }
            recoverPendingSettlementLocked()
        }
    }

    suspend fun startExpedition(routeId: String, strategy: ExpeditionStrategy): ActionOutcome<ExpeditionSession> =
        withContext(Dispatchers.IO) {
            actionMutex.withLock {
                // First recover any half-settled session that already fixed endAtUtcMs
                val recovery = recoverPendingSettlementLocked()
                if (recovery is ActionOutcome.Failure) {
                    return@withLock ActionOutcome.Failure(recovery.reason)
                }

                try {
                    val session = db.withTransaction {
                        val existing = dao.getRunningSession()
                        if (existing != null) {
                            // Idempotent double-tap handling: return active session
                            return@withTransaction existing.toModel()
                        }

                        var bundle = loadBundleInTx()
                        if (bundle.playerState.pendingMorningEcho != null) {
                            when (val resolved = ProgressionEngine.resolveMorningEcho(bundle, chooseOptionA = true)) {
                                is ActionOutcome.Success -> bundle = resolved.value.updatedBundle
                                is ActionOutcome.Failure -> bundle = bundle.copy(
                                    playerState = bundle.playerState.copy(pendingMorningEcho = null)
                                )
                            }
                        }
                        val route = ContentCatalog.routeById(routeId)
                            ?: throw IllegalArgumentException("路线不存在")
                        if (route.id !in bundle.discoveryState.unlockedRouteIds) {
                            throw IllegalStateException("该路线尚未解锁")
                        }

                        val (sessionSeed, nextRngState) = DeterministicRng.advanceSessionSeed(bundle.discoveryState.rngState)
                        val snapshot = ProgressionEngine.createDepartureSnapshot(
                            playerState = bundle.playerState,
                            inventory = bundle.inventory,
                            unlockedKeepsakeIds = bundle.discoveryState.unlockedKeepsakeIds,
                            discoveryState = bundle.discoveryState
                        )
                        val nowUtcMs = clock.currentWallTimeUtcMs()
                        val elapsedMs = clock.elapsedRealtimeMs()
                        val bootId = clock.currentBootId()
                        val sessionId = "exp_${nowUtcMs}_${UUID.randomUUID().toString().take(8)}"

                        val session = ExpeditionSession(
                            sessionId = sessionId,
                            status = SessionStatus.RUNNING,
                            routeId = route.id,
                            strategy = strategy,
                            startAtUtcMs = nowUtcMs,
                            endAtUtcMs = null,
                            startElapsedRealtimeMs = elapsedMs,
                            startBootId = bootId,
                            sessionSeed = sessionSeed,
                            contentVersion = ContentCatalog.CONTENT_VERSION,
                            rulesVersion = ContentCatalog.RULES_VERSION,
                            snapshot = snapshot,
                            isOnboarding = !bundle.discoveryState.onboardingCompleted,
                            nightPhenomenon = ContentCatalog.nightPhenomenonForUtcMs(nowUtcMs)
                        )

                        val nextPlayer = bundle.playerState.copy(
                            lastRouteId = route.id,
                            lastStrategy = strategy
                        )
                        val nextDiscovery = bundle.discoveryState.copy(rngState = nextRngState)

                        dao.upsertPlayerState(PlayerStateEntity(1, nextPlayer.version, encodePlayer(nextPlayer)))
                        dao.upsertDiscoveryState(DiscoveryStateEntity(1, encodeDiscovery(nextDiscovery)))
                        dao.insertSession(session.toEntity())
                        session
                    }
                    ActionOutcome.Success(session)
                } catch (e: Exception) {
                    ActionOutcome.Failure(e.message ?: "无法保存出发状态，请重试")
                }
            }
        }

    suspend fun cancelExpeditionEarly(sessionId: String): ActionOutcome<Unit> = withContext(Dispatchers.IO) {
        actionMutex.withLock {
            try {
                db.withTransaction {
                    val ent = dao.getSessionById(sessionId)
                        ?: throw IllegalStateException("未找到该旅程记录")
                    if (ent.status != SessionStatus.RUNNING.name || ent.endAtUtcMs != null) {
                        throw IllegalStateException("当前旅程已进入结算或结束，无法撤销")
                    }
                    val nowUtc = clock.currentWallTimeUtcMs()
                    val elapsed = DurationResolver.resolveForPreview(ent.toModel(), clock).durationSeconds
                    if (elapsed > 300L) {
                        throw IllegalStateException("出发已超过 5 分钟，请直接点击「结束冒险」进行结算")
                    }
                    dao.markSessionTerminal(sessionId, SessionStatus.CANCELLED.name, nowUtc)
                }
                ActionOutcome.Success(Unit)
            } catch (e: Exception) {
                ActionOutcome.Failure(e.message ?: "撤销出发失败，请重试")
            }
        }
    }

    suspend fun endAndSettleExpedition(sessionId: String? = null): ActionOutcome<SettlementResult> =
        withContext(Dispatchers.IO) {
            actionMutex.withLock {
                settleExpeditionLocked(sessionId)
            }
        }

    /**
     * Persists endAtUtcMs first without settling, used to test process kill / crash recovery mid-settlement.
     */
    suspend fun fixEndTimeOnlyForRecoveryTest(sessionId: String): Long = withContext(Dispatchers.IO) {
        actionMutex.withLock {
            db.withTransaction {
                val ent = dao.getSessionById(sessionId)
                    ?: error("Session $sessionId missing")
                val resolved = DurationResolver.resolveForSettlement(ent.toModel(), clock)
                dao.fixEndTimeIfEmpty(sessionId, resolved.resolvedEndAtUtcMs)
                dao.getSessionById(sessionId)!!.endAtUtcMs!!
            }
        }
    }

    private suspend fun recoverPendingSettlementLocked(): ActionOutcome<SettlementResult?> {
        val running = dao.getRunningSession() ?: return ActionOutcome.Success(null)
        if (running.endAtUtcMs == null) {
            return ActionOutcome.Success(null)
        }
        return when (val settled = settleExpeditionLocked(running.sessionId)) {
            is ActionOutcome.Success -> ActionOutcome.Success(settled.value)
            is ActionOutcome.Failure -> ActionOutcome.Failure(settled.reason)
        }
    }

    private suspend fun settleExpeditionLocked(targetSessionId: String?): ActionOutcome<SettlementResult> {
        return try {
            targetSessionId?.let { id ->
                dao.getSettlementById(id)?.let { row ->
                    return ActionOutcome.Success(decodeSettlement(row.payloadJson) ?: error("战报损坏"))
                }
            }
            // Step 1: Fix endAtUtcMs atomically in DB first
            val fixedSessionAndNote = db.withTransaction {
                val ent = if (targetSessionId != null) {
                    dao.getSessionById(targetSessionId)
                } else {
                    dao.getRunningSession()
                } ?: throw IllegalStateException("当前没有待结算的旅程")

                if (ent.status == SessionStatus.SETTLED.name) {
                    val existing = dao.getSettlementById(ent.sessionId)?.let { decodeSettlement(it.payloadJson) }
                        ?: throw IllegalStateException("旅程已结算但战报记录缺失")
                    return@withTransaction Triple(ent.toModel(), null as String?, existing)
                }
                if (ent.status == SessionStatus.CANCELLED.name) {
                    throw IllegalStateException("该旅程已撤销出发")
                }

                val model = ent.toModel()
                val resolved = DurationResolver.resolveForSettlement(model, clock)
                if (ent.endAtUtcMs == null) {
                    dao.fixEndTimeIfEmpty(ent.sessionId, resolved.resolvedEndAtUtcMs)
                }
                val reloaded = dao.getSessionById(ent.sessionId)!!.toModel()
                Triple(reloaded, resolved.anomalyNote, null as SettlementResult?)
            }

            val existingAlreadySettled = fixedSessionAndNote.third
            if (existingAlreadySettled != null) {
                return ActionOutcome.Success(existingAlreadySettled)
            }

            val fixedSession = fixedSessionAndNote.first
            val anomalyNote = fixedSessionAndNote.second
            val fixedEndMs = fixedSession.endAtUtcMs
                ?: throw IllegalStateException("结束时间未能持久化")

            // Step 2: Deterministic simulation off main thread
            val currentBundle = db.withTransaction { loadBundleInTx() }
            val resolvedDurationSec = ((fixedEndMs - fixedSession.startAtUtcMs).coerceAtLeast(0L)) / 1000L
            val output = ExpeditionSimulator.simulateSettlement(
                session = fixedSession,
                resolvedEndAtUtcMs = fixedEndMs,
                resolvedDurationSeconds = resolvedDurationSec,
                clockAnomalyNote = anomalyNote,
                bundle = currentBundle
            )

            // Step 3: Atomic commit with unique Settlement PK & (sessionId, rewardKind) RewardLedger PK
            val committedSettlement = db.withTransaction {
                val latestSession = dao.getSessionById(fixedSession.sessionId)
                    ?: throw IllegalStateException("会话不存在")
                val existingSettlement = dao.getSettlementById(fixedSession.sessionId)?.let { decodeSettlement(it.payloadJson) }
                if (latestSession.status == SessionStatus.SETTLED.name && existingSettlement != null) {
                    return@withTransaction existingSettlement
                }

                val insertedRow = dao.insertSettlementIfAbsent(
                    SettlementEntity(
                        sessionId = fixedSession.sessionId,
                        routeId = fixedSession.routeId,
                        settledAtUtcMs = fixedEndMs,
                        rulesVersion = fixedSession.rulesVersion,
                        payloadJson = encodeSettlement(output.settlement)
                    )
                )
                if (insertedRow == -1L && existingSettlement != null) {
                    return@withTransaction existingSettlement
                }

                var freshLedgerCount = 0
                for (entry in output.ledgerEntries) {
                    val ledgerRow = dao.insertLedgerIfAbsent(entry.toEntity(json))
                    if (ledgerRow != -1L) freshLedgerCount++
                }
                if (freshLedgerCount == 0 && existingSettlement != null) {
                    return@withTransaction existingSettlement
                }

                val b = output.updatedBundle
                dao.upsertPlayerState(PlayerStateEntity(1, b.playerState.version, encodePlayer(b.playerState)))
                dao.upsertRewardBudget(RewardBudgetEntity(1, encodeBudget(b.budgetState)))
                val discovery = ProgressionEngine.reconcileUnlocks(b.playerState, b.discoveryState, b.inventory)
                dao.upsertDiscoveryState(DiscoveryStateEntity(1, encodeDiscovery(discovery)))
                if (output.settlement.newEquipment.isNotEmpty()) {
                    dao.upsertInventory(output.settlement.newEquipment.map { it.toEntity() })
                }
                dao.markSessionTerminal(fixedSession.sessionId, SessionStatus.SETTLED.name, fixedEndMs)

                // Keep automatic rolling snapshots (latest 3)
                saveSnapshotInTx(
                    nowUtcMs = fixedEndMs,
                    reason = "第 ${b.discoveryState.totalJourneysCompleted} 次旅程结算后",
                    bundle = b,
                    activeSession = null
                )

                output.settlement
            }

            ActionOutcome.Success(committedSettlement)
        } catch (e: Exception) {
            ActionOutcome.Failure(e.message ?: "结算保存失败，请点击重试，已保留当前旅程记录")
        }
    }

    /**
     * Stage-2 sleep integration top-up path: shares the same RewardLedger & transaction boundary.
     * Never claws back rewards, never grants negative deltas, respects 7-day window and consentEpoch.
     */
    suspend fun applyLateSleepRecords(
        sessionId: String,
        records: List<SleepRecord>,
        expectedConsentEpoch: Long
    ): ActionOutcome<SleepTopUpDelta> = withContext(Dispatchers.IO) {
        actionMutex.withLock {
            try {
                val result = db.withTransaction {
                    validateSleepConsent(expectedConsentEpoch)
                    val reportRow = dao.getSettlementById(sessionId) ?: error("基础旅程尚未结算")
                    val report = decodeSettlement(reportRow.payloadJson) ?: error("战报损坏")
                    val endMs = report.endAtUtcMs
                    val nowMs = clock.currentWallTimeUtcMs()
                    if (!SleepBuffPolicy.isWithinAdjustmentWindow(endMs, nowMs)) {
                        throw IllegalStateException("该旅程已超过 7 天补发窗口")
                    }

                    val matched = SleepNormalizer.normalizeForSession(
                        sessionStartUtcMs = report.startAtUtcMs,
                        sessionEndUtcMs = endMs,
                        records = records.filter { it.provider == sleepProvider.providerId },
                        expectedConsentEpoch = expectedConsentEpoch
                    )

                    val baseGold = dao.getLedgerEntry(sessionId, RewardKind.BASE_GOLD.name)?.creditedTotal ?: 0L
                    val baseXp = dao.getLedgerEntry(sessionId, RewardKind.BASE_XP.name)?.creditedTotal ?: 0L
                    val creditedGold = dao.getLedgerEntry(sessionId, RewardKind.BLESSING_GOLD.name)?.creditedTotal ?: 0L
                    val creditedXp = dao.getLedgerEntry(sessionId, RewardKind.BLESSING_XP.name)?.creditedTotal ?: 0L

                    val delta = SleepBuffPolicy.computeTopUpDelta(
                        baseGold = baseGold,
                        baseXp = baseXp,
                        alreadyCreditedGold = creditedGold,
                        alreadyCreditedXp = creditedXp,
                        sleepMatch = matched,
                        goldRemainder = report.blessingGoldRemainder,
                        xpRemainder = report.blessingXpRemainder
                    )

                    if (delta.deltaGold > 0L || delta.deltaXp > 0L) {
                        val bundle = loadBundleInTx()
                        val (nextLevel, nextXp) = ProgressionEngine.applyXp(
                            bundle.playerState.level,
                            bundle.playerState.xp,
                            delta.deltaXp
                        )
                        val nextPlayer = bundle.playerState.copy(
                            level = nextLevel,
                            xp = nextXp,
                            resources = bundle.playerState.resources.addGold(delta.deltaGold)
                        )
                        dao.upsertPlayerState(PlayerStateEntity(1, nextPlayer.version, encodePlayer(nextPlayer)))
                        dao.upsertLedgerEntry(
                            RewardLedgerEntry(
                                sessionId = sessionId,
                                rewardKind = RewardKind.BLESSING_GOLD,
                                creditedTotal = delta.targetBlessingGold,
                                policyVersion = SleepBuffPolicy.POLICY_VERSION,
                                sourceRecordIds = matched.usedRecordIds
                            ).toEntity(json)
                        )
                        dao.upsertLedgerEntry(
                            RewardLedgerEntry(
                                sessionId = sessionId,
                                rewardKind = RewardKind.BLESSING_XP,
                                creditedTotal = delta.targetBlessingXp,
                                policyVersion = SleepBuffPolicy.POLICY_VERSION,
                                sourceRecordIds = matched.usedRecordIds
                            ).toEntity(json)
                        )
                    }
                    val updatedReport = report.copy(
                        blessingGold = delta.targetBlessingGold,
                        blessingXp = delta.targetBlessingXp,
                        sleepBonusBasisPoints = maxOf(report.sleepBonusBasisPoints, delta.totalBasisPoints),
                        sleepBonusPercentTenths = maxOf(report.sleepBonusBasisPoints, delta.totalBasisPoints) / 10
                    )
                    dao.upsertSettlement(reportRow.copy(payloadJson = encodeSettlement(updatedReport)))
                    // Throwing here rolls back every write if permission changed during the operation.
                    validateSleepConsent(expectedConsentEpoch)
                    delta
                }
                ActionOutcome.Success(result)
            } catch (e: Exception) {
                ActionOutcome.Failure(e.message ?: "同步安歇加成失败")
            }
        }
    }

    /** Adapters publish grant/revoke changes here. Revocation and reward commits share the mutex.
     * Each change must use a strictly newer, device-local epoch; it never travels with a save file.
     */
    suspend fun updateSleepConsent(enabled: Boolean, epoch: Long): ActionOutcome<Unit> = withContext(Dispatchers.IO) {
        actionMutex.withLock {
            db.withTransaction {
                val previous = dao.getSleepConsent(sleepProvider.providerId)
                if (previous != null && epoch <= previous.epoch) {
                    return@withTransaction ActionOutcome.Failure("授权状态已更新，请重新读取")
                }
                if (enabled && (sleepProvider.checkAvailability() != SleepProviderAvailability.READY ||
                            sleepProvider.currentConsentEpoch() != epoch)) {
                    return@withTransaction ActionOutcome.Failure("睡眠授权尚未就绪")
                }
                dao.upsertSleepConsent(SleepConsentEntity(sleepProvider.providerId, epoch, enabled))
                ActionOutcome.Success(Unit)
            }
        }
    }

    private suspend fun validateSleepConsent(expectedEpoch: Long) {
        val local = dao.getSleepConsent(sleepProvider.providerId)
        check(local?.enabled == true && local.epoch == expectedEpoch &&
            sleepProvider.checkAvailability() == SleepProviderAvailability.READY &&
            sleepProvider.currentConsentEpoch() == expectedEpoch) { "睡眠授权已变化，本次同步已取消" }
    }

    suspend fun upgradeFacility(facilityType: FacilityType): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.upgradeFacility(bundle, facilityType)
    }

    suspend fun craftEquipment(defId: String): ActionOutcome<Unit> = mutateBundle { bundle ->
        val id = "craft_${clock.currentWallTimeUtcMs()}_${UUID.randomUUID().toString().take(6)}"
        ProgressionEngine.craftEquipment(bundle, defId, id)
    }

    suspend fun upgradeEquipment(instanceId: String): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.upgradeEquipment(bundle, instanceId)
    }

    suspend fun equipItem(instanceId: String): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.equipItem(bundle, instanceId)
    }

    suspend fun unequipSlot(slot: EquipmentSlot): ActionOutcome<Unit> = mutateBundle { bundle ->
        ActionOutcome.Success(ProgressionEngine.unequipSlot(bundle, slot))
    }

    suspend fun toggleItemLock(instanceId: String): ActionOutcome<Unit> = mutateBundle { bundle ->
        ActionOutcome.Success(ProgressionEngine.toggleLock(bundle, instanceId))
    }

    suspend fun salvageSelected(selectedIds: Set<String>): ActionOutcome<SalvageResult> =
        withContext(Dispatchers.IO) {
            actionMutex.withLock {
                try {
                    db.withTransaction {
                        val bundle = loadBundleInTx()
                        when (val out = ProgressionEngine.salvageSelected(bundle, selectedIds)) {
                            is ActionOutcome.Failure -> out
                            is ActionOutcome.Success -> {
                                val next = out.value.updatedState
                                dao.upsertPlayerState(PlayerStateEntity(1, next.playerState.version, encodePlayer(next.playerState)))
                                val keptIds = next.inventory.map { it.instanceId }.toSet()
                                val removedIds = bundle.inventory.map { it.instanceId }.filterNot { it in keptIds }
                                if (removedIds.isNotEmpty()) {
                                    dao.deleteInventoryByIds(removedIds)
                                }
                                out
                            }
                        }
                    }
                } catch (e: Exception) {
                    ActionOutcome.Failure(e.message ?: "拆解装备失败")
                }
            }
        }

    suspend fun togglePassive(passiveId: String): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.togglePassive(bundle, passiveId)
    }

    suspend fun craftKeepsake(keepsakeId: String): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.craftKeepsake(bundle, keepsakeId)
    }

    suspend fun reforgeEquipmentAffix(instanceId: String): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.reforgeEquipmentAffix(bundle, instanceId)
    }

    suspend fun engraveSecondaryAffix(instanceId: String): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.engraveSecondaryAffix(bundle, instanceId)
    }

    suspend fun selectActiveCompanion(companionId: String?): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.selectActiveCompanion(bundle, companionId)
    }

    suspend fun selectTeaBlend(teaId: String?): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.selectTeaBlend(bundle, teaId)
    }

    suspend fun selectDreamBeaconTier(tier: Int): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.selectDreamBeaconTier(bundle, tier)
    }

    suspend fun giftCompanion(companionId: String): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.giftCompanion(bundle, companionId)
    }

    suspend fun unlockAstralNode(nodeId: String): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.unlockAstralNode(bundle, nodeId)
    }

    suspend fun resetAstralNodes(): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.resetAstralNodes(bundle)
    }

    suspend fun transmuteMaterial(recipeId: String): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.transmuteMaterial(bundle, recipeId)
    }

    suspend fun saveLoadout(slotIndex: Int, name: String = "方案 ${slotIndex + 1}", strategy: ExpeditionStrategy? = null): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.saveLoadout(bundle, slotIndex, name, strategy ?: bundle.playerState.lastStrategy)
    }

    suspend fun applyLoadout(slotIndex: Int): ActionOutcome<Unit> = mutateBundle { bundle ->
        ProgressionEngine.applyLoadout(bundle, slotIndex)
    }

    suspend fun resolveMorningEcho(chooseOptionA: Boolean): ActionOutcome<String> = withContext(Dispatchers.IO) {
        actionMutex.withLock {
            try {
                db.withTransaction {
                    val current = loadBundleInTx()
                    when (val res = ProgressionEngine.resolveMorningEcho(current, chooseOptionA)) {
                        is ActionOutcome.Failure -> res
                        is ActionOutcome.Success -> {
                            val next = res.value.updatedBundle
                            dao.upsertPlayerState(PlayerStateEntity(1, next.playerState.version, encodePlayer(next.playerState)))
                            dao.upsertRewardBudget(RewardBudgetEntity(1, encodeBudget(next.budgetState)))
                            dao.upsertDiscoveryState(DiscoveryStateEntity(1, encodeDiscovery(next.discoveryState)))
                            ActionOutcome.Success(res.value.summaryText)
                        }
                    }
                }
            } catch (e: Exception) {
                ActionOutcome.Failure(e.message ?: "处理晨间奇遇失败")
            }
        }
    }

    suspend fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val cur = dao.getSettings() ?: AppSettingsEntity()
                dao.upsertSettings(transform(cur))
            }
        }

    suspend fun exportBackupJson(): ActionOutcome<String> = withContext(Dispatchers.IO) {
        actionMutex.withLock {
            try {
                val payload = db.withTransaction {
                    val bundle = loadBundleInTx()
                    val running = dao.getRunningSession()?.toModel()?.copy(startBootId = null, startElapsedRealtimeMs = null)
                    val settlements = dao.getBackupSettlements(clock.currentWallTimeUtcMs() - SleepBuffPolicy.SEVEN_DAYS_MS).mapNotNull { decodeSettlement(it.payloadJson) }
                    val settlementIds = settlements.map { it.sessionId }.toSet()
        val ledger = dao.getAllLedgerEntries().filter { it.sessionId in settlementIds }.map { it.toModel(json).copy(sourceRecordIds = emptyList()) }
                    SaveBackupPayload(
                        exportedAtUtcMs = clock.currentWallTimeUtcMs(),
                        contentVersion = ContentCatalog.CONTENT_VERSION,
                        rulesVersion = ContentCatalog.RULES_VERSION,
                        playerState = bundle.playerState,
                        budgetState = bundle.budgetState,
                        discoveryState = bundle.discoveryState,
                        inventory = bundle.inventory,
                        activeSession = running,
                        recentSettlements = settlements,
                        ledgerEntries = ledger
                    )
                }
                ActionOutcome.Success(SaveBackupCodec.encode(payload))
            } catch (e: Exception) {
                ActionOutcome.Failure(e.message ?: "导出存档失败")
            }
        }
    }

    suspend fun importBackupJson(rawText: String): ActionOutcome<Unit> = withContext(Dispatchers.IO) {
        actionMutex.withLock {
            val decoded = SaveBackupCodec.decodeAndValidate(rawText)
            if (decoded is ActionOutcome.Failure) {
                return@withLock decoded
            }
            val payload = (decoded as ActionOutcome.Success).value
            try {
                db.withTransaction {
                    val curBundle = loadBundleInTx()
                    val curRunning = dao.getRunningSession()?.toModel()
                    saveSnapshotInTx(clock.currentWallTimeUtcMs(), "导入覆盖前自动保护备份", curBundle, curRunning)
                    replaceAllSaveDataInTx(payload)
                }
                ActionOutcome.Success(Unit)
            } catch (e: Exception) {
                ActionOutcome.Failure(e.message ?: "替换存档失败，已保留原进度")
            }
        }
    }

    suspend fun restoreSnapshot(snapshotId: Long): ActionOutcome<Unit> = withContext(Dispatchers.IO) {
        actionMutex.withLock {
            try {
                val snap = db.withTransaction { dao.getSnapshotById(snapshotId) }
                    ?: return@withLock ActionOutcome.Failure("未找到指定备份快照")
                val decoded = SaveBackupCodec.decodeAndValidate(snap.payloadJson)
                if (decoded is ActionOutcome.Failure) return@withLock decoded
                val payload = (decoded as ActionOutcome.Success).value
                db.withTransaction {
                    replaceAllSaveDataInTx(payload)
                }
                ActionOutcome.Success(Unit)
            } catch (e: Exception) {
                ActionOutcome.Failure(e.message ?: "恢复快照失败")
            }
        }
    }

    private suspend fun mutateBundle(block: (GameStateBundle) -> ActionOutcome<GameStateBundle>): ActionOutcome<Unit> =
        withContext(Dispatchers.IO) {
            actionMutex.withLock {
                try {
                    db.withTransaction {
                        val current = loadBundleInTx()
                        when (val res = block(current)) {
                            is ActionOutcome.Failure -> res
                            is ActionOutcome.Success -> {
                                val next = res.value
                                dao.upsertPlayerState(PlayerStateEntity(1, next.playerState.version, encodePlayer(next.playerState)))
                                dao.upsertRewardBudget(RewardBudgetEntity(1, encodeBudget(next.budgetState)))
                                dao.upsertDiscoveryState(DiscoveryStateEntity(1, encodeDiscovery(next.discoveryState)))
                                dao.upsertInventory(next.inventory.map { it.toEntity() })
                                ActionOutcome.Success(Unit)
                            }
                        }
                    }
                } catch (e: Exception) {
                    ActionOutcome.Failure(e.message ?: "操作保存失败，请重试")
                }
            }
        }

    private suspend fun loadBundleInTx(): GameStateBundle {
        val player = dao.getPlayerState()?.let { decodePlayer(it.payloadJson) } ?: PlayerState()
        val budget = dao.getRewardBudget()?.let { decodeBudget(it.payloadJson) } ?: RewardBudgetState()
        val inv = dao.getAllInventory().map { it.toModel() }
        val discovery = ProgressionEngine.reconcileUnlocks(
            player, dao.getDiscoveryState()?.let { decodeDiscovery(it.payloadJson) } ?: DiscoveryState(), inv
        )
        return GameStateBundle(player, budget, discovery, inv)
    }

    private suspend fun saveSnapshotInTx(
        nowUtcMs: Long,
        reason: String,
        bundle: GameStateBundle,
        activeSession: ExpeditionSession?
    ) {
        val settlements = dao.getBackupSettlements(clock.currentWallTimeUtcMs() - SleepBuffPolicy.SEVEN_DAYS_MS).mapNotNull { decodeSettlement(it.payloadJson) }
        val settlementIds = settlements.map { it.sessionId }.toSet()
        val ledger = dao.getAllLedgerEntries().filter { it.sessionId in settlementIds }.map { it.toModel(json).copy(sourceRecordIds = emptyList()) }
        val payload = SaveBackupPayload(
            exportedAtUtcMs = nowUtcMs,
            contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION,
            playerState = bundle.playerState,
            budgetState = bundle.budgetState,
            discoveryState = bundle.discoveryState,
            inventory = bundle.inventory,
            activeSession = activeSession?.copy(startBootId = null, startElapsedRealtimeMs = null),
            recentSettlements = settlements,
            ledgerEntries = ledger
        )
        dao.insertSnapshot(
            SaveSnapshotEntity(
                createdAtUtcMs = nowUtcMs,
                reason = reason,
                payloadJson = SaveBackupCodec.encode(payload)
            )
        )
        dao.pruneOldSnapshots()
    }

    private suspend fun replaceAllSaveDataInTx(payload: SaveBackupPayload) {
        dao.clearSessions()
        dao.clearSettlements()
        dao.clearLedger()
        dao.clearInventory()

        val reconciledDiscovery = ProgressionEngine.reconcileUnlocks(payload.playerState, payload.discoveryState, payload.inventory)
        dao.upsertPlayerState(PlayerStateEntity(1, payload.playerState.version, encodePlayer(payload.playerState)))
        dao.upsertRewardBudget(RewardBudgetEntity(1, encodeBudget(payload.budgetState)))
        dao.upsertDiscoveryState(DiscoveryStateEntity(1, encodeDiscovery(reconciledDiscovery)))
        if (payload.inventory.isNotEmpty()) {
            dao.upsertInventory(payload.inventory.map { it.toEntity() })
        }
        payload.activeSession?.copy(startBootId = null, startElapsedRealtimeMs = null)?.let { dao.upsertSession(it.toEntity()) }
        for (s in payload.recentSettlements) {
            dao.insertSettlementIfAbsent(
                SettlementEntity(
                    sessionId = s.sessionId,
                    routeId = s.routeId,
                    settledAtUtcMs = s.endAtUtcMs,
                    rulesVersion = s.rulesVersion,
                    payloadJson = encodeSettlement(s)
                )
            )
        }
        for (l in payload.ledgerEntries) {
            dao.insertLedgerIfAbsent(l.toEntity(json))
        }
    }

    private fun encodePlayer(p: PlayerState): String = json.encodeToString(PlayerState.serializer(), p)
    private fun decodePlayer(raw: String): PlayerState = json.decodeFromString(PlayerState.serializer(), raw)
    private fun encodeBudget(b: RewardBudgetState): String = json.encodeToString(RewardBudgetState.serializer(), b)
    private fun decodeBudget(raw: String): RewardBudgetState = json.decodeFromString(RewardBudgetState.serializer(), raw)
    private fun encodeDiscovery(d: DiscoveryState): String = json.encodeToString(DiscoveryState.serializer(), d)
    private fun decodeDiscovery(raw: String): DiscoveryState = json.decodeFromString(DiscoveryState.serializer(), raw)
    private fun encodeSettlement(s: SettlementResult): String = json.encodeToString(SettlementResult.serializer(), s)
    private fun decodeSettlement(raw: String): SettlementResult? = try {
        json.decodeFromString(SettlementResult.serializer(), raw)
    } catch (_: Exception) {
        null
    }

    private fun ExpeditionSession.toEntity(): ExpeditionSessionEntity = ExpeditionSessionEntity(
        sessionId = sessionId,
        status = status.name,
        routeId = routeId,
        strategy = strategy.name,
        startAtUtcMs = startAtUtcMs,
        endAtUtcMs = endAtUtcMs,
        startElapsedRealtimeMs = startElapsedRealtimeMs,
        startBootId = startBootId,
        sessionSeed = sessionSeed,
        contentVersion = contentVersion,
        rulesVersion = rulesVersion,
        snapshotJson = json.encodeToString(ExpeditionPlayerSnapshot.serializer(), snapshot),
        isOnboarding = isOnboarding,
        nightPhenomenonJson = nightPhenomenon?.let { json.encodeToString(NightPhenomenon.serializer(), it) }
    )

    private fun ExpeditionSessionEntity.toModel(): ExpeditionSession = ExpeditionSession(
        sessionId = sessionId,
        status = SessionStatus.valueOf(status),
        routeId = routeId,
        strategy = ExpeditionStrategy.valueOf(strategy),
        startAtUtcMs = startAtUtcMs,
        endAtUtcMs = endAtUtcMs,
        startElapsedRealtimeMs = startElapsedRealtimeMs,
        startBootId = startBootId,
        sessionSeed = sessionSeed,
        contentVersion = contentVersion,
        rulesVersion = rulesVersion,
        snapshot = json.decodeFromString(ExpeditionPlayerSnapshot.serializer(), snapshotJson),
        isOnboarding = isOnboarding,
        nightPhenomenon = nightPhenomenonJson?.let { json.decodeFromString(NightPhenomenon.serializer(), it) }
    )

    private fun EquipmentInstance.toEntity(): InventoryItemEntity = InventoryItemEntity(
        instanceId = instanceId,
        defId = defId,
        tier = tier,
        level = level,
        affix = affix.name,
        locked = locked,
        secondaryAffix = secondaryAffix?.name
    )

    private fun InventoryItemEntity.toModel(): EquipmentInstance {
        return EquipmentInstance(
            instanceId = instanceId,
            defId = defId,
            tier = tier,
            level = level,
            affix = AffixId.valueOf(affix),
            secondaryAffix = secondaryAffix?.let { AffixId.valueOf(it) },
            locked = locked
        )
    }

    private fun RewardLedgerEntry.toEntity(json: Json): RewardLedgerEntity = RewardLedgerEntity(
        sessionId = sessionId,
        rewardKind = rewardKind.name,
        creditedTotal = creditedTotal,
        policyVersion = policyVersion,
        sourceRecordIdsJson = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>()), sourceRecordIds)
    )

    private fun RewardLedgerEntity.toModel(json: Json): RewardLedgerEntry = RewardLedgerEntry(
        sessionId = sessionId,
        rewardKind = RewardKind.valueOf(rewardKind),
        creditedTotal = creditedTotal,
        policyVersion = policyVersion,
        sourceRecordIds = try {
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>()), sourceRecordIdsJson)
        } catch (_: Exception) {
            emptyList()
        }
    )
}
