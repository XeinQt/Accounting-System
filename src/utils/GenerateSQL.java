package utils;

import java.io.PrintWriter;

public class GenerateSQL {
    public static void main(String[] args) {
        try {
            String password = "admin123";
            String hashedPassword = PasswordUtil.hashPassword(password);
            
            String sql = "INSERT INTO admin (username, password_hash, fullname, email) VALUES " +
                        "('admin', '" + hashedPassword.replace("'", "''") + "', 'System Administrator', 'admin');";
            
            // Write to file in project root
            PrintWriter writer = new PrintWriter("admin_insert_hashed.sql", "UTF-8");
            writer.println("-- SQL INSERT Statement with Hashed Password");
            writer.println("-- Username: admin");
            writer.println("-- Password: admin123 (hashed using SHA-256 with salt)");
            writer.println("-- Email: admin");
            writer.println("-- Fullname: System Administrator");
            writer.println("-- Hash Format: base64(salt):base64(hex_hash)");
            writer.println();
            writer.println(sql);
            writer.close();
            
            System.out.println("SQL generated successfully!");
            System.out.println("File: admin_insert_hashed.sql");
            System.out.println("\nSQL Statement:");
            System.out.println(sql);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}




