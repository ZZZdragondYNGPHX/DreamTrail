package com.dreamtrail.core.model

import kotlinx.serialization.Serializable

/**
 * Core combat & exploration stats for a traveler or enemy.
 * First version keeps exactly four readable stats: HP, Attack, Defense, Speed.
 */
@Serializable
data class Stats(
    val maxHp: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val speed: Int = 0
) {
    operator fun plus(other: Stats): Stats = Stats(
        maxHp = maxHp + other.maxHp,
        attack = attack + other.attack,
        defense = defense + other.defense,
        speed = speed + other.speed
    )
}

@Serializable
enum class ExpeditionStrategy(
    val displayName: String,
    val summary: String,
    val requiredExploreHours: Int = 0
) {
    STEADY("稳健", "减少遭遇失败风险，收益平稳，适合新区域或放松休息"),
    EXPLORE("探索", "提高未知节点与线索发现概率，推进故事与图鉴，普通产出略低"),
    GATHER("搜集", "提高路线指定材料产出权重，线索推进较慢，适合打造与营地建设")
}

@Serializable
enum class SessionMode(
    val displayName: String,
    val isFocus: Boolean,
    val maxSingleWindowSeconds: Long
) {
    SLEEP("夜间安眠", isFocus = false, maxSingleWindowSeconds = 12L * 3600L),
    POWER_NAP("午后小憩", isFocus = false, maxSingleWindowSeconds = 2L * 3600L),
    FOCUS_POMODORO("番茄定心", isFocus = true, maxSingleWindowSeconds = 3L * 3600L),
    FOCUS_FLOW("心流漫步", isFocus = true, maxSingleWindowSeconds = 4L * 3600L)
}

@Serializable
enum class EquipmentSlot(val displayName: String) {
    WEAPON("武器"),
    ARMOR("防具"),
    CHARM("护符")
}

@Serializable
enum class BuildArchetype(
    val displayName: String,
    val summary: String
) {
    BASTION("坚盾反震", "强化开局与周期护盾，受击时借护盾反震伤害"),
    GALE("疾风连击", "凭速度优势抢攻破甲，触发额外连击追击"),
    DREAMWEAVE("织梦续航", "战中周期恢复生命与护盾，休整节点收益倍增"),
    SCHOLAR("博学寻宝", "提升线索权重、装备寻获进度与弱点洞察")
}

@Serializable
enum class AffixId(
    val displayName: String,
    val description: String,
    val archetype: BuildArchetype
) {
    SHIELD_STRIKE("护盾猛击", "获得护盾后的下一次攻击伤害提升 25%", BuildArchetype.BASTION),
    BOUNTIFUL_GATHER("丰饶采集", "每次搜集事件额外获得 2 枚金币", BuildArchetype.DREAMWEAVE),
    CLUE_SEEKER("秘闻寻踪", "探索节点更容易发现线索与图鉴事件（线索权重 +35%）", BuildArchetype.SCHOLAR),
    RAIN_WARD("御雨壁垒", "潮湿与寒冷路线上获得额外护盾与 15% 减伤", BuildArchetype.BASTION),
    SWIFT_FIRST_STRIKE("先发制人", "战斗首回合速度 +4 且攻击提升 15%", BuildArchetype.GALE),
    STEADY_RECOVERY("安稳休整", "每次遭遇开局自带少量护盾，整段旅程更耐耗", BuildArchetype.BASTION),
    SALVAGE_INSIGHT("拆解精炼", "搜集产出 +10%，在工坊打造或强化时有微幅折减", BuildArchetype.DREAMWEAVE),
    BOSS_HUNTER("首领克星", "面对区域首领时攻击提升 20%，防护提升 15%", BuildArchetype.GALE),
    DREAM_RESONANCE("梦境共鸣", "休整事件的额外金币与经验奖励提升 35%", BuildArchetype.DREAMWEAVE),
    CLOUD_WALKER("云行轻步", "战斗中速度 +3，在云海温润与林间薄雾路线上受到的伤害降低 12%", BuildArchetype.GALE),
    TREASURE_SENSE("寻宝直觉", "提高装备寻获效率，每次结算额外推进 15% 装备寻获进度", BuildArchetype.SCHOLAR),
    COUNTER_PULSE("震荡反制", "战斗开局获得 8 点护盾，面对首领或复数敌人时攻击提升 12%", BuildArchetype.BASTION),
    THORN_BASTION("荆棘圣盾", "护盾受击时向攻击者反震 35% 防护值的伤害，开局护盾 +6", BuildArchetype.BASTION),
    GALE_COMBO("疾风连斩", "速度高于目标时，每第 2 回合追加一次 45% 伤害的连击", BuildArchetype.GALE),
    DREAM_MENDING("梦息回春", "战斗中每第 3 回合恢复 10% 最大生命与 8 点护盾", BuildArchetype.DREAMWEAVE),
    STARLIGHT_INSIGHT("星海博学", "攻击无视目标 15% 防护，且旅程基础金币与经验 +6%", BuildArchetype.SCHOLAR),
    SANCTUARY_BULWARK("星穹壁垒", "开局护盾 +10，且每回合结束时保留或叠加至多 14 点星穹余盾", BuildArchetype.BASTION),
    CHRONO_STRIKE("时隙破击", "首回合或速度高于目标时额外穿透目标 20% 防护，并自第 3 回合起速度 +2", BuildArchetype.GALE),
    LUCID_PULSE("明晰脉动", "战斗中每第 2 回合回复 6% 最大生命，且休整事件金币与经验额外 +15%", BuildArchetype.DREAMWEAVE),
    ASTRAL_HARMONY("星辉博识", "依据已激活流派共鸣数每层提升 4% 伤害，且装备寻获与图鉴线索权重 +15%", BuildArchetype.SCHOLAR),
    POISE_WARD("定心静御", "开局护盾 +14，战斗受到的伤害降低 10%，且专注定心加成额外 +1%", BuildArchetype.BASTION),
    POMO_BLITZ("番茄疾行", "战斗速度 +4；速度高于目标时每第 3 回合追加一次 40% 伤害的疾笔连斩", BuildArchetype.GALE),
    SOLUNA_DEW("昼夜甘露", "战斗中每第 2 回合回复 5% 最大生命与 6 点护盾，且休整事件金币与经验额外 +15%", BuildArchetype.DREAMWEAVE),
    INK_SCHOLAR("墨韵博闻", "基础金币与经验 +8%，线索权重 +25%，专注墨晶产出 +10%，攻击无视目标 12% 防护", BuildArchetype.SCHOLAR)
}

