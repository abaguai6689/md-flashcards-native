package com.fest.mdflashcards.data

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
                "复习卡片支持 3D 翻面：正面题目，点击翻到背面看答案",
                "复习卡片支持左右滑动评价，带位移、旋转与淡出动画",
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
                "Markdown 导入、自动拆卡、Leitner 复习、速记、统计、备份",
            ),
        ),
    )
}
