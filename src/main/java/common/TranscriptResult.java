package common;

import java.time.Instant;
import java.util.List;

/**
 * STT 음성 변환 요청의 최종 결과 데이터를 담는 객체임.
 * final 필드와 getter로 구성된 불변 클래스로 설계됨.
  *
 * @author 개발자
 */
public class TranscriptResult {

    // 변환 결과 데이터의 고유 식별값 (UUID 문자열)
    private final String id;
    
    private final String userId;
    
    // 음성의 입력 유형 (예: "FILE", "MIC", "SYSTEM", "MIX")
    private final String source;
    
    // 변환된 음성의 언어 코드 (ISO 639-1 규격, 예: "ko", "en")
    private final String language;

    // 시간대별 텍스트 변환 정보 목록
    private final List<TextSegment> segments;
    
    // API로부터 최종 변환되어 넘어온 전체 텍스트 내용
    private final String rawText;
    
    // LLM을 통해 생성된 요약 내용
    private String summary;
    
    // LLM을 통해 추출된 키워드 (쉼표로 구분된 문자열)
    private String keywords;
    private String memo;
    private String audioPath;
    
    // 이 변환 결과가 생성된 로컬 타임스탬프 시각 정보
    private final Instant createdAt;

    /**
     * 기본 필드를 받아 불변 상태로 초기화하는 생성자임.
     */
    public TranscriptResult(String id, String userId, String source, String language, List<TextSegment> segments, String rawText, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.source = source;
        this.language = language;
        this.segments = segments;
        this.rawText = rawText;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getSource() {
        return source;
    }

    public String getLanguage() {
        return language;
    }

    public List<TextSegment> getSegments() {
        return segments;
    }

    public String getRawText() {
        return rawText;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