@Serializable
enum class FacilityType(
    val displayName: String,
    val subtitle: String
) {
    MAP_TABLE("地图桌", "整理远行地图，提升线索发现效率"),
    WORKSHOP("工坊", "解锁与强化装备打造，提升装备基础属性"),
    INN("旅舍", "接待旅途伙伴，提升生命并解锁被动"),
    TEA_HEARTH("茶歇火塘", "冲泡安眠草本茶饮与专注特调，提升旅途休整与研习增益"),
    CONSERVATORY("星尘花房", "培育梦境灵植并精炼盈余材料，提升全队生命与休整产出"),
    SCRIPTORIUM("研习书斋", "收纳专注手札与典籍，提升专注墨晶产出、定心加成与全队博学属性")
}

@Serializable
enum class RegionId(
    val id: String,
    val displayName: String,
    val materialId: String,
    val materialName: String,
    val atmosphere: String
) {
    MISTWOOD("region_mistwood", "雾林", "mat_mistwood_wood", "雾林松木", "潮湿静谧的古老林海，灯笼花与苔藓铺满小径"),
    TIDAL_FLAT("region_tidal_flat", "潮汐滩", "mat_tidal_ore", "潮汐矿砂", "退潮后露出的银白浅滩与海风礁石，藏着沉船与灯塔"),
    STARLIT_RIDGE("region_starlit_ridge", "星陨山脊", "mat_starlit_crystal", "星辉碎晶", "高寒清朗的碎石山脊，夜里能看见落在地表的微光晶石"),
    CLOUD_SANCTUARY("region_cloud_sanctuary", "云栖浮岛", "mat_cloud_silk", "云栖灵绒", "漂浮在山脊云海之上的温润浮岛与古温室遗迹，夜风吹拂着发光的梦绒花与风琴回廊"),
    DREAM_SEA("region_dream_sea", "沉星梦海", "mat_dream_pearl", "沉星梦珠", "云海尽头与夜空交汇的静谧星归之海，漂浮着倒映梦境的古老航标与归梦灯塔"),
    ASTRAL_ARCH("region_astral_arch", "归梦星拱", "mat_astral_amber", "星拱琥珀", "沉星梦海尽头悬浮于永恒晨昏间的古老星穹拱门与逆时回廊，静静保存着历代旅人的梦之余响"),
    SUNLIT_ATHENAEUM("region_sunlit_athenaeum", "晴岚书院", "mat_sunlit_vellum", "晴岚羊皮卷", "沐浴在云巅晴朗日光下的露天藏书长廊与日晷秘馆，书页与松烟墨香随微风轻扬"),
    DAWNLIGHT_SPIRE("region_dawnlight_spire", "晨曦钟塔", "mat_dawn_prism", "破晓棱晶", "耸立于昼夜交界地平线之巅的黄金报时钟塔，同时折射破晓朝霞与静谧星海")
}

