package com.dreamtrail.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dreamtrail.app.data.repository.AppUiDataState
import com.dreamtrail.app.ui.components.JournalCard
import com.dreamtrail.app.ui.components.ResourceSummaryRow
import com.dreamtrail.app.ui.components.TagBadge
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.ProgressionEngine
import com.dreamtrail.core.model.BuildArchetype
import com.dreamtrail.core.model.EquipmentInstance
import com.dreamtrail.core.model.EquipmentSlot
import com.dreamtrail.core.model.PlayerState

@Composable
fun CharacterScreen(
    dataState: AppUiDataState,
    statusNotice: String?,
    errorMessage: String?,
    onEquipItem: (String) -> Unit,
    onUnequipSlot: (EquipmentSlot) -> Unit,
    onUpgradeEquipment: (String) -> Unit,
    onReforgeAffix: (String) -> Unit,
    onEngraveSecondaryAffix: (String) -> Unit = {},
    onToggleLock: (String) -> Unit,
    onSalvageSelected: (Set<String>) -> Unit,
    onTogglePassive: (String) -> Unit,
    duplicateSalvageCandidates: () -> List<String>,
    onUnlockAstralNode: (String) -> Unit = {},
    onResetAstralNodes: () -> Unit = {},
    onSaveLoadout: (Int) -> Unit = {},
    onApplyLoadout: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val player = dataState.playerState
    val workshopLevel = player.facilityLevel(com.dreamtrail.core.model.FacilityType.WORKSHOP)
    val effectiveStats = remember(
        player,
        dataState.inventory,
        dataState.discoveryState
    ) {
        ProgressionEngine.computeEffectiveStats(
            playerState = player,
            inventory = dataState.inventory,
            discoveryState = dataState.discoveryState
        )
    }
    val equippedItems = remember(player, dataState.inventory) {
        val equippedIds = player.equippedInstanceIds()
        dataState.inventory.filter { it.instanceId in equippedIds }
    }
    val resonanceCounts = remember(equippedItems, player.equippedPassives) {
        ProgressionEngine.computeResonanceCounts(equippedItems, player.equippedPassives)
    }
    val activeResonances = remember(equippedItems, player.equippedPassives) {
        ProgressionEngine.computeActiveResonances(equippedItems, player.equippedPassives)
    }
    val dualSynergy = remember(equippedItems, player.equippedPassives) {
        ProgressionEngine.hasDualArchetypeSynergy(equippedItems, player.equippedPassives)
    }
    val maxPassives = ProgressionEngine.maxPassiveSlots(player)
    val totalAstral = ProgressionEngine.totalAstralPoints(player.level)
    val usedAstral = ProgressionEngine.usedAstralPoints(player)
    val xpNeed = ProgressionEngine.xpRequiredForNextLevel(player.level)
    val activeTitle = remember(dataState.discoveryState.unlockedMilestoneIds) {
        ContentCatalog.milestones.lastOrNull { it.id in dataState.discoveryState.unlockedMilestoneIds }
    }

    var bulkMode by remember { mutableStateOf(false) }
    var selectedForSalvage by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ResourceSummaryRow(resources = player.resources)
        }

        if (dataState.runningSession != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DreamTrailColors.CardElevated,
                    border = BorderStroke(1.dp, DreamTrailColors.LanternAmber.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "当前有旅程正在进行中：此时调整装备与被动会立即保存为下一次旅程配置，不会改写本次出发快照。",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.LanternAmber,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (statusNotice != null) {
            item {
                Text(
                    text = statusNotice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DreamTrailColors.SageMist
                )
            }
        }
        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DreamTrailColors.CoralWarn
                )
            }
        }

        // Traveler status & 4 readable stats
        item {
            JournalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "旅人 · ${player.characterName}",
                            style = MaterialTheme.typography.titleLarge,
                            color = DreamTrailColors.ParchmentText
                        )
                        if (activeTitle != null) {
                            TagBadge("称号 · ${activeTitle.title}", DreamTrailColors.StarlightBlue)
                        }
                    }
                    TagBadge("Lv.${player.level} / ${PlayerState.MAX_LEVEL}", DreamTrailColors.LanternAmber)
                }

                if (player.level < PlayerState.MAX_LEVEL && xpNeed > 0L) {
                    val progress = (player.xp.toFloat() / xpNeed.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = DreamTrailColors.SageMist,
                        trackColor = DreamTrailColors.CardElevated
                    )
                    Text(
                        text = "经验：${player.xp} / $xpNeed",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk
                    )
                } else {
                    Text(
                        text = "已达到最高等级 Lv.${PlayerState.MAX_LEVEL}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.LanternAmber
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCell("生命", "${effectiveStats.maxHp}", Modifier.weight(1f))
                    StatCell("攻击", "${effectiveStats.attack}", Modifier.weight(1f))
                    StatCell("防护", "${effectiveStats.defense}", Modifier.weight(1f))
                    StatCell("速度", "${effectiveStats.speed}", Modifier.weight(1f))
                }
            }
        }

        // Build Archetype Resonance summary
        item {
            JournalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "流派共鸣（词条与被动联动）",
                        style = MaterialTheme.typography.titleMedium,
                        color = DreamTrailColors.ParchmentText
                    )
                    TagBadge(
                        text = if (activeResonances.isEmpty()) "未激活" else "${activeResonances.size} 个共鸣中",
                        accent = if (activeResonances.isEmpty()) DreamTrailColors.MutedInk else DreamTrailColors.LanternAmber
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (arch in com.dreamtrail.core.model.BuildArchetype.entries) {
                        val pts = resonanceCounts[arch] ?: 0
                        val tier = activeResonances.firstOrNull { it.archetype == arch }?.tier ?: 0
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = DreamTrailColors.CardElevated,
                            border = if (tier > 0) BorderStroke(1.dp, DreamTrailColors.LanternAmber.copy(alpha = 0.7f)) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(arch.displayName, style = MaterialTheme.typography.labelMedium, color = DreamTrailColors.ParchmentText)
                                Text(
                                    text = "$pts 点" + when (tier) {
                                        3 -> " · 6件"
                                        2 -> " · 4件"
                                        1 -> " · 2件"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (tier > 0) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk
                                )
                            }
                        }
                    }
                }
                if (dualSynergy) {
                    TagBadge("双系协奏生效（全基础属性 +6% · 伤害 +6% · 护盾 +12）", DreamTrailColors.StarlightBlue)
                }
                if (activeResonances.isNotEmpty()) {
                    for (res in activeResonances) {
                        val desc = resonanceTierDescription(res.archetype, res.tier)
                        val countText = when {
                            res.tier >= 3 -> "6"
                            res.tier == 2 -> "4"
                            else -> "2"
                        }
                        Text(
                            text = "• ${res.archetype.displayName}（${countText}件套）：$desc",
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.SageMist
                        )
                    }
                } else {
                    Text(
                        text = "装备主/副词条与被动共提供 2 / 4 / 6 点同流派契合度时自动激活共鸣；双流派各达 2 点可触发双系协奏。",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk
                    )
                }
            }
        }

        // Loadout Presets (3 slots)
        item {
            JournalCard {
                Text(
                    text = "行装预设方案（保存当前装备、被动、策略、伙伴与热茶）",
                    style = MaterialTheme.typography.titleMedium,
                    color = DreamTrailColors.ParchmentText
                )
                for (slotIdx in 0..2) {
                    val preset = player.loadouts.firstOrNull { it.slotIndex == slotIdx }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DreamTrailColors.CardElevated
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = preset?.name ?: "方案 ${slotIdx + 1}（未保存）",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DreamTrailColors.ParchmentText
                                )
                                Text(
                                    text = if (preset != null) {
                                        val compName = preset.companionId?.let { ContentCatalog.companionById(it)?.name } ?: "无随行"
                                        "${preset.strategy.displayName} · $compName · ${preset.equippedPassives.size} 被动"
                                    } else {
                                        "将当前整套配置保存至栏位 ${slotIdx + 1}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DreamTrailColors.MutedInk
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onSaveLoadout(slotIdx) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("存为当前", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { onApplyLoadout(slotIdx) },
                                    enabled = preset != null,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("应用", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Traveler Astral Chart (12 nodes)
        item {
            val remainingAstral = (totalAstral - usedAstral).coerceAtLeast(0)
            JournalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "旅人星图（每 4 级获得 1 点星辉）",
                            style = MaterialTheme.typography.titleMedium,
                            color = DreamTrailColors.ParchmentText
                        )
                        Text(
                            text = "可用星辉：$remainingAstral / $totalAstral 点（已点亮 $usedAstral/${ContentCatalog.astralNodes.size}）",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (remainingAstral > 0) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk
                        )
                    }
                    OutlinedButton(
                        onClick = onResetAstralNodes,
                        enabled = player.unlockedAstralNodeIds.isNotEmpty() && player.resources.gold >= 100L,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("重塑(100金)", maxLines = 1)
                    }
                }
                for (arch in BuildArchetype.entries) {
                    val branchNodes = ContentCatalog.astralNodes.filter { it.archetype == arch }.sortedBy { it.tier }
                    Text(
                        text = "${arch.displayName}星脉",
                        style = MaterialTheme.typography.labelLarge,
                        color = DreamTrailColors.StarlightBlue
                    )
                    for (node in branchNodes) {
                        val unlocked = node.id in player.unlockedAstralNodeIds
                        val prevUnlocked = node.tier <= 1 || branchNodes.firstOrNull { it.tier == node.tier - 1 }?.id in player.unlockedAstralNodeIds
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DreamTrailColors.CardElevated,
                            border = if (unlocked) BorderStroke(1.dp, DreamTrailColors.LanternAmber.copy(alpha = 0.7f)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "${node.tier}阶 · ${node.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (unlocked) DreamTrailColors.LanternAmber else DreamTrailColors.ParchmentText
                                    )
                                    Text(
                                        text = node.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DreamTrailColors.MutedInk
                                    )
                                }
                                if (unlocked) {
                                    TagBadge("已点亮", DreamTrailColors.LanternAmber)
                                } else {
                                    OutlinedButton(
                                        onClick = { onUnlockAstralNode(node.id) },
                                        enabled = remainingAstral > 0 && prevUnlocked,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text(if (prevUnlocked) "点亮" else "需前阶", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3 Equipment Slots
        item {
            JournalCard {
                Text(
                    text = "当前装备槽位（共 3 处）",
                    style = MaterialTheme.typography.titleMedium,
                    color = DreamTrailColors.ParchmentText
                )
                val slots = listOf(
                    EquipmentSlot.WEAPON to player.equippedWeaponId,
                    EquipmentSlot.ARMOR to player.equippedArmorId,
                    EquipmentSlot.CHARM to player.equippedCharmId
                )
                for ((slot, instId) in slots) {
                    val item = dataState.inventory.firstOrNull { it.instanceId == instId }
                    val def = item?.let { ContentCatalog.equipmentById(it.defId) }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DreamTrailColors.CardElevated
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${slot.displayName}：${def?.name ?: "空置"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DreamTrailColors.ParchmentText
                                )
                                if (item != null && def != null) {
                                    val secText = item.secondaryAffix?.let { " + 「${it.displayName}」" } ?: ""
                                    Text(
                                        text = "${item.tier}阶 · 强化Lv.${item.level} · 「${item.affix.displayName}」$secText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DreamTrailColors.SageMist
                                    )
                                } else {
                                    Text(
                                        text = "从下方背包挑选装备或前往工坊打造",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DreamTrailColors.MutedInk
                                    )
                                }
                            }
                            if (item != null) {
                                OutlinedButton(
                                    onClick = { onUnequipSlot(slot) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("卸下")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Journey Passives (max 2 or 3 active)
        item {
            JournalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "旅途被动（最多装配 $maxPassives 个）",
                        style = MaterialTheme.typography.titleMedium,
                        color = DreamTrailColors.ParchmentText
                    )
                    Text(
                        text = "已装配 ${player.equippedPassives.size}/$maxPassives",
                        style = MaterialTheme.typography.labelMedium,
                        color = DreamTrailColors.LanternAmber
                    )
                }
                if (maxPassives < 3) {
                    Text(
                        text = "提示：角色达到 Lv.25 或将旅舍修葺至 Lv.4，可永久解锁第 3 个旅途被动槽位。",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk
                    )
                }
                for (passive in ContentCatalog.passives) {
                    val unlocked = passive.id in dataState.discoveryState.unlockedPassiveIds
                    val active = passive.id in player.equippedPassives
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DreamTrailColors.CardElevated,
                        border = if (active) BorderStroke(1.dp, DreamTrailColors.LanternAmber) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = passive.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (unlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                                    )
                                    if (active) TagBadge("生效中", DreamTrailColors.LanternAmber)
                                }
                                Text(
                                    text = if (unlocked) passive.description else "解锁方式：${passive.unlockHint}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DreamTrailColors.MutedInk
                                )
                            }
                            if (unlocked) {
                                OutlinedButton(
                                    onClick = { onTogglePassive(passive.id) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text(if (active) "卸下" else "装配")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Inventory & safe bulk salvage
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "行囊装备（${dataState.inventory.size} 件）",
                    style = MaterialTheme.typography.titleMedium,
                    color = DreamTrailColors.ParchmentText
                )
                if (dataState.inventory.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            bulkMode = !bulkMode
                            selectedForSalvage = if (bulkMode) duplicateSalvageCandidates().toSet() else emptySet()
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(if (bulkMode) "退出拆解" else "批量拆解重复")
                    }
                }
            }
        }

        if (bulkMode) {
            item {
                JournalCard(containerColor = DreamTrailColors.CardElevated) {
                    Text(
                        text = "安全拆解：已装备与已锁定的物品默认受保护不会被拆解。已勾选 ${selectedForSalvage.size} 件。",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk
                    )
                    Button(
                        onClick = {
                            onSalvageSelected(selectedForSalvage)
                            bulkMode = false
                            selectedForSalvage = emptySet()
                        },
                        enabled = selectedForSalvage.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("确认拆解所选（${selectedForSalvage.size} 件）")
                    }
                }
            }
        }

        if (dataState.inventory.isEmpty()) {
            item {
                JournalCard {
                    Text(
                        text = "行囊暂时为空。完成第一次冒险或升级营地工坊后，即可获得并打造装备。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DreamTrailColors.MutedInk
                    )
                }
            }
        } else {
            val equippedIds = player.equippedInstanceIds()
            items(dataState.inventory, key = { it.instanceId }) { item ->
                val def = ContentCatalog.equipmentById(item.defId) ?: return@items
                val isEquipped = item.instanceId in equippedIds
                val stats = ContentCatalog.effectiveEquipmentStats(def, item.level, workshopLevel)
                val (upgGold, upgMat) = if (item.level < EquipmentInstance.MAX_LEVEL) {
                    ProgressionEngine.adjustedUpgradeCost(def, item.level + 1, player, dataState.inventory)
                } else 0L to 0L
                val nextUpgradeLevel = item.level + 1
                val workshopAllowsUpgrade = nextUpgradeLevel <= 5 || workshopLevel >= 5
                val canUpgrade = item.level < EquipmentInstance.MAX_LEVEL &&
                    workshopAllowsUpgrade &&
                    player.resources.gold >= upgGold &&
                    player.resources.materialCount(def.craftMaterialId) >= upgMat

                JournalCard(
                    borderColor = if (isEquipped) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${def.name} · Lv.${item.level}",
                                style = MaterialTheme.typography.titleSmall,
                                color = DreamTrailColors.ParchmentText
                            )
                            Text(
                                text = "${def.slot.displayName} · ${item.tier}阶 · 主「${item.affix.displayName}」" +
                                    (item.secondaryAffix?.let { " · 副「${it.displayName}」" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.StarlightBlue
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (isEquipped) TagBadge("已穿戴", DreamTrailColors.LanternAmber)
                            if (item.locked) TagBadge("已锁定", DreamTrailColors.SageMist)
                        }
                    }

                    Text(
                        text = "属性：生命+${stats.maxHp} 攻击+${stats.attack} 防护+${stats.defense} 速度+${stats.speed}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.ParchmentText
                    )
                    Text(
                        text = "主词条效果（${item.affix.archetype.displayName}）：${item.affix.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk
                    )
                    item.secondaryAffix?.let { sec ->
                        Text(
                            text = "副词条效果（${sec.archetype.displayName}）：${sec.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.SageMist
                        )
                    }

                    if (bulkMode) {
                        val protected = isEquipped || item.locked
                        val checked = item.instanceId in selectedForSalvage
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = checked,
                                enabled = !protected,
                                onCheckedChange = { want ->
                                    selectedForSalvage = if (want) {
                                        selectedForSalvage + item.instanceId
                                    } else {
                                        selectedForSalvage - item.instanceId
                                    }
                                }
                            )
                            Text(
                                text = if (protected) "已保护（穿戴中或已锁定）" else "选中拆解换回金币与材料",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                    } else {
                        val (refGold, refMat) = ProgressionEngine.reforgeCost(def, item)
                        val canReforge = player.resources.gold >= refGold &&
                            player.resources.materialCount(def.craftMaterialId) >= refMat

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isEquipped) {
                                    Button(
                                        onClick = { onEquipItem(item.instanceId) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Text("穿戴")
                                    }
                                }
                                if (item.level < EquipmentInstance.MAX_LEVEL) {
                                    OutlinedButton(
                                        onClick = { onUpgradeEquipment(item.instanceId) },
                                        enabled = canUpgrade,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        val upgradeLabel = when {
                                            nextUpgradeLevel > 5 && workshopLevel < 5 -> "星尘精铸需工坊Lv.5"
                                            nextUpgradeLevel > 5 -> "精铸(${upgGold}金/${upgMat}料)"
                                            else -> "强化(${upgGold}金/${upgMat}料)"
                                        }
                                        Text(upgradeLabel, maxLines = 1)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onReforgeAffix(item.instanceId) },
                                    enabled = canReforge,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("重铸主词(${refGold}金/${refMat}料)", maxLines = 1)
                                }
                                TextButton(
                                    onClick = { onToggleLock(item.instanceId) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text(if (item.locked) "解锁" else "锁定")
                                }
                            }
                            if (item.level >= 3) {
                                val (engGold, engMat) = ProgressionEngine.engraveCost(def, item)
                                val canEngrave = workshopLevel >= 2 &&
                                    player.resources.gold >= engGold &&
                                    player.resources.materialCount(def.craftMaterialId) >= engMat
                                OutlinedButton(
                                    onClick = { onEngraveSecondaryAffix(item.instanceId) },
                                    enabled = canEngrave,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    val label = if (item.secondaryAffix == null) "铭刻副词条" else "重铸副词条"
                                    Text(
                                        if (workshopLevel < 2) "$label（需工坊 Lv.2）"
                                        else "$label(${engGold}金/${engMat}料)",
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = DreamTrailColors.CardElevated
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = DreamTrailColors.MutedInk)
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = DreamTrailColors.ParchmentText)
        }
    }
}

private fun resonanceTierDescription(archetype: BuildArchetype, tier: Int): String = when (archetype) {
    BuildArchetype.BASTION -> when {
        tier >= 3 -> "开局护盾 +48、防护 +30%，受击反震 38% 防护值伤害"
        tier == 2 -> "开局护盾 +30、防护 +20%，受击反震 28% 防护值伤害"
        else -> "开局护盾 +14、防护 +10%"
    }
    BuildArchetype.GALE -> when {
        tier >= 3 -> "速度 +10、普攻系数 ×1.06，高速或偶回合追击 55% 伤害"
        tier == 2 -> "速度 +6、普攻系数 ×1.06，高速或偶回合追击 35% 伤害"
        else -> "速度 +3、普攻系数 ×1.06"
    }
    BuildArchetype.DREAMWEAVE -> when {
        tier >= 3 -> "开局护盾 +34，每 2 回合回复 11% 最大生命与 12 点护盾"
        tier == 2 -> "开局护盾 +22，每 2 回合回复 8% 最大生命与 8 点护盾"
        else -> "开局护盾 +10，每 3 回合回复 5% 最大生命"
    }
    BuildArchetype.SCHOLAR -> when {
        tier >= 3 -> "攻击 +28%，攻击无视目标 40% 防护（可与星海博学叠加至 52%）"
        tier == 2 -> "攻击 +18%，攻击无视目标 28% 防护（可与星海博学叠加）"
        else -> "攻击 +10%，攻击无视目标 12% 防护"
    }
}
