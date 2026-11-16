package com.dreamtrail.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dreamtrail.app.data.repository.AppUiDataState
import com.dreamtrail.app.ui.components.ArchetypeEmblem
import com.dreamtrail.app.ui.components.EnemyPortraitTile
import com.dreamtrail.app.ui.components.EquipmentIconTile
import com.dreamtrail.app.ui.components.FocusSpiritAvatar
import com.dreamtrail.app.ui.components.JournalCard
import com.dreamtrail.app.ui.components.KeepsakeIconTile
import com.dreamtrail.app.ui.components.SouvenirIconTile
import com.dreamtrail.app.ui.components.TagBadge
import com.dreamtrail.app.ui.components.drawRegionBackdrop
import com.dreamtrail.app.ui.components.regionAccentColor
import com.dreamtrail.app.ui.components.tierAccentColor
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.model.AffixId
import com.dreamtrail.core.model.EnemyDef
import com.dreamtrail.core.model.EquipmentDef
import com.dreamtrail.core.model.EquipmentSlot
import com.dreamtrail.core.model.FocusSpiritDef
import com.dreamtrail.core.model.KeepsakeDef
import com.dreamtrail.core.model.RegionId
import com.dreamtrail.core.model.SouvenirDef

private enum class CompendiumSection(val label: String) {
    ENEMIES("魔物"),
    GEAR("装备"),
    SPIRITS("灵兽"),
    SOUVENIRS("风物"),
    REGIONS("秘闻与地区"),
    MILESTONES("成就与信物")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompendiumScreen(
    dataState: AppUiDataState,
    onNavigateToCompanions: () -> Unit = {},
    onSelectSpirit: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var section by rememberSaveable { mutableStateOf(CompendiumSection.ENEMIES) }
    var enemyRegionFilter by rememberSaveable { mutableStateOf<RegionId?>(null) }
    var gearSlotFilter by rememberSaveable { mutableStateOf<EquipmentSlot?>(null) }
    var expandedRegionId by rememberSaveable { mutableStateOf<String?>(RegionId.MISTWOOD.id) }

    var inspectedEnemy by remember { mutableStateOf<EnemyDef?>(null) }
    var inspectedGear by remember { mutableStateOf<EquipmentDef?>(null) }
    var inspectedSouvenir by remember { mutableStateOf<SouvenirDef?>(null) }
    var inspectedKeepsake by remember { mutableStateOf<KeepsakeDef?>(null) }
    var inspectedSpirit by remember { mutableStateOf<FocusSpiritDef?>(null) }

    val discovery = dataState.discoveryState

    val filteredEnemies = remember(enemyRegionFilter) {
        ContentCatalog.enemies.filter { enemyRegionFilter == null || it.regionId == enemyRegionFilter }
    }
    val enemyRows = remember(filteredEnemies) { filteredEnemies.chunked(3) }

    val filteredGear = remember(gearSlotFilter) {
        ContentCatalog.equipmentDefs.filter { gearSlotFilter == null || it.slot == gearSlotFilter }
    }
    val gearRows = remember(filteredGear) { filteredGear.chunked(3) }
    val souvenirRows = remember { ContentCatalog.souvenirs.chunked(3) }
    val keepsakeRows = remember { ContentCatalog.keepsakes.chunked(3) }

    Column(modifier = modifier.fillMaxSize()) {
        // Quick banner pointing to the promoted Companions top tab
        val unlockedCompCount = ContentCatalog.companionStories.count {
            (discovery.companionChapters[it.id] ?: 0) >= 1
        }
        Surface(
            color = DreamTrailColors.CardElevated,
            border = BorderStroke(1.dp, DreamTrailColors.CardOutline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onNavigateToCompanions)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Groups,
                    contentDescription = null,
                    tint = DreamTrailColors.LanternAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "伙伴手记 · 已结识 $unlockedCompCount/${ContentCatalog.companionStories.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = DreamTrailColors.ParchmentText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = DreamTrailColors.LanternAmber,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        ScrollableTabRow(
            selectedTabIndex = section.ordinal,
            containerColor = DreamTrailColors.CardSurface,
            contentColor = DreamTrailColors.LanternAmber,
            edgePadding = 12.dp
        ) {
            CompendiumSection.entries.forEach { tab ->
                Tab(
                    selected = section == tab,
                    onClick = { section = tab },
                    text = {
                        Text(
                            text = tab.label,
                            color = if (section == tab) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (section) {
                CompendiumSection.ENEMIES -> {
                    item {
                        val masteredCount = discovery.enemyKillCounts.values.count { it >= 15 }
                        JournalCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "魔物图鉴（点按查看属性与特性）",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DreamTrailColors.ParchmentText
                                )
                                TagBadge(
                                    text = "${discovery.discoveredEnemyIds.size}/${ContentCatalog.enemies.size} · 精通$masteredCount",
                                    accent = DreamTrailColors.LanternAmber
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = enemyRegionFilter == null,
                                    onClick = { enemyRegionFilter = null },
                                    label = { Text("全区域") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DreamTrailColors.LanternAmber.copy(alpha = 0.22f),
                                        selectedLabelColor = DreamTrailColors.LanternAmber
                                    )
                                )
                                for (region in RegionId.entries) {
                                    FilterChip(
                                        selected = enemyRegionFilter == region,
                                        onClick = { enemyRegionFilter = region },
                                        label = { Text(region.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = regionAccentColor(region).copy(alpha = 0.22f),
                                            selectedLabelColor = regionAccentColor(region)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    items(enemyRows, key = { row -> row.first().id }) { rowEnemies ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (enemy in rowEnemies) {
                                val seen = enemy.id in discovery.discoveredEnemyIds
                                val kills = discovery.enemyKillCounts[enemy.id] ?: 0
                                Box(modifier = Modifier.weight(1f)) {
                                    EnemyPortraitTile(
                                        enemy = enemy,
                                        discovered = seen,
                                        killCount = kills,
                                        isSelected = inspectedEnemy?.id == enemy.id,
                                        onClick = { inspectedEnemy = enemy }
                                    )
                                }
                            }
                            repeat(3 - rowEnemies.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                CompendiumSection.GEAR -> {
                    item {
                        JournalCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "全装备图谱（已收录 ${discovery.discoveredEquipmentDefIds.size}/${ContentCatalog.equipmentDefs.size}）",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DreamTrailColors.ParchmentText
                                )
                                TagBadge(
                                    text = "词条 ${discovery.discoveredAffixIds.size}/${AffixId.entries.size}",
                                    accent = DreamTrailColors.StarlightBlue
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = gearSlotFilter == null,
                                    onClick = { gearSlotFilter = null },
                                    label = { Text("全部") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DreamTrailColors.LanternAmber.copy(alpha = 0.22f),
                                        selectedLabelColor = DreamTrailColors.LanternAmber
                                    )
                                )
                                for (slot in EquipmentSlot.entries) {
                                    FilterChip(
                                        selected = gearSlotFilter == slot,
                                        onClick = { gearSlotFilter = slot },
                                        label = { Text(slot.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = DreamTrailColors.LanternAmber.copy(alpha = 0.22f),
                                            selectedLabelColor = DreamTrailColors.LanternAmber
                                        )
                                    )
                                }
                            }
                        }
                    }

                    items(gearRows, key = { row -> row.first().id }) { rowGear ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (eq in rowGear) {
                                val found = eq.id in discovery.discoveredEquipmentDefIds
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { inspectedGear = eq },
                                    shape = RoundedCornerShape(14.dp),
                                    color = DreamTrailColors.CardSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (found) tierAccentColor(eq.tier).copy(alpha = 0.55f) else DreamTrailColors.CardOutline
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        EquipmentIconTile(
                                            def = eq,
                                            level = 1,
                                            affix = eq.defaultAffix,
                                            dimmed = !found,
                                            tileSize = 66.dp
                                        )
                                        Text(
                                            text = eq.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (found) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (found) "已收录" else "未发现",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (found) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                        )
                                    }
                                }
                            }
                            repeat(3 - rowGear.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                CompendiumSection.SPIRITS -> {
                    item {
                        JournalCard {
                            Text(
                                text = "伴读小灵兽名录：${discovery.unlockedSpiritIds.size} / ${ContentCatalog.focusSpirits.size}",
                                style = MaterialTheme.typography.titleSmall,
                                color = DreamTrailColors.LanternAmber
                            )
                            Text(
                                text = "伴读灵兽在探索各境与专注研习期间随机结识（白昼专注解锁速度翻倍）。收集即可永久提供属性，还可请 1 只坐在书桌旁陪伴。",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                    }

                    val spiritRows = ContentCatalog.focusSpirits.chunked(3)
                    items(spiritRows, key = { row -> "sp_${row.first().id}" }) { rowSpirits ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (spirit in rowSpirits) {
                                val unlocked = spirit.id in discovery.unlockedSpiritIds
                                val isActive = dataState.playerState.activeSpiritId == spirit.id
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { inspectedSpirit = spirit },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isActive) DreamTrailColors.SageMist.copy(alpha = 0.14f) else DreamTrailColors.CardSurface,
                                    border = BorderStroke(
                                        width = if (isActive) 1.5.dp else 1.dp,
                                        color = if (isActive) DreamTrailColors.SageMist else if (unlocked) regionAccentColor(spirit.regionId).copy(alpha = 0.6f) else DreamTrailColors.CardOutline
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        FocusSpiritAvatar(
                                            spirit = spirit,
                                            size = 50.dp,
                                            unlocked = unlocked,
                                            active = isActive
                                        )
                                        Text(
                                            text = if (unlocked) spirit.name else "未结识",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (unlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isActive) "伴读中" else spirit.regionId.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isActive) DreamTrailColors.SageMist else regionAccentColor(spirit.regionId),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            repeat(3 - rowSpirits.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                CompendiumSection.SOUVENIRS -> {
                    item {
                        JournalCard {
                            Text(
                                text = "旅途风物陈列柜：${discovery.unlockedSouvenirIds.size} / ${ContentCatalog.souvenirs.size}",
                                style = MaterialTheme.typography.titleSmall,
                                color = DreamTrailColors.LanternAmber
                            )
                            Text(
                                text = "单次完整探索 1 小时以上概率拾得各区域珍稀风物，收录后永久生效属性或掉率加成。",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                    }

                    items(souvenirRows, key = { row -> row.first().id }) { rowSouvs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (souv in rowSouvs) {
                                val found = souv.id in discovery.unlockedSouvenirIds
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { inspectedSouvenir = souv },
                                    shape = RoundedCornerShape(14.dp),
                                    color = DreamTrailColors.CardSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (found) regionAccentColor(souv.regionId).copy(alpha = 0.6f) else DreamTrailColors.CardOutline
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        SouvenirIconTile(
                                            souvenir = souv,
                                            unlocked = found,
                                            onClick = { inspectedSouvenir = souv }
                                        )
                                        Text(
                                            text = if (found) souv.name else "未收录风物",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (found) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = souv.regionId.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = regionAccentColor(souv.regionId)
                                        )
                                    }
                                }
                            }
                            repeat(3 - rowSouvs.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                CompendiumSection.REGIONS -> {
                    item {
                        val completedCount = ContentCatalog.chronicles.count {
                            (discovery.chronicleProgress[it.id] ?: 0) >= 3
                        }
                        Text(
                            text = "区域风貌与秘闻录：已完成 $completedCount/${ContentCatalog.chronicles.size} 卷",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.LanternAmber
                        )
                    }

                    items(RegionId.entries, key = { it.id }) { region ->
                        val inv = discovery.regionInvestigation[region.id] ?: 0
                        val regRoutes = ContentCatalog.routes.filter { it.regionId == region }
                        val chron = ContentCatalog.chronicleForRegion(region)
                        val stageDone = chron?.let { (discovery.chronicleProgress[it.id] ?: 0).coerceIn(0, 3) } ?: 0
                        val accent = regionAccentColor(region)
                        val expanded = expandedRegionId == region.id

                        JournalCard(borderColor = accent.copy(alpha = 0.55f)) {
                            // Scenic biome header
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        expandedRegionId = if (expanded) null else region.id
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawRegionBackdrop(regionId = region, phase = 0.2f, reduceMotion = true)
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            listOf(Color(0xCC0D131E), Color(0x550D131E))
                                        )
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = region.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = DreamTrailColors.ParchmentText
                                        )
                                        Text(
                                            text = "${region.materialName} · 调查度 $inv",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accent
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        chron?.let {
                                            TagBadge(
                                                text = if (stageDone >= 3) "秘闻完结" else "秘闻 $stageDone/3",
                                                accent = if (stageDone >= 3) DreamTrailColors.LanternAmber else DreamTrailColors.StarlightBlue
                                            )
                                        }
                                        TagBadge(
                                            text = if (expanded) "收起" else "详情",
                                            accent = DreamTrailColors.SageMist
                                        )
                                    }
                                }
                            }

                            Text(region.atmosphere, style = MaterialTheme.typography.bodySmall, color = DreamTrailColors.MutedInk)

                            if (expanded) {
                                HorizontalDivider(color = DreamTrailColors.CardOutline)
                                Text("区域路线：", style = MaterialTheme.typography.labelLarge, color = DreamTrailColors.ParchmentText)
                                for (r in regRoutes) {
                                    val unlocked = r.id in discovery.unlockedRouteIds
                                    Text(
                                        text = if (unlocked) "✓ ${r.name}（${r.dangerTag}）" else "○ ${r.name} · ${r.unlockDescription}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (unlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                    )
                                }

                                chron?.let { c ->
                                    HorizontalDivider(color = DreamTrailColors.CardOutline)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "秘闻「${c.title}」",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = DreamTrailColors.LanternAmber
                                        )
                                        if (stageDone >= 3) {
                                            TagBadge(c.completionTitle, DreamTrailColors.LanternAmber)
                                        }
                                    }
                                    val rewardText = buildList {
                                        if (c.rewardStats.maxHp > 0) add("生命+${c.rewardStats.maxHp}")
                                        if (c.rewardStats.attack > 0) add("攻击+${c.rewardStats.attack}")
                                        if (c.rewardStats.defense > 0) add("防护+${c.rewardStats.defense}")
                                        if (c.rewardStats.speed > 0) add("速度+${c.rewardStats.speed}")
                                        if (c.rewardGoldBonusPercent > 0) add("金币+${c.rewardGoldBonusPercent}%")
                                        if (c.rewardXpBonusPercent > 0) add("经验+${c.rewardXpBonusPercent}%")
                                        if (c.rewardMaterialBonusPercent > 0) add("材料+${c.rewardMaterialBonusPercent}%")
                                    }.joinToString(" · ")
                                    Text(
                                        text = "完结永久奖励：$rewardText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (stageDone >= 3) DreamTrailColors.LanternAmber else DreamTrailColors.StarlightBlue
                                    )
                                    for (st in c.stages) {
                                        val done = st.stageIndex <= stageDone
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = DreamTrailColors.CardElevated
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = if (done) "✓ ${st.title}" else "○ ${st.title}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (done) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                                                )
                                                Text(
                                                    text = if (done) st.summary else "需调查度 ${st.requiredInvestigation} 且本区探索 ${st.requiredExploreHours} 小时",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (done) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                CompendiumSection.MILESTONES -> {
                    item {
                        JournalCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "首领信物陈列（铭刻请前往「营地」地图桌）",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DreamTrailColors.ParchmentText
                                )
                                TagBadge(
                                    "${discovery.unlockedKeepsakeIds.size}/${ContentCatalog.keepsakes.size}",
                                    DreamTrailColors.LanternAmber
                                )
                            }
                        }
                    }

                    items(keepsakeRows, key = { row -> "ks_${row.first().id}" }) { rowKs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (ks in rowKs) {
                                val unlocked = ks.id in discovery.unlockedKeepsakeIds
                                Box(modifier = Modifier.weight(1f)) {
                                    KeepsakeIconTile(
                                        keepsake = ks,
                                        unlocked = unlocked,
                                        affordable = false,
                                        onClick = { inspectedKeepsake = ks }
                                    )
                                }
                            }
                            repeat(3 - rowKs.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "旅途成就与称号（${discovery.unlockedMilestoneIds.size}/${ContentCatalog.milestones.size}）",
                                style = MaterialTheme.typography.titleSmall,
                                color = DreamTrailColors.LanternAmber
                            )
                            LinearProgressIndicator(
                                progress = {
                                    (discovery.unlockedMilestoneIds.size.toFloat() / ContentCatalog.milestones.size.coerceAtLeast(1))
                                },
                                modifier = Modifier
                                    .widthIn(min = 90.dp, max = 120.dp)
                                    .height(6.dp),
                                color = DreamTrailColors.LanternAmber,
                                trackColor = DreamTrailColors.CardElevated
                            )
                        }
                    }

                    items(ContentCatalog.milestones.chunked(2), key = { row -> "ms_${row.first().id}" }) { rowMs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (ms in rowMs) {
                                val unlocked = ms.id in discovery.unlockedMilestoneIds
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (unlocked) DreamTrailColors.LanternAmber.copy(alpha = 0.12f) else DreamTrailColors.CardSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (unlocked) DreamTrailColors.LanternAmber.copy(alpha = 0.65f) else DreamTrailColors.CardOutline
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                                                contentDescription = null,
                                                tint = if (unlocked) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = ms.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (unlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = ms.requirementSummary,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (unlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                        )
                                        Text(
                                            text = ms.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DreamTrailColors.MutedInk
                                        )
                                    }
                                }
                            }
                            if (rowMs.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Bestiary Detail Dialog
    inspectedEnemy?.let { enemy ->
        val seen = enemy.id in discovery.discoveredEnemyIds
        val kills = discovery.enemyKillCounts[enemy.id] ?: 0
        Dialog(
            onDismissRequest = { inspectedEnemy = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DreamTrailColors.CardSurface,
                border = BorderStroke(1.5.dp, if (enemy.isBoss) DreamTrailColors.CoralWarn else regionAccentColor(enemy.regionId)),
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
                        Box(modifier = Modifier.widthIn(min = 84.dp, max = 96.dp)) {
                            EnemyPortraitTile(enemy = enemy, discovered = seen, killCount = kills)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (seen) enemy.name else "未知${enemy.regionId.displayName}魔物",
                                style = MaterialTheme.typography.titleMedium,
                                color = DreamTrailColors.ParchmentText
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                TagBadge(enemy.regionId.displayName, regionAccentColor(enemy.regionId))
                                if (enemy.isBoss) TagBadge("区域首领", DreamTrailColors.CoralWarn)
                            }
                            Text(
                                text = when {
                                    kills >= 15 -> "精通等级（已击败 $kills 次 · 永久生+2 攻+1）"
                                    kills >= 5 -> "熟悉等级（已击败 $kills 次 · 对其增伤 +8%）"
                                    else -> "已击败 $kills 次"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = DreamTrailColors.LanternAmber
                            )
                        }
                        IconButton(onClick = { inspectedEnemy = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = DreamTrailColors.MutedInk)
                        }
                    }

                    if (seen) {
                        Surface(shape = RoundedCornerShape(10.dp), color = DreamTrailColors.CardElevated) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text("生命 ${enemy.stats.maxHp}", style = MaterialTheme.typography.labelMedium, color = DreamTrailColors.ParchmentText)
                                Text("攻击 ${enemy.stats.attack}", style = MaterialTheme.typography.labelMedium, color = DreamTrailColors.ParchmentText)
                                Text("防护 ${enemy.stats.defense}", style = MaterialTheme.typography.labelMedium, color = DreamTrailColors.ParchmentText)
                                Text("速度 ${enemy.stats.speed}", style = MaterialTheme.typography.labelMedium, color = DreamTrailColors.ParchmentText)
                            }
                        }
                        Text(enemy.description, style = MaterialTheme.typography.bodyMedium, color = DreamTrailColors.MutedInk)
                        enemy.bossTrait?.let { trait ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DreamTrailColors.CardElevated,
                                border = BorderStroke(1.dp, DreamTrailColors.CoralWarn.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("首领特性「${trait.title}」", style = MaterialTheme.typography.labelLarge, color = DreamTrailColors.CoralWarn)
                                    Text(trait.description, style = MaterialTheme.typography.bodySmall, color = DreamTrailColors.ParchmentText)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "前往${enemy.regionId.displayName}进行夜间探索后即可收录此魔物的习性与属性。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DreamTrailColors.MutedInk
                        )
                    }
                }
            }
        }
    }

    // Gear Codex Dialog
    inspectedGear?.let { eq ->
        val found = eq.id in discovery.discoveredEquipmentDefIds
        Dialog(
            onDismissRequest = { inspectedGear = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DreamTrailColors.CardSurface,
                border = BorderStroke(1.5.dp, tierAccentColor(eq.tier).copy(alpha = 0.65f)),
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
                        EquipmentIconTile(def = eq, level = 1, affix = eq.defaultAffix, dimmed = !found, tileSize = 70.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(eq.name, style = MaterialTheme.typography.titleMedium, color = DreamTrailColors.ParchmentText)
                            Text(
                                text = "T${eq.tier} ${eq.slot.displayName} · 产出${eq.regionId.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = tierAccentColor(eq.tier)
                            )
                            TagBadge(if (found) "已收录" else "未获得", if (found) DreamTrailColors.SageMist else DreamTrailColors.MutedInk)
                        }
                        IconButton(onClick = { inspectedGear = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = DreamTrailColors.MutedInk)
                        }
                    }
                    Text(eq.description, style = MaterialTheme.typography.bodyMedium, color = DreamTrailColors.MutedInk)
                    Surface(shape = RoundedCornerShape(10.dp), color = DreamTrailColors.CardElevated) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ArchetypeEmblem(eq.defaultAffix.archetype, size = 16.dp)
                                Text("常见词条「${eq.defaultAffix.displayName}」", style = MaterialTheme.typography.labelLarge, color = DreamTrailColors.StarlightBlue)
                            }
                            Text(eq.defaultAffix.description, style = MaterialTheme.typography.bodySmall, color = DreamTrailColors.ParchmentText)
                            Text(
                                text = "基础属性：生命+${eq.baseStats.maxHp} · 攻击+${eq.baseStats.attack} · 防护+${eq.baseStats.defense} · 速度+${eq.baseStats.speed}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.SageMist
                            )
                        }
                    }
                }
            }
        }
    }

    // Souvenir Curio Dialog
    inspectedSouvenir?.let { souv ->
        val found = souv.id in discovery.unlockedSouvenirIds
        val bonuses = buildList {
            if (souv.bonusStats.maxHp > 0) add("生命+${souv.bonusStats.maxHp}")
            if (souv.bonusStats.attack > 0) add("攻击+${souv.bonusStats.attack}")
            if (souv.bonusStats.defense > 0) add("防护+${souv.bonusStats.defense}")
            if (souv.bonusStats.speed > 0) add("速度+${souv.bonusStats.speed}")
            if (souv.goldBonusPercent > 0) add("金币+${souv.goldBonusPercent}%")
            if (souv.xpBonusPercent > 0) add("经验+${souv.xpBonusPercent}%")
            if (souv.materialBonusPercent > 0) add("材料+${souv.materialBonusPercent}%")
        }.joinToString(" · ")

        Dialog(
            onDismissRequest = { inspectedSouvenir = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DreamTrailColors.CardSurface,
                border = BorderStroke(1.5.dp, regionAccentColor(souv.regionId).copy(alpha = 0.65f)),
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
                        SouvenirIconTile(souvenir = souv, unlocked = found, onClick = {})
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (found) souv.name else "未收录风物", style = MaterialTheme.typography.titleMedium, color = DreamTrailColors.ParchmentText)
                            TagBadge(souv.regionId.displayName, regionAccentColor(souv.regionId))
                        }
                        IconButton(onClick = { inspectedSouvenir = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = DreamTrailColors.MutedInk)
                        }
                    }
                    Text(
                        text = if (found) souv.description else "在${souv.regionId.displayName}完成 1 小时以上夜间旅程有机会拾得此风物。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DreamTrailColors.MutedInk
                    )
                    Text(
                        text = "永久收藏加成：$bonuses",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (found) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                    )
                }
            }
        }
    }

    // Keepsake Codex Read-Only Dialog
    inspectedKeepsake?.let { ks ->
        val unlocked = ks.id in discovery.unlockedKeepsakeIds
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
                    Text(
                        text = if (unlocked) "已在营地地图桌铭刻并永久生效。" else "需消耗 ${ks.bossMarkCost} 首领印记，在「营地 → 地图桌」中铭刻。",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (unlocked) DreamTrailColors.LanternAmber else DreamTrailColors.SageMist
                    )
                }
            }
        }
    }

    inspectedSpirit?.let { spirit ->
        val unlocked = spirit.id in discovery.unlockedSpiritIds
        val isActive = dataState.playerState.activeSpiritId == spirit.id
        val bonuses = listOfNotNull(
            spirit.bonusStats.maxHp.takeIf { it > 0 }?.let { "生命+$it" },
            spirit.bonusStats.attack.takeIf { it > 0 }?.let { "攻击+$it" },
            spirit.bonusStats.defense.takeIf { it > 0 }?.let { "防御+$it" },
            spirit.bonusStats.speed.takeIf { it > 0 }?.let { "速度+$it" },
            spirit.focusXpBonusPercent.takeIf { it > 0 }?.let { "伴读专注经验+$it%" },
            spirit.inkBonusPercent.takeIf { it > 0 }?.let { "伴读墨晶+$it%" },
            spirit.goldBonusPercent.takeIf { it > 0 }?.let { "伴读金币+$it%" }
        ).joinToString(" · ")

        Dialog(
            onDismissRequest = { inspectedSpirit = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DreamTrailColors.CardSurface,
                border = BorderStroke(1.5.dp, regionAccentColor(spirit.regionId).copy(alpha = 0.65f)),
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
                        FocusSpiritAvatar(spirit = spirit, size = 56.dp, unlocked = unlocked, active = isActive)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (unlocked) "${spirit.name} · ${spirit.title}" else "未结识小灵兽", style = MaterialTheme.typography.titleMedium, color = DreamTrailColors.ParchmentText)
                            TagBadge("${spirit.regionId.displayName} · ${spirit.preferredSoundscape}", regionAccentColor(spirit.regionId))
                        }
                        IconButton(onClick = { inspectedSpirit = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = DreamTrailColors.MutedInk)
                        }
                    }
                    Text(
                        text = if (unlocked) spirit.description else "前往${spirit.regionId.displayName}进行探索或白昼专注，即有机会结识这只小灵兽。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DreamTrailColors.MutedInk
                    )
                    Text(
                        text = "效果：$bonuses",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (unlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                    )
                    if (unlocked) {
                        androidx.compose.material3.Button(
                            onClick = {
                                onSelectSpirit(if (isActive) null else spirit.id)
                                inspectedSpirit = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isActive) "取消伴读" else "设为书桌伴读")
                        }
                    }
                }
            }
        }
    }
}
