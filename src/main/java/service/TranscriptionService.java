package service;

import api.SttClient;
import api.SttResponse;
import common.TranscriptResult;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 외부 모듈 연동을 위한 단일 진입점 서비스 클래스임.
 * STT 변환 처리를 통합 수행하여 결과를 반환함.
 */
public class TranscriptionService {

    // OpenAI API 호출을 대행하는 핵심 API 클라이언트 객체
    private final SttClient sttClient = new SttClient();
    
    // DB 저장소 접근 객체
    private final db.TranscriptDao transcriptDao = new db.TranscriptDao();

    /**
     * 로컬 음성 파일을 Whisper API를 통해 텍스트로 변환하고, 메타데이터를 포함한 결과를 반환함.
     * @param audioFile 변환 처리를 진행할 로컬 음성 파일(.wav)의 Path 객체
     * @param language 인식 대상 언어 정보 (예: "ko", "en")
     * @return 식별 고유 ID와 현재 시각, 텍스트가 바인딩된 TranscriptResult 결과 객체
     */
    public TranscriptResult transcribeFile(Path audioFile, String language) {
        // API 클라이언트를 호출하여 음성 변환 응답을 획득함.
        SttResponse response = sttClient.transcribe(audioFile, language);

        // 고유 UUID 및 현재 타임스탬프를 부여하여 최종 TranscriptResult 객체를 빌드함.
        TranscriptResult result = new TranscriptResult(
                UUID.randomUUID().toString(),
                "FILE", // 추후 MIC 등 동적으로 받을 수 있게 수정 가능
                language,
                response.getSegments(),
                response.getText(),
                Instant.now()
        );
        
        // 변환 성공 시 백그라운드 스레드에서 자동으로 로컬 SQLite DB에 저장함.
        try {
            transcriptDao.saveTranscript(result);
            System.out.println("[TranscriptionService] DB 저장 성공: " + result.getId());
        } catch (Exception e) {
            System.err.println("[TranscriptionService] DB 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    /**
     * 로컬 음성 파일을 Whisper API를 통해 텍스트로 변환하고, 메타데이터를 포함한 결과를 비동기적으로 반환함.
     * @param audioFile 변환 처리를 진행할 로컬 음성 파일(.wav)의 Path 객체
     * @param language 인식 대상 언어 정보 (예: "ko", "en")
     * @return TranscriptResult 결과를 담은 CompletableFuture 객체
     */
    public CompletableFuture<TranscriptResult> transcribeFileAsync(Path audioFile, String language) {
        // 비동기 스레드 풀에서 동기 변환 작업을 실행하도록 위임함.
        return CompletableFuture.supplyAsync(() -> transcribeFile(audioFile, language));
    }
}

