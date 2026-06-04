package api;

import common.TextSegment;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI Whisper API 응답 데이터를 객체로 파싱하여 보관하는 DTO 클래스임.
 * 전체 텍스트 및 시간대별 텍스트 세그먼트를 분리 저장함.
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

        if (obj.has("segments") && !obj.isNull("segments")) {
            JSONArray arr = obj.getJSONArray("segments");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject segObj = arr.getJSONObject(i);
                double start = segObj.getDouble("Start");
                double end = segObj.getDouble("End");
                String segText = segObj.getString("Content");
                Integer speaker = segObj.has("Speaker") ? segObj.getInt("Speaker") : null;
                segments.add(new TextSegment(start, end, segText, speaker));
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
