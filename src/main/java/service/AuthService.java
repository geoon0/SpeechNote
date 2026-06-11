package service;

import db.DatabaseManager;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * 사용자 인증(회원가입·로그인·비밀번호 변경·회원 탈퇴)을 처리하는 서비스 클래스임.
 * 비밀번호는 SHA-256으로 해시하여 저장함.
 *
 * @author 개발자
 */
public class AuthService {

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public String register(String username, String password) throws SQLException {
        String insertUser = "INSERT INTO users (id, username, password_hash) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertUser)) {
            String id = UUID.randomUUID().toString();
            pstmt.setString(1, id);
            pstmt.setString(2, username);
            pstmt.setString(3, hashPassword(password));
            pstmt.executeUpdate();
            return id;
        }
    }

    public String login(String username, String password) throws SQLException {
        String selectUser = "SELECT id, password_hash FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectUser)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("id");
                    String storedHash = rs.getString("password_hash");
                    if (storedHash.equals(hashPassword(password))) {
                        return id;
                    }
                }
            }
        }
        return null;
    }

    public boolean changePassword(String userId, String oldPassword, String newPassword) throws SQLException {
        String selectUser = "SELECT password_hash FROM users WHERE id = ?";
        String updateUser = "UPDATE users SET password_hash = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection()) {
            // Verify old password
            try (PreparedStatement selectStmt = conn.prepareStatement(selectUser)) {
                selectStmt.setString(1, userId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        if (!storedHash.equals(hashPassword(oldPassword))) {
                            return false; // Old password doesn't match
                        }
                    } else {
                        return false; // User not found
                    }
                }
            }
            
            // Update to new password
            try (PreparedStatement updateStmt = conn.prepareStatement(updateUser)) {
                updateStmt.setString(1, hashPassword(newPassword));
                updateStmt.setString(2, userId);
                updateStmt.executeUpdate();
                return true;
            }
        }
    }

    public void deleteAccount(String userId) throws SQLException {
        String deleteSql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setString(1, userId);
            pstmt.executeUpdate();
        }
    }
}
