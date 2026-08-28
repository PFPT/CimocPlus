package com.haleydu.cimoc.source;

import android.annotation.SuppressLint;

import com.haleydu.cimoc.core.Manga;
import com.haleydu.cimoc.data.SourceConfigManager;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.parser.JsonIterator;
import com.haleydu.cimoc.parser.MangaParser;
import com.haleydu.cimoc.parser.SearchIterator;
import com.haleydu.cimoc.parser.UrlFilter;
import com.haleydu.cimoc.soup.Node;
import com.haleydu.cimoc.utils.StringUtils;
import com.haleydu.cimoc.utils.UicodeBackslashU;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import okhttp3.Headers;
import okhttp3.Request;

public class DmzjFix extends MangaParser {
    public static final int TYPE = 100;
    public static final String DEFAULT_TITLE = "动漫之家v2Fix";
    private final SourceConfigManager sourceConfigManager;

    public DmzjFix(Source source, SourceConfigManager sourceConfigManager) {
        this.sourceConfigManager = sourceConfigManager;
        init(source, null);
    }

    private String mobileHost() {
        return sourceConfigManager.firstUrl("https://m.dmzj.com", "DMZJV2", "DongManZhiJia", "DMZJ", "DMZJFIX");
    }

    private String pictureHost() {
        return sourceConfigManager.firstUrl("https://images.dmzj.com", "DMZJFIXPICTURE", "DongManZhiJia");
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("manhua.dmzj.com", "/(\\w+)"));
        filter.add(new UrlFilter("m.dmzj.com", "/info/(\\w+).html"));
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException, Exception {
        String encoded = java.net.URLEncoder.encode(keyword, "UTF-8");
        String suffix = StringUtils.format("/search/show/0/%s/%d.json", encoded, Math.max(page - 1, 0));
        List<String> hosts = sourceConfigManager.listUrls(
                "https://v3api.idmzj.com", "DMZJV2SERVER", "DMZJSERVER");
        if (!hosts.contains("https://v3api.dmzj.com")) {
            hosts.add("https://v3api.dmzj.com");
        }
        if (!hosts.contains("http://v3api.dmzj1.com")) {
            hosts.add("http://v3api.dmzj1.com");
        }
        String host = hosts.isEmpty() ? "https://v3api.idmzj.com" : hosts.get(0);
        Request.Builder builder = new Request.Builder()
                .url(host + suffix)
                .header("User-Agent",
                        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        sourceConfigManager.applyHostFallbacks(builder, suffix, hosts);
        return builder.build();
    }

    @Override
    public String getUrl(String cid) {
        return StringUtils.format("%s/info/%s.html", mobileHost(), cid);
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
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
                        String title = firstNonEmpty(object, "title", "name", "comic_name");
                        String cover = firstNonEmpty(object, "cover", "comic_cover");
                        if (!cover.isEmpty() && !cover.startsWith("http") && !cover.startsWith("//")) {
                            cover = pictureHost() + "/" + cover;
                        } else if (cover.startsWith("//")) {
                            cover = "https:" + cover;
                        }
                        String author = firstNonEmpty(object, "authors", "author", "comic_author");
                        String update = object.optString("last_name");
                        if (object.has("last_updatetime")) {
                            try {
                                long time = Long.parseLong(object.getString("last_updatetime")) * 1000;
                                update = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(time));
                            } catch (Exception ignored) {
                            }
                        }
                        return new Comic(TYPE, cid, title, cover, update, author);
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
        String embedded = StringUtils.match("var serchArry=(\\[\\{.*?\\}\\])", html, 1);
        if (embedded == null) {
            embedded = StringUtils.match("var g_search_data = (.*?);", html, 1);
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

    @Override
    public Request getInfoRequest(String cid) {
        String suffix = StringUtils.format("/dynamic/comicinfo/%s.json", cid);
        return sourceConfigManager.hostPathRequest(suffix, "http://api.dmzj.com", "DMZJFIXSERVER", "DongManZhiJia");
    }

    public Headers getHeader() {
        return Headers.of("Referer", pictureHost() + "/");
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        try {
            JSONObject root = new JSONObject(html).getJSONObject("data");
            JSONObject info = root.getJSONObject("info");
            String title = info.getString("title");
            boolean status = !info.getString("status").contains("连载");
            String cover = info.getString("cover");
            String author = info.getString("authors");
            String update = info.getString("last_updatetime");
            update = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Integer.parseInt(update) * 1000));
            String intro = info.getString("description");
            comic.setInfo(title, cover, update, intro, author, status);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        List<Chapter> list1 = new LinkedList<>();

        try {
            JSONArray root = new JSONObject(html).getJSONObject("data").getJSONArray("list");
            for (int i = 0; i < root.length(); i++) {
                JSONObject chapter = root.getJSONObject(i);
                String title = chapter.getString("chapter_name");
                String comic_id = chapter.getString("comic_id");
                String chapter_id = chapter.getString("id");
                String path = comic_id + "/" + chapter_id;
                list.add(new Chapter(Long.parseLong(sourceComic + "000" + i + 1), sourceComic, title, path, "默认线路"));
                list1.add(new Chapter(Long.parseLong(sourceComic + "001" + i + 1), sourceComic, title.concat(" (备用)"), path + "x", "备用线路"));


            }
            list.addAll(list1);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;

        }
        return list;


    }

    @Override
    public Request getImagesRequest(String cid, String path) {

        String suffix = StringUtils.format("/chapinfo/%s.html", path.replace("x", ""));
        return sourceConfigManager.hostPathRequest(suffix, "https://m.dmzj.com", "DMZJV2", "DongManZhiJia", "DMZJ");
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws Manga.NetworkErrorException, JSONException {
        List<ImageUrl> list = new LinkedList<>();
        JSONArray root = new JSONObject(html).getJSONArray("page_url");
        String flag = chapter.getId().toString().replace(chapter.getSourceComic().toString(), "");
        for (int i = 0; i < root.length(); i++) {
            Long comicChapter = chapter.getId();
            String url = root.getString(i);
            Long id = Long.parseLong(comicChapter + "000" + i);

            if (flag.startsWith("001")) {
                url = url.replace("dmzj", "dmzj1");

            }
            list.add(new ImageUrl(id, comicChapter, i + 1, url, false));


        }


        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public String parseCheck(String html) {
        try {
            String update = new JSONObject(html).getJSONObject("data").getString("last_updatetime");
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Integer.parseInt(update) * 1000));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;

    }

}
