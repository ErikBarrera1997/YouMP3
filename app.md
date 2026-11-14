# YouMp3 — Documentación de estructura, funcionalidades y relación de clases

App **Kotlin Multiplatform** (Compose Multiplatform) en un único módulo `:app`.
Permite buscar canciones por título/artista, extraer el audio desde YouTube vía backend,
reproducirlo en un mini-player y guardarlo como archivo MP3. Targets: **Android**
(`androidMain`) y **Desktop JVM** (`desktopMain`); UI y lógica compartidas en `commonMain`.
Paquete raíz: `com.dev.yoump3`.

Toolchain: Gradle 8.13 (wrapper), AGP 8.11.0, Kotlin 2.3.x, Compose MP 1.10.x, Ktor 3.0.3,
kotlinx-serialization, AndroidX Lifecycle ViewModel.

---

## 1. Estructura del proyecto

```text
/workspace
├── build.gradle.kts              # Raíz: plugins y configuración global
├── settings.gradle.kts           # rootProject "YouMp3", incluye :app
├── gradle.properties             # YouMp3Version, YouMp3ServerUrl, flags de Gradle
├── qodana.yaml                   # Análisis de calidad de código (CI Qodana)
├── AGENTS.md                     # Guía para agentes/contribuidores
├── .github/workflows/
│   ├── test_build.yaml           # Gate CI: assembleRelease, lintRelease, compileKotlinDesktop, check
│   └── qodana_code_quality.yml   # Análisis estático Qodana
└── app/
    ├── build.gradle.kts          # Configuración KMP + Android + desktop + tareas generadoras
    └── src/
        ├── commonMain/           # ← Lógica y UI compartidas (el grueso de la app)
        │   ├── composeResources/drawable/   # icon.webp, dark/light_theme, retry, server_error
        │   └── kotlin/com/dev/yoump3/
        │       ├── AppVersion.kt                 # expect val appVersion
        │       ├── AppBackgroundSignal.kt        # señal de paso a segundo plano
        │       ├── viewModels/                   # YouMp3ViewModel + hijos
        │       ├── interfaces/                   # pantallas y componentes Compose
        │       ├── init/                         # YouMp3Api (cliente Ktor) + LoadingScreen
        │       ├── services/                     # AudioPlayer, AudioSaver, Base64Codec, ExtractedAudio
        │       ├── config/                       # AppConfig (interfaz de configuración)
        │       ├── preferences/                  # AppPreferences
        │       ├── dependencies/                 # PlatformDependencies (costura de DI)
        │       ├── consent/                      # DataUsageConsent (texto/claves del permiso)
        │       └── error/                        # AppError / AppErrorKind / AppErrorCatalog
        ├── androidMain/          # Implementaciones Android (actuals) + Manifest y recursos
        │   └── kotlin/com/dev/yoump3/
        │       ├── MainActivity.kt               # punto de entrada Android
        │       ├── YouMp3Application.kt          # Application: contador de Activities visibles
        │       ├── AppVersion.android.kt         # actual appVersion = BuildConfig.VERSION_NAME
        │       ├── config/AndroidAppConfig.kt
        │       ├── preferences/AndroidAppPreferences.kt   # SharedPreferences
        │       ├── services/AndroidAudioPlayer.kt         # MediaPlayer
        │       ├── services/AndroidAudioSaver.kt          # MediaStore / Downloads
        │       ├── services/MediaPlayerService.kt         # Service de reproducción
        │       ├── services/Base64Codec.android.kt
        │       └── interfaces/DownloadLink.android.kt     # Intent ACTION_VIEW
        ├── desktopMain/          # Implementaciones Desktop JVM (actuals)
        │   └── kotlin/com/dev/yoump3/
        │       ├── Main.kt                       # ventana Compose, VM store manual
        │       ├── config/DesktopAppConfig.kt
        │       ├── preferences/DesktopAppPreferences.kt   # java.util.prefs
        │       ├── services/DesktopAudioPlayer.kt         # javax.sound Clip
        │       ├── services/DesktopAudioSaver.kt          # FileSystem downloads
        │       ├── services/Base64Codec.desktop.kt
        │       └── interfaces/DownloadLink.desktop.kt     # Desktop.getGraphicsDevice().browse()
        ├── androidTest/ … ExampleInstrumentedTest.kt
        └── test/java/com/dev/yoump3/             # Tests unitarios JUnit 4
            ├── viewModels/{YouMp3BackTest, SettingsViewModelTest, DataUsageConsentTest}.kt
            ├── error/AppErrorCatalogTest.kt
            └── ExampleUnitTest.kt
```

