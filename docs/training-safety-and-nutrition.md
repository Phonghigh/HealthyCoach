# An toàn tập luyện & dinh dưỡng

Tài liệu này là safety design cho sản phẩm, không thay thế đánh giá của bác sĩ, dietitian hay physio; ứng dụng không chẩn đoán và không được đưa cam kết y khoa.

## 1. Quyền quyết định

| Engine | Vai trò |
| --- | --- |
| Rules Engine | giới hạn tải, ngày nghỉ, taper, không dồn bài nặng |
| Training Engine | chọn loại bài/duration/intensity trong giới hạn |
| Recovery Engine | sử dụng sleep, HRV/RHR, stress và check-in như tín hiệu |
| Nutrition Engine | plan carb/gel/nước theo bài và dữ liệu người dùng |
| Safety Engine | phát hiện red flag, giảm tải/escalate |
| LLM Coach | hỏi rõ, giải thích, động viên; không override engine |

## 2. Tín hiệu và phản ứng

| Tín hiệu | Hành động hệ thống |
| --- | --- |
| Hoàn thành, RPE bình thường, recovery tốt | giữ hoặc tăng nhẹ theo progression rule |
| Không hoàn thành vì mệt toàn thân | giảm bài sau/thêm recovery; không bù bằng bài nặng |
| Ngủ kém + HRV giảm + RHR tăng | đề xuất easy/rest theo rule và nói rõ đây là tín hiệu, không kết luận bệnh |
| Đau khu trú tăng dần khi chạy | dừng quality work, yêu cầu đánh giá lại |
| Đau thay đổi dáng chạy, sưng, đau nhói, không chịu lực | không tiếp tục chạy; khuyến nghị đánh giá chuyên môn |
| Đau ngực, ngất/choáng, khó thở bất thường | dừng tập và tìm trợ giúp y tế khẩn cấp phù hợp |

Pain check-in gồm vị trí, 0–10, thời điểm, xu hướng, sưng và thay đổi dáng chạy. Có thể đề xuất rest/cross-training hoặc chuẩn bị lịch sử cho chuyên gia; không gán nhãn bệnh hoặc thay đổi thuốc/phác đồ.

## 3. Fueling design

| Thời lượng | Hành vi sản phẩm |
| --- | --- |
| <60 phút | thường không cần gel; xét bữa ăn trước và cường độ |
| 60–90 phút | tuỳ bối cảnh; không coi là bắt buộc |
| >90 phút | bắt đầu luyện nạp carbohydrate trong tập |
| 2–3 giờ/race | thực hành strategy đã thử: carb, nước, điện giải |

Điểm khởi đầu endurance thường được nguồn trao đổi dẫn là khoảng **30–60 g carbohydrate/giờ**; mức cao hơn chỉ khi người dùng đã luyện khả năng dung nạp. Một gel thường 20–25 g nhưng app phải dùng lượng carb thực trên nhãn:

`carb mục tiêu/giờ ÷ carb mỗi gel = gel/giờ`.

Không tự động dùng sản phẩm/caffeine mới trong race. Sau chạy, người dùng xác nhận gel thực tế và triệu chứng GI; Data Field chỉ ghi `fuel_reminders_shown` nếu không có xác nhận tin cậy.

## 4. Hydration và meal logging

Water/electrolyte phụ thuộc cá nhân, nóng ẩm, mồ hôi và thời lượng. Ứng dụng có thể log sweat rate:

`(cân nặng trước − cân nặng sau + nước uống − nước tiểu) ÷ giờ chạy`.

Không ép một lượng nước giống nhau, và cần cảnh báo tránh uống quá mức. Photo food là estimate có range/confidence; cần xác nhận món/khẩu phần, không phát biểu chính xác giả tạo hoặc thay quyết định giáo án chỉ vì một ảnh.

## 5. Nội dung thể thao có thể hướng dẫn

Long run, easy/recovery, tempo/threshold, interval, strength, mobility và rest là các loại workload có thể lập kế hoạch. Với mục tiêu hoàn thành, đa số mileage nên ở effort dễ; pace phải điều chỉnh theo nhiệt/ẩm/elevation bằng HR/RPE khi phù hợp.

Nguồn tham khảo được cung cấp: [ACSM](https://acsm.org/athletes-kitchen-optimizing-immune-response/) và [IOC Athlete365 — Beat the Heat](https://www.olympics.com/athlete365/app/uploads/2024/06/Beat-The-Heat-Paris-2024-Athlete365.pdf). Cần clinician/dietitian review trước khi biến thành guideline product chính thức.
