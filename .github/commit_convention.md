# 커밋 컨벤션

## 형식

```
<type>: <subject>

[optional body]

[optional footer]
```

- **type**: 변경 유형 (소문자)
- **subject**: 한 줄 요약 (50자 내외, 명령형)
- **body**: 필요 시 상세 설명

## Type

| Type | 설명 |
|------|------|
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서만 변경 (README, 주석 등) |
| `style` | 코드 의미 변경 없음 (포맷, 세미콜론 등) |
| `refactor` | 리팩터링 (기능 변경 없음) |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드, 설정, 기타 (의존성, .gitignore 등) |

## 예시

```
feat: 사용자 로그인 API 추가
fix: 비밀번호 검증 로직 오류 수정
docs: API 사용법 README 보완
refactor: 인증 미들웨어 분리
chore: .gitignore에 .env 추가
```

## 규칙

- 제목은 **명령형**으로 작성 (예: "추가한다" → "추가")
- 제목 끝에 마침표(.) 사용하지 않음
- 본문이 있으면 제목과 한 줄 띄어쓰기
