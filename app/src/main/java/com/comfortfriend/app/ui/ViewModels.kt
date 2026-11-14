package com.comfortfriend.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.comfortfriend.app.AppContainer
import com.comfortfriend.app.data.db.ConversationEntity
import com.comfortfriend.app.data.db.MemoryEntity
import com.comfortfriend.app.data.db.MemoryKind
import com.comfortfriend.app.data.db.MessageEntity
import com.comfortfriend.app.data.db.MoodEntity
import com.comfortfriend.app.data.db.ObservationEntity
import com.comfortfriend.app.data.prefs.AppSettings
import com.comfortfriend.app.data.prefs.ThemeChoice
import com.comfortfriend.app.data.remote.ChatMessageDto
import com.comfortfriend.app.data.remote.ChatRequest
import com.comfortfriend.app.data.remote.GroqError
import com.comfortfriend.app.domain.ChatMode
import com.comfortfriend.app.domain.Insight
import com.comfortfriend.app.domain.LensMessage
import com.comfortfriend.app.domain.LensMood
import com.comfortfriend.app.domain.MindLens
import com.comfortfriend.app.domain.Prompts
import com.comfortfriend.app.domain.Safety
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VMFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        MainViewModel::class.java -> MainViewModel(c)
        HomeViewModel::class.java -> HomeViewModel(c)
        ChatViewModel::class.java -> ChatViewModel(c)
        MindLensViewModel::class.java -> MindLensViewModel(c)
        MoodViewModel::class.java -> MoodViewModel(c)
        MemoryViewModel::class.java -> MemoryViewModel(c)
        SettingsViewModel::class.java -> SettingsViewModel(c)
        else -> error("Unknown ViewModel ${modelClass.name}")
    } as T
}

/** App-level state: theme and onboarding. */
class MainViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings?> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    fun finishOnboarding(name: String) = viewModelScope.launch {
        c.settings.setUserName(name)
        c.settings.setOnboarded(true)
    }
}

// ---------------------------------------------------------------- Home

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val query = MutableStateFlow("")
    val conversations: StateFlow<List<ConversationEntity>> = query.flatMapLatest { q ->
        if (q.isBlank()) c.db.conversations().observeAll() else c.db.conversations().search(q.trim())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hasKey get() = c.keys.hasKey()

    fun setQuery(q: String) { query.value = q }
    fun logMood(mood: Int) = viewModelScope.launch {
        c.db.moods().insert(MoodEntity(mood = mood, note = "", createdAt = System.currentTimeMillis()))
    }
    fun deleteConversation(id: Long) = viewModelScope.launch { c.db.conversations().delete(id) }
}

// ---------------------------------------------------------------- Chat

