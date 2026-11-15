package com.dreamtrail.core.content

import com.dreamtrail.core.model.*

/**
 * Versioned game content catalog for DreamTrail v4.
 * All names, stories, items, and enemies are original gentle-fantasy travelogue content.
 */
object ContentCatalog {
    const val CONTENT_VERSION = 4
    const val RULES_VERSION = 4

    val passives: List<PassiveDef> = listOf(
        PassiveDef(
            id = "passive_forest_ward",
            name = "林木加护",
            description = "每次战斗开局获得 12 点护盾；在雾林探索时更易带回完整木料。",
            unlockHint = "初始自带",
            archetype = BuildArchetype.BASTION
        ),
        PassiveDef(
            id = "passive_tidal_stride",
            name = "踏浪步法",
            description = "速度 +3；在多雨潮湿路线中防护 +4。",
            unlockHint = "前往潮汐滩或修复旅舍 Lv.1 解锁",
            archetype = BuildArchetype.GALE
        ),
        PassiveDef(
            id = "passive_keen_eye",
            name = "旅者锐目",
            description = "线索事件出现权重 +25%，更快推进区域调查与图鉴。",
            unlockHint = "解锁 2 条路线或完成艾琳第 1 章故事",
            archetype = BuildArchetype.SCHOLAR
        ),
        PassiveDef(
            id = "passive_artisan_touch",
            name = "巧匠心法",
            description = "基础攻击 +3；工坊打造与强化消耗金币减少 10%。",
            unlockHint = "将工坊升至 Lv.2 解锁",
            archetype = BuildArchetype.SCHOLAR
        ),
        PassiveDef(
            id = "passive_ember_resolve",
            name = "炉火余温",
            description = "战斗开局额外获得 8 点护盾，且旅程基础经验产出 +8%。",
            unlockHint = "旅舍升至 Lv.2 或完成诺亚第 1 章故事",
            archetype = BuildArchetype.DREAMWEAVE
        ),
        PassiveDef(
            id = "passive_star_whisper",
            name = "星尘低语",
            description = "词条与被动带来的开局护盾 +15%，面对高地清寒敌人与区域首领时攻击提升 12%。",
            unlockHint = "踏足星陨山脊或完成诺亚第 2 章故事",
            archetype = BuildArchetype.BASTION
        ),
        PassiveDef(
            id = "passive_dream_weaver",
            name = "织梦呼吸",
            description = "休整节点额外获得 4 金币与 5 经验；全旅程基础经验产出提升 6%。",
            unlockHint = "踏足云栖浮岛或完成琳第 1 章故事",
            archetype = BuildArchetype.DREAMWEAVE
        ),
        PassiveDef(
            id = "passive_cloud_aegis",
            name = "云幕庇护",
            description = "生命上限 +20，每次战斗开局获得 10 点护盾；在云海温润路线防护 +5。",
            unlockHint = "旅舍升至 Lv.3 或完成琳第 2 章故事",
            archetype = BuildArchetype.BASTION
        ),
        PassiveDef(
            id = "passive_thorns_oath",
            name = "疾风游刃",
            description = "速度 +4；触发连击时，追击伤害额外计入 10% 攻击力。",
            unlockHint = "工坊升至 Lv.3 或茶歇火塘升至 Lv.2 解锁",
            archetype = BuildArchetype.GALE
        ),
        PassiveDef(
            id = "passive_astral_brush",
            name = "星海描边",
            description = "休整事件额外获得 3 金币与 4 经验；沉星梦海基础金币 +8%，装备寻获进度 +8%。",
            unlockHint = "踏足沉星梦海或完成汐第 1 章故事",
            archetype = BuildArchetype.DREAMWEAVE
        ),
        PassiveDef(
            id = "passive_amber_anchor",
            name = "星珀定心",
            description = "战斗开局额外获得 14 点护盾；面对归梦星拱生灵时攻击伤害 +10%。",
            unlockHint = "踏足归梦星拱或星尘花房升至 Lv.2 解锁",
            archetype = BuildArchetype.BASTION
        ),
        PassiveDef(
            id = "passive_chrono_step",
            name = "时隙盈步",
            description = "速度 +5；每第 4 回合额外斩出一道 30% 攻击力的穿梭伤。",
            unlockHint = "角色达到 Lv.25 或工坊升至 Lv.4 解锁",
            archetype = BuildArchetype.GALE
        ),
        PassiveDef(
            id = "passive_herbarium_bloom",
            name = "灵植微露",
            description = "旅程基础材料产出 +8%。",
            unlockHint = "星尘花房升至 Lv.3 或完成岑第 1 章故事解锁",
            archetype = BuildArchetype.DREAMWEAVE
        ),
        PassiveDef(
            id = "passive_bestiary_scholar",
            name = "博物考据",
            description = "魔物图鉴熟识增伤提升至 +16%，线索发现权重 +18%。",
            unlockHint = "图鉴收录至少 24 种魔物或完成岑第 2 章故事解锁",
            archetype = BuildArchetype.SCHOLAR
        )
    )

    val enemies: List<EnemyDef> = listOf(
        // Mistwood (6 normal + 1 boss)
        EnemyDef("enemy_mist_slime", "苔露史莱姆", RegionId.MISTWOOD, false, Stats(36, 9, 4, 7), 1.0, 0, "裹着青苔与露水的林间软体生物，性格温吞。"),
        EnemyDef("enemy_bark_beetle", "树皮甲虫", RegionId.MISTWOOD, false, Stats(42, 10, 7, 6), 1.0, 6, "背壳像老松皮一样粗糙，受惊时会缩起外壳。"),
        EnemyDef("enemy_spore_fox", "孢子小狐", RegionId.MISTWOOD, false, Stats(38, 12, 4, 11), 1.05, 0, "尾巴沾着微光菌粉，总在雾气浓时蹿过林道。"),
        EnemyDef("enemy_vine_hound", "蔓生猎犬", RegionId.MISTWOOD, false, Stats(50, 13, 6, 10), 1.1, 4, "被野藤缠住护肩的林地野兽，动作敏捷。"),
        EnemyDef("enemy_lantern_moth", "提灯大蛾", RegionId.MISTWOOD, false, Stats(44, 14, 5, 12), 1.1, 5, "翅膀斑纹像纸灯笼，常绕着旅人的油灯打转。"),
        EnemyDef("enemy_hollow_sentinel", "空心树卫", RegionId.MISTWOOD, false, Stats(64, 14, 9, 7), 1.15, 10, "守在古树回廊旁的枯木偶，行动缓慢却很耐打。"),
        EnemyDef("boss_mistwood_stag", "苔角林鹿", RegionId.MISTWOOD, true, Stats(128, 18, 11, 11), 1.25, 18, "雾林深处的古老守护兽，鹿角上开着白色的夜花。", bossTrait = BossTrait.MIST_VEIL),

        // Tidal Flat (6 normal + 1 boss)
        EnemyDef("enemy_salt_crab", "盐壳巨蟹", RegionId.TIDAL_FLAT, false, Stats(66, 16, 12, 8), 1.05, 10, "背着风化礁石的浅滩蟹，钳击沉稳。"),
        EnemyDef("enemy_tide_eel", "银潮海鳗", RegionId.TIDAL_FLAT, false, Stats(58, 19, 8, 14), 1.1, 0, "退潮时留在浅水洼里的发光鳗鱼，速度极快。"),
        EnemyDef("enemy_drift_wisp", "漂流海灵", RegionId.TIDAL_FLAT, false, Stats(62, 18, 10, 12), 1.15, 8, "聚在沉船桅杆旁的微光雾团，带着咸湿的潮气。"),
        EnemyDef("enemy_barnacle_golem", "藤壶石偶", RegionId.TIDAL_FLAT, false, Stats(82, 18, 15, 7), 1.15, 14, "由礁石与贝壳长期凝结而成的海岸偶人。"),
        EnemyDef("enemy_gull_raider", "风暴海鸥", RegionId.TIDAL_FLAT, false, Stats(64, 21, 9, 15), 1.15, 4, "习惯趁海风俯冲叼走亮晶晶饰品的海岸猛禽。"),
        EnemyDef("enemy_sunken_knight", "锈甲水手", RegionId.TIDAL_FLAT, false, Stats(88, 22, 14, 10), 1.2, 12, "穿戴着老式铜甲的沉船守望影，依然守着船钟。"),
        EnemyDef("boss_tidal_warden", "礁钟守望者", RegionId.TIDAL_FLAT, true, Stats(195, 25, 16, 12), 1.3, 28, "旧灯塔下的巨大潮汐机偶，潮声敲响时便会升起水盾。", bossTrait = BossTrait.TIDE_SURGE),

        // Starlit Ridge (6 normal + 1 boss)
        EnemyDef("enemy_frost_marmot", "霜毛岩獭", RegionId.STARLIT_RIDGE, false, Stats(92, 24, 14, 13), 1.1, 8, "披着厚实白毛的山脊小兽，习惯把暖石藏在巢里。"),
        EnemyDef("enemy_crystal_hawk", "晶羽游隼", RegionId.STARLIT_RIDGE, false, Stats(86, 28, 12, 18), 1.15, 6, "羽梢泛着星辉碎光的高山猛禽，先声夺人。"),
        EnemyDef("enemy_meteor_shard", "落星碎灵", RegionId.STARLIT_RIDGE, false, Stats(102, 26, 17, 13), 1.2, 16, "围绕坠落陨石聚拢的微光晶簇。"),
        EnemyDef("enemy_ridge_ram", "寒角岩羊", RegionId.STARLIT_RIDGE, false, Stats(114, 27, 18, 12), 1.2, 14, "在峭壁间稳健奔跑的岩羊，冲撞力道很足。"),
        EnemyDef("enemy_echo_monk", "星台行者", RegionId.STARLIT_RIDGE, false, Stats(108, 30, 15, 16), 1.25, 12, "曾为旧天文台守夜的静默幻影。"),
        EnemyDef("enemy_obsidian_hound", "黑曜哨犬", RegionId.STARLIT_RIDGE, false, Stats(118, 31, 17, 15), 1.25, 15, "星尘矿脉旁巡逻的石雕猎犬。"),
        EnemyDef("boss_starlit_automaton", "陨星古偶", RegionId.STARLIT_RIDGE, true, Stats(275, 34, 22, 15), 1.35, 38, "废墟星台中央的观测核心，随星象流转切换攻防。", bossTrait = BossTrait.STAR_JUDGMENT),

        // Cloud Sanctuary (6 normal + 1 boss)
        EnemyDef("enemy_cloud_puff", "软云绒兽", RegionId.CLOUD_SANCTUARY, false, Stats(126, 32, 18, 16), 1.15, 12, "像蓬松云朵一样漂浮在浮岛边缘的小兽，受惊时会卷起气流。"),
        EnemyDef("enemy_dew_sprite", "露盏花灵", RegionId.CLOUD_SANCTUARY, false, Stats(118, 35, 16, 20), 1.2, 10, "栖息在梦绒花瓣上的微光精灵，身法轻快。"),
        EnemyDef("enemy_wind_harpy", "风琴隼翼", RegionId.CLOUD_SANCTUARY, false, Stats(134, 37, 19, 22), 1.25, 14, "掠过浮岛风琴石柱的羽翼旅鸟，鸣声清越。"),
        EnemyDef("enemy_glass_gardener", "琉璃花匠", RegionId.CLOUD_SANCTUARY, false, Stats(150, 36, 23, 15), 1.25, 20, "守在古温室玻璃廊道里的发条园丁偶，擅长防御。"),
        EnemyDef("enemy_mirage_manta", "云海鳐影", RegionId.CLOUD_SANCTUARY, false, Stats(142, 39, 20, 19), 1.3, 18, "顺着高空暖流缓缓滑翔的半透明云鳐。"),
        EnemyDef("enemy_sanctuary_guard", "栖云守卫", RegionId.CLOUD_SANCTUARY, false, Stats(164, 41, 24, 17), 1.3, 24, "护卫云巅尖塔的古老石甲行者，力道沉稳。"),
        EnemyDef("boss_cloud_leviathan", "云鲸守梦者", RegionId.CLOUD_SANCTUARY, true, Stats(360, 44, 28, 19), 1.4, 48, "游弋在云栖浮岛群最上方的古老云海巨鲸，背脊载着整座温室的星辉。", bossTrait = BossTrait.SKY_GALE),

        // Dream Sea (6 normal + 1 boss)
        EnemyDef("enemy_starlight_jelly", "浮星水母", RegionId.DREAM_SEA, false, Stats(178, 44, 24, 21), 1.25, 16, "在沉星海浅滩漂浮的半透明夜光水母，触须间流转着星尘。"),
        EnemyDef("enemy_coral_crab", "梦礁寄居蟹", RegionId.DREAM_SEA, false, Stats(196, 46, 29, 17), 1.25, 24, "背着古老沉船铜铃的蓝晶寄居蟹，外壳坚硬却不主动伤人。"),
        EnemyDef("enemy_prism_ray", "棱镜鳐鱼", RegionId.DREAM_SEA, false, Stats(188, 49, 25, 24), 1.3, 18, "滑翔在梦海镜面水纹上的彩光鳐，尾鳍能折射夜空星轨。"),
        EnemyDef("enemy_lantern_urchin", "汐灯海胆", RegionId.DREAM_SEA, false, Stats(212, 48, 31, 16), 1.3, 28, "表面长满柔和荧光骨刺的慢行海胆，惊扰时会展开刺环。"),
        EnemyDef("enemy_drift_mariner", "梦航水手影", RegionId.DREAM_SEA, false, Stats(206, 51, 27, 22), 1.35, 22, "巡视沉星水道的古代领航幻影，提着一盏熄不灭的蓝灯。"),
        EnemyDef("enemy_abyss_warden", "沉星守潮偶", RegionId.DREAM_SEA, false, Stats(228, 53, 32, 19), 1.35, 30, "用海晶与沉木拼接的海岸机偶，专司守护旧灯塔航道。"),
        EnemyDef("boss_dream_lighthouse_whale", "司灯渊鲸", RegionId.DREAM_SEA, true, Stats(465, 56, 35, 22), 1.45, 58, "背负沉星海整座灯塔遗迹的深梦巨鲸，每逢潮汐转折便会点亮归航长灯。", bossTrait = BossTrait.DREAM_TIDE_BARRIER),

        // Astral Arch (6 normal + 1 boss)
        EnemyDef("enemy_amber_moth", "星珀夜蛾", RegionId.ASTRAL_ARCH, false, Stats(230, 54, 30, 26), 1.25, 20, "翅膀镶嵌着半透明琥珀鳞粉的星穹巨蛾，在晨昏回廊间安静盘旋。"),
        EnemyDef("enemy_chrono_tortoise", "时隙砂龟", RegionId.ASTRAL_ARCH, false, Stats(258, 56, 36, 18), 1.30, 32, "背壳刻有逆行时刻表盘的古老石龟，沙粒总从它甲缝缓缓上浮。"),
        EnemyDef("enemy_twilight_lynx", "暮光灵猞", RegionId.ASTRAL_ARCH, false, Stats(242, 60, 31, 29), 1.35, 22, "踩在永恒暮色光带上的星眸猞猁，行动快若微风掠影。"),
        EnemyDef("enemy_bell_golem", "古钟偶卫", RegionId.ASTRAL_ARCH, false, Stats(278, 59, 38, 20), 1.35, 36, "守护星拱钟楼的青铜构装体，胸腔中回荡着柔和的整点余音。"),
        EnemyDef("enemy_halo_stag", "穹环白鹿影", RegionId.ASTRAL_ARCH, false, Stats(266, 62, 34, 26), 1.40, 28, "角冠浮现微光星环的虚影白鹿，仿佛雾林古老守望者的遥远回响。"),
        EnemyDef("enemy_arch_chronicler", "星拱记事偶", RegionId.ASTRAL_ARCH, false, Stats(292, 65, 40, 24), 1.40, 38, "执掌无尽手札铜盘的高阶星穹机关偶，试炼每一位踏入终拱的夜行人。"),
        EnemyDef("boss_astral_chronokeeper", "归梦时隙钟灵", RegionId.ASTRAL_ARCH, true, Stats(610, 68, 43, 26), 1.50, 74, "栖身归梦星拱至高日晷圣殿的古老守时之灵，以逆时星砂护持千夜安眠的边界。", bossTrait = BossTrait.CHRONO_RESONANCE)
    )

