package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class for creating and managing database triggers, views, and functions
 */
public class DatabaseObjectsUtil {
    
    /**
     * Initialize all database objects (triggers, views, functions)
     */
    public static void initializeDatabaseObjects(Connection conn) {
        try {
            // Try to read from SQL file first
            File sqlFile = new File("database_triggers_views_functions.sql");
            if (sqlFile.exists()) {
                executeSQLFile(sqlFile, conn);
                System.out.println("Database objects initialized from SQL file!");
                return;
            }
            
            // Try to read from resources
            InputStream is = DatabaseObjectsUtil.class.getResourceAsStream("/database_triggers_views_functions.sql");
            if (is != null) {
                executeSQLStream(is, conn);
                System.out.println("Database objects initialized from resources!");
                return;
            }
            
            // Fallback: create programmatically
            createDatabaseObjectsProgrammatically(conn);
            System.out.println("Database objects created programmatically!");
            
        } catch (Exception e) {
            System.err.println("Warning: Could not initialize database objects from file: " + e.getMessage());
            // Try creating programmatically as fallback
            try {
                createDatabaseObjectsProgrammatically(conn);
                System.out.println("Database objects created programmatically (fallback)!");
            } catch (SQLException e2) {
                System.err.println("Error creating database objects programmatically: " + e2.getMessage());
                e2.printStackTrace();
            }
        }
    }
    
