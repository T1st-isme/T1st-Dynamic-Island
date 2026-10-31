# Giai đoạn 3: tiếng Việt, thiết lập và riêng tư

Đây là thay đổi của bản phát triển, chưa phải thông báo phát hành APK mới trên GitHub.
Các thay đổi giai đoạn 1–2 và tên cài đặt đã lưu vẫn được giữ.

## 1. Chọn tiếng Việt

Vào **Features → General → Language → Tiếng Việt**, hoặc chọn ngôn ngữ ngay trong
**Home → Setup guide**. Với “Theo hệ thống”, máy dùng tiếng Việt sẽ tự chọn tiếng Việt;
tiếng Nga và tiếng Anh vẫn được hỗ trợ. Ngôn ngữ khác dùng tiếng Anh.

Ngôn ngữ áp dụng cho giao diện cài đặt và các nhãn do đảo tạo ra, không dịch tên người gửi,
tin nhắn, tên ứng dụng, lời bài hát, đường phố hoặc nội dung do ứng dụng khác cung cấp.
Ngày giờ dùng locale tương ứng. Báo cáo kỹ thuật sao chép và chẩn đoán parser vẫn có
nhãn tiếng Anh để thuận tiện phân tích.

Các cặp chuỗi Nga/Anh sẵn có tiếp tục dùng được. Tiếng Việt được tra bằng khóa tiếng Anh
chính xác, không đoán hay thay thế từng từ. Chuỗi có biến dùng bản dịch tường minh để giữ
đúng tên ứng dụng, số đếm và thời gian. Nếu chưa có bản dịch, hiển thị tiếng Anh.

## 2. Hướng dẫn thiết lập có thể bỏ qua

Luồng gồm sáu bước:

1. Ngôn ngữ.
2. Trợ năng: cần để vẽ đảo; giải thích khả năng đọc màn hình của dịch vụ.
3. Truy cập thông báo: tùy chọn, cho nhạc và tin nhắn.
4. Quyền tùy chọn: dẫn tới màn hình Quyền và hướng dẫn giữ nền hiện có.
5. Nội dung thông báo và ẩn nội dung khi khóa máy.
6. Căn vị trí/kích thước và thử các ví dụ.

Không có yêu cầu cấp quyền tự động. Bạn có thể quay lại, bỏ qua hoặc chọn “Thiết lập sau”.
Hoàn tất hướng dẫn **không** có nghĩa là đã cấp quyền hay dịch vụ đã kết nối; xem phần
chẩn đoán giai đoạn 1 để xác nhận. Không bật Trợ năng vẫn có thể chỉnh giao diện, nhưng
không có đảo thực sự trên màn hình.

Bước hiện tại được lưu cùng cài đặt để tiếp tục sau khi ứng dụng khởi động lại. Cài đặt
cũ chưa có trường thiết lập được nhận diện là bản nâng cấp, không ép mở hướng dẫn.
Bạn luôn có thể mở lại từ Home. Đặt lại toàn bộ cài đặt cũng đặt lại trạng thái hướng dẫn.

## 3. Quyền riêng tư của thẻ tin nhắn

Vào **Tính năng → Thông báo → Quyền riêng tư**:

| Chế độ | Nội dung được phép hiển thị |
|---|---|
| Đầy đủ | Nội dung, người gửi và ảnh đại diện khi chính sách Android cho phép |
| Chỉ ứng dụng | Tên/biểu tượng ứng dụng, không người gửi, nội dung, lịch sử hoặc ảnh đại diện |
| Chỉ biểu tượng | Biểu tượng ứng dụng hoặc biểu tượng thông báo chung, không tên hay nội dung |

Nút thao tác/trả lời chỉ dùng khi hiển thị đầy đủ và máy đã mở khóa. Những thẻ rút gọn
không mở nội dung riêng tư bằng thao tác chạm. Bạn vẫn có thể tự mở ứng dụng từ màn hình chính.

**Ẩn nội dung khi khóa máy** bật mặc định, kể cả khóa không có PIN hoặc màn hình vừa tắt.
Tắt tùy chọn này không ghi đè hạn chế nhạy cảm/ẩn thông báo trên màn hình khóa của Android.
Chính sách được đánh giá lại khi hiển thị và khi thao tác, không chỉ khi nhận thông báo.
Ứng dụng theo dõi hai cài đặt màn hình khóa của Android và cập nhật giới hạn xếp hạng
thông báo; trạng thái khóa/màn hình được làm mới trước khi gắn cửa sổ đảo.

