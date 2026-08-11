# EPIC-29 实施状态

## 已实现

- [x] 独立 Android App 工程
- [x] Kotlin + Jetpack Compose
- [x] Room 订单 / 商品 / 履约 / 物流事件 / 退款 / 同步状态 / 平台账户表
- [x] `platform + accountId + platformOrderId` 幂等主键
- [x] PlatformAdapter / FetchMode / 统一错误模型
- [x] WebView 本地登录容器
- [x] Debug WebView 调试、Release 关闭
- [x] Cookie / WebStorage 仅 Android 本地保存
- [x] 首次 1 月 / 1 年 backfill 引擎与逐页 checkpoint
- [x] 前台进入立即刷新
- [x] 普通订单前台 5 分钟调度
- [x] App 后台停止主动轮询
- [x] NotificationListenerService 刷新信号
- [x] 通知关键词过滤，不把通知作为订单真值
- [x] 活跃外卖 15 秒实时状态机
- [x] 终态 / 无活跃订单退出实时模式
- [x] Android lint / unit test / assemble CI

## 需要真机 PoC 后固化

- [ ] 美团订单 Fetch Mode 与 normalizer
- [ ] 美团外卖实时数据源
- [ ] 京东历史分页 / 物流 Adapter
- [ ] 淘宝 / 天猫历史分页 / 物流 / 退款 Adapter
- [ ] 拼多多 Adapter
- [ ] LifeTrace Cloud Sync Outbox 对接

这些项目不能通过猜测平台私有接口完成，必须在用户本人正常登录的 Android WebView 环境中验证后再实现。
