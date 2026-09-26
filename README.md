# 末世包租公 V1.2

Android 末世社区经营小游戏。

## V1.2
- 全新图形化社区地图：9 个房间格子、状态图标、社区公告卡片。
- 重做顶部资源 HUD：资金、电力、水、食物、天数、系统等级。
- 租客卡片、系统等级卡片、进度条和统一视觉风格。
- LazyVerticalGrid / LazyColumn 使用稳定 key 与 contentType，减少 Compose 不必要重组。
- 保留 V1.1 自动存档，并将存档命名空间升级到 V1.2。
- 新增 Release 签名配置，可直接生成签名 APK。

## 签名说明
本 ZIP 内置的是“测试/演示签名” `release.keystore`，用于方便安装和测试。
- alias: `apartmentlord`
- store/key password: `ApartmentLordV12!2026`

**不要把这个测试密钥用于正式上架 Google Play 或公开发行版本。正式版本请换成你自己保管的私有 keystore。**

## GitHub Actions
工作流会同时构建 Debug APK 和 Release APK，并上传为 Actions Artifact。
