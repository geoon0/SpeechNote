package api;

import common.TextSegment;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI Whisper API 응답 데이터를 객체로 파싱하여 보관하는 DTO 클래스임.
 * 전체 텍스트 및 시간대별 텍스트 세그먼트를 분리 저장함.
  *
 * @author 개발자
 */
public class SttResponse {

    // 변환 완료된 전체 텍스트
    private final String text;

    // 시간대별 텍스트 변환 세그먼트 목록
    private final List<TextSegment> segments;

    /**
     * 필드를 입력받아 인스턴스를 초기화하는 생성자임.
     */
    public SttResponse(String text, List<TextSegment> segments) {
        this.text = text;
        this.segments = segments;
    }

    /**
     * API로부터 수신한 JSON 응답 문자열을 파싱하여 SttResponse 객체로 생성함.
     * @param json JSON 포맷의 API 응답 바디 문자열
     * @return 파싱이 완료된 SttResponse 인스턴스
     */
    public static SttResponse fromJson(String json) {
        JSONObject obj = new JSONObject(json);
        
        String status = obj.optString("status");
        if ("error".equals(status)) {
            throw new common.ApiException(obj.optString("message", "알 수 없는 에러가 발생했습니다."));
        }
        
        String text = obj.getString("text");
        List<TextSegment> segments = new ArrayList<>();

        // 정상적으로 segments 배열이 온 경우
        if (obj.has("segments") && !obj.isNull("segments") && obj.get("segments") instanceof JSONArray) {
            JSONArray arr = obj.getJSONArray("segments");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject segObj = arr.getJSONObject(i);
                double start = segObj.has("Start") ? segObj.optDouble("Start", 0.0) : segObj.optDouble("start", 0.0);
                double end = segObj.has("End") ? segObj.optDouble("End", 0.0) : segObj.optDouble("end", 0.0);
                String segText = segObj.has("Content") ? segObj.optString("Content", "") : segObj.optString("content", "");
                Integer speaker = null;
                if (segObj.has("Speaker") && !segObj.isNull("Speaker")) speaker = segObj.getInt("Speaker");
                else if (segObj.has("speaker") && !segObj.isNull("speaker")) speaker = segObj.getInt("speaker");
                
                segments.add(new TextSegment(start, end, segText, speaker));
            }
        } 
        
        // 서버 측 오류로 text 필드 안에 JSON 배열 문자열이 그대로 담겨서 온 경우의 예외 처리 (Fallback)
        // 정상적인 파싱을 했는데도 segments가 비어있다면 Fallback 로직을 탑니다.
        if (segments.isEmpty() && (text.contains("[{\"Start\"") || text.contains("[{\"start\"") || text.contains("[{"))) {
            StringBuilder cleanText = new StringBuilder();
            // JSON 문자열이 중간에 잘려서(truncated) 올 수 있으므로, 완전한 {...} 객체만 정규식으로 추출합니다.
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{[^{}]+\\}").matcher(text);
            
            while (m.find()) {
                try {
                    JSONObject segObj = new JSONObject(m.group());
                    
                    // Start/End/Content 중 하나라도 있는지 확인하여 유효한 세그먼트인지 판별
                    if (!segObj.has("Start") && !segObj.has("start") && !segObj.has("Content") && !segObj.has("content")) {
                        continue; 
                    }
                    
                    double start = segObj.has("Start") ? segObj.optDouble("Start", 0.0) : segObj.optDouble("start", 0.0);
                    double end = segObj.has("End") ? segObj.optDouble("End", 0.0) : segObj.optDouble("end", 0.0);
                    String segText = segObj.has("Content") ? segObj.optString("Content", "") : segObj.optString("content", "");
                    
                    Integer speaker = null;
                    if (segObj.has("Speaker") && !segObj.isNull("Speaker")) speaker = segObj.getInt("Speaker");
                    else if (segObj.has("speaker") && !segObj.isNull("speaker")) speaker = segObj.getInt("speaker");
                    
                    segments.add(new TextSegment(start, end, segText, speaker));
                    
                    // [Silence] 같은 메타 태그는 전체 텍스트에서 제외
                    if (!segText.trim().isEmpty() && !segText.toLowerCase().contains("[silence]")) {
                        if (cleanText.length() > 0) cleanText.append(" ");
                        cleanText.append(segText.trim());
                    }
                } catch (Exception e) {
                    // 파싱 불가능한 객체는 무시하고 계속 진행
                }
            }
            
            // 추출된 세그먼트가 하나라도 있다면, 더러운 원본 text를 깨끗하게 추출된 텍스트로 덮어씌움
            if (!segments.isEmpty()) {
                text = cleanText.toString();
            }
        }

        return new SttResponse(text, segments);
    }

    public String getText() {
        return text;
    }

    public List<TextSegment> getSegments() {
        return segments;
    }
}
