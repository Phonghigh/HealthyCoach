# Tiêu chuẩn mã & tài liệu (đề xuất)

**Lưu ý:** chưa có codebase để mô tả convention hiện hữu. Các tiêu chuẩn này là baseline cần được team chấp thuận khi khởi tạo implementation.

## 1. Cấu trúc và trách nhiệm

- Tách ingestion, domain engines, orchestration/API, persistence và delivery adapters (mobile/web/Garmin).
- Domain engines không phụ thuộc UI, LLM hay Garmin SDK; input/output typed và testable.
- Garmin adapter là integration boundary; không để device-specific API lan vào training logic.
- LLM trả structured proposal/explanation; Rules/Safety Engine validate trước khi persist hoặc gửi user.

## 2. Quy ước API và dữ liệu

- API versioned, request validation, error code ổn định và idempotency cho sync/import.
- Giữ timezone/source/unit cho metrics; dùng đơn vị canonical nội bộ và format ở UI.
- Có provenance cho data source và confidence/range cho estimate food.
- Audit mọi plan adjustment với rule version, inputs, quyết định, explanation và consent/actor.
- Không document endpoint, field hay function cho đến khi implementation xác thực tồn tại.

## 3. Error handling và bảo mật

- Không lưu Garmin password; token/secret chỉ ở secret manager/encrypted store, redact logs.
- Retry queue có backoff, idempotency và dead-letter/observability; không retry action safety mù quáng.
- Garmin/health integration thất bại phải degrade về FIT upload/manual flow, không làm mất check-in.
- Phân quyền tối thiểu; export/delete/revoke consent là first-class workflow.

## 4. Testing

| Tầng | Test tối thiểu |
| --- | --- |
| Rules/Safety | table-driven unit tests cho load, missed workout, pain/red flags, taper |
| Nutrition | unit tests cho carb/gel, unit conversion, missing data/range |
| Import | FIT/TCX fixtures, malformed file, duplicate/idempotency |
| API | validation, auth, permission, error contract |
| Garmin | simulator + hardware PoC + firmware/device matrix |
| UX | critical flow onboarding → Today → check-in; accessibility và empty/offline states |

## 5. Documentation maintenance

- Giữ từng file docs dưới 800 dòng; split theo domain khi gần giới hạn.
- Cập nhật architecture/PDR/roadmap khi scope hoặc dependency đổi.
- Mỗi capability Garmin phải ghi `confirmed`, `PoC required` hoặc `not supported` cùng nguồn thiết bị/API.
- Chạy `node .Codex/scripts/validate-docs.cjs docs/` nếu script tồn tại; sửa warning trước khi merge.

Xem [Kiến trúc](./system-architecture.md) và [Garmin & Connect IQ](./garmin-integration-and-connect-iq.md).