### Código generado (no escribir a mano)

Dos tareas Gradle generan `actual`s de `desktopMain` en `build/generated/`:

| Tarea | Archivo generado | Declaración |
|---|---|---|
| `generateDesktopAppVersion` | `AppVersion.desktop.kt` | `actual val appVersion` |
| `generateDesktopServerUrl` | `config/DesktopServerUrl.desktop.kt` | `defaultServerBaseUrl` |

La versión se define solo en `YouMp3Version` (`gradle.properties`); la URL del servidor en
`YouMp3ServerUrl`, con override local por máquina en `local.properties` (`yoump3.serverUrl`).

---

## 2. Funcionalidades

1. **Verificación de conexión al arrancar** — `YouMp3Api.checkConnection()` sondea
   `POST /api/audios/search` con body vacío (no usa `GET /api/health` porque devuelve 503 en
   ciertos estados). Mientras no hay respuesta: ventana `Init` con `LoadingScreen` y fondo
   `SonicCascadeBackground`; si falla: ventana `ConnectionError` con botón de reintento.
2. **Búsqueda de canciones** — pantalla `SongInput`: búsqueda por título/artista
   (`POST /api/audios/search`), listado de resultados (`SearchResultUi`: videoId, título, autor,
   duración).
3. **Consentimiento de uso de datos y cookies** — antes de la primera búsqueda se muestra
   `DataUsageConsentDialog`; la decisión (null=no preguntado / true / false) se persiste en
   `AppPreferences` y viaja en el request de extracción. Bloquea la búsqueda si está denegado;
   reversible desde Ajustes. `DataUsageConsent.VERSION` permite repreguntar si cambia el texto.
4. **Extracción de audio** — `POST /api/audios/extract` devuelve el MP3 en base64 dentro del JSON.
   Se decodifica **una sola vez** con `decodeBase64` (suspend, fuera del hilo de UI para evitar
   OOM) y queda en un único buffer `ExtractedAudio`. Hay presupuesto máximo de tamaño validado
   en el cliente (`requireAudioWithinBudget`). Ventana `ServiceError` si el backend falla.
5. **Reproducción** — `MiniPlayer` con play/pause, seek y posición, sobre la interfaz
   `AudioPlayer` (Android: `MediaPlayer`/`MediaPlayerService`; Desktop: `javax.sound.Clip`).
   El buffer se limpia (`wipe()`) al salir de la pantalla y al pasar a segundo plano
   (`AppBackgroundSignal`).
6. **Descarga/guardado** — `AudioSaver.save()` persiste el archivo (Android: MediaStore/Downloads;
   Desktop: carpeta de descargas) y `DownloadLink` (expect/actual) abre la ubicación o URI.
7. **Ajustes** — tema oscuro/claro (`ThemeMode` → `YouMp3Colors`), selección de servidor
   online/local (`ServerTarget` + URL editable que re-valida la conexión al confirmar), y toggle
   del consentimiento de datos.
8. **Navegación y back centralizados** — `YouMp3ViewModel` es dueño de `AppWindow`
   (Init/Main/ConnectionError/ServiceError) y de la navegación interna (`YouMp3Screen`:
   Home/SongInput/Settings). El render y el back leen del mismo estado.
9. **Gestión de errores tipificada** — catálogo `AppErrorKind` (severidad + título + detalle),
   `AppErrorCatalog` clasifica fallos HTTP y de transporte; las pantallas de error muestran
   textos ya resueltos (ningún composable interpreta códigos HTTP).

---

## 3. Relación de clases (núcleo)

### 3.1 Costura de dependencias y arranque

`PlatformDependencies` es el único punto de inyección: lo construye cada plataforma en su
`main`/`MainActivity` y lo recibe `YouMp3ViewModel`.

