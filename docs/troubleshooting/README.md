# 트러블슈팅 기록

운영·개발 중 겪은 문제와 원인·해결을 한 건당 한 파일로 누적 기록한다.

## 작성 규칙
- 파일명: `YYYY-MM-짧은-설명.md` (예: `2026-06-crawler-empty-ec2-region.md`)
- 권장 섹션: 증상 / 원인 / (배제한 것) / 진단 방법 / 해결 / 교훈
- 새 항목을 추가하면 아래 목록에도 한 줄 추가한다.

## 목록
| 날짜 | 문제 | 파일 |
|------|------|------|
| 2026-06 | 메뉴 크롤러가 200인데 DB 미저장 (EC2 us-east-1 → 한국 전용 소스 접근 불가, 서울 리전 이전으로 해결) | [2026-06-crawler-empty-ec2-region.md](2026-06-crawler-empty-ec2-region.md) |
| 2026-06 | prod 앱 부팅 크래시 루프 (SMTP 미설정으로 JavaMailSender 미구성) | [2026-06-prod-smtp-boot-crashloop.md](2026-06-prod-smtp-boot-crashloop.md) |
