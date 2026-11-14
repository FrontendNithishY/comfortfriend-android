package com.comfortfriend.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(val role: String, val content: String)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int,
    val stream: Boolean,
)

@Serializable
data class StreamChunk(val choices: List<StreamChoice> = emptyList())

@Serializable
data class StreamChoice(val delta: Delta? = null)

@Serializable
data class Delta(val content: String? = null)

@Serializable
data class ModelsResponse(val data: List<ModelDto> = emptyList())

@Serializable
data class ModelDto(val id: String, val active: Boolean? = null)

/** Typed failures so the UI can show friendly, actionable messages. Never contains the key. */
sealed class GroqError(message: String) : Exception(message) {
    object NoKey : GroqError("Add your Groq API key in Settings to start chatting.")
    object InvalidKey : GroqError("Groq rejected this API key. Check it and try again.")
    class RateLimited(val retryAfterSeconds: Long?) :
        GroqError("Groq is rate limiting requests right now. Give it a moment and try again.")
    class Server(val code: Int) : GroqError("Groq had a server problem (HTTP $code). Try again shortly.")
    class Client(val code: Int, detail: String) : GroqError("Request problem (HTTP $code). $detail")
    object Offline : GroqError("You appear to be offline. Your chats are saved; try again when you're connected.")
    object Network : GroqError("Couldn't reach Groq. Check your connection and try again.")
}
