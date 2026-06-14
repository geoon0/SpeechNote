import com.formdev.flatlaf.FlatLightLaf;
import ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * SpeechNote 애플리케이션의 진입점 클래스
 * 
 * @author 개발자
 */
public class SpeechNoteApp {
    
    public static void main(String[] args) {
        // UI 룩앤필을 최신 FlatLightLaf 테마로 변경하여 예쁘게 표시
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            // 실패 시 시스템 기본 테마 적용
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Ignore
            }
        }

        // 데이터베이스 생성 및 테이블 초기화
        db.DatabaseManager.initializeDatabase();

        // Swing UI 요소는 Event Dispatch Thread(EDT) 위에서 안전하게 생성하고 렌더링해야 함.
        SwingUtilities.invokeLater(() -> {
            ui.LoginFrame loginFrame = new ui.LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
