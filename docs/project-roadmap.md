# Lộ trình sản phẩm

Lộ trình ưu tiên vòng lặp coach an toàn trước breadth. Mốc thời gian cần được lập lại sau khi xác nhận đội ngũ, quyền Garmin API và PoC.

## Phase 0 — Discovery & Garmin PoC

- Chốt PDR, data consent, metric và target device.
- Thực hiện năm PoC FR165; chọn fallback workout sync.
- Lập safety rule catalog và clinician/dietitian review path.

**Exit:** architecture/dependency decision, device evidence, risk register và MVP scope freeze.

## Phase 1 — Marathon Coach MVP

- Athlete profile, race goal, availability, FIT/TCX import.
- Baseline, roadmap, weekly plan, Today, workout detail.
- Post-run RPE/pain/fueling check-in, explanation log, adjustment rules.
- Fueling card, sweat-rate logging và weekly review.

**Không gồm:** auto Garmin OAuth/Training API, full food vision, route safety guarantee, multi-sport.

## Phase 2 — Adaptive runner assistant

- Readiness/adaptation, missed-workout scheduling, race-day planner.
- Connect IQ Data Field (sau PoC), food image/barcode với confirmation, recovery/strength và route candidate.
- Garmin API integration nếu được chấp thuận.

## Phase 3 — Healthy Lifestyle Assistant

- Exercise ngoài running, gym detail, sleep/recovery, weight management theo mục tiêu ưu tiên.
- Personal health memory có quyền xem/sửa/xóa; calendar/work-context.

## Phase 4 — Ecosystem có kiểm soát

- Coach/physio/dietitian sharing read-only, export report và partner APIs.
- Chỉ mở rộng data/lab/marketplace sau khi review privacy, clinical risk và consent.

## Ưu tiên backlog

| Ưu tiên | Điều kiện để bắt đầu |
| --- | --- |
| Training engine + feedback loop + safety | luôn trước chatbot và dashboard sâu |
| Garmin Data Field | sau device PoC |
| Garmin Training/Courses API | sau approval và OAuth/security design |
| Food photo | sau MVP loop; require confirmation UX |
| Route planner | sau khi có map/POI quality và disclaimer |

Các dependency/risk chi tiết ở [Giả định & câu hỏi mở](./assumptions-and-open-questions.md).
