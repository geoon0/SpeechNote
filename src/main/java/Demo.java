import common.TranscriptResult;
import common.TextSegment;
import service.TranscriptionService;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 개발된 STT 모듈의 OpenAI API 연동 상태를 검증하기 위한 데모 실행 클래스임.
 * 프로젝트 루트 디렉토리에 'test.wav' 파일 및 'config.properties' 설정이 필요함.
  *
 * @author 개발자
 */
public class Demo {
    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("    🎙️ OpenAI Whisper STT API 검증 데모    ");
        System.out.println("=========================================");

        try {
            // 1. 통합 서비스 객체 생성
            TranscriptionService service = new TranscriptionService();

            System.out.println("[진행] 1. 동기식(Sync) STT 변환 요청 전송 중...");
            
            // 2. test.wav 로컬 음성 파일을 한국어("ko")로 동기식 변환 요청함.
            TranscriptResult result = service.transcribeFile(Path.of("test.wav"), "ko", "demo_user");

            // 3. 변환 완료 후 최종 결과 데이터를 콘솔에 출력함.
            printResult(result, "동기식(Sync)");

            System.out.println("\n[진행] 2. 비동기식(Async) STT 변환 요청 전송 중...");

            // 4. 비동기식 변환 요청 수행 후 응답 대기함.
            CompletableFuture<TranscriptResult> asyncFuture = service.transcribeFileAsync(Path.of("test.wav"), "ko", "demo_user");
            
            // 비동기 실행 흐름 확인을 위한 메시지 출력
            System.out.println("[정보] 비동기 요청 후 메인 스레드는 대기하지 않고 계속 흘러감.");

            TranscriptResult asyncResult = asyncFuture.join();
            printResult(asyncResult, "비동기식(Async)");

        } catch (Exception e) {
            System.err.println("\n❌ 오류 발생! STT 연동 검증 실패함.");
            System.err.println("원인: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printResult(TranscriptResult result, String mode) {
        System.out.println("\n🎉 [" + mode + "] 음성 변환 성공!");
        System.out.println("-----------------------------------------");
        System.out.println("▶️ 고유 ID    : " + result.getId());
        System.out.println("▶️ 입력 소스  : " + result.getSource());
        System.out.println("▶️ 요청 언어  : " + result.getLanguage());
        System.out.println("▶️ 변환 시각  : " + result.getCreatedAt());
        System.out.println("▶️ 변환 텍스트 : " + result.getRawText());
        System.out.println("▶️ 세그먼트 개수: " + result.getSegments().size());
        
        if (!result.getSegments().isEmpty()) {
            System.out.println("▶️ 시간대별 상세 텍스트:");
            for (TextSegment seg : result.getSegments()) {
                String speakerStr = seg.getSpeaker() != null ? " (화자 " + seg.getSpeaker() + ")" : "";
                System.out.printf("   [%.1fs ~ %.1fs]%s %s\n", seg.getStartSec(), seg.getEndSec(), speakerStr, seg.getText());
            }
        }
        System.out.println("-----------------------------------------");
    }
}

