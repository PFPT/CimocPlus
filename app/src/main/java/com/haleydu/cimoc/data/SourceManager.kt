package com.haleydu.cimoc.data

import android.util.SparseArray
import com.haleydu.cimoc.db.SourceDao
import com.haleydu.cimoc.db.SourceRuleDao
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Parser
import com.haleydu.cimoc.script.JsMangaParser
import com.haleydu.cimoc.script.ScriptRunner
import com.haleydu.cimoc.source.Animx2
import com.haleydu.cimoc.source.BaiNian
import com.haleydu.cimoc.source.BaoZiMH
import com.haleydu.cimoc.source.BuKa
import com.haleydu.cimoc.source.CCMH
import com.haleydu.cimoc.source.CCTuku
import com.haleydu.cimoc.source.Cartoonmad
import com.haleydu.cimoc.source.ChuiXue
import com.haleydu.cimoc.source.CopyMH
import com.haleydu.cimoc.source.DM5
import com.haleydu.cimoc.source.DaShuMH
import com.haleydu.cimoc.source.Dmzj
import com.haleydu.cimoc.source.DmzjFix
import com.haleydu.cimoc.source.Dmzjv2
import com.haleydu.cimoc.source.EHentai
import com.haleydu.cimoc.source.GenericHtmlParser
import com.haleydu.cimoc.source.GuFeng
import com.haleydu.cimoc.source.HHAAZZ
import com.haleydu.cimoc.source.HaoMan8
import com.haleydu.cimoc.source.Hhxxee
import com.haleydu.cimoc.source.HotManga
import com.haleydu.cimoc.source.IKanman
import com.haleydu.cimoc.source.JMTT
import com.haleydu.cimoc.source.JiuerMH
import com.haleydu.cimoc.source.KuaiKan
import com.haleydu.cimoc.source.LaiMH
import com.haleydu.cimoc.source.Locality
import com.haleydu.cimoc.source.MH160
import com.haleydu.cimoc.source.MH50
import com.haleydu.cimoc.source.MH517
import com.haleydu.cimoc.source.MH57
import com.haleydu.cimoc.source.MHLove
import com.haleydu.cimoc.source.ManHuaDB
import com.haleydu.cimoc.source.MangaBZ
import com.haleydu.cimoc.source.MangaNel
import com.haleydu.cimoc.source.Mangakakalot
import com.haleydu.cimoc.source.Manhuatai
import com.haleydu.cimoc.source.McCmsParser
import com.haleydu.cimoc.source.MiGu
import com.haleydu.cimoc.source.Null
import com.haleydu.cimoc.source.Ohmanhua
import com.haleydu.cimoc.source.PuFei
import com.haleydu.cimoc.source.QiManWu
import com.haleydu.cimoc.source.QiMiaoMH
import com.haleydu.cimoc.source.ShouJiMH
import com.haleydu.cimoc.source.SixMH
import com.haleydu.cimoc.source.Tencent
import com.haleydu.cimoc.source.TuHao
import com.haleydu.cimoc.source.U17
import com.haleydu.cimoc.source.Vomic
import com.haleydu.cimoc.source.Webtoon
import com.haleydu.cimoc.source.WebtoonDongManManHua
import com.haleydu.cimoc.source.YKMH
import com.haleydu.cimoc.source.YYLS
import com.haleydu.cimoc.source.YYManhua
import okhttp3.Headers
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceManager @Inject constructor(
    private val mSourceDao: SourceDao,
    private val sourceRuleDao: SourceRuleDao,
    private val sourceConfigManager: SourceConfigManager,
    private val preferenceManager: PreferenceManager,
    private val httpClient: OkHttpClient,
    private val scriptRunner: ScriptRunner
) {

    private val mParserArray = SparseArray<Parser>()
    private val titleCache = HashMap<Int, String>()
    private var titlesLoaded = false

    fun list(): List<Source> = mSourceDao.list()

    fun listEnable(): List<Source> = mSourceDao.listEnable()

    fun load(type: Int): Source? = mSourceDao.load(type)

    @Synchronized
    fun insert(source: Source): Long {
        val id = mSourceDao.insert(source)
        titlesLoaded = false
        titleCache.clear()
        return id
    }

    @Synchronized
    fun update(source: Source) {
        mSourceDao.update(source)
        titlesLoaded = false
        titleCache.clear()
    }

    @Synchronized
    fun titleOf(type: Int): String {
        if (!titlesLoaded) {
            for (source in list()) {
                titleCache[source.type] = source.title
            }
            titlesLoaded = true
        }
        titleCache[type]?.let { return it }
        val title = load(type)?.title.orEmpty()
        titleCache[type] = title
        return title
    }

    @Synchronized
    fun getParser(type: Int): Parser {
        var parser = mParserArray.get(type)
        if (parser == null) {
            val rule = sourceRuleDao.load(type)
            if (type >= SourceRuleManager.TYPE_JS_START
                && rule != null && rule.scriptContent != null && rule.scriptContent.isNotEmpty()
            ) {
                var source = load(type)
                if (source == null) {
                    source = Source(null, "JS $type", type, true)
                }
                parser = JsMangaParser(source, rule, scriptRunner)
                mParserArray.put(type, parser)
                return parser
            }
            var source = load(type)
            if (source == null) {
                source = Source(null, "Source $type", type, true)
            }
            try {
                parser = when (type) {
                    IKanman.TYPE -> IKanman(source, sourceConfigManager)
                    Dmzj.TYPE -> Dmzj(source, sourceConfigManager)
                    HHAAZZ.TYPE -> HHAAZZ(source, preferenceManager)
                    CCTuku.TYPE -> CCTuku(source)
                    U17.TYPE -> U17(source)
                    DM5.TYPE -> DM5(source, sourceConfigManager)
                    Webtoon.TYPE -> Webtoon(source)
                    MH57.TYPE -> MH57(source)
                    MH50.TYPE -> MH50(source, preferenceManager, sourceConfigManager)
                    Dmzjv2.TYPE -> Dmzjv2(source)
                    Locality.TYPE -> Locality()
                    MangaNel.TYPE -> MangaNel(source)
                    PuFei.TYPE -> PuFei(source, sourceConfigManager)
                    Tencent.TYPE -> Tencent(source, sourceConfigManager)
                    BuKa.TYPE -> BuKa(source)
                    EHentai.TYPE -> EHentai(source)
                    QiManWu.TYPE -> QiManWu(source)
                    Hhxxee.TYPE -> Hhxxee(source)
                    Cartoonmad.TYPE -> Cartoonmad(source)
                    Animx2.TYPE -> Animx2(source)
                    MH517.TYPE -> MH517(source)
                    MiGu.TYPE -> MiGu(source)
                    BaiNian.TYPE -> BaiNian(source)
                    ChuiXue.TYPE -> ChuiXue(source)
                    TuHao.TYPE -> TuHao(source)
                    SixMH.TYPE -> SixMH(source)
                    ManHuaDB.TYPE -> ManHuaDB(source)
                    Manhuatai.TYPE -> Manhuatai(source, httpClient, sourceConfigManager)
                    GuFeng.TYPE -> GuFeng(source, sourceConfigManager)
                    CCMH.TYPE -> CCMH(source)
                    MHLove.TYPE -> MHLove(source)
                    YYLS.TYPE -> YYLS(source)
                    JMTT.TYPE -> JMTT(source)
                    Mangakakalot.TYPE -> Mangakakalot(source)
                    Ohmanhua.TYPE -> Ohmanhua(source, sourceConfigManager)
                    CopyMH.TYPE -> CopyMH(source, sourceConfigManager, httpClient)
                    HotManga.TYPE -> HotManga(source, httpClient, sourceConfigManager)
                    MangaBZ.TYPE -> MangaBZ(source)
                    WebtoonDongManManHua.TYPE -> WebtoonDongManManHua(source, httpClient)
                    MH160.TYPE -> MH160(source, sourceConfigManager)
                    QiMiaoMH.TYPE -> QiMiaoMH(source, httpClient)
                    YKMH.TYPE -> YKMH(source, sourceConfigManager)
                    DmzjFix.TYPE -> DmzjFix(source, sourceConfigManager)
                    BaoZiMH.TYPE -> BaoZiMH(source, sourceConfigManager.requireConfig(BaoZiMH.TYPE), sourceConfigManager)
                    HaoMan8.TYPE -> HaoMan8(source, sourceConfigManager.requireConfig(HaoMan8.TYPE))
                    McCmsParser.TYPE_DUMANWUORG,
                    McCmsParser.TYPE_TTKANMANHUA,
                    McCmsParser.TYPE_HAODUOMAN,
                    McCmsParser.TYPE_MANHUAYU,
                    McCmsParser.TYPE_YOUMANHUAWU -> McCmsParser(source, sourceConfigManager.requireConfig(type))
                    YYManhua.TYPE -> YYManhua(source, sourceConfigManager.requireConfig(type))
                    LaiMH.TYPE -> LaiMH(source, sourceConfigManager.requireConfig(type))
                    ShouJiMH.TYPE -> ShouJiMH(source, sourceConfigManager.requireConfig(type))
                    Vomic.TYPE -> Vomic(source, sourceConfigManager.requireConfig(type))
                    DaShuMH.TYPE -> DaShuMH(source, sourceConfigManager.requireConfig(type))
                    KuaiKan.TYPE -> KuaiKan(source, sourceConfigManager)
                    JiuerMH.TYPE -> JiuerMH(source, sourceConfigManager.requireConfig(type))
                    else -> GenericHtmlParser(source, sourceConfigManager.requireConfig(type))
                }
            } catch (_: Exception) {
                parser = try {
                    GenericHtmlParser(source, sourceConfigManager.requireConfig(type))
                } catch (_: Exception) {
                    Null()
                }
            }
            if (parser == null) {
                parser = try {
                    GenericHtmlParser(source, sourceConfigManager.requireConfig(type))
                } catch (_: Exception) {
                    Null()
                }
            }
            mParserArray.put(type, parser)
        }
        return parser
    }

    @Synchronized
    fun clearParserCache() {
        mParserArray.clear()
        titlesLoaded = false
        titleCache.clear()
    }

    inner class TitleGetter {
        fun getTitle(type: Int): String {
            return try {
                val title = getParser(type).getTitle()
                title ?: ""
            } catch (_: Exception) {
                ""
            }
        }
    }

    inner class HeaderGetter {
        fun getHeader(type: Int): Headers? {
            return try {
                getParser(type).getHeader()
            } catch (_: Exception) {
                Headers.headersOf()
            }
        }
    }
}
