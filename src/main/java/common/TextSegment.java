package common;

/**
 * 특정 시간 범위의 텍스트 변환 정보를 담는 클래스임.
 * 시작 시간, 끝 시간, 해당 구간의 텍스트 내용을 보관함.
 */
public class TextSegment {
    
    // 구간 시작 시간 (초 단위)
    private final double startSec;
    
    // 구간 종료 시간 (초 단위)
    private final double endSec;
    
    // 해당 구간의 변환된 텍스트 내용
    private final String text;
    
    // 화자 ID (선택 사항)
    private final Integer speaker;

    /**
     * 시간 정보와 텍스트 내용을 매개변수로 받아 초기화하는 생성자임.
     */
    public TextSegment(double startSec, double endSec, String text, Integer speaker) {
        this.startSec = startSec;
        this.endSec = endSec;
        this.text = text;
        this.speaker = speaker;
    }

    public double getStartSec() {
        return startSec;
    }

    public double getEndSec() {
        return endSec;
    }

    public String getText() {
        return text;
    }

    public Integer getSpeaker() {
        return speaker;
    }
}
