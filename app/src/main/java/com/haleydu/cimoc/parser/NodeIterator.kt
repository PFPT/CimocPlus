package com.haleydu.cimoc.parser

import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.soup.Node

abstract class NodeIterator protected constructor(list: List<Node>) : SearchIterator {

    private val iterator: Iterator<Node>? = if (list.isEmpty()) null else list.iterator()

    override fun hasNext(): Boolean = iterator!!.hasNext()

    override fun next(): Comic? = parse(iterator!!.next())

    override fun empty(): Boolean = iterator == null

    protected abstract fun parse(node: Node): Comic?
}
