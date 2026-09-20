# NeoSky Service & Support — Android

A native Android client for customers who own NeoSky/TAS drones: manage registered drones,
raise and track service tickets, log flights, and review warranty, maintenance, invoice and
document history. Built against the REST API described in `../docs/API_SPEC.md`.

Kotlin · Jetpack Compose · Material 3 · MVVM + Clean Architecture · Coroutines/Flow ·
Retrofit + OkHttp + kotlinx.serialization · Hilt · Room · WorkManager · Firebase Cloud Messaging ·
DataStore + EncryptedSharedPreferences.

## Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer.
- **JDK**: 17 (bundled with recent Android Studio; the Gradle build targets JVM 17).
- **Gradle**: the wrapper (`./gradlew`) handles this — it downloads Gradle 8.9 on first run. No
  system-wide Gradle install is required.
- An Android emulator or device running **API 26 (Android 8.0)** or newer.
- The sibling `backend/` service running locally if you want to exercise real API calls (see its
  own README for setup) — the seeded demo login below assumes it is running and seeded.

## Opening the project

1. Open Android Studio → **Open** → select this `android/` directory (not the repo root).
2. Let Gradle sync. The first sync downloads the Gradle distribution, AGP, and all dependencies
   listed in `gradle/libs.versions.toml`.
3. If Android Studio asks to trust the Gradle wrapper or SDK components, accept.

## Firebase Cloud Messaging setup

This project does **not** ship a real `google-services.json` (that file contains project-specific
credentials and must come from your own Firebase project). To enable push notifications:

1. Create (or open) a Firebase project and add an Android app with package name
   `com.neosky.servicesupport`.
2. Download the generated `google-services.json` from the Firebase console.
3. Place it at `android/app/google-services.json` (sibling to `app/build.gradle.kts`).
   `app/google-services.json.example` shows the expected structure for reference.
4. Re-sync Gradle. The `com.google.gms.google-services` plugin is applied automatically once the
   file is present (see the conditional `apply(plugin = ...)` in `app/build.gradle.kts`) — the
   project builds and runs fine without it, just without push notifications.

`app/google-services.json` is git-ignored; never commit your real file.

## Pointing the app at a backend

`BuildConfig.BASE_URL` is set per build type in `app/build.gradle.kts`:

| Build type | Default `BASE_URL` |
|---|---|
| `debug` | `http://10.0.2.2:8000/api/` — the Android emulator's alias for your host machine's `localhost:8000`, matching the `backend/` service's default local port. |
| `release` | `https://api.neosky.example.com/api/` — a placeholder to override at release-build time (e.g. via a CI-injected `buildConfigField` or product flavor for your real production host). |

Running a debug build against the `backend/` service already running on your host machine (see
`../backend/README.md`) needs no changes. Running on a **physical device** instead of the
emulator: change `10.0.2.2` to your host machine's LAN IP in `app/build.gradle.kts`'s `debug`
block (and the matching entry in `app/src/main/res/xml/network_security_config.xml`, which
currently only allows cleartext HTTP to `10.0.2.2`/`localhost`).

## Demo login

Once the backend is running and seeded (`backend/scripts`, see its README):

- **Email**: `aniket@throttle.aero`
- **Password**: `NeoSky@123`

## Running tests

```bash
# Unit tests (JVM, no emulator needed)
./gradlew testDebugUnitTest

# Instrumented tests (needs a connected device or running emulator)
./gradlew connectedDebugAndroidTest
```

Unit tests live under `app/src/test/` (JUnit4, MockK, kotlinx-coroutines-test, Turbine).
The one Compose UI test lives under `app/src/androidTest/`.

## Project structure

```
app/src/main/java/com/neosky/servicesupport/
  core/            network (auth/refresh interceptors, NetworkResult), datastore (secure tokens),
                   di (Hilt modules), util (formatters, status-color mapper, connectivity)
  data/            remote (Retrofit ApiService + DTOs), local (Room entities/DAOs), repository
                   (impl classes), sync (WorkManager flight-log sync worker)
  domain/          model (plain Kotlin domain classes + enums), repository (interfaces),
                   usecase (validation/calculation/orchestration only)
  presentation/    navigation, theme, common (shared composables), and one package per feature
                   (auth, dashboard, drones, tickets, flights, warranty, maintenance, invoices,
                   documents, notifications, search, profile)
  fcm/             Firebase messaging service + notification-type -> screen deep-link mapping
```

Bottom navigation: **Home · My Drones · Tickets · Flight Log · Profile**. Inside a drone:
**Overview · Flight History · Warranty · Maintenance · Service History · Documents** tabs.

## Offline behaviour

- Drones, tickets, flight logs and notifications are cached in Room as read-through caches, so
  they remain viewable offline.
- Logging a flight while offline writes to Room immediately (`PENDING_SYNC`) and enqueues a
  `FlightSyncWorker` (WorkManager, constrained to `NetworkType.CONNECTED`) that uploads it once
  connectivity returns, using the flight's client-generated UUID for idempotency.
- An app-wide banner (`OfflineBanner`) appears whenever connectivity drops.

## Known limitations / deliberate deviations

See the implementation report for the full list; in short: `CustomerRepository`/`DashboardRepository`
concerns were combined into a single `CustomerRepository` (not explicitly named in the original
architecture list, but needed to back Profile/Dashboard cleanly); a couple of admin/engineer-only
endpoints from `API_SPEC.md` §14 are intentionally not implemented, since this app is customer-only.
