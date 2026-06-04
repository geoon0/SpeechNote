package common;

/**
 * 특정 시간 범위의 텍스트 변환 정보를 담는 클래스임.
 * 시작 시간, 끝 시간, 해당 구간의 텍스트 내용을 보관함.
 */
public class Segment {
    
    // 구간 시작 시간 (초 단위)
    private final double startSec;
    
    // 구간 종료 시간 (초 단위)
    private final double endSec;
    
    // 해당 구간의 변환된 텍스트 내용
    private final String text;

    /**
     * 시간 정보와 텍스트 내용을 매개변수로 받아 초기화하는 생성자임.
     */
    public Segment(double startSec, double endSec, String text) {
        this.startSec = startSec;
        this.endSec = endSec;
        this.text = text;
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
}
