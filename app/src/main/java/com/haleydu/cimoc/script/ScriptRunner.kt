package com.haleydu.cimoc.script

import app.cash.quickjs.QuickJs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRunner @Inject constructor(
    private val httpClient: JsHttpClient,
    private val jsoup: JsJsoup,
    private val console: JsConsole
) {

    fun interface Fn1 {
        fun call(a: String): String
    }

    fun interface Fn2 {
        fun call(a: String, b: String): String
    }

    fun interface Fn3 {
        fun call(a: String, b: String, c: String): String
    }

    suspend fun invoke(script: String, function: String, vararg args: Any?): String =
        withContext(Dispatchers.IO) {
            withTimeout(TIMEOUT_MS) {
                invokeBlocking(script, function, *args)
            }
        }

    fun invokeBlocking(script: String, function: String, vararg args: Any?): String {
        val engineRef = AtomicReference<QuickJs>()
        val future = executor.submit<String> {
            val engine = QuickJs.create()
            engineRef.set(engine)
            try {
                install(engine)
                engine.evaluate(script)
                val call = "$function(${args.joinToString(",") { jsLiteral(it) }})"
                val result = engine.evaluate("String($call)")
                result?.toString() ?: ""
            } finally {
                engine.close()
                engineRef.set(null)
            }
        }
        return try {
            future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS) ?: ""
        } catch (e: Exception) {
            future.cancel(true)
            try {
                engineRef.get()?.close()
            } catch (_: Exception) {
            }
            throw e
        }
    }

    fun hasFunction(script: String, function: String): Boolean {
        val engine = QuickJs.create()
        return try {
            install(engine)
            engine.evaluate(script)
            val result = engine.evaluate("typeof $function === 'function'")
            result == true
        } catch (_: Exception) {
            false
        } finally {
            engine.close()
        }
    }

    fun extractMeta(script: String): ScriptMeta {
        val engine = QuickJs.create()
        return try {
            install(engine)
            engine.evaluate(script)
            ScriptMeta(
                intValue(engine, "TYPE") ?: intValue(engine, "type"),
                stringValue(engine, "TITLE") ?: stringValue(engine, "title"),
                stringValue(engine, "VERSION") ?: stringValue(engine, "version")
            )
        } finally {
            engine.close()
        }
    }

    private fun install(engine: QuickJs) {
        val docs = HashMap<Int, org.jsoup.nodes.Document>()
        val elements = HashMap<Int, Element>()
        val nextId = AtomicInteger(1)
        engine.set(
            "__httpGet",
            Fn2::class.java,
            Fn2 { url, headers -> httpClient.get(url, headers.takeIf { it.isNotEmpty() }) }
        )
        engine.set(
            "__httpPost",
            Fn3::class.java,
            Fn3 { url, body, headers ->
                httpClient.post(url, body, headers.takeIf { it.isNotEmpty() })
            }
        )
        engine.set(
            "__consoleLog",
            Fn1::class.java,
            Fn1 { message ->
                console.log(message)
                ""
            }
        )
        engine.set(
            "__jsoupParse",
            Fn1::class.java,
            Fn1 { html ->
                val id = nextId.getAndIncrement()
                docs[id] = Jsoup.parse(html)
                id.toString()
            }
        )
        engine.set(
            "__docSelect",
            Fn2::class.java,
            Fn2 { id, css ->
                val doc = docs[id.toIntOrNull() ?: 0] ?: return@Fn2 "[]"
                val array = JSONArray()
                doc.select(css).forEach { element ->
                    val eid = nextId.getAndIncrement()
                    elements[eid] = element
                    array.put(eid)
                }
                array.toString()
            }
        )
        engine.set(
            "__docSelectFirst",
            Fn2::class.java,
            Fn2 { id, css ->
                val doc = docs[id.toIntOrNull() ?: 0] ?: return@Fn2 ""
                val element = doc.selectFirst(css) ?: return@Fn2 ""
                val eid = nextId.getAndIncrement()
                elements[eid] = element
                eid.toString()
            }
        )
        engine.set(
            "__docText",
            Fn1::class.java,
            Fn1 { id -> docs[id.toIntOrNull() ?: 0]?.text() ?: "" }
        )
        engine.set(
            "__docHtml",
            Fn1::class.java,
            Fn1 { id -> docs[id.toIntOrNull() ?: 0]?.html() ?: "" }
        )
        engine.set(
            "__docTitle",
            Fn1::class.java,
            Fn1 { id -> docs[id.toIntOrNull() ?: 0]?.title() ?: "" }
        )
        engine.set(
            "__elSelect",
            Fn2::class.java,
            Fn2 { id, css ->
                val el = elements[id.toIntOrNull() ?: 0] ?: return@Fn2 "[]"
                val array = JSONArray()
                el.select(css).forEach { child ->
                    val eid = nextId.getAndIncrement()
                    elements[eid] = child
                    array.put(eid)
                }
                array.toString()
            }
        )
        engine.set(
            "__elSelectFirst",
            Fn2::class.java,
            Fn2 { id, css ->
                val el = elements[id.toIntOrNull() ?: 0] ?: return@Fn2 ""
                val child = el.selectFirst(css) ?: return@Fn2 ""
                val eid = nextId.getAndIncrement()
                elements[eid] = child
                eid.toString()
            }
        )
        engine.set("__elText", Fn1::class.java, Fn1 { id -> elements[id.toIntOrNull() ?: 0]?.text() ?: "" })
        engine.set("__elOwnText", Fn1::class.java, Fn1 { id -> elements[id.toIntOrNull() ?: 0]?.ownText() ?: "" })
        engine.set("__elHtml", Fn1::class.java, Fn1 { id -> elements[id.toIntOrNull() ?: 0]?.html() ?: "" })
        engine.set("__elOuterHtml", Fn1::class.java, Fn1 { id -> elements[id.toIntOrNull() ?: 0]?.outerHtml() ?: "" })
        engine.set(
            "__elAttr",
            Fn2::class.java,
            Fn2 { id, name -> elements[id.toIntOrNull() ?: 0]?.attr(name) ?: "" }
        )
        engine.evaluate(HOST_JS)
    }

    private fun intValue(engine: QuickJs, name: String): Int? {
        return try {
            val value = engine.evaluate(name) ?: return null
            when (value) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull()
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun stringValue(engine: QuickJs, name: String): String? {
        return try {
            val value = engine.evaluate(name) ?: return null
            val text = value.toString()
            text.takeIf { it.isNotBlank() && it != "undefined" && it != "null" }
        } catch (_: Exception) {
            null
        }
    }

    private fun jsLiteral(value: Any?): String {
        return when (value) {
            null -> "null"
            is Number, is Boolean -> value.toString()
            else -> JSONObject.quote(value.toString())
        }
    }

    data class ScriptMeta(
        val type: Int?,
        val title: String?,
        val version: String?
    )

    companion object {
        private const val TIMEOUT_MS = 15000L
        private val executor = Executors.newCachedThreadPool()
        private const val HOST_JS = """
var http = {
  get: function(url, headers) {
    if (headers === undefined || headers === null) return __httpGet.call(url, '');
    return __httpGet.call(url, typeof headers === 'string' ? headers : JSON.stringify(headers));
  },
  post: function(url, body, headers) {
    if (headers === undefined || headers === null) return __httpPost.call(url, body, '');
    return __httpPost.call(url, body, typeof headers === 'string' ? headers : JSON.stringify(headers));
  }
};
var console = {
  log: function() {
    var out = [];
    for (var i = 0; i < arguments.length; i++) out.push(String(arguments[i]));
    __consoleLog.call(out.join(' '));
  }
};
function JsDocument(id) {
  this.id = id;
  this.select = function(css) {
    var ids = JSON.parse(__docSelect.call(String(this.id), css));
    var out = [];
    for (var i = 0; i < ids.length; i++) out.push(new JsElement(ids[i]));
    return out;
  };
  this.selectFirst = function(css) {
    var id = __docSelectFirst.call(String(this.id), css);
    return id ? new JsElement(id) : null;
  };
  this.text = function() { return __docText.call(String(this.id)); };
  this.html = function() { return __docHtml.call(String(this.id)); };
  this.title = function() { return __docTitle.call(String(this.id)); };
}
function JsElement(id) {
  this.id = id;
  this.select = function(css) {
    var ids = JSON.parse(__elSelect.call(String(this.id), css));
    var out = [];
    for (var i = 0; i < ids.length; i++) out.push(new JsElement(ids[i]));
    return out;
  };
  this.selectFirst = function(css) {
    var id = __elSelectFirst.call(String(this.id), css);
    return id ? new JsElement(id) : null;
  };
  this.text = function() { return __elText.call(String(this.id)); };
  this.ownText = function() { return __elOwnText.call(String(this.id)); };
  this.html = function() { return __elHtml.call(String(this.id)); };
  this.outerHtml = function() { return __elOuterHtml.call(String(this.id)); };
  this.attr = function(name) { return __elAttr.call(String(this.id), name); };
}
var jsoup = {
  parse: function(html) { return new JsDocument(__jsoupParse.call(html)); }
};
"""
    }
}
