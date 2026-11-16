package com.dreamtrail.core.save

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.ActionOutcome
import com.dreamtrail.core.engine.ProgressionEngine
import com.dreamtrail.core.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

@Serializable
data class SaveBackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtUtcMs: Long,
    val contentVersion: Int,
    val rulesVersion: Int,
    val playerState: PlayerState,
    val budgetState: RewardBudgetState,
    val discoveryState: DiscoveryState,
    val inventory: List<EquipmentInstance>,
    val activeSession: ExpeditionSession? = null,
    val recentSettlements: List<SettlementResult> = emptyList(),
    val ledgerEntries: List<RewardLedgerEntry> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class SaveBackupEnvelope(
    val format: String = FORMAT_ID,
    val sha256Checksum: String,
    val payloadJson: String
) {
    companion object {
        const val FORMAT_ID = "dreamtrail-save-v1"
    }
}

/**
 * Exports and validates versioned, checksum-verified game-only save backups.
 * Never includes health records, auth tokens, or local machine paths.
 */
object SaveBackupCodec {
    const val MAX_IMPORT_BYTES = 2 * 1024 * 1024

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(payload: SaveBackupPayload): String {
        val payloadStr = json.encodeToString(SaveBackupPayload.serializer(), payload)
        val checksum = sha256Hex(payloadStr)
        val envelope = SaveBackupEnvelope(
            format = SaveBackupEnvelope.FORMAT_ID,
            sha256Checksum = checksum,
            payloadJson = payloadStr
        )
        return json.encodeToString(SaveBackupEnvelope.serializer(), envelope)
    }

