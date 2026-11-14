# Contributing

1. Fork or branch, then open a pull request. CI (unit tests + debug APK build) must pass.
2. Run locally: `./gradlew :app:testDebugUnitTest :app:assembleDebug` (JDK 17, Android SDK 35).
3. Keep it private by design: never add analytics, logging of message content, or hardcoded keys.
4. Safety first: changes to `Prompts.kt`, `Safety.kt` or `MindLens.kt` need matching unit tests, and MindLens wording must stay tentative (observations, not facts).
5. Never commit API keys, keystores, signing passwords or exported conversation data (`.gitignore` covers common cases).
6. Use clear commit messages in the imperative mood ("Add mood chart", "Fix retry on 429").