    val routes: List<RouteDef> = listOf(
        // Mistwood
        RouteDef(
            id = "route_mistwood_outer",
            regionId = RegionId.MISTWOOD,
            name = "薄雾外径",
            subtitle = "沿着灯笼花指引的缓坡穿行，雾气柔和",
            dangerTag = "木材较多 · 温和安全",
            environmentTag = EnvironmentTag.FOREST,
            recommendationHint = "最适合首夜出发或轻装放松，稳健与搜集皆宜",
            recommendedPowerTier = 1,
            baseGoldPerHour = 80L,
            baseXpPerHour = 100L,
            baseMaterialPerHour = 6L,
            dangerRate = 0.14,
            enemyIds = listOf("enemy_mist_slime", "enemy_bark_beetle", "enemy_spore_fox"),
            bossId = "boss_mistwood_stag",
            bossInvestigationRequired = 4,
            requiredExploreHours = 0,
            unlockDescription = "初始默认路线"
        ),
        RouteDef(
            id = "route_mistwood_lantern",
            regionId = RegionId.MISTWOOD,
            name = "灯笼花沼泽",
            subtitle = "菌光点亮的浅洼小路，常能捡到旧信物",
            dangerTag = "线索较多 · 林间薄雾",
            environmentTag = EnvironmentTag.FOREST,
            recommendationHint = "适合携带探索策略收集事件线索与护符词条",
            recommendedPowerTier = 1,
            baseGoldPerHour = 94L,
            baseXpPerHour = 115L,
            baseMaterialPerHour = 7L,
            dangerRate = 0.22,
            enemyIds = listOf("enemy_spore_fox", "enemy_vine_hound", "enemy_lantern_moth"),
            bossId = "boss_mistwood_stag",
            bossInvestigationRequired = 5,
            requiredExploreHours = 8,
            unlockDescription = "累计有效探索 8 小时，雾林调查度 3"
        ),
        RouteDef(
            id = "route_mistwood_heart",
            regionId = RegionId.MISTWOOD,
            name = "古树回廊",
            subtitle = "环抱千年巨木的石阶，苔角林鹿常在此现身",
            dangerTag = "首领出没 · 适合 1 阶成型",
            environmentTag = EnvironmentTag.FOREST,
            recommendationHint = "建议强化防具后前往，可稳定积累雾林调查度",
            recommendedPowerTier = 1,
            baseGoldPerHour = 110L,
            baseXpPerHour = 132L,
            baseMaterialPerHour = 8L,
            dangerRate = 0.28,
            enemyIds = listOf("enemy_vine_hound", "enemy_lantern_moth", "enemy_hollow_sentinel"),
            bossId = "boss_mistwood_stag",
            bossInvestigationRequired = 6,
            requiredExploreHours = 20,
            unlockDescription = "累计有效探索 20 小时，雾林调查度 7"
        ),
        // Tidal Flat
        RouteDef(
            id = "route_tidal_shallows",
            regionId = RegionId.TIDAL_FLAT,
            name = "银白浅滩",
            subtitle = "退潮后显现的细沙浅滩，贝壳与矿砂闪烁微光",
            dangerTag = "多雨潮湿 · 矿砂丰富",
            environmentTag = EnvironmentTag.DAMP,
            recommendationHint = "多雨环境，携带防具与“御雨壁垒”或盾系构筑更稳妥",
            recommendedPowerTier = 2,
            baseGoldPerHour = 124L,
            baseXpPerHour = 148L,
            baseMaterialPerHour = 7L,
            dangerRate = 0.24,
            enemyIds = listOf("enemy_salt_crab", "enemy_tide_eel", "enemy_drift_wisp"),
            bossId = "boss_tidal_warden",
            bossInvestigationRequired = 5,
            requiredExploreHours = 32,
            unlockDescription = "累计有效探索 32 小时，雾林调查度 6"
        ),
        RouteDef(
            id = "route_tidal_wreck",
            regionId = RegionId.TIDAL_FLAT,
            name = "搁浅船骨",
            subtitle = "旧商船留下的木骨架，海风吹过仍有铜铃声",
            dangerTag = "多雨潮湿 · 装备线索",
            environmentTag = EnvironmentTag.DAMP,
            recommendationHint = "敌人防护较高，护盾猛击与破甲搭配收益明显",
            recommendedPowerTier = 2,
            baseGoldPerHour = 140L,
            baseXpPerHour = 166L,
            baseMaterialPerHour = 8L,
            dangerRate = 0.30,
            enemyIds = listOf("enemy_drift_wisp", "enemy_barnacle_golem", "enemy_gull_raider"),
            bossId = "boss_tidal_warden",
            bossInvestigationRequired = 6,
            requiredExploreHours = 52,
            unlockDescription = "累计有效探索 52 小时，潮汐滩调查度 4"
        ),
        RouteDef(
            id = "route_tidal_lighthouse",
            regionId = RegionId.TIDAL_FLAT,
            name = "旧灯塔礁石",
            subtitle = "潮汐尽头的石砌灯塔，塔顶风灯尚未完全熄灭",
            dangerTag = "多雨潮湿 · 首领海域",
            environmentTag = EnvironmentTag.DAMP,
            recommendationHint = "2 阶装备成型后挑战礁钟守望者的理想路线",
            recommendedPowerTier = 2,
            baseGoldPerHour = 158L,
            baseXpPerHour = 186L,
            baseMaterialPerHour = 9L,
            dangerRate = 0.34,
            enemyIds = listOf("enemy_barnacle_golem", "enemy_gull_raider", "enemy_sunken_knight"),
            bossId = "boss_tidal_warden",
            bossInvestigationRequired = 7,
            requiredExploreHours = 76,
            unlockDescription = "累计有效探索 76 小时，潮汐滩调查度 8"
        ),
        // Starlit Ridge
        RouteDef(
            id = "route_starlit_pass",
            regionId = RegionId.STARLIT_RIDGE,
            name = "碎石风道",
            subtitle = "顺着山脊云线延伸的石径，夜空格外清朗",
            dangerTag = "高地清寒 · 星辉碎晶",
            environmentTag = EnvironmentTag.CHILL,
            recommendationHint = "敌人速度快，先发制人与安稳休整很实用",
            recommendedPowerTier = 3,
            baseGoldPerHour = 175L,
            baseXpPerHour = 210L,
            baseMaterialPerHour = 8L,
            dangerRate = 0.28,
            enemyIds = listOf("enemy_frost_marmot", "enemy_crystal_hawk", "enemy_meteor_shard"),
            bossId = "boss_starlit_automaton",
            bossInvestigationRequired = 6,
            requiredExploreHours = 112,
            unlockDescription = "累计有效探索 112 小时，潮汐滩调查度 7"
        ),
        RouteDef(
            id = "route_starlit_crater",
            regionId = RegionId.STARLIT_RIDGE,
            name = "微光陨坑",
            subtitle = "坠落星石留下的碗状浅谷，碎晶沿着谷壁生长",
            dangerTag = "高地清寒 · 晶簇密集",
            environmentTag = EnvironmentTag.CHILL,
            recommendationHint = "高收益矿晶采集点，搜集策略回报出色",
            recommendedPowerTier = 3,
            baseGoldPerHour = 196L,
            baseXpPerHour = 232L,
            baseMaterialPerHour = 9L,
            dangerRate = 0.34,
            enemyIds = listOf("enemy_meteor_shard", "enemy_ridge_ram", "enemy_echo_monk"),
            bossId = "boss_starlit_automaton",
            bossInvestigationRequired = 7,
            requiredExploreHours = 136,
            unlockDescription = "累计有效探索 136 小时，星陨山脊调查度 5"
        ),
        RouteDef(
            id = "route_starlit_observatory",
            regionId = RegionId.STARLIT_RIDGE,
            name = "废墟星台",
            subtitle = "山顶半坍塌的石砌星盘，静候重启夜空记录",
            dangerTag = "高地清寒 · 终章首领",
            environmentTag = EnvironmentTag.CHILL,
            recommendationHint = "建议装备 3 阶构筑与“首领克星”挑战陨星古偶",
            recommendedPowerTier = 3,
            baseGoldPerHour = 220L,
            baseXpPerHour = 258L,
            baseMaterialPerHour = 10L,
            dangerRate = 0.38,
            enemyIds = listOf("enemy_ridge_ram", "enemy_echo_monk", "enemy_obsidian_hound"),
            bossId = "boss_starlit_automaton",
            bossInvestigationRequired = 8,
            requiredExploreHours = 164,
            unlockDescription = "累计有效探索 164 小时，星陨山脊调查度 9"
        ),
        // Cloud Sanctuary
        RouteDef(
            id = "route_cloud_bridge",
            regionId = RegionId.CLOUD_SANCTUARY,
            name = "云桥浮阶",
            subtitle = "沿着上升暖流连接起低空浮岛群的石砌云桥",
            dangerTag = "云海温润 · 云栖灵绒",
            environmentTag = EnvironmentTag.CLOUD,
            recommendationHint = "刚抵达云栖浮岛时的平稳落脚点，适合积累云海调查度",
            recommendedPowerTier = 4,
            baseGoldPerHour = 240L,
            baseXpPerHour = 285L,
            baseMaterialPerHour = 9L,
            dangerRate = 0.30,
            enemyIds = listOf("enemy_cloud_puff", "enemy_dew_sprite", "enemy_wind_harpy"),
            bossId = "boss_cloud_leviathan",
            bossInvestigationRequired = 6,
            requiredExploreHours = 184,
            unlockDescription = "累计有效探索 184 小时，星陨山脊调查度 9"
        ),
        RouteDef(
            id = "route_cloud_greenhouse",
            regionId = RegionId.CLOUD_SANCTUARY,
            name = "星露古温室",
            subtitle = "玻璃穹顶下依然盛开着夜光梦绒花的空中植物园",
            dangerTag = "云海温润 · 秘闻密集",
            environmentTag = EnvironmentTag.CLOUD,
            recommendationHint = "探索策略与梦境共鸣词条在此收获尤为丰厚",
            recommendedPowerTier = 4,
            baseGoldPerHour = 265L,
            baseXpPerHour = 315L,
            baseMaterialPerHour = 10L,
            dangerRate = 0.35,
            enemyIds = listOf("enemy_wind_harpy", "enemy_glass_gardener", "enemy_mirage_manta"),
            bossId = "boss_cloud_leviathan",
            bossInvestigationRequired = 7,
            requiredExploreHours = 200,
            unlockDescription = "累计有效探索 200 小时，云栖浮岛调查度 2"
        ),
        RouteDef(
            id = "route_cloud_spire",
            regionId = RegionId.CLOUD_SANCTUARY,
            name = "风琴云巅塔",
            subtitle = "随高空夜风奏响低沉共鸣的浮岛终极长廊",
            dangerTag = "云海温润 · 云鲸首领",
            environmentTag = EnvironmentTag.CLOUD,
            recommendationHint = "建议携 4 阶装备与首领信物迎战云鲸守梦者",
            recommendedPowerTier = 4,
            baseGoldPerHour = 295L,
            baseXpPerHour = 350L,
            baseMaterialPerHour = 11L,
            dangerRate = 0.38,
            enemyIds = listOf("enemy_glass_gardener", "enemy_mirage_manta", "enemy_sanctuary_guard"),
            bossId = "boss_cloud_leviathan",
            bossInvestigationRequired = 8,
            requiredExploreHours = 216,
            unlockDescription = "累计有效探索 216 小时，云栖浮岛调查度 4"
        ),
        // Dream Sea
        RouteDef(
            id = "route_dream_shoal",
            regionId = RegionId.DREAM_SEA,
            name = "星砂浅海",
            subtitle = "退潮后倒映着整片银河的低语浅滩",
            dangerTag = "星海微光 · 五境初探",
            environmentTag = EnvironmentTag.ASTRAL,
            recommendationHint = "建议携 4 阶以上全套装备与夜间热茶启程",
            recommendedPowerTier = 4,
            baseGoldPerHour = 340L,
            baseXpPerHour = 410L,
            baseMaterialPerHour = 13L,
            dangerRate = 0.36,
            enemyIds = listOf("enemy_starlight_jelly", "enemy_coral_crab", "enemy_prism_ray"),
            bossId = "boss_dream_lighthouse_whale",
            bossInvestigationRequired = 8,
            requiredExploreHours = 248,
            unlockDescription = "累计有效探索 248 小时，并踏足云栖浮岛"
        ),
        RouteDef(
            id = "route_dream_corridor",
            regionId = RegionId.DREAM_SEA,
            name = "沉船星廊",
            subtitle = "悬挂着旧日航海灯与浮星水母的水下回廊",
            dangerTag = "星海深潮 · 中阶探索",
            environmentTag = EnvironmentTag.ASTRAL,
            recommendationHint = "建议激活流派共鸣并为 3 级以上装备铭刻副词条",
            recommendedPowerTier = 5,
            baseGoldPerHour = 375L,
            baseXpPerHour = 450L,
            baseMaterialPerHour = 14L,
            dangerRate = 0.40,
            enemyIds = listOf("enemy_coral_crab", "enemy_lantern_urchin", "enemy_drift_mariner"),
            bossId = "boss_dream_lighthouse_whale",
            bossInvestigationRequired = 8,
            requiredExploreHours = 268,
            unlockDescription = "累计有效探索 268 小时，沉星梦海调查度 3"
        ),
        RouteDef(
            id = "route_dream_lighthouse",
            regionId = RegionId.DREAM_SEA,
            name = "司灯渊塔",
            subtitle = "漂浮在梦海尽头的古代导航灯塔与渊鲸栖所",
            dangerTag = "星海终境 · 渊鲸首领",
            environmentTag = EnvironmentTag.ASTRAL,
            recommendationHint = "建议携 5 阶满级装备并准备挑战司灯渊鲸",
            recommendedPowerTier = 5,
            baseGoldPerHour = 420L,
            baseXpPerHour = 505L,
            baseMaterialPerHour = 16L,
            dangerRate = 0.44,
            enemyIds = listOf("enemy_lantern_urchin", "enemy_drift_mariner", "enemy_abyss_warden"),
            bossId = "boss_dream_lighthouse_whale",
            bossInvestigationRequired = 10,
            requiredExploreHours = 288,
            unlockDescription = "累计有效探索 288 小时，沉星梦海调查度 5"
        ),

        // Astral Arch
        RouteDef(
            id = "route_astral_arcade",
            regionId = RegionId.ASTRAL_ARCH,
            name = "琥珀星门外廊",
            subtitle = "悬浮于沉星梦海尽头晨昏交界处的半透明琥珀拱门长街",
            dangerTag = "终境初探 · 星珀回廊",
            environmentTag = EnvironmentTag.SOLAR_ECHO,
            recommendationHint = "建议穿戴 5 阶以上精铸装备与双词条并携带星图加成",
            recommendedPowerTier = 6,
            baseGoldPerHour = 470L,
            baseXpPerHour = 560L,
            baseMaterialPerHour = 17L,
            dangerRate = 0.44,
            enemyIds = listOf("enemy_amber_moth", "enemy_chrono_tortoise", "enemy_twilight_lynx"),
            bossId = "boss_astral_chronokeeper",
            bossInvestigationRequired = 8,
            requiredExploreHours = 312,
            unlockDescription = "累计有效探索 312 小时，并踏足沉星梦海"
        ),
        RouteDef(
            id = "route_astral_cloister",
            regionId = RegionId.ASTRAL_ARCH,
            name = "逆时钟声回廊",
            subtitle = "青铜古钟按安眠潮汐节奏自行共鸣的高空石砌长廊",
            dangerTag = "高阶共鸣 · 时隙试炼",
            environmentTag = EnvironmentTag.SOLAR_ECHO,
            recommendationHint = "建议激活 6 点极鸣共鸣或配置 3 枚合适被动",
            recommendedPowerTier = 6,
            baseGoldPerHour = 520L,
            baseXpPerHour = 620L,
            baseMaterialPerHour = 18L,
            dangerRate = 0.46,
            enemyIds = listOf("enemy_chrono_tortoise", "enemy_bell_golem", "enemy_halo_stag"),
            bossId = "boss_astral_chronokeeper",
            bossInvestigationRequired = 9,
            requiredExploreHours = 336,
            unlockDescription = "累计有效探索 336 小时，归梦星拱调查度 3"
        ),
        RouteDef(
            id = "route_astral_sundial",
            regionId = RegionId.ASTRAL_ARCH,
            name = "星穹日晷圣殿",
            subtitle = "由古老星砂日晷环抱的最终长夜花园，封存千夜安宁回响",
            dangerTag = "终拱圣殿 · 钟灵首领",
            environmentTag = EnvironmentTag.SOLAR_ECHO,
            recommendationHint = "建议准备 6 阶极铸装备与 3-4 阶信标试炼归梦时隙钟灵",
            recommendedPowerTier = 6,
            baseGoldPerHour = 580L,
            baseXpPerHour = 690L,
            baseMaterialPerHour = 20L,
            dangerRate = 0.48,
            enemyIds = listOf("enemy_bell_golem", "enemy_halo_stag", "enemy_arch_chronicler"),
            bossId = "boss_astral_chronokeeper",
            bossInvestigationRequired = 10,
            requiredExploreHours = 360,
            unlockDescription = "累计有效探索 360 小时，归梦星拱调查度 5"
        )
    )

