package com.comfortfriend.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comfortfriend.app.AppContainer
import com.comfortfriend.app.data.db.MemoryEntity
import com.comfortfriend.app.data.db.MemoryKind
import com.comfortfriend.app.data.db.MoodEntity

@Composable
fun MoodChart(moods: List<MoodEntity>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val points = moods.sortedBy { it.createdAt }.takeLast(30)
    if (points.size < 2) {
        Hint("Log a couple of moods and your trend will appear here.", modifier)
        return
    }
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        val w = size.width
        val h = size.height
        for (lvl in 1..5) {
            val y = h - (lvl - 1) / 4f * h
            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        val xs = points.indices.map { it / (points.size - 1f) * w }
        val ys = points.map { h - (it.mood - 1) / 4f * h }
        for (i in 0 until points.size - 1) {
            drawLine(line, Offset(xs[i], ys[i]), Offset(xs[i + 1], ys[i + 1]), strokeWidth = 5f, cap = StrokeCap.Round)
        }
        xs.indices.forEach { i -> drawCircle(line, radius = 7f, center = Offset(xs[i], ys[i])) }
    }
}

@Composable
fun MindLensScreen(container: AppContainer, onNavigate: (String) -> Unit) {
    val vm: MindLensViewModel = viewModel(factory = VMFactory(container))
    val ui by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }
    var confirmErase by remember { mutableStateOf(false) }

    TabScaffold(selected = Routes.MINDLENS, onNavigate = onNavigate) { pad ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = pad.calculateTopPadding() + 24.dp, bottom = pad.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("MindLens", style = MaterialTheme.typography.headlineMedium)
                Hint("Gentle observations from chats you've let me remember. They're guesses to check against your own sense of things, never facts, diagnoses or predictions. It all runs on this phone.")
            }
            if (!ui.enabled) {
                item {
                    SoftCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Insights are switched off", style = MaterialTheme.typography.titleMedium)
                            Hint("Turn on memory and MindLens in the Memory tab if you'd like observations. Nothing is analysed while they're off.")
                            OutlinedButton(onClick = { onNavigate(Routes.MEMORY) }) { Text("Open Memory & privacy") }
                        }
                    }
                }
            } else {
                item {
                    SoftCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Mood trend", style = MaterialTheme.typography.titleMedium)
                            MoodChart(ui.moods)
                        }
                    }
                }
                item { SectionTitle("What I've noticed") }
                if (ui.insights.isEmpty()) {
                    item { Hint("Not enough yet. Patterns need a few conversations (${ui.messageCount} remembered messages so far). I won't guess without evidence.") }
                }
                items(ui.insights) { i ->
                    SoftCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(i.title, style = MaterialTheme.typography.titleMedium)
                            Text("Observation, not fact", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(i.observation, style = MaterialTheme.typography.bodyMedium)
                            Text("To reflect on: ${i.reflection}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { vm.save(i) }) { Text("Save this observation") }
                        }
                    }
                }
            }
            if (ui.saved.isNotEmpty()) {
                item { SectionTitle("Saved observations") }
                items(ui.saved, key = { "s${it.id}" }) { o ->
                    SoftCard {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(o.title, style = MaterialTheme.typography.titleMedium)
                                Text(o.text, style = MaterialTheme.typography.bodyMedium)
                                Hint(formatDate(o.createdAt))
                            }
                            IconButton(onClick = { vm.deleteSaved(o.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete observation") }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { confirmErase = true }, modifier = Modifier.fillMaxWidth()) { Text("Erase MindLens data") }
            }
        }
    }
    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase MindLens data?") },
            text = { Text("Saved observations will be deleted. Your chats and mood journal are kept.") },
            confirmButton = { TextButton(onClick = { vm.eraseInsights(); confirmErase = false }) { Text("Erase") } },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun MoodScreen(container: AppContainer, onNavigate: (String) -> Unit) {
    val vm: MoodViewModel = viewModel(factory = VMFactory(container))
    val moods by vm.moods.collectAsState()
    var selected by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }

    TabScaffold(selected = Routes.MOOD, onNavigate = onNavigate) { pad ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = pad.calculateTopPadding() + 24.dp, bottom = pad.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Mood journal", style = MaterialTheme.typography.headlineMedium)
                Hint("A quick daily check-in. Entirely optional and stored only on this phone.")
            }
            item {
                SoftCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("How are you feeling?", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MoodOptions.forEach { m ->
                                FilterChip(
                                    selected = selected == m.value,
                                    onClick = { selected = m.value },
                                    label = { Text("${m.emoji}", fontSize = 22.sp) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = note, onValueChange = { note = it.take(1000) },
                            label = { Text("Anything you'd like to jot down? (optional)") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2,
                        )
                        GradientButton("Save entry", enabled = selected != 0, onClick = {
                            vm.add(selected, note); selected = 0; note = ""
                        })
                    }
                }
            }
            item { SoftCard { Column { Text("Timeline", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp)); MoodChart(moods) } } }
            items(moods, key = { it.id }) { m ->
                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(moodEmoji(m.mood), fontSize = 30.sp)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(formatDateTime(m.createdAt), style = MaterialTheme.typography.labelLarge)
                            if (m.note.isNotBlank()) Text(m.note, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { vm.delete(m.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete entry") }
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryScreen(container: AppContainer, onNavigate: (String) -> Unit) {
    val vm: MemoryViewModel = viewModel(factory = VMFactory(container))
    val memories by vm.memories.collectAsState()
    val settings by vm.settings.collectAsState()
    var editing by remember { mutableStateOf<MemoryEntity?>(null) }
    var adding by remember { mutableStateOf(false) }

    TabScaffold(selected = Routes.MEMORY, onNavigate = onNavigate) { pad ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = pad.calculateTopPadding() + 24.dp, bottom = pad.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Your memory", style = MaterialTheme.typography.headlineMedium)
                Hint("Only things you chose to save live here. I never quietly build a profile of you. Edit or delete anything, any time.")
            }
            item {
                SoftCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Privacy", style = MaterialTheme.typography.titleMedium)
                        LabeledRow("Use saved memories in chats") { Switch(settings.memoryEnabled, onCheckedChange = vm::setMemoryEnabled) }
                        LabeledRow("Allow MindLens insights") { Switch(settings.mindLensEnabled && settings.memoryEnabled, onCheckedChange = vm::setMindLensEnabled, enabled = settings.memoryEnabled) }
                        Hint("Messages sent to Groq are processed by Groq. Chats stay on this phone otherwise.")
                    }
                }
            }
            item { GradientButton("Add a memory", onClick = { adding = true }) }
            if (memories.isEmpty()) item { Hint("Nothing saved yet. You can tap “Remember this” on a message in chat, or add something here.") }
            items(memories, key = { it.id }) { m ->
                SoftCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(MemoryKind.label(m.kind), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(m.text, style = MaterialTheme.typography.bodyMedium)
                            Hint(formatDate(m.updatedAt))
                        }
                        IconButton(onClick = { editing = m }) { Icon(Icons.Filled.Edit, contentDescription = "Edit memory") }
                        IconButton(onClick = { vm.delete(m.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete memory") }
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        val target = editing
        var kind by remember(target) { mutableStateOf(target?.kind ?: MemoryKind.CONTEXT) }
        var text by remember(target) { mutableStateOf(target?.text ?: "") }
        AlertDialog(
            onDismissRequest = { adding = false; editing = null },
            title = { Text(if (target == null) "Add a memory" else "Edit memory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MemoryKind.all.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { k -> FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(MemoryKind.label(k), fontSize = 12.sp) }) }
                        }
                    }
                    OutlinedTextField(value = text, onValueChange = { text = it.take(500) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (target == null) vm.add(kind, text) else vm.edit(target, kind, text)
                    adding = false; editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { adding = false; editing = null }) { Text("Cancel") } },
        )
    }
}
