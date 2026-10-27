# Flight Info

An Android app for looking up flight information, built for passengers and people picking someone up at Vietnamese airports.
Final project for the Mobile Application Development course at USTH (topic #30: Flight Info).

- **Platform:** Android, Java 17, XML Views (Activity / Fragment)
- **SDK:** minSdk 24 · targetSdk 34 · compileSdk 37
- **Build:** Android Gradle Plugin 9.4.1, Gradle 9.6.0 (wrapper included)

## Features

| # | Feature |
|---|---|
| F1 | Search flights by number (VN215, VJ123…) with recent search history |
| F2 | Flight details: boarding pass, status, gate/terminal, METAR weather, auto-refresh every 60s |
| F3 | Airport list with Departures / Arrivals boards |
| F4 | My Flights: save / unsave flights (SQLite), swipe to delete with Undo |
| F5 | Settings: English / Vietnamese, data source, altitude unit, default airport |
| F6 | Demo (mock) mode, with automatic fallback to demo data when the API fails |
| F7 | Live Map: aircraft currently flying over Vietnam (OpenSky + osmdroid) |
| F8 | Sign in / sign up with Firebase Authentication, guest mode, separate My Flights per account |
| F9 | Add a flight to the Calendar app |
| F10 | My Flights statistics with a custom-drawn bar chart |
| F11 | Flight reminders: a notification before departure |
| F12 | Great-circle route map in the flight details screen |

Full specifications (in Vietnamese): [docs/SPEC.md](docs/SPEC.md), [docs/SPEC_PHASE7.md](docs/SPEC_PHASE7.md).

## Setup

### 1. Requirements
- A recent Android Studio (tested with Android Studio 2026.2) with Android SDK 37.
- JDK 17 or newer. Android Studio ships with one (JBR). To run Gradle from a terminal on a machine without Java:
  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # macOS
  ```

### 2. AirLabs API key (optional)
Live flight data comes from [AirLabs](https://airlabs.co) (free tier). The key is **never** committed to git.

```bash
cp local.properties.example local.properties
```
Then add your key to `local.properties`:
```properties
sdk.dir=/Users/<you>/Library/Android/sdk
AIRLABS_API_KEY=your_key_here
```
Without a key the app still works: in Auto mode it falls back to demo data and shows a "Showing demo data" banner.

### 3. Firebase (sign-in, F8)
The team's Firebase configuration is already in the repo at `app/google-services.json`, so email sign-in works out of the box.

To use your own Firebase project instead:
1. In the [Firebase Console](https://console.firebase.google.com), create a project and add an Android app with the package name `vn.edu.usth.flightinfo`.
2. Authentication → Sign-in method → enable **Email/Password**.
3. Download `google-services.json` and replace the file in `app/`.

Without this file the app still builds and runs, but only **guest mode** is available; the sign-in screen says Firebase is not configured.

### 4. Build and run
- **Android Studio:** open the project root, wait for the Gradle sync to finish, then press Run.
- **Terminal:**
  ```bash
  ./gradlew assembleDebug        # debug APK in app/build/outputs/apk/debug/
  ./gradlew installDebug         # install on the running emulator / connected device
  ```

For the emulator, set Device Manager → Edit → Graphics to **Hardware** and RAM to 4 GB; otherwise the maps are very slow.

## Testing

```bash
./gradlew testDebugUnitTest    # unit tests (JUnit 4 + Robolectric)
./gradlew lintDebug            # Android Lint
```
The first test run can take 10 minutes or more while Gradle downloads Robolectric.

The unit tests cover: the AirLabs / OpenSky / METAR parsers, flight status mapping, live/demo data selection,
`TimeUtils`, `GeoUtils` (distance, bearing, great-circle interpolation, route bounds), the SQLite migration,
the sign-in screens, `FlightStats` and the F11 reminders.

## Demo tips

Before presenting, switch **Settings → Data source → Demo data only**, so the demo depends on neither the network nor the API quota.

- Flight numbers available in the demo data: `VN215`, `VJ123`, `QH201`, `VN161`, `VJ501`, `BL6020`, `TG561`, `SQ193`…
- Demo flight times are shifted to the current time, so flights always have a realistic status (in flight, landed…).
- In demo mode the Live Map shows about 23 aircraft over Vietnam.
- Signing in needs a network connection. Without one, choose "Continue as guest".
- F11: debug builds offer an extra **1 minute** reminder option to test notifications quickly.

## Architecture

```
ui/        Activities, Fragments, Adapters              (callbacks always run on the main thread)
  │
data/      FlightRepository · MapRepository · WeatherRepository · FallbackLoader (Live / Demo / Auto)
  ├─ remote/  ApiClient (Volley) · AirLabsService · OpenSkyService · MetarService
  ├─ parser/  AirLabsParser · OpenSkyParser · MetarParser · AirportParser
  ├─ mock/    MockDataSource · MockTimeShifter          (res/raw/mock_*.json)
  ├─ local/   SavedFlightsDb (SQLite) · AirportStore (assets) · RecentSearches · Prefs · ReminderStore
  ├─ auth/    AuthManager (Firebase Auth + guest mode)
  └─ stats/   FlightStats
model/     Flight · FlightStatus · Airport · Weather · AircraftState · Result<T>
util/      TimeUtils · GeoUtils · UnitUtils · Airlines (ICAO callsign → IATA) · AppExecutors
```

- **No custom backend.** The app calls public APIs directly and uses Firebase for authentication.
  Saved flights and reminders are stored on the device.
- **Threading:** Volley performs network calls asynchronously. SQLite and file access run on an `ExecutorService`,
  with results posted back to the main thread through a `Handler`. Debug builds enable `StrictMode`.
- **UI:** a "Liquid Glass" style in night blue. The tab bar and the map's bottom sheet use a real blur (BlurView) on Android 12+.

### Data sources

| Source | Used for |
|---|---|
| [AirLabs API v9](https://airlabs.co) | Flights, airport schedules |
| [OpenSky Network](https://opensky-network.org) | Aircraft positions for the Live Map |
| [aviationweather.gov](https://aviationweather.gov) | METAR weather |
| [pics.avs.io](https://pics.avs.io) | Airline logos |
| [OpenStreetMap](https://www.openstreetmap.org) | Map tiles (© OpenStreetMap contributors) |
| `assets/airports.json` | Airport list (extracted from OurAirports) |

### Main libraries
AppCompat, Material Components, RecyclerView, ViewPager2, SwipeRefreshLayout, Preference,
Volley 1.2.1, osmdroid 6.1.20, BlurView 3.2.0, Firebase Auth (BoM 35.1.0), Lifecycle ViewModel/LiveData.
Testing: JUnit 4, Robolectric 4.16.1, `org.json`.

## Permissions

| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | API calls |
| `POST_NOTIFICATIONS` | Flight reminders (Android 13+, requested the first time a reminder is set) |
| `RECEIVE_BOOT_COMPLETED` | Restore pending reminders after the device restarts |
| `USE_EXACT_ALARM` (Android 13+), `SCHEDULE_EXACT_ALARM` (Android 12) | Deliver reminders on time, even when the device is idle. Without it, reminders use a 10-minute window |

## Project documents

All documents in `docs/` are written in Vietnamese.

| File | Content |
|---|---|
| [docs/SPEC.md](docs/SPEC.md) | Specification for F1–F7 |
| [docs/SPEC_PHASE7.md](docs/SPEC_PHASE7.md) | Specification for the second round, F8–F12 |
| [docs/PLAN.md](docs/PLAN.md), [docs/PLAN_PHASE7.md](docs/PLAN_PHASE7.md) | Implementation plan by phase |
| [docs/TASKS_PHASE7.md](docs/TASKS_PHASE7.md) | Task split for the second round |
| `docs/F8_IMPLEMENTATION.md` … `docs/F11_IMPLEMENTATION.md`, `docs/F9_TESTING.md` | Implementation and testing notes per feature |
