package com.haleydu.cimoc.source;

import android.net.Uri;
import android.util.Pair;

import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.parser.MangaCategory;
import com.haleydu.cimoc.parser.MangaParser;
import com.haleydu.cimoc.parser.NodeIterator;
import com.haleydu.cimoc.parser.SearchIterator;
import com.haleydu.cimoc.parser.UrlFilter;
import com.haleydu.cimoc.soup.Node;
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Request;

public class GenericHtmlParser extends MangaParser {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final SourceConfig mConfig;
    private final int mType;

    public GenericHtmlParser(Source source, SourceConfig config) {
        mConfig = config;
        mType = source == null ? config.type : source.getType();
        init(source == null ? config.toSource(true) : source, config.hasCategory() ? new Category() : null);
        mTitle = config.title;
    }

    @Override
    protected void initUrlFilterList() {
        try {
            String host = Uri.parse(mConfig.baseUrl).getHost();
            if (host != null) {
                filter.add(new UrlFilter(host));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws Exception {
        String encoded = URLEncoder.encode(keyword, "UTF-8");
        String path = mConfig.search == null ? "" : mConfig.search;
        if (mConfig.searchKey != null && !mConfig.searchKey.isEmpty()) {
            if (page != 1) {
                return null;
            }
            FormBody body = new FormBody.Builder().add(mConfig.searchKey, keyword).build();
            return requestBuilder(absUrl(path)).post(body).build();
        }
        String url;
        if (path.contains("%s") && path.contains("%d")) {
            url = mConfig.baseUrl + StringUtils.format(path, encoded, page);
        } else if (path.contains("%s")) {
            if (page != 1) {
                return null;
            }
            url = mConfig.baseUrl + StringUtils.format(path, encoded);
        } else {
            if (page != 1) {
                return null;
            }
            url = mConfig.baseUrl + path + encoded;
        }
        return requestBuilder(url).build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        final int type = mType;
        final SourceConfig config = mConfig;
        List<Node> nodes;
        try {
            nodes = body.list(config.searchInfoList);
        } catch (Exception e) {
            nodes = new LinkedList<>();
        }
        return new NodeIterator(nodes) {
            @Override
            protected Comic parse(Node node) {
                String cid = extractPath(attrOrHref(node, config.searchInfoCid));
                if (cid == null || cid.isEmpty()) {
                    return null;
                }
                String title = textOrAttr(node, config.searchInfoTitle);
                String cover = coverOf(node, config.searchInfoCover, null);
                String author = textOrAttr(node, config.searchInfoAuthor);
                String update = textOrAttr(node, config.searchInfoUpdate);
                return new Comic(type, cid, title, cover, update, author);
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return absUrl(cid);
    }

    @Override
    public Request getInfoRequest(String cid) {
        return requestBuilder(absUrl(cid)).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        Node body = new Node(html);
        String title = firstText(body, mConfig.parseInfoTitle);
        String cover = coverOf(body, mConfig.parseInfoCover, html);
        String intro = firstText(body, mConfig.parseInfoIntro);
        String author = firstText(body, mConfig.parseInfoAuthor);
        String update = firstText(body, mConfig.parseInfoUpdate);
        boolean finish = isFinish(firstText(body, mConfig.parseInfoStatus));
        comic.setInfo(emptyTo(title, comic.getTitle()),
                emptyTo(cover, comic.getCover()),
                update, intro, author, finish);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        int index = 0;
        index = addChapters(list, body, mConfig.parseChapterList1, sourceComic, index);
        index = addChapters(list, body, mConfig.parseChapterList2, sourceComic, index);
        addChapters(list, body, mConfig.parseChapterList3, sourceComic, index);
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        return requestBuilder(absUrl(path)).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        if (isRegex(mConfig.parseImageList)) {
            parseRegexImages(html, chapter, list);
        } else {
            parseCssImages(html, chapter, list);
        }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return firstText(new Node(html), mConfig.parseInfoUpdate);
    }

    @Override
    public Headers getHeader() {
        return Headers.of(
                "Referer", mConfig.baseUrl + "/",
                "User-Agent", USER_AGENT
        );
    }

    private Request.Builder requestBuilder(String url) {
        Request.Builder builder = new Request.Builder().url(url);
        Headers headers = getHeader();
        for (int i = 0; i < headers.size(); i++) {
            builder.header(headers.name(i), headers.value(i));
        }
        return builder;
    }

    @Override
    public Request getCategoryRequest(String format, int page) {
        String url = StringUtils.format(format, page);
        return requestBuilder(url).build();
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new LinkedList<>();
        if (html == null) {
            return list;
        }
        String trim = html.trim();
        if (trim.startsWith("{") || trim.startsWith("[")) {
            return parseJsonCategory(trim);
        }
        Node body = new Node(html);
        String selector = mConfig.parseCategoryInfoList;
        if (selector == null || selector.isEmpty() || isJsonKey(selector)) {
            selector = mConfig.searchInfoList;
        }
        List<Node> nodes;
        try {
            nodes = body.list(selector);
        } catch (Exception e) {
            return list;
        }
        for (Node node : nodes) {
            Comic comic = parseCategoryNode(node);
            if (comic != null) {
                list.add(comic);
            }
        }
        return list;
    }

    private int addChapters(List<Chapter> list, Node body, String selector, Long sourceComic, int index) {
        if (selector == null || selector.isEmpty()) {
            return index;
        }
        List<Node> nodes;
        try {
            nodes = body.list(selector);
        } catch (Exception e) {
            return index;
        }
        for (Node node : nodes) {
            String title = mConfig.parseChapterTitle.isEmpty()
                    ? node.text()
                    : textOrAttr(node, mConfig.parseChapterTitle);
            String href = mConfig.parseChapterPath.isEmpty()
                    ? firstHref(node)
                    : attrOrHref(node, mConfig.parseChapterPath);
            String path = extractPath(href);
            if (path == null || path.isEmpty()) {
                continue;
            }
            if (title == null || title.isEmpty()) {
                title = path;
            }
            list.add(new Chapter(Long.parseLong(sourceComic + "000" + index), sourceComic, title, path));
            index++;
        }
        return index;
    }

    private void parseCssImages(String html, Chapter chapter, List<ImageUrl> list) {
        String selector = mConfig.parseImageList;
        if (selector == null || selector.isEmpty()) {
            selector = mConfig.parseImageUrl;
        }
        if (selector == null || selector.isEmpty()) {
            return;
        }
        int i = 0;
        List<Node> nodes;
        try {
            nodes = new Node(html).list(selector);
        } catch (Exception e) {
            return;
        }
        for (Node node : nodes) {
            String url = imageAttr(node, mConfig.parseImageUrl);
            url = absImage(url);
            if (url == null || isFault(url)) {
                continue;
            }
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "000" + i);
            list.add(new ImageUrl(id, comicChapter, ++i, url, false));
        }
    }

    private void parseRegexImages(String html, Chapter chapter, List<ImageUrl> list) {
        Matcher matcher = Pattern.compile(mConfig.parseImageList).matcher(html);
        int i = 0;
        while (matcher.find()) {
            String captured = matcher.groupCount() >= 1 ? matcher.group(1) : matcher.group();
            if (captured == null) {
                continue;
            }
            captured = captured.trim();
            if (captured.startsWith("[") || captured.startsWith("{")) {
                try {
                    JSONArray array = captured.startsWith("[") ? new JSONArray(captured) : new JSONArray("[" + captured + "]");
                    for (int n = 0; n < array.length(); n++) {
                        String url = absImage(array.optString(n));
                        if (url == null || isFault(url)) {
                            continue;
                        }
                        Long comicChapter = chapter.getId();
                        Long id = Long.parseLong(comicChapter + "000" + i);
                        list.add(new ImageUrl(id, comicChapter, ++i, url, false));
                    }
                    return;
                } catch (Exception ignored) {
                }
            }
            String[] parts = captured.split(",|;");
            for (String part : parts) {
                String url = absImage(part.replace("\"", "").replace("'", "").trim());
                if (url == null || isFault(url)) {
                    continue;
                }
                Long comicChapter = chapter.getId();
                Long id = Long.parseLong(comicChapter + "000" + i);
                list.add(new ImageUrl(id, comicChapter, ++i, url, false));
            }
        }
    }

    private boolean isRegex(String value) {
        return value != null && (value.contains("(.*") || value.contains("(.+") || value.contains("=\\") || value.contains("='"));
    }

    private String absUrl(String path) {
        if (path == null || path.isEmpty()) {
            return mConfig.baseUrl;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (path.startsWith("//")) {
            return "https:" + path;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return mConfig.baseUrl + path;
    }

    private String absImage(String url) {
        if (url == null) {
            return null;
        }
        url = url.trim();
        if (url.isEmpty()) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        if (url.startsWith("/")) {
            return mConfig.baseUrl + url;
        }
        if (mConfig.imageServerUrl != null && !mConfig.imageServerUrl.isEmpty()) {
            String server = mConfig.imageServerUrl;
            if (!server.endsWith("/") && !url.startsWith("/")) {
                return server + "/" + url;
            }
            return server + url;
        }
        return mConfig.baseUrl + "/" + url;
    }

    private String extractPath(String href) {
        if (href == null) {
            return null;
        }
        href = href.trim();
        if (href.isEmpty() || href.startsWith("javascript:")) {
            return null;
        }
        if (href.startsWith(mConfig.baseUrl)) {
            href = href.substring(mConfig.baseUrl.length());
        }
        int hash = href.indexOf('#');
        if (hash >= 0) {
            href = href.substring(0, hash);
        }
        if (href.isEmpty()) {
            return null;
        }
        return href;
    }

    private String attrOrHref(Node node, String selector) {
        if (selector == null || selector.isEmpty()) {
            return firstHref(node);
        }
        String href = node.href(selector);
        if (href == null || href.isEmpty()) {
            href = node.attr(selector, "href");
        }
        if (href == null || href.isEmpty()) {
            href = node.attr("href");
        }
        return href;
    }

    private String firstHref(Node node) {
        String href = node.href("a");
        if (href == null || href.isEmpty()) {
            href = node.href();
        }
        return href;
    }

    private String textOrAttr(Node node, String selector) {
        if (selector == null || selector.isEmpty()) {
            return node.text();
        }
        String text = node.text(selector);
        if (text == null || text.isEmpty()) {
            text = node.attr(selector, "title");
        }
        return text;
    }

    private String firstText(Node node, String selector) {
        if (selector == null || selector.isEmpty()) {
            return "";
        }
        String text = node.text(selector);
        return text == null ? "" : text;
    }

    private String coverOf(Node node, String selector, String html) {
        if (isRegex(selector)) {
            String found = regexFirst(html != null ? html : outerHtml(node), selector);
            if (found != null && !found.isEmpty()) {
                return absImage(found);
            }
        }
        if (selector == null || selector.isEmpty()) {
            return absImage(imageAttr(node, ""));
        }
        Node child = node;
        try {
            if (node.get() != null && node.get().select(selector).first() != null) {
                child = new Node(node.get().select(selector).first());
            }
        } catch (Exception ignored) {
        }
        return absImage(imageAttr(child, ""));
    }

    private String regexFirst(String html, String pattern) {
        if (html == null || pattern == null || pattern.isEmpty()) {
            return null;
        }
        try {
            Matcher matcher = Pattern.compile(pattern).matcher(html);
            if (matcher.find()) {
                return matcher.groupCount() >= 1 ? matcher.group(1) : matcher.group();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String outerHtml(Node node) {
        try {
            return node.get() == null ? "" : node.get().outerHtml();
        } catch (Exception e) {
            return "";
        }
    }

    private String imageAttr(Node node, String attrHint) {
        String[] attrs;
        if (attrHint != null && !attrHint.isEmpty() && !attrHint.contains(" ") && !attrHint.contains(">") && !attrHint.contains(".")) {
            attrs = new String[]{attrHint, "data-original", "data-src", "data-echo", "src"};
        } else {
            attrs = new String[]{"data-original", "data-src", "data-echo", "src"};
        }
        for (String attr : attrs) {
            String value = node.attr(attr);
            if (value != null && !value.isEmpty() && !value.startsWith("data:")) {
                return value;
            }
        }
        return node.src();
    }

    private boolean isFault(String url) {
        return mConfig.parseImageFaultImage != null
                && !mConfig.parseImageFaultImage.isEmpty()
                && url.contains(mConfig.parseImageFaultImage);
    }

    private String emptyTo(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private Comic parseCategoryNode(Node node) {
        String cid = extractPath(attrOrHref(node, mConfig.parseCategoryInfoCid));
        if ((cid == null || cid.isEmpty()) && !mConfig.parseCategoryInfoCidPre.isEmpty()) {
            String text = textOrAttr(node, mConfig.parseCategoryInfoCid);
            if (text != null && !text.isEmpty()) {
                cid = mConfig.parseCategoryInfoCidPre + text;
            }
        }
        if (cid == null || cid.isEmpty()) {
            return null;
        }
        if (!mConfig.parseCategoryInfoCidPre.isEmpty() && !cid.startsWith("http") && !cid.startsWith(mConfig.parseCategoryInfoCidPre)) {
            cid = mConfig.parseCategoryInfoCidPre + cid;
        }
        String title = textOrAttr(node, mConfig.parseCategoryInfoTitle);
        String cover = coverOf(node, mConfig.parseCategoryInfoCover, null);
        String author = textOrAttr(node, mConfig.parseCategoryInfoAuthor);
        String update = textOrAttr(node, mConfig.parseCategoryInfoUpdate);
        return new Comic(mType, cid, title, cover, update, author);
    }

    private List<Comic> parseJsonCategory(String html) {
        List<Comic> list = new LinkedList<>();
        try {
            JSONObject root = html.startsWith("[") ? new JSONObject().put("list", new JSONArray(html)) : new JSONObject(html);
            String key = isJsonKey(mConfig.parseCategoryInfoList) ? mConfig.parseCategoryInfoList : "list";
            JSONArray array = findJsonArray(root, key);
            if (array == null) {
                array = findJsonArray(root, "books");
            }
            if (array == null) {
                array = findJsonArray(root, "list");
            }
            if (array == null) {
                return list;
            }
            String cidKey = isJsonKey(mConfig.parseCategoryInfoCid) ? mConfig.parseCategoryInfoCid : "id";
            String titleKey = isJsonKey(mConfig.parseCategoryInfoTitle) ? mConfig.parseCategoryInfoTitle : "name";
            String coverKey = isJsonKey(mConfig.parseCategoryInfoCover) ? mConfig.parseCategoryInfoCover : "cover";
            String authorKey = isJsonKey(mConfig.parseCategoryInfoAuthor) ? mConfig.parseCategoryInfoAuthor : "author";
            String updateKey = isJsonKey(mConfig.parseCategoryInfoUpdate) ? mConfig.parseCategoryInfoUpdate : "update";
            String pre = mConfig.parseCategoryInfoCidPre;
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                String cid = object.optString(cidKey);
                if (cid == null || cid.isEmpty()) {
                    continue;
                }
                if (!pre.isEmpty() && !cid.startsWith("http") && !cid.startsWith(pre)) {
                    cid = pre + cid;
                }
                String title = object.optString(titleKey);
                String cover = absImage(object.optString(coverKey));
                String author = object.optString(authorKey);
                String update = object.optString(updateKey);
                list.add(new Comic(mType, cid, title, cover, update, author));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private JSONArray findJsonArray(JSONObject root, String key) {
        if (root == null || key == null || key.isEmpty()) {
            return null;
        }
        JSONArray direct = root.optJSONArray(key);
        if (direct != null) {
            return direct;
        }
        JSONObject data = root.optJSONObject("data");
        if (data != null) {
            JSONArray nested = data.optJSONArray(key);
            if (nested != null) {
                return nested;
            }
        }
        JSONObject results = root.optJSONObject("results");
        if (results != null) {
            return results.optJSONArray(key);
        }
        return null;
    }

    private boolean isJsonKey(String value) {
        return value != null && value.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    private class Category extends MangaCategory {

        @Override
        public boolean isComposite() {
            return true;
        }

        @Override
        public String getFormat(String... args) {
            String path = mConfig.parseCategoryPath;
            path = path.replace("page=%s", "page=%d");
            path = path.replace("page/%s", "page/%d");
            path = fillPercentS(path, args);
            if (!path.contains("%d")) {
                path = path + (path.contains("?") ? "&page=%d" : "?page=%d");
            }
            if (path.startsWith("http://") || path.startsWith("https://")) {
                return path;
            }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            return mConfig.baseUrl + path;
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", "all"));
            return list;
        }

        @Override
        protected boolean hasArea() {
            return countPercentS(mConfig.parseCategoryPath) >= 2;
        }

        @Override
        protected List<Pair<String, String>> getArea() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", "all"));
            return list;
        }

        @Override
        protected boolean hasProgress() {
            return countPercentS(mConfig.parseCategoryPath) >= 3;
        }

        @Override
        protected List<Pair<String, String>> getProgress() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", "all"));
            return list;
        }

        @Override
        protected boolean hasOrder() {
            return countPercentS(mConfig.parseCategoryPath) >= 4;
        }

        @Override
        protected List<Pair<String, String>> getOrder() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", "all"));
            return list;
        }

        private int countPercentS(String path) {
            if (path == null) {
                return 0;
            }
            String normalized = path.replace("page=%s", "page=%d").replace("page/%s", "page/%d");
            int count = 0;
            for (int i = 0; i < normalized.length() - 1; i++) {
                if (normalized.charAt(i) == '%' && normalized.charAt(i + 1) == 's') {
                    count++;
                }
            }
            return count;
        }

        private String fillPercentS(String path, String... args) {
            String[] ordered = new String[]{
                    argAt(args, CATEGORY_SUBJECT),
                    argAt(args, CATEGORY_AREA),
                    argAt(args, CATEGORY_READER),
                    argAt(args, CATEGORY_YEAR),
                    argAt(args, CATEGORY_PROGRESS),
                    argAt(args, CATEGORY_ORDER)
            };
            StringBuilder builder = new StringBuilder();
            int argIndex = 0;
            int i = 0;
            while (i < path.length()) {
                if (i + 1 < path.length() && path.charAt(i) == '%' && path.charAt(i + 1) == 's') {
                    String value = "all";
                    while (argIndex < ordered.length) {
                        String candidate = ordered[argIndex++];
                        if (candidate != null && !candidate.isEmpty()) {
                            value = candidate;
                            break;
                        }
                    }
                    builder.append(value);
                    i += 2;
                } else {
                    builder.append(path.charAt(i));
                    i++;
                }
            }
            return builder.toString();
        }

        private String argAt(String[] args, int index) {
            if (args == null || index >= args.length) {
                return null;
            }
            return args[index];
        }
    }
}
