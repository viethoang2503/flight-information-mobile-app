# Frontend của Flight Info: màn hình Android và tài nguyên giao diện

Thư mục này là lối tắt để tìm file khi thuyết trình. `ui/` và `res/` là liên kết đến mã nguồn và tài nguyên gốc trong `app/src/main/`. App vẫn build từ các đường dẫn gốc.

| Muốn tìm gì? | Mở ở đây | File quan trọng |
|---|---|---|
| Màn hình chính, bốn tab | `ui/` | `MainActivity.java`, `TabFragment.java` |
| Tìm chuyến bay | `ui/search/` | `SearchFragment.java` |
| Sân bay và bảng giờ | `ui/airports/` | `AirportsFragment.java`, `AirportBoardActivity.java`, `BoardFragment.java` |
| Chi tiết chuyến bay và lịch | `ui/detail/` | `FlightDetailActivity.java`, `CalendarHelper.java` |
| Bản đồ máy bay | `ui/map/` | `LiveMapFragment.java`, `MapStyle.java` |
| Bản đồ hành trình | `ui/detail/` | `RouteMapActivity.java`, `RouteMapHelper.java` |
| Chuyến đã lưu | `ui/myflights/` | `MyFlightsFragment.java` |
| Đăng nhập và đăng ký | `ui/auth/` | `LoginActivity.java`, `RegisterActivity.java`, `AuthViewModel.java` |
| Thống kê và biểu đồ | `ui/stats/` | `StatsActivity.java`, `BarChartView.java` |
| Cài đặt | `ui/settings/` | `SettingsFragment.java` |
| Nhắc giờ qua Android | `ui/reminder/` | `ReminderScheduler.java`, `ReminderReceiver.java`, `BootReceiver.java` |
| Adapter và kiểu hiển thị dùng chung | `ui/common/` | `FlightAdapter.java`, `StatusUi.java` |
| Bố cục màn hình | `res/layout/`, `res/layout-land/` | XML của Activity, Fragment và từng item |
| Màu, chữ, style, đa ngôn ngữ | `res/values/`, `res/values-vi/` | `styles.xml`, `themes.xml`, `strings.xml` |
| Icon, nền, menu | `res/drawable/`, `res/menu/` | Hình và menu giao diện |
| Cấu hình Settings | `res/xml/` | `preferences.xml` |

Nếu thầy hỏi một nút có tác dụng gì, mở Activity hoặc Fragment xử lý nút trong `ui/`, tìm listener, rồi lần theo repository trong `backend/data/`. Nếu hỏi màn hình được thiết kế ra sao, mở layout XML tương ứng và style/color trong `res/`.

Trong Android Studio, mã nguồn thật vẫn nằm dưới `app > java > vn.edu.usth.flightinfo`, còn XML dưới `app > res`. Xem [tài liệu ôn thi đầy đủ](../docs/FINAL_EXAM_GUIDE_VI.md).