    val equipmentDefs: List<EquipmentDef> = listOf(
        // Tier 1 (4 base items across all 3 slots)
        EquipmentDef(
            id = "eq_pine_staff",
            name = "松木旅杖",
            slot = EquipmentSlot.WEAPON,
            tier = 1,
            regionId = RegionId.MISTWOOD,
            baseStats = Stats(maxHp = 8, attack = 6, defense = 1, speed = 2),
            defaultAffix = AffixId.BOUNTIFUL_GATHER,
            description = "轻巧顺手的雾林松木手杖，适合边走边拨开草丛搜集资源。",
            craftGoldCost = 140L,
            craftMaterialId = RegionId.MISTWOOD.materialId,
            craftMaterialCost = 10L,
            requiredWorkshopLevel = 1
        ),
        EquipmentDef(
            id = "eq_moss_cloak",
            name = "苔藓风衣",
            slot = EquipmentSlot.ARMOR,
            tier = 1,
            regionId = RegionId.MISTWOOD,
            baseStats = Stats(maxHp = 22, attack = 1, defense = 5, speed = 1),
            defaultAffix = AffixId.STEADY_RECOVERY,
            description = "染着淡绿林色的厚帆布风衣，夜间露水重也不容易着凉。",
            craftGoldCost = 160L,
            craftMaterialId = RegionId.MISTWOOD.materialId,
            craftMaterialCost = 12L,
            requiredWorkshopLevel = 1
        ),
        EquipmentDef(
            id = "eq_bark_buckler",
            name = "硬木护腕甲",
            slot = EquipmentSlot.ARMOR,
            tier = 1,
            regionId = RegionId.MISTWOOD,
            baseStats = Stats(maxHp = 18, attack = 3, defense = 6, speed = 0),
            defaultAffix = AffixId.SHIELD_STRIKE,
            description = "以坚韧树皮叠制的护臂与胸甲，架起护盾后能顺势反击。",
            craftGoldCost = 180L,
            craftMaterialId = RegionId.MISTWOOD.materialId,
            craftMaterialCost = 14L,
            requiredWorkshopLevel = 1
        ),
        EquipmentDef(
            id = "eq_lantern_charm",
            name = "灯笼花坠饰",
            slot = EquipmentSlot.CHARM,
            tier = 1,
            regionId = RegionId.MISTWOOD,
            baseStats = Stats(maxHp = 10, attack = 2, defense = 2, speed = 3),
            defaultAffix = AffixId.CLUE_SEEKER,
            description = "封存着干花蕊的黄铜小坠，在昏暗小径旁更容易留意到旧石碑。",
            craftGoldCost = 150L,
            craftMaterialId = RegionId.MISTWOOD.materialId,
            craftMaterialCost = 11L,
            requiredWorkshopLevel = 1
        ),

        // Tier 2 (Tidal Flat)
        EquipmentDef(
            id = "eq_tidal_blade",
            name = "潮汐短刃",
            slot = EquipmentSlot.WEAPON,
            tier = 2,
            regionId = RegionId.TIDAL_FLAT,
            baseStats = Stats(maxHp = 12, attack = 11, defense = 2, speed = 4),
            defaultAffix = AffixId.SWIFT_FIRST_STRIKE,
            description = "以潮汐矿砂锻打的弧形短刃，挥击带风，抢先压制敏捷海兽。",
            craftGoldCost = 290L,
            craftMaterialId = RegionId.TIDAL_FLAT.materialId,
            craftMaterialCost = 18L,
            requiredWorkshopLevel = 2
        ),
        EquipmentDef(
            id = "eq_rain_coat",
            name = "御雨油布衣",
            slot = EquipmentSlot.ARMOR,
            tier = 2,
            regionId = RegionId.TIDAL_FLAT,
            baseStats = Stats(maxHp = 38, attack = 2, defense = 10, speed = 2),
            defaultAffix = AffixId.RAIN_WARD,
            description = "涂了海蜡的防雨披风，是穿行潮汐滩和旧灯塔的可靠装备。",
            craftGoldCost = 310L,
            craftMaterialId = RegionId.TIDAL_FLAT.materialId,
            craftMaterialCost = 20L,
            requiredWorkshopLevel = 2
        ),
        EquipmentDef(
            id = "eq_coral_shield_charm",
            name = "潮纹盾石佩",
            slot = EquipmentSlot.CHARM,
            tier = 2,
            regionId = RegionId.TIDAL_FLAT,
            baseStats = Stats(maxHp = 22, attack = 5, defense = 6, speed = 2),
            defaultAffix = AffixId.SHIELD_STRIKE,
            description = "刻着退潮纹路的平滑海石，护盾展开时会把力量引向武器。",
            craftGoldCost = 300L,
            craftMaterialId = RegionId.TIDAL_FLAT.materialId,
            craftMaterialCost = 18L,
            requiredWorkshopLevel = 2
        ),
        EquipmentDef(
            id = "eq_shell_compass",
            name = "贝壳怀表",
            slot = EquipmentSlot.CHARM,
            tier = 2,
            regionId = RegionId.TIDAL_FLAT,
            baseStats = Stats(maxHp = 16, attack = 4, defense = 4, speed = 5),
            defaultAffix = AffixId.SALVAGE_INSIGHT,
            description = "潮汐滩信使惯用的旧怀表，帮助辨别优质矿脉与可用零件。",
            craftGoldCost = 280L,
            craftMaterialId = RegionId.TIDAL_FLAT.materialId,
            craftMaterialCost = 16L,
            requiredWorkshopLevel = 2
        ),

        // Tier 3 (Starlit Ridge)
        EquipmentDef(
            id = "eq_star_lance",
            name = "星辉短矛",
            slot = EquipmentSlot.WEAPON,
            tier = 3,
            regionId = RegionId.STARLIT_RIDGE,
            baseStats = Stats(maxHp = 20, attack = 18, defense = 4, speed = 5),
            defaultAffix = AffixId.BOSS_HUNTER,
            description = "嵌着星辉碎晶的轻矛，对强韧的区域首领格外有穿透力。",
            craftGoldCost = 520L,
            craftMaterialId = RegionId.STARLIT_RIDGE.materialId,
            craftMaterialCost = 26L,
            requiredWorkshopLevel = 3
        ),
        EquipmentDef(
            id = "eq_watchman_halberd",
            name = "守夜旅戟",
            slot = EquipmentSlot.WEAPON,
            tier = 3,
            regionId = RegionId.STARLIT_RIDGE,
            baseStats = Stats(maxHp = 26, attack = 16, defense = 7, speed = 4),
            defaultAffix = AffixId.BOUNTIFUL_GATHER,
            description = "长柄兼作探路杖与防身武器，适合长夜高地清寒路线。",
            craftGoldCost = 500L,
            craftMaterialId = RegionId.STARLIT_RIDGE.materialId,
            craftMaterialCost = 24L,
            requiredWorkshopLevel = 3
        ),
        EquipmentDef(
            id = "eq_meteor_plate",
            name = "陨星轻甲",
            slot = EquipmentSlot.ARMOR,
            tier = 3,
            regionId = RegionId.STARLIT_RIDGE,
            baseStats = Stats(maxHp = 60, attack = 4, defense = 16, speed = 3),
            defaultAffix = AffixId.STEADY_RECOVERY,
            description = "掺入微量陨石铁片的软甲，轻量保暖，在寒风里始终保持从容。",
            craftGoldCost = 540L,
            craftMaterialId = RegionId.STARLIT_RIDGE.materialId,
            craftMaterialCost = 28L,
            requiredWorkshopLevel = 3
        ),
        EquipmentDef(
            id = "eq_astral_prism",
            name = "星棱罗盘",
            slot = EquipmentSlot.CHARM,
            tier = 3,
            regionId = RegionId.STARLIT_RIDGE,
            baseStats = Stats(maxHp = 28, attack = 9, defense = 8, speed = 6),
            defaultAffix = AffixId.CLUE_SEEKER,
            description = "能折射夜空微光的六面晶石护符，指引旅人走向最后一段秘闻。",
            craftGoldCost = 510L,
            craftMaterialId = RegionId.STARLIT_RIDGE.materialId,
            craftMaterialCost = 25L,
            requiredWorkshopLevel = 3
        ),

        // Workshop-exclusive masterworks for Regions 1-3 (craftOnly = true)
        EquipmentDef(
            id = "eq_whisper_flute",
            name = "林风木笛",
            slot = EquipmentSlot.CHARM,
            tier = 1,
            regionId = RegionId.MISTWOOD,
            baseStats = Stats(maxHp = 14, attack = 3, defense = 3, speed = 3),
            defaultAffix = AffixId.DREAM_RESONANCE,
            description = "用老松心材雕成的长夜木笛，吹响时能抚平旅途疲惫。",
            craftGoldCost = 210L,
            craftMaterialId = RegionId.MISTWOOD.materialId,
            craftMaterialCost = 16L,
            requiredWorkshopLevel = 1,
            craftOnly = true
        ),
        EquipmentDef(
            id = "eq_tide_halberd",
            name = "潮音长戟",
            slot = EquipmentSlot.WEAPON,
            tier = 2,
            regionId = RegionId.TIDAL_FLAT,
            baseStats = Stats(maxHp = 18, attack = 13, defense = 4, speed = 3),
            defaultAffix = AffixId.COUNTER_PULSE,
            description = "以沉船铜锚重铸的长戟，迎战成群海兽时能震荡回击。",
            craftGoldCost = 340L,
            craftMaterialId = RegionId.TIDAL_FLAT.materialId,
            craftMaterialCost = 22L,
            requiredWorkshopLevel = 2,
            craftOnly = true
        ),
        EquipmentDef(
            id = "eq_star_feather_boots",
            name = "星羽行囊坠",
            slot = EquipmentSlot.CHARM,
            tier = 3,
            regionId = RegionId.STARLIT_RIDGE,
            baseStats = Stats(maxHp = 24, attack = 7, defense = 7, speed = 8),
            defaultAffix = AffixId.TREASURE_SENSE,
            description = "系着晶羽游隼尾羽的轻巧挂坠，总能更快觉察遗落装备的微光。",
            craftGoldCost = 560L,
            craftMaterialId = RegionId.STARLIT_RIDGE.materialId,
            craftMaterialCost = 28L,
            requiredWorkshopLevel = 3,
            craftOnly = true
        ),

        // Tier 4 (Cloud Sanctuary)
        EquipmentDef(
            id = "eq_cloud_harp_bow",
            name = "风琴长弓",
            slot = EquipmentSlot.WEAPON,
            tier = 4,
            regionId = RegionId.CLOUD_SANCTUARY,
            baseStats = Stats(maxHp = 32, attack = 24, defense = 7, speed = 8),
            defaultAffix = AffixId.CLOUD_WALKER,
            description = "以云栖灵绒与轻木制成的共鸣弓，拉弦时伴随温润微风。",
            craftGoldCost = 820L,
            craftMaterialId = RegionId.CLOUD_SANCTUARY.materialId,
            craftMaterialCost = 34L,
            requiredWorkshopLevel = 4
        ),
        EquipmentDef(
            id = "eq_sanctuary_lance",
            name = "栖云长枪",
            slot = EquipmentSlot.WEAPON,
            tier = 4,
            regionId = RegionId.CLOUD_SANCTUARY,
            baseStats = Stats(maxHp = 38, attack = 26, defense = 9, speed = 6),
            defaultAffix = AffixId.BOSS_HUNTER,
            description = "为高空云鲸航道打造的礼仪长枪，能稳健击穿厚重护盾。",
            craftGoldCost = 860L,
            craftMaterialId = RegionId.CLOUD_SANCTUARY.materialId,
            craftMaterialCost = 36L,
            requiredWorkshopLevel = 4
        ),
        EquipmentDef(
            id = "eq_dream_silk_robe",
            name = "梦绒长袍",
            slot = EquipmentSlot.ARMOR,
            tier = 4,
            regionId = RegionId.CLOUD_SANCTUARY,
            baseStats = Stats(maxHp = 84, attack = 6, defense = 21, speed = 5),
            defaultAffix = AffixId.DREAM_RESONANCE,
            description = "用夜间绽放的梦绒花丝织就的长袍，披上便仿佛置身安稳梦乡。",
            craftGoldCost = 840L,
            craftMaterialId = RegionId.CLOUD_SANCTUARY.materialId,
            craftMaterialCost = 35L,
            requiredWorkshopLevel = 4
        ),
        EquipmentDef(
            id = "eq_aether_guard_plate",
            name = "云琉轻铠",
            slot = EquipmentSlot.ARMOR,
            tier = 4,
            regionId = RegionId.CLOUD_SANCTUARY,
            baseStats = Stats(maxHp = 78, attack = 9, defense = 24, speed = 4),
            defaultAffix = AffixId.COUNTER_PULSE,
            description = "嵌有温室玻璃片与风化云石的护甲，面对围攻时愈战愈稳。",
            craftGoldCost = 880L,
            craftMaterialId = RegionId.CLOUD_SANCTUARY.materialId,
            craftMaterialCost = 38L,
            requiredWorkshopLevel = 4
        ),
        EquipmentDef(
            id = "eq_leviathan_bell",
            name = "云鲸风铃坠",
            slot = EquipmentSlot.CHARM,
            tier = 4,
            regionId = RegionId.CLOUD_SANCTUARY,
            baseStats = Stats(maxHp = 42, attack = 12, defense = 11, speed = 9),
            defaultAffix = AffixId.TREASURE_SENSE,
            description = "模仿云鲸低吟调音的银质小铃，指引旅人拾回散落云海的珍宝。",
            craftGoldCost = 850L,
            craftMaterialId = RegionId.CLOUD_SANCTUARY.materialId,
            craftMaterialCost = 34L,
            requiredWorkshopLevel = 4
        ),
        // Tier 5 (Dream Sea crafted / dropped set)
        EquipmentDef(
            id = "eq_star_sea_blade",
            name = "星砂溯潮刃",
            slot = EquipmentSlot.WEAPON,
            tier = 5,
            regionId = RegionId.DREAM_SEA,
            baseStats = Stats(maxHp = 30, attack = 32, defense = 6, speed = 8),
            defaultAffix = AffixId.GALE_COMBO,
            description = "以深海银晶锻造的工坊名作，仅能通过 Lv.5 工坊打造，挥动时曳出星浪追击。",
            craftGoldCost = 1280L,
            craftMaterialId = RegionId.DREAM_SEA.materialId,
            craftMaterialCost = 36L,
            requiredWorkshopLevel = 5,
            craftOnly = true
        ),
        EquipmentDef(
            id = "eq_abyss_lantern_rod",
            name = "渊塔航灯杖",
            slot = EquipmentSlot.WEAPON,
            tier = 5,
            regionId = RegionId.DREAM_SEA,
            baseStats = Stats(maxHp = 36, attack = 29, defense = 8, speed = 6),
            defaultAffix = AffixId.STARLIGHT_INSIGHT,
            description = "镶着古代航标蓝晶的长杖，光线可穿透沉重外壳。",
            craftGoldCost = 1150L,
            craftMaterialId = RegionId.DREAM_SEA.materialId,
            craftMaterialCost = 30L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_astral_shell_coat",
            name = "海晶棘甲衣",
            slot = EquipmentSlot.ARMOR,
            tier = 5,
            regionId = RegionId.DREAM_SEA,
            baseStats = Stats(maxHp = 108, attack = 4, defense = 24, speed = 3),
            defaultAffix = AffixId.THORN_BASTION,
            description = "嵌有沉星珊瑚骨刺的贴身护甲，受击时回敬微弱震波。",
            craftGoldCost = 1200L,
            craftMaterialId = RegionId.DREAM_SEA.materialId,
            craftMaterialCost = 32L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_dream_whale_cloak",
            name = "渊鲸织梦氅",
            slot = EquipmentSlot.ARMOR,
            tier = 5,
            regionId = RegionId.DREAM_SEA,
            baseStats = Stats(maxHp = 118, attack = 2, defense = 22, speed = 5),
            defaultAffix = AffixId.DREAM_MENDING,
            description = "用渊海暖流丝织成的斗篷，可在夜间激战间隙缓缓回气。",
            craftGoldCost = 1220L,
            craftMaterialId = RegionId.DREAM_SEA.materialId,
            craftMaterialCost = 34L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_lighthouse_compass",
            name = "归航星海罗盘",
            slot = EquipmentSlot.CHARM,
            tier = 5,
            regionId = RegionId.DREAM_SEA,
            baseStats = Stats(maxHp = 54, attack = 15, defense = 14, speed = 11),
            defaultAffix = AffixId.STARLIGHT_INSIGHT,
            description = "指向司灯渊塔航向的黄铜星盘佩饰，让每次夜行判断更精准。",
            craftGoldCost = 1180L,
            craftMaterialId = RegionId.DREAM_SEA.materialId,
            craftMaterialCost = 32L,
            requiredWorkshopLevel = 5
        ),

        // Tier 6 Astral Arch (5 endgame pieces covering all 4 new affixes)
        EquipmentDef(
            id = "eq_arch_chronoblade",
            name = "星穹逆时剑",
            slot = EquipmentSlot.WEAPON,
            tier = 6,
            regionId = RegionId.ASTRAL_ARCH,
            baseStats = Stats(maxHp = 36, attack = 38, defense = 11, speed = 13),
            defaultAffix = AffixId.CHRONO_STRIKE,
            description = "刃身封存星珀沙漏微粒的长剑，挥斩间似能抢先半拍触及风向。",
            craftGoldCost = 1650L,
            craftMaterialId = RegionId.ASTRAL_ARCH.materialId,
            craftMaterialCost = 38L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_arch_astral_orbiter",
            name = "星拱共鸣杖",
            slot = EquipmentSlot.WEAPON,
            tier = 6,
            regionId = RegionId.ASTRAL_ARCH,
            baseStats = Stats(maxHp = 42, attack = 35, defense = 13, speed = 11),
            defaultAffix = AffixId.ASTRAL_HARMONY,
            description = "顶端悬浮三枚黄铜星环的手杖，能汇集装备与秘闻的共鸣余韵。",
            craftGoldCost = 1620L,
            craftMaterialId = RegionId.ASTRAL_ARCH.materialId,
            craftMaterialCost = 36L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_arch_sanctuary_mail",
            name = "星穹守望重铠",
            slot = EquipmentSlot.ARMOR,
            tier = 6,
            regionId = RegionId.ASTRAL_ARCH,
            baseStats = Stats(maxHp = 144, attack = 6, defense = 31, speed = 4),
            defaultAffix = AffixId.SANCTUARY_BULWARK,
            description = "由琥珀陶钢片与古钟青铜丝缀合的旅铠，即使激战也能守住稳固防线。",
            craftGoldCost = 1680L,
            craftMaterialId = RegionId.ASTRAL_ARCH.materialId,
            craftMaterialCost = 40L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_arch_dawn_mantle",
            name = "晨昏织梦法袍",
            slot = EquipmentSlot.ARMOR,
            tier = 6,
            regionId = RegionId.ASTRAL_ARCH,
            baseStats = Stats(maxHp = 136, attack = 8, defense = 28, speed = 8),
            defaultAffix = AffixId.LUCID_PULSE,
            description = "染着晨昏渐变微光的轻盈长袍，行走间带着温润安神的节奏呼吸。",
            craftGoldCost = 1640L,
            craftMaterialId = RegionId.ASTRAL_ARCH.materialId,
            craftMaterialCost = 38L,
            requiredWorkshopLevel = 5
        ),
        EquipmentDef(
            id = "eq_arch_censer_charm",
            name = "守钟星珀香坠",
            slot = EquipmentSlot.CHARM,
            tier = 6,
            regionId = RegionId.ASTRAL_ARCH,
            baseStats = Stats(maxHp = 70, attack = 19, defense = 18, speed = 13),
            defaultAffix = AffixId.STARLIGHT_INSIGHT,
            description = "盛着星拱干燥松脂与琥珀碎屑的雕花挂坠，让每夜记录都格外清晰。",
            craftGoldCost = 1600L,
            craftMaterialId = RegionId.ASTRAL_ARCH.materialId,
            craftMaterialCost = 36L,
            requiredWorkshopLevel = 5
        )
    )

