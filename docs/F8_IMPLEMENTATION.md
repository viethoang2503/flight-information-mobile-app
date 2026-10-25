# F8 — Đăng nhập / đăng ký bằng Firebase

Nhánh: `feature/login`. Phạm vi: F8 trong `SPEC_PHASE7.md` và `TASKS_PHASE7.md`.

## 1. Kết nối Firebase của nhóm

1. Trên [Firebase Console](https://console.firebase.google.com/), mở project của nhóm.
2. Thêm ứng dụng Android với application ID **`vn.edu.usth.flightinfo`**.
3. Authentication → Sign-in method → bật **Email/Password**.
4. Tải cấu hình thật, đặt đúng tại **`app/google-services.json`**, rồi Gradle Sync/build lại.
5. Theo quy ước của spec nhóm, commit file cấu hình này để các thành viên cùng build.

Gradle đã thêm Google Services **4.5.0**, Firebase BoM **35.1.0** và `firebase-auth` theo spec.
Plugin Google Services chỉ được áp dụng khi file cấu hình tồn tại. Không có file: app vẫn build,
hiện thông báo đăng nhập email chưa khả dụng, và dùng chế độ khách được. Không dùng project/key giả.
Nếu file có mặt nhưng sai package/JSON, quá trình build báo lỗi cấu hình thay vì che giấu lỗi.

Tài liệu API đã đối chiếu:
- [Android setup](https://firebase.google.com/docs/android/setup)
- [Email/password authentication](https://firebase.google.com/docs/auth/android/password-auth)
- [User profile and password reset](https://firebase.google.com/docs/auth/android/manage-users)

## 2. Các phần đã triển khai

| Thành phần | Vai trò |
|---|---|
| `data/auth/AuthManager` | Singleton bọc Firebase; đăng nhập, đăng ký, cập nhật tên, reset email, đăng xuất, cờ khách, owner UID |
| `data/auth/AuthException` | Lỗi đặc thù Auth qua `Callback<T>` có sẵn |
| `ui/auth/LoginActivity` | Email/password, quên mật khẩu, tạo tài khoản, vào chế độ khách |
| `ui/auth/RegisterActivity` | Tên, email, mật khẩu, xác nhận mật khẩu |
| `ui/auth/AuthActivity` | Inline validation, loading, chặn gửi trùng, Snackbar, nút Done trên bàn phím |
| `ui/auth/AuthViewModel` | Giữ yêu cầu bất đồng bộ và biểu mẫu khi xoay màn hình; quan sát theo lifecycle |
| `ui/auth/AuthErrorMessages` | Dịch lỗi Firebase sang resource EN/VI; không hiển thị thông báo kỹ thuật từ server |
| `ui/auth/AuthNavigation` | Intent clear task khi đổi phiên, tránh Back về màn của tài khoản trước |
| `MainActivity` | Chưa có phiên và chưa chọn khách → Login |
| `SavedFlightsDb` v2 | Primary key `(owner, flight_iata)`; migration giữ JSON/thời gian lưu cũ cho `guest` |
| `FlightRepository` | Chụp owner trước khi đưa công việc SQLite vào executor; UI gọi API repository như cũ |
| `SettingsFragment` | Nhóm Tài khoản ở đầu: tên/email hoặc Khách; đăng nhập; xác nhận đăng xuất |

Giao diện dùng theme kính xanh đêm hiện có. Chuỗi F8 nằm thành khối riêng ở cuối hai file
`values/strings.xml` và `values-vi/strings.xml`. Form cuộn được khi bàn phím mở hoặc ở landscape;
có nhãn, autofill, nút hiện/ẩn mật khẩu và phản hồi khi đang gửi.

Firebase tự lưu phiên đăng nhập. Mật khẩu chỉ tồn tại tạm trong bộ nhớ/ô nhập, không lưu vào
SharedPreferences hay Bundle. Guest flag được lưu bằng SharedPreferences. My Flights vẫn lưu
**cục bộ trên thiết bị**, tách theo UID, không đồng bộ lên Firestore và không chuyển chuyến của
khách sang tài khoản mới (đúng phạm vi spec).

### Tạo tài khoản thành công nhưng cập nhật tên bị gián đoạn

AuthManager ghi tên đang chờ theo UID trước khi `updateProfile`. Nếu cập nhật lỗi, đăng xuất phiên
vừa tạo và thông báo tài khoản đã tồn tại, cần đăng nhập để hoàn tất. Lần đăng nhập kế tiếp thử lại
`updateProfile`; không yêu cầu tạo lại tài khoản. Nếu ứng dụng bị đóng giữa hai bước, `hasSession()`
không đưa người dùng vào Main khi tên vẫn đang chờ cập nhật.

## 3. Liên hệ với slide môn học

Đã đọc bộ 10 PDF được cung cấp trong thư mục `file slide` bên cạnh repository. Các phần áp dụng trực tiếp:

| Nội dung được học | Áp dụng ở F8 |
|---|---|
| `1. Layout.pdf`: View/ViewGroup, inflate XML, LinearLayout, kích thước và padding | Hai layout XML form đăng nhập/đăng ký; các widget truy cập qua `R.id` |
| `2. RelativeLayout and ViewPagerFile.pdf`: tổ chức layout/màn hình | Tái sử dụng mô hình Activity/Fragment của app; F8 dùng Activity riêng cho mỗi biểu mẫu |
| `3. Values & DrawablesFile p1.pdf`, `p2.pdf`: strings, màu, dimen, locale, drawable | Tái dùng styles/drawables của nhóm; tách EN/VI; dimen thích ứng chiều rộng |
| `4. raw and menuFile p1.pdf`, `p2.pdf`: resource, menu, action trong app | Giữ lối vào Settings từ menu hiện có; thêm nhóm tài khoản vào PreferenceScreen |
| `Networking Embedded PackageFile.pdf`, `Networking Volley & Data representationFile.pdf`: INTERNET, request và callback | Dùng quyền INTERNET hiện có; Auth dùng Firebase SDK theo spec, request chuyến bay tiếp tục qua Volley |
| `Threads & TasksFile.pdf`: main thread, worker thread, Handler, cập nhật UI | SQLite qua `AppExecutors.io`; kết quả qua main Handler; Firebase Task callback cập nhật state/UI không chặn main thread |
| `Advanced Touch InputsFile.pdf`: xử lý tương tác View | Listener cho nút form và password toggle; F8 không cần thêm custom gesture |

Firebase Authentication, migration SQLite và Intent flags là yêu cầu bổ sung của spec F8; bộ slide
được cung cấp không có bài Firebase riêng. `ViewModel` hỗ trợ yêu cầu không mất trạng thái khi xoay,
không thay mô hình Java/XML/Activity/Fragment của môn. Không dùng `AsyncTask` đã deprecated.

## 4. Build và kiểm thử

Cần JDK đầy đủ có `javac`/`jlink`, Android SDK tương thích cấu hình gốc của repo (compileSdk 37).
Thiết lập SDK bằng `local.properties` hoặc biến môi trường `ANDROID_HOME`. Ví dụ:

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
Báo cáo test: `app/build/reports/tests/testDebugUnitTest/index.html`.
Báo cáo lint: `app/build/reports/lint-results-debug.html`.

Kết quả xác minh trên bản triển khai: **assembleDebug thành công, 43 test pass, lint 0 lỗi / 15 cảnh báo**.
Các cảnh báo gồm phiên bản dependency, targetSdk và một số view/resource hiện có; không có lỗi lint F8.
Đã render trực tiếp Android Views bằng Robolectric/Skia để kiểm tra Login EN, Register VI và Login
landscape với font 150% (cuộn xuống vẫn chạm được nút khách). Ảnh ở `app/build/outputs/f8-preview/`;
đây là ảnh render trong test, **không phải ảnh chụp emulator**. Chưa có thiết bị ADB kết nối và chưa có
cấu hình Firebase thật của nhóm, nên các kiểm tra tích hợp bên dưới vẫn để mở.

Các test mới chạy Robolectric với Android API 28:
- `SavedFlightsDbTest`: migration v1 → v2 giữ dữ liệu và `saved_at`, kể cả bản ghi cũ chỉ có ICAO; cùng mã chuyến độc lập giữa
  guest và hai UID; update/delete đúng owner; refresh không thêm bản ghi/đổi thứ tự; bind owner.
- `AuthErrorMessagesTest`: mapping lỗi Firebase, nhất là `WeakPassword` phải kiểm tra trước lớp cha
  `InvalidCredentials`; thông báo cho thiếu cấu hình và cập nhật profile chưa xong.
- `AuthScreensTest`: validation email/password/tên/xác nhận, reset chỉ cần email, guest + clear-task,
  giữ dữ liệu biểu mẫu khi Activity được tạo lại.
- `AuthLayoutTest`: render EN/VI, màn nhỏ và landscape với chữ lớn; xác nhận nút khách vẫn truy cập
  được bằng cách cuộn.

### Kiểm tra với Firebase thật (cần cấu hình nhóm)

- [ ] Đăng ký → tên hiển thị đúng trong Settings.
- [ ] Đóng/mở lại app vẫn đăng nhập; đăng xuất → Login, Back không mở lại màn cũ.
- [ ] Đăng nhập sai mật khẩu; email trùng; mất mạng: thông báo dễ hiểu, nút được mở lại.
- [ ] Quên mật khẩu: email reset thực sự đến hộp thư.
- [ ] Tài khoản A lưu VN215; B không thấy; khách chỉ thấy dữ liệu của khách; đăng nhập lại A vẫn thấy.
- [ ] Chế độ Mock/Auto, Search/Detail/Board/Map hoạt động sau khi vào bằng khách hoặc tài khoản.
- [ ] Xoay máy khi đang gửi đăng nhập/đăng ký/reset: không gửi trùng, không bị kẹt loading.
- [ ] Cài đè APK trên bản v1: các chuyến cũ xuất hiện trong chế độ khách.
- [ ] Chụp ảnh màn hình trên emulator/điện thoại và kiểm tra GPU Overdraw theo checklist nhóm.

## 5. Gợi ý nội dung 2 slide trình bày F8

**Slide 1 — Luồng & kiến trúc**
- Main → Login → Register hoặc Guest; thành công → clear task → Main.
- UI Java/XML → AuthManager → Firebase Task → callback → UI.
- Settings hiển thị người dùng và xác nhận đăng xuất.
- Chèn ảnh Login/Register thực tế sau khi kết nối Firebase.

**Slide 2 — Dữ liệu & xử lý bất đồng bộ**
- SQLite v1 → v2: dữ liệu cũ gắn `guest`, khóa chính `(owner, flight_iata)`.
- Mỗi tài khoản có My Flights riêng; I/O chạy bằng Executor/Handler theo bài Threads & Tasks.
- ViewModel giữ request khi xoay máy; validation và thông báo lỗi EN/VI.
- Demo: A lưu chuyến → B không thấy → khách vẫn có dữ liệu cũ.
