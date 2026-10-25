# `index.html` 深度分析（原项目）

> 说明：原仓库根目录没有 `index.html`。真正的单文件入口是构建产物 **`dist/md-flashcards.html`**（约 861 KB），
> 由模板 **`src/index.tpl.html`** 内联 `styles.css`、`core.js`、`app.js`、KaTeX（JS + CSS + 子集字体）和
> 可选题库/图片生成。直接读 `dist/md-flashcards.html` 会看到数万行内联脚本；阅读源码应看 `src/`。

## 1. 文件定位与构建

`scripts/build.py` 把模板里的占位符替换成实际内容：

| 占位符 | 来源 | 作用 |
|---|---|---|
| `/*STYLES*/` | `src/styles.css` | 全部界面样式（836 行） |
| `/*CORE*/` | `src/core.js` | 纯函数：解析、拆卡、Markdown→HTML、zip、调度 |
| `/*APP*/` | `src/app.js` | 全部应用逻辑、状态、视图、导入导出、AI |
| `/*BUNDLED*/` | `src/bundled-decks.js`（可选） | 内置题库 |
| `/*IMAGES*/` | `src/bundled-images.js`（可选） | 内置配图 |
| `/*KATEX*/` | `vendor/katex` | 公式渲染，字体 base64 内联 |

因此它是 **零依赖、可离线单文件 Web App**，不是多页站点。

## 2. DOM 结构：一个页面，七个视图

`index.tpl.html` 只有一套顶栏 + 七个 `<main class="view">`，靠 `.active` 切换：

1. `view-home`：空态引导、全局搜索、标签栏、排序、统计条、备份提醒、继续复习条、导入入口、笔记列表、导入设置、AI 入口。
2. `view-review`：返回、牌组名、进度、洗牌、进度条、卡片（路径/标题/题面/答案）、完成摘要、显示答案/三档评分/撤销。
3. `view-browse`：返回、牌组信息、搜索、卡片列表、重置进度。
4. `view-quick`：分类速记，章节大纲、题目目录、搜索、分组折叠、一屏完整问答。
5. `view-manage`：所有笔记的管理列表（速记/复习/重命名/重置/导出/删除）。
6. `view-settings`：主题、语言、阅读字号/行距/卡宽、AI 供应商、PIN、提醒、存储、关于。
7. `view-stats`：今日/累计/连续天数、热力图/条形图、年月日定位。

所有交互通过 `data-act` 属性 + 一个全局事件委托处理（`onAction`），不是组件框架。

## 3. 数据模型与持久化

顶层 `state`：

```js
{
  version: 1,
  decks: [Deck],
  stats: { [cardId]: { s, n, ok, bad, last, box, dueAt, ivl } },
  daily: { "YYYY-MM-DD": count },
  reviewSession?: { deckIds, queueIds, round2Ids, currentId, phase, rated, ... },
  lastBackupAt, lastChangeAt, backupRemindDismissedAt,
  settings: { mode, gran, qstyle, theme, lang, statsView, reading{fs,lh,cw}, tagFilter, deckSort, ai }
}
```

`Deck`：`id, name, title, createdAt, mode, qstyle, raw, tags, source, bundled, cards[]`。
`Card`：`id, title, question, answer, path[], chapter, tags[], modified`。

存储：优先 IndexedDB（库 `md-review`，对象仓库 `state`），失败退 localStorage（`mdreview.state.v1`）。
保存是 250ms 防抖；复习评分、会话切换、导入/删除等关键路径会 `flushSave()` 立即落盘。
清库时先 `lockWrites` 再关闭数据库，避免写回。

卡片 id 由 `标题 + 题面 + 答案` 的 FNV-1a 哈希生成，因此**重新导入同一份笔记，旧进度自动继承**；重复内容追加 `-2` 后缀。

## 4. 拆卡核心（`core.js`）

### 4.1 预处理
- 去 BOM、统一换行、去 NUL。
- 剥离 YAML frontmatter。
- `codeMask`：逐行标记围栏代码块，保证代码里的 `##`、`---`、`Q:` 不参与结构解析。
- `collectHeadings`：收集 1–6 级标题，清理尾部 `#`，`cleanInline` 去 Markdown 标记。

### 4.2 自动选级
`chooseLevel` 统计 1–5 级标题：
- 候选至少 2 个；
- 优先「数量 ≥ 3 且正文长度中位数 ≤ 卡片上限」；
- 都满足时选**更深**一级；否则选数量最多的层级。

