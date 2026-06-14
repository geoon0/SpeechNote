package ui;

import service.AuthService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 로그인 및 회원가입 화면을 제공하는 프레임 클래스임.
 * 인증 성공 시 사용자 정보를 넘겨 MainFrame으로 전환함.
 *
 * @author 개발자
 */
public class LoginFrame extends JFrame {
    private final AuthService authService = new AuthService();
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("SpeechNote - 로그인");
        setSize(350, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("📝 SpeechNote", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 5, 10));
        formPanel.add(new JLabel("아이디:"));
        usernameField = new JTextField();
        formPanel.add(usernameField);

        formPanel.add(new JLabel("비밀번호:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);
        
        mainPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton loginBtn = new JButton("로그인");
        JButton registerBtn = new JButton("회원가입");
        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        loginBtn.addActionListener(e -> {
            String un = usernameField.getText().trim();
            String pw = new String(passwordField.getPassword());
            if (un.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "아이디와 비밀번호를 입력하세요.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                String userId = authService.login(un, pw);
                if (userId != null) {
                    openMainFrame(userId, un);
                } else {
                    JOptionPane.showMessageDialog(this, "아이디 또는 비밀번호가 틀렸습니다.", "실패", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                common.LoggerUtil.logError("로그인 오류", ex);
                JOptionPane.showMessageDialog(this, "로그인 중 오류가 발생했습니다.\n" + common.UserError.friendly(ex), "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerBtn.addActionListener(e -> {
            String un = usernameField.getText().trim();
            String pw = new String(passwordField.getPassword());
            if (un.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "아이디와 비밀번호를 입력하세요.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                String userId = authService.register(un, pw);
                JOptionPane.showMessageDialog(this, "회원가입 성공! 환영합니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                openMainFrame(userId, un);
            } catch (Exception ex) {
                common.LoggerUtil.logError("회원가입 오류", ex);
                JOptionPane.showMessageDialog(this, "회원가입 실패 (중복된 아이디일 수 있습니다)", "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(mainPanel);
    }

    private void openMainFrame(String userId, String username) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(userId, username);
            mainFrame.setVisible(true);
            this.dispose();
        });
    }
}
