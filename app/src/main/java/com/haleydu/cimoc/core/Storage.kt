package com.haleydu.cimoc.core

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.saf.DocumentFile
import com.haleydu.cimoc.utils.DecryptionUtils
import com.haleydu.cimoc.utils.DocumentUtils
import com.haleydu.cimoc.utils.StringUtils
import java.io.File
import java.io.IOException
import java.io.InputStream

object Storage {

    private const val DOWNLOAD = "download"
    private const val PICTURE = "picture"
    private const val BACKUP = "backup"

    @JvmStatic
    fun initRoot(context: Context, uri: String?): DocumentFile? {
        return if (uri.isNullOrEmpty()) {
            var dir = context.getExternalFilesDir(null)
            if (dir == null) {
                dir = context.filesDir
            }
            val file = File(dir, "Cimoc")
            if (file.exists() || file.mkdirs()) {
                DocumentFile.fromFile(file)
            } else {
                null
            }
        } else if (uri.startsWith("content")) {
            DocumentFile.fromTreeUri(context, Uri.parse(uri))
        } else if (uri.startsWith("file")) {
            DocumentFile.fromFile(File(Uri.parse(uri).path))
        } else {
            DocumentFile.fromFile(File(uri, "Cimoc"))
        }
    }

    fun interface ProgressCallback {
        fun onProgress(message: String)
    }

    private fun copyFile(
        resolver: ContentResolver,
        src: DocumentFile,
        parent: DocumentFile?,
        callback: ProgressCallback
    ): Boolean {
        val dest = parent ?: return false
        val file = DocumentUtils.getOrCreateFile(dest, src.name)
        if (file != null) {
            callback.onProgress(StringUtils.format("正在移动 %s...", src.uri.lastPathSegment))
            try {
                DocumentUtils.writeBinaryToFile(resolver, src, file)
                return true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return false
    }

    private fun copyDir(
        resolver: ContentResolver,
        src: DocumentFile,
        parent: DocumentFile?,
        callback: ProgressCallback
    ): Boolean {
        val dest = parent ?: return false
        if (src.isDirectory) {
            val dir = DocumentUtils.getOrCreateSubDirectory(dest, src.name)
            for (file in src.listFiles()) {
                if (file.isDirectory) {
                    if (!copyDir(resolver, file, dir, callback)) {
                        return false
                    }
                } else if (!copyFile(resolver, file, dir, callback)) {
                    return false
                }
            }
        }
        return true
    }

    private fun copyDir(
        resolver: ContentResolver,
        src: DocumentFile,
        dst: DocumentFile,
        name: String,
        callback: ProgressCallback
    ): Boolean {
        val file = src.findFile(name)
        if (file != null && file.isDirectory) {
            return copyDir(resolver, file, dst, callback)
        }
        return true
    }

    private fun deleteDir(parent: DocumentFile, name: String, callback: ProgressCallback) {
        val file = parent.findFile(name)
        if (file != null && file.isDirectory) {
            callback.onProgress(StringUtils.format("正在删除 %s", file.uri.lastPathSegment))
            file.delete()
        }
    }

    private fun isDirSame(root: DocumentFile, dst: DocumentFile): Boolean {
        return root.uri.scheme == "file" && dst.uri.path!!.endsWith("primary:Cimoc") ||
            root.uri.path == dst.uri.path
    }

    @JvmStatic
    fun moveRootDir(
        resolver: ContentResolver,
        root: DocumentFile?,
        dst: DocumentFile,
        callback: ProgressCallback
    ) {
        val src = root ?: throw RuntimeException()
        if (dst.canRead() && !isDirSame(src, dst)) {
            src.refresh()
            if (copyDir(resolver, src, dst, BACKUP, callback) &&
                copyDir(resolver, src, dst, DOWNLOAD, callback) &&
                copyDir(resolver, src, dst, PICTURE, callback)
            ) {
                deleteDir(src, BACKUP, callback)
                deleteDir(src, DOWNLOAD, callback)
                deleteDir(src, PICTURE, callback)
                return
            }
        }
        throw RuntimeException()
    }

    @JvmStatic
    fun savePicture(
        resolver: ContentResolver,
        root: DocumentFile?,
        stream: InputStream,
        filename: String
    ): Uri {
        try {
            val dir = DocumentUtils.getOrCreateSubDirectory(root, PICTURE)
            if (dir != null) {
                val file = DocumentUtils.getOrCreateFile(dir, filename)
                DocumentUtils.writeBinaryToFile(resolver, file, stream)
                return file?.uri ?: throw RuntimeException()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        throw RuntimeException()
    }

    @JvmStatic
    fun buildImageUrlFromDocumentFile(
        list: List<DocumentFile>,
        chapterStr: String?,
        max: Int,
        chapter: Chapter
    ): List<ImageUrl> {
        var count = 0
        val result = ArrayList<ImageUrl>(list.size)
        for (file in list) {
            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true
            try {
                BitmapFactory.decodeStream(file.openInputStream(), null, opts)
                var uri = file.uri.toString()
                if (uri.startsWith("file")) {
                    uri = DecryptionUtils.urlDecrypt(uri)
                }
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "300" + count).toLong()
                val image = ImageUrl(id, chapter.sourceComic, ++count, uri, false)
                image.height = opts.outHeight
                image.width = opts.outWidth
                image.chapter = chapterStr
                result.add(image)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (count >= max) {
                break
            }
        }
        return result
    }
}
