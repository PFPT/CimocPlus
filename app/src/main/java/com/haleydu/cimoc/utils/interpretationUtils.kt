package com.haleydu.cimoc.utils

import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.source.CCMH
import com.haleydu.cimoc.source.Cartoonmad
import com.haleydu.cimoc.source.DM5
import com.haleydu.cimoc.source.MH50
import com.haleydu.cimoc.source.MiGu

object interpretationUtils {

    @JvmStatic
    fun isReverseOrder(comic: Comic): Boolean {
        val type = comic.source
        return type == MH50.TYPE ||
            type == MiGu.TYPE ||
            type == CCMH.TYPE ||
            type == Cartoonmad.TYPE ||
            type == DM5.TYPE
    }
}
