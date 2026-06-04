package api;

import common.ApiConfig;
import common.ApiException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OpenAI Whisper STT API를 호출하여 음성 파일을 텍스트로 변환하는 클라이언트 클래스임.
 * java.net.http.HttpClient와 MultipartBodyPublisher를 활용하여 요청을 수행함.
 */
public class SttClient {

    // java.util.logging 로거 설정
    private static final Logger logger = Logger.getLogger(SttClient.class.getName());

    // Java 내장 HTTP 통신 객체 (ConnectTimeout 10초)
    private final HttpClient http;

    public SttClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 파일 경로와 사용할 언어를 입력받아 STT 변환 결과를 SttResponse 객체로 반환함.
     * @param audioFile 변환 처리를 할 음성 파일 경로
     * @param language 인식 대상 언어 코드 (예: "ko", "en")
     * @return 텍스트 및 세그먼트 데이터가 매핑된 SttResponse 인스턴스
     */
    public SttResponse transcribe(Path audioFile, String language) {
        logger.info("[SttClient] STT 변환 작업 요청 시작함. 파일: " + audioFile.getFileName() + ", 언어: " + language);
        try {
            // multipart 전송용 고유 바운더리 생성
            String boundary = "----Boundary" + System.currentTimeMillis();

            // 1. multipart 본문 생성 (MultipartBodyPublisher 헬퍼 클래스 호출)
            HttpRequest.BodyPublisher bodyPublisher = MultipartBodyPublisher.ofMultipart(audioFile, language, boundary);

            // 2. HttpRequest 객체 빌드 (URL, 인증 헤더, Content-Type, timeout 5분)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ApiConfig.getSttUrl()))
                    .timeout(Duration.ofMinutes(5))
                    .header("Authorization", "Bearer " + ApiConfig.getApiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(bodyPublisher)
                    .build();

            // 3. HTTP 동기 전송 수행
            logger.info("[SttClient] OpenAI Whisper API 서버로 HTTP POST 요청을 전송함.");
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            // 4. HTTP 상태 코드에 따른 예외 분기 처리
            int status = response.statusCode();
            logger.info("[SttClient] HTTP 응답 수신함. 상태 코드: " + status);
            
            if (status >= 400 && status < 500) {
                String errMsg = "입력 오류 (" + status + "): " + response.body();
                logger.warning("[SttClient] API 호출 오류 발생함. " + errMsg);
                throw new ApiException(errMsg);
            } else if (status >= 500) {
                String errMsg = "서버 오류 (" + status + "), 잠시 후 재시도";
                logger.warning("[SttClient] API 호출 오류 발생함. " + errMsg);
                throw new ApiException(errMsg);
            }

            // 5. JSON 파싱을 수행하여 SttResponse 객체 생성 및 반환
            logger.info("[SttClient] STT 응답 JSON 파싱을 성공적으로 완료함.");
            return SttResponse.fromJson(response.body());

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt(); // 인터럽트 상태 복구
            }
            logger.log(Level.SEVERE, "[SttClient] STT 통신 또는 파일 입출력 중 네트워크 오류 발생함.", e);
            throw new ApiException("네트워크 오류", e);
        }
    }
}

