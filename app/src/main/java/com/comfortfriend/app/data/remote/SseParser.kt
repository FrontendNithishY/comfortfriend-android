package com.comfortfriend.app.data.remote

import kotlinx.serialization.json.Json

sealed interface SseEvent {
    data class Content(val text: String) : SseEvent
    object Done : SseEvent
    object Ignore : SseEvent
}

/** Parses one line of an OpenAI-compatible server-sent-events stream. Pure and unit-tested. */
object SseParser {
    fun parseLine(line: String, json: Json): SseEvent {
        if (!line.startsWith("data:")) return SseEvent.Ignore
        val payload = line.removePrefix("data:").trim()
        if (payload.isEmpty()) return SseEvent.Ignore
        if (payload == "[DONE]") return SseEvent.Done
        return try {
            val chunk = json.decodeFromString(StreamChunk.serializer(), payload)
            val text = chunk.choices.firstOrNull()?.delta?.content
            if (text.isNullOrEmpty()) SseEvent.Ignore else SseEvent.Content(text)
        } catch (e: Exception) {
            SseEvent.Ignore
        }
    }
}
