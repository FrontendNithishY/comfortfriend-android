package com.comfortfriend.app.data

import com.comfortfriend.app.data.db.AppDatabase
import com.comfortfriend.app.data.db.ConversationEntity
import com.comfortfriend.app.data.db.MemoryEntity
import com.comfortfriend.app.data.db.MessageEntity
import com.comfortfriend.app.data.db.MoodEntity
import com.comfortfriend.app.data.db.ObservationEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ExportBundle(
    val app: String,
    val exportedAt: Long,
    val note: String,
    val conversations: List<ConversationEntity>,
    val messages: List<MessageEntity>,
    val memories: List<MemoryEntity>,
    val moods: List<MoodEntity>,
    val observations: List<ObservationEntity>,
)

/** Export and erase of everything stored locally. The API key is never included in exports. */
class DataManager(private val db: AppDatabase, private val json: Json) {

    suspend fun exportJson(now: Long = System.currentTimeMillis()): String {
        val bundle = ExportBundle(
            app = "ComfortFriend",
            exportedAt = now,
            note = "Your personal data from ComfortFriend. It contains private conversations; store it somewhere safe. The API key is never exported.",
            conversations = db.conversations().all(),
            messages = db.messages().all(),
            memories = db.memories().all(),
            moods = db.moods().all(),
            observations = db.observations().all(),
        )
        return json.encodeToString(ExportBundle.serializer(), bundle)
    }

    suspend fun clearConversations() = db.conversations().deleteAll() // messages cascade

    suspend fun clearMemories() = db.memories().deleteAll()

    suspend fun clearMoods() = db.moods().deleteAll()

    suspend fun clearInsights() = db.observations().deleteAll()

    suspend fun clearEverything() {
        clearConversations(); clearMemories(); clearMoods(); clearInsights()
    }
}
