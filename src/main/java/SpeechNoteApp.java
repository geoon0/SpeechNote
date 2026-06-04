import ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class SpeechNoteApp {
    
    public static void main(String[] args) {
        // UI 룩앤필을 시스템 기본 테마로 변경하여 좀 더 예쁘게 표시
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 실패 시 기본 자바 룩앤필 사용
        }

        // Swing UI 요소는 Event Dispatch Thread(EDT) 위에서 안전하게 생성하고 렌더링해야 함.
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
