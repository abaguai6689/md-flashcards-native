# 闪卡背诵 · 框架与实现详解

本文是给维护者看的完整说明：架构、数据模型、拆卡算法、每个页面的实现、图片/备份/更新链路、构建发布与已知取舍。  
面向使用者的简要说明见 [README.md](README.md)。

---

## 1. 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose（BOM 2024.12.01）+ Material 3 |
| 玻璃效果 | Haze 1.2.2（真实背景模糊，低版本回退 tint） |
| 导航 | Navigation Compose 2.8.5，`@Serializable` 类型安全路由 |
| 状态 | AndroidX ViewModel 2.8.7 + Kotlin Coroutines 1.9 + StateFlow |
| 序列化 | kotlinx.serialization JSON |
| 持久化 | 应用私有 `filesDir/state.json` + `filesDir/images/` |
| 文件选择 | Storage Access Framework（不需要存储权限） |
| 网络 | `HttpURLConnection`，仅用于 GitHub Release 更新检查 |
| 构建 | AGP 8.7.3 / Gradle 8.9 / JDK 17 / compileSdk 35 / minSdk 26 |
| CI | GitHub Actions：单测 → assembleDebug → artifact / Release |

应用只有一个 Activity，没有 Fragment、XML 布局、WebView、Room、Retrofit、Hilt。

---

## 2. 总体架构

```
┌──────────────────────────── UI 层 ────────────────────────────┐
│ MainActivity.setContent { MdFlashcardsApp() }                  │
│   MdFlashcardsTheme（动态色 / 阅读设置）                        │
│   CompositionLocalProvider(LocalStrings)                       │
│   AppContent                                                   │
│     NavHost(RootRoute)                                         │
│       HorizontalPager                                          │
│         0 HomeScreen    1 StatsScreen    2 SettingsScreen      │
│         LiquidGlassNavBar（3 个 Tab）                           │
│     NavHost 详情路由：Deck / Review / Quick / Browse            │
└───────────────────────────┬────────────────────────────────────┘
                            │ StateFlow + 事件
┌───────────────────────────▼────────────────────────────────────┐
│ ViewModel 层                                                   │
│ HomeViewModel  DeckViewModel  ReviewViewModel                  │
│ QuickViewModel BrowseViewModel StatsViewModel SettingsViewModel │
│  - 组合 Repository 状态                                        │
│  - 持有页面级临时状态（查询、选择、更新检查结果）               │
│  - 调用 Repository 命令                                        │
└───────────────────────────┬────────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────────┐
│ Repository（唯一权威状态）                                     │
│ FlashcardRepository                                            │
│  StateFlow<AppState> / update {} / 250ms 防抖 + 原子落盘        │
│  导入 / 图片 / 备份 / 评分 / 会话 / 删除 / 设置                 │
└───────────────────────────┬────────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────────┐
│ 纯函数领域层                                                   │
│ CardParser（拆卡）  Scheduler（Leitner）  MarkdownParser（渲染）│
│ UpdateChecker（版本比较）  Changelog（随包更新日志）            │
└─────────────────────────────────────────────────────────────────┘
```

### 2.1 单向数据流

1. 用户操作触发 ViewModel 命令。
2. Repository 在 `@Synchronized update {}` 中生成新的不可变 `AppState`，写入 `MutableStateFlow`。
3. 250ms 防抖后把 JSON 原子写入磁盘（临时文件 + rename）。
4. Compose 通过 `collectAsStateWithLifecycle()` 拿到新状态重组。
5. 评分、确认导入、切换会话等关键路径会立即落盘（避免杀进程丢状态）。

### 2.2 依赖注入

没有 Hilt/Koin。`MdFlashcardsApplication` 在 `onCreate` 创建唯一的 `FlashcardRepository`；
`repository()` Composable 从 `Application` 取；ViewModel 用轻量 `VmFactory` + `appViewModel(key) {}` 创建。
这是有意保持的“小项目简单依赖”风格。

