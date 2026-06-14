package api;

import common.ApiConfig;
import common.ApiException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Google Gemini API를 호출하여 변환된 텍스트의 요약 및 키워드를 추출하는 클라이언트 클래스임.
 *
 * @author 개발자
 */
public class LlmClient {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";
    private final HttpClient httpClient;

    public LlmClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * STT 결과 텍스트를 입력받아 Gemini LLM 요약 및 키워드 추출을 수행함.
     * @param text 원본 텍스트
     * @return [0] 요약문, [1] 키워드 목록
     */
    public String[] summarizeAndExtractKeywords(String text) {
        String apiKey = ApiConfig.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ApiException("API 키가 설정되지 않았습니다. 설정에서 API 키를 입력해주세요.");
        }

        String prompt = "다음 텍스트를 분석해서 요구사항에 맞게 JSON 형식으로만 응답해줘. " +
                        "1) summary: 텍스트의 핵심 내용을 3줄 이내로 요약해줘. " +
                        "2) keywords: 핵심 키워드 5개를 쉼표로 구분한 문자열로 만들어줘.\n\n" +
                        "형식: {\"summary\": \"...\", \"keywords\": \"...\"}\n\n" +
                        "텍스트: " + text;

        JSONObject part = new JSONObject().put("text", prompt);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject generationConfig = new JSONObject()
                .put("temperature", 0.3)
                .put("responseMimeType", "application/json");
        JSONObject requestBody = new JSONObject()
                .put("contents", new JSONArray().put(content))
                .put("generationConfig", generationConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + apiKey))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JSONObject resObj = new JSONObject(response.body());
                String contentText = resObj
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                JSONObject parsed = new JSONObject(contentText);
                String summary = parsed.optString("summary", "요약 생성 실패");
                String keywords = parsed.optString("keywords", "키워드 추출 실패");

                return new String[]{summary, keywords};
            } else {
                throw new ApiException(buildErrorMessage(response.statusCode(), response.body()));
            }
        } catch (IOException | InterruptedException e) {
            throw new ApiException("AI 서버(LLM) 통신 중 오류가 발생했습니다. 네트워크 상태를 확인하고 다시 시도해 주세요.", e);
        }
    }

    /**
     * Gemini API의 오류 응답(JSON)을 사람이 읽기 쉬운 메시지로 변환함.
     * 거대한 원본 JSON을 그대로 노출하지 않고, 상태 코드별로 핵심만 안내함.
     */
    private String buildErrorMessage(int status, String body) {
        // 응답 JSON에서 error.message / error.status 추출 시도
        String apiMessage = null;
        String apiStatus = null;
        try {
            JSONObject err = new JSONObject(body).optJSONObject("error");
            if (err != null) {
                apiMessage = err.optString("message", null);
                apiStatus = err.optString("status", null);
            }
        } catch (Exception ignored) {
            // 본문이 JSON이 아니면 무시하고 일반 메시지로 처리
        }

        if (status == 429 || "RESOURCE_EXHAUSTED".equals(apiStatus)) {
            return "Gemini API 무료 사용량(quota)을 초과했습니다.\n"
                 + "잠시 후 다시 시도하거나, Google AI Studio에서 새 API 키를 발급받아 설정에 입력해 주세요.";
        }
        if (status == 400 || status == 403) {
            return "Gemini API 키가 올바르지 않거나 권한이 없습니다. 설정에서 API 키를 확인해 주세요."
                 + (apiMessage != null ? "\n(상세: " + apiMessage + ")" : "");
        }
        if (status >= 500) {
            return "Gemini 서버 오류가 발생했습니다 (" + status + "). 잠시 후 다시 시도해 주세요.";
        }
        // 그 외: 추출된 메시지가 있으면 그것을, 없으면 상태 코드만 안내
        return "Gemini API 오류 (" + status + ")" + (apiMessage != null ? ": " + apiMessage : "");
    }
}