@Serializable
enum class EnvironmentTag(val displayName: String) {
    FOREST("林间薄雾"),
    DAMP("多雨潮湿"),
    CHILL("高地清寒"),
    CLOUD("云海温润"),
    ASTRAL("星海静流"),
    SOLAR_ECHO("晨昏回响"),
    SUNLIT("晴岚书香"),
    DAWNLIGHT("破晓金辉")
}

@Serializable
enum class SessionStatus {
    IDLE,
    RUNNING,
    SETTLED,
    CANCELLED
}

@Serializable
enum class RewardKind {
    BASE_GOLD,
    BASE_XP,
    BASE_MAT_MISTWOOD,
    BASE_MAT_TIDAL,
    BASE_MAT_STARLIT,
    BASE_MAT_CLOUD,
    BASE_MAT_DREAM_SEA,
    BASE_MAT_ASTRAL_ARCH,
    BASE_MAT_ATHENAEUM,
    BASE_MAT_SPIRE,
    BASE_INSPIRATION_INK,
    BASE_BOSS_MARK,
    BLESSING_GOLD,
    BLESSING_XP
}

@Serializable
data class ResourceBag(
    val gold: Long = 0L,
    val materials: Map<String, Long> = emptyMap(),
    val bossMarks: Long = 0L,
    val inspirationInk: Long = 0L
) {
    fun materialCount(materialId: String): Long = materials[materialId] ?: 0L

    fun addGold(delta: Long): ResourceBag = copy(gold = (gold + delta).coerceAtLeast(0L))

    fun addMaterial(materialId: String, delta: Long): ResourceBag {
        if (delta == 0L) return this
        val updated = materials.toMutableMap()
        val next = ((updated[materialId] ?: 0L) + delta).coerceAtLeast(0L)
        if (next == 0L) updated.remove(materialId) else updated[materialId] = next
        return copy(materials = updated)
    }

    fun addBossMarks(delta: Long): ResourceBag = copy(bossMarks = (bossMarks + delta).coerceAtLeast(0L))

    fun addInspirationInk(delta: Long): ResourceBag = copy(inspirationInk = (inspirationInk + delta).coerceAtLeast(0L))

    operator fun plus(other: ResourceBag): ResourceBag {
        val merged = materials.toMutableMap()
        for ((k, v) in other.materials) {
            val next = (merged[k] ?: 0L) + v
            if (next > 0L) merged[k] = next
        }
        return ResourceBag(
            gold = gold + other.gold,
            materials = merged,
            bossMarks = bossMarks + other.bossMarks,
            inspirationInk = inspirationInk + other.inspirationInk
        )
    }

    fun hasNonNegativeValues(): Boolean =
        gold >= 0L && bossMarks >= 0L && inspirationInk >= 0L && materials.values.all { it >= 0L }
}

@Serializable
data class EquipmentDef(
    val id: String,
    val name: String,
    val slot: EquipmentSlot,
    val tier: Int,
    val regionId: RegionId,
    val baseStats: Stats,
    val defaultAffix: AffixId,
    val description: String,
    val craftGoldCost: Long,
    val craftMaterialId: String,
    val craftMaterialCost: Long,
    val requiredWorkshopLevel: Int,
    val craftOnly: Boolean = false
)

@Serializable
data class EquipmentInstance(
    val instanceId: String,
    val defId: String,
    val tier: Int,
    val level: Int = 1,
    val affix: AffixId,
    val secondaryAffix: AffixId? = null,
    val locked: Boolean = false
) {
    companion object {
        const val MAX_LEVEL = 7
    }
}

@Serializable
data class ActiveResonance(
    val archetype: BuildArchetype,
    val points: Int,
    val tier: Int
)

@Serializable
data class PassiveDef(
    val id: String,
    val name: String,
    val description: String,
    val unlockHint: String,
    val archetype: BuildArchetype = BuildArchetype.BASTION
)

