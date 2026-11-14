package com.comfortfriend.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val mode: String,
    /** True when the user allows memory and MindLens to use this conversation. */
    val remember: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("conversationId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    /** "user" or "assistant" */
    val role: String,
    val content: String,
    val createdAt: Long,
)

@Serializable
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** PREFERENCE, CONTEXT, GOAL, SUMMARY or PATTERN */
    val kind: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(tableName = "moods")
data class MoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 1 (very low) to 5 (great) */
    val mood: Int,
    val note: String,
    val createdAt: Long,
)

@Serializable
@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val text: String,
    val createdAt: Long,
)

object MemoryKind {
    const val PREFERENCE = "PREFERENCE"
    const val CONTEXT = "CONTEXT"
    const val GOAL = "GOAL"
    const val SUMMARY = "SUMMARY"
    const val PATTERN = "PATTERN"
    val all = listOf(PREFERENCE, CONTEXT, GOAL, SUMMARY, PATTERN)

    fun label(kind: String) = when (kind) {
        PREFERENCE -> "Preference"
        CONTEXT -> "Personal context"
        GOAL -> "Goal"
        SUMMARY -> "Conversation summary"
        PATTERN -> "Pattern I agreed with"
        else -> kind
    }
}
