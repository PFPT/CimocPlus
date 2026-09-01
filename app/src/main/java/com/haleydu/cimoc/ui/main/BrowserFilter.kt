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

@AndroidEntryPoint
class BrowserFilter : AppCompatActivity() {

    @Inject
    lateinit var sourceManager: SourceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openReaderByIntent(intent)
        finish()
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

    private fun openReader(uri: Uri) {
        try {
            for (type in registeredTypes()) {
                val parser = sourceManager.getParser(type)
                if (parser.isHere(uri)) {
                    val comicId = parser.getComicId(uri) ?: continue
                    openDetailActivity(type, comicId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openReaderByIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_VIEW == action) {
            val uri = intent.data
            if (uri != null) {
                openReader(uri)
            } else {
                Toast.makeText(this, "url不合法", Toast.LENGTH_SHORT).show()
            }
        }
        if (Intent.ACTION_SEND == action && "text/plain" == type) {
            try {
                openReader(
                    Uri.parse(
                        intent.getStringExtra(Intent.EXTRA_TEXT)!!
                            .replace("https://m.ykmh.commanhua", "https://m.ykmh.com/manhua")
                    )
                )
            } catch (_: Exception) {
                Toast.makeText(this, "url不合法", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
