package ui;

import audio.AudioDeviceManager;
import audio.AudioRecorder;
import common.TextSegment;
import common.TranscriptResult;
import db.TranscriptDao;
import service.TranscriptionService;

import javax.sound.sampled.Mixer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainFrame extends JFrame {

    private final TranscriptionService transcriptionService = new TranscriptionService();
    private final AudioRecorder audioRecorder = new AudioRecorder();
    private final TranscriptDao transcriptDao = new TranscriptDao();
    
    private Path selectedFile = null;
    private Mixer.Info currentMicrophone = null;

    private JLabel fileLabel;
    private JButton selectFileBtn;
    private JButton transcribeBtn;
    
    private JButton settingsBtn;
    private JButton startRecordBtn;
    private JButton stopRecordBtn;
    
    private JProgressBar progressBar;
    private JTextArea resultArea;
    private JButton copyBtn;
    private JButton deleteBtn; // 신규 추가: 삭제 버튼
    
    // 신규 추가: 좌측 과거 기록 목록 패널용 변수들
    private JList<TranscriptResult> historyList;
    private DefaultListModel<TranscriptResult> historyListModel;

    public MainFrame() {
        setTitle("SpeechNote - STT 변환기");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 좌측 패널이 들어갈 공간 확보를 위해 넓이를 900으로 확장
        setSize(900, 600); 
        setLocationRelativeTo(null); 
        
        List<Mixer.Info> mics = AudioDeviceManager.getMicrophones();
        if (!mics.isEmpty()) {
            currentMicrophone = mics.get(0);
        }
        
        initUI();
        
        // 창이 뜰 때 DB에서 과거 기록을 싹 긁어옴
        loadHistoryFromDB();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // 1. 상단 패널 (조작 버튼들)
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
        topPanel.add(new JLabel("  |  "));
        topPanel.add(selectFileBtn);
        topPanel.add(fileLabel);
        topPanel.add(transcribeBtn);

        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(topPanel, BorderLayout.NORTH);

        // 2. 중앙 좌우 분할 패널 (JSplitPane)
        
        // 2-1. 좌측 패널 (과거 기록 목록)
        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 보기 좋은 모양을 위해 직접 만든 커스텀 디자인(Renderer) 적용
        historyList.setCellRenderer(new HistoryCellRenderer());
        
        JScrollPane leftScrollPane = new JScrollPane(historyList);
        leftScrollPane.setPreferredSize(new Dimension(280, 0));
        leftScrollPane.setBorder(BorderFactory.createTitledBorder("과거 기록 목록"));

        // 2-2. 우측 패널 (로딩 바 + 결과 출력창)
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

        // 좌우 패널을 합체
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScrollPane, rightPanel);
        splitPane.setDividerLocation(280); 
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(splitPane, BorderLayout.CENTER);

        // 3. 하단 패널 (기타 액션)
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        deleteBtn = new JButton("🗑️ 기록 삭제");
        deleteBtn.setEnabled(false);
        bottomPanel.add(deleteBtn);
        
        copyBtn = new JButton("텍스트 복사");
        copyBtn.setEnabled(false);
        bottomPanel.add(copyBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        setupListeners();
    }

    /**
     * 좌측 JList의 각 항목들을 예쁘게 꾸며주는 커스텀 렌더러
     * - 날짜와 내용을 두 줄로 예쁘게 출력
     */
    private static class HistoryCellRenderer extends DefaultListCellRenderer {
        // 날짜를 보기 편하게(예: 06-04 15:30) 바꿔주는 포맷터
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof TranscriptResult) {
                TranscriptResult tr = (TranscriptResult) value;
                String timeStr = formatter.format(tr.getCreatedAt());
                
                // 첫 18글자만 살짝 보여주는 미리보기 만들기
                String preview = tr.getRawText().replace("\n", " ");
                if (preview.length() > 18) {
                    preview = preview.substring(0, 18) + "...";
                }
                
                String icon = "FILE".equals(tr.getSource()) ? "📁 " : "🎤 ";
                setText("<html><b style='font-size:11px'>" + icon + timeStr + "</b><br><span style='color:gray; font-size:10px'>" + preview + "</span></html>");
                setBorder(new EmptyBorder(8, 8, 8, 8)); // 위아래 여백을 넉넉히
            }
            return this;
        }
    }

    private void setupListeners() {
        // [과거 기록 목록] 클릭(선택)했을 때의 이벤트
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                TranscriptResult selected = historyList.getSelectedValue();
                if (selected != null) {
                    renderResultToTextArea(selected);
                    deleteBtn.setEnabled(true);
                } else {
                    deleteBtn.setEnabled(false);
                }
            }
        });

        settingsBtn.addActionListener(e -> {
            SettingsDialog dialog = new SettingsDialog(this, currentMicrophone);
            dialog.setVisible(true);
            currentMicrophone = dialog.getSelectedMicrophone();
        });

        startRecordBtn.addActionListener(e -> {
            if (currentMicrophone == null) {
                JOptionPane.showMessageDialog(this, "마이크가 선택되지 않았습니다.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                audioRecorder.start(currentMicrophone);
                
                startRecordBtn.setEnabled(false);
                stopRecordBtn.setEnabled(true);
                selectFileBtn.setEnabled(false);
                transcribeBtn.setEnabled(false);
                settingsBtn.setEnabled(false);
                copyBtn.setEnabled(false);
                deleteBtn.setEnabled(false);
                
                // 새로운 변환을 시작하므로 기존 선택 해제
                historyList.clearSelection();
                
                resultArea.setText("");
                progressBar.setString("🔴 마이크 녹음 중입니다...");
                progressBar.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "오류:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        stopRecordBtn.addActionListener(e -> {
            Path recordedFile = audioRecorder.stop();
            startRecordBtn.setEnabled(true);
            stopRecordBtn.setEnabled(false);
            
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
            historyList.clearSelection(); // 로딩 중에는 목록 선택 방지
            
            progressBar.setString("서버에서 변환 중입니다. 잠시만 기다려주세요...");
            progressBar.setVisible(true);
            resultArea.setText("");

            transcriptionService.transcribeFileAsync(selectedFile, "ko")
                .thenAccept(result -> SwingUtilities.invokeLater(() -> handleSuccess(result)))
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

        // [기록 삭제] 버튼
        deleteBtn.addActionListener(e -> {
            TranscriptResult selected = historyList.getSelectedValue();
            if (selected == null) return;
            
            int confirm = JOptionPane.showConfirmDialog(this, "선택한 변환 기록을 영구적으로 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    transcriptDao.deleteTranscript(selected.getId());
                    historyListModel.removeElement(selected); // 리스트에서 제거
                    resultArea.setText(""); // 텍스트 영역 비우기
                    copyBtn.setEnabled(false);
                    deleteBtn.setEnabled(false);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "삭제 실패:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
    
    /**
     * 프로그램이 커질 때 무거운 DB 로드 작업을 백그라운드 스레드에서 처리합니다.
     */
    private void loadHistoryFromDB() {
        SwingWorker<List<TranscriptResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TranscriptResult> doInBackground() throws Exception {
                // Dao를 통해 모든 과거 기록 불러오기 (최신순 정렬)
                return transcriptDao.getAllTranscripts();
            }

            @Override
            protected void done() {
                try {
                    List<TranscriptResult> results = get();
                    historyListModel.clear();
                    for (TranscriptResult tr : results) {
                        historyListModel.addElement(tr); // JList에 데이터 심기
                    }
                } catch (Exception e) {
                    System.err.println("DB 로드 실패: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleSuccess(TranscriptResult result) {
        // 방금 변환이 끝나서 DB에 저장된 따끈따끈한 결과를, 좌측 리스트 맨 위(0번째)에 꽂아 넣습니다.
        historyListModel.add(0, result);
        historyList.setSelectedIndex(0); // 새로 추가된 걸 자동으로 선택해서 우측에 띄워줌!
        
        progressBar.setVisible(false);
        transcribeBtn.setEnabled(true);
        selectFileBtn.setEnabled(true);
        startRecordBtn.setEnabled(true);
        settingsBtn.setEnabled(true);
        copyBtn.setEnabled(true);
    }
    
    /**
     * TranscriptResult 객체를 분해해서 우측 텍스트 창에 예쁘게 그려주는 공통 로직
     */
    private void renderResultToTextArea(TranscriptResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("▶ 전체 텍스트:\n").append(result.getRawText()).append("\n\n");
        sb.append("▶ 시간대별 상세:\n");
        for (TextSegment seg : result.getSegments()) {
            String speakerStr = seg.getSpeaker() != null ? " (화자 " + seg.getSpeaker() + ")" : "";
            sb.append(String.format("[%.1fs ~ %.1fs]%s %s\n", seg.getStartSec(), seg.getEndSec(), speakerStr, seg.getText()));
        }
        
        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0); // 너무 길면 위에서부터 볼 수 있게 스크롤을 맨 위로 올림
        copyBtn.setEnabled(true);
    }

    private void handleError(Throwable ex) {
        progressBar.setVisible(false);
        transcribeBtn.setEnabled(true);
        selectFileBtn.setEnabled(true);
        startRecordBtn.setEnabled(true);
        settingsBtn.setEnabled(true);
        
        String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        JOptionPane.showMessageDialog(this, "오류:\n" + errorMsg, "에러", JOptionPane.ERROR_MESSAGE);
    }
}
