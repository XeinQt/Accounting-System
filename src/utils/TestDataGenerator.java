package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Utility class to generate test data for Promissory Notes
 */
public class TestDataGenerator {
    
    /**
     * Execute SQL script to insert 10 students with due dates
     */
    public static void insertTestStudentsWithDueDates() {
        try (Connection conn = DatabaseUtil.getConnection()) {
            
            // Ensure we have a school year
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT IGNORE INTO school_year (year_range) VALUES ('2024-2025')")) {
                pstmt.executeUpdate();
            }
            
            // Ensure we have a semester
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT IGNORE INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount) VALUES (50000.00, 50000.00, 25000.00)")) {
                pstmt.executeUpdate();
            }
            
            // Get IDs
            int schoolYearId;
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT school_year_id FROM school_year WHERE year_range = '2024-2025' LIMIT 1");
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    schoolYearId = rs.getInt(1);
                } else {
                    throw new Exception("Could not find school year");
                }
            }
            
            int semesterId;
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT semester_id FROM semester LIMIT 1");
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    semesterId = rs.getInt(1);
                } else {
                    throw new Exception("Could not find semester");
                }
            }
            
            // Insert students
            String[] studentNumbers = {
                "STU-2024-001", "STU-2024-002", "STU-2024-003", "STU-2024-004", "STU-2024-005",
                "STU-2024-006", "STU-2024-007", "STU-2024-008", "STU-2024-009", "STU-2024-010"
            };
            String[] names = {
                "John Michael Santos", "Maria Cristina Reyes", "Juan Carlos Dela Cruz",
                "Anna Patricia Garcia", "Robert James Villanueva", "Sarah Jane Fernandez",
                "Mark Anthony Torres", "Jennifer Rose Mendoza", "Christian Paul Ramos",
                "Michelle Ann Bautista"
            };
            String[] majors = {
                "Computer Science", "Information Technology", "Business Administration",
                "Computer Engineering", "Accounting", "Information Systems",
                "Computer Science", "Business Administration", "Information Technology",
                "Accounting"
            };
            String[] years = {
                "1st Year", "2nd Year", "3rd Year", "1st Year", "2nd Year",
                "3rd Year", "4th Year", "1st Year", "2nd Year", "3rd Year"
            };
            String[] deps = {
                "IT", "IT", "Business", "Engineering", "Business",
                "IT", "IT", "Business", "IT", "Business"
            };
            String[] colleges = {
                "College of Computing", "College of Computing", "College of Business",
                "College of Engineering", "College of Business", "College of Computing",
                "College of Computing", "College of Business", "College of Computing",
                "College of Business"
            };
            
            int[] studentIds = new int[10];
            for (int i = 0; i < 10; i++) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO student (student_number, fullname, major, year, dep, college, school_year_id, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, 'active') " +
                        "ON DUPLICATE KEY UPDATE fullname = VALUES(fullname), status = 'active'")) {
                    pstmt.setString(1, studentNumbers[i]);
                    pstmt.setString(2, names[i]);
                    pstmt.setString(3, majors[i]);
                    pstmt.setString(4, years[i]);
                    pstmt.setString(5, deps[i]);
                    pstmt.setString(6, colleges[i]);
                    pstmt.setInt(7, schoolYearId);
                    pstmt.executeUpdate();
                }
                
                // Get student ID
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT student_id FROM student WHERE student_number = ?")) {
                    pstmt.setString(1, studentNumbers[i]);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        studentIds[i] = rs.getInt(1);
                    }
                }
            }
            
            // Create belong records
            int[] belongIds = new int[10];
            for (int i = 0; i < 10; i++) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO belong (student_id, school_year_id, semester_id, status) " +
                        "VALUES (?, ?, ?, 'active') " +
                        "ON DUPLICATE KEY UPDATE status = 'active'")) {
                    pstmt.setInt(1, studentIds[i]);
                    pstmt.setInt(2, schoolYearId);
                    pstmt.setInt(3, semesterId);
                    pstmt.executeUpdate();
                }
                
                // Get belong ID
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT belong_id FROM belong WHERE student_id = ? AND school_year_id = ? AND semester_id = ? LIMIT 1")) {
                    pstmt.setInt(1, studentIds[i]);
                    pstmt.setInt(2, schoolYearId);
                    pstmt.setInt(3, semesterId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        belongIds[i] = rs.getInt(1);
                    }
                }
            }
            
            // Delete existing due dates in range
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM duedate WHERE due_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)")) {
                pstmt.executeUpdate();
            }
            
            // Create due dates
            int[] dueDateIds = new int[10];
            String[] messages = {
                "Payment due today",
                "Payment due tomorrow",
                "Payment due in 2 days",
                "Payment due in 3 days",
                "Payment due in 4 days",
                "Payment due in 5 days",
                "Payment due in 6 days",
                "Payment due in 7 days",
                "Payment was due yesterday",
                "Payment was due 2 days ago"
            };
            
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate[] dueDates = {
                today,
                today.plusDays(1),
                today.plusDays(2),
                today.plusDays(3),
                today.plusDays(4),
                today.plusDays(5),
                today.plusDays(6),
                today.plusDays(7),
                today.minusDays(1),
                today.minusDays(2)
            };
            
            for (int i = 0; i < 10; i++) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO duedate (due_date, message) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setDate(1, java.sql.Date.valueOf(dueDates[i]));
                    pstmt.setString(2, messages[i]);
                    pstmt.executeUpdate();
                    
                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        dueDateIds[i] = rs.getInt(1);
                    }
                }
            }
            
            // Delete existing payables
            for (int belongId : belongIds) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "DELETE FROM student_payables WHERE belong_id = ?")) {
                    pstmt.setInt(1, belongId);
                    pstmt.executeUpdate();
                }
            }
            
            // Create student_payables
            double[] amountsPaid = {10000.00, 20000.00, 5000.00, 15000.00, 25000.00, 0.00, 30000.00, 0.00, 10000.00, 5000.00};
            String[] statuses = {"PARTIAL", "PARTIAL", "PARTIAL", "PARTIAL", "PARTIAL", "UNPAID", "PARTIAL", "UNPAID", "PARTIAL", "PARTIAL"};
            
            for (int i = 0; i < 10; i++) {
                double remainingBalance = 50000.00 - amountsPaid[i];
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO student_payables (belong_id, downpayment_amount, amount_paid, remaining_balance, status, duedate_id) " +
                        "VALUES (?, 50000.00, ?, ?, ?, ?)")) {
                    pstmt.setInt(1, belongIds[i]);
                    pstmt.setDouble(2, amountsPaid[i]);
                    pstmt.setDouble(3, remainingBalance);
                    pstmt.setString(4, statuses[i]);
                    pstmt.setInt(5, dueDateIds[i]);
                    pstmt.executeUpdate();
                }
            }
            
            System.out.println("Successfully inserted 10 students with due dates!");
            
        } catch (Exception e) {
            System.err.println("Error inserting test data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        insertTestStudentsWithDueDates();
    }
}
