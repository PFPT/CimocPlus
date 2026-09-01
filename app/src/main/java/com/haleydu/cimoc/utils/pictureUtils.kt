package com.haleydu.cimoc.utils

object pictureUtils {

    @JvmStatic
    fun isPictureFormat(suffix: String): Boolean {
        val value = suffix.lowercase()
        return value == "bmp" ||
            value == "jpg" ||
            value == "png" ||
            value == "tif" ||
            value == "gif" ||
            value == "pcx" ||
            value == "tga" ||
            value == "exif" ||
            value == "fpx" ||
            value == "svg" ||
            value == "psd" ||
            value == "cdr" ||
            value == "pcd" ||
            value == "dxf" ||
            value == "ufo" ||
            value == "eps" ||
            value == "ai" ||
            value == "raw" ||
            value == "wmf" ||
            value == "webp"
    }
}
