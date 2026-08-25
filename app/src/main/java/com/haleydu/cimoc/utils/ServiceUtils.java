package com.haleydu.cimoc.utils;

import android.content.Context;
import android.content.Intent;

import com.haleydu.cimoc.service.DownloadService;

public class ServiceUtils {

    public static boolean isServiceRunning(Context context, Class<?> service) {
        return DownloadService.class.equals(service) && DownloadService.isRunning();
    }

    public static void stopService(Context context, Class<?> service) {
        context.stopService(new Intent(context, DownloadService.class));
    }

}
