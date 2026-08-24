package com.haleydu.cimoc.core;

import com.haleydu.cimoc.data.ChapterManager;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.parser.Parser;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MangaImages {

    public static List<ImageUrl> getImageUrls(OkHttpClient client, Parser parser, int source, String cid, String path, String title, ChapterManager mChapterManager) throws InterruptedIOException {
        List<ImageUrl> list = new ArrayList<>();
        Response response = null;
        try {
            if (!list.isEmpty()) {
                return list;
            }
            Request request = parser.getImagesRequest(cid, path);
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                List<Chapter> chapter = mChapterManager.getChapter(path, title);
                if (chapter != null && chapter.size() >= 1) {
                    list.addAll(parser.parseImages(response.body().string(), chapter.get(0)));
                }
                if (list.size() == 0) {
                    list.addAll(parser.parseImages(response.body().string()));
                }
            } else {
                throw new Manga.NetworkErrorException();
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return list;
    }

    public static String getLazyUrl(OkHttpClient client, Parser parser, String url) throws InterruptedIOException {
        Response response = null;
        try {
            Request request = parser.getLazyRequest(url);
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return parser.parseLazy(response.body().string(), url);
            } else {
                throw new Manga.NetworkErrorException();
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }
}