    fun decodeAndValidate(rawText: String): ActionOutcome<SaveBackupPayload> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return ActionOutcome.Failure("导入内容为空")
        }
        if (trimmed.toByteArray(Charsets.UTF_8).size > MAX_IMPORT_BYTES) {
            return ActionOutcome.Failure("存档文件体积超出上限，已取消导入")
        }

        val envelope = try {
            json.decodeFromString(SaveBackupEnvelope.serializer(), trimmed)
        } catch (_: Exception) {
            return ActionOutcome.Failure("存档格式无法识别，未修改当前进度")
        }

        if (envelope.format != SaveBackupEnvelope.FORMAT_ID) {
            return ActionOutcome.Failure("不支持的存档格式版本：${envelope.format}")
        }

        val expectedHash = sha256Hex(envelope.payloadJson)
        if (!expectedHash.equals(envelope.sha256Checksum, ignoreCase = true)) {
            return ActionOutcome.Failure("存档校验和不匹配，文件可能已损坏，已保护当前存档")
        }

        val payload = try {
            json.decodeFromString(SaveBackupPayload.serializer(), envelope.payloadJson)
        } catch (_: Exception) {
            return ActionOutcome.Failure("存档内容解析失败，未修改当前进度")
        }

        val validationError = validatePayload(payload)
        if (validationError != null) {
            return ActionOutcome.Failure("存档校验未通过：$validationError")
        }

        return ActionOutcome.Success(payload)
    }

    fun validatePayload(payload: SaveBackupPayload): String? {
        if (payload.schemaVersion != SaveBackupPayload.CURRENT_SCHEMA_VERSION) {
            return "存档版本不受支持 (${payload.schemaVersion})"
        }
        if (!supportedVersion(payload.rulesVersion, payload.contentVersion)) return "未知的规则或内容版本"
        val p = payload.playerState
        if (p.level !in 1..PlayerState.MAX_LEVEL) return "角色等级超出范围"
        if (p.version != PlayerState.CURRENT_VERSION) return "未知的角色版本"
        if (p.xp < 0L) return "角色经验不能为负"
        if (!p.resources.hasNonNegativeValues()) return "存在负数资源余额"
        if (p.facilities.values.any { it !in 0..5 }) return "营地设施等级超出范围"
        if (ContentCatalog.routeById(p.lastRouteId) == null) return "引用了不存在的路线 ${p.lastRouteId}"
        if (p.dreamBeaconTier !in 0..4) return "深梦信标阶数超出范围"
        val astralNodeIds = ContentCatalog.astralNodes.map { it.id }.toSet()
        if (p.unlockedAstralNodeIds.any { it !in astralNodeIds }) return "未知的旅人星图节点"
        if (p.unlockedAstralNodeIds.size > ProgressionEngine.totalAstralPoints(p.level)) return "旅人星图点数超出等级额度"
        for (node in ContentCatalog.astralNodes.filter { it.id in p.unlockedAstralNodeIds && it.tier > 1 }) {
            if (ContentCatalog.astralNodes.none { it.archetype == node.archetype && it.tier == node.tier - 1 && it.id in p.unlockedAstralNodeIds }) {
                return "旅人星图缺少前置节点"
            }
        }
        if (p.equippedPassives.size > ProgressionEngine.maxPassiveSlots(p) ||
            p.equippedPassives.distinct().size != p.equippedPassives.size ||
            p.equippedPassives.any { ContentCatalog.passiveById(it) == null }) return "被动配置异常"
        if (p.loadouts.any { it.slotIndex !in 0..2 } || p.loadouts.map { it.slotIndex }.distinct().size != p.loadouts.size) {
            return "配置预设栏位异常"
        }
        if (p.activeCompanionId != null && ContentCatalog.companionById(p.activeCompanionId) == null) {
            return "未知随行伙伴 ${p.activeCompanionId}"
        }
        if (p.activeTeaBlendId != null && ContentCatalog.teaBlendById(p.activeTeaBlendId) == null) {
            return "未知睡前热茶配方 ${p.activeTeaBlendId}"
        }
        if (p.pendingMorningEcho != null && ContentCatalog.morningEchoById(p.pendingMorningEcho.echoId) == null) {
            return "未知的晨间奇遇 ${p.pendingMorningEcho.echoId}"
        }
        if (p.pendingMorningEcho != null && ContentCatalog.morningEchoById(p.pendingMorningEcho.echoId)?.regionId != p.pendingMorningEcho.regionId) {
            return "晨间奇遇地区不匹配"
        }
        if (p.pendingTeaEcho != null && ContentCatalog.teaEchoById(p.pendingTeaEcho.echoId) == null) {
            return "未知的茶歇奇遇 ${p.pendingTeaEcho.echoId}"
        }
        if (p.pendingTeaEcho != null && ContentCatalog.teaEchoById(p.pendingTeaEcho.echoId)?.regionId != p.pendingTeaEcho.regionId) {
            return "茶歇奇遇地区不匹配"
        }
        if (p.activeSpiritId != null && ContentCatalog.spiritById(p.activeSpiritId) == null) {
            return "未知伴读灵兽 ${p.activeSpiritId}"
        }
        if (p.nightPrepCharge !in 0..100) return "夜行筹备度超出范围"
        if (p.focusVigourBuff?.let { !validVigour(it) } == true) return "专注神采数值异常"
        val allTags = ContentCatalog.defaultFocusTags + p.customFocusTags
        val tagIds = allTags.map { it.id }.toSet()
        if (p.customFocusTags.size > 6 || tagIds.size != allTags.size ||
            p.customFocusTags.any { it.id.isBlank() || it.name.isBlank() || it.name.length > 10 } ||
            p.selectedFocusTagId !in tagIds) return "专注标签异常"
        val taskIds = p.focusTasks.map { it.id }.toSet()
        if (p.focusTasks.size > 20 || taskIds.size != p.focusTasks.size || p.focusTasks.any {
                it.id.isBlank() || it.title.isBlank() || it.title.length > 40 || it.tagId !in tagIds ||
                    it.targetPomodoros !in 1..8 || it.completedPomodoros !in 0..it.targetPomodoros ||
                    it.completed != (it.completedPomodoros == it.targetPomodoros)
            }) return "待办进度异常"
        if (p.activeFocusTaskId != null && p.focusTasks.none { it.id == p.activeFocusTaskId && !it.completed }) return "关联待办异常"

        val invIds = payload.inventory.map { it.instanceId }.toSet()
        if (invIds.size != payload.inventory.size) return "背包存在重复实例编号"
        for (item in payload.inventory) {
            val def = ContentCatalog.equipmentById(item.defId) ?: return "未知装备定义 ${item.defId}"
            if (item.tier != def.tier) return "装备阶数与定义不匹配"
            if (item.level !in 1..EquipmentInstance.MAX_LEVEL) return "装备强化等级异常"
            if (item.secondaryAffix != null && item.secondaryAffix == item.affix) return "装备主副词条重复"
        }
        for (equippedId in p.equippedInstanceIds()) {
            if (equippedId !in invIds) return "已装备物品不在背包中"
        }
        for ((id, slot) in listOf(p.equippedWeaponId to EquipmentSlot.WEAPON, p.equippedArmorId to EquipmentSlot.ARMOR, p.equippedCharmId to EquipmentSlot.CHARM)) {
            if (id != null && payload.inventory.first { it.instanceId == id }.let { ContentCatalog.equipmentById(it.defId)?.slot } != slot) {
                return "装备与穿戴栏位不匹配"
            }
        }

        val b = payload.budgetState
        if (b.remainderSeconds !in 0..299 || b.focusRemainderSeconds !in 0..299) return "节点余数秒超出范围"
        if (b.equipmentProgressSeconds !in 0..7199) return "装备保底计数为负"
        if (b.equipmentProgressRemainder !in 0..99) return "装备进度余量超出范围"
        if (b.dispatchProgressSeconds !in 0..7199 || b.spiritProgressSeconds !in 0..3599 ||
            b.nightPrepRemainderSeconds !in 0..59) return "累计进度计数超出范围"
        for (intervals in listOf(b.creditedIntervals, b.focusCreditedIntervals)) {
            if (intervals.any { it.startSec < 0L || it.endSec <= it.startSec } ||
                intervals.zipWithNext().any { (a, next) -> a.endSec > next.startSec }) return "收益时间区间异常"
        }
        if (b.taskBountyUtcSeconds.size > 6 || b.taskBountyUtcSeconds.any { it < 0L }) return "悬赏计数异常"
        if (b.rarePityByRegion.values.any { it < 0 }) return "稀有保底计数为负"
        if (b.bossProgressSecondsByRegion.values.any { it < 0 }) return "首领进度为负"

        if (payload.discoveryState.totalJourneysCompleted < 0 || payload.discoveryState.totalCreditedSeconds < 0L ||
            payload.discoveryState.totalFocusSessionsCompleted < 0 || payload.discoveryState.totalFocusCreditedSeconds < 0L ||
            payload.discoveryState.completedTasksCount < 0 || payload.discoveryState.totalRestSessionsCompleted < 0) {
            return "统计计数不能为负"
        }
        val spiritIds = ContentCatalog.focusSpirits.map { it.id }.toSet()
        if (payload.discoveryState.unlockedSpiritIds.any { it !in spiritIds }) return "未知的伴读小灵兽"
        if (p.activeSpiritId != null && p.activeSpiritId !in payload.discoveryState.unlockedSpiritIds) return "伴读小灵兽尚未解锁"
        for (daily in listOf(payload.discoveryState.dailyFocusSecondsByDate, payload.discoveryState.dailySleepSecondsByDate)) {
            if (daily.any { (date, seconds) -> seconds < 0L || runCatching { java.time.LocalDate.parse(date) }.isFailure }) return "每日统计异常"
        }
        if (payload.discoveryState.focusSecondsByTag.any { (tag, seconds) -> tag !in tagIds || seconds < 0L }) return "专注标签统计异常"
        val keepsakeIds = ContentCatalog.keepsakes.map { it.id }.toSet()
        if (payload.discoveryState.unlockedKeepsakeIds.any { it !in keepsakeIds }) return "未知的首领信物"
        val milestoneIds = ContentCatalog.milestones.map { it.id }.toSet()
        if (payload.discoveryState.unlockedMilestoneIds.any { it !in milestoneIds }) return "未知的里程碑"
        val souvenirIds = ContentCatalog.souvenirs.map { it.id }.toSet()
        if (payload.discoveryState.unlockedSouvenirIds.any { it !in souvenirIds }) return "未知的风物志藏品"
        if (payload.discoveryState.companionAffinity.values.any { it < 0 }) return "伙伴好感计数不能为负"
        if (payload.discoveryState.dreamBeaconClears.any { (tier, count) -> tier !in 0..4 || count < 0 }) {
            return "深梦信标通关记录异常"
        }
        if (payload.discoveryState.enemyKillCounts.values.any { it < 0 }) return "魔物讨伐计数不能为负"
        if (payload.discoveryState.enemyKillCounts.keys.any { ContentCatalog.enemyById(it) == null }) return "未知的魔物讨伐记录"
        val chronicleIds = ContentCatalog.chronicles.map { it.id }.toSet()
        if (payload.discoveryState.chronicleProgress.any { (cid, st) -> cid !in chronicleIds || st !in 0..3 }) {
            return "秘闻卷宗进度异常"
        }
        val commissionIds = ContentCatalog.commissions.map { it.id }.toSet()
        if (payload.discoveryState.completedCommissionIds.any { it !in commissionIds }) return "未知的营地委托记录"

        for ((key, value) in b.resourceRemainders) {
            val denominator = if (key in setOf("blessing_gold", "blessing_xp")) 10000L else com.dreamtrail.core.engine.ResourceAccrual.RATE_DENOMINATOR
            if (key !in setOf("gold", "xp", "ink", "blessing_gold", "blessing_xp") && RegionId.entries.none { it.materialId == key }) return "未知的资源余量"
            if (value !in 0 until denominator) return "资源余量超出范围"
        }
        if (payload.discoveryState.regionCreditedSeconds.values.any { it < 0 }) return "地区探索时长为负"
        val sess = payload.activeSession
        if (sess != null) {
            if (!supportedVersion(sess.rulesVersion, sess.contentVersion)) return "未知的旅程规则版本"
            if (sess.status != SessionStatus.RUNNING) return "进行中旅程状态异常"
            if (sess.distractionCount < 0 || sess.flipFocusSeconds < 0 || runCatching { java.time.ZoneId.of(sess.zoneId) }.isFailure) return "旅程计时数据异常"
            if (sess.targetDurationSeconds != null && sess.targetDurationSeconds !in 300L..sess.sessionMode.maxSingleWindowSeconds) return "旅程目标时长异常"
            if (sess.sessionMode in setOf(SessionMode.FOCUS_POMODORO, SessionMode.POWER_NAP) && sess.targetDurationSeconds == null) return "旅程缺少目标时长"
            if (sess.sessionMode.isFocus && sess.focusTagId !in tagIds) return "旅程专注标签异常"
            if (sess.snapshot.focusVigourBuff?.let { !validVigour(it) } == true) return "旅程神采数值异常"
            if (sess.snapshot.facilityLevels.values.any { it !in 0..5 } || sess.snapshot.nightPrepTier !in 0..3) return "旅程设施或筹备等级异常"
            if (sess.snapshot.equippedItems.size > EquipmentSlot.entries.size) return "旅程装备数量异常"
            if (sess.snapshot.unlockedSpiritIds.any { it !in spiritIds } ||
                (sess.snapshot.activeSpiritId != null && sess.snapshot.activeSpiritId !in sess.snapshot.unlockedSpiritIds)) return "旅程灵兽配置异常"
            val weatherPercentages = buildList {
                sess.nightPhenomenon?.let {
                    addAll(listOf(it.goldBonusPercent, it.xpBonusPercent, it.materialBonusPercent,
                        it.dropBonusPercent, it.combatAtkPercent, it.combatDefPercent))
                }
                sess.dayPhenomenon?.let {
                    addAll(listOf(it.goldBonusPercent, it.xpBonusPercent, it.materialBonusPercent,
                        it.dropBonusPercent, it.combatAtkPercent, it.combatDefPercent, it.inkBonusPercent))
                }
            }
            if (weatherPercentages.any { it !in 0..100 }) return "旅程天象加成异常"
            if (sess.snapshot.unlockedKeepsakeIds.any { it !in keepsakeIds }) return "旅程快照包含未知的首领信物"
            val stats = sess.snapshot.effectiveStats
            if (sess.snapshot.level !in 1..PlayerState.MAX_LEVEL || stats.maxHp <= 0 || stats.attack <= 0 || stats.defense < 0 || stats.speed < 0) return "旅程快照属性异常"
            if (sess.snapshot.equippedItems.any { ContentCatalog.equipmentById(it.defId) == null }) return "旅程快照包含未知装备"
            if (sess.startAtUtcMs < 0 || (sess.endAtUtcMs != null && sess.endAtUtcMs < sess.startAtUtcMs)) return "旅程时间异常"
            if (payload.recentSettlements.any { it.sessionId == sess.sessionId } || payload.ledgerEntries.any { it.sessionId == sess.sessionId }) return "进行中旅程已存在结算记录"
        }
        if (payload.recentSettlements.map { it.sessionId }.distinct().size != payload.recentSettlements.size) return "重复战报"
        for (report in payload.recentSettlements) {
            if (!supportedVersion(report.rulesVersion, report.contentVersion)) return "未知的战报版本"
            if (report.endAtUtcMs < report.startAtUtcMs || report.baseGold < 0 || report.baseXp < 0 ||
                report.blessingGold < 0 || report.blessingXp < 0 || report.sleepBonusBasisPoints !in 300..1000 ||
                report.blessingGoldRemainder !in 0..9999 || report.blessingXpRemainder !in 0..9999) return "战报数值异常"
        }
        if (payload.ledgerEntries.map { it.sessionId to it.rewardKind }.distinct().size != payload.ledgerEntries.size) return "重复账本条目"
        if (sess != null && ContentCatalog.routeById(sess.routeId) == null) {
            return "进行中会话引用了不存在的路线"
        }
        if (payload.ledgerEntries.any { it.creditedTotal < 0L }) {
            return "奖励账本包含负数条目"
        }
        return null
    }

    private fun supportedVersion(rules: Int, content: Int) = rules == ContentCatalog.RULES_VERSION && content == ContentCatalog.CONTENT_VERSION

    private fun validVigour(buff: FocusVigourBuff): Boolean =
        buff.remainingFocusSeconds in 1L..14400L && buff.goldBonusPercent in 0..100 &&
            buff.xpBonusPercent in 0..100 && buff.inkBonusPercent in 0..100

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
