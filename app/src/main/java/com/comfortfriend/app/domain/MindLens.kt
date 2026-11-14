package com.comfortfriend.app.domain

import java.util.Calendar

/** A single user message the person allowed ComfortFriend to remember. */
data class LensMessage(val text: String, val timeMillis: Long)

/** A mood entry on a 1..5 scale. */
data class LensMood(val mood: Int, val timeMillis: Long)

enum class InsightKind { TOPIC, WORRY, SELF_TALK, AVOIDANCE, MOOD, TRIGGER }

data class Insight(
    val kind: InsightKind,
    val title: String,
    /** Always phrased as a tentative observation, never as a fact. */
    val observation: String,
    /** A gentle alternative way of looking at it, or a reflection prompt. */
    val reflection: String,
)

/**
 * MindLens runs entirely on-device over the data the person allowed. It uses transparent keyword
 * rules (no hidden profiling, no network). Everything it says is an observation to be checked
 * by the person, never a diagnosis, a certainty about feelings, or a prediction.
 */
object MindLens {
    private val topics: Map<String, List<String>> = mapOf(
        "exams and studying" to listOf("exam", "test", "study", "studying", "marks", "grades", "assignment", "revision"),
        "work or career" to listOf("work", "job", "boss", "career", "interview", "deadline", "colleague"),
        "friends and relationships" to listOf("friend", "girlfriend", "boyfriend", "partner", "relationship", "crush", "breakup", "ex "),
        "family" to listOf("mom", "dad", "mother", "father", "parents", "family", "brother", "sister"),
        "sleep and energy" to listOf("sleep", "tired", "insomnia", "exhausted", "can't sleep", "cant sleep"),
        "money" to listOf("money", "fees", "salary", "debt", "rent", "afford"),
        "the future" to listOf("future", "career path", "what will happen", "life plan", "purpose"),
        "how I measure up" to listOf("better than me", "compare", "not good enough", "behind everyone", "left behind"),
    )

    private val worryWords = listOf("what if", "worried", "worry", "anxious", "scared", "afraid", "nervous", "overthink", "panic", "stress")
    private val selfTalk = listOf(
        "i'm stupid", "im stupid", "i am stupid", "i'm useless", "im useless", "i am useless", "i'm a failure", "i am a failure",
        "i'm not good enough", "im not good enough", "i am not good enough", "i can't do anything", "i cant do anything",
        "i always mess", "i always fail", "i never get", "everyone is better than me", "i'm worthless", "i am worthless",
        "i hate myself", "i'm dumb", "im dumb",
    )
    private val avoidance = listOf(
        "avoid", "put off", "putting off", "procrastinat", "didn't want to", "didnt want to", "skipped", "skipping",
        "can't face", "cant face", "ignoring", "hiding from",
    )

    const val MIN_EVIDENCE = 3

