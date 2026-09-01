package com.haleydu.cimoc.saf

import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Collections
import java.util.Comparator

@RequiresApi(21)
@TargetApi(21)
internal class TreeDocumentFile : DocumentFile {
    private val mContext: android.content.Context
    private var mUri: Uri
    private var mDisplayName: String? = null
    private var mMimeType: String? = null
    private var mSubFiles: MutableMap<String, DocumentFile>? = null

    private constructor(
        parent: DocumentFile?,
        context: android.content.Context,
        uri: Uri,
        displayName: String?,
        mimeType: String?
    ) : super(parent) {
        mContext = context
        mUri = uri
        mDisplayName = displayName
        mMimeType = mimeType
    }

    constructor(parent: DocumentFile?, context: android.content.Context, uri: Uri) : super(parent) {
        mContext = context
        mUri = uri
        query()
    }

    private fun list() {
        mSubFiles = HashMap()
        val resolver = mContext.contentResolver
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mUri, DocumentsContract.getDocumentId(mUri))
        var c: Cursor? = null
        try {
            c = resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )
            while (c!!.moveToNext()) {
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(mUri, c.getString(0))
                val displayName = c.getString(1)
                mSubFiles!![displayName!!] = TreeDocumentFile(this, mContext, documentUri, displayName, c.getString(2))
            }
        } finally {
            closeQuietly(c)
        }
    }

    private fun query() {
        var c: Cursor? = null
        try {
            c = mContext.contentResolver.query(
                mUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )
            if (c != null && c.moveToNext()) {
                mDisplayName = c.getString(0)
                mMimeType = c.getString(1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            closeQuietly(c)
        }
    }

    override fun createFile(displayName: String): DocumentFile? {
        if (!checkSubFiles()) {
            return null
        }
        var doc = findFile(displayName)
        if (doc != null) {
            return null
        }
        try {
            val result = DocumentsContract.createDocument(mContext.contentResolver, mUri, null, displayName)
            if (result != null) {
                doc = TreeDocumentFile(this, mContext, result, displayName, null)
                mSubFiles!![displayName] = doc
            }
        } catch (e: FileNotFoundException) {
        }
        return doc
    }

    override fun createDirectory(displayName: String): DocumentFile? {
        if (!checkSubFiles()) {
            return null
        }
        var doc = findFile(displayName)
        if (doc != null) {
            return null
        }
        try {
            val result = DocumentsContract.createDocument(
                mContext.contentResolver,
                mUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                displayName
            )
            if (result != null) {
                doc = TreeDocumentFile(this, mContext, result, displayName, DocumentsContract.Document.MIME_TYPE_DIR)
                mSubFiles!![displayName] = doc
            }
        } catch (e: FileNotFoundException) {
        }
        return doc
    }

    override val uri: Uri
        get() = mUri

    override val name: String
        get() = mDisplayName ?: ""

    override val type: String?
        get() = mMimeType

    override val isDirectory: Boolean
        get() = DocumentsContract.Document.MIME_TYPE_DIR == mMimeType

    override val isFile: Boolean
        get() = !isDirectory

    override fun length(): Long {
        var c: Cursor? = null
        try {
            c = mContext.contentResolver.query(
                mUri,
                arrayOf(DocumentsContract.Document.COLUMN_SIZE),
                null,
                null,
                null
            )
            return if (c!!.moveToFirst() && !c.isNull(0)) {
                c.getLong(0)
            } else {
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        } finally {
            closeQuietly(c)
        }
    }

    override fun canRead(): Boolean {
        return mContext.checkCallingOrSelfUriPermission(mUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun canWrite(): Boolean {
        return mContext.checkCallingOrSelfUriPermission(mUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun delete(): Boolean {
        try {
            if (DocumentsContract.deleteDocument(mContext.contentResolver, mUri)) {
                (parentFile as TreeDocumentFile).mSubFiles!!.remove(mDisplayName ?: "")
                return true
            }
        } catch (e: FileNotFoundException) {
        }
        return false
    }

    override fun exists(): Boolean {
        val resolver = mContext.contentResolver
        var c: Cursor? = null
        return try {
            c = resolver.query(
                mUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null
            )
            c != null && c.count > 0
        } finally {
            closeQuietly(c)
        }
    }

    @Throws(FileNotFoundException::class)
    override fun openInputStream(): InputStream {
        return mContext.contentResolver.openInputStream(mUri)!!
    }

    override fun listFiles(filter: DocumentFileFilter?, comp: Comparator<in DocumentFile>?): List<DocumentFile> {
        if (!checkSubFiles()) {
            return ArrayList()
        }
        val iterator = mSubFiles!!.entries.iterator()
        val list = ArrayList<DocumentFile>(mSubFiles!!.size)
        while (iterator.hasNext()) {
            val file = iterator.next().value
            if (filter == null || filter.call(file)) {
                list.add(file)
            }
        }
        if (comp != null) {
            Collections.sort(list, comp)
        }
        return list
    }

    override fun listFiles(): Array<DocumentFile> {
        if (!checkSubFiles()) {
            return arrayOf()
        }
        val size = mSubFiles!!.size
        val iterator = mSubFiles!!.entries.iterator()
        return Array(size) { iterator.next().value }
    }

    override fun refresh() {
        if (mSubFiles != null) {
            mSubFiles!!.clear()
            list()
        }
    }

    override fun findFile(displayName: String): DocumentFile? {
        if (!checkSubFiles()) {
            return null
        }
        return mSubFiles!![displayName]
    }

    override fun renameTo(displayName: String): Boolean {
        try {
            val result = DocumentsContract.renameDocument(mContext.contentResolver, mUri, displayName)
            if (result != null) {
                mUri = result
                return true
            }
        } catch (e: FileNotFoundException) {
        }
        return false
    }

    private fun checkSubFiles(): Boolean {
        if (!isDirectory) {
            return false
        }
        if (mSubFiles == null) {
            list()
        }
        return true
    }

    companion object {
        private fun closeQuietly(closeable: AutoCloseable?) {
            if (closeable != null) {
                try {
                    closeable.close()
                } catch (rethrown: RuntimeException) {
                    throw rethrown
                } catch (ignored: Exception) {
                }
            }
        }
    }
}
