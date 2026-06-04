package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite 데이터베이스 파일 생성 및 스키마 초기화를 담당하는 매니저 클래스임.
 */
public class DatabaseManager {

    // 로컬 프로그램 폴더 내에 speechnote.db 파일 형태로 저장됨
    private static final String DB_URL = "jdbc:sqlite:speechnote.db";

    /**
     * 데이터베이스 커넥션을 반환함.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * 프로그램 시작 시 호출되어 테이블이 존재하지 않으면 새로 생성함.
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 외래 키 제약 조건 활성화 (SQLite는 기본이 비활성이므로 명시 필요)
            stmt.execute("PRAGMA foreign_keys = ON;");

            // 변환된 텍스트 전체 결과를 저장하는 테이블 (id는 TranscriptResult의 UUID)
            String createTranscriptsTable = "CREATE TABLE IF NOT EXISTS transcripts (" +
                    "id TEXT PRIMARY KEY," +
                    "source TEXT," +
                    "language TEXT," +
                    "raw_text TEXT," +
                    "created_at TEXT" + // ISO-8601 포맷 타임스탬프
                    ");";
            stmt.execute(createTranscriptsTable);

            // 시간대별 화자 및 세그먼트 내용을 저장하는 테이블
            String createSegmentsTable = "CREATE TABLE IF NOT EXISTS segments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "transcript_id TEXT," +
                    "start_sec REAL," +
                    "end_sec REAL," +
                    "speaker INTEGER," + // null 허용
                    "content TEXT," +
                    "FOREIGN KEY(transcript_id) REFERENCES transcripts(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createSegmentsTable);

            System.out.println("로컬 SQLite 데이터베이스(speechnote.db) 연동 및 초기화 성공.");

        } catch (SQLException e) {
            System.err.println("데이터베이스 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
