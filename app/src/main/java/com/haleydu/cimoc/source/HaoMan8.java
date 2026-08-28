package com.haleydu.cimoc.source;

import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.parser.JsonIterator;
import com.haleydu.cimoc.parser.SearchIterator;
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;

import okhttp3.Request;

public class HaoMan8 extends GenericHtmlParser {

    public static final int TYPE = 206;
    public static final String DEFAULT_TITLE = "好漫8";
    private static final String DEFAULT_API = "https://v4api.zaimanhua.com";

    public HaoMan8(Source source, SourceConfig config) {
        super(source, config);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    private String apiHost() {
        String server = mConfig.serverUrl;
        if (server != null) {
            server = server.trim();
            if (server.startsWith("http")) {
                if (server.endsWith("/")) {
                    server = server.substring(0, server.length() - 1);
                }
                return server;
            }
        }
        return DEFAULT_API;
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws Exception {
        String encoded = URLEncoder.encode(keyword, "UTF-8");
        String path = StringUtils.format(
                "/app/v1/search/index?keyword=%s&source=0&page=%d&size=20",
                encoded, page);
        String host = apiHost();
        Request.Builder builder = requestBuilder(host + path);
        if (builder == null) {
            return null;
        }
        if (!DEFAULT_API.equals(host)) {
            builder.header("X-Cimoc-Fallback", DEFAULT_API + path);
        }
        return builder.build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        JSONArray array = extractList(html);
        if (array == null) {
            return super.getSearchIterator(html, page);
        }
        return new JsonIterator(array) {
            @Override
            protected Comic parse(JSONObject object) {
                String cid = firstString(object, "id", "comic_id", "comicId");
                if (cid.isEmpty()) {
                    return null;
                }
                String title = firstString(object, "title", "name", "comic_name");
                String cover = firstString(object, "cover", "coverUrl", "picUrl", "pic");
                if (cover.startsWith("//")) {
                    cover = "https:" + cover;
                }
                String author = firstString(object, "authors", "author", "author_name");
                String update = firstString(object, "last_update_chapter_name", "last_name", "status");
                return new Comic(TYPE, cid, title, cover, update, author);
            }
        };
    }

    private static JSONArray extractList(String html) {
        if (html == null) {
            return null;
        }
        String trimmed = html.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return null;
        }
        try {
            if (trimmed.startsWith("[")) {
                return new JSONArray(trimmed);
            }
            JSONObject root = new JSONObject(trimmed);
            JSONArray direct = firstArray(root, "comicList", "list", "comics", "data");
            if (direct != null) {
                return direct;
            }
            JSONObject data = root.optJSONObject("data");
            if (data != null) {
                return firstArray(data, "comicList", "list", "comics", "result");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static JSONArray firstArray(JSONObject object, String... keys) {
        for (String key : keys) {
            JSONArray array = object.optJSONArray(key);
            if (array != null && array.length() > 0) {
                return array;
            }
        }
        return null;
    }

    private static String firstString(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = object.optString(key, "");
            if (value != null && !value.isEmpty() && !"null".equals(value)) {
                return value;
            }
        }
        return "";
    }
}
