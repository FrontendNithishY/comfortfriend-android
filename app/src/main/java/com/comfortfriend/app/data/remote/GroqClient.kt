package com.comfortfriend.app.data.remote

import com.comfortfriend.app.data.secure.SecureKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Header
import java.io.IOException
import java.util.concurrent.TimeUnit

interface GroqApi {
    @GET("models")
    suspend fun models(@Header("Authorization") bearer: String): ModelsResponse
}

class GroqClient(
    private val http: OkHttpClient,
    private val api: GroqApi,
    private val json: Json,
    private val keys: SecureKeyStore,
    private val isOnline: () -> Boolean,
) {
    /** Lists chat-capable models. Pass [candidateKey] to test a key before saving it. */
    suspend fun listModels(candidateKey: String? = null): List<String> {
        val key = candidateKey?.trim()?.takeIf { it.isNotEmpty() } ?: keys.get() ?: throw GroqError.NoKey
        if (!isOnline()) throw GroqError.Offline
        try {
            val res = api.models("Bearer $key")
            return res.data
                .filter { it.active != false }
                .map { it.id }
                .filterNot { id -> NON_CHAT.any { id.contains(it, ignoreCase = true) } }
                .sorted()
        } catch (e: HttpException) {
            throw mapHttp(e.code(), e.response()?.headers()?.get("Retry-After"), "")
        } catch (e: IOException) {
            throw GroqError.Network
        }
    }

    /**
     * Streams assistant text. Retries (max [MAX_RETRIES]) on rate limits, 5xx and network
     * errors, but only if no text has been emitted yet, so partial replies are never duplicated.
     */
    fun streamChat(request: ChatRequest): Flow<String> = flow {
        val key = keys.get() ?: throw GroqError.NoKey
        if (!isOnline()) throw GroqError.Offline
        val body = json.encodeToString(ChatRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url(CHAT_URL)
            .header("Authorization", "Bearer $key")
            .header("Accept", "text/event-stream")
            .post(body)
            .build()

        var attempt = 0
        while (true) {
            var emitted = false
            try {
                val call = http.newCall(req)
                currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
                val response: Response = call.execute()
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        val detail = if (resp.code in 400..499) extractDetail(resp) else ""
                        throw mapHttp(resp.code, resp.header("Retry-After"), detail)
                    }
                    val source = resp.body?.source() ?: throw GroqError.Network
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val line = source.readUtf8Line() ?: break
                        when (val ev = SseParser.parseLine(line, json)) {
                            is SseEvent.Content -> { emitted = true; emit(ev.text) }
                            SseEvent.Done -> return@flow
                            SseEvent.Ignore -> Unit
                        }
                    }
                }
                return@flow
            } catch (e: GroqError) {
                if (!emitted && attempt < MAX_RETRIES && isRetryable(e)) {
                    delay(backoffMillis(attempt, e))
                    attempt++
                    continue
                }
                throw e
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                if (!emitted && attempt < MAX_RETRIES) {
                    delay(backoffMillis(attempt, null))
                    attempt++
                    continue
                }
                throw if (emitted) GroqError.Network else GroqError.Network
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun isRetryable(e: GroqError) = e is GroqError.RateLimited || e is GroqError.Server

    private fun backoffMillis(attempt: Int, e: GroqError?): Long {
        val hinted = (e as? GroqError.RateLimited)?.retryAfterSeconds?.times(1000)
        return (hinted ?: (1000L shl attempt)).coerceIn(500L, 8000L)
    }

    private fun extractDetail(resp: Response): String = try {
        val text = resp.peekBody(2048).string()
        val m = Regex("\"message\"\\s*:\\s*\"([^\"]{1,200})").find(text)
        m?.groupValues?.get(1) ?: ""
    } catch (e: Exception) { "" }

    private fun mapHttp(code: Int, retryAfter: String?, detail: String): GroqError = when (code) {
        401, 403 -> GroqError.InvalidKey
        429 -> GroqError.RateLimited(retryAfter?.toLongOrNull())
        in 500..599 -> GroqError.Server(code)
        else -> GroqError.Client(code, detail)
    }

    companion object {
        const val BASE_URL = "https://api.groq.com/openai/v1/"
        const val CHAT_URL = "https://api.groq.com/openai/v1/chat/completions"
        const val MAX_RETRIES = 2
        private val NON_CHAT = listOf("whisper", "tts", "playai", "orpheus", "guard", "embed", "distil-whisper")

        fun buildHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build() // deliberately no logging interceptor: keys and chats must never be logged
    }
}
