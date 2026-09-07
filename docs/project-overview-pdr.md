# HealthyCoach — Project overview & PDR

**Phiên bản:** 0.1 (khởi tạo)  
**Cập nhật:** 07/09/2026  
**Trạng thái:** định nghĩa sản phẩm; chưa có implementation để xác minh.

## 1. Mục đích

HealthyCoach khởi đầu là **AI Marathon Coach** cho người có nền chạy khoảng 20–35 km/tuần, mục tiêu trước mắt là hoàn thành 42,195 km ngày 22/01/2027 an toàn. Giá trị cốt lõi là tạo vòng lặp: dữ liệu thực tế → đánh giá → kế hoạch tuần → thực hiện → check-in → điều chỉnh có giải thích.

> Lời hứa MVP: mỗi tuần hệ thống đọc kết quả chạy thực tế và tạo tuần tiếp theo để giúp người dùng hoàn thành marathon an toàn.

Sau khi chứng minh được vòng lặp này, sản phẩm có thể mở rộng thành Healthy Lifestyle Assistant cho tập luyện, dinh dưỡng và phục hồi. Marathon không bị thay thế trong MVP.

## 2. Vấn đề và định vị

Nhiều dashboard wearable hiển thị số liệu nhưng không trả lời rõ: hôm nay chạy gì, ở effort nào, có cần fuel không và kế hoạch thay đổi ra sao. Phản hồi cộng đồng về Garmin Connect+ là tín hiệu khám phá, không phải nghiên cứu đại diện; chúng gợi ý khoảng trống về roadmap minh bạch, giải thích thay đổi, fueling gắn với buổi chạy và dữ liệu đau chủ quan.

HealthyCoach không cạnh tranh bằng “thêm AI”, mà điều phối **giáo án + Garmin + recovery + dinh dưỡng + đau + bối cảnh** thành một quyết định khả thi hôm nay.

## 3. Người dùng, phạm vi và không-phạm-vi

| Hạng mục | Quyết định P0 |
| --- | --- |
| Người dùng chính | Runner chuẩn bị marathon, ưu tiên mục tiêu hoàn thành hơn thành tích |
| Dữ liệu | FIT/TCX upload trước; Garmin chính thức sau khi được chấp thuận |
| Kênh chính | Mobile hằng ngày; Garmin khi chạy; web cho roadmap/analytics |
| Bài tập | Easy, recovery, long run, quality phù hợp, strength, rest |
| Kết quả bắt buộc | Kế hoạch tuần minh bạch, workout chi tiết, post-run check-in, điều chỉnh có lý do |
| Ngoài MVP | Chẩn đoán y khoa, voice coach realtime, hỗ trợ mọi Garmin, meal plan toàn diện, social/marketplace |

## 4. Yêu cầu chức năng

### P0 — MVP

1. Tạo hồ sơ athlete, race goal, lịch có thể tập, ngày nghỉ, lịch sử chấn thương và giày.
2. Nhập/parse hoạt động FIT hoặc TCX; hiển thị mileage tuần, long run, pace, HR/zone khi dữ liệu có.
3. Phân tích baseline 4–8 tuần, tạo roadmap Base → Build → Peak → Taper → Race.
4. Tạo weekly plan gồm mileage, số buổi, long run, easy/recovery, quality, strength và rest.
5. Hiển thị target theo segment bằng pace range, HR zone và RPE; nêu fallback khi nóng ẩm hoặc GPS/HR kém tin cậy.
6. Thu check-in sau chạy dưới 30 giây: RPE, đau, fueling, vấn đề tiêu hoá và hoàn thành bài.
7. Điều chỉnh kế hoạch theo rule có giải thích; không dồn bài nặng để bù buổi lỡ.
8. Tạo fueling card cho long run: mục tiêu carbohydrate, gel, nước/điện giải và ghi nhận thực tế.
9. Pain check-in/body map trên mobile, phân loại rủi ro và safety escalation; không chẩn đoán.

### P1 sau MVP lõi

- Readiness/adaptation score, race-day planner, food photo logging có xác nhận khẩu phần.
- Route planner có POI nước/WC/an toàn và xuất course.
- Connect IQ Coach Data Field, đồng bộ structured workout qua Garmin khi quyền API cho phép.

## 5. Yêu cầu phi chức năng

| Nhóm | Yêu cầu |
| --- | --- |
| An toàn | Rules/Safety Engine luôn chặn LLM khỏi vượt tải hoặc đưa ra khuyến nghị nguy hiểm. |
| Minh bạch | Mọi điều chỉnh phải có nguyên nhân, ảnh hưởng và lịch sử thay đổi. |
| Offline khi chạy | Đồng hồ phải chạy package đã tải trước; không phụ thuộc AI/network liên tục. |
| Riêng tư | Không lưu mật khẩu Garmin dạng plaintext; nêu rõ dữ liệu đọc/ghi, cho phép rút quyền/xuất/xoá dữ liệu. |
| Khả dụng | Màn hình Today trả lời năm câu hỏi chính, thuật ngữ sâu chỉ mở khi cần. |
| Khả năng kiểm thử | Quy tắc tải, safety, nutrition calculation và plan adjustment là logic xác định được, tách khỏi LLM. |

## 6. Tiêu chí chấp nhận MVP

- Người dùng hoàn tất onboarding trong khoảng 3–5 phút hoặc upload FIT thay thế Garmin.
- Người dùng xem được bài hôm nay, target và lý do trong một màn hình.
- Sau một activity, check-in tạo được kết luận và hành động cho bài/tuần kế tiếp.
- Bỏ lỡ buổi không tạo hai buổi nặng liền kề và có explanation log.
- Long run có fueling plan theo sản phẩm người dùng đã khai báo; không khẳng định “đã dùng” nếu chưa xác nhận.
- Pain red flag dẫn đến dừng/giảm tải và khuyến nghị hỗ trợ chuyên môn, không đưa chẩn đoán.

## 7. Chỉ số thành công đề xuất

- Activation: hoàn tất onboarding, import và xác nhận kế hoạch đầu tiên.
- Adherence: tỷ lệ planned workout được hoàn thành/điều chỉnh có chủ đích.
- Comprehension: tỷ lệ người dùng hiểu lý do thay đổi (phản hồi “hữu ích”).
- Safety: red flag được escalated; không có auto-progression khi điều kiện chặn kích hoạt.
- Fueling learning: tỷ lệ long run có fueling plan và feedback dung nạp.

Các metric, ngưỡng và deadline sản phẩm chính thức còn cần product owner xác nhận. Chi tiết interface ở [Phạm vi & giao diện](./product-scope-and-interfaces.md), safety ở [An toàn tập luyện & dinh dưỡng](./training-safety-and-nutrition.md).
