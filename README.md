# ComfortFriend

A private Android AI emotional-support companion. Kotlin, Jetpack Compose, Material 3, MVVM, Room, DataStore, Retrofit/OkHttp, and the Groq OpenAI-compatible Chat Completions API (bring your own key).

> ComfortFriend is a supportive companion, **not** a medical professional or crisis service. It never diagnoses, never advises on medication, and shows crisis guidance if a message suggests self-harm.

## Features

- **Chat** with streaming replies, typing indicator, copy, regenerate, stop, new chat, saved history and search.
- **8 conversation modes** (Just Listen, Comfort Me, Reality Check, Make Me Laugh, Motivate Me, Help Me Decide, Overthinking, Study Support) plus automatic inference of the right approach.
- **MindLens**: on-device, rule-based observations (recurring topics, repeated worries, harsh self-talk, avoidance, mood trend, possible triggers). Always phrased as tentative observations, requires evidence before saying anything, can be switched off and erased.
- **Memory**: only what you approve (tap "Remember this", save an editable summary, or add manually). Review, edit, delete, disable.
- **Mood journal** with timeline chart and notes.
- **Data control**: export everything to JSON (never includes the API key), delete conversations / memories / moods / everything.
- **Settings**: Groq key (test, replace, remove), model picker, temperature and token limits, theme (system/light/dark).
- Safety: on-device crisis keyword screen shows helplines and switches the model into crisis-care behaviour (no humour).

## Build

Requirements: JDK 17, Android SDK (platform 35, build-tools 35.x). See [docs/BUILD.md](docs/BUILD.md).

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions (`.github/workflows/android-build.yml`) runs tests and builds the debug APK on every push and pull request and uploads it as the `comfortfriend-debug-apk` artifact. If the repository secrets `CF_KEYSTORE_B64`, `CF_KEYSTORE_PASSWORD`, `CF_KEY_ALIAS`, `CF_KEY_PASSWORD` are set, it also builds a signed release APK.

## Install & first run

1. Download the APK artifact from the Actions run, unzip it, and copy `app-debug.apk` to your phone (Android 8.0 / API 26 or newer).
2. Open it and allow "install unknown apps" for your file manager or browser when prompted.
3. Open ComfortFriend, then Settings > AI connection, paste your Groq key from console.groq.com, tap **Test & save key**, and pick a model.

## Dependencies (pinned)

| Component | Version |
|---|---|
| Android Gradle Plugin | 8.7.3 |
| Gradle | 8.9 |
| Kotlin / KSP | 2.0.21 / 2.0.21-1.0.28 |
| Compose BOM | 2024.10.01 (Material 3 1.3.x) |
| Navigation Compose | 2.8.4 |
| Lifecycle | 2.8.7 |
| Room | 2.6.1 |
| DataStore | 1.1.1 |
| Retrofit / OkHttp | 2.11.0 / 4.12.0 |
| kotlinx.serialization / coroutines | 1.7.3 / 1.9.0 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 |

## Privacy and security

See [SECURITY.md](SECURITY.md). In short: your key is Keystore-encrypted, nothing is logged, there are no analytics, chats stay on-device, and messages needed for each reply are processed by Groq under its terms. Direct client-side API access cannot fully protect the key from a determined attacker with device control.

## Project layout

```
app/src/main/java/com/comfortfriend/app/
  data/    Room (db), Keystore (secure), DataStore (prefs), Groq client (remote), export/erase
  domain/  Prompts, Modes, Safety, MindLens
  ui/      Compose screens, ViewModels, theme
```
