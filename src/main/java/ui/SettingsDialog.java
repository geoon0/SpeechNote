package ui;

import audio.AudioDeviceManager;

import javax.sound.sampled.Mixer;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 사용할 마이크(오디오 채널)를 선택할 수 있는 설정 팝업 창임.
 */
public class SettingsDialog extends JDialog {

    private Mixer.Info selectedMicrophone;
    private JComboBox<String> micComboBox;
    private List<Mixer.Info> microphones;

    public SettingsDialog(JFrame parent, Mixer.Info currentMicrophone) {
        super(parent, "오디오 설정", true); // Modal 창으로 띄움
        this.selectedMicrophone = currentMicrophone;
        
        initUI();
        
        // 콤보박스가 잘리지 않도록 크기 여유있게 조정
        setSize(450, 200);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        
        centerPanel.add(new JLabel("사용할 마이크 선택:"), BorderLayout.NORTH);
        
        // 연결된 마이크 목록 가져오기
        microphones = AudioDeviceManager.getMicrophones();
        String[] micNames = new String[microphones.size()];
        int selectedIndex = 0;
        
        for (int i = 0; i < microphones.size(); i++) {
            Mixer.Info info = microphones.get(i);
            micNames[i] = info.getName();
            // 이전에 선택해둔 마이크가 있다면 해당 인덱스로 기본 선택
            if (selectedMicrophone != null && info.getName().equals(selectedMicrophone.getName())) {
                selectedIndex = i;
            }
        }
        
        micComboBox = new JComboBox<>(micNames);
        if (microphones.size() > 0) {
            micComboBox.setSelectedIndex(selectedIndex);
        } else {
            micComboBox.addItem("사용 가능한 마이크가 없습니다");
            micComboBox.setEnabled(false);
        }
        
        centerPanel.add(micComboBox, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
        
        // 하단 버튼부
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
        
        JButton saveBtn = new JButton("확인");
        JButton cancelBtn = new JButton("취소");
        
        saveBtn.addActionListener(e -> {
            if (microphones.size() > 0) {
                selectedMicrophone = microphones.get(micComboBox.getSelectedIndex());
            }
            dispose();
        });
        
        cancelBtn.addActionListener(e -> dispose());
        
        bottomPanel.add(saveBtn);
        bottomPanel.add(cancelBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 창이 닫힌 후 사용자가 최종적으로 선택한 마이크 정보를 반환함.
     */
    public Mixer.Info getSelectedMicrophone() {
        return selectedMicrophone;
    }
}
