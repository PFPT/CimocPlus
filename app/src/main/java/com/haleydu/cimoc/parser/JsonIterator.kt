package com.haleydu.cimoc.parser

import com.haleydu.cimoc.model.Comic
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

abstract class JsonIterator(private val array: JSONArray?) : SearchIterator {

    private var index = 0

    override fun hasNext(): Boolean = index < array!!.length()

    override fun next(): Comic? {
        return try {
            parse(array!!.getJSONObject(index++))
        } catch (e: JSONException) {
            null
        }
    }

    override fun empty(): Boolean = array == null || array.length() == 0

    @Throws(JSONException::class)
    protected abstract fun parse(`object`: JSONObject): Comic?
}
