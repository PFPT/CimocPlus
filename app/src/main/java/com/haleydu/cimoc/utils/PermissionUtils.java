package com.haleydu.cimoc.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.saf.DocumentFile;

public class PermissionUtils {

    public static boolean hasStoragePermission(Activity activity) {
        App app = (App) activity.getApplication();
        DocumentFile root = app.getDocumentFile();
        if (root != null && root.canWrite()) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasAllPermissions(Activity activity) {
        return hasStoragePermission(activity) && hasNotificationPermission(activity);
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
                && NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static int checkPermission(@NonNull Activity activity, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(activity, permission);
    }
}
