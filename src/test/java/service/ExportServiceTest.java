package service;

import common.TextSegment;
import common.TranscriptResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExportService.exportToSrt: SRT 자막 출력(특히 시간 포맷) 테스트.
 * 임시 파일(@TempDir)에 출력해 내용을 검증하므로 외부 의존성 없음.
 */
class ExportServiceTest {

    @Test
    @DisplayName("초 단위 시간이 SRT 타임코드(HH:MM:SS,mmm)로 변환된다")
    void writesSrtWithCorrectTimecodeAndText(@TempDir Path tempDir) throws Exception {
        List<TextSegment> segments = new ArrayList<>();
        segments.add(new TextSegment(1.5, 3.0, "안녕하세요", null));
        segments.add(new TextSegment(65.25, 70.0, "회의 시작", 2));

        TranscriptResult result = new TranscriptResult(
                "test-id", "user-1", "FILE", "ko", segments, "원문", Instant.now());

        Path out = tempDir.resolve("out.srt");
        new ExportService().exportToSrt(result, out);

        String srt = new String(Files.readAllBytes(out), StandardCharsets.UTF_8);

        // 1.5초 → 00:00:01,500 / 3.0초 → 00:00:03,000
        assertTrue(srt.contains("00:00:01,500 --> 00:00:03,000"), "내용:\n" + srt);
        assertTrue(srt.contains("안녕하세요"));

        // 65.25초 → 00:01:05,250, 화자 번호 표기 포함
        assertTrue(srt.contains("00:01:05,250 --> 00:01:10,000"), "내용:\n" + srt);
        assertTrue(srt.contains("[화자 2] 회의 시작"));

        // 자막 인덱스(1, 2) 포함
        assertTrue(srt.contains("1"));
        assertTrue(srt.contains("2"));
    }
}
