package common;

import java.time.Instant;

/**
 * STT 음성 변환 요청의 최종 결과 데이터를 담는 객체임.
 * final 필드와 getter로 구성된 불변 클래스로 설계됨.
 */
public class TranscriptResult {

    // 변환 결과 데이터의 고유 식별값 (UUID 문자열)
    private final String id;
    
    // 음성의 입력 유형 (예: "FILE", "MIC", "SYSTEM", "MIX")
    private final String source;
    
    // 변환된 음성의 언어 코드 (ISO 639-1 규격, 예: "ko", "en")
    private final String language;
    
    // API로부터 최종 변환되어 넘어온 전체 텍스트 내용
    private final String text;
    
    // 이 변환 결과가 생성된 로컬 타임스탬프 시각 정보
    private final Instant createdAt;

    /**
     * 모든 필드를 한 번에 받아 불변 상태로 초기화하는 생성자임.
     */
    public TranscriptResult(String id, String source, String language, String text, Instant createdAt) {
        this.id = id;
        this.source = source;
        this.language = language;
        this.text = text;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getLanguage() {
        return language;
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
