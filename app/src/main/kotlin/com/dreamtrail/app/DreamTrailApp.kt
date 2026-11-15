package com.dreamtrail.app

import android.app.Application
import com.dreamtrail.app.data.local.DreamTrailDatabase
import com.dreamtrail.app.data.repository.GameRepository
import com.dreamtrail.app.time.AndroidClockProvider
import com.dreamtrail.app.time.ClockProvider

class DreamTrailApp : Application() {
    val database: DreamTrailDatabase by lazy { DreamTrailDatabase.open(this) }
    val clockProvider: ClockProvider by lazy { AndroidClockProvider(this) }
    val repository: GameRepository by lazy { GameRepository(database, clockProvider) }
}
