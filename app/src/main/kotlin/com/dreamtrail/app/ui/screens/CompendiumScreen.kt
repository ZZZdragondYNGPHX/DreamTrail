package com.dreamtrail.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dreamtrail.app.data.repository.AppUiDataState
import com.dreamtrail.app.ui.components.JournalCard
import com.dreamtrail.app.ui.components.TagBadge
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.ProgressionEngine
import com.dreamtrail.core.model.AffixId
import com.dreamtrail.core.model.RegionId

private enum class CompendiumSection(val label: String) {
    COMPANIONS("伙伴"),
    REGIONS("地区"),
    CHRONICLES("秘闻"),
    ENEMIES("魔物"),
    GEAR("装备"),
    SOUVENIRS("风物"),
    MILESTONES("成就")
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CompendiumScreen(
    dataState: AppUiDataState,
    statusNotice: String? = null,
    errorMessage: String? = null,
    onGiftCompanion: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var section by remember { mutableStateOf(CompendiumSection.COMPANIONS) }
    val discovery = dataState.discoveryState

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (item in CompendiumSection.entries) {
                val active = item == section
                OutlinedButton(
                    onClick = { section = item },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .widthIn(min = 64.dp)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (active) DreamTrailColors.LanternAmber else DreamTrailColors.CardSurface,
                        contentColor = if (active) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText
                    )
                ) {
                    Text(item.label, maxLines = 1)
                }
            }
        }

        statusNotice?.let { notice ->
            Text(
                text = notice,
                color = DreamTrailColors.SageMist,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        errorMessage?.let { error ->
            Text(
                text = error,
                color = DreamTrailColors.CoralWarn,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (section) {
                CompendiumSection.COMPANIONS -> {
                    val player = dataState.playerState
                    items(ContentCatalog.companionStories, key = { it.id }) { story ->
                        val unlockedChapters = discovery.companionChapters[story.id] ?: 0
                        val affinity = discovery.companionAffinity[story.id] ?: 0
                        val affinityRank = ProgressionEngine.companionAffinityRank(affinity)
                        val giftMatId = story.dispatchRegionId.materialId
                        val canGift = unlockedChapters >= 1 &&
                            player.resources.gold >= 60L &&
                            player.resources.materialCount(giftMatId) >= 4L
                        JournalCard {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${story.name} · ${story.title}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DreamTrailColors.ParchmentText
                                )
                                TagBadge(
                                    text = "章节 $unlockedChapters/${story.chapters.size} · 羁绊Lv.$affinityRank($affinity)",
                                    accent = DreamTrailColors.LanternAmber
                                )
                            }
                            Text(
                                text = "羁绊加成（Lv.$affinityRank/5）：Lv.2留守+25% · Lv.3战技+25% · Lv.4永久属性 · Lv.5非随行半效光环",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                            if (story.combatSkillName.isNotBlank()) {
                                Text(
                                    text = "随行战技「${story.combatSkillName}」：${story.combatRoleSummary}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DreamTrailColors.StarlightBlue
                                )
                            }
                            if (story.dispatchRoleSummary.isNotBlank()) {
                                Text(
                                    text = "留守协理：${story.dispatchRoleSummary}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DreamTrailColors.SageMist
                                )
                            }
                            if (unlockedChapters >= 1) {
                                OutlinedButton(
                                    onClick = { onGiftCompanion(story.id) },
                                    enabled = canGift,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("赠送补给（60金 + 4${story.dispatchRegionId.materialName} → 默契+4）")
                                }
                            }
                            for (ch in story.chapters) {
                                val unlocked = ch.chapterIndex <= unlockedChapters
                                Surface(
                                    color = DreamTrailColors.CardElevated,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = ch.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (unlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                                        )
                                        Text(
                                            text = if (unlocked) ch.summary else "解锁条件：旅舍 Lv.${ch.requiredInnLevel} 且 ${ch.regionId.displayName}调查度达 ${ch.requiredInvestigation}，累计有效探索 ${ch.requiredExploreHours} 小时",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (unlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                CompendiumSection.CHRONICLES -> {
                    item {
                        val completedCount = ContentCatalog.chronicles.count {
                            (discovery.chronicleProgress[it.id] ?: 0) >= 3
                        }
                        Text(
                            text = "区域秘闻录：已完成 $completedCount / ${ContentCatalog.chronicles.size}（完成全部三章将获永久属性与探索加成）",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.LanternAmber
                        )
                    }
                    items(ContentCatalog.chronicles, key = { it.id }) { chron ->
                        val stageDone = (discovery.chronicleProgress[chron.id] ?: 0).coerceIn(0, 3)
                        val isComplete = stageDone >= 3
                        JournalCard {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${chron.title}（${chron.regionId.displayName}）",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DreamTrailColors.ParchmentText
                                )
                                TagBadge(
                                    text = if (isComplete) "已完结 · ${chron.completionTitle}" else "进度 $stageDone/3",
                                    accent = if (isComplete) DreamTrailColors.LanternAmber else DreamTrailColors.SageMist
                                )
                            }
                            Text(
                                text = chron.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                            val rewardText = buildList {
                                if (chron.rewardStats.maxHp > 0) add("生命+${chron.rewardStats.maxHp}")
                                if (chron.rewardStats.attack > 0) add("攻击+${chron.rewardStats.attack}")
                                if (chron.rewardStats.defense > 0) add("防护+${chron.rewardStats.defense}")
                                if (chron.rewardStats.speed > 0) add("速度+${chron.rewardStats.speed}")
                                if (chron.rewardGoldBonusPercent > 0) add("金币+${chron.rewardGoldBonusPercent}%")
                                if (chron.rewardXpBonusPercent > 0) add("经验+${chron.rewardXpBonusPercent}%")
                                if (chron.rewardMaterialBonusPercent > 0) add("材料+${chron.rewardMaterialBonusPercent}%")
                            }.joinToString(" · ")
                            Text(
                                text = "完结永久奖励：$rewardText",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isComplete) DreamTrailColors.LanternAmber else DreamTrailColors.StarlightBlue
                            )
                            for (st in chron.stages) {
                                val done = st.stageIndex <= stageDone
                                Surface(
                                    color = DreamTrailColors.CardElevated,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = if (done) "✓ ${st.title}" else "○ ${st.title}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (done) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                                        )
                                        Text(
                                            text = if (done) st.summary else "推进条件：${chron.regionId.displayName}调查度达 ${st.requiredInvestigation} 且本区累计有效探索 ${st.requiredExploreHours} 小时",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (done) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                CompendiumSection.REGIONS -> {
                    items(RegionId.entries) { region ->
                        val inv = discovery.regionInvestigation[region.id] ?: 0
                        val regRoutes = ContentCatalog.routes.filter { it.regionId == region }
                        JournalCard {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = region.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DreamTrailColors.ParchmentText
                                )
                                TagBadge("调查度 $inv", DreamTrailColors.SageMist)
                            }
                            Text(
                                text = region.atmosphere,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DreamTrailColors.MutedInk
                            )
                            for (r in regRoutes) {
                                val unlocked = r.id in discovery.unlockedRouteIds
                                Text(
                                    text = if (unlocked) "✓ ${r.name}（${r.dangerTag}）" else "○ ${r.name} · ${r.unlockDescription}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (unlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                                )
                            }
                        }
                    }
                }

                CompendiumSection.ENEMIES -> {
                    item {
                        val masteredCount = discovery.enemyKillCounts.values.count { it >= 15 }
                        Text(
                            text = "已记录魔物：${discovery.discoveredEnemyIds.size} / ${ContentCatalog.enemies.size} · 精通图鉴：$masteredCount 种",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.LanternAmber
                        )
                    }
                    items(ContentCatalog.enemies, key = { it.id }) { enemy ->
                        val seen = enemy.id in discovery.discoveredEnemyIds
                        val kills = discovery.enemyKillCounts[enemy.id] ?: 0
                        val masteryLabel = when {
                            kills >= 15 -> " · 精通(对其增伤 + 永久生命+2/攻+1)"
                            kills >= 5 -> " · 熟悉(对其伤害+8%)"
                            else -> ""
                        }
                        JournalCard {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (seen) enemy.name else "未遭遇的${enemy.regionId.displayName}生物",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (seen) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                                )
                                TagBadge(
                                    text = if (enemy.isBoss) "区域首领" else enemy.regionId.displayName,
                                    accent = if (enemy.isBoss) DreamTrailColors.CoralWarn else DreamTrailColors.SageMist
                                )
                            }
                            Text(
                                text = if (seen) {
                                    val traitLine = enemy.bossTrait?.let { "\n首领特性「${it.title}」：${it.description}" } ?: ""
                                    "${enemy.description}\n生命 ${enemy.stats.maxHp} · 攻 ${enemy.stats.attack} · 防 ${enemy.stats.defense} · 速 ${enemy.stats.speed}\n已击败 $kills 次$masteryLabel$traitLine"
                                } else {
                                    "前往${enemy.regionId.displayName}冒险后即可收录其习性与属性。"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                    }
                }

                CompendiumSection.GEAR -> {
                    item {
                        Text(
                            text = "装备图鉴：${discovery.discoveredEquipmentDefIds.size} / ${ContentCatalog.equipmentDefs.size} · 词条：${discovery.discoveredAffixIds.size} / ${AffixId.entries.size}",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.LanternAmber
                        )
                    }
                    items(ContentCatalog.equipmentDefs, key = { it.id }) { eq ->
                        val found = eq.id in discovery.discoveredEquipmentDefIds
                        JournalCard {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${eq.name}（${eq.slot.displayName} · ${eq.tier}阶）",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DreamTrailColors.ParchmentText
                                )
                                TagBadge(
                                    text = if (found) "已收录" else "未发现",
                                    accent = if (found) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                )
                            }
                            Text(
                                text = eq.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                            Text(
                                text = "常见词条「${eq.defaultAffix.displayName}」：${eq.defaultAffix.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.StarlightBlue
                            )
                        }
                    }
                }

                CompendiumSection.SOUVENIRS -> {
                    item {
                        Text(
                            text = "旅途风物志：${discovery.unlockedSouvenirIds.size} / ${ContentCatalog.souvenirs.size}（完整探索 1 小时以上概率带回区域藏品）",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.LanternAmber
                        )
                    }
                    items(ContentCatalog.souvenirs, key = { it.id }) { souv ->
                        val found = souv.id in discovery.unlockedSouvenirIds
                        JournalCard {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (found) "${souv.name}（${souv.regionId.displayName}）" else "未收录风物（${souv.regionId.displayName}）",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (found) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                                )
                                TagBadge(
                                    text = if (found) "已收录" else "未发现",
                                    accent = if (found) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk
                                )
                            }
                            Text(
                                text = if (found) souv.description else "在${souv.regionId.displayName}完成 1 小时以上夜间旅程有机会拾得此风物。",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                            val bonuses = buildList {
                                if (souv.bonusStats.maxHp > 0) add("生命+${souv.bonusStats.maxHp}")
                                if (souv.bonusStats.attack > 0) add("攻击+${souv.bonusStats.attack}")
                                if (souv.bonusStats.defense > 0) add("防护+${souv.bonusStats.defense}")
                                if (souv.bonusStats.speed > 0) add("速度+${souv.bonusStats.speed}")
                                if (souv.goldBonusPercent > 0) add("金币+${souv.goldBonusPercent}%")
                                if (souv.xpBonusPercent > 0) add("经验+${souv.xpBonusPercent}%")
                                if (souv.materialBonusPercent > 0) add("材料+${souv.materialBonusPercent}%")
                            }.joinToString(" · ")
                            Text(
                                text = "收藏加成：$bonuses",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (found) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                            )
                        }
                    }
                }

                CompendiumSection.MILESTONES -> {
                    item {
                        Text(
                            text = "旅途成就与称号：${discovery.unlockedMilestoneIds.size} / ${ContentCatalog.milestones.size} · 首领信物：${discovery.unlockedKeepsakeIds.size} / ${ContentCatalog.keepsakes.size}",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.LanternAmber
                        )
                    }
                    items(ContentCatalog.milestones, key = { it.id }) { ms ->
                        val unlocked = ms.id in discovery.unlockedMilestoneIds
                        JournalCard {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = ms.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (unlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                                )
                                TagBadge(
                                    text = if (unlocked) "已达成" else "未达成",
                                    accent = if (unlocked) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk
                                )
                            }
                            Text(
                                text = "条件：${ms.requirementSummary}",
                                style = MaterialTheme.typography.bodySmall,
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
            }
        }
    }
}
