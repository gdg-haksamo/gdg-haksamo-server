package com.gdg.haksamo.domain.menu.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * prod용 실제 Gemini 호출로 메뉴 한줄설명·탄단지 생성 (Generative Language REST API).
 * 한 청크를 한 번에 보내고, 각 메뉴를 {@code id}로 echo시켜 매칭한다(위치 의존 X → 누락/순서변경 안전).
 * 실패 시 빈 리스트를 반환해 호출부가 그 청크만 건너뛰게 한다(크롤·다른 청크에 영향 없음).
 */
@Slf4j
@Component
@Profile("prod")
public class RestMenuInfoGenerator implements MenuInfoGenerator {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public RestMenuInfoGenerator(
            ObjectMapper objectMapper,
            @Value("${gemini.base-url}") String baseUrl,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(3))
                .withReadTimeout(Duration.ofSeconds(20)); // 배치라 추천보다 여유
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public List<GeneratedMenuInfo> generate(List<MenuInfoTarget> targets) {
        if (targets.isEmpty()) {
            return List.of();
        }
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", buildPrompt(targets))))),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.4));
        try {
            String raw = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parse(raw);
        } catch (Exception e) {
            log.warn("메뉴정보 Gemini 호출 실패(청크 스킵): {}", e.toString());
            return List.of();
        }
    }

    private String buildPrompt(List<MenuInfoTarget> targets) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 학식 메뉴 정보 생성기입니다. 아래 메뉴 각각에 대해 1인분 기준으로 만들어주세요.\n");
        sb.append("- description: 한국어 한 문장(존댓말, 광고성 과장 금지)\n");
        sb.append("- calories(kcal)/protein(g)/carb(g)/fat(g): 합리적인 정수 추정값\n");
        sb.append("- 반드시 주어진 id를 그대로 echo 하고, 모든 id를 빠짐없이 포함하세요.\n\n");
        sb.append("[메뉴]\n");
        for (MenuInfoTarget t : targets) {
            sb.append("id=").append(t.menuId())
                    .append(" | ").append(t.name())
                    .append(" | 식당=").append(t.restaurant() == null ? "미상" : t.restaurant())
                    .append('\n');
        }
        sb.append("\n반드시 아래 JSON 형식으로만 답하세요(다른 텍스트 금지):\n");
        sb.append("{\"items\":[{\"id\":<정수>,\"description\":\"<설명>\",\"calories\":<정수>,")
                .append("\"protein\":<정수>,\"carb\":<정수>,\"fat\":<정수>}]}");
        return sb.toString();
    }

    private List<GeneratedMenuInfo> parse(String raw) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text.isMissingNode() || text.asText().isBlank()) {
            return List.of();
        }
        JsonNode items = objectMapper.readTree(text.asText()).path("items");
        List<GeneratedMenuInfo> result = new ArrayList<>();
        for (JsonNode node : items) {
            if (!node.hasNonNull("id")) {
                continue;
            }
            result.add(new GeneratedMenuInfo(
                    node.path("id").asLong(),
                    node.path("description").asText(null),
                    intOrNull(node, "calories"),
                    intOrNull(node, "protein"),
                    intOrNull(node, "carb"),
                    intOrNull(node, "fat")));
        }
        return result;
    }

    private Integer intOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isNumber() ? v.asInt() : null;
    }
}
