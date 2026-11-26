package com.haleydu.cimoc.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.haleydu.cimoc.network.NetworkPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideWifiOnlyInterceptor(
        @ApplicationContext context: Context,
        networkPolicy: NetworkPolicy
    ): WifiOnlyInterceptor = WifiOnlyInterceptor(context, networkPolicy)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        wifiOnlyInterceptor: WifiOnlyInterceptor
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http")
        return OkHttpClient.Builder()
            .cache(Cache(cacheDir, 50L * 1024L * 1024L))
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(wifiOnlyInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                val next = if (request.header("User-Agent") == null) {
                    request.newBuilder()
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        )
                        .build()
                } else {
                    request
                }
                chain.proceed(next)
            }
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                val type = response.header("Content-Type").orEmpty()
                if (type.startsWith("image/")) {
                    response
                } else {
                    response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "no-store")
                        .build()
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideKtorClient(okHttpClient: OkHttpClient): io.ktor.client.HttpClient {
        return io.ktor.client.HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
            engine {
                preconfigured = okHttpClient
            }
        }
    }

}

class WifiOnlyInterceptor(
    context: Context,
    private val networkPolicy: NetworkPolicy
) : Interceptor {
    private val connectivity =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun intercept(chain: Interceptor.Chain): Response {
        if (networkPolicy.isConnectOnlyWifi() && !onWifi()) {
            throw IOException("wifi only")
        }
        return chain.proceed(chain.request())
    }

    @Suppress("DEPRECATION")
    private fun onWifi(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val info = connectivity.activeNetworkInfo
            return info != null && info.isConnected && info.type == ConnectivityManager.TYPE_WIFI
        }
        val network = connectivity.activeNetwork ?: return false
        val caps = connectivity.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
