# 空间链接 (SpaceLink) 代码移除说明

备份日期：2026-08-27
备份目的：移除 "插入空间链接" (SpaceLink) 相关 UI、展示、编辑代码，保留 "空间文件" (SpaceFile) 相关代码不动。
备份目录路径：`G:\retimeboxlite\backup_spacelink_remove_20260827\`

## 备份文件列表

| 源路径 | 备份名 | 移除的 SpaceLink 代码说明 |
|---|---|---|
| `app/src/main/java/com/retimebox/lite/ui/record/RecordEditorScreen.kt` | `RecordEditorScreen.kt` | ① 工具栏 "插入空间链接" 按钮（注释的 `Icons.Filled.Link`）；② `showSpaceLinkDialog` / `editingSpaceLinkId` / `spaceLinkType` / `spaceLinkUrl` / `spaceLinkName` / `spaceLinkThumbnail` / `spaceLinkThumbnailPickerLauncher` / `referencedSpaceLinks` 状态变量；③ 已添加内容中 SpaceLink 列表渲染及 `SpaceLinkPreviewCard` 回调；④ SpaceLink 新增/编辑 AlertDialog；⑤ `SpaceLinkPreviewCard` composable；⑥ SpaceLink 新增 / 更新 / 删除按钮的 onclick 逻辑；⑦ 相关 import。 |
| `app/src/main/java/com/retimebox/lite/ui/record/RecordDetailScreen.kt` | `RecordDetailScreen.kt` | ① 笔记内容 markdown 渲染中 `[spacelink]` 短代码匹配分支（用 `spaceLinks` state 调 `SpaceLinkCard`）；② `SpaceLinkCard` composable 定义（预览页空间链接卡片）；③ `onOpenSpaceLink: (Long) -> Unit` 参数；④ 从 `recordDetailViewModel.spaceLinks` 收集 state；⑤ 相关 import。 |
| `app/src/main/java/com/retimebox/lite/ui/space/SpaceScreen.kt` | `SpaceScreen.kt` | ① `onOpenSpaceLink: (Long) -> Unit` 参数；② `SpaceEntryType.LINK` 条目渲染与点击分支；③ 批量编辑 `LINK` 类型 Snackbar 分支；④ 从 viewModel `selectedEntries` 过滤 LINK；⑤ 相关 import。**注意：空间文件条目继续使用 FILE 分支，完整保留。** |
| `app/src/main/java/com/retimebox/lite/viewmodel/SpaceViewModel.kt` | `SpaceViewModel.kt` | ① `spaceLinkRepository` 注入；② `itemsInFolder` combine 中 SpaceLink 条目查询 (`observeByFolder` / `observeAll`) 与 `SpaceEntry.fromLink(...)` 构造；③ `selectedEntries` 中 LINK 处理；④ `batchDelete` / `batchMoveToFolder` 针对 LINK IDs 的操作；⑤ `getSpaceLinkById` / `renameFolder` / `updateFolderName` 对 SpaceLink 的更新；⑥ Root 目录 SpaceLink 相关逻辑。**注意：SpaceFile 所有逻辑完整保留。** |
| `app/src/main/java/com/retimebox/lite/viewmodel/RecordEditorViewModel.kt` | `RecordEditorViewModel.kt` | ① `_referencedSpaceLinks` / `referencedSpaceLinks` StateFlow；② `loadRecord` 后 `RefType.SPACE_LINK` 分支加载空间链接；③ `addSpaceLinkReference` / `updateSpaceLinkReference` 方法；④ `removeReference` 中 `RefType.SPACE_LINK` 分支（含 txt 文件删除）；⑤ `save()` 中 SPACE_LINK 保存分支。**注意：SPACE_FILE 方法完整保留。** |
| `app/src/main/java/com/retimebox/lite/viewmodel/RecordDetailViewModel.kt` | `RecordDetailViewModel.kt` | ① `_spaceLinks` / `spaceLinks` StateFlow；② `loadReferences` 中 `RefType.SPACE_LINK` 分支；③ 相关 import。**注意：SPACE_FILE 分支完整保留。** |
| `app/src/main/java/com/retimebox/lite/util/RichEditorHelper.kt` | `RichEditorHelper.kt` | ① `SPACE_LINK_PATTERN` 常量；② `SpaceLinkInfo` data class；③ `createSpaceLinkShortcode` 方法；④ `extractSpaceLinkInfo` 方法；⑤ `updateSpaceLinkShortcode` 方法；⑥ `removeReference` 中 `RefType.SPACE_LINK` 分支（正则替换 `[spacelink]...`）。**注意：SpaceFile 相关 shortcode 函数完整保留。** |
| `app/src/main/java/com/retimebox/lite/ui/home/HomeScreen.kt` | `HomeScreen.kt` | ① `onOpenSpaceLink` 回调定义（用于 SpaceScreen / SpaceFileEntry 导航跳转 Viewer）；② NavGraph 传入 SpaceScreen 的 onOpenSpaceLink lambda。**注意：onOpenSpaceFile 完整保留（空间文件仍需跳转 Viewer）。** |

## 注意事项

**以下部分 "不要删除"（保留数据库与仓库实体，不做 UI 展示）：**

- `data/local/entity/SpaceLinkItem.kt` 实体类
- `data/local/entity/SpaceType.kt` 枚举
- `data/local/dao/SpaceLinkItemDao.kt` DAO
- `data/repository/SpaceLinkRepository.kt` Repository
- `AppDatabase.kt` 中 SpaceLinkItemDao 注册
- `data/model/SpaceEntryType.kt` 的 `LINK` 枚举值（如果与 FILE 共用但不再用 LINK 可保留，不影响）

原因：现有笔记中的 `[spacelink]` 短代码可能仍在数据库和已生成的 .md 文件中存在。保留实体层与 DAO 可以避免旧数据读取崩溃。如果后续确认清理，再单独做数据迁移。

**NavGraph.kt 改动：**

- SpaceLinkViewer 路由若存在且仅用于 spaceLinks，可移除。但注意：若 SpaceFileViewer 与 SpaceLinkViewer 为同一页面（共用 Activity/Route），则不删除，仅删掉跳转 SpaceLink 的分支。

## 恢复方式

直接把备份目录里的 .kt 文件覆盖回源文件，即可恢复 SpaceLink 全部功能（工具栏插入按钮 / 预览页卡片 / 空间页 LINK 条目 / 批量编辑 / 数据库同步）。
