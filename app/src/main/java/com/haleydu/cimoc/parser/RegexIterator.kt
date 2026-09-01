package com.haleydu.cimoc.parser

import com.haleydu.cimoc.model.Comic
import java.util.regex.Matcher

abstract class RegexIterator protected constructor(private val match: Matcher) : SearchIterator {

    override fun hasNext(): Boolean = match.find()

    override fun next(): Comic? = parse(match)

    override fun empty(): Boolean = false

    protected abstract fun parse(match: Matcher): Comic?
}
