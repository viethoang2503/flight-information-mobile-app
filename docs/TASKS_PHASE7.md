# Phase 7: Phân công task cho 5 người

Mỗi người làm **1 tính năng** trên một nhánh riêng. Chi tiết yêu cầu: [SPEC_PHASE7.md](SPEC_PHASE7.md). Kế hoạch tổng: [PLAN_PHASE7.md](PLAN_PHASE7.md).

| Người | Tính năng | Nhánh | Thời gian |
|---|---|---|---|
| 1 | F8 Đăng nhập (Firebase) | `feature/login` | 2–3 ngày |
| 2 | F9 Thêm vào Lịch (+ Onboarding nếu kịp) | `feature/calendar` | ½–1 ngày (+1 ngày) |
| 3 | F10 Thống kê My Flights | `feature/stats` | 2 ngày |
| 4 | F11 Nhắc giờ bay | `feature/reminder` | 2 ngày |
| 5 | F12 Bản đồ hành trình | `feature/route-map` | 2 ngày |

---

## Quy tắc chung (ai cũng phải đọc)

1. **Chỉ bắt đầu sau khi trưởng nhóm báo bước 7.0 đã merge vào `main`.** Bước này đã chuẩn bị sẵn Firebase, `MapStyle`, `GeoUtils`, và các "móc" trong màn Detail.
2. Tạo nhánh từ `main` mới nhất:
   ```bash
   git checkout main && git pull
   git checkout -b feature/<tên>
   ```
3. Emulator: Device Manager → Edit → **Graphics = Hardware**, RAM 4 GB. Không chỉnh thì app rất giật.
4. Giao diện: dùng style có sẵn, không tự đặt màu:
   - `Widget.FlightInfo.Glass`: card kính.
   - `Widget.FlightInfo.Button.Primary`: nút xanh chính.
   - `Widget.FlightInfo.Toolbar`, `Widget.FlightInfo.SectionHeader`.
   - Ô nhập (TextInputLayout) đã tự có kiểu kính.
5. Chuỗi: không hard-code. Thêm vào **cả** `values/strings.xml` (EN) **và** `values-vi/strings.xml` (VI), thành **một khối riêng ở cuối file**, có comment tên tính năng:
   ```xml
   <!-- F9 Calendar -->
   <string name="action_add_calendar">Add to calendar</string>
   ```
6. Đọc file, đọc SQLite: chạy qua `AppExecutors.get().io(...)`, rồi trả kết quả về `main(...)`. Không làm trên main thread.
7. Không sửa code của phần khác. Với `FlightDetailActivity`, chỉ điền vào hàm "móc" dành cho mình.
8. Trước khi tạo Pull Request:
   ```bash
   git pull origin main
   ./gradlew assembleDebug testDebugUnitTest lintDebug
   ```
   Cả ba phải pass. PR kèm **ảnh chụp màn hình** chạy trên emulator, và nhờ một người khác review.
9. Commit message rõ ràng, ví dụ `F9: add flight to calendar from detail screen`.

---

## Người 1: F8 Đăng nhập (Firebase Authentication)

**Nhánh:** `feature/login`. **Kiến thức trình bày trên slide:** nhiều Activity + Intent flags, Firebase Auth, callback bất đồng bộ, SQLite migration.

### Task
- [ ] **1. `data/auth/AuthManager`** (singleton)
  - Bọc `FirebaseAuth`.
  - Hàm: `currentUser()`, `isGuest()`, `ownerId()` (uid, hoặc `"guest"`), `signIn`, `register`, `resetPassword`, `signOut`, `continueAsGuest`.
  - Cờ "khách" lưu trong SharedPreferences.
  - Callback dùng `Callback<T>` có sẵn.
- [ ] **2. `LoginActivity`** + `activity_login.xml`
  - Ô email, ô mật khẩu (`app:endIconMode="password_toggle"`), nút **Đăng nhập**.
  - Link **Quên mật khẩu?** → `sendPasswordResetEmail`.
  - Link **Tạo tài khoản**.
  - Nút **Dùng thử không cần đăng nhập**.
- [ ] **3. `RegisterActivity`** + `activity_register.xml`
  - Tên hiển thị, email, mật khẩu, nhập lại mật khẩu.
  - Đăng ký → `createUserWithEmailAndPassword` → `updateProfile(displayName)`.
