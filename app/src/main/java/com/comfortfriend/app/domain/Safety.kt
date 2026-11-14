package com.comfortfriend.app.domain

/**
 * Lightweight, on-device keyword screen used ONLY to show a crisis-support card and to tell the
 * model to switch to crisis-care behaviour. It is deliberately simple and will miss things; the
 * system prompt carries the main safety behaviour. It never diagnoses and never leaves the device.
 */
object Safety {
    private val patterns = listOf(
        "kill myself", "killing myself", "end my life", "end it all", "want to die", "wanna die",
        "don't want to live", "dont want to live", "do not want to live", "better off dead",
        "suicide", "suicidal", "take my own life", "hurt myself", "harm myself", "self harm",
        "self-harm", "cut myself", "no reason to live", "can't go on", "cant go on",
    )

    fun isCrisis(text: String): Boolean {
        val t = text.lowercase().replace('’', '\'')
        return patterns.any { t.contains(it) }
    }

    const val CRISIS_NOTE =
        "SAFETY NOTICE: The person's latest message may indicate suicidal thoughts or self-harm. " +
            "Respond with calm, warm, human compassion. No humour, no slang, no lecturing. Reflect what they said, " +
            "ask directly and gently whether they are safe right now, encourage contacting local emergency services or a crisis " +
            "line and a trusted person who can be with them, and stay with them in the conversation. Do not give methods. Keep it short."

    const val CRISIS_CARD_TITLE = "You matter, and you don't have to carry this alone"
    const val CRISIS_CARD_BODY =
        "If you might act on these thoughts or are in immediate danger, call your local emergency number now, " +
            "or go to the nearest emergency room, and try to be with someone you trust.\n\n" +
            "Helplines (please verify for your country):\n" +
            "• India: Tele-MANAS 14416\n" +
            "• US: call or text 988\n" +
            "• UK & Ireland: Samaritans 116 123\n" +
            "• Elsewhere: findahelpline.com lists services by country\n\n" +
            "ComfortFriend is a supportive companion, not a medical professional or a crisis service."
}
