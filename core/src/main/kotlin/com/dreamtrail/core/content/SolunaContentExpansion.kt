package com.dreamtrail.core.content

import com.dreamtrail.core.model.*

/**
 * v5 《昼梦途 · SolunaTrail》扩充内容库：
 * - 4 个全新专注与昼夜被动技能（共 18 个）
 * - 第 7 境「晴岚书院」与第 8 境「晨曦钟塔」魔物、路线、装备、秘闻、风物、奇遇与信物
 * - 前 6 境各 +3 条日间专注采风/研习事件（共 128 条）
 * - 6 款日间专注特调茶咖、8 种晴岚天象、16 只伴读小灵兽、16 个午后茶歇奇遇、6 个默认专注标签
 * - 第 4 阶星图节点（共 16 个）、3 个新花房转化配方、4 项专注/高阶委托、10 项专注与双核里程碑
 */
internal object SolunaContentExpansion {

    val extraPassives: List<PassiveDef> = listOf(
        PassiveDef(
            id = "passive_sunlit_focus",
            name = "晴岚定心",
            description = "开局获得 14 点护盾；在晴岚书香与破晓金辉路线中防护 +6，专注墨晶产出 +10%。",
            unlockHint = "研习书斋升至 Lv.1 或累计专注达 4 小时解锁",
            archetype = BuildArchetype.BASTION
        ),
        PassiveDef(
            id = "passive_quill_tempo",
            name = "疾笔成章",
            description = "速度 +5；每第 3 回合追加一道 32% 攻击力的书卷追击。",
            unlockHint = "踏足晨曦钟塔或完成朝露第 1 章故事解锁",
            archetype = BuildArchetype.GALE
        ),
        PassiveDef(
            id = "passive_soluna_breath",
            name = "昼夜调息",
            description = "休整节点额外获得 6 金币与 8 经验；全旅程基础经验产出 +7%。",
            unlockHint = "研习书斋升至 Lv.3 或累计完成 12 次专注解锁",
            archetype = BuildArchetype.DREAMWEAVE
        ),
        PassiveDef(
            id = "passive_archivist_eye",
            name = "典籍洞察",
            description = "线索发现权重 +20%；装备寻获与图鉴进度提升，攻击额外穿透 8% 防护。",
            unlockHint = "踏足晴岚书院或完成温言第 1 章故事解锁",
            archetype = BuildArchetype.SCHOLAR
        )
    )

    val extraEnemies: List<EnemyDef> = listOf(
        // Sunlit Athenaeum (6 normal + 1 boss)
        EnemyDef("enemy_parchment_sprite", "折页纸灵", RegionId.SUNLIT_ATHENAEUM, false, Stats(270, 62, 34, 29), 1.30, 24, "由散落手抄卷轴化生的轻盈纸灵，迎风在廊柱间翩翩滑翔。"),
        EnemyDef("enemy_ink_fox", "踏墨小狐", RegionId.SUNLIT_ATHENAEUM, false, Stats(256, 67, 32, 33), 1.35, 22, "四爪沾着淡金松烟墨的小狐，常把书签偷偷藏进窗台花盆。"),
        EnemyDef("enemy_sundial_owl", "晷影枭卫", RegionId.SUNLIT_ATHENAEUM, false, Stats(288, 66, 38, 28), 1.35, 32, "栖息在露天藏书院日晷指针上的白羽枭鸟，目光敏锐。"),
        EnemyDef("enemy_scroll_automaton", "理卷铜偶", RegionId.SUNLIT_ATHENAEUM, false, Stats(312, 68, 43, 22), 1.40, 40, "执掌书院梯架整备的发条铜偶，行动稳重而防御扎实。"),
        EnemyDef("enemy_quill_griffin", "羽笔狮鹫雏", RegionId.SUNLIT_ATHENAEUM, false, Stats(298, 71, 37, 31), 1.40, 30, "羽尖泛着青铜墨泽的高空守护兽，喜爱倾听整齐翻书声。"),
        EnemyDef("enemy_vellum_knight", "典籍护卫影", RegionId.SUNLIT_ATHENAEUM, false, Stats(326, 73, 44, 26), 1.45, 42, "披着羊皮长袍的书院守护虚影，试炼每位登临顶层的学者。"),
        EnemyDef("boss_athenaeum_curator", "司卷星灵·墨羽", RegionId.SUNLIT_ATHENAEUM, true, Stats(720, 76, 48, 29), 1.52, 86, "镇守晴岚书院穹顶秘藏馆的古老星灵，以金墨典籍结界庇护世间清醒的求索者。", bossTrait = BossTrait.INK_BARRIER),

        // Dawnlight Spire (6 normal + 1 boss)
        EnemyDef("enemy_dawn_lark", "破晓百灵偶", RegionId.DAWNLIGHT_SPIRE, false, Stats(324, 74, 40, 35), 1.38, 28, "每逢清晨第一缕金辉便奏出清亮乐音的钟塔机关雀。"),
        EnemyDef("enemy_prism_beetle", "棱镜甲虫", RegionId.DAWNLIGHT_SPIRE, false, Stats(352, 76, 48, 24), 1.40, 44, "背壳由七彩破晓棱晶凝成，能折射阳光形成坚固光膜。"),
        EnemyDef("enemy_horizon_lynx", "晨昏影猞", RegionId.DAWNLIGHT_SPIRE, false, Stats(336, 80, 42, 36), 1.45, 32, "穿梭在昼夜分界石阶上的敏捷灵兽，攻势连贯犀利。"),
        EnemyDef("enemy_solstice_warden", "双曜石卫", RegionId.DAWNLIGHT_SPIRE, false, Stats(374, 79, 51, 25), 1.45, 48, "雕刻着日轮与月弧双徽记的钟塔守护巨像，耐力惊人。"),
        EnemyDef("enemy_chime_seraph", "风铃羽影", RegionId.DAWNLIGHT_SPIRE, false, Stats(356, 82, 46, 33), 1.48, 38, "随着钟塔铜铃共振起舞的光尘元素，守护着高塔中枢。"),
        EnemyDef("enemy_zenith_arbiter", "天枢校时偶", RegionId.DAWNLIGHT_SPIRE, false, Stats(388, 85, 52, 31), 1.50, 50, "校准着白昼专注与夜间安眠节律的高阶黄金发条卫士。"),
        EnemyDef("boss_spire_soluna_warden", "双曜守时神偶", RegionId.DAWNLIGHT_SPIRE, true, Stats(860, 88, 56, 32), 1.56, 102, "伫立在晨曦钟塔最顶层日晷之巅的终极机关神偶，左手托持旭日专注之辉，右手轻捧星夜好眠之灯。", bossTrait = BossTrait.SOLUNA_ECLIPSE)
    )

    val extraRoutes: List<RouteDef> = listOf(
        // Sunlit Athenaeum
        RouteDef(
            id = "route_athenaeum_cloister",
            regionId = RegionId.SUNLIT_ATHENAEUM,
            name = "晴岚长廊",
            subtitle = "晨光穿过高挑石拱，书页翻动声随微风轻响的云顶外廊",
            dangerTag = "晴岚书香 · 七境初探",
            environmentTag = EnvironmentTag.SUNLIT,
            recommendationHint = "可由归梦星拱进阶或专注积累解锁，适合搭配博学寻宝流派",
            recommendedPowerTier = 7,
            baseGoldPerHour = 630L,
            baseXpPerHour = 750L,
            baseMaterialPerHour = 21L,
            dangerRate = 0.44,
            enemyIds = listOf("enemy_parchment_sprite", "enemy_ink_fox", "enemy_sundial_owl"),
            bossId = "boss_athenaeum_curator",
            bossInvestigationRequired = 8,
            requiredExploreHours = 416,
            unlockDescription = "累计探索 416 小时并解锁归梦星拱，或累计专注满 24 小时且角色达 Lv.35"
        ),
        RouteDef(
            id = "route_athenaeum_stacks",
            regionId = RegionId.SUNLIT_ATHENAEUM,
            name = "云端藏书阁",
            subtitle = "悬梯与黄铜灯架交错的千层露天书库，墨晶光粒在阳光下浮动",
            dangerTag = "晴岚书香 · 墨韵密集",
            environmentTag = EnvironmentTag.SUNLIT,
            recommendationHint = "线索事件丰富，搭配温言或「墨韵博闻」效率出众",
            recommendedPowerTier = 7,
            baseGoldPerHour = 690L,
            baseXpPerHour = 820L,
            baseMaterialPerHour = 23L,
            dangerRate = 0.47,
            enemyIds = listOf("enemy_sundial_owl", "enemy_scroll_automaton", "enemy_quill_griffin"),
            bossId = "boss_athenaeum_curator",
            bossInvestigationRequired = 9,
            requiredExploreHours = 448,
            unlockDescription = "累计探索 448 小时（或专注 32 小时），晴岚书院调查度 3"
        ),
        RouteDef(
            id = "route_athenaeum_dome",
            regionId = RegionId.SUNLIT_ATHENAEUM,
            name = "日晷穹顶秘馆",
            subtitle = "镶满水晶窗格的书院最高殿堂，司卷星灵在此守望清醒与思考",
            dangerTag = "书院穹顶 · 星灵首领",
            environmentTag = EnvironmentTag.SUNLIT,
            recommendationHint = "建议携 7 阶装备与研习书斋加成迎战司卷星灵·墨羽",
            recommendedPowerTier = 7,
            baseGoldPerHour = 760L,
            baseXpPerHour = 900L,
            baseMaterialPerHour = 25L,
            dangerRate = 0.50,
            enemyIds = listOf("enemy_scroll_automaton", "enemy_quill_griffin", "enemy_vellum_knight"),
            bossId = "boss_athenaeum_curator",
            bossInvestigationRequired = 10,
            requiredExploreHours = 480,
            unlockDescription = "累计探索 480 小时（或专注 40 小时），晴岚书院调查度 5"
        ),
        // Dawnlight Spire
        RouteDef(
            id = "route_spire_steps",
            regionId = RegionId.DAWNLIGHT_SPIRE,
            name = "破晓石阶",
            subtitle = "沿着昼夜分界光带盘旋而上的金白石阶，晨风清冽醒神",
            dangerTag = "破晓金辉 · 终境启程",
            environmentTag = EnvironmentTag.DAWNLIGHT,
            recommendationHint = "高阶速度与破甲交锋频繁，建议配备心流疾袭与定心守御",
            recommendedPowerTier = 8,
            baseGoldPerHour = 830L,
            baseXpPerHour = 980L,
            baseMaterialPerHour = 26L,
            dangerRate = 0.48,
            enemyIds = listOf("enemy_dawn_lark", "enemy_prism_beetle", "enemy_horizon_lynx"),
            bossId = "boss_spire_soluna_warden",
            bossInvestigationRequired = 8,
            requiredExploreHours = 516,
            unlockDescription = "累计探索 516 小时并踏足晴岚书院，或累计专注满 48 小时且角色达 Lv.42"
        ),
        RouteDef(
            id = "route_spire_pendulum",
            regionId = RegionId.DAWNLIGHT_SPIRE,
            name = "黄金钟摆廊",
            subtitle = "巨大双色摆锤随日出与月落轻鸣晃动的高空机械殿堂",
            dangerTag = "破晓金辉 · 双曜试炼",
            environmentTag = EnvironmentTag.DAWNLIGHT,
            recommendationHint = "适合检验 8 阶成型流派与 4 阶星图共鸣的巅峰路线",
            recommendedPowerTier = 8,
            baseGoldPerHour = 910L,
            baseXpPerHour = 1080L,
            baseMaterialPerHour = 28L,
            dangerRate = 0.50,
            enemyIds = listOf("enemy_prism_beetle", "enemy_solstice_warden", "enemy_chime_seraph"),
            bossId = "boss_spire_soluna_warden",
            bossInvestigationRequired = 9,
            requiredExploreHours = 552,
            unlockDescription = "累计探索 552 小时（或专注 58 小时），晨曦钟塔调查度 3"
        ),
        RouteDef(
            id = "route_spire_zenith",
            regionId = RegionId.DAWNLIGHT_SPIRE,
            name = "双曜天枢之巅",
            subtitle = "白昼旭日与静夜星海在此重叠，守护着一切专注与好眠的终极归处",
            dangerTag = "天枢之巅 · 终极首领",
            environmentTag = EnvironmentTag.DAWNLIGHT,
            recommendationHint = "建议携满阶双曜神装与朝露/岑挑战双曜守时神偶",
            recommendedPowerTier = 8,
            baseGoldPerHour = 1020L,
            baseXpPerHour = 1200L,
            baseMaterialPerHour = 30L,
            dangerRate = 0.52,
            enemyIds = listOf("enemy_solstice_warden", "enemy_chime_seraph", "enemy_zenith_arbiter"),
            bossId = "boss_spire_soluna_warden",
            bossInvestigationRequired = 10,
            requiredExploreHours = 588,
            unlockDescription = "累计探索 588 小时（或专注 70 小时），晨曦钟塔调查度 5"
        )
    )

