package com.haleydu.cimoc.parser

import com.haleydu.cimoc.model.Comic

interface SearchIterator {
    fun empty(): Boolean
    fun hasNext(): Boolean
    fun next(): Comic?
}
