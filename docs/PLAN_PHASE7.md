# Flight Info: Plan đợt 2 (Phase 7, F8–F12)

Dựa trên [SPEC_PHASE7.md](SPEC_PHASE7.md). Danh sách task để gửi từng người: [TASKS_PHASE7.md](TASKS_PHASE7.md). Plan chính (Phase 0–6 và 8) xem ở [PLAN.md](PLAN.md).

Ký hiệu: 🧪 = cách kiểm tra bước đã xong.

## Tiến độ
| Bước | Người | Trạng thái |
|---|---|---|
| 7.0 Chuẩn bị chung | Trưởng nhóm | ⏳ |
| 7.1 F8 Đăng nhập (Firebase) | Người 1 | Đã triển khai; build/test/lint pass; chờ Firebase nhóm và demo thiết bị ([chi tiết](F8_IMPLEMENTATION.md)) |
| 7.2 F9 Thêm vào Lịch | Người 2 | ⏳ |
| 7.3 F10 Thống kê My Flights | Người 3 | ⏳ |
| 7.4 F11 Nhắc giờ bay | Người 4 | ⏳ |
| 7.5 F12 Bản đồ hành trình | Người 5 | ⏳ |
| 7.6 Gộp & kiểm thử chung | Cả nhóm | ⏳ |

---

## Cách làm việc chung
- Mỗi người một nhánh tách từ `main` **sau khi bước 7.0 đã merge**: `feature/login`, `feature/calendar`, `feature/stats`, `feature/reminder`, `feature/route-map`.
- Trước khi mở Pull Request: `git pull origin main` rồi chạy `./gradlew assembleDebug testDebugUnitTest lintDebug`. Cả ba phải pass.
- Mỗi PR cần một người khác review, và kèm ảnh chụp màn hình chạy trên emulator.
- **File dùng chung dễ xung đột khi merge:** `strings.xml` (EN + VI), `AndroidManifest.xml`, `FlightDetailActivity.java`, `menu_flight_detail.xml`, `view_flight_info.xml`.
  - Chuỗi: thêm thành một khối riêng ở **cuối file**, mở đầu bằng comment `<!-- F9 Calendar -->`.
  - Manifest: chỉ thêm thẻ của mình, không sắp xếp lại.
  - FlightDetailActivity: chỉ điền vào hàm "móc" đã tạo sẵn ở bước 7.0, không sửa chỗ khác.
- **Emulator:** Device Manager → Edit → Graphics = **Hardware** (nếu không app rất giật), RAM 4 GB.

## 7.0 Chuẩn bị chung (trưởng nhóm, làm trước, khoảng ½ ngày)
Mục đích: tạo sẵn những phần mà nhiều người cùng đụng vào, để 5 nhánh sau ít xung đột.

1. **Commit Phase 6** (Live Map) lên `main`.
2. **Firebase:**
   - Tạo project, thêm app `vn.edu.usth.flightinfo`, bật Email/Password.
   - Đặt `google-services.json` vào `app/` và commit.
   - Thêm plugin `com.google.gms.google-services` 4.5.0 + `firebase-bom` 35.1.0 + `firebase-auth` vào Gradle. 🧪 Build được.
3. **`ui/map/MapStyle`:** chuyển từ `LiveMapFragment` sang:
   - `NIGHT_BLUE_MATRIX` và `applyNightStyle(MapView)`.
   - `loadConfigThen(Fragment hoặc Activity, Runnable)`.
   - `airportIcon(Context, String iata)` và `dotAnchorU(...)`.
   - 🧪 Live Map vẫn chạy y như cũ.
4. **`GeoUtils`:** thêm `distanceKm`, `bearing`, `interpolate(lat1, lon1, lat2, lon2, fraction)` kèm unit test (F10 và F12 cùng dùng). 🧪 `testDebugUnitTest` pass.
5. **Móc sẵn trong Detail:**
   - `menu_flight_detail.xml` thêm hai item `action_add_calendar` (`ic_event`) và `action_remind` (`ic_alarm`).
   - `FlightDetailActivity` thêm hai hàm rỗng `onAddToCalendar()` và `onRemindMe()` (tạm hiện Toast "Coming soon"), được gọi từ `onOptionsItemSelected`.
   - `view_flight_info.xml` thêm `FrameLayout` id `route_card_container` (tạm `gone`) phía trên mục "Flight details".
