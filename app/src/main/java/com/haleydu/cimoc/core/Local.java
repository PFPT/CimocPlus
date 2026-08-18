package com.haleydu.cimoc.core;

import android.util.Pair;

import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Task;
import com.haleydu.cimoc.saf.DocumentFile;
import com.haleydu.cimoc.source.Locality;
import com.haleydu.cimoc.utils.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Hiroshi on 2017/5/20.
 */

public class Local {

    private static Pattern chapterPattern = null;

    public static List<Pair<Comic, ArrayList<Task>>> scan(final DocumentFile root) {
        List<Pair<Comic, ArrayList<Task>>> result = new ArrayList<>();

        ScanInfo info = new ScanInfo(root);
        countPicture(info);
        if (info.count > 5) {
            Pair<Comic, ArrayList<Task>> pair = Pair.create(buildComic(info.dir, info.cover), new ArrayList<Task>());
            pair.second.add(buildTask(info.dir, info.count, true));
            result.add(pair);
        } else {
            List<DocumentFile> list = new LinkedList<>();
            list.add(root);

            while (!list.isEmpty()) {
                DocumentFile dir = list.get(0);

                List<ScanInfo> guessChapter = new LinkedList<>();
                List<ScanInfo> guessComic = new LinkedList<>();
                List<DocumentFile> guessOther = classify(guessChapter, guessComic, dir);

                if (guessChapter.size() > 2 * guessComic.size()) {
                    result.add(merge(dir, guessChapter, guessComic));
                } else {
                    split(guessChapter, result);
                    split(guessComic, result);
                    list.addAll(guessOther);
                }

                list.remove(0);
            }
        }
        return result;
    }

    public static List<ImageUrl> images(final DocumentFile dir, final Chapter chapter) {
        List<DocumentFile> files = dir.listFiles(new DocumentFile.DocumentFileFilter() {
            @Override
            public boolean call(DocumentFile file) {
                return file.isFile() && StringUtils.endWith(file.getName(), "jpg", "png", "jpeg");
            }
        }, new Comparator<DocumentFile>() {
            @Override
            public int compare(DocumentFile lhs, DocumentFile rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        List<ImageUrl> list = Storage.buildImageUrlFromDocumentFile(files, chapter.getTitle(), chapter.getCount(), chapter);
        if (list.size() != 0) {
            return list;
        }
        throw new RuntimeException();
    }

    private static void countPicture(ScanInfo info) {
        String name = null;
        int other = 0;
        for (DocumentFile file : info.dir.listFiles()) {
            if (file.isFile() && StringUtils.endWith(file.getName(), "png", "jpg", "jpeg")) {
                ++info.count;
            } else {
                ++other;
            }
            if (name == null || file.getName().compareTo(name) < 0) {
                name = file.getName();
                info.cover = file.getUri().toString();
            }
            if (other > 5) {
                info.count = 0;
                break;
            }
        }
    }

    private static List<DocumentFile> classify(List<ScanInfo> chapter,
                                               List<ScanInfo> comic,
                                               DocumentFile dir) {
        List<DocumentFile> other = new LinkedList<>();
        for (DocumentFile file : dir.listFiles()) {
            if (file.isDirectory()) {
                ScanInfo info = new ScanInfo(file);
                countPicture(info);
                if (info.count > 5) {
                    if (isNameChapter(file)) {
                        chapter.add(info);
                    } else {
                        comic.add(info);
                    }
                } else {
                    other.add(file);
                }
            }
        }
        return other;
    }

    private static boolean isNameChapter(DocumentFile file) {
        if (chapterPattern == null) {
            chapterPattern = Pattern.compile("^[^(\\[]{0,5}[0-9]+|[0-9]+.{0,5}$");
        }
        Matcher matcher = chapterPattern.matcher(file.getName());
        return matcher.find() && ((float) matcher.group().length() / file.getName().length() > 0.8);
    }

    private static Comic buildComic(DocumentFile dir, String cover) {
        return new Comic(null, Locality.TYPE, dir.getUri().toString(), dir.getName(), cover,
                false, true, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static Task buildTask(DocumentFile dir, int count, boolean single) {
        return single ? new Task(null, -1, dir.getUri().toString(), "第01话", count, count) :
                new Task(null, -1, dir.getUri().toString(), dir.getName(), count, count);
    }

    private static Pair<Comic, ArrayList<Task>> merge(DocumentFile dir, List<ScanInfo> list1, List<ScanInfo> list2) {
        Pair<Comic, ArrayList<Task>> pair = Pair.create(buildComic(dir, list1.get(0).cover), new ArrayList<Task>());
        for (ScanInfo info : list1) {
            pair.second.add(buildTask(info.dir, info.count, false));
        }
        for (ScanInfo info : list2) {
            pair.second.add(buildTask(info.dir, info.count, false));
        }
        return pair;
    }

    private static void split(List<ScanInfo> list, List<Pair<Comic, ArrayList<Task>>> result) {
        for (ScanInfo info : list) {
            Pair<Comic, ArrayList<Task>> pair = Pair.create(buildComic(info.dir, info.cover), new ArrayList<Task>());
            pair.second.add(buildTask(info.dir, info.count, true));
            result.add(pair);
        }
    }

    private static class ScanInfo {
        DocumentFile dir = null;
        String cover = null;
        int count = 0;

        ScanInfo(DocumentFile dir) {
            this.dir = dir;
        }
    }

}
