package com.haleydu.cimoc.source;

import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.parser.NodeIterator;
import com.haleydu.cimoc.parser.SearchIterator;
import com.haleydu.cimoc.soup.Node;
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Request;

public class Vomic extends GenericHtmlParser {

    public static final int TYPE = 227;

    private String mKeyword = "";

    public Vomic(Source source, SourceConfig config) {
        super(source, config);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws Exception {
        mKeyword = keyword == null ? "" : keyword.trim();
        if (page != 1) {
            return null;
        }
        return buildGet(mConfig.baseUrl + "/");
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        List<Node> nodes;
        try {
            nodes = body.list("a[href*=/detail/]");
        } catch (Exception e) {
            return super.getSearchIterator(html, page);
        }
        final String keyword = mKeyword;
        return new NodeIterator(nodes) {
            @Override
            protected Comic parse(Node node) {
                String cid = extractPath(node.href());
                if (cid == null || cid.isEmpty()) {
                    return null;
                }
                String title = node.text("div.title");
                if (title == null || title.isEmpty()) {
                    title = node.text();
                }
                if (!keyword.isEmpty() && (title == null || !title.contains(keyword))) {
                    return null;
                }
                String cover = node.src("img");
                if (cover != null && cover.startsWith("//")) {
                    cover = "https:" + cover;
                }
                return new Comic(TYPE, cid, title, cover, null, null);
            }
        };
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        String title = meta(html, "og:title");
        if (title != null) {
            int dash = title.indexOf(" - ");
            if (dash > 0) {
                title = title.substring(0, dash).trim();
            }
        }
        String cover = meta(html, "og:image");
        if (cover != null) {
            cover = cover.replace("&amp;", "&");
        }
        String intro = StringUtils.match("简介：</span><span[^>]*>(.*?)</span>", html, 1);
        if (intro != null) {
            intro = intro.replace("<!-- -->", "").trim();
        }
        boolean finish = isFinish(html);
        comic.setInfo(empty(title, comic.getTitle()),
                empty(cover, comic.getCover()),
                null, intro, null, finish);
        return comic;
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        Long comicChapter = chapter == null ? null : chapter.getId();
        if (html == null || comicChapter == null) {
            return list;
        }
        JSONArray array = findImgList(html);
        if (array == null) {
            return list;
        }
        int index = 0;
        for (int i = 0; i < array.length(); i++) {
            String url;
            JSONObject item = array.optJSONObject(i);
            if (item != null) {
                url = item.optString("url", "");
                if (url.isEmpty()) {
                    url = item.optString("src", "");
                }
            } else {
                url = array.optString(i, "");
            }
            url = absImage(url);
            if (url == null) {
                continue;
            }
            Long id = Long.parseLong(comicChapter + "000" + index);
            list.add(new ImageUrl(id, comicChapter, ++index, url, false));
        }
        return list;
    }

    private JSONArray findImgList(String html) {
        Matcher matcher = Pattern.compile("\"img_list\"\\s*:\\s*(\\[.*?\\])").matcher(html);
        while (matcher.find()) {
            try {
                JSONArray array = new JSONArray(matcher.group(1));
                if (array.length() > 0) {
                    return array;
                }
            } catch (Exception ignored) {
            }
        }
        String json = StringUtils.match("\"chapterData\"\\s*:\\s*(\\{.*?\\})", html, 1);
        if (json != null) {
            try {
                return new JSONObject(json).optJSONArray("img_list");
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String meta(String html, String property) {
        String value = StringUtils.match("property=\"" + property + "\" content=\"([^\"]+)\"", html, 1);
        if (value == null) {
            value = StringUtils.match("content=\"([^\"]+)\" property=\"" + property + "\"", html, 1);
        }
        return value;
    }

    private static String empty(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
