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

构建与模拟器按顺序运行。构建内存上限 4 GiB，使用单 worker；模拟器内存上限 5 GiB，包含系统与图形开销，以只读模式运行且不保存快照。

调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

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

发布前递增 `app/build.gradle.kts` 中的版本号，验证签名和安装流程。GitHub Release 上传 APK 与 SHA-256 校验文件。后续更新使用相同发布密钥；从不同签名的调试包切换时，先导出存档，再卸载并安装发布包。

验证完成后停止测试服务，清理临时设备、测试数据和构建产物；交付 APK 保存在忽略提交的 `dist/`。
