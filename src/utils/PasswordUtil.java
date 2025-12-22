package utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password utility class for hashing and verifying passwords.
 * Uses SHA-256 with salt for password hashing.
 * 
 * Format: salt:hash (both base64 encoded)
 */
public class PasswordUtil {
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16; // 16 bytes = 128 bits
    
    /**
     * Hashes a password with a randomly generated salt.
     * 
     * @param password The plain text password to hash
     * @return A string in the format "salt:hash" (both base64 encoded)
     */
    public static String hashPassword(String password) {
        try {
            // Generate random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            
            // Hash password with salt
            String hash = hashWithSalt(password, salt);
            
            // Encode salt and hash to base64
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hash.getBytes());
            
            // Return format: salt:hash
            return saltBase64 + ":" + hashBase64;
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    /**
     * Verifies a password against a stored hash.
     * 
     * @param password The plain text password to verify
     * @param storedHash The stored hash in format "salt:hash"
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            // Handle plain text passwords (for migration)
            if (storedHash == null || storedHash.isEmpty()) {
                return false;
            }
            
            // Check if it's a plain text password (no colon separator)
            if (!storedHash.contains(":")) {
                // Legacy plain text - compare directly for backward compatibility during migration
                return password.equals(storedHash);
            }
            
            // Split salt and hash
            String[] parts = storedHash.split(":", 2);
            if (parts.length != 2) {
                return false;
            }
            
            // Decode salt and hash from base64
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            String storedHashValue = new String(Base64.getDecoder().decode(parts[1]));
            
            // Hash the provided password with the stored salt
            String computedHash = hashWithSalt(password, salt);
            
            // Compare hashes
            return computedHash.equals(storedHashValue);
        } catch (IllegalArgumentException e) {
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Hashes a password with a given salt.
     * 
     * @param password The password to hash
     * @param salt The salt to use
     * @return The hashed password as a hex string
     */
    private static String hashWithSalt(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            
            // Add salt to password
            digest.update(salt);
            digest.update(password.getBytes("UTF-8"));
            
            // Get hash
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
            
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password with salt", e);
        }
    }
}
