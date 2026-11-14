package com.comfortfriend.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comfortfriend.app.AppContainer
import com.comfortfriend.app.data.db.MessageEntity
import com.comfortfriend.app.domain.ChatMode
import com.comfortfriend.app.domain.Safety
import com.comfortfriend.app.ui.theme.DeepPurple
import com.comfortfriend.app.ui.theme.GradientBackground
import com.comfortfriend.app.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(container: AppContainer, conversationId: Long, initialMode: String, onBack: () -> Unit, onNewChat: () -> Unit) {
    val vm: ChatViewModel = viewModel(factory = VMFactory(container))
    LaunchedEffect(conversationId) { vm.load(conversationId, ChatMode.fromName(initialMode)) }
    val st by vm.state.collectAsState()
    val settings by container.settings.settings.collectAsState(initial = com.comfortfriend.app.data.prefs.AppSettings())
    val snack = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(st.notice) { st.notice?.let { snack.showSnackbar(it); vm.dismissNotice() } }
    LaunchedEffect(st.messages.size, st.streaming, st.isStreaming) {
        val extra = if (st.isStreaming) 1 else 0
        val count = st.messages.size + extra
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snack) },
            topBar = {
                TopAppBar(
                    title = { Text(st.title, maxLines = 1, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                    actions = {
                        TextButton(onClick = onNewChat) { Text("New chat") }
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More options") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (st.remember) "Remembering this chat ✓" else "Don't remember this chat ✓") },
                                onClick = { vm.setRemember(!st.remember); menu = false },
                            )
                            if (settings.memoryEnabled) {
                                DropdownMenuItem(
                                    text = { Text("Save a summary to memory") },
                                    enabled = st.conversationId != null && st.messages.size >= 2 && !st.summaryLoading,
                                    onClick = { vm.requestSummary(); menu = false },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete this chat") },
                                enabled = st.conversationId != null,
                                onClick = { confirmDelete = true; menu = false },
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(top = pad.calculateTopPadding()).imePadding()) {
                // Mode chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ChatMode.entries) { m ->
                        FilterChip(
                            selected = st.mode == m,
                            onClick = { vm.setMode(m) },
                            label = { Text("${m.emoji} ${m.label}") },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                if (st.showCrisisCard) CrisisCard(onDismiss = vm::dismissCrisis)

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (st.messages.isEmpty() && !st.isStreaming) {
                        EmptyChat(st.mode, onPick = { input = it })
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val lastAssistantId = st.messages.lastOrNull { it.role == "assistant" }?.id
                            items(st.messages, key = { it.id }) { m ->
                                Bubble(
                                    m = m,
                                    canRegenerate = m.id == lastAssistantId && !st.isStreaming && st.messages.last().id == m.id,
                                    canRemember = settings.memoryEnabled,
                                    onCopy = { clipboard.setText(AnnotatedString(m.content)) },
                                    onRegenerate = vm::regenerate,
                                    onRemember = { vm.rememberMessage(m.content) },
                                )
                            }
                            if (st.isStreaming) {
                                item(key = "streaming") {
                                    val text = st.streaming.orEmpty()
                                    if (text.isEmpty()) TypingIndicator() else AssistantBubble(text)
                                }
                            }
                        }
                    }
                }

                st.error?.let { err ->
                    SoftCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Column {
                            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            Row {
                                TextButton(onClick = vm::retry) { Text("Try again") }
                                TextButton(onClick = vm::dismissError) { Text("Dismiss") }
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Say what's on your mind…") },
                        shape = RoundedCornerShape(26.dp),
                        maxLines = 5,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier.size(52.dp).background(Brush.linearGradient(listOf(DeepPurple, Violet)), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (st.isStreaming) {
                            IconButton(onClick = vm::stop) { Icon(Icons.Filled.Close, contentDescription = "Stop", tint = Color.White) }
                        } else {
                            IconButton(onClick = { vm.send(input); input = "" }, enabled = input.isNotBlank()) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    st.summaryDraft?.let { draft ->
        var text by remember(draft) { mutableStateOf(draft) }
        AlertDialog(
            onDismissRequest = vm::discardSummary,
            title = { Text("Save to memory?") },
            text = {
                Column {
                    Text("Edit this however you like. It's only saved if you tap Save.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { vm.saveSummary(text) }) { Text("Save") } },
            dismissButton = { TextButton(onClick = vm::discardSummary) { Text("Discard") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this chat?") },
            text = { Text("The conversation will be removed from this phone. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.deleteConversation(onBack) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EmptyChat(mode: ChatMode, onPick: (String) -> Unit) {
    val starters = when (mode) {
        ChatMode.STUDY -> listOf("I can't focus on studying", "I'm overthinking tomorrow's exam")
        ChatMode.DECIDE -> listOf("I'm stuck between two options", "Help me decide what to do next")
        ChatMode.OVERTHINK -> listOf("My mind won't stop spiralling", "I keep replaying something I said")
        ChatMode.LAUGH -> listOf("Cheer me up, I had a rough day", "Tell me something silly")
        else -> listOf("I'm overthinking something", "I had a rough day", "I just need to vent")
    }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(mode.emoji, fontSize = 44.sp)
        Spacer(Modifier.height(8.dp))
        Text(mode.label, style = MaterialTheme.typography.headlineSmall)
        Hint(mode.blurb)
        Spacer(Modifier.height(16.dp))
        starters.forEach { s ->
            TextButton(onClick = { onPick(s) }) { Text("“$s”") }
        }
    }
}

@Composable
private fun Bubble(
    m: MessageEntity,
    canRegenerate: Boolean,
    canRemember: Boolean,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onRemember: () -> Unit,
) {
    if (m.role == "user") {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Box(
                Modifier.widthIn(max = 320.dp)
                    .background(Brush.linearGradient(listOf(DeepPurple, Violet)), RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) { Text(m.content, color = Color.White, style = MaterialTheme.typography.bodyLarge) }
            Row {
                TextButton(onClick = onCopy, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Copy", fontSize = 12.sp) }
                if (canRemember) TextButton(onClick = onRemember, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Remember this", fontSize = 12.sp) }
            }
        }
    } else {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            AssistantBubble(m.content)
            Row {
                TextButton(onClick = onCopy, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Copy", fontSize = 12.sp) }
                if (canRegenerate) TextButton(onClick = onRegenerate, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Regenerate", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun AssistantBubble(text: String) {
    Box(
        Modifier.widthIn(max = 330.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) { Text(text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge) }
}

@Composable
private fun TypingIndicator() {
    val t = rememberInfiniteTransition(label = "typing")
    Row(
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(3) { i ->
            val a by t.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600, delayMillis = i * 150), RepeatMode.Reverse), label = "dot$i",
            )
            Box(Modifier.size(8.dp).alpha(a).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

@Composable
private fun CrisisCard(onDismiss: () -> Unit) {
    SoftCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column {
            Text(Safety.CRISIS_CARD_TITLE, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(Safety.CRISIS_CARD_BODY, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDismiss) { Text("Okay, close this") }
        }
    }
}
