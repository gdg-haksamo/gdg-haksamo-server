package com.gdg.haksamo.domain.recommendation.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * prod용 실제 Gemini 호출 (Generative Language REST API, API 키 방식).
 * {@code GEMINI_API_KEY}만 env로 주입하면 동작한다(GCP 서비스계정·SDK 불필요).
 *
 * <p>응답은 JSON 모드로 강제하고, 후보 index와 한 줄 이유만 받아 메뉴를 매칭한다.
 * 호출/파싱 실패는 {@link ErrorCode#RECOMMENDATION_UNAVAILABLE}(503)로 올린다.
 */
@Slf4j
@Component
@Profile("prod")
public class RestGeminiClient implements GeminiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public RestGeminiClient(
            ObjectMapper objectMapper,
            @Value("${gemini.base-url}") String baseUrl,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model) {
        // Gemini 응답 지연이 요청 스레드를 오래 묶지 않도록 connect/read 타임아웃을 명시한다.
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(3))
                .withReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public List<GeminiPick> recommend(GeminiRecommendationRequest request) {
        String prompt = buildPrompt(request);
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.9));
        try {
            String raw = restClient.post()
                    // API 키는 query string(로그·프록시에 노출 위험) 대신 헤더로 전달한다.
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parse(raw);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini 추천 호출 실패", e);
            throw new BusinessException(ErrorCode.RECOMMENDATION_UNAVAILABLE);
        }
    }

    private String buildPrompt(GeminiRecommendationRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 경북대학교 학식 추천 도우미입니다.\n");
        sb.append("아래 [").append(request.mealLabel()).append(" 메뉴] 중에서 사용자에게 추천할 메뉴를 좋은 순서대로 최대 ")
                .append(request.count()).append("개 고르세요.\n");
        sb.append("- 서로 다른 메뉴를 고르고, 가장 추천하는 것을 맨 앞에 두세요.\n");
        sb.append("- 반드시 아래 목록의 index 중에서만 고르세요(목록에 없는 메뉴를 만들지 마세요).\n");
        sb.append("- 선호 키워드가 있으면 우선 반영하고, 없으면 영양 균형과 보편적 선호로 고르세요.\n");
        sb.append("- 사용자의 선호 식당(★ 표시) 메뉴를 우선적으로 고려하세요.\n\n");

        sb.append("[").append(request.mealLabel()).append(" 메뉴]\n");
        for (GeminiCandidate c : request.candidates()) {
            sb.append("index=").append(c.index())
                    .append(c.favorite() ? " | ★선호식당" : "")
                    .append(" | ").append(c.name())
                    .append(" | 식당=").append(c.restaurant() == null ? "미상" : c.restaurant())
                    .append(" | 칼로리=").append(c.calories() == null ? "?" : c.calories())
                    .append(" 단백질=").append(c.protein() == null ? "?" : c.protein())
                    .append(" 탄수=").append(c.carb() == null ? "?" : c.carb())
                    .append(" 지방=").append(c.fat() == null ? "?" : c.fat())
                    .append('\n');
        }

        sb.append("\n[사용자 선호 키워드]\n");
        sb.append(request.likedKeywords().isEmpty() ? "없음" : String.join(", ", request.likedKeywords()));
        sb.append("\n[사용자 선호 식당]\n");
        sb.append(request.favoriteRestaurants().isEmpty() ? "없음" : String.join(", ", request.favoriteRestaurants()));

        sb.append("\n\n반드시 아래 JSON 형식으로만 답하세요(다른 텍스트 금지):\n");
        sb.append("{\"picks\":[{\"index\":<메뉴 index 정수>}]}");
        return sb.toString();
    }

    /** Gemini 응답(JSON)에서 picks 배열을 추출한다. */
    private List<GeminiPick> parse(String raw) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text.isMissingNode() || text.asText().isBlank()) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_UNAVAILABLE);
        }
        JsonNode picksNode = objectMapper.readTree(text.asText()).path("picks");
        List<GeminiPick> picks = new ArrayList<>();
        for (JsonNode node : picksNode) {
            picks.add(new GeminiPick(node.path("index").asInt(-1)));
        }
        return picks;
    }
}
