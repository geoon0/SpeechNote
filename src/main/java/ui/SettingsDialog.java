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
        
        // '사용 안 함' 옵션을 포함한 이름 배열
        String[] micNames = new String[audioInputs.size() + 1];
        micNames[0] = "선택 안 함 (None)";
        int micSelectedIndex = 0;
        int sysSelectedIndex = 0;
        
        for (int i = 0; i < audioInputs.size(); i++) {
            Mixer.Info info = audioInputs.get(i);
            micNames[i + 1] = info.getName();
            if (selectedMicrophone != null && info.getName().equals(selectedMicrophone.getName())) {
                micSelectedIndex = i + 1;
            }
            if (selectedSystemAudio != null && info.getName().equals(selectedSystemAudio.getName())) {
                sysSelectedIndex = i + 1;
            }
        }
        
        micComboBox = new JComboBox<>(micNames);
        micComboBox.setSelectedIndex(micSelectedIndex);
        audioPanel.add(micComboBox);
        
        audioPanel.add(new JLabel("사용할 시스템 오디오 장치 (Stereo Mix 등):"));
        sysComboBox = new JComboBox<>(micNames);
        sysComboBox.setSelectedIndex(sysSelectedIndex);
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
            selectedSystemAudio = (sysIdx == 0) ? null : audioInputs.get(sysIdx - 1);

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
