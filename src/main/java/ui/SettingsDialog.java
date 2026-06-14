package ui;

import audio.AudioDeviceManager;
import common.ApiConfig;

import javax.sound.sampled.Mixer;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 사용할 마이크 및 시스템 오디오 장치 선택, API 설정을 할 수 있는 설정 팝업 창임.
  *
 * @author 개발자
 */
public class SettingsDialog extends JDialog {

    private Mixer.Info selectedMicrophone;
    private Mixer.Info selectedSystemAudio;

    private JComboBox<String> micComboBox;
    private JComboBox<String> sysComboBox;
    private List<Mixer.Info> audioInputs;
    private List<Mixer.Info> sysAudioInputs; // 루프백 후보 우선 정렬

    private JTextField urlField;
    private JPasswordField keyField;

    public SettingsDialog(JFrame parent, Mixer.Info currentMicrophone, Mixer.Info currentSystemAudio) {
        super(parent, "환경 설정", true); // Modal 창으로 띄움
        this.selectedMicrophone = currentMicrophone;
        this.selectedSystemAudio = currentSystemAudio;
        
        initUI();
        
        // 창 크기 여유있게 조정
        setSize(500, 350);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        
        // 1. 오디오 장치 선택 영역
        JPanel audioPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        audioPanel.add(new JLabel("사용할 마이크 선택:"));
        
        audioInputs = AudioDeviceManager.getAudioInputs();

        // 시스템 오디오 목록: 루프백 후보(Monitor, Mix 등)를 앞으로 정렬
        sysAudioInputs = new java.util.ArrayList<>();
        List<Mixer.Info> nonLoopback = new java.util.ArrayList<>();
        for (Mixer.Info info : audioInputs) {
            if (AudioDeviceManager.isLikelyLoopback(info)) sysAudioInputs.add(info);
            else nonLoopback.add(info);
        }
        sysAudioInputs.addAll(nonLoopback);

        // 마이크 드롭다운
        String[] micNames = new String[audioInputs.size() + 1];
        micNames[0] = "선택 안 함 (None)";
        int micSelectedIndex = 0;
        for (int i = 0; i < audioInputs.size(); i++) {
            micNames[i + 1] = audioInputs.get(i).getName();
            if (selectedMicrophone != null && audioInputs.get(i).getName().equals(selectedMicrophone.getName())) {
                micSelectedIndex = i + 1;
            }
        }
        micComboBox = new JComboBox<>(micNames);
        micComboBox.setSelectedIndex(micSelectedIndex);
        audioPanel.add(micComboBox);

        // 시스템 오디오 드롭다운: 루프백 후보에 ★ 표시
        String[] sysNames = new String[sysAudioInputs.size() + 1];
        sysNames[0] = "선택 안 함 (None)";
        int sysSelectedIndex = 0;
        for (int i = 0; i < sysAudioInputs.size(); i++) {
            Mixer.Info info = sysAudioInputs.get(i);
            String label = AudioDeviceManager.isLikelyLoopback(info) ? "★ " + info.getName() : info.getName();
            sysNames[i + 1] = label;
            if (selectedSystemAudio != null && info.getName().equals(selectedSystemAudio.getName())) {
                sysSelectedIndex = i + 1;
            }
        }

        audioPanel.add(new JLabel("사용할 시스템 오디오 장치 (★ = 루프백 권장):"));
        sysComboBox = new JComboBox<>(sysNames);
        sysComboBox.setSelectedIndex(sysSelectedIndex);

        // 루프백 장치가 없으면 경고 툴팁 표시
        boolean hasLoopback = sysAudioInputs.stream().anyMatch(AudioDeviceManager::isLikelyLoopback);
        if (!hasLoopback) {
            sysComboBox.setToolTipText("Windows 소리 설정 → 녹음 탭 → 빈 곳 우클릭 → '비활성 장치 표시' → 스테레오 믹스 활성화");
        } else {
            sysComboBox.setToolTipText("★ 표시된 장치가 시스템 소리 캡처에 적합합니다");
        }
        audioPanel.add(sysComboBox);
        
        centerPanel.add(audioPanel);
        
        // 2. API 설정 영역
        JPanel apiPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        apiPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        apiPanel.add(new JLabel("API Base URL:"));
        urlField = new JTextField(ApiConfig.getSttUrl());
        apiPanel.add(urlField);
        
        apiPanel.add(new JLabel("API Key:"));
        keyField = new JPasswordField(ApiConfig.getApiKey());
        apiPanel.add(keyField);
        
        centerPanel.add(apiPanel);
        add(centerPanel, BorderLayout.CENTER);
        
        // 하단 버튼부
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
        
        JButton saveBtn = new JButton("저장");
        JButton cancelBtn = new JButton("취소");
        
        saveBtn.addActionListener(e -> {
            int micIdx = micComboBox.getSelectedIndex();
            selectedMicrophone = (micIdx == 0) ? null : audioInputs.get(micIdx - 1);
            
            int sysIdx = sysComboBox.getSelectedIndex();
            selectedSystemAudio = (sysIdx == 0) ? null : sysAudioInputs.get(sysIdx - 1);

            // API 설정 저장
            String url = urlField.getText().trim();
            String key = new String(keyField.getPassword()).trim();
            ApiConfig.saveConfig(url, key);
            
            dispose();
        });
        
        cancelBtn.addActionListener(e -> dispose());
        
        bottomPanel.add(saveBtn);
        bottomPanel.add(cancelBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public Mixer.Info getSelectedMicrophone() {
        return selectedMicrophone;
    }

    public Mixer.Info getSelectedSystemAudio() {
        return selectedSystemAudio;
    }
}
