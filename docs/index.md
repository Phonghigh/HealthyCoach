# HealthyCoach — Tài liệu dự án

HealthyCoach bắt đầu như một **AI Marathon Coach** hỗ trợ người chạy hoàn thành marathon an toàn, trước mắt hướng đến ngày đua 22/01/2027. Hệ thống biến dữ liệu Garmin và check-in thành một kế hoạch có thể thực hiện hôm nay; không phải chatbot tóm tắt chỉ số.

## Bắt đầu ở đây

- [Tổng quan & PDR](./project-overview-pdr.md) — vấn đề, mục tiêu, yêu cầu và tiêu chí thành công.
- [Phạm vi sản phẩm & giao diện](./product-scope-and-interfaces.md) — phân vai Garmin, mobile và web.
- [Kiến trúc hệ thống](./system-architecture.md) — thành phần, dữ liệu và ranh giới quyết định.
- [Luồng cốt lõi](./core-workflows.md) — onboarding đến điều chỉnh tuần kế tiếp.
- [Garmin & Connect IQ](./garmin-integration-and-connect-iq.md) — năng lực đã xác nhận và giới hạn cần PoC.

## Thực thi

- [Kế hoạch PoC](./proof-of-concept-plan.md) — năm thử nghiệm trên Forerunner 165.
- [Lộ trình](./project-roadmap.md) — MVP đến Healthy Lifestyle Assistant.
- [Tiêu chuẩn mã & tài liệu](./code-standards.md) — nguyên tắc triển khai khi codebase bắt đầu có mã.
- [An toàn tập luyện & dinh dưỡng](./training-safety-and-nutrition.md) — safety rails, fueling và giới hạn tư vấn.

## Trạng thái hiện tại

**Phase 02 (Project scaffolding) hoàn thành:** Backend (NestJS + TypeScript + PostgreSQL), Android (Kotlin + Jetpack Compose) và schema Prisma (12 bảng) đã tồn tại. Stack kỹ thuật đã được quyết định (xem [MVP plan](../plans/260907-2130-mvp-implementation/plan.md)). Tài liệu hướng dẫn sản phẩm (`system-architecture.md`, `core-workflows.md`, v.v.) vẫn là định hướng; triển khai theo các giai đoạn được lập kế hoạch.

Các điều kiện cần quyết định hoặc kiểm thử tiếp được tập trung tại [Giả định & câu hỏi mở](./assumptions-and-open-questions.md).