    val eventTemplates: List<EventTemplate> = listOf(
        // Mistwood (10 events)
        EventTemplate(
            id = "ev_mw_fallen_pine",
            regionId = RegionId.MISTWOOD,
            title = "风折的红松",
            kind = EventKind.GATHER,
            weight = 35,
            cooldownJourneys = 0,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            firstDiscoveryText = "路边躺着一截被昨夜山风放倒的干松，木纹紧实，正好带回营地修缮工具台。",
            repeatText = "在同一处林间空地又检拾到一批干燥的红松枝料。",
            bonusGold = 12L, bonusXp = 14L, bonusMaterial = 3L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_mw_tidal_tablet",
            regionId = RegionId.MISTWOOD,
            title = "刻着潮纹的石片",
            kind = EventKind.CLUE,
            weight = 24,
            cooldownJourneys = 2,
            minInvestigation = 0,
            minRegionExploreHours = 8,
            firstDiscoveryText = "苔藓底下露出一块温润石片，上面刻着指向海边浅滩的波浪记号。",
            repeatText = "再次描摹林中石片上的潮纹符号，核对了地图桌上的旧标注。",
            bonusGold = 18L, bonusXp = 28L, bonusMaterial = 1L, investigationGain = 2,
            companionId = "comp_airen"
        ),
        EventTemplate(
            id = "ev_mw_messenger_satchel",
            regionId = RegionId.MISTWOOD,
            title = "树梢挂着的邮包",
            kind = EventKind.CLUE,
            weight = 18,
            cooldownJourneys = 3,
            minInvestigation = 1,
            minRegionExploreHours = 16,
            firstDiscoveryText = "替路过的信使艾琳取下挂在枝桠上的防水皮包，她笑着留下一份雾林小径草图。",
            repeatText = "在树屋驿站遇见信使艾琳的记号，顺手帮忙把沿路木牌扶正。",
            bonusGold = 24L, bonusXp = 30L, bonusMaterial = 1L, investigationGain = 2,
            companionId = "comp_airen"
        ),
        EventTemplate(
            id = "ev_mw_lantern_tea",
            regionId = RegionId.MISTWOOD,
            title = "林间热茶棚",
            kind = EventKind.REST,
            weight = 22,
            cooldownJourneys = 1,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            firstDiscoveryText = "荒废的采药棚里炉灰尚温，旅人烧了一壶松针水，安静地整理完背包再继续前行。",
            repeatText = "在熟识的茶棚旁短暂歇脚，顺便把鞋子上的湿泥拍净。",
            bonusGold = 10L, bonusXp = 20L, bonusMaterial = 1L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_mw_resin_hollow",
            regionId = RegionId.MISTWOOD,
            title = "琥珀树脂树洞",
            kind = EventKind.GATHER,
            weight = 26,
            cooldownJourneys = 0,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            requiredStrategy = ExpeditionStrategy.GATHER,
            firstDiscoveryText = "老槐树裂缝间凝着成块的金黄树脂，用小刀小心撬下来装进木盒。",
            repeatText = "收集到一袋清香的林木树脂，工坊很需要这种粘合料。",
            bonusGold = 16L, bonusXp = 16L, bonusMaterial = 4L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_mw_moss_bridge",
            regionId = RegionId.MISTWOOD,
            title = "青苔石拱桥",
            kind = EventKind.CLUE,
            weight = 20,
            cooldownJourneys = 1,
            minInvestigation = 2,
            minRegionExploreHours = 24,
            firstDiscoveryText = "跨越溪涧的石拱桥缝里夹着一枚旧徽章，看来曾有另一队旅人在雾林测量地形。",
            repeatText = "听着石桥下清澈的流水声走过雾林南段，顺手记下水位变化。",
            bonusGold = 20L, bonusXp = 26L, bonusMaterial = 1L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_mw_mushroom_ring",
            regionId = RegionId.MISTWOOD,
            title = "微光菌环",
            kind = EventKind.CLUE,
            weight = 18,
            cooldownJourneys = 2,
            minInvestigation = 2,
            minRegionExploreHours = 32,
            firstDiscoveryText = "夜露凝结时，林地草坪围出一圈微光蘑菇，照亮了平时难见的隐秘兽径。",
            repeatText = "绕开菌环走过沼泽边缘，采回几株能作照明引子的荧光干菌。",
            bonusGold = 15L, bonusXp = 24L, bonusMaterial = 2L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_mw_woodcutter_cache",
            regionId = RegionId.MISTWOOD,
            title = "旧伐木工棚",
            kind = EventKind.GATHER,
            weight = 22,
            cooldownJourneys = 1,
            minInvestigation = 1,
            minRegionExploreHours = 8,
            firstDiscoveryText = "木棚墙角堆着码放整齐的备料木条，还附带一小袋用来买灯油的铜币。",
            repeatText = "在工棚周边整理出散落的木板，带回营地补充建材。",
            bonusGold = 22L, bonusXp = 18L, bonusMaterial = 3L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_mw_stag_tracks",
            regionId = RegionId.MISTWOOD,
            title = "林鹿的浅蹄印",
            kind = EventKind.CLUE,
            weight = 16,
            cooldownJourneys = 2,
            minInvestigation = 3,
            minRegionExploreHours = 40,
            firstDiscoveryText = "湿润的林土上印着比寻常野鹿更大一圈的蹄印，旁边的蕨叶上还留着白色花瓣。",
            repeatText = "顺着新鲜的林鹿足迹望去，雾气深处传来一声悠长的鹿鸣。",
            bonusGold = 18L, bonusXp = 32L, bonusMaterial = 1L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_mw_owl_perch",
            regionId = RegionId.MISTWOOD,
            title = "长夜枭的栖木",
            kind = EventKind.REST,
            weight = 20,
            cooldownJourneys = 1,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            firstDiscoveryText = "老猫头鹰蹲在路标柱上静静打量着来客，像是在替夜行的旅人放哨。",
            repeatText = "夜风穿过松林梢头，路标下的小火堆安静地烧了一小会儿。",
            bonusGold = 14L, bonusXp = 22L, bonusMaterial = 1L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_mw_elder_tree_echo",
            regionId = RegionId.MISTWOOD,
            title = "千年古树下的树洞手记",
            kind = EventKind.CLUE,
            weight = 16,
            cooldownJourneys = 2,
            minInvestigation = 5,
            minRegionExploreHours = 56,
            firstDiscoveryText = "深入雾林深处时，在老松树洞里翻出前代巡林人留下的油纸笔记，描绘着雾林与云海的交界风向。",
            repeatText = "顺着古树树冠间隙望向夜空，确认雾林深处的几条支线依旧畅通。",
            bonusGold = 24L, bonusXp = 34L, bonusMaterial = 2L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_mw_rogue_briar",
            regionId = RegionId.MISTWOOD,
            title = "林道巡游的棘藤兽",
            kind = EventKind.ENCOUNTER,
            weight = 16,
            cooldownJourneys = 1,
            minInvestigation = 4,
            minRegionExploreHours = 56,
            firstDiscoveryText = "一头浑身披着干藤的林兽拦在路口，旅人稳健挥杖将它驱离后，在灌木后捡回不少松木备料。",
            repeatText = "再次击退试探营地外缘的迷途棘藤兽，顺手修整了受损的路桩。",
            bonusGold = 26L, bonusXp = 36L, bonusMaterial = 2L, investigationGain = 1
        ),

        // Tidal Flat (10 events)
        EventTemplate(
            id = "ev_tf_silver_sand",
            regionId = RegionId.TIDAL_FLAT,
            title = "退潮的矿砂带",
            kind = EventKind.GATHER,
            weight = 34,
            cooldownJourneys = 0,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            firstDiscoveryText = "海潮褪去后，浅水洼底沉着闪亮的铁砂与细晶，用布筛一淘便装满小半袋。",
            repeatText = "沿着浅滩水线仔细筛淘出干净的潮汐矿砂。",
            bonusGold = 20L, bonusXp = 22L, bonusMaterial = 3L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_tf_messenger_beacon",
            regionId = RegionId.TIDAL_FLAT,
            title = "海风中的旧信筒",
            kind = EventKind.CLUE,
            weight = 22,
            cooldownJourneys = 2,
            minInvestigation = 0,
            minRegionExploreHours = 8,
            firstDiscoveryText = "在半埋沙里的铜制信筒中找到艾琳留给灯塔守夜人的潮汐表，海路一下子明朗起来。",
            repeatText = "把被潮水打湿的路标重新挂回礁石木桩上，记录今日潮位。",
            bonusGold = 28L, bonusXp = 38L, bonusMaterial = 2L, investigationGain = 2,
            companionId = "comp_airen"
        ),
        EventTemplate(
            id = "ev_tf_ship_bell",
            regionId = RegionId.TIDAL_FLAT,
            title = "沉船桅杆的铜铃",
            kind = EventKind.CLUE,
            weight = 20,
            cooldownJourneys = 2,
            minInvestigation = 2,
            minRegionExploreHours = 16,
            firstDiscoveryText = "搁浅的货船舱室里还锁着一箱干爽工具，铜铃上的刻字写着“望星航线”。",
            repeatText = "攀上斜倾的船舷张望潮线，顺便收拢被浪冲上来的船木金属扣件。",
            bonusGold = 32L, bonusXp = 40L, bonusMaterial = 2L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_tf_tidepool_rest",
            regionId = RegionId.TIDAL_FLAT,
            title = "背风礁洞篝火",
            kind = EventKind.REST,
            weight = 22,
            cooldownJourneys = 1,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            firstDiscoveryText = "找到一处刚好挡住海风的干燥石穴，烤干披风后又恢复了满满精神。",
            repeatText = "听着远处有节奏的浪拍声在礁洞里小憩片刻。",
            bonusGold = 18L, bonusXp = 30L, bonusMaterial = 1L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_tf_pearl_oyster",
            regionId = RegionId.TIDAL_FLAT,
            title = "礁隙贝壳群",
            kind = EventKind.GATHER,
            weight = 26,
            cooldownJourneys = 0,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            requiredStrategy = ExpeditionStrategy.GATHER,
            firstDiscoveryText = "低潮露出整片青灰礁隙，里面不仅有厚壳海贝，还有不少高纯度沙矿。",
            repeatText = "撬开退潮露出的礁石缝，收获了一把沉甸甸的矿砂与海贝。",
            bonusGold = 26L, bonusXp = 24L, bonusMaterial = 4L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_tf_noah_lens",
            regionId = RegionId.TIDAL_FLAT,
            title = "观星者的旧镜片",
            kind = EventKind.CLUE,
            weight = 18,
            cooldownJourneys = 3,
            minInvestigation = 3,
            minRegionExploreHours = 24,
            firstDiscoveryText = "灯塔底层的石桌上留着观测员诺亚的航海日志，里面第一次提到了北方的星陨山脊。",
            repeatText = "擦拭干净灯塔下层的铜镜支架，海雾消散时能看见远方山脉轮廓。",
            bonusGold = 30L, bonusXp = 44L, bonusMaterial = 2L, investigationGain = 2,
            companionId = "comp_noah"
        ),
        EventTemplate(
            id = "ev_tf_sea_glass",
            regionId = RegionId.TIDAL_FLAT,
            title = "磨圆的海玻璃",
            kind = EventKind.GATHER,
            weight = 20,
            cooldownJourneys = 1,
            minInvestigation = 1,
            minRegionExploreHours = 32,
            firstDiscoveryText = "浅滩上散布着被海浪打磨得像玉石一样的蓝绿玻璃，和硬币一样好出手。",
            repeatText = "一路拾起几片透亮的海玻璃，给旅舍窗台添些小装饰。",
            bonusGold = 34L, bonusXp = 24L, bonusMaterial = 2L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_tf_warden_echo",
            regionId = RegionId.TIDAL_FLAT,
            title = "潮钟的低鸣",
            kind = EventKind.CLUE,
            weight = 16,
            cooldownJourneys = 2,
            minInvestigation = 4,
            minRegionExploreHours = 8,
            firstDiscoveryText = "防波堤深处传出浑厚铜钟回响，礁钟守望者的巡逻路线被清晰记录了下来。",
            repeatText = "避开涨潮期走过防波堤外缘，摸清了灯塔守望者的换防空隙。",
            bonusGold = 28L, bonusXp = 46L, bonusMaterial = 2L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_tf_salt_kiln",
            regionId = RegionId.TIDAL_FLAT,
            title = "海边荒废盐灶",
            kind = EventKind.GATHER,
            weight = 22,
            cooldownJourneys = 1,
            minInvestigation = 1,
            minRegionExploreHours = 40,
            firstDiscoveryText = "旧盐灶底还留着粗盐块与矿灰，打包带回工坊做回火材料再合适不过。",
            repeatText = "在老盐灶遗址旁补装了一袋海盐矿粉。",
            bonusGold = 24L, bonusXp = 26L, bonusMaterial = 3L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_tf_driftwood_bench",
            regionId = RegionId.TIDAL_FLAT,
            title = "漂流木长椅",
            kind = EventKind.REST,
            weight = 18,
            cooldownJourneys = 1,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            firstDiscoveryText = "不知哪位前人用巨大漂流木搭了张面向大海的长椅，刚好能检查一下鞋带与护甲扣。",
            repeatText = "在海边长椅上伴着湿润海风调整呼吸，紧接着继续赶路。",
            bonusGold = 18L, bonusXp = 32L, bonusMaterial = 1L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_tf_sunken_chart",
            regionId = RegionId.TIDAL_FLAT,
            title = "铜盒里的旧海图",
            kind = EventKind.CLUE,
            weight = 16,
            cooldownJourneys = 2,
            minInvestigation = 5,
            minRegionExploreHours = 56,
            firstDiscoveryText = "从潮汐礁洞深处打捞出一只密封铜盒，里面的海图记载了海风顺着山脊升向浮岛的季节。",
            repeatText = "对照铜盒旧海图校对暗礁位置，避开了几处涨潮暗流。",
            bonusGold = 34L, bonusXp = 48L, bonusMaterial = 2L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_tf_reef_crab_pack",
            regionId = RegionId.TIDAL_FLAT,
            title = "阻路的甲壳群",
            kind = EventKind.ENCOUNTER,
            weight = 16,
            cooldownJourneys = 1,
            minInvestigation = 4,
            minRegionExploreHours = 56,
            firstDiscoveryText = "几只背负矿砂礁石的大蟹堵住了石堤出口，利落清开道路后收获了满满一袋矿砂。",
            repeatText = "稳稳击退拦在防波堤上的潮汐甲蟹，并从礁石缝里搜出散落金属。",
            bonusGold = 36L, bonusXp = 44L, bonusMaterial = 3L, investigationGain = 1
        ),

        // Starlit Ridge (10 events)
        EventTemplate(
            id = "ev_sr_crystal_vein",
            regionId = RegionId.STARLIT_RIDGE,
            title = "裂隙中的星辉晶",
            kind = EventKind.GATHER,
            weight = 34,
            cooldownJourneys = 0,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            firstDiscoveryText = "山脊冷风吹散积雪后，暗色岩缝里露出淡蓝微光的晶簇，小心敲下一小捧。",
            repeatText = "沿山脊岩脉又采集到数块透亮的星辉碎晶。",
            bonusGold = 32L, bonusXp = 34L, bonusMaterial = 3L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_sr_noah_star_chart",
            regionId = RegionId.STARLIT_RIDGE,
            title = "观星者的铜制星盘",
            kind = EventKind.CLUE,
            weight = 22,
            cooldownJourneys = 2,
            minInvestigation = 0,
            minRegionExploreHours = 8,
            firstDiscoveryText = "在山路避风石龛里找到了诺亚留下的手绘星图，标注了陨坑与古星台的准确位置。",
            repeatText = "对照诺亚星图确认夜间方位，在清寒的高地上没有迷路。",
            bonusGold = 38L, bonusXp = 54L, bonusMaterial = 2L, investigationGain = 2,
            companionId = "comp_noah"
        ),
        EventTemplate(
            id = "ev_sr_meteor_dust",
            regionId = RegionId.STARLIT_RIDGE,
            title = "陨坑余温",
            kind = EventKind.REST,
            weight = 22,
            cooldownJourneys = 1,
            minInvestigation = 0,
            minRegionExploreHours = 16,
            firstDiscoveryText = "一块深埋地表的陨铁石仍带着微微暖意，旅人靠在旁边驱散了不少高地寒气。",
            repeatText = "在微暖的陨石洼地休整了一阵，记录下周围星晶生长的规律。",
            bonusGold = 26L, bonusXp = 42L, bonusMaterial = 2L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_sr_observatory_gate",
            regionId = RegionId.STARLIT_RIDGE,
            title = "星台石门铭文",
            kind = EventKind.CLUE,
            weight = 20,
            cooldownJourneys = 2,
            minInvestigation = 2,
            minRegionExploreHours = 0,
            firstDiscoveryText = "半掩的石门上刻着历代守夜人的名字，石槽里恰好能嵌进一枚旅者携带的灯石。",
            repeatText = "拂去星台走廊上的砂砾，重温石壁上刻录的星象周期。",
            bonusGold = 42L, bonusXp = 58L, bonusMaterial = 2L, investigationGain = 2,
            companionId = "comp_noah"
        ),
        EventTemplate(
            id = "ev_sr_quartz_pocket",
            regionId = RegionId.STARLIT_RIDGE,
            title = "高寒晶洞",
            kind = EventKind.GATHER,
            weight = 25,
            cooldownJourneys = 0,
            minInvestigation = 1,
            minRegionExploreHours = 0,
            requiredStrategy = ExpeditionStrategy.GATHER,
            firstDiscoveryText = "石壁后方藏着一人高的天然晶洞，地面散落着许多已经脱落的完整星晶。",
            repeatText = "在高寒晶洞边缘拾满了一袋纯净星辉碎晶。",
            bonusGold = 36L, bonusXp = 36L, bonusMaterial = 4L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_sr_wind_chime",
            regionId = RegionId.STARLIT_RIDGE,
            title = "山巅石风铃",
            kind = EventKind.CLUE,
            weight = 18,
            cooldownJourneys = 2,
            minInvestigation = 2,
            minRegionExploreHours = 24,
            firstDiscoveryText = "风道石柱上悬着细碎水晶片串成的风铃，夜风吹过时能提醒前方是否结霜。",
            repeatText = "听着山巅清脆的水晶风铃声顺利通过碎石坡。",
            bonusGold = 34L, bonusXp = 48L, bonusMaterial = 2L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_sr_courier_cairn",
            regionId = RegionId.STARLIT_RIDGE,
            title = "山脊堆石路标",
            kind = EventKind.CLUE,
            weight = 18,
            cooldownJourneys = 2,
            minInvestigation = 1,
            minRegionExploreHours = 32,
            firstDiscoveryText = "在齐腰高的堆石路标顶上发现艾琳刚换上去的蓝布条，原来她的送信路线一直通到了这里。",
            repeatText = "往路标石堆上多添了一块稳当的扁石，替后来旅人标明夜路。",
            bonusGold = 30L, bonusXp = 44L, bonusMaterial = 2L, investigationGain = 1,
            companionId = "comp_airen"
        ),
        EventTemplate(
            id = "ev_sr_automaton_gear",
            regionId = RegionId.STARLIT_RIDGE,
            title = "静默星仪齿轮",
            kind = EventKind.CLUE,
            weight = 16,
            cooldownJourneys = 2,
            minInvestigation = 4,
            minRegionExploreHours = 8,
            firstDiscoveryText = "星台偏殿里滚落着一只精密黄铜齿轮，显然是陨星古偶定期维护留下的零件。",
            repeatText = "沿着星台外廊观察古偶的巡行轨道，找到更好的避让与反击节奏。",
            bonusGold = 45L, bonusXp = 62L, bonusMaterial = 2L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_sr_silver_lichen",
            regionId = RegionId.STARLIT_RIDGE,
            title = "银叶雪苔",
            kind = EventKind.GATHER,
            weight = 22,
            cooldownJourneys = 1,
            minInvestigation = 0,
            minRegionExploreHours = 40,
            firstDiscoveryText = "峭壁背阴处生长着稀有的银叶地衣，连同石粉一起刮下，在集市上很抢手。",
            repeatText = "顺路收集了几把晾干后能入药的银叶雪苔。",
            bonusGold = 40L, bonusXp = 32L, bonusMaterial = 2L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_sr_dawn_ridge",
            regionId = RegionId.STARLIT_RIDGE,
            title = "星落前的微光时刻",
            kind = EventKind.REST,
            weight = 20,
            cooldownJourneys = 1,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            firstDiscoveryText = "黎明前的山脊最安静，整片云海在脚下缓慢流动，令人浑身轻快。",
            repeatText = "在星陨山脊目送最后几颗星子隐入晨曦，收好行囊准备返程。",
            bonusGold = 30L, bonusXp = 50L, bonusMaterial = 1L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_sr_sky_rope_clue",
            regionId = RegionId.STARLIT_RIDGE,
            title = "垂落云海的缆绳锚桩",
            kind = EventKind.CLUE,
            weight = 18,
            cooldownJourneys = 2,
            minInvestigation = 6,
            minRegionExploreHours = 56,
            firstDiscoveryText = "在星台背风悬崖边发现一根系着夜光布条的古铜锚桩，似乎与云海之上的浮岛航道相连。",
            repeatText = "检查悬崖锚桩的绳结与风向，等待云海气流平稳后记录在案。",
            bonusGold = 46L, bonusXp = 64L, bonusMaterial = 2L, investigationGain = 2,
            companionId = "comp_lin"
        ),
        EventTemplate(
            id = "ev_sr_frost_golem_skirmish",
            regionId = RegionId.STARLIT_RIDGE,
            title = "星台外廊的寒石巡卫",
            kind = EventKind.ENCOUNTER,
            weight = 16,
            cooldownJourneys = 1,
            minInvestigation = 5,
            minRegionExploreHours = 56,
            firstDiscoveryText = "一具守卫陨坑边缘的寒石巡卫苏醒挡路，沉着化解冲击后获得了它护持的碎晶。",
            repeatText = "顺手解除寒石巡卫的警戒姿态，从石座旁采回星辉碎晶。",
            bonusGold = 48L, bonusXp = 60L, bonusMaterial = 3L, investigationGain = 1
        ),

        // Cloud Sanctuary (12 events)
        EventTemplate(
            id = "ev_cs_dream_silk_tuft",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "浮阶边的梦绒花丛",
            kind = EventKind.GATHER,
            weight = 34,
            cooldownJourneys = 0,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            firstDiscoveryText = "石砌云桥两侧缠满如丝绒般柔软的微光花絮，摘下一把放入包里格外轻盈。",
            repeatText = "沿着云桥栏杆收集夜间新抽丝的云栖灵绒。",
            bonusGold = 44L, bonusXp = 48L, bonusMaterial = 3L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_cs_lin_watering_can",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "温室门口的铜制水壶",
            kind = EventKind.CLUE,
            weight = 24,
            cooldownJourneys = 2,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            firstDiscoveryText = "温室门前摆着一只擦拭得亮晶晶的铜水壶，园丁琳留下的纸条写着如何辨别可安全落脚的浮石。",
            repeatText = "替园丁琳把温室门口被风吹斜的木牌扶正，顺带记录浮岛潮汐高度。",
            bonusGold = 50L, bonusXp = 68L, bonusMaterial = 2L, investigationGain = 2,
            companionId = "comp_lin"
        ),
        EventTemplate(
            id = "ev_cs_warm_cloud_bench",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "玻璃廊道暖风小憩",
            kind = EventKind.REST,
            weight = 22,
            cooldownJourneys = 1,
            minInvestigation = 0,
            minRegionExploreHours = 0,
            firstDiscoveryText = "古温室的暖气管道仍在缓缓送风，旅人靠在藤椅旁整理行囊，听着云海沙沙流过。",
            repeatText = "在玻璃廊道暖风中静坐小憩，调整呼吸后继续沿着浮阶漫步。",
            bonusGold = 38L, bonusXp = 58L, bonusMaterial = 1L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_cs_organ_pipe_note",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "风琴石柱的低鸣乐谱",
            kind = EventKind.CLUE,
            weight = 20,
            cooldownJourneys = 2,
            minInvestigation = 1,
            minRegionExploreHours = 8,
            firstDiscoveryText = "风琴塔底座刻着一套古老音阶，风穿过不同石管时竟对应着各座浮岛的方位。",
            repeatText = "聆听风琴石柱随夜风奏出的平稳长音，记下高空航道的风速变化。",
            bonusGold = 52L, bonusXp = 72L, bonusMaterial = 2L, investigationGain = 2,
            companionId = "comp_noah"
        ),
        EventTemplate(
            id = "ev_cs_dew_cistern",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "凝露琉璃水池",
            kind = EventKind.GATHER,
            weight = 26,
            cooldownJourneys = 0,
            minInvestigation = 0,
            minRegionExploreHours = 8,
            requiredStrategy = ExpeditionStrategy.GATHER,
            firstDiscoveryText = "浅浅的琉璃蓄水池里漂满夜间凝结的云丝与晶露，捞起后刚好能补充工坊耗材。",
            repeatText = "在蓄水池边缘挑拣出纯净结实的云栖灵绒。",
            bonusGold = 48L, bonusXp = 46L, bonusMaterial = 4L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_cs_airen_sky_post",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "云端邮筒的蓝丝带",
            kind = EventKind.CLUE,
            weight = 18,
            cooldownJourneys = 2,
            minInvestigation = 2,
            minRegionExploreHours = 16,
            firstDiscoveryText = "浮岛高处竟立着一座锈迹斑斑的信箱，艾琳在箱顶系上新丝带，表示所有远方信件都已连通。",
            repeatText = "检查云端信箱里留存的天气简报，替艾琳记录今天云层厚度。",
            bonusGold = 54L, bonusXp = 74L, bonusMaterial = 2L, investigationGain = 2,
            companionId = "comp_airen"
        ),
        EventTemplate(
            id = "ev_cs_harpy_skirmish",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "云桥巡哨的风琴隼翼",
            kind = EventKind.ENCOUNTER,
            weight = 18,
            cooldownJourneys = 1,
            minInvestigation = 1,
            minRegionExploreHours = 16,
            firstDiscoveryText = "两只守护巢穴的风琴隼翼俯冲试探，旅人稳稳格挡后赢得尊重，捡回巢边散落的灵绒。",
            repeatText = "轻巧避开风琴隼翼的俯冲并护住行囊，顺利穿越云桥中段。",
            bonusGold = 56L, bonusXp = 68L, bonusMaterial = 3L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_cs_seed_vault",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "古温室种子库",
            kind = EventKind.CLUE,
            weight = 18,
            cooldownJourneys = 2,
            minInvestigation = 3,
            minRegionExploreHours = 24,
            firstDiscoveryText = "推开贴满玻璃花纹的侧门，里面整齐封存着几百瓶夜光花种与浮岛修建日记。",
            repeatText = "帮琳整理种子库木架上的玻璃瓶，核对花期与云层湿度。",
            bonusGold = 58L, bonusXp = 76L, bonusMaterial = 2L, investigationGain = 2,
            companionId = "comp_lin"
        ),
        EventTemplate(
            id = "ev_cs_noah_sky_mirror",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "高空星象折射镜",
            kind = EventKind.CLUE,
            weight = 18,
            cooldownJourneys = 2,
            minInvestigation = 3,
            minRegionExploreHours = 24,
            firstDiscoveryText = "诺亚曾在笔记里画过的浮空铜镜就悬在温室正顶，透过它能清晰望见整条星陨山脊。",
            repeatText = "擦去折射镜框上的夜露，确认星象刻度依旧准星。",
            bonusGold = 60L, bonusXp = 80L, bonusMaterial = 2L, investigationGain = 2,
            companionId = "comp_noah"
        ),
        EventTemplate(
            id = "ev_cs_starlit_silk_loom",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "停摆的云绒织机",
            kind = EventKind.GATHER,
            weight = 22,
            cooldownJourneys = 1,
            minInvestigation = 2,
            minRegionExploreHours = 32,
            firstDiscoveryText = "工坊旧址里还保留着半卷织好的发光绒布与铜币钱袋，恰好充作远征补给。",
            repeatText = "从旧织机梭芯旁取下缠好的云栖灵绒线轴。",
            bonusGold = 62L, bonusXp = 54L, bonusMaterial = 3L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_cs_whale_song_terrace",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "云鲸回响露台",
            kind = EventKind.REST,
            weight = 20,
            cooldownJourneys = 1,
            minInvestigation = 4,
            minRegionExploreHours = 40,
            firstDiscoveryText = "站在最上方的白石露台，远方云鲸低沉而温柔的吟唱穿过云海，让人彻底忘却白昼烦躁。",
            repeatText = "伴着远处云鲸缓缓游过的风声在露台小憩片刻，整装待归。",
            bonusGold = 48L, bonusXp = 70L, bonusMaterial = 2L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_cs_gardener_construct",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "迷途的发条园丁",
            kind = EventKind.ENCOUNTER,
            weight = 18,
            cooldownJourneys = 2,
            minInvestigation = 5,
            minRegionExploreHours = 48,
            firstDiscoveryText = "一台发条卡住的琉璃花匠把旅人当成闯入温室的乱摘客，重新校准它的发条后获得丰厚谢礼。",
            repeatText = "协助安抚巡逻过度的发条园丁偶，沿途修剪出一条安稳夜路。",
            bonusGold = 66L, bonusXp = 84L, bonusMaterial = 3L, investigationGain = 2
        ),
        // Dream Sea (12 events)
        EventTemplate(
            id = "ev_ds_star_sand_ripple",
            regionId = RegionId.DREAM_SEA,
            title = "星砂浅汐纹",
            kind = EventKind.GATHER,
            weight = 34,
            cooldownJourneys = 0,
            minInvestigation = 0,
            firstDiscoveryText = "退潮的银白浅湾闪着一层细碎荧光砂，你仔细筛出晶莹干净的沉星砂囊。",
            repeatText = "沿着星砂浅汐纹收集晶砂，海风凉爽而平缓。",
            bonusGold = 38L, bonusXp = 44L, bonusMaterial = 2L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_ds_drifting_buoy",
            regionId = RegionId.DREAM_SEA,
            title = "无名航标浮筒",
            kind = EventKind.CLUE,
            weight = 26,
            cooldownJourneys = 1,
            minInvestigation = 0,
            firstDiscoveryText = "从漂浮木桶内壁刻痕中破译了旧航线缩写，确认通往沉船星廊的潮隙方位。",
            repeatText = "核对航标浮筒的潮汐刻字，确认深海水道并无暗流变化。",
            bonusGold = 32L, bonusXp = 54L, bonusMaterial = 1L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_ds_coral_tea_rock",
            regionId = RegionId.DREAM_SEA,
            title = "温热海蚀石亭",
            kind = EventKind.REST,
            weight = 22,
            cooldownJourneys = 0,
            minInvestigation = 0,
            firstDiscoveryText = "海蚀岩洞里透出地下暖泉的水汽，你在背风岩阶支起行囊安心歇息。",
            repeatText = "听着海蚀洞温柔的潮音小憩片刻，疲惫感随海雾散去。",
            bonusGold = 24L, bonusXp = 58L, bonusMaterial = 0L, investigationGain = 0
        ),
        EventTemplate(
            id = "ev_ds_jelly_lantern_band",
            regionId = RegionId.DREAM_SEA,
            title = "浮星水母灯阵",
            kind = EventKind.ENCOUNTER,
            weight = 22,
            cooldownJourneys = 1,
            minInvestigation = 1,
            firstDiscoveryText = "一群半透明的浮星水母顺夜流飘近，你用随身铜铃温和引开它们并拾起脱落晶囊。",
            repeatText = "顺着水母灯阵微微偏航避险，还捞起一枚被浪推上岸的夜光贝。",
            bonusGold = 54L, bonusXp = 66L, bonusMaterial = 2L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_ds_xiu_sketchbook",
            regionId = RegionId.DREAM_SEA,
            title = "汐的湿透画夹",
            kind = EventKind.CLUE,
            weight = 20,
            cooldownJourneys = 2,
            minInvestigation = 1,
            companionId = "comp_xiu",
            firstDiscoveryText = "在珊瑚礁旁遇见正用干布抢救写生画稿的画师汐，她用一页沉星海浅滩速写与你交换夜间见闻。",
            repeatText = "替汐撑起防潮斗篷，她顺手把最新标注的星海潮隙绘页添进你的旅途手记。",
            bonusGold = 42L, bonusXp = 64L, bonusMaterial = 1L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_ds_sunken_bell_frame",
            regionId = RegionId.DREAM_SEA,
            title = "半埋旧船钟架",
            kind = EventKind.GATHER,
            weight = 20,
            cooldownJourneys = 1,
            minInvestigation = 2,
            firstDiscoveryText = "从半截木质船首像旁拆下一块完好的防锈铜锭与几缕沉星砂。",
            repeatText = "清理船钟架周边海沙，带回趁手的古船零件。",
            bonusGold = 48L, bonusXp = 54L, bonusMaterial = 2L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_ds_mirror_pool",
            regionId = RegionId.DREAM_SEA,
            title = "无波水镜海盆",
            kind = EventKind.CLUE,
            weight = 18,
            cooldownJourneys = 1,
            minInvestigation = 2,
            requiredStrategy = ExpeditionStrategy.STEADY,
            firstDiscoveryText = "无风的海盆清晰映出双星倒影，正好校准了沉船星廊入口方位。",
            repeatText = "趁海面如镜确认星位偏差，记录一段安静而精确的航程笔记。",
            bonusGold = 36L, bonusXp = 70L, bonusMaterial = 1L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_ds_abyss_wreck_crate",
            regionId = RegionId.DREAM_SEA,
            title = "旧商船封蜡木箱",
            kind = EventKind.GATHER,
            weight = 16,
            cooldownJourneys = 2,
            minInvestigation = 3,
            requiredStrategy = ExpeditionStrategy.EXPLORE,
            firstDiscoveryText = "小心撬开仍保留蜂蜡封口的沉船货箱，里面整整齐齐放着旅银币与星砂瓶。",
            repeatText = "翻捡沉船暗舱里的备用补给，稳步收好密封材料。",
            bonusGold = 72L, bonusXp = 60L, bonusMaterial = 3L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_ds_mariner_salute",
            regionId = RegionId.DREAM_SEA,
            title = "旧灯塔巡夜影",
            kind = EventKind.ENCOUNTER,
            weight = 18,
            cooldownJourneys = 2,
            minInvestigation = 4,
            minRegionExploreHours = 12,
            firstDiscoveryText = "提着蓝灯的古代水手影朝你缓缓举灯致意，护送一段无风险途后留下一小袋星晶。",
            repeatText = "与巡夜水手影各自举灯相照，平安越过水流湍急的海峡段落。",
            bonusGold = 64L, bonusXp = 82L, bonusMaterial = 2L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_ds_xiu_night_easel",
            regionId = RegionId.DREAM_SEA,
            title = "崖边夜光画架",
            kind = EventKind.REST,
            weight = 15,
            cooldownJourneys = 3,
            minInvestigation = 5,
            companionId = "comp_xiu",
            minRegionExploreHours = 20,
            firstDiscoveryText = "汐在司灯渊塔下搭起防风小画棚，煮了一壶暖海风果茶分你半盏，讲起沉星海古代画师们的守夜习惯。",
            repeatText = "夜风经过汐的小画架旁发出沙沙轻响，你和着潮声沉稳休整了一阵。",
            bonusGold = 46L, bonusXp = 86L, bonusMaterial = 2L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_ds_lighthouse_lens",
            regionId = RegionId.DREAM_SEA,
            title = "失落的棱镜残片",
            kind = EventKind.CLUE,
            weight = 16,
            cooldownJourneys = 2,
            minInvestigation = 6,
            minRegionExploreHours = 28,
            firstDiscoveryText = "在灯塔底座拾得一片折射银河光带的水晶棱镜，清晰指明司灯渊鲸的栖息海沟。",
            repeatText = "拂去棱镜残片上的海盐，对渊鲸深梦航路的了解又深入一分。",
            bonusGold = 58L, bonusXp = 88L, bonusMaterial = 2L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_ds_beacon_echo",
            regionId = RegionId.DREAM_SEA,
            title = "深梦古钟回响",
            kind = EventKind.ENCOUNTER,
            weight = 14,
            cooldownJourneys = 3,
            minInvestigation = 7,
            minRegionExploreHours = 36,
            firstDiscoveryText = "水下灯塔传出低沉平和的长钟余韵，震落的古代金属牌刻着沉星航队的敬礼纹章。",
            repeatText = "跟随古钟低吟的节拍穿行沉星水廊，满载旅途收获踏上归途。",
            bonusGold = 78L, bonusXp = 96L, bonusMaterial = 3L, investigationGain = 2
        ),

        // Late-game extra events (+1 each for regions 1..5 -> 13/region)
        EventTemplate(
            id = "ev_mw_root_archive",
            regionId = RegionId.MISTWOOD,
            title = "古松年轮石匣",
            kind = EventKind.CLUE,
            weight = 14,
            cooldownJourneys = 3,
            minInvestigation = 6,
            minRegionExploreHours = 64,
            firstDiscoveryText = "你拂开千年松根下的苔衣，掘出一只封存初代守林人星图残卷的松木匣。",
            repeatText = "再度查验松根石匣的年轮刻痕，雾林深处的水汽与山风都显得格外亲切。",
            bonusGold = 32L, bonusXp = 44L, bonusMaterial = 3L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_tf_sunken_bell_vault",
            regionId = RegionId.TIDAL_FLAT,
            title = "退潮石穹密窖",
            kind = EventKind.GATHER,
            weight = 14,
            cooldownJourneys = 3,
            minInvestigation = 6,
            minRegionExploreHours = 64,
            firstDiscoveryText = "罕见的低潮露出半座旧港石穹，里面安静躺着完好的潮汐计时沙漏与矿袋。",
            repeatText = "沿着低潮石阶再次巡视密窖，拾得几块圆润的海盐矿砂。",
            bonusGold = 48L, bonusXp = 58L, bonusMaterial = 3L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_sr_observatory_seal",
            regionId = RegionId.STARLIT_RIDGE,
            title = "旧天文台星盘暗格",
            kind = EventKind.CLUE,
            weight = 14,
            cooldownJourneys = 3,
            minInvestigation = 6,
            minRegionExploreHours = 64,
            firstDiscoveryText = "转动生锈的赤道铜环后，石柱暗格弹出指向更远云巅与星拱的古代刻度。",
            repeatText = "依循天文台赤道环重校星辰方位，夜间行进的判断更加准确。",
            bonusGold = 60L, bonusXp = 74L, bonusMaterial = 3L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_cs_organ_harmonic",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "云巅风琴长阶共鸣",
            kind = EventKind.REST,
            weight = 14,
            cooldownJourneys = 3,
            minInvestigation = 6,
            minRegionExploreHours = 64,
            firstDiscoveryText = "云层升起时，整排风琴石柱吹奏出绵长和弦，让紧绷了一路的肩颈彻底松弛。",
            repeatText = "伴着风琴石柱悠远的和声稍作安歇，云海暖风轻轻托住旅人的梦绪。",
            bonusGold = 68L, bonusXp = 84L, bonusMaterial = 3L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_ds_arch_gate_glimpse",
            regionId = RegionId.DREAM_SEA,
            title = "星海彼端拱门微光",
            kind = EventKind.CLUE,
            weight = 14,
            cooldownJourneys = 3,
            minInvestigation = 6,
            minRegionExploreHours = 64,
            firstDiscoveryText = "航船穿过雾带时，远方海平线一瞬映出半座悬空琥珀石拱门与静谧的钟声回响。",
            repeatText = "再次遥望星海彼端的晨昏微光，你在航图边记下前往归梦星拱的风向。",
            bonusGold = 84L, bonusXp = 104L, bonusMaterial = 3L, investigationGain = 2
        ),

        // Astral Arch (13 events)
        EventTemplate(
            id = "ev_aa_amber_column",
            regionId = RegionId.ASTRAL_ARCH,
            title = "琥珀刻字长柱",
            kind = EventKind.CLUE,
            weight = 20,
            cooldownJourneys = 1,
            minInvestigation = 0,
            firstDiscoveryText = "拱门外廊的半透明琥珀柱里封印着历代旅人安眠前写下的问候小字。",
            repeatText = "轻触微温的琥珀柱面，细碎金光映出今夜前方的安全石阶。",
            bonusGold = 54L, bonusXp = 72L, bonusMaterial = 2L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_aa_dust_hourglass",
            regionId = RegionId.ASTRAL_ARCH,
            title = "逆流碎砂坛",
            kind = EventKind.GATHER,
            weight = 20,
            cooldownJourneys = 1,
            minInvestigation = 0,
            firstDiscoveryText = "石坛中的金色星珀微粒正缓慢向上飘浮，你接住一把沉淀在底座的晶亮琥珀碎。",
            repeatText = "在石坛边稳稳装好凝结的星拱琥珀碎屑，顺手清点了随身囊袋。",
            bonusGold = 46L, bonusXp = 60L, bonusMaterial = 3L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_aa_bell_pavilion",
            regionId = RegionId.ASTRAL_ARCH,
            title = "风铃守时亭",
            kind = EventKind.REST,
            weight = 18,
            cooldownJourneys = 2,
            minInvestigation = 0,
            firstDiscoveryText = "八角小亭悬挂的青铜风铃每隔一刻轻鸣一声，在暮色辉光里令人沉静安神。",
            repeatText = "在守时亭檐下闭目小憩，青铜铃声抚平夜风带来的疲倦。",
            bonusGold = 42L, bonusXp = 78L, bonusMaterial = 1L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_aa_starlight_sundial",
            regionId = RegionId.ASTRAL_ARCH,
            title = "黄铜星轨日晷",
            kind = EventKind.CLUE,
            weight = 18,
            cooldownJourneys = 1,
            minInvestigation = 1,
            firstDiscoveryText = "校对倾斜的青铜晷针后，日晷投下的微光刻度标记出逆时钟声回廊的暗道位置。",
            repeatText = "借星轨日晷重确认各境星象节点，记录下清晰的星图摘要。",
            bonusGold = 56L, bonusXp = 82L, bonusMaterial = 2L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_aa_twilight_bridge",
            regionId = RegionId.ASTRAL_ARCH,
            title = "暮光悬桥",
            kind = EventKind.ENCOUNTER,
            weight = 17,
            cooldownJourneys = 2,
            minInvestigation = 1,
            firstDiscoveryText = "两座星拱之间由半凝固的琥珀流光铺就桥面，踏上去竟像厚地毯般踏实稳当。",
            repeatText = "踏着温润流光走过暮光悬桥，顺道在桥头栏杆上挂了一枚路标缎带。",
            bonusGold = 64L, bonusXp = 86L, bonusMaterial = 2L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_aa_amber_workshop",
            regionId = RegionId.ASTRAL_ARCH,
            title = "旧时制钟石桌",
            kind = EventKind.GATHER,
            weight = 17,
            cooldownJourneys = 2,
            minInvestigation = 2,
            minRegionExploreHours = 4,
            firstDiscoveryText = "古时铸钟匠留下的石桌摆满黄铜小齿轮与抛光琥珀块，一切仍如刚收工般整洁。",
            repeatText = "在制钟石桌抽屉中仔细挑选了几块合用的琥珀坯料。",
            bonusGold = 58L, bonusXp = 76L, bonusMaterial = 3L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_aa_halo_herd",
            regionId = RegionId.ASTRAL_ARCH,
            title = "白鹿星环巡礼",
            kind = EventKind.ENCOUNTER,
            weight = 16,
            cooldownJourneys = 2,
            minInvestigation = 2,
            minRegionExploreHours = 6,
            firstDiscoveryText = "三只角戴星环的虚影白鹿列队穿行长街，为首者停步低头，朝你的提灯行了一个古老致意。",
            repeatText = "静立拱廊石柱后目送白鹿影群安然踱过长街，地上落下两枚微光珀粉。",
            bonusGold = 72L, bonusXp = 92L, bonusMaterial = 2L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_aa_cen_tea_break",
            regionId = RegionId.ASTRAL_ARCH,
            title = "钟楼檐角的陈年松茶",
            kind = EventKind.REST,
            weight = 16,
            cooldownJourneys = 3,
            minInvestigation = 3,
            companionId = "comp_cen",
            minRegionExploreHours = 8,
            firstDiscoveryText = "岑从怀表铜盒取出封存许久的烘干松针叶，用小酒精炉煮了一盏带着淡琥珀香的热茶。",
            repeatText = "岑替钟楼齿轮滴了两滴松油后坐回檐角，你们安静分饮完一壶晚茶。",
            bonusGold = 58L, bonusXp = 96L, bonusMaterial = 2L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_aa_chronicle_archive",
            regionId = RegionId.ASTRAL_ARCH,
            title = "无名旅者手札架",
            kind = EventKind.CLUE,
            weight = 16,
            cooldownJourneys = 2,
            minInvestigation = 4,
            minRegionExploreHours = 12,
            firstDiscoveryText = "长廊尽头的铜书架整整齐齐码着几十本皮革手账，扉页写着同一句话：「愿今晚睡个好觉」。",
            repeatText = "翻阅前人手账中夹着的干燥花片，对照补全了圣殿区域的观察笔记。",
            bonusGold = 68L, bonusXp = 98L, bonusMaterial = 2L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_aa_pendulum_hall",
            regionId = RegionId.ASTRAL_ARCH,
            title = "千钟共振石厅",
            kind = EventKind.GATHER,
            weight = 15,
            cooldownJourneys = 2,
            minInvestigation = 4,
            minRegionExploreHours = 16,
            firstDiscoveryText = "石厅正中巨大的星摆每摆动一次，周围数百只小摆钟就一同亮起柔光，却无半点嘈杂声。",
            repeatText = "顺着大星摆宁静的节律穿行石厅，收获一批被星风吹聚的晶莹碎珀。",
            bonusGold = 78L, bonusXp = 104L, bonusMaterial = 3L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_aa_solstice_garden",
            regionId = RegionId.ASTRAL_ARCH,
            title = "晨昏交界温室",
            kind = EventKind.GATHER,
            weight = 15,
            cooldownJourneys = 3,
            minInvestigation = 5,
            minRegionExploreHours = 20,
            firstDiscoveryText = "由半透明琥珀玻璃罩起的空中苗圃里，雾林松芽与梦海海菊竟同时静静开放。",
            repeatText = "照料晨昏温室里新萌发的夜光幼苗后，带走几颗熟透坠落的星拱琥珀籽。",
            bonusGold = 70L, bonusXp = 94L, bonusMaterial = 4L, investigationGain = 1
        ),
        EventTemplate(
            id = "ev_aa_echo_mosaic",
            regionId = RegionId.ASTRAL_ARCH,
            title = "六境星图镶嵌画",
            kind = EventKind.CLUE,
            weight = 15,
            cooldownJourneys = 2,
            minInvestigation = 6,
            minRegionExploreHours = 24,
            firstDiscoveryText = "圣殿地砖用六种夜光矿石拼出从雾林到星拱的完整长途航路，最后一枚空白徽位正对着你的旅灯。",
            repeatText = "蹲在星图镶嵌画前校对星轨线条，对归梦时隙钟灵的律动节奏了然于胸。",
            bonusGold = 82L, bonusXp = 112L, bonusMaterial = 2L, investigationGain = 2
        ),
        EventTemplate(
            id = "ev_aa_chronokeeper_chime",
            regionId = RegionId.ASTRAL_ARCH,
            title = "圣殿整点祈安钟声",
            kind = EventKind.ENCOUNTER,
            weight = 14,
            cooldownJourneys = 3,
            minInvestigation = 7,
            minRegionExploreHours = 30,
            firstDiscoveryText = "日晷圣殿深处传来温厚悠长的零点钟声，钟灵并未阻拦来客，而是放下了一段柔和的金珀试炼音阶。",
            repeatText = "听完日晷圣殿响起的祈安长钟，沿着安静铺开的晨光石阶整装折返。",
            bonusGold = 94L, bonusXp = 124L, bonusMaterial = 3L, investigationGain = 2
        )
    )

