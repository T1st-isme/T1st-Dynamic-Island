# Giai đoạn 4 — Tối ưu và đo trên điện thoại thật

## Phạm vi thay đổi

- Vòng đời overlay chỉ ở `RESUMED` khi được bật, trong lịch hiển thị, không bị ẩn
  do xoay ngang và màn hình đang bật. Khi không thỏa điều kiện, chuyển về `CREATED`.
- `RenderingEffect` dùng `repeatOnLifecycle(STARTED)` để hủy cả coroutine đang
  `delay()`, không chỉ dừng việc xin frame. Đồng hồ UI, equalizer, deadline thẻ
  và hiệu ứng Easter egg khởi động lại từ thời gian/trạng thái hiện tại khi hiện lại.
- Cảm biến nghiêng Liquid Glass theo cùng vòng đời, đồng thời tắt khi bật
  Reduce effects hoặc Battery Saver có hiệu lực.
- Thẻ live hết hạn và xem trước chiều rộng chờ deadline, thay vì kiểm tra mỗi
  giây / mỗi 250 ms. Đổi giờ hoặc múi giờ làm các deadline theo giờ hệ thống
  và lịch hiển thị được tính lại qua broadcast, không thêm polling.
- Timer/báo thức thật, cuộc gọi, nhận thông báo và cập nhật trạng thái media
  vẫn thuộc dịch vụ; không dừng chúng cùng tác vụ vẽ.

Không thay đổi FPS mặc định, tốc độ lấy mẫu màn hình Clock/navigation hoặc hình
dạng/độ dài hoạt ảnh. Việc ẩn bằng alpha trong khoảng chụp ảnh màn hình ngắn vẫn
giữ vòng đời hoạt động; tối ưu này nhắm tới các trạng thái ẩn lâu dài.

## Kiểm tra tự động trên Windows

Yêu cầu JDK 17 và Android SDK 35; không cần Android Studio:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Nếu máy ít RAM, có thể dùng giới hạn chỉ cho lần chạy này:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug --console=plain --max-workers=2 `
  '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8' `
  '-Pkotlin.compiler.execution.strategy=in-process'
```

`OverlayLifecycleOwnerTest` kiểm tra:

1. Không kích hoạt lifecycle trước khi được tạo.
2. Hủy vòng cập nhật khi ẩn; không có tick trong 60 giây thời gian ảo;
   hiện lại chạy ngay, không chạy bù, không tạo vòng lặp trùng; destroy dừng hẳn.
3. Tác vụ domain độc lập vẫn hoàn thành khi tác vụ hiển thị đã dừng.

Đây là JVM test với thời gian ảo, **không chứng minh mức hao pin, FPS, thao tác
cảm ứng hoặc khả năng đánh thức màn hình trên Android thật**.

## Chọn đúng điện thoại và cài APK

```powershell
adb devices -l
$serial = 'THAY_BANG_SERIAL_DIEN_THOAI'
$package = 'com.akira.island'
adb -s $serial get-state
adb -s $serial install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Không chọn thiết bị `UFI` chỉ vì nó cũng hiện trong ADB. Mọi lệnh bên dưới phải
có `-s $serial`. Nếu khác khóa ký với bản đang cài, không tự gỡ ứng dụng: gỡ sẽ
mất thiết lập. Dùng cùng khóa ký hoặc chủ động sao lưu/cấu hình lại.

Sau cài đặt: mở ứng dụng, kiểm tra Accessibility và Notification access, căn lại
Island nếu cần. Không chạy debugger/breakpoint trong các lượt đo.

## Kịch bản hồi quy bắt buộc

| Kịch bản | Cách thử | Kết quả cần đạt |
|---|---|---|
| Nhạc và lời bài hát | Phát nhạc, mở rộng Island, tắt màn hình 1 phút rồi bật | Vị trí bài hát/lời nhạc cập nhật đúng, không đứng ở thời điểm cũ |
| Timer thật | Tạo timer, tắt màn hình trước lúc hết giờ | Timer vẫn reo; hành vi bật màn hình theo thiết lập Wake for alerts |
| Cuộc gọi | Gọi đến khi màn hình tắt, thử tắt Wake for alerts rồi thử lại | Không mất cuộc gọi; không tự bật màn hình khi tính năng đã tắt |
| Ẩn theo cài đặt | Tắt Enabled khi nhạc đang phát, chờ 1 phút, bật lại | Không mất trạng thái media; hiệu ứng hoạt động lại |
| Xoay ngang | Bật Hide in landscape, xoay ngang 1 phút rồi xoay dọc | Island ẩn/hiện đúng; đồng hồ không chạy bù từng tick |
| Lịch | Đặt lịch để đi qua ranh giới bật/tắt | Ẩn/hiện đúng; không mất timer/cuộc gọi |
| Tiết kiệm pin | Bật Liquid Glass, thử Reduce effects và Android Battery Saver | Cảm biến nghiêng ngừng khi giảm hiệu ứng có hiệu lực; bật lại hoạt động |
| Thẻ thử hết hạn | Home → Try it → live/navigation, ẩn hoặc tắt màn hình quá hạn | Thẻ thử không quay lại sau khi hiện Island |
| Chỉnh chiều rộng | Kéo thanh Width with activity liên tục rồi thả | Preview không mất giữa lúc kéo; biến mất sau thời gian chờ |
| Đổi giờ/múi giờ | Trên máy test, thử thẻ đang chờ hết hạn và lịch rồi chỉnh giờ | Deadline/lịch tính lại; khôi phục giờ tự động sau thử nghiệm |
| Tắt dịch vụ | Tắt Accessibility, bật lại vài lần | Không crash, không nhân đôi cập nhật/cảm biến |

