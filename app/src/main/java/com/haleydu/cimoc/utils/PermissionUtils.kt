package com.haleydu.cimoc.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.haleydu.cimoc.App

object PermissionUtils {

    @JvmStatic
    fun hasStoragePermission(activity: Activity): Boolean {
        val app = activity.application as App
        val root = app.documentFile
        if (root != null && root.canWrite()) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }
        return checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun hasAllPermissions(activity: Activity): Boolean {
        return hasStoragePermission(activity) && hasNotificationPermission(activity)
    }

    @JvmStatic
    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @JvmStatic
    fun checkPermission(activity: Activity, permission: String): Int {
        return ContextCompat.checkSelfPermission(activity, permission)
    }
}
