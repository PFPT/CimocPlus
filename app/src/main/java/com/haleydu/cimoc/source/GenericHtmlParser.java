package com.haleydu.cimoc.source;

import android.net.Uri;

import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.parser.MangaParser;
import com.haleydu.cimoc.parser.NodeIterator;
import com.haleydu.cimoc.parser.SearchIterator;
import com.haleydu.cimoc.parser.UrlFilter;
import com.haleydu.cimoc.soup.Node;
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;

import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;

public class GenericHtmlParser extends MangaParser {

    private final SourceConfig mConfig;
    private final int mType;

    public GenericHtmlParser(Source source, SourceConfig config) {
        mConfig = config;
        mType = source == null ? config.type : source.getType();
        init(source == null ? config.toSource(true) : source, null);
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
        String path = mConfig.search;
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
        return new Request.Builder().url(url).build();
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
        return new Request.Builder().url(absUrl(cid)).build();
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
        return new Request.Builder().url(absUrl(path)).build();
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
        return Headers.of("Referer", mConfig.baseUrl + "/");
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
}
