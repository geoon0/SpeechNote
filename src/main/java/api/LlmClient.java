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
                throw new ApiException("Gemini API 오류 (" + response.statusCode() + "): " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new ApiException("LLM 통신 중 오류 발생: " + e.getMessage());
        }
    }
}
