package com.haleydu.cimoc.misc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.utils.PermissionUtils;

public class NotificationWrapper {

    private final Context mContext;
    private final NotificationManager mManager;
    private final NotificationCompat.Builder mBuilder;
    private final int mId;

    public NotificationWrapper(Context context, String id, @DrawableRes int icon, boolean ongoing) {
        mContext = context.getApplicationContext();
        String title = context.getString(R.string.app_name);
        mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mManager.createNotificationChannel(new NotificationChannel(id, id, NotificationManager.IMPORTANCE_LOW));
        }
        mBuilder = new NotificationCompat.Builder(context, id);
        mBuilder.setContentTitle(title)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), icon))
                .setOngoing(ongoing)
                .setOnlyAlertOnce(true);
        mId = id.hashCode();
    }

    public Notification getNotification() {
        return mBuilder.build();
    }

    public int getId() {
        return mId;
    }

    public void post(int progress, int max) {
        mBuilder.setProgress(max, progress, false);
        notifyIfAllowed();
    }

    public void post(String content, int progress, int max) {
        mBuilder.setContentText(content).setTicker(content);
        post(progress, max);
    }

    public void post(String content, boolean ongoing) {
        mBuilder.setOngoing(ongoing);
        post(content, 0, 0);
    }

    public void cancel() {
        mManager.cancel(mId);
    }

    private void notifyIfAllowed() {
        if (!PermissionUtils.hasNotificationPermission(mContext)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !NotificationManagerCompat.from(mContext).areNotificationsEnabled()) {
            return;
        }
        mManager.notify(mId, mBuilder.build());
    }

}
