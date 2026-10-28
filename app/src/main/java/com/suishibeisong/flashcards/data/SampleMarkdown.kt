package com.suishibeisong.flashcards.data

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.media.MediaScannerConnection
import androidx.core.content.ContextCompat
import java.io.File

object SampleMarkdown {
    const val FILE_NAME = "md-flashcards-sample.md"

    val content = """
        ---
        title: 背诵小猪手 · 导入模板
        tags: [模板, 示例, 使用说明]
        ---

        <!--
        使用说明（导入时会被忽略）：
        1. 这是一份格式模板，不是正式卡组；可以直接修改，或让 AI 按每种格式扩写。
        2. 推荐导入方式：拆卡模式「按二级标题」，题面风格「提问式」。
        3. 每种格式前的 <!-- 注释 --> 只用于阅读，App 解析时会自动移除。
        4. 图片需要和 Markdown 一起选择，或把 Markdown + images/ 打包成 ZIP。
        -->

        # 背诵小猪手 · 导入模板

        <!--
        格式一：二级标题 + Q/A（最推荐）
        二级标题作为章节；每个「问题 / 答案」是一张卡。
        适合按章节组织，卡片正面是问题，背面是答案。
        -->

        ## 格式一 · 二级标题 + Q/A

        <!-- 说明：问题：/ 答案： 也可以写成 Q: / A:、Question: / Answer:。 -->

        问题：什么是间隔重复？

        答案：在快要忘记时复习，用逐渐拉长的间隔巩固记忆。

        问题：为什么间隔重复有效？

        答案：它把复习安排在遗忘临界点，用更少的次数形成长期记忆。

        <!--
        格式二：三级标题
        二级标题是章节，三级标题是卡片；适合「经络 → 穴位」「章节 → 小节」这种两层结构。
        导入时选择「按三级标题」。
        -->

        ## 格式二 · 三级标题

        ### 卡片一：TCP 三次握手

        客户端发送 SYN，服务端回复 SYN+ACK，客户端再回复 ACK。

        ### 卡片二：四次挥手

        主动关闭方发送 FIN，对方回复 ACK；对方发送 FIN，主动方回复 ACK。

        <!--
        格式三：只有标题，没有 Q/A
        标题会作为题目，标题下面的正文会作为答案；适合把一段笔记直接变成一张卡。
        -->

        ## 格式三 · 标题式卡片

        这是一段没有 Q/A 标记的内容。

        导入后在「按二级标题」模式下，这个二级标题就是题目，下面这段正文就是答案。

        <!--
        格式四：Q/A 模式
        整个文件只有问题和答案，没有章节标题也可以；适合纯题库。
        导入时选择「Q/A」模式，卡片标题会自动取问题。
        -->

        ## 格式四 · Q/A 模式

        问题：HTTP 和 HTTPS 的区别是什么？

        答案：HTTPS 在 HTTP 和 TCP 之间增加 TLS，提供加密、身份认证和完整性校验。

        问题：常见状态码 404 表示什么？

        答案：请求的资源不存在。

        <!--
        格式五：分隔线模式
        用三个及以上的 --- 或 *** 分隔每一张卡；适合从旧笔记直接切分。
        导入时选择「分隔线」模式。
        -->

        ## 格式五 · 分隔线模式

        ---

        第一张卡：什么是虚拟内存？

        虚拟内存把物理内存抽象成连续地址空间，让每个进程都以为自己独占内存。

        ---

        第二张卡：什么是进程？

        进程是资源分配的基本单位，拥有独立地址空间。

        <!--
        格式六：段落模式
        没有标题、没有 Q/A 时，每个自然段会变成一张卡；适合短段落笔记。
        导入时选择「段落」模式。
        -->

        ## 格式六 · 段落模式

        这是第一个自然段。段落模式下，它会成为一张卡片的答案，第一行会作为题面。

        这是第二个自然段。空行会被视为段落边界。

        这是第三个自然段。段落不要太长，否则会按粒度继续切分。

        <!--
        格式七：表格、列表和代码块
        答案中可以放 Markdown 表格、列表、引用和代码块；代码块里的 ##、Q:、A: 不会被误拆。
        -->

        ## 格式七 · 表格 / 列表 / 代码块

        问题：Markdown 答案里可以放哪些内容？

        答案：可以放表格、列表、引用和代码块。

        | 语法 | 用途 |
        | --- | --- |
        | 表格 | 对比概念 |
        | 列表 | 分点说明 |
        | 代码块 | 保留代码和特殊符号 |

        - 第一点：先理解
        - 第二点：再记忆
        - 第三点：用自己的话复述

        ```kotlin
        // 代码块里的 Q: 和 ## 不会被当成卡片
        fun hello() = println("hello")
        ```

        <!--
        格式八：图片
        图片必须单独占一行，才会渲染成图片；写在句子中间只会显示 [图片] 占位。
        标准写法：![说明](images/图片名.png)
        Obsidian 写法：![[图片名.png]] 或 ![[图片名.png|320]]
        图片按文件名匹配，目录可以不同；导入时选择图片，或使用包含 images/ 的 ZIP。
        -->

        ## 格式八 · 图片

        问题：怎样在卡片答案里插入图片？

        答案：图片单独占一行，并使用下面的标准写法。

        ![示例图片](images/sample-image.png)

        也可以使用 Obsidian 双链写法：

        ![[sample-image.png|320]]

        <!--
        格式九：挖空题面
        在「题面风格」选择「挖空」时，答案中的 **加粗文字** 或 `行内代码` 会被替换为横线，生成填空卡。
        至少需要两处加粗/代码才会生成挖空题。
        -->

        ## 格式九 · 挖空题面

        问题：挖空模式会隐藏什么？

        答案：它会隐藏 **关键概念** 和 `关键代码`，让你先回忆再翻面。

        <!--
        格式十：YAML frontmatter 与标签
        文件最上方的 --- 区块会被跳过，不会变成卡片；tags 会作为卡组标签。
        答案里也可以写 #标签，App 会提取为卡片标签。
        -->

        ## 格式十 · 标签

        问题：怎样给卡组和卡片加标签？

        答案：在文件顶部写 tags，或在答案里写 #记忆技巧。

        <!--
        格式十一：ZIP 与图片目录
        图片较多时，建议打包成 ZIP；导入 ZIP 时会自动读取 Markdown 和 images/。
        ZIP 结构示例：
        我的卡组.zip
        ├── 我的卡组.md
        └── images/
            └── 图片.png
        -->

        ## 格式十一 · ZIP 结构

        问题：图片很多时应该怎么导入？

        答案：把 Markdown 和 images/ 文件夹一起打包成 ZIP，导入时直接选择 ZIP。

        <!--
        最后：以上格式可以混用。实际做卡组时，建议只保留一种主要结构，避免自动模式选错层级。
        如果每个穴位都被识别成章节，通常是因为用了三级标题；改成二级标题 + Q/A 即可按经络分组。
        -->
    """.trimIndent() + "\n"

    /** Saves to the public Downloads collection on Android 10+, app-specific Downloads on older systems. */
    fun save(context: Context): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "text/markdown")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Download/$FILE_NAME"
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return null
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = File(dir, FILE_NAME)
            file.writeText(content, Charsets.UTF_8)
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("text/markdown"), null)
            "Download/$FILE_NAME"
        }
    }.getOrNull()
}
