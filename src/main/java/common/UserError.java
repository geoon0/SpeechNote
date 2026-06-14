package common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.nio.file.NoSuchFileException;
import java.sql.SQLException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * 내부 예외(Throwable)를 사용자가 이해할 수 있는 한국어 안내 메시지로 변환하는 유틸리티 클래스임.
 * 화면에는 이 메시지를 보여주고, 원본 예외는 호출부에서 LoggerUtil로 따로 기록한다.
 *
 * @author 개발자
 */
public final class UserError {

    private UserError() {}

    /**
     * 예외를 사용자 친화적 메시지로 변환함. (비동기 래퍼는 벗겨내고 원인 예외 기준으로 판단)
     */
    public static String friendly(Throwable t) {
        Throwable e = unwrap(t);
        String msg = e.getMessage() == null ? "" : e.getMessage();

        // 1) 네트워크
        if (e instanceof UnknownHostException) {
            return "서버에 접속할 수 없습니다. 인터넷 연결 상태를 확인해 주세요.";
        }
        if (e instanceof ConnectException) {
            return "서버에 연결할 수 없습니다. 잠시 후 다시 시도하거나, 설정의 API 주소를 확인해 주세요.";
        }
        if (e instanceof HttpTimeoutException || msg.contains("timed out") || msg.contains("timeout")) {
            return "서버 응답이 지연되고 있습니다. 네트워크 상태를 확인하고 다시 시도해 주세요.";
        }
        if (msg.contains("네트워크 오류")) {
            return "네트워크 오류가 발생했습니다. 인터넷 연결을 확인하고 다시 시도해 주세요.";
        }

        // 2) 오디오
        if (e instanceof UnsupportedAudioFileException) {
            return "지원하지 않는 오디오 형식입니다. WAV 형식의 파일을 사용해 주세요.";
        }
        if (e instanceof LineUnavailableException || msg.contains("No line matching")) {
            return "오디오 장치를 사용할 수 없습니다. 스피커·마이크 연결과 Windows 기본 장치 설정을 확인해 주세요.";
        }

        // 3) 파일
        if (e instanceof FileNotFoundException || e instanceof NoSuchFileException) {
            return "파일을 찾을 수 없습니다. 파일이 이동·삭제되지 않았는지 확인해 주세요.";
        }

        // 4) 데이터베이스
        if (e instanceof SQLException) {
            if (msg.contains("UNIQUE") || msg.toLowerCase().contains("constraint")) {
                return "이미 존재하는 값입니다.";
            }
            return "데이터 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
        }

        // 5) 이미 사용자용으로 정제된 메시지(ApiException 등)는 그대로 사용
        if (e instanceof ApiException && !msg.isEmpty()) {
            return msg;
        }

        // 6) 일반 입출력
        if (e instanceof IOException && !msg.isEmpty()) {
            return "입출력 중 오류가 발생했습니다: " + shorten(msg);
        }

        // 7) 그 외
        if (msg.isEmpty()) {
            return "알 수 없는 오류가 발생했습니다. (자세한 내용은 logs/error.log 참고)";
        }
        return shorten(msg);
    }

    /** CompletableFuture 등 래퍼 예외를 벗겨 실제 원인 예외를 찾음. */
    private static Throwable unwrap(Throwable t) {
        Throwable e = t;
        int guard = 0;
        while (e != null && e.getCause() != null && guard++ < 8
                && (e instanceof CompletionException
                    || e instanceof ExecutionException
                    || e.getMessage() == null)) {
            e = e.getCause();
        }
        return e == null ? t : e;
    }

    private static String shorten(String s) {
        s = s.trim();
        if (s.length() > 200) {
            return s.substring(0, 200) + "… (자세한 내용은 logs/error.log 참고)";
        }
        return s;
    }
}