    val extraEquipmentDefs: List<EquipmentDef> = listOf(
        // Tier 7 Sunlit Athenaeum (5 pieces)
        EquipmentDef(
            id = "eq_athenaeum_quill_lance",
            name = "晴岚金翎枪",
            slot = EquipmentSlot.WEAPON,
            tier = 7,
            regionId = RegionId.SUNLIT_ATHENAEUM,
            baseStats = Stats(maxHp = 48, attack = 44, defense = 14, speed = 15),
            defaultAffix = AffixId.INK_SCHOLAR,
            description = "以晴岚金翎笔意锻铸的修长礼枪，每一击都带着锐利透彻的洞察。",
            craftGoldCost = 2150L,
            craftMaterialId = RegionId.SUNLIT_ATHENAEUM.materialId,
            craftMaterialCost = 42L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_athenaeum_folio_blade",
            name = "疾风手稿刃",
            slot = EquipmentSlot.WEAPON,
            tier = 7,
            regionId = RegionId.SUNLIT_ATHENAEUM,
            baseStats = Stats(maxHp = 44, attack = 46, defense = 12, speed = 17),
            defaultAffix = AffixId.POMO_BLITZ,
            description = "刃身铭刻韵律刻度的轻量仪剑，专为高效连续攻势而锻。",
            craftGoldCost = 2200L,
            craftMaterialId = RegionId.SUNLIT_ATHENAEUM.materialId,
            craftMaterialCost = 44L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_athenaeum_vellum_coat",
            name = "羊皮星纹学者袍",
            slot = EquipmentSlot.ARMOR,
            tier = 7,
            regionId = RegionId.SUNLIT_ATHENAEUM,
            baseStats = Stats(maxHp = 168, attack = 10, defense = 35, speed = 7),
            defaultAffix = AffixId.POISE_WARD,
            description = "外覆柔韧防风羊皮纸衬的晴岚长袍，让人置身喧嚣也能安如磐石。",
            craftGoldCost = 2240L,
            craftMaterialId = RegionId.SUNLIT_ATHENAEUM.materialId,
            craftMaterialCost = 45L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_athenaeum_dew_shawl",
            name = "晨露书院披肩",
            slot = EquipmentSlot.ARMOR,
            tier = 7,
            regionId = RegionId.SUNLIT_ATHENAEUM,
            baseStats = Stats(maxHp = 176, attack = 8, defense = 33, speed = 9),
            defaultAffix = AffixId.SOLUNA_DEW,
            description = "沾染晨曦露水的轻暖学士披肩，持久战与专注漫步皆能从容续航。",
            craftGoldCost = 2180L,
            craftMaterialId = RegionId.SUNLIT_ATHENAEUM.materialId,
            craftMaterialCost = 42L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_athenaeum_ink_seal",
            name = "澄明墨印坠",
            slot = EquipmentSlot.CHARM,
            tier = 7,
            regionId = RegionId.SUNLIT_ATHENAEUM,
            baseStats = Stats(maxHp = 82, attack = 22, defense = 20, speed = 15),
            defaultAffix = AffixId.INK_SCHOLAR,
            description = "封存着金粉松烟墨的玻璃坠饰，研习时总能捕捉关键灵感线索。",
            craftGoldCost = 2120L,
            craftMaterialId = RegionId.SUNLIT_ATHENAEUM.materialId,
            craftMaterialCost = 40L,
            requiredWorkshopLevel = 5
        ),

        // Tier 8 Dawnlight Spire (5 pieces)
        EquipmentDef(
            id = "eq_spire_soluna_halberd",
            name = "双曜天枢戟",
            slot = EquipmentSlot.WEAPON,
            tier = 8,
            regionId = RegionId.DAWNLIGHT_SPIRE,
            baseStats = Stats(maxHp = 60, attack = 54, defense = 18, speed = 18),
            defaultAffix = AffixId.POMO_BLITZ,
            description = "融合晨曦棱晶与星夜青铜的至高礼戟，破风而行势不可挡。",
            craftGoldCost = 2850L,
            craftMaterialId = RegionId.DAWNLIGHT_SPIRE.materialId,
            craftMaterialCost = 48L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_spire_dawn_scepter",
            name = "破晓时计权杖",
            slot = EquipmentSlot.WEAPON,
            tier = 8,
            regionId = RegionId.DAWNLIGHT_SPIRE,
            baseStats = Stats(maxHp = 68, attack = 51, defense = 20, speed = 16),
            defaultAffix = AffixId.INK_SCHOLAR,
            description = "杖首嵌着能自动校对昼夜潮汐的黄金星盘，攻防与洞见俱佳。",
            craftGoldCost = 2820L,
            craftMaterialId = RegionId.DAWNLIGHT_SPIRE.materialId,
            craftMaterialCost = 46L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_spire_bastion_aegis",
            name = "钟塔金辉重甲",
            slot = EquipmentSlot.ARMOR,
            tier = 8,
            regionId = RegionId.DAWNLIGHT_SPIRE,
            baseStats = Stats(maxHp = 210, attack = 12, defense = 42, speed = 8),
            defaultAffix = AffixId.POISE_WARD,
            description = "用晨曦钟塔流金合金与棱晶编织的圣铠，无论清醒还是守夜都安稳如山。",
            craftGoldCost = 2900L,
            craftMaterialId = RegionId.DAWNLIGHT_SPIRE.materialId,
            craftMaterialCost = 50L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_spire_eclipse_robe",
            name = "日暮双生法袍",
            slot = EquipmentSlot.ARMOR,
            tier = 8,
            regionId = RegionId.DAWNLIGHT_SPIRE,
            baseStats = Stats(maxHp = 202, attack = 14, defense = 39, speed = 11),
            defaultAffix = AffixId.SOLUNA_DEW,
            description = "一面泛着晴空暖金、内衬沉入深邃星蓝的双面旅袍，见证昼夜守望。",
            craftGoldCost = 2860L,
            craftMaterialId = RegionId.DAWNLIGHT_SPIRE.materialId,
            craftMaterialCost = 48L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_spire_prism_astrolabe",
            name = "天枢棱晶表",
            slot = EquipmentSlot.CHARM,
            tier = 8,
            regionId = RegionId.DAWNLIGHT_SPIRE,
            baseStats = Stats(maxHp = 98, attack = 26, defense = 24, speed = 18),
            defaultAffix = AffixId.POISE_WARD,
            description = "折射七道破晓光环的精密怀表坠饰，能稳定守护每次心流与安眠。",
            craftGoldCost = 2800L,
            craftMaterialId = RegionId.DAWNLIGHT_SPIRE.materialId,
            craftMaterialCost = 45L,
            requiredWorkshopLevel = 5
        )
    )

    val extraCompanions: List<CompanionStoryDef> = listOf(
        CompanionStoryDef(
            id = "comp_wen",
            name = "温言",
            title = "晴岚司书",
            passiveId = "passive_archivist_eye",
            combatSkillName = "墨羽破阵",
            combatRoleSummary = "随行时开局获得 18 点护盾，并使旅人穿透目标 12% 防护",
            dispatchRoleSummary = "营地协理时每 2 小时整理典籍抄本，带回晴岚羊皮卷、金币与澄明墨晶",
            dispatchRegionId = RegionId.SUNLIT_ATHENAEUM,
            studyGreeting = "「书页沙沙作响的时候，心反而最容易静下来。今天想先推进哪一桩念头？」",
            studyFocusLine = "温言替你在笔记本角落夹上一枚松木书签，继续低头誊录典籍。",
            distractionNudgeLine = "「偶尔抬头喘口气或查查资料没关系，准备好了再回到书桌前就好。」",
            chapters = listOf(
                CompanionChapterDef(
                    chapterIndex = 1,
                    title = "第一章 · 墨香书签",
                    requiredInnLevel = 0,
                    requiredExploreHours = 0,
                    requiredInvestigation = 0,
                    regionId = RegionId.SUNLIT_ATHENAEUM,
                    summary = "当你在白天安安静静走完三轮专注后，戴着细框铜镜的司书温言循着墨香来到营地，替书斋带来了第一架可翻阅的旅行手札。"
                ),
                CompanionChapterDef(
                    chapterIndex = 2,
                    title = "第二章 · 藏书阁梯架",
                    requiredInnLevel = 3,
                    requiredExploreHours = 416,
                    requiredInvestigation = 2,
                    regionId = RegionId.SUNLIT_ATHENAEUM,
                    summary = "同温言一起登临云端藏书阁后，你们把沉眠百年的古代旅人高效笔记重新编目，整个研习书斋焕然一新。"
                ),
                CompanionChapterDef(
                    chapterIndex = 3,
                    title = "第三章 · 清醒者的穹顶",
                    requiredInnLevel = 4,
                    requiredExploreHours = 464,
                    requiredInvestigation = 6,
                    regionId = RegionId.SUNLIT_ATHENAEUM,
                    summary = "在日晷穹顶秘馆金色的午后光线里，温言合上新编的《昼梦总录》笑道：专注并非苦役，而是与时间达成温和契约。"
                )
            )
        ),
        CompanionStoryDef(
            id = "comp_zhaolu",
            name = "朝露",
            title = "晨钟制图师",
            passiveId = "passive_quill_tempo",
            combatSkillName = "破晓校时",
            combatRoleSummary = "随行时速度 +4；每第 2 回合协同发出破晓疾袭",
            dispatchRoleSummary = "营地协理时每 2 小时测绘天枢晷针刻度，带回破晓棱晶、经验与澄明墨晶",
            dispatchRegionId = RegionId.DAWNLIGHT_SPIRE,
            studyGreeting = "「滴答、滴答——把这一刻留给手头重要的事，剩下的交给我们一起守望。」",
            studyFocusLine = "朝露一边轻敲铜质校准音叉，一边替你记录稳步推进的时间刻度。",
            distractionNudgeLine = "「节拍稍微偏了一小格也不要紧，把这一小段稳稳接上就行。」",
            chapters = listOf(
                CompanionChapterDef(
                    chapterIndex = 1,
                    title = "第一章 · 破晓音叉",
                    requiredInnLevel = 0,
                    requiredExploreHours = 0,
                    requiredInvestigation = 0,
                    regionId = RegionId.DAWNLIGHT_SPIRE,
                    summary = "累计专注迈过 6 小时后，背着铜制经纬仪的制图师朝露敲门而来，教你用昼夜节律的黄金比例划分专研与休憩。"
                ),
                CompanionChapterDef(
                    chapterIndex = 2,
                    title = "第二章 · 双曜摆锤图",
                    requiredInnLevel = 4,
                    requiredExploreHours = 516,
                    requiredInvestigation = 2,
                    regionId = RegionId.DAWNLIGHT_SPIRE,
                    summary = "与朝露并肩校准黄金钟摆廊的巨型铜锤后，你们发现白昼的专心致志正悄悄照亮夜晚的深沉好梦。"
                ),
                CompanionChapterDef(
                    chapterIndex = 3,
                    title = "第三章 · 昼夜交响时刻",
                    requiredInnLevel = 5,
                    requiredExploreHours = 564,
                    requiredInvestigation = 6,
                    regionId = RegionId.DAWNLIGHT_SPIRE,
                    summary = "站在双曜天枢之巅迎向旭日与星辰同时亮起的地平线，朝露郑重记下你的名字——白昼拼搏与夜间安眠在此合一。"
                )
            )
        )
    )

