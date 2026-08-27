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
import java.util.regex.Pattern;

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
            if (list.isEmpty()) {
                parseAmpImages(html, chapter, list);
            }
            for (ImageUrl image : list) {
                String[] urls = image.getUrls();
                if (urls == null || urls.length == 0) {
                    continue;
                }
                String[] rewritten = new String[urls.length];
                for (int i = 0; i < urls.length; i++) {
                    rewritten[i] = rewriteCdn(urls[i]);
                }
                image.setUrls(rewritten);
            }
        } catch (Exception e) {
            Log.e("BaoZiMH", "parseImages", e);
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
        int i = 0;
        for (Node node : nodes) {
            String url = firstImageUrl(node);
            url = rewriteCdn(url);
            if (url == null || url.isEmpty() || !seen.add(url)) {
                continue;
            }
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "000" + i);
            list.add(new ImageUrl(id, comicChapter, ++i, url, false));
        }
    }

    private String firstImageUrl(Node node) {
        String[] attrs = new String[]{"src", "data-src", "data-original", "data-echo"};
        for (String attr : attrs) {
            String value = node.attr(attr);
            if (value != null && !value.isEmpty() && !value.startsWith("data:")) {
                return value.trim();
            }
        }
        return null;
    }

    private String rewriteCdn(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        String def = sourceConfigManager.getRawField("BAOZIMH", "DEFAULT_TOP_DOMIAN", ".baozicdn.com");
        String current = sourceConfigManager.getRawField("BAOZIMH", "CURRENT_TOP_DOMIAN", ".baozicdn.com");
        if (def == null || def.isEmpty() || current == null || current.isEmpty() || def.equals(current)) {
            return url;
        }
        String[] parts = url.split(Pattern.quote(def), 2);
        if (parts.length < 2) {
            return url;
        }
        return parts[0] + current + parts[1];
    }
}
