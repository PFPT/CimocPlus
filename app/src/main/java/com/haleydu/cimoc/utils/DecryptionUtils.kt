package com.haleydu.cimoc.utils

import android.util.Base64
import org.mozilla.javascript.ClassShutter
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.security.Key
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object DecryptionUtils {

    @JvmStatic
    @Throws(Exception::class)
    fun decryptAES(value: String, key: String): String {
        val secretKey = SecretKeySpec(key.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val code = Base64.decode(value, Base64.NO_WRAP)
        return String(cipher.doFinal(code))
    }

    @JvmStatic
    @Throws(Exception::class)
    fun desDecrypt(keyString: String, cipherString: String): String {
        val cipherBytes = Base64.decode(cipherString, Base64.DEFAULT)
        val keySpec = DESKeySpec(keyString.toByteArray())
        val key: Key = SecretKeyFactory.getInstance("DES").generateSecret(keySpec)
        val cipher = Cipher.getInstance("DES")
        cipher.init(Cipher.DECRYPT_MODE, key)
        val result = cipher.doFinal(cipherBytes)
        return String(result, charset("UTF-8"))
    }

    @JvmStatic
    @Throws(Exception::class)
    fun aesDecrypt(value: String, key: String, ivs: String): String {
        val secretKey = SecretKeySpec(key.toByteArray(charset("UTF-8")), "AES")
        val iv = IvParameterSpec(ivs.toByteArray(charset("UTF-8")))
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
        val code = Base64.decode(value, Base64.DEFAULT)
        return String(cipher.doFinal(code))
    }

    @JvmStatic
    @Throws(Exception::class)
    fun aesCbcDecryptEmbeddedIv(base64: String, key: String): String? {
        val raw = Base64.decode(base64, Base64.DEFAULT)
        if (raw == null || raw.size <= 16) {
            return null
        }
        val iv = Arrays.copyOfRange(raw, 0, 16)
        val ct = Arrays.copyOfRange(raw, 16, raw.size)
        val secretKey = SecretKeySpec(key.toByteArray(charset("UTF-8")), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return String(cipher.doFinal(ct), charset("UTF-8"))
    }

    @JvmStatic
    @Throws(UnsupportedEncodingException::class)
    fun base64Decrypt(cipherString: String): String {
        val cipherBytes = Base64.decode(cipherString, Base64.DEFAULT)
        return String(cipherBytes, charset("UTF-8"))
    }

    @JvmStatic
    fun evalDecrypt(jsCode: String): String {
        return evalDecrypt(jsCode, null)
    }

    @JvmStatic
    fun evalDecrypt(jsCode: String, varName: String?): String {
        val rhino = Context.enter()
        try {
            rhino.optimizationLevel = -1
            val scope: Scriptable = rhino.initSafeStandardObjects()
            val setter = rhino.classShutterSetter
            if (setter != null) {
                setter.setClassShutter(ClassShutter { false })
            }
            try {
                val `object` = rhino.evaluateString(scope, jsCode, null, 1, null)
                return if (varName == null) {
                    Context.toString(`object`)
                } else {
                    val jsObject = scope.get(varName, scope)
                    Context.toString(jsObject)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return ""
            }
        } finally {
            Context.exit()
        }
    }

    @JvmStatic
    fun urlDecrypt(str: String): String {
        return try {
            URLDecoder.decode(str, "utf-8")
        } catch (e: Exception) {
            str
        }
    }

    @JvmStatic
    fun LZ64Decrypt(str: String?): String? {
        val valStrBase64 = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            62, 0, 0, 0, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 0, 0, 0, 64, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
            0, 0, 0, 0, 0, 0, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
        ).map { it.toChar() }.toCharArray()
        if (str == null) {
            return null
        }
        if (str.isEmpty()) {
            return ""
        }
        val input = str.toCharArray()
        return LZ64Decrypt(input.size, 32, input, valStrBase64, 0)
    }

    private fun LZ64Decrypt(length: Int, resetValue: Int, getNextValue: CharArray, modify: CharArray?, offset: Int): String {
        val dictionary = ArrayList<String>()
        var enlargeIn = 4
        var dictSize = 4
        var numBits = 3
        var position = resetValue
        var index = 1
        var resb: Int
        var maxpower: Int
        var power: Int
        var entry: String
        var w: String
        var c: String
        val result = ArrayList<String>()
        var bits: Char
        var `val` = if (modify == null) (getNextValue[0].code + offset).toChar() else modify[getNextValue[0].code]

        var i = 0.toChar()
        while (i.code < 3) {
            dictionary.add(i.code, i.toString())
            i++
        }

        bits = 0.toChar()
        maxpower = 2
        power = 0
        while (power != maxpower) {
            resb = `val`.code and position
            position = position shr 1
            if (position == 0) {
                position = resetValue
                `val` = if (modify == null) (getNextValue[index++].code + offset).toChar() else modify[getNextValue[index++].code]
            }
            bits = (bits.code or ((if (resb > 0) 1 else 0) shl power)).toChar()
            power++
        }

        when (bits.code) {
            0 -> {
                bits = 0.toChar()
                maxpower = 8
                power = 0
                while (power != maxpower) {
                    resb = `val`.code and position
                    position = position shr 1
                    if (position == 0) {
                        position = resetValue
                        `val` = if (modify == null) (getNextValue[index++].code + offset).toChar() else modify[getNextValue[index++].code]
                    }
                    bits = (bits.code or ((if (resb > 0) 1 else 0) shl power)).toChar()
                    power++
                }
                c = bits.toString()
            }
            1 -> {
                bits = 0.toChar()
                maxpower = 16
                power = 0
                while (power != maxpower) {
                    resb = `val`.code and position
                    position = position shr 1
                    if (position == 0) {
                        position = resetValue
                        `val` = if (modify == null) (getNextValue[index++].code + offset).toChar() else modify[getNextValue[index++].code]
                    }
                    bits = (bits.code or ((if (resb > 0) 1 else 0) shl power)).toChar()
                    power++
                }
                c = bits.toString()
            }
            else -> return ""
        }
        dictionary.add(c)
        w = c
        result.add(w)
        while (true) {
            if (index > length) {
                return ""
            }

            bits = 0.toChar()
            maxpower = numBits
            power = 0
            while (power != maxpower) {
                resb = `val`.code and position
                position = position shr 1
                if (position == 0) {
                    position = resetValue
                    `val` = if (modify == null) (getNextValue[index++].code + offset).toChar() else modify[getNextValue[index++].code]
                }
                bits = (bits.code or ((if (resb > 0) 1 else 0) shl power)).toChar()
                power++
            }
            var cc: Int
            cc = bits.code
            when (cc) {
                0 -> {
                    bits = 0.toChar()
                    maxpower = 8
                    power = 0
                    while (power != maxpower) {
                        resb = `val`.code and position
                        position = position shr 1
                        if (position == 0) {
                            position = resetValue
                            `val` = if (modify == null) (getNextValue[index++].code + offset).toChar() else modify[getNextValue[index++].code]
                        }
                        bits = (bits.code or ((if (resb > 0) 1 else 0) shl power)).toChar()
                        power++
                    }
                    dictionary.add(bits.toString())
                    cc = dictSize++
                    enlargeIn--
                }
                1 -> {
                    bits = 0.toChar()
                    maxpower = 16
                    power = 0
                    while (power != maxpower) {
                        resb = `val`.code and position
                        position = position shr 1
                        if (position == 0) {
                            position = resetValue
                            `val` = if (modify == null) (getNextValue[index++].code + offset).toChar() else modify[getNextValue[index++].code]
                        }
                        bits = (bits.code or ((if (resb > 0) 1 else 0) shl power)).toChar()
                        power++
                    }
                    dictionary.add(bits.toString())
                    cc = dictSize++
                    enlargeIn--
                }
                2 -> {
                    val sb = StringBuilder(result.size)
                    for (s in result) {
                        sb.append(s)
                    }
                    return sb.toString()
                }
            }

            if (enlargeIn == 0) {
                enlargeIn = 1 shl numBits
                numBits++
            }

            if (cc < dictionary.size && dictionary[cc] != null) {
                entry = dictionary[cc]
            } else {
                if (cc == dictSize) {
                    entry = w + w[0]
                } else {
                    return ""
                }
            }
            result.add(entry)

            dictionary.add(w + entry[0])
            dictSize++
            enlargeIn--

            w = entry

            if (enlargeIn == 0) {
                enlargeIn = 1 shl numBits
                numBits++
            }
        }
    }
}
