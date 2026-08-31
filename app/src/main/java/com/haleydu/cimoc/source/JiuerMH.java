package com.haleydu.cimoc.source;

import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;

import java.util.LinkedList;
import java.util.List;

public class JiuerMH extends GenericHtmlParser {

    public static final int TYPE = 233;

    public JiuerMH(Source source, SourceConfig config) {
        super(source, config);
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        Long comicChapter = chapter == null ? null : chapter.getId();
        if (html == null || comicChapter == null) {
            return list;
        }
        String raw = StringUtils.match("chapterImages\\s*=\\s*(\\[[\\s\\S]*?\\])", html, 1);
        if (raw == null || raw.isEmpty()) {
            return super.parseImages(html, chapter);
        }
        try {
            JSONArray array = new JSONArray(raw);
            String server = StringUtils.match("getCih\\(\\)\\s*\\{\\s*return\\s*\"([^\"]+)\"", html, 1);
            if (server == null || server.isEmpty()) {
                server = mConfig.imageServerUrl;
            }
            if (server == null) {
                server = "";
            }
            if (server.endsWith("/")) {
                server = server.substring(0, server.length() - 1);
            }
            String chapterPath = StringUtils.match("chapterPath\\s*=\\s*\"([^\"]*)\"", html, 1);
            if (chapterPath == null) {
                chapterPath = "";
            }
            for (int i = 0; i < array.length(); i++) {
                String path = array.optString(i).trim();
                if (path.isEmpty()) {
                    continue;
                }
                String url;
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    url = path;
                } else {
                    if (!path.startsWith("/")) {
                        String prefix = chapterPath;
                        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
                            prefix = prefix + "/";
                        }
                        path = prefix + path;
                        if (!path.startsWith("/")) {
                            path = "/" + path;
                        }
                    }
                    url = server + path;
                }
                Long id = Long.parseLong(comicChapter + "000" + i);
                list.add(new ImageUrl(id, comicChapter, i + 1, url, false));
            }
        } catch (Exception ignored) {
        }
        if (list.isEmpty()) {
            return super.parseImages(html, chapter);
        }
        return list;
    }
}
