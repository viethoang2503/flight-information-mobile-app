# Flight Info: Product & Technical Spec

USTH Mobile Application Development, Final Project (topic #30: Flight Info)
Platform: Android · Language: **Java** · UI: **XML Views** (Activity / Fragment)

---

## 1. Mục tiêu

App tra cứu thông tin chuyến bay cho hành khách và người đi đón ở Việt Nam:

- Biết chuyến bay của mình đang ở trạng thái nào (đúng giờ, trễ, đã cất cánh, đã hạ cánh), ở gate/terminal nào.
- Xem bảng bay đi/đến của một sân bay giống bảng điện tử ở sân bay.
- Xem máy bay đang bay trên bầu trời Việt Nam theo thời gian thực.
- **Luôn demo được**: khi API hết quota hoặc mất mạng, app tự chuyển sang dữ liệu mẫu (mock).

Ngoài ra, app phải thể hiện được các kiến thức của course để trình bày theo template của thầy: Architecture → Activities → Networking → Optimization (overdraw) → Demo.

## 2. Phạm vi

### Trong phạm vi (final)
| # | Tính năng | Ưu tiên |
|---|---|---|
| F1 | Tra cứu chuyến bay theo mã (VN215, VJ123…) + lịch sử tìm kiếm | MVP |
| F2 | Chi tiết chuyến bay | MVP |
| F3 | Danh sách sân bay + bảng bay Departures / Arrivals | MVP |
| F4 | My Flights: lưu / bỏ lưu chuyến bay | MVP |
| F5 | Settings: ngôn ngữ EN/VI, nguồn dữ liệu, đơn vị, sân bay mặc định | MVP |
| F6 | Mock mode + tự động fallback | MVP |
| F7 | Live Map: máy bay trên bầu trời Việt Nam (OpenSky + osmdroid) | Mở rộng (đã chốt làm) |

### Ngoài phạm vi (ghi vào phần "Future work" của slide)
Push notification khi đổi trạng thái, sân bay gần tôi (Location), đặt vé, đăng nhập, widget màn hình chính.

## 3. Nguồn dữ liệu

| Nguồn | Dùng cho | Endpoint | Auth |
|---|---|---|---|
| **AirLabs** v9 | F1, F2, F3 | `GET https://airlabs.co/api/v9/flight?flight_iata=VN215&api_key=KEY`<br>`GET https://airlabs.co/api/v9/schedules?dep_iata=HAN&api_key=KEY` (hoặc `arr_iata`) | API key (free tier) |
| **OpenSky Network** | F7 | `GET https://opensky-network.org/api/states/all?lamin=8&lomin=102&lamax=24&lomax=110` | Ẩn danh (có rate limit) |
| **aviationweather.gov** | Thời tiết trong F2 | `GET https://aviationweather.gov/api/data/metar?ids=VVTS&format=json` | Không |
| **Logo hãng bay** | F1–F4 | `https://pics.avs.io/200/200/{IATA}.png` | Không |
| **assets/airports.json** | F3, F7, F2 | File tĩnh (~40 sân bay VN + khu vực, trích từ OurAirports) | Không |
| **res/raw/mock_*.json** | F6 | Dữ liệu mẫu cùng format với AirLabs / OpenSky | Không |

**API key:** đặt trong `local.properties` (`AIRLABS_API_KEY=...`), Gradle đưa vào `BuildConfig.AIRLABS_API_KEY`. Không commit key lên git.

**Response AirLabs:** thường có dạng `{"request":{…},"response":…}`, khi lỗi thì `{"error":{"message","code"}}`. Parser chấp nhận cả `response` là object, là array, hoặc toàn bộ body là array.

**Trạng thái chuyến bay:** AirLabs trả về `scheduled | active | en-route | landed | cancelled`. App quy đổi sang enum `FlightStatus`:
`SCHEDULED, DELAYED (scheduled mà trễ ≥ 15'), EN_ROUTE (active/en-route), LANDED, CANCELLED, UNKNOWN`. Nếu API không trả `dep_delayed` thì số phút trễ được tính từ `dep_estimated_ts − dep_time_ts`.

## 4. Chế độ dữ liệu (F6)

Setting `data_mode` có 3 giá trị:

| Mode | Hành vi |
|---|---|
| **Auto** (mặc định) | Gọi API thật. Nếu lỗi (không có key, hết quota, lỗi mạng, timeout 10s) thì dùng mock và hiện banner *"Đang hiển thị dữ liệu mẫu"* |
| **Live** | Chỉ gọi API thật. Nếu lỗi thì hiện màn hình lỗi kèm nút Retry |
| **Mock** | Chỉ dùng mock, không gọi mạng. Dùng khi demo |

Mock data:
- `mock_flights.json`: 30 chuyến giữa HAN, SGN, DAD, CXR, PQC, HPH, VII, BKK, SIN, ICN, NRT, đủ mọi trạng thái. Dữ liệu được viết quanh một mốc thời gian cố định (`anchor_ts`). Khi load, mọi mốc giờ được **dời theo (bây giờ − anchor)**, làm tròn 5 phút, nên demo luôn trông như dữ liệu hiện tại và trạng thái luôn khớp với giờ.
- Dữ liệu mock được sinh bằng `tools/gen_mock.py`.
- `mock_opensky.json`: 23 máy bay trên không phận VN (gồm các chuyến đang bay trong mock_flights). Máy bay được cho "bay" theo hướng và vận tốc, cứ 30 phút lặp lại một lần.
- `mock_metar.json`: METAR cho các sân bay chính.

Mỗi kết quả trả về UI đều kèm `DataSource { LIVE, MOCK }` để UI biết lúc nào phải hiện banner.

## 5. Màn hình & luồng

```
MainActivity (Toolbar + BottomNavigation)
 ├─ SearchFragment ──────┐
 ├─ AirportsFragment ──── AirportBoardActivity (ViewPager2 + TabLayout: BoardFragment ×2)
 ├─ LiveMapFragment ─────┤                │
 └─ MyFlightsFragment ───┴──────────► FlightDetailActivity
Toolbar menu: Refresh · Settings ──► SettingsActivity (PreferenceFragmentCompat)
```

### 5.1 MainActivity
- Toolbar: tiêu đề theo tab, action **Refresh** (`ifRoom`), **Settings** (`never`, nằm trong overflow).
- BottomNavigationView: Search, Airports, Map, My Flights, đặt trong thanh kính nổi (bo tròn, làm mờ nội dung cuộn bên dưới). Đổi tab bằng `FragmentManager` (show/hide, giữ state).
- Banner mock (viên thuốc kính có chấm vàng "Showing demo data") nằm trên Fragment container, chỉ hiện khi kết quả đến từ mock.

### 5.2 SearchFragment (F1)
- Ô nhập mã chuyến bay (EditText, viết hoa, IME action Search), nút Search.
- Validate theo regex `^[A-Z0-9]{2}\d{1,4}$`. Sai thì báo lỗi ngay trên ô nhập.
- Bên dưới là danh sách **Recent** (tối đa 10, lưu trong SharedPreferences), mỗi item có logo hãng, mã chuyến, tuyến, giờ, chip trạng thái.
- Khi đang tải: ProgressBar. Khi tìm thành công: mở FlightDetailActivity. Không tìm thấy: hiện Snackbar.

### 5.3 FlightDetailActivity (F2)
Nhận extra `EXTRA_FLIGHT_IATA` và (tuỳ chọn) `EXTRA_FLIGHT_JSON` để hiện ngay không cần gọi lại API.
- Header: logo hãng (Volley `NetworkImageView`), tên hãng, mã chuyến.
- **Boarding-pass card** (nền 9-patch): `HAN ──✈── SGN`, tên thành phố, giờ dự kiến và giờ cập nhật (giờ dự kiến gạch ngang nếu bị trễ), thanh tiến trình % bay tính theo `dep_time_ts / arr_time_ts / now`.
- Lưới thông tin: Status (chip màu), Delay, Terminal/Gate đi, Terminal/Gate/Baggage đến, Aircraft, Registration, Duration.
- Thời tiết sân bay đến (METAR): nhiệt độ, gió, tầm nhìn, flight category, chuỗi raw METAR (bấm vào để mở rộng/thu gọn, dùng `LayoutParams`, khớp bài Touch).
- Toolbar actions: ☆ Lưu/Bỏ lưu (F4), Share (Intent `ACTION_SEND`), Refresh.
- Auto-refresh mỗi 60s khi màn hình đang hiển thị: bắt đầu ở `onResume()`, dừng ở `onPause()`.
- Xoay màn hình: lưu chuyến bay hiện tại trong `onSaveInstanceState`. Có `layout-land`: boarding-pass card bên trái, thông tin bên phải.

### 5.4 AirportsFragment + AirportBoardActivity (F3)
- AirportsFragment: RecyclerView danh sách sân bay đọc từ `assets/airports.json` (đọc ở background thread, trả kết quả về qua Handler). Có ô lọc theo mã/tên/thành phố. Sân bay mặc định (từ Settings) được ghim lên đầu.
- AirportBoardActivity: tiêu đề "Nội Bài (HAN)", **TabLayout + ViewPager2** với 2 trang `BoardFragment(type=DEPARTURES|ARRIVALS)`.
- Mỗi dòng: giờ (giờ cũ gạch ngang nếu trễ), mã chuyến, logo, sân bay đi/đến, terminal/gate, chip trạng thái. Font monospace cho giờ và mã chuyến, giống bảng điện tử.
- SwipeRefreshLayout. Sắp xếp theo giờ. Bấm vào một dòng thì mở FlightDetailActivity.

### 5.5 LiveMapFragment (F7)
- osmdroid `MapView`, khung ban đầu là Việt Nam. Pinch zoom và kéo bản đồ (multi-touch).
- Marker máy bay là icon xoay theo `true_track`. Bấm vào marker thì hiện bottom sheet: callsign, quốc gia, độ cao (m/ft theo Settings), tốc độ (km/h), vertical rate, nút **"Xem chuyến bay"**. Nút này đổi callsign ICAO sang IATA (`HVN215` → `VN215`, theo bảng `HVN→VN, VJC→VJ, BAV→QH, PIC→BL, VAG→VU`, …) rồi mở FlightDetailActivity.
- Tự refresh mỗi 30s khi tab đang hiển thị. Có ô đếm số máy bay đang hiển thị.
- Sân bay trong `airports.json` được vẽ thành marker nhỏ.

### 5.6 MyFlightsFragment (F4)
- RecyclerView các chuyến bay đã lưu (SQLite). Khi mở tab sẽ refresh trạng thái từng chuyến.
- Vuốt sang trái để xoá (ItemTouchHelper), có Snackbar "Undo".
- Khi danh sách trống: icon + câu hướng dẫn lưu chuyến bay.

### 5.7 SettingsActivity (F5)
| Key | Kiểu | Mặc định |
|---|---|---|
| `language` | List: System / English / Tiếng Việt | System |
| `data_mode` | List: Auto / Live / Mock | Auto |
| `altitude_unit` | List: m / ft | m |
| `default_airport` | List (từ airports.json) | HAN |
| `auto_refresh` | Switch | On |
| `about` | Thông tin nhóm, nguồn dữ liệu | – |

Đổi ngôn ngữ ngay trong app bằng `AppCompatDelegate.setApplicationLocales()`.

## 6. UI / Visual

Phong cách **Liquid Glass, xanh đêm** (mockup đã duyệt: https://claude.ai/artifact/3j4QGJa1jSrcQY4g5uDJ8R).

- **Theme:** `Theme.MaterialComponents.NoActionBar` (chỉ có bản tối, vì hiệu ứng kính cần nền xanh đậm phía sau) + Toolbar trong suốt.
- **Nền:** gradient navy → xanh đêm có 2 vầng sáng xanh, render sẵn thành **một bitmap** (`tools/gen_window_bg.py`) làm `android:windowBackground`. Status bar trong suốt để gradient chạy lên tận mép trên.
- **Kính:**
  - *Kính giả* cho card, ô nhập, chip: nền trắng trong suốt 18%→5% (sáng ở góc trên trái như phản chiếu), viền 1dp trắng 20%, bo 24dp (`bg_glass_card.xml`). Ripple bo theo góc card.
  - *Mờ thật* (thư viện BlurView, `RenderEffect`) chỉ cho **thanh tab nổi** và bottom sheet trên bản đồ, trên Android 12+. Máy cũ hơn dùng nền gần đặc thay vì blur phần mềm (chậm).
- **Màu (`colors.xml`):** xanh chủ đạo `#2F6BFF` → `#5B95FF` (nút chính gradient), link `#8FB8FF`, chữ trắng / trắng 68%. Amber `#FFC24B` chỉ còn cho trạng thái Delayed và sao "đã lưu". Màu trạng thái (làm sáng cho nền tối): on-time `#5BD68A`, delayed `#FFC24B`, en-route `#5AA9FF`, landed `#9FB1C7`, cancelled `#FF6B6B`. Chip trạng thái = kính nhuộm màu + chấm tròn.
- **Không hard-code**: mọi string, dimen, color đều để trong `res/values*`. Có `values-vi/strings.xml` đầy đủ.
- **Typography:** mã chuyến bay và giờ dùng `monospace`, cỡ trong `dimens.xml`.
- **Drawable:** vector icon, `bg_boarding_pass.9.png` (9-patch vé kính có khấc, `tools/gen_ninepatch.py`), shape/ripple xml cho kính, nút, chip.
- **Overdraw:** gradient nằm trong `windowBackground` (1 bitmap = 1 lần vẽ, thay vì layer-list 3 lớp). Root layout không set background. Card kính chỉ là 1 lớp. Thanh tab: gộp lớp phủ màu vào drawable nền và bỏ nền trong suốt của BottomNavigationView (từ 4 lớp còn 2). Mục tiêu khi bật *Debug GPU Overdraw*: không có vùng đỏ lớn.

## 7. Kiến trúc

```
ui/            Activities, Fragments, Adapters, ViewHolders
  │  (callback, luôn được gọi trên main thread)
data/FlightRepository · MapRepository · WeatherRepository
  │
  ├─ remote/   ApiClient (Volley RequestQueue + ImageLoader singleton, khởi tạo trong FlightInfoApp)
  │            AirLabsService · OpenSkyService · MetarService
  ├─ parser/   AirLabsParser · OpenSkyParser · MetarParser   (JSONObject / JSONArray)
  ├─ mock/     MockDataSource (res/raw, Executor + Handler)
  └─ local/    AirportStore (assets) · SavedFlightsDb (SQLiteOpenHelper) · RecentSearches (SharedPreferences) · Prefs
model/         Flight · FlightStatus · Airport · Weather · AircraftState · Result<T>(data, DataSource)
util/          TimeUtils · UnitUtils · CallsignMapper · Executors
```

- **Package:** `vn.edu.usth.flightinfo`
- **SDK:** minSdk 24, compileSdk 35, targetSdk 34 (giữ cửa sổ kiểu cũ, không bắt buộc edge-to-edge như targetSdk 35). Java 17.
- **Thư viện:** AppCompat, Material, RecyclerView, ViewPager2, SwipeRefreshLayout, Preference, **Volley 1.2.1**, **osmdroid 6.1.x**. Test: JUnit 4 + `org.json:json` (vì `org.json` trên JVM unit test là stub).
- **Threading:**
  - Network: Volley (async, callback chạy trên main thread).
  - Đọc file assets/raw, SQLite: `ExecutorService` + `Handler(Looper.getMainLooper())`. Đây là bản thay thế AsyncTask (đã deprecated) nhưng cùng mô hình Thread/Handler của bài 6.
  - Không làm I/O trên main thread. Bật `StrictMode` ở debug build để kiểm tra.
- **Cache:** Volley disk cache cho response, `ImageLoader` + `LruCache` cho logo.
- **Permissions:** `INTERNET`, `ACCESS_NETWORK_STATE` (cả hai đều là normal permission). osmdroid không cần quyền storage trên API 29+ vì dùng cache trong thư mục riêng của app.

## 8. Yêu cầu phi chức năng
- Không ANR, không crash khi: mất mạng, JSON thiếu field, API trả lỗi, xoay màn hình khi request đang chạy (huỷ request theo tag ở `onStop`).
- Thời gian tới khi hiện dữ liệu: mock < 300ms, live tuỳ mạng (timeout 10s, 1 lần retry).
- Text đọc được ở cỡ chữ hệ thống lớn. Mọi ImageView/icon button có `contentDescription`.

## 9. Tiêu chí hoàn thành (Acceptance)
1. Ở chế độ Mock, tắt mạng: Search "VN215" → Detail hiển thị đủ thông tin. Board HAN có ≥ 5 chuyến mỗi tab. Map có máy bay.
2. Ở chế độ Auto, không có API key: tự fallback sang mock và hiện banner.
3. Ở chế độ Auto, có key và có mạng: dữ liệu live hiển thị, không có banner.
4. Lưu chuyến bay → khởi động lại app → vẫn còn trong My Flights.
5. Đổi ngôn ngữ sang Tiếng Việt: toàn bộ UI là tiếng Việt.
6. Xoay màn hình ở mọi màn: không mất dữ liệu, không crash.
7. Debug GPU Overdraw: không có vùng đỏ lớn.
8. Unit test cho các parser và `FlightStatus` mapping đều pass.