    val extraKeepsakes: List<KeepsakeDef> = listOf(
        KeepsakeDef(
            id = "keepsake_athenaeum_quill",
            name = "晴岚金翎笔",
            regionId = RegionId.SUNLIT_ATHENAEUM,
            bossMarkCost = 2L,
            goldCost = 2600L,
            materialId = RegionId.SUNLIT_ATHENAEUM.materialId,
            materialCost = 44L,
            effectSummary = "永久攻击 +10，速度 +3；专注墨晶与全境经验 +8%",
            description = "司卷星灵·墨羽留下的黄金羽笔，墨迹在日光下泛着安神淡金微光。"
        ),
        KeepsakeDef(
            id = "keepsake_soluna_sundial",
            name = "双曜星晷仪",
            regionId = RegionId.DAWNLIGHT_SPIRE,
            bossMarkCost = 3L,
            goldCost = 3400L,
            materialId = RegionId.DAWNLIGHT_SPIRE.materialId,
            materialCost = 48L,
            effectSummary = "永久生命 +42，攻击 +10，防护 +8；昼夜全收益 +6%",
            description = "依照双曜天枢之巅圣物缩微打造的精密桌面日晷，昼转旭日、夜映繁星。"
        )
    )

    val extraMilestones: List<MilestoneDef> = listOf(
        MilestoneDef("ms_first_focus", "初试心流", "完成第 1 次白昼专注（番茄或心流）结算", "把放下手机专心做事的这一刻，写成一段稳稳向前的冒险。"),
        MilestoneDef("ms_focus_10h", "十时定力", "累计有效专注时长达到 10 小时", "在无数个 25 分钟积累中，看见自己的专注与角色同步生长。"),
        MilestoneDef("ms_focus_50h", "晴岚求索者", "累计有效专注时长达到 50 小时", "五十小时清醒而温柔的投入，足够在山海之间踏出清晰道路。"),
        MilestoneDef("ms_poise_master", "如水定心", "单次满 15 分钟以上的专注达成 10% 满额专注定力祝福", "未受打扰地沉浸于热爱之事，收获一份圆融自足的心流余韵。"),
        MilestoneDef("ms_scriptorium_builder", "书斋掌灯人", "将营地「研习书斋」修葺至 Lv.3 及以上", "墨香与典籍为伴，让每一次白昼专研都沉淀为恒久底蕴。"),
        MilestoneDef("ms_spirit_collector", "灵兽挚友", "收集并结识至少 6 只「伴读小灵兽」", "这些毛茸茸或微光闪烁的小生灵，最喜欢安静陪在书桌一角。"),
        MilestoneDef("ms_task_finisher", "悬赏履约人", "累计完成至少 10 项今日行囊待办任务", "把宏大目标拆进一件件眼前小事，在现实与旅途双双打卡达成。"),
        MilestoneDef("ms_soluna_cycle", "昼夜双行者", "同时拥有有效专注记录与夜间安眠记录并累计激活昼夜互哺", "白昼全神贯注，夜晚踏实安眠，让时间回到顺畅自然的节律。"),
        MilestoneDef("ms_athenaeum_scholar", "晴岚登堂客", "踏足晴岚书院并解锁全部 21 条探索路线", "穿过古卷林立的晴岚长廊，登顶云端藏书秘馆。"),
        MilestoneDef("ms_dawnlight_master", "双曜巡礼者", "踏足晨曦钟塔并解锁全部 24 条昼夜路线", "连通八境山海与昼夜天枢，在自己的节奏里从容前行。")
    )

    val extraFocusDrinks: List<TeaBlendDef> = listOf(
        TeaBlendDef(
            id = "tea_pine_cold_brew",
            name = "冰滴松针冷萃",
            subtitle = "专注首选：攻击 +3，专注经验 +8%，墨晶 +10%",
            requiredTeaHearthLevel = 1,
            bonusAtk = 3,
            xpMultiplierBonus = 0.08,
            favoredForFocus = true,
            inkBonusPercent = 10
        ),
        TeaBlendDef(
            id = "tea_citrus_rosemary",
            name = "柑橘迷迭香露",
            subtitle = "专注首选：生命 +18，金币 +6%，墨晶 +14%",
            requiredTeaHearthLevel = 2,
            bonusHp = 18,
            goldMultiplierBonus = 0.06,
            favoredForFocus = true,
            inkBonusPercent = 14
        ),
        TeaBlendDef(
            id = "tea_barley_sea_latte",
            name = "海盐焙茶拿铁",
            subtitle = "专注首选：攻击 +5，防护 +3，装备寻获 +10%，墨晶 +16%",
            requiredTeaHearthLevel = 3,
            bonusAtk = 5,
            bonusDef = 3,
            dropRateBonus = 0.10,
            favoredForFocus = true,
            inkBonusPercent = 16
        ),
        TeaBlendDef(
            id = "tea_cloud_matcha",
            name = "云栖浮沫抹茶",
            subtitle = "专注首选：生命 +26，金币 +8%，经验 +8%，墨晶 +20%",
            requiredTeaHearthLevel = 4,
            bonusHp = 26,
            goldMultiplierBonus = 0.08,
            xpMultiplierBonus = 0.08,
            favoredForFocus = true,
            inkBonusPercent = 20
        ),
        TeaBlendDef(
            id = "tea_sunlit_pour_over",
            name = "晴岚琥珀手冲",
            subtitle = "专注首选：攻击 +8，防护 +6，掉落 +15%，墨晶 +25%",
            requiredTeaHearthLevel = 5,
            bonusAtk = 8,
            bonusDef = 6,
            dropRateBonus = 0.15,
            favoredForFocus = true,
            inkBonusPercent = 25
        ),
        TeaBlendDef(
            id = "tea_soluna_nectar",
            name = "晨昏双曜甘露",
            subtitle = "昼夜兼宜：全属性大幅提升，经验 +10%，容错 +1，墨晶 +28%",
            requiredTeaHearthLevel = 5,
            bonusAtk = 9,
            bonusDef = 8,
            bonusHp = 36,
            goldMultiplierBonus = 0.08,
            xpMultiplierBonus = 0.10,
            dropRateBonus = 0.14,
            extraFailureTolerance = 1,
            favoredForFocus = true,
            inkBonusPercent = 28
        )
    )

    val dayPhenomena: List<DayPhenomenon> = listOf(
        DayPhenomenon("day_mist_sunbeam", "林间晨曦光柱", "斜照林雾的暖阳让人神清气爽", RegionId.MISTWOOD, EnvironmentTag.FOREST, xpBonusPercent = 8, materialBonusPercent = 10, inkBonusPercent = 12),
        DayPhenomenon("day_crisp_sea_breeze", "清朗海风晴日", "海面水波澄净，最宜整理思绪", RegionId.TIDAL_FLAT, EnvironmentTag.DAMP, goldBonusPercent = 10, dropBonusPercent = 8, inkBonusPercent = 12),
        DayPhenomenon("day_alpine_clarity", "高地晴雪澄空", "视野极远的高原澄空气流", RegionId.STARLIT_RIDGE, EnvironmentTag.CHILL, xpBonusPercent = 12, combatAtkPercent = 6, inkBonusPercent = 14),
        DayPhenomenon("day_halo_cloud_sea", "云海彩晕日", "日晕环绕云栖浮岛，温柔提神", RegionId.CLOUD_SANCTUARY, EnvironmentTag.CLOUD, goldBonusPercent = 10, materialBonusPercent = 10, inkBonusPercent = 14),
        DayPhenomenon("day_sapphire_lagoon", "湛蓝静海昼", "梦海浅湾清澈如镜，闪着银砂微光", RegionId.DREAM_SEA, EnvironmentTag.ASTRAL, goldBonusPercent = 12, dropBonusPercent = 12, inkBonusPercent = 16),
        DayPhenomenon("day_amber_noon", "琥珀正午回响", "星拱钟楼的阳光正午静谧沉稳", RegionId.ASTRAL_ARCH, EnvironmentTag.SOLAR_ECHO, xpBonusPercent = 12, combatDefPercent = 8, inkBonusPercent = 16),
        DayPhenomenon("day_sunlit_breeze", "晴岚翻书微风", "书院书页轻扬，灵感和线索倍增", RegionId.SUNLIT_ATHENAEUM, EnvironmentTag.SUNLIT, xpBonusPercent = 14, dropBonusPercent = 10, inkBonusPercent = 22),
        DayPhenomenon("day_golden_horizon", "破晓金辉映昼", "钟塔顶层的金色光带校准一切节拍", RegionId.DAWNLIGHT_SPIRE, EnvironmentTag.DAWNLIGHT, goldBonusPercent = 12, materialBonusPercent = 12, inkBonusPercent = 24)
    )

