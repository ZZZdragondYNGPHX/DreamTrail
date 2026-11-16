package com.dreamtrail.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlayerStateEntity::class,
        ExpeditionSessionEntity::class,
        SettlementEntity::class,
        RewardLedgerEntity::class,
        RewardBudgetEntity::class,
        InventoryItemEntity::class,
        DiscoveryStateEntity::class,
        SaveSnapshotEntity::class,
        AppSettingsEntity::class,
        SleepConsentEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class DreamTrailDatabase : RoomDatabase() {
    abstract fun dao(): DreamTrailDao

    companion object {
        const val DB_NAME = "dreamtrail.db"

        fun open(context: Context, dbName: String = DB_NAME): DreamTrailDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                DreamTrailDatabase::class.java,
                dbName
            )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        }

        fun openInMemory(context: Context): DreamTrailDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                DreamTrailDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
