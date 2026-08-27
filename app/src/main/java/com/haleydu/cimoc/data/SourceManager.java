package com.haleydu.cimoc.data;
import android.util.SparseArray;

import com.haleydu.cimoc.db.SourceDao;
import com.haleydu.cimoc.db.SourceRuleDao;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.model.SourceRule;
import com.haleydu.cimoc.parser.Parser;
import com.haleydu.cimoc.script.JsMangaParser;
import com.haleydu.cimoc.script.ScriptRunner;
import com.haleydu.cimoc.source.*;
import com.haleydu.cimoc.source.WebtoonDongManManHua;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Headers;
import okhttp3.OkHttpClient;

@Singleton
public class SourceManager {

    private SourceDao mSourceDao;
    private SourceRuleDao sourceRuleDao;
    private SourceConfigManager sourceConfigManager;
    private PreferenceManager preferenceManager;
    private OkHttpClient httpClient;
    private ScriptRunner scriptRunner;
    private SparseArray<Parser> mParserArray = new SparseArray<>();

    @Inject
    public SourceManager(SourceDao sourceDao, SourceRuleDao sourceRuleDao, SourceConfigManager sourceConfigManager,
                         PreferenceManager preferenceManager, OkHttpClient httpClient, ScriptRunner scriptRunner) {
        mSourceDao = sourceDao;
        this.sourceRuleDao = sourceRuleDao;
        this.sourceConfigManager = sourceConfigManager;
        this.preferenceManager = preferenceManager;
        this.httpClient = httpClient;
        this.scriptRunner = scriptRunner;
    }

    public List<Source> list() {
        return mSourceDao.list();
    }

    public List<Source> listEnable() {
        return mSourceDao.listEnable();
    }

    public Source load(int type) {
        return mSourceDao.load(type);
    }

    public long insert(Source source) {
        return mSourceDao.insert(source);
    }

    public void update(Source source) {
        mSourceDao.update(source);
    }

    public Parser getParser(int type) {
        Parser parser = mParserArray.get(type);
        if (parser == null) {
            SourceRule rule = sourceRuleDao.load(type);
            if (type >= SourceRuleManager.TYPE_JS_START
                    && rule != null && rule.getScriptContent() != null && !rule.getScriptContent().isEmpty()) {
                Source source = load(type);
                if (source == null) {
                    source = new Source(null, "JS " + type, type, true);
                }
                parser = new JsMangaParser(source, rule, scriptRunner);
                mParserArray.put(type, parser);
                return parser;
            }
            Source source = load(type);
            switch (type) {
                case IKanman.TYPE:
                    parser = new IKanman(source);
                    break;
                case Dmzj.TYPE:
                    parser = new Dmzj(source);
                    break;
                case HHAAZZ.TYPE:
                    parser = new HHAAZZ(source, preferenceManager);
                    break;
                case CCTuku.TYPE:
                    parser = new CCTuku(source);
                    break;
                case U17.TYPE:
                    parser = new U17(source);
                    break;
                case DM5.TYPE:
                    parser = new DM5(source);
                    break;
                case Webtoon.TYPE:
                    parser = new Webtoon(source);
                    break;
                case MH57.TYPE:
                    parser = new MH57(source);
                    break;
                case MH50.TYPE:
                    parser = new MH50(source, preferenceManager, sourceConfigManager);
                    break;
                case Dmzjv2.TYPE:
                    parser = new Dmzjv2(source);
                    break;
                case Locality.TYPE:
                    parser = new Locality();
                    break;
                case MangaNel.TYPE:
                    parser = new MangaNel(source);
                    break;

                //feilong
                case PuFei.TYPE:
                    parser = new PuFei(source, sourceConfigManager);
                    break;
                case Tencent.TYPE:
                    parser = new Tencent(source);
                    break;
                case BuKa.TYPE:
                    parser = new BuKa(source);
                    break;
                case EHentai.TYPE:
                    parser = new EHentai(source);
                    break;
                case QiManWu.TYPE:
                    parser = new QiManWu(source);
                    break;
                case Hhxxee.TYPE:
                    parser = new Hhxxee(source);
                    break;
                case Cartoonmad.TYPE:
                    parser = new Cartoonmad(source);
                    break;
                case Animx2.TYPE:
                    parser = new Animx2(source);
                    break;
                case MH517.TYPE:
                    parser = new MH517(source);
                    break;
                case MiGu.TYPE:
                    parser = new MiGu(source);
                    break;
                case BaiNian.TYPE:
                    parser = new BaiNian(source);
                    break;
                case ChuiXue.TYPE:
                    parser = new ChuiXue(source);
                    break;
                case TuHao.TYPE:
                    parser = new TuHao(source);
                    break;
                case SixMH.TYPE:
                    parser = new SixMH(source);
                    break;
                case ManHuaDB.TYPE:
                    parser = new ManHuaDB(source);
                    break;
                case Manhuatai.TYPE:
                    parser = new Manhuatai(source, httpClient, sourceConfigManager);
                    break;
                case GuFeng.TYPE:
                    parser = new GuFeng(source, sourceConfigManager);
                    break;
                case CCMH.TYPE:
                    parser = new CCMH(source);
                    break;
                case MHLove.TYPE:
                    parser = new MHLove(source);
                    break;
                case YYLS.TYPE:
                    parser = new YYLS(source);
                    break;
                case JMTT.TYPE:
                    parser = new JMTT(source);
                    break;

                //haleydu
                case Mangakakalot.TYPE:
                    parser = new Mangakakalot(source);
                    break;
                case Ohmanhua.TYPE:
                    parser = new Ohmanhua(source, sourceConfigManager);
                    break;
                case CopyMH.TYPE:
                    parser = new CopyMH(source, sourceConfigManager, httpClient);
                    break;
                case HotManga.TYPE:
                    parser = new HotManga(source, httpClient, sourceConfigManager);
                    break;
                case MangaBZ.TYPE:
                    parser = new MangaBZ(source);
                    break;
                case WebtoonDongManManHua.TYPE:
                    parser = new WebtoonDongManManHua(source, httpClient);
                    break;
                case MH160.TYPE:
                    parser = new MH160(source, sourceConfigManager);
                    break;
                case QiMiaoMH.TYPE:
                    parser = new QiMiaoMH(source, httpClient);
                    break;
                case YKMH.TYPE:
                    parser = new YKMH(source, sourceConfigManager);
                    break;
                case DmzjFix.TYPE:
                    parser = new DmzjFix(source);
                    break;
                case BaoZiMH.TYPE:
                    SourceConfig baoziConfig = sourceConfigManager.getConfig(BaoZiMH.TYPE);
                    parser = baoziConfig != null
                            ? new BaoZiMH(source, baoziConfig, sourceConfigManager)
                            : new Null();
                    break;
                default:
                    SourceConfig config = sourceConfigManager.getConfig(type);
                    if (config != null) {
                        parser = new GenericHtmlParser(source, config);
                    } else {
                        parser = new Null();
                    }
                    break;
            }
            mParserArray.put(type, parser);
        }
        return parser;
    }

    public void clearParserCache() {
        mParserArray.clear();
    }

    public class TitleGetter {

        public String getTitle(int type) {
            return getParser(type).getTitle();
        }

    }

    public class HeaderGetter {

        public Headers getHeader(int type) {
            return getParser(type).getHeader();
        }

    }
}
