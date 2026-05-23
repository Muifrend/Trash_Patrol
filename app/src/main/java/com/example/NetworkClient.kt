package com.example

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface TrashPatrolApi {
    @Multipart
    @POST("android/report")
    suspend fun submitReport(
        @Part image: MultipartBody.Part,
        @Part("mission_id") missionId: RequestBody,
        @Part location: MultipartBody.Part?
    ): Response<ReportResponse>

    @GET("missions/{mission_id}")
    suspend fun getMission(
        @Path("mission_id") missionId: String
    ): Response<MissionResponse>

    @Multipart
    @POST("android/confirmation")
    suspend fun submitConfirmation(
        @Part image: MultipartBody.Part,
        @Part("mission_id") missionId: RequestBody
    ): Response<ConfirmationResponse>

    @GET("health")
    suspend fun checkHealth(): Response<ResponseBody>
}

object NetworkClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun getOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val headerInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("ngrok-skip-browser-warning", "true")
                .header("Accept", "application/json")
            val request = requestBuilder.build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Dynamically builds retrofitted TrashPatrolApi representation based on the provided base url.
     */
    fun getApi(baseUrl: String): TrashPatrolApi {
        // Enforce trailing slash for Retrofit base URL rule
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(getOkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TrashPatrolApi::class.java)
    }
}
