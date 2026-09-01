package com.haleydu.cimoc.helper

import com.haleydu.cimoc.BuildConfig
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.db.SourceDao
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.source.Animx2
import com.haleydu.cimoc.source.BaiNian
import com.haleydu.cimoc.source.BuKa
import com.haleydu.cimoc.source.CCMH
import com.haleydu.cimoc.source.CCTuku
import com.haleydu.cimoc.source.Cartoonmad
import com.haleydu.cimoc.source.ChuiXue
import com.haleydu.cimoc.source.CopyMH
import com.haleydu.cimoc.source.DM5
import com.haleydu.cimoc.source.Dmzj
import com.haleydu.cimoc.source.DmzjFix
import com.haleydu.cimoc.source.Dmzjv2
import com.haleydu.cimoc.source.EHentai
import com.haleydu.cimoc.source.GuFeng
import com.haleydu.cimoc.source.HHAAZZ
import com.haleydu.cimoc.source.Hhxxee
import com.haleydu.cimoc.source.HotManga
import com.haleydu.cimoc.source.IKanman
import com.haleydu.cimoc.source.JMTT
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
import com.haleydu.cimoc.source.MiGu
import com.haleydu.cimoc.source.Ohmanhua
import com.haleydu.cimoc.source.PuFei
import com.haleydu.cimoc.source.QiManWu
import com.haleydu.cimoc.source.QiMiaoMH
import com.haleydu.cimoc.source.SixMH
import com.haleydu.cimoc.source.Tencent
import com.haleydu.cimoc.source.TuHao
import com.haleydu.cimoc.source.U17
import com.haleydu.cimoc.source.Webtoon
import com.haleydu.cimoc.source.WebtoonDongManManHua
import com.haleydu.cimoc.source.YKMH
import com.haleydu.cimoc.source.YYLS

object UpdateHelper {

    private val VERSION = BuildConfig.VERSION_CODE

    private val RESTORE_TYPES = intArrayOf(
        CopyMH.TYPE,
        Ohmanhua.TYPE,
        Tencent.TYPE,
        IKanman.TYPE,
        DM5.TYPE,
        MH160.TYPE,
        YKMH.TYPE,
        HotManga.TYPE,
        ManHuaDB.TYPE,
        GuFeng.TYPE,
        Manhuatai.TYPE,
        DmzjFix.TYPE,
        JMTT.TYPE,
        MangaBZ.TYPE,
        PuFei.TYPE,
        Cartoonmad.TYPE,
        Webtoon.TYPE,
        WebtoonDongManManHua.TYPE,
        U17.TYPE
    )

    @JvmStatic
    fun update(manager: PreferenceManager, sourceConfigManager: SourceConfigManager, sourceDao: SourceDao) {
        val version = manager.getInt(PreferenceManager.PREF_APP_VERSION, 0)
        if (version != VERSION) {
            initSource(sourceDao)
            manager.putInt(PreferenceManager.PREF_APP_VERSION, VERSION)
        }
        sourceConfigManager.applyToDatabase()
        restoreEnabledSources(manager, sourceConfigManager, sourceDao)
    }

    private fun restoreEnabledSources(
        manager: PreferenceManager,
        sourceConfigManager: SourceConfigManager,
        sourceDao: SourceDao
    ) {
        if (manager.getBoolean(PreferenceManager.PREF_SOURCE_RESTORE_ENABLE, false)) {
            return
        }
        for (type in RESTORE_TYPES) {
            val source = sourceDao.load(type)
            if (source != null && !source.enable) {
                source.enable = true
                sourceDao.update(source)
            }
        }
        for (generic in sourceConfigManager.listGenericSources()) {
            val source = sourceDao.load(generic.type)
            if (source != null && !source.enable) {
                source.enable = true
                sourceDao.update(source)
            }
        }
        manager.putBoolean(PreferenceManager.PREF_SOURCE_RESTORE_ENABLE, true)
    }

    private fun initSource(dao: SourceDao) {
        val list = ArrayList<Source>()
        list.add(IKanman.getDefaultSource())
        list.add(Dmzj.getDefaultSource())
        list.add(HHAAZZ.getDefaultSource())
        list.add(CCTuku.getDefaultSource())
        list.add(U17.getDefaultSource())
        list.add(DM5.getDefaultSource())
        list.add(Webtoon.getDefaultSource())
        list.add(MH57.getDefaultSource())
        list.add(MH50.getDefaultSource())
        list.add(Dmzjv2.getDefaultSource())
        list.add(MangaNel.getDefaultSource())
        list.add(Mangakakalot.getDefaultSource())
        list.add(PuFei.getDefaultSource())
        list.add(Cartoonmad.getDefaultSource())
        list.add(Animx2.getDefaultSource())
        list.add(MH517.getDefaultSource())
        list.add(BaiNian.getDefaultSource())
        list.add(MiGu.getDefaultSource())
        list.add(Tencent.getDefaultSource())
        list.add(BuKa.getDefaultSource())
        list.add(EHentai.getDefaultSource())
        list.add(QiManWu.getDefaultSource())
        list.add(Hhxxee.getDefaultSource())
        list.add(ChuiXue.getDefaultSource())
        list.add(BaiNian.getDefaultSource())
        list.add(TuHao.getDefaultSource())
        list.add(SixMH.getDefaultSource())
        list.add(MangaBZ.getDefaultSource())
        list.add(ManHuaDB.getDefaultSource())
        list.add(Manhuatai.getDefaultSource())
        list.add(GuFeng.getDefaultSource())
        list.add(CCMH.getDefaultSource())
        list.add(Manhuatai.getDefaultSource())
        list.add(MHLove.getDefaultSource())
        list.add(GuFeng.getDefaultSource())
        list.add(YYLS.getDefaultSource())
        list.add(JMTT.getDefaultSource())
        list.add(Ohmanhua.getDefaultSource())
        list.add(CopyMH.getDefaultSource())
        list.add(HotManga.getDefaultSource())
        list.add(WebtoonDongManManHua.getDefaultSource())
        list.add(MH160.getDefaultSource())
        list.add(QiMiaoMH.getDefaultSource())
        list.add(YKMH.getDefaultSource())
        list.add(DmzjFix.getDefaultSource())
        for (source in list) {
            val existing = dao.load(source.type)
            if (existing == null) {
                dao.insert(source)
            } else if (source.title != existing.title) {
                existing.title = source.title
                dao.update(existing)
            }
        }
    }
}
