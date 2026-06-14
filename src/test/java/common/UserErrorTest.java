package common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserError.friendly: 내부 예외 → 사용자용 한글 메시지 변환 로직 테스트.
 */
class UserErrorTest {

    @Test
    @DisplayName("네트워크(호스트 못 찾음) 예외는 인터넷 연결 안내로 변환된다")
    void networkError() {
        String msg = UserError.friendly(new UnknownHostException("api.example.com"));
        assertTrue(msg.contains("인터넷"), "메시지: " + msg);
    }

    @Test
    @DisplayName("지원하지 않는 오디오 형식 예외는 WAV 안내로 변환된다")
    void unsupportedAudio() {
        String msg = UserError.friendly(new UnsupportedAudioFileException());
        assertTrue(msg.contains("WAV"), "메시지: " + msg);
    }

    @Test
    @DisplayName("DB UNIQUE 제약 위반은 '이미 존재' 안내로 변환된다")
    void duplicateDbValue() {
        String msg = UserError.friendly(new SQLException("UNIQUE constraint failed: users.username"));
        assertTrue(msg.contains("이미 존재"), "메시지: " + msg);
    }

    @Test
    @DisplayName("비동기 래퍼(CompletionException)는 벗겨내고 원인으로 판단한다")
    void unwrapsAsyncWrapper() {
        Throwable wrapped = new CompletionException(new UnknownHostException("x"));
        String msg = UserError.friendly(wrapped);
        assertTrue(msg.contains("인터넷"), "메시지: " + msg);
    }

    @Test
    @DisplayName("이미 정제된 ApiException 메시지는 그대로 전달한다")
    void passthroughApiException() {
        String msg = UserError.friendly(new ApiException("요청이 거부되었습니다"));
        assertEquals("요청이 거부되었습니다", msg);
    }

    @Test
    @DisplayName("메시지가 없는 예외는 일반 안내 문구로 변환된다")
    void emptyMessageFallback() {
        String msg = UserError.friendly(new RuntimeException());
        assertTrue(msg.contains("알 수 없는 오류"), "메시지: " + msg);
    }
}
