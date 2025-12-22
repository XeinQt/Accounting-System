package dao;

import models.Student;
import utils.DatabaseUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {
    
    /**
     * Check if a column exists in the student table
     */
    private boolean columnExists(String columnName) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, "student", columnName);
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
    
    public List<Student> getAllStudents() {
        return getAllStudents(null, null);
    }
    
    public List<Student> getAllStudents(Integer schoolYearId) {
        return getAllStudents(schoolYearId, null);
    }
    
    public List<Student> getAllStudents(Integer schoolYearId, String status) {
        return getAllStudents(schoolYearId, status, null);
    }
    
    /**
     * Get all students using stored procedure (more efficient)
     */
    public List<Student> getAllStudents(Integer schoolYearId, String status, String semester) {
        // Try using stored procedure first
        try {
            return getAllStudentsUsingProcedure(schoolYearId, status, semester, null, null, null);
        } catch (Exception e) {
            // Fallback to direct query
            System.err.println("Warning: Could not use sp_get_students procedure, falling back to direct query: " + e.getMessage());
        }
        
        List<Student> students = new ArrayList<>();
        String sql;
        List<Object> params = new ArrayList<>();
        
        if (schoolYearId != null) {
            // If semester is specified, use INNER JOIN to only get students with belong records for that semester
            // Otherwise use LEFT JOIN to include all students
            if (semester != null && !semester.trim().isEmpty()) {
                // Use LEFT JOIN to show all students, but filter by semester for those who have belong records
                // This shows: 1) Students enrolled in the selected semester, 2) Students without belong records yet
                sql = "SELECT DISTINCT s.*, COALESCE(b.school_year_id, s.school_year_id) as belong_school_year_id, " +
                      "COALESCE(b.status, 'active') as belong_status, " +
                      "CASE " +
                      "  WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                      "  WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                      "  WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                      "  ELSE '' " +
                      "END as semester_name " +
                      "FROM student s " +
                      "LEFT JOIN belong b ON s.student_id = b.student_id AND b.school_year_id = ? " +
                      "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                      "WHERE (b.school_year_id = ? OR (b.school_year_id IS NULL AND s.school_year_id = ?)) " +
                      "AND (b.belong_id IS NULL OR " + // Show students without belong records
                      "(" +
                      "CASE " +
                      "  WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                      "  WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                      "  WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                      "  ELSE '' " +
                      "END = ?))";
                params.add(schoolYearId);
                params.add(schoolYearId);
                params.add(schoolYearId);
                params.add(semester.trim());
                
                // Filter by belong.status (for semester-specific deactivation)
                if (status != null && !status.trim().isEmpty()) {
                    sql += " AND (b.belong_id IS NULL OR COALESCE(b.status, 'active') = ?)";
                    params.add(status.toLowerCase());
                } else {
                    // Default to active belong records if no status specified, but still show students without belong records
                    sql += " AND (b.belong_id IS NULL OR COALESCE(b.status, 'active') = 'active')";
                }
                
                // Group by student_id to prevent duplicates when multiple belong records exist
                sql += " GROUP BY s.student_id";
            } else {
                // Use LEFT JOIN when no semester filter to include all students
                sql = "SELECT DISTINCT s.*, COALESCE(b.school_year_id, s.school_year_id) as belong_school_year_id, " +
                      "COALESCE(b.status, 'active') as belong_status, " +
                      "CASE " +
                      "  WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                      "  WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                      "  WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                      "  ELSE '' " +
                      "END as semester_name " +
                      "FROM student s " +
                      "LEFT JOIN belong b ON s.student_id = b.student_id AND b.school_year_id = ? " +
                      "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                      "WHERE (b.school_year_id = ? OR (b.school_year_id IS NULL AND s.school_year_id = ?))";
                params.add(schoolYearId);
                params.add(schoolYearId);
                params.add(schoolYearId);
                
                // Filter by student status and belong status
                // Show students without belong records OR students with matching status
                if (status != null && !status.trim().isEmpty()) {
                    sql += " AND (b.belong_id IS NULL OR COALESCE(b.status, 'active') = ?)";
                    sql += " AND COALESCE(s.status, 'active') = ?";
                    params.add(status.toLowerCase());
                    params.add(status.toLowerCase());
                } else {
                    // Default to active records, but still show students without belong records
                    sql += " AND (b.belong_id IS NULL OR COALESCE(b.status, 'active') = 'active')";
                    sql += " AND COALESCE(s.status, 'active') = 'active'";
                }
                
                // Group by student_id to prevent duplicates when multiple belong records exist
                sql += " GROUP BY s.student_id";
            }
        } else {
            sql = "SELECT s.*, " +
                  "COALESCE(b.status, 'active') as belong_status, " +
                  "CASE " +
                  "  WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                  "  WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                  "  WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                  "  ELSE '' " +
                  "END as semester_name " +
                  "FROM student s " +
                  "LEFT JOIN belong b ON s.student_id = b.student_id " +
                  "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                  "WHERE 1=1";
            
            // Filter by belong.status
            if (status != null && !status.trim().isEmpty()) {
                sql += " AND COALESCE(b.status, 'active') = ?";
                params.add(status.toLowerCase());
            } else {
                // Default to active belong records
                sql += " AND COALESCE(b.status, 'active') = 'active'";
            }
            
            // Add semester filter in SQL if specified
            if (semester != null && !semester.trim().isEmpty()) {
                sql += " AND (" +
                       "CASE " +
                       "  WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                       "  WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                       "  WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                       "  ELSE '' " +
                       "END = ?)";
                params.add(semester.trim());
            }
            
            // Group by student_id to prevent duplicates when multiple belong records exist
            sql += " GROUP BY s.student_id";
        }
        
        sql += " ORDER BY s.student_id DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            // Use a Set to track student IDs we've already added to prevent duplicates
            java.util.Set<Integer> addedStudentIds = new java.util.HashSet<>();
            while (rs.next()) {
                Student student = mapResultSetToStudent(rs);
                // Only add if we haven't seen this student_id before
                if (!addedStudentIds.contains(student.getStudentId())) {
                    students.add(student);
                    addedStudentIds.add(student.getStudentId());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting all students: " + e.getMessage());
            e.printStackTrace();
        }
        
        return students;
    }
    
    /**
     * Get all students using stored procedure
     */
    private List<Student> getAllStudentsUsingProcedure(
            Integer schoolYearId, String status, String semester, 
            String searchTerm, String year, String major) {
        List<Student> students = new ArrayList<>();
        // Use a Set to track student IDs we've already added to prevent duplicates
        java.util.Set<Integer> addedStudentIds = new java.util.HashSet<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.CallableStatement cstmt = conn.prepareCall("{CALL sp_get_students(?, ?, ?, ?, ?, ?)}")) {
            
            cstmt.setObject(1, schoolYearId);
            cstmt.setString(2, status);
            cstmt.setString(3, semester);
            cstmt.setString(4, searchTerm);
            cstmt.setString(5, year);
            cstmt.setString(6, major);
            
            ResultSet rs = cstmt.executeQuery();
            while (rs.next()) {
                int studentId = rs.getInt("student_id");
                
                // Only add if we haven't seen this student_id before
                if (!addedStudentIds.contains(studentId)) {
                    Student student = new Student();
                    student.setStudentId(studentId);
                    student.setStudentNumber(rs.getString("student_number"));
                    student.setFirstName(rs.getString("first_name"));
                    student.setMiddleName(rs.getString("middle_name"));
                    student.setLastName(rs.getString("last_name"));
                    // Fullname is auto-generated from first/middle/last name, so we don't set it directly
                    student.setMajor(rs.getString("major"));
                    student.setYear(rs.getString("year"));
                    student.setStatus(rs.getString("student_status"));
                    
                    // Set semester from result
                    String semesterName = rs.getString("semester_name");
                    if (semesterName != null && !semesterName.isEmpty()) {
                        student.setSemester(semesterName);
                    }
                    
                    // Set school year ID
                    Integer syId = rs.getInt("school_year_id");
                    if (!rs.wasNull()) {
                        student.setSchoolYearId(syId);
                    }
                    
                    students.add(student);
                    addedStudentIds.add(studentId);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting students from procedure", e);
        }
        
        return students;
    }
    
    /**
     * Add student using stored procedure
     */
    private boolean addStudentUsingProcedure(Student student) {
        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.CallableStatement cstmt = conn.prepareCall("{CALL sp_add_student(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            
            cstmt.setString(1, student.getStudentNumber());
            cstmt.setString(2, student.getFirstName());
            cstmt.setString(3, student.getMiddleName());
            cstmt.setString(4, student.getLastName());
            cstmt.setString(5, student.getMajor());
            cstmt.setString(6, student.getYear());
            cstmt.setInt(7, student.getSchoolYearId());
            cstmt.setString(8, student.getSemester());
            
            cstmt.registerOutParameter(9, java.sql.Types.INTEGER); // p_student_id
            cstmt.registerOutParameter(10, java.sql.Types.VARCHAR); // p_result
            
            cstmt.execute();
            
            String result = cstmt.getString(10);
            return "CREATED".equals(result) || "ENROLLED".equals(result) || "REACTIVATED".equals(result) || "EXISTS".equals(result);
        } catch (SQLException e) {
            throw new RuntimeException("Error adding student via procedure", e);
        }
    }
    
    /**
     * Update student using stored procedure
     */
    private boolean updateStudentUsingProcedure(Student student) {
        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.CallableStatement cstmt = conn.prepareCall("{CALL sp_update_student(?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            
            cstmt.setInt(1, student.getStudentId());
            cstmt.setString(2, student.getStudentNumber());
            cstmt.setString(3, student.getFirstName());
            cstmt.setString(4, student.getMiddleName());
            cstmt.setString(5, student.getLastName());
            cstmt.setString(6, student.getMajor());
            cstmt.setString(7, student.getYear());
            cstmt.setInt(8, student.getSchoolYearId());
            cstmt.setString(9, student.getSemester());
            
            cstmt.registerOutParameter(10, java.sql.Types.VARCHAR); // p_result
            
            cstmt.execute();
            
            String result = cstmt.getString(10);
            return "SUCCESS".equals(result);
        } catch (SQLException e) {
            throw new RuntimeException("Error updating student via procedure", e);
        }
    }
    
    /**
     * Deactivate student using stored procedure
     */
    private boolean deactivateStudentUsingProcedure(int studentId) {
        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.CallableStatement cstmt = conn.prepareCall("{CALL sp_deactivate_student(?, ?)}")) {
            
            cstmt.setInt(1, studentId);
            cstmt.registerOutParameter(2, java.sql.Types.VARCHAR); // p_result
            
            cstmt.execute();
            
            String result = cstmt.getString(2);
            return "SUCCESS".equals(result);
        } catch (SQLException e) {
            throw new RuntimeException("Error deactivating student via procedure", e);
        }
    }
    
    /**
     * Reactivate student using stored procedure
     */
    private boolean reactivateStudentUsingProcedure(int studentId) {
        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.CallableStatement cstmt = conn.prepareCall("{CALL sp_reactivate_student(?, ?)}")) {
            
            cstmt.setInt(1, studentId);
            cstmt.registerOutParameter(2, java.sql.Types.VARCHAR); // p_result
            
            cstmt.execute();
            
            String result = cstmt.getString(2);
            return "SUCCESS".equals(result);
        } catch (SQLException e) {
            throw new RuntimeException("Error reactivating student via procedure", e);
        }
    }
    
    public List<Student> getStudentsBySchoolYear(Integer schoolYearId) {
        return getStudentsBySchoolYear(schoolYearId, null);
    }
    
    public List<Student> getStudentsBySchoolYear(Integer schoolYearId, String status) {
        return getAllStudents(schoolYearId, status);
    }
    
    /**
     * Search students using stored procedure (more efficient)
     */
    public List<Student> searchStudents(String searchTerm, String year, String major) {
        return searchStudents(searchTerm, year, major, null, null);
    }
    
    public List<Student> searchStudents(String searchTerm, String year, String major, Integer schoolYearId) {
        return searchStudents(searchTerm, year, major, schoolYearId, null);
    }
    
    public List<Student> searchStudents(String searchTerm, String year, String major, Integer schoolYearId, String status) {
        return searchStudents(searchTerm, year, major, schoolYearId, status, null);
    }
    
    public List<Student> searchStudents(String searchTerm, String year, String major, Integer schoolYearId, String status, String semester) {
        List<Student> students = new ArrayList<>();
        StringBuilder sql;
        List<Object> params = new ArrayList<>();
        
        if (schoolYearId != null) {
            // If semester is specified, use INNER JOIN to only get students with belong records for that semester
            // Otherwise use LEFT JOIN to include all students
            if (semester != null && !semester.trim().isEmpty()) {
                // Use LEFT JOIN to show all students, but filter by semester for those who have belong records
                // This shows: 1) Students enrolled in the selected semester, 2) Students without belong records yet
                sql = new StringBuilder("SELECT DISTINCT s.*, COALESCE(b.school_year_id, s.school_year_id) as belong_school_year_id, " +
                        "COALESCE(b.status, 'active') as belong_status, " +
                        "CASE " +
                        "  WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                        "  WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                        "  WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                        "  ELSE '' " +
                        "END as semester_name " +
                        "FROM student s " +
                        "LEFT JOIN belong b ON s.student_id = b.student_id AND b.school_year_id = ? " +
                        "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                        "WHERE (b.school_year_id = ? OR (b.school_year_id IS NULL AND s.school_year_id = ?)) " +
                        "AND (b.belong_id IS NULL OR " + // Show students without belong records
                        "(" +
                        "CASE " +
                        "  WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                        "  WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                        "  WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                        "  ELSE '' " +
                        "END = ?))");
                params.add(schoolYearId);
                params.add(schoolYearId);
                params.add(schoolYearId);
                params.add(semester.trim());
                
                // Filter by belong.status (for semester-specific deactivation)
                if (status != null && !status.trim().isEmpty()) {
                    sql.append(" AND (b.belong_id IS NULL OR COALESCE(b.status, 'active') = ?)");
                    params.add(status.toLowerCase());
                } else {
                    // Default to active belong records if no status specified, but still show students without belong records
                    sql.append(" AND (b.belong_id IS NULL OR COALESCE(b.status, 'active') = 'active')");
                }
            } else {
                // Use LEFT JOIN when no semester filter to include all students
                sql = new StringBuilder("SELECT DISTINCT s.*, COALESCE(b.school_year_id, s.school_year_id) as belong_school_year_id, " +
                        "COALESCE(b.status, 'active') as belong_status, " +
                        "CASE " +
                        "  WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                        "  WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                        "  WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                        "  ELSE '' " +
                        "END as semester_name " +
                        "FROM student s " +
                        "LEFT JOIN belong b ON s.student_id = b.student_id AND b.school_year_id = ? " +
                        "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                        "WHERE (b.school_year_id = ? OR (b.school_year_id IS NULL AND s.school_year_id = ?))");
                params.add(schoolYearId);
                params.add(schoolYearId);
                params.add(schoolYearId);
                
                // Filter by belong.status (for semester-specific deactivation)
                if (status != null && !status.trim().isEmpty()) {
                    sql.append(" AND COALESCE(b.status, 'active') = ?");
                    params.add(status.toLowerCase());
                } else {
                    // Default to active belong records if no status specified
                    sql.append(" AND COALESCE(b.status, 'active') = 'active'");
                }
            }
        } else {
            sql = new StringBuilder("SELECT s.*, " +
                    "COALESCE(b.status, 'active') as belong_status, " +
                    "CASE " +
                    "  WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                    "  WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                    "  WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                    "  ELSE '' " +
                    "END as semester_name " +
                    "FROM student s " +
                    "LEFT JOIN belong b ON s.student_id = b.student_id " +
                    "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                    "WHERE 1=1");
            
            // Filter by belong.status
            if (status != null && !status.trim().isEmpty()) {
                sql.append(" AND COALESCE(b.status, 'active') = ?");
                params.add(status.toLowerCase());
            } else {
                // Default to active belong records
                sql.append(" AND COALESCE(b.status, 'active') = 'active'");
            }
        }
        
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Search across student_number and concatenated name fields
            sql.append(" AND (s.student_number LIKE ? OR " +
                      "CONCAT(COALESCE(s.first_name, ''), ' ', COALESCE(s.middle_name, ''), ' ', COALESCE(s.last_name, '')) LIKE ? OR " +
                      "COALESCE(s.fullname, '') LIKE ?)");
            String searchPattern = "%" + searchTerm + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (year != null && !year.trim().isEmpty()) {
            sql.append(" AND s.year = ?");
            params.add(year);
        }
        
        if (major != null && !major.trim().isEmpty()) {
            sql.append(" AND s.major LIKE ?");
            params.add("%" + major + "%");
        }
        
        // Add semester filter (only if not already filtered in WHERE clause above)
        // When semester is specified with schoolYearId, it's already filtered in the WHERE clause using INNER JOIN
        // So we only need to add it here if semester is specified but schoolYearId is null
        if (semester != null && !semester.trim().isEmpty() && schoolYearId == null) {
            sql.append(" AND (CASE " +
                      "  WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem' " +
                      "  WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem' " +
                      "  WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem' " +
                      "  ELSE '' " +
                      "END) = ?");
            params.add(semester);
        }
        // Note: When schoolYearId is not null and semester is specified, 
        // the semester filter is already applied in the WHERE clause above using INNER JOIN
        
        sql.append(" ORDER BY s.student_id DESC");
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Student student = mapResultSetToStudent(rs);
                students.add(student);
            }
        } catch (SQLException e) {
            System.err.println("Error searching students: " + e.getMessage());
            e.printStackTrace();
        }
        
        return students;
    }
    
    public Student getStudentById(int studentId) {
        String sql = "SELECT * FROM student WHERE student_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToStudent(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting student by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public Student getStudentByNumber(String studentNumber) {
        String sql = "SELECT * FROM student WHERE student_number = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, studentNumber);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToStudent(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting student by number: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Check if a student with the same student number AND full name in the same school year already exists
     * This allows the same student to be enrolled in multiple school years,
     * but prevents duplicate enrollment (same ID + same name) in the same school year
     */
    public boolean studentExistsInSchoolYear(String studentNumber, String fullname, Integer schoolYearId) {
        if (schoolYearId == null) {
            // If no school year specified, check by student number and full name
            // Support both old fullname column and new firstName/middleName/lastName columns
            String sql = "SELECT COUNT(*) as count FROM student WHERE student_number = ? " +
                        "AND (fullname = ? OR " +
                        "CONCAT(COALESCE(first_name, ''), ' ', COALESCE(middle_name, ''), ' ', COALESCE(last_name, '')) = ?)";
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, studentNumber);
                pstmt.setString(2, fullname);
                pstmt.setString(3, fullname);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            } catch (SQLException e) {
                System.err.println("Error checking if student exists: " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }
        
        // Check if student exists with same student_number AND fullname AND school_year_id
        // (via belong table or student.school_year_id)
        // Support both old fullname column and new firstName/middleName/lastName columns
        String sql = "SELECT COUNT(*) as count FROM student s " +
                    "WHERE s.student_number = ? " +
                    "AND (s.fullname = ? OR " +
                    "CONCAT(COALESCE(s.first_name, ''), ' ', COALESCE(s.middle_name, ''), ' ', COALESCE(s.last_name, '')) = ?) " +
                    "AND (" +
                    "    s.school_year_id = ? " +
                    "    OR EXISTS (" +
                    "        SELECT 1 FROM belong b " +
                    "        WHERE b.student_id = s.student_id " +
                    "        AND b.school_year_id = ?" +
                    "    )" +
                    ")";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, studentNumber);
            pstmt.setString(2, fullname);
            pstmt.setString(3, fullname);
            pstmt.setInt(4, schoolYearId);
            pstmt.setInt(5, schoolYearId);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking if student exists in school year: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Check if a student with the same full name in the same school year already exists.
     * This is a separate check from student number uniqueness.
     */
    public boolean studentNameExistsInSchoolYear(String fullname, Integer schoolYearId) {
        if (schoolYearId == null) {
            // If no school year specified, just check by full name
            String sql = "SELECT COUNT(*) as count FROM student WHERE " +
                        "(fullname = ? OR " +
                        "CONCAT(COALESCE(first_name, ''), ' ', COALESCE(middle_name, ''), ' ', COALESCE(last_name, '')) = ?)";
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fullname);
                pstmt.setString(2, fullname);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            } catch (SQLException e) {
                System.err.println("Error checking if student name exists: " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }

        // Check if student exists with same fullname AND school_year_id
        String sql = "SELECT COUNT(*) as count FROM student s " +
                    "WHERE (s.fullname = ? OR " +
                    "CONCAT(COALESCE(s.first_name, ''), ' ', COALESCE(s.middle_name, ''), ' ', COALESCE(s.last_name, '')) = ?) " +
                    "AND (" +
                    "    s.school_year_id = ? " +
                    "    OR EXISTS (" +
                    "        SELECT 1 FROM belong b " +
                    "        WHERE b.student_id = s.student_id " +
                    "        AND b.school_year_id = ?" +
                    "    )" +
                    ")";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fullname);
            pstmt.setString(2, fullname);
            pstmt.setInt(3, schoolYearId);
            pstmt.setInt(4, schoolYearId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking if student name exists in school year: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
    
    /**
     * Check if a student ID (student_number) already exists in the system
     * This prevents duplicate student IDs regardless of name
     */
    public boolean studentIdExists(String studentNumber) {
        if (studentNumber == null || studentNumber.trim().isEmpty()) {
            return false;
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT COUNT(*) as count FROM student WHERE student_number = ? AND COALESCE(status, 'active') = 'active'")) {
            
            pstmt.setString(1, studentNumber.trim());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking if student ID exists: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Check if a student ID is used by another student (different student_id)
     */
    public boolean studentIdExistsForAnother(String studentNumber, int currentStudentId) {
        if (studentNumber == null || studentNumber.trim().isEmpty()) {
            return false;
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT COUNT(*) as count FROM student WHERE student_number = ? AND student_id <> ? AND COALESCE(status, 'active') = 'active'")) {
            
            pstmt.setString(1, studentNumber.trim());
            pstmt.setInt(2, currentStudentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking if student ID exists for another student: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Check if a full name (first, middle, last) already belongs to a different student.
     */
    public boolean fullNameExistsForAnother(String firstName, String middleName, String lastName, int currentStudentId) {
        StringBuilder fullNameBuilder = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) fullNameBuilder.append(firstName.trim());
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
            fullNameBuilder.append(middleName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
            fullNameBuilder.append(lastName.trim());
        }
        String fullname = fullNameBuilder.toString().trim();
        if (fullname.isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) as count FROM student s " +
                     "WHERE s.student_id <> ? AND COALESCE(s.status, 'active') = 'active' AND " +
                     "(s.fullname = ? OR " +
                     "CONCAT(COALESCE(s.first_name, ''), ' ', COALESCE(s.middle_name, ''), ' ', COALESCE(s.last_name, '')) = ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentStudentId);
            pstmt.setString(2, fullname);
            pstmt.setString(3, fullname);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking if full name exists for another student: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Check if a student with the same first name, middle name, and last name
     * already exists for the same school year and semester
     */
    public boolean fullNameExistsForSchoolYearAndSemester(String firstName, String middleName, String lastName, 
                                                          Integer schoolYearId, String semester) {
        if (schoolYearId == null || semester == null || semester.trim().isEmpty()) {
            return false; // Can't check without school year and semester
        }
        
        // Build full name for comparison
        StringBuilder fullNameBuilder = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) fullNameBuilder.append(firstName.trim());
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
            fullNameBuilder.append(middleName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
            fullNameBuilder.append(lastName.trim());
        }
        String fullname = fullNameBuilder.toString().trim();
        if (fullname.isEmpty()) {
            return false;
        }
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            // First, find the semester_id that matches the semester name
            int semesterId = getSemesterIdBySemesterName(conn, semester);
            if (semesterId == -1) {
                return false; // Can't find semester
            }
            
            // Check if student exists with same name in the same school year and semester
            String sql = "SELECT COUNT(*) as count FROM student s " +
                        "INNER JOIN belong b ON s.student_id = b.student_id " +
                        "WHERE b.school_year_id = ? " +
                        "AND b.semester_id = ? " +
                        "AND COALESCE(b.status, 'active') = 'active' " +
                        "AND COALESCE(s.status, 'active') = 'active' " +
                        "AND (s.fullname = ? OR " +
                        "CONCAT(COALESCE(s.first_name, ''), ' ', COALESCE(s.middle_name, ''), ' ', COALESCE(s.last_name, '')) = ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, schoolYearId);
                pstmt.setInt(2, semesterId);
                pstmt.setString(3, fullname);
                pstmt.setString(4, fullname);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if full name exists for school year and semester: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Check if a student ID already exists in a specific semester and school year
     * This prevents duplicate enrollments of the same student ID in the same semester and school year
     */
    public boolean studentIdExistsInSemesterAndSchoolYear(String studentNumber, Integer schoolYearId, String semester) {
        if (studentNumber == null || studentNumber.trim().isEmpty() || schoolYearId == null || semester == null || semester.trim().isEmpty()) {
            return false;
        }
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            // First, find the semester_id that matches the semester name
            int semesterId = getSemesterIdBySemesterName(conn, semester);
            if (semesterId == -1) {
                return false; // Can't find semester
            }
            
            // Check if student with this ID exists in the same semester and school year
            String sql = "SELECT COUNT(*) as count FROM student s " +
                        "INNER JOIN belong b ON s.student_id = b.student_id " +
                        "WHERE s.student_number = ? " +
                        "AND b.school_year_id = ? " +
                        "AND b.semester_id = ? " +
                        "AND COALESCE(s.status, 'active') = 'active' " +
                        "AND COALESCE(b.status, 'active') = 'active'";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, studentNumber.trim());
                pstmt.setInt(2, schoolYearId);
                pstmt.setInt(3, semesterId);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if student ID exists in semester and school year: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Check if a student ID already exists in a specific semester and school year for another student
     * This is used during update to prevent changing to an ID that's already used by another student
     */
    public boolean studentIdExistsInSemesterAndSchoolYearForAnother(String studentNumber, Integer schoolYearId, String semester, int currentStudentId) {
        if (studentNumber == null || studentNumber.trim().isEmpty() || schoolYearId == null || semester == null || semester.trim().isEmpty()) {
            return false;
        }
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            // First, find the semester_id that matches the semester name
            int semesterId = getSemesterIdBySemesterName(conn, semester);
            if (semesterId == -1) {
                return false; // Can't find semester
            }
            
            // Check if student with this ID exists in the same semester and school year, excluding current student
            String sql = "SELECT COUNT(*) as count FROM student s " +
                        "INNER JOIN belong b ON s.student_id = b.student_id " +
                        "WHERE s.student_number = ? " +
                        "AND s.student_id <> ? " +
                        "AND b.school_year_id = ? " +
                        "AND b.semester_id = ? " +
                        "AND COALESCE(s.status, 'active') = 'active' " +
                        "AND COALESCE(b.status, 'active') = 'active'";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, studentNumber.trim());
                pstmt.setInt(2, currentStudentId);
                pstmt.setInt(3, schoolYearId);
                pstmt.setInt(4, semesterId);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if student ID exists in semester and school year for another student: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Get student information by student ID in a specific semester and school year
     */
    public Student getStudentByStudentIdInSemesterAndSchoolYear(String studentNumber, Integer schoolYearId, String semester) {
        if (studentNumber == null || studentNumber.trim().isEmpty() || schoolYearId == null || semester == null || semester.trim().isEmpty()) {
            return null;
        }
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            // First, find the semester_id that matches the semester name
            int semesterId = getSemesterIdBySemesterName(conn, semester);
            if (semesterId == -1) {
                return null; // Can't find semester
            }
            
            String sql = "SELECT s.* FROM student s " +
                        "INNER JOIN belong b ON s.student_id = b.student_id " +
                        "WHERE s.student_number = ? " +
                        "AND b.school_year_id = ? " +
                        "AND b.semester_id = ? " +
                        "AND COALESCE(s.status, 'active') = 'active' " +
                        "AND COALESCE(b.status, 'active') = 'active' " +
                        "LIMIT 1";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, studentNumber.trim());
                pstmt.setInt(2, schoolYearId);
                pstmt.setInt(3, semesterId);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return mapResultSetToStudent(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting student by student ID in semester and school year: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Get student by student ID (student_number) only
     */
    public Student getStudentByStudentId(String studentNumber) {
        if (studentNumber == null || studentNumber.trim().isEmpty()) {
            return null;
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM student WHERE student_number = ? AND COALESCE(status, 'active') = 'active' LIMIT 1")) {
            
            pstmt.setString(1, studentNumber.trim());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToStudent(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting student by student ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Check if a student with the same full name and ID number already exists in ANY course/program
     * This prevents adding the same student to different courses
     */
    public boolean studentExistsByFullNameAndId(String studentNumber, String firstName, String middleName, String lastName) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Build full name for comparison
            StringBuilder fullNameBuilder = new StringBuilder();
            if (firstName != null && !firstName.trim().isEmpty()) fullNameBuilder.append(firstName.trim());
            if (middleName != null && !middleName.trim().isEmpty()) {
                if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                fullNameBuilder.append(middleName.trim());
            }
            if (lastName != null && !lastName.trim().isEmpty()) {
                if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                fullNameBuilder.append(lastName.trim());
            }
            String fullname = fullNameBuilder.toString().trim();
            
            if (fullname.isEmpty() || studentNumber == null || studentNumber.trim().isEmpty()) {
                return false; // Can't check without name and ID
            }
            
            // Check if student exists with same student_number AND fullname (regardless of course/program)
            String sql = "SELECT COUNT(*) as count FROM student s " +
                        "WHERE s.student_number = ? " +
                        "AND (s.fullname = ? OR " +
                        "CONCAT(COALESCE(s.first_name, ''), ' ', COALESCE(s.middle_name, ''), ' ', COALESCE(s.last_name, '')) = ?) " +
                        "AND COALESCE(s.status, 'active') = 'active'";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, studentNumber.trim());
                pstmt.setString(2, fullname);
                pstmt.setString(3, fullname);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if student exists by full name and ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Get existing student information by full name and ID number
     * Returns the student's current course/program information
     */
    public Student getStudentByFullNameAndId(String studentNumber, String firstName, String middleName, String lastName) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Build full name for comparison
            StringBuilder fullNameBuilder = new StringBuilder();
            if (firstName != null && !firstName.trim().isEmpty()) fullNameBuilder.append(firstName.trim());
            if (middleName != null && !middleName.trim().isEmpty()) {
                if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                fullNameBuilder.append(middleName.trim());
            }
            if (lastName != null && !lastName.trim().isEmpty()) {
                if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                fullNameBuilder.append(lastName.trim());
            }
            String fullname = fullNameBuilder.toString().trim();
            
            if (fullname.isEmpty() || studentNumber == null || studentNumber.trim().isEmpty()) {
                return null;
            }
            
            String sql = "SELECT * FROM student s " +
                        "WHERE s.student_number = ? " +
                        "AND (s.fullname = ? OR " +
                        "CONCAT(COALESCE(s.first_name, ''), ' ', COALESCE(s.middle_name, ''), ' ', COALESCE(s.last_name, '')) = ?) " +
                        "AND COALESCE(s.status, 'active') = 'active' " +
                        "LIMIT 1";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, studentNumber.trim());
                pstmt.setString(2, fullname);
                pstmt.setString(3, fullname);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return mapResultSetToStudent(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting student by full name and ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Check if a student with the same information (ID, name), year, program (major), and semester already exists
     * This prevents duplicate enrollments for the same student in the same year, program, and semester
     */
    public boolean studentExistsWithYearProgramSemester(String studentNumber, String firstName, String middleName, String lastName,
                                                         String year, String major, Integer schoolYearId, String semester) {
        if (schoolYearId == null || semester == null || semester.trim().isEmpty()) {
            return false; // Can't check without school year and semester
        }
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            // First, find the semester_id that matches the semester name
            int semesterId = getSemesterIdBySemesterName(conn, semester);
            if (semesterId == -1) {
                return false; // Can't find semester
            }
            
            // Build full name for comparison
            StringBuilder fullNameBuilder = new StringBuilder();
            if (firstName != null && !firstName.trim().isEmpty()) fullNameBuilder.append(firstName.trim());
            if (middleName != null && !middleName.trim().isEmpty()) {
                if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                fullNameBuilder.append(middleName.trim());
            }
            if (lastName != null && !lastName.trim().isEmpty()) {
                if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                fullNameBuilder.append(lastName.trim());
            }
            String fullname = fullNameBuilder.toString().trim();
            
            // Check if student exists with same student_number, name, year, major, school_year_id, and semester_id
            String sql = "SELECT COUNT(*) as count FROM student s " +
                        "INNER JOIN belong b ON s.student_id = b.student_id " +
                        "WHERE s.student_number = ? " +
                        "AND (s.fullname = ? OR " +
                        "CONCAT(COALESCE(s.first_name, ''), ' ', COALESCE(s.middle_name, ''), ' ', COALESCE(s.last_name, '')) = ?) " +
                        "AND COALESCE(s.year, '') = ? " +
                        "AND COALESCE(s.major, '') = ? " +
                        "AND b.school_year_id = ? " +
                        "AND b.semester_id = ? " +
                        "AND COALESCE(b.status, 'active') = 'active'";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, studentNumber);
                pstmt.setString(2, fullname);
                pstmt.setString(3, fullname);
                pstmt.setString(4, year != null ? year : "");
                pstmt.setString(5, major != null ? major : "");
                pstmt.setInt(6, schoolYearId);
                pstmt.setInt(7, semesterId);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if student exists with year, program, and semester: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Check if a student with the same information exists but is deactivated
     * Returns true if exists and deactivated, false otherwise
     */
    public boolean studentExistsDeactivated(String studentNumber, String firstName, String middleName, String lastName,
                                            String year, String major, Integer schoolYearId, String semester) {
        if (schoolYearId == null || semester == null || semester.trim().isEmpty()) {
            return false;
        }
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            // First, find the semester_id that matches the semester name
            int semesterId = getSemesterIdBySemesterName(conn, semester);
            if (semesterId == -1) {
                return false; // Can't find semester
            }
            
            // Build full name for comparison
            StringBuilder fullNameBuilder = new StringBuilder();
            if (firstName != null && !firstName.trim().isEmpty()) fullNameBuilder.append(firstName.trim());
            if (middleName != null && !middleName.trim().isEmpty()) {
                if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                fullNameBuilder.append(middleName.trim());
            }
            if (lastName != null && !lastName.trim().isEmpty()) {
                if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                fullNameBuilder.append(lastName.trim());
            }
            String fullname = fullNameBuilder.toString().trim();
            
            // Check if student exists with same info but deactivated
            String sql = "SELECT COUNT(*) as count FROM student s " +
                        "INNER JOIN belong b ON s.student_id = b.student_id " +
                        "WHERE s.student_number = ? " +
                        "AND (s.fullname = ? OR " +
                        "CONCAT(COALESCE(s.first_name, ''), ' ', COALESCE(s.middle_name, ''), ' ', COALESCE(s.last_name, '')) = ?) " +
                        "AND COALESCE(s.year, '') = ? " +
                        "AND COALESCE(s.major, '') = ? " +
                        "AND b.school_year_id = ? " +
                        "AND b.semester_id = ? " +
                        "AND COALESCE(b.status, 'active') = 'deactivated'";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, studentNumber);
                pstmt.setString(2, fullname);
                pstmt.setString(3, fullname);
                pstmt.setString(4, year != null ? year : "");
                pstmt.setString(5, major != null ? major : "");
                pstmt.setInt(6, schoolYearId);
                pstmt.setInt(7, semesterId);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if student exists deactivated: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Add student using stored procedure (more efficient)
     */
    public boolean addStudent(Student student) {
        // Try using stored procedure first
        try {
            return addStudentUsingProcedure(student);
        } catch (Exception e) {
            // Fallback to direct query
            System.err.println("Warning: Could not use sp_add_student procedure, falling back to direct query: " + e.getMessage());
        }
        
        // Support both old schema (fullname, dep, college) and new schema (first_name, middle_name, last_name)
        // Build SQL dynamically based on which columns exist
        boolean hasStatusColumn = columnExists("status");
        boolean hasNameColumns = columnExists("first_name");
        
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO student (student_number, fullname, major, year, school_year_id");
        if (hasNameColumns) {
            sqlBuilder.append(", first_name, middle_name, last_name");
        }
        if (hasStatusColumn) {
            sqlBuilder.append(", status");
        }
        sqlBuilder.append(") VALUES (?, ?, ?, ?, ?");
        if (hasNameColumns) {
            sqlBuilder.append(", ?, ?, ?");
        }
        if (hasStatusColumn) {
            sqlBuilder.append(", ?");
        }
        sqlBuilder.append(")");
        
        String sql = sqlBuilder.toString();
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            
            try {
                int studentId;
                
                // Check if student with this student_number already exists
                String checkSql = "SELECT student_id FROM student WHERE student_number = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, student.getStudentNumber());
                    ResultSet checkRs = checkStmt.executeQuery();
                    
                    if (checkRs.next()) {
                        // Student already exists; allow adding a new enrollment (different semester/year)
                        studentId = checkRs.getInt("student_id");

                        // Ensure school year and semester are provided for the new enrollment
                        if (student.getSchoolYearId() == null || student.getSemester() == null || student.getSemester().trim().isEmpty()) {
                        conn.rollback();
                            System.err.println("Error: School year or semester missing for additional enrollment.");
                            return false;
                        }

                        int semesterId = getOrCreateSemester(conn, student.getSemester());

                        // Check if belong record for this year + semester already exists (active or deactivated)
                        String existBelongSql = "SELECT belong_id, COALESCE(status, 'active') as status FROM belong WHERE student_id = ? AND school_year_id = ? AND semester_id = ? LIMIT 1";
                        try (PreparedStatement existBelongStmt = conn.prepareStatement(existBelongSql)) {
                            existBelongStmt.setInt(1, studentId);
                            existBelongStmt.setInt(2, student.getSchoolYearId());
                            existBelongStmt.setInt(3, semesterId);
                            ResultSet existBelongRs = existBelongStmt.executeQuery();
                            if (existBelongRs.next()) {
                                String status = existBelongRs.getString("status");
                                if ("active".equalsIgnoreCase(status)) {
                                    // Already active in this year+semester -> block
                                    conn.rollback();
                                    System.err.println("Error: Duplicate enrollment for same school year and semester.");
                        return false;
                                } else {
                                    // Reactivate existing belong record
                                    int belongId = existBelongRs.getInt("belong_id");
                                    String reactivateSql = "UPDATE belong SET status = 'active' WHERE belong_id = ?";
                                    try (PreparedStatement reactivateStmt = conn.prepareStatement(reactivateSql)) {
                                        reactivateStmt.setInt(1, belongId);
                                        reactivateStmt.executeUpdate();
                                    }
                                    conn.commit();
                                    return true;
                                }
                            }
                        }

                        // Create new belong record for this year + semester
                        String belongSql = "INSERT INTO belong (student_id, school_year_id, semester_id, status) VALUES (?, ?, ?, 'active')";
                        try (PreparedStatement belongStmt = conn.prepareStatement(belongSql)) {
                            belongStmt.setInt(1, studentId);
                            belongStmt.setInt(2, student.getSchoolYearId());
                            belongStmt.setInt(3, semesterId);
                            belongStmt.executeUpdate();
                        }

                        conn.commit();
                        return true;
                    } else {
                        // Student doesn't exist, create new student record
                        PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
                        
                        int paramIndex = 1;
                        pstmt.setString(paramIndex++, student.getStudentNumber());
                        // Also store fullname for backward compatibility
                        pstmt.setString(paramIndex++, student.getFullname());
                        pstmt.setString(paramIndex++, student.getMajor());
                        pstmt.setString(paramIndex++, student.getYear());
                        if (student.getSchoolYearId() != null) {
                            pstmt.setInt(paramIndex++, student.getSchoolYearId());
                        } else {
                            pstmt.setNull(paramIndex++, java.sql.Types.INTEGER);
                        }
                        
                        if (hasNameColumns) {
                            pstmt.setString(paramIndex++, student.getFirstName());
                            pstmt.setString(paramIndex++, student.getMiddleName());
                            pstmt.setString(paramIndex++, student.getLastName());
                        }
                        
                        if (hasStatusColumn) {
                            // Set status (default to "active" if not set)
                            String status = student.getStatus() != null && !student.getStatus().trim().isEmpty() ? 
                                           student.getStatus() : "active";
                            pstmt.setString(paramIndex++, status);
                        }
                        
                        int rowsAffected = pstmt.executeUpdate();
                        if (rowsAffected == 0) {
                            conn.rollback();
                            return false;
                        }
                        
                        // Get generated student_id
                        ResultSet rs = pstmt.getGeneratedKeys();
                        if (rs.next()) {
                            studentId = rs.getInt(1);
                        } else {
                            conn.rollback();
                            return false;
                        }
                        pstmt.close();
                    }
                }
                
                // If school year is set, create belong record (or check if it already exists)
                if (student.getSchoolYearId() != null) {
                    // Get or create a semester based on the student's semester selection
                    String semester = student.getSemester();
                    int semesterId = getOrCreateSemester(conn, semester);
                    
                    // Check if belong record already exists for this student, school year, and semester
                    String checkBelongSql = "SELECT belong_id FROM belong WHERE student_id = ? AND school_year_id = ? AND semester_id = ?";
                    try (PreparedStatement checkBelongStmt = conn.prepareStatement(checkBelongSql)) {
                        checkBelongStmt.setInt(1, studentId);
                        checkBelongStmt.setInt(2, student.getSchoolYearId());
                        checkBelongStmt.setInt(3, semesterId);
                        ResultSet belongRs = checkBelongStmt.executeQuery();
                        
                        if (!belongRs.next()) {
                            // Belong record doesn't exist, create it
                            String belongSql = "INSERT INTO belong (student_id, school_year_id, semester_id, status) VALUES (?, ?, ?, 'active')";
                            PreparedStatement belongStmt = conn.prepareStatement(belongSql);
                            belongStmt.setInt(1, studentId);
                            belongStmt.setInt(2, student.getSchoolYearId());
                            belongStmt.setInt(3, semesterId);
                            belongStmt.executeUpdate();
                            belongStmt.close();
                        }
                        // If belong record already exists, do nothing (it's already there)
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
            System.err.println("Error adding student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get or create a semester based on the semester name (1st Sem, 2nd Sem, Summer Sem)
     * Creates a semester record with the appropriate amount set (> 0) for the selected semester
     */
    private int getOrCreateSemester(Connection conn, String semester) throws SQLException {
        if (semester == null || semester.trim().isEmpty()) {
            // If no semester specified, use default (all zeros)
            return getOrCreateDefaultSemester(conn);
        }
        
        // Determine which amount should be set based on semester name
        double firstSemAmount = 0.0;
        double secondSemAmount = 0.0;
        double summerSemAmount = 0.0;
        
        if (semester.equals("1st Sem")) {
            firstSemAmount = 1.0; // Set to 1.0 to indicate this is 1st Sem
        } else if (semester.equals("2nd Sem")) {
            secondSemAmount = 1.0; // Set to 1.0 to indicate this is 2nd Sem
        } else if (semester.equals("Summer Sem")) {
            summerSemAmount = 1.0; // Set to 1.0 to indicate this is Summer Sem
        } else {
            // Unknown semester, use default
            return getOrCreateDefaultSemester(conn);
        }
        
        // Try to find an existing semester with matching amounts
        String findSql = "SELECT semester_id FROM semester WHERE " +
                        "first_sem_amount = ? AND second_sem_amount = ? AND summer_sem_amount = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(findSql)) {
            pstmt.setDouble(1, firstSemAmount);
            pstmt.setDouble(2, secondSemAmount);
            pstmt.setDouble(3, summerSemAmount);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("semester_id");
            }
        }
        
        // Create a new semester with the appropriate amount
        String insertSql = "INSERT INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setDouble(1, firstSemAmount);
            pstmt.setDouble(2, secondSemAmount);
            pstmt.setDouble(3, summerSemAmount);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        throw new SQLException("Failed to get or create semester");
    }
    
    /**
     * Get or create a default semester with zero amounts
     */
    private int getOrCreateDefaultSemester(Connection conn) throws SQLException {
        // Try to find an existing semester with all zero amounts
        String findSql = "SELECT semester_id FROM semester WHERE first_sem_amount = 0 AND second_sem_amount = 0 AND summer_sem_amount = 0 LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(findSql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("semester_id");
            }
        }
        
        // Create a new default semester
        String insertSql = "INSERT INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount) VALUES (0, 0, 0)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        throw new SQLException("Failed to get or create default semester");
    }
    
    /**
     * Update student using stored procedure (more efficient)
     */
    public boolean updateStudent(Student student) {
        // Try using stored procedure first
        try {
            return updateStudentUsingProcedure(student);
        } catch (Exception e) {
            // Fallback to direct query
            System.err.println("Warning: Could not use sp_update_student procedure, falling back to direct query: " + e.getMessage());
        }
        
        // Support both old schema (fullname, dep, college) and new schema (first_name, middle_name, last_name)
        // Build SQL dynamically based on which columns exist
        boolean hasStatusColumn = columnExists("status");
        boolean hasNameColumns = columnExists("first_name");
        
        StringBuilder sqlBuilder = new StringBuilder("UPDATE student SET student_number = ?, fullname = ?, major = ?, year = ?, school_year_id = ?");
        if (hasNameColumns) {
            sqlBuilder.append(", first_name = ?, middle_name = ?, last_name = ?");
        }
        if (hasStatusColumn) {
            sqlBuilder.append(", status = ?");
        }
        sqlBuilder.append(" WHERE student_id = ?");
        
        String sql = sqlBuilder.toString();
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            
            try {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                
                int paramIndex = 1;
                pstmt.setString(paramIndex++, student.getStudentNumber());
                // Also update fullname for backward compatibility
                pstmt.setString(paramIndex++, student.getFullname());
                pstmt.setString(paramIndex++, student.getMajor());
                pstmt.setString(paramIndex++, student.getYear());
                if (student.getSchoolYearId() != null) {
                    pstmt.setInt(paramIndex++, student.getSchoolYearId());
                } else {
                    pstmt.setNull(paramIndex++, java.sql.Types.INTEGER);
                }
                
                if (hasNameColumns) {
                    pstmt.setString(paramIndex++, student.getFirstName());
                    pstmt.setString(paramIndex++, student.getMiddleName());
                    pstmt.setString(paramIndex++, student.getLastName());
                }
                
                if (hasStatusColumn) {
                    // Update status (preserve existing if not set, default to "active")
                    String status = student.getStatus() != null && !student.getStatus().trim().isEmpty() ? 
                                   student.getStatus() : "active";
                    pstmt.setString(paramIndex++, status);
                }
                
                pstmt.setInt(paramIndex++, student.getStudentId());
                
                int rowsAffected = pstmt.executeUpdate();
                pstmt.close();
                
                if (rowsAffected == 0) {
                    conn.rollback();
                    return false;
                }
                
                // If school year is set, ensure belong record exists with correct semester
                if (student.getSchoolYearId() != null) {
                    ensureBelongRecord(conn, student.getStudentId(), student.getSchoolYearId(), student.getSemester());
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
            System.err.println("Error updating student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Ensure a belong record exists for the student and school year with the correct semester
     * Creates one if it doesn't exist, or updates the semester if it does exist
     */
    private void ensureBelongRecord(Connection conn, int studentId, int schoolYearId, String semester) throws SQLException {
        // Get or create the appropriate semester
        int semesterId = getOrCreateSemester(conn, semester);
        
        // Check if belong record already exists for this student and school year
        String checkSql = "SELECT belong_id FROM belong WHERE student_id = ? AND school_year_id = ? LIMIT 1";
        boolean updatedExisting = false;
        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, schoolYearId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // Belong record already exists, update the semester_id and reactivate it
                int belongId = rs.getInt("belong_id");
                String updateSql = "UPDATE belong SET semester_id = ?, status = 'active' WHERE belong_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, semesterId);
                    updateStmt.setInt(2, belongId);
                    updateStmt.executeUpdate();
                }
                updatedExisting = true;
            }
        }
        
        if (!updatedExisting) {
        // Create belong record with active status
        String insertSql = "INSERT INTO belong (student_id, school_year_id, semester_id, status) VALUES (?, ?, ?, 'active')";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, schoolYearId);
            pstmt.setInt(3, semesterId);
                pstmt.executeUpdate();
            }
        }
        
        // Deactivate other semester enrollments for the same student and school year
        deactivateOtherBelongRecords(conn, studentId, schoolYearId, semesterId);
    }

    /**
     * Deactivate other belong records for the same student and school year, keeping only the specified semester active.
     */
    private void deactivateOtherBelongRecords(Connection conn, int studentId, int schoolYearId, int keepSemesterId) throws SQLException {
        String deactivateSql = "UPDATE belong SET status = 'deactivated' " +
                               "WHERE student_id = ? AND school_year_id = ? AND semester_id <> ? AND COALESCE(status, 'active') = 'active'";
        try (PreparedStatement pstmt = conn.prepareStatement(deactivateSql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, schoolYearId);
            pstmt.setInt(3, keepSemesterId);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Create belong records for all existing students that don't have them
     * This is a utility method to sync existing data
     */
    public int syncBelongRecordsForExistingStudents() {
        int count = 0;
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Get all students with school_year_id but no belong record
                String sql = "SELECT s.student_id, s.school_year_id " +
                            "FROM student s " +
                            "WHERE s.school_year_id IS NOT NULL " +
                            "AND NOT EXISTS (" +
                            "    SELECT 1 FROM belong b " +
                            "    WHERE b.student_id = s.student_id " +
                            "    AND b.school_year_id = s.school_year_id" +
                            ")";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    
                    int semesterId = getOrCreateDefaultSemester(conn);
                    
                    while (rs.next()) {
                        int studentId = rs.getInt("student_id");
                        int schoolYearId = rs.getInt("school_year_id");
                        
                        // Create belong record with active status
                        String insertSql = "INSERT INTO belong (student_id, school_year_id, semester_id, status) VALUES (?, ?, ?, 'active')";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, studentId);
                            insertStmt.setInt(2, schoolYearId);
                            insertStmt.setInt(3, semesterId);
                            insertStmt.executeUpdate();
                            count++;
                        }
                    }
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error syncing belong records: " + e.getMessage());
            e.printStackTrace();
        }
        
        return count;
    }
    
    /**
     * Deactivate a specific belong record (student + school year + semester combination)
     * This deactivates only the specific semester enrollment, not all enrollments for the student
     */
    public boolean deactivateBelongRecord(int studentId, int schoolYearId, String semester) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Find the semester_id by joining belong with semester table and matching the semester pattern
            String findSemesterSql = "SELECT b.semester_id FROM belong b " +
                                    "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                                    "WHERE b.student_id = ? AND b.school_year_id = ? " +
                                    "AND (" +
                                    "  (sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) AND ? = '1st Sem') " +
                                    "  OR (sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) AND ? = '2nd Sem') " +
                                    "  OR (sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND ? = 'Summer Sem') " +
                                    ") LIMIT 1";
            
            int semesterId = -1;
            try (PreparedStatement findStmt = conn.prepareStatement(findSemesterSql)) {
                findStmt.setInt(1, studentId);
                findStmt.setInt(2, schoolYearId);
                findStmt.setString(3, semester);
                findStmt.setString(4, semester);
                findStmt.setString(5, semester);
                ResultSet rs = findStmt.executeQuery();
                if (rs.next()) {
                    semesterId = rs.getInt("semester_id");
                }
            }
            
            if (semesterId == -1) {
                System.err.println("Error: Could not find belong record with semester: " + semester);
                return false;
            }
            
            // Update the belong record's status
            String sql = "UPDATE belong SET status = 'deactivated' " +
                        "WHERE student_id = ? AND school_year_id = ? AND semester_id = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, studentId);
                pstmt.setInt(2, schoolYearId);
                pstmt.setInt(3, semesterId);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error deactivating belong record: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get semester_id based on semester name (1st Sem, 2nd Sem, Summer Sem)
     */
    private int getSemesterIdBySemesterName(Connection conn, String semester) throws SQLException {
        if (semester == null || semester.trim().isEmpty()) {
            return -1;
        }
        
        String sql;
        if (semester.equals("1st Sem")) {
            sql = "SELECT semester_id FROM semester WHERE first_sem_amount > 0 " +
                  "AND (second_sem_amount = 0 OR second_sem_amount IS NULL) " +
                  "AND (summer_sem_amount = 0 OR summer_sem_amount IS NULL) LIMIT 1";
        } else if (semester.equals("2nd Sem")) {
            sql = "SELECT semester_id FROM semester WHERE second_sem_amount > 0 " +
                  "AND (first_sem_amount = 0 OR first_sem_amount IS NULL) " +
                  "AND (summer_sem_amount = 0 OR summer_sem_amount IS NULL) LIMIT 1";
        } else if (semester.equals("Summer Sem")) {
            sql = "SELECT semester_id FROM semester WHERE summer_sem_amount > 0 " +
                  "AND (first_sem_amount = 0 OR first_sem_amount IS NULL) " +
                  "AND (second_sem_amount = 0 OR second_sem_amount IS NULL) LIMIT 1";
        } else {
            return -1;
        }
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("semester_id");
            }
        }
        
        return -1;
    }
    
    /**
     * Deactivate a student (soft delete) - sets status to "deactivated" instead of deleting
     * @deprecated Use deactivateBelongRecord for semester-specific deactivation
     */
    /**
     * Deactivate student using stored procedure (more efficient)
     */
    public boolean deactivateStudent(int studentId) {
        // Try using stored procedure first
        try {
            return deactivateStudentUsingProcedure(studentId);
        } catch (Exception e) {
            // Fallback to direct query
            System.err.println("Warning: Could not use sp_deactivate_student procedure, falling back to direct query: " + e.getMessage());
        }
        
        if (!columnExists("status")) {
            // If status column doesn't exist, we can't deactivate
            // For now, just return false or we could add the column here
            return false;
        }
        
        String sql = "UPDATE student SET status = 'deactivated' WHERE student_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deactivating student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Find all student IDs that match the given criteria: ID, name, year, major, school year, and semester
     * All criteria must match exactly for a student to be included
     */
    public List<Integer> findMatchingStudentIds(String studentNumber, String firstName, String middleName, String lastName, 
                                                 String year, String major, Integer schoolYearId, String semester) {
        List<Integer> studentIds = new ArrayList<>();
        
        if (!columnExists("status")) {
            return studentIds;
        }
        
        // Require at least student number and name to match
        if ((studentNumber == null || studentNumber.trim().isEmpty()) ||
            (firstName == null || firstName.trim().isEmpty()) ||
            (lastName == null || lastName.trim().isEmpty())) {
            return studentIds; // Can't match without at least ID and name
        }
        
        StringBuilder sql = new StringBuilder("SELECT DISTINCT s.student_id " +
                "FROM student s " +
                "LEFT JOIN belong b ON s.student_id = b.student_id " +
                "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                "WHERE 1=1");
        
        List<Object> params = new ArrayList<>();
        
        // Match by student number (required)
        sql.append(" AND s.student_number = ?");
        params.add(studentNumber);
        
        // Match by first name (required)
        sql.append(" AND COALESCE(s.first_name, '') = ?");
        params.add(firstName);
        
        // Match by middle name - handle both cases: if provided, must match; if empty, must be empty/null
        if (middleName != null && !middleName.trim().isEmpty()) {
            sql.append(" AND COALESCE(s.middle_name, '') = ?");
            params.add(middleName);
        } else {
            // If middle name is empty in search criteria, match only students with empty/null middle name
            sql.append(" AND (s.middle_name IS NULL OR s.middle_name = '' OR TRIM(s.middle_name) = '')");
        }
        
        // Match by last name (required)
        sql.append(" AND COALESCE(s.last_name, '') = ?");
        params.add(lastName);
        
        // Match by year - only if provided, must match exactly
        if (year != null && !year.trim().isEmpty()) {
            sql.append(" AND COALESCE(s.year, '') = ?");
            params.add(year);
        }
        // If year is null/empty, don't filter by it (skip this criteria)
        
        // Match by major - only if provided, must match exactly
        if (major != null && !major.trim().isEmpty()) {
            sql.append(" AND COALESCE(s.major, '') = ?");
            params.add(major);
        }
        // If major is null/empty, don't filter by it (skip this criteria)
        
        // Match by school year - only if provided, must match exactly
        if (schoolYearId != null) {
            // Match by student.school_year_id OR belong.school_year_id for the same student
            sql.append(" AND (s.school_year_id = ? OR EXISTS (SELECT 1 FROM belong b2 WHERE b2.student_id = s.student_id AND b2.school_year_id = ?))");
            params.add(schoolYearId);
            params.add(schoolYearId);
        }
        // If school year is null, don't filter by it (skip this criteria)
        
        // Match by semester - only if provided, must match exactly
        if (semester != null && !semester.trim().isEmpty()) {
            if (semester.equals("1st Sem")) {
                sql.append(" AND EXISTS (SELECT 1 FROM belong b3 JOIN semester sem3 ON b3.semester_id = sem3.semester_id " +
                          "WHERE b3.student_id = s.student_id " +
                          "AND sem3.first_sem_amount > 0 " +
                          "AND (sem3.second_sem_amount = 0 OR sem3.second_sem_amount IS NULL) " +
                          "AND (sem3.summer_sem_amount = 0 OR sem3.summer_sem_amount IS NULL))");
            } else if (semester.equals("2nd Sem")) {
                sql.append(" AND EXISTS (SELECT 1 FROM belong b3 JOIN semester sem3 ON b3.semester_id = sem3.semester_id " +
                          "WHERE b3.student_id = s.student_id " +
                          "AND sem3.second_sem_amount > 0 " +
                          "AND (sem3.first_sem_amount = 0 OR sem3.first_sem_amount IS NULL) " +
                          "AND (sem3.summer_sem_amount = 0 OR sem3.summer_sem_amount IS NULL))");
            } else if (semester.equals("Summer Sem")) {
                sql.append(" AND EXISTS (SELECT 1 FROM belong b3 JOIN semester sem3 ON b3.semester_id = sem3.semester_id " +
                          "WHERE b3.student_id = s.student_id " +
                          "AND sem3.summer_sem_amount > 0 " +
                          "AND (sem3.first_sem_amount = 0 OR sem3.first_sem_amount IS NULL) " +
                          "AND (sem3.second_sem_amount = 0 OR sem3.second_sem_amount IS NULL))");
            }
        }
        // If semester is null/empty, don't filter by it (skip this criteria)
        
        // Only get active students
        sql.append(" AND COALESCE(s.status, 'active') = 'active'");
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                studentIds.add(rs.getInt("student_id"));
            }
        } catch (SQLException e) {
            System.err.println("Error finding matching students: " + e.getMessage());
            e.printStackTrace();
        }
        
        return studentIds;
    }
    
    /**
     * Deactivate multiple students by their IDs
     */
    public int deactivateStudents(List<Integer> studentIds) {
        if (studentIds == null || studentIds.isEmpty() || !columnExists("status")) {
            return 0;
        }
        
        if (studentIds.size() == 1) {
            return deactivateStudent(studentIds.get(0)) ? 1 : 0;
        }
        
        String sql = "UPDATE student SET status = 'deactivated' WHERE student_id IN (";
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < studentIds.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }
        sql += placeholders.toString() + ")";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < studentIds.size(); i++) {
                pstmt.setInt(i + 1, studentIds.get(i));
            }
            
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deactivating students: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Reactivate a specific belong record (student + school year + semester combination)
     * This reactivates only the specific semester enrollment
     */
    public boolean reactivateBelongRecord(int studentId, int schoolYearId, String semester) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Find the semester_id by joining belong with semester table and matching the semester pattern
            String findSemesterSql = "SELECT b.semester_id FROM belong b " +
                                    "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                                    "WHERE b.student_id = ? AND b.school_year_id = ? " +
                                    "AND (" +
                                    "  (sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) AND ? = '1st Sem') " +
                                    "  OR (sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) AND ? = '2nd Sem') " +
                                    "  OR (sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND ? = 'Summer Sem') " +
                                    ") LIMIT 1";
            
            int semesterId = -1;
            try (PreparedStatement findStmt = conn.prepareStatement(findSemesterSql)) {
                findStmt.setInt(1, studentId);
                findStmt.setInt(2, schoolYearId);
                findStmt.setString(3, semester);
                findStmt.setString(4, semester);
                findStmt.setString(5, semester);
                ResultSet rs = findStmt.executeQuery();
                if (rs.next()) {
                    semesterId = rs.getInt("semester_id");
                }
            }
            
            if (semesterId == -1) {
                System.err.println("Error: Could not find belong record with semester: " + semester);
                return false;
            }
            
            // Update the belong record's status
            String sql = "UPDATE belong SET status = 'active' " +
                        "WHERE student_id = ? AND school_year_id = ? AND semester_id = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, studentId);
                pstmt.setInt(2, schoolYearId);
                pstmt.setInt(3, semesterId);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error reactivating belong record: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Reactivate a student - sets status back to "active"
     * @deprecated Use reactivateBelongRecord for semester-specific reactivation
     */
    /**
     * Reactivate student using stored procedure (more efficient)
     */
    public boolean reactivateStudent(int studentId) {
        // Try using stored procedure first
        try {
            return reactivateStudentUsingProcedure(studentId);
        } catch (Exception e) {
            // Fallback to direct query
            System.err.println("Warning: Could not use sp_reactivate_student procedure, falling back to direct query: " + e.getMessage());
        }
        
        if (!columnExists("status")) {
            // If status column doesn't exist, we can't reactivate
            return false;
        }
        
        String sql = "UPDATE student SET status = 'active' WHERE student_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error reactivating student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * @deprecated Use deactivateStudent instead. This method is kept for backward compatibility.
     */
    @Deprecated
    public boolean deleteStudent(int studentId) {
        return deactivateStudent(studentId);
    }
    
    public int getTotalStudents() {
        return getTotalStudents(null);
    }
    
    public int getTotalStudents(Integer schoolYearId) {
        return getTotalStudents(schoolYearId, null);
    }
    
    /**
     * Get total students using dashboard summary view (more efficient)
     */
    public int getTotalStudents(Integer schoolYearId, String semester) {
        // Try using view first
        try {
            return getTotalStudentsFromView(schoolYearId, semester);
        } catch (Exception e) {
            // Fallback to direct query
        }
        
        String sql = "SELECT COUNT(DISTINCT s.student_id) as total " +
                  "FROM student s " +
                  "JOIN belong b ON s.student_id = b.student_id " +
                  "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                  "WHERE 1=1";
        
        if (schoolYearId != null) {
            sql += " AND b.school_year_id = ?";
        }
        
        if (semester != null) {
            // Match semester based on which amount is set
            if ("1st Sem".equals(semester)) {
                sql += " AND sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL)";
            } else if ("2nd Sem".equals(semester)) {
                sql += " AND sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL)";
            } else if ("Summer Sem".equals(semester)) {
                sql += " AND sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL)";
            }
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("Error getting total students: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Get total students from dashboard summary view
     */
    private int getTotalStudentsFromView(Integer schoolYearId, String semester) {
        String sql = "SELECT SUM(total_students_enrolled) as total " +
                    "FROM v_dashboard_summary WHERE 1=1";
        
        if (schoolYearId != null) {
            sql += " AND school_year_id = ?";
        }
        
        if (semester != null && !"All Semesters".equals(semester)) {
            sql += " AND semester_name = ?";
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            if (semester != null && !"All Semesters".equals(semester)) {
                pstmt.setString(paramIndex++, semester);
            }
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting total students from view", e);
        }
        
        return 0;
    }

    public int getOverdueCount() {
        return getOverdueCount(null);
    }
    
    public int getOverdueCount(Integer schoolYearId) {
        return getOverdueCount(schoolYearId, null);
    }
    
    /**
     * Get overdue count using dashboard summary view (more efficient)
     */
    public int getOverdueCount(Integer schoolYearId, String semester) {
        // Try using view first
        try {
            return getOverdueCountFromView(schoolYearId, semester);
        } catch (Exception e) {
            // Fallback to direct query
        }
        
        String sql = "SELECT COUNT(DISTINCT s.student_id) as overdue_count " +
                     "FROM student s " +
                     "JOIN belong b ON s.student_id = b.student_id " +
                     "JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                     "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                     "WHERE sp.status = 'OVERDUE'";
        
        if (schoolYearId != null) {
            sql += " AND b.school_year_id = ?";
        }
        
        if (semester != null) {
            // Match semester based on which amount is set
            if ("1st Sem".equals(semester)) {
                sql += " AND sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL)";
            } else if ("2nd Sem".equals(semester)) {
                sql += " AND sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL)";
            } else if ("Summer Sem".equals(semester)) {
                sql += " AND sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL)";
            }
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("overdue_count");
            }
        } catch (SQLException e) {
            System.err.println("Error getting overdue count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Get overdue count from dashboard summary view
     */
    private int getOverdueCountFromView(Integer schoolYearId, String semester) {
        String sql = "SELECT SUM(overdue_payments_count) as overdue_count " +
                    "FROM v_dashboard_summary WHERE 1=1";
        
        if (schoolYearId != null) {
            sql += " AND school_year_id = ?";
        }
        
        if (semester != null && !"All Semesters".equals(semester)) {
            sql += " AND semester_name = ?";
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            if (semester != null && !"All Semesters".equals(semester)) {
                pstmt.setString(paramIndex++, semester);
            }
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("overdue_count");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting overdue count from view", e);
        }
        
        return 0;
    }
    
    /**
     * Get students who paid count from dashboard summary view
     */
    public int getStudentsWhoPaidCount(Integer schoolYearId, String semester) {
        String sql = "SELECT SUM(students_who_paid) as count " +
                    "FROM v_dashboard_summary WHERE 1=1";
        
        if (schoolYearId != null) {
            sql += " AND school_year_id = ?";
        }
        
        if (semester != null && !"All Semesters".equals(semester)) {
            sql += " AND semester_name = ?";
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            if (semester != null && !"All Semesters".equals(semester)) {
                pstmt.setString(paramIndex++, semester);
            }
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Error getting students who paid count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    public List<String> getAllMajors() {
        List<String> majors = new ArrayList<>();
        String sql = "SELECT DISTINCT major FROM student WHERE major IS NOT NULL AND major != '' ORDER BY major";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                majors.add(rs.getString("major"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting majors: " + e.getMessage());
            e.printStackTrace();
        }
        
        return majors;
    }
    
    public List<String> getAllYears() {
        List<String> years = new ArrayList<>();
        String sql = "SELECT DISTINCT year FROM student WHERE year IS NOT NULL AND year != '' ORDER BY year";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                years.add(rs.getString("year"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting years: " + e.getMessage());
            e.printStackTrace();
        }
        
        return years;
    }
    
    private Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setStudentId(rs.getInt("student_id"));
        student.setStudentNumber(rs.getString("student_number"));
        
        // Try to get from new columns first, fallback to fullname
        try {
            String firstName = rs.getString("first_name");
            String middleName = rs.getString("middle_name");
            String lastName = rs.getString("last_name");
            if (firstName != null || middleName != null || lastName != null) {
                student.setFirstName(firstName != null ? firstName : "");
                student.setMiddleName(middleName != null ? middleName : "");
                student.setLastName(lastName != null ? lastName : "");
            } else {
                // Fallback to fullname if new columns don't exist or are null
                String fullname = rs.getString("fullname");
                if (fullname != null && !fullname.trim().isEmpty()) {
                    // Try to parse fullname into parts (simple split by space)
                    String[] parts = fullname.trim().split("\\s+");
                    if (parts.length >= 2) {
                        student.setFirstName(parts[0]);
                        student.setLastName(parts[parts.length - 1]);
                        if (parts.length > 2) {
                            student.setMiddleName(String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1)));
                        }
                    } else if (parts.length == 1) {
                        student.setFirstName(parts[0]);
                    }
                }
            }
        } catch (SQLException e) {
            // Column doesn't exist, use fullname
            String fullname = rs.getString("fullname");
            if (fullname != null && !fullname.trim().isEmpty()) {
                String[] parts = fullname.trim().split("\\s+");
                if (parts.length >= 2) {
                    student.setFirstName(parts[0]);
                    student.setLastName(parts[parts.length - 1]);
                    if (parts.length > 2) {
                        student.setMiddleName(String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1)));
                    }
                } else if (parts.length == 1) {
                    student.setFirstName(parts[0]);
                }
            }
        }
        
        student.setMajor(rs.getString("major"));
        student.setYear(rs.getString("year"));
        
        // Use belong_school_year_id if available (from filtered query), otherwise use school_year_id
        try {
            int belongSchoolYearId = rs.getInt("belong_school_year_id");
            if (!rs.wasNull()) {
                student.setSchoolYearId(belongSchoolYearId);
            } else {
                // Fallback to student.school_year_id
                int schoolYearId = rs.getInt("school_year_id");
                if (!rs.wasNull()) {
                    student.setSchoolYearId(schoolYearId);
                }
            }
        } catch (SQLException e) {
            // Column doesn't exist, use school_year_id
            int schoolYearId = rs.getInt("school_year_id");
            if (!rs.wasNull()) {
                student.setSchoolYearId(schoolYearId);
            }
        }
        
        // Read status field - prefer belong_status (from belong table) over student.status
        try {
            String belongStatus = rs.getString("belong_status");
            if (belongStatus != null && !belongStatus.trim().isEmpty()) {
                student.setStatus(belongStatus);
            } else {
                // Fallback to student.status if belong_status is not available
                try {
                    String status = rs.getString("status");
                    student.setStatus(status != null && !status.trim().isEmpty() ? status : "active");
                } catch (SQLException e) {
                    // Column doesn't exist, default to active
                    student.setStatus("active");
                }
            }
        } catch (SQLException e) {
            // belong_status column doesn't exist, try student.status
            try {
                String status = rs.getString("status");
                student.setStatus(status != null && !status.trim().isEmpty() ? status : "active");
            } catch (SQLException e2) {
                // Column doesn't exist, default to active
                student.setStatus("active");
            }
        }
        
        // Read semester_name field if available - always set a value to avoid null
        try {
            String semester = rs.getString("semester_name");
            if (semester != null && !semester.trim().isEmpty()) {
                student.setSemester(semester.trim());
            } else {
                student.setSemester(""); // Set empty string instead of null
            }
        } catch (SQLException e) {
            // Column doesn't exist, default to empty
            student.setSemester("");
        }
        
        return student;
    }
}