```text
MainActivity (android) ─┐                       ┌─ AndroidAppConfig      ─implements→ AppConfig
DesktopApp Main.kt ──────┤ construyen →  PlatformDependencies
                       │                       ├─ AndroidAppPreferences ─implements→ AppPreferences
                       │                       ├─ DesktopAppPreferences ─implements→ AppPreferences
                       │                       ├─ DesktopAppConfig      ─implements→ AppConfig
                       │                       ├─ AndroidAudioSaver     ─implements→ AudioSaver
                       │                       ├─ DesktopAudioSaver     ─implements→ AudioSaver
                       │                       └─ audioPlayer = createAudioPlayer()  (expect/actual)
                       ▼
                YouMp3ViewModel(dependencies) : ViewModel
```

### 3.2 Composición de ViewModels (jerarquía de propiedad)

`YouMp3ViewModel` es la **fuente de verdad**: posee el estado de ventanas, crea el API y es
dueño de los tres ViewModels hijos (construidos a mano; solo él recibe `onCleared()`, que
propaga el dispose a `songInputViewModel` y cierra `api`).

```text
YouMp3ViewModel  ── expone ──▶ YouMp3UiState { currentScreen: YouMp3Screen, appReady, connectionError }
 │                             activeWindow: AppWindow  (Init | Main | ConnectionError | ServiceError)
 ├── owns ──▶ SettingsViewModel(appPreferences, appConfig, onServerUrlCommitted)
 │               └── SettingsUiState { ThemeMode, ServerTarget, serverUrl, dataUsageConsent }
 │               └── expone YouMp3Colors (DarkThemeColors | LightThemeColors)
 ├── owns ──▶ SongInputViewModel(api, audioSaver, audioPlayer, appPreferences)
 │               ├── SongInputUiState (búsqueda, extracción, descarga, consentimiento, serviceError)
 │               ├── usa YouMp3Api ──────────▶ (Ktor) /api/audios/search · /api/audios/extract
 │               ├── usa AudioSaver (interface)
 │               ├── usa AudioPlayer (interface) ─uses──▶ ExtractedAudio (buffer único, wipe())
 │               │        └── AudioPlayerState (isPlaying, durationMs, positionMs)
 │               ├── usa decodeBase64() (expect/actual suspend)
 │               ├── lee/escribe AppPreferences (consentimiento)
 │               └── escucha AppBackgroundSignal (libera audio en segundo plano)
 └── owns ──▶ ErrorStatusViewModel ── showError(AppError) ──▶ ErrorStatusUiState
```

### 3.3 UI (interfaces/) → qué ViewModel consume

```text
YouMp3App(viewModel)                    // router según viewModel.activeWindow
 ├── LoadingScreen (init/)               // AppWindow.Init + SonicCascadeBackground
 ├── ErrorStatusScreen                   // ConnectionError / ServiceError ← ErrorStatusViewModel
 └── Main ─▶ YouMp3Screen(currentScreen)
      ├── Home/SongInput ─▶ SongInputScreen   ← SongInputViewModel  (buscador + resultados)
      │        ├── SearchStatusView           // estado de búsqueda/extracción
      │        ├── ExtractionStatusView + MiniPlayer  // player sobre AudioPlayer.state
      │        ├── DataUsageConsentDialog     ← DataUsageConsent (constantes)
      │        └── DownloadLink (expect/actual)
      └── Settings ─▶ SettingsScreenContent   ← SettingsViewModel
                          └── ThemeColors.kt: YouMp3Colors + LocalAppColors (CompositionLocal)
```

Dependencias transversales de estilo/fondo: `SonicCascadeBackground` (animación de red
"sonic") recibe los colores de `ThemeColors`; ninguna pantalla pinta colores crudos.

### 3.4 Capa de red (init/YouMp3Api.kt)

```text
YouMp3Api(baseUrl: () -> String)          // lambda: resuelve la URL efectiva en cada petición
 ├── HttpClient (Ktor; engine okhttp en Android, cio en Desktop)
 ├── checkConnection(): Result<Unit>      // sonda POST /api/audios/search vacía
 ├── searchSongs(...) ─▶ AudioSearchResponse / List<AudioSearchResult>
 ├── extractAudio(...) ─▶ AudioExtractionResponse  // audioBase64
 ├── ApiException(error: AppError) : Exception     // puente capa red → dominio
 └── DTO serializables: AudioExtractionRequest/Response, AudioSearchRequest/Response,
     ExtractedAudioResponse, AudioSearchResult
        ▲ traduce errores con
     AppErrorCatalog.fromHttp()/fromTransport() ─▶ AppError(kind: AppErrorKind,
                                                              severity: AppErrorSeverity)
```

