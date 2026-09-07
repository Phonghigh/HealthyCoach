# Giả định & câu hỏi mở

## Giả định đang dùng

- Mục tiêu marathon mẫu là 22/01/2027; phạm vi MVP phục vụ mục tiêu hoàn thành.
- Target device PoC là Garmin Forerunner 165; support thiết bị khác chưa được xác nhận.
- Workspace ban đầu không có code; mọi stack/schema/API là đề xuất.
- Garmin official APIs yêu cầu approval riêng; FIT/TCX import là fallback MVP.
- Health data dùng cho coaching/risk triage, không thay thế tư vấn y tế.

## Câu hỏi cần quyết định

| Chủ đề | Câu hỏi | Ảnh hưởng |
| --- | --- | --- |
| Product | chỉ personal tool hay sản phẩm thương mại, thị trường/ngôn ngữ nào? | consent, scale, monetization |
| Target | race/date/runner baseline có cố định không? | plan templates và acceptance criteria |
| Garmin | có account Developer Program/Training/Courses API chưa? | OAuth, auto-sync, launch scope |
| Device | firmware FR165 và những model launch nào? | PoC matrix và Data Field design |
| Clinical | ai duyệt safety rules/nutrition content? | risk, wording, escalation |
| Privacy | retention, data residency, export/delete, coach sharing? | architecture/security |
| Maps | provider/POI source và trách nhiệm xác minh nước/WC/safety? | route feature feasibility |
| Technical | stack đề xuất có được chấp nhận không? | project bootstrap/ADRs |
| Metrics | metric thành công, telemetry và north star là gì? | prioritization/analytics |

## Rủi ro chính và giảm thiểu

| Rủi ro | Giảm thiểu |
| --- | --- |
| `setWorkout()` không ổn định theo firmware | PoC 4; fallback Training API/manual workout |
| Không được Garmin cấp API | FIT/TCX import + Connect IQ SDK; không hứa auto-sync |
| Người dùng hiểu sai health advice | safety wording, red flags, clinician review, no diagnosis |
| Food recognition sai khẩu phần | confidence/range + user confirmation; không đổi training chỉ vì một ảnh |
| Mạng khi run không ổn định | pre-run offline package, post-run sync |
| Cảnh báo gây phiền | rolling pace, tolerance, cooldown, hardware test |

Không còn câu hỏi kỹ thuật nào có thể tự xác minh chỉ từ các trao đổi nguồn; các quyết định trên cần product owner và thử nghiệm thiết bị thật.
