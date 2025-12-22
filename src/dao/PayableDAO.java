package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import models.StudentPayableView;
import utils.DatabaseUtil;

public class PayableDAO {
    
    /**
     * Get all students with their payable amounts for each semester
     * Returns a list grouped by student, showing amounts for 1st Sem, 2nd Sem, and Summer Sem
     * If semester is specified, only shows payables for that semester
     * Tries to use stored procedure first, falls back to direct query
     */
    public List<StudentPayableView> getAllStudentPayables(Integer schoolYearId, String semester) {
        // Try using stored procedure first
        try {
            return getAllStudentPayablesUsingProcedure(schoolYearId, semester, null, null, null);
        } catch (Exception e) {
            // Fallback to direct query
            System.err.println("Warning: Could not use sp_get_student_payables procedure, falling back to direct query: " + e.getMessage());
        }
        
        List<StudentPayableView> payables = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT s.student_id, s.student_number, " +
                        "COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS student_name, " +
                        "s.major AS program, s.year, " +
                        "CASE " +
                        "  WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                        "  WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                        "  WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                        "  ELSE '' " +
                        "END as semester_name, " +
                        "COALESCE(MAX(sp.downpayment_amount), 0) AS first_sem_amount, " +
                        "COALESCE(MAX(sp.downpayment_amount), 0) AS second_sem_amount, " +
                        "COALESCE(MAX(sp.downpayment_amount), 0) AS summer_sem_amount, " +
                        "MAX(sp.downpayment_amount) as downpayment_amount, MAX(d.due_date) as due_date " +
                        "FROM student s " +
                        "INNER JOIN belong b ON s.student_id = b.student_id " +
                        "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                        "LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                        "LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                        "WHERE COALESCE(s.status, 'active') = 'active' " +
                        "AND COALESCE(b.status, 'active') = 'active' " +
                        "AND (sp.downpayment_amount IS NULL OR sp.downpayment_amount > 0)";
            
            if (schoolYearId != null) {
                sql += " AND b.school_year_id = ?";
            }
            
            if (semester != null && !semester.trim().isEmpty()) {
                if ("1st Sem".equals(semester)) {
                    sql += " AND sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL)";
                } else if ("2nd Sem".equals(semester)) {
                    sql += " AND sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL)";
                } else if ("Summer Sem".equals(semester)) {
                    sql += " AND sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL)";
                }
            }
            
            sql += " GROUP BY s.student_id, s.student_number, s.first_name, s.middle_name, s.last_name, s.fullname, s.major, s.year " +
                   "ORDER BY s.student_number";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int paramIndex = 1;
                if (schoolYearId != null) {
                    pstmt.setInt(paramIndex++, schoolYearId);
                }
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    StudentPayableView view = new StudentPayableView();
                    view.setStudentId(rs.getInt("student_id"));
                    view.setStudentNumber(rs.getString("student_number"));
                    view.setStudentName(rs.getString("student_name"));
                    view.setProgram(rs.getString("program"));
                    view.setYear(rs.getString("year"));
                    view.setSemester(rs.getString("semester_name"));
                    // Use downpayment_amount from student_payables, or 0 if no payable exists
                    double downpayment = rs.getDouble("downpayment_amount");
                    if (rs.wasNull()) {
                        downpayment = 0;
                    }
                    String semName = rs.getString("semester_name");
                    
                    // Set amount based on semester
                    if ("1st Sem".equals(semName)) {
                        view.setFirstSemAmount(downpayment);
                        view.setSecondSemAmount(0);
                        view.setSummerSemAmount(0);
                    } else if ("2nd Sem".equals(semName)) {
                        view.setFirstSemAmount(0);
                        view.setSecondSemAmount(downpayment);
                        view.setSummerSemAmount(0);
                    } else if ("Summer Sem".equals(semName)) {
                        view.setFirstSemAmount(0);
                        view.setSecondSemAmount(0);
                        view.setSummerSemAmount(downpayment);
                    } else {
                        // Default to 0 if semester is unknown
                        view.setFirstSemAmount(0);
                        view.setSecondSemAmount(0);
                        view.setSummerSemAmount(0);
                    }
                    
                    java.sql.Date dueDate = rs.getDate("due_date");
                    if (dueDate != null) {
                        view.setDueDate(dueDate.toLocalDate());
                    }
                    
                    payables.add(view);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting student payables: " + e.getMessage());
            e.printStackTrace();
        }
        
