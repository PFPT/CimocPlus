package com.haleydu.cimoc.saf

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Comparator

abstract class DocumentFile internal constructor(private val mParent: DocumentFile?) {

    abstract fun createFile(displayName: String): DocumentFile?
    abstract fun createDirectory(displayName: String): DocumentFile?
    abstract val uri: Uri
    abstract val name: String
    abstract val type: String?
    val parentFile: DocumentFile?
        get() = mParent
    abstract val isDirectory: Boolean
    abstract val isFile: Boolean
    abstract fun length(): Long
    abstract fun canRead(): Boolean
    abstract fun canWrite(): Boolean
    abstract fun delete(): Boolean
    abstract fun exists(): Boolean
    @Throws(FileNotFoundException::class)
    abstract fun openInputStream(): InputStream
    fun listFiles(filter: DocumentFileFilter?): List<DocumentFile> {
        return listFiles(filter, null)
    }
    fun listFiles(comp: Comparator<in DocumentFile>): Array<DocumentFile> {
        val files = listFiles()
        files.sortWith(comp)
        return files
    }
    abstract fun listFiles(filter: DocumentFileFilter?, comp: Comparator<in DocumentFile>?): List<DocumentFile>
    abstract fun listFiles(): Array<DocumentFile>
    abstract fun refresh()
    abstract fun findFile(displayName: String): DocumentFile?
    abstract fun renameTo(displayName: String): Boolean

    fun interface DocumentFileFilter {
        fun call(file: DocumentFile): Boolean
    }

    companion object {
        @JvmStatic
        fun fromFile(file: File): DocumentFile {
            return RawDocumentFile(null, file)
        }

        @JvmStatic
        fun fromTreeUri(context: Context, treeUri: Uri): DocumentFile? {
            if (Build.VERSION.SDK_INT >= 21) {
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
                return TreeDocumentFile(null, context, documentUri)
            }
            return null
        }

        @JvmStatic
        fun fromSubTreeUri(context: Context, uri: Uri): DocumentFile? {
            if (Build.VERSION.SDK_INT >= 21) {
                return TreeDocumentFile(null, context, uri)
            }
            return null
        }
    }
}
