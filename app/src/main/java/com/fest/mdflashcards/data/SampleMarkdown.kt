package com.fest.mdflashcards.data

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
        # 示例笔记 · 计算机网络

        这是一份用来体验拆卡的示例 Markdown。可以直接修改它，或让 AI 按这个结构扩写后重新导入。

        ## TCP 三次握手

        客户端发送 **SYN**，服务端回复 **SYN+ACK**，客户端再回 **ACK**。

        ### 为什么不是两次

        防止历史连接请求造成资源浪费，同时确认双方收发能力。

        ## HTTP 与 HTTPS 区别

        HTTPS 在 HTTP 与 TCP 之间加了 TLS 层，提供加密、身份认证与完整性校验。

        ## 进程与线程

        进程是资源分配的基本单位，线程是 CPU 调度的基本单位；同一进程内的线程共享地址空间。

        ## 常见状态码

        | 状态码 | 含义 |
        | --- | --- |
        | 200 | 成功 |
        | 301 | 永久重定向 |
        | 404 | 资源不存在 |
        | 500 | 服务器内部错误 |

        ## 问答示例

        Q: 为什么数据库索引常用 B+ 树？
        A: B+ 树矮胖、磁盘 IO 少，叶子节点链表适合范围查询。

        Q: 什么是虚拟内存？
        A: 把物理内存抽象成连续的地址空间，让每个进程都以为自己独占内存。
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
