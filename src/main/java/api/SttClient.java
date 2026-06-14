package api;

import common.ApiConfig;
import common.ApiException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 커스텀 STT API 서버를 호출하여 음성 파일을 텍스트로 변환하는 클라이언트 클래스임.
 * (서버 주소는 config.properties의 stt.api.url에서 읽어옴)
 * java.net.http.HttpClient와 MultipartBodyPublisher를 활용하여 요청을 수행함.
  *
 * @author 개발자
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
        
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            attempt++;
            try {
                // multipart 전송용 고유 바운더리 생성
                String boundary = "----Boundary" + System.currentTimeMillis();

                // 1. multipart 본문 생성 (내부 헬퍼 메서드 호출)
                HttpRequest.BodyPublisher bodyPublisher = ofMultipart(audioFile, language, boundary);

                // 2. HttpRequest 객체 빌드 (URL, Content-Type, timeout 5분)
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ApiConfig.getSttUrl()))
                        .timeout(Duration.ofMinutes(5))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(bodyPublisher)
                        .build();

                // 3. HTTP 동기 전송 수행
                logger.info("[SttClient] STT API 서버로 HTTP POST 요청 전송 시도 (" + attempt + "/" + maxRetries + ")");
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

                // 4. HTTP 상태 코드에 따른 예외 분기 처리
                int status = response.statusCode();
                logger.info("[SttClient] HTTP 응답 수신함. 상태 코드: " + status);
                
                if (status >= 400 && status < 500) {
                    // 원본 응답 본문은 로그로만 남기고, 화면에는 사용자 친화적 메시지를 던짐
                    logger.warning("[SttClient] STT 4xx 응답 (" + status + "): " + response.body());
                    String friendly;
                    if (status == 401 || status == 403) {
                        friendly = "STT 서버 인증에 실패했습니다. 설정에서 API 주소·키를 확인해 주세요.";
                    } else if (status == 413) {
                        friendly = "오디오 파일이 너무 큽니다. 더 짧은 파일로 다시 시도해 주세요.";
                    } else {
                        friendly = "STT 요청이 거부되었습니다 (코드 " + status + "). 파일 형식이나 설정을 확인해 주세요.";
                    }
                    throw new ApiException(friendly); // 4xx는 재시도 없이 즉시 실패
                } else if (status >= 500) {
                    String errMsg = "STT 서버 오류 (" + status + ")가 발생했습니다. 잠시 후 다시 시도해 주세요.";
                    logger.warning("[SttClient] API 서버 오류 발생함. status=" + status);
                    if (attempt >= maxRetries) {
                        throw new ApiException(errMsg);
                    }
                    // 재시도 대기 (1초 -> 2초)
                    Thread.sleep(1000 * attempt);
                    continue; // 다음 루프로 이동
                }

                // 5. JSON 파싱을 수행하여 SttResponse 객체 생성 및 반환
                logger.info("[SttClient] STT 응답 JSON 파싱을 성공적으로 완료함.");
                return SttResponse.fromJson(response.body());

            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt(); // 인터럽트 상태 복구
                    throw new ApiException("네트워크 작업이 취소되었습니다.", e);
                }
                
                logger.log(Level.WARNING, "[SttClient] STT 통신 또는 파일 입출력 중 오류 발생 (" + attempt + "/" + maxRetries + ")", e);
                if (attempt >= maxRetries) {
                    throw new ApiException("네트워크 오류 - 최대 재시도 횟수 초과", e);
                }
                
                try {
                    Thread.sleep(1000 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ApiException("재시도 대기 중 취소되었습니다.", ie);
                }
            }
        }
        throw new ApiException("STT 변환에 실패했습니다.");
    }

    /**
     * 파일 및 파라미터를 기반으로 Multipart 바이트 배열 바디를 생성하여 BodyPublisher로 반환함.
     * @param file 변환을 수행할 음성 파일의 경로
     * @param language 음성 파일의 언어 코드 (예: "ko", "en")
     * @param boundary multipart 경계 문자열
     * @return HTTP 요청 바디에 사용할 BodyPublisher 객체
     * @throws IOException 파일 읽기 중 예외 발생 시
     */
    private static HttpRequest.BodyPublisher ofMultipart(Path file, String language, String boundary) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 1) file 파트 작성 (Content-Type: audio/wav, 오디오 바이트)
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getFileName().toString() + "\"\r\n").getBytes());
        out.write("Content-Type: audio/wav\r\n\r\n".getBytes());
        out.write(Files.readAllBytes(file));
        out.write("\r\n".getBytes());

        // 전체 multipart 통신의 마감 바운더리 작성
        out.write(("--" + boundary + "--\r\n").getBytes());

        return HttpRequest.BodyPublishers.ofByteArray(out.toByteArray());
    }
}