@Serializable
enum class BossTrait(
    val title: String,
    val description: String
) {
    MIST_VEIL("林雾护盾", "每 4 回合重新凝聚一层林雾护盾，稳固自身防线。"),
    TIDE_SURGE("潮汐涌动", "每 3 回合掀起汐涌，按自身攻击力 35% 造成额外冲击。"),
    STAR_JUDGMENT("星陨裁决", "首领生命低于 50% 后激发星陨，攻击提升 18%。"),
    SKY_GALE("云霄烈风", "每 3 回合追加一次轻盈打击并小幅回复生命。"),
    DREAM_TIDE_BARRIER("深梦灯幕", "每 4 回合同时唤起星海障壁与自我修补，持续施压。"),
    CHRONO_RESONANCE("逆时共鸣", "每隔 3 回合回溯并巩固自身星辉护盾，同时触发额外时隙冲击。"),
    INK_BARRIER("墨典结界", "每 3 回合展开金墨典籍护盾并触发一道书灵冲击。"),
    SOLUNA_ECLIPSE("双曜轮转", "每 3 回合切换昼夜双曜光环，凝聚强力护盾并追加破晓重击。")
}

@Serializable
enum class EliteTrait(
    val title: String,
    val description: String
) {
    IRON_SHELL("甲壳硬化", "开局额外获得 10 点硬化护盾，受到的基础打击略微折减。"),
    GALE_PROWL("疾影突袭", "速度提升并擅长首轮抢攻。"),
    REGEN_SPORE("回春孢子", "每隔 3 回合吸收梦雾微光回复少量生命。")
}

@Serializable
data class EnemyDef(
    val id: String,
    val name: String,
    val regionId: RegionId,
    val isBoss: Boolean,
    val stats: Stats,
    val skillCoeff: Double,
    val openingShield: Int,
    val description: String,
    val bossTrait: BossTrait? = null
)

@Serializable
data class RouteDef(
    val id: String,
    val regionId: RegionId,
    val name: String,
    val subtitle: String,
    val dangerTag: String,
    val environmentTag: EnvironmentTag,
    val recommendationHint: String,
    val recommendedPowerTier: Int,
    val baseGoldPerHour: Long,
    val baseXpPerHour: Long,
    val baseMaterialPerHour: Long,
    val dangerRate: Double,
    val enemyIds: List<String>,
    val bossId: String,
    val bossInvestigationRequired: Int,
    val unlockDescription: String,
    val requiredExploreHours: Int = 0
)

@Serializable
data class EventTemplate(
    val id: String,
    val regionId: RegionId,
    val title: String,
    val kind: EventKind,
    val weight: Int,
    val cooldownJourneys: Int,
    val minInvestigation: Int,
    val requiredStrategy: ExpeditionStrategy? = null,
    val firstDiscoveryText: String,
    val repeatText: String,
    val bonusGold: Long,
    val bonusXp: Long,
    val bonusMaterial: Long,
    val investigationGain: Int,
    val companionId: String? = null,
    val minRegionExploreHours: Int = 0,
    val focusOnly: Boolean = false
)

@Serializable
enum class EventKind {
    GATHER,
    CLUE,
    REST,
    ENCOUNTER
}

@Serializable
data class TeaBlendDef(
    val id: String,
    val name: String,
    val subtitle: String,
    val requiredTeaHearthLevel: Int,
    val bonusAtk: Int = 0,
    val bonusDef: Int = 0,
    val bonusHp: Int = 0,
    val goldMultiplierBonus: Double = 0.0,
    val xpMultiplierBonus: Double = 0.0,
    val dropRateBonus: Double = 0.0,
    val extraFailureTolerance: Int = 0,
    val favoredForFocus: Boolean = false,
    val inkBonusPercent: Int = 0
)

@Serializable
data class NightPhenomenon(
    val id: String,
    val name: String,
    val subtitle: String,
    val favoredRegionId: RegionId,
    val favoredEnvironment: EnvironmentTag,
    val goldBonusPercent: Int = 0,
    val xpBonusPercent: Int = 0,
    val materialBonusPercent: Int = 0,
    val dropBonusPercent: Int = 0,
    val combatAtkPercent: Int = 0,
    val combatDefPercent: Int = 0
)

@Serializable
data class DayPhenomenon(
    val id: String,
    val name: String,
    val subtitle: String,
    val favoredRegionId: RegionId,
    val favoredEnvironment: EnvironmentTag,
    val goldBonusPercent: Int = 0,
    val xpBonusPercent: Int = 0,
    val materialBonusPercent: Int = 0,
    val dropBonusPercent: Int = 0,
    val combatAtkPercent: Int = 0,
    val combatDefPercent: Int = 0,
    val inkBonusPercent: Int = 0
)

