package com.haleydu.cimoc.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.haleydu.cimoc.app.CimocComposeActivity
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.source.Animx2
import com.haleydu.cimoc.source.BaiNian
import com.haleydu.cimoc.source.BuKa
import com.haleydu.cimoc.source.Cartoonmad
import com.haleydu.cimoc.source.ChuiXue
import com.haleydu.cimoc.source.DM5
import com.haleydu.cimoc.source.Dmzjv2
import com.haleydu.cimoc.source.Hhxxee
import com.haleydu.cimoc.source.HotManga
import com.haleydu.cimoc.source.IKanman
import com.haleydu.cimoc.source.MH50
import com.haleydu.cimoc.source.MH517
import com.haleydu.cimoc.source.MH57
import com.haleydu.cimoc.source.ManHuaDB
import com.haleydu.cimoc.source.MiGu
import com.haleydu.cimoc.source.PuFei
import com.haleydu.cimoc.source.SixMH
import com.haleydu.cimoc.source.Tencent
import com.haleydu.cimoc.source.TuHao
import com.haleydu.cimoc.source.U17
import com.haleydu.cimoc.source.YKMH
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.concurrent.thread

@AndroidEntryPoint
class BrowserFilter : AppCompatActivity() {

    @Inject
    lateinit var sourceManager: SourceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incoming = intent
        thread(name = "browser-filter", isDaemon = true) {
            val resolved = resolve(incoming)
            runOnUiThread {
                if (resolved.invalid) {
                    Toast.makeText(this, "url不合法", Toast.LENGTH_SHORT).show()
                }
                for (match in resolved.matches) {
                    openDetailActivity(match.first, match.second)
                }
                finish()
            }
        }
    }

    private fun openDetailActivity(source: Int, comicId: String) {
        startActivity(CimocComposeActivity.detailIntent(this, source, comicId))
    }

    private fun registeredTypes(): List<Int> = listOf(
        Dmzjv2.TYPE,
        BuKa.TYPE,
        PuFei.TYPE,
        Cartoonmad.TYPE,
        Animx2.TYPE,
        MH517.TYPE,
        BaiNian.TYPE,
        MiGu.TYPE,
        Tencent.TYPE,
        U17.TYPE,
        MH57.TYPE,
        MH50.TYPE,
        DM5.TYPE,
        IKanman.TYPE,
        Hhxxee.TYPE,
        ChuiXue.TYPE,
        ManHuaDB.TYPE,
        TuHao.TYPE,
        YKMH.TYPE,
        SixMH.TYPE,
        HotManga.TYPE
    )

    private fun resolve(intent: Intent): Resolved {
        val matches = ArrayList<Pair<Int, String>>()
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_VIEW == action) {
            val uri = intent.data ?: return Resolved(matches, invalid = true)
            collect(uri, matches)
            return Resolved(matches, invalid = false)
        }
        if (Intent.ACTION_SEND == action && "text/plain" == type) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return Resolved(matches, invalid = true)
            val uri = runCatching {
                Uri.parse(text.replace("https://m.ykmh.commanhua", "https://m.ykmh.com/manhua"))
            }.getOrNull() ?: return Resolved(matches, invalid = true)
            collect(uri, matches)
        }
        return Resolved(matches, invalid = false)
    }

    private fun collect(uri: Uri, matches: MutableList<Pair<Int, String>>) {
        for (type in registeredTypes()) {
            val parser = runCatching { sourceManager.getParser(type) }.getOrNull() ?: continue
            val here = runCatching { parser.isHere(uri) }.getOrDefault(false)
            if (!here) continue
            val comicId = runCatching { parser.getComicId(uri) }.getOrNull() ?: continue
            matches.add(type to comicId)
        }
    }

    private data class Resolved(val matches: List<Pair<Int, String>>, val invalid: Boolean)
}
