package ui;

import audio.AudioDeviceManager;
import audio.AudioRecorder;
import common.TextSegment;
import common.TranscriptResult;
import service.TranscriptionService;

import javax.sound.sampled.Mixer;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class MainFrame extends JFrame {

    private final TranscriptionService transcriptionService = new TranscriptionService();
    private final AudioRecorder audioRecorder = new AudioRecorder();
    
    private Path selectedFile = null;
    private Mixer.Info currentMicrophone = null;

    private JLabel fileLabel;
    private JButton selectFileBtn;
    private JButton transcribeBtn;
    
    // 신규 추가된 마이크 관련 버튼
    private JButton settingsBtn;
    private JButton startRecordBtn;
    private JButton stopRecordBtn;
    
    private JProgressBar progressBar;
    private JTextArea resultArea;
    private JButton copyBtn;

    public MainFrame() {
        setTitle("SpeechNote - STT 변환기");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 550);
        setLocationRelativeTo(null); // 화면 중앙에 배치
        
        // 시스템에 연결된 기본 마이크를 우선 할당
        List<Mixer.Info> mics = AudioDeviceManager.getMicrophones();
        if (!mics.isEmpty()) {
            currentMicrophone = mics.get(0);
        }
        
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // 상단 패널: 파일 선택 및 녹음 제어
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        settingsBtn = new JButton("설정 ⚙️");
        startRecordBtn = new JButton("🔴 녹음 시작");
        stopRecordBtn = new JButton("⬛ 녹음 중지");
        stopRecordBtn.setEnabled(false);

        selectFileBtn = new JButton("음성 파일 선택");
        fileLabel = new JLabel("선택된 파일 없음");
        transcribeBtn = new JButton("변환 시작");
        transcribeBtn.setEnabled(false);

        topPanel.add(settingsBtn);
        topPanel.add(startRecordBtn);
        topPanel.add(stopRecordBtn);
        
        // 구분자 추가
        topPanel.add(new JLabel("  |  "));
        
        topPanel.add(selectFileBtn);
        topPanel.add(fileLabel);
        topPanel.add(transcribeBtn);

        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(topPanel, BorderLayout.NORTH);

        // 중앙 패널: 로딩 바 및 결과 텍스트 에어리어
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
        progressBar.setString("작업을 진행 중입니다...");

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(resultArea);

        centerPanel.add(progressBar, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 하단 패널: 복사 등 부가 액션
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        copyBtn = new JButton("텍스트 복사");
        copyBtn.setEnabled(false);
        bottomPanel.add(copyBtn);

        add(bottomPanel, BorderLayout.SOUTH);

        // 이벤트 리스너 등록
        setupListeners();
    }

    private void setupListeners() {
        // [설정] 버튼: 마이크 선택 창 띄우기
        settingsBtn.addActionListener(e -> {
            SettingsDialog dialog = new SettingsDialog(this, currentMicrophone);
            dialog.setVisible(true);
            currentMicrophone = dialog.getSelectedMicrophone();
        });

        // [녹음 시작] 버튼
        startRecordBtn.addActionListener(e -> {
            if (currentMicrophone == null) {
                JOptionPane.showMessageDialog(this, "선택된 마이크가 없습니다. [설정]에서 마이크를 선택해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                audioRecorder.start(currentMicrophone);
                
                // UI 상태 변경
                startRecordBtn.setEnabled(false);
                stopRecordBtn.setEnabled(true);
                selectFileBtn.setEnabled(false);
                transcribeBtn.setEnabled(false);
                settingsBtn.setEnabled(false);
                copyBtn.setEnabled(false);
                
                resultArea.setText("");
                progressBar.setString("🔴 마이크 녹음 중입니다...");
                progressBar.setVisible(true);
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "마이크 접근에 실패했습니다:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        // [녹음 중지] 버튼
        stopRecordBtn.addActionListener(e -> {
            Path recordedFile = audioRecorder.stop();
            
            // UI 상태 복원 (일부)
            startRecordBtn.setEnabled(true);
            stopRecordBtn.setEnabled(false);
            
            if (recordedFile != null) {
                selectedFile = recordedFile;
                fileLabel.setText(recordedFile.getFileName().toString());
                transcribeBtn.setEnabled(true);
                
                // 자동으로 변환 버튼을 누르는 효과
                transcribeBtn.doClick();
            } else {
                progressBar.setVisible(false);
                selectFileBtn.setEnabled(true);
                settingsBtn.setEnabled(true);
            }
        });

        // [파일 선택] 버튼
        selectFileBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                selectedFile = file.toPath();
                fileLabel.setText(file.getName());
                transcribeBtn.setEnabled(true);
            }
        });

        // [변환 시작] 버튼
        transcribeBtn.addActionListener(e -> {
            if (selectedFile == null) return;

            // UI 상태 변경: 변환 중
            transcribeBtn.setEnabled(false);
            selectFileBtn.setEnabled(false);
            startRecordBtn.setEnabled(false);
            settingsBtn.setEnabled(false);
            
            progressBar.setString("서버에서 변환 중입니다. 잠시만 기다려주세요...");
            progressBar.setVisible(true);
            resultArea.setText("");
            copyBtn.setEnabled(false);

            transcriptionService.transcribeFileAsync(selectedFile, "ko")
                .thenAccept(result -> SwingUtilities.invokeLater(() -> handleSuccess(result)))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> handleError(ex));
                    return null;
                });
        });

        // [텍스트 복사] 버튼
        copyBtn.addActionListener(e -> {
            String text = resultArea.getText();
            if (!text.isEmpty()) {
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(text), null);
                JOptionPane.showMessageDialog(this, "텍스트가 클립보드에 복사되었습니다.", "복사 완료", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private void handleSuccess(TranscriptResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("▶ 전체 텍스트:\n").append(result.getRawText()).append("\n\n");
        sb.append("▶ 시간대별 상세:\n");
        for (TextSegment seg : result.getSegments()) {
            String speakerStr = seg.getSpeaker() != null ? " (화자 " + seg.getSpeaker() + ")" : "";
            sb.append(String.format("[%.1fs ~ %.1fs]%s %s\n", seg.getStartSec(), seg.getEndSec(), speakerStr, seg.getText()));
        }
        
        resultArea.setText(sb.toString());
        
        // UI 상태 복원
        progressBar.setVisible(false);
        transcribeBtn.setEnabled(true);
        selectFileBtn.setEnabled(true);
        startRecordBtn.setEnabled(true);
        settingsBtn.setEnabled(true);
        copyBtn.setEnabled(true);
    }

    private void handleError(Throwable ex) {
        progressBar.setVisible(false);
        transcribeBtn.setEnabled(true);
        selectFileBtn.setEnabled(true);
        startRecordBtn.setEnabled(true);
        settingsBtn.setEnabled(true);
        
        String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        JOptionPane.showMessageDialog(this, "변환 중 오류가 발생했습니다:\n" + errorMsg, "에러", JOptionPane.ERROR_MESSAGE);
    }
}
