package com.haleydu.cimoc.parser

import android.util.Pair
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_AREA
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_ORDER
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_PROGRESS
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_READER
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_YEAR

abstract class MangaCategory : Category {

    open override fun isComposite(): Boolean = false

    protected abstract fun getSubject(): List<Pair<String, String>>

    protected open fun hasArea(): Boolean = false

    protected open fun getArea(): List<Pair<String, String>>? = null

    protected open fun hasReader(): Boolean = false

    protected open fun getReader(): List<Pair<String, String>>? = null

    protected open fun hasProgress(): Boolean = false

    protected open fun getProgress(): List<Pair<String, String>>? = null

    protected open fun hasYear(): Boolean = false

    protected open fun getYear(): List<Pair<String, String>>? = null

    protected open fun hasOrder(): Boolean = false

    protected open fun getOrder(): List<Pair<String, String>>? = null

    open override fun hasAttribute(@Category.Attribute attr: Int): Boolean {
        return when (attr) {
            CATEGORY_SUBJECT -> true
            CATEGORY_AREA -> hasArea()
            CATEGORY_READER -> hasReader()
            CATEGORY_PROGRESS -> hasProgress()
            CATEGORY_YEAR -> hasYear()
            CATEGORY_ORDER -> hasOrder()
            else -> false
        }
    }

    open override fun getAttrList(@Category.Attribute attr: Int): List<Pair<String, String>>? {
        return when (attr) {
            CATEGORY_SUBJECT -> getSubject()
            CATEGORY_AREA -> getArea()
            CATEGORY_READER -> getReader()
            CATEGORY_PROGRESS -> getProgress()
            CATEGORY_YEAR -> getYear()
            CATEGORY_ORDER -> getOrder()
            else -> null
        }
    }
}
