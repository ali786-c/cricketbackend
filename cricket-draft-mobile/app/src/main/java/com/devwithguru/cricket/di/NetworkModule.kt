package com.devwithguru.cricket.di

import com.devwithguru.cricket.data.api.ApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ── Base URL ──────────────────────────────────────────
    // Change this to your Laravel backend URL
    private const val BASE_URL = "https://backedn.careerinpak.com/"

    // ── Timeout Config ────────────────────────────────────
    private const val CONNECT_TIMEOUT = 15L
    private const val READ_TIMEOUT = 20L
    private const val WRITE_TIMEOUT = 20L

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // Safety: catch any SecurityException/RuntimeException from network
                try {
                    val request = chain.request().newBuilder()
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(request)
                } catch (e: SecurityException) {
                    // Return a fake 503 response instead of crashing
                    okhttp3.Response.Builder()
                        .code(503)
                        .message("Network unavailable")
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .request(chain.request())
                        .body(okhttp3.ResponseBody.create(
                            "application/json".toMediaType(),
                            "{\"error\":\"Network permission not granted\"}"
                        ))
                        .build()
                }
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }
}
