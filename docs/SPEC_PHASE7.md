# Flight Info: Spec đợt 2 (Phase 7, F8–F12)

Bổ sung cho [SPEC.md](SPEC.md). Đợt này thêm 5 tính năng để chia cho 5 thành viên:

| # | Tính năng | Bắt buộc | Độ khó |
|---|---|---|---|
| F8 | Đăng nhập / đăng ký (Firebase Authentication) + chế độ khách | ✅ | ★★★ |
| F9 | Thêm chuyến bay vào Lịch | ✅ | ★ |
| F10 | Thống kê My Flights (biểu đồ tự vẽ) | | ★★ |
| F11 | Nhắc giờ bay (notification trước giờ cất cánh) | | ★★ |
| F12 | Bản đồ hành trình trong Detail | | ★★ |

Kế hoạch thực hiện và phân công: [PLAN_PHASE7.md](PLAN_PHASE7.md).

## 1. Quy tắc chung
- **Giao diện:** dùng lại style kính có sẵn: `Widget.FlightInfo.Glass` (card), `Widget.FlightInfo.Button.Primary` (nút chính), `Widget.FlightInfo.TextInput` (ô nhập, đã là mặc định của theme), `Widget.FlightInfo.Toolbar`, `Widget.FlightInfo.SectionHeader`. Không thêm màu hard-code. Màu mới (nếu cần) để trong `colors.xml`.
- **Chuỗi:** mọi chuỗi có đủ EN (`values/strings.xml`) và VI (`values-vi/strings.xml`). Mỗi tính năng thêm chuỗi thành **một khối riêng có comment tên tính năng**, ví dụ `<!-- F9 Calendar -->`, để merge không bị xung đột.
- **Luồng:** I/O (SQLite, file) chạy qua `AppExecutors.io()`, kết quả trả về main thread. Network dùng Volley qua repository như các phần trước.
- **Không phá tính năng cũ:** chế độ Mock và Auto vẫn chạy như trước. Xoay màn hình không mất dữ liệu, không crash.
- **Commit:** mỗi tính năng một nhánh `feature/<tên>`. Commit message rõ ràng.

## 2. F8: Đăng nhập (Firebase Authentication)

