package com.haleydu.cimoc.source;

import android.util.Pair;

import com.google.common.collect.Lists;
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
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import taobe.tec.jcc.JChineseConvertor;

import static com.haleydu.cimoc.core.Manga.getResponseBody;

public class CopyMH extends MangaParser {
    public static final int TYPE = 26;
    public static final String DEFAULT_TITLE = "拷贝漫画";

    private final SourceConfigManager sourceConfigManager;
    private final OkHttpClient httpClient;

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    public CopyMH(Source source, SourceConfigManager sourceConfigManager, OkHttpClient httpClient) {
        this.sourceConfigManager = sourceConfigManager;
        this.httpClient = httpClient;
        init(source, new Category());
    }

    private Request apiRequest(String path) {
        List<String> hosts = sourceConfigManager.getCopyApiHosts();
        String primary = hosts.get(0) + path;
        Request.Builder builder = new Request.Builder()
                .url(primary)
                .addHeader("User-Agent", "COPY/3.0.0")
                .addHeader("Accept", "application/json")
                .addHeader("version", sourceConfigManager.getCopyVersion())
                .addHeader("platform", "1")
                .addHeader("webp", "1")
                .addHeader("region", "1");
        if (hosts.size() > 1) {
            StringBuilder fallback = new StringBuilder();
            for (int i = 1; i < hosts.size(); i++) {
                if (fallback.length() > 0) {
                    fallback.append(',');
                }
                fallback.append(hosts.get(i)).append(path);
            }
            builder.header("X-Cimoc-Fallback", fallback.toString());
        }
        return builder.build();
    }

    private String webBase() {
        return sourceConfigManager.getCopyWebBase();
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        String path = StringUtils.format("/api/v3/search/comic?platform=1&limit=30&offset=%d&q=%s",
                (page - 1) * 30, keyword);
        List<String> hosts = sourceConfigManager.getCopyWebHosts();
        String primary = hosts.get(0) + path;
        Request.Builder builder = new Request.Builder()
                .url(primary)
                .addHeader("User-Agent", "COPY/3.0.0")
                .addHeader("Accept", "application/json")
                .addHeader("version", sourceConfigManager.getCopyVersion())
                .addHeader("platform", "1")
                .addHeader("webp", "1")
                .addHeader("region", "1");
        if (hosts.size() > 1) {
            StringBuilder fallback = new StringBuilder();
            for (int i = 1; i < hosts.size(); i++) {
                if (fallback.length() > 0) {
                    fallback.append(',');
                }
                fallback.append(hosts.get(i)).append(path);
            }
            builder.header("X-Cimoc-Fallback", fallback.toString());
        }
        return builder.build();
    }

