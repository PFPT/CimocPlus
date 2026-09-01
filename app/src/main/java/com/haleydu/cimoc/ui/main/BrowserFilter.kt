package com.haleydu.cimoc.ui.main
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.haleydu.cimoc.R
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
import com.haleydu.cimoc.ui.common.BaseActivity
import com.haleydu.cimoc.ui.detail.DetailActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BrowserFilter : BaseActivity() {

    @Inject
    lateinit var sourceManager: SourceManager

    override fun getLayoutRes(): Int {
        return R.layout.activity_browser_filter
    }

    override fun getDefaultTitle(): String {
        return "jumping..."
    }

    fun openDetailActivity(source: Int, comicId: String) {
        val intent = DetailActivity.createIntent(this, null, source, comicId)
        startActivity(intent)
    }

    private fun registUrlListener(): List<Int> {
        val list = ArrayList<Int>()
        list.add(Dmzjv2.TYPE)
        list.add(BuKa.TYPE)
        list.add(PuFei.TYPE)
        list.add(Cartoonmad.TYPE)
        list.add(Animx2.TYPE)
        list.add(MH517.TYPE)
        list.add(BaiNian.TYPE)
        list.add(MiGu.TYPE)
        list.add(Tencent.TYPE)
        list.add(U17.TYPE)
        list.add(MH57.TYPE)
        list.add(MH50.TYPE)
        list.add(DM5.TYPE)
        list.add(IKanman.TYPE)
        list.add(Hhxxee.TYPE)
        list.add(BaiNian.TYPE)
        list.add(ChuiXue.TYPE)
        list.add(ManHuaDB.TYPE)
        list.add(TuHao.TYPE)
        list.add(YKMH.TYPE)
        list.add(SixMH.TYPE)
        list.add(HotManga.TYPE)
        return list
    }

    private fun openReader(uri: Uri) {
        try {
            for (i in registUrlListener()) {
                val comicId = if (sourceManager.getParser(i).isHere(uri)) {
                    sourceManager.getParser(i).getComicId(uri)
                } else {
                    null
                }
                if (comicId != null) {
                    openDetailActivity(i, comicId)
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
                Toast.makeText(this, "url不合法", Toast.LENGTH_SHORT)
            }
        }

        if (Intent.ACTION_SEND == action && "text/plain" == type) {
            try {
                openReader(Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT)!!.replace("https://m.ykmh.commanhua", "https://m.ykmh.com/manhua")))
            } catch (ex: Exception) {
                Toast.makeText(this, "url不合法", Toast.LENGTH_SHORT)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser_filter)
        openReaderByIntent(intent)
        finish()
    }
}
