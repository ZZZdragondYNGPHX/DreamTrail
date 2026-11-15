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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dreamtrail.app.data.repository.AppUiDataState
import com.dreamtrail.app.ui.components.ArchetypeEmblem
import com.dreamtrail.app.ui.components.CampDioramaCanvas
import com.dreamtrail.app.ui.components.CampStation
import com.dreamtrail.app.ui.components.CharacterPortraitBox
import com.dreamtrail.app.ui.components.EquipmentIconTile
import com.dreamtrail.app.ui.components.JournalCard
import com.dreamtrail.app.ui.components.KeepsakeIconTile
import com.dreamtrail.app.ui.components.ResourceBadgeIcon
import com.dreamtrail.app.ui.components.ResourceSummaryRow
import com.dreamtrail.app.ui.components.ResourceVisualKind
import com.dreamtrail.app.ui.components.TagBadge
import com.dreamtrail.app.ui.components.TeaCupIconBox
import com.dreamtrail.app.ui.components.companionAccentColor
import com.dreamtrail.app.ui.components.materialAccentColor
import com.dreamtrail.app.ui.components.regionAccentColor
import com.dreamtrail.app.ui.components.tierAccentColor
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.ProgressionEngine
import com.dreamtrail.core.model.EquipmentDef
import com.dreamtrail.core.model.FacilityType
import com.dreamtrail.core.model.KeepsakeDef
import com.dreamtrail.core.model.RegionId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampScreen(
    dataState: AppUiDataState,
    onUpgradeFacility: (FacilityType) -> Unit,
    onCraftEquipment: (String) -> Unit,
    onCraftKeepsake: (String) -> Unit,
    onSelectTeaBlend: (String?) -> Unit = {},
    onTransmuteMaterial: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val player = dataState.playerState
    val discovery = dataState.discoveryState
    val workshopLevel = player.facilityLevel(FacilityType.WORKSHOP)
    val teaHearthLevel = player.facilityLevel(FacilityType.TEA_HEARTH)
    val conservatoryLevel = player.facilityLevel(FacilityType.CONSERVATORY)

    var selectedStation by rememberSaveable { mutableStateOf(CampStation.WORKSHOP) }
    var craftTierFilter by rememberSaveable { mutableStateOf<Int?>(null) }
    var mapTableSubView by rememberSaveable { mutableStateOf(0) } // 0 = Keepsakes, 1 = Regions
    var inspectedBlueprint by remember { mutableStateOf<EquipmentDef?>(null) }
    var inspectedKeepsake by remember { mutableStateOf<KeepsakeDef?>(null) }

    val craftDefs = remember(craftTierFilter) {
        ContentCatalog.equipmentDefs.filter { def ->
            craftTierFilter == null || def.tier == craftTierFilter
        }
    }
    val craftRows = remember(craftDefs) { craftDefs.chunked(3) }
    val teaRows = remember { ContentCatalog.teaBlends.chunked(2) }
    val keepsakeRows = remember { ContentCatalog.keepsakes.chunked(3) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ResourceSummaryRow(resources = player.resources)
        }

        // 2D Interactive Camp Diorama
        item {
            JournalCard(borderColor = DreamTrailColors.LanternAmber.copy(alpha = 0.4f)) {
                Text(
                    text = "营地 · 点击建筑查看",
                    style = MaterialTheme.typography.labelLarge,
                    color = DreamTrailColors.ParchmentText
                )
                CampDioramaCanvas(
                    playerState = player,
                    discoveryState = discovery,
                    selectedStation = selectedStation,
                    onSelectStation = { selectedStation = it }
                )

                // 6 Station Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (station in CampStation.entries) {
                        val isSelected = station == selectedStation
                        val badgeSuffix = when (val f = station.facility) {
                            null -> "${discovery.completedCommissionIds.size}/${ContentCatalog.commissions.size}"
                            else -> "Lv.${player.facilityLevel(f)}"
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) DreamTrailColors.LanternAmber else DreamTrailColors.CardElevated,
                            border = BorderStroke(1.dp, if (isSelected) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline),
                            modifier = Modifier.heightIn(min = 48.dp).selectable(selected = isSelected, role = Role.Tab) { selectedStation = station }
                        ) {
                            Text(
                                text = "${station.label} · $badgeSuffix",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // Station Upgrade Card (shared for the 5 facilities)
        selectedStation.facility?.let { facility ->
            item {
                val level = player.facilityLevel(facility)
                val stageLook = facilityVisualDescription(facility, level)
                JournalCard(
                    borderColor = if (level > 0) DreamTrailColors.SageMist.copy(alpha = 0.6f) else DreamTrailColors.CardOutline
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${facility.displayName} · 当前状态",
                                style = MaterialTheme.typography.titleMedium,
                                color = DreamTrailColors.ParchmentText
                            )
                            Text(
                                text = facility.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                        TagBadge(
                            text = if (level == 0) "待修葺" else "Lv.$level / 5",
                            accent = if (level == 0) DreamTrailColors.MutedInk else DreamTrailColors.LanternAmber
                        )
                    }

                    Text(
                        text = stageLook,
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.SageMist
                    )

                    if (level < 5) {
                        val target = level + 1
                        val (goldCost, matPair) = ContentCatalog.facilityUpgradeCost(facility, target)
                        val (matId, matCost) = matPair
                        val matName = RegionId.entries.firstOrNull { it.materialId == matId }?.materialName ?: matId
                        val requiredHours = ContentCatalog.facilityRequiredExploreHours(facility, target)
                        val explored = discovery.totalCreditedSeconds >= requiredHours * 3600L
                        val affordable = explored &&
                            player.resources.gold >= goldCost &&
                            player.resources.materialCount(matId) >= matCost

                        if (!explored) {
                            Text(
                                text = "累计有效探索 ${requiredHours} 小时后解锁（当前 ${discovery.totalCreditedSeconds / 3600} 小时）",
                                style = MaterialTheme.typography.labelSmall,
                                color = DreamTrailColors.CoralWarn
                            )
                        }
                        Button(
                            onClick = { onUpgradeFacility(facility) },
                            enabled = affordable,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Upgrade,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = if (level == 0) {
                                    "修葺 Lv.1（${goldCost}金 + ${matCost}$matName）"
                                } else {
                                    "升级至 Lv.$target（${goldCost}金 + ${matCost}$matName）"
                                }
                            )
                        }
                    } else {
                        TagBadge("已建至满级 Lv.5，所有设施增益生效", DreamTrailColors.LanternAmber)
                    }
                }
            }
        }

        // Station-Specific Workbench
        when (selectedStation) {
            CampStation.WORKSHOP -> {
                item {
                    JournalCard {
                        Text(
                            text = "装备打造 · 工坊 Lv.$workshopLevel",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.ParchmentText
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = craftTierFilter == null,
                                onClick = { craftTierFilter = null },
                                label = { Text("全部阶位") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DreamTrailColors.LanternAmber.copy(alpha = 0.22f),
                                    selectedLabelColor = DreamTrailColors.LanternAmber
                                )
                            )
                            for (tier in 1..6) {
                                FilterChip(
                                    selected = craftTierFilter == tier,
                                    onClick = { craftTierFilter = tier },
                                    label = { Text("T$tier") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = tierAccentColor(tier).copy(alpha = 0.22f),
                                        selectedLabelColor = tierAccentColor(tier)
                                    )
                                )
                            }
                        }
                    }
                }

                items(craftRows, key = { row -> row.first().id }) { rowDefs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (def in rowDefs) {
                            val unlocked = workshopLevel >= def.requiredWorkshopLevel
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { inspectedBlueprint = def },
                                shape = RoundedCornerShape(14.dp),
                                color = DreamTrailColors.CardSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (unlocked) tierAccentColor(def.tier).copy(alpha = 0.55f) else DreamTrailColors.CardOutline
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    EquipmentIconTile(
                                        def = def,
                                        level = 1,
                                        affix = def.defaultAffix,
                                        dimmed = !unlocked,
                                        tileSize = 68.dp
                                    )
                                    Text(
                                        text = def.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (unlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (unlocked) "工坊Lv.${def.requiredWorkshopLevel}" else "需Lv.${def.requiredWorkshopLevel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (unlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                    )
                                }
                            }
                        }
                        repeat(3 - rowDefs.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            CampStation.TEA_HEARTH -> {
                item {
                    Text(
                        text = if (teaHearthLevel == 0) {
                            "茶桌茶罐尚未启用，修葺「茶歇火塘」Lv.1 后即可冲泡安神助眠茶。"
                        } else {
                            "睡前茶席（点选茶杯即可在下次夜行中享用）"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk
                    )
                }

                items(teaRows, key = { row -> row.first().id }) { rowTeas ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (tea in rowTeas) {
                            val unlocked = teaHearthLevel >= tea.requiredTeaHearthLevel
                            val active = player.activeTeaBlendId == tea.id
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = unlocked) {
                                        onSelectTeaBlend(if (active) null else tea.id)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (active) DreamTrailColors.LanternAmber.copy(alpha = 0.14f) else DreamTrailColors.CardSurface,
                                border = BorderStroke(
                                    width = if (active) 1.5.dp else 1.dp,
                                    color = if (active) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TeaCupIconBox(tea = tea, unlocked = unlocked, active = active, size = 42.dp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tea.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (unlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            TagBadge(
                                                text = if (active) "已冲泡" else "火塘Lv.${tea.requiredTeaHearthLevel}",
                                                accent = if (active) DreamTrailColors.LanternAmber else if (unlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                            )
                                        }
                                    }
                                    Text(
                                        text = tea.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DreamTrailColors.MutedInk
                                    )
                                }
                            }
                        }
                        if (rowTeas.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            CampStation.CONSERVATORY -> {
                item {
                    Text(
                        text = if (conservatoryLevel == 0) {
                            "玻璃花房尚在筹备中，修葺至 Lv.1 后开启材料炼金催化配方。"
                        } else {
                            "星尘催化炼金台（当前花房 Lv.$conservatoryLevel）"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk
                    )
                }

                items(ContentCatalog.transmuteRecipes, key = { it.id }) { recipe ->
                    val unlocked = conservatoryLevel >= recipe.requiredConservatoryLevel
                    val inputMatName = RegionId.entries.firstOrNull { it.materialId == recipe.inputMaterialId }?.materialName
                        ?: recipe.inputMaterialId
                    val outMatId = recipe.outputMaterialId
                    val outMatName = outMatId?.let { id ->
                        RegionId.entries.firstOrNull { it.materialId == id }?.materialName ?: id
                    }
                    val affordable = unlocked &&
                        player.resources.gold >= recipe.inputGold &&
                        player.resources.materialCount(recipe.inputMaterialId) >= recipe.inputMaterialAmount

                    JournalCard(
                        borderColor = if (unlocked) DreamTrailColors.SageMist.copy(alpha = 0.55f) else DreamTrailColors.CardOutline
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(recipe.title, style = MaterialTheme.typography.titleSmall, color = DreamTrailColors.ParchmentText)
                                Text(recipe.description, style = MaterialTheme.typography.bodySmall, color = DreamTrailColors.MutedInk)
                            }
                            TagBadge(
                                text = "花房 Lv.${recipe.requiredConservatoryLevel}",
                                accent = if (unlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                            )
                        }

                        // Visual input -> output alchemy bar
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DreamTrailColors.CardElevated
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AlchemyIngredientBadge(
                                        ResourceVisualKind.GOLD,
                                        DreamTrailColors.LanternAmber,
                                        "${recipe.inputGold}金"
                                    )
                                    AlchemyIngredientBadge(
                                        ResourceVisualKind.MATERIAL,
                                        materialAccentColor(recipe.inputMaterialId),
                                        "${recipe.inputMaterialAmount}$inputMatName"
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = DreamTrailColors.LanternAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                FlowRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (outMatId != null && recipe.outputMaterialAmount > 0L) {
                                        AlchemyIngredientBadge(
                                            ResourceVisualKind.MATERIAL,
                                            materialAccentColor(outMatId),
                                            "${recipe.outputMaterialAmount}$outMatName"
                                        )
                                    }
                                    if (recipe.outputBossMarks > 0L) {
                                        AlchemyIngredientBadge(
                                            ResourceVisualKind.BOSS_MARK,
                                            DreamTrailColors.CoralWarn,
                                            "${recipe.outputBossMarks}印记"
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { onTransmuteMaterial(recipe.id) },
                            enabled = affordable,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp)
                        ) {
                            Text(
                                when {
                                    !unlocked -> "需星尘花房 Lv.${recipe.requiredConservatoryLevel}"
                                    !affordable -> "金币或素材不足"
                                    else -> "启动催化炼金"
                                }
                            )
                        }
                    }
                }
            }

            CampStation.MAP_TABLE -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("首领奇物陈列 (${discovery.unlockedKeepsakeIds.size}/${ContentCatalog.keepsakes.size})", "各域调查进度").forEachIndexed { idx, label ->
                            val selected = mapTableSubView == idx
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { mapTableSubView = idx },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) DreamTrailColors.StarlightBlue.copy(alpha = 0.22f) else DreamTrailColors.CardSurface,
                                border = BorderStroke(1.dp, if (selected) DreamTrailColors.StarlightBlue else DreamTrailColors.CardOutline)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) DreamTrailColors.StarlightBlue else DreamTrailColors.MutedInk
                                    )
                                }
                            }
                        }
                    }
                }

                if (mapTableSubView == 0) {
                    items(keepsakeRows, key = { row -> row.first().id }) { rowKs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (ks in rowKs) {
                                val unlocked = ks.id in discovery.unlockedKeepsakeIds
                                val affordable = !unlocked &&
                                    player.resources.bossMarks >= ks.bossMarkCost &&
                                    player.resources.gold >= ks.goldCost &&
                                    player.resources.materialCount(ks.materialId) >= ks.materialCost
                                Box(modifier = Modifier.weight(1f)) {
                                    KeepsakeIconTile(
                                        keepsake = ks,
                                        unlocked = unlocked,
                                        affordable = affordable,
                                        onClick = { inspectedKeepsake = ks }
                                    )
                                }
                            }
                            repeat(3 - rowKs.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(RegionId.entries, key = { it.id }) { region ->
                        val inv = discovery.regionInvestigation[region.id] ?: 0
                        val chron = ContentCatalog.chronicleForRegion(region)
                        val stageDone = chron?.let { (discovery.chronicleProgress[it.id] ?: 0).coerceIn(0, 3) } ?: 0
                        val rAccent = regionAccentColor(region)
                        JournalCard(borderColor = rAccent.copy(alpha = 0.45f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(region.displayName, style = MaterialTheme.typography.titleSmall, color = rAccent)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TagBadge("调查度 $inv", rAccent)
                                    chron?.let { TagBadge("秘闻 $stageDone/3", DreamTrailColors.LanternAmber) }
                                }
                            }
                            LinearProgressIndicator(
                                progress = { (inv / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = rAccent,
                                trackColor = DreamTrailColors.CardElevated
                            )
                            Text(region.atmosphere, style = MaterialTheme.typography.bodySmall, color = DreamTrailColors.MutedInk)
                        }
                    }
                }
            }

            CampStation.INN -> {
                item {
                    val maxPassives = ProgressionEngine.maxPassiveSlots(player)
                    val innLv = player.facilityLevel(FacilityType.INN)
                    JournalCard {
                        Text(
                            text = "山野旅舍寄宿名册（旅舍 Lv.$innLv）",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.ParchmentText
                        )
                        Text(
                            text = "当前旅途被动栏位上限：$maxPassives / 3 槽。提升旅舍等级与探索地区可依次邀请五位伙伴入驻营地。",
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.MutedInk
                        )
                        for (comp in ContentCatalog.companionStories) {
                            val chCount = discovery.companionChapters[comp.id] ?: 0
                            val rank = ProgressionEngine.companionAffinityRank(discovery.companionAffinity[comp.id] ?: 0)
                            val ch1 = comp.chapters.firstOrNull()
                            val isEscort = player.activeCompanionId == comp.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DreamTrailColors.CardElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (chCount > 0) companionAccentColor(comp.id).copy(alpha = 0.55f) else DreamTrailColors.CardOutline
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CharacterPortraitBox(
                                        portraitKey = comp.id,
                                        size = 48.dp,
                                        accentColor = companionAccentColor(comp.id),
                                        locked = chCount == 0
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${comp.name} · ${comp.title}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (chCount > 0) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                                        )
                                        Text(
                                            text = if (chCount > 0) {
                                                if (isEscort) "今夜随行备战中 · 羁绊 Lv.$rank"
                                                else "留守协理：${comp.dispatchRoleSummary}"
                                            } else {
                                                "入驻条件：旅舍 Lv.${ch1?.requiredInnLevel ?: 1} · ${comp.dispatchRegionId.displayName}调查 ${ch1?.requiredInvestigation ?: 0}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (chCount > 0) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                        )
                                    }
                                    TagBadge(
                                        text = when {
                                            chCount == 0 -> "未入驻"
                                            isEscort -> "随行中"
                                            else -> "留守中"
                                        },
                                        accent = if (chCount > 0) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk
                                    )
                                }
                            }
                        }
                    }
                }
            }

            CampStation.COMMISSIONS -> {
                item {
                    val doneCount = discovery.completedCommissionIds.size
                    JournalCard {
                        Text(
                            text = "营地委托告示牌（已完成 $doneCount/${ContentCatalog.commissions.size}）",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.ParchmentText
                        )
                        Text(
                            text = "夜间探险在对应区域达成节点要求并归来结算时，自动验收领取赏格。",
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.MutedInk
                        )
                    }
                }

                items(ContentCatalog.commissions, key = { it.id }) { comm ->
                    val completed = comm.id in discovery.completedCommissionIds
                    val rAccent = regionAccentColor(comm.regionId)
                    JournalCard(
                        borderColor = if (completed) DreamTrailColors.LanternAmber else rAccent.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${comm.title} · ${comm.regionId.displayName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DreamTrailColors.ParchmentText
                                )
                                Text(
                                    text = comm.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DreamTrailColors.MutedInk
                                )
                            }
                            TagBadge(
                                text = if (completed) "已交付" else "需≥${comm.requiredMinNodes}节点",
                                accent = if (completed) DreamTrailColors.LanternAmber else rAccent
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (comm.rewardGold > 0L) {
                                AlchemyIngredientBadge(ResourceVisualKind.GOLD, DreamTrailColors.LanternAmber, "${comm.rewardGold}金")
                            }
                            if (comm.rewardXp > 0L) {
                                AlchemyIngredientBadge(ResourceVisualKind.XP, DreamTrailColors.StarlightBlue, "${comm.rewardXp}经验")
                            }
                            val rr = comm.rewardMaterialRegion
                            if (rr != null && comm.rewardMaterialAmount > 0L) {
                                AlchemyIngredientBadge(
                                    ResourceVisualKind.MATERIAL,
                                    materialAccentColor(rr.materialId),
                                    "${comm.rewardMaterialAmount}${rr.materialName}"
                                )
                            }
                            if (comm.rewardBossMarks > 0L) {
                                AlchemyIngredientBadge(ResourceVisualKind.BOSS_MARK, DreamTrailColors.CoralWarn, "${comm.rewardBossMarks}印记")
                            }
                            if (comm.rewardAffinity > 0) {
                                TagBadge("伙伴默契+${comm.rewardAffinity}", Color(0xFFF08A9D))
                            }
                        }
                    }
                }
            }
        }
    }

    // Workshop Craft Dialog
    inspectedBlueprint?.let { def ->
        val unlocked = workshopLevel >= def.requiredWorkshopLevel
        val (goldCost, matCost) = ProgressionEngine.adjustedCraftCost(def, player, dataState.inventory)
        val affordable = unlocked &&
            player.resources.gold >= goldCost &&
            player.resources.materialCount(def.craftMaterialId) >= matCost

        Dialog(
            onDismissRequest = { inspectedBlueprint = null },
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
                        EquipmentIconTile(def = def, level = 1, affix = def.defaultAffix, tileSize = 70.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(def.name, style = MaterialTheme.typography.titleMedium, color = DreamTrailColors.ParchmentText)
                            Text(
                                text = "T${def.tier} ${def.slot.displayName} · 需工坊 Lv.${def.requiredWorkshopLevel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = tierAccentColor(def.tier)
                            )
                        }
                        IconButton(onClick = { inspectedBlueprint = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = DreamTrailColors.MutedInk)
                        }
                    }
                    Surface(shape = RoundedCornerShape(10.dp), color = DreamTrailColors.CardElevated) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ArchetypeEmblem(def.defaultAffix.archetype, size = 16.dp)
                                Text(
                                    text = "默认词条「${def.defaultAffix.displayName}」",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DreamTrailColors.StarlightBlue
                                )
                            }
                            Text(def.defaultAffix.description, style = MaterialTheme.typography.bodySmall, color = DreamTrailColors.ParchmentText)
                            Text(
                                text = "基础属性：生命+${def.baseStats.maxHp} · 攻击+${def.baseStats.attack} · 防护+${def.baseStats.defense} · 速度+${def.baseStats.speed}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.SageMist
                            )
                        }
                    }
                    Text(
                        text = "锻造消耗：$goldCost 金币（现有 ${player.resources.gold}） + $matCost ${def.regionId.materialName}（现有 ${player.resources.materialCount(def.craftMaterialId)}）",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (affordable) DreamTrailColors.ParchmentText else DreamTrailColors.CoralWarn
                    )
                    Button(
                        onClick = {
                            onCraftEquipment(def.id)
                            inspectedBlueprint = null
                        },
                        enabled = affordable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            when {
                                !unlocked -> "需工坊 Lv.${def.requiredWorkshopLevel}"
                                !affordable -> "金币或素材尚不足"
                                else -> "打造「${def.name}」"
                            }
                        )
                    }
                }
            }
        }
    }

    // Keepsake Detail & Craft Dialog
    inspectedKeepsake?.let { ks ->
        val unlocked = ks.id in discovery.unlockedKeepsakeIds
        val matName = RegionId.entries.firstOrNull { it.materialId == ks.materialId }?.materialName ?: ks.materialId
        val affordable = !unlocked &&
            player.resources.bossMarks >= ks.bossMarkCost &&
            player.resources.gold >= ks.goldCost &&
            player.resources.materialCount(ks.materialId) >= ks.materialCost

        Dialog(
            onDismissRequest = { inspectedKeepsake = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DreamTrailColors.CardSurface,
                border = BorderStroke(1.5.dp, if (unlocked) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline),
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${ks.name}（${ks.regionId.displayName}）", style = MaterialTheme.typography.titleMedium, color = DreamTrailColors.ParchmentText)
                            Text(ks.effectSummary, style = MaterialTheme.typography.labelLarge, color = DreamTrailColors.StarlightBlue)
                        }
                        IconButton(onClick = { inspectedKeepsake = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = DreamTrailColors.MutedInk)
                        }
                    }
                    Text(ks.description, style = MaterialTheme.typography.bodyMedium, color = DreamTrailColors.MutedInk)
                    if (unlocked) {
                        TagBadge("已铭刻并在陈列架永久生效", DreamTrailColors.LanternAmber)
                    } else {
                        Text(
                            text = "铭刻消耗：${ks.bossMarkCost} 首领印记 · ${ks.goldCost} 金币 · ${ks.materialCost} $matName",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (affordable) DreamTrailColors.ParchmentText else DreamTrailColors.CoralWarn
                        )
                        Button(
                            onClick = {
                                onCraftKeepsake(ks.id)
                                inspectedKeepsake = null
                            },
                            enabled = affordable,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text(if (affordable) "铭刻「${ks.name}」" else "印记或资源尚不足")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlchemyIngredientBadge(
    kind: ResourceVisualKind,
    accent: Color,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DreamTrailColors.CardSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ResourceBadgeIcon(kind = kind, accent = accent, size = 13.dp)
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = DreamTrailColors.ParchmentText)
        }
    }
}

