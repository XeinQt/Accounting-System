package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import models.SchoolYear;
import utils.DatabaseUtil;

public class SchoolYearDAO {
    
    public List<SchoolYear> getAllSchoolYears() {
        List<SchoolYear> schoolYears = new ArrayList<>();
        String sql = "SELECT * FROM school_year ORDER BY school_year_id DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                SchoolYear sy = new SchoolYear();
                sy.setSchoolYearId(rs.getInt("school_year_id"));
                sy.setYearRange(rs.getString("year_range"));
                // Check if is_active column exists, default to true if not
                try {
                    sy.setActive(rs.getBoolean("is_active"));
                } catch (SQLException e) {
                    // Column doesn't exist yet, default to true
                    sy.setActive(true);
                }
                schoolYears.add(sy);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all school years: " + e.getMessage());
            e.printStackTrace();
        }
        
        return schoolYears;
    }
    
    /**
     * Get only active school years, ordered by newest first
     */
    public List<SchoolYear> getActiveSchoolYears() {
        List<SchoolYear> schoolYears = new ArrayList<>();
        // Try with is_active filter first
        String sql = "SELECT * FROM school_year WHERE is_active = 1 ORDER BY school_year_id DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                SchoolYear sy = new SchoolYear();
                sy.setSchoolYearId(rs.getInt("school_year_id"));
                sy.setYearRange(rs.getString("year_range"));
                sy.setActive(true);
                schoolYears.add(sy);
            }
        } catch (SQLException e) {
            // If column doesn't exist, fallback to getting all (assuming all are active)
            try {
                String sqlFallback = "SELECT * FROM school_year ORDER BY school_year_id DESC";
                try (Connection conn = DatabaseUtil.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sqlFallback);
                     ResultSet rs = pstmt.executeQuery()) {
                    
                    while (rs.next()) {
                        SchoolYear sy = new SchoolYear();
                        sy.setSchoolYearId(rs.getInt("school_year_id"));
                        sy.setYearRange(rs.getString("year_range"));
                        sy.setActive(true);
                        schoolYears.add(sy);
                    }
                }
            } catch (SQLException e2) {
                System.err.println("Error getting active school years: " + e2.getMessage());
                e2.printStackTrace();
            }
        }
        
        return schoolYears;
    }
    
    public SchoolYear getSchoolYearById(int schoolYearId) {
        String sql = "SELECT * FROM school_year WHERE school_year_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, schoolYearId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                SchoolYear sy = new SchoolYear();
                sy.setSchoolYearId(rs.getInt("school_year_id"));
                sy.setYearRange(rs.getString("year_range"));
                try {
                    sy.setActive(rs.getBoolean("is_active"));
                } catch (SQLException e) {
                    sy.setActive(true);
                }
                return sy;
            }
        } catch (SQLException e) {
            System.err.println("Error getting school year by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public SchoolYear getSchoolYearByRange(String yearRange) {
        String sql = "SELECT * FROM school_year WHERE year_range = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, yearRange);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                SchoolYear sy = new SchoolYear();
                sy.setSchoolYearId(rs.getInt("school_year_id"));
                sy.setYearRange(rs.getString("year_range"));
                try {
                    sy.setActive(rs.getBoolean("is_active"));
                } catch (SQLException e) {
                    sy.setActive(true);
                }
                return sy;
            }
        } catch (SQLException e) {
            System.err.println("Error getting school year by range: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public boolean addSchoolYear(SchoolYear schoolYear) {
        // Try with is_active column first, fallback if column doesn't exist
        String sql = "INSERT INTO school_year (year_range, is_active) VALUES (?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, schoolYear.getYearRange());
            pstmt.setBoolean(2, schoolYear.isActive());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            // If column doesn't exist, try without it
            try {
                String sqlFallback = "INSERT INTO school_year (year_range) VALUES (?)";
                try (Connection conn = DatabaseUtil.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sqlFallback)) {
                    pstmt.setString(1, schoolYear.getYearRange());
                    int rowsAffected = pstmt.executeUpdate();
                    return rowsAffected > 0;
                }
            } catch (SQLException e2) {
                System.err.println("Error adding school year: " + e2.getMessage());
                e2.printStackTrace();
                return false;
            }
        }
    }
    
    public boolean updateSchoolYear(SchoolYear schoolYear) {
        // Try with is_active column first, fallback if column doesn't exist
        String sql = "UPDATE school_year SET year_range = ?, is_active = ? WHERE school_year_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, schoolYear.getYearRange());
            pstmt.setBoolean(2, schoolYear.isActive());
            pstmt.setInt(3, schoolYear.getSchoolYearId());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            // If column doesn't exist, try without it
            try {
                String sqlFallback = "UPDATE school_year SET year_range = ? WHERE school_year_id = ?";
                try (Connection conn = DatabaseUtil.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sqlFallback)) {
                    pstmt.setString(1, schoolYear.getYearRange());
                    pstmt.setInt(2, schoolYear.getSchoolYearId());
                    int rowsAffected = pstmt.executeUpdate();
                    return rowsAffected > 0;
                }
            } catch (SQLException e2) {
                System.err.println("Error updating school year: " + e2.getMessage());
                e2.printStackTrace();
                return false;
            }
        }
    }
    
    public boolean deleteSchoolYear(int schoolYearId) {
        String sql = "DELETE FROM school_year WHERE school_year_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, schoolYearId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting school year: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if a school year is being used by any students
     * Returns true if the school year has students (via student.school_year_id or belong.school_year_id)
     */
    public boolean hasStudents(int schoolYearId) {
        String sql = "SELECT COUNT(*) as count FROM (" +
                     "  SELECT student_id FROM student WHERE school_year_id = ? " +
                     "  UNION " +
                     "  SELECT DISTINCT student_id FROM belong WHERE school_year_id = ?" +
                     ") as combined";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, schoolYearId);
            pstmt.setInt(2, schoolYearId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking if school year has students: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
}