### 4.3 拆分模式
- `h1/h2/h3`：按对应标题切；小节超长（`> 1.5 × 上限`）时先尝试更深一级子标题，再按段落切块；父小节前言保留为「概览卡」。
- `qa`：识别 `Q:/A:`、`Question:/Answer:`、`问：/答：`、`**题目：**`、`Q1、/Q1.`，且**必须带分隔符**，避免把 `QPS:`、`API:`、`A/B` 吃掉首字母。问答标记前的正文保留为概览卡。
- `hr`：按 `--- / *** / ___` 分块，第一行当题面。
- `para`：按空行聚段，再合并到 ~800/1600/3000 字。
- `auto`：标题 → Q/A → 分隔线（必须产生多块）→ 段落兜底；单标题长文按段落切。

### 4.4 提问生成
- 标题本身含 `？/为什么/如何/区别/原理/…` 时不重复生成。
- 否则按内容特征选模板：对比、流程、原理、用途、优缺点、列表（≥3 个列表项）、表格、定义、通用。
- 支持 `zh-Hans / zh-Hant / en` 三套模板。
- `cloze`：把答案里的 `**加粗**` / `` `行内代码` `` 挖成 `____`，少于 2 个挖空点则退回提问式。

### 4.5 更新稳定
`finalizeCards` 统一 trim、补题面、补标题、清洗 path/chapter、按内容哈希生成 id。

## 5. Markdown 渲染

`renderMarkdown` 是手写块级解析器 + 行内替换：
- 围栏代码（高亮钩子、复制按钮、`data-raw`）、标题、分隔线、引用（含 Obsidian callout）、表格、有序/无序列表、段落。
- 行内：图片（含 `![[wiki]]` 与宽度）、链接白名单（拒绝 `javascript:`/`data:`/`//`）、加粗、斜体、删除线、行内代码。
- 先转义 HTML，再做格式替换；代码块只转义一次。
- 公式：`$...$` / `$$...$$` 经 `isMathCandidate` 过滤（中文、金额、范围不当公式），交给 KaTeX。

## 6. 复习调度

Leitner 三盒：

| 评分 | box | 间隔 | 到期 |
|---|---|---|---|
| 记住了 | min(3, box+1) | 1 / 3 / 7 天 | now + ivl |
| 模糊 | 1 | 1 天 | now + 1 天 |
| 忘了 | 1 | 0 | 立即 |

新卡/无 box 视为今日到期。复习队列：先洗牌，到期卡优先；数量限制时优先保留到期卡。
评分后 `daily[today]++`；首轮的模糊/忘记卡进入第 2 轮；支持撤销上一张（恢复卡片状态、队列、计数）。

## 7. 其他功能

- **导入**：多选 md/txt/zip；zip 手写解析（EOCD/CD，stored+deflate，`__MACOSX` 跳过，GBK 回退）。
- **图片**：按文件名引用、IndexedDB 存 blob、同名不同内容自动改名、未引用清理。
- **备份**：JSON（单笔记）或 zip（`backup.json` + `images/`）；导入预览、覆盖/合并、进度不覆盖。
- **统计**：GitHub 风格热力图（26 周）+ 7 天条形图，按年月日锚定。
- **AI**：多供应商（OpenAI 兼容 + Anthropic），只改题面不改答案，Key 仅本地。
- **Android 壳**：WebView + 虚拟 https origin + 文件选择 + JS 保存桥 + PIN/提醒。

## 8. 优点与局限

**优点**
- 单文件、离线、零后端；Markdown 解析与拆卡规则细致，有 515 行纯函数测试。
- 数据模型扁平、备份可迁移；卡片 id 内容寻址，重导入不丢进度。
- 代码块掩码、Q/A 误识别防护、XSS 白名单等边界处理成熟。

**局限**
- DOM 字符串拼接 + 全局 `state`，视图与业务高度耦合，难以类型约束和单测 UI。
- 单文件 860 KB，KaTeX/字体全内联；AI 受 CORS；图片不进 JSON 备份（zip 才带）。
- 无 SM-2/FSRS；无原生列表虚拟化；无系统级导航/手势/无障碍语义。
- WebView 壳的返回键、文件选择、通知/提醒都依赖 JS 桥，体验受浏览器内核限制。

## 9. 到原生 Compose 的映射

| Web | 本仓库（Compose） |
|---|---|
| 七个 `.view` + `showView` | 单 Activity + Navigation Compose 类型安全路由 |
| 全局 `state` + `scheduleSave` | `FlashcardRepository`：唯一权威 `StateFlow<AppState>` + JSON 落盘 |
| 直接操作 DOM | ViewModel + 不可变 UI State + Compose |
| `core.js` | `CardParser.kt`（1:1 移植）+ JUnit |
| `app.js` 内联字符串 | `Strings.kt` 三语言 + `LocalStrings` |
| 手写 HTML 渲染 | `Markdown.kt` 块解析 + `AnnotatedString` |
| IndexedDB | 应用私有 `state.json`（原子替换写） |
| WebView 壳 | 原生 SAF 文件选择、JSON 备份、Material 3 动态色 |