    val focusSpirits: List<FocusSpiritDef> = listOf(
        // Mistwood
        FocusSpiritDef("spirit_moss_owlet", RegionId.MISTWOOD, "绒苔小鸮", "林雾陪读者", "总在翻书间隙轻眨圆眼的迷你猫头鹰，羽毛散发淡淡松香。", "雾林细雨", true, Stats(maxHp = 8), focusXpBonusPercent = 2),
        FocusSpiritDef("spirit_rain_frog", RegionId.MISTWOOD, "听雨小树蛙", "节拍小哨兵", "安静趴在玻璃水杯沿，随着雨滴落下的节奏打节拍。", "雾林细雨", true, Stats(defense = 2), inkBonusPercent = 3),
        // Tidal Flat
        FocusSpiritDef("spirit_shell_otter", RegionId.TIDAL_FLAT, "抱贝海獭", "算珠小管家", "怀里紧紧抱住一枚闪亮白贝壳，最擅长帮你清点收获。", "银滩潮声", true, Stats(maxHp = 10), goldBonusPercent = 2),
        FocusSpiritDef("spirit_tide_puffin", RegionId.TIDAL_FLAT, "白浪海鹦", "海风信使雀", "短喙夹着卷好的便利贴站在窗棂边，提醒别忘了休息喝水。", "银滩潮声", true, Stats(speed = 2), focusXpBonusPercent = 2),
        // Starlit Ridge
        FocusSpiritDef("spirit_star_dormouse", RegionId.STARLIT_RIDGE, "摘星睡鼠", "壁炉伴睡友", "窝在暖羊毛围巾里打盹，清醒时会递出一颗发光小星石。", "火塘柴火", false, Stats(maxHp = 12, defense = 1), inkBonusPercent = 3),
        FocusSpiritDef("spirit_quartz_hare", RegionId.STARLIT_RIDGE, "碎晶雪兔", "雪岭探路兔", "耳尖半透明像冰晶一样清澈，让人在长时段工作时保持清醒。", "沉静棕噪", true, Stats(attack = 2, speed = 1), focusXpBonusPercent = 3),
        // Cloud Sanctuary
        FocusSpiritDef("spirit_cloud_lamb", RegionId.CLOUD_SANCTUARY, "云朵小羊", "浮岛午休伴", "软绵绵得像一团飘浮的积云，枕在它旁边的影子都能多歇三分。", "云巅风铃", false, Stats(maxHp = 16), goldBonusPercent = 2),
        FocusSpiritDef("spirit_breeze_hummingbird", RegionId.CLOUD_SANCTUARY, "风铃蜂鸟", "花蜜协理官", "翅膀振动出几近无声的五音共鸣，替专注的人梳理思绪。", "云巅风铃", true, Stats(speed = 3), inkBonusPercent = 4),
        // Dream Sea
        FocusSpiritDef("spirit_bubble_dolphin", RegionId.DREAM_SEA, "浮星小海豚", "深海潜游者", "吐出带着银河碎光的泡泡，最合拍需要长时间心流潜航的夜晚。", "沉静棕噪", true, Stats(attack = 3), focusXpBonusPercent = 3),
        FocusSpiritDef("spirit_pearl_hermit", RegionId.DREAM_SEA, "梦珠小寄居", "桌面藏宝蟹", "背着透明琉璃螺壳在桌角慢爬，把散落金币一枚枚码齐。", "银滩潮声", false, Stats(defense = 3, maxHp = 10), goldBonusPercent = 3),
        // Astral Arch
        FocusSpiritDef("spirit_sand_fox", RegionId.ASTRAL_ARCH, "时隙沙狐", "逆沙守时灵", "尾巴尖飘散细碎琥珀沙粒，提醒你把握眼下每一分钟。", "书斋钟摆", true, Stats(attack = 4, speed = 2), inkBonusPercent = 4),
        FocusSpiritDef("spirit_amber_mothlet", RegionId.ASTRAL_ARCH, "金珀小灯蛾", "夜读护灯灵", "静静停在台灯灯罩旁，散出刚好不刺眼的温润暖光。", "火塘柴火", false, Stats(maxHp = 18, defense = 2), focusXpBonusPercent = 3),
        // Sunlit Athenaeum
        FocusSpiritDef("spirit_ink_cat", RegionId.SUNLIT_ATHENAEUM, "踏墨书猫", "书斋巡检官", "绝不会踩乱写满字的草稿纸，只会用尾巴轻压住书角防风。", "书斋钟摆", true, Stats(attack = 4, maxHp = 14), inkBonusPercent = 6),
        FocusSpiritDef("spirit_page_sparrow", RegionId.SUNLIT_ATHENAEUM, "折页云雀", "晨读唤醒雀", "叼着一片晒干的橄榄叶立在书架上，完成一轮番茄钟就会轻啾祝贺。", "雾林细雨", true, Stats(speed = 3, defense = 2), focusXpBonusPercent = 4),
        // Dawnlight Spire
        FocusSpiritDef("spirit_dawn_finch", RegionId.DAWNLIGHT_SPIRE, "报时金雀", "双曜领航鸟", "羽翼一面金灿如朝阳、一面深蓝似繁星，自带精确节律感。", "书斋钟摆", true, Stats(attack = 5, speed = 3), goldBonusPercent = 4),
        FocusSpiritDef("spirit_eclipse_sloth", RegionId.DAWNLIGHT_SPIRE, "日暮小考拉", "从容平衡灵", "用行动示范什么叫“专注时心无旁骛，休息时彻底松弛”。", "沉静棕噪", false, Stats(maxHp = 24, defense = 4), inkBonusPercent = 5)
    )

    val defaultFocusTags: List<FocusTagDef> = listOf(
        FocusTagDef("tag_study", "学习备考", BuildArchetype.SCHOLAR, "线索发现权重 +15%，加快图鉴与秘闻推进"),
        FocusTagDef("tag_work", "深度工作", BuildArchetype.BASTION, "专注金币产出 +12%，稳步建设营地"),
        FocusTagDef("tag_code", "编程钻研", BuildArchetype.GALE, "装备寻获进度 +15%，更快攒齐心仪词条"),
        FocusTagDef("tag_read", "阅读写作", BuildArchetype.SCHOLAR, "专注经验产出 +12%，稳步提升旅人等级"),
        FocusTagDef("tag_create", "灵感创作", BuildArchetype.DREAMWEAVE, "澄明墨晶产出 +15%，更快修葺研习书斋"),
        FocusTagDef("tag_mindful", "静心冥想", BuildArchetype.DREAMWEAVE, "休整与材料采集微幅提升，抚平身心疲惫")
    )

