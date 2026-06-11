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
 * OpenAI Chat API를 호출하여 변환된 텍스트의 요약 및 키워드를 추출하는 클라이언트 클래스임.
  *
 * @author 개발자
 */
public class LlmClient {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private final HttpClient httpClient;

    public LlmClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * STT 결과 텍스트를 입력받아 LLM 요약 및 키워드 추출을 수행함.
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

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-3.5-turbo");
        
        JSONArray messages = new JSONArray();
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "You are a helpful assistant that summarizes text and extracts keywords. You only reply in JSON format.");
        messages.put(systemMsg);
        
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.put(userMsg);
        
        requestBody.put("messages", messages);
        requestBody.put("response_format", new JSONObject().put("type", "json_object"));
        requestBody.put("temperature", 0.3);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JSONObject resObj = new JSONObject(response.body());
                JSONArray choices = resObj.getJSONArray("choices");
                String content = choices.getJSONObject(0).getJSONObject("message").getString("content");
                
                JSONObject parsed = new JSONObject(content);
                String summary = parsed.optString("summary", "요약 생성 실패");
                String keywords = parsed.optString("keywords", "키워드 추출 실패");
                
                return new String[]{summary, keywords};
            } else {
                throw new ApiException("LLM API 오류 (" + response.statusCode() + "): " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new ApiException("LLM 통신 중 오류 발생: " + e.getMessage());
        }
    }
}
