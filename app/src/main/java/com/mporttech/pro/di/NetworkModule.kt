package com.mporttech.pro.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mporttech.pro.core.auth.TokenAuthenticator
import com.mporttech.pro.core.auth.TokenStore
import com.mporttech.pro.core.common.Constants
import com.mporttech.pro.core.security.CertificatePinning
import com.mporttech.pro.data.remote.AuthApi
import com.mporttech.pro.data.remote.MportApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStore: TokenStore): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val token = tokenStore.accessToken()
        val builder = original.newBuilder().header("Accept", "application/json")
        if (!token.isNullOrBlank() && original.header("Authorization") == null) {
            builder.header("Authorization", "Bearer $token")
        }
        chain.proceed(builder.build())
    }

    @Provides
    @Singleton
    fun provideOkHttp(
        authInterceptor: Interceptor,
        authenticator: TokenAuthenticator
    ): OkHttpClient {
        val log = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .addInterceptor(log)

        // Certificate pinning (SPKI). Applied only when ENABLE_CERT_PINNING=true
        // and CertificatePinning.HOST_PINS contains real sha256/... values.
        CertificatePinning.buildPinnerOrNull(Constants.API_BASE_URL)?.let { pinner ->
            builder.certificatePinner(pinner)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideMportApi(retrofit: Retrofit): MportApi =
        retrofit.create(MportApi::class.java)
}