Không dùng máy chính để chỉnh giờ nếu có ứng dụng nghiệp vụ/2FA đang hoạt động.
Ưu tiên kiểm tra hồi quy trước khi đo lâu.

## Đo trước–sau có thể lặp lại

### Điều kiện so sánh

- Baseline là bản trước thay đổi giai đoạn 4 (revision `3c3d9d1`), candidate là
  bản có thay đổi. Build cả hai với cùng variant, JDK/SDK và khóa ký.
- Nếu đo trên thư mục chính đã tích hợp giai đoạn 1–3, baseline cũng phải giữ
  nguyên các thay đổi đó; chỉ khác candidate ở phần giai đoạn 4.
- Nếu chưa có APK baseline, build revision gốc trong một checkout riêng trước.
  Không so APK release baseline với APK debug candidate và kết luận do tối ưu.
- Cùng điện thoại, Android, cài đặt Island, ứng dụng nhạc/bài hát, kết nối,
  độ sáng cố định, tần số quét, mức pin và nhiệt độ bắt đầu tương đương.
- Chờ máy nguội; tắt tự động cập nhật ứng dụng trong lúc đo. Không dừng
  breakpoint. Đo ít nhất 3 lượt mỗi bản, đổi thứ tự baseline/candidate.
- Đo pin khi **không sạc USB**. Có thể lấy snapshot trước/sau bằng USB và rút cáp
  trong thời gian đo, hoặc dùng Wireless debugging nếu thiết bị hỗ trợ.

### Thu thập snapshot

Chạy PowerShell tại repository. Thay serial trước khi chạy. Dữ liệu sinh ra ở
`app/build/performance/` (được Git bỏ qua), không tự gửi lên mạng:

```powershell
$serial = 'THAY_BANG_SERIAL_DIEN_THOAI'
$package = 'com.akira.island'
$run = 'baseline-media-on-01' # Đổi cho mỗi bản, kịch bản và lượt
$out = Join-Path (Get-Location) "app\build\performance\$run"
New-Item -ItemType Directory -Force -Path $out | Out-Null

function Save-Snapshot([string]$point) {
    Get-Date -Format o | Out-File "$out\$point-time.txt"
    adb -s $serial shell dumpsys battery | Out-File "$out\$point-battery.txt"
    adb -s $serial shell dumpsys batterystats $package | Out-File "$out\$point-batterystats.txt"
    adb -s $serial shell dumpsys cpuinfo | Out-File "$out\$point-cpuinfo.txt"
    adb -s $serial shell dumpsys meminfo $package | Out-File "$out\$point-memory.txt"
    adb -s $serial shell dumpsys gfxinfo $package framestats | Out-File "$out\$point-frames.txt"
    adb -s $serial shell dumpsys sensorservice | Out-File "$out\$point-sensors.txt"
}

adb -s $serial shell dumpsys gfxinfo $package reset
Save-Snapshot 'start'
# Thực hiện đúng kịch bản, rồi:
Save-Snapshot 'end'
```

Kiểm tra lệnh ADB thành công; nếu mất kết nối hoặc dump bị từ chối, đánh dấu
lượt đó không hợp lệ thay vì coi dữ liệu trống là 0. Không reset batterystats
toàn máy: lấy chênh lệch bộ đếm trước/sau trong cùng lượt. Nếu máy reboot,
ứng dụng đổi UID hoặc bộ đếm reset thì không dùng lượt đó.

Dump có thể chứa danh sách ứng dụng/UID và thông tin thiết bị. Chỉ chia sẻ
phần cần thiết sau khi loại dữ liệu cá nhân; không gửi Logcat/nội dung thông báo.

