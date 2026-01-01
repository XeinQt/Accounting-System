package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Migration utility to encrypt existing payable amounts in the database.
 * Run this once to encrypt all existing payable data.
 */
public class PayableEncryptionMigration {
    
    /**
     * Migrate all existing payable amounts to encrypted format in place
     * This converts DECIMAL columns to VARCHAR and encrypts the values directly
     */
    public static void migratePayableAmounts() {
        System.out.println("Starting payable amounts encryption migration...");
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Step 1: Check if columns need type conversion (DECIMAL -> VARCHAR)
                boolean needsTypeConversion = checkIfNeedsTypeConversion(conn);
                
                if (needsTypeConversion) {
                    System.out.println("Converting DECIMAL columns to VARCHAR...");
                    convertColumnsToVarchar(conn);
                }
                
                // Step 2: Remove encrypted_* columns if they exist (we're encrypting in place now)
                removeEncryptedColumns(conn);
                
                // Step 3: Encrypt existing plain values in place
                String selectSql = "SELECT payable_id, belong_id, downpayment_amount, amount_paid, remaining_balance " +
                                  "FROM student_payables";
                
                int migrated = 0;
                int skipped = 0;
                
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                     ResultSet rs = selectStmt.executeQuery()) {
                    
                    while (rs.next()) {
                        int payableId = rs.getInt("payable_id");
                        int belongId = rs.getInt("belong_id");
                        
                        // Get values - try as string first (if already VARCHAR), then as double (if DECIMAL)
                        String downpaymentStr = rs.getString("downpayment_amount");
                        String amountPaidStr = rs.getString("amount_paid");
                        String remainingStr = rs.getString("remaining_balance");
                        
                        // Check if already encrypted (base64 format)
                        boolean alreadyEncrypted = PayableEncryptionUtil.isEncrypted(downpaymentStr) &&
                                                  PayableEncryptionUtil.isEncrypted(amountPaidStr) &&
                                                  PayableEncryptionUtil.isEncrypted(remainingStr);
                        
                        if (alreadyEncrypted) {
                            // Already encrypted, skip
                            continue;
                        }
                        
                        // Parse as double (handles both DECIMAL and plain VARCHAR numbers)
                        double downpayment = 0.0;
                        double amountPaid = 0.0;
                        double remaining = 0.0;
                        
                        try {
                            downpayment = Double.parseDouble(downpaymentStr);
                            amountPaid = Double.parseDouble(amountPaidStr);
                            remaining = Double.parseDouble(remainingStr);
                        } catch (NumberFormatException e) {
                            // If parsing fails, try getting as double from ResultSet
                            try {
                                downpayment = rs.getDouble("downpayment_amount");
                                amountPaid = rs.getDouble("amount_paid");
                                remaining = rs.getDouble("remaining_balance");
                            } catch (Exception ex) {
                                System.err.println("Warning: Could not parse amounts for payable_id " + payableId);
                                skipped++;
                                continue;
                            }
                        }
                        
                        // Get student_id from belong record for encryption
                        int studentId = getStudentIdFromBelong(conn, belongId);
                        
                        // Encrypt amounts
                        String encryptedDownpayment = PayableEncryptionUtil.encryptAmount(downpayment, studentId);
                        String encryptedAmountPaid = PayableEncryptionUtil.encryptAmount(amountPaid, studentId);
                        String encryptedRemaining = PayableEncryptionUtil.encryptAmount(remaining, studentId);
                        
                        if (encryptedDownpayment != null && encryptedAmountPaid != null && encryptedRemaining != null) {
                            // Update with encrypted values in the same columns
                            String updateSql = "UPDATE student_payables SET " +
                                             "downpayment_amount = ?, " +
                                             "amount_paid = ?, " +
                                             "remaining_balance = ? " +
                                             "WHERE payable_id = ?";
                            
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setString(1, encryptedDownpayment);
                                updateStmt.setString(2, encryptedAmountPaid);
                                updateStmt.setString(3, encryptedRemaining);
                                updateStmt.setInt(4, payableId);
                                updateStmt.executeUpdate();
                                migrated++;
                            }
                        } else {
                            skipped++;
                            System.err.println("Warning: Failed to encrypt payable_id " + payableId);
                        }
                    }
                }
                
                conn.commit();
                System.out.println("Migration completed!");
                System.out.println("Migrated: " + migrated + " payables");
                if (skipped > 0) {
                    System.out.println("Skipped: " + skipped + " payables (encryption failed or already encrypted)");
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            System.err.println("Error migrating payable amounts: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if columns are DECIMAL type and need conversion to VARCHAR
     */
    private static boolean checkIfNeedsTypeConversion(Connection conn) throws SQLException {
        java.sql.DatabaseMetaData meta = conn.getMetaData();
        ResultSet columns = meta.getColumns(null, null, "student_payables", "downpayment_amount");
        if (columns.next()) {
            String typeName = columns.getString("TYPE_NAME");
            return "DECIMAL".equalsIgnoreCase(typeName) || "NUMERIC".equalsIgnoreCase(typeName);
        }
        return false;
    }
    
    /**
     * Convert DECIMAL columns to VARCHAR
     */
    private static void convertColumnsToVarchar(Connection conn) throws SQLException {
        try (java.sql.Statement stmt = conn.createStatement()) {
            // MySQL requires MODIFY COLUMN to change type
            stmt.execute("ALTER TABLE student_payables MODIFY COLUMN downpayment_amount VARCHAR(255) NOT NULL DEFAULT '0.00'");
            stmt.execute("ALTER TABLE student_payables MODIFY COLUMN amount_paid VARCHAR(255) NOT NULL DEFAULT '0.00'");
            stmt.execute("ALTER TABLE student_payables MODIFY COLUMN remaining_balance VARCHAR(255) NOT NULL DEFAULT '0.00'");
            System.out.println("Columns converted to VARCHAR successfully");
        } catch (SQLException e) {
            // Columns might already be VARCHAR or conversion might have failed
            System.err.println("Warning: Could not convert columns (they might already be VARCHAR): " + e.getMessage());
        }
    }
    
    /**
     * Remove encrypted_* columns if they exist (we're encrypting in place now)
     */
    private static void removeEncryptedColumns(Connection conn) throws SQLException {
        try (java.sql.Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("ALTER TABLE student_payables DROP COLUMN encrypted_downpayment");
                System.out.println("Removed encrypted_downpayment column");
            } catch (SQLException e) {
                // Column doesn't exist, ignore
            }
            try {
                stmt.execute("ALTER TABLE student_payables DROP COLUMN encrypted_amount_paid");
                System.out.println("Removed encrypted_amount_paid column");
            } catch (SQLException e) {
                // Column doesn't exist, ignore
            }
            try {
                stmt.execute("ALTER TABLE student_payables DROP COLUMN encrypted_remaining_balance");
                System.out.println("Removed encrypted_remaining_balance column");
            } catch (SQLException e) {
                // Column doesn't exist, ignore
            }
        }
    }
    
    /**
     * Get student_id from belong record
     */
    private static int getStudentIdFromBelong(Connection conn, int belongId) throws SQLException {
        String sql = "SELECT student_id FROM belong WHERE belong_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, belongId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("student_id");
            }
        }
        return 0; // Default if not found
    }
    
    /**
     * Main method to run migration
     */
    public static void main(String[] args) {
        System.out.println("Payable Encryption Migration Tool");
        System.out.println("=================================");
        migratePayableAmounts();
    }
}

