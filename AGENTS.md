# AGENTS.md

## What this is

Kotlin Multiplatform (Compose Multiplatform) app in a single `:app` module. Targets: Android (`androidMain`) and Desktop JVM (`desktopMain`), shared UI/logic in `commonMain`. Package root: `com.dev.yoump3`.

Toolchain: Gradle 8.13 (wrapper), AGP 8.11.0, Kotlin 2.3.21, Compose MP 1.10.3, JDK 21 in CI (JVM target 11).

## Commands

CI order (`.github/workflows/test_build.yaml`) is the full gate:

```sh
./gradlew assembleRelease            # Android APKs (ABI splits + universal)
./gradlew lintRelease                # Android Lint — no ktlint/detekt/spotless exists
./gradlew :app:compileKotlinDesktop  # desktop compile
./gradlew check                      # all unit tests
```

Useful locally:

```sh
./gradlew :app:run                                   # run desktop app
./gradlew :app:testDebugUnitTest --tests "com.dev.yoump3.viewModels.YouMp3BackTest"  # single test
```

There are no `commonTest`/`desktopTest` sources. Unit tests are plain JUnit 4 under `app/src/test/java/` (Android unit-test layout). `desktopTest` task exists but has nothing to run.

## Generated code — do not hand-write

Two Gradle tasks generate `actual` declarations for `desktopMain` into `build/generated/` (see `app/build.gradle.kts:163-200`):

- `generateDesktopAppVersion` → `com/dev/yoump3/AppVersion.desktop.kt` (`actual val appVersion`)
- `generateDesktopServerUrl` → `com/dev/yoump3/config/DesktopServerUrl.desktop.kt` (`defaultServerBaseUrl`)

If desktop compilation fails on unresolved `appVersion`/`defaultServerBaseUrl`, the fix is in the Gradle task or `gradle.properties`, never a new source file. Android equivalents are checked in (`AppVersion.android.kt`, `BuildConfig.SERVER_BASE_URL`).

## Version and server URL

- Bump the app version only in `YouMp3Version` (`gradle.properties`). `versionCode` and the desktop MSI version are derived from it.
- Backend URL: `YouMp3ServerUrl` in `gradle.properties` (committed default). Per-machine override in gitignored `local.properties`: `yoump3.serverUrl=https://<host>` (emulator → `10.0.2.2`). Do not commit `local.properties`.

## Architecture

- `YouMp3ViewModel` is the single source of truth: owns window state (`AppWindow`), navigation, and all child ViewModels (`viewModels/YouMp3ViewModel.kt`).
- Child ViewModels are built by hand, not by the framework. Only `YouMp3ViewModel` gets `onCleared()` — it must keep propagating dispose (audio, background listeners, Ktor clients) or things leak.
- `PlatformDependencies` (`dependencies/PlatformDependencies.kt`) is the DI seam; tests construct ViewModels through it with fakes (e.g. injectable `audioPlayer` so tests need no Android `Context`).
- expect/actual split: `appVersion`, `Base64Codec`, `AudioPlayer`, `AudioSaver`, `AppPreferences`, `AppConfig`, `DownloadLink`. Put shared logic in `commonMain`; only platform mechanics go in the actuals.
- UI screens live in `interfaces/`, Ktor API client in `init/YouMp3Api.kt`.

## Conventions

- Comments and doc strings are written in Spanish; match the surrounding language when editing existing files.
- Repositories are enforced via `RepositoriesMode.FAIL_ON_PROJECT_REPOS` — never add `repositories {}` in module build files.
- Release builds sign with `keystore.properties` if present, else fall back to the debug key (both gitignored). No keystore needed for local builds.
- `compose.resources` generates a public `Res` class in `com.dev.yoump3.generated.resources`.
