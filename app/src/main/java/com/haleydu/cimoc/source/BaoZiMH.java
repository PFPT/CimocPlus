package com.haleydu.cimoc.source;

import android.util.Log;

import com.haleydu.cimoc.data.SourceConfigManager;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.soup.Node;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

public class BaoZiMH extends GenericHtmlParser {

    public static final int TYPE = 200;
    public static final String DEFAULT_TITLE = "包子漫画";

    private final SourceConfigManager sourceConfigManager;

    public BaoZiMH(Source source, SourceConfig config, SourceConfigManager sourceConfigManager) {
        super(source, config);
        this.sourceConfigManager = sourceConfigManager;
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
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
            String url = rewriteCdn(firstImageUrl(node));
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
            String[] rewritten = new String[urls.length];
            for (int i = 0; i < urls.length; i++) {
                rewritten[i] = rewriteCdn(urls[i]);
            }
            image.setUrls(rewritten);
        } catch (Exception ignored) {
        }
    }

    private String firstImageUrl(Node node) {
        String[] attrs = new String[]{"src", "data-src", "data-original", "data-echo", "srcset"};
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
            String def = sourceConfigManager.getRawField("BAOZIMH", "DEFAULT_TOP_DOMIAN", ".baozicdn.com");
            String current = sourceConfigManager.getRawField("BAOZIMH", "CURRENT_TOP_DOMIAN", ".baozicdn.com");
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
