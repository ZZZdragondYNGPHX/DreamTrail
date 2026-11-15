package com.dreamtrail.core.content

import com.dreamtrail.core.model.AffixId
import com.dreamtrail.core.model.EquipmentSlot
import com.dreamtrail.core.model.RegionId

/**
 * Validates referential integrity, weights, counts, and unlock reachability in ContentCatalog.
 * Inspired by YAIRPG's Verify_Game_Objects() check, implemented cleanly in Kotlin.
 */
object ContentValidator {
    fun validate(catalog: ContentCatalog = ContentCatalog): List<String> {
        val errors = mutableListOf<String>()

        if (catalog.routes.size != 18) errors += "Expected 18 routes, got ${catalog.routes.size}"
        if (catalog.passives.size != 14) errors += "Expected 14 passives, got ${catalog.passives.size}"
        if (catalog.equipmentDefs.size != 30) errors += "Expected 30 equipment defs, got ${catalog.equipmentDefs.size}"
        if (catalog.eventTemplates.size != 78) errors += "Expected 78 event templates, got ${catalog.eventTemplates.size}"
        if (catalog.companionStories.size != 5) errors += "Expected 5 companion stories, got ${catalog.companionStories.size}"
        if (catalog.keepsakes.size != 10) errors += "Expected 10 keepsakes, got ${catalog.keepsakes.size}"
        if (catalog.milestones.size != 20) errors += "Expected 20 milestones, got ${catalog.milestones.size}"
        if (catalog.teaBlends.size != 7) errors += "Expected 7 tea blends, got ${catalog.teaBlends.size}"
        if (catalog.souvenirs.size != 18) errors += "Expected 18 souvenirs, got ${catalog.souvenirs.size}"
        if (catalog.morningEchoes.size != 12) errors += "Expected 12 morning echoes, got ${catalog.morningEchoes.size}"
        if (catalog.nightPhenomena.size < 7) errors += "Expected at least 7 night phenomena, got ${catalog.nightPhenomena.size}"
        if (catalog.chronicles.size != 6) errors += "Expected 6 chronicle quests, got ${catalog.chronicles.size}"
        if (catalog.astralNodes.size != 12) errors += "Expected 12 astral nodes, got ${catalog.astralNodes.size}"
        if (catalog.transmuteRecipes.size != 6) errors += "Expected 6 transmute recipes, got ${catalog.transmuteRecipes.size}"
        if (catalog.commissions.size != 6) errors += "Expected 6 camp commissions, got ${catalog.commissions.size}"

        val normalEnemies = catalog.enemies.filter { !it.isBoss }
        val bosses = catalog.enemies.filter { it.isBoss }
        if (normalEnemies.size != 36) errors += "Expected 36 normal enemies, got ${normalEnemies.size}"
        if (bosses.size != 6) errors += "Expected 6 bosses, got ${bosses.size}"
        for (boss in bosses) {
            if (boss.bossTrait == null) errors += "Boss ${boss.id} is missing bossTrait"
        }

        checkUniqueIds(catalog.routes.map { it.id }, "route", errors)
        checkUniqueIds(catalog.enemies.map { it.id }, "enemy", errors)
        checkUniqueIds(catalog.equipmentDefs.map { it.id }, "equipment", errors)
        checkUniqueIds(catalog.passives.map { it.id }, "passive", errors)
        checkUniqueIds(catalog.eventTemplates.map { it.id }, "event", errors)
        checkUniqueIds(catalog.companionStories.map { it.id }, "companion", errors)
        checkUniqueIds(catalog.keepsakes.map { it.id }, "keepsake", errors)
        checkUniqueIds(catalog.milestones.map { it.id }, "milestone", errors)
        checkUniqueIds(catalog.teaBlends.map { it.id }, "tea", errors)
        checkUniqueIds(catalog.souvenirs.map { it.id }, "souvenir", errors)
        checkUniqueIds(catalog.morningEchoes.map { it.id }, "morningEcho", errors)
        checkUniqueIds(catalog.chronicles.map { it.id }, "chronicle", errors)
        checkUniqueIds(catalog.astralNodes.map { it.id }, "astralNode", errors)
        checkUniqueIds(catalog.transmuteRecipes.map { it.id }, "transmuteRecipe", errors)
        checkUniqueIds(catalog.commissions.map { it.id }, "commission", errors)

        val enemyIds = catalog.enemies.map { it.id }.toSet()
        val passiveIds = catalog.passives.map { it.id }.toSet()
        val companionIds = catalog.companionStories.map { it.id }.toSet()

        for (route in catalog.routes) {
            if (route.enemyIds.isEmpty()) errors += "Route ${route.id} has empty enemyIds"
            for (eid in route.enemyIds) {
                if (eid !in enemyIds) errors += "Route ${route.id} references missing enemy $eid"
            }
            val boss = catalog.enemyById(route.bossId)
            if (boss == null || !boss.isBoss) {
                errors += "Route ${route.id} references invalid boss ${route.bossId}"
            }
            if (route.baseGoldPerHour <= 0 || route.baseXpPerHour <= 0 || route.baseMaterialPerHour <= 0) {
                errors += "Route ${route.id} has non-positive base yield"
            }
            if (route.dangerRate < 0.0 || route.dangerRate > 1.0) {
                errors += "Route ${route.id} dangerRate out of [0,1]: ${route.dangerRate}"
            }
        }

        for (region in RegionId.entries) {
            val regRoutes = catalog.routes.filter { it.regionId == region }
            if (regRoutes.size != 3) errors += "Region $region should have 3 routes, has ${regRoutes.size}"
            val regEvents = catalog.eventsForRegion(region)
            if (regEvents.size != 13) errors += "Region $region should have 13 events, has ${regEvents.size}"
            val regEquip = catalog.equipmentPoolForRegion(region)
            if (regEquip.isEmpty()) errors += "Region $region has no equipment pool"
            val regSouvenirs = catalog.souvenirsForRegion(region)
            if (regSouvenirs.size != 3) errors += "Region $region should have 3 souvenirs, has ${regSouvenirs.size}"
            val regEchoes = catalog.morningEchoesForRegion(region)
            if (regEchoes.size != 2) errors += "Region $region should have 2 morning echoes, has ${regEchoes.size}"
        }

        val representedAffixes = catalog.equipmentDefs.map { it.defaultAffix }.toSet()
        if (representedAffixes.size != AffixId.entries.size) {
            errors += "Not all affixes are represented across equipmentDefs: missing ${AffixId.entries.toSet() - representedAffixes}"
        }
        val representedSlots = catalog.equipmentDefs.map { it.slot }.toSet()
        if (representedSlots.size != EquipmentSlot.entries.size) {
            errors += "Not all equipment slots are represented"
        }

        var distinctFirstTexts = 0
        for (ev in catalog.eventTemplates) {
            if (ev.weight < 0) errors += "Event ${ev.id} has negative weight"
            if (ev.cooldownJourneys < 0) errors += "Event ${ev.id} has negative cooldown"
            if (ev.firstDiscoveryText.isBlank() || ev.repeatText.isBlank()) {
                errors += "Event ${ev.id} has blank narrative text"
            }
            if (ev.firstDiscoveryText != ev.repeatText) distinctFirstTexts++
            if (ev.companionId != null && ev.companionId !in companionIds) {
                errors += "Event ${ev.id} references unknown companion ${ev.companionId}"
            }
        }
        if (distinctFirstTexts < 12) {
            errors += "Expected at least 12 events with distinct first/repeat text, got $distinctFirstTexts"
        }

        for (comp in catalog.companionStories) {
            if (comp.passiveId !in passiveIds) errors += "Companion ${comp.id} references unknown passive ${comp.passiveId}"
            if (comp.chapters.map { it.chapterIndex } != (1..comp.chapters.size).toList()) {
                errors += "Companion ${comp.id} chapters are not 1..N sequential"
            }
        }

        val materialIds = RegionId.entries.map { it.materialId }.toSet()
        for (ks in catalog.keepsakes) {
            if (ks.bossMarkCost <= 0L) errors += "Keepsake ${ks.id} has non-positive bossMarkCost"
            if (ks.materialId !in materialIds) errors += "Keepsake ${ks.id} references unknown material ${ks.materialId}"
        }

        val chronicleRegions = catalog.chronicles.map { it.regionId }.toSet()
        if (chronicleRegions.size != RegionId.entries.size) {
            errors += "Chronicles should cover every region once"
        }
        for (c in catalog.chronicles) {
            if (c.stages.map { it.stageIndex } != listOf(1, 2, 3)) {
                errors += "Chronicle ${c.id} stages must be 1..3 sequential"
            }
        }
        val astralGroups = catalog.astralNodes.groupBy { it.archetype }
        if (astralGroups.size != 4) {
            errors += "Expected 4 astral archetypes, got ${astralGroups.size}"
        }
        astralGroups.forEach { (arch, nodes) ->
            if (nodes.map { it.tier }.sorted() != listOf(1, 2, 3)) {
                errors += "Astral branch $arch must have tiers 1..3"
            }
        }
        for (rx in catalog.transmuteRecipes) {
            if (rx.requiredConservatoryLevel !in 1..5) errors += "Transmute ${rx.id} invalid conservatory level"
            if (rx.inputMaterialId !in materialIds) errors += "Transmute ${rx.id} unknown input material"
            if (rx.outputMaterialId != null && rx.outputMaterialId !in materialIds) {
                errors += "Transmute ${rx.id} unknown output material"
            }
            if (rx.outputMaterialAmount <= 0L && rx.outputBossMarks <= 0L) {
                errors += "Transmute ${rx.id} produces nothing"
            }
        }
        for (comm in catalog.commissions) {
            if (comm.requiredMinNodes < 12) errors += "Commission ${comm.id} requiredMinNodes too small: ${comm.requiredMinNodes}"
        }

        return errors
    }

    private fun checkUniqueIds(ids: List<String>, label: String, errors: MutableList<String>) {
        val seen = mutableSetOf<String>()
        for (id in ids) {
            if (!seen.add(id)) errors += "Duplicate $label id: $id"
        }
    }
}
