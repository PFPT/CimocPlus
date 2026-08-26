package com.haleydu.cimoc.data;
import android.content.Context;
import android.util.SparseArray;

import com.haleydu.cimoc.db.SourceDao;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.source.CopyMH;
import com.haleydu.cimoc.source.Ohmanhua;
import com.haleydu.cimoc.source.Tencent;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Singleton
public class SourceConfigManager {

    public static final String PREF_SOURCE_BASE_URL_JSON = "pref_source_base_url_json";

    private static final String[] REMOTE_URLS = {
            "https://raw.githubusercontent.com/haleydu-test/cimocUpdate/main/sourceBaseUrl.json",
            "https://miuscapp.com/cimoc/sourceBaseUrl.json",
            "http://www.cimoc.top/cimoc/sourc/master/raw/sourceBaseUrl.json",
            "https://gitcode.net/Haleydutest/cupdate/-/raw/master/sourceBaseUrl.json"
    };

    private static final String[] COPY_API_PREFERRED = {
            "https://mapi.copy2000.site",
            "https://api.copy2000.online",
            "https://www.copy3000.com"
    };

    private static final String[] COPY_WEB_FALLBACKS = {
            "https://www.copy3000.com",
            "https://www.2026copy.com",
            "https://www.mangacopy.com",
            "https://www.copy20.com",
            "https://www.2025copy.com"
    };

    private static final String[] HOTMANGA_FALLBACKS = {
            "https://www.relamanhua.com",
            "https://www.manga2020.com",
            "https://mapi.hotmangasg.com:12001"
    };

    private static final Map<String, Item> GENERIC_ITEMS = new LinkedHashMap<>();

    static {
        GENERIC_ITEMS.put("BAOZIMH", new Item(200, "包子漫画"));
        GENERIC_ITEMS.put("YIMMH", new Item(201, "忆漫画"));
        GENERIC_ITEMS.put("DUMH", new Item(202, "独漫画"));
        GENERIC_ITEMS.put("FEIXUEMH", new Item(203, "飞雪漫画"));
        GENERIC_ITEMS.put("MANHUAWU", new Item(204, "漫画屋"));
        GENERIC_ITEMS.put("YEMANMH", new Item(205, "野蛮漫画"));
        GENERIC_ITEMS.put("HAOMAN8", new Item(206, "好漫8"));
        GENERIC_ITEMS.put("HAOMAN6", new Item(207, "好漫6"));
        GENERIC_ITEMS.put("MITUI", new Item(208, "米推漫画"));
        GENERIC_ITEMS.put("MH360", new Item(209, "360漫画"));
        GENERIC_ITEMS.put("BMG", new Item(210, "百漫谷"));
        GENERIC_ITEMS.put("BAINIANMH", new Item(211, "百年漫画"));
        GENERIC_ITEMS.put("QIMIAO", new Item(212, "奇妙漫画"));
        GENERIC_ITEMS.put("PUFEIMH", new Item(213, "扑飞漫画"));
        GENERIC_ITEMS.put("XINXINMH", new Item(214, "新新漫画"));
        GENERIC_ITEMS.put("BODONGMH", new Item(215, "波动漫画"));
        GENERIC_ITEMS.put("IQIYI", new Item(216, "爱奇艺漫画"));
        GENERIC_ITEMS.put("BOLEFU", new Item(217, "博乐福"));
        GENERIC_ITEMS.put("QIXIMH", new Item(218, "七夕漫画"));
        GENERIC_ITEMS.put("ZERO", new Item(219, "Zero"));
    }

    private final PreferenceManager preferenceManager;
    private final SourceDao sourceDao;
    private final OkHttpClient httpClient;
    private Context mContext;
    private final SparseArray<SourceConfig> mConfigByType = new SparseArray<>();
    private final List<String> mCopyApiHosts = new ArrayList<>();
    private final List<String> mCopyWebHosts = new ArrayList<>();
    private JSONObject mRootJson;
    private String mCopyVersion = "2025.11.21";

