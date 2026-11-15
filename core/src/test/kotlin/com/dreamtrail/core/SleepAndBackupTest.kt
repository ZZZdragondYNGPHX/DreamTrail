package com.dreamtrail.core

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.ActionOutcome
import com.dreamtrail.core.model.*
import com.dreamtrail.core.save.SaveBackupCodec
import com.dreamtrail.core.save.SaveBackupPayload
import com.dreamtrail.core.sleep.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SleepAndBackupTest {

    @Test
    fun `base blessing is 3 percent and late sleep record only credits positive delta`() {
        val baseGold = 600L
        val baseXp = 750L

        val baseOnly = SleepBuffPolicy.computeBuff(baseGold, baseXp, sleepMatch = null)
        assertEquals(300, baseOnly.totalBasisPoints)
        assertEquals(18L, baseOnly.targetBlessingGold)
        assertEquals(22L, baseOnly.targetBlessingXp)

        // Proposal example: 7h asleep (+3%), 45m deep (+0.75%), 60m REM (+1%) = 7.75% (775 bps) -> 46 gold, 58 xp
        val match = NormalizedSleepMatch(
            matchedAsleepSeconds = 7 * 3600L,
            matchedDeepSeconds = 45 * 60L,
            matchedRemSeconds = 60 * 60L,
            usedRecordIds = listOf("hc_1"),
            qualityFlags = emptySet()
        )
        val topUp = SleepBuffPolicy.computeTopUpDelta(
            baseGold = baseGold,
            baseXp = baseXp,
            alreadyCreditedGold = baseOnly.targetBlessingGold,
            alreadyCreditedXp = baseOnly.targetBlessingXp,
            sleepMatch = match
        )
        assertEquals(775, topUp.totalBasisPoints)
        assertEquals(46L, topUp.targetBlessingGold)
        assertEquals(58L, topUp.targetBlessingXp)
        assertEquals(28L, topUp.deltaGold)
        assertEquals(36L, topUp.deltaXp)

        // Downward revision never claws back already credited reward
        val revisedDown = NormalizedSleepMatch(3600L, 0L, 0L, listOf("hc_1"), emptySet())
        val noClawback = SleepBuffPolicy.computeTopUpDelta(
            baseGold = baseGold,
            baseXp = baseXp,
            alreadyCreditedGold = 46L,
            alreadyCreditedXp = 58L,
            sleepMatch = revisedDown
        )
        assertEquals(0L, noClawback.deltaGold)
        assertEquals(0L, noClawback.deltaXp)
        assertEquals(46L, noClawback.targetBlessingGold)
    }

    @Test
    fun `sleep normalizer deduplicates sources isolates overbound stages and respects consentEpoch`() {
        val sStart = 1_700_000_000_000L
        val sEnd = sStart + 8 * 3600_000L

        // Two origins write same night: must choose ONE origin, never sum
        val recA = SleepRecord(
            provider = "health_connect", dataOrigin = "com.xiaomi.wearable",
            sourceRecordId = "r1", sourceRevision = "v1",
            startAtUtcMs = sStart, endAtUtcMs = sEnd,
            asleepSeconds = 7 * 3600L, deepSeconds = 3600L, remSeconds = 3600L, lightSeconds = 5 * 3600L,
            observedAtUtcMs = sEnd + 1000L, consentEpoch = 2L
        )
        val recB = SleepRecord(
            provider = "health_connect", dataOrigin = "com.other.app",
            sourceRecordId = "r2", sourceRevision = "v1",
            startAtUtcMs = sStart, endAtUtcMs = sEnd,
            asleepSeconds = 6 * 3600L, deepSeconds = 1800L, remSeconds = 1800L, lightSeconds = 5 * 3600L,
            observedAtUtcMs = sEnd + 2000L, consentEpoch = 2L
        )
        val recStaleConsent = recA.copy(sourceRecordId = "r_old", consentEpoch = 1L)

        val matched = SleepNormalizer.normalizeForSession(
            sessionStartUtcMs = sStart,
            sessionEndUtcMs = sEnd,
            records = listOf(recA, recB, recStaleConsent),
            expectedConsentEpoch = 2L,
            preferredOriginOrder = listOf("com.xiaomi.wearable")
        )
        assertEquals(7 * 3600L, matched.matchedAsleepSeconds)
        assertEquals(listOf("r1"), matched.usedRecordIds)

        // Overbound stage sum (deep + rem + light > asleep) isolates stage fields
        val corrupt = recA.copy(asleepSeconds = 3600L, deepSeconds = 4000L, remSeconds = 2000L, lightSeconds = 1000L)
        val corruptRes = SleepNormalizer.normalizeForSession(sStart, sEnd, listOf(corrupt), 2L)
        assertTrue(QualityFlag.STAGE_OVERBOUND in corruptRes.qualityFlags)
        assertNull(corruptRes.matchedDeepSeconds)
        assertNull(corruptRes.matchedRemSeconds)
        assertEquals(3600L, corruptRes.matchedAsleepSeconds)
    }

    @Test
    fun `save backup roundtrip succeeds and rejects tampered or negative payloads`() {
        val payload = SaveBackupPayload(
            exportedAtUtcMs = 1_700_000_000_000L,
            contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION,
            playerState = PlayerState(
                level = 4,
                xp = 25L,
                resources = ResourceBag(gold = 450L, materials = mapOf(RegionId.MISTWOOD.materialId to 22L)),
                equippedWeaponId = "w1"
            ),
            budgetState = RewardBudgetState(remainderSeconds = 120),
            discoveryState = DiscoveryState(totalJourneysCompleted = 3),
            inventory = listOf(
                EquipmentInstance("w1", "eq_pine_staff", 1, 2, AffixId.BOUNTIFUL_GATHER, locked = true)
            )
        )

        val encoded = SaveBackupCodec.encode(payload)
        val decoded = SaveBackupCodec.decodeAndValidate(encoded)
        assertTrue(decoded is ActionOutcome.Success)
        assertEquals(payload, (decoded as ActionOutcome.Success).value)

        // Tamper gold value in encoded string -> checksum mismatch must fail
        val tampered = encoded.replace("450", "99999")
        val tamperRes = SaveBackupCodec.decodeAndValidate(tampered)
        assertTrue(tamperRes is ActionOutcome.Failure)

        // Re-encode with negative gold -> semantic validation must fail
        val negPayload = payload.copy(playerState = payload.playerState.copy(resources = ResourceBag(gold = -10L)))
        val negEncoded = SaveBackupCodec.encode(negPayload)
        val negRes = SaveBackupCodec.decodeAndValidate(negEncoded)
        assertTrue(negRes is ActionOutcome.Failure)
    }
}
