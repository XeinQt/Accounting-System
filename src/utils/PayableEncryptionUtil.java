package utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for encrypting and decrypting payable amounts.
 * Uses AES encryption to protect financial data in the database.
 * 
 * Format: encrypted_value (base64 encoded)
 */
public class PayableEncryptionUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int KEY_SIZE = 256; // AES-256
    
    // Secret key - in production, this should be stored securely (e.g., in a config file or environment variable)
    // For now, using a fixed key derived from a secret phrase
    private static final String SECRET_PHRASE = "DorPayAccountingSystem2024SecureKey";
    private static SecretKey secretKey;
    
    static {
        try {
            // Generate or derive secret key from phrase
            secretKey = generateKeyFromPhrase(SECRET_PHRASE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption key", e);
        }
    }
    
    /**
     * Generate a secret key from a phrase using SHA-256
     */
    private static SecretKey generateKeyFromPhrase(String phrase) throws Exception {
        java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(phrase.getBytes(StandardCharsets.UTF_8));
        // Use first 32 bytes for AES-256
        return new SecretKeySpec(key, ALGORITHM);
    }
    
    /**
     * Encrypt a payable amount
     * 
     * @param amount The amount to encrypt
     * @param studentId Optional student ID for additional security (can be used to derive per-student keys)
     * @return Encrypted amount as base64 string, or null if encryption fails
     */
    public static String encryptAmount(double amount, Integer studentId) {
        try {
            String amountStr = String.format("%.2f", amount);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(amountStr.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            System.err.println("Error encrypting amount: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Encrypt a payable amount (without student ID)
     */
    public static String encryptAmount(double amount) {
        return encryptAmount(amount, null);
    }
    
    /**
     * Decrypt a payable amount
     * 
     * @param encryptedAmount The encrypted amount (base64 string)
     * @param studentId Optional student ID (for future per-student key support)
     * @return Decrypted amount as double, or 0.0 if decryption fails
     */
    public static double decryptAmount(String encryptedAmount, Integer studentId) {
        if (encryptedAmount == null || encryptedAmount.trim().isEmpty()) {
            return 0.0;
        }
        
        try {
            // Check if it's already a plain number (for backward compatibility during migration)
            try {
                return Double.parseDouble(encryptedAmount);
            } catch (NumberFormatException e) {
                // Not a plain number, proceed with decryption
            }
            
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedAmount);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String decryptedStr = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            return Double.parseDouble(decryptedStr);
        } catch (Exception e) {
            System.err.println("Error decrypting amount: " + e.getMessage());
            // Return 0.0 on error - this allows the system to continue functioning
            return 0.0;
        }
    }
    
    /**
     * Decrypt a payable amount (without student ID)
     */
    public static double decryptAmount(String encryptedAmount) {
        return decryptAmount(encryptedAmount, null);
    }
    
    /**
     * Check if a string is encrypted (base64 format)
     */
    public static boolean isEncrypted(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        // Try to parse as double - if it fails, it's likely encrypted
        try {
            Double.parseDouble(value);
            return false; // It's a plain number
        } catch (NumberFormatException e) {
            // Check if it's valid base64
            try {
                Base64.getDecoder().decode(value);
                return true; // It's base64, likely encrypted
            } catch (IllegalArgumentException ex) {
                return false; // Not valid base64
            }
        }
    }
}

