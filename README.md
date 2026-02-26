# Qtiqo Share (Android MVP)

`Qtiqo Share` is a photo and file sharing app that auto-generates a public link so users can share their photos and files.

Share link domain requirement is enforced:

- `https://imagelink.qtiqo.com/s/{shareToken}`

Deep links for `https://imagelink.qtiqo.com/s/...` are configured in `app/src/main/AndroidManifest.xml` and Navigation Compose.

## Tech stack

- Kotlin + Jetpack Compose
- Navigation Compose
- MVVM + Coroutines + Flow
- Hilt DI
- Room (uploads local cache)
- WorkManager (background uploads + foreground notification)
- Retrofit + OkHttp (real backend API interfaces/DTOs included)
- EncryptedSharedPreferences (JWT session token)
- FakeBackend mode (default ON) for offline demo/testing

## Project structure

- `app/src/main/java/com/qtiqo/share/data` : Room, fake backend, prefs, Retrofit APIs/DTOs, repositories
- `app/src/main/java/com/qtiqo/share/ui` : Compose navigation, screens, theme, viewmodels
- `app/src/main/java/com/qtiqo/share/worker` : upload worker + foreground notification

## How to run

1. Open the project in Android Studio (Hedgehog+ / recent stable).
2. Let Gradle sync.
3. Run the `app` configuration on an emulator or device (API 26+).

### Command line (Windows)

If `gradle/wrapper/gradle-wrapper.jar` is present, run:

```powershell
.\gradlew.bat assembleDebug
```

If the wrapper JAR is missing in your environment, Android Studio can still sync using local Gradle, or regenerate wrapper files with:

```powershell
gradle wrapper
```

## FakeBackend mode (default)

The app runs without a real server when **FakeBackend (Debug)** is enabled (Profile screen).

What FakeBackend does:

- Simulates auth (`Sign Up`, `Sign In`, `Forgot Password`, `Logout`)
- Simulates upload progress in `WorkManager`
- Generates public share tokens
- Stores fake files in memory
- Tracks revoked tokens in memory
- Resolves public links in-app (`/s/{token}`)

Notes:

- FakeBackend data is in-memory for the backend simulation, so it resets after process death.
- Room keeps local upload records on device.
- Public view resolution in FakeBackend uses both Room and the in-memory fake backend state.

## MVP flow test (recommended)

1. Sign in with demo user:
   - `demo@qtiqo.com` / `demo123`
   - or admin: `admin` / `admin123`
2. Go to `Upload` tab.
3. Tap `Choose a File` and pick any file using the system picker.
4. Watch upload progress (cancel/retry supported).
5. Open the file in `File Detail`.
6. Copy/share the generated link (`https://imagelink.qtiqo.com/s/{token}`).
7. Toggle `Allow Downloads`, then test the public view.
8. Use `Revoke Link` and `Regenerate Link` to verify token invalidation/regeneration.
9. Log in as `admin` and open the `Admin` tab for dashboard + user list.

## Real backend mode

Turn OFF **FakeBackend (Debug)** in Profile to use the Retrofit backend path.

Included API interfaces/DTOs cover:

- `POST /auth/signup`, `/auth/login`, `/auth/forgot`, `/auth/logout`
- `POST /files/init`
- `PUT uploadUrl` (raw upload interface declared)
- `POST /files/complete`
- `GET /files`
- `GET /files/{id}`
- `PATCH /files/{id}`
- `POST /files/{id}/revoke`
- `POST /files/{id}/regenerate`
- `GET /public/{shareToken}`

Current MVP keeps the **real upload execution path intentionally placeholder-only** and will fail with a message directing you to FakeBackend mode unless you wire your backend implementation.

## Key files

- `app/src/main/AndroidManifest.xml` : deep links + app config
- `app/src/main/java/com/qtiqo/share/data/repo/UploadRepository.kt` : upload orchestration + fake/real switch
- `app/src/main/java/com/qtiqo/share/worker/UploadWorker.kt` : background upload worker + notification
- `app/src/main/java/com/qtiqo/share/ui/QtiqoShareAppRoot.kt` : app navigation + bottom tabs

## Known limitations (MVP)

- Real backend upload byte streaming is stubbed (APIs are defined, fake mode is fully runnable)
- FakeBackend server state resets after app process restart