private fun facilityVisualDescription(type: FacilityType, level: Int): String = when (type) {
    FacilityType.MAP_TABLE -> when (level) {
        0 -> "积灰的旧木桌，仅能看清营地外侧一条林道。"
        1 -> "铺上防潮油布与雾林草图，更容易辨认沿途线索。"
        2 -> "木桌边缘钉起黄铜罗盘与古树回廊测量尺。"
        3 -> "展开高程图与海岸测量工具，继续记录远方线索。"
        else -> "全幅精细手绘地图铺满整张长桌，夜间风灯照得清清楚楚。"
    }
    FacilityType.WORKSHOP -> when (level) {
        0 -> "露天简易工具架，暂无风箱与锻台。"
        1 -> "搭起木屋顶与小铁砧，可打造 1 阶雾林装备（属性 +5%）。"
        2 -> "添置海砂磨床，可打造 2 阶潮汐滩装备并领悟「巧匠心法」。"
        3 -> "建成高地晶石熔炉，解锁全 3 阶星陨山脊装备打造。"
        4 -> "建起云绒风箱与云栖工台，解锁 4 阶云栖浮岛装备打造（属性 +20%）。"
        else -> "星尘锻炉与全套精工台落成，解锁 5~6 阶装备打造与 Lv.6~7 星尘精铸。"
    }
    FacilityType.INN -> when (level) {
        0 -> "只有一圈石砌篝火与挡风帆布。"
        1 -> "支起木柱帐篷与热茶壶，艾琳会在此歇脚（生命+4，解锁踏浪步法）。"
        2 -> "铺好干草床与松木长凳，诺亚常在夜晚借用桌灯（解锁炉火余温）。"
        3 -> "石墙暖炉旅舍落成，琳带回云绒帐幔（解锁云幕庇护）。"
        4 -> "二层观星阁修建完毕，汐与岑开始常驻，解锁第 3 个旅途被动槽位。"
        else -> "温暖明亮的山野旅舍，陪伴旅人度过每一段安稳夜行。"
    }
    FacilityType.TEA_HEARTH -> when (level) {
        0 -> "空置的鹅卵石炉座，尚待修葺铜壶与草药架。"
        1 -> "支起铜嘴炭炉，可冲泡「甘菊安神饮」（永久生命+3）。"
        2 -> "挂满干燥薄荷与海盐罐，解锁「海盐薄荷茶」及被动「疾风游刃」。"
        3 -> "添置星茴瓷罐与温杯架，解锁「星茴暖身露」。"
        4 -> "建起云雾熏茶屉，解锁「云雾烘青茶」与后期高阶花茶。"
        else -> "满屋清芬的百草火塘，夜间探索可调配全 7 款秘制热茶。"
    }
    FacilityType.CONSERVATORY -> when (level) {
        0 -> "旅舍外侧的小块星辉苗圃，尚待搭起玻璃花房。"
        1 -> "支起暖光玻璃棚，培养基础材料精炼配方并小幅提升基础生命。"
        2 -> "架起雾露滴灌架，解锁进阶材料精炼并提升全队续航。"
        3 -> "引入陨星温床，可直接将中阶素材催化为云絮微尘与琥珀木。"
        4 -> "星尘温室趋于完善，能稳定凝聚首领印记与深海微光。"
        else -> "繁星与草木共生的花房温室，大幅提升属性上限并开启全部催化配方。"
    }
}