- [ ] **4. Kiểm tra đầu vào**, lỗi hiện ngay trên ô nhập: email đúng dạng (`Patterns.EMAIL_ADDRESS`), mật khẩu ≥ 6 ký tự, hai mật khẩu khớp, tên không rỗng.
- [ ] **5. Khi đang gửi:** ProgressBar + khoá nút.
- [ ] **6. Dịch lỗi Firebase** sang câu dễ hiểu, hiện bằng Snackbar:
  - Sai email hoặc mật khẩu.
  - Email đã có tài khoản.
  - Mật khẩu yếu.
  - Mất mạng.
  - Lỗi chung.
- [ ] **7. `MainActivity`:** chưa đăng nhập và không phải khách → mở `LoginActivity` với `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`, rồi `finish()`. Đăng nhập xong mở lại Main với cùng cờ.
- [ ] **8. `SavedFlightsDb` version 2**
  - Thêm cột `owner`. Khoá chính đổi thành `(owner, flight_iata)`.
  - Migration 1 → 2 chép dữ liệu cũ thành `owner = 'guest'`, **không được xoá dữ liệu**.
  - `FlightRepository` truyền `AuthManager.ownerId()`.
- [ ] **9. Settings:** thêm nhóm **Tài khoản** ở đầu.
  - Dòng tên + email (hoặc "Khách").
  - **Đăng xuất**: có hộp thoại xác nhận, xong về Login.
  - Khi đang là khách thì hiện **Đăng nhập**.
- [ ] **10. Manifest:** khai báo `LoginActivity` và `RegisterActivity` (`exported=false`). Thêm chuỗi EN/VI.

### Tự kiểm tra
- [ ] Đăng ký → tắt app → mở lại vẫn đang đăng nhập.
- [ ] Đăng xuất → về màn Login, bấm Back không quay lại app.
- [ ] Hai tài khoản trên cùng máy thấy My Flights riêng.
- [ ] Sai mật khẩu, email đã dùng, tắt mạng: có thông báo, không crash.
- [ ] Chế độ khách dùng được mọi tính năng.
- [ ] Cài đè lên bản cũ đã có chuyến lưu: vào chế độ khách vẫn thấy các chuyến đó.

---

## Người 2: F9 Thêm vào Lịch (+ Onboarding nếu kịp)

**Nhánh:** `feature/calendar`. **Kiến thức trình bày trên slide:** Intent ngầm định, Intent extras (+ ViewPager2, SharedPreferences nếu làm Onboarding).

### Task
- [ ] **1. `ui/detail/CalendarHelper`**: `static Intent buildIntent(Context, Flight, Airport dep)`
  - `Intent.ACTION_INSERT` + `CalendarContract.Events.CONTENT_URI`.
  - Tiêu đề: `VN215 HAN → SGN`.
  - Giờ bắt đầu: `getBestDepTs() * 1000`. Giờ kết thúc: `getBestArrTs() * 1000`.
  - Địa điểm: tên sân bay đi.
  - Mô tả: hãng · terminal · gate · trạng thái.
  - Mọi chữ lấy từ `strings.xml`.
- [ ] **2.** Thiếu giờ đến → giờ kết thúc = giờ đi + thời lượng chuyến (không có thì + 1 giờ).
- [ ] **3.** Điền vào hàm móc `onAddToCalendar()` trong `FlightDetailActivity`: `startActivity(...)`, bắt `ActivityNotFoundException` → Snackbar "Không tìm thấy ứng dụng Lịch".
- [ ] **4.** Chuỗi EN/VI.

### Phần thêm nếu kịp: Onboarding
- [ ] `OnboardingActivity`: ViewPager2 với 3 trang (Tìm chuyến bay, Bản đồ trực tiếp, Lưu chuyến bay), mỗi trang icon + tiêu đề + mô tả trên tấm kính.
- [ ] Chấm chỉ trang (TabLayout + TabLayoutMediator), nút **Bỏ qua** và **Tiếp** / **Bắt đầu**.
- [ ] Chỉ hiện lần đầu: lưu cờ `onboarding_done` trong SharedPreferences. `MainActivity` kiểm tra cờ này.

