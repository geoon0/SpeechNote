package ui;

import audio.AudioDeviceManager;
import audio.AudioRecorder;
import common.TextSegment;
import common.TranscriptResult;
import db.TranscriptDao;
import service.AuthService;
import service.ExportService;
import service.TranscriptionService;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MainFrame 클래스 - Two-Split 메모장 중심 UI 개편 적용 + F-06~F-19 통합
 *
 * @author 개발자
 */
public class MainFrame extends JFrame {
    // Services
    private final TranscriptionService transcriptionService = new TranscriptionService();
    private final AudioRecorder audioRecorder = new AudioRecorder();
    private final TranscriptDao transcriptDao = new TranscriptDao();
    private final ExportService exportService = new ExportService();
    private final AuthService authService = new AuthService();
    
    // User Context
    private final String userId;
    private final String username;

    // State
    private Path selectedFile = null;
    private Mixer.Info currentMicrophone = null;
    private Mixer.Info currentSystemAudio = null;
    private CompletableFuture<TranscriptResult> currentTranscribeTask = null;
    private TranscriptResult currentDisplayedResult = null;
    private List<TranscriptResult> allHistory = new ArrayList<>();
    private SourceDataLine audioLine = null;
    private Thread audioThread = null;
    private volatile boolean audioStopRequested = false;
    
    // Top Bar UI
    private JLabel titleLabel;
    private JButton userMenuBtn;
    private JButton startRecordBtn;
    private JButton pauseRecordBtn;
    private JButton stopRecordBtn;
    private JButton cancelBtn;
    private JLabel timerLabel;
    private JProgressBar levelMeter;
    private JButton selectFileBtn;
    private JLabel fileLabel;
    private JComboBox<String> langComboBox;
    private JButton transcribeBtn;
    private JButton saveMemoBtn;
    private JButton newMemoBtn;
    private JProgressBar progressBar; 
    
    // Left Sidebar UI
    private JTextField searchField;
    private JComboBox<String> sortComboBox;
    private JList<TranscriptResult> historyList;
    private DefaultListModel<TranscriptResult> historyListModel;
    
    // Center Split UI
    private JTextArea sttArea;
    private JTextArea memoArea;
    private JButton editSttBtn;
    private JButton playAudioBtn;
    private JButton stopAudioBtn;
    
    // Right Sidebar UI (AI Assistant)
    private JTextArea summaryArea;
    private JTextArea keywordsArea;
    private JButton summarizeBtn;

    private Timer recordTimer;
    private int recordSeconds = 0;
    private boolean isSttEditMode = false;

    // 메모 자동 저장(Auto-save)
    private Timer autoSaveTimer;
    private boolean suppressMemoAutoSave = false;
    private boolean handlingSelection = false;
    private JLabel saveStatusLabel;

    public MainFrame(String userId, String username) {
        this.userId = userId;
        this.username = username;
        setTitle("SpeechNote Workspace - 자바 기반 음성 인식 메모장");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 800); 
        setLocationRelativeTo(null); 
        
        List<Mixer.Info> inputs = AudioDeviceManager.getAudioInputs();
        if (!inputs.isEmpty()) {
            currentMicrophone = inputs.get(0);
        }
        
        initUI();
        loadHistoryFromDB();

