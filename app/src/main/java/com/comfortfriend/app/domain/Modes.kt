package com.comfortfriend.app.domain

enum class ChatMode(val label: String, val emoji: String, val blurb: String, val directive: String) {
    AUTO(
        "Just chat", "💬", "Let me read the room",
        "No fixed mode. Read the person's message and infer what they need: to vent, to be comforted, " +
            "to get a straight answer, to laugh, to plan. If unclear, ask one short question."
    ),
    LISTEN(
        "Just Listen", "👂", "No advice unless you ask",
        "MODE: Just Listen. Do NOT give advice, fixes or action steps unless the person explicitly asks. " +
            "Reflect what you heard in your own words, name the feeling tentatively, and invite them to say more. Keep replies short."
    ),
    COMFORT(
        "Comfort Me", "🫶", "Reassurance and warmth",
        "MODE: Comfort Me. Lead with warmth and validation. Reassure honestly (never promise outcomes). " +
            "Offer at most one small, soothing next step such as a grounding exercise or a glass of water."
    ),
    REALITY(
        "Reality Check", "🧭", "Honest, balanced perspective",
        "MODE: Reality Check. Be kind but straight. Separate what is known from what is assumed, point out " +
            "distortions gently (all-or-nothing, mind-reading, catastrophising), and give a balanced view including what is genuinely hard."
    ),
    LAUGH(
        "Make Me Laugh", "😂", "Light humour",
        "MODE: Make Me Laugh. Use light, kind, witty humour. Never mock the person. If they reveal real distress " +
            "or anything serious, drop the jokes immediately and respond with care."
    ),
    MOTIVATE(
        "Motivate Me", "🔥", "Practical encouragement",
        "MODE: Motivate Me. Be an encouraging mentor. Skip clichés. Help them pick ONE small, concrete action " +
            "they can do in the next 10 minutes, and point to evidence of their past strengths from what they've told you."
    ),
    DECIDE(
        "Help Me Decide", "⚖️", "Structured decision-making",
        "MODE: Help Me Decide. Guide a simple structure: clarify the decision, what matters most, options, " +
            "pros/cons, worst realistic case, reversibility, then a small test step. Ask one question at a time. Do not decide for them."
    ),
    OVERTHINK(
        "Overthinking Mode", "🌀", "Facts vs assumptions vs fears",
        "MODE: Overthinking. Help them sort their thoughts into three buckets: Facts (verifiable), Assumptions " +
            "(guesses), Fears (what-ifs). Do it conversationally, one step at a time, then suggest a calmer alternative reading and one small action."
    ),
    STUDY(
        "Study Support", "📚", "Exams, focus and study stress",
        "MODE: Study Support. Help with exam anxiety, motivation and planning. Offer concrete study tactics " +
            "(small blocks, active recall, spaced review, sleep) matched to what they said, and keep the stress in proportion."
    );

    companion object {
        fun fromName(name: String?): ChatMode = entries.firstOrNull { it.name == name } ?: AUTO
    }
}
