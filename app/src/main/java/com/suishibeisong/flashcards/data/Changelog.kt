package com.suishibeisong.flashcards.data

data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<String>,
)

/**
 * Bundled changelog. Update this list with every release so "更新日志"
 * always matches the installed APK, without needing network access.
 */
object Changelog {
    val entries = listOf(
        ChangelogEntry(
            version = "1.5.1",
            date = "2026-10-26",
            changes = listOf(
                "预测性返回：Activity 级显式开启 enableOnBackInvokedCallback",
                "通用设置新增系统检测：若 enable_back_animation=0，提示开启系统预测性返回动画并提供开发者选项入口",
                "说明：该开关属于 Android 系统设置，应用无权修改；需系统开启后预测性返回预览才会生效",
            ),
        ),
        ChangelogEntry(
            version = "1.5.0",
            date = "2026-10-26",
            changes = listOf(
                "笔记详情顶部新增「卡片 / 已掌握 / 尚未背诵 / 已标记」四类数量",
                "设置新增「启动时检查更新」开关；启动检测到新版时弹窗显示版本号与 GitHub 最新更新日志",
                "更新弹窗提供「立即更新 / 暂不更新」，立即更新打开对应 Release 页面",
                "软件内更新日志保持本地完整历史；更新弹窗改为读取 GitHub Release 的正文，便于发布后修改",
                "新增预测性返回（Android 13+），并在设置 → 通用中提供开关",
                "设置拆分为通用 / 阅读 / 数据三个子页面，设置首页只保留入口，关于仍完整显示在最下方",
            ),
        ),
        ChangelogEntry(
            version = "1.4.0",
            date = "2026-10-26",
            changes = listOf(
                "全应用学习相关文案统一为「学习」，包括待学习、开始学习、学习设置、学习进度等",
                "笔记详情新增状态筛选：全部状态 / 已掌握 / 尚未背诵 / 已标记，仅作用于当前笔记",
                "模糊、忘了自动归入「已标记」；记住了归入「已掌握」；未学习归入「尚未背诵」",
                "修复导航栏与卡片点击时出现矩形高亮直角的问题，高亮/水波纹改为跟随圆角形状",
                "主页笔记支持长按弹出编辑框；三点菜单也新增「编辑笔记」入口",
            ),
        ),
        ChangelogEntry(
            version = "1.3.0",
            date = "2026-10-26",
            changes = listOf(
                "应用名称改为「闪卡背诵」，包名改为 com.suishibeisong.flashcards",
                "仓库更名为 suishibeisong（随时背诵），更新检查地址同步切换",
                "底栏参考 AndroidLiquidGlass / liquid_glass_widgets：真实模糊、滑动玻璃胶囊、可拖拽切换",
                "学习卡片参考知识卡片：4:5 卡片比例、500ms 弹性翻面、正面题签/居中题目、背面答案",
                "由于包名变更，1.3.0 会作为新应用安装，旧版本可手动卸载",
            ),
        ),
        ChangelogEntry(
            version = "1.2.1",
            date = "2026-10-26",
            changes = listOf(
                "导入按钮与页面底部留白按系统导航栏高度自适应，三键导航下不再被遮挡",
            ),
        ),
        ChangelogEntry(
            version = "1.2.0",
            date = "2026-10-26",
            changes = listOf(
                "底栏改用 Haze 真实背景模糊 + 半透明玻璃，样式对齐 Ghost Downloader 的悬浮胶囊导航",
                "三页 Pager 预组合相邻页面，减少左右滑动时的掉帧",
                "导入笔记 FAB 上移，避开底部导航栏",
                "使用固定签名密钥，之后版本可直接覆盖安装，无需卸载",
                "说明：由于早期版本使用 CI 临时 debug 签名，从 1.1.x 升级到 1.2.0 仍需卸载一次；1.2.0 之后不再需要",
            ),
        ),
        ChangelogEntry(
            version = "1.1.1",
            date = "2026-10-26",
            changes = listOf(
                "Android 9 及以下保存示例时申请存储权限，确保写入公共 Download 目录",
            ),
        ),
        ChangelogEntry(
            version = "1.1.0",
            date = "2026-10-26",
            changes = listOf(
                "主页底部三个 Tab 改为液态玻璃材质，带按压缩放与高光反馈",
                "主页支持左右滑动切换「首页 / 统计 / 设置」",
                "学习卡片支持 3D 翻面：正面题目，点击翻到背面看答案",
                "学习卡片支持左右滑动评价，带位移、旋转与淡出动画",
                "新增导入图片，并在卡片中按文件名本地渲染",
                "导入页新增「保存示例 Markdown 到 Download」",
                "设置页新增检查更新、当前版本与更新日志",
                "关于页改为当前仓库地址，移除旧说明",
            ),
        ),
        ChangelogEntry(
            version = "1.0.2",
            date = "2026-10-25",
            changes = listOf(
                "修复速记与浏览列表的高度约束",
                "补充状态序列化与 Web 备份兼容测试",
            ),
        ),
        ChangelogEntry(
            version = "1.0.1",
            date = "2026-10-25",
            changes = listOf(
                "修复持久化错误流初始化顺序",
                "新增 CI 自动测试与 APK 构建",
            ),
        ),
        ChangelogEntry(
            version = "1.0.0",
            date = "2026-10-25",
            changes = listOf(
                "首个原生 Compose 版本",
                "Markdown 导入、自动拆卡、Leitner 学习、速记、统计、备份",
            ),
        ),
    )
}
