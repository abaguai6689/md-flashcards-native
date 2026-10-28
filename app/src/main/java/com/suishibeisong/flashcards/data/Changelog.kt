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
            version = "1.9.0",
            date = "2026-10-28",
            changes = listOf(
                "修复更换章节后仍进入上次学习章节的问题：每次开始学习使用独立的复习 ViewModel",
                "笔记详情保留章节、状态、数量、薄弱卡选择，退出学习返回后不重置",
                "右滑记住时卡片边框改为绿色，左滑仍为红色",
                "移除预测性返回功能、设置开关和相关代码",
                "云端卡组元数据新增 id / version，启动时自动核对已下载卡组的版本并提示更新",
                "云端导入的笔记在详情页显示文件说明、上传人、时间、大小、版本和文件 ID",
                "修复云端加载/下载转圈不居中、云端文件更名为「穴位定位」",
            ),
        ),
        ChangelogEntry(
            version = "1.8.1",
            date = "2026-10-28",
            changes = listOf(
                "示例模板重写：每种导入格式前都加入详细说明注释，覆盖二级/三级标题、Q/A、分隔线、段落、表格、列表、代码块、图片、挖空、标签和 ZIP",
                "导入解析会忽略 HTML 注释，模板里的说明不会变成卡片",
                "云端仓库的针灸卡组更新为仅保留耳穴章节，元数据同步更新",
            ),
        ),
        ChangelogEntry(
            version = "1.8.0",
            date = "2026-10-28",
            changes = listOf(
                "导入笔记新增「从本地导入 / 从云端导入」两个入口",
                "云端导入读取 GitHub 公共卡组仓库 suishibeisong-cloud，显示文件大小、上传时间、上传人、文件说明",
                "云端卡组按同目录的 <文件名>.meta.json 读取元数据，支持 md / txt / zip",
                "新增云端仓库及 README 上传格式说明，并上传按经络分类的针灸卡组",
            ),
        ),
        ChangelogEntry(
            version = "1.7.1",
            date = "2026-10-28",
            changes = listOf(
                "应用名称改为「背诵小猪手」，启动图标替换为用户提供的图片",
                "修复随机学习按钮为直角的问题，恢复与相邻按钮一致的圆角",
                "笔记详情顶部四类数量改为跟随当前章节筛选，不再始终显示全书数量",
            ),
        ),
        ChangelogEntry(
            version = "1.7.0",
            date = "2026-10-27",
            changes = listOf(
                "升级 Compose 1.10 / Kotlin 2.2.20 / AGP 8.9.3 / Gradle 8.11.1 / compileSdk 36",
                "正式接入 SharedNav：二级页面使用 SharedContainer 从触发卡片/按钮展开，替换自写的缩放过渡",
                "接入 SharedNav 的返回栈与预测性返回；设置中的预测性返回开关继续生效",
                "移除页面透明交叉渐变，解决二级页面闪烁和生硬感",
                "分类页卡片移除与展开后尺寸不匹配的矩形高光，点击反馈不再出现错位高光",
                "更新弹窗和设置「前往下载」直接打开 APK 资产链接",
            ),
        ),
        ChangelogEntry(
            version = "1.6.1",
            date = "2026-10-27",
            changes = listOf(
                "修复二级页面缩放进入时的闪烁：去掉页面透明度交叉渐变，上一页保持静止，新页从点击位置缩放覆盖",
                "笔记详情分类数量改为进入独立分类页，不再用弹窗显示",
                "分类页同一时间只展开一张卡片的答案，点新卡自动收起旧卡",
                "更新弹窗和设置页「前往下载」直接打开最新 APK 资产下载链接，不再跳 Release 页面",
            ),
        ),
        ChangelogEntry(
            version = "1.6.0",
            date = "2026-10-26",
            changes = listOf(
                "二级页面改为从点击位置缩放入场，返回时缩回触发位置；设置子页、笔记详情、学习、速记、浏览统一生效",
                "所有弹窗改为从触发按钮/菜单项的位置缩放弹出，不再从屏幕中心出现",
                "笔记详情顶部四类数量可点击，弹出该分类的卡片列表",
                "分类列表默认只显示题目；点击题目卡片，卡片下边界平滑展开答案",
                "参考 SharedNav 的容器共享/普通动效思路，保持轻量、无额外依赖",
            ),
        ),
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
