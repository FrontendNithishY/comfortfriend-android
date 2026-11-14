# Security policy

## How ComfortFriend handles secrets and data

- **No bundled keys.** No Groq key is in the source, resources or APK. Each user enters their own key in the app.
- **Encrypted at rest.** The key is encrypted with AES-256-GCM using a key held in the Android Keystore (hardware-backed where the device supports it). Only ciphertext is stored, in app-private storage excluded from backups.
- **Never logged.** The app has no logging of keys or conversation content, and the HTTP client has no logging interceptor.
- **Local data.** Conversations, memories, moods and MindLens observations live in a local Room database. Cloud backup and device transfer are disabled for the app's data.
- **No analytics.** There are no analytics or tracking SDKs.
- **Network.** HTTPS only (cleartext traffic is disabled). Only the recent conversation context (and memories the user saved, if memory is on) is sent to Groq for each reply.

## Known limitation

Because the app calls Groq directly from the device, the API key must be decrypted in memory to make requests. A sufficiently capable attacker with control of an unlocked, rooted or compromised device could extract and use it. Mitigations: use a dedicated Groq key with spending/rate limits, revoke it at console.groq.com if the device is lost, and keep your device updated and locked. A server-side proxy would remove this exposure but would reintroduce a hosted component and is out of scope.

The on-device database itself is not separately encrypted; it relies on Android's per-user file-based encryption. Exports (JSON) are plain text, so store them carefully.

## Reporting a vulnerability

Please open a private security advisory on the GitHub repository (Security > Advisories) rather than a public issue. Do not include real API keys or personal conversations in reports.