### Tự kiểm tra
- [ ] VN215 (chế độ Mock) → bấm 📅 → app Lịch mở với tiêu đề "VN215 HAN → SGN", đúng giờ đi, giờ đến, địa điểm.
- [ ] Chuyến thiếu giờ đến: không crash, sự kiện dài theo thời lượng chuyến.
- [ ] (Onboarding) Cài mới → hiện 3 trang. Mở lại lần sau → không hiện nữa.

---

## Người 3: F10 Thống kê My Flights

**Nhánh:** `feature/stats`. **Kiến thức trình bày trên slide:** Custom View (`onMeasure`, `onDraw`, Canvas/Paint), xử lý chạm (`onTouchEvent`), Executor + Handler, unit test.

### Task
- [ ] **1. `data/stats/FlightStats`** (Java thuần, không dùng Android API) + **unit test**:
  - Số chuyến.
  - Tổng quãng đường (km, dùng `GeoUtils.distanceKm`).
  - Tổng giờ bay.
  - Tỉ lệ đúng giờ (không trễ ≥ 15 phút, không bị huỷ).
  - Top 5 hãng (còn lại gộp thành "Khác").
  - Sân bay đi nhiều nhất.
- [ ] **2. `ui/stats/BarChartView`** (custom View):
  - `setData(List<Bar>)`, với `Bar(label, value)`.
  - `onDraw`: cột bo góc gradient xanh, nhãn dưới cột, số trên cột.
  - `onTouchEvent`: chạm vào cột thì cột đó sáng màu amber và hiện bong bóng số.
  - Kích thước dùng dp/sp, có `contentDescription`.
- [ ] **3. `StatsActivity`** + layout:
  - Toolbar có nút Back. Nội dung trong `NestedScrollView`.
  - Lưới 2×2 ô số liệu (card kính), biểu đồ "Số chuyến theo hãng", dòng "Sân bay đi nhiều nhất".
- [ ] **4.** Lấy dữ liệu bằng `FlightRepository.getSavedFlights` + `AirportStore.load`, tính trên `AppExecutors.io()`.
- [ ] **5.** Thẻ kính **"Xem thống kê"** ở đầu tab My Flights (`MyFlightsFragment`), ẩn khi danh sách trống.
- [ ] **6.** Manifest: khai báo `StatsActivity` (parent = MainActivity). Thêm chuỗi EN/VI.

### Tự kiểm tra
- [ ] Lưu VN215, VJ123, QH201 → các số liệu khớp với tính tay.
- [ ] Chạm vào cột thì hiện số. Chạm chỗ khác thì bỏ chọn.
- [ ] Xoay màn hình: không mất dữ liệu, không crash.
- [ ] `./gradlew testDebugUnitTest` pass (có test cho `FlightStats`).

---

## Người 4: F11 Nhắc giờ bay

**Nhánh:** `feature/reminder`. **Kiến thức trình bày trên slide:** BroadcastReceiver, AlarmManager, Notification + NotificationChannel, PendingIntent, xin quyền lúc chạy.

### Task
- [ ] **1. `data/local/ReminderStore`** (SharedPreferences): lưu flightIata → thời điểm nhắc + JSON chuyến bay.
- [ ] **2. `ui/reminder/ReminderScheduler`**:
  - `schedule(flight, leadMinutes)`: `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, ...)`.
  - `cancel(flightIata)`.
  - PendingIntent dùng `FLAG_IMMUTABLE`, `requestCode = flightIata.hashCode()`.
  - Thời điểm nhắc đã qua → báo lỗi, không đặt.
- [ ] **3. `ReminderReceiver`**:
  - Tạo kênh `flight_reminders`.
  - Notification "VN215 sắp cất cánh", nội dung "13:35 · HAN → SGN · Gate 11".
  - Bấm vào mở FlightDetailActivity.
  - Xoá lời nhắc khỏi `ReminderStore` sau khi đã hiện.
