import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class GenerateHash {
    public static void main(String[] args) {
        try {
            String password = "admin123";
            
            // Generate random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            // Hash password with salt
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            digest.update(password.getBytes("UTF-8"));
            byte[] hashBytes = digest.digest();
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // Encode salt and hash to base64
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hexString.toString().getBytes());
            
            // Return format: salt:hash
            String hashedPassword = saltBase64 + ":" + hashBase64;
            
            // Generate SQL - escape single quotes
            String sql = "INSERT INTO admin (username, password_hash, fullname, email) VALUES " +
                        "('admin', '" + hashedPassword.replace("'", "''") + "', 'System Administrator', 'admin');";
            
            // Write to file
            java.io.PrintWriter writer = new java.io.PrintWriter("admin_insert_hashed.sql", "UTF-8");
            writer.println("-- SQL INSERT Statement with Hashed Password");
            writer.println("-- Username: admin");
            writer.println("-- Password: admin123 (hashed)");
            writer.println("-- Email: admin");
            writer.println("-- Fullname: System Administrator");
            writer.println("-- Hash Algorithm: SHA-256 with salt");
            writer.println();
            writer.println(sql);
            writer.close();
            
            System.out.println("========================================");
            System.out.println("SQL INSERT Statement Generated!");
            System.out.println("========================================");
            System.out.println(sql);
            System.out.println("========================================");
            System.out.println("File saved as: admin_insert_hashed.sql");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}




