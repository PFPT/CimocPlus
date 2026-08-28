package com.haleydu.cimoc.source;

import android.util.Pair;

import com.haleydu.cimoc.data.SourceConfigManager;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.parser.JsonIterator;
import com.haleydu.cimoc.parser.MangaCategory;
import com.haleydu.cimoc.parser.MangaParser;
import com.haleydu.cimoc.parser.SearchIterator;
import com.haleydu.cimoc.parser.UrlFilter;
import com.haleydu.cimoc.utils.LogUtil;
import com.haleydu.cimoc.utils.StringUtils;
import com.haleydu.cimoc.utils.UicodeBackslashU;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by Hiroshi on 2016/7/8.
 */
public class Dmzj extends MangaParser {

    public static final int TYPE = 1;
    public static final String DEFAULT_TITLE = "动漫之家";
    private final SourceConfigManager sourceConfigManager;

    public Dmzj(Source source, SourceConfigManager sourceConfigManager) {
        this.sourceConfigManager = sourceConfigManager;
        init(source, new Category());
    }

    private String mobileHost() {
        return sourceConfigManager.firstUrl("http://m.dmzj1.com", "DMZJ", "DongManZhiJia", "DMZJV2");
    }

    private String pictureHost() {
        return sourceConfigManager.firstUrl("http://images.dmzj1.com", "DMZJPICTURE", "DongManZhiJia");
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        String encoded = keyword;
        try {
            encoded = java.net.URLEncoder.encode(keyword, "UTF-8");
        } catch (Exception ignored) {
        }
        String suffix = StringUtils.format("/search/show/0/%s/%d.json", encoded, Math.max(page - 1, 0));
        List<String> hosts = sourceConfigManager.listUrls(
                "https://v3api.idmzj.com", "DMZJV2SERVER", "DMZJSERVER");
        addHost(hosts, "https://v3api.dmzj.com");
        addHost(hosts, "http://v3api.dmzj1.com");
        String host = hosts.isEmpty() ? "https://v3api.idmzj.com" : hosts.get(0);
        Request.Builder builder = new Request.Builder()
                .url(host + suffix)
                .header("User-Agent",
                        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        sourceConfigManager.applyHostFallbacks(builder, suffix, hosts);
        return builder.build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        try {
            JSONArray array = searchArray(html);
            if (array == null) {
                return null;
            }
            return new JsonIterator(array) {
                @Override
                protected Comic parse(JSONObject object) {
                    try {
                        String cid = object.optString("id");
                        if (cid.isEmpty()) {
                            return null;
                        }
                        String title = firstNonEmpty(object, "title", "comic_name", "name");
                        String cover = firstNonEmpty(object, "cover", "comic_cover");
                        if (cover.startsWith("//")) {
                            cover = "https:" + cover;
                        }
                        String author = firstNonEmpty(object, "authors", "comic_author", "author");
                        return new Comic(TYPE, cid, title, cover, null, author);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONArray searchArray(String html) throws JSONException {
        if (html == null) {
            return null;
        }
        String trimmed = html.trim();
        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }
        String embedded = StringUtils.match("var g_search_data = (.*?);", html, 1);
        if (embedded == null) {
            embedded = StringUtils.match("var serchArry=(\\[\\{.*?\\}\\])", html, 1);
        }
        if (embedded == null) {
            return null;
        }
        return new JSONArray(UicodeBackslashU.unicodeToCn(embedded).replace("\\/", "/"));
    }

    private static String firstNonEmpty(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = object.optString(key, "");
            if (value != null && !value.isEmpty() && !"null".equals(value)) {
                return value;
            }
        }
        return "";
    }

    private static void addHost(List<String> hosts, String host) {
        if (host != null && !host.isEmpty() && !hosts.contains(host)) {
            hosts.add(host);
        }
    }

    @Override
    public String getUrl(String cid) {
        return StringUtils.format("%s/info/%s.html", mobileHost(), cid);
    }

    @Override
    public Request getInfoRequest(String cid) {
        cid = "comic_"+cid+"_android";
        String suffix = StringUtils.format("/comic/%s.json", cid);
        return sourceConfigManager.hostPathRequest(suffix, "http://v3api.dmzj1.com", "DMZJSERVER", "DMZJV2SERVER");
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        try {
            JSONObject object = new JSONObject(html);
            String title = object.getString("title");
            String cover = object.getString("cover");
            Long time = object.has("last_updatetime") ? object.getLong("last_updatetime") * 1000 : null;
            String update = time == null ? null : StringUtils.getFormatTime("yyyy-MM-dd", time);
            String intro = object.optString("description");
            StringBuilder sb = new StringBuilder();
            JSONArray array = object.getJSONArray("authors");
            for (int i = 0; i < array.length(); ++i) {
                sb.append(array.getJSONObject(i).getString("tag_name")).append(" ");
            }
            String author = sb.toString();
            boolean status = object.getJSONArray("status").getJSONObject(0).getInt("tag_id") == 2310;
            comic.setInfo(title, cover, update, intro, author, status);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        try {
            JSONObject object = new JSONObject(html);
            JSONArray array = object.getJSONArray("chapters");
            int k=0;
            for (int i = 0; i != array.length(); ++i) {
                JSONArray data = array.getJSONObject(i).getJSONArray("data");
                for (int j = 0; j != data.length(); ++j) {
                    JSONObject chapter = data.getJSONObject(j);
                    String title = chapter.getString("chapter_title");
                    String path = chapter.getString("chapter_id");
                    list.add(new Chapter(Long.parseLong(sourceComic + "000" + k++), sourceComic, title, path));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String suffix = StringUtils.format("/chapter/%s/%s.json", cid, path);
        return sourceConfigManager.hostPathRequest(suffix, "http://v3api.dmzj1.com", "DMZJSERVER", "DMZJV2SERVER");
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        try {
            JSONObject object = new JSONObject(html);
            JSONArray array = object.getJSONArray("page_url");
            for (int i = 0; i < array.length(); ++i) {
                Long comicChapter = chapter.getId();
                Long id = Long.parseLong(comicChapter + "000" + i);
                list.add(new ImageUrl(id, comicChapter, i + 1, array.getString(i), false));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        try {
            JSONObject object = new JSONObject(html);
            long time = object.getLong("last_updatetime") * 1000;
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(time));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new LinkedList<>();
        try {
            String decodeJsonString = UicodeBackslashU.unicodeToCn(html).replace("\\/","/");
            JSONArray array = new JSONArray(decodeJsonString);
            for (int i = 0; i != array.length(); ++i) {
                try {
                    JSONObject object = array.getJSONObject(i);
                    if (object.optInt("hidden", 1) != 1) {
                        String cid = object.getString("id");
                        String title = object.getString("comic_name");
                        String cover = pictureHost() + "/".concat(object.getString("comic_cover"));
                        Long time = object.has("last_updatetime") ? object.getLong("last_updatetime") * 1000 : null;
                        String update = time == null ? null : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(time));
                        String author = object.optString("comic_author");
                        list.add(new Comic(TYPE, cid, title, cover, update, author));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", mobileHost() + "/");
    }

    private class Category extends MangaCategory {

        @Override
        public boolean isComposite() {
            return true;
        }

        @Override
        public String getFormat(String... args) {
            return StringUtils.format("%s/classify/%s-%s-%s-%s-%s-%%d.json",
                    mobileHost(),
                    args[CATEGORY_SUBJECT], args[CATEGORY_READER], args[CATEGORY_PROGRESS], args[CATEGORY_AREA], args[CATEGORY_ORDER]);
        }

        @Override
        public List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", "0"));
            list.add(Pair.create("冒险", "1"));
            list.add(Pair.create("欢乐向", "2"));
            list.add(Pair.create("格斗", "3"));
            list.add(Pair.create("科幻", "4"));
            list.add(Pair.create("爱情", "5"));
            list.add(Pair.create("竞技", "6"));
            list.add(Pair.create("魔法", "7"));
            list.add(Pair.create("校园", "8"));
            list.add(Pair.create("悬疑", "9"));
            list.add(Pair.create("恐怖", "10"));
            list.add(Pair.create("生活亲情", "11"));
            list.add(Pair.create("百合", "12"));
            list.add(Pair.create("伪娘", "13"));
            list.add(Pair.create("耽美", "14"));
            list.add(Pair.create("后宫", "15"));
            list.add(Pair.create("萌系", "16"));
            list.add(Pair.create("治愈", "17"));
            list.add(Pair.create("武侠", "18"));
            list.add(Pair.create("职场", "19"));
            list.add(Pair.create("奇幻", "20"));
            list.add(Pair.create("节操", "21"));
            list.add(Pair.create("轻小说", "22"));
            list.add(Pair.create("搞笑", "23"));
            return list;
        }

        @Override
        public boolean hasArea() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getArea() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", "0"));
            list.add(Pair.create("日本", "1"));
            list.add(Pair.create("内地", "2"));
            list.add(Pair.create("欧美", "3"));
            list.add(Pair.create("港台", "4"));
            list.add(Pair.create("韩国", "5"));
            list.add(Pair.create("其他", "6"));
            return list;
        }

        @Override
        public boolean hasReader() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getReader() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", "0"));
            list.add(Pair.create("少年", "3262"));
            list.add(Pair.create("少女", "3263"));
            list.add(Pair.create("青年", "3264"));
            return list;
        }

        @Override
        public boolean hasProgress() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getProgress() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", "0"));
            list.add(Pair.create("连载", "1"));
            list.add(Pair.create("完结", "2"));
            return list;
        }

        @Override
        public boolean hasOrder() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getOrder() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("更新", "1"));
            list.add(Pair.create("人气", "0"));
            return list;
        }

    }

}
