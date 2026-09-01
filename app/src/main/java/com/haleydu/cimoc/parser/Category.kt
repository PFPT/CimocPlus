package com.haleydu.cimoc.parser

import androidx.annotation.IntDef
import android.util.Pair

interface Category {

    fun isComposite(): Boolean

    fun getFormat(vararg args: String): String

    fun hasAttribute(@Attribute attr: Int): Boolean

    fun getAttrList(@Attribute attr: Int): List<Pair<String, String>>?

    @IntDef(CATEGORY_SUBJECT, CATEGORY_AREA, CATEGORY_READER, CATEGORY_YEAR, CATEGORY_PROGRESS, CATEGORY_ORDER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Attribute

    companion object {
        const val CATEGORY_SUBJECT = 0
        const val CATEGORY_AREA = 1
        const val CATEGORY_READER = 2
        const val CATEGORY_YEAR = 3
        const val CATEGORY_PROGRESS = 4
        const val CATEGORY_ORDER = 5
    }
}
