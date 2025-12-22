import java.security.*;
import java.util.Base64;
import java.io.*;

public class QuickHash {
    public static void main(String[] args) throws Exception {
        String password = "admin123";
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        digest.update(password.getBytes("UTF-8"));
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hexString.toString().getBytes());
        String hashedPassword = saltBase64 + ":" + hashBase64;
        String sql = "INSERT INTO admin (username, password_hash, fullname, email) VALUES ('admin', '" + hashedPassword.replace("'", "''") + "', 'System Administrator', 'admin');";
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("admin_insert_hashed.sql"));
            writer.println("-- SQL INSERT with hashed password for admin");
            writer.println("-- Password: admin123");
            writer.println("-- Hash Algorithm: SHA-256 with salt");
            writer.println("");
            writer.println(sql);
            writer.close();
            System.err.println("SUCCESS: File created!");
            System.out.println(sql);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}




