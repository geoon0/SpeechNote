package ui;

import audio.AudioDeviceManager;
import audio.AudioRecorder;
import common.TextSegment;
import common.TranscriptResult;
import db.TranscriptDao;
import service.ExportService;
import service.TranscriptionService;

import javax.sound.sampled.Mixer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainFrame extends JFrame {

    private final TranscriptionService transcriptionService = new TranscriptionService();
    private final AudioRecorder audioRecorder = new AudioRecorder();
    private final TranscriptDao transcriptDao = new TranscriptDao();
    private final ExportService exportService = new ExportService();
    
    private Path selectedFile = null;
    private Mixer.Info currentMicrophone = null;
    private Mixer.Info currentSystemAudio = null;
    private CompletableFuture<TranscriptResult> currentTranscribeTask = null;

    private JLabel fileLabel;
    private JButton selectFileBtn;
    private JButton transcribeBtn;
    
    private JButton settingsBtn;
    private JButton startRecordBtn;
    private JButton pauseRecordBtn;
    private JButton stopRecordBtn;
    private JButton cancelBtn;
    private JLabel timerLabel;
    private JProgressBar levelMeter;
    
    private JComboBox<String> langComboBox;
    
    private JProgressBar progressBar;
    private JTextArea resultArea;
    private JButton copyBtn;
    private JButton exportTxtBtn;
    private JButton exportSrtBtn;
    private JButton exportDocxBtn;
    private JButton deleteBtn;
    private JButton summarizeBtn;
    
    private JList<TranscriptResult> historyList;
    private DefaultListModel<TranscriptResult> historyListModel;
    
    private Timer recordTimer;
    private int recordSeconds = 0;

    public MainFrame() {
        setTitle("SpeechNote - STT 변환기");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600); 
        setLocationRelativeTo(null); 
        
        List<Mixer.Info> inputs = AudioDeviceManager.getAudioInputs();
        if (!inputs.isEmpty()) {
            currentMicrophone = inputs.get(0);
        }
        
        initUI();
        loadHistoryFromDB();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // 1. 상단 패널 (조작 버튼들)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        settingsBtn = new JButton("설정 ⚙️");
        startRecordBtn = new JButton("🔴 녹음 시작");
        pauseRecordBtn = new JButton("⏸ 일시정지");
        pauseRecordBtn.setEnabled(false);
        stopRecordBtn = new JButton("⬛ 녹음 중지");
        stopRecordBtn.setEnabled(false);
        cancelBtn = new JButton("❌ 취소");
        cancelBtn.setEnabled(false);
        timerLabel = new JLabel("00:00");
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        levelMeter = new JProgressBar(0, 100);
        levelMeter.setStringPainted(true);
        levelMeter.setString("Mic Level");
        levelMeter.setPreferredSize(new Dimension(100, 20));

        selectFileBtn = new JButton("음성 파일 선택");
        fileLabel = new JLabel("선택된 파일 없음");
        
        langComboBox = new JComboBox<>(new String[]{"한국어(ko)", "영어(en)", "자동(auto)"});
        
        transcribeBtn = new JButton("변환 시작");
        transcribeBtn.setEnabled(false);

        topPanel.add(settingsBtn);
        topPanel.add(startRecordBtn);
        topPanel.add(pauseRecordBtn);
        topPanel.add(stopRecordBtn);
        topPanel.add(cancelBtn);
        topPanel.add(new JLabel(" [ "));
        topPanel.add(timerLabel);
        topPanel.add(new JLabel(" ] "));
        topPanel.add(levelMeter);
        topPanel.add(new JLabel(" | "));
        topPanel.add(selectFileBtn);
        topPanel.add(fileLabel);
        topPanel.add(langComboBox);
        topPanel.add(transcribeBtn);

        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(topPanel, BorderLayout.NORTH);

        // 2. 중앙 좌우 분할 패널
        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new HistoryCellRenderer());
        
        JScrollPane leftScrollPane = new JScrollPane(historyList);
        leftScrollPane.setPreferredSize(new Dimension(280, 0));
        leftScrollPane.setBorder(BorderFactory.createTitledBorder("과거 기록 목록"));

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(BorderFactory.createTitledBorder("상세 내용"));
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane rightScrollPane = new JScrollPane(resultArea);

        rightPanel.add(progressBar, BorderLayout.NORTH);
        rightPanel.add(rightScrollPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScrollPane, rightPanel);
        splitPane.setDividerLocation(280); 
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(splitPane, BorderLayout.CENTER);

        // 3. 하단 패널 (기타 액션)
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        deleteBtn = new JButton("🗑️ 삭제");
        deleteBtn.setEnabled(false);
        bottomPanel.add(deleteBtn);
        
        summarizeBtn = new JButton("✨ 요약/키워드 추출");
        summarizeBtn.setEnabled(false);
        bottomPanel.add(summarizeBtn);
        
        exportTxtBtn = new JButton("📄 TXT");
        exportTxtBtn.setEnabled(false);
        bottomPanel.add(exportTxtBtn);
        
        exportSrtBtn = new JButton("🎬 SRT");
        exportSrtBtn.setEnabled(false);
        bottomPanel.add(exportSrtBtn);
        
        exportDocxBtn = new JButton("📝 DOCX");
        exportDocxBtn.setEnabled(false);
        bottomPanel.add(exportDocxBtn);
        
        copyBtn = new JButton("복사");
        copyBtn.setEnabled(false);
        bottomPanel.add(copyBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        setupListeners();
    }

    private static class HistoryCellRenderer extends DefaultListCellRenderer {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof TranscriptResult) {
                TranscriptResult tr = (TranscriptResult) value;
                String timeStr = formatter.format(tr.getCreatedAt());
                String preview = tr.getRawText().replace("\n", " ");
                if (preview.length() > 18) preview = preview.substring(0, 18) + "...";
                String icon = "FILE".equals(tr.getSource()) ? "📁 " : "🎤 ";
                setText("<html><b style='font-size:11px'>" + icon + timeStr + "</b><br><span style='color:gray; font-size:10px'>" + preview + "</span></html>");
                setBorder(new EmptyBorder(8, 8, 8, 8));
            }
            return this;
        }
    }

    private void updateTimerLabel() {
        int m = recordSeconds / 60;
        int s = recordSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", m, s));
    }

    private void setupListeners() {
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                TranscriptResult selected = historyList.getSelectedValue();
                if (selected != null) {
                    renderResultToTextArea(selected);
                    deleteBtn.setEnabled(true);
                    exportTxtBtn.setEnabled(true);
                    exportSrtBtn.setEnabled(true);
                    exportDocxBtn.setEnabled(true);
                    summarizeBtn.setEnabled(true);
                } else {
                    deleteBtn.setEnabled(false);
                    exportTxtBtn.setEnabled(false);
                    exportSrtBtn.setEnabled(false);
                    exportDocxBtn.setEnabled(false);
                    summarizeBtn.setEnabled(false);
                }
            }
        });

        settingsBtn.addActionListener(e -> {
            SettingsDialog dialog = new SettingsDialog(this, currentMicrophone, currentSystemAudio);
            dialog.setVisible(true);
            currentMicrophone = dialog.getSelectedMicrophone();
            currentSystemAudio = dialog.getSelectedSystemAudio();
        });

        startRecordBtn.addActionListener(e -> {
            if (currentMicrophone == null && currentSystemAudio == null) {
                JOptionPane.showMessageDialog(this, "녹음할 오디오 장치(마이크 또는 시스템)가 선택되지 않았습니다.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                audioRecorder.startRecording(currentMicrophone, currentSystemAudio);
                
                startRecordBtn.setEnabled(false);
                pauseRecordBtn.setEnabled(true);
                pauseRecordBtn.setText("⏸ 일시정지");
                stopRecordBtn.setEnabled(true);
                cancelBtn.setEnabled(true);
                selectFileBtn.setEnabled(false);
                transcribeBtn.setEnabled(false);
                settingsBtn.setEnabled(false);
                copyBtn.setEnabled(false);
                deleteBtn.setEnabled(false);
                exportTxtBtn.setEnabled(false);
                exportSrtBtn.setEnabled(false);
                exportDocxBtn.setEnabled(false);
                summarizeBtn.setEnabled(false);
                
                historyList.clearSelection();
                resultArea.setText("");
                progressBar.setString("🔴 마이크 녹음 중입니다...");
                progressBar.setVisible(true);
                
                recordSeconds = 0;
                updateTimerLabel();
                if (recordTimer != null) recordTimer.stop();
                // 초시계는 1초마다, 미터기는 100ms마다 갱신하기 위해 tick 카운터 사용
                int[] tick = {0};
                recordTimer = new Timer(100, evt -> {
                    if (audioRecorder.getState() == AudioRecorder.State.RECORDING) {
                        tick[0]++;
                        if (tick[0] >= 10) {
                            recordSeconds++;
                            updateTimerLabel();
                            tick[0] = 0;
                        }
                    }
                    if (audioRecorder.getState() == AudioRecorder.State.RECORDING || audioRecorder.getState() == AudioRecorder.State.PAUSED) {
                        levelMeter.setValue(audioRecorder.getCurrentLevel());
                    } else {
                        levelMeter.setValue(0);
                    }
                });
                recordTimer.start();
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "오류:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        pauseRecordBtn.addActionListener(e -> {
            if (audioRecorder.getState() == AudioRecorder.State.RECORDING) {
                audioRecorder.pauseRecording();
                pauseRecordBtn.setText("▶ 재개");
                progressBar.setString("⏸ 녹음 일시정지됨");
            } else if (audioRecorder.getState() == AudioRecorder.State.PAUSED) {
                audioRecorder.resumeRecording();
                pauseRecordBtn.setText("⏸ 일시정지");
                progressBar.setString("🔴 마이크 녹음 중입니다...");
            }
        });

        stopRecordBtn.addActionListener(e -> {
            if (recordTimer != null) recordTimer.stop();
            Path recordedFile = audioRecorder.stopRecording();
            
            startRecordBtn.setEnabled(true);
            pauseRecordBtn.setEnabled(false);
            stopRecordBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
            
            if (recordedFile != null) {
                selectedFile = recordedFile;
                fileLabel.setText(recordedFile.getFileName().toString());
                transcribeBtn.setEnabled(true);
                transcribeBtn.doClick(); // 알아서 변환 버튼을 눌러줌
            } else {
                progressBar.setVisible(false);
                selectFileBtn.setEnabled(true);
                settingsBtn.setEnabled(true);
            }
        });

        cancelBtn.addActionListener(e -> {
            if (audioRecorder.getState() == AudioRecorder.State.RECORDING || audioRecorder.getState() == AudioRecorder.State.PAUSED) {
                audioRecorder.cancelRecording();
                if (recordTimer != null) recordTimer.stop();
                timerLabel.setText("00:00");
                levelMeter.setValue(0);
                resetUI();
                progressBar.setVisible(true);
                progressBar.setString("❌ 녹음이 취소되었습니다.");
                // 2초 뒤 알림 숨김
                Timer hideTimer = new Timer(2000, evt -> progressBar.setVisible(false));
                hideTimer.setRepeats(false);
                hideTimer.start();
            } else if (currentTranscribeTask != null && !currentTranscribeTask.isDone()) {
                currentTranscribeTask.cancel(true);
                resetUI();
                progressBar.setVisible(true);
                progressBar.setString("❌ 변환이 취소되었습니다.");
                Timer hideTimer = new Timer(2000, evt -> progressBar.setVisible(false));
                hideTimer.setRepeats(false);
                hideTimer.start();
            }
        });

        selectFileBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile().toPath();
                fileLabel.setText(selectedFile.getFileName().toString());
                transcribeBtn.setEnabled(true);
            }
        });

        transcribeBtn.addActionListener(e -> {
            if (selectedFile == null) return;

            transcribeBtn.setEnabled(false);
            selectFileBtn.setEnabled(false);
            startRecordBtn.setEnabled(false);
            settingsBtn.setEnabled(false);
            copyBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
            exportTxtBtn.setEnabled(false);
            exportSrtBtn.setEnabled(false);
            exportDocxBtn.setEnabled(false);
            summarizeBtn.setEnabled(false);
            cancelBtn.setEnabled(true);
            historyList.clearSelection(); 
            
            progressBar.setString("서버에서 변환 중입니다. 잠시만 기다려주세요...");
            progressBar.setVisible(true);
            resultArea.setText("");

            String selectedLang = (String) langComboBox.getSelectedItem();
            String langCode = "ko";
            if (selectedLang.contains("en")) langCode = "en";
            else if (selectedLang.contains("auto")) langCode = "";

            currentTranscribeTask = transcriptionService.transcribeFileAsync(selectedFile, langCode);
            currentTranscribeTask.thenAccept(result -> SwingUtilities.invokeLater(() -> handleSuccess(result)))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> handleError(ex));
                    return null;
                });
        });

        copyBtn.addActionListener(e -> {
            String text = resultArea.getText();
            if (!text.isEmpty()) {
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(text), null);
                JOptionPane.showMessageDialog(this, "복사 완료!", "알림", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        exportTxtBtn.addActionListener(e -> {
            exportFile("txt");
        });
        
        exportSrtBtn.addActionListener(e -> {
            exportFile("srt");
        });
        
        exportDocxBtn.addActionListener(e -> {
            exportFile("docx");
        });

        summarizeBtn.addActionListener(e -> {
            TranscriptResult selected = historyList.getSelectedValue();
            if (selected == null) return;
            
            progressBar.setString("LLM 요약 및 키워드 추출 중입니다...");
            progressBar.setVisible(true);
            summarizeBtn.setEnabled(false);
            
            CompletableFuture<TranscriptResult> future = transcriptionService.summarizeAsync(selected);
            future.thenAccept(result -> SwingUtilities.invokeLater(() -> {
                progressBar.setVisible(false);
                summarizeBtn.setEnabled(true);
                renderResultToTextArea(result);
                historyList.repaint(); // 업데이트된 모델 렌더링
                JOptionPane.showMessageDialog(this, "요약 및 키워드 추출이 완료되었습니다.", "완료", JOptionPane.INFORMATION_MESSAGE);
            })).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    summarizeBtn.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "LLM 통신 오류:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                });
                return null;
            });
        });

        deleteBtn.addActionListener(e -> {
            TranscriptResult selected = historyList.getSelectedValue();
            if (selected == null) return;
            
            int confirm = JOptionPane.showConfirmDialog(this, "선택한 변환 기록을 영구적으로 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    transcriptDao.deleteTranscript(selected.getId());
                    historyListModel.removeElement(selected);
                    resultArea.setText("");
                    copyBtn.setEnabled(false);
                    deleteBtn.setEnabled(false);
                    exportTxtBtn.setEnabled(false);
                    exportSrtBtn.setEnabled(false);
                    exportDocxBtn.setEnabled(false);
                    summarizeBtn.setEnabled(false);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "삭제 실패:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void resetUI() {
        startRecordBtn.setEnabled(true);
        pauseRecordBtn.setEnabled(false);
        stopRecordBtn.setEnabled(false);
        cancelBtn.setEnabled(false);
        selectFileBtn.setEnabled(true);
        if (selectedFile != null) transcribeBtn.setEnabled(true);
        settingsBtn.setEnabled(true);
    }

    private void loadHistoryFromDB() {
        SwingWorker<List<TranscriptResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TranscriptResult> doInBackground() throws Exception {
                return transcriptDao.getAllTranscripts();
            }
            @Override
            protected void done() {
                try {
                    List<TranscriptResult> results = get();
                    historyListModel.clear();
                    for (TranscriptResult tr : results) {
                        historyListModel.addElement(tr);
                    }
                } catch (Exception e) {
                    System.err.println("DB 로드 실패: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleSuccess(TranscriptResult result) {
        historyListModel.add(0, result);
        historyList.setSelectedIndex(0);
        
        progressBar.setVisible(false);
        cancelBtn.setEnabled(false);
        resetUI();
    }
    
    private void renderResultToTextArea(TranscriptResult result) {
        StringBuilder sb = new StringBuilder();
        
        if (result.getSummary() != null && !result.getSummary().isEmpty()) {
            sb.append("▶ 요약 (Summary):\n").append(result.getSummary()).append("\n\n");
        }
        if (result.getKeywords() != null && !result.getKeywords().isEmpty()) {
            sb.append("▶ 키워드:\n").append(result.getKeywords()).append("\n\n");
        }
        
        sb.append("▶ 전체 텍스트:\n").append(result.getRawText()).append("\n\n");
        sb.append("▶ 시간대별 상세:\n");
        for (TextSegment seg : result.getSegments()) {
            String speakerStr = seg.getSpeaker() != null ? " (화자 " + seg.getSpeaker() + ")" : "";
            sb.append(String.format("[%.1fs ~ %.1fs]%s %s\n", seg.getStartSec(), seg.getEndSec(), speakerStr, seg.getText()));
        }
        
        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0);
        copyBtn.setEnabled(true);
    }
    
    private void exportFile(String ext) {
        TranscriptResult selected = historyList.getSelectedValue();
        if (selected == null) return;
        
        JFileChooser fileChooser = new JFileChooser();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").withZone(ZoneId.systemDefault());
        String defaultFileName = dtf.format(selected.getCreatedAt()) + "_" + selected.getId().substring(0, 8) + "." + ext;
        fileChooser.setSelectedFile(new File(defaultFileName));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                if ("txt".equals(ext)) {
                    Files.writeString(file.toPath(), selected.getRawText(), StandardCharsets.UTF_8);
                } else if ("srt".equals(ext)) {
                    exportService.exportToSrt(selected, file.toPath());
                } else if ("docx".equals(ext)) {
                    exportService.exportToDocx(selected, file.toPath());
                }
                JOptionPane.showMessageDialog(this, "파일이 정상적으로 저장되었습니다.", "저장 성공", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "저장 실패:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleError(Throwable ex) {
        if (ex.getCause() instanceof java.util.concurrent.CancellationException) {
            // 취소된 경우는 이미 cancelBtn.addActionListener에서 처리됨
            return;
        }
        progressBar.setVisible(false);
        cancelBtn.setEnabled(false);
        resetUI();
        
        String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        JOptionPane.showMessageDialog(this, "오류:\n" + errorMsg, "에러", JOptionPane.ERROR_MESSAGE);
    }
}
