package com.haleydu.cimoc.utils

import android.content.Context
import android.content.Intent
import com.haleydu.cimoc.service.DownloadService

object ServiceUtils {

    @JvmStatic
    fun isServiceRunning(context: Context, service: Class<*>): Boolean {
        return DownloadService::class.java == service && DownloadService.isRunning()
    }

    @JvmStatic
    fun stopService(context: Context, service: Class<*>) {
        context.stopService(Intent(context, DownloadService::class.java))
    }
}
