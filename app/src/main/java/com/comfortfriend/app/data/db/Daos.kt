package com.comfortfriend.app.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert suspend fun insert(c: ConversationEntity): Long
    @Update suspend fun update(c: ConversationEntity)
    @Query("SELECT * FROM conversations WHERE id = :id") suspend fun get(id: Long): ConversationEntity?
    @Query("SELECT * FROM conversations WHERE id = :id") fun observe(id: Long): Flow<ConversationEntity?>
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC") fun observeAll(): Flow<List<ConversationEntity>>
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC") suspend fun all(): List<ConversationEntity>
    @Query("SELECT * FROM conversations WHERE remember = 1") suspend fun remembered(): List<ConversationEntity>
    @Query("DELETE FROM conversations WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM conversations") suspend fun deleteAll()

    @Query(
        "SELECT DISTINCT c.* FROM conversations c LEFT JOIN messages m ON m.conversationId = c.id " +
            "WHERE c.title LIKE '%' || :q || '%' OR m.content LIKE '%' || :q || '%' ORDER BY c.updatedAt DESC"
    )
    fun search(q: String): Flow<List<ConversationEntity>>
}

@Dao
interface MessageDao {
    @Insert suspend fun insert(m: MessageEntity): Long
    @Delete suspend fun delete(m: MessageEntity)
    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY createdAt ASC, id ASC")
    fun observe(cid: Long): Flow<List<MessageEntity>>
    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY createdAt ASC, id ASC")
    suspend fun forConversation(cid: Long): List<MessageEntity>
    @Query("SELECT * FROM messages ORDER BY createdAt ASC, id ASC") suspend fun all(): List<MessageEntity>
    @Query(
        "SELECT m.* FROM messages m JOIN conversations c ON c.id = m.conversationId " +
            "WHERE c.remember = 1 AND m.role = 'user' ORDER BY m.createdAt ASC"
    )
    suspend fun rememberedUserMessages(): List<MessageEntity>
}

@Dao
interface MemoryDao {
    @Insert suspend fun insert(m: MemoryEntity): Long
    @Update suspend fun update(m: MemoryEntity)
    @Query("DELETE FROM memories WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM memories") suspend fun deleteAll()
    @Query("SELECT * FROM memories ORDER BY updatedAt DESC") fun observeAll(): Flow<List<MemoryEntity>>
    @Query("SELECT * FROM memories ORDER BY updatedAt DESC") suspend fun all(): List<MemoryEntity>
}

@Dao
interface MoodDao {
    @Insert suspend fun insert(m: MoodEntity): Long
    @Query("DELETE FROM moods WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM moods") suspend fun deleteAll()
    @Query("SELECT * FROM moods ORDER BY createdAt DESC") fun observeAll(): Flow<List<MoodEntity>>
    @Query("SELECT * FROM moods ORDER BY createdAt DESC") suspend fun all(): List<MoodEntity>
}

@Dao
interface ObservationDao {
    @Insert suspend fun insert(o: ObservationEntity): Long
    @Query("DELETE FROM observations WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM observations") suspend fun deleteAll()
    @Query("SELECT * FROM observations ORDER BY createdAt DESC") fun observeAll(): Flow<List<ObservationEntity>>
    @Query("SELECT * FROM observations ORDER BY createdAt DESC") suspend fun all(): List<ObservationEntity>
}

@Database(
    entities = [
        ConversationEntity::class, MessageEntity::class, MemoryEntity::class,
        MoodEntity::class, ObservationEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversations(): ConversationDao
    abstract fun messages(): MessageDao
    abstract fun memories(): MemoryDao
    abstract fun moods(): MoodDao
    abstract fun observations(): ObservationDao
}
