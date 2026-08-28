package com.haleydu.cimoc.source;

import android.util.Log;

import com.haleydu.cimoc.data.SourceConfigManager;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.parser.NodeIterator;
import com.haleydu.cimoc.parser.SearchIterator;
import com.haleydu.cimoc.soup.Node;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;

public class BaoZiMH extends GenericHtmlParser {

    public static final int TYPE = 200;
    public static final String DEFAULT_TITLE = "包子漫画";
    private static final String REFERER = "https://www.baozimh.com/";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String ACCEPT = "image/webp,image/apng,image/*,*/*;q=0.8";
    private static final String DEFAULT_CDN = ".baozicdn.com";

    private final SourceConfigManager sourceConfigManager;

    public BaoZiMH(Source source, SourceConfig config, SourceConfigManager sourceConfigManager) {
        super(source, config);
        this.sourceConfigManager = sourceConfigManager;
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        List<Node> cards;
        try {
            cards = new Node(html).list("div.comics-card");
        } catch (Exception e) {
            cards = null;
        }
        if (cards == null || cards.isEmpty()) {
            return super.getSearchIterator(html, page);
        }
        return new NodeIterator(cards) {
            @Override
            protected Comic parse(Node node) {
                String cid = comicPath(node);
                if (cid == null || cid.isEmpty()) {
                    return null;
                }
                String title = node.attr("a.comics-card__poster", "title");
                if (title == null || title.isEmpty()) {
                    title = node.text("a.comics-card__title, .comics-card__title, small");
                }
                String cover = cardCover(node);
                String author = node.text("small");
                return new Comic(TYPE, cid, title, cover, null, author);
            }
        };
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        Comic parsed = super.parseInfo(html, comic);
        Node body = new Node(html);
        String title = parsed.getTitle();
        if (title == null || title.isEmpty()) {
            title = body.text("h1.comics-detail__title, .comics-detail__title");
        }
        String cover = parsed.getCover();
        if (cover == null || cover.isEmpty() || (!cover.startsWith("http://") && !cover.startsWith("https://"))) {
            String ampCover = ampCover(body);
            if (ampCover != null && !ampCover.isEmpty()) {
                cover = ampCover;
            }
        } else {
            cover = absImage(cover);
        }
        String intro = parsed.getIntro();
        if (intro == null || intro.isEmpty()) {
            intro = body.text("p.comics-detail__desc, div.comics-detail__info > p");
        }
        String author = parsed.getAuthor();
        if (author == null || author.isEmpty()) {
            author = body.text("h2.comics-detail__author, .comics-detail__author");
        }
        boolean finish = parsed.getFinish() != null && parsed.getFinish();
        parsed.setInfo(title, cover, parsed.getUpdate(), intro, author, finish);
        return parsed;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list;
        try {
            list = super.parseChapter(html, comic, sourceComic);
        } catch (Exception e) {
            list = null;
        }
        if (list != null && !list.isEmpty()) {
            return list;
        }
        list = new LinkedList<>();
        if (sourceComic == null) {
            return list;
        }
        List<Node> nodes;
        try {
            nodes = new Node(html).list(
                    "div.comics-chapters a, div#chapter-items a, div#chapters_other_list a, a.comics-chapters"
            );
        } catch (Exception e) {
            return list;
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (Node node : nodes) {
            String href = node.href();
            if (href == null || href.isEmpty()) {
                href = node.href("a");
            }
            String path = extractPath(href);
            if (path == null || path.isEmpty() || !seen.add(path)) {
                continue;
            }
            String title = node.text("span");
            if (title == null || title.isEmpty()) {
                title = node.text();
            }
            if (title == null || title.isEmpty()) {
                title = path;
            }
            try {
                list.add(new Chapter(Long.parseLong(sourceComic + "000" + index), sourceComic, title, path));
                index++;
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        try {
            List<ImageUrl> parsed = super.parseImages(html, chapter);
            if (parsed != null) {
                list.addAll(parsed);
            }
        } catch (Exception e) {
            Log.e("BaoZiMH", "parseImages", e);
        }
        if (list.isEmpty()) {
            parseAmpImages(html, chapter, list);
        }
        for (ImageUrl image : list) {
            rewriteImage(image);
        }
        return list;
    }

    @Override
    public Headers getHeader() {
        return Headers.of(
                "Referer", REFERER,
                "User-Agent", USER_AGENT,
                "Accept", ACCEPT
        );
    }

    @Override
    public Headers getHeader(String url) {
        return getHeader();
    }

    @Override
    public Headers getHeader(List<ImageUrl> list) {
        return getHeader();
    }

    private String comicPath(Node node) {
        String path = extractPath(node.href("a.comics-card__poster"));
        if (isComicPath(path)) {
            return path;
        }
        try {
            List<Node> links = node.list("a");
            for (Node link : links) {
                path = extractPath(link.href());
                if (isComicPath(path)) {
                    return path;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isComicPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String lower = path.toLowerCase();
        return lower.contains("/comic/") || lower.contains("/manga/");
    }

    private String cardCover(Node node) {
        try {
            List<Node> imgs = node.list("amp-img, img");
            for (Node img : imgs) {
                String url = absImage(firstImageUrl(img));
                if (url != null && !url.isEmpty()) {
                    return url;
                }
            }
        } catch (Exception ignored) {
        }
        return absImage(firstImageUrl(node));
    }

    private String ampCover(Node body) {
        try {
            List<Node> imgs = body.list(
                    "div.comics-detail amp-img, div.l-content amp-img, .comics-detail__poster amp-img, amp-img"
            );
            for (Node img : imgs) {
                String url = absImage(firstImageUrl(img));
                if (url != null && !url.isEmpty()) {
                    return url;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void parseAmpImages(String html, Chapter chapter, List<ImageUrl> list) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<Node> nodes;
        try {
            nodes = new Node(html).list(
                    ".comic-contain amp-img, ul.comic-contain li amp-img, .comic-contain img, amp-img.comic-contain__item"
            );
        } catch (Exception e) {
            return;
        }
        Long comicChapter = chapter == null ? null : chapter.getId();
        if (comicChapter == null) {
            return;
        }
        int i = 0;
        for (Node node : nodes) {
            String url = absImage(firstImageUrl(node));
            if (url == null || url.isEmpty() || !seen.add(url)) {
                continue;
            }
            Long id = Long.parseLong(comicChapter + "000" + i);
            list.add(new ImageUrl(id, comicChapter, ++i, url, false));
        }
    }

    private void rewriteImage(ImageUrl image) {
        try {
            String[] urls = image.getUrls();
            if (urls == null || urls.length == 0) {
                return;
            }
            LinkedHashSet<String> all = new LinkedHashSet<>();
            for (String url : urls) {
                for (String variant : fallbackUrls(url)) {
                    if (variant != null && !variant.isEmpty()) {
                        all.add(variant);
                    }
                }
            }
            if (!all.isEmpty()) {
                image.setUrls(all.toArray(new String[0]));
            }
        } catch (Exception ignored) {
        }
    }

    private String[] fallbackUrls(String url) {
        url = absImage(url);
        if (url == null || url.isEmpty()) {
            return new String[0];
        }
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        String rewritten = rewriteCdn(url);
        if (rewritten != null && !rewritten.isEmpty()) {
            variants.add(rewritten);
        }
        variants.add(url);
        String def = defaultCdn();
        if (url.contains(def)) {
            for (String suffix : cdnSuffixes()) {
                int index = url.indexOf(def);
                if (index < 0) {
                    continue;
                }
                variants.add(url.substring(0, index) + suffix + url.substring(index + def.length()));
            }
        }
        return variants.toArray(new String[0]);
    }

    private List<String> cdnSuffixes() {
        List<String> suffixes = new ArrayList<>();
        String raw = sourceConfigManager.getRawField("BAOZIMH", "REQ_DOMAINS", "");
        try {
            JSONArray array = new JSONArray(raw.replace('\'', '"'));
            for (int i = 0; i < array.length(); i++) {
                JSONArray item = array.optJSONArray(i);
                if (item == null || item.length() == 0) {
                    continue;
                }
                String suffix = item.optString(0);
                if (suffix != null && !suffix.isEmpty()) {
                    suffixes.add(suffix);
                }
            }
        } catch (Exception ignored) {
        }
        String def = defaultCdn();
        if (!suffixes.contains(def)) {
            suffixes.add(def);
        }
        return suffixes;
    }

    private String defaultCdn() {
        String def = sourceConfigManager.getRawField("BAOZIMH", "DEFAULT_TOP_DOMIAN", DEFAULT_CDN);
        if (def == null || def.isEmpty()) {
            return DEFAULT_CDN;
        }
        return def;
    }

    private String firstImageUrl(Node node) {
        String[] attrs = new String[]{"srcset", "data-src", "data-original", "data-echo", "src"};
        for (String attr : attrs) {
            String value = node.attr(attr);
            if (value == null || value.isEmpty() || value.startsWith("data:")) {
                continue;
            }
            value = value.trim();
            if ("srcset".equals(attr)) {
                value = firstSrcsetUrl(value);
                if (value == null || value.isEmpty()) {
                    continue;
                }
            }
            return value;
        }
        return null;
    }

    private String firstSrcsetUrl(String srcset) {
        String[] candidates = srcset.split(",");
        if (candidates.length == 0) {
            return null;
        }
        String first = candidates[0].trim();
        int space = first.indexOf(' ');
        if (space > 0) {
            first = first.substring(0, space);
        }
        return first;
    }

    private String rewriteCdn(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        try {
            String def = defaultCdn();
            String current = sourceConfigManager.getRawField("BAOZIMH", "CURRENT_TOP_DOMIAN", DEFAULT_CDN);
            if (def == null || def.isEmpty() || current == null || current.isEmpty() || def.equals(current)) {
                return url;
            }
            int index = url.indexOf(def);
            if (index < 0) {
                return url;
            }
            return url.substring(0, index) + current + url.substring(index + def.length());
        } catch (Exception e) {
            return url;
        }
    }
}
