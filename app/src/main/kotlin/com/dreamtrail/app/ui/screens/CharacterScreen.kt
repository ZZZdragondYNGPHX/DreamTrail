package com.dreamtrail.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dreamtrail.app.data.repository.AppUiDataState
import com.dreamtrail.app.ui.components.ArchetypeEmblem
import com.dreamtrail.app.ui.components.AstralConstellationCanvas
import com.dreamtrail.app.ui.components.CharacterPortraitBox
import com.dreamtrail.app.ui.components.EquipmentIconTile
import com.dreamtrail.app.ui.components.JournalCard
import com.dreamtrail.app.ui.components.PassiveEmblemTile
import com.dreamtrail.app.ui.components.ResourceSummaryRow
import com.dreamtrail.app.ui.components.TagBadge
import com.dreamtrail.app.ui.components.archetypeColor
import com.dreamtrail.app.ui.components.tierAccentColor
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.ProgressionEngine
import com.dreamtrail.core.model.BuildArchetype
import com.dreamtrail.core.model.EquipmentDef
import com.dreamtrail.core.model.EquipmentInstance
import com.dreamtrail.core.model.EquipmentSlot
import com.dreamtrail.core.model.FacilityType
import com.dreamtrail.core.model.PlayerState
import com.dreamtrail.core.model.Stats

private enum class CharacterSubTab(val label: String) {
    GEAR("装备"),
    ASTRAL("星图"),
    PASSIVES("被动与预设")
}