data class ChatUiState(
    val conversationId: Long? = null,
    val title: String = "New chat",
    val mode: ChatMode = ChatMode.AUTO,
    val remember: Boolean = true,
    val messages: List<MessageEntity> = emptyList(),
    val streaming: String? = null,
    val isStreaming: Boolean = false,
    val error: String? = null,
    val showCrisisCard: Boolean = false,
    val summaryDraft: String? = null,
    val summaryLoading: Boolean = false,
    val notice: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    private var job: Job? = null
    private var loaded = false
    private var observeJob: Job? = null

    /** Call once per screen. [conversationId] = 0 starts a fresh chat lazily (saved on first message). */
    fun load(conversationId: Long, initialMode: ChatMode) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val settings = c.settings.settings.first()
            if (conversationId > 0) {
                val conv = c.db.conversations().get(conversationId)
                if (conv != null) { attach(conv); return@launch }
            }
            _state.update { it.copy(mode = initialMode, remember = settings.memoryEnabled) }
        }
    }

    private fun attach(conv: ConversationEntity) {
        _state.update { it.copy(conversationId = conv.id, title = conv.title, mode = ChatMode.fromName(conv.mode), remember = conv.remember) }
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            c.db.messages().observe(conv.id).collect { msgs -> _state.update { it.copy(messages = msgs) } }
        }
    }

    private suspend fun ensureConversation(firstText: String): Long {
        _state.value.conversationId?.let { return it }
        val now = System.currentTimeMillis()
        val s = _state.value
        val conv = ConversationEntity(
            title = firstText.trim().replace('\n', ' ').take(40).ifBlank { "New chat" },
            mode = s.mode.name, remember = s.remember, createdAt = now, updatedAt = now,
        )
        val id = c.db.conversations().insert(conv)
        attach(conv.copy(id = id))
        return id
    }

    fun setMode(mode: ChatMode) {
        _state.update { it.copy(mode = mode) }
        viewModelScope.launch {
            val id = _state.value.conversationId ?: return@launch
            c.db.conversations().get(id)?.let { c.db.conversations().update(it.copy(mode = mode.name)) }
        }
    }

    fun setRemember(v: Boolean) {
        _state.update { it.copy(remember = v) }
        viewModelScope.launch {
            val id = _state.value.conversationId ?: return@launch
            c.db.conversations().get(id)?.let { c.db.conversations().update(it.copy(remember = v)) }
        }
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || _state.value.isStreaming) return
        viewModelScope.launch {
            val id = ensureConversation(clean)
            c.db.messages().insert(MessageEntity(conversationId = id, role = "user", content = clean, createdAt = System.currentTimeMillis()))
            if (Safety.isCrisis(clean)) _state.update { it.copy(showCrisisCard = true) }
            generate(id)
        }
    }

    fun regenerate() {
        if (_state.value.isStreaming) return
        viewModelScope.launch {
            val id = _state.value.conversationId ?: return@launch
            val msgs = c.db.messages().forConversation(id)
            val last = msgs.lastOrNull() ?: return@launch
            if (last.role == "assistant") c.db.messages().delete(last)
            generate(id)
        }
    }

    fun retry() {
        if (_state.value.isStreaming) return
        viewModelScope.launch { _state.value.conversationId?.let { generate(it) } }
    }

    fun stop() { job?.cancel() }

    private fun generate(conversationId: Long) {
        _state.update { it.copy(isStreaming = true, streaming = "", error = null) }
        job = viewModelScope.launch {
            val settings = c.settings.settings.first()
            val history = c.db.messages().forConversation(conversationId)
            val memories = if (settings.memoryEnabled) c.db.memories().all() else emptyList()
            val lastUser = history.lastOrNull { it.role == "user" }?.content.orEmpty()
            val crisis = Safety.isCrisis(lastUser)
            val msgs = Prompts.buildMessages(_state.value.mode, settings.userName, memories, history, crisis)
            val req = ChatRequest(settings.model, msgs, settings.temperature.toDouble(), settings.maxTokens, stream = true)
            val sb = StringBuilder()
            var failure: String? = null
            try {
                c.groq.streamChat(req).collect { piece ->
                    sb.append(piece)
                    _state.update { it.copy(streaming = sb.toString()) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // user pressed stop: keep what we have
            } catch (e: GroqError) {
                failure = e.message
            } catch (e: Exception) {
                failure = "Something went wrong. Please try again."
            }
            // Must complete even if the user pressed Stop, so partial replies are kept and the UI resets.
            withContext(NonCancellable) {
                val finalText = sb.toString().trim()
                if (finalText.isNotEmpty()) {
                    c.db.messages().insert(MessageEntity(conversationId = conversationId, role = "assistant", content = finalText, createdAt = System.currentTimeMillis()))
                }
                c.db.conversations().get(conversationId)?.let { c.db.conversations().update(it.copy(updatedAt = System.currentTimeMillis())) }
                _state.update { it.copy(isStreaming = false, streaming = null, error = failure) }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
    fun dismissCrisis() = _state.update { it.copy(showCrisisCard = false) }
    fun dismissNotice() = _state.update { it.copy(notice = null) }

    /** User explicitly chose to remember a message. */
    fun rememberMessage(text: String) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        c.db.memories().insert(MemoryEntity(kind = MemoryKind.CONTEXT, text = text.trim().take(400), createdAt = now, updatedAt = now))
        _state.update { it.copy(notice = "Saved to your memory. You can edit or delete it any time.") }
    }

    /** Asks the model for a short summary, shown for the user to edit and approve. Nothing is saved automatically. */
    fun requestSummary() {
        val id = _state.value.conversationId ?: return
        _state.update { it.copy(summaryLoading = true) }
        viewModelScope.launch {
            val settings = c.settings.settings.first()
            val history = c.db.messages().forConversation(id).takeLast(Prompts.MAX_HISTORY_MESSAGES)
            val msgs = buildList {
                add(ChatMessageDto("system", Prompts.SUMMARY_INSTRUCTION))
                history.forEach { add(ChatMessageDto(it.role, it.content)) }
            }
            val req = ChatRequest(settings.model, msgs, 0.3, 200, stream = true)
            val sb = StringBuilder()
            try {
                c.groq.streamChat(req).collect { sb.append(it) }
                _state.update { it.copy(summaryDraft = sb.toString().trim(), summaryLoading = false) }
            } catch (e: GroqError) {
                _state.update { it.copy(summaryLoading = false, error = e.message) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.update { it.copy(summaryLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(summaryLoading = false, error = "Couldn't create a summary.") }
            }
        }
    }

    fun saveSummary(text: String) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        if (text.isNotBlank()) c.db.memories().insert(MemoryEntity(kind = MemoryKind.SUMMARY, text = text.trim(), createdAt = now, updatedAt = now))
        _state.update { it.copy(summaryDraft = null, notice = if (text.isNotBlank()) "Summary saved to memory." else null) }
    }

    fun discardSummary() = _state.update { it.copy(summaryDraft = null) }

    fun deleteConversation(onDone: () -> Unit) = viewModelScope.launch {
        _state.value.conversationId?.let { c.db.conversations().delete(it) }
        onDone()
    }
}

// ---------------------------------------------------------------- MindLens

data class MindLensUi(
    val enabled: Boolean = true,
    val insights: List<Insight> = emptyList(),
    val moods: List<MoodEntity> = emptyList(),
    val saved: List<ObservationEntity> = emptyList(),
    val messageCount: Int = 0,
)

class MindLensViewModel(private val c: AppContainer) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    val state: StateFlow<MindLensUi> = kotlinx.coroutines.flow.combine(
        c.settings.settings, c.db.moods().observeAll(), c.db.observations().observeAll(), refresh,
    ) { s, moods, saved, _ ->
        if (!s.mindLensEnabled || !s.memoryEnabled) {
            MindLensUi(enabled = false, moods = moods, saved = saved)
        } else {
            val msgs = c.db.messages().rememberedUserMessages().map { LensMessage(it.content, it.createdAt) }
            val insights = MindLens.analyze(msgs, moods.map { LensMood(it.mood, it.createdAt) })
            MindLensUi(true, insights, moods, saved, msgs.size)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MindLensUi())

    fun refresh() { refresh.update { it + 1 } }
    fun save(i: Insight) = viewModelScope.launch {
        c.db.observations().insert(ObservationEntity(title = i.title, text = i.observation, createdAt = System.currentTimeMillis()))
    }
    fun deleteSaved(id: Long) = viewModelScope.launch { c.db.observations().delete(id) }
    fun eraseInsights() = viewModelScope.launch { c.data.clearInsights() }
}

// ---------------------------------------------------------------- Mood

class MoodViewModel(private val c: AppContainer) : ViewModel() {
    val moods: StateFlow<List<MoodEntity>> = c.db.moods().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun add(mood: Int, note: String) = viewModelScope.launch {
        c.db.moods().insert(MoodEntity(mood = mood, note = note.trim(), createdAt = System.currentTimeMillis()))
    }
    fun delete(id: Long) = viewModelScope.launch { c.db.moods().delete(id) }
}

// ---------------------------------------------------------------- Memory

class MemoryViewModel(private val c: AppContainer) : ViewModel() {
    val memories: StateFlow<List<MemoryEntity>> = c.db.memories().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings: StateFlow<AppSettings> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    fun add(kind: String, text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        val now = System.currentTimeMillis()
        c.db.memories().insert(MemoryEntity(kind = kind, text = text.trim(), createdAt = now, updatedAt = now))
    }
    fun edit(m: MemoryEntity, kind: String, text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        c.db.memories().update(m.copy(kind = kind, text = text.trim(), updatedAt = System.currentTimeMillis()))
    }
    fun delete(id: Long) = viewModelScope.launch { c.db.memories().delete(id) }
    fun setMemoryEnabled(v: Boolean) = viewModelScope.launch { c.settings.setMemoryEnabled(v) }
    fun setMindLensEnabled(v: Boolean) = viewModelScope.launch { c.settings.setMindLensEnabled(v) }
}

// ---------------------------------------------------------------- Settings

data class AiConfigUi(
    val hasKey: Boolean = false,
    val testing: Boolean = false,
    val models: List<String> = emptyList(),
    val message: String? = null,
    val messageIsError: Boolean = false,
    val exportJson: String? = null,
)

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    private val _ai = MutableStateFlow(AiConfigUi(hasKey = c.keys.hasKey()))
    val ai: StateFlow<AiConfigUi> = _ai

    /** Tests a key (typed or stored) and, if it works, saves it and loads the model list. */
    fun testAndSaveKey(typedKey: String) {
        val key = typedKey.trim()
        _ai.update { it.copy(testing = true, message = null) }
        viewModelScope.launch {
            try {
                val models = c.groq.listModels(key.ifEmpty { null })
                if (key.isNotEmpty()) c.keys.save(key)
                val current = c.settings.settings.first().model
                if (current !in models && models.isNotEmpty()) {
                    c.settings.setModel(models.firstOrNull { it == AppSettings.DEFAULT_MODEL } ?: models.first())
                }
                _ai.update { it.copy(hasKey = c.keys.hasKey(), testing = false, models = models, message = "Connected. ${models.size} models available.", messageIsError = false) }
            } catch (e: GroqError) {
                _ai.update { it.copy(testing = false, message = e.message, messageIsError = true) }
            } catch (e: Exception) {
                _ai.update { it.copy(testing = false, message = "Couldn't test the connection.", messageIsError = true) }
            }
        }
    }

    fun loadModels() {
        if (!c.keys.hasKey()) return
        viewModelScope.launch {
            try {
                val models = c.groq.listModels()
                _ai.update { it.copy(models = models) }
            } catch (e: GroqError) {
                _ai.update { it.copy(message = e.message, messageIsError = true) }
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun removeKey() {
        c.keys.clear()
        _ai.update { AiConfigUi(hasKey = false, message = "API key removed from this device.", messageIsError = false) }
    }

    fun setModel(m: String) = viewModelScope.launch { c.settings.setModel(m) }
    fun setTemperature(v: Float) = viewModelScope.launch { c.settings.setTemperature(v) }
    fun setMaxTokens(v: Int) = viewModelScope.launch { c.settings.setMaxTokens(v) }
    fun setTheme(t: ThemeChoice) = viewModelScope.launch { c.settings.setTheme(t) }
    fun setName(n: String) = viewModelScope.launch { c.settings.setUserName(n) }
    fun setMemoryEnabled(v: Boolean) = viewModelScope.launch { c.settings.setMemoryEnabled(v) }
    fun setMindLensEnabled(v: Boolean) = viewModelScope.launch { c.settings.setMindLensEnabled(v) }

    fun prepareExport() = viewModelScope.launch { _ai.update { it.copy(exportJson = c.data.exportJson()) } }
    fun exportConsumed() = _ai.update { it.copy(exportJson = null) }

    fun clearConversations() = viewModelScope.launch { c.data.clearConversations(); note("All conversations deleted.") }
    fun clearMemories() = viewModelScope.launch { c.data.clearMemories(); note("All memories deleted.") }
    fun clearMoods() = viewModelScope.launch { c.data.clearMoods(); note("Mood journal cleared.") }
    fun clearInsights() = viewModelScope.launch { c.data.clearInsights(); note("MindLens data erased.") }
    fun clearEverything() = viewModelScope.launch { c.data.clearEverything(); note("All local data deleted.") }
    private fun note(m: String) = _ai.update { it.copy(message = m, messageIsError = false) }
    fun dismissMessage() = _ai.update { it.copy(message = null) }
}
