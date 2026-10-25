# Flight Info: Implementation Plan

Dựa trên [SPEC.md](SPEC.md). Mỗi phase kết thúc ở trạng thái **build được và chạy được**, rồi commit (commit message rõ ràng, vì thầy có xem).

Ký hiệu: 🧪 = cách kiểm tra phase đã xong.

## Tiến độ
| Phase | Trạng thái |
|---|---|
| 0. Khung project | ✅ Xong. Đã nâng lên AGP 9.4.1 + Gradle 9.6.0, compileSdk 37 để chạy với Android Studio 2026.2 (JDK 25) |
| 1. Model + Parser + Mock | ✅ Xong, 29 unit test pass (`./gradlew testDebugUnitTest`) |
| 2. Data layer | ✅ Xong, build được |
| 3. Main + Search + Detail | ✅ Xong. Đã chạy trên emulator: tìm VN215 ra Detail, xoay ngang giữ dữ liệu |
| 4. Airports + Board | ✅ Xong. Đã chạy trên emulator: danh sách sân bay, Board HAN |
| 4.5 Giao diện Liquid Glass | ✅ Xong. Áp cho Search, Detail, Airports, Board. Đã chạy trên emulator |
| 5. My Flights + Settings | ✅ Xong. Đã chạy trên emulator: lưu, vuốt xoá + Undo, tắt app vẫn còn, đổi tiếng Việt |
| 6–7 | ⏳ Chưa làm |

---

## Phase 0: Khung project
- `settings.gradle`, `build.gradle` (root + app), `gradle.properties`, `gradle/wrapper/*`, `.gitignore`, `local.properties.example`
- `AndroidManifest.xml` (INTERNET, ACCESS_NETWORK_STATE, `FlightInfoApp`, các activity, locales config)
- `FlightInfoApp extends Application`: khởi tạo `ApiClient`, osmdroid config, StrictMode (debug)
- `BuildConfig.AIRLABS_API_KEY` đọc từ `local.properties`
- Theme, `colors.xml`, `dimens.xml`, `strings.xml` + `values-vi`, `values-night`
- 🧪 Android Studio sync được, app chạy ra màn hình trống có Toolbar

## Phase 1: Model + Parser + Mock (lõi dữ liệu, chưa có UI)
- `model/`: `Flight`, `FlightStatus`, `Airport`, `Weather`, `AircraftState`, `DataSource`, `Result<T>`
- `parser/`: `AirLabsParser` (flight, schedules, error), `OpenSkyParser`, `MetarParser`
- `res/raw/`: `mock_flights.json`, `mock_opensky.json`, `mock_metar.json`
- `assets/airports.json`
- `util/`: `TimeUtils` (parse `yyyy-MM-dd HH:mm`, dời ngày về hôm nay, tính % bay), `UnitUtils`, `CallsignMapper`, `AppExecutors`
- Unit test: parser, status mapping, callsign mapping, TimeUtils
- 🧪 `./gradlew test` pass

## Phase 2: Data layer
- `remote/ApiClient` (RequestQueue + ImageLoader singleton, timeout 10s, retry 1 lần, huỷ request theo tag)
- `remote/AirLabsService`, `OpenSkyService`, `MetarService`
- `mock/MockDataSource`
- `local/AirportStore`, `SavedFlightsDb`, `RecentSearches`, `Prefs`
- `FlightRepository`, `MapRepository`, `WeatherRepository`: logic Auto / Live / Mock + fallback
- 🧪 Unit test cho logic chọn nguồn dữ liệu (Repository nhận source qua interface để test được)

## Phase 3: Main shell + Search + Detail (F1, F2, F6)
- `MainActivity`: Toolbar, menu (Refresh, Settings), BottomNavigation, quản lý 4 fragment bằng show/hide, banner mock
- `SearchFragment` + `RecentAdapter`
- `FlightDetailActivity` + `layout-land`, 9-patch boarding pass, chip trạng thái, METAR expand/collapse, Save/Share/Refresh, auto-refresh theo lifecycle
- 🧪 Mock mode: tìm VN215 → Detail đầy đủ; xoay màn hình không mất dữ liệu

