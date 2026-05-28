package api;

import common.ApiConfig;
import common.ApiException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * OpenAI Whisper STT API를 호출하여 음성 파일을 텍스트로 변환하는 클라이언트 클래스임.
 * java.net.http.HttpClient와 바이트 기반 Multipart 본문 빌더를 사용함.
 */
public class SttClient {

    // Java 내장 HTTP 통신 객체 (ConnectTimeout 10초)
    private final HttpClient http;

    public SttClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 파일 경로와 사용할 언어를 입력받아 STT 변환 텍스트 결과를 반환함.
     */
    public String transcribe(Path audioFile, String language) {
        try {
            // multipart 전송용 고유 바운더리 생성
            String boundary = "----Boundary" + System.currentTimeMillis();

            // 1. multipart 본문 생성 (private 헬퍼 메서드 호출)
            byte[] body = buildMultipartBody(audioFile, language, boundary);

            // 2. HttpRequest 객체 빌드 (URL, 인증 헤더, Content-Type, timeout 5분)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ApiConfig.getSttUrl()))
                    .timeout(Duration.ofMinutes(5))
                    .header("Authorization", "Bearer " + ApiConfig.getApiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            // 3. HTTP 동기 전송 수행
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            // 4. HTTP 상태 코드에 따른 예외 분기 처리
            int status = response.statusCode();
            if (status >= 400 && status < 500) {
                throw new ApiException("입력 오류 (" + status + "): " + response.body());
            } else if (status >= 500) {
                throw new ApiException("서버 오류 (" + status + "), 잠시 후 재시도");
            }

            // 5. JSON 파싱을 통해 text 필드값 추출 및 반환
            return new JSONObject(response.body()).getString("text");

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt(); // 인터럽트 상태 복구
            }
            throw new ApiException("네트워크 오류", e);
        }
    }

    /**
     * Multipart/form-data 규격에 부합하는 바이트 배열 바디를 빌드하는 내부 메서드임.
     */
    private byte[] buildMultipartBody(Path file, String language, String boundary) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 1) file 파트 작성 (Content-Type: audio/wav, 오디오 바이트)
        writeFilePart(out, file, boundary);

        // 2) model 파트 작성 ("whisper-1")
        writeTextPart(out, "model", "whisper-1", boundary);

        // 3) language 파트 작성 (ISO 639-1 언어 코드)
        writeTextPart(out, "language", language, boundary);

        // 전체 multipart 통신의 마감 바운더리 작성
        out.write(("--" + boundary + "--\r\n").getBytes());
        return out.toByteArray();
    }

    /**
     * 일반 텍스트 데이터를 Multipart 스트림에 작성하는 헬퍼 메서드임.
     */
    private void writeTextPart(ByteArrayOutputStream out, String name, String value, String boundary) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes());
        out.write((value + "\r\n").getBytes());
    }

    /**
     * 업로드할 오디오 파일의 바이너리 데이터를 Multipart 스트림에 작성하는 헬퍼 메서드임.
     */
    private void writeFilePart(ByteArrayOutputStream out, Path file, String boundary) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getFileName().toString() + "\"\r\n").getBytes());
        out.write("Content-Type: audio/wav\r\n\r\n".getBytes());
        out.write(Files.readAllBytes(file));
        out.write("\r\n".getBytes());
    }
}