    @Inject
    public SourceConfigManager(@ApplicationContext Context context, PreferenceManager preferenceManager,
                               SourceDao sourceDao, OkHttpClient httpClient) {
        this.preferenceManager = preferenceManager;
        this.sourceDao = sourceDao;
        this.httpClient = httpClient;
        init(context);
    }

    public void init(Context context) {
        mContext = context.getApplicationContext();
        String json = preferenceManager.getString(PREF_SOURCE_BASE_URL_JSON, "");
        if (!parse(json)) {
            parse(readAssetJson());
        }
    }

    public boolean fetchRemote() {
        Set<String> urls = new LinkedHashSet<>();
        for (String url : REMOTE_URLS) {
            urls.add(url);
        }
        JSONObject local = parseObject(preferenceManager.getString(PREF_SOURCE_BASE_URL_JSON, readAssetJson()));
        if (local != null) {
            addUrl(urls, local.optString("backup_update"));
            addUrl(urls, local.optString("update_backup_my"));
        }
        for (String url : urls) {
            String json = download(httpClient, url);
            if (parse(json)) {
                preferenceManager.putString(PREF_SOURCE_BASE_URL_JSON, json);
                return true;
            }
        }
        return false;
    }

    public synchronized void applyToDatabase() {
        SourceDao dao = sourceDao;
        for (int i = 0; i < mConfigByType.size(); i++) {
            SourceConfig config = mConfigByType.valueAt(i);
            upsert(dao, config.toSource(true));
        }
        upsert(dao, CopyMH.getDefaultSource());
        upsert(dao, Ohmanhua.getDefaultSource());
        upsert(dao, Tencent.getDefaultSource());
    }

    public synchronized List<Source> listGenericSources() {
        List<Source> list = new ArrayList<>();
        for (int i = 0; i < mConfigByType.size(); i++) {
            list.add(mConfigByType.valueAt(i).toSource(true));
        }
        return list;
    }

    public synchronized SourceConfig getConfig(int type) {
        return mConfigByType.get(type);
    }

    public synchronized List<String> getCopyApiHosts() {
        List<String> hosts = new ArrayList<>();
        for (String host : COPY_API_PREFERRED) {
            addUnique(hosts, host);
        }
        for (String host : mCopyApiHosts) {
            addUnique(hosts, host);
        }
        for (String host : COPY_WEB_FALLBACKS) {
            addUnique(hosts, host);
        }
        return hosts;
    }

    public synchronized List<String> getCopyWebHosts() {
        List<String> hosts = new ArrayList<>();
        for (String host : COPY_API_PREFERRED) {
            addUnique(hosts, host);
        }
        for (String host : mCopyWebHosts) {
            addUnique(hosts, host);
        }
        for (String host : COPY_WEB_FALLBACKS) {
            addUnique(hosts, host);
        }
        return hosts;
    }

    public synchronized List<String> getHotMangaHosts() {
        List<String> hosts = new ArrayList<>();
        addUnique(hosts, getUrl("HOTMANGA", ""));
        addUnique(hosts, getUrl("HOTMANGASERVER", ""));
        for (String host : HOTMANGA_FALLBACKS) {
            addUnique(hosts, host);
        }
        return hosts;
    }

    public String getCopyApiBase() {
        List<String> hosts = getCopyApiHosts();
        return hosts.isEmpty() ? "https://mapi.copy2000.site" : hosts.get(0);
    }

    public String getCopyWebBase() {
        List<String> hosts = getCopyWebHosts();
        return hosts.isEmpty() ? "https://www.copy3000.com" : hosts.get(0);
    }

    public synchronized String getCopyVersion() {
        return mCopyVersion == null || mCopyVersion.isEmpty() ? "2025.11.21" : mCopyVersion;
    }

    public synchronized String getUrl(String key, String fallback) {
        return firstHttp(rawValue(key, "baseUrl"), fallback);
    }

    public synchronized String getField(String key, String field, String fallback) {
        return firstHttp(rawValue(key, field), fallback);
    }

