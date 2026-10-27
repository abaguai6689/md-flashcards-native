# 闪卡背诵 · Flashcards Recite

[![Android CI](https://github.com/abaguai6689/suishibeisong/actions/workflows/android.yml/badge.svg)](https://github.com/abaguai6689/suishibeisong/actions/workflows/android.yml)

一款原创的 Android 本地闪卡应用：把 Markdown 笔记拆成一张张卡片，随机抽背、按章节速记、查看学习统计。

**纯本地 · 无账号 · 无服务器 · 无广告**。数据只保存在手机应用私有目录，备份由你自己导出。

- 仓库：https://github.com/abaguai6689/suishibeisong
- 最新版本：**v1.7.0**
- 详细架构与实现说明：[README-DETAILED.md](README-DETAILED.md)

## 功能

### 导入与拆卡

- 导入 `.md`、`.markdown`、`.txt`、`.zip`，支持一次多选；也可以直接粘贴 Markdown。
- 拆卡模式：自动选级、按二级标题、按三级标题、Q/A、分隔线、段落。
- 题面风格：提问式、标题式、挖空式。
- 粒度：细（≤800 字/张）、中（≤1600 字/张）、粗（≤3000 字/张）。
- 代码块中的 `##`、`Q:` 不会被误判；YAML frontmatter 自动跳过。
- 卡片 id 由「标题 + 题面 + 答案」内容哈希生成，重新导入同一篇笔记会继承已掌握进度。
- **导入图片**：选择图片后按文件名匹配 Markdown 引用，图片复制到应用私有目录，卡片内直接渲染；图片不参与 JSON 备份。
- **保存示例**：导入页可一键把示例 Markdown 写入系统 `Download` 目录，方便用 AI 改写后重新导入。

### 学习

- Leitner 三盒：记住 1 / 3 / 7 天，模糊明天，忘记立即到期。
- 学习数量 20 / 50 / 全部；只看薄弱卡；按章节筛选。
- 卡片正面题目，**点击 3D 翻面**到背面答案。
- 背面**左滑忘了 / 右滑记住**，带位移、旋转、淡出动画；也保留三档评分按钮。
- 撤销上一张；首轮模糊/忘记卡自动进入第 2 轮；退出后可继续上次会话。

### 速记、浏览、统计

- 分类速记：章节 Chip、题目目录、折叠、搜索、点击定位高亮。
- 浏览/编辑：搜索、改标题/题面/答案、删卡、重置单卡进度。
- 统计：今日 / 累计 / 连续天数、26 周热力图、近 7 天条形图。

### 设置与更新

- 液态玻璃底部导航（按压形变 + 移动高光），左右滑动切换首页 / 统计 / 设置。
- 主题跟随系统 / 浅色 / 深色；简中 / 繁中 / English；字号、行距、内容宽度。
- 备份导出/导入（合并或替换）、清空数据。
- **检查更新**：与 GitHub 最新 Release 版本号比较，提示是否最新，可直接前往下载。
- **更新日志**：内置随 APK 版本更新的更新记录。

## 构建

要求 JDK 17 + Android SDK 35：

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleRelease
# app/build/outputs/apk/release/app-release.apk（固定签名，可直接覆盖安装）
```

GitHub Actions 在 push / PR / 手动触发 / `v*` tag 时自动运行单测、构建 **release** APK；tag 会创建 Release 并附带 APK。

## 项目结构

```
app/src/main/java/com/fest/mdflashcards/
├── MainActivity.kt / MdFlashcardsApplication.kt
├── data/       模型、Repository、拆卡导入、图片、更新检查、更新日志
├── domain/     CardParser（纯函数）、Scheduler
├── ui/         主题、导航、组件、七个页面
└── viewmodel/  Home / Deck / Review / Quick / Browse / Stats / Settings
```

完整模块说明、数据模型、算法与设计取舍见 [README-DETAILED.md](README-DETAILED.md)。

## 已知限制

- 暂不支持 AI 提炼、PIN 应用锁、每日提醒、标签筛选；
- 图片只保存在本机，JSON 备份不含图片，换机需重新导入；
- 公式按 Markdown 文本显示，不渲染 KaTeX；
- 更新检查使用 GitHub 公共 API，未登录时受速率限制。

## License / 第三方

MIT，见 [LICENSE](LICENSE)。界面参考与依赖声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
