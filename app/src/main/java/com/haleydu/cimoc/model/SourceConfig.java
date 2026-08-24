package com.haleydu.cimoc.model;

import org.json.JSONObject;

public class SourceConfig {

    public final String key;
    public final int type;
    public final String title;
    public final String baseUrl;
    public final String serverUrl;
    public final String search;
    public final String searchInfoList;
    public final String searchInfoCid;
    public final String searchInfoTitle;
    public final String searchInfoCover;
    public final String searchInfoAuthor;
    public final String searchInfoUpdate;
    public final String parseInfoTitle;
    public final String parseInfoCover;
    public final String parseInfoIntro;
    public final String parseInfoAuthor;
    public final String parseInfoUpdate;
    public final String parseInfoStatus;
    public final String parseChapterList1;
    public final String parseChapterList2;
    public final String parseChapterList3;
    public final String parseChapterPath;
    public final String parseChapterTitle;
    public final String parseImageList;
    public final String parseImageUrl;
    public final String parseImageFaultImage;
    public final String imageServerUrl;
    public final String parseCategoryPath;
    public final String parseCategoryInfoList;
    public final String parseCategoryInfoCid;
    public final String parseCategoryInfoCidPre;
    public final String parseCategoryInfoTitle;
    public final String parseCategoryInfoCover;
    public final String parseCategoryInfoAuthor;
    public final String parseCategoryInfoUpdate;

    public SourceConfig(String key, int type, String title, JSONObject object) {
        this.key = key;
        this.type = type;
        this.title = title;
        this.baseUrl = trimSlash(opt(object, "baseUrl"));
        this.serverUrl = opt(object, "serverUrl");
        this.search = opt(object, "search");
        this.searchInfoList = opt(object, "searchInfoList");
        this.searchInfoCid = opt(object, "searchInfoCid");
        this.searchInfoTitle = opt(object, "searchInfoTitle");
        this.searchInfoCover = opt(object, "searchInfoCover");
        this.searchInfoAuthor = opt(object, "searchInfoAuthor");
        this.searchInfoUpdate = opt(object, "searchInfoUpdate");
        this.parseInfoTitle = opt(object, "parseInfoTitle");
        this.parseInfoCover = opt(object, "parseInfoCover");
        this.parseInfoIntro = opt(object, "parseInfoIntro");
        this.parseInfoAuthor = opt(object, "parseInfoAuthor");
        this.parseInfoUpdate = opt(object, "parseInfoUpdate");
        this.parseInfoStatus = opt(object, "parseInfoStatus");
        this.parseChapterList1 = opt(object, "parseChapterList1");
        this.parseChapterList2 = opt(object, "parseChapterList2");
        this.parseChapterList3 = opt(object, "parseChapterList3");
        this.parseChapterPath = opt(object, "parseChapterPath");
        this.parseChapterTitle = opt(object, "parseChapterTitle");
        this.parseImageList = opt(object, "parseImageList");
        this.parseImageUrl = firstNonEmpty(opt(object, "parseImageUrl"), opt(object, "parseImageSrc"));
        this.parseImageFaultImage = opt(object, "parseImageFaultImage");
        this.imageServerUrl = firstNonEmpty(opt(object, "imageServerUrl"), this.serverUrl);
        this.parseCategoryPath = opt(object, "parseCategoryPath");
        this.parseCategoryInfoList = firstNonEmpty(opt(object, "parseCategoryInfoList"), this.searchInfoList);
        this.parseCategoryInfoCid = firstNonEmpty(opt(object, "parseCategoryInfoCid"), this.searchInfoCid);
        this.parseCategoryInfoCidPre = opt(object, "parseCategoryInfoCidPre");
        this.parseCategoryInfoTitle = firstNonEmpty(opt(object, "parseCategoryInfoTitle"), this.searchInfoTitle);
        this.parseCategoryInfoCover = firstNonEmpty(opt(object, "parseCategoryInfoCover"), this.searchInfoCover);
        this.parseCategoryInfoAuthor = firstNonEmpty(opt(object, "parseCategoryInfoAuthor"), this.searchInfoAuthor);
        this.parseCategoryInfoUpdate = firstNonEmpty(opt(object, "parseCategoryInfoUpdate"), this.searchInfoUpdate);
    }

    public boolean hasCategory() {
        return !parseCategoryPath.isEmpty();
    }

    public boolean isComplete() {
        return !baseUrl.isEmpty()
                && !search.isEmpty()
                && !searchInfoList.isEmpty()
                && !parseChapterList1.isEmpty();
    }

    public Source toSource(boolean enable) {
        return new Source(null, title, type, enable);
    }

    private static String opt(JSONObject object, String name) {
        String value = object.optString(name, "");
        return value == null ? "" : value.trim();
    }

    private static String firstNonEmpty(String a, String b) {
        return a == null || a.isEmpty() ? (b == null ? "" : b) : a;
    }

    private static String trimSlash(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