    val companionStories: List<CompanionStoryDef> = listOf(
        CompanionStoryDef(
            id = "comp_airen",
            name = "艾琳",
            title = "穿雾信使",
            passiveId = "passive_keen_eye",
            combatSkillName = "驿路速递",
            combatRoleSummary = "随行时速度 +3；每第 2 回合触发迅捷箭雨，协助攻击",
            dispatchRoleSummary = "营地协理时每 2 小时沿雾林古道递送余件，带回金币与雾林松木",
            dispatchRegionId = RegionId.MISTWOOD,
            chapters = listOf(
                CompanionChapterDef(
                    chapterIndex = 1,
                    title = "第一章 · 湿漉漉的邮包",
                    requiredInnLevel = 0,
                    requiredExploreHours = 24,
                    requiredInvestigation = 2,
                    regionId = RegionId.MISTWOOD,
                    summary = "你在雾林帮艾琳取回挂在枝桠上的邮包，她告诉你自己正沿着旧驿道把停运多年的灯塔信件一封封送回。"
                ),
                CompanionChapterDef(
                    chapterIndex = 2,
                    title = "第二章 · 潮汐时刻表",
                    requiredInnLevel = 1,
                    requiredExploreHours = 64,
                    requiredInvestigation = 4,
                    regionId = RegionId.TIDAL_FLAT,
                    summary = "艾琳来到营地旅舍借火烘干外衣，并把潮汐滩旧信筒里的航线记录借你抄录，地图桌上的海岸路线随之更清晰了。"
                ),
                CompanionChapterDef(
                    chapterIndex = 3,
                    title = "第三章 · 山脊上的蓝布条",
                    requiredInnLevel = 2,
                    requiredExploreHours = 136,
                    requiredInvestigation = 4,
                    regionId = RegionId.STARLIT_RIDGE,
                    summary = "艾琳终于将最后一封跨越林海与浅滩的信送达高地守夜人后代手里，她在山巅留下标记，与你约定继续描绘更大的旅图。"
                ),
                CompanionChapterDef(
                    chapterIndex = 4,
                    title = "第四章 · 云端邮筒",
                    requiredInnLevel = 3,
                    requiredExploreHours = 200,
                    requiredInvestigation = 3,
                    regionId = RegionId.CLOUD_SANCTUARY,
                    summary = "艾琳将最后一枚风铃信标系在浮岛云桥的信箱顶上，从此雾林、海岸、山脊与云海的驿站终于连成一张温暖地图。"
                )
            )
        ),
        CompanionStoryDef(
            id = "comp_noah",
            name = "诺亚",
            title = "静夜观星者",
            passiveId = "passive_ember_resolve",
            combatSkillName = "星象校准",
            combatRoleSummary = "随行时强化护盾并在第 3 回合释放破甲星束，克制高防目标",
            dispatchRoleSummary = "营地协理时每 2 小时校订星表与潮汐手记，带回经验与金币",
            dispatchRegionId = RegionId.STARLIT_RIDGE,
            chapters = listOf(
                CompanionChapterDef(
                    chapterIndex = 1,
                    title = "第一章 · 褪色的望远镜",
                    requiredInnLevel = 1,
                    requiredExploreHours = 80,
                    requiredInvestigation = 3,
                    regionId = RegionId.TIDAL_FLAT,
                    summary = "从灯塔底层带回的观测日记把年轻的观星者诺亚引到了你的营地，他说那座旧天文台曾在十年前记录过一次奇异流星。"
                ),
                CompanionChapterDef(
                    chapterIndex = 2,
                    title = "第二章 · 石龛星图",
                    requiredInnLevel = 2,
                    requiredExploreHours = 152,
                    requiredInvestigation = 3,
                    regionId = RegionId.STARLIT_RIDGE,
                    summary = "对照山脊避风石龛找回的铜制星盘，诺亚校准了整座废墟星台的轨道记录，并教你如何在寒夜保持体温与从容。"
                ),
                CompanionChapterDef(
                    chapterIndex = 3,
                    title = "第三章 · 重亮的星台",
                    requiredInnLevel = 3,
                    requiredExploreHours = 184,
                    requiredInvestigation = 8,
                    regionId = RegionId.STARLIT_RIDGE,
                    summary = "随着废墟星台的机械古偶重归平静，诺亚点亮了观测台上的微光棱镜。夜空依旧静谧，但从此不再被遗忘。"
                ),
                CompanionChapterDef(
                    chapterIndex = 4,
                    title = "第四章 · 浮空折射镜",
                    requiredInnLevel = 3,
                    requiredExploreHours = 216,
                    requiredInvestigation = 5,
                    regionId = RegionId.CLOUD_SANCTUARY,
                    summary = "在古温室穹顶找到那面与星台遥相呼应的铜镜后，诺亚终于补全了十年前的流星轨迹，并将其留在你的营地笔记里。"
                )
            )
        ),
        CompanionStoryDef(
            id = "comp_lin",
            name = "琳",
            title = "云海园丁",
            passiveId = "passive_dream_weaver",
            combatSkillName = "晨露花盏",
            combatRoleSummary = "随行时每第 2 回合治疗旅人并补充护盾，提升续航",
            dispatchRoleSummary = "营地协理时每 2 小时照料花圃与干草架，带回对应区域材料与少量金币",
            dispatchRegionId = RegionId.CLOUD_SANCTUARY,
            chapters = listOf(
                CompanionChapterDef(
                    chapterIndex = 1,
                    title = "第一章 · 浮阶铜水壶",
                    requiredInnLevel = 2,
                    requiredExploreHours = 184,
                    requiredInvestigation = 1,
                    regionId = RegionId.CLOUD_SANCTUARY,
                    summary = "循着悬崖锚桩登临云栖浮岛时，你在温室门口遇见正在照料夜光梦绒花的园丁琳，她慷慨分享了安眠呼吸法。"
                ),
                CompanionChapterDef(
                    chapterIndex = 2,
                    title = "第二章 · 种子库钥匙",
                    requiredInnLevel = 2,
                    requiredExploreHours = 200,
                    requiredInvestigation = 3,
                    regionId = RegionId.CLOUD_SANCTUARY,
                    summary = "一起整理完古温室的百余瓶夜光花种后，琳替你的旅舍缝制了一床云绒帐幔，让整夜休整更添安稳庇护。"
                ),
                CompanionChapterDef(
                    chapterIndex = 3,
                    title = "第三章 · 与云鲸同眠",
                    requiredInnLevel = 3,
                    requiredExploreHours = 224,
                    requiredInvestigation = 6,
                    regionId = RegionId.CLOUD_SANCTUARY,
                    summary = "当风琴云巅塔的夜风再度奏响平和长音，琳邀你在温室露台看云鲸游过月影，这段跨越四境的夜游仍在温柔延续。"
                )
            )
        ),
        CompanionStoryDef(
            id = "comp_xiu",
            name = "汐",
            title = "寻梦画师",
            passiveId = "passive_astral_brush",
            combatSkillName = "星海画卷",
            combatRoleSummary = "随行时开局获得 16 点护盾，攻击伤害提升 10%",
            dispatchRoleSummary = "营地协理时每 2 小时临摹风物画稿，带回金币与沉星梦珠；沉星梦海解锁前带回星辉碎晶",
            dispatchRegionId = RegionId.DREAM_SEA,
            chapters = listOf(
                CompanionChapterDef(
                    chapterIndex = 1,
                    title = "第一章 · 海盐颜料盘",
                    requiredInnLevel = 2,
                    requiredExploreHours = 184,
                    requiredInvestigation = 4,
                    regionId = RegionId.STARLIT_RIDGE,
                    summary = "汐背着装满贝壳粉颜料的写生箱来到高地驿站，她说南方海岸之外还有一片夜里像银河落海的水域。"
                ),
                CompanionChapterDef(
                    chapterIndex = 2,
                    title = "第二章 · 潮汐速写册",
                    requiredInnLevel = 3,
                    requiredExploreHours = 248,
                    requiredInvestigation = 2,
                    regionId = RegionId.DREAM_SEA,
                    summary = "共同踏上星砂浅海后，汐替你的营地绘下沉船与浮星水母的第一幅夜景水彩，整间旅舍都像泛起柔光。"
                ),
                CompanionChapterDef(
                    chapterIndex = 3,
                    title = "第三章 · 司灯渊塔长卷",
                    requiredInnLevel = 4,
                    requiredExploreHours = 280,
                    requiredInvestigation = 6,
                    regionId = RegionId.DREAM_SEA,
                    summary = "在司灯渊鲸缓缓游过古灯塔顶部的那一夜，汐落笔完成跨越五境的长卷，将整片安稳夜色留在画纸上。"
                )
            )
        ),
        CompanionStoryDef(
            id = "comp_cen",
            name = "岑",
            title = "守钟巡礼者",
            passiveId = "passive_herbarium_bloom",
            combatSkillName = "晨昏星摆",
            combatRoleSummary = "随行时开局提供 12 点护盾；每第 3 回合恢复生命、补充护盾并协助攻击",
            dispatchRoleSummary = "营地协理时每 2 小时巡护归梦长廊，带回经验、金币与星拱或沉星材料",
            dispatchRegionId = RegionId.ASTRAL_ARCH,
            chapters = listOf(
                CompanionChapterDef(
                    chapterIndex = 1,
                    title = "第一章 · 校准黄铜怀表",
                    requiredInnLevel = 3,
                    requiredExploreHours = 248,
                    requiredInvestigation = 2,
                    regionId = RegionId.DREAM_SEA,
                    summary = "岑提着一盏盛放微光琥珀碎片的黄铜提灯走进营地，替星尘花房的温湿度钟校准了第一圈发条。"
                ),
                CompanionChapterDef(
                    chapterIndex = 2,
                    title = "第二章 · 琥珀回廊重逢",
                    requiredInnLevel = 4,
                    requiredExploreHours = 312,
                    requiredInvestigation = 2,
                    regionId = RegionId.ASTRAL_ARCH,
                    summary = "踏入归梦星拱后，岑翻开随身记录几代守夜人铃铛声的旧册子，帮你在星穹外廊挂好第一盏归途灯。"
                ),
                CompanionChapterDef(
                    chapterIndex = 3,
                    title = "第三章 · 整点无眠之约",
                    requiredInnLevel = 5,
                    requiredExploreHours = 352,
                    requiredInvestigation = 6,
                    regionId = RegionId.ASTRAL_ARCH,
                    summary = "在日晷圣殿最安宁的整点钟声里，岑合上怀表笑道：星摆会代我们记住长夜里的每一点微光，而你只管安心好眠。"
                )
            )
        )
    )

