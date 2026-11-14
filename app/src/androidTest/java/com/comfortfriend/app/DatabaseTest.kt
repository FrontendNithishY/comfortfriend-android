package com.comfortfriend.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.comfortfriend.app.data.DataManager
import com.comfortfriend.app.data.db.AppDatabase
import com.comfortfriend.app.data.db.ConversationEntity
import com.comfortfriend.app.data.db.MemoryEntity
import com.comfortfriend.app.data.db.MessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    @Test fun conversationPersistsAndSearchWorks() = runBlocking {
        val id = db.conversations().insert(ConversationEntity(title = "Exam stress", mode = "STUDY", remember = true, createdAt = 1, updatedAt = 1))
        db.messages().insert(MessageEntity(conversationId = id, role = "user", content = "I keep worrying about chemistry", createdAt = 2))
        assertEquals(1, db.messages().forConversation(id).size)
        assertEquals(1, db.conversations().search("chemistry").first().size)
        assertEquals(0, db.conversations().search("football").first().size)
    }

    @Test fun deletingConversationCascadesMessages() = runBlocking {
        val id = db.conversations().insert(ConversationEntity(title = "t", mode = "AUTO", remember = false, createdAt = 1, updatedAt = 1))
        db.messages().insert(MessageEntity(conversationId = id, role = "user", content = "hi", createdAt = 2))
        db.conversations().delete(id)
        assertTrue(db.messages().all().isEmpty())
    }

    @Test fun onlyRememberedConversationsFeedMindLens() = runBlocking {
        val a = db.conversations().insert(ConversationEntity(title = "a", mode = "AUTO", remember = true, createdAt = 1, updatedAt = 1))
        val b = db.conversations().insert(ConversationEntity(title = "b", mode = "AUTO", remember = false, createdAt = 1, updatedAt = 1))
        db.messages().insert(MessageEntity(conversationId = a, role = "user", content = "kept", createdAt = 2))
        db.messages().insert(MessageEntity(conversationId = b, role = "user", content = "private", createdAt = 3))
        val got = db.messages().rememberedUserMessages().map { it.content }
        assertEquals(listOf("kept"), got)
    }

    @Test fun exportContainsDataAndClearEverythingErasesIt() = runBlocking {
        val dm = DataManager(db, Json { prettyPrint = true })
        db.memories().insert(MemoryEntity(kind = "GOAL", text = "Pass the exam", createdAt = 1, updatedAt = 1))
        val json = dm.exportJson(now = 5)
        assertTrue(json.contains("Pass the exam"))
        assertFalse(json.contains("gsk_"))
        dm.clearEverything()
        assertTrue(db.memories().all().isEmpty())
    }
}
