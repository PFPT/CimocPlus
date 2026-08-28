package com.haleydu.cimoc.source;

import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.soup.Node;
import com.haleydu.cimoc.utils.DecryptionUtils;
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class YYManhua extends GenericHtmlParser {

    public static final int TYPE = 224;

    public YYManhua(Source source, SourceConfig config) {
        super(source, config);
    }

    private String imageHost() {
        if (mConfig.imageServerUrl != null && !mConfig.imageServerUrl.isEmpty()) {
            return mConfig.imageServerUrl;
        }
        return "https://i.hamreus.com";
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        String baseText = body.id("__VIEWSTATE").attr("value");
        if (!StringUtils.isEmpty(baseText)) {
            body = new Node(DecryptionUtils.LZ64Decrypt(baseText));
        }
        int i = 0;
        for (Node node : body.list("div.chapter-list")) {
            List<Node> uls = node.list("ul");
            Collections.reverse(uls);
            for (Node ul : uls) {
                for (Node li : ul.list("li > a")) {
                    String title = li.attr("title");
                    if (title == null || title.isEmpty()) {
                        title = li.text();
                    }
                    String href = li.href();
                    String path = extractPath(href);
                    if (path == null || path.isEmpty()) {
                        path = li.hrefWithSplit(2);
                    }
                    if (path == null || path.isEmpty()) {
                        continue;
                    }
                    try {
                        list.add(new Chapter(Long.parseLong(sourceComic + "000" + i), sourceComic, title, path));
                        i++;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (list.isEmpty()) {
            return super.parseChapter(html, comic, sourceComic);
        }
        return list;
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        Long comicChapter = chapter == null ? null : chapter.getId();
        if (html == null || comicChapter == null) {
            return list;
        }
        String packed = StringUtils.match("\\(function\\(p,a,c,k,e,d\\).*?0,\\{\\}\\)\\)", html, 0);
        if (packed == null) {
            return super.parseImages(html, chapter);
        }
        try {
            String replaceable = StringUtils.split(packed, ",", -3);
            String fake = StringUtils.split(replaceable, "'", 1);
            String real = DecryptionUtils.LZ64Decrypt(fake);
            packed = packed.replace(replaceable, StringUtils.format("'%s'.split('|')", real));
            String result = DecryptionUtils.evalDecrypt(packed);
            String jsonString = result.substring(12, result.length() - 12);
            JSONObject object = new JSONObject(jsonString);
            String path = object.getString("path");
            String e = object.getJSONObject("sl").getString("e");
            String m = object.getJSONObject("sl").getString("m");
            JSONArray array = object.getJSONArray("files");
            for (int i = 0; i != array.length(); ++i) {
                Long id = Long.parseLong(comicChapter + "000" + i);
                String url = StringUtils.format("%s%s%s?e=%s&m=%s", imageHost(), path, array.getString(i), e, m);
                list.add(new ImageUrl(id, comicChapter, i + 1, url, false));
            }
        } catch (Exception ignored) {
        }
        return list;
    }
}