    val keepsakes: List<KeepsakeDef> = listOf(
        KeepsakeDef(
            id = "keepsake_stag_lantern",
            name = "苔角提灯",
            regionId = RegionId.MISTWOOD,
            bossMarkCost = 1L,
            goldCost = 220L,
            materialId = RegionId.MISTWOOD.materialId,
            materialCost = 18L,
            effectSummary = "永久生命上限 +10，雾林探路经验 +5%",
            description = "用苔角林鹿脱落的木质小枝与干花瓣制成的营地提灯，整夜散发柔和草木香。"
        ),
        KeepsakeDef(
            id = "keepsake_tidal_chime",
            name = "潮汐铜钟佩",
            regionId = RegionId.TIDAL_FLAT,
            bossMarkCost = 1L,
            goldCost = 420L,
            materialId = RegionId.TIDAL_FLAT.materialId,
            materialCost = 24L,
            effectSummary = "永久防护 +3，潮汐滩材料采集 +8%",
            description = "取自礁钟守望者残片的微小铜铃，能提醒旅人避开夜潮暗浪。"
        ),
        KeepsakeDef(
            id = "keepsake_star_orrery",
            name = "微光星仪模型",
            regionId = RegionId.STARLIT_RIDGE,
            bossMarkCost = 1L,
            goldCost = 720L,
            materialId = RegionId.STARLIT_RIDGE.materialId,
            materialCost = 28L,
            effectSummary = "永久攻击 +4，星陨山脊金币收益 +8%",
            description = "依照陨星古偶中心齿轮缩微复制的桌面星仪，夜晚会随星位缓缓转动。"
        ),
        KeepsakeDef(
            id = "keepsake_cloud_music_box",
            name = "云鲸八音盒",
            regionId = RegionId.CLOUD_SANCTUARY,
            bossMarkCost = 1L,
            goldCost = 1100L,
            materialId = RegionId.CLOUD_SANCTUARY.materialId,
            materialCost = 32L,
            effectSummary = "永久速度 +2，装备寻获进度 +10%",
            description = "记录着云鲸守梦者吟唱旋律的银质八音盒，是睡前最安神的小摆件。"
        ),
        KeepsakeDef(
            id = "keepsake_everwarm_kettle",
            name = "炉火温茶壶",
            regionId = RegionId.MISTWOOD,
            bossMarkCost = 2L,
            goldCost = 520L,
            materialId = RegionId.TIDAL_FLAT.materialId,
            materialCost = 20L,
            effectSummary = "每次战斗开局额外获得 8 点护盾",
            description = "铜胎外包防寒软木的远行茶壶，让旅人在任何险途开局都能从容应对。"
        ),
        KeepsakeDef(
            id = "keepsake_dream_catcher",
            name = "梦绒捕梦网",
            regionId = RegionId.CLOUD_SANCTUARY,
            bossMarkCost = 2L,
            goldCost = 1350L,
            materialId = RegionId.CLOUD_SANCTUARY.materialId,
            materialCost = 40L,
            effectSummary = "全区域旅程金币与经验基础产出 +5%",
            description = "以四境素材编织的安眠纪念物，见证旅人走过雾林、浅滩、山脊与浮岛。"
        ),
        KeepsakeDef(
            id = "keepsake_abyss_astrolabe",
            name = "渊海星盘盏",
            regionId = RegionId.DREAM_SEA,
            bossMarkCost = 2L,
            goldCost = 1650L,
            materialId = RegionId.DREAM_SEA.materialId,
            materialCost = 36L,
            effectSummary = "永久生命 +24，攻击 +6；沉星梦海金币与材料 +8%",
            description = "依照司灯渊鲸背脊古老星盘仿制的铜制水盏，盛满清浅海水时倒映出微光银河。"
        ),
        KeepsakeDef(
            id = "keepsake_tea_porcelain",
            name = "素釉夜茶盏",
            regionId = RegionId.MISTWOOD,
            bossMarkCost = 1L,
            goldCost = 880L,
            materialId = RegionId.STARLIT_RIDGE.materialId,
            materialCost = 22L,
            effectSummary = "永久防护 +4，速度 +2；每夜装备与材料收获节奏更稳健",
            description = "经火塘炭火温养的粗陶茶盏，握在掌心总能让人定下心来整理行装。"
        ),
        KeepsakeDef(
            id = "keepsake_astral_pendulum",
            name = "时隙星摆",
            regionId = RegionId.ASTRAL_ARCH,
            bossMarkCost = 2L,
            goldCost = 2100L,
            materialId = RegionId.ASTRAL_ARCH.materialId,
            materialCost = 42L,
            effectSummary = "永久生命 +30，攻击 +8；全旅程经验 +6%，归梦星拱材料 +8%",
            description = "依照归梦时隙钟灵主摆缩刻的琥珀黄铜坠饰，摆动时让人感到安心平稳的时间流速。"
        ),
        KeepsakeDef(
            id = "keepsake_conservatory_bell",
            name = "灵植玻璃铃",
            regionId = RegionId.CLOUD_SANCTUARY,
            bossMarkCost = 1L,
            goldCost = 1350L,
            materialId = RegionId.CLOUD_SANCTUARY.materialId,
            materialCost = 32L,
            effectSummary = "永久防护 +6，速度 +3；营地协理派遣收获额外 +15%",
            description = "挂在星尘花房檐口的细嘴吹制玻璃风铃，花苗伸芽时会响起几乎听不见的清音。"
        )
    )

    val milestones: List<MilestoneDef> = listOf(
        MilestoneDef("ms_first_step", "启程守夜人", "完成第 1 次夜间冒险结算", "迈出规律休息与夜间记录的第一步。"),
        MilestoneDef("ms_mistwood_explorer", "雾林寻灯客", "解锁全部 3 条雾林路线", "熟悉灯笼花小径与古树回廊的每一条树荫岔路。"),
        MilestoneDef("ms_tidal_listener", "听潮旅人", "解锁全部 3 条潮汐滩路线", "在退潮银沙与旧灯塔之间记下完整潮汐时刻。"),
        MilestoneDef("ms_starlit_watcher", "星台守望者", "解锁全部 3 条星陨山脊路线", "登上高寒废墟星台，重新校准十年前的夜空记录。"),
        MilestoneDef("ms_cloud_walker", "云海漫步者", "踏足云栖浮岛并解锁全部 12 条路线", "四境长途驿道尽数连通，夜风吹拂着每座营地灯火。"),
        MilestoneDef("ms_boss_slayer", "首领见证者", "累积击败任意区域首领获得首领印记或铭刻首领信物", "以稳健准备通过各境古老守卫者的试炼。"),
        MilestoneDef("ms_master_artisan", "工坊巧匠", "将任意一件装备强化至满级 Lv.5", "把沿途拾得的器材打磨至最趁手的模样。"),
        MilestoneDef("ms_camp_builder", "灯火筑梦师", "将任意营地设施修葺至 Lv.4 及以上", "从简易篝火一路建起温暖完备的山野驿站。"),
        MilestoneDef("ms_lore_collector", "旅途博物家", "记录至少 30 条事件或收录 18 种以上魔物", "把沿途遇见的每一株植物、旧信与夜行生灵写进手札。"),
        MilestoneDef("ms_night_guardian", "百夜同行者", "累计有效探索达 160 小时并解锁至少 3 位伙伴故事线", "与营地伙伴一起度过漫长而安稳的百夜休憩时光。"),
        MilestoneDef("ms_dream_sea_voyager", "沉星远航者", "踏足沉星梦海并解锁全部 15 条探索路线", "沿着星砂浅海与司灯渊塔连通第五境夜航路线。"),
        MilestoneDef("ms_souvenir_curator", "风物拾遗官", "在「旅途风物志」中收录至少 8 件区域风物藏品", "收集旅途中每一枚贝壳、松果与航海棱镜。"),
        MilestoneDef("ms_resonance_master", "共鸣构筑师", "激活至少 1 个流派的 4 件套深度共鸣", "通过装备词条、副词条与被动的精妙搭配成型一套夜行流派。"),
        MilestoneDef("ms_beacon_conqueror", "深梦破晓人", "在深梦信标 1 阶及以上难度完成任意夜间冒险结算", "点亮更高挑战的深梦信标，带回额外的首领印记见证。"),
        MilestoneDef("ms_astral_arch_pioneer", "星拱巡礼客", "踏足归梦星拱并解锁全部 18 条探索路线", "沿着逆时星廊连通最终的第六境归梦长街。"),
        MilestoneDef("ms_triple_passive", "三心融梦", "同时装备 3 个被动技能", "突破 Lv.25 或修复旅舍 Lv.4，学会同时运转三重夜行心得。"),
        MilestoneDef("ms_resonance_tier3", "六曜极鸣", "激活任意流派的 6 点满阶共鸣", "将武器、防具、护符与心法彻底拧成一股纯澈流派。"),
        MilestoneDef("ms_masterwork_artisan", "星尘极铸", "将任意一件装备星尘精铸至 Lv.7", "在工坊炉火与星尘淬炼中锻造出伴随终夜的珍品器材。"),
        MilestoneDef("ms_chronicle_archivist", "秘闻归档人", "完成任意 2 卷区域秘闻卷宗的终章", "把旅途中零散的传闻与线索连成完整的长卷故事。"),
        MilestoneDef("ms_bestiary_expert", "百兽博物志", "至少 12 种魔物的讨伐记录达到熟识精通（15 次及以上）", "对梦境生灵习性了如指掌的真正夜行学者。")
    )

    val teaBlends: List<TeaBlendDef> = listOf(
        TeaBlendDef(
            id = "tea_chamomile",
            name = "甘菊安神饮",
            subtitle = "生命 +16，防护 +2，夜间金币 +4%",
            requiredTeaHearthLevel = 1,
            bonusHp = 16,
            bonusDef = 2,
            goldMultiplierBonus = 0.04
        ),
        TeaBlendDef(
            id = "tea_sea_salt_mint",
            name = "海盐薄荷茶",
            subtitle = "攻击 +3，全旅程经验 +8%",
            requiredTeaHearthLevel = 2,
            bonusAtk = 3,
            xpMultiplierBonus = 0.08
        ),
        TeaBlendDef(
            id = "tea_star_anise",
            name = "星茴暖身露",
            subtitle = "攻击 +4，防护 +4，金币产出 +8%",
            requiredTeaHearthLevel = 3,
            bonusAtk = 4,
            bonusDef = 4,
            goldMultiplierBonus = 0.08
        ),
        TeaBlendDef(
            id = "tea_cloud_oolong",
            name = "云雾烘青茶",
            subtitle = "生命 +22，金币 +5%，装备与风物寻获 +12%",
            requiredTeaHearthLevel = 4,
            bonusHp = 22,
            goldMultiplierBonus = 0.05,
            dropRateBonus = 0.12
        ),
        TeaBlendDef(
            id = "tea_pine_needle",
            name = "古松长夜煎",
            subtitle = "全属性提升，经验 +6%，危险转稳容错额外 +1 次",
            requiredTeaHearthLevel = 5,
            bonusAtk = 5,
            bonusDef = 5,
            bonusHp = 20,
            xpMultiplierBonus = 0.06,
            extraFailureTolerance = 1
        ),
        TeaBlendDef(
            id = "tea_silver_moon_oolong",
            name = "银月浮香乌龙",
            subtitle = "攻击 +7，生命 +24，金币产出 +9%，装备与风物寻获 +10%",
            requiredTeaHearthLevel = 4,
            bonusAtk = 7,
            bonusHp = 24,
            goldMultiplierBonus = 0.09,
            dropRateBonus = 0.10
        ),
        TeaBlendDef(
            id = "tea_astral_amber_brew",
            name = "琥珀星归晚露",
            subtitle = "攻击 +8，防护 +7，生命 +32，金币 +6%，经验 +10%，掉落 +14%，容错 +1",
            requiredTeaHearthLevel = 5,
            bonusAtk = 8,
            bonusDef = 7,
            bonusHp = 32,
            goldMultiplierBonus = 0.06,
            xpMultiplierBonus = 0.10,
            dropRateBonus = 0.14,
            extraFailureTolerance = 1
        )
    )