@Serializable
data class FocusVigourBuff(
    val name: String,
    val remainingFocusSeconds: Long,
    val xpBonusPercent: Int = 12,
    val goldBonusPercent: Int = 10,
    val inkBonusPercent: Int = 25
)

@Serializable
data class MorningEchoBuff(
    val sourceEchoId: String,
    val name: String,
    val goldBonusPercent: Int = 0,
    val xpBonusPercent: Int = 0,
    val materialBonusPercent: Int = 0,
    val combatAtkBonus: Int = 0,
    val combatDefBonus: Int = 0
)

@Serializable
data class MorningEchoOutcome(
    val label: String,
    val resultSummary: String,
    val rewardGold: Long = 0L,
    val rewardXp: Long = 0L,
    val rewardMaterialRegion: RegionId? = null,
    val rewardMaterialAmount: Long = 0L,
    val nextNightBuff: MorningEchoBuff? = null
)

@Serializable
data class MorningEchoDef(
    val id: String,
    val regionId: RegionId,
    val title: String,
    val prompt: String,
    val choiceA: MorningEchoOutcome,
    val choiceB: MorningEchoOutcome
)

@Serializable
data class PendingMorningEcho(
    val echoId: String,
    val regionId: RegionId,
    val sessionId: String
)

@Serializable
data class TeaEchoOutcome(
    val label: String,
    val resultSummary: String,
    val rewardGold: Long = 0L,
    val rewardXp: Long = 0L,
    val rewardInk: Long = 0L,
    val rewardMaterialRegion: RegionId? = null,
    val rewardMaterialAmount: Long = 0L,
    val nextBuff: MorningEchoBuff? = null
)

@Serializable
data class TeaEchoDef(
    val id: String,
    val regionId: RegionId,
    val title: String,
    val prompt: String,
    val choiceA: TeaEchoOutcome,
    val choiceB: TeaEchoOutcome
)

@Serializable
data class PendingTeaEcho(
    val echoId: String,
    val regionId: RegionId,
    val sessionId: String
)

@Serializable
data class FocusSpiritDef(
    val id: String,
    val regionId: RegionId,
    val name: String,
    val title: String,
    val description: String,
    val preferredSoundscape: String,
    val favoredForFocus: Boolean = true,
    val bonusStats: Stats = Stats(),
    val focusXpBonusPercent: Int = 0,
    val goldBonusPercent: Int = 0,
    val inkBonusPercent: Int = 0
)

@Serializable
data class FocusTagDef(
    val id: String,
    val name: String,
    val archetype: BuildArchetype,
    val summary: String
)

@Serializable
data class FocusTaskItem(
    val id: String,
    val title: String,
    val tagId: String = "tag_study",
    val targetPomodoros: Int = 1,
    val completedPomodoros: Int = 0,
    val completed: Boolean = false,
    val createdAtUtcMs: Long = 0L
)

@Serializable
data class SouvenirDef(
    val id: String,
    val regionId: RegionId,
    val name: String,
    val description: String,
    val bonusStats: Stats = Stats(),
    val goldBonusPercent: Int = 0,
    val xpBonusPercent: Int = 0,
    val materialBonusPercent: Int = 0
)

@Serializable
data class CompanionStoryDef(
    val id: String,
    val name: String,
    val title: String,
    val passiveId: String,
    val chapters: List<CompanionChapterDef>,
    val combatSkillName: String = "",
    val combatRoleSummary: String = "",
    val dispatchRoleSummary: String = "",
    val dispatchRegionId: RegionId = RegionId.MISTWOOD,
    val studyGreeting: String = "",
    val studyFocusLine: String = "",
    val distractionNudgeLine: String = ""
)

@Serializable
data class CompanionChapterDef(
    val chapterIndex: Int,
    val title: String,
    val requiredInnLevel: Int,
    val requiredInvestigation: Int,
    val regionId: RegionId,
    val summary: String,
    val requiredExploreHours: Int = 0
)

@Serializable
data class KeepsakeDef(
    val id: String,
    val name: String,
    val regionId: RegionId,
    val bossMarkCost: Long,
    val goldCost: Long,
    val materialId: String,
    val materialCost: Long,
    val effectSummary: String,
    val description: String
)

@Serializable
data class MilestoneDef(
    val id: String,
    val title: String,
    val requirementSummary: String,
    val description: String
)

@Serializable
data class ChronicleStageDef(
    val stageIndex: Int,
    val title: String,
    val requiredInvestigation: Int,
    val requiredExploreHours: Int,
    val summary: String
)

