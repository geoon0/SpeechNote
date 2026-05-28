import common.TranscriptResult;
import service.TranscriptionService;

import java.nio.file.Path;

/**
 * 개발된 STT 모듈의 OpenAI API 연동 상태를 검증하기 위한 데모 실행 클래스임.
 * 프로젝트 루트 디렉토리에 'test.wav' 파일 및 'config.properties' 설정이 필요함.
 */
public class Demo {
    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("    🎙️ OpenAI Whisper STT API 검증 데모    ");
        System.out.println("=========================================");

        try {
            // 1. 통합 서비스 객체 생성
            TranscriptionService service = new TranscriptionService();

            System.out.println("[진행] 'test.wav' 파일 변환 요청 전송 중...");
            
            // 2. test.wav 로컬 음성 파일을 한국어("ko")로 변환 요청함.
            TranscriptResult result = service.transcribeFile(Path.of("test.wav"), "ko");

            // 3. 변환 완료 후 최종 결과 데이터를 콘솔에 출력함.
            System.out.println("\n🎉 음성 변환 성공!");
            System.out.println("-----------------------------------------");
            System.out.println("▶️ 고유 ID    : " + result.getId());
            System.out.println("▶️ 입력 소스  : " + result.getSource());
            System.out.println("▶️ 요청 언어  : " + result.getLanguage());
            System.out.println("▶️ 변환 시각  : " + result.getCreatedAt());
            System.out.println("▶️ 변환 텍스트 : " + result.getText());
            System.out.println("-----------------------------------------");

        } catch (Exception e) {
            System.err.println("\n❌ 오류 발생! STT 연동 검증 실패함.");
            System.err.println("원인: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
