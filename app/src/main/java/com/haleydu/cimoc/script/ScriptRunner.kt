package com.haleydu.cimoc.script

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRunner @Inject constructor(
    private val httpClient: JsHttpClient,
    private val jsoup: JsJsoup,
    private val console: JsConsole
) {

    suspend fun invoke(script: String, function: String, vararg args: Any?): String =
        withContext(Dispatchers.IO) {
            withTimeout(TIMEOUT_MS) {
                invokeBlocking(script, function, *args)
            }
        }

    fun invokeBlocking(script: String, function: String, vararg args: Any?): String {
        val future = executor.submit<String> {
            withEngine { context, scope ->
                context.evaluateString(scope, script, "script", 1, null)
                val call = "$function(${args.joinToString(",") { jsLiteral(it) }})"
                val result = context.evaluateString(scope, "String($call)", "call", 1, null)
                Context.toString(result) ?: ""
            }
        }
        return try {
            future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS) ?: ""
        } catch (e: Exception) {
            future.cancel(true)
            throw e
        }
    }

    fun hasFunction(script: String, function: String): Boolean {
        return try {
            withEngine { context, scope ->
                context.evaluateString(scope, script, "script", 1, null)
                val result = context.evaluateString(
                    scope,
                    "typeof $function === 'function'",
                    "check",
                    1,
                    null
                )
                Context.toBoolean(result)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun extractMeta(script: String): ScriptMeta {
        return withEngine { context, scope ->
            context.evaluateString(scope, script, "script", 1, null)
            ScriptMeta(
                intValue(context, scope, "TYPE") ?: intValue(context, scope, "type"),
                stringValue(context, scope, "TITLE") ?: stringValue(context, scope, "title"),
                stringValue(context, scope, "VERSION") ?: stringValue(context, scope, "version")
            )
        }
    }

    private fun <T> withEngine(block: (Context, Scriptable) -> T): T {
        val context = Context.enter()
        try {
            context.optimizationLevel = -1
            val scope = context.initSafeStandardObjects()
            install(scope)
            return block(context, scope)
        } finally {
            Context.exit()
        }
    }

    private fun install(scope: Scriptable) {
        val docs = HashMap<Int, org.jsoup.nodes.Document>()
        val elements = HashMap<Int, Element>()
        val nextId = AtomicInteger(1)
        putFn(scope, "__httpGet") { args ->
            httpClient.get(arg(args, 0), arg(args, 1).takeIf { it.isNotEmpty() })
        }
        putFn(scope, "__httpPost") { args ->
            httpClient.post(
                arg(args, 0),
                arg(args, 1),
                arg(args, 2).takeIf { it.isNotEmpty() }
            )
        }
        putFn(scope, "__consoleLog") { args ->
            console.log(arg(args, 0))
            ""
        }
        putFn(scope, "__jsoupParse") { args ->
            val id = nextId.getAndIncrement()
            docs[id] = Jsoup.parse(arg(args, 0))
            id.toString()
        }
        putFn(scope, "__docSelect") { args ->
            val doc = docs[arg(args, 0).toIntOrNull() ?: 0] ?: return@putFn "[]"
            val array = JSONArray()
            doc.select(arg(args, 1)).forEach { element ->
                val eid = nextId.getAndIncrement()
                elements[eid] = element
                array.put(eid)
            }
            array.toString()
        }
        putFn(scope, "__docSelectFirst") { args ->
            val doc = docs[arg(args, 0).toIntOrNull() ?: 0] ?: return@putFn ""
            val element = doc.selectFirst(arg(args, 1)) ?: return@putFn ""
            val eid = nextId.getAndIncrement()
            elements[eid] = element
            eid.toString()
        }
        putFn(scope, "__docText") { args ->
            docs[arg(args, 0).toIntOrNull() ?: 0]?.text() ?: ""
        }
        putFn(scope, "__docHtml") { args ->
            docs[arg(args, 0).toIntOrNull() ?: 0]?.html() ?: ""
        }
        putFn(scope, "__docTitle") { args ->
            docs[arg(args, 0).toIntOrNull() ?: 0]?.title() ?: ""
        }
        putFn(scope, "__elSelect") { args ->
            val el = elements[arg(args, 0).toIntOrNull() ?: 0] ?: return@putFn "[]"
            val array = JSONArray()
            el.select(arg(args, 1)).forEach { child ->
                val eid = nextId.getAndIncrement()
                elements[eid] = child
                array.put(eid)
            }
            array.toString()
        }
        putFn(scope, "__elSelectFirst") { args ->
            val el = elements[arg(args, 0).toIntOrNull() ?: 0] ?: return@putFn ""
            val child = el.selectFirst(arg(args, 1)) ?: return@putFn ""
            val eid = nextId.getAndIncrement()
            elements[eid] = child
            eid.toString()
        }
        putFn(scope, "__elText") { args ->
            elements[arg(args, 0).toIntOrNull() ?: 0]?.text() ?: ""
        }
        putFn(scope, "__elOwnText") { args ->
            elements[arg(args, 0).toIntOrNull() ?: 0]?.ownText() ?: ""
        }
        putFn(scope, "__elHtml") { args ->
            elements[arg(args, 0).toIntOrNull() ?: 0]?.html() ?: ""
        }
        putFn(scope, "__elOuterHtml") { args ->
            elements[arg(args, 0).toIntOrNull() ?: 0]?.outerHtml() ?: ""
        }
        putFn(scope, "__elAttr") { args ->
            elements[arg(args, 0).toIntOrNull() ?: 0]?.attr(arg(args, 1)) ?: ""
        }
        Context.getCurrentContext().evaluateString(scope, HOST_JS, "host", 1, null)
    }

    private fun putFn(scope: Scriptable, name: String, impl: (Array<out Any?>) -> String) {
        ScriptableObject.putProperty(scope, name, object : BaseFunction() {
            override fun call(
                cx: Context,
                scope: Scriptable,
                thisObj: Scriptable,
                args: Array<out Any>
            ): Any {
                return impl(args)
            }
        })
    }

    private fun arg(args: Array<out Any?>, index: Int): String {
        if (index >= args.size) return ""
        val value = args[index] ?: return ""
        if (Undefined.isUndefined(value)) return ""
        return Context.toString(value)
    }

    private fun intValue(context: Context, scope: Scriptable, name: String): Int? {
        return try {
            val value = context.evaluateString(scope, name, name, 1, null) ?: return null
            if (Undefined.isUndefined(value)) return null
            when (value) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull()
                else -> Context.toString(value).toIntOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun stringValue(context: Context, scope: Scriptable, name: String): String? {
        return try {
            val value = context.evaluateString(scope, name, name, 1, null) ?: return null
            if (Undefined.isUndefined(value)) return null
            val text = Context.toString(value)
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
    if (headers === undefined || headers === null) return __httpGet(url, '');
    return __httpGet(url, typeof headers === 'string' ? headers : JSON.stringify(headers));
  },
  post: function(url, body, headers) {
    if (headers === undefined || headers === null) return __httpPost(url, body, '');
    return __httpPost(url, body, typeof headers === 'string' ? headers : JSON.stringify(headers));
  }
};
var console = {
  log: function() {
    var out = [];
    for (var i = 0; i < arguments.length; i++) out.push(String(arguments[i]));
    __consoleLog(out.join(' '));
  }
};
function JsDocument(id) {
  this.id = id;
  this.select = function(css) {
    var ids = JSON.parse(__docSelect(String(this.id), css));
    var out = [];
    for (var i = 0; i < ids.length; i++) out.push(new JsElement(ids[i]));
    return out;
  };
  this.selectFirst = function(css) {
    var id = __docSelectFirst(String(this.id), css);
    return id ? new JsElement(id) : null;
  };
  this.text = function() { return __docText(String(this.id)); };
  this.html = function() { return __docHtml(String(this.id)); };
  this.title = function() { return __docTitle(String(this.id)); };
}
function JsElement(id) {
  this.id = id;
  this.select = function(css) {
    var ids = JSON.parse(__elSelect(String(this.id), css));
    var out = [];
    for (var i = 0; i < ids.length; i++) out.push(new JsElement(ids[i]));
    return out;
  };
  this.selectFirst = function(css) {
    var id = __elSelectFirst(String(this.id), css);
    return id ? new JsElement(id) : null;
  };
  this.text = function() { return __elText(String(this.id)); };
  this.ownText = function() { return __elOwnText(String(this.id)); };
  this.html = function() { return __elHtml(String(this.id)); };
  this.outerHtml = function() { return __elOuterHtml(String(this.id)); };
  this.attr = function(name) { return __elAttr(String(this.id), name); };
}
var jsoup = {
  parse: function(html) { return new JsDocument(__jsoupParse(html)); }
};
"""
    }
}
