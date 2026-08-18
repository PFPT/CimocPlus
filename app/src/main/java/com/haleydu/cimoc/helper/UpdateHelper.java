package com.haleydu.cimoc.helper;

import com.haleydu.cimoc.BuildConfig;
import com.haleydu.cimoc.db.SourceDao;
import com.haleydu.cimoc.manager.PreferenceManager;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.source.*;
import com.haleydu.cimoc.source.WebtoonDongManManHua;
import com.haleydu.cimoc.manager.SourceConfigManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hiroshi on 2017/1/18.
 */

public class UpdateHelper {

    // 1.04.08.008
    private static final int VERSION = BuildConfig.VERSION_CODE;

    public static void update(PreferenceManager manager, SourceConfigManager sourceConfigManager, SourceDao sourceDao) {
        int version = manager.getInt(PreferenceManager.PREF_APP_VERSION, 0);
        if (version != VERSION) {
            initSource(sourceDao);
            manager.putInt(PreferenceManager.PREF_APP_VERSION, VERSION);
        }
        sourceConfigManager.applyToDatabase();
        restoreEnabledSources(manager, sourceConfigManager, sourceDao);
    }

    private static final int[] RESTORE_TYPES = {
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
    };

    private static void restoreEnabledSources(PreferenceManager manager, SourceConfigManager sourceConfigManager,
                                              SourceDao sourceDao) {
        if (manager.getBoolean(PreferenceManager.PREF_SOURCE_RESTORE_ENABLE, false)) {
            return;
        }
        for (int type : RESTORE_TYPES) {
            Source source = sourceDao.load(type);
            if (source != null && !source.getEnable()) {
                source.setEnable(true);
                sourceDao.update(source);
            }
        }
        for (Source generic : sourceConfigManager.listGenericSources()) {
            Source source = sourceDao.load(generic.getType());
            if (source != null && !source.getEnable()) {
                source.setEnable(true);
                sourceDao.update(source);
            }
        }
        manager.putBoolean(PreferenceManager.PREF_SOURCE_RESTORE_ENABLE, true);
    }

    private static void initSource(SourceDao dao) {
        List<Source> list = new ArrayList<>();
        list.add(IKanman.getDefaultSource());
        list.add(Dmzj.getDefaultSource());
        list.add(HHAAZZ.getDefaultSource());
        list.add(CCTuku.getDefaultSource());
        list.add(U17.getDefaultSource());
        list.add(DM5.getDefaultSource());
        list.add(Webtoon.getDefaultSource());
        //list.add(HHSSEE.getDefaultSource());
        list.add(MH57.getDefaultSource());
        list.add(MH50.getDefaultSource());
        list.add(Dmzjv2.getDefaultSource());
        list.add(MangaNel.getDefaultSource());
        list.add(Mangakakalot.getDefaultSource());
        list.add(PuFei.getDefaultSource());
        list.add(Cartoonmad.getDefaultSource());
        list.add(Animx2.getDefaultSource());
        list.add(MH517.getDefaultSource());
        list.add(BaiNian.getDefaultSource());
        list.add(MiGu.getDefaultSource());
        list.add(Tencent.getDefaultSource());
        list.add(BuKa.getDefaultSource());
        list.add(EHentai.getDefaultSource());
        list.add(QiManWu.getDefaultSource());
        list.add(Hhxxee.getDefaultSource());
        list.add(ChuiXue.getDefaultSource());
        list.add(BaiNian.getDefaultSource());
        list.add(TuHao.getDefaultSource());
        list.add(SixMH.getDefaultSource());
        list.add(MangaBZ.getDefaultSource());
        list.add(ManHuaDB.getDefaultSource());
        list.add(Manhuatai.getDefaultSource());
        list.add(GuFeng.getDefaultSource());
        list.add(CCMH.getDefaultSource());
        list.add(Manhuatai.getDefaultSource());
        list.add(MHLove.getDefaultSource());
        list.add(GuFeng.getDefaultSource());
        list.add(YYLS.getDefaultSource());
        list.add(JMTT.getDefaultSource());
        list.add(Ohmanhua.getDefaultSource());
        list.add(CopyMH.getDefaultSource());
        list.add(HotManga.getDefaultSource());
        list.add(WebtoonDongManManHua.getDefaultSource());
        list.add(MH160.getDefaultSource());
        list.add(QiMiaoMH.getDefaultSource());
        list.add(YKMH.getDefaultSource());
        list.add(DmzjFix.getDefaultSource());
        for (Source source : list) {
            Source existing = dao.load(source.getType());
            if (existing == null) {
                dao.insert(source);
            } else if (!source.getTitle().equals(existing.getTitle())) {
                existing.setTitle(source.getTitle());
                dao.update(existing);
            }
        }
    }
}