@Serializable
data class ChronicleQuestDef(
    val id: String,
    val regionId: RegionId,
    val title: String,
    val description: String,
    val stages: List<ChronicleStageDef>,
    val rewardStats: Stats = Stats(),
    val rewardGoldBonusPercent: Int = 0,
    val rewardXpBonusPercent: Int = 0,
    val rewardMaterialBonusPercent: Int = 0,
    val completionTitle: String
)

@Serializable
data class AstralNodeDef(
    val id: String,
    val archetype: BuildArchetype,
    val tier: Int,
    val title: String,
    val description: String,
    val bonusStats: Stats = Stats(),
    val goldBonusPercent: Int = 0,
    val xpBonusPercent: Int = 0,
    val materialBonusPercent: Int = 0,
    val combatDamagePercent: Int = 0
)

@Serializable
data class TransmuteRecipeDef(
    val id: String,
    val title: String,
    val description: String,
    val requiredConservatoryLevel: Int,
    val inputGold: Long,
    val inputMaterialId: String,
    val inputMaterialAmount: Long,
    val outputMaterialId: String? = null,
    val outputMaterialAmount: Long = 0L,
    val outputBossMarks: Long = 0L,
    val outputInspirationInk: Long = 0L
)

@Serializable
data class CommissionDef(
    val id: String,
    val title: String,
    val regionId: RegionId,
    val description: String,
    val requiredMinNodes: Int,
    val rewardGold: Long,
    val rewardXp: Long,
    val rewardMaterialRegion: RegionId? = null,
    val rewardMaterialAmount: Long = 0L,
    val rewardBossMarks: Long = 0L,
    val rewardAffinity: Int = 1,
    val focusOnly: Boolean = false
)

@Serializable
data class LoadoutPreset(
    val slotIndex: Int,
    val name: String,
    val weaponId: String? = null,
    val armorId: String? = null,
    val charmId: String? = null,
    val equippedPassives: List<String> = emptyList(),
    val strategy: ExpeditionStrategy = ExpeditionStrategy.STEADY,
    val companionId: String? = null,
    val teaBlendId: String? = null
)

@Serializable
data class PlayerState(
    val version: Int = CURRENT_VERSION,
    val characterName: String = "晨风",
    val level: Int = 1,
    val xp: Long = 0L,
    val resources: ResourceBag = ResourceBag(gold = 60L, materials = mapOf(RegionId.MISTWOOD.materialId to 6L)),
    val equippedWeaponId: String? = null,
    val equippedArmorId: String? = null,
    val equippedCharmId: String? = null,
    val equippedPassives: List<String> = listOf("passive_forest_ward"),
    val facilities: Map<FacilityType, Int> = mapOf(
        FacilityType.MAP_TABLE to 0,
        FacilityType.WORKSHOP to 0,
        FacilityType.INN to 0,
        FacilityType.TEA_HEARTH to 0,
        FacilityType.CONSERVATORY to 0,
        FacilityType.SCRIPTORIUM to 0
    ),
    val lastRouteId: String = "route_mistwood_outer",
    val lastStrategy: ExpeditionStrategy = ExpeditionStrategy.STEADY,
    val activeCompanionId: String? = null,
    val activeTeaBlendId: String? = null,
    val dreamBeaconTier: Int = 0,
    val pendingMorningEcho: PendingMorningEcho? = null,
    val activeMorningBuff: MorningEchoBuff? = null,
    val unlockedAstralNodeIds: Set<String> = emptySet(),
    val loadouts: List<LoadoutPreset> = emptyList(),
    val nightPrepCharge: Int = 0,
    val focusVigourBuff: FocusVigourBuff? = null,
    val pendingTeaEcho: PendingTeaEcho? = null,
    val activeTeaEchoBuff: MorningEchoBuff? = null,
    val activeSpiritId: String? = null,
    val selectedFocusTagId: String = "tag_study",
    val customFocusTags: List<FocusTagDef> = emptyList(),
    val focusTasks: List<FocusTaskItem> = emptyList(),
    val activeFocusTaskId: String? = null
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val MAX_LEVEL = 65
        const val MAX_EQUIPPED_PASSIVES = 3
    }

    fun facilityLevel(type: FacilityType): Int = facilities[type] ?: 0

    fun equippedInstanceIds(): Set<String> =
        listOfNotNull(equippedWeaponId, equippedArmorId, equippedCharmId).toSet()
}

