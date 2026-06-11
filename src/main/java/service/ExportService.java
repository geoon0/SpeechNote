package service;

import common.TextSegment;
import common.TranscriptResult;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * ExportService 클래스
 *
 * @author 개발자
 */
public class ExportService {

    /**
     * 변환 결과를 SRT 자막 포맷으로 내보냅니다.
     */
    public void exportToSrt(TranscriptResult result, Path destPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        List<TextSegment> segments = result.getSegments();
        for (int i = 0; i < segments.size(); i++) {
            TextSegment seg = segments.get(i);
            sb.append(i + 1).append("\n");
            sb.append(formatSrtTime(seg.getStartSec())).append(" --> ").append(formatSrtTime(seg.getEndSec())).append("\n");
            String speakerStr = seg.getSpeaker() != null ? "[화자 " + seg.getSpeaker() + "] " : "";
            sb.append(speakerStr).append(seg.getText()).append("\n\n");
        }
        Files.writeString(destPath, sb.toString(), StandardCharsets.UTF_8);
    }

    private String formatSrtTime(double seconds) {
        long millis = (long) (seconds * 1000);
        long hr = millis / 3600000;
        millis %= 3600000;
        long min = millis / 60000;
        millis %= 60000;
        long sec = millis / 1000;
        millis %= 1000;
        return String.format("%02d:%02d:%02d,%03d", hr, min, sec, millis);
    }

    /**
     * 변환 결과와 요약, 키워드를 DOCX 포맷으로 내보냅니다.
     */
    public void exportToDocx(TranscriptResult result, Path destPath) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(destPath.toFile())) {

            // 제목
            XWPFParagraph titlePara = document.createParagraph();
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("음성 기록: " + result.getId().substring(0, 8));
            titleRun.setBold(true);
            titleRun.setFontSize(16);

            // 요약 및 키워드 (존재할 경우)
            if (result.getSummary() != null && !result.getSummary().isEmpty()) {
                XWPFParagraph summaryTitle = document.createParagraph();
                XWPFRun stRun = summaryTitle.createRun();
                stRun.setText("요약 (Summary)");
                stRun.setBold(true);
                
                XWPFParagraph summaryBody = document.createParagraph();
                summaryBody.createRun().setText(result.getSummary());
            }

            if (result.getKeywords() != null && !result.getKeywords().isEmpty()) {
                XWPFParagraph kwTitle = document.createParagraph();
                XWPFRun kwRun = kwTitle.createRun();
                kwRun.setText("키워드: " + result.getKeywords());
                kwRun.setItalic(true);
            }

            // 본문 (시간대별)
            XWPFParagraph contentTitle = document.createParagraph();
            XWPFRun ctRun = contentTitle.createRun();
            ctRun.setText("상세 내용");
            ctRun.setBold(true);
            
            for (TextSegment seg : result.getSegments()) {
                XWPFParagraph p = document.createParagraph();
                XWPFRun r = p.createRun();
                String speakerStr = seg.getSpeaker() != null ? " (화자 " + seg.getSpeaker() + ")" : "";
                String timeStr = String.format("[%.1fs ~ %.1fs]%s", seg.getStartSec(), seg.getEndSec(), speakerStr);
                r.setText(timeStr + " " + seg.getText());
            }

            document.write(out);
        }
    }
}
