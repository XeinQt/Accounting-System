package utils;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility to generate SQL INSERT statement for admin with hashed password.
 * Run this to get the SQL statement you can directly insert into the database.
 */
public class GenerateAdminSQL {
    
    public static void main(String[] args) {
        String username = "admin";
        String password = "admin123";
        String email = "admin";
        String fullname = "System Administrator";
        
        try {
            // Hash the password
            String hashedPassword = PasswordUtil.hashPassword(password);
            
            // Generate SQL INSERT statement
            String sql = String.format(
                "INSERT INTO admin (username, password_hash, fullname, email) VALUES ('%s', '%s', '%s', '%s');",
                username,
                hashedPassword.replace("'", "''"), // Escape single quotes for SQL
                fullname,
                email
            );
            
            // Print to console
            System.out.println("SQL INSERT Statement:");
            System.out.println("====================");
            System.out.println(sql);
            System.out.println();
            System.out.println("Hashed Password (for reference):");
            System.out.println(hashedPassword);
            
            // Also write to file
            try (FileWriter writer = new FileWriter("admin_insert.sql")) {
                writer.write("-- SQL INSERT statement for admin with hashed password\n");
                writer.write("-- Username: " + username + "\n");
                writer.write("-- Password: " + password + " (hashed)\n");
                writer.write("-- Email: " + email + "\n");
                writer.write("-- Fullname: " + fullname + "\n\n");
                writer.write(sql + "\n");
                System.out.println("\nSQL statement also saved to: admin_insert.sql");
            } catch (IOException e) {
                System.err.println("Could not write to file: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error generating SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
