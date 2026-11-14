package com.comfortfriend.app

import com.comfortfriend.app.data.remote.SseEvent
import com.comfortfriend.app.data.remote.SseParser
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SseParserTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun parsesContentDelta() {
        val ev = SseParser.parseLine("""data: {"id":"x","choices":[{"index":0,"delta":{"content":"Hey"}}]}""", json)
        assertEquals(SseEvent.Content("Hey"), ev)
    }

    @Test fun recognisesDone() {
        assertTrue(SseParser.parseLine("data: [DONE]", json) === SseEvent.Done)
    }

    @Test fun ignoresRoleOnlyAndEmptyDeltas() {
        val role = SseParser.parseLine("""data: {"choices":[{"delta":{"role":"assistant"}}]}""", json)
        val empty = SseParser.parseLine("""data: {"choices":[{"delta":{"content":""}}]}""", json)
        assertTrue(role === SseEvent.Ignore)
        assertTrue(empty === SseEvent.Ignore)
    }

    @Test fun ignoresNonDataAndMalformedLines() {
        assertTrue(SseParser.parseLine(": keep-alive", json) === SseEvent.Ignore)
        assertTrue(SseParser.parseLine("", json) === SseEvent.Ignore)
        assertTrue(SseParser.parseLine("data: {not json", json) === SseEvent.Ignore)
    }
}
