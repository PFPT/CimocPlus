package com.haleydu.cimoc.service

import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Pair
import androidx.collection.LongSparseArray
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.haleydu.cimoc.App
import com.haleydu.cimoc.R
import com.haleydu.cimoc.core.Download
import com.haleydu.cimoc.core.MangaImages
import com.haleydu.cimoc.data.ChapterManager
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.data.TaskManager
import com.haleydu.cimoc.di.AppEntryPoint
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.misc.NotificationWrapper
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.parser.Parser
import com.haleydu.cimoc.saf.DocumentFile
import com.haleydu.cimoc.utils.DocumentUtils
import com.haleydu.cimoc.utils.JMTTUtil
import com.haleydu.cimoc.utils.StringUtils
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    private lateinit var mWorkerArray: LongSparseArray<Pair<Worker, Future<*>>>
    private lateinit var mExecutorService: ExecutorService
    private var mNotification: NotificationWrapper? = null

    @Inject
    lateinit var mTaskManager: TaskManager

    @Inject
    lateinit var mSourceManager: SourceManager

    @Inject
    lateinit var mChapterManager: ChapterManager

    @Inject
    lateinit var mHttpClient: OkHttpClient

    private lateinit var mContentResolver: ContentResolver

    override fun onBind(intent: Intent): IBinder {
        return DownloadServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()
        sInstance = this
        sRunning.set(true)
        val manager = EntryPointAccessors.fromApplication(
            applicationContext,
            AppEntryPoint::class.java
        ).preferenceManager()
        val num = manager.getInt(PreferenceManager.PREF_DOWNLOAD_THREAD, 2)
        mWorkerArray = LongSparseArray()
        mExecutorService = Executors.newFixedThreadPool(num)
        mContentResolver = contentResolver
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            AppEventBus.post(AppEvent(AppEvent.EVENT_DOWNLOAD_START))
            if (mNotification == null) {
                mNotification = NotificationWrapper(
                    this,
                    NOTIFICATION_DOWNLOAD,
                    R.drawable.ic_file_download_white_24dp,
                    true
                )
                mNotification!!.post(getString(R.string.download_service_doing), true)
            }
            startAsForeground()
            val list = IntentCompat.getParcelableArrayListExtra(intent, Extra.EXTRA_TASK, Task::class.java)
            if (list != null) {
                for (task in list) {
                    val worker = Worker(task)
                    val future = mExecutorService.submit(worker)
                    addWorker(task.id!!, worker, future)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val notification = mNotification!!.notification
        val id = mNotification!!.id
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(id, notification)
        }
    }

    override fun onDestroy() {
        if (sInstance === this) sInstance = null
        sRunning.set(false)
        super.onDestroy()
        if (mNotification != null) {
            mExecutorService.shutdownNow()
            notifyCompleted()
        }
    }

    fun getAppInstance(): App {
        return application as App
    }

    @Synchronized
    fun addWorker(id: Long, worker: Worker, future: Future<*>) {
        if (mWorkerArray.get(id) == null) {
            mWorkerArray.put(id, Pair.create(worker, future))
        }
    }

    @Synchronized
    fun removeDownload(id: Long?) {
        val key = id ?: return
        val pair = mWorkerArray.get(key)
        if (pair != null) {
            pair.second.cancel(true)
            mWorkerArray.remove(key)
        }
    }

    @Synchronized
    fun completeDownload(id: Long) {
        mWorkerArray.remove(id)
        if (mWorkerArray.size() == 0) {
            notifyCompleted()
            stopSelf()
        }
    }

    private fun notifyCompleted() {
        if (mNotification != null) {
            mNotification!!.post(getString(R.string.download_service_done), false)
            mNotification!!.cancel()
            mNotification = null
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        mWorkerArray.clear()
        AppEventBus.post(AppEvent(AppEvent.EVENT_DOWNLOAD_STOP))
    }

    @Synchronized
    fun initTask(list: List<Task>) {
        for (task in list) {
            val pair = mWorkerArray.get(task.id!!)
            if (pair != null) {
                task.state = pair.first.mTask.state
            }
        }
    }

    inner class Worker(task: Task) : Runnable {
        var mTask: Task = task
        private var mParse: Parser = mSourceManager.getParser(task.source)

        override fun run() {
            try {
                val list = onDownloadParse()
                val size = list.size
                if (size != 0) {
                    val dir = Download.updateChapterIndex(mContentResolver, getAppInstance().documentFile!!, mTask)
                    if (dir != null) {
                        mTask.max = size
                        mTask.state = Task.STATE_DOING
                        var success = false
                        var i = mTask.progress
                        while (i < size) {
                            onDownloadProgress(i)
                            val image = list[i]
                            var count = 0
                            success = false
                            while (count++ < 20 && !success) {
                                val urls = image.urls
                                var j = 0
                                while (!success && j < urls!!.size) {
                                    val url = if (image.lazy) MangaImages.getLazyUrl(mHttpClient, mParse, urls[j]) else urls[j]
                                    val request = buildRequest(mParse.getHeader(url ?: ""), url)
                                    success = RequestAndWrite(dir, request, i + 1, url)
                                    j++
                                }
                            }
                            if (!success) {
                                AppEventBus.post(AppEvent(AppEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_ERROR, mTask.id))
                                break
                            }
                            i++
                        }
                        if (success) {
                            onDownloadProgress(size)
                        }
                    } else {
                        AppEventBus.post(AppEvent(AppEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_ERROR, mTask.id))
                    }
                } else {
                    AppEventBus.post(AppEvent(AppEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_ERROR, mTask.id))
                }
            } catch (e: InterruptedIOException) {
                AppEventBus.post(AppEvent(AppEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_PAUSE, mTask.id))
            }
            completeDownload(mTask.id!!)
        }

        @Throws(InterruptedIOException::class)
        private fun RequestAndWrite(parent: DocumentFile, request: Request?, num: Int, url: String?): Boolean {
            if (request != null) {
                var response: Response? = null
                try {
                    if (mTask.source == 72) {
                        val mJMTTHttpClient = OkHttpClient().newBuilder()
                            .followRedirects(true)
                            .followSslRedirects(true)
                            .retryOnConnectionFailure(true)
                            .addInterceptor { chain ->
                                val url1 = chain.request().url.toString()
                                val response1 = chain.proceed(chain.request())
                                if (!url1.lowercase().contains("media/photos")) return@addInterceptor response1
                                val cha = url1.substring(url1.indexOf("photos/") + 7, url1.lastIndexOf("/")).toInt()
                                if (cha < 220980) return@addInterceptor response1
                                val res = JMTTUtil().decodeImage(response1.body!!.byteStream())
                                val mediaType = "image/avif,image/webp,image/apng,image/*,*/*".toMediaTypeOrNull()
                                val outputBytes = res.toResponseBody(mediaType)
                                response1.newBuilder().body(outputBytes).build()
                            }
                            .build()
                        response = mJMTTHttpClient.newCall(request).execute()
                    } else {
                        response = mHttpClient.newCall(request).execute()
                    }
                    if (response.isSuccessful) {
                        var displayName = buildFileName(num, url)
                        displayName = displayName.replace("[:/(\\\\)(\\?)<>\"(\\|)(\\.)]".toRegex(), "_") + ".jpg"
                        val file = DocumentUtils.getOrCreateFile(parent, displayName)
                        DocumentUtils.writeBinaryToFile(mContentResolver, file!!, response.body!!.byteStream())
                        return true
                    }
                } catch (e: SocketTimeoutException) {
                    e.printStackTrace()
                } catch (e: InterruptedIOException) {
                    throw e
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    response?.close()
                }
            }
            return false
        }

        private fun buildRequest(headers: Headers?, url: String?): Request? {
            if (StringUtils.isEmpty(url)) {
                return null
            }
            return Request.Builder()
                .cacheControl(CacheControl.Builder().noStore().build())
                .headers(headers!!)
                .url(url!!)
                .get()
                .build()
        }

        private fun buildFileName(num: Int, url: String?): String {
            var suffix = StringUtils.split(url, "\\.", -1)
            if (suffix == null) {
                suffix = "jpg"
            } else {
                suffix = suffix.split("\\?".toRegex()).toTypedArray()[0]
            }
            return StringUtils.format("%03d.%s", num, suffix)
        }

        @Throws(InterruptedIOException::class)
        private fun onDownloadParse(): List<ImageUrl> {
            mTask.state = Task.STATE_PARSE
            AppEventBus.post(AppEvent(AppEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_PARSE, mTask.id))
            return MangaImages.getImageUrls(mHttpClient, mParse, mTask.source, mTask.cid ?: "", mTask.path, mTask.title, mChapterManager)
        }

        private fun onDownloadProgress(progress: Int) {
            mTask.progress = progress
            mTaskManager.update(mTask)
            AppEventBus.post(AppEvent(AppEvent.EVENT_TASK_PROCESS, mTask.id, progress, mTask.max))
        }
    }

    inner class DownloadServiceBinder : Binder() {
        val service: DownloadService
            get() = this@DownloadService
    }

    companion object {
        private const val NOTIFICATION_DOWNLOAD = "NOTIFICATION_DOWNLOAD"
        private val sRunning = AtomicBoolean(false)
        @Volatile
        private var sInstance: DownloadService? = null

        @JvmStatic
        fun createIntent(context: Context, task: Task): Intent {
            val list = ArrayList<Task>(1)
            list.add(task)
            return createIntent(context, list)
        }

        @JvmStatic
        fun createIntent(context: Context, list: ArrayList<Task>): Intent {
            val intent = Intent(context, DownloadService::class.java)
            intent.putParcelableArrayListExtra(Extra.EXTRA_TASK, list)
            return intent
        }

        @JvmStatic
        fun start(context: Context, intent: Intent) {
            ContextCompat.startForegroundService(context, intent)
        }

        @JvmStatic
        fun isRunning(): Boolean {
            return sRunning.get()
        }

        @JvmStatic
        fun pauseTask(id: Long) {
            sInstance?.removeDownload(id)
        }

        @JvmStatic
        fun syncTaskState(list: List<Task>) {
            sInstance?.initTask(list)
        }
    }
}
