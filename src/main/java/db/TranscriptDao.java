package db;

import common.TextSegment;
import common.TranscriptResult;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * DB에 접근하여 TranscriptResult 데이터를 저장하거나 불러오는 Data Access Object 클래스임.
 */
public class TranscriptDao {

    /**
     * 변환된 음성 결과 전체(메인 텍스트 + 시간대별 세그먼트 배열)를 DB에 저장함.
     * 트랜잭션을 사용하여 두 테이블 저장이 원자적으로 이루어지도록 보장함.
     * @param result API로부터 받은 최종 변환 결과 객체
     */
    public void saveTranscript(TranscriptResult result) throws SQLException {
        String insertTranscript = "INSERT INTO transcripts (id, user_id, source, language, raw_text, summary, keywords, memo, audio_path, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertSegment = "INSERT INTO segments (transcript_id, start_sec, end_sec, speaker, content) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            // 자동 커밋을 끄고 트랜잭션 시작
            conn.setAutoCommit(false);

            try (PreparedStatement tStmt = conn.prepareStatement(insertTranscript);
                 PreparedStatement sStmt = conn.prepareStatement(insertSegment)) {

                // 1. transcripts 테이블 저장
                tStmt.setString(1, result.getId());
                tStmt.setString(2, result.getUserId());
                tStmt.setString(3, result.getSource());
                tStmt.setString(4, result.getLanguage());
                tStmt.setString(5, result.getRawText());
                tStmt.setString(6, result.getSummary());
                tStmt.setString(7, result.getKeywords());
                tStmt.setString(8, result.getMemo());
                tStmt.setString(9, result.getAudioPath());
                tStmt.setString(10, result.getCreatedAt().toString());
                tStmt.executeUpdate();

                // 2. segments 테이블 저장 (Batch 처리로 성능 향상)
                for (TextSegment seg : result.getSegments()) {
                    sStmt.setString(1, result.getId());
                    sStmt.setDouble(2, seg.getStartSec());
                    sStmt.setDouble(3, seg.getEndSec());
                    if (seg.getSpeaker() != null) {
                        sStmt.setInt(4, seg.getSpeaker());
                    } else {
                        sStmt.setNull(4, Types.INTEGER);
                    }
                    sStmt.setString(5, seg.getText());
                    sStmt.addBatch();
                }
                sStmt.executeBatch();

                // 정상 처리되면 DB에 완전히 반영
                conn.commit();
            } catch (SQLException e) {
                // 에러 발생 시 변경사항 롤백
                conn.rollback();
                throw e;
            } finally {
                // 커넥션을 원래 상태로 복구
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * DB에 저장된 특정 사용자의 모든 변환 기록을 불러옴 (최신순).
     * @return TranscriptResult 객체 리스트
     */
    public List<TranscriptResult> getAllTranscripts(String userId) throws SQLException {
        List<TranscriptResult> results = new ArrayList<>();
        String selectTranscripts = "SELECT * FROM transcripts WHERE user_id = ? ORDER BY created_at DESC";
        String selectSegments = "SELECT * FROM segments WHERE transcript_id = ? ORDER BY start_sec ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement tStmt = conn.prepareStatement(selectTranscripts)) {
            
            tStmt.setString(1, userId);
            try (ResultSet rs = tStmt.executeQuery();
                 PreparedStatement sStmt = conn.prepareStatement(selectSegments)) {
                
                while (rs.next()) {
                    String id = rs.getString("id");
                    String source = rs.getString("source");
                    String language = rs.getString("language");
                    String rawText = rs.getString("raw_text");
                    String summary = rs.getString("summary");
                    String keywords = rs.getString("keywords");
                    String memo = rs.getString("memo");
                    String audioPath = rs.getString("audio_path");
                    Instant createdAt = Instant.parse(rs.getString("created_at"));

                    // 해당 트랜스크립트의 세부 세그먼트 배열 조회
                    sStmt.setString(1, id);
                    List<TextSegment> segments = new ArrayList<>();
                    try (ResultSet sRs = sStmt.executeQuery()) {
                        while (sRs.next()) {
                            double start = sRs.getDouble("start_sec");
                            double end = sRs.getDouble("end_sec");
                            Integer speaker = sRs.getObject("speaker") != null ? sRs.getInt("speaker") : null;
                            String content = sRs.getString("content");
                            segments.add(new TextSegment(start, end, content, speaker));
                        }
                    }

                    TranscriptResult tr = new TranscriptResult(id, userId, source, language, segments, rawText, createdAt);
                    tr.setSummary(summary);
                    tr.setKeywords(keywords);
                    tr.setMemo(memo);
                    tr.setAudioPath(audioPath);
                    results.add(tr);
                }
            }
        }
        return results;
    }

    /**
     * 특정 ID의 변환 기록을 DB에서 삭제함. (외래 키 CASCADE로 segments 자동 삭제됨)
     * @param id 삭제할 트랜스크립트의 고유 UUID
     */
    public void deleteTranscript(String id) throws SQLException {
        String deleteSql = "DELETE FROM transcripts WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * 특정 ID의 변환 기록에 LLM 요약과 키워드를 업데이트함.
     * @param result 요약과 키워드가 채워진 TranscriptResult 객체
     */
    public void updateLlmData(TranscriptResult result) throws SQLException {
        String updateSql = "UPDATE transcripts SET summary = ?, keywords = ?, memo = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, result.getSummary());
            pstmt.setString(2, result.getKeywords());
            pstmt.setString(3, result.getMemo());
            pstmt.setString(4, result.getId());
            pstmt.executeUpdate();
        }
    }

    /**
     * 특정 ID의 변환 원문 텍스트를 수정하여 업데이트함 (F-12)
     */
    public void updateRawText(String id, String newText) throws SQLException {
        String updateSql = "UPDATE transcripts SET raw_text = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, newText);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        }
    }
}
