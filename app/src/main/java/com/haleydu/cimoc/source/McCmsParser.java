package com.haleydu.cimoc.source;

import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.utils.DecryptionUtils;
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class McCmsParser extends GenericHtmlParser {

    public static final int TYPE_DUMANWUORG = 220;
    public static final int TYPE_TTKANMANHUA = 221;
    public static final int TYPE_HAODUOMAN = 222;
    public static final int TYPE_MANHUAYU = 223;

    private static final String[] KEYS = {
            "5V&RoR%Jf@pJPydF",
            "9S8$vJnU2ANeSRoF"
    };

    public McCmsParser(Source source, SourceConfig config) {
        super(source, config);
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> decrypted = decryptParams(html, chapter);
        if (!decrypted.isEmpty()) {
            return decrypted;
        }
        return super.parseImages(html, chapter);
    }

    private List<ImageUrl> decryptParams(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        Long comicChapter = chapter == null ? null : chapter.getId();
        if (html == null || comicChapter == null) {
            return list;
        }
        String packed = StringUtils.match("params\\s*=\\s*'([^']+)'", html, 1);
        if (packed == null) {
            packed = StringUtils.match("params\\s*=\\s*\"([^\"]+)\"", html, 1);
        }
        if (packed == null) {
            return list;
        }
        JSONObject object = null;
        for (String key : KEYS) {
            try {
                String json = DecryptionUtils.aesCbcDecryptEmbeddedIv(packed, key);
                if (json == null) {
                    continue;
                }
                json = json.trim();
                if (!json.startsWith("{")) {
                    continue;
                }
                object = new JSONObject(json);
                break;
            } catch (Exception ignored) {
            }
        }
        if (object == null) {
            return list;
        }
        String prefix = imagePrefix(object);
        JSONArray array = object.optJSONArray("chapter_images");
        if (array == null || array.length() == 0) {
            array = object.optJSONArray("images");
        }
        if (array == null) {
            return list;
        }
        int index = 0;
        for (int i = 0; i < array.length(); i++) {
            String url = array.optString(i, "");
            if (url.isEmpty()) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    url = firstString(item, "url", "src", "image", "path");
                }
            }
            url = absChapterImage(url, prefix);
            if (url == null) {
                continue;
            }
            Long id = Long.parseLong(comicChapter + "000" + index);
            list.add(new ImageUrl(id, comicChapter, ++index, url, false));
        }
        return list;
    }

    private String imagePrefix(JSONObject object) {
        JSONArray hosts = object.optJSONArray("images_hosts");
        if (hosts != null && hosts.length() > 0) {
            String host = hosts.optString(0, "").trim();
            if (!host.isEmpty()) {
                return trimSlash(host);
            }
        }
        String cdn = object.optString("cdnurl", "").trim();
        if (!cdn.isEmpty()) {
            return trimSlash(cdn);
        }
        if (mConfig.imageServerUrl != null && !mConfig.imageServerUrl.isEmpty()) {
            return trimSlash(mConfig.imageServerUrl);
        }
        return "";
    }

    private String absChapterImage(String url, String prefix) {
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
        if (!prefix.isEmpty()) {
            if (!url.startsWith("/")) {
                url = "/" + url;
            }
            return prefix + url;
        }
        return absImage(url);
    }

    private static String firstString(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = object.optString(key, "");
            if (value != null && !value.isEmpty() && !"null".equals(value)) {
                return value;
            }
        }
        return "";
    }

    private static String trimSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