---

## 3. 目录与模块职责

```
app/src/main/java/com/fest/mdflashcards/
├── MainActivity.kt                 // edge-to-edge + setContent
├── MdFlashcardsApplication.kt      // 全局 Repository
├── data/
│   ├── Models.kt                   // AppState / Deck / Card / CardStat / Settings / ReviewSession
│   ├── FlashcardRepository.kt      // 状态、持久化、导入、图片、备份、评分、会话
│   ├── SampleMarkdown.kt           // 示例 md + 写入 Download
│   ├── UpdateChecker.kt            // GitHub Release + 版本比较
│   └── Changelog.kt                // 随包更新的更新日志
├── domain/
│   ├── ParsedModels.kt             // 拆卡纯数据
│   ├── CardParser.kt               // Markdown → 卡片（拆卡规则实现）
│   └── Scheduler.kt                // Leitner 三盒
├── ui/
│   ├── App.kt                      // 主题、字符串、Pager、NavHost
│   ├── ViewModelCompat.kt          // repository() / VmFactory / appViewModel
│   ├── navigation/Routes.kt        // Root / Deck / Review / Quick / Browse
│   ├── theme/Theme.kt, Motion.kt   // 动态色、形状、排版、阅读参数
│   ├── i18n/Strings.kt             // 简中 / 繁中 / English
│   ├── components/Common.kt        // 液态玻璃导航、Chip、确认框、空态
│   ├── components/Markdown.kt      // Markdown 块解析 + AnnotatedString + 本地图片
│   └── pages/                      // Home / Deck / Review / Quick / Browse / Stats / Settings
└── viewmodel/                      // 每个页面对应一个 ViewModel
```

---

## 4. 数据模型

`AppState` 是唯一根状态：

```kotlin
data class AppState(
    val version: Int = 1,
    val decks: List<Deck>,
    val stats: Map<String, CardStat>,       // cardId -> 学习状态
    val daily: Map<String, Int>,            // yyyy-MM-dd -> 当天学习次数
    val reviewSession: ReviewSession?,      // 未完成会话
    val lastBackupAt: Long,
    val lastChangeAt: Long,
    val settings: Settings,
)
```

- `Deck`：`id/name/title/createdAt/mode/qstyle/raw/tags/source/bundled/cards`；
- `Card`：`id/title/question/answer/path/chapter/tags/modified`；
- `CardStat`：序列化字段沿用 `s/n/ok/bad/last/box/dueAt/ivl`，兼容历史 JSON 备份；
- `Settings`：主题、语言、拆卡默认值、统计视图、阅读参数 `{fs,lh,cw}`；
- `ReviewSession`：`deckIds/filter/deckName/phase/total/rated/okCount/fuzzyCount/badCount/queueIds/round2Ids/currentId/savedAt`。

### 卡片 id 与进度继承

`CardParser.hash32("title|question|answer")` 生成 FNV-1a 8 位十六进制；重复内容追加 `-2`。
因此同一篇笔记重新导入时，卡片 id 不变，`stats[cardId]` 自动沿用。

---

## 5. 拆卡算法（CardParser）

### 5.1 预处理

1. 去 BOM、CRLF→LF、去 NUL；
2. 剥离 YAML frontmatter；
3. `codeMask` 标出围栏代码块行，后续标题/QA/分隔线检测全部跳过这些行；
4. `collectHeadings` 收集 1–6 级标题，`cleanInline` 去掉 Markdown 标记。

### 5.2 自动选级

统计 1–5 级标题：候选至少 2 个；优先“数量 ≥ 3 且正文长度中位数 ≤ 卡片上限”；
多个满足时选更深一级；否则选数量最多的层级。

### 5.3 拆分模式

