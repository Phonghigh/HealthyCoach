# Kế hoạch Proof of Concept — Garmin Forerunner 165

## Mục tiêu

Xác minh capability thực tế trên chính thiết bị/firmware mục tiêu trước khi xây backend/mobile lớn. Tài liệu API chứng minh bề mặt API, không thay thế PoC về lifecycle, foreground, alert và UX.

## Thiết lập chung

- Thiết bị: Forerunner 165 (ghi model, firmware, Connect IQ version, OS phone và Garmin Connect version).
- Tạo log timestamp, screenshot/video, FIT export và kết quả pass/fail cho từng PoC.
- Test ở developer mode, sau đó test activity Run GPS thật; không làm gián đoạn activity chính của người dùng.

## PoC 1 — Embedded Data Field

**Build:** full-screen Data Field hiển thị pace, HR, elapsed time.  
**Pass:** cài được; thêm được vào Run; GPS/FIT native vẫn hoạt động; đổi giữa data page được; dữ liệu hợp lý trong run.

## PoC 2 — Alert và rung

**Build:** sau 60 giây show `TEST GEL REMINDER` + vibration.  
**Pass:** rung/alert hiển thị cả khi user đang ở Garmin page khác (nếu API hứa hẹn); activity không pause; alert đóng đúng; không spam khi retry/cooldown.

## PoC 3 — Structured workout reader

**Build:** workout thủ công: warm-up 2 phút → run 3 phút → cool-down 2 phút. Data Field render current step, remaining, target và next step.  
**Pass:** `getCurrentWorkoutStep()` chính xác; callback step complete xuất hiện; UI chuyển đúng step; Run native vẫn điều khiển workout.

## PoC 4 — `setWorkout()`

**Build:** Data Field thử đặt 2 phút warm-up → 3 phút target pace → 2 phút cool-down, với permission `ActivityControl`.  
**Pass:** Garmin native nhận step/target; không cần Device App; activity ghi đúng.  
**Decision:** đây là method cần xác thực nhất. Nếu fail/không ổn định, fallback là workout manual/import hoặc Training API khi được cấp quyền.

## PoC 5 — Custom FIT fields

**Build:** FitContributor ghi `pace_compliance` và `fuel_reminders_shown`.  
**Pass:** field tồn tại sau sync/export, giá trị kiểm được, FIT activity không lỗi. Không ghi gel consumed nếu chưa có input đáng tin cậy.

## Non-goals PoC

- Không làm chat/AI realtime, meal capture, full map hoặc chẩn đoán.
- Không suy diễn kết quả FR165 sang mọi Garmin.
- Không coi tap xác nhận gel, live configuration hay Data Field control route là đã support nếu chưa kiểm thử.

## Exit criteria

Chỉ bắt đầu Garmin MVP khi PoC 1–3 pass, PoC 5 không phá FIT, và PoC 4 có quyết định rõ (pass hoặc fallback). Lập device-capability matrix sau thử nghiệm.
