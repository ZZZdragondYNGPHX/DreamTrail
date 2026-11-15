package com.dreamtrail.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.dreamtrail.core.model.FacilityType
import com.dreamtrail.core.model.RegionId

@Composable
fun CampScreen(
    dataState: AppUiDataState,
    statusNotice: String?,
    errorMessage: String?,
    onUpgradeFacility: (FacilityType) -> Unit,
    onCraftEquipment: (String) -> Unit,
    onCraftKeepsake: (String) -> Unit,
    onSelectTeaBlend: (String?) -> Unit = {},
    onTransmuteMaterial: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val player = dataState.playerState
    val workshopLevel = player.facilityLevel(FacilityType.WORKSHOP)
    val teaHearthLevel = player.facilityLevel(FacilityType.TEA_HEARTH)
    val conservatoryLevel = player.facilityLevel(FacilityType.CONSERVATORY)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ResourceSummaryRow(resources = player.resources)
        }

        if (statusNotice != null) {
            item {
                Text(statusNotice, style = MaterialTheme.typography.bodyMedium, color = DreamTrailColors.SageMist)
            }
        }
        if (errorMessage != null) {
            item {
                Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = DreamTrailColors.CoralWarn)
            }
        }

        item {
            Text(
                text = "营地设施（各最高 Lv.5）",
                style = MaterialTheme.typography.titleMedium,
                color = DreamTrailColors.ParchmentText
            )
        }

        items(FacilityType.entries) { facility ->
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
                            text = facility.displayName,
                            style = MaterialTheme.typography.titleLarge,
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
                    text = "营地状态：$stageLook",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DreamTrailColors.SageMist
                )

                if (level < 5) {
                    val target = level + 1
                    val (goldCost, matPair) = ContentCatalog.facilityUpgradeCost(facility, target)
                    val (matId, matCost) = matPair
                    val matName = RegionId.entries.firstOrNull { it.materialId == matId }?.materialName ?: matId
                    val requiredHours = ContentCatalog.facilityRequiredExploreHours(facility, target)
                    val explored = dataState.discoveryState.totalCreditedSeconds >= requiredHours * 3600L
                    val affordable = explored && player.resources.gold >= goldCost && player.resources.materialCount(matId) >= matCost
                    if (!explored) Text("累计有效探索 ${requiredHours} 小时后可修葺（当前 ${dataState.discoveryState.totalCreditedSeconds / 3600} 小时）", color = DreamTrailColors.MutedInk)

                    Text(
                        text = "升至 Lv.$target 需要：$goldCost 金币 + $matCost $matName",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (affordable) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                    )

                    Button(
                        onClick = { onUpgradeFacility(facility) },
                        enabled = affordable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(if (level == 0) "首次修葺 ${facility.displayName}（Lv.1）" else "升级 ${facility.displayName} 至 Lv.$target")
                    }
                } else {
                    Text(
                        text = "已完善至最高等级，所有相关加成已生效。",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.LanternAmber
                    )
                }
            }
        }

        item {
            Text(
                text = "睡前草本热茶（茶歇火塘 Lv.$teaHearthLevel）",
                style = MaterialTheme.typography.titleMedium,
                color = DreamTrailColors.ParchmentText,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = if (teaHearthLevel == 0) {
                    "修葺「茶歇火塘」Lv.1 后可免费配置睡前热茶，在整夜探索提供对应增益。"
                } else {
                    "点选茶饮即可在下次夜间出发时生效（可随时切换，不消耗金币）。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = DreamTrailColors.MutedInk
            )
        }

        items(ContentCatalog.teaBlends, key = { it.id }) { tea ->
            val unlocked = teaHearthLevel >= tea.requiredTeaHearthLevel
            val active = player.activeTeaBlendId == tea.id
            JournalCard(
                borderColor = if (active) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tea.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.ParchmentText
                        )
                        Text(
                            text = tea.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.SageMist
                        )
                    }
                    TagBadge(
                        text = if (active) "已冲泡" else "需火塘 Lv.${tea.requiredTeaHearthLevel}",
                        accent = if (active) DreamTrailColors.LanternAmber else if (unlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                    )
                }
                OutlinedButton(
                    onClick = { onSelectTeaBlend(if (active) null else tea.id) },
                    enabled = unlocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        when {
                            !unlocked -> "需茶歇火塘 Lv.${tea.requiredTeaHearthLevel}"
                            active -> "收起「${tea.name}」"
                            else -> "今晚冲泡「${tea.name}」"
                        }
                    )
                }
            }
        }

        item {
            Text(
                text = "首领信物与营地奇物（已铭刻 ${dataState.discoveryState.unlockedKeepsakeIds.size}/${ContentCatalog.keepsakes.size}）",
                style = MaterialTheme.typography.titleMedium,
                color = DreamTrailColors.ParchmentText,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = "消耗首领印记与地区材料铭刻奇物，永久提升旅人与营地收益。",
                style = MaterialTheme.typography.bodySmall,
                color = DreamTrailColors.MutedInk
            )
        }

        items(ContentCatalog.keepsakes, key = { it.id }) { ks ->
            val unlocked = ks.id in dataState.discoveryState.unlockedKeepsakeIds
            val matName = RegionId.entries.firstOrNull { it.materialId == ks.materialId }?.materialName ?: ks.materialId
            val affordable = !unlocked &&
                player.resources.bossMarks >= ks.bossMarkCost &&
                player.resources.gold >= ks.goldCost &&
                player.resources.materialCount(ks.materialId) >= ks.materialCost

            JournalCard(
                borderColor = if (unlocked) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${ks.name}（${ks.regionId.displayName}）",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.ParchmentText
                        )
                        Text(
                            text = "永久效果：${ks.effectSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.StarlightBlue
                        )
                    }
                    TagBadge(
                        text = if (unlocked) "已陈列生效" else "需 ${ks.bossMarkCost} 首领印记",
                        accent = if (unlocked) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk
                    )
                }

                Text(
                    text = ks.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.MutedInk
                )

                if (!unlocked) {
                    Text(
                        text = "消耗：${ks.bossMarkCost} 首领印记 + ${ks.goldCost} 金币 + ${ks.materialCost} $matName",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (affordable) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                    )
                    Button(
                        onClick = { onCraftKeepsake(ks.id) },
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

        item {
            Text(
                text = "工坊打造（当前工坊 Lv.$workshopLevel）",
                style = MaterialTheme.typography.titleMedium,
                color = DreamTrailColors.ParchmentText,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (workshopLevel == 0) {
                Text(
                    text = "修葺工坊至 Lv.1 后即可开启 1 阶装备打造；此处可提前查看打造清单。",
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.MutedInk
                )
            }
        }

        items(ContentCatalog.equipmentDefs, key = { it.id }) { def ->
            val unlocked = workshopLevel >= def.requiredWorkshopLevel
            val (goldCost, matCost) = ProgressionEngine.adjustedCraftCost(def, player, dataState.inventory)
            val affordable = unlocked &&
                player.resources.gold >= goldCost &&
                player.resources.materialCount(def.craftMaterialId) >= matCost

            JournalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${def.name}（${def.slot.displayName} · ${def.tier}阶）",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.ParchmentText
                        )
                        Text(
                            text = "词条「${def.defaultAffix.displayName}」 · ${def.defaultAffix.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.StarlightBlue
                        )
                    }
                    TagBadge(
                        text = "需工坊 Lv.${def.requiredWorkshopLevel}",
                        accent = if (unlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                    )
                }

                Text(
                    text = "基础属性：生命+${def.baseStats.maxHp} 攻击+${def.baseStats.attack} 防护+${def.baseStats.defense} 速度+${def.baseStats.speed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.ParchmentText
                )

                Text(
                    text = "材料：${goldCost} 金币 + ${matCost} ${def.regionId.materialName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.MutedInk
                )

                OutlinedButton(
                    onClick = { onCraftEquipment(def.id) },
                    enabled = affordable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        when {
                            !unlocked -> "待解锁（需工坊 Lv.${def.requiredWorkshopLevel}）"
                            !affordable -> "资源尚不足"
                            else -> "打造「${def.name}」"
                        }
                    )
                }
            }
        }

        item {
            Text(
                text = "星尘花房精炼配方（当前花房 Lv.$conservatoryLevel）",
                style = MaterialTheme.typography.titleMedium,
                color = DreamTrailColors.ParchmentText,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = if (conservatoryLevel == 0) {
                    "修葺「星尘花房」Lv.1 后可将溢出的地区材料转化为高阶素材、星芒徽记或首领印记。"
                } else {
                    "消耗富余材料与少量金币进行催化精炼，助力锻造高阶装备与奇物。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = DreamTrailColors.MutedInk
            )
        }

        items(ContentCatalog.transmuteRecipes, key = { it.id }) { recipe ->
            val unlocked = conservatoryLevel >= recipe.requiredConservatoryLevel
            val inputMatName = RegionId.entries.firstOrNull { it.materialId == recipe.inputMaterialId }?.materialName
                ?: recipe.inputMaterialId
            val outputLabel = buildList {
                if (recipe.outputMaterialId != null && recipe.outputMaterialAmount > 0L) {
                    val outName = RegionId.entries.firstOrNull { it.materialId == recipe.outputMaterialId }?.materialName
                        ?: recipe.outputMaterialId
                    add("${recipe.outputMaterialAmount} $outName")
                }
                if (recipe.outputBossMarks > 0L) {
                    add("${recipe.outputBossMarks} 首领印记")
                }
            }.joinToString(" + ")
            val affordable = unlocked &&
                player.resources.gold >= recipe.inputGold &&
                player.resources.materialCount(recipe.inputMaterialId) >= recipe.inputMaterialAmount

            JournalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recipe.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.ParchmentText
                        )
                        Text(
                            text = recipe.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.MutedInk
                        )
                    }
                    TagBadge(
                        text = "需花房 Lv.${recipe.requiredConservatoryLevel}",
                        accent = if (unlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                    )
                }
                Text(
                    text = "投入：${recipe.inputGold} 金币 + ${recipe.inputMaterialAmount} $inputMatName → 产出：$outputLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.StarlightBlue
                )
                OutlinedButton(
                    onClick = { onTransmuteMaterial(recipe.id) },
                    enabled = affordable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        when {
                            !unlocked -> "需星尘花房 Lv.${recipe.requiredConservatoryLevel}"
                            !affordable -> "金币或材料不足"
                            else -> "催化转化"
                        }
                    )
                }
            }
        }

        item {
            val doneCount = dataState.discoveryState.completedCommissionIds.size
            Text(
                text = "营地常驻委托（已完成 $doneCount/${ContentCatalog.commissions.size} · 探险结算自动验收）",
                style = MaterialTheme.typography.titleMedium,
                color = DreamTrailColors.ParchmentText,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = "满足对应地区单次夜间节点要求并归来结算时，自动领取额外补给、印记与随行伙伴默契。",
                style = MaterialTheme.typography.bodySmall,
                color = DreamTrailColors.MutedInk
            )
        }

        items(ContentCatalog.commissions, key = { it.id }) { comm ->
            val completed = comm.id in dataState.discoveryState.completedCommissionIds
            JournalCard(
                borderColor = if (completed) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${comm.title}（${comm.regionId.displayName}）",
                        style = MaterialTheme.typography.titleSmall,
                        color = DreamTrailColors.ParchmentText
                    )
                    TagBadge(
                        text = if (completed) "已交付" else "需≥${comm.requiredMinNodes}节点",
                        accent = if (completed) DreamTrailColors.LanternAmber else DreamTrailColors.SageMist
                    )
                }
                Text(
                    text = comm.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.MutedInk
                )
                val rewardSummary = buildList {
                    if (comm.rewardGold > 0L) add("${comm.rewardGold}金币")
                    if (comm.rewardXp > 0L) add("${comm.rewardXp}经验")
                    val rewardRegion = comm.rewardMaterialRegion
                    if (rewardRegion != null && comm.rewardMaterialAmount > 0L) {
                        add("${comm.rewardMaterialAmount}${rewardRegion.materialName}")
                    }
                    if (comm.rewardBossMarks > 0L) add("${comm.rewardBossMarks}印记")
                    if (comm.rewardAffinity > 0) add("伙伴默契+${comm.rewardAffinity}")
                }.joinToString(" · ")
                Text(
                    text = "委托赏格：$rewardSummary",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (completed) DreamTrailColors.LanternAmber else DreamTrailColors.StarlightBlue
                )
            }
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
