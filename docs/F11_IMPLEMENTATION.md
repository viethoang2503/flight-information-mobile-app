# F11 – Nhắc giờ bay

Đã kiểm tra ngày 26/10/2026 trên nhánh `feature/reminder`. Không commit, push,
merge, rebase hoặc reset. Các thay đổi chưa commit và file untracked được giữ lại.

## Môi trường đã thiết lập

- Eclipse Temurin JDK 17.0.20.1+1 x64, ZIP chính thức Adoptium, đã kiểm tra SHA-256.
- MSI chính thức đã kiểm tra SHA-256 và chữ ký Eclipse Foundation; cài MSI thất bại
  với mã 1603 / lỗi 1925 (thiếu quyền cài cho tất cả người dùng). Đã dùng ZIP dự phòng.
- JDK nằm trong thư mục người dùng; JAVA_HOME/PATH chỉ được đặt cho phiên build.
- Android Command-line Tools build 15859902 từ Google, đã kiểm tra SHA-256.
- Đã được người dùng đồng ý cài SDK và chấp nhận giấy phép.
- SDK Platform API 37.0, Build Tools 36.0.0, platform-tools đã cài.
- `local.properties` chứa sdk.dir với đường dẫn Windows được escape; Git bỏ qua file.
- Giữ nguyên Gradle 9.6.0, AGP 9.4.1, compileSdk 37 và targetSdk 34.

Để mở phiên PowerShell build mới với JDK ZIP đã cài:

```powershell
$env:JAVA_HOME = Join-Path $env:LOCALAPPDATA 'flightinfo-tools/jdk-17.0.20.1+1'
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
java -version
javac -version
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

## File thuộc F11

- `app/src/main/java/vn/edu/usth/flightinfo/data/local/ReminderStore.java`
- `app/src/main/java/vn/edu/usth/flightinfo/ui/reminder/ReminderScheduler.java`
- `app/src/main/java/vn/edu/usth/flightinfo/ui/reminder/ReminderReceiver.java`
- `app/src/main/java/vn/edu/usth/flightinfo/ui/reminder/BootReceiver.java`
- `app/src/main/java/vn/edu/usth/flightinfo/ui/detail/FlightDetailActivity.java`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/menu/menu_flight_detail.xml`
- `app/src/main/res/drawable/ic_alarm.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-vi/strings.xml`
- `app/src/test/java/vn/edu/usth/flightinfo/ui/reminder/ReminderTest.java`

Tái sử dụng `ic_flight.xml`, model Flight, AirLabsParser, TimeUtils và AppExecutors.
Không sửa layout `view_flight_info.xml` hoặc tính năng khác.

## Hành vi

Action Nhắc tôi mở AlertDialog theo theme hiện tại; chọn 30 phút, 1 giờ, 2 giờ,
3 giờ trước cất cánh; debug có thêm 1 phút trước cất cánh. Lời nhắc đã đặt hiển thị
thời điểm và cho phép huỷ. Thời điểm đã qua hoặc dữ liệu không hợp lệ bị từ chối.

AlarmManager dùng setAndAllowWhileIdle(RTC_WAKEUP), PendingIntent immutable,
requestCode theo hashCode mã chuyến. Mỗi mã chuyến có một lời nhắc lưu bằng
SharedPreferences kèm triggerAt và JSON. Không yêu cầu exact-alarm permission.

NotificationChannel flight_reminders hiển thị mã chuyến, giờ đi, tuyến và cửa;
bấm thông báo mở đúng Detail, giữ chế độ Mock. Receiver xoá lời nhắc đã xử lý.
Huỷ nhắc huỷ cả alarm và notification. Android 13+ xin POST_NOTIFICATIONS qua
ActivityResultContracts.RequestPermission; từ chối không chặn việc lưu lời nhắc.
BootReceiver khôi phục lịch còn hiệu lực trên AppExecutors với goAsync/finish.

## Kết quả đã chạy

- assembleDebug: thành công; APK tại `app/build/outputs/apk/debug/app-debug.apk`.
- testDebugUnitTest: 54 test, 0 failure, 0 error, 0 skipped; trong đó 8 test F11.
- lintDebug: thành công sau khi sửa escape sdk.dir và chạy lại phân tích với
  `--rerun-tasks` vì lần chạy thường còn tái sử dụng kết quả cũ.
- XML resource hợp lệ; 16 chuỗi EN/VI và tham số định dạng khớp.
- git diff --check: đạt.

Test F11 chạy bằng Robolectric, bao gồm đặt/đổi/huỷ lịch, dữ liệu lỗi, thời điểm đã
qua, notification mở Detail, Mock, quyền Android 13 và logic khôi phục lịch reboot.
Không coi đây là kiểm thử trên thiết bị thật.

adb devices đã chạy, không có thiết bị kết nối. Chưa kiểm tra thực tế: hộp thoại,
xin/từ chối quyền, Doze và thời gian nhận thông báo, bấm notification, xoay màn hình,
khởi động lại máy. Lựa chọn debug 1 phút nghĩa là trước giờ cất cánh 1 phút theo
công thức spec; không phải 1 phút sau khi bấm. Alarm không chính xác có thể đến trễ.

Cảnh báo còn lại: dependency/target SDK, layout/accessibility/icon ngoài F11;
thiếu cấu hình Firebase; thư viện native không strip được; API test Robolectric
deprecated. Không thay đổi các phần này hoặc tắt kiểm tra lint.

Báo cáo chi tiết: `app/build/reports/tests/testDebugUnitTest/index.html` và
`app/build/reports/lint-results-debug.html` (được Git bỏ qua).
