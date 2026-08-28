package com.haleydu.cimoc.source;

import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

public class LaiMH extends GenericHtmlParser {

    public static final int TYPE = 225;
    private static final String[] IMAGE_HOSTS = {
            "https://mhpic5eer.tgmhfc.uk",
            "https://mhpic7899-5.tgmhfc.uk",
            "https://mhpic7ffr.tgmhfc.uk",
            "https://mhpicwwt.tgmhfc.uk",
            "https://mhpicwwx.tgmhfc.uk"
    };

    public LaiMH(Source source, SourceConfig config) {
        super(source, config);
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        Long comicChapter = chapter == null ? null : chapter.getId();
        if (html == null || comicChapter == null) {
            return list;
        }
        String json = StringUtils.match("var mhInfo=(\\{.*?\\});", html, 1);
        if (json == null) {
            return super.parseImages(html, chapter);
        }
        try {
            JSONObject object = new JSONObject(json);
            JSONArray images = object.optJSONArray("images");
            if (images == null || images.length() == 0) {
                return list;
            }
            String path = encodePath(object.optString("path", ""));
            String host = object.optString("host", "").trim();
            if (host.isEmpty()) {
                host = mConfig.imageServerUrl;
            }
            if (host == null || host.isEmpty()) {
                host = IMAGE_HOSTS[0];
            }
            if (!host.startsWith("http")) {
                host = "https://" + host;
            }
            host = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
            if (!path.isEmpty() && !path.startsWith("/")) {
                path = "/" + path;
            }
            if (!path.endsWith("/") && !path.isEmpty()) {
                path = path + "/";
            }
            int index = 0;
            for (int i = 0; i < images.length(); i++) {
                String name = images.optString(i, "").trim();
                if (name.isEmpty()) {
                    continue;
                }
                String url = name.startsWith("http") ? name : host + path + name;
                Long id = Long.parseLong(comicChapter + "000" + index);
                list.add(new ImageUrl(id, comicChapter, ++index, url, false));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private static String encodePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String[] parts = path.split("/", -1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append('/');
            }
            if (parts[i].isEmpty()) {
                continue;
            }
            try {
                builder.append(URLEncoder.encode(parts[i], "UTF-8").replace("+", "%20"));
            } catch (Exception e) {
                builder.append(parts[i]);
            }
        }
        return builder.toString();
    }
}
