package com.comfortfriend.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comfortfriend.app.AppContainer
import com.comfortfriend.app.domain.ChatMode
import com.comfortfriend.app.ui.theme.DeepPurple
import com.comfortfriend.app.ui.theme.GradientBackground
import com.comfortfriend.app.ui.theme.Lavender
import com.comfortfriend.app.ui.theme.Violet
import java.util.Calendar

@Composable
fun WelcomeScreen(onStart: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    GradientBackground {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(112.dp).background(Brush.linearGradient(listOf(DeepPurple, Violet)), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("🌙", fontSize = 52.sp) }
            Spacer(Modifier.height(28.dp))
            Text("ComfortFriend", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))
            Text(
                "Hey, I'm glad you're here. Think of me as a friend who listens properly, notices the little things, and tells you the truth kindly.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("What should I call you? (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            GradientButton("Get started", onClick = { onStart(name) })
            Spacer(Modifier.height(20.dp))
            Text(
                "I'm an AI companion, not a doctor or therapist. Your chats stay on this phone; only what's needed for each reply is sent to Groq, the AI provider you connect with your own key.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun greeting(name: String): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val part = when {
        h < 5 -> "Still up"
        h < 12 -> "Good morning"
        h < 17 -> "Good afternoon"
        h < 22 -> "Good evening"
        else -> "Late night"
    }
    return if (name.isBlank()) "$part 👋" else "$part, $name 👋"
}

@Composable
fun HomeScreen(container: AppContainer, onNavigate: (String) -> Unit, onOpenChat: (Long, String) -> Unit) {
    val vm: HomeViewModel = viewModel(factory = VMFactory(container))
    val settings by vm.settings.collectAsState()
    val convs by vm.conversations.collectAsState()
    val query by vm.query.collectAsState()
    var checkedIn by remember { mutableStateOf<String?>(null) }

    TabScaffold(selected = Routes.HOME, onNavigate = onNavigate) { pad ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = pad.calculateTopPadding() + 24.dp, bottom = pad.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(greeting(settings.userName), style = MaterialTheme.typography.headlineMedium)
                Text("How are things on your side today?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!vm.hasKey) {
                item {
                    SoftCard(onClick = { onNavigate(Routes.SETTINGS) }) {
                        Column {
                            Text("One quick step first", style = MaterialTheme.typography.titleMedium)
                            Hint("Add your Groq API key in Settings so I can reply. It's stored encrypted on this device.")
                        }
                    }
                }
            }
            item { GradientButton("Start a conversation", onClick = { onOpenChat(0L, ChatMode.AUTO.name) }) }
            item {
                SoftCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Daily check-in", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MoodOptions.forEach { m ->
                                Column(
                                    Modifier.clickable { vm.logMood(m.value); checkedIn = "Logged: ${m.label} ${m.emoji}" }.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(m.emoji, fontSize = 30.sp)
                                    Text(m.label, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                        Hint(checkedIn ?: "Tap how you're feeling. Add a note any time in the Mood tab.")
                    }
                }
            }
            item {
                Text("Pick a vibe", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ChatMode.entries.filter { it != ChatMode.AUTO }) { mode ->
                        SoftCard(modifier = Modifier.width(168.dp), onClick = { onOpenChat(0L, mode.name) }) {
                            Column {
                                Text(mode.emoji, fontSize = 28.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(mode.label, style = MaterialTheme.typography.titleMedium)
                                Text(mode.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item {
                Text("Recent conversations", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    placeholder = { Text("Search your conversations") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (convs.isEmpty()) {
                item { Hint(if (query.isBlank()) "Nothing here yet. Your chats will show up here, saved only on this phone." else "No conversations match that search.") }
            }
            items(convs, key = { it.id }) { c ->
                SoftCard(onClick = { onOpenChat(c.id, c.mode) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Hint("${ChatMode.fromName(c.mode).label} · ${formatDateTime(c.updatedAt)}")
                        }
                        IconButton(onClick = { vm.deleteConversation(c.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete conversation", tint = Lavender.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}
