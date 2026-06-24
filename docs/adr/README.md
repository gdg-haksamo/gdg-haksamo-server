# ADR — 기술 판단 기록 (Architecture Decision Records)

설계·기술 선택의 **이유와 트레이드오프**를 시점과 함께 남긴다. "왜 이렇게 했나"를 코드만 보고는 알 수 없을 때, 나중(또는 다른 팀원)이 맥락을 복원할 수 있게 한다.

## 규칙
- 파일명: `NNNN-제목.md` (4자리 번호 순번).
- 한 결정 = 한 파일. 결정이 바뀌면 새 ADR을 추가하고 옛 ADR에 "Superseded by NNNN" 표기(파일은 지우지 않음 — 기록 보존).
- 상태: `제안` / `채택` / `폐기` / `대체됨`.

## 목록

| 번호 | 제목 | 상태 |
|------|------|------|
| [0001](0001-ai-recommendation-generation-strategy.md) | AI 추천 생성 전략 (끼니별 per-user vs 서명 dedup) | 채택 |
| [0002](0002-menu-info-gemini-batch-generation.md) | 크롤 신규 메뉴 정보(한줄설명·탄단지) Gemini 배치 생성 | 채택 |
