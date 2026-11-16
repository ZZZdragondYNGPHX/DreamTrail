package com.dreamtrail.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.dreamtrail.app.data.repository.AppUiDataState
import com.dreamtrail.app.ui.components.CharacterPortraitBox
import com.dreamtrail.app.ui.components.JournalCard
import com.dreamtrail.app.ui.components.TagBadge
import com.dreamtrail.app.ui.components.companionAccentColor
import com.dreamtrail.app.ui.components.regionAccentColor
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.ProgressionEngine
import com.dreamtrail.core.model.CompanionStoryDef

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanionsScreen(
    dataState: AppUiDataState,
    onSelectCompanion: (String?) -> Unit,
    onGiftCompanion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val player = dataState.playerState
    val discovery = dataState.discoveryState
    val companions = ContentCatalog.companionStories
    var selectedCompanionId by rememberSaveable {
        mutableStateOf(
            player.activeCompanionId
                ?: companions.firstOrNull { (discovery.companionChapters[it.id] ?: 0) >= 1 }?.id
                ?: companions.first().id
        )
    }
    val story = companions.firstOrNull { it.id == selectedCompanionId } ?: companions.first()
    val unlockedChapters = discovery.companionChapters[story.id] ?: 0
    val affinity = discovery.companionAffinity[story.id] ?: 0
    val affinityRank = ProgressionEngine.companionAffinityRank(affinity)
    val isEscort = player.activeCompanionId == story.id
    val giftMatId = story.dispatchRegionId.materialId
    val canGift = unlockedChapters >= 1 &&
        player.resources.gold >= 60L &&
        player.resources.materialCount(giftMatId) >= 4L

    var selectedChapterIndex by rememberSaveable(story.id) {
        mutableIntStateOf(
            unlockedChapters.coerceAtLeast(1).coerceAtMost(story.chapters.size.coerceAtLeast(1))
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top active escort & camp dispatch summary bar
        item {
            val activeCompanion = player.activeCompanionId?.let { ContentCatalog.companionById(it) }
            val campDispatchers = companions.filter {
                (discovery.companionChapters[it.id] ?: 0) >= 1 && it.id != player.activeCompanionId
            }
            JournalCard(
                containerColor = DreamTrailColors.CardElevated,
                borderColor = companionAccentColor(activeCompanion?.id).copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CharacterPortraitBox(
                        portraitKey = activeCompanion?.id,
                        size = 48.dp,
                        accentColor = companionAccentColor(activeCompanion?.id),
                        locked = activeCompanion == null
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (activeCompanion != null) "今夜随行：${activeCompanion.name} · ${activeCompanion.title}" else "今夜独行（未指定随行伙伴）",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.ParchmentText
                        )
                        Text(
                            text = if (campDispatchers.isEmpty()) "其余已结识伙伴将在营地自动留守协理"
                            else "营地留守协理：${campDispatchers.joinToString("、") { it.name }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.MutedInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (activeCompanion != null) {
                        OutlinedButton(
                            onClick = { onSelectCompanion(null) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("改独行", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 5-companion portrait gallery selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (comp in companions) {
                    val chCount = discovery.companionChapters[comp.id] ?: 0
                    val rank = ProgressionEngine.companionAffinityRank(discovery.companionAffinity[comp.id] ?: 0)
                    val isSelected = comp.id == story.id
                    val compAccent = companionAccentColor(comp.id)
                    val escorting = player.activeCompanionId == comp.id

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) compAccent.copy(alpha = 0.16f) else DreamTrailColors.CardSurface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) compAccent else DreamTrailColors.CardOutline
                        ),
                        modifier = Modifier.selectable(selected = isSelected, role = Role.Tab) { selectedCompanionId = comp.id }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CharacterPortraitBox(
                                portraitKey = comp.id,
                                size = 60.dp,
                                accentColor = compAccent,
                                locked = chCount == 0,
                                badgeText = when {
                                    escorting -> "随行"
                                    chCount > 0 -> "Lv.$rank"
                                    else -> null
                                }
                            )
                            Text(
                                text = comp.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (chCount > 0) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                            )
                        }
                    }
                }
            }
        }

        // Main Character Stage for selected companion
        item {
            CompanionDetailStageCard(
                story = story,
                unlockedChapters = unlockedChapters,
                affinity = affinity,
                affinityRank = affinityRank,
                isEscort = isEscort,
                canGift = canGift,
                playerGold = player.resources.gold,
                ownedGiftMat = player.resources.materialCount(giftMatId),
                onToggleEscort = {
                    if (isEscort) onSelectCompanion(null) else onSelectCompanion(story.id)
                },
                onGiftCompanion = { onGiftCompanion(story.id) }
            )
        }

        // Side-by-side Combat vs Camp Dispatch Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val auraActive = unlockedChapters >= 1 && !isEscort && affinityRank >= 5
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = DreamTrailColors.CardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isEscort || auraActive) DreamTrailColors.StarlightBlue.copy(alpha = 0.65f) else DreamTrailColors.CardOutline
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = DreamTrailColors.StarlightBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "随行战技",
                                style = MaterialTheme.typography.titleSmall,
                                color = DreamTrailColors.StarlightBlue
                            )
                        }
                        Text(
                            text = story.combatSkillName.ifBlank { "夜行护卫" },
                            style = MaterialTheme.typography.labelLarge,
                            color = DreamTrailColors.ParchmentText
                        )
                        Text(
                            text = story.combatRoleSummary.ifBlank { "随行触发协同战斗增益" },
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.MutedInk
                        )
                        when {
                            isEscort -> TagBadge("今夜生效中", DreamTrailColors.LanternAmber)
                            auraActive -> TagBadge("Lv.5 半效光环", DreamTrailColors.StarlightBlue)
                            unlockedChapters > 0 -> TagBadge("留守待命", DreamTrailColors.MutedInk)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = DreamTrailColors.CardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (unlockedChapters > 0 && !isEscort) DreamTrailColors.SageMist.copy(alpha = 0.65f) else DreamTrailColors.CardOutline
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HomeWork,
                                contentDescription = null,
                                tint = DreamTrailColors.SageMist,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "留守协理",
                                style = MaterialTheme.typography.titleSmall,
                                color = DreamTrailColors.SageMist
                            )
                        }
                        Text(
                            text = "${story.dispatchRegionId.displayName} · ${story.dispatchRegionId.materialName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = regionAccentColor(story.dispatchRegionId)
                        )
                        Text(
                            text = story.dispatchRoleSummary.ifBlank { "驻守营地自动采集与整理补给" },
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.MutedInk
                        )
                        if (unlockedChapters > 0 && !isEscort) {
                            TagBadge(
                                text = if (affinityRank >= 2) "协理中 (+25%)" else "协理生效中",
                                accent = DreamTrailColors.SageMist
                            )
                        }
                    }
                }
            }
        }

        // Story Reader Tabs & Parchment Scroll
        item {
            JournalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        tint = DreamTrailColors.LanternAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "同行手记 · 篇章故事",
                        style = MaterialTheme.typography.titleMedium,
                        color = DreamTrailColors.ParchmentText,
                        modifier = Modifier.weight(1f)
                    )
                    TagBadge("$unlockedChapters/${story.chapters.size} 已解锁", DreamTrailColors.LanternAmber)
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (ch in story.chapters) {
                        val unlocked = ch.chapterIndex <= unlockedChapters
                        val selected = ch.chapterIndex == selectedChapterIndex
                        FilterChip(
                            selected = selected,
                            onClick = { selectedChapterIndex = ch.chapterIndex },
                            label = {
                                Text(
                                    text = if (unlocked) "第${ch.chapterIndex}章" else "第${ch.chapterIndex}章(锁)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DreamTrailColors.LanternAmber.copy(alpha = 0.22f),
                                selectedLabelColor = DreamTrailColors.LanternAmber
                            )
                        )
                    }
                }

                val currentChapter = story.chapters.firstOrNull { it.chapterIndex == selectedChapterIndex }
                    ?: story.chapters.first()
                val chapterUnlocked = currentChapter.chapterIndex <= unlockedChapters

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DreamTrailColors.CardElevated,
                    border = BorderStroke(
                        1.dp,
                        if (chapterUnlocked) companionAccentColor(story.id).copy(alpha = 0.45f) else DreamTrailColors.CardOutline
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = currentChapter.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (chapterUnlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                        )
                        Text(
                            text = if (chapterUnlocked) {
                                currentChapter.summary
                            } else if (currentChapter.chapterIndex == 1 && story.id == "comp_wen") {
                                "解锁条件：累计完成 3 轮白昼专注，或晴岚书院调查度达 2"
                            } else if (currentChapter.chapterIndex == 1 && story.id == "comp_zhaolu") {
                                "解锁条件：累计白昼专注满 8 小时，或累计探索达 416 小时"
                            } else {
                                "解锁条件：旅舍 Lv.${currentChapter.requiredInnLevel} 且 ${currentChapter.regionId.displayName}调查度达 ${currentChapter.requiredInvestigation}，累计有效探索 ${currentChapter.requiredExploreHours} 小时"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (chapterUnlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompanionDetailStageCard(
    story: CompanionStoryDef,
    unlockedChapters: Int,
    affinity: Int,
    affinityRank: Int,
    isEscort: Boolean,
    canGift: Boolean,
    playerGold: Long,
    ownedGiftMat: Long,
    onToggleEscort: () -> Unit,
    onGiftCompanion: () -> Unit
) {
    val accent = companionAccentColor(story.id)
    val nextThreshold = remember(affinityRank) {
        when (affinityRank) {
            1 -> 3
            2 -> 7
            3 -> 12
            4 -> 18
            else -> 18
        }
    }
    val prevThreshold = when (affinityRank) {
        1 -> 0
        2 -> 3
        3 -> 7
        4 -> 12
        else -> 18
    }
    val rankProgress = if (affinityRank >= 5) 1f else {
        ((affinity - prevThreshold).toFloat() / (nextThreshold - prevThreshold).coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    JournalCard(
        borderColor = accent.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CharacterPortraitBox(
                portraitKey = story.id,
                size = 104.dp,
                accentColor = accent,
                locked = unlockedChapters == 0,
                badgeText = if (isEscort) "今夜随行" else null
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = story.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = DreamTrailColors.ParchmentText
                    )
                    TagBadge(
                        text = if (unlockedChapters == 0) "尚未结识" else story.title,
                        accent = if (unlockedChapters == 0) DreamTrailColors.MutedInk else accent
                    )
                }

                // Heart rank gauge
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (rankIdx in 1..5) {
                        val filled = unlockedChapters > 0 && rankIdx <= affinityRank
                        Icon(
                            imageVector = if (filled) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = if (filled) Color(0xFFF08A9D) else DreamTrailColors.MutedInk.copy(alpha = 0.45f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    }
                    Text(
                        text = if (unlockedChapters > 0) "羁绊 Lv.$affinityRank/5 · 默契 $affinity" else "结识后开启羁绊",
                        style = MaterialTheme.typography.labelMedium,
                        color = DreamTrailColors.ParchmentText
                    )
                }

                if (unlockedChapters > 0) {
                    LinearProgressIndicator(
                        progress = { rankProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Color(0xFFF08A9D),
                        trackColor = DreamTrailColors.CardElevated
                    )
                } else {
                    val ch1 = story.chapters.firstOrNull()
                    if (ch1 != null) {
                        val condText = when (story.id) {
                            "comp_wen" -> "结识条件：累计完成 3 轮白昼专注，或晴岚书院调查度达 2"
                            "comp_zhaolu" -> "结识条件：累计专注满 8 小时，或累计探索达 416 小时"
                            else -> "结识条件：旅舍 Lv.${ch1.requiredInnLevel} · ${ch1.regionId.displayName}调查 ${ch1.requiredInvestigation} · 探索 ${ch1.requiredExploreHours}h"
                        }
                        Text(
                            text = condText,
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.MutedInk
                        )
                    }
                }
            }
        }

        if (unlockedChapters >= 1 && story.studyGreeting.isNotBlank()) {
            Text(
                text = story.studyGreeting,
                style = MaterialTheme.typography.bodySmall,
                color = DreamTrailColors.SageMist
            )
        }

        // Affinity perk pills
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                2 to "Lv.2 留守+25%",
                3 to "Lv.3 战技+25%",
                4 to "Lv.4 永久属性",
                5 to "Lv.5 留守半效光环"
            ).forEach { (reqRank, label) ->
                val active = unlockedChapters > 0 && affinityRank >= reqRank
                TagBadge(
                    text = label,
                    accent = if (active) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk
                )
            }
        }

        if (unlockedChapters >= 1) {
            Text(
                text = "赠礼需 60 金币和 4 ${story.dispatchRegionId.materialName}（现有 $playerGold 金币、$ownedGiftMat 材料）",
                style = MaterialTheme.typography.bodySmall,
                color = if (canGift) DreamTrailColors.MutedInk else DreamTrailColors.CoralWarn
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onToggleEscort,
                    colors = if (isEscort) {
                        ButtonDefaults.buttonColors(
                            containerColor = DreamTrailColors.CardElevated,
                            contentColor = DreamTrailColors.ParchmentText
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                ) {
                    Text(if (isEscort) "设为留守" else "今夜随行")
                }

                OutlinedButton(
                    onClick = onGiftCompanion,
                    enabled = canGift,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                ) {
                    Text(
                        text = "赠送补给 · 默契+4",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