| 模式 | 行为 |
|---|---|
| `auto` | 标题 → Q/A → 分隔线（必须多块）→ 段落；单标题长文按段落切 |
| `h1/h2/h3` | 按对应标题切；超长小节先尝试更深子标题，再按段落切 |
| `qa` | 识别 `Q:/A:`、`Question:/Answer:`、`问：/答：`、`Q1、`，必须带分隔符 |
| `hr` | 按 `--- / *** / ___` 分块 |
| `para` | 空行聚段后按 800/1600/3000 字合并 |

超长小节下拆时，父小节第一个子标题之前的导语会保留为“概览卡”，避免丢内容。

### 5.4 题面生成

- 标题本身含 `？/为什么/如何/区别/原理/…` 时不重复生成；
- 否则按内容特征选模板：对比、流程、原理、用途、优缺点、列表（≥3 项）、表格、定义、通用；
- `cloze` 把答案里的 `**加粗**` 或 `` `代码` `` 挖成 `____`，少于 2 个挖空点退回提问；
- 支持简中、繁中、英文三套模板。

---

## 6. Markdown 渲染

`ui/components/Markdown.kt` 是零第三方依赖的简化渲染器：

- 块级：围栏代码、标题、分隔线、引用（含 `[!note]` callout）、表格、有序/无序列表、段落、**独立图片**；
- 行内：粗体、斜体、删除线、行内代码、链接、`![alt](path)`；
- 链接不执行 `javascript:`（当前实现只做样式，不打开不可信 scheme）；
- 代码块使用等宽字体 + 横向滚动；
- 表格用等宽列 Row 渲染；
- 图片按 **文件名** 到 `filesDir/images/` 查找，采样解码（最大边 1400px），避免 OOM；找不到显示“图片未导入”。

---

## 7. 图片导入链路

1. 导入页“选择图片”通过 SAF 多选 `image/*`；
2. `FlashcardRepository.importImages()` 读取字节，SHA-256 前 12 位 + 原文件名生成唯一文件名，写入 `filesDir/images/`；
3. 返回 `原文件名 -> 存储文件名` 映射；
4. `importMarkdown()` / `importZip()` 在解析前用 `rewriteImageRefs()` 重写：
   - `![alt](images/a.png)` → `![alt](<hash>-a.png)`；
   - `![[images/a.png|300]]` → `![[<hash>-a.png|300]]`；
5. ZIP 内的图片条目也会先解出、存储、重写；
6. 卡片答案/题面仍保存原始 Markdown（已重写图片名），渲染时按 basename 查找本地文件。

> 限制：图片不写入 JSON 备份；换机后需要重新导入图片。同一文件名不同内容会因 hash 不同而共存，但映射表按 basename 覆盖，极端情况下以最后一次为准。

---

## 8. 学习调度与交互

### 8.1 Leitner 三盒

| 评分 | box | 间隔 | dueAt |
|---|---|---|---|
| 记住 | min(3, box+1) | 1/3/7 天 | now + ivl |
| 模糊 | 1 | 1 天 | now + 1 天 |
| 忘记 | 1 | 0 | now |

新卡/无 box 视为今日到期。队列先洗牌，到期卡优先；数量限制优先保留到期卡。
评分后更新 `stats` 与 `daily`；首轮模糊/忘记卡进入 `round2`；支持撤销（恢复 stat、队列、计数）。

### 8.2 3D 翻面

`ReviewScreen.FlipSwipeCard`：

- `rotationY` 在 0° 与 180° 之间做 460ms 缓动；
- 前 90° 只显示正面，后 90° 显示旋转 180° 的背面，避免镜像；
- 点击正面 → `vm.reveal()`；点击背面 → `vm.hideAnswer()`；
- 两面内容分别可垂直滚动，提示文字固定在底部。

### 8.3 状态分组

- 笔记详情页的「状态」筛选只作用于当前 `deckId` 的卡片：
  - 全部状态：不过滤；
  - 已掌握：`CardStat.status == "known"`；
  - 尚未背诵：无 `CardStat` 或 `reviews == 0`；
  - 已标记：`reviews > 0` 且尚未掌握（即点过“模糊/忘了”）。
