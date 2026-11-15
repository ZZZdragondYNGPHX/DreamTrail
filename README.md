# 梦途 · DreamTrail

一款 Android 离线睡眠挂机 RPG。睡前选好路线、装备和同行伙伴，醒来收取旅途战报，逐步探索世界、修葺营地并培养旅人。

关屏或关闭应用后，旅程仍按离线时间结算。游戏进度保存在设备上，可通过 JSON 存档备份和转移。

## 下载与游玩

从 [Releases](https://github.com/ZZZdragondYNGPHX/DreamTrail/releases) 下载 APK，支持 Android 8.0 及以上。

1. 在「旅途」选择路线和策略，点击出发。
2. 放下手机休息；守夜页提供极暗模式和屏幕固定。
3. 醒来结束守夜，查看装备、材料、发现与成长。
4. 在「角色」「伙伴」「营地」「图鉴」整理装备、安排伙伴、建设设施和查看探索记录。

首次旅程提供快速体验奖励。出发后 5 分钟内可以撤销，撤销不发放奖励。每次最多结算 12 小时，滚动 24 小时内最多获得 8 小时有效探索收益。

「设置与存档」支持导出、导入和恢复最近 3 份自动快照。当前版本使用手动结算，不需要手环或健康权限。

## 游戏内容

- 6 个区域、18 条路线，36 种普通魔物、6 位机制首领及精英遭遇。
- 30 件装备原型、20 种词条、4 大流派共鸣、14 个被动和旅人星图。
- 5 座营地设施，包含茶饮、装备打造、材料精炼与营地委托。
- 5 位伙伴及羁绊成长、区域秘闻、风物收藏、晨间奇遇和深梦信标挑战。
- 3 栏行装预设，旅人最高 Lv.50，装备最高强化至 Lv.7。

## 开发

技术栈：Kotlin、Jetpack Compose、Room；游戏规则为纯 Kotlin JVM 模块。

```text
app/       Android 界面、持久化、系统时钟与存档文件交互
core/      游戏内容、战斗、成长、离线结算与单元测试
docs/      架构与构建说明
gradle/    Gradle Wrapper 与依赖版本
tools/     本地 Android 构建与模拟器服务
```

使用 JDK 21 和 Android SDK 36：

```bash
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Linux 下在 Codex 中开发时，通过独立 systemd 服务运行 Android 任务：

```bash
bash tools/android-service.sh build
```

详细说明见 [构建与发布](docs/development.md) 和 [架构](docs/architecture.md)。

## 许可

采用 [PolyForm Noncommercial 1.0.0](LICENSE)，允许非商业使用、修改和分发，禁止商业用途。第三方依赖保留各自许可证。
