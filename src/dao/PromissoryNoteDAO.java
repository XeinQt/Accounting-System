package dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import models.AcademicYearSemesterBalance;
import models.PromissoryNote;
import models.PromissoryNoteView;
import utils.DatabaseUtil;
import utils.PayableEncryptionUtil;

public class PromissoryNoteDAO {
    
    /**
     * Get all students with due dates for promissory note generation
     * Shows students whose due date is today or in the past (overdue)
     */
    public List<PromissoryNoteView> getStudentsForPromissoryNotes(Integer schoolYearId) {
        return getStudentsForPromissoryNotes(schoolYearId, null);
    }
    
    /**
     * Get all students with due dates for promissory note generation with semester filter
     * Shows students whose due date is today or in the past (overdue)
     * @param schoolYearId School year ID filter
     * @param semester Semester filter (1st Sem, 2nd Sem, Summer Sem)
     */
    public List<PromissoryNoteView> getStudentsForPromissoryNotes(Integer schoolYearId, String semester) {
        List<PromissoryNoteView> students = new ArrayList<>();
        
        // Query to get students with due dates who are UNPAID or PARTIAL (not fully PAID)
        String sql = "SELECT " +
                    "s.student_id, " +
                    "s.student_number, " +
                    "COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) as student_name, " +
                    "s.major, " +
                    "s.year, " +
                    "GROUP_CONCAT(sp.downpayment_amount SEPARATOR '||') as downpayment_amounts, " +
                    "GROUP_CONCAT(sp.amount_paid SEPARATOR '||') as amount_paid_values, " +
                    "GROUP_CONCAT(sp.remaining_balance SEPARATOR '||') as remaining_balance_values, " +
                    "MAX(s.student_id) as student_id_for_decrypt, " +
                    "MAX(d.due_date) as due_date, " +
                    "MAX(sp.status) as status " +
                    "FROM student s " +
                    "INNER JOIN belong b ON s.student_id = b.student_id " +
                    "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                    "INNER JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                    "INNER JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                    "WHERE COALESCE(s.status, 'active') = 'active' " +
                    "AND COALESCE(b.status, 'active') = 'active' " +
                    "AND d.due_date IS NOT NULL " +
                    "AND d.due_date <= CURDATE() " +
                    "AND sp.status IN ('UNPAID', 'PARTIAL', 'OVERDUE') ";
        
        if (schoolYearId != null) {
            sql += "AND b.school_year_id = ? ";
        }
        
        // Filter by semester if provided
        if (semester != null && !semester.trim().isEmpty() && !"All Semesters".equals(semester)) {
            if ("1st Sem".equals(semester)) {
                sql += "AND sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) ";
            } else if ("2nd Sem".equals(semester)) {
                sql += "AND sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) ";
            } else if ("Summer Sem".equals(semester)) {
                sql += "AND sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) ";
            }
        }
        
        sql += "GROUP BY s.student_id, s.student_number, s.first_name, s.middle_name, s.last_name, s.fullname, s.major, s.year " +
               "HAVING MAX(d.due_date) IS NOT NULL " +
               "AND MAX(d.due_date) <= CURDATE() " +
               "AND sp.remaining_balance IS NOT NULL AND sp.remaining_balance != '' " +
               "ORDER BY MAX(d.due_date) ASC, s.student_number ASC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            
            System.out.println("=== PROMISSORY NOTES QUERY ===");
            System.out.println("SQL: " + sql);
            if (schoolYearId != null) {
                System.out.println("School Year ID: " + schoolYearId);
            }
            System.out.println("Semester: " + semester);
            
            ResultSet rs = pstmt.executeQuery();
            int count = 0;
            
            while (rs.next()) {
                count++;
                PromissoryNoteView view = new PromissoryNoteView();
                view.setStudentId(rs.getInt("student_id"));
                view.setStudentNumber(rs.getString("student_number"));
                view.setStudentName(rs.getString("student_name"));
                view.setMajor(rs.getString("major"));
                view.setYear(rs.getString("year"));
                view.setDepartment(null); // Not in database schema
                view.setCollege(null); // Not in database schema
                
                // Decrypt and sum amounts
                int studentId = rs.getInt("student_id_for_decrypt");
                double totalAmount = 0;
                double amountPaid = 0;
                double remainingBalance = 0;
                
                try {
                    String downpaymentAmountsStr = rs.getString("downpayment_amounts");
                    if (downpaymentAmountsStr != null && !downpaymentAmountsStr.isEmpty()) {
                        String[] amounts = downpaymentAmountsStr.split("\\|\\|");
                        for (String amount : amounts) {
                            if (amount != null && !amount.trim().isEmpty()) {
                                totalAmount += PayableEncryptionUtil.decryptAmount(amount, studentId);
                            }
                        }
                    }
                    
                    String amountPaidValuesStr = rs.getString("amount_paid_values");
                    if (amountPaidValuesStr != null && !amountPaidValuesStr.isEmpty()) {
                        String[] amounts = amountPaidValuesStr.split("\\|\\|");
                        for (String amount : amounts) {
                            if (amount != null && !amount.trim().isEmpty()) {
                                amountPaid += PayableEncryptionUtil.decryptAmount(amount, studentId);
                            }
                        }
                    }
                    
                    String remainingBalanceValuesStr = rs.getString("remaining_balance_values");
                    if (remainingBalanceValuesStr != null && !remainingBalanceValuesStr.isEmpty()) {
                        String[] amounts = remainingBalanceValuesStr.split("\\|\\|");
                        for (String amount : amounts) {
                            if (amount != null && !amount.trim().isEmpty()) {
                                remainingBalance += PayableEncryptionUtil.decryptAmount(amount, studentId);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error decrypting amounts in PromissoryNoteDAO: " + e.getMessage());
                }
                
                // Filter out records with zero remaining balance (since we can't filter in SQL)
                if (remainingBalance <= 0) {
                    continue;
                }
                
                // Use downpayment_amount as total (what student owes)
                view.setTotalAmount(totalAmount);
                view.setAmountPaid(amountPaid);
                view.setRemainingBalance(remainingBalance > 0 ? remainingBalance : 0);
                
                java.sql.Date dueDateSql = rs.getDate("due_date");
                if (dueDateSql != null) {
                    view.setDueDate(dueDateSql.toLocalDate());
                }
                
                // Get status from database or calculate dynamically
                String dbStatus = rs.getString("status");
                if (dbStatus != null) {
                    // Map database status to display status
                    if ("PAID".equals(dbStatus)) {
                        view.setStatus("Paid");
                    } else if ("PARTIAL".equals(dbStatus)) {
                        view.setStatus("Partial");
                    } else if ("OVERDUE".equals(dbStatus)) {
                        view.setStatus("OVERDUE");
                    } else {
                        view.setStatus("UNPAID");
                    }
                } else {
                    // Calculate status dynamically if not in DB
                    if (Math.abs(amountPaid - totalAmount) < 0.01 || amountPaid >= totalAmount) {
                        view.setStatus("Paid");
                    } else if (amountPaid > 0) {
                        view.setStatus("Partial");
                    } else {
                        view.setStatus("UNPAID");
                    }
                }
                
                // Only add if not fully paid and has due date
                if (view.getDueDate() != null && view.getRemainingBalance() > 0) {
                    students.add(view);
                    System.out.println("Added student: " + view.getStudentNumber() + " - " + view.getStudentName() + 
                                     ", Due: " + view.getDueDate() + ", Balance: " + view.getRemainingBalance() + 
                                     ", Status: " + view.getStatus());
                } else {
                    System.out.println("Skipped student: " + view.getStudentNumber() + " - " + view.getStudentName() + 
                                     " (Due: " + view.getDueDate() + ", Balance: " + view.getRemainingBalance() + ")");
                }
            }
            
            System.out.println("Total students found: " + count);
            System.out.println("Total students added: " + students.size());
            System.out.println("=============================");
            
        } catch (SQLException e) {
            System.err.println("Error getting students for promissory notes: " + e.getMessage());
            e.printStackTrace();
        }
        
        return students;
    }
    
    /**
     * Save a promissory note to the database
     */
    public boolean savePromissoryNote(int studentId, LocalDate createdDate, LocalDate dueDateExtended, 
                                      double remainingBalance, String noteText) {
        String sql = "INSERT INTO promissory_note (created_date, due_date_extended, remaining_balance_snapshot, note_text) " +
                    "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, java.sql.Date.valueOf(createdDate));
            if (dueDateExtended != null) {
                pstmt.setDate(2, java.sql.Date.valueOf(dueDateExtended));
            } else {
                pstmt.setNull(2, Types.DATE);
            }
            pstmt.setDouble(3, remainingBalance);
            pstmt.setString(4, noteText);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error saving promissory note: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get unpaid balances by academic year and semester for a specific student
     */
    public List<AcademicYearSemesterBalance> getUnpaidBalancesByAcademicYearAndSemester(int studentId) {
        List<AcademicYearSemesterBalance> balances = new ArrayList<>();
        
        String sql = "SELECT " +
                    "sy.year_range as academic_year, " +
                    "CASE " +
                    "  WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st' " +
                    "  WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd' " +
                    "  WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer' " +
                    "  ELSE NULL " +
                    "END as semester, " +
                    "GROUP_CONCAT(sp.amount_paid SEPARATOR '||') as amount_paid_values, " +
                    "SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount) as total_semester_amount " +
                    "FROM student s " +
                    "INNER JOIN belong b ON s.student_id = b.student_id " +
                    "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                    "INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id " +
                    "LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                    "WHERE s.student_id = ? " +
                    "AND (sp.status IS NULL OR sp.status != 'Paid') " +
                    "GROUP BY sy.year_range, " +
                    "  CASE " +
                    "    WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st' " +
                    "    WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd' " +
                    "    WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer' " +
                    "    ELSE NULL " +
                    "  END " +
                    "HAVING total_semester_amount > 0 " +
                    "ORDER BY sy.year_range, semester";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String academicYear = rs.getString("academic_year");
                String semester = rs.getString("semester");
                
                // Calculate remaining balance: total_semester_amount - sum of encrypted amount_paid
                double totalSemesterAmount = rs.getDouble("total_semester_amount");
                double totalAmountPaid = 0;
                try {
                    String amountPaidValuesStr = rs.getString("amount_paid_values");
                    if (amountPaidValuesStr != null && !amountPaidValuesStr.isEmpty()) {
                        String[] amounts = amountPaidValuesStr.split("\\|\\|");
                        for (String amount : amounts) {
                            if (amount != null && !amount.trim().isEmpty()) {
                                totalAmountPaid += PayableEncryptionUtil.decryptAmount(amount, studentId);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error decrypting amount_paid in getUnpaidBalancesByAcademicYearAndSemester: " + e.getMessage());
                }
                double amount = totalSemesterAmount - totalAmountPaid;
                
                // Extract year from year_range (e.g., "2023-2024" -> "2024")
                if (academicYear != null && academicYear.contains("-")) {
                    String[] parts = academicYear.split("-");
                    if (parts.length == 2) {
                        academicYear = parts[1]; // Use the second year
                    }
                }
                
                // Convert semester format
                if (semester != null) {
                    if (semester.equals("1st")) {
                        semester = "1st";
                    } else if (semester.equals("2nd")) {
                        semester = "2nd";
                    } else if (semester.equals("Summer")) {
                        semester = "Summer";
                    }
                }
                
                if (academicYear != null && semester != null && amount > 0) {
                    balances.add(new AcademicYearSemesterBalance(academicYear, semester, amount));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting unpaid balances by academic year and semester: " + e.getMessage());
            e.printStackTrace();
        }
        
        return balances;
    }
    
    /**
     * Get all promissory notes
     */
    public List<PromissoryNote> getAllPromissoryNotes() {
        List<PromissoryNote> notes = new ArrayList<>();
        
        String sql = "SELECT * FROM promissory_note ORDER BY created_date DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                PromissoryNote note = new PromissoryNote();
                note.setPromissoryId(rs.getInt("promissory_id"));
                
                java.sql.Date createdDateSql = rs.getDate("created_date");
                if (createdDateSql != null) {
                    note.setCreatedDate(createdDateSql.toLocalDate());
                }
                
                java.sql.Date dueDateExtendedSql = rs.getDate("due_date_extended");
                if (dueDateExtendedSql != null) {
                    note.setDueDateExtended(dueDateExtendedSql.toLocalDate());
                }
                
                note.setRemainingBalanceSnapshot(rs.getDouble("remaining_balance_snapshot"));
                note.setNoteText(rs.getString("note_text"));
                
                notes.add(note);
            }
        } catch (SQLException e) {
            System.err.println("Error getting promissory notes: " + e.getMessage());
            e.printStackTrace();
        }
        
        return notes;
    }
}

