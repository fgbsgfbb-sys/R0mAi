package com.example.api

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class HfSibling(
    val rfilename: String
)

@JsonClass(generateAdapter = true)
data class HfModel(
    val id: String,
    val author: String? = "HuggingFace",
    val downloads: Int? = 0,
    val likes: Int? = 0,
    val siblings: List<HfSibling>? = null
)

interface HuggingFaceApi {
    @GET("api/models")
    suspend fun searchModels(
        @Query("search") query: String,
        @Query("limit") limit: Int = 20,
        @Query("full") full: Boolean = true,
        @Header("Authorization") authHeader: String? = null
    ): List<HfModel>
}

object HfRetrofitClient {
    private const val BASE_URL = "https://huggingface.co/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: HuggingFaceApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(HuggingFaceApi::class.java)
    }
}
