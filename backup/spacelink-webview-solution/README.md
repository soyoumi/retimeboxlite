# 空间链接 WebView 方案 — 源码备份说明

## 概述
本目录备份了"空间链接 WebView 预览页面"的源码实现。该方案基于 Android 原生 WebView，用于在线浏览全景照片、全景视频、高斯泼溅（GSplat）等 3D 空间场景。

本方案强烈不建议使用，评估几乎很难成功！
经过很多次尝试，在线网页是全景图片时仍然是黑屏，如果是全景视频或高斯泼溅场景时则正常显示正常浏览。

## 包含文件

| 文件 | 说明 |
|---|---|
| `SpaceLinkWebView.kt` | 空间链接 WebView 页面主实现（Compose UI + AndroidView 桥接） |
| `SpaceLinkItem.kt` | Room 数据库实体类（空间链接数据模型） |
| `SpaceLinkItemDao.kt` | Room DAO 接口（空间链接数据访问） |
| `SpaceLinkRepository.kt` | 数据仓库层（业务逻辑封装） |

## 架构说明

### 数据模型
```kotlin
SpaceLinkItem(
    id: Long              // 主键
    spaceType: SpaceType  // 空间类型：全景照片/全景视频/GSplat
    webUrl: String        // 在线页面 URL
    name: String          // 链接名称
    thumbnailUrl: String? // 缩略图
    sourceType: SourceType // 来源类型
    bindRecordId: Long?   // 绑定记录 ID
    folderId: Long        // 文件夹 ID
    createTime: Long      // 创建时间
)
```

### WebView 渲染流程

1. **导航到空间链接页面** → 传入 `spaceLinkId`
2. **LaunchedEffect** 查询 `SpaceLinkRepository.findById()` 获取 `SpaceLinkItem`
3. **AndroidView.factory** 创建 `android.webkit.WebView`，配置：
   - `javaScriptEnabled = true` — 启用 JS
   - `domStorageEnabled = true` — 启用 DOM 存储
   - `mediaPlaybackRequiresUserGesture = false` — 媒体自动播放
   - `mixedContentMode = ALWAYS_ALLOW` — 允许混合内容
   - `loadWithOverviewMode + useWideViewPort` — 响应式布局
4. **AndroidView.update** 加载 URL：`webView.loadUrl(linkItem.webUrl)`
5. **onPageFinished** 后注入 `injectCanvasFix()` JS 修正 Canvas 尺寸
6. **1秒后再次注入** 确保异步加载的全景组件也能正确渲染

### 三种内容类型的渲染差异

| 类型 | 渲染方式 | Canvas 依赖 |
|---|---|---|
| GSplat | 持续 requestAnimationFrame 渲染 | 中等（持续渲染循环自动恢复） |
| 全景视频 | 视频纹理驱动渲染循环 | 中等（视频帧触发重绘） |
| 全景图片 | 一次性渲染静态图像 | 高（仅初始化时渲染一次） |

### Canvas Fix 机制

针对全景图片页面黑屏问题的修复策略：

1. `onPageFinished` 后延迟 300ms 注入 JS
2. JS 查找所有 `<canvas>` 元素
3. 检测 `canvas.width === 0 || canvas.height === 0` 的 Canvas
4. 从父元素或 `window.innerWidth/innerHeight` 获取正确尺寸
5. 修正 Canvas 尺寸并触发 `resize` 事件
6. 1秒后再次执行，覆盖异步初始化的全景组件

### 全屏支持

- TopAppBar 全屏按钮 → 锁定 `SCREEN_ORIENTATION_SENSOR_LANDSCAPE` + 沉浸式 UI
- 退出全屏恢复原方向和系统 UI
- `WebChromeClient.onShowCustomView/onHideCustomView` 支持 HTML5 全屏视频

## 已知限制

1. **全景图片黑屏**：部分全景图片网站初始化时 Canvas 尺寸为 0，WebGL 渲染一次后停止。`injectCanvasFix()` 尝试修正，但无法保证所有网站兼容。
2. **WebGL2 支持**：依赖设备 Chrome 版本。Android 原生 WebView 的 WebGL2 支持有限。
3. **硬件加速**：默认系统硬件加速，不支持显式 `LAYER_TYPE_HARDWARE`（会干扰 WebGL）。
4. **跨域限制**：iframe 嵌入的全景内容受 CSP 策略限制。

## 依赖

- `androidx.compose.ui:ui-viewinterop` — Compose ↔ AndroidView 桥接
- `android.webkit.WebView` — 原生 WebView
- `androidx.room:room-runtime` — Room 数据库
- `com.retimebox.lite` 项目中的 Repository/Entity 类

## 适用场景

- 在线全景照片/视频浏览（需要 WebGL2 的网页）
- 高斯泼溅（GSplat）3D 场景浏览
- 任何基于 WebGL 的交互式 3D 网页
