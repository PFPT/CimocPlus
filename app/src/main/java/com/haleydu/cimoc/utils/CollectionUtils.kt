package com.haleydu.cimoc.utils

object CollectionUtils {

    fun interface Mapper<T, R> {
        fun map(value: T): R
    }

    @JvmStatic
    fun <E> minus(lhs: Collection<E>, rhs: Collection<E>): Collection<E> {
        val collection = ArrayList(lhs)
        (lhs as MutableCollection<E>).removeAll(rhs)
        return collection
    }

    @JvmStatic
    fun <T, R> map(origin: Collection<T>, func: Mapper<T, R>): List<R> {
        val result = ArrayList<R>(origin.size)
        for (element in origin) {
            result.add(func.map(element))
        }
        return result
    }

    @JvmStatic
    fun unbox(list: List<Int>): IntArray {
        val result = IntArray(list.size)
        for (i in result.indices) {
            result[i] = list[i]
        }
        return result
    }
}