    val nightPhenomena: List<NightPhenomenon> = listOf(
        NightPhenomenon(
            id = "phenom_mist_dew",
            name = "润木朝露夜",
            subtitle = "雾林潮气湿润，草木茂盛",
            favoredRegionId = RegionId.MISTWOOD,
            favoredEnvironment = EnvironmentTag.FOREST,
            xpBonusPercent = 8,
            materialBonusPercent = 12
        ),
        NightPhenomenon(
            id = "phenom_silver_tide",
            name = "银汐低潮夜",
            subtitle = "海岸长退潮，沉箱与贝壳显露",
            favoredRegionId = RegionId.TIDAL_FLAT,
            favoredEnvironment = EnvironmentTag.DAMP,
            goldBonusPercent = 12,
            dropBonusPercent = 8
        ),
        NightPhenomenon(
            id = "phenom_clear_meteor",
            name = "澄空流星夜",
            subtitle = "高地无云，碎星辉光照亮险峰",
            favoredRegionId = RegionId.STARLIT_RIDGE,
            favoredEnvironment = EnvironmentTag.CHILL,
            xpBonusPercent = 12,
            combatAtkPercent = 6
        ),
        NightPhenomenon(
            id = "phenom_warm_tradewind",
            name = "暖流浮云夜",
            subtitle = "温润高空信风稳定托起云海浮道",
            favoredRegionId = RegionId.CLOUD_SANCTUARY,
            favoredEnvironment = EnvironmentTag.CLOUD,
            goldBonusPercent = 10,
            materialBonusPercent = 10
        ),
        NightPhenomenon(
            id = "phenom_star_sea_tide",
            name = "银河倒映夜",
            subtitle = "星海风平浪静，深海荧光格外明亮",
            favoredRegionId = RegionId.DREAM_SEA,
            favoredEnvironment = EnvironmentTag.ASTRAL,
            goldBonusPercent = 12,
            dropBonusPercent = 12
        ),
        NightPhenomenon(
            id = "phenom_quiet_lantern",
            name = "静风守灯夜",
            subtitle = "整夜薄雾安宁，全线防护更稳健",
            favoredRegionId = RegionId.MISTWOOD,
            favoredEnvironment = EnvironmentTag.FOREST,
            xpBonusPercent = 8,
            combatDefPercent = 8
        ),
        NightPhenomenon(
            id = "phenom_astral_dawn",
            name = "晨昏琥珀夜",
            subtitle = "归梦星拱星辉流转，逆时回廊产出与掉落更丰厚",
            favoredRegionId = RegionId.ASTRAL_ARCH,
            favoredEnvironment = EnvironmentTag.SOLAR_ECHO,
            xpBonusPercent = 12,
            materialBonusPercent = 12,
            dropBonusPercent = 10
        )
    )

    val souvenirs: List<SouvenirDef> = listOf(
        // Mistwood
        SouvenirDef("souv_mw_moss_stamp", RegionId.MISTWOOD, "青苔木刻章", "老旅人在松桩留下的植物图章，盖在手账角边。", Stats(maxHp = 8), goldBonusPercent = 2),
        SouvenirDef("souv_mw_pinecone_bell", RegionId.MISTWOOD, "干燥松果风铃", "微风经过林道时会发出轻轻的木响。", Stats(defense = 2), materialBonusPercent = 3),
        SouvenirDef("souv_mw_amber_leaf", RegionId.MISTWOOD, "露水琥珀叶", "包裹着一滴雾林晨露的半透明金叶。", Stats(speed = 1), xpBonusPercent = 2),
        // Tidal Flat
        SouvenirDef("souv_tf_spiral_conch", RegionId.TIDAL_FLAT, "潮声螺旋贝", "贴近耳边能听见退潮节拍的白纹海螺。", Stats(maxHp = 10, defense = 1), goldBonusPercent = 2),
        SouvenirDef("souv_tf_lighthouse_key", RegionId.TIDAL_FLAT, "旧灯塔备匙", "镀铜防锈的灯塔仓库备用钥匙。", Stats(attack = 2), materialBonusPercent = 3),
        SouvenirDef("souv_tf_seaglass_marble", RegionId.TIDAL_FLAT, "海玻璃弹珠", "被浪花打磨得温润圆滑的蔚蓝玻璃珠。", Stats(speed = 2), xpBonusPercent = 2),
        // Starlit Ridge
        SouvenirDef("souv_sr_frost_quartz", RegionId.STARLIT_RIDGE, "寒霜六棱晶", "高地石缝凝结的透明石英，触手清凉提神。", Stats(attack = 3), xpBonusPercent = 3),
        SouvenirDef("souv_sr_brass_eyepiece", RegionId.STARLIT_RIDGE, "黄铜星镜片", "天文台望远镜遗落的精密瞄准镜片。", Stats(attack = 2, speed = 2), goldBonusPercent = 2),
        SouvenirDef("souv_sr_meteor_pebble", RegionId.STARLIT_RIDGE, "星陨小砾石", "带着微弱磁性的深灰陨铁石子。", Stats(maxHp = 12, defense = 2), materialBonusPercent = 3),
        // Cloud Sanctuary
        SouvenirDef("souv_cs_cloud_seed", RegionId.CLOUD_SANCTUARY, "梦绒花干燥荚", "轻轻晃动便会发出细沙般悦耳沙响的花荚。", Stats(maxHp = 14), xpBonusPercent = 3),
        SouvenirDef("souv_cs_organ_pipe", RegionId.CLOUD_SANCTUARY, "迷你风琴哨", "依风琴云巅塔石柱等比缩小的音哨模型。", Stats(speed = 2, defense = 2), goldBonusPercent = 3),
        SouvenirDef("souv_cs_silk_ribbon", RegionId.CLOUD_SANCTUARY, "晴空织梦带", "琳替温室系花用的柔和银线缎带。", Stats(maxHp = 12, attack = 2), materialBonusPercent = 3),
        // Dream Sea
        SouvenirDef("souv_ds_star_sand_vial", RegionId.DREAM_SEA, "银河星砂瓶", "封存着浅海夜光星砂的小小玻璃坠瓶。", Stats(attack = 4, speed = 1), goldBonusPercent = 3),
        SouvenirDef("souv_ds_whale_scale", RegionId.DREAM_SEA, "渊鲸荧光鳞", "司灯渊鲸游过航道时温和蜕落的贝母鳞片。", Stats(maxHp = 18, defense = 3), materialBonusPercent = 4),
        SouvenirDef("souv_ds_beacon_prism", RegionId.DREAM_SEA, "渊塔微光棱镜", "能在暗处折射出七道安宁光带的航海棱镜。", Stats(attack = 3, defense = 2, speed = 2), xpBonusPercent = 4),
        // Astral Arch
        SouvenirDef("souv_aa_hourglass_shard", RegionId.ASTRAL_ARCH, "逆时砂漏碎片", "砂粒会随睡前呼吸缓缓上浮的细小琥珀砂漏。", Stats(maxHp = 22, speed = 2), xpBonusPercent = 4),
        SouvenirDef("souv_aa_chime_key", RegionId.ASTRAL_ARCH, "古钟守望微匙", "岑赠予旅人的小小黄铜调音匙，轻敲会发出安神音律。", Stats(attack = 5, defense = 3), goldBonusPercent = 4),
        SouvenirDef("souv_aa_star_bookmark", RegionId.ASTRAL_ARCH, "晨昏星轨书签", "刻着六境航路坐标的透亮琥珀片，能记下每夜梦途。", Stats(maxHp = 18, attack = 3, defense = 3), materialBonusPercent = 4)
    )

    val morningEchoes: List<MorningEchoDef> = listOf(
        MorningEchoDef(
            id = "echo_mw_letter",
            regionId = RegionId.MISTWOOD,
            title = "树洞里未寄出的便签",
            prompt = "清晨收队经过古松枝桠时，你发现一截防潮蜡布裹着的两小袋拾遗物。",
            choiceA = MorningEchoOutcome("立刻拆检补给", "拆开蜡布清点了旅银币与几截干燥松木。", rewardGold = 48L, rewardMaterialRegion = RegionId.MISTWOOD, rewardMaterialAmount = 4L),
            choiceB = MorningEchoOutcome("记录森林微风向", "记下木纹风向，下一次夜行金币与经验收益小幅提升。", rewardXp = 32L, nextNightBuff = MorningEchoBuff("echo_mw_letter", "林风指引", goldBonusPercent = 6, xpBonusPercent = 6))
        ),
        MorningEchoDef(
            id = "echo_mw_mushroom",
            regionId = RegionId.MISTWOOD,
            title = "沾露微光伞菌",
            prompt = "路边石阶旁冒出一簇清晨才张开的金色微光菌。",
            choiceA = MorningEchoOutcome("采撷晾干入库", "获得一笔可观木材折价与经验收获。", rewardGold = 36L, rewardXp = 42L, rewardMaterialRegion = RegionId.MISTWOOD, rewardMaterialAmount = 3L),
            choiceB = MorningEchoOutcome("碾碎抹上行囊肩带", "淡淡清香让人整夜精神从容，下次出征防护 +3、材料 +8%。", rewardGold = 18L, nextNightBuff = MorningEchoBuff("echo_mw_mushroom", "晨菌清香", materialBonusPercent = 8, combatDefBonus = 3))
        ),
        MorningEchoDef(
            id = "echo_tf_bottle",
            regionId = RegionId.TIDAL_FLAT,
            title = "搁浅的密封漂流瓶",
            prompt = "清晨第一道潮水褪去，一枚装着海图的琥珀色玻璃瓶滚到了营鞋旁。",
            choiceA = MorningEchoOutcome("兑换瓶中银币与海盐", "直接收获海贝矿石与旅途资金。", rewardGold = 72L, rewardMaterialRegion = RegionId.TIDAL_FLAT, rewardMaterialAmount = 4L),
            choiceB = MorningEchoOutcome("描摹潮隙航线图", "获得下次夜间探索材料与金币双加成。", rewardXp = 48L, nextNightBuff = MorningEchoBuff("echo_tf_bottle", "潮图微调", goldBonusPercent = 7, materialBonusPercent = 7))
        ),
        MorningEchoDef(
            id = "echo_tf_bell",
            regionId = RegionId.TIDAL_FLAT,
            title = "退潮石阶上的铜哨",
            prompt = "礁石缝间露出一支刻着海鸥纹的旧铜哨。",
            choiceA = MorningEchoOutcome("打磨变卖换补给", "顺利充实钱包与浅滩素材库。", rewardGold = 84L, rewardXp = 40L),
            choiceB = MorningEchoOutcome("试吹校准防卫节奏", "清越哨音振奋心神，下次出征攻击 +4、防御 +2。", rewardXp = 35L, nextNightBuff = MorningEchoBuff("echo_tf_bell", "海鸥铜哨", combatAtkBonus = 4, combatDefBonus = 2))
        ),
        MorningEchoDef(
            id = "echo_sr_prism",
            regionId = RegionId.STARLIT_RIDGE,
            title = "晨光里的星台碎镜",
            prompt = "第一缕日光映亮高地残碑时，一块星晶棱镜正把光芒投向山谷。",
            choiceA = MorningEchoOutcome("小心地收进矿袋", "立即获得星尘碎晶与一笔经验奖励。", rewardXp = 85L, rewardMaterialRegion = RegionId.STARLIT_RIDGE, rewardMaterialAmount = 4L),
            choiceB = MorningEchoOutcome("折射校对随身罗盘", "使下一晚经验产出 +10%、攻击 +4。", rewardGold = 45L, nextNightBuff = MorningEchoBuff("echo_sr_prism", "晨星校准", xpBonusPercent = 10, combatAtkBonus = 4))
        ),
        MorningEchoDef(
            id = "echo_sr_hearth",
            regionId = RegionId.STARLIT_RIDGE,
            title = "石龛里未熄的余炭",
            prompt = "昨晚经过的高地避风石龛还留着温热炭灰与一把干草。",
            choiceA = MorningEchoOutcome("清点石龛余粮", "补充不少旅途金币与高地补给。", rewardGold = 105L, rewardXp = 65L),
            choiceB = MorningEchoOutcome("带走一片暖石贴身", "下次出征防护 +5、经验 +6%。", rewardMaterialRegion = RegionId.STARLIT_RIDGE, rewardMaterialAmount = 2L, nextNightBuff = MorningEchoBuff("echo_sr_hearth", "暖石护身", xpBonusPercent = 6, combatDefBonus = 5))
        ),
        MorningEchoDef(
            id = "echo_cs_seedpod",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "随晨风飘落的云绒花瓣",
            prompt = "离开浮岛云桥时，大片夜光梦绒花瓣顺着晨风落进背包。",
            choiceA = MorningEchoOutcome("全部封装带回工坊", "立刻得到一批云绒花与丰厚经验。", rewardGold = 110L, rewardMaterialRegion = RegionId.CLOUD_SANCTUARY, rewardMaterialAmount = 4L),
            choiceB = MorningEchoOutcome("酿成安眠干花袋", "下次夜间探索金币与材料同步 +8%。", rewardXp = 90L, nextNightBuff = MorningEchoBuff("echo_cs_seedpod", "云绒花香", goldBonusPercent = 8, materialBonusPercent = 8))
        ),
        MorningEchoDef(
            id = "echo_cs_chime",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "风琴柱顶的共鸣余韵",
            prompt = "晨光照亮浮岛风琴石柱，空气里飘荡着温润长音。",
            choiceA = MorningEchoOutcome("整理随行见闻手稿", "立得大量探索心得经验与金币。", rewardGold = 135L, rewardXp = 115L),
            choiceB = MorningEchoOutcome("随着旋律调整呼吸步频", "下次出征攻击 +5、防御 +4。", rewardGold = 60L, nextNightBuff = MorningEchoBuff("echo_cs_chime", "风琴共鸣", combatAtkBonus = 5, combatDefBonus = 4))
        ),
        MorningEchoDef(
            id = "echo_ds_glass",
            regionId = RegionId.DREAM_SEA,
            title = "渊塔台阶的星砂贝匣",
            prompt = "海风退去后，一段沉星岩阶上安静躺着嵌有微光蓝晶的木匣。",
            choiceA = MorningEchoOutcome("开匣收取沉星材料", "立刻收获高额沉星砂与经验。", rewardGold = 160L, rewardMaterialRegion = RegionId.DREAM_SEA, rewardMaterialAmount = 5L),
            choiceB = MorningEchoOutcome("擦亮航灯蓝晶随身携行", "下次出征金币 +10%、材料 +8%。", rewardXp = 110L, nextNightBuff = MorningEchoBuff("echo_ds_glass", "蓝灯照航", goldBonusPercent = 10, materialBonusPercent = 8))
        ),
        MorningEchoDef(
            id = "echo_ds_sketch",
            regionId = RegionId.DREAM_SEA,
            title = "潮汐绘下的银河残页",
            prompt = "海面退留的泡沫竟如水墨般勾出渊鲸游过的轨线。",
            choiceA = MorningEchoOutcome("临摹成画卖给行商", "立刻换取一笔丰厚旅金与经验。", rewardGold = 195L, rewardXp = 140L),
            choiceB = MorningEchoOutcome("揣摩渊鲸航向顺势而行", "下次出征经验 +10%、攻击 +6、防御 +4。", rewardMaterialRegion = RegionId.DREAM_SEA, rewardMaterialAmount = 3L, nextNightBuff = MorningEchoBuff("echo_ds_sketch", "星海灵感", xpBonusPercent = 10, combatAtkBonus = 6, combatDefBonus = 4))
        ),
        MorningEchoDef(
            id = "echo_aa_bell_note",
            regionId = RegionId.ASTRAL_ARCH,
            title = "星拱古钟上的新刻痕",
            prompt = "清晨收起提灯时，你发现古钟铜壁多出一圈淡淡金色回响记号。",
            choiceA = MorningEchoOutcome("收集凝落的星拱珀屑", "立刻收获星拱琥珀、金币与大量经验。", rewardGold = 220L, rewardXp = 180L, rewardMaterialRegion = RegionId.ASTRAL_ARCH, rewardMaterialAmount = 5L),
            choiceB = MorningEchoOutcome("铭记古钟整点节拍", "下次出征经验 +12%、材料 +10%、攻击 +7、防御 +5。", rewardMaterialRegion = RegionId.ASTRAL_ARCH, rewardMaterialAmount = 3L, nextNightBuff = MorningEchoBuff("echo_aa_bell_note", "古钟节律", xpBonusPercent = 12, materialBonusPercent = 10, combatAtkBonus = 7, combatDefBonus = 5))
        ),
        MorningEchoDef(
            id = "echo_aa_amber_letter",
            regionId = RegionId.ASTRAL_ARCH,
            title = "封在琥珀里的回信纸",
            prompt = "琥珀底座松动后滑出一张仍散发松墨香的窄信签。",
            choiceA = MorningEchoOutcome("将纪念信页寄给营地工坊", "立刻得到一笔丰厚酬金与星拱琥珀。", rewardGold = 260L, rewardXp = 150L, rewardMaterialRegion = RegionId.ASTRAL_ARCH, rewardMaterialAmount = 4L),
            choiceB = MorningEchoOutcome("把回信夹进旅行手账扉页", "下次出征金币 +12%、经验 +10%、攻防 +6。", rewardMaterialRegion = RegionId.ASTRAL_ARCH, rewardMaterialAmount = 3L, nextNightBuff = MorningEchoBuff("echo_aa_amber_letter", "晨昏祝愿", goldBonusPercent = 12, xpBonusPercent = 10, combatAtkBonus = 6, combatDefBonus = 6))
        )
    )

