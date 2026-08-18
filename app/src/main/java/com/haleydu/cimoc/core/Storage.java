package com.haleydu.cimoc.core;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.saf.DocumentFile;
import com.haleydu.cimoc.utils.DecryptionUtils;
import com.haleydu.cimoc.utils.DocumentUtils;
import com.haleydu.cimoc.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hiroshi on 2016/10/16.
 */

public class Storage {

    private static String DOWNLOAD = "download";
    private static String PICTURE = "picture";
    private static String BACKUP = "backup";

    public static DocumentFile initRoot(Context context, String uri) {
        if (uri == null || uri.isEmpty()) {
            File dir = context.getExternalFilesDir(null);
            if (dir == null) {
                dir = context.getFilesDir();
            }
            File file = new File(dir, "Cimoc");
            if (file.exists() || file.mkdirs()) {
                return DocumentFile.fromFile(file);
            }
            return null;
        } else if (uri.startsWith("content")) {
            return DocumentFile.fromTreeUri(context, Uri.parse(uri));
        } else if (uri.startsWith("file")) {
            return DocumentFile.fromFile(new File(Uri.parse(uri).getPath()));
        } else {
            return DocumentFile.fromFile(new File(uri, "Cimoc"));
        }
    }

    public interface ProgressCallback {
        void onProgress(String message);
    }

    private static boolean copyFile(ContentResolver resolver, DocumentFile src,
                                    DocumentFile parent, ProgressCallback callback) {
        DocumentFile file = DocumentUtils.getOrCreateFile(parent, src.getName());
        if (file != null) {
            callback.onProgress(StringUtils.format("正在移动 %s...", src.getUri().getLastPathSegment()));
            try {
                DocumentUtils.writeBinaryToFile(resolver, src, file);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean copyDir(ContentResolver resolver, DocumentFile src,
                                   DocumentFile parent, ProgressCallback callback) {
        if (src.isDirectory()) {
            DocumentFile dir = DocumentUtils.getOrCreateSubDirectory(parent, src.getName());
            for (DocumentFile file : src.listFiles()) {
                if (file.isDirectory()) {
                    if (!copyDir(resolver, file, dir, callback)) {
                        return false;
                    }
                } else if (!copyFile(resolver, file, dir, callback)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean copyDir(ContentResolver resolver, DocumentFile src,
                                   DocumentFile dst, String name, ProgressCallback callback) {
        DocumentFile file = src.findFile(name);
        if (file != null && file.isDirectory()) {
            return copyDir(resolver, file, dst, callback);
        }
        return true;
    }

    private static void deleteDir(DocumentFile parent, String name, ProgressCallback callback) {
        DocumentFile file = parent.findFile(name);
        if (file != null && file.isDirectory()) {
            callback.onProgress(StringUtils.format("正在删除 %s", file.getUri().getLastPathSegment()));
            file.delete();
        }
    }

    private static boolean isDirSame(DocumentFile root, DocumentFile dst) {
        return root.getUri().getScheme().equals("file") && dst.getUri().getPath().endsWith("primary:Cimoc") ||
                root.getUri().getPath().equals(dst.getUri().getPath());
    }

    public static void moveRootDir(final ContentResolver resolver, final DocumentFile root, final DocumentFile dst,
                                   ProgressCallback callback) {
        if (dst.canRead() && !isDirSame(root, dst)) {
            root.refresh();
            if (copyDir(resolver, root, dst, BACKUP, callback) &&
                    copyDir(resolver, root, dst, DOWNLOAD, callback) &&
                    copyDir(resolver, root, dst, PICTURE, callback)) {
                deleteDir(root, BACKUP, callback);
                deleteDir(root, DOWNLOAD, callback);
                deleteDir(root, PICTURE, callback);
                return;
            }
        }
        throw new RuntimeException();
    }

    public static Uri savePicture(final ContentResolver resolver, final DocumentFile root,
                                  final InputStream stream, final String filename) {
        try {
            DocumentFile dir = DocumentUtils.getOrCreateSubDirectory(root, PICTURE);
            if (dir != null) {
                DocumentFile file = DocumentUtils.getOrCreateFile(dir, filename);
                DocumentUtils.writeBinaryToFile(resolver, file, stream);
                return file.getUri();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }

    public static List<ImageUrl> buildImageUrlFromDocumentFile(List<DocumentFile> list, String chapterStr, int max, Chapter chapter) {
        int count = 0;
        List<ImageUrl> result = new ArrayList<>(list.size());
        for (DocumentFile file : list) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try {
                BitmapFactory.decodeStream(file.openInputStream(), null, opts);
                String uri = file.getUri().toString();
                if (uri.startsWith("file")) {   // content:// 解码会出错 file:// 中文路径如果不解码 Fresco 读取不了
                    uri = DecryptionUtils.urlDecrypt(uri);
                }
                Long comicChapter = chapter.getId();
                Long id = Long.parseLong(comicChapter + "300" + count);
                ImageUrl image = new ImageUrl(id, chapter.getSourceComic(),++count, uri, false);
                image.setHeight(opts.outHeight);
                image.setWidth(opts.outWidth);
                image.setChapter(chapterStr);
                result.add(image);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (count >= max) {
                break;
            }
        }
        return result;
    }

}