## Phase 4: Airports + Board (F3)
- `AirportsFragment` (lọc, ghim sân bay mặc định), `AirportAdapter`
- `AirportBoardActivity` + `BoardPagerAdapter` (ViewPager2 + TabLayoutMediator), `BoardFragment`, `FlightRowAdapter`, SwipeRefresh
- 🧪 Board HAN có 2 tab vuốt được, bấm vào dòng mở Detail

## Phase 4.5: Giao diện Liquid Glass
- Theme tối, nền gradient 1 bitmap (`tools/gen_window_bg.py`), status bar trong suốt
- Drawable kính: card, pill, nút tròn toolbar, nút chính gradient xanh, ô METAR; 9-patch vé kính (`tools/gen_ninepatch.py`)
- Thanh tab nổi dùng BlurView (`RenderEffect`, Android 12+), máy cũ dùng nền gần đặc
- Chip trạng thái kính nhuộm màu (`StatusUi`), TabLayout dạng công tắc kính
- Màn mới (Phase 5–6) dùng các style `Widget.FlightInfo.*` sẵn có
- 🧪 Debug GPU Overdraw: card 1 lớp (xanh dương), thanh tab tối đa 2 lớp

## Phase 5: My Flights + Settings (F4, F5)
- `MyFlightsFragment`: SQLite, refresh trạng thái, swipe-to-delete + Undo, empty state
- `SettingsActivity` + `SettingsFragment` (PreferenceFragmentCompat), đổi ngôn ngữ, data mode, đơn vị, sân bay mặc định, About
- 🧪 Lưu → kill app → vẫn còn. Đổi sang VI → toàn bộ UI là tiếng Việt

## Phase 6: Live Map (F7)
- `LiveMapFragment`: osmdroid MapView, marker máy bay xoay theo hướng bay, marker sân bay, bottom sheet thông tin, nút "Xem chuyến bay", refresh 30s khi tab đang hiển thị, ô đếm số máy bay
- Chuyển `Configuration.getInstance().load(...)` của osmdroid ra khỏi `FlightInfoApp.onCreate` (StrictMode báo đọc/ghi đĩa trên main thread lúc mở app)
- 🧪 Mock: có ~25 máy bay. Live: có dữ liệu từ OpenSky

## Phase 7: Hoàn thiện & tối ưu
- Rà overdraw (bỏ background thừa), `tools:` attrs, contentDescription, kiểm tra hard-coded string (Lint)
- App icon (adaptive, vector)
- `README.md`: cách cài, đặt API key, chạy, cấu trúc project
- Kiểm tra lại toàn bộ Acceptance trong SPEC §9
- Ghi chú cho slide: sơ đồ kiến trúc, bảng Activity, phần Networking (Volley async, REST/JSON), ảnh chụp overdraw trước/sau

---

## Chia việc cho nhóm 5 người (sau khi Phase 0–2 xong)

| Người | Phần | Phase |
|---|---|---|
| 1 | Data layer, parser, mock, test | 1, 2 |
| 2 | MainActivity, Search, Flight Detail | 3 |
| 3 | Airports, Airport Board | 4 |
| 4 | Live Map | 6 |
| 5 | My Flights, Settings, i18n, overdraw, slide | 5, 7 |

## Rủi ro
| Rủi ro | Cách xử lý |
|---|---|
| Hết quota AirLabs | Mode Auto tự fallback sang mock; demo bằng Mock mode |
| OpenSky bị rate limit hoặc chậm | Fallback sang `mock_opensky.json`, refresh không dưới 30s |
| Free tier AirLabs thiếu field (gate, delay…) | UI hiện "—" cho field trống; mock có đủ field để demo |
| Mạng lúc demo kém | Bật Mock mode trước khi lên trình bày |
| Version AGP/Gradle lệch với Android Studio đang cài | Dùng AGP ổn định. Nếu Studio đề nghị nâng cấp (AGP Upgrade Assistant) thì chấp nhận |
