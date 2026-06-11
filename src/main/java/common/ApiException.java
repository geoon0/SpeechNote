package common;

/**
 * STT API 연동 과정에서 발생하는 예외를 처리하기 위한 예외 클래스임.
 * RuntimeException을 상속받아 정의됨.
  *
 * @author 개발자
 */
public class ApiException extends RuntimeException {

    /**
     * 예외 메시지를 입력받는 생성자임.
     * @param message 에러 원인을 설명하는 메시지
     */
    public ApiException(String message) {
        super(message);
    }

    /**
     * 예외 메시지와 함께 원인이 되는 하위 예외를 함께 받아 처리하는 생성자임.
     * @param message 에러 원인을 설명하는 메시지
     * @param cause 실제 발생한 내부 예외(IOException 등)
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
