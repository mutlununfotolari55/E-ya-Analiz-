package com.example

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int? = null,
    val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Message(
    val role: String,
    val content: List<Content>
)

@JsonClass(generateAdapter = true)
data class Content(
    val type: String,
    val text: String? = null,
    val image_url: ImageUrl? = null
)

@JsonClass(generateAdapter = true)
data class ImageUrl(
    val url: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponse(
    val choices: List<Choice>?
)

@JsonClass(generateAdapter = true)
data class Choice(
    val message: ResponseMessage?
)

@JsonClass(generateAdapter = true)
data class ResponseMessage(
    val content: String?
)

interface OpenRouterService {
    @Headers("X-Title: Evren AI")
    @POST("chat/completions")
    suspend fun generateContent(
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse

    @retrofit2.http.Streaming
    @Headers("X-Title: Evren AI")
    @POST("chat/completions")
    suspend fun streamContent(
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body request: OpenRouterRequest
    ): okhttp3.ResponseBody
}

object OpenRouterClient {
    private var cachedBaseUrl: String? = null
    private var cachedApi: OpenRouterService? = null

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    fun getApi(baseUrl: String = EvrenPreferences.DEFAULT_BASE_URL): OpenRouterService {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val existing = cachedApi
        if (existing != null && cachedBaseUrl == normalized) {
            return existing
        }
        val service = Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenRouterService::class.java)
        cachedBaseUrl = normalized
        cachedApi = service
        return service
    }

    val api: OpenRouterService
        get() = getApi(EvrenPreferences.DEFAULT_BASE_URL)
}