- [ ] **4. `BootReceiver`**: sau khi khởi động lại máy, đặt lại các lời nhắc chưa tới giờ.
- [ ] **5.** Điền vào hàm móc `onRemindMe()` trong `FlightDetailActivity`:
  - Hộp thoại chọn **30 phút / 1 giờ / 2 giờ / 3 giờ**.
  - Chuyến đã có nhắc thì thêm dòng **Huỷ nhắc**.
  - Có thêm lựa chọn **"1 phút"** chỉ ở debug build (`BuildConfig.DEBUG`) để test.
- [ ] **6.** Android 13+: xin quyền `POST_NOTIFICATIONS` bằng `ActivityResultContracts.RequestPermission`. Bị từ chối thì Snackbar giải thích, không crash.
- [ ] **7.** Manifest:
  - 2 receiver (`exported=false`).
  - Quyền `POST_NOTIFICATIONS` và `RECEIVE_BOOT_COMPLETED`.
  - `BootReceiver` có intent-filter `BOOT_COMPLETED`.
  - Thêm chuỗi EN/VI.

### Tự kiểm tra
- [ ] Đặt nhắc "1 phút" → khoảng 1 phút sau notification hiện. Bấm vào mở đúng Detail của chuyến.
- [ ] Huỷ nhắc → không còn notification.
- [ ] Từ chối quyền thông báo → không crash, có giải thích.
- [ ] Đặt nhắc → khởi động lại emulator → lời nhắc vẫn hiện đúng giờ.

---

## Người 5: F12 Bản đồ hành trình trong Detail

**Nhánh:** `feature/route-map`. **Kiến thức trình bày trên slide:** osmdroid Polyline/Marker, tính toán toạ độ (great-circle), Activity + Intent extras, vòng đời MapView.

### Task
- [ ] **1. `ui/detail/RouteMapHelper`**: hàm dùng chung cho thẻ nhỏ lẫn màn lớn, nhận `MapView`, sân bay đi/đến, % đã bay.
  - Đường bay great-circle khoảng 64 điểm (`GeoUtils.interpolate`). Phần đã bay: nét liền màu `brand_primary_light`. Phần còn lại: nét đứt màu `text_hint`.
  - Máy bay (`ic_flight`, màu `brand_accent`) đặt theo % đã bay, xoay theo `GeoUtils.bearing`.
  - Hai chấm sân bay có nhãn: dùng `MapStyle.airportIcon`.
  - Zoom vừa khung (`zoomToBoundingBox` có lề). Áp `MapStyle.applyNightStyle`.
- [ ] **2. Thẻ kính "Hành trình"** đặt vào `route_card_container` (đã có sẵn trong `view_flight_info.xml`):
  - MapView cao 180dp, không nhận cử chỉ.
  - Ẩn thẻ khi thiếu toạ độ sân bay. Toạ độ lấy từ `AirportStore`.
- [ ] **3. `RouteMapActivity`**: bản đồ toàn màn hình (zoom và kéo được, nút Back), nhận chuyến bay qua Intent extras. Bấm vào thẻ thì mở màn này.
- [ ] **4.** Vòng đời: gọi `mapView.onResume()` / `onPause()` / `onDetach()` đúng chỗ, cả trong Detail lẫn RouteMapActivity.
- [ ] **5.** Nạp cấu hình osmdroid bằng `MapStyle.loadConfigThen(...)` trước khi tạo MapView.
- [ ] **6.** Manifest: khai báo `RouteMapActivity` (parent = FlightDetailActivity). Thêm chuỗi EN/VI.

### Tự kiểm tra
- [ ] Detail VN215 (Mock) có thẻ hành trình HAN → SGN. Máy bay nằm đúng khoảng % đã bay, mũi hướng về SGN.
- [ ] Bấm vào thẻ → mở bản đồ lớn, zoom và kéo được.
- [ ] Xoay màn hình: không crash.
- [ ] Chuyến có sân bay không có trong `airports.json` → thẻ bị ẩn, không crash.

---

## Sau khi xong (cả nhóm)

1. Trưởng nhóm merge theo thứ tự **F9 → F12 → F11 → F10 → F8** (ít xung đột trước, F8 đổi DB nên để cuối).
2. Chạy lại toàn bộ tiêu chí trong `SPEC.md` §9 và `SPEC_PHASE7.md` §8.
3. Mỗi người làm **1–2 slide** về phần mình: tính năng làm gì, dùng kiến thức gì của môn, ảnh chụp màn hình.
