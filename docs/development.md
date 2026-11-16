# 构建与发布

## 环境

- JDK 21。
- Android SDK：Platform 36 与 Build Tools 35.0.0 或以上。
- 使用 `ANDROID_HOME` 或本地 `local.properties` 的 `sdk.dir` 指定 SDK。

## 验证

```bash
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

核心测试覆盖内容引用、离线预算、战斗、成长和存档。应用测试使用 Robolectric 与 Room，覆盖持久化、重复操作、恢复和 ViewModel 状态。

Linux 的 Android 构建与模拟器通过 systemd 用户服务运行。设置 `JAVA_HOME` 和 `ANDROID_HOME` 后执行：

```bash
bash tools/android-service.sh build
bash tools/android-service.sh status
bash tools/android-service.sh logs build
bash tools/android-service.sh emulator YourAvdName
bash tools/android-service.sh stop
```

构建与模拟器按顺序运行。专用验证可设置 `ANDROID_AVD_HOME`、`ANDROID_EMULATOR_HOME` 与 `ANDROID_ADB_SERVER_PORT`，把设备磁盘和 adb 服务隔离到可清理的测试目录与端口。构建内存上限 4 GiB，使用单 worker；模拟器内存上限 5 GiB，包含系统与图形开销，以只读模式运行且不保存快照。

调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

UI 回归测试位于 `app/src/androidTest/`，覆盖标签状态恢复、详情弹窗滚动、热茶选择、夜间守夜结算、白昼专注与声景设置。新增待办添加/完成/清理、呼吸引导和手账入口另需在真实窗口检查。测试会替换测试应用的存档，只在本项目的专用模拟器中运行。先通过构建服务生成两个 APK，构建结束后再启动模拟器并安装、执行测试：

```bash
bash tools/android-service.sh build :app:assembleDebug :app:assembleDebugAndroidTest
# 构建服务结束后执行
bash tools/android-service.sh emulator YourTestAvd -port 5580
adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5580 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5580 shell am instrument -w com.dreamtrail.app.test/androidx.test.runner.AndroidJUnitRunner
```

同时验证标准字体和系统大字体；小屏回归可设置 `wm size 640x1136`、`wm density 320`、`settings put system font_scale 1.5`。验证结束后停止服务并删除专用测试设备及数据。

## 签名发布

创建发布密钥，将签名配置保存在仓库外的 `release.properties`：

```properties
storeFile=/absolute/path/to/release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=dreamtrail
keyPassword=YOUR_KEY_PASSWORD
```

通过 `DREAMTRAIL_SIGNING_PROPERTIES` 指定该文件：

```bash
export DREAMTRAIL_SIGNING_PROPERTIES=/absolute/path/to/release.properties
bash tools/android-service.sh build :core:test :app:testDebugUnitTest :app:assembleRelease :app:lintRelease
```

普通构建不需要签名配置；未指定配置时，Release APK 为未签名文件。签名 APK 位于 `app/build/outputs/apk/release/app-release.apk`。

发布前递增 `app/build.gradle.kts` 中的版本号，验证签名和安装流程。GitHub Release 上传 APK 与 SHA-256 校验文件。本版只支持当前数据格式；测试和发布验证使用全新安装。设备若装有旧开发版，应卸载后安装，本版不导入旧版存档。后续正式版本使用相同发布密钥。

验证完成后停止测试服务，清理临时设备、测试数据和构建产物；交付 APK 保存在忽略提交的 `dist/`。
