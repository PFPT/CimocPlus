package com.haleydu.cimoc.source;

import com.google.common.collect.Lists;
import com.haleydu.cimoc.data.SourceConfigManager;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.parser.MangaParser;
import com.haleydu.cimoc.parser.NodeIterator;
import com.haleydu.cimoc.parser.SearchIterator;
import com.haleydu.cimoc.parser.UrlFilter;
import com.haleydu.cimoc.soup.Node;
import com.haleydu.cimoc.utils.DecryptionUtils;
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;

import static com.haleydu.cimoc.utils.DecryptionUtils.evalDecrypt;

/**
 * Created by FEILONG on 2017/12/21.
 * need fix
 */

public class Tencent extends MangaParser {

    public static final int TYPE = 51;
    public static final String DEFAULT_TITLE = "腾讯动漫";
    private static final String MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    private final SourceConfigManager sourceConfigManager;

    public Tencent(Source source, SourceConfigManager sourceConfigManager) {
        this.sourceConfigManager = sourceConfigManager;
        init(source, null);
    }

    private String mobileHost() {
        return sourceConfigManager.firstUrl("https://m.ac.qq.com", "TENCENTM", "TENCENT");
    }

    private String webHost() {
        return sourceConfigManager.firstUrl("http://ac.qq.com", "TENCENT", "TENCENTM");
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        if (page != 1) {
            return null;
        }
        String path = StringUtils.format("/search/result?word=%s", keyword);
        return mobileRequest(path);
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        return new NodeIterator(body.list(".comic-item")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.attr("a", "href");
                cid = cid.substring("/comic/index/id/".length());
                String title = node.text(".comic-title");
                String cover = node.attr(".cover-image", "src");
                String update = node.text(".comic-update");
                String author = "UNKNOWN";
                return new Comic(TYPE, cid, title, cover, update, author);
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return webHost() + "/Comic/ComicInfo/id/".concat(cid);
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("ac.qq.com"));
        filter.add(new UrlFilter("m.ac.qq.com"));
    }

    @Override
    public Request getInfoRequest(String cid) {
        return mobileRequest("/comic/index/id/".concat(cid));
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text("div.head-title-tags > h1");
        String cover = body.src("div.head-banner > img");
        String update = "";
        String author = body.text("li.author-wr");
        String intro = body.text("div.head-info-desc");
        boolean status = isFinish("连载中");//todo: fix here
        comic.setInfo(title, cover, update, intro, author, status);
        return comic;
    }

    @Override
    public Request getChapterRequest(String html, String cid) {
        return mobileRequest("/comic/chapterList/id/".concat(cid));
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        int i=0;
        for (Node node : new Node(html).list("ul.normal > li.chapter-item")) {
            String title = node.text("a");
            String href = node.href("a");
            if (href == null || href.isEmpty()) {
                continue;
            }
            String path = href.contains("/cid/") ? href.substring(href.lastIndexOf("/cid/") + 5) : href;
            list.add(new Chapter(Long.parseLong(sourceComic + "000" + i++), sourceComic, title, path));
        }
        return Lists.reverse(list);
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String suffix = StringUtils.format("/chapter/index/id/%s/cid/%s", cid, path);
        return mobileRequest(suffix);
    }

    private String splice(String str, int from, int length) {
        return str.substring(0, from) + str.substring(from + length, str.length());
    }

    private String decodeData(String str, String nonce) {
        nonce = evalDecrypt(nonce);
        Matcher m = Pattern.compile("\\d+[a-zA-Z]+").matcher(nonce);
        final List<String> matches = new ArrayList<>();
        while (m.find()) {
            matches.add(m.group(0));
        }
        int len = matches.size();
        while ((len--) != 0) {
            str = splice(str,
                    Integer.parseInt(StringUtils.match("^\\d+", matches.get(len), 0)) & 255,
                    StringUtils.replaceAll(matches.get(len), "\\d+", "").length()
            );
        }
        return str;
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws JSONException {
        List<ImageUrl> list = new LinkedList<>();
        String str = StringUtils.match("data:\\s*'(.*)?',", html, 1);
        if (str == null) {
            return list;
        }
        String nonce = StringUtils.match("<script>window.*?=(.*?)<\\/script>", html, 1);
        if (nonce == null) {
            throw new JSONException("nonce");
        }
        try {
            str = DecryptionUtils.base64Decrypt(decodeData(str, nonce));
        } catch (Exception e) {
            throw new JSONException(e.getMessage() != null ? e.getMessage() : "decode");
        }
        JSONObject object = new JSONObject(str);
        JSONArray array = object.optJSONArray("picture");
        if (array == null) {
            throw new JSONException("picture");
        }
        boolean preview = isPaywalled(object) || (array.length() <= 1 && hasPayFlag(object));
        for (int i = 0; i != array.length(); ++i) {
            String url = pictureUrl(array.getJSONObject(i));
            if (url == null || url.isEmpty()) {
                continue;
            }
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "000" + i);
            ImageUrl imageUrl = new ImageUrl(id, comicChapter, i + 1, url, false);
            imageUrl.setPreview(preview);
            list.add(imageUrl);
        }
        return list;
    }

    private String pictureUrl(JSONObject item) {
        String url = item.optString("url");
        if (url == null || url.isEmpty()) {
            url = item.optString("src");
        }
        if (url == null || url.isEmpty()) {
            url = item.optString("pic");
        }
        return url;
    }

    private boolean isPaywalled(JSONObject root) {
        return isDenied(root)
                || isDenied(root.optJSONObject("chapter"))
                || isDenied(root.optJSONObject("comic"));
    }

    private boolean isDenied(JSONObject obj) {
        if (obj == null) {
            return false;
        }
        if (obj.has("canRead") && !obj.optBoolean("canRead")) {
            return true;
        }
        if (obj.has("buy") && !obj.optBoolean("buy")) {
            return true;
        }
        if (obj.optInt("needPay", 0) == 1) {
            return true;
        }
        if (obj.optInt("payState", 0) > 1) {
            return true;
        }
        if (obj.optInt("payStatus", 0) > 1) {
            return true;
        }
        return false;
    }

    private boolean hasPayFlag(JSONObject root) {
        JSONObject chapter = root.optJSONObject("chapter");
        if (chapter == null) {
            chapter = root;
        }
        return chapter.has("payState")
                || chapter.has("payStatus")
                || chapter.has("needPay")
                || chapter.has("vip")
                || chapter.has("vipState")
                || root.has("payState");
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return new Node(html).text("div.book-detail > div.cont-list > dl:eq(2) > dd");
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new LinkedList<>();
        Node body = new Node(html);
        for (Node node : body.list("li > a")) {
            String cid = node.hrefWithSplit(1);
            String title = node.text("h3");
            String cover = node.attr("div > img", "data-src");
            String update = node.text("dl:eq(5) > dd");
            String author = node.text("dl:eq(2) > dd");
            list.add(new Comic(TYPE, cid, title, cover, update, author));
        }
        return list;
    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", mobileHost(), "User-Agent", MOBILE_UA);
    }

    private Request mobileRequest(String path) {
        Request request = sourceConfigManager.hostPathRequest(path, "https://m.ac.qq.com", "TENCENTM", "TENCENT");
        return request.newBuilder().header("User-Agent", MOBILE_UA).build();
    }

}
