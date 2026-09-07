# Codebase summary

**Cập nhật:** 07/09/2026  
**Nguồn kiểm chứng:** `repomix-output.xml` tại workspace root, được tạo bằng Repomix ngày 07/09/2026.

## Trạng thái đã kiểm chứng

Snapshot Repomix có directory structure và danh sách file rỗng. Khi snapshot được tạo, workspace chưa chứa mã nguồn, package manifest, cấu hình, test, migration hay tài liệu dự án có thể phân tích.

Do đó hiện chưa thể xác nhận:

- framework, ngôn ngữ, endpoint, module hoặc database schema;
- biến môi trường, cơ chế xác thực hay mô hình quyền;
- integration Garmin, parser FIT/TCX, queue hoặc LLM;
- test, CI/CD, deployment hoặc coverage.

## Tài liệu này có gì

Các file trong `docs/` là product/technical specification được tổng hợp từ tám trao đổi nguồn. Chúng ghi rõ đâu là:

- **đã xác nhận trong tài liệu Garmin**;
- **cần PoC trên thiết bị thật**;
- **đề xuất thiết kế**, chưa phải code đã triển khai.

## Hành động khi bắt đầu code

1. Tạo project structure và package manifests.
2. Cập nhật tài liệu này bằng tên module, dependency, entry point và lệnh chạy thực tế.
3. Chỉ bổ sung API/schema sau khi chúng tồn tại trong code hoặc migration.
4. Chạy lại `repomix` sau các milestone đáng kể để đối chiếu tài liệu với implementation.

Xem [Tổng quan & PDR](./project-overview-pdr.md) để biết phạm vi được đề xuất và [Tiêu chuẩn mã](./code-standards.md) để biết tiêu chuẩn khởi tạo.
