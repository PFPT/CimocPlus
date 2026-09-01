package com.haleydu.cimoc.misc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.haleydu.cimoc.R
import com.haleydu.cimoc.utils.PermissionUtils

class NotificationWrapper(context: Context, id: String, @DrawableRes icon: Int, ongoing: Boolean) {

    private val mContext: Context = context.applicationContext
    private val mManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val mBuilder: NotificationCompat.Builder
    private val mId: Int

    val notification: Notification
        get() = mBuilder.build()

    val id: Int
        get() = mId

    init {
        val title = context.getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mManager.createNotificationChannel(NotificationChannel(id, id, NotificationManager.IMPORTANCE_LOW))
        }
        mBuilder = NotificationCompat.Builder(context, id)
        mBuilder.setContentTitle(title)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, icon))
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
        mId = id.hashCode()
    }

    fun post(progress: Int, max: Int) {
        mBuilder.setProgress(max, progress, false)
        notifyIfAllowed()
    }

    fun post(content: String, progress: Int, max: Int) {
        mBuilder.setContentText(content).setTicker(content)
        post(progress, max)
    }

    fun post(content: String, ongoing: Boolean) {
        mBuilder.setOngoing(ongoing)
        post(content, 0, 0)
    }

    fun cancel() {
        mManager.cancel(mId)
    }

    private fun notifyIfAllowed() {
        if (!PermissionUtils.hasNotificationPermission(mContext) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(mContext).areNotificationsEnabled()
        ) {
            return
        }
        mManager.notify(mId, mBuilder.build())
    }
}