        return payables;
    }
    
    /**
     * Get student payable by student ID and school year
     */
    public StudentPayableView getStudentPayable(int studentId, Integer schoolYearId) {
        List<StudentPayableView> payables = getAllStudentPayables(schoolYearId, null);
        for (StudentPayableView payable : payables) {
            if (payable.getStudentId() == studentId) {
                return payable;
            }
        }
        return null;
    }
    
    /**
     * Add or update payable amounts for a student
     * This will create/update belong records and student_payables records
     * If semester is specified, only saves that semester amount
     * Tries to use stored procedure first, falls back to direct query
     */
    public boolean saveStudentPayable(int studentId, Integer schoolYearId, double firstSem, double secondSem, double summerSem, String semester) {
        // Try using stored procedure first
        try {
            double amount = 0;
            if (semester != null && !semester.isEmpty()) {
                if (semester.equals("1st Sem")) {
                    amount = firstSem;
                } else if (semester.equals("2nd Sem")) {
                    amount = secondSem;
                } else if (semester.equals("Summer Sem")) {
                    amount = summerSem;
                }
            }
            
            if (amount > 0) {
                return saveStudentPayableUsingProcedure(studentId, schoolYearId, amount, semester);
            }
        } catch (Exception e) {
            // Fallback to direct query
            System.err.println("Warning: Could not use sp_save_student_payable procedure, falling back to direct query: " + e.getMessage());
        }
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Get or create school year
                if (schoolYearId == null) {
                    schoolYearId = this.getFirstSchoolYearId(conn);
                }
                
                // If semester is specified, only save that semester
                if (semester != null && !semester.isEmpty()) {
                    double amount = 0;
                    int semesterId = 0;
                    
                    if (semester.equals("1st Sem")) {
                        amount = firstSem;
                        semesterId = getOrCreateSemester(conn, amount, 0, 0);
                    } else if (semester.equals("2nd Sem")) {
                        amount = secondSem;
                        semesterId = getOrCreateSemester(conn, 0, amount, 0);
                    } else if (semester.equals("Summer Sem")) {
                        amount = summerSem;
                        semesterId = getOrCreateSemester(conn, 0, 0, amount);
                    }
                    
                    if (amount > 0) {
                        // Get or create belong record
                        int belongId = getOrCreateBelong(conn, studentId, schoolYearId, semesterId);
                        
                        // Update or insert student_payable
                        updateOrInsertPayable(conn, belongId, amount);
                    }
                } else {
                    // Original logic: save all semesters
                    int firstSemId = getOrCreateSemester(conn, firstSem, 0, 0);
                    int secondSemId = getOrCreateSemester(conn, 0, secondSem, 0);
                    int summerSemId = getOrCreateSemester(conn, 0, 0, summerSem);
                    
                    // For each semester, get or create belong record and student_payable
                    int[] semIds = {firstSemId, secondSemId, summerSemId};
                    double[] amounts = {firstSem, secondSem, summerSem};
                    
                    for (int i = 0; i < semIds.length; i++) {
                        if (amounts[i] > 0) {
                            // Get or create belong record
                            int belongId = getOrCreateBelong(conn, studentId, schoolYearId, semIds[i]);
                            
                            // Update or insert student_payable
                            updateOrInsertPayable(conn, belongId, amounts[i]);
                        }
                    }
                }
                
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error saving student payable: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Update student payable (same as saveStudentPayable - it handles both add and update)
     */
    public boolean updateStudentPayable(int studentId, Integer schoolYearId, double firstSem, double secondSem, double summerSem, String semester) {
        return saveStudentPayable(studentId, schoolYearId, firstSem, secondSem, summerSem, semester);
    }
    
    /**
     * Delete payable for a student
     * If semester is provided, deletes only for that semester
     * Tries to use stored procedure first, falls back to direct query
     */
    public boolean deleteStudentPayable(int studentId, Integer schoolYearId) {
        return deleteStudentPayable(studentId, schoolYearId, null);
    }
    
    /**
     * Delete payable for a student by semester and school year
     * Tries to use stored procedure first, falls back to direct query
     */
    public boolean deleteStudentPayable(int studentId, Integer schoolYearId, String semester) {
        // Try using stored procedure first
        try {
            return deleteStudentPayableUsingProcedure(studentId, schoolYearId, semester);
        } catch (Exception e) {
            // Fallback to direct query
            System.err.println("Warning: Could not use sp_delete_student_payable procedure, falling back to direct query: " + e.getMessage());
        }
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                String sql;
                if (semester != null && !semester.trim().isEmpty()) {
                    // Delete for specific semester
                    sql = "DELETE sp FROM student_payables sp " +
                          "INNER JOIN belong b ON sp.belong_id = b.belong_id " +
                          "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                          "WHERE b.student_id = ? " +
                          (schoolYearId != null ? "AND b.school_year_id = ? " : "") +
                          "AND (";
                    
                    if ("1st Sem".equals(semester)) {
                        sql += "sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL)";
                    } else if ("2nd Sem".equals(semester)) {
                        sql += "sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL)";
                    } else if ("Summer Sem".equals(semester)) {
                        sql += "sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL)";
                    }
                    sql += ")";
                } else {
                    // Delete all payables for the student in the school year
                    sql = "DELETE sp FROM student_payables sp " +
                          "JOIN belong b ON sp.belong_id = b.belong_id " +
                          "WHERE b.student_id = ? " +
                          (schoolYearId != null ? "AND b.school_year_id = ?" : "");
                }
                
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    int paramIndex = 1;
                    pstmt.setInt(paramIndex++, studentId);
                    if (schoolYearId != null) {
                        pstmt.setInt(paramIndex++, schoolYearId);
                    }
                    
                    int rowsAffected = pstmt.executeUpdate();
                    conn.commit();
                    return rowsAffected > 0;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error deleting student payable: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if a student has payables for a specific semester and school year
     */
    public boolean hasStudentPayable(int studentId, Integer schoolYearId, String semester) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Get school year ID if not provided
            if (schoolYearId == null) {
                schoolYearId = this.getFirstSchoolYearId(conn);
            }
            
            // Build semester condition
            String semesterCondition = "";
            if (semester.equals("1st Sem")) {
                semesterCondition = "sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL)";
            } else if (semester.equals("2nd Sem")) {
                semesterCondition = "sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL)";
            } else if (semester.equals("Summer Sem")) {
                semesterCondition = "sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL)";
            } else {
                return false;
            }
            
            // Check if student has payable for this semester and school year
            String sql = "SELECT COUNT(*) as count " +
                        "FROM student_payables sp " +
                        "JOIN belong b ON sp.belong_id = b.belong_id " +
                        "JOIN semester sem ON b.semester_id = sem.semester_id " +
                        "WHERE b.student_id = ? " +
                        "AND b.school_year_id = ? " +
                        "AND sp.downpayment_amount > 0 " +
                        "AND (" + semesterCondition + ")";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, studentId);
                pstmt.setInt(2, schoolYearId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if student has payable: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * Get all student payables using stored procedure
     */
    private List<StudentPayableView> getAllStudentPayablesUsingProcedure(
            Integer schoolYearId, String semester, String searchTerm, String year, String program) {
        List<StudentPayableView> payables = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.CallableStatement cstmt = conn.prepareCall("{CALL sp_get_student_payables(?, ?, ?, ?, ?)}")) {
            
            cstmt.setObject(1, schoolYearId);
            cstmt.setString(2, semester);
            cstmt.setString(3, searchTerm);
            cstmt.setString(4, year);
            cstmt.setString(5, program);
            
            ResultSet rs = cstmt.executeQuery();
            while (rs.next()) {
                StudentPayableView view = new StudentPayableView();
                view.setStudentId(rs.getInt("student_id"));
                view.setStudentNumber(rs.getString("student_number"));
                view.setStudentName(rs.getString("student_name"));
                view.setProgram(rs.getString("program"));
                view.setYear(rs.getString("year"));
                
                // Get semester name from the result
                String semesterName = rs.getString("semester_name");
                if (semesterName == null) {
                    semesterName = rs.getString("semester");
                }
                view.setSemester(semesterName != null ? semesterName : "");
                
                // Get payable amount (may be NULL if no payable exists)
                double payableAmount = 0;
                try {
                    payableAmount = rs.getDouble("payable_amount");
                    if (rs.wasNull()) {
                        payableAmount = 0;
                    }
                } catch (SQLException e) {
                    payableAmount = 0;
                }
                
                // Set amount based on semester
                if ("1st Sem".equals(semesterName)) {
                    view.setFirstSemAmount(payableAmount);
                    view.setSecondSemAmount(0);
                    view.setSummerSemAmount(0);
                } else if ("2nd Sem".equals(semesterName)) {
                    view.setFirstSemAmount(0);
                    view.setSecondSemAmount(payableAmount);
                    view.setSummerSemAmount(0);
                } else if ("Summer Sem".equals(semesterName)) {
                    view.setFirstSemAmount(0);
                    view.setSecondSemAmount(0);
                    view.setSummerSemAmount(payableAmount);
                } else {
                    view.setFirstSemAmount(0);
                    view.setSecondSemAmount(0);
                    view.setSummerSemAmount(0);
                }
                
                java.sql.Date dueDate = rs.getDate("due_date");
                if (dueDate != null) {
                    view.setDueDate(dueDate.toLocalDate());
                }
                
                payables.add(view);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting student payables from procedure", e);
        }
        
        return payables;
    }
    
    /**
     * Save student payable using stored procedure
     */
    private boolean saveStudentPayableUsingProcedure(
            int studentId, Integer schoolYearId, double amount, String semester) {
        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.CallableStatement cstmt = conn.prepareCall("{CALL sp_save_student_payable(?, ?, ?, ?, ?)}")) {
            
            cstmt.setInt(1, studentId);
            cstmt.setObject(2, schoolYearId);
            cstmt.setDouble(3, amount);
            cstmt.setString(4, semester);
            cstmt.registerOutParameter(5, java.sql.Types.VARCHAR);
            
            cstmt.executeUpdate();
            
            String result = cstmt.getString(5);
            return "CREATED".equals(result) || "UPDATED".equals(result);
        } catch (SQLException e) {
            throw new RuntimeException("Error saving student payable from procedure", e);
        }
    }
    
    /**
     * Delete student payable using stored procedure
     */
    private boolean deleteStudentPayableUsingProcedure(
            int studentId, Integer schoolYearId, String semester) {
        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.CallableStatement cstmt = conn.prepareCall("{CALL sp_delete_student_payable(?, ?, ?, ?)}")) {
            
            cstmt.setInt(1, studentId);
            cstmt.setObject(2, schoolYearId);
            cstmt.setString(3, semester);
            cstmt.registerOutParameter(4, java.sql.Types.VARCHAR);
            
            cstmt.executeUpdate();
            
            String result = cstmt.getString(4);
            return "DELETED".equals(result);
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting student payable from procedure", e);
        }
    }
    
    private int getOrCreateSemester(Connection conn, double firstSem, double secondSem, double summerSem) throws SQLException {
        // Try to find existing semester with matching amounts
        String findSql = "SELECT semester_id FROM semester WHERE " +
                        "ABS(first_sem_amount - ?) < 0.01 AND " +
                        "ABS(second_sem_amount - ?) < 0.01 AND " +
                        "ABS(summer_sem_amount - ?) < 0.01 " +
                        "LIMIT 1";
        
        try (PreparedStatement pstmt = conn.prepareStatement(findSql)) {
            pstmt.setDouble(1, firstSem);
            pstmt.setDouble(2, secondSem);
            pstmt.setDouble(3, summerSem);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("semester_id");
            }
        }
        
        // Create new semester record
        String insertSql = "INSERT INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setDouble(1, firstSem);
            pstmt.setDouble(2, secondSem);
            pstmt.setDouble(3, summerSem);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        throw new SQLException("Failed to create semester record");
    }
    
    private int getFirstSchoolYearId(Connection conn) throws SQLException {
        String sql = "SELECT school_year_id FROM school_year ORDER BY school_year_id DESC LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("school_year_id");
            }
        }
        return 1; // Default
    }
    
    private int getOrCreateBelong(Connection conn, int studentId, int schoolYearId, int semesterId) throws SQLException {
        // First, check if active belong record exists
        String checkActiveSql = "SELECT belong_id FROM belong WHERE student_id = ? AND school_year_id = ? AND semester_id = ? AND COALESCE(status, 'active') = 'active'";
        try (PreparedStatement pstmt = conn.prepareStatement(checkActiveSql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, schoolYearId);
            pstmt.setInt(3, semesterId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("belong_id");
            }
        }
        
        // If no active record, check for deactivated one
        String checkDeactivatedSql = "SELECT belong_id FROM belong WHERE student_id = ? AND school_year_id = ? AND semester_id = ? AND COALESCE(status, 'active') = 'deactivated'";
        try (PreparedStatement pstmt = conn.prepareStatement(checkDeactivatedSql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, schoolYearId);
            pstmt.setInt(3, semesterId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int belongId = rs.getInt("belong_id");
                // Reactivate the deactivated belong record
                String reactivateSql = "UPDATE belong SET status = 'active' WHERE belong_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(reactivateSql)) {
                    updateStmt.setInt(1, belongId);
                    updateStmt.executeUpdate();
                }
                return belongId;
            }
        }
        
        // Create new belong record if none exists
        String insertSql = "INSERT INTO belong (student_id, school_year_id, semester_id, status) VALUES (?, ?, ?, 'active')";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, schoolYearId);
            pstmt.setInt(3, semesterId);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        throw new SQLException("Failed to create belong record");
    }
    
    private void updateOrInsertPayable(Connection conn, int belongId, double amount) throws SQLException {
        // First, get student_id, school_year_id, and semester_id from belong record
        int studentId = -1;
        int schoolYearId = -1;
        int semesterId = -1;
        String getBelongSql = "SELECT student_id, school_year_id, semester_id FROM belong WHERE belong_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(getBelongSql)) {
            pstmt.setInt(1, belongId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                studentId = rs.getInt("student_id");
                schoolYearId = rs.getInt("school_year_id");
                semesterId = rs.getInt("semester_id");
            }
        }
        
        // Check if payable exists for this student/school_year/semester combination
        // This ensures we update existing payable instead of creating duplicate
        int payableId = -1;
        String checkSql = "SELECT sp.payable_id, sp.belong_id FROM student_payables sp " +
                         "INNER JOIN belong b ON sp.belong_id = b.belong_id " +
                         "WHERE b.student_id = ? AND b.school_year_id = ? AND b.semester_id = ? " +
                         "AND COALESCE(b.status, 'active') = 'active' " +
                         "LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, schoolYearId);
            pstmt.setInt(3, semesterId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                payableId = rs.getInt("payable_id");
                // Use the belong_id from the existing payable to ensure consistency
                belongId = rs.getInt("belong_id");
            }
        }
        
        // If still not found, check by belong_id directly (fallback)
        if (payableId == -1) {
            String checkByBelongSql = "SELECT payable_id FROM student_payables WHERE belong_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkByBelongSql)) {
                pstmt.setInt(1, belongId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    payableId = rs.getInt("payable_id");
                }
            }
        }
        
        if (payableId != -1) {
            // Update existing payable - regenerate due date (2 months from today)
            LocalDate dueDate = LocalDate.now().plusMonths(2);
            
            // Get existing due date ID or create new one
            String getDuedateSql = "SELECT duedate_id FROM student_payables WHERE payable_id = ?";
            int duedateId = -1;
            try (PreparedStatement getStmt = conn.prepareStatement(getDuedateSql)) {
                getStmt.setInt(1, payableId);
                ResultSet rs = getStmt.executeQuery();
                if (rs.next()) {
                    duedateId = rs.getInt("duedate_id");
                }
            }
            
            // Update or create due date
            if (duedateId > 0) {
                // Update existing due date
                String updateDuedateSql = "UPDATE duedate SET due_date = ? WHERE duedate_id = ?";
                try (PreparedStatement duedateStmt = conn.prepareStatement(updateDuedateSql)) {
                    duedateStmt.setDate(1, java.sql.Date.valueOf(dueDate));
                    duedateStmt.setInt(2, duedateId);
                    duedateStmt.executeUpdate();
                }
            } else {
                // Create new due date
                String insertDuedateSql = "INSERT INTO duedate (due_date) VALUES (?)";
                try (PreparedStatement duedateStmt = conn.prepareStatement(insertDuedateSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    duedateStmt.setDate(1, java.sql.Date.valueOf(dueDate));
                    duedateStmt.executeUpdate();
                    ResultSet duedateRs = duedateStmt.getGeneratedKeys();
                    if (duedateRs.next()) {
                        duedateId = duedateRs.getInt(1);
                    }
                }
            }
            
            // Update payable with new amount and due date
            String updateSql = "UPDATE student_payables SET downpayment_amount = ?, remaining_balance = ?, duedate_id = ? WHERE payable_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, amount);
                updateStmt.setDouble(2, amount); // Initially, remaining balance = downpayment amount
                updateStmt.setInt(3, duedateId);
                updateStmt.setInt(4, payableId);
                updateStmt.executeUpdate();
            }
        } else {
            // Insert new - create due date (2 months from today)
            LocalDate dueDate = LocalDate.now().plusMonths(2);
            
            // Create due date record
            String insertDuedateSql = "INSERT INTO duedate (due_date) VALUES (?)";
            int duedateId = -1;
            try (PreparedStatement duedateStmt = conn.prepareStatement(insertDuedateSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                duedateStmt.setDate(1, java.sql.Date.valueOf(dueDate));
                duedateStmt.executeUpdate();
                ResultSet duedateRs = duedateStmt.getGeneratedKeys();
                if (duedateRs.next()) {
                    duedateId = duedateRs.getInt(1);
                }
            }
            
            // Insert new payable with due date
            String insertSql = "INSERT INTO student_payables (belong_id, downpayment_amount, amount_paid, remaining_balance, status, duedate_id) VALUES (?, ?, 0, ?, 'UNPAID', ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, belongId);
                insertStmt.setDouble(2, amount);
                insertStmt.setDouble(3, amount);
                insertStmt.setInt(4, duedateId);
                insertStmt.executeUpdate();
            }
        }
    }
}

