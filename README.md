# LifeTrace Order

LifeTrace 的独立 Android 订单采集端，对应主项目 **EPIC-29：购物订单聚合与 Android 本地采集**。

## 当前目标

- Android 本地完成购物平台登录与验证；
- Cookie / Token / WebView Profile 仅保留在设备本地；
- 订单标准化为 `UnifiedOrder` 后写入 Room；
- 首次历史回填支持断点续传；
- 后续以前台增量同步为主；
- 通知只作为刷新信号，不作为订单真值；
- 活跃外卖在 App 前台进入 10～20 秒级追踪；
- App 后台停止主动高频轮询。

## 技术栈

- Kotlin 2.3.21
- Jetpack Compose
- Room
- Coroutines / Flow
- Android WebView
- NotificationListenerService

构建基线：JDK 17、Gradle 8.13、Android Gradle Plugin 8.13.2、compileSdk 36。

## 目录

```text
app/src/main/java/com/lifetrace/order/
├── data/          Room 与 Repository
├── domain/        UnifiedOrder 与错误模型
├── platform/      平台 Adapter / WebView 会话
├── sync/          前台同步与实时外卖状态机
├── notification/  通知触发器
└── ui/            Compose UI / 平台登录 WebView
```

## 本地运行

1. 使用 Android Studio 打开仓库；
2. 安装 Android SDK 36；
3. 使用 JDK 17；
4. Sync Gradle；
5. 运行 `app` Debug 配置到 Android 8.0+ 设备或模拟器。

命令行环境已经安装 Gradle 8.13 时可执行：

```bash
gradle :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

## 安全边界

本仓库不会实现验证码绕过、滑块破解、Root 读取第三方 App 私有数据或 Accessibility 自动操作。平台登录态不进入 LifeTrace Cloud，也不得写入普通日志。

## 实施状态

当前版本先完成可运行 Android 基础设施和平台 PoC 容器。真实美团 / 京东 / 淘宝订单接口需要在 Android 真机完成正常登录后逐个平台验证 Fetch Mode（native-http / webview-fetch / dom-fallback），平台差异必须封装在 Adapter 内。
