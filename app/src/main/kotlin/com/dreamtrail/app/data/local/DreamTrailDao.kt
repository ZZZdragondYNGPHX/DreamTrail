package com.dreamtrail.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DreamTrailDao {
    // Player state
    @Query("SELECT * FROM player_state WHERE id = 1")
    suspend fun getPlayerState(): PlayerStateEntity?

    @Query("SELECT * FROM player_state WHERE id = 1")
    fun observePlayerState(): Flow<PlayerStateEntity?>

    @Upsert
    suspend fun upsertPlayerState(entity: PlayerStateEntity)

    // Expedition sessions
    @Query("SELECT * FROM expedition_sessions WHERE status = 'RUNNING' ORDER BY start_at_utc_ms DESC LIMIT 1")
    suspend fun getRunningSession(): ExpeditionSessionEntity?

    @Query("SELECT * FROM expedition_sessions WHERE status = 'RUNNING' ORDER BY start_at_utc_ms DESC LIMIT 1")
    fun observeRunningSession(): Flow<ExpeditionSessionEntity?>

    @Query("SELECT * FROM expedition_sessions WHERE session_id = :sessionId")
    suspend fun getSessionById(sessionId: String): ExpeditionSessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(entity: ExpeditionSessionEntity)

    @Query("UPDATE expedition_sessions SET end_at_utc_ms = :endAtUtcMs WHERE session_id = :sessionId AND status = 'RUNNING' AND end_at_utc_ms IS NULL")
    suspend fun fixEndTimeIfEmpty(sessionId: String, endAtUtcMs: Long): Int

    @Query("UPDATE expedition_sessions SET status = :status, end_at_utc_ms = :endAtUtcMs WHERE session_id = :sessionId AND status = 'RUNNING'")
    suspend fun markSessionTerminal(sessionId: String, status: String, endAtUtcMs: Long): Int

    @Query("UPDATE expedition_sessions SET distraction_count = :distractionCount, flip_focus_sec = :flipFocusSec WHERE session_id = :sessionId AND status = 'RUNNING'")
    suspend fun updateRunningSessionFocusStats(sessionId: String, distractionCount: Int, flipFocusSec: Long): Int

    @Query("DELETE FROM expedition_sessions")
    suspend fun clearSessions()

    @Upsert
    suspend fun upsertSession(entity: ExpeditionSessionEntity)

    // Settlements
    @Query("SELECT * FROM settlements WHERE session_id = :sessionId")
    suspend fun getSettlementById(sessionId: String): SettlementEntity?

    @Query("SELECT * FROM settlements ORDER BY settled_at_utc_ms DESC LIMIT :limit")
    suspend fun getRecentSettlements(limit: Int = 30): List<SettlementEntity>

    @Query("SELECT * FROM settlements ORDER BY settled_at_utc_ms DESC LIMIT :limit")
    fun observeRecentSettlements(limit: Int = 30): Flow<List<SettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSettlementIfAbsent(entity: SettlementEntity): Long

    @Query("DELETE FROM settlements")
    suspend fun clearSettlements()

    // Reward ledger
    @Query("SELECT * FROM reward_ledger WHERE session_id = :sessionId")
    suspend fun getLedgerForSession(sessionId: String): List<RewardLedgerEntity>

    @Query("SELECT * FROM reward_ledger WHERE session_id = :sessionId AND reward_kind = :rewardKind")
    suspend fun getLedgerEntry(sessionId: String, rewardKind: String): RewardLedgerEntity?

    @Query("SELECT * FROM reward_ledger")
    suspend fun getAllLedgerEntries(): List<RewardLedgerEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLedgerIfAbsent(entity: RewardLedgerEntity): Long

    @Upsert
    suspend fun upsertLedgerEntry(entity: RewardLedgerEntity)

    @Query("DELETE FROM reward_ledger")
    suspend fun clearLedger()

    // Reward budget
    @Query("SELECT * FROM reward_budget WHERE id = 1")
    suspend fun getRewardBudget(): RewardBudgetEntity?

    @Query("SELECT * FROM reward_budget WHERE id = 1")
    fun observeRewardBudget(): Flow<RewardBudgetEntity?>

    @Upsert
    suspend fun upsertRewardBudget(entity: RewardBudgetEntity)

    // Inventory
    @Query("SELECT * FROM inventory_items ORDER BY tier DESC, level DESC, instance_id ASC")
    suspend fun getAllInventory(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items ORDER BY tier DESC, level DESC, instance_id ASC")
    fun observeInventory(): Flow<List<InventoryItemEntity>>

    @Upsert
    suspend fun upsertInventory(items: List<InventoryItemEntity>)

    @Query("DELETE FROM inventory_items WHERE instance_id IN (:ids)")
    suspend fun deleteInventoryByIds(ids: List<String>)

    @Query("DELETE FROM inventory_items")
    suspend fun clearInventory()

    // Discovery
    @Query("SELECT * FROM discovery_state WHERE id = 1")
    suspend fun getDiscoveryState(): DiscoveryStateEntity?

    @Query("SELECT * FROM discovery_state WHERE id = 1")
    fun observeDiscoveryState(): Flow<DiscoveryStateEntity?>

    @Upsert
    suspend fun upsertDiscoveryState(entity: DiscoveryStateEntity)

    // Save snapshots (rolling max 3)
    @Query("SELECT * FROM save_snapshots ORDER BY snapshot_id DESC LIMIT 3")
    suspend fun getLatestSnapshots(): List<SaveSnapshotEntity>

    @Query("SELECT * FROM save_snapshots ORDER BY snapshot_id DESC LIMIT 3")
    fun observeLatestSnapshots(): Flow<List<SaveSnapshotEntity>>

    @Query("SELECT * FROM save_snapshots WHERE snapshot_id = :snapshotId")
    suspend fun getSnapshotById(snapshotId: Long): SaveSnapshotEntity?

    @Insert
    suspend fun insertSnapshot(entity: SaveSnapshotEntity): Long

    @Query("DELETE FROM save_snapshots WHERE snapshot_id NOT IN (SELECT snapshot_id FROM save_snapshots ORDER BY snapshot_id DESC LIMIT 3)")
    suspend fun pruneOldSnapshots()

    // App settings
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Upsert
    suspend fun upsertSettings(entity: AppSettingsEntity)

    @Query("SELECT * FROM sleep_consent WHERE providerId = :providerId")
    suspend fun getSleepConsent(providerId: String): SleepConsentEntity?

    @Upsert
    suspend fun upsertSleepConsent(consent: SleepConsentEntity)

    @Upsert
    suspend fun upsertSettlement(settlement: SettlementEntity)

    @Query("SELECT * FROM settlements WHERE settled_at_utc_ms >= :cutoff OR session_id IN (SELECT session_id FROM settlements ORDER BY settled_at_utc_ms DESC LIMIT 30) ORDER BY settled_at_utc_ms DESC")
    suspend fun getBackupSettlements(cutoff: Long): List<SettlementEntity>
}
