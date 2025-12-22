package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utility class to migrate existing plain text passwords to hashed passwords.
 * Run this once to hash all existing passwords in the database.
 */
public class PasswordMigration {
    
    /**
     * Migrates all plain text passwords in the admin table to hashed passwords.
     * Only hashes passwords that are not already hashed (don't contain ':' separator).
     */
    public static void migratePasswords() {
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Get all admins
            String selectSql = "SELECT admin_id, username, password_hash FROM admin";
            
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                 ResultSet rs = selectStmt.executeQuery()) {
                
                int migratedCount = 0;
                
                while (rs.next()) {
                    int adminId = rs.getInt("admin_id");
                    String username = rs.getString("username");
                    String passwordHash = rs.getString("password_hash");
                    
                    // Check if password is already hashed (contains ':' separator)
                    if (passwordHash != null && !passwordHash.contains(":")) {
                        // It's a plain text password, hash it
                        String hashedPassword = PasswordUtil.hashPassword(passwordHash);
                        
                        // Update in database
                        String updateSql = "UPDATE admin SET password_hash = ? WHERE admin_id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, hashedPassword);
                            updateStmt.setInt(2, adminId);
                            updateStmt.executeUpdate();
                            
                            System.out.println("Migrated password for admin: " + username);
                            migratedCount++;
                        }
                    } else {
                        System.out.println("Password already hashed for admin: " + username);
                    }
                }
                
                System.out.println("Password migration completed. Migrated " + migratedCount + " passwords.");
            }
        } catch (SQLException e) {
            System.err.println("Error migrating passwords: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Main method to run password migration.
     * Can be called from application startup or run separately.
     */
    public static void main(String[] args) {
        System.out.println("Starting password migration...");
        migratePasswords();
        System.out.println("Password migration finished.");
    }
}
