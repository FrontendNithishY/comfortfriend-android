package com.comfortfriend.app.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comfortfriend.app.AppContainer
import com.comfortfriend.app.BuildConfig
import com.comfortfriend.app.data.prefs.ThemeChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class Destructive(val title: String, val body: String) {
    CONVERSATIONS("Delete all conversations?", "Every chat and its messages will be removed from this phone."),
    MEMORIES("Delete all memories?", "Everything saved in your Memory tab will be removed."),
    MOODS("Clear the mood journal?", "All mood entries will be removed."),
    EVERYTHING("Delete all local data?", "Conversations, memories, moods and MindLens data will be removed. Your API key and settings are kept."),
}

@Composable
fun SettingsScreen(container: AppContainer, onNavigate: (String) -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = VMFactory(container))
    val s by vm.settings.collectAsState()
    val ai by vm.ai.collectAsState()
    val context = LocalContext.current

    var keyInput by remember { mutableStateOf("") }
    var modelMenu by remember { mutableStateOf(false) }
    var pendingDestructive by remember { mutableStateOf<Destructive?>(null) }
    var pendingExportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var nameInput by remember(s.userName) { mutableStateOf(s.userName) }

    LaunchedEffect(Unit) { vm.loadModels() }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) { pendingExportUri = uri; vm.prepareExport() }
    }
    LaunchedEffect(ai.exportJson, pendingExportUri) {
        val json = ai.exportJson
        val uri = pendingExportUri
        if (json != null && uri != null) {
            exportStatus = try {
                writeText(context, uri, json); "Exported. Keep that file somewhere safe: it contains private conversations."
            } catch (e: Exception) { "Export failed. Please try again." }
            pendingExportUri = null
            vm.exportConsumed()
        }
    }

    val modelOptions = (ai.models + s.model).distinct()

    TabScaffold(selected = Routes.SETTINGS, onNavigate = onNavigate) { pad ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = pad.calculateTopPadding() + 24.dp, bottom = pad.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }

            // ---- AI configuration
            item {
                SoftCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("AI connection (Groq)", style = MaterialTheme.typography.titleMedium)
                        Hint(if (ai.hasKey) "A key is saved (encrypted on this device). Paste a new one to replace it." else "Paste your own Groq API key. Get one at console.groq.com.")
                        OutlinedTextField(
                            value = keyInput, onValueChange = { keyInput = it },
                            label = { Text(if (ai.hasKey) "Replace API key" else "Groq API key") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                        GradientButton(
                            if (ai.testing) "Testing…" else if (keyInput.isBlank() && ai.hasKey) "Test saved key" else "Test & save key",
                            enabled = !ai.testing && (keyInput.isNotBlank() || ai.hasKey),
                            onClick = { vm.testAndSaveKey(keyInput); keyInput = "" },
                        )
                        ai.message?.let {
                            Text(it, color = if (ai.messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (ai.hasKey) OutlinedButton(onClick = vm::removeKey, modifier = Modifier.fillMaxWidth()) { Text("Remove key from this device") }

                        Text("Model", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { modelMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(s.model) }
                        DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                            modelOptions.forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { vm.setModel(m); modelMenu = false }) }
                        }
                        if (ai.hasKey) TextButton(onClick = vm::loadModels) { Text("Refresh model list") }

                        Text("Creativity (temperature): ${"%.1f".format(s.temperature)}", style = MaterialTheme.typography.bodyLarge)
                        Slider(value = s.temperature, onValueChange = vm::setTemperature, valueRange = 0f..1.5f)
                        Text("Max reply length: ${s.maxTokens} tokens", style = MaterialTheme.typography.bodyLarge)
                        Slider(value = s.maxTokens.toFloat(), onValueChange = { vm.setMaxTokens(it.roundToInt()) }, valueRange = 100f..2000f)

                        Hint("Security note: your key is encrypted with an Android Keystore key and never logged or put in the app files. Because this app calls Groq directly from your phone, a determined attacker with control of your device could still use the key. Use a key with limits you're comfortable with, and revoke it at console.groq.com if the phone is lost.")
                    }
                }
            }

            // ---- Appearance & profile
            item {
                SoftCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Appearance", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeChoice.entries.forEach { t ->
                                FilterChip(selected = s.theme == t, onClick = { vm.setTheme(t) }, label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) })
                            }
                        }
                        OutlinedTextField(
                            value = nameInput, onValueChange = { nameInput = it.take(40); vm.setName(nameInput) },
                            label = { Text("What should I call you?") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ---- Memory controls
            item {
                SoftCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Memory & insights", style = MaterialTheme.typography.titleMedium)
                        LabeledRow("Use saved memories") { Switch(s.memoryEnabled, onCheckedChange = vm::setMemoryEnabled) }
                        LabeledRow("MindLens insights") { Switch(s.mindLensEnabled && s.memoryEnabled, onCheckedChange = vm::setMindLensEnabled, enabled = s.memoryEnabled) }
                        TextButton(onClick = { onNavigate(Routes.MEMORY) }) { Text("Review & edit memories") }
                    }
                }
            }

            // ---- Data management
            item {
                SoftCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your data", style = MaterialTheme.typography.titleMedium)
                        Hint("Everything is stored only on this phone. Export gives you a JSON file (never includes your API key).")
                        OutlinedButton(onClick = { exportLauncher.launch("comfortfriend-export.json") }, modifier = Modifier.fillMaxWidth()) { Text("Export my data") }
                        exportStatus?.let { Hint(it) }
                        OutlinedButton(onClick = { pendingDestructive = Destructive.CONVERSATIONS }, modifier = Modifier.fillMaxWidth()) { Text("Delete all conversations") }
                        OutlinedButton(onClick = { pendingDestructive = Destructive.MEMORIES }, modifier = Modifier.fillMaxWidth()) { Text("Delete all memories") }
                        OutlinedButton(onClick = { pendingDestructive = Destructive.MOODS }, modifier = Modifier.fillMaxWidth()) { Text("Clear mood journal") }
                        OutlinedButton(onClick = { pendingDestructive = Destructive.EVERYTHING }, modifier = Modifier.fillMaxWidth()) { Text("Delete everything") }
                    }
                }
            }

            // ---- About & safety
            item {
                SoftCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("About & safety", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "ComfortFriend is an AI emotional-support companion. It is not a doctor, therapist or crisis service, can't diagnose anything, and can make mistakes. " +
                                "If you're in danger or thinking about harming yourself, contact your local emergency number or a crisis line right away (India Tele-MANAS 14416, US 988, UK/IE Samaritans 116 123, or findahelpline.com), and reach out to someone you trust.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Privacy: your chats are stored on this phone. For each reply, the recent conversation (and any memories you saved, if memory is on) is sent to Groq, which processes it under its own terms. No analytics or tracking are included.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Hint("Version ${BuildConfig.VERSION_NAME}")
                    }
                }
            }
        }
    }

    pendingDestructive?.let { d ->
        AlertDialog(
            onDismissRequest = { pendingDestructive = null },
            title = { Text(d.title) },
            text = { Text(d.body + " This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    when (d) {
                        Destructive.CONVERSATIONS -> vm.clearConversations()
                        Destructive.MEMORIES -> vm.clearMemories()
                        Destructive.MOODS -> vm.clearMoods()
                        Destructive.EVERYTHING -> vm.clearEverything()
                    }
                    pendingDestructive = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDestructive = null }) { Text("Cancel") } },
        )
    }
}

private suspend fun writeText(context: Context, uri: android.net.Uri, text: String) = withContext(Dispatchers.IO) {
    val out = context.contentResolver.openOutputStream(uri) ?: error("Cannot open destination")
    out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
}