    public boolean isSourceBaseUrlJson(String json) {
        JSONObject object = parseObject(json);
        if (object == null) {
            return false;
        }
        if (object.has("CopyManHua") || object.has("BAOZIMH") || object.has("BaoziManHua")
                || object.has("COPYMH") || object.has("COPYMHSERVER")) {
            return true;
        }
        for (String key : GENERIC_ITEMS.keySet()) {
            if (object.has(key)) {
                return true;
            }
        }
        if (object.has("YiManHua") || object.has("BaiManGu") || object.has("PuFeiManHua")
                || object.has("BoLeFu") || object.has("DuManHua")) {
            return true;
        }
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            JSONObject item = object.optJSONObject(keys.next());
            if (item != null && item.has("baseUrl")) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean importJson(String json) {
        if (!isSourceBaseUrlJson(json)) {
            return false;
        }
        if (!parse(json)) {
            String previous = preferenceManager.getString(PREF_SOURCE_BASE_URL_JSON, "");
            if (!parse(previous)) {
                parse(readAssetJson());
            }
            return false;
        }
        preferenceManager.putString(PREF_SOURCE_BASE_URL_JSON, json);
        applyToDatabase();
        return true;
    }

    private synchronized boolean parse(String json) {
        JSONObject object = parseObject(json);
        if (object == null) {
            return false;
        }
        mRootJson = object;
        mConfigByType.clear();
        mCopyApiHosts.clear();
        mCopyWebHosts.clear();
        mCopyVersion = "2025.11.21";
        for (Map.Entry<String, Item> entry : GENERIC_ITEMS.entrySet()) {
            JSONObject item = object.optJSONObject(entry.getKey());
            Item meta = entry.getValue();
            SourceConfig config = item == null ? null : new SourceConfig(entry.getKey(), meta.type, meta.title, item);
            if (config == null || !config.isComplete()) {
                JSONObject alias = aliasOf(object, entry.getKey());
                if (alias != null) {
                    SourceConfig fallback = new SourceConfig(entry.getKey(), meta.type, meta.title, alias);
                    if (fallback.isComplete()) {
                        config = fallback;
                    }
                }
            }
            if (config != null && config.isComplete()) {
                mConfigByType.put(meta.type, config);
            }
        }
        addExtraConfigs(object);
        parseCopyHosts(object.optJSONObject("CopyManHua"));
        addHost(mCopyWebHosts, object.optString("COPYMH"));
        addHost(mCopyApiHosts, object.optString("COPYMHSERVER"));
        String hhaazz = object.optString("HHAAZZ");
        String sw = object.optString("HHAAZZ_SW");
        if (hhaazz != null && !hhaazz.isEmpty()) {
            preferenceManager.putString(PreferenceManager.PREF_HHAAZZ_BASEURL, hhaazz);
        }
        if (sw != null && !sw.isEmpty()) {
            preferenceManager.putString(PreferenceManager.PREF_HHAAZZ_SW, sw);
        }
        return mConfigByType.size() > 0 || object.has("CopyManHua") || object.has("BAOZIMH");
    }

    private void addExtraConfigs(JSONObject object) {
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (GENERIC_ITEMS.containsKey(key) || skipExtraKey(key)) {
                continue;
            }
            JSONObject item = object.optJSONObject(key);
            if (item == null) {
                continue;
            }
            String title = item.optString("title", "").trim();
            if (title.isEmpty()) {
                title = key;
            }
            int type = extraTypeOf(key);
            while (mConfigByType.get(type) != null) {
                type++;
            }
            SourceConfig config = new SourceConfig(key, type, title, item);
            if (config.isComplete()) {
                mConfigByType.put(type, config);
            }
        }
    }

    private static boolean skipExtraKey(String key) {
        return "BaoziManHua".equals(key)
                || "CopyManHua".equals(key)
                || "YiManHua".equals(key)
                || "BaiManGu".equals(key)
                || "PuFeiManHua".equals(key)
                || "BoLeFu".equals(key)
                || "DuManHua".equals(key)
                || "MH50".equals(key)
                || "CoCoManHua".equals(key)
                || "ManHuaCat".equals(key);
    }

    private static int extraTypeOf(String key) {
        int hash = key.hashCode() & 0x7fffffff;
        return 300 + (hash % 9000);
    }

