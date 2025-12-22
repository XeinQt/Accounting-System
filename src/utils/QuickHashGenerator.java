package utils;

/**
 * Quick utility to generate a single hashed password for SQL insertion.
 * This is a simplified version that doesn't require database connection.
 */
public class QuickHashGenerator {
    public static void main(String[] args) {
        try {
            String password = "admin123";
            String hashedPassword = PasswordUtil.hashPassword(password);
            
            String sql = "INSERT INTO admin (username, password_hash, fullname, email) VALUES " +
                        "('admin', '" + hashedPassword.replace("'", "''") + "', 'System Administrator', 'admin');";
            
            System.out.println("========================================");
            System.out.println("SQL INSERT Statement:");
            System.out.println("========================================");
            System.out.println(sql);
            System.out.println("========================================");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
