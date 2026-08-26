package com.haleydu.cimoc.di

import android.content.Context
import android.net.wifi.WifiManager
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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
        val trustAllCerts = TrustAllCerts()
        val cacheDir = File(context.cacheDir, "http")
        return OkHttpClient.Builder()
            .cache(Cache(cacheDir, 256L * 1024L * 1024L))
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .sslSocketFactory(createSslSocketFactory(trustAllCerts), trustAllCerts)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
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
                val cacheControl = if (type.startsWith("image/")) {
                    "public, max-age=604800"
                } else {
                    "no-store"
                }
                response.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", cacheControl)
                    .build()
            }
            .build()
    }

    private fun createSslSocketFactory(trustManager: X509TrustManager): SSLSocketFactory {
        val sc = SSLContext.getInstance("TLS")
        sc.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        return sc.socketFactory
    }

    private class TrustAllCerts : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}

class WifiOnlyInterceptor(
    context: Context,
    private val networkPolicy: NetworkPolicy
) : Interceptor {
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!wifiManager.isWifiEnabled && networkPolicy.isConnectOnlyWifi()) {
            throw IOException("wifi only")
        }
        return chain.proceed(chain.request())
    }
}