- “记住”会写入 `known`，因此自动移出“已标记”；再次“模糊/忘了”会回到“已标记”。
- 该状态筛选与章节、薄弱卡、数量可以叠加使用，开始学习时只把这些卡片放入队列。

### 8.4 左右滑动评价

- 仅在答案面启用 `detectHorizontalDragGestures`；
- 拖动时 `translationX` 跟随，`rotationZ` 随位移倾斜，透明度随距离下降，边框变绿/红；
- 松手超过 28% 宽度 → 动画飞出 → `vm.rate(GOT/FORGOT)`；
- 未超过阈值 → spring 回中；
- 新卡片通过 `LaunchedEffect(card.id)` 重置位移。

---

## 9. 页面与导航

### 9.1 根容器

`RootRoute` 内使用 `HorizontalPager(3)`：

- 0 首页、1 统计、2 设置；
- 底部 `FloatingNavBar` 与 `pager.currentPage` 双向同步：点按 `animateScrollToPage`，滑动更新高亮；
- 详情路由（Deck/Review/Quick/Browse）通过同一个 `NavHost` push，根容器离开组合，底栏自然隐藏；
- 返回根容器时 Pager 页码由 saveable 状态恢复。

### 9.2 液态玻璃底栏

`FloatingNavBar` 不是普通 `NavigationBar`：

- 外层 `shadow(18.dp)` + 圆角 32dp；
- 使用 **Haze** 的 `hazeSource` / `hazeEffect` 做**真实背景模糊**（Android 12+ RenderEffect；低版本回退 tint），背景色半透明；
- 1dp 白色渐变描边 + 顶部高光；
- 选中指示块随手指水平拖动，松手吸附到最近 Tab；
- 每个 Tab 使用 `MutableInteractionSource.collectIsPressedAsState()`，按压缩放至 0.92；
- 点击有 `TextHandleMove` 触感反馈；
- 选中项用主色着色与半透明胶囊背景；
- 三页 Pager 设置 `beyondViewportPageCount = 2`，提前组合相邻页面，降低滑动掉帧。

> 导航栏样式对齐 Ghost Downloader 的悬浮胶囊玻璃：背景模糊、半透明、细描边、柔和阴影、按压形变，而不是大块渐变或夸张光斑。

### 9.3 页面职责

| 页面 | 关键内容 |
|---|---|
| Home | 统计条、继续学习、搜索/排序、笔记卡片、FAB、导入 Sheet（md/zip/图片/粘贴/示例/拆卡选项）；长按笔记或三点菜单可打开编辑框 |
| Deck | 笔记信息（卡片/已掌握/尚未背诵/已标记）、三项快捷入口、学习设置（数量/薄弱/章节/状态）、重命名/重置/删除 |
| Review | 3D 翻面卡片、滑动评分、三档按钮、撤销、第 2 轮、完成摘要 |
| Quick | 章节 Chip、题目目录 Sheet、折叠分组、搜索、跳转高亮 |
| Browse | 搜索、卡片列表、编辑对话框、删卡、重置单卡 |
| Stats | 今日/累计/连续、26 周热力图、7 天条形图、按日点击 |
| Settings | 通用 / 阅读 / 数据入口；关于卡（版本、仓库、检查更新、启动检查、更新日志） |
| General | 主题、语言、预测性返回开关 |
| Reading | 字号、行距、卡片宽度 |
| Data | 导出备份、导入备份、清空数据 |

---

## 10. 持久化与备份

- 文件：`filesDir/state.json`；图片：`filesDir/images/`；
- `persistNow()` 写 `state.json.tmp` 再 rename，避免半写入；
- 关键路径（评分、会话、导入、删除）会立即 flush；
- 备份导出：SAF `CreateDocument("application/json")`；内容为 `AppState`（含进度与设置）；
- 备份导入：`OpenDocument` 后选择“合并/替换”；
  - 合并：同名笔记加 `(导入N)`，只补缺失卡片进度；daily 相加；
  - 替换：整体替换；