    fun analyze(messages: List<LensMessage>, moods: List<LensMood>, nowMillis: Long = System.currentTimeMillis()): List<Insight> {
        val out = mutableListOf<Insight>()
        val lower = messages.map { it.text.lowercase().replace('’', '\'') to it.timeMillis }

        // Recurring topics
        val topicCounts = topics.mapValues { (_, words) -> lower.count { (t, _) -> words.any { t.contains(it) } } }
        topicCounts.filter { it.value >= MIN_EVIDENCE }.entries.sortedByDescending { it.value }.take(3).forEach { (topic, n) ->
            out += Insight(
                InsightKind.TOPIC,
                "Coming up often: $topic",
                "In $n of the messages you let me remember, $topic came up. I might be reading too much into it, but it could be taking up space in your mind lately.",
                "Does that match how it feels? What part of it is weighing on you most?",
            )
        }

        // Repeated worries
        val worryCount = lower.count { (t, _) -> worryWords.any { t.contains(it) } }
        if (worryCount >= MIN_EVIDENCE) {
            val examLinked = lower.count { (t, _) -> worryWords.any { t.contains(it) } && topics.getValue("exams and studying").any { t.contains(it) } }
            val extra = if (examLinked >= 2) " It seems to show up around exams and studying: sometimes several 'what if' outcomes appear before anything has actually happened. Could that be happening again?" else ""
            out += Insight(
                InsightKind.WORRY,
                "Worry shows up a fair bit",
                "Worry-type words appeared in $worryCount of your messages.$extra",
                "Try sorting a current worry into facts, assumptions and fears. Which bucket does it mostly land in?",
            )
        }

        // Negative self-talk
        val selfTalkCount = lower.count { (t, _) -> selfTalk.any { t.contains(it) } }
        if (selfTalkCount >= 2) {
            out += Insight(
                InsightKind.SELF_TALK,
                "Harsh words toward yourself",
                "I noticed $selfTalkCount messages where you described yourself quite harshly. I can't know what's behind that, and it's only based on your wording.",
                "If a friend said this about themselves, what would you tell them? Is there one piece of evidence that doesn't fit the harsh version?",
            )
        }

        // Avoidance
        val avoidCount = lower.count { (t, _) -> avoidance.any { t.contains(it) } }
        if (avoidCount >= 2) {
            out += Insight(
                InsightKind.AVOIDANCE,
                "Possible avoiding or putting off",
                "A few messages mention avoiding or putting things off ($avoidCount times). It might be a way of coping with something that feels heavy, or it might just be a busy patch.",
                "Is there one tiny first step (two minutes) that would make the thing feel less big?",
            )
        }

        // Mood trend
        moodTrend(moods, nowMillis)?.let { out += it }

        // Possible triggers: topics on low-mood days
        out += triggers(lower, moods)

        return out
    }

    private fun dayKey(ms: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ms; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        return c.timeInMillis
    }

    internal fun moodTrend(moods: List<LensMood>, now: Long): Insight? {
        val day = 24L * 60 * 60 * 1000
        val recent = moods.filter { it.timeMillis >= now - 7 * day }
        val before = moods.filter { it.timeMillis < now - 7 * day && it.timeMillis >= now - 14 * day }
        if (recent.size < 2 || before.size < 2) return null
        val diff = recent.map { it.mood }.average() - before.map { it.mood }.average()
        return when {
            diff <= -0.7 -> Insight(
                InsightKind.MOOD, "Mood has dipped this week",
                "Your logged mood averages a bit lower this week than the week before. That's only based on the check-ins you chose to log.",
                "Has anything changed recently: sleep, workload, people? Small things count too.",
            )
            diff >= 0.7 -> Insight(
                InsightKind.MOOD, "Mood has lifted this week",
                "Your logged mood averages higher this week than the week before. Nice.",
                "What do you think helped? It might be worth repeating.",
            )
            else -> null
        }
    }

    private fun triggers(lower: List<Pair<String, Long>>, moods: List<LensMood>): List<Insight> {
        val lowDays = moods.filter { it.mood <= 2 }.map { dayKey(it.timeMillis) }.toSet()
        if (lowDays.size < 2) return emptyList()
        val msgsOnLowDays = lower.filter { (_, ms) -> dayKey(ms) in lowDays }
        if (msgsOnLowDays.size < 2) return emptyList()
        val hits = topics.mapValues { (_, words) ->
            msgsOnLowDays.filter { (t, _) -> words.any { t.contains(it) } }.map { dayKey(it.second) }.toSet().size
        }
        val (topic, days) = hits.maxByOrNull { it.value } ?: return emptyList()
        if (days < 2) return emptyList()
        return listOf(
            Insight(
                InsightKind.TRIGGER,
                "Something that may be linked to low days",
                "On $days of your lower-mood days, $topic also came up. That doesn't mean one causes the other; it's just a pattern in what you shared.",
                "Does that ring true, or does it feel off? You're the expert on you.",
            )
        )
    }
}