        // 창을 닫기 직전 작성 중이던 메모를 저장
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                flushMemo();
            }
        });
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // 1. 상단 패널 (Top Header)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        
        // 상단 - 타이틀 및 저장/내보내기
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titleLabel = new JLabel("📝 SpeechNote Workspace");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titlePanel.add(titleLabel);
        
        JPanel rightActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        newMemoBtn = new JButton("➕ 새 메모");
        newMemoBtn.setToolTipText("빈 메모를 새로 작성합니다");
        saveMemoBtn = new JButton("💾 메모 저장");
        saveMemoBtn.setToolTipText("현재 메모를 즉시 저장합니다 (입력을 멈추면 자동 저장됩니다)");
        saveStatusLabel = new JLabel(" ");
        saveStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        saveStatusLabel.setForeground(new Color(0, 128, 0));
        userMenuBtn = new JButton("👤 " + username + " ▼");
        userMenuBtn.setToolTipText("환경설정 · 비밀번호 변경 · 로그아웃 · 회원 탈퇴");
        setupUserMenu();
        rightActionsPanel.add(newMemoBtn);
        rightActionsPanel.add(saveMemoBtn);
        rightActionsPanel.add(saveStatusLabel);
        rightActionsPanel.add(Box.createHorizontalStrut(10));
        rightActionsPanel.add(userMenuBtn);
        
        topPanel.add(titlePanel, BorderLayout.WEST);
        topPanel.add(rightActionsPanel, BorderLayout.EAST);
        
        // 상단 - 컨트롤: 역할별 그룹(🎙 녹음 / 📁 파일 변환)으로 분리
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        startRecordBtn = new JButton("🔴 녹음 시작");
        startRecordBtn.setToolTipText("마이크/시스템 오디오 녹음을 시작합니다");
        pauseRecordBtn = new JButton("⏸ 일시정지");
        pauseRecordBtn.setEnabled(false);
        stopRecordBtn = new JButton("⬛ 중지");
        stopRecordBtn.setToolTipText("녹음을 끝내고 곧바로 변환합니다");
        stopRecordBtn.setEnabled(false);
        cancelBtn = new JButton("❌ 취소");
        cancelBtn.setToolTipText("진행 중인 녹음 또는 변환을 취소합니다");
        cancelBtn.setEnabled(false);
        timerLabel = new JLabel("00:00");
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        levelMeter = new JProgressBar(0, 100);
        levelMeter.setStringPainted(true);
        levelMeter.setString("입력 레벨");
        levelMeter.setPreferredSize(new Dimension(110, 22));

        selectFileBtn = new JButton("📁 오디오 업로드");
        selectFileBtn.setToolTipText("변환할 오디오 파일을 선택합니다");
        fileLabel = new JLabel("선택된 파일 없음");
        langComboBox = new JComboBox<>(new String[]{"한국어(ko)", "영어(en)", "자동(auto)"});
        langComboBox.setToolTipText("인식할 언어를 선택합니다");
        transcribeBtn = new JButton("변환 시작 ▶");
        transcribeBtn.setToolTipText("선택한 파일을 텍스트로 변환합니다");
        transcribeBtn.setEnabled(false);
        
        // 🎙 녹음 그룹: 녹음 제어 + 경과 시간 + 입력 레벨 미터
        JPanel recordGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
        recordGroup.setBorder(BorderFactory.createTitledBorder("🎙 녹음"));
        recordGroup.add(startRecordBtn);
        recordGroup.add(pauseRecordBtn);
        recordGroup.add(stopRecordBtn);
        recordGroup.add(cancelBtn);
        recordGroup.add(Box.createHorizontalStrut(8));
        recordGroup.add(new JLabel("⏱"));
        recordGroup.add(timerLabel);
        recordGroup.add(levelMeter);

        // 📁 파일 변환 그룹: 파일 선택 + 언어 + 변환 실행
        JPanel fileGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
        fileGroup.setBorder(BorderFactory.createTitledBorder("📁 파일 변환"));
        fileGroup.add(selectFileBtn);
        fileGroup.add(fileLabel);
        fileGroup.add(new JLabel("  언어"));
        fileGroup.add(langComboBox);
        fileGroup.add(transcribeBtn);

        controlPanel.add(recordGroup);
        controlPanel.add(fileGroup);
        
        topPanel.add(controlPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // 2. 중앙 레이아웃 (좌: 목록, 중: Split 메모장, 우: AI 패널)
        JPanel centerWrapper = new JPanel(new BorderLayout());
        
        // 2-1. 좌측 사이드바 (Left Sidebar)
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(240, 0));
        leftPanel.setBorder(new EmptyBorder(0, 10, 10, 5));
        
        JPanel filterPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new JTextField("🔍 검색어 입력...");
        searchField.setForeground(Color.GRAY);
        addContextMenu(searchField);
        
        sortComboBox = new JComboBox<>(new String[]{"최신순", "제목(메모)순"});
        
        filterPanel.add(searchField, BorderLayout.NORTH);
        filterPanel.add(sortComboBox, BorderLayout.SOUTH);
        leftPanel.add(filterPanel, BorderLayout.NORTH);
        
        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new HistoryCellRenderer());
        
        JScrollPane leftScrollPane = new JScrollPane(historyList);
        leftScrollPane.setBorder(BorderFactory.createTitledBorder("RECENT (최근 항목)"));
        leftPanel.add(leftScrollPane, BorderLayout.CENTER);
        
        // 2-2. 중앙 패널 (Two-Split)
        sttArea = new JTextArea();
        sttArea.setEditable(false);
        sttArea.setLineWrap(true);
        sttArea.setWrapStyleWord(true);
        sttArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sttArea.setMargin(new Insets(10, 10, 10, 10));
        addContextMenu(sttArea);
        JScrollPane sttScroll = new JScrollPane(sttArea);
        
        // STT Header with Edit and Play controls
        JPanel sttHeaderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        playAudioBtn = new JButton("▶ 재생");
        stopAudioBtn = new JButton("⏹ 정지");
        editSttBtn = new JButton("✏️ 텍스트 편집");
        playAudioBtn.setEnabled(false);
        stopAudioBtn.setEnabled(false);
        editSttBtn.setEnabled(false);
        
        sttHeaderPanel.add(playAudioBtn);
        sttHeaderPanel.add(stopAudioBtn);
        sttHeaderPanel.add(editSttBtn);
        
        JPanel sttContainer = new JPanel(new BorderLayout());
        sttContainer.setBorder(BorderFactory.createTitledBorder("🗣️ 실시간 음성 기록"));
        sttContainer.add(sttHeaderPanel, BorderLayout.NORTH);
        sttContainer.add(sttScroll, BorderLayout.CENTER);
        
        memoArea = new JTextArea();
        memoArea.setLineWrap(true);
        memoArea.setWrapStyleWord(true);
        memoArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        memoArea.setMargin(new Insets(10, 10, 10, 10));
        addContextMenu(memoArea);
        JScrollPane memoScroll = new JScrollPane(memoArea);
        memoScroll.setBorder(BorderFactory.createTitledBorder("📝 내 개인 메모"));
        
        JSplitPane twoSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sttContainer, memoScroll);
        twoSplitPane.setDividerLocation(400);
        twoSplitPane.setResizeWeight(0.5);
        
        JPanel centerContentPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
        centerContentPanel.add(progressBar, BorderLayout.NORTH);
        centerContentPanel.add(twoSplitPane, BorderLayout.CENTER);
        
        // 2-3. 우측 사이드바 (AI Assistant)
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setPreferredSize(new Dimension(280, 0));
        rightPanel.setBorder(new EmptyBorder(0, 5, 10, 10));
        
        JPanel aiHeader = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel aiLabel = new JLabel("✨ AI 어시스턴트");
        aiLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        aiHeader.add(aiLabel);
        
        JPanel aiContent = new JPanel();
        aiContent.setLayout(new BoxLayout(aiContent, BoxLayout.Y_AXIS));
        
        summaryArea = new JTextArea(6, 20);
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        addContextMenu(summaryArea);
        JScrollPane sumScroll = new JScrollPane(summaryArea);
        sumScroll.setBorder(BorderFactory.createTitledBorder("실시간 요약"));
        
        keywordsArea = new JTextArea(4, 20);
        keywordsArea.setEditable(false);
        keywordsArea.setLineWrap(true);
        keywordsArea.setWrapStyleWord(true);
        addContextMenu(keywordsArea);
        JScrollPane keyScroll = new JScrollPane(keywordsArea);
        keyScroll.setBorder(BorderFactory.createTitledBorder("핵심 키워드"));
        
        aiContent.add(sumScroll);
        aiContent.add(Box.createVerticalStrut(10));
        aiContent.add(keyScroll);
        
        JPanel aiActions = new JPanel(new GridLayout(0, 1, 5, 5));
        summarizeBtn = new JButton("✨ 새로고침 (요약/키워드 생성)");
        summarizeBtn.setEnabled(false);
        aiActions.add(summarizeBtn);
        
        aiContent.add(Box.createVerticalStrut(10));
        aiContent.add(aiActions);
        
        rightPanel.add(aiHeader, BorderLayout.NORTH);
        rightPanel.add(aiContent, BorderLayout.CENTER);
        
        // 조합
        centerWrapper.add(leftPanel, BorderLayout.WEST);
        centerWrapper.add(centerContentPanel, BorderLayout.CENTER);
        centerWrapper.add(rightPanel, BorderLayout.EAST);
        
        add(centerWrapper, BorderLayout.CENTER);

        setupListeners();
    }

    private void setupUserMenu() {
        JPopupMenu userMenu = new JPopupMenu();
        JMenuItem settingsItem = new JMenuItem("⚙️ 환경 설정");
        JMenuItem pwdItem = new JMenuItem("🔒 비밀번호 변경");
        JMenuItem logoutItem = new JMenuItem("🚪 로그아웃");
        JMenuItem deleteItem = new JMenuItem("🗑️ 회원 탈퇴");

        settingsItem.addActionListener(e -> {
            SettingsDialog dialog = new SettingsDialog(this, currentMicrophone, currentSystemAudio);
            dialog.setVisible(true);
            currentMicrophone = dialog.getSelectedMicrophone();
            currentSystemAudio = dialog.getSelectedSystemAudio();
        });

        pwdItem.addActionListener(e -> changePassword());
        logoutItem.addActionListener(e -> logout());
        deleteItem.addActionListener(e -> deleteAccount());

        userMenu.add(settingsItem);
        userMenu.addSeparator();
        userMenu.add(pwdItem);
        userMenu.add(logoutItem);
        userMenu.addSeparator();
        userMenu.add(deleteItem);

        userMenuBtn.addActionListener(e -> userMenu.show(userMenuBtn, 0, userMenuBtn.getHeight()));
    }

    private void changePassword() {
        JPasswordField oldPwdField = new JPasswordField(10);
        JPasswordField newPwdField = new JPasswordField(10);
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("현재 비밀번호:"));
        panel.add(oldPwdField);
        panel.add(new JLabel("새 비밀번호:"));
        panel.add(newPwdField);

        int result = JOptionPane.showConfirmDialog(this, panel, "비밀번호 변경", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                boolean success = authService.changePassword(userId, new String(oldPwdField.getPassword()), new String(newPwdField.getPassword()));
                if (success) {
                    JOptionPane.showMessageDialog(this, "비밀번호가 성공적으로 변경되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "현재 비밀번호가 일치하지 않습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                common.LoggerUtil.logError("비밀번호 변경 오류", ex);
                JOptionPane.showMessageDialog(this, "오류가 발생했습니다: " + common.UserError.friendly(ex), "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteAccount() {
        int confirm = JOptionPane.showConfirmDialog(this, "정말 회원 탈퇴를 진행하시겠습니까?\n모든 데이터가 삭제되며 복구할 수 없습니다.", "회원 탈퇴", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                authService.deleteAccount(userId);
                JOptionPane.showMessageDialog(this, "탈퇴가 완료되었습니다. 이용해 주셔서 감사합니다.", "탈퇴 완료", JOptionPane.INFORMATION_MESSAGE);
                logout();
            } catch (Exception ex) {
                common.LoggerUtil.logError("회원 탈퇴 오류", ex);
                JOptionPane.showMessageDialog(this, "오류가 발생했습니다: " + common.UserError.friendly(ex), "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void logout() {
        flushMemo();
        stopPlaybackInternal();
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);
        this.dispose();
    }

    private static class HistoryCellRenderer extends DefaultListCellRenderer {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof TranscriptResult) {
                TranscriptResult tr = (TranscriptResult) value;
                String timeStr = formatter.format(tr.getCreatedAt());
                String preview = tr.getMemo() != null && !tr.getMemo().isEmpty() ? tr.getMemo().replace("\n", " ") : 
                                (tr.getRawText() != null ? tr.getRawText().replace("\n", " ") : "내용 없음");
                if (preview.length() > 14) preview = preview.substring(0, 14) + "...";
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
        // 메모 자동 저장: 입력이 멈추고 1.5초 후 저장 (디바운스)
        autoSaveTimer = new Timer(1500, e -> autoPersistMemo());
        autoSaveTimer.setRepeats(false);
        memoArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { scheduleAutoSave(); }
            public void removeUpdate(DocumentEvent e) { scheduleAutoSave(); }
            public void changedUpdate(DocumentEvent e) { scheduleAutoSave(); }
        });

        // Search & Filter Listeners (F-15, F-16)
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (searchField.getText().equals("🔍 검색어 입력...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (searchField.getText().isEmpty()) {
                    searchField.setForeground(Color.GRAY);
                    searchField.setText("🔍 검색어 입력...");
                }
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateHistoryList(); }
            public void removeUpdate(DocumentEvent e) { updateHistoryList(); }
            public void changedUpdate(DocumentEvent e) { updateHistoryList(); }
        });

        sortComboBox.addActionListener(e -> updateHistoryList());

        historyList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || handlingSelection) return;
            handlingSelection = true;
            try {
                // 클릭한 항목을 먼저 확보한 뒤, 이전 메모를 저장하고 새 항목을 렌더링
                TranscriptResult selected = historyList.getSelectedValue();
                flushMemo();
                currentDisplayedResult = selected;
                if (selected != null) {
                    renderResultToUI(selected);
                    enableActions(true);
                } else {
                    enableActions(false);
                }
            } finally {
                handlingSelection = false;
            }
        });
        
        // F-12: Edit STT Text
        editSttBtn.addActionListener(e -> {
            if (currentDisplayedResult == null) return;
            if (!isSttEditMode) {
                isSttEditMode = true;
                sttArea.setEditable(true);
                sttArea.setBackground(new Color(255, 255, 220));
                editSttBtn.setText("💾 수정 완료");
                JOptionPane.showMessageDialog(this, "STT 원문 편집 모드로 전환되었습니다.", "편집 모드", JOptionPane.INFORMATION_MESSAGE);
            } else {
                isSttEditMode = false;
                sttArea.setEditable(false);
                sttArea.setBackground(UIManager.getColor("TextArea.background"));
                editSttBtn.setText("✏️ 텍스트 편집");
                try {
                    String newText = sttArea.getText();
                    transcriptDao.updateRawText(currentDisplayedResult.getId(), newText);
                    // Update current list model item to reflect changes
                    int idx = historyList.getSelectedIndex();
                    TranscriptResult tr = new TranscriptResult(
                            currentDisplayedResult.getId(), currentDisplayedResult.getUserId(),
                            currentDisplayedResult.getSource(), currentDisplayedResult.getLanguage(),
                            currentDisplayedResult.getSegments(), newText, currentDisplayedResult.getCreatedAt()
                    );
                    tr.setMemo(currentDisplayedResult.getMemo());
                    tr.setSummary(currentDisplayedResult.getSummary());
                    tr.setKeywords(currentDisplayedResult.getKeywords());
                    tr.setAudioPath(currentDisplayedResult.getAudioPath());
                    
                    if (idx != -1) {
                        historyListModel.set(idx, tr);
                    }
                    // Update allHistory list
                    for (int i=0; i<allHistory.size(); i++) {
                        if (allHistory.get(i).getId().equals(tr.getId())) {
                            allHistory.set(i, tr);
                            break;
                        }
                    }
                    currentDisplayedResult = tr;
                    JOptionPane.showMessageDialog(this, "수정된 원문이 성공적으로 저장되었습니다.", "저장 완료", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    common.LoggerUtil.logError("원문 저장 오류", ex);
                    JOptionPane.showMessageDialog(this, "오류가 발생했습니다: " + common.UserError.friendly(ex), "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // F-18: Play Audio
        playAudioBtn.addActionListener(e -> {
            if (currentDisplayedResult == null || currentDisplayedResult.getAudioPath() == null) return;
            File audioFile = new File(currentDisplayedResult.getAudioPath());
            if (!audioFile.exists()) {
                JOptionPane.showMessageDialog(this, "오디오 파일을 찾을 수 없습니다: " + audioFile.getAbsolutePath(), "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                stopPlaybackInternal(); // 재생 중이던 것 정지

                AudioInputStream rawAis = AudioSystem.getAudioInputStream(audioFile);
                AudioFormat baseFormat = rawAis.getFormat();
                // 녹음(16kHz)을 그대로 요청하면 일부 장치에서 라인을 못 잡으므로,
                // 보편적으로 지원되는 44.1kHz PCM(little-endian)으로 변환해 SourceDataLine으로 스트리밍 재생한다.
                AudioFormat playFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED, 44100f, 16,
                        baseFormat.getChannels(), baseFormat.getChannels() * 2, 44100f, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, playFormat);

                // 출력 장치(재생 라인)가 아예 없으면 암호 같은 형식 오류 대신 안내 메시지
                if (!AudioSystem.isLineSupported(info)) {
                    rawAis.close();
                    JOptionPane.showMessageDialog(this,
                            "재생할 수 있는 오디오 출력 장치를 찾지 못했습니다.\n" +
                            "스피커·헤드폰이 연결되어 Windows 기본 출력 장치로 설정돼 있는지 확인해 주세요.",
                            "재생 불가", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                AudioInputStream playAis = AudioSystem.getAudioInputStream(playFormat, rawAis);
                audioLine = (SourceDataLine) AudioSystem.getLine(info);
                audioLine.open(playFormat);
                audioLine.start();
                audioStopRequested = false;
                playAudioBtn.setEnabled(false);
                stopAudioBtn.setEnabled(true);

                final SourceDataLine line = audioLine;
                audioThread = new Thread(() -> {
                    byte[] buf = new byte[4096];
                    try {
                        int n;
                        while (!audioStopRequested && (n = playAis.read(buf)) != -1) {
                            line.write(buf, 0, n);
                        }
                        if (!audioStopRequested) line.drain();
                    } catch (Exception ex) {
                        common.LoggerUtil.logError("오디오 재생 스레드 오류", ex);
                    } finally {
                        try { line.stop(); line.close(); } catch (Exception ignored) {}
                        try { playAis.close(); } catch (Exception ignored) {}
                        SwingUtilities.invokeLater(() -> {
                            playAudioBtn.setEnabled(currentDisplayedResult != null && currentDisplayedResult.getAudioPath() != null);
                            stopAudioBtn.setEnabled(false);
                        });
                    }
                }, "audio-playback");
                audioThread.setDaemon(true);
                audioThread.start();
            } catch (Exception ex) {
                common.LoggerUtil.logError("오디오 재생 오류", ex);
                JOptionPane.showMessageDialog(this, "오디오 재생 중 오류:\n" + common.UserError.friendly(ex), "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        stopAudioBtn.addActionListener(e -> {
            stopPlaybackInternal();
            playAudioBtn.setEnabled(currentDisplayedResult != null && currentDisplayedResult.getAudioPath() != null);
            stopAudioBtn.setEnabled(false);
        });

        newMemoBtn.addActionListener(e -> {
            flushMemo();
            historyList.clearSelection();
            currentDisplayedResult = null;
            sttArea.setText("");
            setMemoText("");
            summaryArea.setText("");
            keywordsArea.setText("");
            memoArea.requestFocusInWindow();
            enableActions(false);
        });

        saveMemoBtn.addActionListener(e -> {
            if (autoSaveTimer != null) autoSaveTimer.stop();
            String text = memoArea.getText();
            if (text.trim().isEmpty() && currentDisplayedResult == null) {
                JOptionPane.showMessageDialog(this, "저장할 내용이 없습니다.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (currentDisplayedResult != null) {
                    currentDisplayedResult.setMemo(text);
                    transcriptDao.updateLlmData(currentDisplayedResult);
                    markSaved();
                    JOptionPane.showMessageDialog(this, "메모가 성공적으로 업데이트되었습니다.", "저장 완료", JOptionPane.INFORMATION_MESSAGE);
                    historyList.repaint(); // To update the memo preview in list
                } else {
                    TranscriptResult newMemo = new TranscriptResult(
                            java.util.UUID.randomUUID().toString(), userId,
                            "MEMO", "ko", new java.util.ArrayList<>(), "", java.time.Instant.now()
                    );
                    newMemo.setMemo(text);
                    transcriptDao.saveTranscript(newMemo);
                    currentDisplayedResult = newMemo;
                    allHistory.add(0, newMemo);
                    updateHistoryList();
                    historyList.setSelectedIndex(0);
                    markSaved();
                    JOptionPane.showMessageDialog(this, "새 메모가 성공적으로 저장되었습니다.", "저장 완료", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                common.LoggerUtil.logError("메모 저장 오류", ex);
                JOptionPane.showMessageDialog(this, "메모 저장 실패:\n" + common.UserError.friendly(ex), "오류", JOptionPane.ERROR_MESSAGE);
            }
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
                userMenuBtn.setEnabled(false);
                enableActions(false);
                
                historyList.clearSelection();
                currentDisplayedResult = null;
                sttArea.setText("");
                setMemoText("");
                summaryArea.setText("");
                keywordsArea.setText("");
                progressBar.setString("🔴 마이크 녹음 중입니다...");
                progressBar.setVisible(true);
                
                recordSeconds = 0;
                updateTimerLabel();
                if (recordTimer != null) recordTimer.stop();
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
                common.LoggerUtil.logError("녹음 시작 오류", ex);
                JOptionPane.showMessageDialog(this, "녹음을 시작할 수 없습니다:\n" + common.UserError.friendly(ex), "오류", JOptionPane.ERROR_MESSAGE);
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
                transcribeBtn.doClick(); // 자동 변환
            } else {
                progressBar.setVisible(false);
                selectFileBtn.setEnabled(true);
                userMenuBtn.setEnabled(true);
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
            userMenuBtn.setEnabled(false);
            enableActions(false);
            cancelBtn.setEnabled(true);
            historyList.clearSelection(); 
            
            currentDisplayedResult = null;
            progressBar.setString("서버에서 변환 중입니다. 잠시만 기다려주세요...");
            progressBar.setVisible(true);
            sttArea.setText("");
            setMemoText("");
            summaryArea.setText("");
            keywordsArea.setText("");

            String selectedLang = (String) langComboBox.getSelectedItem();
            String langCode = "ko";
            if (selectedLang.contains("en")) langCode = "en";
            else if (selectedLang.contains("auto")) langCode = "";

            currentTranscribeTask = transcriptionService.transcribeFileAsync(selectedFile, langCode, userId);
            currentTranscribeTask.thenAccept(result -> SwingUtilities.invokeLater(() -> handleSuccess(result, selectedFile)))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> handleError(ex));
                    return null;
                });
        });

        historyList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = historyList.locationToIndex(e.getPoint());
                    if (row != -1) {
                        historyList.setSelectedIndex(row);
                        JPopupMenu popup = new JPopupMenu();
                        
                        JMenuItem copyMenu = new JMenuItem("📋 텍스트 복사");
                        JMenuItem txtMenu = new JMenuItem("💾 TXT 저장 (메모 포함)");
                        JMenuItem srtMenu = new JMenuItem("📁 SRT 자막 추출");
                        JMenuItem docxMenu = new JMenuItem("📄 DOCX 문서 추출");
                        JMenuItem deleteMenu = new JMenuItem("🗑️ 기록 삭제");
                        
                        copyMenu.addActionListener(evt -> copyRecord());
                        txtMenu.addActionListener(evt -> exportFile("txt"));
                        srtMenu.addActionListener(evt -> exportFile("srt"));
                        docxMenu.addActionListener(evt -> exportFile("docx"));
                        deleteMenu.addActionListener(evt -> deleteRecord());
                        
                        popup.add(copyMenu);
                        popup.add(txtMenu);
                        popup.add(srtMenu);
                        popup.add(docxMenu);
                        popup.addSeparator();
                        popup.add(deleteMenu);
                        
                        popup.show(historyList, e.getX(), e.getY());
                    }
                }
            }
        });

        summarizeBtn.addActionListener(e -> {
            TranscriptResult selected = historyList.getSelectedValue();
            if (selected == null) return;
            flushMemo(); // 요약 후 재렌더링 시 미저장 메모가 덮어써지지 않도록 먼저 저장

            progressBar.setString("LLM 요약 및 키워드 추출 중입니다...");
            progressBar.setVisible(true);
            summarizeBtn.setEnabled(false);
            
            CompletableFuture<TranscriptResult> future = transcriptionService.summarizeAsync(selected);
            future.thenAccept(result -> SwingUtilities.invokeLater(() -> {
                progressBar.setVisible(false);
                summarizeBtn.setEnabled(true);
                renderResultToUI(result);
                historyList.repaint();
                JOptionPane.showMessageDialog(this, "요약 및 키워드 추출이 완료되었습니다.", "완료", JOptionPane.INFORMATION_MESSAGE);
            })).exceptionally(ex -> {
                common.LoggerUtil.logError("LLM 요약/키워드 처리 오류", ex);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    summarizeBtn.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "AI 요약·키워드 생성에 실패했습니다.\n\n" + common.UserError.friendly(ex), "오류", JOptionPane.ERROR_MESSAGE);
                });
                return null;
            });
        });
    }

    /** 메모 입력이 감지되면 자동 저장 타이머를 재시작함 (디바운스). */
    private void scheduleAutoSave() {
        if (suppressMemoAutoSave) return;
        saveStatusLabel.setForeground(Color.GRAY);
        saveStatusLabel.setText("입력 중...");
        if (autoSaveTimer != null) autoSaveTimer.restart();
    }

    /** 대기 중인 자동 저장을 즉시 수행함 (기록 전환·녹음 시작·창 닫기 등 이탈 직전 호출). */
    private void flushMemo() {
        if (autoSaveTimer != null) autoSaveTimer.stop();
        autoPersistMemo();
    }

    /**
     * 현재 메모 영역의 내용을 DB에 저장함. 팝업 없이 조용히 동작함.
     * - 기존 기록이면 메모를 갱신하고, 새 메모면 레코드를 생성함.
     * - 입력 중 커서/포커스가 튀지 않도록 목록 선택은 변경하지 않음.
     */
    private void autoPersistMemo() {
        if (suppressMemoAutoSave) return;
        String text = memoArea.getText();
        if (text.trim().isEmpty()) {
            // 내용이 비면, 부가 데이터 없는 순수 메모는 자동 삭제
            if (currentDisplayedResult != null && isDeletableEmptyMemo(currentDisplayedResult)) {
                deleteEmptyMemo(currentDisplayedResult);
            }
            return; // 저장할 내용 없음
        }
        try {
            if (currentDisplayedResult != null) {
                String existing = currentDisplayedResult.getMemo() != null ? currentDisplayedResult.getMemo() : "";
                if (text.equals(existing)) return; // 변경 없음 → 불필요한 쓰기 방지
                currentDisplayedResult.setMemo(text);
                transcriptDao.updateLlmData(currentDisplayedResult);
                historyList.repaint();
            } else {
                TranscriptResult newMemo = new TranscriptResult(
                        java.util.UUID.randomUUID().toString(), userId,
                        "MEMO", "ko", new java.util.ArrayList<>(), "", java.time.Instant.now());
                newMemo.setMemo(text);
                transcriptDao.saveTranscript(newMemo);
                currentDisplayedResult = newMemo;
                allHistory.add(0, newMemo);
                handlingSelection = true; // 목록 재구성이 선택 리스너를 건드리지 않도록 보호
                try {
                    updateHistoryList();
                } finally {
                    handlingSelection = false;
                }
            }
            markSaved();
        } catch (Exception ex) {
            common.LoggerUtil.logError("메모 자동 저장 실패", ex);
            saveStatusLabel.setForeground(Color.RED);
            saveStatusLabel.setText("⚠ 자동 저장 실패");
        }
    }

    /** 부가 데이터(STT 원문·요약·키워드)가 없는 순수 메모인지 판별함. */
    private boolean isDeletableEmptyMemo(TranscriptResult r) {
        return "MEMO".equals(r.getSource())
                && isBlank(r.getRawText())
                && isBlank(r.getSummary())
                && isBlank(r.getKeywords());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 내용이 빈 순수 메모 레코드를 DB와 목록에서 제거함. */
    private void deleteEmptyMemo(TranscriptResult r) {
        try {
            transcriptDao.deleteTranscript(r.getId());
            allHistory.removeIf(t -> t.getId().equals(r.getId()));
            currentDisplayedResult = null;
            handlingSelection = true; // 목록 재구성이 선택 리스너를 건드리지 않도록 보호
            try {
                updateHistoryList();
            } finally {
                handlingSelection = false;
            }
            enableActions(false);
            saveStatusLabel.setText(" ");
        } catch (Exception ex) {
            common.LoggerUtil.logError("빈 메모 자동 삭제 실패", ex);
        }
    }

    /** 저장 완료 상태를 상단에 표시함. */
    private void markSaved() {
        String t = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                .format(java.time.LocalTime.now());
        saveStatusLabel.setForeground(new Color(0, 128, 0));
        saveStatusLabel.setText("✓ 자동 저장됨 " + t);
    }

    /** 메모 영역을 프로그램적으로 설정함 (자동 저장 트리거 없이). */
    private void setMemoText(String text) {
        suppressMemoAutoSave = true;
        memoArea.setText(text);
        memoArea.setCaretPosition(0);
        suppressMemoAutoSave = false;
        if (autoSaveTimer != null) autoSaveTimer.stop();
        saveStatusLabel.setText(" ");
    }

    private void updateHistoryList() {
        String filter = searchField.getText();
        if (filter.equals("🔍 검색어 입력...")) filter = "";
        filter = filter.toLowerCase();

        boolean isSortByTitle = sortComboBox.getSelectedIndex() == 1;

        List<TranscriptResult> filtered = new ArrayList<>();
        for (TranscriptResult tr : allHistory) {
            boolean match = false;
            if (filter.isEmpty()) {
                match = true;
            } else {
                if (tr.getMemo() != null && tr.getMemo().toLowerCase().contains(filter)) match = true;
                if (tr.getRawText() != null && tr.getRawText().toLowerCase().contains(filter)) match = true;
                if (tr.getSummary() != null && tr.getSummary().toLowerCase().contains(filter)) match = true;
                if (tr.getKeywords() != null && tr.getKeywords().toLowerCase().contains(filter)) match = true;
            }
            if (match) filtered.add(tr);
        }

        if (isSortByTitle) {
            filtered.sort((o1, o2) -> {
                String t1 = o1.getMemo() != null ? o1.getMemo() : "";
                String t2 = o2.getMemo() != null ? o2.getMemo() : "";
                return t1.compareTo(t2);
            });
        } else {
            // Latest first
            filtered.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        }

        historyListModel.clear();
        for (TranscriptResult tr : filtered) {
            historyListModel.addElement(tr);
        }
    }

    private void enableActions(boolean enable) {
        summarizeBtn.setEnabled(enable);
        editSttBtn.setEnabled(enable && currentDisplayedResult != null && !currentDisplayedResult.getSource().equals("MEMO"));
        
        if (enable && currentDisplayedResult != null && currentDisplayedResult.getAudioPath() != null) {
            playAudioBtn.setEnabled(true);
        } else {
            playAudioBtn.setEnabled(false);
            stopAudioBtn.setEnabled(false);
        }
    }

    private void resetUI() {
        startRecordBtn.setEnabled(true);
        pauseRecordBtn.setEnabled(false);
        stopRecordBtn.setEnabled(false);
        cancelBtn.setEnabled(false);
        selectFileBtn.setEnabled(true);
        if (selectedFile != null) transcribeBtn.setEnabled(true);
        userMenuBtn.setEnabled(true);
    }

    private void loadHistoryFromDB() {
        SwingWorker<List<TranscriptResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TranscriptResult> doInBackground() throws Exception {
                return transcriptDao.getAllTranscripts(userId);
            }
            @Override
            protected void done() {
                try {
                    allHistory = get();
                    updateHistoryList();
                } catch (Exception e) {
                    common.LoggerUtil.logError("기록 DB 로드 실패", e);
                }
            }
        };
        worker.execute();
    }

    private void handleSuccess(TranscriptResult result, Path audioFile) {
        // Need to create a new TranscriptResult that includes userId and audioPath
        TranscriptResult finalResult = new TranscriptResult(
                result.getId(), userId, result.getSource(), result.getLanguage(),
                result.getSegments(), result.getRawText(), result.getCreatedAt()
        );
        finalResult.setAudioPath(audioFile.toAbsolutePath().toString());
        
        try {
            transcriptDao.saveTranscript(finalResult);
            allHistory.add(0, finalResult);
            updateHistoryList();
            historyList.setSelectedIndex(0);
            
            progressBar.setVisible(false);
            resetUI();
            
            int confirm = JOptionPane.showConfirmDialog(this, 
                "변환이 완료되었습니다. 지금 바로 AI 요약 및 키워드를 생성하시겠습니까?", 
                "AI 요약 실행", JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                summarizeBtn.doClick();
            }
        } catch (Exception ex) {
            common.LoggerUtil.logError("변환 결과 DB 저장 오류", ex);
            handleError(ex);
        }
    }

    /** 현재 재생 중인 오디오를 즉시 정지하고 라인을 닫음. */
    private void stopPlaybackInternal() {
        audioStopRequested = true;
        SourceDataLine line = audioLine;
        if (line != null) {
            try { line.stop(); line.flush(); line.close(); } catch (Exception ignored) {}
        }
        audioLine = null;
        audioThread = null;
    }

    private void renderResultToUI(TranscriptResult result) {
        stopPlaybackInternal();
        stopAudioBtn.setEnabled(false);

        currentDisplayedResult = result;
        
        // 좌측: 음성 기록 세그먼트 파싱
        StringBuilder sb = new StringBuilder();
        for (TextSegment seg : result.getSegments()) {
            String speakerStr = seg.getSpeaker() != null ? " [화자 " + seg.getSpeaker() + "]" : "";
            sb.append(String.format("%s %.1fs ~ %.1fs\n%s\n\n", speakerStr, seg.getStartSec(), seg.getEndSec(), seg.getText()));
        }
        if (sb.length() == 0 && result.getRawText() != null) {
             sb.append(result.getRawText()); // 세그먼트가 없으면 전체 원문 출력
        }
        
        isSttEditMode = false;
        sttArea.setEditable(false);
        sttArea.setBackground(UIManager.getColor("TextArea.background"));
        editSttBtn.setText("✏️ 텍스트 편집");
        
        sttArea.setText(sb.toString().trim());
        sttArea.setCaretPosition(0);
        
        // 중앙 우측: 내 개인 메모 (불러오기는 자동 저장을 트리거하지 않도록 setMemoText 사용)
        setMemoText(result.getMemo() != null ? result.getMemo() : "");
        
        // 우측 AI 사이드바: 요약 및 키워드
        summaryArea.setText(result.getSummary() != null ? result.getSummary() : "아직 요약이 없습니다. 새로고침을 눌러주세요.");
        summaryArea.setCaretPosition(0);
        
        keywordsArea.setText(result.getKeywords() != null ? result.getKeywords() : "아직 키워드가 없습니다.");
        keywordsArea.setCaretPosition(0);
    }
    
    private void exportFile(String ext) {
        try {
            TranscriptResult selected = historyList.getSelectedValue();
            if (selected == null) return;
            
            JFileChooser fileChooser = new JFileChooser();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").withZone(ZoneId.systemDefault());
            String defaultFileName = dtf.format(selected.getCreatedAt()) + "_" + selected.getId().substring(0, 8) + "." + ext;
            fileChooser.setSelectedFile(new File(defaultFileName));
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if ("txt".equals(ext)) {
                    // txt 내보내기 시 메모도 포함
                    String exportContent = "▶ STT 음성 기록\n" + selected.getRawText() + "\n\n" +
                                           "▶ 내 개인 메모\n" + (selected.getMemo() != null ? selected.getMemo() : "") + "\n\n" +
                                           "▶ AI 요약\n" + (selected.getSummary() != null ? selected.getSummary() : "");
                    Files.writeString(file.toPath(), exportContent, StandardCharsets.UTF_8);
                } else if ("srt".equals(ext)) {
                    exportService.exportToSrt(selected, file.toPath());
                } else if ("docx".equals(ext)) {
                    exportService.exportToDocx(selected, file.toPath());
                }
                JOptionPane.showMessageDialog(this, "파일이 정상적으로 저장되었습니다.", "저장 성공", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            common.LoggerUtil.logError("파일 저장 중 오류 발생", ex);
            JOptionPane.showMessageDialog(this, "파일 저장 중 오류가 발생했습니다:\n" + common.UserError.friendly(ex), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleError(Throwable ex) {
        if (ex.getCause() instanceof java.util.concurrent.CancellationException) {
            return;
        }
        common.LoggerUtil.logError("처리 중 오류 발생", ex);
        progressBar.setVisible(false);
        cancelBtn.setEnabled(false);
        resetUI();
        
        JOptionPane.showMessageDialog(this, "처리 중 오류가 발생했습니다.\n\n" + common.UserError.friendly(ex), "오류", JOptionPane.ERROR_MESSAGE);
    }
    
    private void addContextMenu(javax.swing.text.JTextComponent component) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("복사(Copy)");
        JMenuItem cutItem = new JMenuItem("잘라내기(Cut)");
        JMenuItem pasteItem = new JMenuItem("붙여넣기(Paste)");
        JMenuItem selectAllItem = new JMenuItem("전체 선택(Select All)");
        
        copyItem.addActionListener(e -> component.copy());
        cutItem.addActionListener(e -> component.cut());
        pasteItem.addActionListener(e -> component.paste());
        selectAllItem.addActionListener(e -> component.selectAll());
        
        popup.add(copyItem);
        popup.add(cutItem);
        popup.add(pasteItem);
        popup.addSeparator();
        popup.add(selectAllItem);
        
        component.setComponentPopupMenu(popup);
    }

    private void copyRecord() {
        String text = sttArea.getText();
        if (!text.isEmpty()) {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(text), null);
            JOptionPane.showMessageDialog(this, "음성 기록 복사 완료!", "알림", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void deleteRecord() {
        TranscriptResult selected = historyList.getSelectedValue();
        if (selected == null) return;
        
        int confirm = JOptionPane.showConfirmDialog(this, "선택한 변환 기록을 영구적으로 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                transcriptDao.deleteTranscript(selected.getId());
                allHistory.removeIf(t -> t.getId().equals(selected.getId()));
                currentDisplayedResult = null;
                sttArea.setText("");
                setMemoText("");
                summaryArea.setText("");
                keywordsArea.setText("");
                updateHistoryList();
                enableActions(false);
            } catch (Exception ex) {
                common.LoggerUtil.logError("기록 삭제 오류", ex);
                JOptionPane.showMessageDialog(this, "삭제에 실패했습니다:\n" + common.UserError.friendly(ex), "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