    private JSONObject aliasOf(JSONObject object, String key) {
        if ("BAOZIMH".equals(key)) {
            return object.optJSONObject("BaoziManHua");
        }
        if ("YIMMH".equals(key)) {
            return object.optJSONObject("YiManHua");
        }
        if ("BMG".equals(key)) {
            return object.optJSONObject("BaiManGu");
        }
        if ("PUFEIMH".equals(key)) {
            return object.optJSONObject("PuFeiManHua");
        }
        if ("BOLEFU".equals(key)) {
            return object.optJSONObject("BoLeFu");
        }
        if ("DUMH".equals(key)) {
            return object.optJSONObject("DuManHua");
        }
        return null;
    }

    private void parseCopyHosts(JSONObject object) {
        if (object == null) {
            return;
        }
        addHost(mCopyWebHosts, object.optString("baseUrl"));
        addHost(mCopyWebHosts, object.optString("serverUrl"));
        addHost(mCopyApiHosts, object.optString("serverUrl2"));
        addHost(mCopyApiHosts, object.optString("serverUrl3"));
        addHost(mCopyApiHosts, object.optString("serverUrl"));
        addHost(mCopyWebHosts, object.optString("serverUrl4"));
        String version = object.optString("serverCodeversion", "");
        if (version != null && !version.trim().isEmpty()) {
            mCopyVersion = version.trim();
        }
    }

    private String rawValue(String key, String field) {
        if (mRootJson == null || key == null) {
            return "";
        }
        String direct = mRootJson.optString(key, "");
        if (direct != null && direct.trim().startsWith("http")) {
            return direct.trim();
        }
        JSONObject object = mRootJson.optJSONObject(key);
        if (object != null) {
            return object.optString(field, "").trim();
        }
        return "";
    }

    private static String firstHttp(String value, String fallback) {
        if (value != null) {
            String host = value.trim();
            if (host.startsWith("http")) {
                if (host.endsWith("/")) {
                    host = host.substring(0, host.length() - 1);
                }
                return host;
            }
        }
        return fallback;
    }

    private void addUnique(List<String> hosts, String host) {
        if (host == null || host.isEmpty() || !host.startsWith("http") || hosts.contains(host)) {
            return;
        }
        hosts.add(host);
    }

    private void addHost(List<String> hosts, String raw) {
        if (raw == null) {
            return;
        }
        String host = raw.trim();
        if (host.isEmpty() || host.contains("/api/")) {
            if (host.startsWith("http") && host.contains("/api/")) {
                int idx = host.indexOf("/api/");
                host = host.substring(0, idx);
            } else if (host.isEmpty() || !host.startsWith("http")) {
                return;
            }
        }
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        if (host.startsWith("http") && !hosts.contains(host)) {
            hosts.add(host);
        }
    }

    private void upsert(SourceDao dao, Source source) {
        Source existing = dao.load(source.getType());
        if (existing == null) {
            dao.insert(source);
            return;
        }
        boolean changed = false;
        if (!source.getTitle().equals(existing.getTitle())) {
            existing.setTitle(source.getTitle());
            changed = true;
        }
        if (!existing.getEnable()) {
            existing.setEnable(true);
            changed = true;
        }
        if (changed) {
            dao.update(existing);
        }
    }

    private String readAssetJson() {
        if (mContext == null) {
            return "";
        }
        BufferedReader reader = null;
        try {
            InputStream input = mContext.getAssets().open("sourceBaseUrl.json");
            reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String download(OkHttpClient client, String url) {
        Response response = null;
        try {
            Request request = new Request.Builder().url(url).build();
            response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                if (body != null && body.trim().startsWith("{")) {
                    return body;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }

    private JSONObject parseObject(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(json);
        } catch (Exception e) {
            return null;
        }
    }

    private void addUrl(Set<String> urls, String url) {
        if (url != null && url.startsWith("http")) {
            urls.add(url);
        }
    }

    private static class Item {
        final int type;
        final String title;

        Item(int type, String title) {
            this.type = type;
            this.title = title;
        }
    }
}
