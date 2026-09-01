package com.haleydu.cimoc.fresco

import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import com.facebook.common.logging.FLog
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.producers.BaseNetworkFetcher
import com.facebook.imagepipeline.producers.BaseProducerContextCallbacks
import com.facebook.imagepipeline.producers.Consumer
import com.facebook.imagepipeline.producers.FetchState
import com.facebook.imagepipeline.producers.NetworkFetcher
import com.facebook.imagepipeline.producers.ProducerContext
import com.haleydu.cimoc.App
import com.haleydu.cimoc.ui.widget.CustomToast
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.Executor

class OkHttpNetworkFetcher(
    private val mOkHttpClient: OkHttpClient,
    headers: Headers?
) : BaseNetworkFetcher<OkHttpNetworkFetcher.OkHttpNetworkFetchState>() {

    private var mCancellationExecutor: Executor? = null
    private var mHeaders: Headers? = headers

    init {
        try {
            mCancellationExecutor = mOkHttpClient.dispatcher.executorService
        } catch (e: NullPointerException) {
            CustomToast.showToast(App.getActivity(), "网络连接失败，请检查网络！！", 2000)
        }
    }

    fun setHeaders(headers: Headers?) {
        mHeaders = headers
    }

    override fun createFetchState(
        consumer: Consumer<EncodedImage>,
        context: ProducerContext
    ): OkHttpNetworkFetchState {
        return OkHttpNetworkFetchState(consumer, context)
    }

    override fun fetch(fetchState: OkHttpNetworkFetchState, callback: NetworkFetcher.Callback) {
        fetchState.submitTime = SystemClock.elapsedRealtime()
        val uri: Uri = fetchState.uri
        val builder = Request.Builder()
            .cacheControl(CacheControl.Builder().noStore().build())
            .url(uri.toString())
            .get()
        if (mHeaders != null) {
            builder.headers(mHeaders!!)
        }
        val request = builder.build()
        val call = mOkHttpClient.newCall(request)

        fetchState.context.addCallbacks(
            object : BaseProducerContextCallbacks() {
                override fun onCancellationRequested() {
                    if (Looper.myLooper() != Looper.getMainLooper()) {
                        call.cancel()
                    } else if (mCancellationExecutor != null) {
                        mCancellationExecutor!!.execute { call.cancel() }
                    } else {
                        call.cancel()
                    }
                }
            }
        )

        call.enqueue(
            object : okhttp3.Callback {
                override fun onResponse(call: Call, response: Response) {
                    fetchState.responseTime = SystemClock.elapsedRealtime()
                    val body = response.body
                    try {
                        if (!response.isSuccessful) {
                            handleException(call, IOException("Unexpected HTTP code $response"), callback)
                            return
                        }
                        var contentLength = body!!.contentLength()
                        if (contentLength < 0) {
                            contentLength = 0
                        }
                        callback.onResponse(body.byteStream(), contentLength.toInt())
                    } catch (e: Exception) {
                        handleException(call, e, callback)
                    } finally {
                        try {
                            body!!.close()
                        } catch (e: Exception) {
                            FLog.w(TAG, "Exception when closing response body", e)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    handleException(call, e, callback)
                }
            }
        )
    }

    override fun onFetchCompletion(fetchState: OkHttpNetworkFetchState, byteSize: Int) {
        fetchState.fetchCompleteTime = SystemClock.elapsedRealtime()
    }

    override fun getExtraMap(fetchState: OkHttpNetworkFetchState, byteSize: Int): Map<String, String> {
        val extraMap = HashMap<String, String>(4)
        extraMap[QUEUE_TIME] = (fetchState.responseTime - fetchState.submitTime).toString()
        extraMap[FETCH_TIME] = (fetchState.fetchCompleteTime - fetchState.responseTime).toString()
        extraMap[TOTAL_TIME] = (fetchState.fetchCompleteTime - fetchState.submitTime).toString()
        extraMap[IMAGE_SIZE] = byteSize.toString()
        return extraMap
    }

    private fun handleException(call: Call, e: Exception, callback: NetworkFetcher.Callback) {
        if (call.isCanceled) {
            callback.onCancellation()
        } else {
            callback.onFailure(e)
        }
    }

    class OkHttpNetworkFetchState(
        consumer: Consumer<EncodedImage>,
        producerContext: ProducerContext
    ) : FetchState(consumer, producerContext) {
        @JvmField
        var submitTime: Long = 0
        @JvmField
        var responseTime: Long = 0
        @JvmField
        var fetchCompleteTime: Long = 0
    }

    companion object {
        private const val TAG = "OkHttpNetworkFetchProducer"
        private const val QUEUE_TIME = "queue_time"
        private const val FETCH_TIME = "fetch_time"
        private const val TOTAL_TIME = "total_time"
        private const val IMAGE_SIZE = "image_size"
    }
}
