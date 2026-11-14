package com.comfortfriend.app

import com.comfortfriend.app.domain.InsightKind
import com.comfortfriend.app.domain.LensMessage
import com.comfortfriend.app.domain.LensMood
import com.comfortfriend.app.domain.MindLens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MindLensTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L

    private fun msgs(vararg t: String) = t.mapIndexed { i, s -> LensMessage(s, now - i * day) }

    @Test fun needsEvidenceBeforeSayingAnything() {
        val out = MindLens.analyze(msgs("my exam is soon", "ok"), emptyList(), now)
        assertTrue(out.isEmpty())
    }

    @Test fun findsRecurringTopicAndWorryAroundExams() {
        val out = MindLens.analyze(
            msgs(
                "What if I fail the exam tomorrow",
                "I am worried about my exam results",
                "studying is so stressful, what if I forget everything",
            ),
            emptyList(), now,
        )
        assertTrue(out.any { it.kind == InsightKind.TOPIC && it.title.contains("exams") })
        val worry = out.first { it.kind == InsightKind.WORRY }
        assertTrue(worry.observation.contains("exam", ignoreCase = true))
    }

    @Test fun observationsAreTentativeNotCertain() {
        val out = MindLens.analyze(msgs("I'm useless", "i am a failure", "ok"), emptyList(), now)
        val st = out.first { it.kind == InsightKind.SELF_TALK }
        assertTrue(st.observation.contains("can't know") || st.observation.contains("only based"))
        assertFalse(out.any { it.observation.contains("you will", ignoreCase = true) })
    }

    @Test fun detectsMoodDip() {
        val moods = listOf(
            LensMood(4, now - 10 * day), LensMood(5, now - 9 * day),
            LensMood(2, now - 2 * day), LensMood(2, now - 1 * day),
        )
        val out = MindLens.analyze(emptyList(), moods, now)
        assertEquals(1, out.count { it.kind == InsightKind.MOOD })
        assertTrue(out.first { it.kind == InsightKind.MOOD }.title.contains("dipped"))
    }

    @Test fun noInsightsFromNothing() {
        assertTrue(MindLens.analyze(emptyList(), emptyList(), now).isEmpty())
    }
}