6. Cập nhật bảng tiến độ ở trên. Merge vào `main`, báo cả nhóm tách nhánh.

## 7.1 F8 Đăng nhập (Người 1, khoảng 2–3 ngày)
1. `data/auth/AuthManager`: bọc `FirebaseAuth`, cờ khách trong SharedPreferences, `ownerId()`.
2. Layout + `LoginActivity`: kiểm tra đầu vào, ProgressBar, dịch lỗi Firebase, quên mật khẩu, chế độ khách.
3. Layout + `RegisterActivity`: tên hiển thị, `updateProfile`.
4. `MainActivity`: chuyển sang Login khi chưa đăng nhập và không phải khách (clear task).
5. `SavedFlightsDb` v2: cột `owner`, khoá chính `(owner, flight_iata)`, migration giữ dữ liệu cũ thành `guest`. `FlightRepository` truyền `AuthManager.ownerId()`.
6. Settings: nhóm **Tài khoản** (thông tin, Đăng xuất / Đăng nhập), hộp thoại xác nhận đăng xuất.
7. Chuỗi EN/VI.
- 🧪
  - Đăng ký → thoát app → mở lại vẫn đăng nhập.
  - Đăng xuất → về Login.
  - Hai tài khoản có My Flights riêng.
  - Sai mật khẩu hoặc mất mạng: có thông báo, không crash.
  - Cài đè lên bản cũ: chuyến đã lưu vẫn còn khi dùng chế độ khách.

## 7.2 F9 Thêm vào Lịch (Người 2, khoảng ½–1 ngày)
1. `ui/detail/CalendarHelper.buildIntent(Context, Flight, Airport dep)`: tiêu đề, giờ đi/đến (mili giây), địa điểm, mô tả, tất cả lấy từ `strings.xml`.
2. Điền vào `onAddToCalendar()`: `startActivity`, bắt `ActivityNotFoundException` rồi hiện Snackbar.
3. Xử lý trường hợp thiếu giờ đến (giờ đi + thời lượng, hoặc + 1 giờ).
4. Chuỗi EN/VI.
- 🧪 VN215 (mock) → app Lịch mở với tiêu đề "VN215 HAN → SGN" và đúng giờ. Thử trên chuyến thiếu giờ đến: không crash.
- Nếu còn thời gian (khuyến khích): làm thêm phần Onboarding 3 trang (ViewPager2) cho lần đầu mở app.

## 7.3 F10 Thống kê (Người 3, khoảng 2 ngày)
1. `data/stats/FlightStats` (Java thuần) + unit test: số chuyến, tổng km, tổng giờ, tỉ lệ đúng giờ, top hãng, sân bay đi nhiều nhất.
2. `ui/stats/BarChartView`: `onMeasure`, `onDraw` (cột gradient bo góc, nhãn, giá trị), `onTouchEvent` (chọn cột, bong bóng số), `contentDescription`.
3. Layout + `StatsActivity`: lưới 4 ô số liệu, biểu đồ, dòng sân bay. Tính trên `AppExecutors.io()`.
4. Thẻ "Xem thống kê" ở đầu tab My Flights (ẩn khi trống).
5. Chuỗi EN/VI.
- 🧪 Lưu VN215, VJ123, QH201 → số liệu khớp với tính tay. Chạm vào cột thì hiện số. Xoay màn hình vẫn đúng. Unit test pass.

## 7.4 F11 Nhắc giờ bay (Người 4, khoảng 2 ngày)
1. `data/local/ReminderStore` (SharedPreferences).
2. `ui/reminder/ReminderScheduler`: `schedule(flight, leadMinutes)` và `cancel(flightIata)` bằng AlarmManager, PendingIntent `FLAG_IMMUTABLE`.
3. `ReminderReceiver`: tạo kênh thông báo + notification + PendingIntent mở Detail.
4. `BootReceiver`: đặt lại các lời nhắc chưa tới giờ.
5. Điền vào `onRemindMe()`: hộp thoại chọn thời gian, xin quyền `POST_NOTIFICATIONS`, lựa chọn "1 phút" ở debug build.
6. Manifest: 2 receiver + 2 quyền. Chuỗi EN/VI.
- 🧪 Nhắc "1 phút" → notification hiện, bấm vào mở đúng Detail. Huỷ nhắc → không còn notification. Từ chối quyền → không crash. Khởi động lại emulator → lời nhắc vẫn còn.