### 3.5 Tabla expect/actual (puentes entre source sets)

| Declaración `expect` (commonMain) | `actual` Android | `actual` Desktop |
|---|---|---|
| `val appVersion` | `AppVersion.android.kt` (BuildConfig) | generado (`AppVersion.desktop.kt`) |
| `fun createAudioPlayer(): AudioPlayer` | `AndroidAudioPlayer` (MediaPlayer + `MediaPlayerService`) | `DesktopAudioPlayer` (javax.sound Clip) |
| `suspend fun decodeBase64(String): ByteArray` | `Base64Codec.android.kt` | `Base64Codec.desktop.kt` |
| `@Composable fun DownloadLink(...)` | `DownloadLink.android.kt` (Intent) | `DownloadLink.desktop.kt` (Browser) |
| `interface AppConfig` | `AndroidAppConfig` (BuildConfig.SERVER_BASE_URL) | `DesktopAppConfig` (+ `DesktopServerUrl` generado) |
| `interface AppPreferences` | `AndroidAppPreferences` (SharedPreferences) | `DesktopAppPreferences` (java.util.prefs) |
| `interface AudioSaver` | `AndroidAudioSaver` (MediaStore) | `DesktopAudioSaver` (FileSystem) |

Regla del proyecto: la lógica compartida vive en `commonMain`; los `actual` solo contienen
mecánica de plataforma.

### 3.6 Ciclo de vida y limpieza (relación crítica)

```text
Android:  YouMp3Application (Application) ── cuenta Activities visibles ──▶ AppBackgroundSignal.onAppBackgrounded()
          MainActivity ── setContent ──▶ YouMp3App(viewModel)
Desktop:  Main.kt onCloseRequest ──▶ AppBackgroundSignal.onAppBackgrounded() ──▶ exitApplication()

AppBackgroundSignal.Listener (fun interface)
   ◀── suscrito por SongInputViewModel ── al recibir: audioPlayer.release() + ExtractedAudio.wipe()

YouMp3ViewModel.onCleared() ──▶ songInputViewModel.dispose() + api.close()
   (único ViewModel en el ViewModelStore; los hijos se limpian a mano o hay fugas)
```

---

## 4. Flujo principal (secuencia resumida)

```text
Arranque ─▶ YouMp3ViewModel ─▶ api.checkConnection()
   OK ─▶ AppWindow.Main ─▶ Home/SongInput
Usuario escribe query ─▶ [¿consentimiento? si no hay: DataUsageConsentDialog]
   ─▶ SongInputViewModel.search() ─▶ YouMp3Api.searchSongs ─▶ lista SearchResultUi
Selección de resultado ─▶ SongInputViewModel.extract()
   ─▶ YouMp3Api.extractAudio (base64 en JSON)
   ─▶ decodeBase64() ─▶ ExtractedAudio (buffer único)
   ─▶ AudioPlayer.load(audio) ─▶ MiniPlayer (play/seek)
   ─▶ AudioSaver.save(fileName, contentType, bytes) ─▶ DownloadLink abre destino
Salida de pantalla / segundo plano ─▶ release() + wipe()
Error de backend en cualquier paso ─▶ AppError(AppErrorCatalog) ─▶ ServiceError ─▶ ErrorStatusScreen
```

---

## 5. Pruebas (relación con el diseño)

Los tests JUnit 4 de `app/src/test/java/` aprovechan `PlatformDependencies` como costura de DI:
construyen los ViewModels con fakes (p. ej. `audioPlayer` inyectable evita necesitar un
`Context` de Android).

| Test | Cubre |
|---|---|
| `viewModels/YouMp3BackTest` | navegación/back sobre `AppWindow`/`YouMp3Screen` |
| `viewModels/SettingsViewModelTest` | temas, servidores, normalización de URL |
| `viewModels/DataUsageConsentTest` | máquina de estados del consentimiento |
| `error/AppErrorCatalogTest` | clasificación HTTP/transporte → `AppErrorKind` |

## 6. Convenciones

- Comentarios y docstrings **en español**.
- Versiones solo en `YouMp3Version`; nunca editar código generado.
- `RepositoriesMode.FAIL_ON_PROJECT_REPOS`: no declarar repositorios en módulos.
- Gate CI completo: `assembleRelease` → `lintRelease` → `:app:compileKotlinDesktop` → `check`.
