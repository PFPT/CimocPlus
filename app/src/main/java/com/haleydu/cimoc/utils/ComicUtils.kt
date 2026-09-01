package com.haleydu.cimoc.utils

import androidx.collection.LongSparseArray
import com.haleydu.cimoc.model.Comic

object ComicUtils {

    @JvmStatic
    fun buildComicMap(list: List<Comic>): LongSparseArray<Comic> {
        val array = LongSparseArray<Comic>()
        for (comic in list) {
            array.put(comic.id!!, comic)
        }
        return array
    }
}
