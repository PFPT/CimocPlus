package com.haleydu.cimoc.saf

import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.Collections
import java.util.Comparator

internal class RawDocumentFile(parent: DocumentFile?, private var mFile: File) : DocumentFile(parent) {

    override fun createFile(displayName: String): DocumentFile? {
        val target = File(mFile, displayName)
        if (!target.exists()) {
            try {
                if (!target.createNewFile()) {
                    return null
                }
            } catch (e: IOException) {
                return null
            }
        }
        return RawDocumentFile(this, target)
    }

    override fun createDirectory(displayName: String): DocumentFile? {
        val target = File(mFile, displayName)
        if (target.isDirectory || target.mkdir()) {
            return RawDocumentFile(this, target)
        }
        return null
    }

    override val uri: Uri
        get() = Uri.fromFile(mFile)

    override val name: String
        get() = mFile.name

    override val type: String?
        get() {
            if (!mFile.isDirectory) {
                return getTypeForName(mFile.name)
            }
            return null
        }

    override val isDirectory: Boolean
        get() = mFile.isDirectory

    override val isFile: Boolean
        get() = mFile.isFile

    override fun length(): Long {
        return mFile.length()
    }

    override fun canRead(): Boolean {
        return mFile.canRead()
    }

    override fun canWrite(): Boolean {
        return mFile.canWrite()
    }

    override fun delete(): Boolean {
        deleteContents(mFile)
        return mFile.delete()
    }

    override fun exists(): Boolean {
        return mFile.exists()
    }

    @Throws(FileNotFoundException::class)
    override fun openInputStream(): InputStream {
        return BufferedInputStream(FileInputStream(mFile))
    }

    override fun listFiles(filter: DocumentFileFilter?, comp: Comparator<in DocumentFile>?): List<DocumentFile> {
        val results = ArrayList<DocumentFile>()
        val files = mFile.listFiles()
        if (files != null) {
            for (file in files) {
                val doc: DocumentFile = RawDocumentFile(this, file)
                if (filter == null || filter.call(doc)) {
                    results.add(doc)
                }
            }
        }
        if (comp != null) {
            Collections.sort(results, comp)
        }
        return results
    }

    override fun listFiles(): Array<DocumentFile> {
        val files = mFile.listFiles()
        return Array(files!!.size) { i -> RawDocumentFile(this, files[i]) }
    }

    override fun refresh() {}

    override fun findFile(displayName: String): DocumentFile? {
        for (file in listFiles()) {
            if (displayName == file.name) {
                return file
            }
        }
        return null
    }

    override fun renameTo(displayName: String): Boolean {
        val target = File(mFile.parentFile, displayName)
        return if (mFile.renameTo(target)) {
            mFile = target
            true
        } else {
            false
        }
    }

    companion object {
        private fun getTypeForName(name: String): String {
            val lastDot = name.lastIndexOf('.')
            if (lastDot >= 0) {
                val extension = name.substring(lastDot + 1).lowercase()
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (mime != null) {
                    return mime
                }
            }
            return "application/octet-stream"
        }

        private fun deleteContents(dir: File): Boolean {
            val files = dir.listFiles()
            var success = true
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        success = success and deleteContents(file)
                    }
                    if (!file.delete()) {
                        success = false
                    }
                }
            }
            return success
        }
    }
}