    val extraEvents: List<EventTemplate> = listOf(
        // Mistwood (+3 -> 16)
        EventTemplate("ev_mw_focus_sketch", RegionId.MISTWOOD, "林荫木桌速写", EventKind.CLUE, 20, 1, 0, firstDiscoveryText = "白天穿行雾林外径时，你在一张晒得暖洋洋的树桩木桌旁静坐记录，顺手拓下稀奇苔纹。", repeatText = "在树桩木桌旁对照笔记，又记下一串林鸟啼鸣时刻。", bonusGold = 18L, bonusXp = 28L, bonusMaterial = 2L, investigationGain = 1, focusOnly = true),
        EventTemplate("ev_mw_sunbeam_herbs", RegionId.MISTWOOD, "晨光晾晒香草架", EventKind.GATHER, 22, 0, 0, firstDiscoveryText = "阳光透过松针照在山野草药架上，空气满是微甜木香，你采下干透的草捆收入囊中。", repeatText = "帮营地信使翻晒晨光草药架，顺手装回一束干燥松枝。", bonusGold = 16L, bonusXp = 22L, bonusMaterial = 3L, investigationGain = 0, focusOnly = true),
        EventTemplate("ev_mw_noon_brook", RegionId.MISTWOOD, "午后清泉茶歇石", EventKind.REST, 20, 1, 0, firstDiscoveryText = "清澈山泉从石缝涌出，刚好适合专注告一段落时洗把脸、听水声放空片刻。", repeatText = "听着山泉叮咚声在溪石边稍作舒展，精神一振。", bonusGold = 14L, bonusXp = 24L, bonusMaterial = 1L, investigationGain = 1, focusOnly = true),

        // Tidal Flat (+3 -> 16)
        EventTemplate("ev_tf_sunlit_tide_log", RegionId.TIDAL_FLAT, "日光晒盐观测亭", EventKind.CLUE, 20, 1, 0, firstDiscoveryText = "日头正高时，浅滩石碑上的潮位刻度被阳光照得清晰无比，省去了半天摸索。", repeatText = "趁海面澄明记下今日低潮浅沙路径。", bonusGold = 28L, bonusXp = 38L, bonusMaterial = 2L, investigationGain = 1, focusOnly = true),
        EventTemplate("ev_tf_drift_desk", RegionId.TIDAL_FLAT, "旧灯塔窗台读本", EventKind.REST, 20, 1, 0, firstDiscoveryText = "灯塔二楼朝海的宽大窗台摆着几册被海风翻黄的航海笔记，伴着浪声阅读格外专注。", repeatText = "倚在灯塔窗台伴着海鸥鸣声读完一段旧日记。", bonusGold = 24L, bonusXp = 42L, bonusMaterial = 1L, investigationGain = 1, focusOnly = true),
        EventTemplate("ev_tf_amber_shell_pile", RegionId.TIDAL_FLAT, "晴日闪光贝砂堆", EventKind.GATHER, 22, 0, 0, firstDiscoveryText = "日光下格外耀眼的贝壳堆里混着高纯度海矿砂与几枚银质船纽扣。", repeatText = "在阳光照亮的礁石湾拾回干净矿砂。", bonusGold = 26L, bonusXp = 28L, bonusMaterial = 3L, investigationGain = 0, focusOnly = true),

        // Starlit Ridge (+3 -> 16)
        EventTemplate("ev_sr_alpine_study_nook", RegionId.STARLIT_RIDGE, "晴雪观景石案", EventKind.REST, 20, 1, 0, firstDiscoveryText = "高地背风处架着一块平整石案，远眺连绵雪峰让人心里所有的浮躁都安静下来。", repeatText = "在晴雪石案边喝口温水整理行囊记录，思路澄明。", bonusGold = 34L, bonusXp = 54L, bonusMaterial = 2L, investigationGain = 1, focusOnly = true),
        EventTemplate("ev_sr_quartz_refraction", RegionId.STARLIT_RIDGE, "日光棱晶折射阵", EventKind.CLUE, 20, 1, 0, firstDiscoveryText = "正午斜阳穿透星辉碎晶簇，在岩壁投出隐约的古代测量坐标。", repeatText = "顺着阳光折射光斑核对了山脊北段地图。", bonusGold = 40L, bonusXp = 58L, bonusMaterial = 2L, investigationGain = 2, focusOnly = true),
        EventTemplate("ev_sr_sunny_crystal_shelf", RegionId.STARLIT_RIDGE, "向阳岩脊采晶坡", EventKind.GATHER, 22, 0, 0, firstDiscoveryText = "白昼暖阳融开了薄冰，一排整齐的星辉晶簇露了出来。", repeatText = "趁着白昼暖光轻松凿下几块完好碎晶。", bonusGold = 36L, bonusXp = 40L, bonusMaterial = 3L, investigationGain = 0, focusOnly = true),

        // Cloud Sanctuary (+3 -> 16)
        EventTemplate("ev_cs_sun_conservatory", RegionId.CLOUD_SANCTUARY, "日光玻璃花房长桌", EventKind.REST, 20, 1, 0, firstDiscoveryText = "玻璃穹顶把云海反射的阳光柔和散开，在花香环绕的长桌旁专心研读简直是享受。", repeatText = "伴着温室水滴落在叶片的轻响舒心休整。", bonusGold = 48L, bonusXp = 74L, bonusMaterial = 2L, investigationGain = 1, focusOnly = true),
        EventTemplate("ev_cs_cloud_silk_dry_rack", RegionId.CLOUD_SANCTUARY, "云风晾丝木架", EventKind.GATHER, 22, 0, 0, firstDiscoveryText = "晴朗高空信风把成捆梦绒丝吹得蓬松干燥，顺手帮琳收进两袋好料。", repeatText = "沿着向阳云桥收拢晒得松软的云栖灵绒。", bonusGold = 52L, bonusXp = 56L, bonusMaterial = 3L, investigationGain = 0, focusOnly = true),
        EventTemplate("ev_cs_sky_chart_chalk", RegionId.CLOUD_SANCTUARY, "云巅白石测风板", EventKind.CLUE, 18, 2, 1, firstDiscoveryText = "白石板上的铅笔风向算式原来是前代学者留下的云海上升气流规律。", repeatText = "用随身炭笔把今日浮岛气压数据续写在测风板上。", bonusGold = 54L, bonusXp = 78L, bonusMaterial = 2L, investigationGain = 2, focusOnly = true),

        // Dream Sea (+3 -> 16)
        EventTemplate("ev_ds_azure_lagoon_desk", RegionId.DREAM_SEA, "澄海浅湾写生台", EventKind.CLUE, 20, 1, 0, firstDiscoveryText = "白昼的梦海呈现琉璃般的清透浅蓝，水底航标与古碑轮廓一览无余。", repeatText = "趁正午光影穿透浅海，迅速描摹水下航道轮廓。", bonusGold = 64L, bonusXp = 86L, bonusMaterial = 2L, investigationGain = 2, focusOnly = true),
        EventTemplate("ev_ds_sea_breeze_nap", RegionId.DREAM_SEA, "海风吊床露台", EventKind.REST, 20, 1, 0, firstDiscoveryText = "系在沉船桅杆与灯塔柱之间的帆布吊床微晃，午后小憩十分恰当。", repeatText = "吹着温柔的蓝海微风放松双肩，准备迎接下一轮专研。", bonusGold = 52L, bonusXp = 90L, bonusMaterial = 1L, investigationGain = 1, focusOnly = true),
        EventTemplate("ev_ds_sunlit_pearl_reef", RegionId.DREAM_SEA, "白昼采珠礁带", EventKind.GATHER, 22, 0, 0, firstDiscoveryText = "阳光直照浅湾珊瑚缝，几颗沉星梦珠在沙底闪着温润贝光。", repeatText = "在明净浅海拾起多颗饱满的沉星梦珠。", bonusGold = 68L, bonusXp = 68L, bonusMaterial = 3L, investigationGain = 0, focusOnly = true),

        // Astral Arch (+3 -> 16)
        EventTemplate("ev_aa_noon_sundial_shadow", RegionId.ASTRAL_ARCH, "日晷正午校准碑", EventKind.CLUE, 20, 1, 0, firstDiscoveryText = "当正午日影恰好压住星拱日晷的中心刻痕，石柱侧面滑出一卷《时间手札》。", repeatText = "记下正午晷影角度，为日间专注与夜间安眠对齐刻度。", bonusGold = 76L, bonusXp = 108L, bonusMaterial = 2L, investigationGain = 2, focusOnly = true),
        EventTemplate("ev_aa_amber_tea_gallery", RegionId.ASTRAL_ARCH, "琥珀长廊茶歇座", EventKind.REST, 20, 1, 0, firstDiscoveryText = "琥珀石壁把日光滤成蜂蜜般的柔和金光，坐在这里读一章书几乎忘了时间。", repeatText = "在琥珀长廊柔光里放慢呼吸，攒足心流精力。", bonusGold = 68L, bonusXp = 104L, bonusMaterial = 2L, investigationGain = 1, focusOnly = true),
        EventTemplate("ev_aa_sunlit_amber_vein", RegionId.ASTRAL_ARCH, "晨光琥珀采集檐", EventKind.GATHER, 22, 0, 0, firstDiscoveryText = "白天干燥的风让琥珀壁檐凝出大块透亮晶滴，轻敲即可成整落袋。", repeatText = "采集一袋通透干净的星拱琥珀块。", bonusGold = 80L, bonusXp = 90L, bonusMaterial = 3L, investigationGain = 0, focusOnly = true),

        // Sunlit Athenaeum (16 events)
        EventTemplate("ev_sa_sun_desk", RegionId.SUNLIT_ATHENAEUM, "晴岚长桌抄本", EventKind.CLUE, 28, 0, 0, firstDiscoveryText = "露天藏书长廊的橡木长桌摆着翻开一半的《万物专研集》，字迹工整清秀。", repeatText = "在晴岚长桌旁补抄了一页失落的旅行索引。", bonusGold = 86L, bonusXp = 122L, bonusMaterial = 2L, investigationGain = 2, companionId = "comp_wen"),
        EventTemplate("ev_sa_vellum_drying", RegionId.SUNLIT_ATHENAEUM, "晾晒羊皮卷架", EventKind.GATHER, 32, 0, 0, firstDiscoveryText = "书院南廊挂着成排刚涂过蜂蜡防潮的洁净羊皮纸卷，温言说盈余料正可携回工坊。", repeatText = "从南廊木架收集了成卷结实的晴岚羊皮卷。", bonusGold = 82L, bonusXp = 96L, bonusMaterial = 3L, investigationGain = 0),
        EventTemplate("ev_sa_espresso_kiosk", RegionId.SUNLIT_ATHENAEUM, "院角手冲咖啡亭", EventKind.REST, 24, 1, 0, firstDiscoveryText = "书院石亭里放着铜制手冲壶与小磨豆机，淡淡烘焙香气瞬间令人神清气爽。", repeatText = "在咖啡亭小憩并顺手冲了半壶提神清茶。", bonusGold = 72L, bonusXp = 118L, bonusMaterial = 1L, investigationGain = 1),
        EventTemplate("ev_sa_brass_ladder", RegionId.SUNLIT_ATHENAEUM, "滑动黄铜书梯", EventKind.CLUE, 22, 1, 1, firstDiscoveryText = "沿滑轨推开三层高书架，后壁露出一份标着“藏书阁密门”的平面简图。", repeatText = "顺着黄铜书梯爬上高架核对目录编号，收获颇丰。", bonusGold = 92L, bonusXp = 128L, bonusMaterial = 2L, investigationGain = 2),
        EventTemplate("ev_sa_paper_sprite_chase", RegionId.SUNLIT_ATHENAEUM, "散落书页整理赛", EventKind.ENCOUNTER, 20, 1, 1, firstDiscoveryText = "一群调皮的折页纸灵卷走整摞手稿，你配合同伴轻巧截住风口，顺便捡回遗落书匣。", repeatText = "稳稳安抚住调皮的纸灵群，把书架理得井井有条。", bonusGold = 98L, bonusXp = 126L, bonusMaterial = 3L, investigationGain = 1),
        EventTemplate("ev_sa_inkwell_fountain", RegionId.SUNLIT_ATHENAEUM, "松烟金墨泉眼", EventKind.GATHER, 26, 0, 0, requiredStrategy = ExpeditionStrategy.GATHER, firstDiscoveryText = "院落深处石钵汇集着天然松烟微粒与山泉，沉淀出的墨坯品质极高。", repeatText = "从墨泉石钵边缘舀起凝结细腻的制卷材料。", bonusGold = 88L, bonusXp = 102L, bonusMaterial = 4L, investigationGain = 0),
        EventTemplate("ev_sa_wen_annotation", RegionId.SUNLIT_ATHENAEUM, "温言的朱批手记", EventKind.CLUE, 20, 2, 2, companionId = "comp_wen", firstDiscoveryText = "温言递来一本批注密密的厚册子，教你用“二十五分心流节律”大幅加快梳理进度。", repeatText = "跟着温言的批注思路对照核验，很快理顺繁杂线索。", bonusGold = 96L, bonusXp = 136L, bonusMaterial = 2L, investigationGain = 2),
        EventTemplate("ev_sa_cat_bookmark", RegionId.SUNLIT_ATHENAEUM, "窗台晒太阳的踏墨猫", EventKind.REST, 20, 1, 1, firstDiscoveryText = "一只黑爪小猫正压住一沓未风干的晴岚地图午睡，陪它晒了会太阳心情大好。", repeatText = "轻手轻脚替小猫挡去强光后继续前行，精神格外舒畅。", bonusGold = 76L, bonusXp = 122L, bonusMaterial = 2L, investigationGain = 1),
        EventTemplate("ev_sa_sundial_garden", RegionId.SUNLIT_ATHENAEUM, "日晷迷迭香花圃", EventKind.GATHER, 22, 1, 2, firstDiscoveryText = "花圃的迷迭香随日光角度散出清醒木质香，枝条间还卡着不少旅者遗失的银币。", repeatText = "修剪几枝干爽迷迭香带回茶歇火塘备用。", bonusGold = 90L, bonusXp = 106L, bonusMaterial = 3L, investigationGain = 0),
        EventTemplate("ev_sa_echo_balcony", RegionId.SUNLIT_ATHENAEUM, "风铃抄经露台", EventKind.REST, 18, 2, 2, firstDiscoveryText = "云层从露台下方缓缓流过，唯有风吹页角的沙沙白噪音陪伴整段沉思时光。", repeatText = "在露台长椅静听白噪音般的穿堂微风，彻底放空杂念。", bonusGold = 82L, bonusXp = 130L, bonusMaterial = 1L, investigationGain = 1),
        EventTemplate("ev_sa_scroll_golem_drill", RegionId.SUNLIT_ATHENAEUM, "理卷铜偶巡检考", EventKind.ENCOUNTER, 18, 1, 3, minRegionExploreHours = 4, firstDiscoveryText = "发条理卷铜偶例行检验来客通行证，稳健过招证明实力后获准开启高阶书库。", repeatText = "配合理卷铜偶完成图书清点巡检，获得书院嘉奖。", bonusGold = 108L, bonusXp = 138L, bonusMaterial = 3L, investigationGain = 1),
        EventTemplate("ev_sa_stained_glass_clue", RegionId.SUNLIT_ATHENAEUM, "彩窗投映的钟塔坐标", EventKind.CLUE, 18, 2, 4, minRegionExploreHours = 8, firstDiscoveryText = "清晨光线穿过秘馆最高处的琉璃彩窗，在地面拼出指向晨曦钟塔的黄金轴线。", repeatText = "循着地面彩色光斑再次标定了云巅东向的高塔方位。", bonusGold = 104L, bonusXp = 148L, bonusMaterial = 2L, investigationGain = 2),
        EventTemplate("ev_sa_quill_workshop", RegionId.SUNLIT_ATHENAEUM, "制笔匠旧工位", EventKind.GATHER, 20, 1, 3, firstDiscoveryText = "旧工位抽屉里整齐放着裁好的羊皮卷坯与抛光黄铜笔套。", repeatText = "从制笔工位拾回可用材料，工坊一定派得上用场。", bonusGold = 94L, bonusXp = 112L, bonusMaterial = 3L, investigationGain = 0),
        EventTemplate("ev_sa_reading_club", RegionId.SUNLIT_ATHENAEUM, "午后伴读茶会迹", EventKind.REST, 16, 2, 4, firstDiscoveryText = "石桌边围摆着七把木椅，桌心留着几张“各自专心二十五分钟，茶点自取”的暖心便条。", repeatText = "在伴读石桌旁享受一段无言却踏实的陪伴时光。", bonusGold = 88L, bonusXp = 140L, bonusMaterial = 2L, investigationGain = 1),
        EventTemplate("ev_sa_archive_seal_clue", RegionId.SUNLIT_ATHENAEUM, "穹顶司卷印鉴匣", EventKind.CLUE, 16, 2, 6, minRegionExploreHours = 12, firstDiscoveryText = "开启玻璃陈列匣后，一枚刻着双翼墨羽的印鉴揭示了司卷星灵·墨羽的考验规律。", repeatText = "核对陈列匣里的星灵守则，对战局节拍愈发胸有成竹。", bonusGold = 112L, bonusXp = 156L, bonusMaterial = 2L, investigationGain = 2),
        EventTemplate("ev_sa_curator_trial", RegionId.SUNLIT_ATHENAEUM, "墨羽分身随堂试炼", EventKind.ENCOUNTER, 15, 2, 7, minRegionExploreHours = 16, firstDiscoveryText = "两道金墨凝聚的试炼书灵随风起舞，冷静破解护盾后满载珍贵典籍素材。", repeatText = "娴熟拆解墨羽随堂试炼，赢得整馆掌声般的风铃轻鸣。", bonusGold = 120L, bonusXp = 162L, bonusMaterial = 3L, investigationGain = 2),

        // Dawnlight Spire (16 events)
        EventTemplate("ev_ds2_horizon_steps", RegionId.DAWNLIGHT_SPIRE, "晨昏光带台阶", EventKind.CLUE, 26, 0, 0, firstDiscoveryText = "台阶石板同时反射着朝霞金辉与星夜深蓝，每一步都像踏在时间节律的琴键上。", repeatText = "沿着光带石阶记录下昼夜交接的准确刻度。", bonusGold = 108L, bonusXp = 146L, bonusMaterial = 2L, investigationGain = 2),
        EventTemplate("ev_ds2_prism_cluster", RegionId.DAWNLIGHT_SPIRE, "石檐下的棱晶簇", EventKind.GATHER, 32, 0, 0, firstDiscoveryText = "钟塔外檐挂满结晶体般澄澈的破晓棱晶，一缕阳光穿过便折出七道柔光。", repeatText = "在外檐稳稳采收一批纯净破晓棱晶。", bonusGold = 104L, bonusXp = 120L, bonusMaterial = 3L, investigationGain = 0),
        EventTemplate("ev_ds2_soluna_bench", RegionId.DAWNLIGHT_SPIRE, "双曜观景石凳", EventKind.REST, 24, 1, 0, firstDiscoveryText = "面向东方朝霞与西方星海的半圆石凳，坐在这里深呼吸四拍、吐气四拍，心瞬间安稳。", repeatText = "在双曜石凳上随呼吸律动短暂放松，再度神采奕奕。", bonusGold = 92L, bonusXp = 142L, bonusMaterial = 1L, investigationGain = 1),
        EventTemplate("ev_ds2_pendulum_sketch", RegionId.DAWNLIGHT_SPIRE, "朝露的节律测绘图", EventKind.CLUE, 22, 2, 1, companionId = "comp_zhaolu", firstDiscoveryText = "朝露用铜尺在羊皮纸上标出黄金钟摆廊最省力的步伐频率，让人一路游刃有余。", repeatText = "参照朝露的节律测绘图校准步频，线索进度迅速向前。", bonusGold = 116L, bonusXp = 158L, bonusMaterial = 2L, investigationGain = 2),
        EventTemplate("ev_ds2_bronze_chime", RegionId.DAWNLIGHT_SPIRE, "报时铜雀哨站", EventKind.ENCOUNTER, 20, 1, 1, firstDiscoveryText = "几只机关报时雀盘旋验证整点暗号，顺利应答节拍后带回亮闪闪的棱晶赠礼。", repeatText = "与机关报时雀对完整点哨音，愉快清点旅途补给。", bonusGold = 124L, bonusXp = 154L, bonusMaterial = 3L, investigationGain = 1),
        EventTemplate("ev_ds2_quartz_vault", RegionId.DAWNLIGHT_SPIRE, "日轮折射矿窖", EventKind.GATHER, 26, 0, 0, requiredStrategy = ExpeditionStrategy.GATHER, firstDiscoveryText = "钟塔基座暗室封存着用来打磨钟面的高纯度棱晶块与金币箱。", repeatText = "在折射矿窖角落规整收好一批亮晶晶的破晓棱晶。", bonusGold = 112L, bonusXp = 128L, bonusMaterial = 4L, investigationGain = 0),
        EventTemplate("ev_ds2_zenith_gearbox", RegionId.DAWNLIGHT_SPIRE, "天枢齿轮调音室", EventKind.CLUE, 20, 1, 2, firstDiscoveryText = "拨动刻着“昼”与“夜”的两枚黄铜咬合轮，钟塔底层立刻响起和谐舒缓的双音和弦。", repeatText = "倾听调音室发出的浑厚双音，更深刻地摸清钟塔运作机制。", bonusGold = 120L, bonusXp = 164L, bonusMaterial = 2L, investigationGain = 2),
        EventTemplate("ev_ds2_dawn_tea_alcove", RegionId.DAWNLIGHT_SPIRE, "钟壁暖灯茶龛", EventKind.REST, 20, 1, 2, firstDiscoveryText = "壁龛里点着永不炫目的暖金灯盏，热茶香气混着清晨露气令人倍感安心。", repeatText = "在暖灯茶龛前舒展双臂小憩一会儿。", bonusGold = 98L, bonusXp = 152L, bonusMaterial = 2L, investigationGain = 1),
        EventTemplate("ev_ds2_horizon_mirror", RegionId.DAWNLIGHT_SPIRE, "八境连光星镜", EventKind.CLUE, 18, 2, 3, minRegionExploreHours = 4, firstDiscoveryText = "透过塔腰巨型棱镜回望，雾林、潮汐滩、山脊、云岛、梦海、星拱与书院尽在一条光路上。", repeatText = "用星镜逐一检视走过的八境灯火，心中满是踏实成就感。", bonusGold = 128L, bonusXp = 172L, bonusMaterial = 2L, investigationGain = 2),
        EventTemplate("ev_ds2_clockmaker_stash", RegionId.DAWNLIGHT_SPIRE, "铸钟宗师备料箱", EventKind.GATHER, 20, 1, 3, firstDiscoveryText = "铭牌写着“留给守信者的储备箱”里装满精良棱晶与旅银币。", repeatText = "从备料箱中领取一份工整码放的棱晶补给。", bonusGold = 132L, bonusXp = 138L, bonusMaterial = 3L, investigationGain = 0),
        EventTemplate("ev_ds2_eclipse_warden_test", RegionId.DAWNLIGHT_SPIRE, "双曜石卫守门阵", EventKind.ENCOUNTER, 18, 1, 4, minRegionExploreHours = 8, firstDiscoveryText = "两尊日月双卫同时亮起试炼光环，稳健切换攻守节拍后堂堂正正通过阶门。", repeatText = "再度破开双曜石卫的合击阵型，收获格外丰厚。", bonusGold = 142L, bonusXp = 176L, bonusMaterial = 3L, investigationGain = 1),
        EventTemplate("ev_ds2_zhaolu_chronometer", RegionId.DAWNLIGHT_SPIRE, "朝露的双指针怀表", EventKind.REST, 16, 2, 4, companionId = "comp_zhaolu", minRegionExploreHours = 8, firstDiscoveryText = "朝露将一枚昼夜双刻度小怀表借你对照计时，说每段专注后的休息也是旅程不可或缺的一半。", repeatText = "听着朝露怀表轻柔准确的走时声惬意小憩。", bonusGold = 110L, bonusXp = 168L, bonusMaterial = 2L, investigationGain = 2),
        EventTemplate("ev_ds2_solar_fresco", RegionId.DAWNLIGHT_SPIRE, "穹顶昼夜壁画", EventKind.CLUE, 16, 2, 5, minRegionExploreHours = 12, firstDiscoveryText = "穹顶金箔壁画描绘着昼行研习、夜归安眠的古老城市，每颗星辰都对应一段坚持的时光。", repeatText = "仰望昼夜壁画寻找缺失星轨，进一步掌握天枢之巅脉络。", bonusGold = 136L, bonusXp = 184L, bonusMaterial = 2L, investigationGain = 2),
        EventTemplate("ev_ds2_aurora_crystal_rain", RegionId.DAWNLIGHT_SPIRE, "天枢光屑微雨", EventKind.GATHER, 18, 1, 6, minRegionExploreHours = 14, firstDiscoveryText = "整点敲响时塔尖飘落细密温柔的棱晶光屑，在披风上凝出一把晶莹碎晶。", repeatText = "趁整点钟声落下时收集纯净的破晓棱晶微粒。", bonusGold = 138L, bonusXp = 152L, bonusMaterial = 4L, investigationGain = 0),
        EventTemplate("ev_ds2_soluna_altar", RegionId.DAWNLIGHT_SPIRE, "天枢日晷誓约台", EventKind.CLUE, 16, 2, 7, minRegionExploreHours = 18, firstDiscoveryText = "双曜守时神偶底座刻着温和箴言：“白昼尽心耕耘，长夜自在好眠”，随后便敞开了顶层光梯。", repeatText = "驻足天枢日晷台校验随身铭印，已为巅峰对话做好万全准备。", bonusGold = 148L, bonusXp = 192L, bonusMaterial = 3L, investigationGain = 2),
        EventTemplate("ev_ds2_zenith_resonance", RegionId.DAWNLIGHT_SPIRE, "双曜终章共鸣礼", EventKind.ENCOUNTER, 15, 2, 8, minRegionExploreHours = 22, firstDiscoveryText = "朝日光辉与星海微浪在钟塔尖端交汇成金色鸣响，击败天枢守护幻影后满载荣光凯旋。", repeatText = "伴随天枢巅峰的金色和鸣击败巡卫，带着满满自信返回营地。", bonusGold = 158L, bonusXp = 204L, bonusMaterial = 4L, investigationGain = 2)
    )

