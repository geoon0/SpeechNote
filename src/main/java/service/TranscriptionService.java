package service;

import api.LlmClient;
import api.SttClient;
import api.SttResponse;
import common.TranscriptResult;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * 외부 모듈 연동을 위한 단일 진입점 서비스 클래스임.
 * STT 변환 처리를 통합 수행하여 결과를 반환함.
  *
 * @author 개발자
 */
public class TranscriptionService {

    // OpenAI API 호출을 대행하는 핵심 API 클라이언트 객체
    private final SttClient sttClient = new SttClient();
    
    // LLM API 클라이언트 객체
    private final LlmClient llmClient = new LlmClient();
    
    // DB 저장소 접근 객체
    private final db.TranscriptDao transcriptDao = new db.TranscriptDao();

    /**
     * 로컬 음성 파일을 Whisper API를 통해 텍스트로 변환하고, 메타데이터를 포함한 결과를 반환함.
     * @param audioFile 변환 처리를 진행할 로컬 음성 파일(.wav)의 Path 객체
     * @param language 인식 대상 언어 정보 (예: "ko", "en")
     * @return 식별 고유 ID와 현재 시각, 텍스트가 바인딩된 TranscriptResult 결과 객체
     */
    public TranscriptResult transcribeFile(Path audioFile, String language, String userId) {
        // API 클라이언트를 호출하여 음성 변환 응답을 획득함.
        SttResponse response = sttClient.transcribe(audioFile, language);

        // 변환 도중 취소(인터럽트) 요청이 있었는지 확인
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("STT 변환이 취소되었습니다.");
        }

        // 고유 UUID 및 현재 타임스탬프를 부여하여 최종 TranscriptResult 객체를 빌드함.
        TranscriptResult result = new TranscriptResult(
                UUID.randomUUID().toString(),
                userId,
                "FILE", // 추후 MIC 등 동적으로 받을 수 있게 수정 가능
                language,
                response.getSegments(),
                response.getText(),
                Instant.now()
        );
        
        // 참고: 변환 결과의 DB 저장은 userId·audioPath를 채운 뒤 MainFrame에서 일괄 수행함.
        
        return result;
    }

    /**
     * 로컬 음성 파일을 Whisper API를 통해 텍스트로 변환하고, 메타데이터를 포함한 결과를 비동기적으로 반환함.
     * @param audioFile 변환 처리를 진행할 로컬 음성 파일(.wav)의 Path 객체
     * @param language 인식 대상 언어 정보 (예: "ko", "en")
     * @return TranscriptResult 결과를 담은 CompletableFuture 객체
     */
    public CompletableFuture<TranscriptResult> transcribeFileAsync(Path audioFile, String language, String userId) {
        // 비동기 스레드 풀에서 동기 변환 작업을 실행하도록 위임함.
        // Future.cancel() 호출 시 InterruptedException이 발생할 수 있도록 스레드를 강제 인터럽트하는 것은
        // CompletableFuture 자체적으로는 지원하지 않지만 프레임워크 타임아웃과 cancel 요청에 응답할 수 있음.
        return CompletableFuture.supplyAsync(() -> transcribeFile(audioFile, language, userId));
    }

    /**
     * 기존 TranscriptResult 텍스트를 LLM에 전달하여 요약과 키워드를 생성하고 DB에 업데이트함.
     * @param result 기존 STT 변환 결과
     * @return 요약과 키워드가 채워진 동일한 결과 객체
     */
    public TranscriptResult summarize(TranscriptResult result) {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("LLM 요약이 취소되었습니다.");
        }
        
        String[] llmData = llmClient.summarizeAndExtractKeywords(result.getRawText());
        result.setSummary(llmData[0]);
        result.setKeywords(llmData[1]);
        
        try {
            transcriptDao.updateLlmData(result);
        } catch (Exception e) {
            common.LoggerUtil.logError("LLM 결과 DB 업데이트 실패", e);
        }
        return result;
    }

    /**
     * LLM 요약/키워드 추출을 비동기적으로 수행함.
     */
    public CompletableFuture<TranscriptResult> summarizeAsync(TranscriptResult result) {
        return CompletableFuture.supplyAsync(() -> summarize(result));
    }
}

