# Spring 학습 가이드 (학사모 프로젝트 기준)

이 문서는 학사모 프로젝트를 진행하는 데 필요한 Spring 지식을 정리한 것입니다.
각 항목마다 **학습 깊이**와 **이 프로젝트에서 어디에 쓰이는지**를 명시합니다.
AI에게 물어볼 때 이 문서의 키워드를 그대로 사용하세요.

---

## 1. Spring Boot 기초

**키워드**
- `@SpringBootApplication`
- 자동 설정 (Auto Configuration)
- `application.properties` / 프로파일(`spring.profiles.active`)
- `@Bean`, `@Component`, `@Service`, `@Repository`
- 의존성 주입 (DI) — `@Autowired` vs 생성자 주입

**학습 깊이**
자동 설정이 "왜" 동작하는지보다 "무엇을 해주는지"만 알면 됩니다.
생성자 주입이 `@Autowired`보다 권장되는 이유 정도는 이해해야 합니다.

**프로젝트 적용**
- 모든 클래스 구조의 기반
- `application.properties`에서 DB/JWT 설정 로딩

---

## 2. Spring MVC — REST API

**키워드**
- `@RestController`, `@RequestMapping`
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`
- `@RequestBody`, `@PathVariable`, `@RequestParam`
- `ResponseEntity<T>`
- HTTP 상태코드 (200, 201, 400, 401, 403, 404)

**학습 깊이**
CRUD API를 혼자 만들 수 있는 수준.
`ResponseEntity`로 상태코드와 바디를 함께 반환하는 패턴을 익히세요.

**프로젝트 적용**
- 모든 API 엔드포인트 (Menu 조회, Review 작성, 추천 조회 등)

---

## 3. DTO 패턴

**키워드**
- DTO (Data Transfer Object)
- Request DTO / Response DTO 분리
- 엔티티를 직접 반환하지 않는 이유
- 수동 변환 (`from()` 정적 메서드 패턴)

**학습 깊이**
"엔티티 ↔ DTO 변환을 왜 하는지"와 "어떻게 하는지"를 알면 됩니다.
MapStruct 같은 매핑 라이브러리는 이 프로젝트에서 불필요합니다.

**프로젝트 적용**
- 모든 API의 요청/응답 객체

---

## 4. Spring Data JPA

**키워드**
- `@Entity`, `@Table`, `@Id`, `@GeneratedValue`
- `@Column`
- 연관관계: `@ManyToOne`, `@OneToMany`, `@JoinColumn`
- `FetchType.LAZY` vs `EAGER`
- `JpaRepository<Entity, ID>` — `save()`, `findById()`, `findAll()`
- 쿼리 메서드 (`findByUserId`, `findByDateAndRestaurantId` 등)
- `@Query` (JPQL)
- `@Transactional`

**학습 깊이**
- 단순 조회/저장: `JpaRepository` 메서드로 처리
- 조건이 2개 이상인 조회: 쿼리 메서드 네이밍 규칙
- 그 이상 복잡한 쿼리: `@Query`로 JPQL 직접 작성
- N+1 문제가 무엇인지는 알아야 합니다 (해결까지는 이 프로젝트에서 불필요)
- `FetchType.LAZY`를 기본으로 쓰는 이유는 이해하세요

**프로젝트 적용**
- 모든 도메인의 데이터 조회/저장 (Menu, Review, Recommendation 등)

---

## 5. Lombok

**키워드**
- `@Getter`, `@Setter`
- `@Builder`
- `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`
- `@Slf4j`

**학습 깊이**
각 어노테이션이 어떤 코드를 자동 생성하는지만 알면 됩니다.
Entity에는 `@Setter`를 쓰지 않는 이유 정도는 파악하세요.

**프로젝트 적용**
- 전체 클래스에 공통 적용

---

## 6. Validation

**키워드**
- `@Valid`
- `@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Min`, `@Max`
- `@MethodArgumentNotValidException` 처리

**학습 깊이**
Request DTO 필드에 검증 어노테이션을 붙이고,
컨트롤러에서 `@Valid`로 활성화하는 패턴만 알면 됩니다.

**프로젝트 적용**
- 회원가입 이메일 형식 검증, 리뷰 별점 범위(1~5) 검증 등

---

## 7. 전역 예외 처리

**키워드**
- `@RestControllerAdvice`
- `@ExceptionHandler`
- 커스텀 예외 클래스 (예: `NotFoundException extends RuntimeException`)
- 공통 에러 응답 형식

**학습 깊이**
커스텀 예외를 만들고, `@RestControllerAdvice`에서 잡아서
일관된 JSON 응답으로 반환하는 패턴을 구현할 수 있으면 됩니다.

**프로젝트 적용**
- 존재하지 않는 메뉴/유저 조회, 권한 없는 리뷰 삭제 등 모든 예외 상황

---

## 8. Spring Security + JWT

**키워드**
- `SecurityFilterChain`, `HttpSecurity`
- `OncePerRequestFilter`
- `UsernamePasswordAuthenticationToken`
- `SecurityContextHolder`
- `@AuthenticationPrincipal`
- JWT 구조 (Header.Payload.Signature)
- Access Token / Refresh Token

**학습 깊이**
- Security 설정 전체를 처음부터 짜는 것은 어렵습니다. 패턴을 보고 이해하는 수준으로 시작하세요.
- "JWT 필터가 요청마다 토큰을 검증해서 SecurityContext에 인증 정보를 넣는다"는 흐름을 이해하는 게 핵심입니다.
- `@AuthenticationPrincipal`로 컨트롤러에서 현재 유저 정보를 꺼내는 방법은 반드시 익히세요.

**프로젝트 적용**
- 회원가입/로그인 API, 인증이 필요한 모든 API

---

## 9. 스케줄러

**키워드**
- `@EnableScheduling`
- `@Scheduled(cron = "...")`
- cron 표현식 (초 분 시 일 월 요일)

**학습 깊이**
cron 표현식 형식만 알면 됩니다. 복잡한 스케줄링은 불필요합니다.

**프로젝트 적용**
- 생협 크롤러: 매일 오전 특정 시간에 메뉴 데이터 자동 수집
- FCM 푸시 알림: 매일 오전 10시에 선호 키워드 매칭 후 발송

---

## 10. 외부 HTTP 요청 (WebClient)

**키워드**
- `WebClient`
- `spring-boot-starter-webflux` (WebClient만 쓰는 경우에도 필요)
- `.post()`, `.uri()`, `.bodyValue()`, `.retrieve()`, `.bodyToMono()`

**학습 깊이**
Gemini API에 POST 요청을 보내고 응답을 받는 수준만 필요합니다.
리액티브 프로그래밍(Mono/Flux) 개념을 깊게 알 필요는 없습니다.
`.block()`으로 동기 처리하는 방식으로 사용하면 됩니다.

**프로젝트 적용**
- Gemini API 호출

---

## 11. FCM (Firebase Cloud Messaging)

**키워드**
- `firebase-admin` SDK
- `FirebaseApp.initializeApp()`
- `Message`, `Notification`, `FirebaseMessaging.getInstance().send()`

**학습 깊이**
Firebase Admin SDK 초기화 방법과 단건 메시지 발송 방법만 알면 됩니다.
공식 문서 + AI 질문으로 해결 가능한 수준입니다.

**프로젝트 적용**
- 선호 키워드 매칭 시 푸시 알림 발송

---

## 학습 순서 (권장)

```
1단계 (API 만들기)
  Spring Boot 기초 → Spring MVC → DTO 패턴 → Spring Data JPA → Lombok → Validation

2단계 (안정화)
  전역 예외 처리 → Spring Security + JWT

3단계 (기능 확장)
  스케줄러 → WebClient (Gemini) → FCM
```

---

## AI에게 질문할 때 유용한 프롬프트 패턴

```
"Spring Boot 3.x + Java 17 환경에서 [키워드]를 [상황]에 적용하는 코드 예시를 보여줘"

예시:
- "Spring Boot 3.x에서 @RestControllerAdvice로 커스텀 예외를 JSON으로 반환하는 코드 보여줘"
- "JpaRepository에서 date와 restaurantId로 Menu 목록을 조회하는 쿼리 메서드 작성법"
- "OncePerRequestFilter로 JWT를 검증하고 SecurityContext에 인증 정보 넣는 패턴"
```