    val extraSouvenirs: List<SouvenirDef> = listOf(
        // Sunlit Athenaeum
        SouvenirDef("souv_sa_wax_seal", RegionId.SUNLIT_ATHENAEUM, "松烟火漆印", "温言常用来封装整理好手稿的黄铜徽章，印面刻着一枚翻开的书页。", Stats(maxHp = 22, attack = 4), xpBonusPercent = 4),
        SouvenirDef("souv_sa_gold_nib", RegionId.SUNLIT_ATHENAEUM, "金翎备用笔尖", "在晴岚书院风中也不会刮纸的柔韧金笔尖。", Stats(attack = 5, speed = 3), goldBonusPercent = 4),
        SouvenirDef("souv_sa_pressed_rosemary", RegionId.SUNLIT_ATHENAEUM, "押花迷迭香签", "夹在硬壳笔记本扉页的干燥香草标本，翻开书就有清香。", Stats(maxHp = 20, defense = 4), materialBonusPercent = 4),
        // Dawnlight Spire
        SouvenirDef("souv_ds2_soluna_coin", RegionId.DAWNLIGHT_SPIRE, "昼夜双面纪念币", "一面刻着二十五分刻度、另一面绘着弯月星海的沉甸甸金币。", Stats(attack = 6, defense = 4), goldBonusPercent = 5),
        SouvenirDef("souv_ds2_tuning_fork", RegionId.DAWNLIGHT_SPIRE, "天枢黄金音叉", "朝露送给律己者的礼物，轻敲时会稳定发出 432Hz 温润余音。", Stats(maxHp = 28, speed = 3), xpBonusPercent = 5),
        SouvenirDef("souv_ds2_aurora_prism", RegionId.DAWNLIGHT_SPIRE, "破晓七彩棱镜", "悬在营地窗边，能把清晨第一缕光线折成温暖彩虹的小挂饰。", Stats(maxHp = 24, attack = 5, defense = 4), materialBonusPercent = 5)
    )

    val extraMorningEchoes: List<MorningEchoDef> = listOf(
        MorningEchoDef(
            id = "echo_sa_open_book",
            regionId = RegionId.SUNLIT_ATHENAEUM,
            title = "长桌边摊开的晴岚索引册",
            prompt = "清晨收队离开晴岚书院时，风把一页记满高效书摘的羊皮纸吹到你的提灯旁。",
            choiceA = MorningEchoOutcome("装订入库换作研习经费", "直接换取丰厚旅金、经验与晴岚羊皮卷。", rewardGold = 280L, rewardXp = 220L, rewardMaterialRegion = RegionId.SUNLIT_ATHENAEUM, rewardMaterialAmount = 5L),
            choiceB = MorningEchoOutcome("临摹首段心法贴在书桌前", "下次冒险金币 +14%、经验 +12%、攻击 +7。", rewardXp = 140L, nextNightBuff = MorningEchoBuff("echo_sa_open_book", "晴岚开卷悟", goldBonusPercent = 14, xpBonusPercent = 12, combatAtkBonus = 7))
        ),
        MorningEchoDef(
            id = "echo_sa_ink_jar",
            regionId = RegionId.SUNLIT_ATHENAEUM,
            title = "阳光暖透的松烟金墨瓶",
            prompt = "石亭石板上搁着一小瓶刚沉澄透亮的备用松烟墨坯与金粉袋。",
            choiceA = MorningEchoOutcome("连瓶带回营地研习书斋", "立得大量晴岚羊皮卷与金币经验酬谢。", rewardGold = 250L, rewardXp = 240L, rewardMaterialRegion = RegionId.SUNLIT_ATHENAEUM, rewardMaterialAmount = 6L),
            choiceB = MorningEchoOutcome("蘸墨校准地图桌路线标尺", "下次出征材料 +12%、防御 +7、攻击 +6。", rewardMaterialRegion = RegionId.SUNLIT_ATHENAEUM, rewardMaterialAmount = 3L, nextNightBuff = MorningEchoBuff("echo_sa_ink_jar", "墨尺准星", materialBonusPercent = 12, combatAtkBonus = 6, combatDefBonus = 7))
        ),
        MorningEchoDef(
            id = "echo_ds2_dawn_chime",
            regionId = RegionId.DAWNLIGHT_SPIRE,
            title = "钟塔顶层的双曜晨钟余音",
            prompt = "旭日初升时，钟塔垂下一根系着破晓棱晶屑的金色报时绳结。",
            choiceA = MorningEchoOutcome("收下棱晶碎与天枢谢礼", "立刻收获高额破晓棱晶、金币与经验。", rewardGold = 340L, rewardXp = 280L, rewardMaterialRegion = RegionId.DAWNLIGHT_SPIRE, rewardMaterialAmount = 5L),
            choiceB = MorningEchoOutcome("记下双曜节拍带入今日行事", "下次出征金币 +14%、经验 +14%、材料 +12%。", rewardXp = 180L, nextNightBuff = MorningEchoBuff("echo_ds2_dawn_chime", "双曜朝暮律", goldBonusPercent = 14, xpBonusPercent = 14, materialBonusPercent = 12))
        ),
        MorningEchoDef(
            id = "echo_ds2_horizon_lens",
            regionId = RegionId.DAWNLIGHT_SPIRE,
            title = "晨昏交界棱镜片",
            prompt = "在天枢观景石凳旁发现一枚折射出白昼与星夜双重光斑的精密镜片。",
            choiceA = MorningEchoOutcome("送往工坊锻作精铸辅料", "立刻赢得高阶破晓棱晶与丰厚酬金。", rewardGold = 380L, rewardXp = 250L, rewardMaterialRegion = RegionId.DAWNLIGHT_SPIRE, rewardMaterialAmount = 6L),
            choiceB = MorningEchoOutcome("佩在领口护持下段险途", "下次冒险攻防各 +9，全收益小幅走高。", rewardMaterialRegion = RegionId.DAWNLIGHT_SPIRE, rewardMaterialAmount = 3L, nextNightBuff = MorningEchoBuff("echo_ds2_horizon_lens", "破晓明镜", goldBonusPercent = 10, xpBonusPercent = 10, combatAtkBonus = 9, combatDefBonus = 9))
        )
    )

