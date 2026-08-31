package com.haleydu.cimoc.source;

import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.utils.DecryptionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DaShuMH extends GenericHtmlParser {

    public static final int TYPE = 228;
    private static final Pattern PACKED = Pattern.compile(
            "\\(function\\(p,a,c,k,e,d\\).*?0,\\{\\}\\)\\)");
    private static final Pattern IMAGE_URL = Pattern.compile("https?://[^\"'\\s]+");

    public DaShuMH(Source source, SourceConfig config) {
        super(source, config);
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        Long comicChapter = chapter == null ? null : chapter.getId();
        if (html == null || comicChapter == null) {
            return list;
        }
        Matcher packedMatcher = PACKED.matcher(html);
        if (!packedMatcher.find()) {
            return super.parseImages(html, chapter);
        }
        try {
            String result = DecryptionUtils.evalDecrypt(packedMatcher.group());
            if (result == null || result.isEmpty()) {
                return super.parseImages(html, chapter);
            }
            Matcher urlMatcher = IMAGE_URL.matcher(result);
            int index = 0;
            while (urlMatcher.find()) {
                String url = urlMatcher.group();
                if (url.contains("banquan") || url.contains("icon_loading") || url.contains("grey")) {
                    continue;
                }
                url = absImage(url);
                if (url == null) {
                    continue;
                }
                Long id = Long.parseLong(comicChapter + "000" + index);
                list.add(new ImageUrl(id, comicChapter, ++index, url, false));
            }
        } catch (Exception ignored) {
        }
        if (list.isEmpty()) {
            return super.parseImages(html, chapter);
        }
        return list;
    }
}
