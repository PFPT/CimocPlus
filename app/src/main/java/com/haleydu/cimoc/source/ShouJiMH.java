package com.haleydu.cimoc.source;

import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceConfig;
import com.haleydu.cimoc.utils.DecryptionUtils;
import com.haleydu.cimoc.utils.StringUtils;

import java.util.LinkedList;
import java.util.List;

public class ShouJiMH extends GenericHtmlParser {

    public static final int TYPE = 226;

    public ShouJiMH(Source source, SourceConfig config) {
        super(source, config);
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        Long comicChapter = chapter == null ? null : chapter.getId();
        if (html == null || comicChapter == null) {
            return list;
        }
        String packed = StringUtils.match("eval\\(.*\\)", html, 0);
        if (packed == null) {
            return super.parseImages(html, chapter);
        }
        try {
            String decrypted = DecryptionUtils.evalDecrypt(packed, "newImgs");
            if (decrypted == null || decrypted.isEmpty() || "undefined".equals(decrypted)) {
                decrypted = DecryptionUtils.evalDecrypt(packed);
            }
            if (decrypted == null || decrypted.isEmpty()) {
                return list;
            }
            String[] array = decrypted.split(",");
            int index = 0;
            for (String item : array) {
                String url = item.trim();
                if (url.isEmpty()) {
                    continue;
                }
                url = absImage(url);
                Long id = Long.parseLong(comicChapter + "000" + index);
                list.add(new ImageUrl(id, comicChapter, ++index, url, false));
            }
        } catch (Exception ignored) {
        }
        return list;
    }
}