    val teaEchoes: List<TeaEchoDef> = listOf(
        TeaEchoDef("tea_mw_pine_cookie", RegionId.MISTWOOD, "火塘边刚烤好的松子小饼", "专心一轮后从笔记本抬头，发现艾琳顺手留了两块温热松子酥和一小袋松木炭。",
            TeaEchoOutcome("趁热吃掉补充精力", "心神大振，立得金币、经验与 2 颗澄明墨晶。", rewardGold = 38L, rewardXp = 44L, rewardInk = 2L),
            TeaEchoOutcome("分装一半作晚间随行干粮", "下次出行材料 +8%、防御 +3，并附赠 1 墨晶。", rewardInk = 1L, rewardMaterialRegion = RegionId.MISTWOOD, rewardMaterialAmount = 3L, nextBuff = MorningEchoBuff("tea_mw_pine_cookie", "松饼余香", materialBonusPercent = 8, combatDefBonus = 3))),
        TeaEchoDef("tea_mw_dew_card", RegionId.MISTWOOD, "夹着三叶草书签的明信片", "一阵穿堂微风掀开草稿页，露出夹在里面的四叶草护符书签。",
            TeaEchoOutcome("兑作书斋小额津贴", "立即获得经验、雾林松木与 2 墨晶。", rewardGold = 30L, rewardXp = 52L, rewardInk = 2L, rewardMaterialRegion = RegionId.MISTWOOD, rewardMaterialAmount = 2L),
            TeaEchoOutcome("贴在行囊内侧祈愿顺遂", "下轮旅程经验 +8%、金币 +6%。", rewardXp = 28L, nextBuff = MorningEchoBuff("tea_mw_dew_card", "四叶晴风", goldBonusPercent = 6, xpBonusPercent = 8))),
        TeaEchoDef("tea_tf_sea_salt_candy", RegionId.TIDAL_FLAT, "海风小铺的薄荷海盐糖", "专注收官伸个懒腰时，桌角滚来一颗用蓝纸包好的清凉海盐薄荷糖。",
            TeaEchoOutcome("立刻含化提神并点算物资", "立即获赠金币、矿砂与 3 澄明墨晶。", rewardGold = 56L, rewardXp = 48L, rewardInk = 3L, rewardMaterialRegion = RegionId.TIDAL_FLAT, rewardMaterialAmount = 3L),
            TeaEchoOutcome("记下海盐配方备战下回", "下轮旅程攻击 +4、金币 +8%。", rewardGold = 30L, nextBuff = MorningEchoBuff("tea_tf_sea_salt_candy", "海盐清凉", goldBonusPercent = 8, combatAtkBonus = 4))),
        TeaEchoDef("tea_tf_conch_memo", RegionId.TIDAL_FLAT, "白贝壳镇纸下的手写便条", "潮声退去，你在书桌镇纸下看见自己上周随手记下的金句摘抄。",
            TeaEchoOutcome("整理归档收获灵感", "立即收获经验、金币与 3 墨晶。", rewardGold = 48L, rewardXp = 64L, rewardInk = 3L),
            TeaEchoOutcome("带着这份笃定继续探索", "下轮旅程材料 +10%、防御 +3。", rewardInk = 1L, rewardMaterialRegion = RegionId.TIDAL_FLAT, rewardMaterialAmount = 2L, nextBuff = MorningEchoBuff("tea_tf_conch_memo", "潮音笃定", materialBonusPercent = 10, combatDefBonus = 3))),
        TeaEchoDef("tea_sr_warm_cocoa", RegionId.STARLIT_RIDGE, "高地驿站的肉桂可可杯", "刚退出沉浸计时，手边瓷杯升起恰到好处的肉桂甜香，还有两枚晶矿压在杯垫下。",
            TeaEchoOutcome("趁温饮尽并收妥碎晶", "立得星辉碎晶、经验与 3 澄明墨晶。", rewardGold = 68L, rewardXp = 76L, rewardInk = 3L, rewardMaterialRegion = RegionId.STARLIT_RIDGE, rewardMaterialAmount = 3L),
            TeaEchoOutcome("暖手整备待下次远征", "下轮旅程经验 +10%、攻击 +5。", rewardGold = 36L, nextBuff = MorningEchoBuff("tea_sr_warm_cocoa", "暖杯锐意", xpBonusPercent = 10, combatAtkBonus = 5))),
        TeaEchoDef("tea_sr_quartz_pen", RegionId.STARLIT_RIDGE, "六棱晶镇尺旁的新墨痕", "窗外日光落在一支晶柄蘸水笔上，刚好能顺畅续完今天的收尾总结。",
            TeaEchoOutcome("润色今日总结领赏", "收获 4 枚澄明墨晶与可观经验。", rewardGold = 72L, rewardXp = 82L, rewardInk = 4L),
            TeaEchoOutcome("将这份明晰带入夜巡", "下轮旅程金币 +8%、材料 +8%、防御 +4。", rewardInk = 2L, nextBuff = MorningEchoBuff("tea_sr_quartz_pen", "晶笔明晰", goldBonusPercent = 8, materialBonusPercent = 8, combatDefBonus = 4))),
        TeaEchoDef("tea_cs_matcha_tart", RegionId.CLOUD_SANCTUARY, "温室花房送来的云朵茶点", "琳端来一盘刚蒸好的白芸豆云朵糕，盘底藏着一把细腻梦绒丝。",
            TeaEchoOutcome("安心享用这份茶歇礼", "获得金币、云栖灵绒与 4 澄明墨晶。", rewardGold = 94L, rewardXp = 96L, rewardInk = 4L, rewardMaterialRegion = RegionId.CLOUD_SANCTUARY, rewardMaterialAmount = 3L),
            TeaEchoOutcome("封好香袋作为下次护符", "下轮旅程金币 +10%、防御 +5。", rewardXp = 60L, nextBuff = MorningEchoBuff("tea_cs_matcha_tart", "云糕温润", goldBonusPercent = 10, combatDefBonus = 5))),
        TeaEchoDef("tea_cs_breeze_score", RegionId.CLOUD_SANCTUARY, "浮岛风琴的午后轻和弦", "风琴石柱吹过一段温柔的降落音阶，正好替这段高强度专研按下歇息休止符。",
            TeaEchoOutcome("收录乐谱摘抄进图鉴", "直接赢得 4 澄明墨晶与经验金币。", rewardGold = 104L, rewardXp = 112L, rewardInk = 4L),
            TeaEchoOutcome("以轻盈步频开启下一程", "下轮旅程经验 +10%、攻击 +6。", rewardInk = 2L, rewardMaterialRegion = RegionId.CLOUD_SANCTUARY, rewardMaterialAmount = 2L, nextBuff = MorningEchoBuff("tea_cs_breeze_score", "轻风和弦", xpBonusPercent = 10, combatAtkBonus = 6))),
        TeaEchoDef("tea_ds_blue_ink", RegionId.DREAM_SEA, "汐留下的海蓝水彩墨水", "茶歇抬头时，桌面多了半瓶闪烁微光的海蓝颜料与几颗圆润梦珠。",
            TeaEchoOutcome("存入书斋颜料柜", "立刻得到沉星梦珠、金币与 5 墨晶。", rewardGold = 128L, rewardXp = 124L, rewardInk = 5L, rewardMaterialRegion = RegionId.DREAM_SEA, rewardMaterialAmount = 3L),
            TeaEchoOutcome("顺手点亮夜航蓝灯", "下轮旅程材料 +12%、攻击 +6。", rewardXp = 84L, nextBuff = MorningEchoBuff("tea_ds_blue_ink", "深海蓝墨", materialBonusPercent = 12, combatAtkBonus = 6))),
        TeaEchoDef("tea_ds_wave_sketch", RegionId.DREAM_SEA, "被潮水打湿却更清晰的草稿", "那张写到一半的构想草稿经潮声灵感一激，关键难关豁然开朗。",
            TeaEchoOutcome("立刻把构想落地入账", "收获 5 墨晶与丰厚经验金币。", rewardGold = 140L, rewardXp = 148L, rewardInk = 5L),
            TeaEchoOutcome("留作下轮探险强力推演", "下轮旅程经验 +12%、金币 +8%、防御 +5。", rewardInk = 2L, nextBuff = MorningEchoBuff("tea_ds_wave_sketch", "浪纹灵光", goldBonusPercent = 8, xpBonusPercent = 12, combatDefBonus = 5))),
        TeaEchoDef("tea_aa_amber_sand", RegionId.ASTRAL_ARCH, "停顿在恰好刻度的沙漏", "砂粒刚好停在最后一片金色刻线上，令人忍不住会心一笑。",
            TeaEchoOutcome("收集星拱琥珀与墨晶", "立刻得到 6 澄明墨晶与星拱琥珀。", rewardGold = 165L, rewardXp = 160L, rewardInk = 6L, rewardMaterialRegion = RegionId.ASTRAL_ARCH, rewardMaterialAmount = 3L),
            TeaEchoOutcome("将这份从容刻进下次出发", "下轮旅程金币 +12%、经验 +10%、攻击 +7。", rewardInk = 3L, nextBuff = MorningEchoBuff("tea_aa_amber_sand", "砂漏从容", goldBonusPercent = 12, xpBonusPercent = 10, combatAtkBonus = 7))),
        TeaEchoDef("tea_aa_bell_espresso", RegionId.ASTRAL_ARCH, "岑手冲的微甜琥珀特调", "岑把一小盅带着柑橘皮香的热饮推过来：“辛苦了，喘口气再走也不迟。”",
            TeaEchoOutcome("欣然饮下收获干劲", "收获 6 墨晶与高额旅金经验。", rewardGold = 180L, rewardXp = 175L, rewardInk = 6L),
            TeaEchoOutcome("借余韵稳固后续防线", "下轮旅程材料 +10%、防御 +7、攻击 +5。", rewardMaterialRegion = RegionId.ASTRAL_ARCH, rewardMaterialAmount = 3L, nextBuff = MorningEchoBuff("tea_aa_bell_espresso", "琥珀醇香", materialBonusPercent = 10, combatAtkBonus = 5, combatDefBonus = 7))),
        TeaEchoDef("tea_sa_quill_gift", RegionId.SUNLIT_ATHENAEUM, "温言代抄完的参考书目页", "桌面多了一张字迹清雅的整理卡片，帮你在迷茫处省下了整整半天弯路。",
            TeaEchoOutcome("归入研习书斋收藏", "立得 7 澄明墨晶与晴岚羊皮卷。", rewardGold = 210L, rewardXp = 210L, rewardInk = 7L, rewardMaterialRegion = RegionId.SUNLIT_ATHENAEUM, rewardMaterialAmount = 4L),
            TeaEchoOutcome("贴在行装边加快下轮效率", "下轮旅程经验 +14%、金币 +10%、攻击 +8。", rewardInk = 3L, nextBuff = MorningEchoBuff("tea_sa_quill_gift", "晴岚批注", goldBonusPercent = 10, xpBonusPercent = 14, combatAtkBonus = 8))),
        TeaEchoDef("tea_sa_cat_stretch", RegionId.SUNLIT_ATHENAEUM, "伸懒腰拱背的踏墨书猫", "小猫醒来伸了个大大的懒腰，爪底轻轻按出一枚淡金色猫爪墨印。",
            TeaEchoOutcome("盖进手账兑换心意赏金", "立刻获赠 7 墨晶与丰厚旅途资源。", rewardGold = 230L, rewardXp = 195L, rewardInk = 7L),
            TeaEchoOutcome("让好运陪你开启下一局", "下轮旅程材料 +12%、金币 +10%、防御 +7。", rewardMaterialRegion = RegionId.SUNLIT_ATHENAEUM, rewardMaterialAmount = 3L, nextBuff = MorningEchoBuff("tea_sa_cat_stretch", "猫爪墨印", goldBonusPercent = 10, materialBonusPercent = 12, combatDefBonus = 7))),
        TeaEchoDef("tea_ds2_fork_echo", RegionId.DAWNLIGHT_SPIRE, "朝露轻敲的天枢音叉共鸣", "茶歇时听见一声清澄如晨露的金属回响，眼皮和肩膀同时轻快下来。",
            TeaEchoOutcome("封存这段金辉频率入库", "收获 8 澄明墨晶与破晓棱晶。", rewardGold = 260L, rewardXp = 240L, rewardInk = 8L, rewardMaterialRegion = RegionId.DAWNLIGHT_SPIRE, rewardMaterialAmount = 4L),
            TeaEchoOutcome("以黄金节奏开启下段行程", "下轮旅程经验 +15%、金币 +12%、攻防各 +8。", rewardInk = 4L, nextBuff = MorningEchoBuff("tea_ds2_fork_echo", "音叉黄金律", goldBonusPercent = 12, xpBonusPercent = 15, combatAtkBonus = 8, combatDefBonus = 8))),
        TeaEchoDef("tea_ds2_soluna_toast", RegionId.DAWNLIGHT_SPIRE, "昼夜交界的金箔书签卡", "一张双面金箔卡片静静夹在茶盘边，祝贺你稳稳拿下这一段清醒投入。",
            TeaEchoOutcome("兑换为旅途与研习双倍补给", "立得高额金币、经验与 8 枚澄明墨晶。", rewardGold = 290L, rewardXp = 260L, rewardInk = 8L),
            TeaEchoOutcome("佩在手腕迎向更高挑战", "下轮旅程全收益 +12%、攻击 +9、防御 +7。", rewardMaterialRegion = RegionId.DAWNLIGHT_SPIRE, rewardMaterialAmount = 3L, nextBuff = MorningEchoBuff("tea_ds2_soluna_toast", "双曜祝福", goldBonusPercent = 12, xpBonusPercent = 12, materialBonusPercent = 12, combatAtkBonus = 9, combatDefBonus = 7)))
    )

