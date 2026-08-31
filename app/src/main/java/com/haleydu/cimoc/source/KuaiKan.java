package com.haleydu.cimoc.source;

import com.haleydu.cimoc.data.SourceConfigManager;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.parser.JsonIterator;
import com.haleydu.cimoc.parser.MangaParser;
import com.haleydu.cimoc.parser.SearchIterator;
import com.haleydu.cimoc.parser.UrlFilter;
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class KuaiKan extends MangaParser {

    public static final int TYPE = 231;
    public static final String DEFAULT_TITLE = "快看漫画";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private final SourceConfigManager sourceConfigManager;

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    public KuaiKan(Source source, SourceConfigManager sourceConfigManager) {
        this.sourceConfigManager = sourceConfigManager;
        if (source == null) {
            source = getDefaultSource();
        }
        init(source, null);
    }

    private String webHost() {
        return sourceConfigManager.firstUrl("https://www.kuaikanmanhua.com", "KUAIKANMANHUA");
    }

    private String apiHost() {
        return sourceConfigManager.firstUrl("https://api.kkmh.com", "KUAIKANMANHUASERVER", "KUAIKANMANHUA");
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("kuaikanmanhua.com"));
        filter.add(new UrlFilter("kkmh.com"));
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws Exception {
        String encoded = URLEncoder.encode(keyword, "UTF-8");
        int offset = Math.max(page - 1, 0) * 20;
        String url = StringUtils.format("%s/v1/search/topic?q=%s&offset=%d&limit=20",
                apiHost(), encoded, offset);
        return apiGet(url);
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        JSONArray array = jsonArray(html, "hit");
        if (array == null) {
            array = new JSONArray();
        }
        return new JsonIterator(array) {
            @Override
            protected Comic parse(JSONObject object) {
                String cid = object.optString("id");
                if (cid.isEmpty() || "0".equals(cid)) {
                    return null;
                }
                String title = object.optString("title");
                String cover = firstNonEmpty(object, "vertical_image_url", "cover_image_url");
                String author = "";
                JSONObject user = object.optJSONObject("user");
                if (user != null) {
                    author = user.optString("nickname");
                }
                return new Comic(TYPE, cid, title, cover, null, author);
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return webHost() + "/web/topic/" + cid;
    }

    @Override
    public Request getInfoRequest(String cid) {
        return apiGet(webHost() + "/v2/pweb/topic/" + cid);
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        try {
            JSONObject info = topicInfo(html);
            if (info == null) {
                return comic;
            }
            String title = info.optString("title");
            String cover = firstNonEmpty(info, "vertical_image_url", "cover_image_url");
            String intro = info.optString("description");
            String author = "";
            JSONObject user = info.optJSONObject("user");
            if (user != null) {
                author = user.optString("nickname");
            }
            JSONArray comics = info.optJSONArray("comics");
            String update = "";
            if (comics != null && comics.length() > 0) {
                JSONObject last = comics.optJSONObject(comics.length() - 1);
                if (last != null) {
                    update = last.optString("created_at");
                }
            }
            boolean finish = intro.contains("完结") || info.optString("update_status").contains("完结");
            comic.setInfo(title.isEmpty() ? comic.getTitle() : title,
                    cover.isEmpty() ? comic.getCover() : cover,
                    update, intro, author, finish);
        } catch (Exception ignored) {
        }
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        try {
            JSONObject info = topicInfo(html);
            JSONArray comics = info == null ? null : info.optJSONArray("comics");
            if (comics == null) {
                comics = new JSONObject(html).optJSONObject("data").optJSONArray("comics");
            }
            if (comics == null) {
                return list;
            }
            int index = 0;
            for (int i = 0; i < comics.length(); i++) {
                JSONObject item = comics.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                boolean locked = item.optBoolean("locked", false);
                boolean pay = item.optBoolean("is_pay_comic", false);
                boolean free = item.optBoolean("is_free", false);
                if (locked || (pay && !free)) {
                    continue;
                }
                String path = item.optString("id");
                if (path.isEmpty() || "0".equals(path)) {
                    continue;
                }
                String title = item.optString("title");
                if (title.isEmpty()) {
                    title = path;
                }
                list.add(new Chapter(Long.parseLong(sourceComic + "000" + index), sourceComic, title, path));
                index++;
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        return apiGet(apiHost() + "/v2/comic/" + path);
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        Long comicChapter = chapter == null ? null : chapter.getId();
        if (html == null || comicChapter == null) {
            return list;
        }
        try {
            JSONObject root = new JSONObject(html);
            JSONObject data = root.optJSONObject("data");
            if (data == null) {
                return list;
            }
            JSONArray images = data.optJSONArray("images");
            if (images == null) {
                return list;
            }
            for (int i = 0; i < images.length(); i++) {
                String url = images.optString(i);
                if (url == null || url.isEmpty()) {
                    continue;
                }
                Long id = Long.parseLong(comicChapter + "000" + i);
                list.add(new ImageUrl(id, comicChapter, i + 1, url, false));
            }
        } catch (Exception ignored) {
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
            JSONObject info = topicInfo(html);
            JSONArray comics = info == null ? null : info.optJSONArray("comics");
            if (comics != null && comics.length() > 0) {
                JSONObject last = comics.optJSONObject(comics.length() - 1);
                if (last != null) {
                    return last.optString("created_at");
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    @Override
    public Headers getHeader() {
        return Headers.of(
                "Referer", webHost() + "/",
                "User-Agent", USER_AGENT,
                "Accept", "application/json"
        );
    }

    private Request apiGet(String url) {
        return new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", webHost() + "/")
                .header("Accept", "application/json")
                .build();
    }

    private JSONObject topicInfo(String html) {
        try {
            JSONObject data = new JSONObject(html).optJSONObject("data");
            if (data == null) {
                return null;
            }
            return data.optJSONObject("topic_info");
        } catch (Exception e) {
            return null;
        }
    }

    private JSONArray jsonArray(String html, String key) {
        try {
            JSONObject root = new JSONObject(html);
            JSONObject data = root.optJSONObject("data");
            if (data != null) {
                JSONArray nested = data.optJSONArray(key);
                if (nested != null) {
                    return nested;
                }
            }
            return root.optJSONArray(key);
        } catch (Exception e) {
            return null;
        }
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
}