- 兼容历史 JSON 的字段名（`s/n/ok/bad/last/box/dueAt/ivl`），未知字段忽略。

---

## 11. 检查更新与更新日志

- `UpdateChecker` 请求 `https://api.github.com/repos/abaguai6689/suishibeisong/releases/latest`；
- 解析 `tag_name`，去掉 `v` 前缀；
- 把版本按数字段比较（`1.1.0 > 1.0.9`）；
- 结果：`UP_TO_DATE / AVAILABLE / FAILED`；
- 新版本时展示 GitHub Release 链接；
- 更新日志来自 `Changelog.entries`，是 **随 APK 编译** 的本地数据，因此每次发版必须同步更新；
- 网络失败不阻塞应用，只显示错误文案。

---

## 12. 主题与国际化

- `MdFlashcardsTheme`：Android 12+ 使用系统动态色（Material You），低版本使用内置紫蓝色板；
- 主题模式：跟随系统 / 浅色 / 深色；
- `SideEffect` 同步状态栏/导航栏图标明暗；
- 三语言 `Strings.kt`（简中 / 繁中 / English）通过 `CompositionLocal` 提供，切换立即重组；
- 阅读设置：字号 `fs`（0.92/1/1.12）、行距 `lh`（0.86/1/1.2）、内容宽度 `cw`（暂只存储，页面使用全宽）；
- 形状：24/32dp 大圆角；动效：`GhostMotion` 贝塞尔与 spring。

---

## 13. 构建、测试与发布

### 本地

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

单测：

- `CardParserTest`：17 个用例，覆盖标题拆卡、代码块掩码、Q/A、长文下拆、题面模板、挖空、哈希、Leitner；
- `StateSerializationTest`：状态往返、历史备份字段兼容。

### CI

`.github/workflows/android.yml`：

1. `actions/checkout`
2. `setup-java@v4`（Temurin 17）
3. `gradle/actions/setup-gradle@v4`
4. `./gradlew :app:testDebugUnitTest`
5. `./gradlew :app:assembleRelease`
6. 上传 artifact `md-flashcards-release-apk`
7. tag `v*` 时复制为 `FlashcardsRecite-<tag>-release.apk` 并创建 Release

### 版本发布流程

1. 修改 `app/build.gradle.kts` 的 `versionCode` / `versionName`；
2. 在 `Changelog.entries` 顶部新增一条（日期 + 变更点）；
3. 运行 `assembleDebug` 与单测；
4. commit + push；
5. `git tag vX.Y.Z && git push origin vX.Y.Z`；
6. Actions 自动构建并发布 Release。

---

## 14. 已知限制与后续方向

- 不做 KaTeX 公式渲染，公式按原文显示；
- 不做 AI 提炼、PIN、提醒、标签筛选；
- 图片不进 JSON 备份，不做未引用图片清理；
- 内容宽度设置尚未作用到卡片布局；
- 更新检查未使用 GitHub Token，受匿名 API 限流；
- 备份合并的 daily 采用相加，可能与“只补缺失”的语义不完全一致。

后续若扩展，建议优先：图片备份/清理 → 全库搜索 → 标签筛选 → 公式渲染 → 可选的 AI 题面提炼。

---

## 15. 贡献约定

- 纯函数优先放进 `domain/`，并补 JUnit；
- 业务状态只允许 Repository 修改，ViewModel 不持有可变业务状态；
- 新增页面必须使用类型安全路由，并在 `README` / `README-DETAILED` 补充说明；
- 发版必须同步 `versionName`、`versionCode`、`Changelog`；
- 不引入 WebView / 远程配置 / 账号系统，保持纯本地。

---

## 16. 检查更新与远程更新日志