    val extraChronicles: List<ChronicleQuestDef> = listOf(
        ChronicleQuestDef(
            id = "chron_sunlit_athenaeum",
            regionId = RegionId.SUNLIT_ATHENAEUM,
            title = "晴岚手抄百卷目录",
            description = "协助温言理清云顶露天书院遗落的编目索引，唤醒司卷星灵守护的清醒传承。",
            stages = listOf(
                ChronicleStageDef(1, "序章 · 晴岚长桌索引", 3, 4, "依据晴岚长廊抄本核清东翼与南翼手稿馆的目录编号。"),
                ChronicleStageDef(2, "二章 · 藏书滑梯暗格", 6, 12, "沿黄铜滑梯登上百尺高层书阁，收回失落多年的修习手札。"),
                ChronicleStageDef(3, "终章 · 晴岚大典合璧", 9, 20, "日晷穹顶秘馆金光照彻每卷羊皮古书，求索者的每分专研都有回响。")
            ),
            rewardStats = Stats(maxHp = 36, attack = 10, speed = 4),
            rewardXpBonusPercent = 8,
            completionTitle = "晴岚手抄目录成卷：永久生命 +36，攻击 +10，速度 +4，全境经验 +8%"
        ),
        ChronicleQuestDef(
            id = "chron_dawnlight_spire",
            regionId = RegionId.DAWNLIGHT_SPIRE,
            title = "双曜昼夜校准总典",
            description = "与朝露一起登上黄金天文钟塔，将白昼专注与夜间安眠的齿轮重新合榫。",
            stages = listOf(
                ChronicleStageDef(1, "序章 · 破晓刻痕尺", 3, 4, "沿着晨昏光带台阶记下第一组昼夜交界基准刻度。"),
                ChronicleStageDef(2, "二章 · 钟摆共振校准", 6, 12, "在黄金钟摆廊将昼轮与夜轮对准黄金比例，钟塔重归和谐。"),
                ChronicleStageDef(3, "终章 · 双曜恒流之约", 9, 22, "天枢之巅同时映亮晨光与银河，昼勤夜安的旅途从此生生不息。")
            ),
            rewardStats = Stats(maxHp = 50, attack = 12, defense = 8, speed = 5),
            rewardGoldBonusPercent = 8,
            rewardXpBonusPercent = 8,
            rewardMaterialBonusPercent = 8,
            completionTitle = "双曜校准总典圆满：永久大幅增强全属性，全收益 +8%"
        )
    )

    val extraAstralNodes: List<AstralNodeDef> = listOf(
        AstralNodeDef("astral_bastion_4", BuildArchetype.BASTION, 4, "双曜不动磐", "永久生命 +62，防护 +12，战斗伤害 +8%。", Stats(maxHp = 62, defense = 12), combatDamagePercent = 8),
        AstralNodeDef("astral_gale_4", BuildArchetype.GALE, 4, "破晓流星矢", "永久攻击 +14，速度 +7，战斗伤害 +10%。", Stats(attack = 14, speed = 7), combatDamagePercent = 10),
        AstralNodeDef("astral_dream_4", BuildArchetype.DREAMWEAVE, 4, "晨昏常青园", "永久生命 +52，金币 +10%，经验 +10%，材料 +8%。", Stats(maxHp = 52), goldBonusPercent = 10, xpBonusPercent = 10, materialBonusPercent = 8),
        AstralNodeDef("astral_scholar_4", BuildArchetype.SCHOLAR, 4, "天枢万卷宗", "永久攻击 +12，防护 +9，经验 +10%，战斗伤害 +8%。", Stats(attack = 12, defense = 9), xpBonusPercent = 10, combatDamagePercent = 8)
    )

    val extraTransmuteRecipes: List<TransmuteRecipeDef> = listOf(
        TransmuteRecipeDef(
            id = "tx_amber_to_vellum",
            title = "琥珀熏制晴岚羊皮卷",
            description = "花房 Lv.5 启用：将 16 份星拱琥珀配以温和松油熟化为 10 份晴岚羊皮卷。",
            requiredConservatoryLevel = 5,
            inputGold = 680L,
            inputMaterialId = RegionId.ASTRAL_ARCH.materialId,
            inputMaterialAmount = 16L,
            outputMaterialId = RegionId.SUNLIT_ATHENAEUM.materialId,
            outputMaterialAmount = 10L
        ),
        TransmuteRecipeDef(
            id = "tx_vellum_to_prism",
            title = "晴岚卷淬破晓棱晶",
            description = "花房 Lv.5 启用：在日光折射架下以 16 份晴岚羊皮卷精炼出 10 份破晓棱晶。",
            requiredConservatoryLevel = 5,
            inputGold = 860L,
            inputMaterialId = RegionId.SUNLIT_ATHENAEUM.materialId,
            inputMaterialAmount = 16L,
            outputMaterialId = RegionId.DAWNLIGHT_SPIRE.materialId,
            outputMaterialAmount = 10L
        ),
        TransmuteRecipeDef(
            id = "tx_mistwood_to_ink",
            title = "松烟凝萃澄明墨晶",
            description = "花房 Lv.1 启用：以 20 份雾林松木提炼松烟，凝成 12 枚澄明墨晶，方便纯安眠旅人建设书斋。",
            requiredConservatoryLevel = 1,
            inputGold = 80L,
            inputMaterialId = RegionId.MISTWOOD.materialId,
            inputMaterialAmount = 20L,
            outputInspirationInk = 12L
        )
    )

    val extraCommissions: List<CommissionDef> = listOf(
        CommissionDef(
            id = "comm_mistwood_quick_notes",
            title = "晨间廿五分速记协理",
            regionId = RegionId.MISTWOOD,
            description = "于雾林单次专注完成至少 5 个节点（25 分钟标准番茄钟即可达成），带回松枝样片与墨晶酬谢。",
            requiredMinNodes = 5,
            rewardGold = 45L,
            rewardXp = 65L,
            rewardMaterialRegion = RegionId.MISTWOOD,
            rewardMaterialAmount = 4L,
            rewardAffinity = 1,
            focusOnly = true
        ),
        CommissionDef(
            id = "comm_tidal_focus_notes",
            title = "潮音五十刻专研委托",
            regionId = RegionId.TIDAL_FLAT,
            description = "于潮汐滩专注完成至少 10 个节点（50 分钟双番茄专研），校验旧港日间潮表。",
            requiredMinNodes = 10,
            rewardGold = 120L,
            rewardXp = 160L,
            rewardMaterialRegion = RegionId.TIDAL_FLAT,
            rewardMaterialAmount = 6L,
            rewardAffinity = 2,
            focusOnly = true
        ),
        CommissionDef(
            id = "comm_athenaeum_indexing",
            title = "晴岚藏书阁编目赏格",
            regionId = RegionId.SUNLIT_ATHENAEUM,
            description = "于晴岚书院稳定完成至少 24 个节点（2 小时专注或夜探），替温言带回整批羊皮典籍索引。",
            requiredMinNodes = 24,
            rewardGold = 1400L,
            rewardXp = 1680L,
            rewardMaterialRegion = RegionId.SUNLIT_ATHENAEUM,
            rewardMaterialAmount = 16L,
            rewardBossMarks = 1L,
            rewardAffinity = 3
        ),
        CommissionDef(
            id = "comm_spire_calibration",
            title = "天枢双曜摆锤校时单",
            regionId = RegionId.DAWNLIGHT_SPIRE,
            description = "于晨曦钟塔稳定完成至少 24 个节点，与朝露共同校准破晓棱晶摆钟。",
            requiredMinNodes = 24,
            rewardGold = 1850L,
            rewardXp = 2200L,
            rewardMaterialRegion = RegionId.DAWNLIGHT_SPIRE,
            rewardMaterialAmount = 18L,
            rewardBossMarks = 1L,
            rewardAffinity = 3
        )
    )
}