@Serializable
data class ExpeditionPlayerSnapshot(
    val level: Int,
    val effectiveStats: Stats,
    val equippedItems: List<EquipmentInstance>,
    val equippedPassives: List<String>,
    val facilityLevels: Map<FacilityType, Int>,
    val unlockedKeepsakeIds: Set<String> = emptySet(),
    val unlockedSouvenirIds: Set<String> = emptySet(),
    val activeCompanionId: String? = null,
    val dispatchCompanionIds: List<String> = emptyList(),
    val activeTeaBlendId: String? = null,
    val dreamBeaconTier: Int = 0,
    val activeMorningBuff: MorningEchoBuff? = null,
    val unlockedAstralNodeIds: Set<String> = emptySet(),
    val companionAffinity: Map<String, Int> = emptyMap(),
    val enemyKillCounts: Map<String, Int> = emptyMap(),
    val chronicleProgress: Map<String, Int> = emptyMap(),
    val nightPrepTier: Int = 0,
    val focusVigourBuff: FocusVigourBuff? = null,
    val activeTeaEchoBuff: MorningEchoBuff? = null,
    val activeSpiritId: String? = null,
    val unlockedSpiritIds: Set<String> = emptySet()
)

@Serializable
data class ExpeditionSession(
    val sessionId: String,
    val status: SessionStatus,
    val routeId: String,
    val strategy: ExpeditionStrategy,
    val startAtUtcMs: Long,
    val endAtUtcMs: Long? = null,
    val startElapsedRealtimeMs: Long? = null,
    val startBootId: String? = null,
    val sessionSeed: Long,
    val contentVersion: Int,
    val rulesVersion: Int,
    val snapshot: ExpeditionPlayerSnapshot,
    val isOnboarding: Boolean = false,
    val nightPhenomenon: NightPhenomenon? = null,
    val sessionMode: SessionMode = SessionMode.SLEEP,
    val targetDurationSeconds: Long? = null,
    val focusTagId: String? = null,
    val linkedTaskId: String? = null,
    val dayPhenomenon: DayPhenomenon? = null,
    val distractionCount: Int = 0,
    val flipFocusSeconds: Long = 0L,
    val zoneId: String = "UTC"
)

@Serializable
data class SecondInterval(
    val startSec: Long,
    val endSec: Long
) {
    init {
        require(endSec > startSec) { "Interval endSec ($endSec) must be > startSec ($startSec)" }
    }

    val durationSec: Long get() = endSec - startSec
}

@Serializable
data class RewardBudgetState(
    val creditedIntervals: List<SecondInterval> = emptyList(),
    val remainderSeconds: Int = 0,
    val equipmentProgressSeconds: Int = 0,
    val rarePityByRegion: Map<String, Int> = emptyMap(),
    val bossProgressSecondsByRegion: Map<String, Int> = emptyMap(),
    val resourceRemainders: Map<String, Long> = emptyMap(),
    val eventCooldownUntilSeconds: Map<String, Long> = emptyMap(),
    val equipmentProgressRemainder: Int = 0,
    val focusCreditedIntervals: List<SecondInterval> = emptyList(),
    val focusRemainderSeconds: Int = 0,
    val dispatchProgressSeconds: Int = 0,
    val spiritProgressSeconds: Int = 0,
    val taskBountyUtcSeconds: List<Long> = emptyList(),
    val nightPrepRemainderSeconds: Int = 0
)

@Serializable
data class DiscoveryState(
    val rngState: Long = 0x5DEECE66DL,
    val seenEventIds: Set<String> = emptySet(),
    val regionInvestigation: Map<String, Int> = emptyMap(),
    val unlockedRouteIds: Set<String> = setOf("route_mistwood_outer"),
    val unlockedPassiveIds: Set<String> = setOf("passive_forest_ward"),
    val companionChapters: Map<String, Int> = emptyMap(),
    val discoveredEnemyIds: Set<String> = emptySet(),
    val discoveredEquipmentDefIds: Set<String> = emptySet(),
    val discoveredAffixIds: Set<AffixId> = emptySet(),
    val totalJourneysCompleted: Int = 0,
    val totalCreditedSeconds: Long = 0L,
    val onboardingCompleted: Boolean = false,
    val regionCreditedSeconds: Map<String, Long> = emptyMap(),
    val unlockedKeepsakeIds: Set<String> = emptySet(),
    val unlockedMilestoneIds: Set<String> = emptySet(),
    val unlockedSouvenirIds: Set<String> = emptySet(),
    val companionAffinity: Map<String, Int> = emptyMap(),
    val dreamBeaconClears: Map<Int, Int> = emptyMap(),
    val chronicleProgress: Map<String, Int> = emptyMap(),
    val enemyKillCounts: Map<String, Int> = emptyMap(),
    val completedCommissionIds: Set<String> = emptySet(),
    val totalFocusSessionsCompleted: Int = 0,
    val totalFocusCreditedSeconds: Long = 0L,
    val unlockedSpiritIds: Set<String> = emptySet(),
    val focusSecondsByTag: Map<String, Long> = emptyMap(),
    val dailyFocusSecondsByDate: Map<String, Long> = emptyMap(),
    val dailySleepSecondsByDate: Map<String, Long> = emptyMap(),
    val completedTasksCount: Int = 0,
    val totalRestSessionsCompleted: Int = 0
)