    /**
     * Execute SQL file from file system
     */
    private static void executeSQLFile(File sqlFile, Connection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(sqlFile))) {
            executeSQL(reader, conn);
        }
    }
    
    /**
     * Execute SQL from input stream
     */
    private static void executeSQLStream(InputStream is, Connection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            executeSQL(reader, conn);
        }
    }
    
    /**
     * Execute SQL statements from reader
     */
    private static void executeSQL(BufferedReader reader, Connection conn) throws Exception {
        StringBuilder sql = new StringBuilder();
        String line;
        String delimiter = ";";
        boolean inFunction = false;
        boolean inTrigger = false;
        
        try (Statement stmt = conn.createStatement()) {
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                
                // Skip comments and empty lines
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                
                // Check for DELIMITER change
                if (trimmed.toUpperCase().startsWith("DELIMITER")) {
                    delimiter = trimmed.substring(10).trim();
                    continue;
                }
                
                // Check for function/trigger start
                if (trimmed.toUpperCase().contains("CREATE FUNCTION") || 
                    trimmed.toUpperCase().contains("CREATE TRIGGER")) {
                    inFunction = true;
                    inTrigger = true;
                }
                
                sql.append(line).append("\n");
                
                // Execute when we find the delimiter
                if (trimmed.endsWith(delimiter)) {
                    String statement = sql.toString().replace(delimiter, ";").trim();
                    if (!statement.isEmpty() && !statement.equals(";")) {
                        try {
                            // For functions and triggers, we need to handle DELIMITER differently
                            if (inFunction || inTrigger) {
                                // Remove DELIMITER statements and execute
                                statement = statement.replaceAll("(?i)DELIMITER\\s+[^;]+;", "");
                                if (statement.trim().length() > 0) {
                                    stmt.execute(statement);
                                }
                                inFunction = false;
                                inTrigger = false;
                            } else {
                                stmt.execute(statement);
                            }
                        } catch (SQLException e) {
                            // Ignore errors for objects that already exist
                            if (!e.getMessage().contains("already exists") && 
                                !e.getMessage().contains("Duplicate") &&
                                !e.getMessage().contains("Unknown")) {
                                System.err.println("Warning executing SQL: " + e.getMessage());
                                System.err.println("Statement: " + statement.substring(0, Math.min(100, statement.length())));
                            }
                        }
                    }
                    sql.setLength(0);
                }
            }
        }
    }
    
    /**
     * Create database objects programmatically as fallback
     */
    private static void createDatabaseObjectsProgrammatically(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Create functions
            createFunctions(stmt);
            
            // Create views
            createViews(stmt);
            
            // Create procedures
            createProcedures(stmt);
            
            // Note: Triggers are complex and should be created from SQL file
            // For now, we'll skip them in programmatic creation
        }
    }
    
    /**
     * Create database functions
     */
    private static void createFunctions(Statement stmt) throws SQLException {
        // Function: calculate_total_payable
        try {
            stmt.execute("DROP FUNCTION IF EXISTS calculate_total_payable");
            stmt.execute(
                "CREATE FUNCTION calculate_total_payable(p_belong_id INT) " +
                "RETURNS DECIMAL(10,2) " +
                "READS SQL DATA DETERMINISTIC " +
                "BEGIN " +
                "  DECLARE total_amount DECIMAL(10,2) DEFAULT 0.00; " +
                "  SELECT COALESCE(" +
                "    (SELECT COALESCE(first_sem_amount, 0) + COALESCE(second_sem_amount, 0) + COALESCE(summer_sem_amount, 0) " +
                "     FROM semester s INNER JOIN belong b ON s.semester_id = b.semester_id " +
                "     WHERE b.belong_id = p_belong_id), 0" +
                "  ) INTO total_amount; " +
                "  RETURN total_amount; " +
                "END"
            );
        } catch (SQLException e) {
            if (!e.getMessage().contains("already exists")) {
                System.err.println("Warning creating function calculate_total_payable: " + e.getMessage());
            }
        }
        
        // Function: calculate_remaining_balance
        try {
            stmt.execute("DROP FUNCTION IF EXISTS calculate_remaining_balance");
            stmt.execute(
                "CREATE FUNCTION calculate_remaining_balance(p_belong_id INT) " +
                "RETURNS DECIMAL(10,2) " +
                "READS SQL DATA DETERMINISTIC " +
                "BEGIN " +
                "  DECLARE total_payable DECIMAL(10,2) DEFAULT 0.00; " +
                "  DECLARE amount_paid DECIMAL(10,2) DEFAULT 0.00; " +
                "  SELECT COALESCE(" +
                "    (SELECT COALESCE(first_sem_amount, 0) + COALESCE(second_sem_amount, 0) + COALESCE(summer_sem_amount, 0) " +
                "     FROM semester s INNER JOIN belong b ON s.semester_id = b.semester_id " +
                "     WHERE b.belong_id = p_belong_id), 0" +
                "  ) INTO total_payable; " +
                "  SELECT COALESCE(SUM(amount_paid), 0) INTO amount_paid " +
                "  FROM student_payables WHERE belong_id = p_belong_id; " +
                "  RETURN GREATEST(total_payable - amount_paid, 0.00); " +
                "END"
            );
        } catch (SQLException e) {
            if (!e.getMessage().contains("already exists")) {
                System.err.println("Warning creating function calculate_remaining_balance: " + e.getMessage());
            }
        }
        
        // Function: determine_payment_status
        try {
            stmt.execute("DROP FUNCTION IF EXISTS determine_payment_status");
            stmt.execute(
                "CREATE FUNCTION determine_payment_status(p_belong_id INT) " +
                "RETURNS VARCHAR(20) " +
                "READS SQL DATA DETERMINISTIC " +
                "BEGIN " +
                "  DECLARE total_payable DECIMAL(10,2) DEFAULT 0.00; " +
                "  DECLARE amount_paid DECIMAL(10,2) DEFAULT 0.00; " +
                "  DECLARE due_date_val DATE; " +
                "  DECLARE status_val VARCHAR(20); " +
                "  SELECT COALESCE(" +
                "    (SELECT COALESCE(first_sem_amount, 0) + COALESCE(second_sem_amount, 0) + COALESCE(summer_sem_amount, 0) " +
                "     FROM semester s INNER JOIN belong b ON s.semester_id = b.semester_id " +
                "     WHERE b.belong_id = p_belong_id), 0" +
                "  ) INTO total_payable; " +
                "  SELECT COALESCE(SUM(amount_paid), 0) INTO amount_paid " +
                "  FROM student_payables WHERE belong_id = p_belong_id; " +
                "  SELECT d.due_date INTO due_date_val " +
                "  FROM student_payables sp " +
                "  LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                "  WHERE sp.belong_id = p_belong_id LIMIT 1; " +
                "  IF amount_paid >= total_payable OR ABS(amount_paid - total_payable) < 0.01 THEN " +
                "    SET status_val = 'PAID'; " +
                "  ELSEIF amount_paid > 0 THEN " +
                "    IF due_date_val IS NOT NULL AND due_date_val < CURDATE() THEN " +
                "      SET status_val = 'OVERDUE'; " +
                "    ELSE " +
                "      SET status_val = 'PARTIAL'; " +
                "    END IF; " +
                "  ELSE " +
                "    IF due_date_val IS NOT NULL AND due_date_val < CURDATE() THEN " +
                "      SET status_val = 'OVERDUE'; " +
                "    ELSE " +
                "      SET status_val = 'UNPAID'; " +
                "    END IF; " +
                "  END IF; " +
                "  RETURN status_val; " +
                "END"
            );
        } catch (SQLException e) {
            if (!e.getMessage().contains("already exists")) {
                System.err.println("Warning creating function determine_payment_status: " + e.getMessage());
            }
        }
        
        // Function: get_student_fullname
        try {
            stmt.execute("DROP FUNCTION IF EXISTS get_student_fullname");
            stmt.execute(
                "CREATE FUNCTION get_student_fullname(p_student_id INT) " +
                "RETURNS VARCHAR(200) " +
                "READS SQL DATA DETERMINISTIC " +
                "BEGIN " +
                "  DECLARE full_name VARCHAR(200); " +
                "  SELECT CONCAT_WS(' ', " +
                "    COALESCE(first_name, ''), " +
                "    COALESCE(middle_name, ''), " +
                "    COALESCE(last_name, ''), " +
                "    CASE " +
                "      WHEN first_name IS NULL AND middle_name IS NULL AND last_name IS NULL " +
                "      THEN fullname ELSE '' " +
                "    END" +
                "  ) INTO full_name FROM student WHERE student_id = p_student_id; " +
                "  RETURN TRIM(full_name); " +
                "END"
            );
        } catch (SQLException e) {
            if (!e.getMessage().contains("already exists")) {
                System.err.println("Warning creating function get_student_fullname: " + e.getMessage());
            }
        }
    }
    
    /**
     * Create database views
     */
    private static void createViews(Statement stmt) throws SQLException {
        // View: v_student_payment_summary
        try {
            stmt.execute("DROP VIEW IF EXISTS v_student_payment_summary");
            stmt.execute(
                "CREATE VIEW v_student_payment_summary AS " +
                "SELECT s.student_id, s.student_number, " +
                "  COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS full_name, " +
                "  s.major, s.year, sy.year_range AS school_year, b.belong_id, " +
                "  CASE " +
                "    WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                "    WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                "    WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                "    ELSE 'Unknown' " +
                "  END AS semester_name, " +
                "  COALESCE(sem.first_sem_amount, 0) + COALESCE(sem.second_sem_amount, 0) + COALESCE(sem.summer_sem_amount, 0) AS total_payable, " +
                "  COALESCE(SUM(sp.amount_paid), 0) AS total_paid, " +
                "  calculate_remaining_balance(b.belong_id) AS remaining_balance, " +
                "  determine_payment_status(b.belong_id) AS payment_status, " +
                "  MAX(d.due_date) AS due_date, MAX(sp.payable_id) AS latest_payable_id " +
                "FROM student s " +
                "INNER JOIN belong b ON s.student_id = b.student_id " +
                "INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id " +
                "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                "LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                "LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                "WHERE COALESCE(s.status, 'active') = 'active' " +
                "  AND COALESCE(b.status, 'active') = 'active' " +
                "GROUP BY s.student_id, s.student_number, s.major, s.year, sy.year_range, b.belong_id, sem.semester_id"
            );
        } catch (SQLException e) {
            System.err.println("Warning creating view v_student_payment_summary: " + e.getMessage());
        }
        
        // View: v_overdue_payments
        try {
            stmt.execute("DROP VIEW IF EXISTS v_overdue_payments");
            // Use simpler query that doesn't rely on functions in WHERE clause for better compatibility
            stmt.execute(
                "CREATE VIEW v_overdue_payments AS " +
                "SELECT s.student_id, s.student_number, " +
                "  COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS full_name, " +
                "  sy.year_range AS school_year, b.belong_id, sp.payable_id, " +
                "  calculate_remaining_balance(b.belong_id) AS remaining_balance, " +
                "  d.due_date, DATEDIFF(CURDATE(), d.due_date) AS days_overdue " +
                "FROM student s " +
                "INNER JOIN belong b ON s.student_id = b.student_id " +
                "INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id " +
                "INNER JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                "LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                "WHERE d.due_date IS NOT NULL " +
                "  AND d.due_date < CURDATE() " +
                "  AND sp.status != 'PAID' " +
                "  AND COALESCE(s.status, 'active') = 'active' " +
                "  AND COALESCE(b.status, 'active') = 'active'"
            );
        } catch (SQLException e) {
            System.err.println("Warning creating view v_overdue_payments: " + e.getMessage());
            e.printStackTrace();
        }
        
        // View: v_top_payers
        try {
            stmt.execute("DROP VIEW IF EXISTS v_top_payers");
            stmt.execute(
                "CREATE VIEW v_top_payers AS " +
                "SELECT s.student_id, s.student_number, " +
                "  COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS student_name, " +
                "  sy.year_range AS school_year, " +
                "  b.school_year_id, " +
                "  CASE " +
                "    WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                "    WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                "    WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                "    ELSE NULL " +
                "  END AS semester_name, " +
                "  SUM(sp.amount_paid) AS total_amount_paid, " +
                "  COUNT(DISTINCT sp.payable_id) AS payment_count " +
                "FROM student s " +
                "INNER JOIN belong b ON s.student_id = b.student_id " +
                "INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id " +
                "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                "INNER JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                "WHERE sp.amount_paid > 0 " +
                "  AND COALESCE(s.status, 'active') = 'active' " +
                "  AND COALESCE(b.status, 'active') = 'active' " +
                "GROUP BY s.student_id, s.student_number, sy.year_range, b.school_year_id, sem.semester_id " +
                "ORDER BY total_amount_paid DESC"
            );
        } catch (SQLException e) {
            System.err.println("Warning creating view v_top_payers: " + e.getMessage());
        }
        
        // View: v_dashboard_summary
        try {
            stmt.execute("DROP VIEW IF EXISTS v_dashboard_summary");
            stmt.execute(
                "CREATE VIEW v_dashboard_summary AS " +
                "SELECT b.school_year_id, sy.year_range AS school_year, " +
                "  CASE " +
                "    WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                "    WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                "    WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                "    ELSE NULL " +
                "  END AS semester_name, " +
                "  COALESCE(SUM(sp.amount_paid), 0) AS total_payments_collected, " +
                "  COUNT(DISTINCT CASE WHEN sp.amount_paid > 0 THEN s.student_id END) AS students_who_paid, " +
                "  COUNT(DISTINCT s.student_id) AS total_students_enrolled, " +
                "  COUNT(DISTINCT CASE WHEN d.due_date IS NOT NULL AND d.due_date < CURDATE() AND sp.status != 'PAID' THEN sp.payable_id END) AS overdue_payments_count " +
                "FROM student s " +
                "INNER JOIN belong b ON s.student_id = b.student_id " +
                "INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id " +
                "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                "LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                "LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                "WHERE COALESCE(s.status, 'active') = 'active' " +
                "  AND COALESCE(b.status, 'active') = 'active' " +
                "GROUP BY b.school_year_id, sy.year_range, sem.semester_id"
            );
        } catch (SQLException e) {
            System.err.println("Warning creating view v_dashboard_summary: " + e.getMessage());
        }
        
        // View: v_latest_payments
        try {
            stmt.execute("DROP VIEW IF EXISTS v_latest_payments");
            stmt.execute(
                "CREATE VIEW v_latest_payments AS " +
                "SELECT s.student_id, s.student_number, " +
                "  COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS student_name, " +
                "  sy.year_range AS school_year, b.school_year_id, " +
                "  CASE " +
                "    WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                "    WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                "    WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                "    ELSE NULL " +
                "  END AS semester_name, " +
                "  COALESCE(sem.first_sem_amount, 0) + COALESCE(sem.second_sem_amount, 0) + COALESCE(sem.summer_sem_amount, 0) AS payable_amount, " +
                "  sp.payable_id, sp.amount_paid, sp.status, d.due_date, sp.payable_id AS sort_key " +
                "FROM student s " +
                "INNER JOIN belong b ON s.student_id = b.student_id " +
                "INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id " +
                "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                "INNER JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                "LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                "WHERE COALESCE(s.status, 'active') = 'active' " +
                "  AND COALESCE(b.status, 'active') = 'active'"
            );
        } catch (SQLException e) {
            System.err.println("Warning creating view v_latest_payments: " + e.getMessage());
        }
        
        // View: v_payment_statistics_monthly (enhanced)
        try {
            stmt.execute("DROP VIEW IF EXISTS v_payment_statistics_monthly");
            stmt.execute(
                "CREATE VIEW v_payment_statistics_monthly AS " +
                "SELECT DATE_FORMAT(d.due_date, '%Y-%m') AS payment_month, " +
                "  DATE_FORMAT(d.due_date, '%M %Y') AS month_name, " +
                "  DATE_FORMAT(d.due_date, '%b') AS month_abbr, " +
                "  COUNT(DISTINCT sp.payable_id) AS total_payments, " +
                "  COUNT(DISTINCT CASE WHEN sp.status = 'PAID' THEN sp.payable_id END) AS paid_count, " +
                "  COUNT(DISTINCT CASE WHEN sp.status = 'PARTIAL' THEN sp.payable_id END) AS partial_count, " +
                "  COUNT(DISTINCT CASE WHEN sp.status = 'UNPAID' THEN sp.payable_id END) AS unpaid_count, " +
                "  COUNT(DISTINCT CASE WHEN sp.status = 'OVERDUE' THEN sp.payable_id END) AS overdue_count, " +
                "  SUM(sp.amount_paid) AS total_amount_paid, " +
                "  SUM(sp.remaining_balance) AS total_remaining_balance, " +
                "  b.school_year_id, " +
                "  CASE " +
                "    WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                "    WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                "    WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                "    ELSE NULL " +
                "  END AS semester_name " +
                "FROM student_payables sp " +
                "INNER JOIN belong b ON sp.belong_id = b.belong_id " +
                "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                "LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                "WHERE COALESCE(b.status, 'active') = 'active' " +
                "GROUP BY DATE_FORMAT(d.due_date, '%Y-%m'), DATE_FORMAT(d.due_date, '%M %Y'), DATE_FORMAT(d.due_date, '%b'), b.school_year_id, sem.semester_id " +
                "ORDER BY payment_month DESC"
            );
        } catch (SQLException e) {
            System.err.println("Warning creating view v_payment_statistics_monthly: " + e.getMessage());
        }
        
        // View: v_student_enrollment
        try {
            stmt.execute("DROP VIEW IF EXISTS v_student_enrollment");
            stmt.execute(
                "CREATE VIEW v_student_enrollment AS " +
                "SELECT s.student_id, s.student_number, " +
                "  COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS student_name, " +
                "  s.first_name, s.middle_name, s.last_name, s.fullname, " +
                "  s.major, s.year, s.status AS student_status, " +
                "  b.belong_id, b.school_year_id, sy.year_range AS school_year, b.semester_id, " +
                "  CASE " +
                "    WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                "    WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                "    WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                "    ELSE NULL " +
                "  END AS semester_name, " +
                "  COALESCE(b.status, 'active') AS enrollment_status, " +
                "  COALESCE(sem.first_sem_amount, 0) + COALESCE(sem.second_sem_amount, 0) + COALESCE(sem.summer_sem_amount, 0) AS total_payable " +
                "FROM student s " +
                "LEFT JOIN belong b ON s.student_id = b.student_id " +
                "LEFT JOIN school_year sy ON b.school_year_id = sy.school_year_id " +
                "LEFT JOIN semester sem ON b.semester_id = sem.semester_id"
            );
        } catch (SQLException e) {
            System.err.println("Warning creating view v_student_enrollment: " + e.getMessage());
        }
    }
    
    /**
     * Create stored procedures
     * Note: Procedures are complex and should ideally be created from SQL file
     * This is a fallback for programmatic creation
     */
    private static void createProcedures(Statement stmt) throws SQLException {
        // Note: Stored procedures with DELIMITER are best created from SQL file
        // These are placeholders that will be created from the SQL file
        // If the SQL file execution fails, these won't be created programmatically
        // as they require DELIMITER handling which is complex in JDBC
        
        System.out.println("Note: Stored procedures should be created from database_triggers_views_functions.sql file");
        System.out.println("Procedures to be created:");
        System.out.println("  - sp_get_students");
        System.out.println("  - sp_add_student");
        System.out.println("  - sp_update_student");
        System.out.println("  - sp_deactivate_student");
        System.out.println("  - sp_reactivate_student");
        System.out.println("  - sp_get_student_statistics");
    }
}

