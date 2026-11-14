# Build and installation guide

## Local build

1. Install JDK 17 and the Android SDK command-line tools.
2. `sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"`
3. Create `local.properties` with `sdk.dir=/path/to/Android/sdk` (it is git-ignored), or set `ANDROID_HOME`.
4. `./gradlew :app:testDebugUnitTest :app:assembleDebug`
5. Output: `app/build/outputs/apk/debug/app-debug.apk`

Instrumentation tests (need an emulator or device): `./gradlew :app:connectedDebugAndroidTest`

## Release signing (user-managed)

Create your own keystore and **never commit it**:

```bash
keytool -genkeypair -v -keystore comfortfriend.jks -alias comfortfriend -keyalg RSA -keysize 2048 -validity 10000
export CF_KEYSTORE_PATH=$PWD/comfortfriend.jks CF_KEYSTORE_PASSWORD=... CF_KEY_ALIAS=comfortfriend CF_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

Without these values the release build is produced unsigned and cannot be installed.

For CI, store the keystore as base64 (`base64 -w0 comfortfriend.jks`) in the repository secret `CF_KEYSTORE_B64`, plus `CF_KEYSTORE_PASSWORD`, `CF_KEY_ALIAS`, `CF_KEY_PASSWORD`.

## CI

`.github/workflows/android-build.yml` runs on every push and pull request: unit tests, lint (non-blocking), debug APK build, APK existence check, then uploads the APK as an artifact.

## Installing on a phone

Enable "Install unknown apps" for the app you use to open the APK, then open `app-debug.apk`. The debug build uses the id `com.comfortfriend.app.debug`, so it can sit beside a release build.
