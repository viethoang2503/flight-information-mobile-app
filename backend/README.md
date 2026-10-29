# Backend của Flight Info: tầng xử lý dữ liệu trong Android

Thư mục này là lối tắt để tìm file khi thuyết trình. Các thư mục con là liên kết đến mã nguồn gốc trong `app/src/main/java/vn/edu/usth/flightinfo/`; chúng không tạo thêm một server và không được Gradle biên dịch lần thứ hai.

Project không có backend tự viết và chạy riêng. App gọi AirLabs, OpenSky và Aviation Weather qua Internet. Firebase Authentication quản lý tài khoản. Phần ở đây xử lý dữ liệu ngay trên điện thoại.

Lý do theo kiến trúc hiện có: các dịch vụ trên đã cung cấp flight data, vị trí aircraft, METAR và tài khoản; các chuyến người dùng lưu chỉ cần SQLite trên máy trong phạm vi hiện tại. Tầng `data/` chọn Live/Mock và fallback, tạo request, parse JSON thành model, đọc/ghi local storage và tính thống kê. Các phép tính như độ trễ, trạng thái và khoảng cách cũng chạy trong app. Không có một process server riêng của nhóm nhận request từ app. Nếu cần đồng bộ dữ liệu giữa nhiều máy hoặc giữ AirLabs key phía server, project phải bổ sung backend thật. Đọc [giải thích chi tiết trong tài liệu ôn thi](../docs/FINAL_EXAM_GUIDE_VI.md#13-backend-thực-sự-nằm-ở-đâu).

| Muốn tìm gì? | Mở ở đây | File quan trọng |
|---|---|---|
| Luồng lấy chuyến bay | `data/` | `FlightRepository.java`, `FallbackLoader.java` |
| Các HTTP API | `data/remote/` | `ApiClient.java`, `AirLabsService.java`, `OpenSkyService.java`, `MetarService.java` |
| Đổi JSON thành đối tượng Java | `data/parser/` | `AirLabsParser.java`, `OpenSkyParser.java`, `MetarParser.java` |
| SQLite và cài đặt local | `data/local/` | `SavedFlightsDb.java`, `Prefs.java`, `AirportStore.java` |
| Chế độ demo | `data/mock/` và `mock-json/` | `MockDataSource.java`, `MockTimeShifter.java` |
| Xác thực Firebase | `data/auth/` | `AuthManager.java` |
| Thống kê | `data/stats/` | `FlightStats.java` |
| Kiểu dữ liệu | `model/` | `Flight.java`, `FlightStatus.java`, `Result.java` |
| Tính thời gian, tọa độ, đơn vị | `util/` | `TimeUtils.java`, `GeoUtils.java`, `UnitUtils.java` |
| Danh sách sân bay đóng gói | `assets/` | `airports.json` |

Đường đi dễ trình bày nhất là `frontend/ui/search/SearchFragment.java` → `data/FlightRepository.java` → `data/FallbackLoader.java` → `data/remote/AirLabsService.java` → `data/remote/ApiClient.java` → `data/parser/AirLabsParser.java` → `model/Flight.java` → `frontend/ui/detail/FlightDetailActivity.java`.

Các mục `backend/` và `frontend/` chỉ phục vụ tìm file. Trong Android Studio, mã nguồn thật vẫn nằm dưới `app > java > vn.edu.usth.flightinfo`; package Java và đường dẫn build không thay đổi. Xem [tài liệu ôn thi đầy đủ](../docs/FINAL_EXAM_GUIDE_VI.md).
