package com.haleydu.cimoc.utils

import android.content.ContentResolver
import com.haleydu.cimoc.saf.DocumentFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object DocumentUtils {

    @JvmStatic
    fun getOrCreateFile(parent: DocumentFile?, displayName: String): DocumentFile? {
        if (parent == null) return null
        val file = parent.findFile(displayName)
        if (file != null) {
            if (file.isFile) {
                return file
            }
            return null
        }
        return parent.createFile(displayName)
    }

    @JvmStatic
    fun findFile(parent: DocumentFile?, vararg filenames: String): DocumentFile? {
        var current = parent
        if (current != null) {
            for (filename in filenames) {
                current = current!!.findFile(filename)
                if (current == null) {
                    return null
                }
            }
        }
        return current
    }

    @JvmStatic
    fun countWithoutSuffix(dir: DocumentFile, suffix: String): Int {
        var count = 0
        if (dir.isDirectory) {
            for (file in dir.listFiles()) {
                if (file.isFile && !file.name.endsWith(suffix)) {
                    count++
                }
            }
        }
        return count
    }

    @JvmStatic
    fun listFilesWithSuffix(dir: DocumentFile, vararg suffix: String): Array<String> {
        val list = ArrayList<String>()
        if (dir.isDirectory) {
            for (file in dir.listFiles()) {
                if (file.isFile) {
                    val name = file.name
                    for (str in suffix) {
                        if (name.endsWith(str)) {
                            list.add(name)
                            break
                        }
                    }
                }
            }
        }
        return list.toTypedArray()
    }

    @JvmStatic
    fun getOrCreateSubDirectory(parent: DocumentFile?, displayName: String): DocumentFile? {
        if (parent == null) return null
        val file = parent.findFile(displayName)
        if (file != null) {
            if (file.isDirectory) {
                return file
            }
            return null
        }
        return parent.createDirectory(displayName)
    }

    @JvmStatic
    fun readLineFromFile(resolver: ContentResolver, file: DocumentFile?): String? {
        if (file == null) return null
        var input: java.io.InputStream? = null
        var reader: BufferedReader? = null
        try {
            input = resolver.openInputStream(file.uri)
            if (input != null) {
                reader = BufferedReader(InputStreamReader(input))
                return reader.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            closeStream(input, reader)
        }
        return null
    }

    @JvmStatic
    fun readCharFromFile(resolver: ContentResolver, file: DocumentFile, count: Int): CharArray? {
        var input: java.io.InputStream? = null
        var reader: BufferedReader? = null
        try {
            input = resolver.openInputStream(file.uri)
            if (input != null) {
                reader = BufferedReader(InputStreamReader(input))
                val buffer = CharArray(count)
                if (reader.read(buffer, 0, count) == count) {
                    return buffer
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            closeStream(input, reader)
        }
        return null
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeStringToFile(resolver: ContentResolver, file: DocumentFile?, data: String) {
        val target = file ?: throw IOException()
        var output: java.io.OutputStream? = null
        var writer: BufferedWriter? = null
        try {
            output = resolver.openOutputStream(target.uri)
            if (output != null) {
                writer = BufferedWriter(OutputStreamWriter(output))
                writer.write(data)
                writer.flush()
            } else {
                throw IOException()
            }
        } finally {
            closeStream(output, writer)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeBinaryToFile(resolver: ContentResolver, file: DocumentFile?, input: InputStream) {
        val target = file ?: throw IOException()
        var inputStream: BufferedInputStream? = null
        var outputStream: BufferedOutputStream? = null
        try {
            val output = resolver.openOutputStream(target.uri)
            if (output != null) {
                inputStream = BufferedInputStream(input, 8192)
                outputStream = BufferedOutputStream(output, 8192)
                val buffer = ByteArray(8192)
                var length: Int
                while (inputStream.read(buffer).also { length = it } != -1) {
                    outputStream.write(buffer, 0, length)
                }
                output.flush()
            } else {
                closeStream(input)
                throw FileNotFoundException()
            }
        } finally {
            closeStream(inputStream, outputStream)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeBinaryToFile(resolver: ContentResolver, src: DocumentFile, dst: DocumentFile) {
        val input = resolver.openInputStream(src.uri)
        writeBinaryToFile(resolver, dst, input!!)
    }

    private fun closeStream(vararg stream: Closeable?) {
        for (closeable in stream) {
            if (closeable != null) {
                try {
                    closeable.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
