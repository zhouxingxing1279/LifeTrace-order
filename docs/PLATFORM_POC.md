# 平台真机 PoC 指南

EPIC-29 的平台差异必须通过真实 Android WebView 登录环境验证，禁止根据网页猜测私有接口。

## PoC 顺序

1. 美团：当前订单、活跃外卖状态、通知触发；
2. 京东：历史分页、订单详情、物流；
3. 淘宝 / 天猫：历史分页、物流、退款 / 售后；
4. 拼多多：验证同一 Adapter 契约。

## 每个平台必须确认

- WebView 关闭 / 重开后登录态是否仍存在；
- 最稳定 Fetch Mode：`NATIVE_HTTP` / `WEBVIEW_FETCH` / `DOM_FALLBACK`；
- 历史订单分页 cursor 和停止边界；
- 活跃订单单独刷新方式；
- 登录失效和人机验证的可识别信号；
- 限流、访问拒绝、解析失败如何分类；
- 哪些字段可以稳定标准化到 `UnifiedOrder`。

## 安全规则

- 不记录 Cookie / Token 的值；
- 不把 WebView Profile 上传云端；
- 不自动破解滑块、验证码或短信验证；
- 不读取第三方 App 私有数据库；
- 不使用 Accessibility 自动点击作为核心抓取方案；
- 通知正文只用于 refresh signal，不作为订单真值。

## 调试建议

Debug 构建开启 WebView DevTools，Release 构建自动关闭。可通过 Chrome WebView 调试观察用户本人正常登录后页面发起的网络请求；确认数据源后，再将实现封装到对应平台 Adapter，禁止把平台字段泄漏到 UI / Room 公共模型之外。