### Các lượt đo

| Kịch bản | Thời gian | Trọng tâm |
|---|---:|---|
| Idle, màn hình tắt | 30 phút | CPU/wakelock nền; không có hiệu ứng hoạt động |
| Nhạc, màn hình bật | 10 phút | CPU, frame/jank, equalizer; cùng bài hát |
| Nhạc, màn hình tắt | 30 phút | Chi phí còn lại khi không hiển thị |
| Nhạc, overlay ẩn | 10 phút | Tắt Enabled hoặc Hide in landscape; giảm tác vụ visual |
| Liquid Glass | 5 phút mỗi trạng thái | So sensorservice khi hiện/ẩn, bật/tắt Reduce effects |
| Mở rộng/thu gọn | 30 lần cùng thao tác | Frame time/jank và phản hồi chạm |
| Thẻ live thử | Đủ thời gian hết hạn | Không còn nhịp cập nhật 1 giây cho việc lọc hết hạn |

CPU trong `dumpsys cpuinfo` là cửa sổ lấy mẫu ngắn, không phải tổng năng lượng.
Dùng chênh lệch CPU/wakelock trong batterystats khi có; phần trăm pin chỉ là
chỉ số thô. `gfxinfo` chủ yếu hữu ích lúc màn hình bật; xem số frame của lượt
đo, không so tổng bộ đếm cũ.

Nếu cần xác nhận coroutine/frame không chạy khi ẩn, dùng Developer options →
System tracing (Perfetto), bật các mục scheduling/CPU frequency/graphics/view
có trên thiết bị. Capture một đoạn hiện → ẩn → hiện, cùng trạng thái nhạc.
Trace là bằng chứng về công việc CPU/render, không tự chứng minh lượng pin tiết kiệm.

### Mẫu báo cáo

| Bản | Kịch bản/lượt | Thiết bị/API | Phút | CPU delta nếu có | Wakelock delta | Frame/jank | Heap | Pin đầu/cuối |
|---|---|---|---:|---:|---:|---|---|---|
| baseline | | | | | | | | |
| candidate | | | | | | | | |

Chỉ coi giai đoạn đo hoàn tất khi:

- Các kịch bản hồi quy đạt trên máy thật.
- Trace không có vòng tick/frame visual định kỳ khi overlay ẩn/màn hình tắt.
- Sensor Liquid Glass không còn đăng ký cho ứng dụng khi không đủ điều kiện.
- Không có tăng heap kéo dài sau nhiều lần ẩn/hiện hoặc bật/tắt dịch vụ.
- Số liệu trước–sau có ít nhất 3 lượt hợp lệ và không có hồi quy rõ về CPU/jank.
  Không đặt phần trăm tiết kiệm pin cố định trước khi có baseline.

## Trạng thái xác minh và phần tiếp theo

Đã build debug và chạy JVM test trên Windows. **Chưa đo trên điện thoại thật;
chưa có con số tiết kiệm pin/FPS trước–sau để công bố.** Thiết bị `PKG110` cần
được kết nối lại để thực hiện các bước trên.

Ở worktree độc lập, `lintDebug` chưa đạt, báo 6 lỗi và 22 cảnh báo. Các lỗi nằm ở
đoạn code không thay đổi trong đợt này: `MissingPermission` khi đọc thông tin
Bluetooth trong `SystemEvents.kt`, `NewApi` trong `Messages.kt` và
`NotifListener.kt`. Chưa chạy lint riêng trên revision baseline; không tạo
baseline/suppress để che lỗi. Báo cáo cục bộ:
`app/build/reports/lint-results-debug.html`.

Sau khi tích hợp vào thư mục chính có các thay đổi giai đoạn 1–3, đã chạy lại
`testDebugUnitTest assembleDebug lintDebug`: **cả ba task đều đạt**, tổng cộng
78 unit test, không lỗi hoặc bỏ qua. Kết quả lint ở thư mục chính thay thế
kết quả lỗi của worktree độc lập ở trên; vẫn chưa xác minh trên điện thoại thật.

Các hướng rà soát tiếp, chưa thay đổi trong đợt này:

- Giảm tạo/hủy job deadline khi Clock reader đọc lại cùng trạng thái.
- Rà soát quyền sở hữu/recycle accessibility node và hardware buffer của
  screenshot navigation; chặn kết quả callback đến sau khi reader đã dừng.
- Gom teardown của notification listener, hạn chế sao chép map không đổi.

Cần trace/kiểm thử tương thích trước khi thay chu kỳ lấy mẫu Clock/navigation;
không giảm tần suất theo cảm tính vì có thể bỏ lỡ thao tác hoặc chỉ đường.