## 7.5 F12 Bản đồ hành trình (Người 5, khoảng 2 ngày)
1. `ui/detail/RouteMapView` hoặc `RouteMapHelper`: vẽ great-circle (đã bay / còn lại), máy bay theo % và hướng, chấm sân bay, zoom vừa khung, dùng `MapStyle`.
2. Thẻ kính trong `route_card_container`, MapView 180dp, không nhận cử chỉ, ẩn khi thiếu toạ độ.
3. `RouteMapActivity` toàn màn hình (zoom, kéo, Back), nhận dữ liệu chuyến qua Intent extras.
4. Lifecycle: gọi `MapView.onResume/onPause/onDetach` đúng chỗ (thẻ trong Detail lẫn activity).
5. Chuỗi EN/VI.
- 🧪 Detail VN215 có thẻ hành trình, máy bay nằm đúng khoảng % và hướng về SGN. Bấm vào mở bản đồ lớn. Xoay màn hình không crash. Chuyến thiếu toạ độ thì ẩn thẻ.

## 7.6 Gộp & kiểm thử chung (cả nhóm, khoảng 1 ngày)
- **Thứ tự merge gợi ý** (ít xung đột trước): F9 → F12 → F11 → F10 → F8. F8 để cuối vì đổi DB và luồng mở app.
- Chạy lại toàn bộ tiêu chí trong [SPEC.md](SPEC.md) §9 và [SPEC_PHASE7.md](SPEC_PHASE7.md) §8.
- Chế độ Mock + tắt mạng: mọi màn vẫn chạy (riêng login cần mạng, khi đó dùng chế độ khách).
- Debug GPU Overdraw cho các màn mới.
- Cập nhật slide: mỗi người 1–2 slide về phần mình (tính năng, kiến thức của môn, ảnh chụp).

## Phân công

| Người | Tính năng | Nhánh | Kiến thức trình bày trên slide |
|---|---|---|---|
| Trưởng nhóm | 7.0 Chuẩn bị, Firebase, merge | `main` | Kiến trúc, quy trình làm nhóm |
| 1 | F8 Đăng nhập | `feature/login` | Activity + Intent flags, Firebase Auth, SQLite migration |
| 2 | F9 Thêm vào Lịch (+ Onboarding nếu kịp) | `feature/calendar` | Intent ngầm định (+ ViewPager2) |
| 3 | F10 Thống kê | `feature/stats` | Custom View, Canvas, xử lý chạm |
| 4 | F11 Nhắc giờ bay | `feature/reminder` | BroadcastReceiver, AlarmManager, Notification, quyền runtime |
| 5 | F12 Bản đồ hành trình | `feature/route-map` | osmdroid Polyline, tính toán toạ độ |

F9 nhẹ hơn các phần khác. Nên giao cho người kiêm thêm việc làm slide hoặc Onboarding.

## Rủi ro
| Rủi ro | Cách xử lý |
|---|---|
| Thiếu `google-services.json`, build lỗi | Trưởng nhóm commit file ở bước 7.0 trước khi mọi người tách nhánh |
| Lúc demo không có mạng nên không đăng nhập được | Dùng chế độ khách. Hoặc đăng nhập sẵn trước khi demo (phiên đăng nhập được lưu) |
| Xung đột khi merge `strings.xml` / Detail | Khối chuỗi riêng có comment. Chỉ sửa trong hàm móc của bước 8.0. Merge theo thứ tự ở 7.6 |
| Notification không hiện trên emulator | Kiểm tra đã cấp quyền thông báo. Dùng lựa chọn "1 phút" ở debug. Không bật Do Not Disturb |
| Báo thức bị trễ vài phút (Doze) | Chấp nhận được (không dùng exact alarm). Ghi rõ trong slide |
| Migration DB làm mất dữ liệu | Viết migration chép dữ liệu sang bảng mới. Test bằng cách cài đè lên bản cũ đã có chuyến lưu |