    val chronicles: List<ChronicleQuestDef> = listOf(
        ChronicleQuestDef(
            id = "chron_mistwood",
            regionId = RegionId.MISTWOOD,
            title = "提灯花古径手记",
            description = "顺着初代守林人留下的松木路牌与荧光花种，查明雾林深处古树常青的秘密。",
            stages = listOf(
                ChronicleStageDef(1, "序章 · 林边油灯牌", 3, 2, "收集到最初几张绘有提灯花标记的林区路线抄本。"),
                ChronicleStageDef(2, "二章 · 年轮暗室", 6, 8, "在古松树根下读懂关于雾气周期与守鹿人的旧录。"),
                ChronicleStageDef(3, "终章 · 雾林长青约", 9, 16, "将失落的守林徽印归位，整座雾林的草木都似轻轻回礼。")
            ),
            rewardStats = Stats(maxHp = 18, defense = 3),
            rewardMaterialBonusPercent = 5,
            completionTitle = "雾林守灯录已成卷：永久生命 +18，防护 +3，全境材料 +5%"
        ),
        ChronicleQuestDef(
            id = "chron_tidal_flat",
            regionId = RegionId.TIDAL_FLAT,
            title = "失落灯塔潮汐簿",
            description = "拼回礁石间散落的黄铜潮汐页片，核对旧港百年前每逢朔望的退潮航道。",
            stages = listOf(
                ChronicleStageDef(1, "序章 · 蚀刻铜汐尺", 3, 4, "依据沙滩浅洼的刻度复原出第一段低潮避险规律。"),
                ChronicleStageDef(2, "二章 · 沉船雾笛室", 6, 10, "找回船舱保存完好的玻璃信号灯芯与铜钟记录簿。"),
                ChronicleStageDef(3, "终章 · 银沙航道图", 9, 18, "旧灯塔的铜钟与你的夜行时刻重新对齐，潮声自此澄明。")
            ),
            rewardStats = Stats(attack = 4, speed = 3),
            rewardGoldBonusPercent = 5,
            completionTitle = "旧港潮汐簿补全：永久攻击 +4，速度 +3，全境金币 +5%"
        ),
        ChronicleQuestDef(
            id = "chron_starlit_ridge",
            regionId = RegionId.STARLIT_RIDGE,
            title = "天文台赤道仪残铭",
            description = "追寻废弃观测站观测员停笔的那一夜究竟看见了怎样的云海流星。",
            stages = listOf(
                ChronicleStageDef(1, "序章 · 碎晶目镜", 3, 4, "重新磨亮裂纹铜镜后，山脊废墟上的旧坐标再度清晰。"),
                ChronicleStageDef(2, "二章 · 陨铁日历盘", 6, 10, "把山顶散落的金属月相牌逐一嵌回赤道铜环底座。"),
                ChronicleStageDef(3, "终章 · 穹顶星象录", 9, 18, "天文台尘封百年的主镜微微转动，指明通往浮岛的上升暖流。")
            ),
            rewardStats = Stats(attack = 5, defense = 4),
            rewardXpBonusPercent = 5,
            completionTitle = "星台星象录复原：永久攻击 +5，防护 +4，全境经验 +5%"
        ),
        ChronicleQuestDef(
            id = "chron_cloud_sanctuary",
            regionId = RegionId.CLOUD_SANCTUARY,
            title = "空中温室育梦册",
            description = "顺着琳寻得的半本育花笔记，重现失传的夜光梦绒花与风琴和弦共鸣术。",
            stages = listOf(
                ChronicleStageDef(1, "序章 · 梦绒芽样本", 3, 4, "采收三片不同海拔温室培育的绒花花萼对照纹路。"),
                ChronicleStageDef(2, "二章 · 风琴音孔图", 6, 10, "按照花房图纸疏通石柱共鸣孔，让微风吹响柔和歌调。"),
                ChronicleStageDef(3, "终章 · 浮岛繁花谱", 9, 20, "空中温室满架的星绒花同时亮起柔光，花蜜香气直抵营地。")
            ),
            rewardStats = Stats(maxHp = 28, speed = 3),
            rewardMaterialBonusPercent = 6,
            completionTitle = "浮岛育梦册编毕：永久生命 +28，速度 +3，全境材料 +6%"
        ),
        ChronicleQuestDef(
            id = "chron_dream_sea",
            regionId = RegionId.DREAM_SEA,
            title = "渊鲸领航灯塔志",
            description = "解读沉星海底灯塔刻痕与渊鲸吟唱之间的呼应，标出归梦彼岸之海门。",
            stages = listOf(
                ChronicleStageDef(1, "序章 · 星砂沉船牌", 3, 4, "辨认水下沉船船首镌刻的领航灯记与水手印纹。"),
                ChronicleStageDef(2, "二章 · 渊塔折射阵", 6, 12, "调准司灯渊塔顶层七枚海晶棱镜的夜光焦距。"),
                ChronicleStageDef(3, "终章 · 沉星远航志", 9, 22, "司灯渊鲸长鸣一声潜入星渊，海面亮起指向星拱的航标。")
            ),
            rewardStats = Stats(attack = 7, defense = 5),
            rewardGoldBonusPercent = 6,
            completionTitle = "沉星灯塔志定稿：永久攻击 +7，防护 +5，全境金币 +6%"
        ),
        ChronicleQuestDef(
            id = "chron_astral_arch",
            regionId = RegionId.ASTRAL_ARCH,
            title = "千夜守时总录",
            description = "同岑一起清点星穹日晷圣殿中封存的琥珀怀表，集齐横跨六境的安眠誓约。",
            stages = listOf(
                ChronicleStageDef(1, "序章 · 琥珀刻柱目录", 3, 4, "登记外廊九根琥珀柱中最早的旅人手记年份。"),
                ChronicleStageDef(2, "二章 · 逆时钟灵铭文", 6, 12, "破译千钟共振厅正中那面双指针逆行大摆钟的刻度意义。"),
                ChronicleStageDef(3, "终章 · 归梦总典", 9, 20, "六境星图徽印尽数亮起，归梦星拱将整夜星辉稳稳留给好眠之人。")
            ),
            rewardStats = Stats(maxHp = 40, attack = 8, defense = 6, speed = 4),
            rewardGoldBonusPercent = 5,
            rewardXpBonusPercent = 8,
            rewardMaterialBonusPercent = 5,
            completionTitle = "千夜守时总录圆满：永久生命 +40，攻击 +8，防护 +6，速度 +4，全产出提升"
        )
    )

    val astralNodes: List<AstralNodeDef> = listOf(
        // Bastion branch
        AstralNodeDef("astral_bastion_1", BuildArchetype.BASTION, 1, "铁壁初星", "永久生命 +16，防护 +3。", Stats(maxHp = 16, defense = 3)),
        AstralNodeDef("astral_bastion_2", BuildArchetype.BASTION, 2, "穹盾中枢", "永久生命 +28，防护 +5。", Stats(maxHp = 28, defense = 5)),
        AstralNodeDef("astral_bastion_3", BuildArchetype.BASTION, 3, "不倒圣拱", "永久生命 +44，防护 +8，战斗伤害 +6%。", Stats(maxHp = 44, defense = 8), combatDamagePercent = 6),
        // Gale branch
        AstralNodeDef("astral_gale_1", BuildArchetype.GALE, 1, "掠风前哨", "永久攻击 +4，速度 +2。", Stats(attack = 4, speed = 2)),
        AstralNodeDef("astral_gale_2", BuildArchetype.GALE, 2, "流矢双星", "永久攻击 +6，速度 +4。", Stats(attack = 6, speed = 4)),
        AstralNodeDef("astral_gale_3", BuildArchetype.GALE, 3, "破晓时翼", "永久攻击 +9，速度 +5，战斗伤害 +8%。", Stats(attack = 9, speed = 5), combatDamagePercent = 8),
        // Dreamweave branch
        AstralNodeDef("astral_dream_1", BuildArchetype.DREAMWEAVE, 1, "微光苗圃", "永久生命 +14，夜间金币产出 +4%。", Stats(maxHp = 14), goldBonusPercent = 4),
        AstralNodeDef("astral_dream_2", BuildArchetype.DREAMWEAVE, 2, "露息晶枝", "永久生命 +24，全材料产出 +6%。", Stats(maxHp = 24), materialBonusPercent = 6),
        AstralNodeDef("astral_dream_3", BuildArchetype.DREAMWEAVE, 3, "星花常盛", "永久生命 +36，金币 +8%，经验 +8%。", Stats(maxHp = 36), goldBonusPercent = 8, xpBonusPercent = 8),
        // Scholar branch
        AstralNodeDef("astral_scholar_1", BuildArchetype.SCHOLAR, 1, "夜烛手记", "永久攻击 +3，经验产出 +4%。", Stats(attack = 3), xpBonusPercent = 4),
        AstralNodeDef("astral_scholar_2", BuildArchetype.SCHOLAR, 2, "星盘校准", "永久防护 +5，经验 +6%，材料 +5%。", Stats(defense = 5), xpBonusPercent = 6, materialBonusPercent = 5),
        AstralNodeDef("astral_scholar_3", BuildArchetype.SCHOLAR, 3, "博物星冠", "永久攻击 +7，防护 +6，经验 +8%，战斗伤害 +6%。", Stats(attack = 7, defense = 6), xpBonusPercent = 8, combatDamagePercent = 6)
    )

    val transmuteRecipes: List<TransmuteRecipeDef> = listOf(
        TransmuteRecipeDef(
            id = "tx_wood_to_ore",
            title = "松炭凝铸潮汐矿砂",
            description = "花房 Lv.1 启用：以盈余雾林松木配合温和焙烧析出 10 份潮汐矿砂。",
            requiredConservatoryLevel = 1,
            inputGold = 60L,
            inputMaterialId = RegionId.MISTWOOD.materialId,
            inputMaterialAmount = 24L,
            outputMaterialId = RegionId.TIDAL_FLAT.materialId,
            outputMaterialAmount = 10L
        ),
        TransmuteRecipeDef(
            id = "tx_ore_to_crystal",
            title = "潮汐砂萃星辉碎晶",
            description = "花房 Lv.2 启用：用灵植酸液洗去杂盐，浓缩出 10 份星辉碎晶。",
            requiredConservatoryLevel = 2,
            inputGold = 140L,
            inputMaterialId = RegionId.TIDAL_FLAT.materialId,
            inputMaterialAmount = 22L,
            outputMaterialId = RegionId.STARLIT_RIDGE.materialId,
            outputMaterialAmount = 10L
        ),
        TransmuteRecipeDef(
            id = "tx_crystal_to_silk",
            title = "星晶滋育云栖灵绒",
            description = "花房 Lv.3 启用：在温室暖架用星晶粉末供养梦绒幼株，收获 10 份云栖灵绒。",
            requiredConservatoryLevel = 3,
            inputGold = 240L,
            inputMaterialId = RegionId.STARLIT_RIDGE.materialId,
            inputMaterialAmount = 20L,
            outputMaterialId = RegionId.CLOUD_SANCTUARY.materialId,
            outputMaterialAmount = 10L
        ),
        TransmuteRecipeDef(
            id = "tx_silk_to_pearl",
            title = "云绒凝露沉星梦珠",
            description = "花房 Lv.4 启用：用夜露浸泡灵绒结出珠胎，淬得 10 份沉星梦珠。",
            requiredConservatoryLevel = 4,
            inputGold = 360L,
            inputMaterialId = RegionId.CLOUD_SANCTUARY.materialId,
            inputMaterialAmount = 18L,
            outputMaterialId = RegionId.DREAM_SEA.materialId,
            outputMaterialAmount = 10L
        ),
        TransmuteRecipeDef(
            id = "tx_pearl_to_amber",
            title = "梦珠封珀成星拱琥珀",
            description = "花房 Lv.5 启用：在琥珀培养槽内慢火固化梦珠精华，得到 10 份星拱琥珀。",
            requiredConservatoryLevel = 5,
            inputGold = 520L,
            inputMaterialId = RegionId.DREAM_SEA.materialId,
            inputMaterialAmount = 16L,
            outputMaterialId = RegionId.ASTRAL_ARCH.materialId,
            outputMaterialAmount = 10L
        ),
        TransmuteRecipeDef(
            id = "tx_amber_to_mark",
            title = "星拱琥珀铭刻首领印记",
            description = "花房 Lv.5 启用：由工坊与花房联合精炼 20 份星拱琥珀，熔铸 1 枚首领印记。",
            requiredConservatoryLevel = 5,
            inputGold = 900L,
            inputMaterialId = RegionId.ASTRAL_ARCH.materialId,
            inputMaterialAmount = 20L,
            outputBossMarks = 1L
        )
    )

    val commissions: List<CommissionDef> = listOf(
        CommissionDef(
            id = "comm_mistwood_timber",
            title = "林道修灯木料采集单",
            regionId = RegionId.MISTWOOD,
            description = "于雾林稳定完成至少 12 个节点（1 小时以上），为营地补给松木灯柱材料。",
            requiredMinNodes = 12,
            rewardGold = 60L,
            rewardXp = 80L,
            rewardMaterialRegion = RegionId.MISTWOOD,
            rewardMaterialAmount = 5L,
            rewardAffinity = 1
        ),
        CommissionDef(
            id = "comm_tidal_beacons",
            title = "旧港潮表校准协理",
            regionId = RegionId.TIDAL_FLAT,
            description = "于潮汐滩完成至少 24 个节点（2 小时以上），带回潮汐矿砂与水手观测录。",
            requiredMinNodes = 24,
            rewardGold = 180L,
            rewardXp = 220L,
            rewardMaterialRegion = RegionId.TIDAL_FLAT,
            rewardMaterialAmount = 8L,
            rewardAffinity = 2
        ),
        CommissionDef(
            id = "comm_starlit_lenses",
            title = "天文台微光晶坯征集",
            regionId = RegionId.STARLIT_RIDGE,
            description = "于星陨山脊完成至少 24 个节点，提供研磨望远镜片所需的纯净星晶。",
            requiredMinNodes = 24,
            rewardGold = 320L,
            rewardXp = 380L,
            rewardMaterialRegion = RegionId.STARLIT_RIDGE,
            rewardMaterialAmount = 10L,
            rewardAffinity = 2
        ),
        CommissionDef(
            id = "comm_cloud_seedlings",
            title = "温室夜光花种扩繁",
            regionId = RegionId.CLOUD_SANCTUARY,
            description = "于云栖浮岛完成至少 36 个节点（3 小时以上），收集鲜活梦绒花株移入花房。",
            requiredMinNodes = 36,
            rewardGold = 520L,
            rewardXp = 620L,
            rewardMaterialRegion = RegionId.CLOUD_SANCTUARY,
            rewardMaterialAmount = 12L,
            rewardBossMarks = 1L,
            rewardAffinity = 2
        ),
        CommissionDef(
            id = "comm_dream_sea_charts",
            title = "深梦海灯塔补光巡检",
            regionId = RegionId.DREAM_SEA,
            description = "于沉星梦海完成至少 36 个节点，带回校准灯塔折射阵的渊海梦珠。",
            requiredMinNodes = 36,
            rewardGold = 780L,
            rewardXp = 920L,
            rewardMaterialRegion = RegionId.DREAM_SEA,
            rewardMaterialAmount = 14L,
            rewardBossMarks = 1L,
            rewardAffinity = 3
        ),
        CommissionDef(
            id = "comm_astral_pendulums",
            title = "星拱怀表发条整备",
            regionId = RegionId.ASTRAL_ARCH,
            description = "于归梦星拱完成至少 36 个节点，协助岑维护归梦钟楼的星珀摆轮。",
            requiredMinNodes = 36,
            rewardGold = 1100L,
            rewardXp = 1350L,
            rewardMaterialRegion = RegionId.ASTRAL_ARCH,
            rewardMaterialAmount = 16L,
            rewardBossMarks = 1L,
            rewardAffinity = 3
        )
    )

    private val routesById: Map<String, RouteDef> = routes.associateBy { it.id }
    private val enemiesById: Map<String, EnemyDef> = enemies.associateBy { it.id }
    private val equipmentById: Map<String, EquipmentDef> = equipmentDefs.associateBy { it.id }
    private val passivesById: Map<String, PassiveDef> = passives.associateBy { it.id }
    private val keepsakesById: Map<String, KeepsakeDef> = keepsakes.associateBy { it.id }
    private val companionsById: Map<String, CompanionStoryDef> = companionStories.associateBy { it.id }
    private val teaBlendsById: Map<String, TeaBlendDef> = teaBlends.associateBy { it.id }
    private val souvenirsById: Map<String, SouvenirDef> = souvenirs.associateBy { it.id }
    private val morningEchoesById: Map<String, MorningEchoDef> = morningEchoes.associateBy { it.id }
    private val chroniclesById: Map<String, ChronicleQuestDef> = chronicles.associateBy { it.id }
    private val astralNodesById: Map<String, AstralNodeDef> = astralNodes.associateBy { it.id }
    private val transmuteRecipesById: Map<String, TransmuteRecipeDef> = transmuteRecipes.associateBy { it.id }
    private val commissionsById: Map<String, CommissionDef> = commissions.associateBy { it.id }

    fun routeById(id: String): RouteDef? = routesById[id]
    fun enemyById(id: String): EnemyDef? = enemiesById[id]
    fun equipmentById(id: String): EquipmentDef? = equipmentById[id]
    fun passiveById(id: String): PassiveDef? = passivesById[id]
    fun keepsakeById(id: String): KeepsakeDef? = keepsakesById[id]
    fun companionById(id: String): CompanionStoryDef? = companionsById[id]
    fun teaBlendById(id: String): TeaBlendDef? = teaBlendsById[id]
    fun souvenirById(id: String): SouvenirDef? = souvenirsById[id]
    fun morningEchoById(id: String): MorningEchoDef? = morningEchoesById[id]
    fun chronicleById(id: String): ChronicleQuestDef? = chroniclesById[id]
    fun chronicleForRegion(regionId: RegionId): ChronicleQuestDef? = chronicles.firstOrNull { it.regionId == regionId }
    fun astralNodeById(id: String): AstralNodeDef? = astralNodesById[id]
    fun transmuteRecipeById(id: String): TransmuteRecipeDef? = transmuteRecipesById[id]
    fun commissionById(id: String): CommissionDef? = commissionsById[id]

    fun nightPhenomenonForUtcMs(utcMs: Long): NightPhenomenon {
        val dayIndex = utcMs.coerceAtLeast(0L) / 86_400_000L
        return nightPhenomena[(dayIndex % nightPhenomena.size).toInt()]
    }

    fun equipmentPoolForRegion(regionId: RegionId): List<EquipmentDef> =
        equipmentDefs.filter { it.regionId == regionId && !it.craftOnly }

    fun eventsForRegion(regionId: RegionId): List<EventTemplate> =
        eventTemplates.filter { it.regionId == regionId }

    fun souvenirsForRegion(regionId: RegionId): List<SouvenirDef> =
        souvenirs.filter { it.regionId == regionId }

    fun morningEchoesForRegion(regionId: RegionId): List<MorningEchoDef> =
        morningEchoes.filter { it.regionId == regionId }

    fun facilityRequiredExploreHours(targetLevel: Int): Int = listOf(0, 24, 64, 112, 168)[targetLevel - 1]

    fun facilityRequiredExploreHours(type: FacilityType, targetLevel: Int): Int =
        when (type) {
            FacilityType.TEA_HEARTH -> listOf(32, 80, 136, 192, 248)[targetLevel - 1]
            FacilityType.CONSERVATORY -> listOf(40, 128, 176, 248, 312)[targetLevel - 1]
            else -> facilityRequiredExploreHours(targetLevel)
        }

    fun facilityUpgradeCost(type: FacilityType, targetLevel: Int): Pair<Long, Pair<String, Long>> {
        require(targetLevel in 1..5) { "Facility targetLevel must be 1..5" }
        val goldCosts = when (type) {
            FacilityType.TEA_HEARTH -> listOf(180L, 620L, 1450L, 2800L, 5200L)
            FacilityType.CONSERVATORY -> listOf(120L, 420L, 960L, 1850L, 3400L)
            else -> listOf(100L, 650L, 1600L, 3200L, 6000L)
        }
        val materialCosts = when (type) {
            FacilityType.TEA_HEARTH -> listOf(12L, 48L, 105L, 185L, 280L)
            FacilityType.CONSERVATORY -> listOf(12L, 24L, 36L, 42L, 40L)
            else -> listOf(10L, 60L, 130L, 230L, 360L)
        }
        val materialId = when {
            type == FacilityType.TEA_HEARTH && targetLevel >= 5 -> RegionId.CLOUD_SANCTUARY.materialId
            type == FacilityType.CONSERVATORY && targetLevel >= 5 -> RegionId.CLOUD_SANCTUARY.materialId
            type == FacilityType.CONSERVATORY && targetLevel == 4 -> RegionId.STARLIT_RIDGE.materialId
            targetLevel <= 2 -> RegionId.MISTWOOD.materialId
            targetLevel <= 4 -> RegionId.TIDAL_FLAT.materialId
            else -> RegionId.STARLIT_RIDGE.materialId
        }
        val gold = goldCosts[targetLevel - 1]
        val mat = materialCosts[targetLevel - 1]
        return gold to (materialId to mat)
    }

    fun equipmentUpgradeCost(def: EquipmentDef, targetLevel: Int): Pair<Long, Pair<String, Long>> {
        require(targetLevel in 2..EquipmentInstance.MAX_LEVEL)
        val factor = when (targetLevel) {
            2 -> 1.0
            3 -> 1.5
            4 -> 2.25
            5 -> 3.38
            6 -> 4.6
            else -> 6.2
        }
        val baseGold = (def.craftGoldCost * 0.6)
        val baseMat = (def.craftMaterialCost * 0.5)
        val gold = kotlin.math.round(baseGold * factor).toLong().coerceAtLeast(30L)
        val mat = kotlin.math.round(baseMat * factor).toLong().coerceAtLeast(3L)
        return gold to (def.craftMaterialId to mat)
    }

    fun effectiveEquipmentStats(def: EquipmentDef, level: Int, workshopLevel: Int): Stats {
        val lvlScale = 1.0 + (level - 1) * 0.18
        val workshopBonus = 1.0 + workshopLevel * 0.05
        val mult = lvlScale * workshopBonus
        return Stats(
            maxHp = kotlin.math.round(def.baseStats.maxHp * mult).toInt(),
            attack = kotlin.math.round(def.baseStats.attack * mult).toInt(),
            defense = kotlin.math.round(def.baseStats.defense * mult).toInt(),
            speed = def.baseStats.speed + ((level - 1) / 2)
        )
    }
}