    @Override
    public String getUrl(String cid) {
        return webBase() + "/comic/".concat(cid);
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("copy3000.com", "\\w+", 0));
        filter.add(new UrlFilter("2026copy.com", "\\w+", 0));
        filter.add(new UrlFilter("2025copy.com", "\\w+", 0));
        filter.add(new UrlFilter("mangacopy.com", "\\w+", 0));
        filter.add(new UrlFilter("copymanga.com", "\\w+", 0));
        filter.add(new UrlFilter("copy20.com", "\\w+", 0));
        filter.add(new UrlFilter("copy2000.site", "\\w+", 0));
        filter.add(new UrlFilter("copy2000.online", "\\w+", 0));
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        try {
            JSONObject jsonObject = new JSONObject(html);
            return new JsonIterator(jsonObject.getJSONObject("results").getJSONArray("list")) {
                @Override
                protected Comic parse(JSONObject object) {
                    try {
                        JChineseConvertor jChineseConvertor = JChineseConvertor.getInstance();
                        String cid = object.getString("path_word");
                        String title = jChineseConvertor.t2s(object.getString("name"));
                        String cover = object.getString("cover");
                        String author = object.getJSONArray("author").getJSONObject(0).getString("name").trim();
                        return new Comic(TYPE, cid, title, cover, null, author);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Request getInfoRequest(String cid) {
        return apiRequest("/api/v3/comic2/".concat(cid) + "?platform=1");
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        try {
            JSONObject comicInfo = new JSONObject(html).getJSONObject("results");
            JSONObject body = comicInfo.getJSONObject("comic");
            String cover = body.getString("cover");
            String intro = body.getString("brief");
            String title = body.getString("name");
            String update = body.getString("datetime_updated");
            String author = ((JSONObject) body.getJSONArray("author").get(0)).getString("name");
            boolean finish = body.getJSONObject("status").getInt("value") != 0;
            JSONObject group = comicInfo.getJSONObject("groups");
            comic.note = group;
            comic.setInfo(title, cover, update, intro, author, finish);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return comic;
    }

    @Override
    public Request getChapterRequest(String html, String cid) {
        return apiRequest(String.format("/api/v3/comic/%s/group/default/chapters?limit=500&offset=0&platform=1", cid));
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        List<Chapter> list = new LinkedList<>();
        JSONObject jsonObject = new JSONObject(html);
        JSONArray array = jsonObject.getJSONObject("results").getJSONArray("list");
        int index = 0;
        for (int i = 0; i < array.length(); ++i) {
            String title = array.getJSONObject(i).getString("name");
            String path = array.getJSONObject(i).getString("uuid");
            list.add(new Chapter(Long.parseLong(sourceComic + "000" + index++), sourceComic, title, path, "默认"));
        }
        try {
            JSONObject groups = (JSONObject) comic.note;
            Iterator<String> keys = groups.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.equals("default")) continue;
                String path_word = groups.getJSONObject(key).getString("path_word");
                String PathName = groups.getJSONObject(key).getString("name");
                html = getResponseBody(httpClient, apiRequest(String.format("/api/v3/comic/%s/group/%s/chapters?limit=500&offset=0&platform=1", comic.getCid(), path_word)));
                jsonObject = new JSONObject(html);
                array = jsonObject.getJSONObject("results").getJSONArray("list");
                for (int i = 0; i < array.length(); ++i) {
                    String title = array.getJSONObject(i).getString("name");
                    String path = array.getJSONObject(i).getString("uuid");
                    list.add(new Chapter(Long.parseLong(sourceComic + "000" + index++), sourceComic, title, path, PathName));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Lists.reverse(list);
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        return apiRequest(StringUtils.format("/api/v3/comic/%s/chapter2/%s?platform=1", cid, path));
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws JSONException {
        List<ImageUrl> list = new LinkedList<>();
        JSONObject jsonObject = new JSONObject(html);
        JSONArray array = jsonObject.getJSONObject("results").getJSONObject("chapter").getJSONArray("contents");
        for (int i = 0; i < array.length(); ++i) {
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "000" + i);
            String url = array.getJSONObject(i).getString("url");
            list.add(new ImageUrl(id, comicChapter, i + 1, url, false));
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
            JSONObject comicInfo = new JSONObject(html).getJSONObject("results");
            JSONObject body = comicInfo.getJSONObject("comic");
            return body.getString("datetime_updated");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", webBase() + "/", "User-Agent", "COPY/3.0.0");
    }

    @Override
    public Request getCategoryRequest(String format, int page) {
        int offset = Math.max(page - 1, 0) * 30;
        String path = format.contains("%d") ? StringUtils.format(format, offset) : format;
        return apiRequest(path);
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new LinkedList<>();
        try {
            JSONArray array = new JSONObject(html).getJSONObject("results").getJSONArray("list");
            JChineseConvertor convertor = JChineseConvertor.getInstance();
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String cid = object.getString("path_word");
                String title = convertor.t2s(object.getString("name"));
                String cover = object.getString("cover");
                String author = null;
                JSONArray authors = object.optJSONArray("author");
                if (authors != null && authors.length() > 0) {
                    author = authors.getJSONObject(0).optString("name").trim();
                }
                String update = object.optString("datetime_updated");
                list.add(new Comic(TYPE, cid, title, cover, update, author));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static class Category extends MangaCategory {

        @Override
        public boolean isComposite() {
            return true;
        }

        @Override
        public String getFormat(String... args) {
            String theme = value(args, CATEGORY_SUBJECT);
            String top = value(args, CATEGORY_AREA);
            String status = value(args, CATEGORY_PROGRESS);
            String ordering = value(args, CATEGORY_ORDER);
            if (ordering.isEmpty()) {
                ordering = "-datetime_updated";
            }
            StringBuilder builder = new StringBuilder("/api/v3/comics?limit=30&offset=%d&platform=1&ordering=");
            builder.append(ordering);
            if (!theme.isEmpty()) {
                builder.append("&theme=").append(theme);
            }
            if (!top.isEmpty()) {
                builder.append("&top=").append(top);
            }
            if (!status.isEmpty()) {
                builder.append("&status=").append(status);
            }
            return builder.toString();
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("爱情", "aiqing"));
            list.add(Pair.create("冒险", "maoxian"));
            list.add(Pair.create("奇幻", "qihuan"));
            list.add(Pair.create("百合", "baihe"));
            list.add(Pair.create("校园", "xiaoyuan"));
            list.add(Pair.create("科幻", "kehuan"));
            list.add(Pair.create("生活", "shenghuo"));
            list.add(Pair.create("热血", "rexue"));
            list.add(Pair.create("搞笑", "gaoxiao"));
            list.add(Pair.create("都市", "dushi"));
            list.add(Pair.create("悬疑", "xuanyi"));
            list.add(Pair.create("仙侠", "xianxia"));
            list.add(Pair.create("恐怖", "kongbu"));
            return list;
        }

        @Override
        protected boolean hasArea() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getArea() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("日漫", "japan"));
            list.add(Pair.create("韩漫", "korea"));
            list.add(Pair.create("美漫", "west"));
            return list;
        }

        @Override
        public boolean hasProgress() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getProgress() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("连载中", "1"));
            list.add(Pair.create("已完结", "2"));
            return list;
        }

        @Override
        protected boolean hasOrder() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getOrder() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("更新", "-datetime_updated"));
            list.add(Pair.create("热门", "-popular"));
            return list;
        }

        private String value(String[] args, int index) {
            if (args == null || index >= args.length || args[index] == null) {
                return "";
            }
            return args[index];
        }
    }
}
