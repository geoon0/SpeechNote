package api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Multipart/form-data 요청 바디를 생성하는 헬퍼 클래스임.
 * OpenAI Whisper API 규격에 맞춰 오디오 파일 및 파라미터 바디를 구성함.
 */
public class MultipartBodyPublisher {

    // 인스턴스화 방지용 private 생성자임
    private MultipartBodyPublisher() {}

    /**
     * 파일 및 파라미터를 기반으로 Multipart 바이트 배열 바디를 생성하여 BodyPublisher로 반환함.
     * @param file 변환을 수행할 음성 파일의 경로
     * @param language 음성 파일의 언어 코드 (예: "ko", "en")
     * @param boundary multipart 경계 문자열
     * @return HTTP 요청 바디에 사용할 BodyPublisher 객체
     * @throws IOException 파일 읽기 중 예외 발생 시
     */
    public static HttpRequest.BodyPublisher ofMultipart(Path file, String language, String boundary) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 1) file 파트 작성 (Content-Type: audio/wav, 오디오 바이트)
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getFileName().toString() + "\"\r\n").getBytes());
        out.write("Content-Type: audio/wav\r\n\r\n".getBytes());
        out.write(Files.readAllBytes(file));
        out.write("\r\n".getBytes());

        // 2) model 파트 작성 ("whisper-1")
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write("Content-Disposition: form-data; name=\"model\"\r\n\r\n".getBytes());
        out.write("whisper-1\r\n".getBytes());

        // 3) language 파트 작성 (ISO 639-1 언어 코드)
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write("Content-Disposition: form-data; name=\"language\"\r\n\r\n".getBytes());
        out.write((language + "\r\n").getBytes());

        // 4) response_format 파트 작성 (segments 정보를 얻기 위해 verbose_json으로 세팅함)
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write("Content-Disposition: form-data; name=\"response_format\"\r\n\r\n".getBytes());
        out.write("verbose_json\r\n".getBytes());

        // 전체 multipart 통신의 마감 바운더리 작성
        out.write(("--" + boundary + "--\r\n").getBytes());

        return HttpRequest.BodyPublishers.ofByteArray(out.toByteArray());
    }
}
