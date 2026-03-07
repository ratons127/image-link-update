package com.qtiqo.share.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.qtiqo.share.data.local.AppDatabase
import com.qtiqo.share.data.local.dao.UploadDao
import com.qtiqo.share.data.prefs.SecureSessionStore
import com.qtiqo.share.data.remote.api.AuthApi
import com.qtiqo.share.data.remote.api.FilesApi
import com.qtiqo.share.data.remote.api.PublicApi
import com.qtiqo.share.data.remote.api.RawUploadApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "qtiqo-share.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideUploadDao(db: AppDatabase): UploadDao = db.uploadDao()

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver = context.contentResolver

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttp(sessionStore: SecureSessionStore): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val path = request.url.encodedPath
                val shouldAttachJwt = path.startsWith("/files") || path.startsWith("/admin") || path.startsWith("/me")
                val hasAuthHeader = request.header("Authorization") != null
                val token = sessionStore.sessionFlow.value?.token
                val nextRequest = if (shouldAttachJwt && !hasAuthHeader && !token.isNullOrBlank()) {
                    request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    request
                }
                chain.proceed(nextRequest)
            }
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://imagelink.qtiqo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    fun provideFilesApi(retrofit: Retrofit): FilesApi = retrofit.create(FilesApi::class.java)

    @Provides
    fun providePublicApi(retrofit: Retrofit): PublicApi = retrofit.create(PublicApi::class.java)

    @Provides
    fun provideRawUploadApi(retrofit: Retrofit): RawUploadApi = retrofit.create(RawUploadApi::class.java)
}
