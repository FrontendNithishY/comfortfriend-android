package com.comfortfriend.app

import com.comfortfriend.app.data.db.MemoryEntity
import com.comfortfriend.app.data.db.MemoryKind
import com.comfortfriend.app.data.db.MessageEntity
import com.comfortfriend.app.domain.ChatMode
import com.comfortfriend.app.domain.Prompts
import com.comfortfriend.app.domain.Safety
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyAndPromptTest {
    @Test fun detectsCrisisPhrases() {
        assertTrue(Safety.isCrisis("I want to die"))
        assertTrue(Safety.isCrisis("i keep thinking about suicide"))
        assertTrue(Safety.isCrisis("I DON’T WANT TO LIVE anymore".replace("DON’T", "DON'T")))
    }

    @Test fun doesNotFlagOrdinaryStress() {
        assertFalse(Safety.isCrisis("I'm dying to know my exam result"))
        assertFalse(Safety.isCrisis("This exam is killing my sleep schedule"))
    }

    private fun msg(i: Int, role: String = "user", text: String = "m$i") =
        MessageEntity(id = i.toLong(), conversationId = 1, role = role, content = text, createdAt = i.toLong())

    @Test fun systemPromptIncludesModeMemoryAndName() {
        val mem = MemoryEntity(1, MemoryKind.GOAL, "Wants to pass chemistry", 0, 0)
        val out = Prompts.buildMessages(ChatMode.LISTEN, "Asha", listOf(mem), listOf(msg(1)), crisis = false)
        val sys = out.first()
        assertEquals("system", sys.role)
        assertTrue(sys.content.contains("Just Listen"))
        assertTrue(sys.content.contains("Asha"))
        assertTrue(sys.content.contains("Wants to pass chemistry"))
        assertFalse(sys.content.contains("SAFETY NOTICE"))
    }

    @Test fun crisisFlagAddsSafetyNotice() {
        val out = Prompts.buildMessages(ChatMode.LAUGH, "", emptyList(), listOf(msg(1)), crisis = true)
        assertTrue(out.first().content.contains("SAFETY NOTICE"))
    }

    @Test fun historyIsTrimmedToLimits() {
        val many = (1..100).map { msg(it, if (it % 2 == 0) "assistant" else "user") }
        val out = Prompts.buildMessages(ChatMode.AUTO, "", emptyList(), many, crisis = false)
        assertEquals(Prompts.MAX_HISTORY_MESSAGES + 1, out.size)
        assertEquals("m100", out.last().content)

        val huge = (1..10).map { msg(it, text = "x".repeat(5000)) }
        val trimmed = Prompts.buildMessages(ChatMode.AUTO, "", emptyList(), huge, crisis = false)
        val chars = trimmed.drop(1).sumOf { it.content.length }
        assertTrue(chars <= Prompts.MAX_CONTEXT_CHARS)
    }

    @Test fun noApiKeyOrSecretsInSystemPrompt() {
        assertFalse(Prompts.SYSTEM.contains("gsk_"))
    }
}