### Cài đặt Firebase (trưởng nhóm làm 1 lần)
1. [Firebase Console](https://console.firebase.google.com): tạo project, thêm app Android với package `vn.edu.usth.flightinfo`.
2. Authentication → Sign-in method → bật **Email/Password**.
3. Tải `google-services.json`, đặt vào `app/`, **commit lên repo** để cả nhóm build được. File này không phải mật khẩu: nó chỉ định danh project, quyền truy cập do Firebase quản lý.
4. Gradle:
   - root `build.gradle`: `id 'com.google.gms.google-services' version '4.5.0' apply false`
   - `app/build.gradle`: plugin `com.google.gms.google-services`, `implementation platform('com.google.firebase:firebase-bom:35.1.0')`, `implementation 'com.google.firebase:firebase-auth'`

### Màn hình
- **LoginActivity**
  - Logo app + tiêu đề, trên một tấm kính.
  - Ô **Email**, ô **Mật khẩu** (có nút hiện/ẩn: `endIconMode="password_toggle"`).
  - Nút **Đăng nhập** → `signInWithEmailAndPassword`.
  - Link **Quên mật khẩu?** → `sendPasswordResetEmail(email)`, báo "Đã gửi email đặt lại mật khẩu".
  - Link **Tạo tài khoản** → RegisterActivity.
  - Nút phụ **Dùng thử không cần đăng nhập** → lưu cờ khách, vào MainActivity.
- **RegisterActivity**
  - Tên hiển thị, Email, Mật khẩu, Nhập lại mật khẩu.
  - Nút **Tạo tài khoản** → `createUserWithEmailAndPassword`, rồi `updateProfile(displayName)`, rồi vào MainActivity.
- **Kiểm tra đầu vào** (hiện lỗi ngay trên ô nhập, trước khi gọi Firebase): email đúng dạng (`Patterns.EMAIL_ADDRESS`), mật khẩu ≥ 6 ký tự, hai mật khẩu khớp, tên không rỗng.
- **Khi đang gửi:** hiện ProgressBar, khoá nút để tránh bấm nhiều lần.
- **Lỗi từ Firebase** được dịch sang câu dễ hiểu (EN/VI), hiện bằng Snackbar:

| Exception | Thông báo |
|---|---|
| `FirebaseAuthInvalidCredentialsException`, `FirebaseAuthInvalidUserException` | Email hoặc mật khẩu không đúng |
| `FirebaseAuthUserCollisionException` | Email này đã có tài khoản |
| `FirebaseAuthWeakPasswordException` | Mật khẩu quá yếu |
| `FirebaseNetworkException` | Không có kết nối mạng |
| khác | Lỗi chung (`error_generic`) |

### Luồng mở app
- `MainActivity.onCreate`: nếu `FirebaseAuth.getInstance().getCurrentUser() == null` **và** chưa chọn chế độ khách thì `startActivity(LoginActivity)` với `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`, rồi `finish()`.
- Đăng nhập hoặc đăng ký thành công: mở MainActivity với cùng cờ, để nút Back không quay lại màn login.
- Gom logic vào `data/auth/AuthManager` (singleton):
  - `currentUser()`, `isGuest()`, `ownerId()` (uid, hoặc `"guest"`).
  - `signIn`, `register`, `resetPassword`, `signOut`, `continueAsGuest`.
  - Callback dùng `Callback<T>` có sẵn.

### My Flights theo từng tài khoản
- `SavedFlightsDb`: thêm cột `owner TEXT NOT NULL`. Khoá chính đổi thành `(owner, flight_iata)`. `DB_VERSION = 2`.
- Migration `onUpgrade` 1 → 2: tạo bảng mới, chép dữ liệu cũ với `owner = 'guest'`, xoá bảng cũ, đổi tên bảng mới. **Không** xoá dữ liệu người dùng.
- Mọi hàm của `SavedFlightsDb` nhận thêm `owner`. `FlightRepository` lấy `owner` từ `AuthManager.ownerId()`, nên các màn khác không phải sửa gì.

### Settings
- Nhóm mới **Tài khoản** ở đầu:
  - Dòng thông tin: tên + email (hoặc "Khách").
  - Nút **Đăng xuất** (khi đã đăng nhập) → hộp thoại xác nhận → `signOut` → về LoginActivity (clear task).
  - Nút **Đăng nhập** (khi là khách) → LoginActivity.

### Kiến thức thể hiện
Nhiều Activity + Intent flags, callback bất đồng bộ (`Task.addOnCompleteListener`), SharedPreferences, SQLite migration.

## 3. F9: Thêm vào Lịch
- Màn Detail: thêm action **📅 Thêm vào lịch** (`ic_event`, `showAsAction="ifRoom"`) vào `menu_flight_detail.xml`.
- Bấm vào → Intent ngầm định:
  ```java
  new Intent(Intent.ACTION_INSERT)
      .setData(CalendarContract.Events.CONTENT_URI)
      .putExtra(CalendarContract.Events.TITLE, "VN215 HAN → SGN")
      .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, depMillis)
      .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, arrMillis)
      .putExtra(CalendarContract.Events.EVENT_LOCATION, "Noi Bai International Airport")
      .putExtra(CalendarContract.Events.DESCRIPTION, "Vietnam Airlines · Terminal 1 · Gate 11 · On time");
  ```
- **Giờ:** dùng `getBestDepTs()` / `getBestArrTs()` (giờ cập nhật nếu có), nhân 1000 để ra mili giây. Thiếu giờ đến thì đặt giờ kết thúc = giờ đi + thời lượng (hoặc + 1 giờ).
- **Chuỗi tiêu đề, mô tả** lấy từ `strings.xml` (có format), không hard-code.
- Không có app Lịch (`ActivityNotFoundException`) → Snackbar "Không tìm thấy ứng dụng Lịch".
- Code gom trong `ui/detail/CalendarHelper` (`static Intent buildIntent(Context, Flight, Airport dep)`). Trong FlightDetailActivity chỉ thêm một nhánh xử lý trong `onOptionsItemSelected`.
- Không cần quyền gì.

### Kiến thức thể hiện
Intent ngầm định, Intent extras, `startActivity` sang app khác.

## 4. F10: Thống kê My Flights
- **Lối vào:** thẻ kính **"Xem thống kê"** ở đầu tab My Flights (ẩn khi danh sách trống). Bấm vào → `StatsActivity`.
- **StatsActivity:**
  - Toolbar có nút Back. Nội dung trong `NestedScrollView`.
  - **Lưới 2×2 ô số liệu** (card kính):
    - Số chuyến.
    - Tổng quãng đường (km, great-circle giữa hai sân bay).
    - Tổng giờ bay (từ `duration`, hoặc giờ đến − giờ đi).
    - Tỉ lệ đúng giờ (% chuyến không trễ ≥ 15 phút và không bị huỷ).
  - **Biểu đồ cột** "Số chuyến theo hãng" (top 5, các hãng còn lại gộp thành "Khác"). Mỗi cột có logo hoặc mã hãng ở dưới.
  - Dòng **"Sân bay đi nhiều nhất"**: mã + tên thành phố + số lần.
- **`BarChartView`** (custom View, `ui/stats/`):
  - `setData(List<Bar> bars)` với `Bar(label, value)`.
  - `onDraw`: trục gốc mảnh, cột bo góc trên với gradient `brand_primary_light → brand_primary`, nhãn dưới cột, giá trị trên cột.
  - `onTouchEvent`: chạm vào cột nào thì cột đó sáng lên (màu `brand_accent`) và hiện bong bóng số.
  - Kích thước theo `dp`/`sp`. Có `contentDescription` tóm tắt dữ liệu.
- **Logic tính toán** tách thành class thuần Java `data/stats/FlightStats` (không dùng Android API) để viết unit test:
  - `GeoUtils.distanceKm(lat1, lon1, lat2, lon2)` (haversine).
  - `FlightStats.from(List<Flight>, Map<String, Airport>)` → các số liệu ở trên.
- **Dữ liệu:** `FlightRepository.getSavedFlights` (đã lọc theo tài khoản) và `AirportStore.load`, tính trên `AppExecutors.io()`.

### Kiến thức thể hiện
Custom View (`onDraw`, `onMeasure`), xử lý chạm, Executor + Handler, unit test.

## 5. F11: Nhắc giờ bay
- Màn Detail: action **🔔 Nhắc tôi** (`ic_alarm`).
- Hộp thoại (style kính, dùng `AlertDialog` của theme) cho chọn: **30 phút / 1 giờ / 2 giờ / 3 giờ trước khi cất cánh**. Nếu chuyến đã có lời nhắc thì có thêm dòng **Huỷ nhắc** và hiện giờ đã đặt.
- **Đặt nhắc:** `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, triggerAt, pendingIntent)`. Cách này cho phép lệch vài phút, nên **không cần** quyền `SCHEDULE_EXACT_ALARM`.
  - `triggerAt = bestDepTs*1000 − lead`. Nếu thời điểm này đã qua thì báo Snackbar "Đã quá thời điểm nhắc".
  - PendingIntent dùng `FLAG_IMMUTABLE`, `requestCode = flightIata.hashCode()` (mỗi chuyến một lời nhắc).
- **`ReminderReceiver`** (BroadcastReceiver, `exported=false`):
  - Tạo kênh `flight_reminders` (Android 8+).
  - Hiện notification: tiêu đề "VN215 sắp cất cánh", nội dung "13:35 · HAN → SGN · Gate 11", icon `ic_flight`.
  - Bấm vào → FlightDetailActivity (PendingIntent, `FLAG_IMMUTABLE`).
  - Xoá lời nhắc đã dùng khỏi danh sách lưu.
- **Quyền:** Android 13+ cần `POST_NOTIFICATIONS`. Xin bằng `ActivityResultContracts.RequestPermission` ngay lúc đặt nhắc lần đầu. Bị từ chối thì Snackbar giải thích. Không chặn việc lưu lời nhắc.
- **Lưu lời nhắc:** SharedPreferences (`ReminderStore`: flightIata → triggerAt + chuỗi JSON của chuyến bay).
- **`BootReceiver`** (`RECEIVE_BOOT_COMPLETED`): đặt lại các lời nhắc chưa tới giờ sau khi máy khởi động lại.
- **Code:** `ui/reminder/ReminderScheduler`, `ReminderReceiver`, `BootReceiver`, `data/local/ReminderStore`.
- **Mẹo để test:** thêm lựa chọn ẩn "1 phút" (chỉ hiện ở debug build: `BuildConfig.DEBUG`).

### Kiến thức thể hiện
BroadcastReceiver, AlarmManager, Notification + NotificationChannel, PendingIntent, runtime permission.

## 6. F12: Bản đồ hành trình trong Detail
- **Thẻ kính "Hành trình"** trong Detail, nằm dưới boarding pass (`view_flight_info.xml`, phía trên mục "Flight details").
  - Bên trong là một `MapView` cao 180dp, bo góc, **không nhận cử chỉ** (`setOnTouchListener` trả `true`, tắt multi-touch).
  - Bấm vào thẻ → `RouteMapActivity` toàn màn hình (zoom và kéo được, nút Back).
- **Nội dung bản đồ** (cả thẻ nhỏ lẫn toàn màn hình):
  - Đường bay **great-circle** giữa hai sân bay, khoảng 64 điểm (`GeoUtils.interpolate`):
    - Phần đã bay: `Polyline` nét liền màu `brand_primary_light`, dày 3dp.
    - Phần còn lại: nét đứt (`DashPathEffect`), màu `text_hint`.
  - **Máy bay** (`ic_flight`, màu `brand_accent`) đặt tại điểm tương ứng với % đã bay (`TimeUtils.progressPercent`), xoay theo `GeoUtils.bearing` tại điểm đó. Chưa cất cánh thì đặt ở sân bay đi; đã hạ cánh thì đặt ở sân bay đến.
  - Hai **chấm sân bay** có nhãn mã IATA (dùng lại cách vẽ ở Live Map).
  - Bản đồ tự zoom vừa khung (`zoomToBoundingBox` có lề).
- **Toạ độ sân bay** lấy từ `AirportStore`. Thiếu toạ độ thì ẩn thẻ.
- **Dùng chung với Live Map:** chuyển bộ lọc màu xanh đêm, phần nạp cấu hình osmdroid và hàm vẽ icon sân bay từ `LiveMapFragment` sang `ui/map/MapStyle`. Phần này làm ở bước chuẩn bị (xem PLAN).
- **Unit test:** `GeoUtils.interpolate`, `GeoUtils.bearing` (HAN → SGN khoảng 160–170°).

### Kiến thức thể hiện
osmdroid Polyline/Marker, tính toán hình học, Activity + Intent extras.

## 7. Quyền và manifest (tổng hợp)

| Thêm | Tính năng | Ghi chú |
|---|---|---|
| `LoginActivity`, `RegisterActivity` | F8 | `exported=false`. MainActivity vẫn là launcher. |
| `StatsActivity` | F10 | parent = MainActivity |
| `RouteMapActivity` | F12 | parent = FlightDetailActivity |
| `ReminderReceiver` | F11 | `exported=false` |
| `BootReceiver` + `RECEIVE_BOOT_COMPLETED` | F11 | normal permission |
| `POST_NOTIFICATIONS` | F11 | runtime, Android 13+ |

## 8. Tiêu chí hoàn thành
1. **F8:**
   - Đăng ký tài khoản mới → đăng xuất → đăng nhập lại được.
   - Sai mật khẩu, email đã dùng, mất mạng: báo lỗi dễ hiểu, không crash.
   - Hai tài khoản trên cùng máy thấy My Flights riêng.
   - Chế độ khách dùng được mọi tính năng.
   - Nâng cấp từ bản cũ không mất chuyến đã lưu (thành của khách).
2. **F9:** bấm 📅 trên VN215 thì app Lịch mở ra với tiêu đề, giờ đi, giờ đến, địa điểm đúng.
3. **F10:**
   - Lưu ≥ 3 chuyến của 2 hãng: các số liệu đúng (kiểm bằng tay).
   - Chạm vào cột thì hiện số.
   - Xoay màn hình không mất dữ liệu.
   - Unit test `FlightStats` và `GeoUtils.distanceKm` pass.
4. **F11:**
   - Đặt nhắc "1 phút" (debug) thì notification hiện đúng giờ. Bấm vào mở đúng Detail.
   - Huỷ nhắc thì không còn notification.
   - Từ chối quyền thông báo thì app không crash.
5. **F12:**
   - Detail VN215 có thẻ hành trình HAN → SGN, máy bay nằm đúng khoảng % đã bay và hướng về SGN.
   - Bấm vào mở bản đồ lớn.
   - Unit test `interpolate` và `bearing` pass.
6. **Chung:** chuỗi đủ EN/VI, lint không có lỗi, `./gradlew testDebugUnitTest` pass, không có vùng đỏ lớn khi bật Debug GPU Overdraw.
