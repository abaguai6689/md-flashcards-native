# MD 闪记卡 · Native

[![Android CI](https://github.com/abaguai6689/md-flashcards-native/actions/workflows/android.yml/badge.svg)](https://github.com/abaguai6689/md-flashcards-native/actions/workflows/android.yml)


把一份很长的 Markdown 笔记拆成一张张闪卡，在 Android 上随机抽背、按章节速记、看复习统计。

本仓库是 [fest77/flash-memory-cards](https://github.com/fest77/flash-memory-cards) 的 **原生 Android 重构版**：
不再使用 WebView 单文件网页，而是按 **Ghost-Downloader-Android 的前端架构** 重写为
**Kotlin + Jetpack Compose + MVVM + Repository + 类型安全 Navigation** 的原生应用。

> 原项目分析见 [docs/ANALYSIS.md](docs/ANALYSIS.md)。

## 功能

- 导入 `.md / .markdown / .txt / .zip`，支持多选；也可直接粘贴 Markdown。
- 拆卡：自动 / 二级标题 / 三级标题 / Q-A / 分隔线 / 段落；提问式 / 标题式 / 挖空式；三档粒度。
- 卡片：内容哈希 id，重新导入同名笔记会替换卡片但**继承已掌握进度**。
- 复习：Leitner 三盒（1 / 3 / 7 天）、到期优先、20/50/全部、只看薄弱卡、按章节、左右滑动评分、撤销、第 2 轮、会话持久化（退出可继续）。
- 分类速记：章节大纲、题目目录、折叠、搜索、一屏完整问答。
- 浏览/编辑：搜索、改标题/题面/答案、删卡、重置单卡进度。
- 统计：今日 / 累计 / 连续天数、26 周热力图、近 7 天条形图。
- 设置：主题（跟随系统/浅色/深色）、语言（简中/繁中/English）、字号/行距/内容宽度、备份导出/导入（合并或替换）、清空数据。
- 原生 Material 3 动态取色（Android 12+）、edge-to-edge、大标题折叠、悬浮胶囊底部导航。

## 架构

```
Compose UI (pages/components)
        │ collectAsStateWithLifecycle
ViewModel (Home / Deck / Review / Quick / Browse / Stats / Settings)
        │ 命令 + StateFlow
FlashcardRepository  ← 唯一权威状态，JSON 原子落盘（filesDir/state.json）
        │
CardParser / Scheduler / MarkdownParser（纯 Kotlin，可单测）
```

- 单 Activity，Navigation Compose **类型安全路由**（`@Serializable` routes）。
- 无 Hilt/Koin：`Application` 持有 repository，ViewModel 通过轻量工厂创建（与原项目“全局 engine”一致）。
- 无 Room/Retrofit：数据是纯 JSON；导入/导出走 SAF，不申请存储权限。
- `StateFlow` + `WhileSubscribed(5s)`；评分/会话关键路径立即落盘。

## 目录

```
app/src/main/java/com/fest/mdflashcards/
├── MainActivity.kt / MdFlashcardsApplication.kt
├── data/          Models.kt（@Serializable）、FlashcardRepository.kt
├── domain/        CardParser.kt（core.js 移植）、Scheduler.kt
├── ui/
│   ├── App.kt、ViewModelCompat.kt
│   ├── navigation/Routes.kt
│   ├── theme/Theme.kt、Motion.kt
│   ├── i18n/Strings.kt
│   ├── components/Common.kt、Markdown.kt
│   └── pages/Home / Deck / Review / Quick / Browse / Stats / Settings
└── viewmodel/
app/src/test/.../CardParserTest.kt   # 17 个拆卡/调度单测
```

## 构建

要求 JDK 17 + Android SDK 35。

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions 在 push / PR / tag / 手动触发时运行单测并构建，上传 `md-flashcards-debug-apk`；
打 `v*` tag 会创建 Release 并附带 APK。

## 与原版的功能取舍（v1.0）

已实现：拆卡全模式、提问模板、Leitner 复习、速记、浏览编辑、统计、主题/语言/阅读设置、JSON 备份。
暂未实现（路线图）：

- 图片导入与渲染、zip 备份中的图片；
- AI 提炼题面（多供应商）；
- PIN 应用锁、每日提醒；
- 全库跨笔记搜索、标签筛选、笔记合并/移动卡片；
- 与 Web 版 IndexedDB 的直接迁移（可通过 JSON 备份导入）。

这些取舍是为了先把“导入 → 拆卡 → 复习 → 统计”的闭环做成类型安全、可测试的原生实现。

## License

MIT，与原项目一致。原项目版权归 fest77；本重构版保留其数据格式与核心算法语义。
