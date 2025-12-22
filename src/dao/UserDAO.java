package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import models.Admin;
import utils.DatabaseUtil;
import utils.PasswordUtil;

public class UserDAO {
    public Admin authenticate(String username, String password) {
        // Get admin by username first, then verify password hash
        String sql = "SELECT * FROM admin WHERE username = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                
                // Verify password using PasswordUtil
                if (PasswordUtil.verifyPassword(password, storedHash)) {
                    Admin admin = new Admin();
                    admin.setAdminId(rs.getInt("admin_id"));
                    admin.setUsername(rs.getString("username"));
                    admin.setPasswordHash(rs.getString("password_hash"));
                    admin.setFullname(rs.getString("fullname"));
                    admin.setEmail(rs.getString("email"));
                    return admin;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public Admin getAdminByUsername(String username) {
        String sql = "SELECT * FROM admin WHERE username = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Admin admin = new Admin();
                admin.setAdminId(rs.getInt("admin_id"));
                admin.setUsername(rs.getString("username"));
                admin.setPasswordHash(rs.getString("password_hash"));
                admin.setFullname(rs.getString("fullname"));
                admin.setEmail(rs.getString("email"));
                return admin;
            }
        } catch (SQLException e) {
            System.err.println("Error getting admin by username: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public Admin getAdminById(int adminId) {
        String sql = "SELECT * FROM admin WHERE admin_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, adminId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Admin admin = new Admin();
                admin.setAdminId(rs.getInt("admin_id"));
                admin.setUsername(rs.getString("username"));
                admin.setPasswordHash(rs.getString("password_hash"));
                admin.setFullname(rs.getString("fullname"));
                admin.setEmail(rs.getString("email"));
                return admin;
            }
        } catch (SQLException e) {
            System.err.println("Error getting admin by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public boolean updateAdmin(Admin admin) {
        String sql = "UPDATE admin SET fullname = ?, email = ?, password_hash = ? WHERE admin_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, admin.getFullname());
            pstmt.setString(2, admin.getEmail());
            
            // Hash password if it's not already hashed (check if it contains colon separator)
            String passwordHash = admin.getPasswordHash();
            if (passwordHash != null && !passwordHash.contains(":")) {
                // It's a plain text password, hash it
                passwordHash = PasswordUtil.hashPassword(passwordHash);
            }
            pstmt.setString(3, passwordHash);
            pstmt.setInt(4, admin.getAdminId());
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating admin: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