@Serializable
data class KeyEventSummary(
    val nodeIndex: Int,
    val title: String,
    val detail: String,
    val isFirstDiscovery: Boolean,
    val highlightReason: String? = null
)

@Serializable
data class MergedActivitySummary(
    val label: String,
    val count: Int
)

@Serializable
data class SettlementResult(
    val sessionId: String,
    val routeId: String,
    val strategy: ExpeditionStrategy,
    val startAtUtcMs: Long,
    val endAtUtcMs: Long,
    val rawDurationSeconds: Long,
    val windowCappedSeconds: Long,
    val effectiveCreditedSeconds: Long,
    val completedNodes: Int,
    val clockAnomalyNote: String? = null,
    val budgetNote: String? = null,
    val baseGold: Long,
    val baseXp: Long,
    val blessingGold: Long,
    val blessingXp: Long,
    val sleepBonusPercentTenths: Int = 30,
    val materialsGained: Map<String, Long>,
    val bossMarksGained: Long,
    val newEquipment: List<EquipmentInstance>,
    val duplicateConvertedGold: Long,
    val keyEvents: List<KeyEventSummary>,
    val mergedActivities: List<MergedActivitySummary>,
    val nextStepSuggestion: String,
    val dangerFailures: Int,
    val switchedToSafeGather: Boolean,
    val leveledUpFrom: Int,
    val leveledUpTo: Int,
    val rulesVersion: Int,
    val contentVersion: Int,
    val initialBlessingGold: Long = blessingGold,
    val initialBlessingXp: Long = blessingXp,
    val sleepBonusBasisPoints: Int = sleepBonusPercentTenths * 10,
    val blessingGoldRemainder: Long = 0L,
    val blessingXpRemainder: Long = 0L,
    val nightPhenomenonName: String? = null,
    val activeCompanionName: String? = null,
    val teaBlendName: String? = null,
    val dreamBeaconTier: Int = 0,
    val newSouvenirIds: List<String> = emptyList(),
    val dispatchSummaries: List<String> = emptyList(),
    val pendingMorningEchoTitle: String? = null,
    val chronicleUpdateText: String? = null,
    val completedCommissionTitles: List<String> = emptyList(),
    val eliteVictories: Int = 0,
    val sessionMode: SessionMode = SessionMode.SLEEP,
    val targetDurationSeconds: Long? = null,
    val focusTagId: String? = null,
    val linkedTaskTitle: String? = null,
    val taskCompletedNow: Boolean = false,
    val inspirationInkGained: Long = 0L,
    val nightPrepGained: Int = 0,
    val nightPrepTierUsed: Int = 0,
    val focusVigourGrantedMinutes: Int = 0,
    val distractionCount: Int = 0,
    val dayPhenomenonName: String? = null,
    val newSpiritIds: List<String> = emptyList(),
    val pendingTeaEchoTitle: String? = null,
    val recommendedBreakMinutes: Int = 0,
    val reflectionMood: String? = null
)

@Serializable
data class RewardLedgerEntry(
    val sessionId: String,
    val rewardKind: RewardKind,
    val creditedTotal: Long,
    val policyVersion: Int,
    val sourceRecordIds: List<String> = emptyList()
)

@Serializable
data class ExpeditionPreview(
    val sessionId: String,
    val elapsedSeconds: Long,
    val effectiveElapsedSeconds: Long,
    val estimatedNodes: Int,
    val recentHighlights: List<String>,
    val budgetNotice: String? = null
)

@Serializable
data class SleepBuffPreview(
    val blessingGold: Long,
    val blessingXp: Long,
    val totalPercentTenths: Int
)

@Serializable
data class GameStateBundle(
    val playerState: PlayerState,
    val budgetState: RewardBudgetState,
    val discoveryState: DiscoveryState,
    val inventory: List<EquipmentInstance>
)
