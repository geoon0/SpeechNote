package api;

import common.ApiException;
import common.TextSegment;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SttResponse.fromJson 파싱 로직 테스트.
 * 외부 서버 없이 JSON 문자열만으로 검증 가능한 순수 로직.
 */
class SttResponseTest {

    @Test
    @DisplayName("정상 JSON(소문자 필드)을 텍스트와 세그먼트로 파싱한다")
    void parsesNormalLowercaseJson() {
        String json = new JSONObject()
                .put("text", "안녕 세계")
                .put("segments", new JSONArray().put(new JSONObject()
                        .put("start", 0.0).put("end", 1.5).put("content", "안녕").put("speaker", 1)))
                .toString();

        SttResponse res = SttResponse.fromJson(json);

        assertEquals("안녕 세계", res.getText());
        assertEquals(1, res.getSegments().size());
        TextSegment seg = res.getSegments().get(0);
        assertEquals(0.0, seg.getStartSec());
        assertEquals(1.5, seg.getEndSec());
        assertEquals("안녕", seg.getText());
        assertEquals(Integer.valueOf(1), seg.getSpeaker());
    }

    @Test
    @DisplayName("대문자 필드(Start/End/Content/Speaker)도 파싱한다")
    void parsesCapitalizedFields() {
        String json = new JSONObject()
                .put("text", "hi")
                .put("segments", new JSONArray().put(new JSONObject()
                        .put("Start", 1.0).put("End", 2.0).put("Content", "hello").put("Speaker", 2)))
                .toString();

        SttResponse res = SttResponse.fromJson(json);

        TextSegment seg = res.getSegments().get(0);
        assertEquals(1.0, seg.getStartSec());
        assertEquals(2.0, seg.getEndSec());
        assertEquals("hello", seg.getText());
        assertEquals(Integer.valueOf(2), seg.getSpeaker());
    }

    @Test
    @DisplayName("segments 없이 text에 JSON 배열이 담겨 와도 fallback으로 복구한다")
    void recoversFromBrokenSegmentsInText() {
        JSONArray inner = new JSONArray()
                .put(new JSONObject().put("Start", 0.0).put("End", 1.0).put("Content", "가"))
                .put(new JSONObject().put("Start", 1.0).put("End", 2.0).put("Content", "나"));
        // text 필드 안에 배열 문자열이 그대로 담겨 온 비정상 응답
        String json = new JSONObject().put("text", inner.toString()).toString();

        SttResponse res = SttResponse.fromJson(json);

        List<TextSegment> segs = res.getSegments();
        assertEquals(2, segs.size());
        assertEquals("가", segs.get(0).getText());
        assertEquals("나", segs.get(1).getText());
        // 더러운 원본 text가 정제된 텍스트로 대체됨
        assertEquals("가 나", res.getText());
    }

    @Test
    @DisplayName("status=error 응답은 ApiException을 던진다")
    void throwsOnErrorStatus() {
        String json = new JSONObject()
                .put("status", "error")
                .put("message", "테스트 오류")
                .toString();

        ApiException ex = assertThrows(ApiException.class, () -> SttResponse.fromJson(json));
        assertTrue(ex.getMessage().contains("테스트 오류"));
    }
}