Danh sách **Ứng dụng gửi tin nhắn** gồm các ứng dụng đã có tin nhắn trên đảo và các ứng dụng
đang bị chặn. Ứng dụng mới được cho phép mặc định; đây là danh sách chặn, không phải danh
sách “chỉ cho phép”. Nếu danh sách trống, nhận một thông báo thông thường từ ứng dụng khác
sau khi cấp quyền truy cập thông báo.

Khi một ứng dụng đang bị chặn, thẻ đang hiện bị ẩn và thẻ tới lượt trong hàng đợi bị loại.
Đây không phải thao tác xóa toàn bộ hàng đợi: nếu bật lại trước khi một thẻ tới lượt,
thẻ đó có thể được hiển thị theo chính sách mới. Thẻ đã bị loại không được phát lại.
Việc chặn không thu hồi quyền hệ thống.
Riêng khi thông báo gốc bị xóa hoặc dịch vụ đọc thông báo ngắt kết nối, thẻ hiện tại và
thẻ chờ thuộc nguồn đã mất được thu hồi trước khi xóa giới hạn xếp hạng Android, để
không lộ lại nội dung trong khung hình chuyển tiếp.

Các tùy chọn này chỉ áp dụng **thẻ tin nhắn thông thường**, không ẩn người gọi, bài hát,
lộ trình, số tiền thanh toán hay dữ liệu trong màn hình chẩn đoán chuyên biệt. Các loại đó
vẫn có công tắc tính năng riêng. Không có tuyên bố rằng mọi dữ liệu trên đảo đều được che.

Nội dung thông báo không được tải lên bởi tính năng riêng tư này. Một số tính năng khác
vốn có dùng mạng, như tìm lời bài hát và kiểm tra cập nhật. Chẩn đoán thanh toán/chỉ đường
có thể chứa dữ liệu cá nhân trong bộ nhớ; hãy kiểm tra nội dung trước khi chủ động sao chép
và gửi báo cáo.

## 4. Kiểm tra tự động

Với JDK 17 và Android SDK 35:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon --console=plain
```

Kiểm thử mới bao gồm phân giải ngôn ngữ, bản dịch/fallback, chuyển đổi cài đặt cũ,
lưu/khôi phục bước thiết lập, chế độ riêng tư, trạng thái khóa, bộ lọc ứng dụng,
điều kiện thao tác và đánh giá lại thông báo chờ.

Các kiểm thử JVM không thay thế việc kiểm tra TalkBack, cỡ chữ lớn, camera/cutout,
thứ tự callback Android hoặc hành vi khóa máy thật.

## 5. Checklist trên điện thoại

- Cập nhật từ bản giai đoạn 2: không bị ép mở hướng dẫn, giữ ngôn ngữ/cài đặt cũ.
- Mở hướng dẫn từ Home; chuyển tiếng Việt, thoát/tiếp tục và trở về từ Cài đặt Android.
- Bỏ qua thông báo, cuộc gọi và Bluetooth: vẫn hoàn tất được, không có hộp thoại quyền tự bật.
- Ở giữa hướng dẫn, khởi động lại ứng dụng: tiếp tục đúng bước.
- Chọn “Theo hệ thống”; đổi giữa tiếng Việt, Anh, Nga: màn hình và nhãn đảo cập nhật.
- Nhận tin nhắn thật; thử ba chế độ nội dung. Không lộ người gửi/ảnh/lịch sử khi rút gọn.
- Đang mở lịch sử hoặc nhập trả lời, khóa máy hoặc chuyển sang chế độ kín đáo:
  lịch sử/ô nhập biến mất, không thể dùng thao tác cũ để gửi hoặc mở tin nhắn.
- Thử khóa có PIN, khóa không PIN và tắt màn hình. Thử cả thông báo đến trước khi khóa.
- Chặn ứng dụng khi có nhiều thông báo đang chờ: không có thẻ nguồn đó trong thời gian chặn.
- Bật lại trước khi hàng đợi xử lý hết: thẻ vẫn còn chờ có thể được hiển thị; thẻ đã bị loại không phát lại.
- Khi tắt ẩn nội dung trong ứng dụng, thay đổi cài đặt thông báo màn hình khóa của Android:
  thẻ đang hiện/chờ vẫn phải tuân theo giới hạn mới, kể cả thông báo đã nhận khi máy mở khóa.
- Xác nhận nhạc/cuộc gọi/chỉ đường không bị bộ lọc tin nhắn xóa nhầm.
- Bật cỡ chữ hệ thống lớn và TalkBack; kiểm tra nhãn nút, dấu tiếng Việt, cuộn trang,
  tên ứng dụng dài, màn hình ngang và chuyển ngôn ngữ.

Không tự cài APK, thay đổi quyền trên điện thoại, commit hoặc push trong giai đoạn này.