@OptIn(ExperimentalLayoutApi::class)
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
    modifier: Modifier = Modifier,
    actionInProgress: Boolean = false
) {
    val player = dataState.playerState
    val workshopLevel = player.facilityLevel(FacilityType.WORKSHOP)
    val effectiveStats = remember(player, dataState.inventory, dataState.discoveryState) {
        ProgressionEngine.computeEffectiveStats(
            playerState = player,
            inventory = dataState.inventory,
            discoveryState = dataState.discoveryState
        )
    }
    val equippedIds = remember(player) { player.equippedInstanceIds() }
    val equippedItems = remember(equippedIds, dataState.inventory) {
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

    var subTab by rememberSaveable { mutableStateOf(CharacterSubTab.GEAR) }
    var slotFilter by rememberSaveable { mutableStateOf<EquipmentSlot?>(null) }
    var bulkMode by remember { mutableStateOf(false) }
    var selectedForSalvage by remember { mutableStateOf(setOf<String>()) }
    var inspectedItemId by remember { mutableStateOf<String?>(null) }
    var selectedAstralNodeId by rememberSaveable {
        mutableStateOf(ContentCatalog.astralNodes.firstOrNull()?.id ?: "")
    }
    var focusedPassiveId by rememberSaveable {
        mutableStateOf(
            player.equippedPassives.firstOrNull()
                ?: ContentCatalog.passives.firstOrNull()?.id.orEmpty()
        )
    }

    val filteredInventory = remember(dataState.inventory, slotFilter, equippedIds) {
        dataState.inventory
            .filter { inst ->
                if (slotFilter == null) true
                else ContentCatalog.equipmentById(inst.defId)?.slot == slotFilter
            }
            .sortedWith(
                compareByDescending<EquipmentInstance> { it.instanceId in equippedIds }
                    .thenByDescending { it.tier }
                    .thenByDescending { it.level }
            )
    }
    val inventoryRows = remember(filteredInventory) { filteredInventory.chunked(4) }
    val passiveRows = remember { ContentCatalog.passives.chunked(2) }

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
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Compact Traveler Hero Header
        item {
            JournalCard(borderColor = DreamTrailColors.LanternAmber.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CharacterPortraitBox(
                        portraitKey = "traveler",
                        size = 76.dp,
                        accentColor = DreamTrailColors.LanternAmber,
                        badgeText = "Lv.${player.level}"
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "旅人 · ${player.characterName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = DreamTrailColors.ParchmentText
                            )
                            if (activeTitle != null) {
                                TagBadge(activeTitle.title, DreamTrailColors.StarlightBlue)
                            }
                        }
                        if (player.level < PlayerState.MAX_LEVEL && xpNeed > 0L) {
                            val progress = (player.xp.toFloat() / xpNeed.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = DreamTrailColors.SageMist,
                                trackColor = DreamTrailColors.CardElevated
                            )
                            Text(
                                text = "经验 ${player.xp} / $xpNeed · 最高 Lv.${PlayerState.MAX_LEVEL}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DreamTrailColors.MutedInk
                            )
                        } else {
                            Text(
                                text = "已达最高等级 Lv.${PlayerState.MAX_LEVEL}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DreamTrailColors.LanternAmber
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatCell("生命", "${effectiveStats.maxHp}", Modifier.weight(1f))
                    StatCell("攻击", "${effectiveStats.attack}", Modifier.weight(1f))
                    StatCell("防护", "${effectiveStats.defense}", Modifier.weight(1f))
                    StatCell("速度", "${effectiveStats.speed}", Modifier.weight(1f))
                }
            }
        }

        // 3 Sub-tab Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (tab in CharacterSubTab.entries) {
                    val selected = tab == subTab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .selectable(selected = selected, role = Role.Tab) { subTab = tab },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) DreamTrailColors.LanternAmber else DreamTrailColors.CardSurface,
                        border = BorderStroke(1.dp, if (selected) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        when (subTab) {
            CharacterSubTab.GEAR -> {
                // Paper-Doll Stage & 3 Equipped Slots + Resonance Halo
                item {
                    val weaponInst = dataState.inventory.firstOrNull { it.instanceId == player.equippedWeaponId }
                    val armorInst = dataState.inventory.firstOrNull { it.instanceId == player.equippedArmorId }
                    val charmInst = dataState.inventory.firstOrNull { it.instanceId == player.equippedCharmId }

                    JournalCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "身上装备",
                                style = MaterialTheme.typography.titleSmall,
                                color = DreamTrailColors.ParchmentText
                            )
                            if (dualSynergy) {
                                TagBadge("双系协奏生效", DreamTrailColors.StarlightBlue)
                            }
                        }

                        // Paper Doll 3-slot visual layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PaperDollSlot(
                                slot = EquipmentSlot.WEAPON,
                                item = weaponInst,
                                onInspect = { inspectedItemId = it.instanceId },
                                onSelectFilter = { slotFilter = EquipmentSlot.WEAPON }
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CharacterPortraitBox(
                                    portraitKey = "traveler",
                                    size = 92.dp,
                                    accentColor = activeResonances.firstOrNull()?.let { archetypeColor(it.archetype) }
                                        ?: DreamTrailColors.LanternAmber
                                )
                                Text(
                                    text = if (activeResonances.isNotEmpty()) "${activeResonances.size} 共鸣激活" else "无共鸣激活",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activeResonances.isNotEmpty()) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk
                                )
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                PaperDollSlot(
                                    slot = EquipmentSlot.ARMOR,
                                    item = armorInst,
                                    onInspect = { inspectedItemId = it.instanceId },
                                    onSelectFilter = { slotFilter = EquipmentSlot.ARMOR }
                                )
                                PaperDollSlot(
                                    slot = EquipmentSlot.CHARM,
                                    item = charmInst,
                                    onInspect = { inspectedItemId = it.instanceId },
                                    onSelectFilter = { slotFilter = EquipmentSlot.CHARM }
                                )
                            }
                        }

                        // 4 Archetype Resonance Cells
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (arch in BuildArchetype.entries) {
                                val pts = resonanceCounts[arch] ?: 0
                                val tier = activeResonances.firstOrNull { it.archetype == arch }?.tier ?: 0
                                val accent = archetypeColor(arch)
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = DreamTrailColors.CardElevated,
                                    border = if (tier > 0) BorderStroke(1.dp, accent.copy(alpha = 0.75f)) else BorderStroke(1.dp, DreamTrailColors.CardOutline)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        ArchetypeEmblem(archetype = arch, size = 16.dp)
                                        Text(arch.displayName, style = MaterialTheme.typography.labelSmall, color = DreamTrailColors.ParchmentText)
                                        Text(
                                            text = if (tier > 0) "$pts 点 · ${tier * 2}件" else "$pts 点",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (tier > 0) accent else DreamTrailColors.MutedInk
                                        )
                                    }
                                }
                            }
                        }

                        if (activeResonances.isNotEmpty()) {
                            for (res in activeResonances) {
                                val desc = resonanceTierDescription(res.archetype, res.tier)
                                Text(
                                    text = "• ${res.archetype.displayName}（${if (res.tier >= 3) 6 else res.tier * 2}件）：$desc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DreamTrailColors.SageMist
                                )
                            }
                        }
                        if (dualSynergy) {
                            Text(
                                text = "• 双系协奏：全基础属性 +6% · 伤害 +6% · 开局护盾 +12",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.StarlightBlue
                            )
                        }
                    }
                }

                // Inventory Grid Header & Filters
                item {
                    JournalCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Backpack,
                                    contentDescription = null,
                                    tint = DreamTrailColors.LanternAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "行囊（${filteredInventory.size}/${dataState.inventory.size} 件）",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DreamTrailColors.ParchmentText
                                )
                            }
                            if (dataState.inventory.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        bulkMode = !bulkMode
                                        selectedForSalvage = if (bulkMode) duplicateSalvageCandidates().toSet() else emptySet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.heightIn(min = 38.dp)
                                ) {
                                    Text(if (bulkMode) "退出拆解" else "批量拆解", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = slotFilter == null,
                                onClick = { slotFilter = null },
                                label = { Text("全部") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DreamTrailColors.LanternAmber.copy(alpha = 0.22f),
                                    selectedLabelColor = DreamTrailColors.LanternAmber
                                )
                            )
                            for (slot in EquipmentSlot.entries) {
                                FilterChip(
                                    selected = slotFilter == slot,
                                    onClick = { slotFilter = slot },
                                    label = { Text(slot.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DreamTrailColors.LanternAmber.copy(alpha = 0.22f),
                                        selectedLabelColor = DreamTrailColors.LanternAmber
                                    )
                                )
                            }
                        }

                        if (bulkMode) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DreamTrailColors.CardElevated,
                                border = BorderStroke(1.dp, DreamTrailColors.CoralWarn.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "安全拆解模式：已穿戴或已锁定装备默认保护。已选 ${selectedForSalvage.size} 件。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DreamTrailColors.MutedInk
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { selectedForSalvage = duplicateSalvageCandidates().toSet() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("重选重复")
                                        }
                                        Button(
                                            onClick = {
                                                onSalvageSelected(selectedForSalvage)
                                                bulkMode = false
                                                selectedForSalvage = emptySet()
                                            },
                                            enabled = selectedForSalvage.isNotEmpty(),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Text("确认拆解(${selectedForSalvage.size})")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredInventory.isEmpty()) {
                    item {
                        JournalCard {
                            Text(
                                text = if (dataState.inventory.isEmpty()) {
                                    "行囊暂时为空。完成第一次冒险或升级营地工坊后，即可获得并打造装备。"
                                } else {
                                    "当前筛选槽位暂无装备，请切换上方标签或前往工坊打造。"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                    }
                } else {
                    items(inventoryRows, key = { row -> row.first().instanceId }) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (inst in rowItems) {
                                val def = ContentCatalog.equipmentById(inst.defId)
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (def != null) {
                                        val isEquipped = inst.instanceId in equippedIds
                                        val isProtected = isEquipped || inst.locked
                                        EquipmentIconTile(
                                            def = def,
                                            level = inst.level,
                                            affix = inst.affix,
                                            secondaryAffix = inst.secondaryAffix,
                                            isEquipped = isEquipped,
                                            isLocked = inst.locked,
                                            isSelected = inspectedItemId == inst.instanceId,
                                            showBulkCheck = if (bulkMode) inst.instanceId in selectedForSalvage else null,
                                            dimmed = bulkMode && isProtected,
                                            tileSize = 74.dp,
                                            onClick = {
                                                if (bulkMode) {
                                                    if (!isProtected) {
                                                        selectedForSalvage = if (inst.instanceId in selectedForSalvage) {
                                                            selectedForSalvage - inst.instanceId
                                                        } else {
                                                            selectedForSalvage + inst.instanceId
                                                        }
                                                    }
                                                } else {
                                                    inspectedItemId = inst.instanceId
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            repeat(4 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            CharacterSubTab.ASTRAL -> {
                item {
                    val remainingAstral = (totalAstral - usedAstral).coerceAtLeast(0)
                    val selectedNode = ContentCatalog.astralNodeById(selectedAstralNodeId)
                        ?: ContentCatalog.astralNodes.first()
                    val branchNodes = ContentCatalog.astralNodes
                        .filter { it.archetype == selectedNode.archetype }
                        .sortedBy { it.tier }
                    val nodeUnlocked = selectedNode.id in player.unlockedAstralNodeIds
                    val prevUnlocked = selectedNode.tier <= 1 ||
                        branchNodes.firstOrNull { it.tier == selectedNode.tier - 1 }?.id in player.unlockedAstralNodeIds

                    JournalCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "旅人星盘（每 4 级获得 1 点星辉）",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DreamTrailColors.ParchmentText
                                )
                                Text(
                                    text = "可用星辉：$remainingAstral / $totalAstral · 已点亮 $usedAstral/${ContentCatalog.astralNodes.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (remainingAstral > 0) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk
                                )
                            }
                            OutlinedButton(
                                onClick = onResetAstralNodes,
                                enabled = player.unlockedAstralNodeIds.isNotEmpty() && player.resources.gold >= 100L,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text("重塑(100金)", maxLines = 1)
                            }
                        }

                        AstralConstellationCanvas(
                            unlockedNodeIds = player.unlockedAstralNodeIds,
                            selectedNodeId = selectedNode.id,
                            onSelectNode = { selectedAstralNodeId = it.id }
                        )

                        // Selected star node detail card
                        val nodeAccent = archetypeColor(selectedNode.archetype)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DreamTrailColors.CardElevated,
                            border = BorderStroke(
                                1.5.dp,
                                if (nodeUnlocked) nodeAccent else DreamTrailColors.CardOutline
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ArchetypeEmblem(archetype = selectedNode.archetype, size = 22.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${selectedNode.archetype.displayName}星脉 · 第${selectedNode.tier}阶「${selectedNode.title}」",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (nodeUnlocked) DreamTrailColors.LanternAmber else DreamTrailColors.ParchmentText
                                        )
                                    }
                                    TagBadge(
                                        text = if (nodeUnlocked) "已点亮" else "未点亮",
                                        accent = if (nodeUnlocked) nodeAccent else DreamTrailColors.MutedInk
                                    )
                                }
                                Text(
                                    text = selectedNode.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DreamTrailColors.SageMist
                                )
                                if (!nodeUnlocked) {
                                    Button(
                                        onClick = { onUnlockAstralNode(selectedNode.id) },
                                        enabled = remainingAstral > 0 && prevUnlocked,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 46.dp)
                                    ) {
                                        Text(
                                            when {
                                                !prevUnlocked -> "需先点亮 ${selectedNode.archetype.displayName} 第${selectedNode.tier - 1}阶节点"
                                                remainingAstral <= 0 -> "可用星辉不足（升级后获取）"
                                                else -> "消耗 1 星辉点亮此节点"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CharacterSubTab.PASSIVES -> {
                // Active Passive Slots Bar
                item {
                    JournalCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "已装配被动槽位（${player.equippedPassives.size}/$maxPassives）",
                                style = MaterialTheme.typography.titleSmall,
                                color = DreamTrailColors.ParchmentText
                            )
                            if (maxPassives < 3) {
                                TagBadge("Lv.25或旅舍Lv.4解锁第3槽", DreamTrailColors.MutedInk)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (idx in 0 until maxPassives) {
                                val pId = player.equippedPassives.getOrNull(idx)
                                val pDef = pId?.let { ContentCatalog.passiveById(it) }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(enabled = pDef != null) {
                                            pDef?.let { focusedPassiveId = it.id }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (pDef != null) DreamTrailColors.LanternAmber.copy(alpha = 0.14f) else DreamTrailColors.CardElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (pDef != null) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (pDef != null) {
                                            ArchetypeEmblem(pDef.archetype, size = 18.dp)
                                            Text(
                                                text = pDef.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = DreamTrailColors.ParchmentText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = "空槽 ${idx + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = DreamTrailColors.MutedInk
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Focused Passive Detail & Toggle Card
                item {
                    val focusedPassive = ContentCatalog.passiveById(focusedPassiveId)
                        ?: ContentCatalog.passives.first()
                    val unlocked = focusedPassive.id in dataState.discoveryState.unlockedPassiveIds
                    val active = focusedPassive.id in player.equippedPassives
                    val accent = archetypeColor(focusedPassive.archetype)

                    JournalCard(borderColor = accent.copy(alpha = 0.55f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ArchetypeEmblem(focusedPassive.archetype, size = 26.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${focusedPassive.name}（${focusedPassive.archetype.displayName}系）",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DreamTrailColors.ParchmentText
                                )
                                Text(
                                    text = if (unlocked) focusedPassive.description else "解锁条件：${focusedPassive.unlockHint}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (unlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                )
                            }
                            if (unlocked) {
                                Button(
                                    onClick = { onTogglePassive(focusedPassive.id) },
                                    colors = if (active) {
                                        ButtonDefaults.buttonColors(
                                            containerColor = DreamTrailColors.CardElevated,
                                            contentColor = DreamTrailColors.ParchmentText
                                        )
                                    } else ButtonDefaults.buttonColors()
                                ) {
                                    Text(if (active) "卸下" else "装配")
                                }
                            }
                        }
                    }
                }

                // 2-Column Passive Emblem Grid
                items(passiveRows, key = { row -> row.first().id }) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (passive in rowItems) {
                            val unlocked = passive.id in dataState.discoveryState.unlockedPassiveIds
                            val equipped = passive.id in player.equippedPassives
                            Box(modifier = Modifier.weight(1f)) {
                                PassiveEmblemTile(
                                    passive = passive,
                                    unlocked = unlocked,
                                    equipped = equipped,
                                    onClick = { focusedPassiveId = passive.id }
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Loadout Presets (3 slots)
                item {
                    JournalCard {
                        Text(
                            text = "行装预设方案（一键切换装备、被动、策略与伙伴）",
                            style = MaterialTheme.typography.titleSmall,
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
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.heightIn(min = 44.dp)
                                        ) {
                                            Text("存为当前", maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = { onApplyLoadout(slotIdx) },
                                            enabled = preset != null,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.heightIn(min = 44.dp)
                                        ) {
                                            Text("应用", maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Live Equipment Detail & Forge Dialog
    val inspectedItem = inspectedItemId?.let { id -> dataState.inventory.firstOrNull { it.instanceId == id } }
    val inspectedDef = inspectedItem?.let { ContentCatalog.equipmentById(it.defId) }
    if (inspectedItem != null && inspectedDef != null) {
        val equippedSlotInstId = when (inspectedDef.slot) {
            EquipmentSlot.WEAPON -> player.equippedWeaponId
            EquipmentSlot.ARMOR -> player.equippedArmorId
            EquipmentSlot.CHARM -> player.equippedCharmId
        }
        val currentSlotInst = equippedSlotInstId?.let { id -> dataState.inventory.firstOrNull { it.instanceId == id } }
        val currentSlotDef = currentSlotInst?.let { ContentCatalog.equipmentById(it.defId) }

        GearDetailForgeDialog(
            item = inspectedItem,
            def = inspectedDef,
            isEquipped = inspectedItem.instanceId in equippedIds,
            currentEquippedInSlot = if (currentSlotInst?.instanceId != inspectedItem.instanceId) currentSlotInst to currentSlotDef else null,
            player = player,
            inventory = dataState.inventory,
            workshopLevel = workshopLevel,
            statusNotice = statusNotice,
            errorMessage = errorMessage,
            actionInProgress = actionInProgress,
            onDismiss = { inspectedItemId = null },
            onEquip = { onEquipItem(inspectedItem.instanceId) },
            onUnequip = { onUnequipSlot(inspectedDef.slot) },
            onUpgrade = { onUpgradeEquipment(inspectedItem.instanceId) },
            onReforge = { onReforgeAffix(inspectedItem.instanceId) },
            onEngraveSecondary = { onEngraveSecondaryAffix(inspectedItem.instanceId) },
            onToggleLock = { onToggleLock(inspectedItem.instanceId) }
        )
    }
}

@Composable
private fun PaperDollSlot(
    slot: EquipmentSlot,
    item: EquipmentInstance?,
    onInspect: (EquipmentInstance) -> Unit,
    onSelectFilter: () -> Unit
) {
    val def = item?.let { ContentCatalog.equipmentById(it.defId) }
    if (item != null && def != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            EquipmentIconTile(
                def = def,
                level = item.level,
                affix = item.affix,
                secondaryAffix = item.secondaryAffix,
                isEquipped = true,
                isLocked = item.locked,
                tileSize = 68.dp,
                onClick = { onInspect(item) }
            )
            Text(
                text = slot.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = DreamTrailColors.MutedInk
            )
        }
    } else {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DreamTrailColors.NightBackground,
            border = BorderStroke(1.dp, DreamTrailColors.CardOutline),
            modifier = Modifier
                .size(68.dp)
                .clickable(onClick = onSelectFilter)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = DreamTrailColors.MutedInk.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = slot.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = DreamTrailColors.MutedInk
                )
            }
        }
    }
}

@Composable
private fun GearDetailForgeDialog(
    item: EquipmentInstance,
    def: EquipmentDef,
    isEquipped: Boolean,
    currentEquippedInSlot: Pair<EquipmentInstance?, EquipmentDef?>?,
    player: PlayerState,
    inventory: List<EquipmentInstance>,
    workshopLevel: Int,
    statusNotice: String?,
    errorMessage: String?,
    actionInProgress: Boolean,
    onDismiss: () -> Unit,
    onEquip: () -> Unit,
    onUnequip: () -> Unit,
    onUpgrade: () -> Unit,
    onReforge: () -> Unit,
    onEngraveSecondary: () -> Unit,
    onToggleLock: () -> Unit
) {
    val stats = ContentCatalog.effectiveEquipmentStats(def, item.level, workshopLevel)
    val equippedStats: Stats? = currentEquippedInSlot?.let { (curInst, curDef) ->
        if (curInst != null && curDef != null) {
            ContentCatalog.effectiveEquipmentStats(curDef, curInst.level, workshopLevel)
        } else null
    }

    val nextUpgradeLevel = item.level + 1
    val (upgGold, upgMat) = if (item.level < EquipmentInstance.MAX_LEVEL) {
        ProgressionEngine.adjustedUpgradeCost(def, nextUpgradeLevel, player, inventory)
    } else 0L to 0L
    val workshopAllowsUpgrade = nextUpgradeLevel <= 5 || workshopLevel >= 5
    val canUpgrade = item.level < EquipmentInstance.MAX_LEVEL &&
        workshopAllowsUpgrade &&
        player.resources.gold >= upgGold &&
        player.resources.materialCount(def.craftMaterialId) >= upgMat

    val (refGold, refMat) = ProgressionEngine.reforgeCost(def, item)
    val canReforge = player.resources.gold >= refGold &&
        player.resources.materialCount(def.craftMaterialId) >= refMat

    val matName = RegionIdNameHelper.materialName(def.craftMaterialId)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DreamTrailColors.CardSurface,
            border = BorderStroke(1.5.dp, tierAccentColor(def.tier).copy(alpha = 0.65f)),
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EquipmentIconTile(
                        def = def,
                        level = item.level,
                        affix = item.affix,
                        secondaryAffix = item.secondaryAffix,
                        isEquipped = isEquipped,
                        isLocked = item.locked,
                        tileSize = 72.dp
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = def.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = DreamTrailColors.ParchmentText
                        )
                        Text(
                            text = "${def.slot.displayName} · T${item.tier} · 强化 Lv.${item.level}/${EquipmentInstance.MAX_LEVEL}",
                            style = MaterialTheme.typography.bodySmall,
                            color = tierAccentColor(def.tier)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (isEquipped) TagBadge("已穿戴", DreamTrailColors.LanternAmber)
                            if (item.locked) TagBadge("已锁定", DreamTrailColors.SageMist)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = DreamTrailColors.MutedInk)
                    }
                }

                // Stats & Comparison Deltas
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DreamTrailColors.CardElevated
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CompareStatCell("生命", stats.maxHp, equippedStats?.maxHp)
                        CompareStatCell("攻击", stats.attack, equippedStats?.attack)
                        CompareStatCell("防护", stats.defense, equippedStats?.defense)
                        CompareStatCell("速度", stats.speed, equippedStats?.speed)
                    }
                }

                // Primary & Secondary Affix Cards
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DreamTrailColors.CardElevated,
                    border = BorderStroke(1.dp, archetypeColor(item.affix.archetype).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ArchetypeEmblem(item.affix.archetype, size = 16.dp)
                            Text(
                                text = "主词条「${item.affix.displayName}」（${item.affix.archetype.displayName}）",
                                style = MaterialTheme.typography.labelLarge,
                                color = DreamTrailColors.StarlightBlue
                            )
                        }
                        Text(
                            text = item.affix.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.ParchmentText
                        )

                        HorizontalDivider(color = DreamTrailColors.CardOutline)

                        val sec = item.secondaryAffix
                        if (sec != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ArchetypeEmblem(sec.archetype, size = 16.dp)
                                Text(
                                    text = "副词条「${sec.displayName}」（${sec.archetype.displayName}）",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DreamTrailColors.SageMist
                                )
                            }
                            Text(
                                text = sec.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.ParchmentText
                            )
                        } else {
                            Text(
                                text = if (item.level < 3) "副词条：装备强化至 Lv.3 且工坊 Lv.2 后可铭刻第 2 条词条"
                                else "副词条：尚未铭刻（可在下方消耗资源铭刻）",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                    }
                }

                if (actionInProgress) {
                    Text("正在保存…", color = DreamTrailColors.MutedInk, style = MaterialTheme.typography.bodyMedium)
                }
                errorMessage?.let {
                    Text(it, color = DreamTrailColors.CoralWarn, style = MaterialTheme.typography.bodyMedium)
                }
                if (errorMessage == null) statusNotice?.let {
                    Text(it, color = DreamTrailColors.SageMist, style = MaterialTheme.typography.bodyMedium)
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isEquipped) {
                        OutlinedButton(
                            onClick = onUnequip,
                            enabled = !actionInProgress,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                        ) {
                            Text("卸下")
                        }
                    } else {
                        Button(
                            onClick = onEquip,
                            enabled = !actionInProgress,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                        ) {
                            Text("穿戴")
                        }
                    }
                    OutlinedButton(
                        onClick = onToggleLock,
                        enabled = !actionInProgress,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                    ) {
                        Icon(
                            imageVector = if (item.locked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(if (item.locked) "解锁" else "锁定")
                    }
                }

                if (item.level < EquipmentInstance.MAX_LEVEL) {
                    OutlinedButton(
                        onClick = onUpgrade,
                        enabled = canUpgrade && !actionInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                    ) {
                        val upgradeLabel = when {
                            nextUpgradeLevel > 5 && workshopLevel < 5 -> "星尘精铸至 Lv.$nextUpgradeLevel（需工坊 Lv.5）"
                            nextUpgradeLevel > 5 -> "精铸 Lv.$nextUpgradeLevel（${upgGold}金 + ${upgMat}${matName}）"
                            else -> "强化至 Lv.$nextUpgradeLevel（${upgGold}金 + ${upgMat}${matName}）"
                        }
                        Text(upgradeLabel)
                    }
                }

                OutlinedButton(
                    onClick = onReforge,
                    enabled = canReforge && !actionInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                ) {
                    Text("重铸主词条（${refGold}金 + ${refMat}${matName}）")
                }

                if (item.level >= 3) {
                    val (engGold, engMat) = ProgressionEngine.engraveCost(def, item)
                    val canEngrave = workshopLevel >= 2 &&
                        player.resources.gold >= engGold &&
                        player.resources.materialCount(def.craftMaterialId) >= engMat
                    val label = if (item.secondaryAffix == null) "铭刻副词条" else "重铸副词条"
                    OutlinedButton(
                        onClick = onEngraveSecondary,
                        enabled = canEngrave && !actionInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                    ) {
                        Text(
                            if (workshopLevel < 2) "$label（需工坊 Lv.2）"
                            else "$label（${engGold}金 + ${engMat}${matName}）"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareStatCell(label: String, value: Int, compareBase: Int?) {
    val delta = compareBase?.let { value - it } ?: 0
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = DreamTrailColors.MutedInk)
        Text("+$value", style = MaterialTheme.typography.titleSmall, color = DreamTrailColors.ParchmentText)
        if (compareBase != null && delta != 0) {
            Text(
                text = if (delta > 0) "▲+$delta" else "▼$delta",
                style = MaterialTheme.typography.labelSmall,
                color = if (delta > 0) DreamTrailColors.SageMist else DreamTrailColors.CoralWarn
            )
        }
    }
}

private object RegionIdNameHelper {
    fun materialName(materialId: String): String =
        com.dreamtrail.core.model.RegionId.entries.firstOrNull { it.materialId == materialId }?.materialName ?: "材料"
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = DreamTrailColors.CardElevated
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = DreamTrailColors.MutedInk)
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