- 软件内「关于 → 更新日志」读取 `data/Changelog.kt`，随 APK 打包，保持完整历史，不联网。
- 启动检查更新读取 GitHub `releases/latest`：
  - Release `body` 作为弹窗中的最新更新日志；
  - `tag_name` 与 `BuildConfig.VERSION_NAME` 做数字段比较；
  - 只有检测到更高版本且「启动时检查更新」开启时才弹窗。
- 发布时通过 `.github/workflows/android.yml` 的 `body_path: RELEASE_NOTES.md` 把仓库根目录的 `RELEASE_NOTES.md` 写进 GitHub Release 正文；以后只改这个文件即可更新弹窗内容，不需要改 APK。
- 「立即更新」打开对应 Release 页面；「暂不更新」仅关闭当次弹窗，下次启动仍会检查。

## 17. 预测性返回

- `AndroidManifest.xml` 开启 `android:enableOnBackInvokedCallback="true"`；
- Navigation Compose 2.8.5 的 `NavHost` 在 Android 13+ 自动接入系统预测性返回；
- 设置 → 通用中的「预测性返回」关闭时，在非根页面注册 `BackHandler` 立即返回，避免系统预览动画；
- 系统开关 `Settings.Global.ENABLE_BACK_ANIMATION`（`enable_back_animation`）必须为 1，否则任何 App 都不会出现返回预览。通用页会检测该值并提示，App 无权修改系统设置；
- 手动开启：开发者选项 → 预测性返回动画，或 `adb shell settings put global enable_back_animation 1`。

## 18. 页面与弹窗动效

- 根节点用 `pointerInput(PointerEventPass.Initial)` 记录最近一次按下位置，换算成 `TransformOrigin`；
- `NavHost` 的 `enterTransition` 使用 `scaleIn(transformOrigin = tapOrigin)`，让二级页面从按钮位置展开；
- `popExitTransition` 缩回同一位置，`exit/popEnter` 使用淡出/淡入；
- 所有 `AlertDialog` 替换为 `OriginAlertDialog`：`Dialog` 内的 `AnimatedVisibility + scaleIn(transformOrigin)`，从触发位置弹出；
- 不引入 SharedNav 依赖，避免 Compose 版本耦合。
- 笔记详情顶部四类数量可点击，进入独立的 `DeckCardsRoute` 分类页；卡片默认只显示题目，点击用 `animateContentSize + AnimatedVisibility` 展开答案，且同一时间只允许一张展开。

## 19. 界面参考与许可

- 液态玻璃底栏的视觉与交互参考：
  - [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（Apache-2.0）
  - [sdegenaar/liquid_glass_widgets](https://github.com/sdegenaar/liquid_glass_widgets)（MIT）
- 学习卡片翻面布局参考「知识卡片 1.0」的 4:5 卡片、500ms 弹性翻转、正反面题签/答案结构。
- 背景模糊使用 [Haze](https://github.com/chrisbanes/haze)（Apache-2.0）。
- 以上均为设计/交互参考；本项目代码为 Kotlin/Compose 独立实现。
- 第三方许可见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 20. 签名与覆盖安装

- 固定签名文件：`keystore/md-flashcards.jks`，alias `mdflashcards`；
- 包名：`com.suishibeisong.flashcards`；应用名：闪卡背诵；仓库：`abaguai6689/suishibeisong`；
- debug / release 都使用同一个 `signingConfigs.fixed`，因此后续版本可直接覆盖安装；
- 可通过环境变量 `MD_KEYSTORE_PASSWORD` / `MD_KEY_ALIAS` / `MD_KEY_PASSWORD` 覆盖默认值；
- **注意**：该 keystore 随公开仓库分发，只适合个人侧载/内测，不能用于 Google Play 上架；上架请换私有密钥并从 CI Secrets 注入；
- 1.1.x 及更早版本使用 CI 临时 debug 签名，升级到 1.2.0 需要卸载一次；1.2.0 之后不再需要。
