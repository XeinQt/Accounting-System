package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import models.PaymentView;
import utils.DatabaseUtil;

public class PaymentDAO {
    
    /**
     * Get all payment views with student and payable information
     */
    public List<PaymentView> getAllPaymentViews(Integer schoolYearId) {
        return getAllPaymentViews(schoolYearId, null);
    }
    
    /**
     * Get all payment views with student and payable information, filtered by semester
     * Tries to use stored procedure first, falls back to direct query
     */
    public List<PaymentView> getAllPaymentViews(Integer schoolYearId, String semester) {
        // Try using stored procedure first
        try {
            return getAllPaymentViewsUsingProcedure(schoolYearId, semester, null, null, null);
        } catch (Exception e) {
            // Fallback to direct query
            System.err.println("Warning: Could not use sp_get_payments procedure, falling back to direct query: " + e.getMessage());
        }
        List<PaymentView> payments = new ArrayList<>();
        
        String sql = "SELECT " +
                    "s.student_id, " +
                    "s.student_number, " +
                    "s.fullname, " +
                    "COALESCE(MAX(CASE WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN sem.first_sem_amount ELSE 0 END), 0) as first_sem, " +
                    "COALESCE(MAX(CASE WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN sem.second_sem_amount ELSE 0 END), 0) as second_sem, " +
                    "COALESCE(MAX(CASE WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN sem.summer_sem_amount ELSE 0 END), 0) as summer_sem, " +
                    "COALESCE(SUM(sp.downpayment_amount), 0) as total_downpayment, " +
                    "COALESCE(SUM(sp.amount_paid), 0) as amount_paid, " +
                    "MAX(d.due_date) as due_date, " +
                    "MAX(sp.status) as status " +
                    "FROM student s " +
                    "INNER JOIN belong b ON s.student_id = b.student_id " +
                    "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                    "INNER JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                    "LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                    "WHERE 1=1 ";
        
        if (schoolYearId != null) {
            sql += "AND b.school_year_id = ? ";
        }
        
        // Filter by semester if provided
        if (semester != null && !semester.isEmpty() && !"All Semesters".equals(semester)) {
            if ("1st Sem".equals(semester)) {
                sql += "AND sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) ";
            } else if ("2nd Sem".equals(semester)) {
                sql += "AND sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) ";
            } else if ("Summer Sem".equals(semester)) {
                sql += "AND sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) ";
            }
        }
        
        sql += "GROUP BY s.student_id, s.student_number, s.fullname " +
               "ORDER BY s.student_id DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PaymentView view = new PaymentView();
                view.setStudentId(rs.getInt("student_id"));
                view.setStudentNumber(rs.getString("student_number"));
                view.setStudentName(rs.getString("fullname"));
                view.setFirstSemAmount(rs.getDouble("first_sem"));
                view.setSecondSemAmount(rs.getDouble("second_sem"));
                view.setSummerSemAmount(rs.getDouble("summer_sem"));
                
                double total = view.getFirstSemAmount() + view.getSecondSemAmount() + view.getSummerSemAmount();
                view.setTotalAmount(total);
                
                // Get actual down payment from database - only show if admin has entered it
                double totalDownPayment = rs.getDouble("total_downpayment");
                // Only set down payment if it's been explicitly entered (not auto-calculated)
                // If totalDownPayment is 0 or NULL, it means admin hasn't entered it yet
                view.setDownPayment(totalDownPayment > 0 ? totalDownPayment : 0);
                
                view.setAmountPaid(rs.getDouble("amount_paid"));
                
                java.sql.Date dueDateSql = rs.getDate("due_date");
                if (dueDateSql != null) {
                    view.setDueDate(dueDateSql.toLocalDate());
                }
                
                // Always recalculate status based on current amount paid vs total of all semesters
                // Status should be dynamically calculated, not stored
                if (Math.abs(view.getAmountPaid() - view.getTotalAmount()) < 0.01 || view.getAmountPaid() >= view.getTotalAmount()) {
                    view.setStatus("Paid");
                    // Clear due date when fully paid
                    view.setDueDate(null);
                } else if (view.getAmountPaid() > 0) {
                    view.setStatus("Partial");
                } else {
                    view.setStatus("UNPAID");
                }
                
                payments.add(view);
            }
        } catch (SQLException e) {
            System.err.println("Error getting payment views: " + e.getMessage());
            e.printStackTrace();
        }
        
        return payments;
    }
    
    /**
     * Get payment view for a specific student
     */
    public PaymentView getPaymentView(int studentId, Integer schoolYearId) {
        String sql = "SELECT " +
                    "s.student_id, " +
                    "s.student_number, " +
                    "s.fullname, " +
                    "COALESCE(MAX(CASE WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN sem.first_sem_amount ELSE 0 END), 0) as first_sem, " +
                    "COALESCE(MAX(CASE WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN sem.second_sem_amount ELSE 0 END), 0) as second_sem, " +
                    "COALESCE(MAX(CASE WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN sem.summer_sem_amount ELSE 0 END), 0) as summer_sem, " +
                    "COALESCE(SUM(sp.downpayment_amount), 0) as total_downpayment, " +
                    "COALESCE(SUM(sp.amount_paid), 0) as amount_paid, " +
                    "MAX(d.due_date) as due_date, " +
                    "MAX(sp.status) as status " +
                    "FROM student s " +
                    "LEFT JOIN belong b ON s.student_id = b.student_id " +
                    (schoolYearId != null ? "AND b.school_year_id = ? " : "") +
                    "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                    "LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                    "LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                    "WHERE s.student_id = ? " +
                    "GROUP BY s.student_id, s.student_number, s.fullname";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            pstmt.setInt(paramIndex++, studentId);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                PaymentView view = new PaymentView();
                view.setStudentId(rs.getInt("student_id"));
                view.setStudentNumber(rs.getString("student_number"));
                view.setStudentName(rs.getString("fullname"));
                view.setFirstSemAmount(rs.getDouble("first_sem"));
                view.setSecondSemAmount(rs.getDouble("second_sem"));
                view.setSummerSemAmount(rs.getDouble("summer_sem"));
                
                double total = view.getFirstSemAmount() + view.getSecondSemAmount() + view.getSummerSemAmount();
                view.setTotalAmount(total);
                
                // Get actual down payment from database - only show if admin has entered it
                double totalDownPayment = rs.getDouble("total_downpayment");
                // Only set down payment if it's been explicitly entered (not auto-calculated)
                // If totalDownPayment is 0 or NULL, it means admin hasn't entered it yet
                view.setDownPayment(totalDownPayment > 0 ? totalDownPayment : 0);
                
                view.setAmountPaid(rs.getDouble("amount_paid"));
                
                java.sql.Date dueDateSql = rs.getDate("due_date");
                if (dueDateSql != null) {
                    view.setDueDate(dueDateSql.toLocalDate());
                }
                
                // Always recalculate status based on current amount paid vs total of all semesters
                // Status should be dynamically calculated, not stored
                if (Math.abs(view.getAmountPaid() - view.getTotalAmount()) < 0.01 || view.getAmountPaid() >= view.getTotalAmount()) {
                    view.setStatus("Paid");
                    // Clear due date when fully paid
                    view.setDueDate(null);
                } else if (view.getAmountPaid() > 0) {
                    view.setStatus("Partial");
                } else {
                    view.setStatus("UNPAID");
                }
                
                return view;
            }
        } catch (SQLException e) {
            System.err.println("Error getting payment view: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Add or update payment for a student
     * This updates the downpayment_amount and amount_paid in student_payables and creates/updates due date
     * amountPaid is the total accumulated amount (not the increment)
     * Tries to use stored procedure first, falls back to direct query
     */
    public boolean savePayment(int studentId, Integer schoolYearId, double downPayment, double amountPaid, LocalDate dueDate, String status) {
        // Try using stored procedure first
        try {
            return savePaymentUsingProcedure(studentId, schoolYearId, downPayment, amountPaid, dueDate, status);
        } catch (Exception e) {
            // Fallback to direct query
            System.err.println("Warning: Could not use sp_save_payment procedure, falling back to direct query: " + e.getMessage());
        }
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Get all belong records for this student and school year
                String getBelongSql = "SELECT b.belong_id, sp.payable_id " +
                                      "FROM belong b " +
                                      "LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                                      "WHERE b.student_id = ? " +
                                      (schoolYearId != null ? "AND b.school_year_id = ? " : "");
                
                List<Integer> belongIds = new ArrayList<>();
                try (PreparedStatement pstmt = conn.prepareStatement(getBelongSql)) {
                    int paramIndex = 1;
                    pstmt.setInt(paramIndex++, studentId);
                    if (schoolYearId != null) {
                        pstmt.setInt(paramIndex++, schoolYearId);
                    }
                    
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        belongIds.add(rs.getInt("belong_id"));
                    }
                }
                
                if (belongIds.isEmpty()) {
                    return false; // No payables found for this student
                }
                
                // Get total payable amount from all semesters for this student
                String getTotalPayableSql = "SELECT COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) as total_payable " +
                                           "FROM belong b " +
                                           "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                                           "WHERE b.student_id = ? " +
                                           (schoolYearId != null ? "AND b.school_year_id = ? " : "") +
                                           "AND COALESCE(b.status, 'active') = 'active'";
                double totalPayable = 0;
                try (PreparedStatement pstmt = conn.prepareStatement(getTotalPayableSql)) {
                    int paramIndex = 1;
                    pstmt.setInt(paramIndex++, studentId);
                    if (schoolYearId != null) {
                        pstmt.setInt(paramIndex++, schoolYearId);
                    }
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        totalPayable = rs.getDouble("total_payable");
                    }
                }
                
                // Cap amount_paid at totalPayable to prevent any excess (even 0.01)
                if (amountPaid > totalPayable) {
                    amountPaid = totalPayable;
                }
                
                // Update ALL payable records for this student in this school year
                // Distribute the amount_paid proportionally or update all with the same amount
                // For now, we'll update all payables with the same amount_paid (total divided by count)
                String getPayablesSql = "SELECT sp.payable_id, sp.belong_id, sp.downpayment_amount, sp.amount_paid " +
                                       "FROM student_payables sp " +
                                       "INNER JOIN belong b ON sp.belong_id = b.belong_id " +
                                       "WHERE b.student_id = ? " +
                                       (schoolYearId != null ? "AND b.school_year_id = ? " : "") +
                                       "AND COALESCE(b.status, 'active') = 'active'";
                
                List<Integer> payableIds = new ArrayList<>();
                List<Integer> belongIdsForPayables = new ArrayList<>();
                List<Double> payableAmounts = new ArrayList<>();
                
                try (PreparedStatement pstmt = conn.prepareStatement(getPayablesSql)) {
                    int paramIndex = 1;
                    pstmt.setInt(paramIndex++, studentId);
                    if (schoolYearId != null) {
                        pstmt.setInt(paramIndex++, schoolYearId);
                    }
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        payableIds.add(rs.getInt("payable_id"));
                        belongIdsForPayables.add(rs.getInt("belong_id"));
                        payableAmounts.add(rs.getDouble("downpayment_amount"));
                    }
                }
                
                // If no payables exist, create one for the first belong record
                if (payableIds.isEmpty() && !belongIds.isEmpty()) {
                    int firstBelongId = belongIds.get(0);
                    // Get downpayment amount from semester
                    String getSemesterAmountSql = "SELECT COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) as total " +
                                                 "FROM belong b " +
                                                 "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                                                 "WHERE b.belong_id = ?";
                    double semesterAmount = 0;
                    try (PreparedStatement pstmt = conn.prepareStatement(getSemesterAmountSql)) {
                        pstmt.setInt(1, firstBelongId);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            semesterAmount = rs.getDouble("total");
                        }
                    }
                    
                    // Create new payable
                    double remainingBalance = Math.max(semesterAmount - amountPaid, 0);
                    String insertPayableSql = "INSERT INTO student_payables (belong_id, downpayment_amount, amount_paid, remaining_balance, status) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertPayableSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                        pstmt.setInt(1, firstBelongId);
                        pstmt.setDouble(2, semesterAmount);
                        pstmt.setDouble(3, amountPaid);
                        pstmt.setDouble(4, remainingBalance);
                        pstmt.setString(5, status);
                        pstmt.executeUpdate();
                        ResultSet rs = pstmt.getGeneratedKeys();
                        if (rs.next()) {
                            payableIds.add(rs.getInt(1));
                            belongIdsForPayables.add(firstBelongId);
                            payableAmounts.add(semesterAmount);
                        }
                    }
                }
                
                // Update all existing payables with the new amount_paid
                // Distribute proportionally based on each payable's downpayment_amount
                double totalPayableAmount = payableAmounts.stream().mapToDouble(Double::doubleValue).sum();
                
                for (int i = 0; i < payableIds.size(); i++) {
                    int payableId = payableIds.get(i);
                    double payableAmount = payableAmounts.get(i);
                    
                    // Calculate proportional amount_paid for this payable
                    double proportionalAmountPaid = 0;
                    if (totalPayableAmount > 0) {
                        proportionalAmountPaid = (payableAmount / totalPayableAmount) * amountPaid;
                    } else {
                        // If no total, distribute equally
                        proportionalAmountPaid = amountPaid / payableIds.size();
                    }
                    
                    // Calculate remaining balance for this payable
                    double remainingBalance = Math.max(payableAmount - proportionalAmountPaid, 0);
                    
                    // Determine status for this payable
                    String payableStatus = status;
                    if (Math.abs(proportionalAmountPaid - payableAmount) < 0.01 || proportionalAmountPaid >= payableAmount) {
                        payableStatus = "PAID";
                    } else if (proportionalAmountPaid > 0) {
                        payableStatus = "PARTIAL";
                    } else {
                        payableStatus = "UNPAID";
                    }
                    
                    // Update this payable
                    String updatePayableSql = "UPDATE student_payables SET amount_paid = ?, remaining_balance = ?, status = ? WHERE payable_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updatePayableSql)) {
                        pstmt.setDouble(1, proportionalAmountPaid);
                        pstmt.setDouble(2, remainingBalance);
                        pstmt.setString(3, payableStatus);
                        pstmt.setInt(4, payableId);
                        pstmt.executeUpdate();
                    }
                }
                
                // Use the first payable for due date handling
                int payableId = payableIds.isEmpty() ? 0 : payableIds.get(0);
                
                // Create or update due date (only if not paid)
                if (dueDate != null && !"Paid".equals(status)) {
                    // Check if duedate exists for this payable
                    String checkDuedateSql = "SELECT duedate_id FROM student_payables WHERE payable_id = ?";
                    Integer duedateId = null;
                    try (PreparedStatement pstmt = conn.prepareStatement(checkDuedateSql)) {
                        pstmt.setInt(1, payableId);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            duedateId = rs.getInt("duedate_id");
                            if (rs.wasNull()) {
                                duedateId = null;
                            }
                        }
                    }
                    
                    if (duedateId == null) {
                        // Create new duedate
                        String insertDuedateSql = "INSERT INTO duedate (due_date) VALUES (?)";
                        try (PreparedStatement pstmt = conn.prepareStatement(insertDuedateSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                            pstmt.setDate(1, java.sql.Date.valueOf(dueDate));
                            pstmt.executeUpdate();
                            ResultSet rs = pstmt.getGeneratedKeys();
                            if (rs.next()) {
                                duedateId = rs.getInt(1);
                            }
                        }
                        
                        // Update payable with duedate_id
                        String updateDuedateSql = "UPDATE student_payables SET duedate_id = ? WHERE payable_id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(updateDuedateSql)) {
                            pstmt.setInt(1, duedateId);
                            pstmt.setInt(2, payableId);
                            pstmt.executeUpdate();
                        }
                    } else {
                        // Update existing duedate
                        String updateDuedateSql = "UPDATE duedate SET due_date = ? WHERE duedate_id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(updateDuedateSql)) {
                            pstmt.setDate(1, java.sql.Date.valueOf(dueDate));
                            pstmt.setInt(2, duedateId);
                            pstmt.executeUpdate();
                        }
                    }
                } else if ("Paid".equals(status)) {
                    // If status is "Paid", remove due date (set duedate_id to NULL)
                    String removeDuedateSql = "UPDATE student_payables SET duedate_id = NULL WHERE payable_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(removeDuedateSql)) {
                        pstmt.setInt(1, payableId);
                        pstmt.executeUpdate();
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
            System.err.println("Error saving payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Delete payment (reset amount_paid to 0)
     * IMPORTANT: This only affects amount_paid in student_payables table.
     * It does NOT modify payables in the semester table (first_sem_amount, second_sem_amount, summer_sem_amount).
     * Tries to use stored procedure first, falls back to direct query
     */
    public boolean deletePayment(int studentId, Integer schoolYearId) {
        // Try using stored procedure first
        try {
            return deletePaymentUsingProcedure(studentId, schoolYearId);
        } catch (Exception e) {
            // Fallback to direct query
            System.err.println("Warning: Could not use sp_delete_payment procedure, falling back to direct query: " + e.getMessage());
        }
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Get all belong records for this student and school year
                String getBelongSql = "SELECT b.belong_id, sp.payable_id, sem.first_sem_amount, sem.second_sem_amount, sem.summer_sem_amount " +
                                      "FROM belong b " +
                                      "LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                                      "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                                      "WHERE b.student_id = ? " +
                                      (schoolYearId != null ? "AND b.school_year_id = ? " : "");
                
                List<Integer> payableIds = new ArrayList<>();
                List<Double> totalPayables = new ArrayList<>();
                
                try (PreparedStatement pstmt = conn.prepareStatement(getBelongSql)) {
                    int paramIndex = 1;
                    pstmt.setInt(paramIndex++, studentId);
                    if (schoolYearId != null) {
                        pstmt.setInt(paramIndex++, schoolYearId);
                    }
                    
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        int payableId = rs.getInt("payable_id");
                        if (payableId > 0) {
                            payableIds.add(payableId);
                            
                            // Calculate total payables from semester amounts
                            double firstSem = rs.getDouble("first_sem_amount");
                            double secondSem = rs.getDouble("second_sem_amount");
                            double summerSem = rs.getDouble("summer_sem_amount");
                            double total = firstSem + secondSem + summerSem;
                            totalPayables.add(total);
                        }
                    }
                }
                
                if (payableIds.isEmpty()) {
                    conn.rollback();
                    return false; // No payables found for this student
                }
                
                // Update each payable record - reset amount_paid to 0
                // Calculate remaining_balance based on total payables from semester (not downpayment_amount)
                for (int i = 0; i < payableIds.size(); i++) {
                    int payableId = payableIds.get(i);
                    double totalPayable = totalPayables.get(i);
                    
                    // Reset amount_paid to 0, recalculate remaining_balance from total payables
                    // remaining_balance = total payables - amount_paid (which will be 0)
                    String updateSql = "UPDATE student_payables " +
                                      "SET amount_paid = 0, " +
                                      "    remaining_balance = ?, " +
                                      "    status = 'UNPAID', " +
                                      "    duedate_id = NULL " +
                                      "WHERE payable_id = ?";
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                        pstmt.setDouble(1, totalPayable); // remaining_balance = total payables (since amount_paid = 0)
                        pstmt.setInt(2, payableId);
                        pstmt.executeUpdate();
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
            System.err.println("Error deleting payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public double getTotalPayments() {
        return getTotalPayments(null);
    }
    
    public double getTotalPayments(Integer schoolYearId) {
        return getTotalPayments(schoolYearId, null);
    }
    
    /**
     * Get total payments using dashboard summary view (more efficient)
     */
    public double getTotalPayments(Integer schoolYearId, String semester) {
        // Try using view first
        try {
            return getTotalPaymentsFromView(schoolYearId, semester);
        } catch (Exception e) {
            // Fallback to direct query
        }
        
        String sql = "SELECT SUM(sp.amount_paid) as total " +
                  "FROM student_payables sp " +
                  "JOIN belong b ON sp.belong_id = b.belong_id " +
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
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("Error getting total payments: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0.0;
    }
    
    /**
     * Get total payments from dashboard summary view
     */
    private double getTotalPaymentsFromView(Integer schoolYearId, String semester) {
        String sql = "SELECT SUM(total_payments_collected) as total " +
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
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting total payments from view", e);
        }
        
        return 0.0;
    }
    
    /**
     * Get latest payments (for dashboard compatibility)
     * Returns Payment objects instead of PaymentView
     */
    public List<models.Payment> getLatestPayments(int limit) {
        return getLatestPayments(limit, null);
    }
    
    public List<models.Payment> getLatestPayments(int limit, Integer schoolYearId) {
        return getLatestPayments(limit, schoolYearId, null);
    }
    
    /**
     * Get latest payments using view (more efficient)
     */
    public List<models.Payment> getLatestPayments(int limit, Integer schoolYearId, String semester) {
        // Try using view first
        try {
            return getLatestPaymentsFromView(limit, schoolYearId, semester);
        } catch (Exception e) {
            // Fallback to direct query
        }
        
        List<models.Payment> payments = new ArrayList<>();
        
        String sql = "SELECT " +
                    "sp.payable_id, " +
                    "b.student_id, " +
                    "s.student_number, " +
                    "s.fullname, " +
                    "sp.amount_paid as amount, " +
                    "d.due_date as payment_date, " +
                    "sp.status " +
                    "FROM student_payables sp " +
                    "JOIN belong b ON sp.belong_id = b.belong_id " +
                    "JOIN student s ON b.student_id = s.student_id " +
                    "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                    "LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                    "WHERE sp.amount_paid > 0 ";
        
        if (schoolYearId != null) {
            sql += "AND b.school_year_id = ? ";
        }
        
        if (semester != null) {
            // Match semester based on which amount is set
            if ("1st Sem".equals(semester)) {
                sql += "AND sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) ";
            } else if ("2nd Sem".equals(semester)) {
                sql += "AND sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) ";
            } else if ("Summer Sem".equals(semester)) {
                sql += "AND sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) ";
            }
        }
        
        sql += "ORDER BY d.due_date DESC, sp.payable_id DESC " +
               "LIMIT ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            pstmt.setInt(paramIndex, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                models.Payment payment = new models.Payment();
                payment.setId(rs.getInt("payable_id"));
                payment.setStudentId(rs.getInt("student_id"));
                payment.setAmount(rs.getDouble("amount"));
                
                java.sql.Date paymentDateSql = rs.getDate("payment_date");
                if (paymentDateSql != null) {
                    payment.setPaymentDate(paymentDateSql.toLocalDate());
                } else {
                    payment.setPaymentDate(java.time.LocalDate.now());
                }
                
                payment.setStatus(rs.getString("status"));
                payment.setStudentIdStr(rs.getString("student_number"));
                payment.setStudentName(rs.getString("fullname"));
                
                payments.add(payment);
            }
        } catch (SQLException e) {
            System.err.println("Error getting latest payments: " + e.getMessage());
            e.printStackTrace();
        }
        
        return payments;
    }
    
    /**
     * Get latest payments from view
     */
    private List<models.Payment> getLatestPaymentsFromView(int limit, Integer schoolYearId, String semester) {
        List<models.Payment> payments = new ArrayList<>();
        
        String sql = "SELECT payable_id, student_id, student_number, student_name, " +
                    "payable_amount, amount_paid, status, due_date " +
                    "FROM v_latest_payments WHERE 1=1";
        
        if (schoolYearId != null) {
            sql += " AND school_year_id = ?";
        }
        
        if (semester != null && !"All Semesters".equals(semester)) {
            sql += " AND semester_name = ?";
        }
        
        sql += " ORDER BY sort_key DESC LIMIT ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            if (semester != null && !"All Semesters".equals(semester)) {
                pstmt.setString(paramIndex++, semester);
            }
            pstmt.setInt(paramIndex, limit);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                models.Payment payment = new models.Payment();
                payment.setId(rs.getInt("payable_id"));
                payment.setStudentId(rs.getInt("student_id"));
                payment.setAmount(rs.getDouble("amount_paid"));
                
                java.sql.Date paymentDateSql = rs.getDate("due_date");
                if (paymentDateSql != null) {
                    payment.setPaymentDate(paymentDateSql.toLocalDate());
                } else {
                    payment.setPaymentDate(java.time.LocalDate.now());
                }
                
                payment.setStatus(rs.getString("status"));
                payment.setStudentIdStr(rs.getString("student_number"));
                payment.setStudentName(rs.getString("student_name"));
                
                payments.add(payment);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting latest payments from view", e);
        }
        
        return payments;
    }
    
    /**
     * Get top payers (for dashboard compatibility)
     * Returns Payment objects instead of PaymentView
     */
    public List<models.Payment> getTopPayers(int limit) {
        return getTopPayers(limit, null);
    }
    
    public List<models.Payment> getTopPayers(int limit, Integer schoolYearId) {
        return getTopPayers(limit, schoolYearId, null);
    }
    
    /**
     * Get top payers using the database view (more efficient)
     */
    public List<models.Payment> getTopPayersUsingView(int limit, Integer schoolYearId, String semester) {
        List<models.Payment> topPayers = new ArrayList<>();
        
        String sql = "SELECT student_id, student_number, student_name, school_year, total_amount_paid, payment_count " +
                    "FROM v_top_payers WHERE 1=1 ";
        
        if (schoolYearId != null) {
            sql += "AND school_year_id = ? ";
        }
        
        if (semester != null && !"All Semesters".equals(semester)) {
            sql += "AND semester_name = ? ";
        }
        
        sql += "ORDER BY total_amount_paid DESC LIMIT ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            if (semester != null && !"All Semesters".equals(semester)) {
                pstmt.setString(paramIndex++, semester);
            }
            pstmt.setInt(paramIndex, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                models.Payment payment = new models.Payment();
                payment.setStudentId(rs.getInt("student_id"));
                payment.setStudentIdStr(rs.getString("student_number"));
                payment.setStudentName(rs.getString("student_name"));
                payment.setAmount(rs.getDouble("total_amount_paid"));
                topPayers.add(payment);
            }
        } catch (SQLException e) {
            System.err.println("Error getting top payers from view: " + e.getMessage());
            e.printStackTrace();
        }
        
        return topPayers;
    }
    
    public List<models.Payment> getTopPayers(int limit, Integer schoolYearId, String semester) {
        List<models.Payment> topPayers = new ArrayList<>();
        
        // Try using view first, fallback to direct query
        try {
            topPayers = getTopPayersUsingView(limit, schoolYearId, semester);
            if (!topPayers.isEmpty()) {
                return topPayers;
            }
        } catch (Exception e) {
            // Fallback to direct query
        }
        
        String sql = "SELECT " +
                    "s.student_id, " +
                    "s.student_number, " +
                    "s.fullname, " +
                    "SUM(sp.amount_paid) as total_amount " +
                    "FROM student_payables sp " +
                    "JOIN belong b ON sp.belong_id = b.belong_id " +
                    "JOIN student s ON b.student_id = s.student_id " +
                    "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                    "WHERE sp.amount_paid > 0 ";
        
        if (schoolYearId != null) {
            sql += "AND b.school_year_id = ? ";
        }
        
        if (semester != null) {
            // Match semester based on which amount is set
            if ("1st Sem".equals(semester)) {
                sql += "AND sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) ";
            } else if ("2nd Sem".equals(semester)) {
                sql += "AND sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) ";
            } else if ("Summer Sem".equals(semester)) {
                sql += "AND sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) ";
            }
        }
        
        sql += "GROUP BY s.student_id, s.student_number, s.fullname " +
               "ORDER BY total_amount DESC " +
               "LIMIT ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            pstmt.setInt(paramIndex, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                models.Payment payment = new models.Payment();
                payment.setStudentId(rs.getInt("student_id"));
                payment.setStudentIdStr(rs.getString("student_number"));
                payment.setStudentName(rs.getString("fullname"));
                payment.setAmount(rs.getDouble("total_amount"));
                topPayers.add(payment);
            }
        } catch (SQLException e) {
            System.err.println("Error getting top payers: " + e.getMessage());
            e.printStackTrace();
        }
        
        return topPayers;
    }
    
    /**
     * Get all students with due dates for notifications
     * @param schoolYearId Optional school year filter
     * @param within7Days If true, only return students with due dates within 7 days or overdue
     * @return List of PaymentView with due date information
     */
    public List<PaymentView> getNotifications(Integer schoolYearId, boolean within7Days) {
        List<PaymentView> notifications = new ArrayList<>();
        
        String sql = "SELECT " +
                    "s.student_id, " +
                    "s.student_number, " +
                    "s.fullname, " +
                    "COALESCE(MAX(CASE WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN sem.first_sem_amount ELSE 0 END), 0) as first_sem, " +
                    "COALESCE(MAX(CASE WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN sem.second_sem_amount ELSE 0 END), 0) as second_sem, " +
                    "COALESCE(MAX(CASE WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN sem.summer_sem_amount ELSE 0 END), 0) as summer_sem, " +
                    "COALESCE(SUM(sp.downpayment_amount), 0) as total_downpayment, " +
                    "COALESCE(SUM(sp.amount_paid), 0) as amount_paid, " +
                    "MAX(d.due_date) as due_date, " +
                    "MAX(sp.status) as status " +
                    "FROM student s " +
                    "LEFT JOIN belong b ON s.student_id = b.student_id " +
                    (schoolYearId != null ? "AND b.school_year_id = ? " : "") +
                    "LEFT JOIN semester sem ON b.semester_id = sem.semester_id " +
                    "LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                    "LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                    "WHERE d.due_date IS NOT NULL " +
                    "AND (sp.status IS NULL OR sp.status != 'Paid') "; // Exclude paid students
        
        if (within7Days) {
            // Get due dates that are today or within 7 days (today - 7 days to today + 7 days)
            // This includes overdue dates and upcoming dates
            sql += "AND d.due_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                   "AND d.due_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) ";
        }
        
        sql += "GROUP BY s.student_id, s.student_number, s.fullname " +
               "HAVING COALESCE(SUM(sp.amount_paid), 0) < COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) " + // Exclude fully paid
               "ORDER BY d.due_date ASC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PaymentView view = new PaymentView();
                view.setStudentId(rs.getInt("student_id"));
                view.setStudentNumber(rs.getString("student_number"));
                view.setStudentName(rs.getString("fullname"));
                view.setFirstSemAmount(rs.getDouble("first_sem"));
                view.setSecondSemAmount(rs.getDouble("second_sem"));
                view.setSummerSemAmount(rs.getDouble("summer_sem"));
                
                double total = view.getFirstSemAmount() + view.getSecondSemAmount() + view.getSummerSemAmount();
                view.setTotalAmount(total);
                
                double totalDownPayment = rs.getDouble("total_downpayment");
                view.setDownPayment(totalDownPayment > 0 ? totalDownPayment : 0);
                
                view.setAmountPaid(rs.getDouble("amount_paid"));
                
                java.sql.Date dueDateSql = rs.getDate("due_date");
                if (dueDateSql != null) {
                    view.setDueDate(dueDateSql.toLocalDate());
                }
                
                // Always recalculate status based on current amount paid vs total
                if (Math.abs(view.getAmountPaid() - view.getTotalAmount()) < 0.01 || view.getAmountPaid() >= view.getTotalAmount()) {
                    view.setStatus("Paid");
                    // Clear due date when fully paid
                    view.setDueDate(null);
                } else if (view.getAmountPaid() > 0) {
                    view.setStatus("Partial");
                } else {
                    view.setStatus("UNPAID");
                }
                
                notifications.add(view);
            }
        } catch (SQLException e) {
            System.err.println("Error getting notifications: " + e.getMessage());
            e.printStackTrace();
        }
        
        return notifications;
    }
    
    /**
     * Get monthly payment data for chart
     * Returns a map with month names as keys and arrays [paidAmount, unpaidAmount] as values
     */
    public java.util.Map<String, double[]> getMonthlyPaymentData(Integer schoolYearId) {
        return getMonthlyPaymentData(schoolYearId, null);
    }
    
    /**
     * Get monthly payment data using view (more efficient)
     */
    public java.util.Map<String, double[]> getMonthlyPaymentData(Integer schoolYearId, String semester) {
        // Try using view first
        try {
            return getMonthlyPaymentDataFromView(schoolYearId, semester);
        } catch (Exception e) {
            // Fallback to direct query
        }
        
        java.util.Map<String, double[]> monthlyData = new java.util.HashMap<>();
        
        // Initialize all months with zero values
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (String month : months) {
            monthlyData.put(month, new double[]{0.0, 0.0});
        }
        
        // Query to get paid amounts by month (using due_date or payment_date)
        String sql = "SELECT " +
                    "DATE_FORMAT(COALESCE(d.due_date, NOW()), '%b') as month_name, " +
                    "SUM(CASE WHEN sp.status = 'Paid' OR (sp.amount_paid >= (sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount)) THEN sp.amount_paid ELSE 0 END) as paid_amount, " +
                    "SUM(CASE WHEN sp.status != 'Paid' AND (sp.amount_paid < (sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount) OR sp.amount_paid IS NULL) THEN (sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount) - COALESCE(sp.amount_paid, 0) ELSE 0 END) as unpaid_amount " +
                    "FROM student_payables sp " +
                    "JOIN belong b ON sp.belong_id = b.belong_id " +
                    "JOIN semester sem ON b.semester_id = sem.semester_id " +
                    "LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                    "WHERE 1=1 ";
        
        if (schoolYearId != null) {
            sql += "AND b.school_year_id = ? ";
        }
        
        if (semester != null) {
            // Match semester based on which amount is set
            if ("1st Sem".equals(semester)) {
                sql += "AND sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) ";
            } else if ("2nd Sem".equals(semester)) {
                sql += "AND sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) ";
            } else if ("Summer Sem".equals(semester)) {
                sql += "AND sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) ";
            }
        }
        
        sql += "GROUP BY DATE_FORMAT(COALESCE(d.due_date, NOW()), '%b'), DATE_FORMAT(COALESCE(d.due_date, NOW()), '%m') " +
               "ORDER BY DATE_FORMAT(COALESCE(d.due_date, NOW()), '%m')";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String monthName = rs.getString("month_name");
                if (monthName != null) {
                    // Convert to 3-letter format (Jan, Feb, etc.)
                    String monthKey = monthName.length() >= 3 ? monthName.substring(0, 3) : monthName;
                    double paid = rs.getDouble("paid_amount");
                    double unpaid = rs.getDouble("unpaid_amount");
                    monthlyData.put(monthKey, new double[]{paid, unpaid});
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting monthly payment data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return monthlyData;
    }
    
    /**
     * Get monthly payment data from view
     */
    private java.util.Map<String, double[]> getMonthlyPaymentDataFromView(Integer schoolYearId, String semester) {
        java.util.Map<String, double[]> monthlyData = new java.util.HashMap<>();
        
        // Initialize all months with zero values
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (String month : months) {
            monthlyData.put(month, new double[]{0.0, 0.0});
        }
        
        String sql = "SELECT month_abbr, total_amount_paid as paid_amount, total_remaining_balance as unpaid_amount " +
                    "FROM v_payment_statistics_monthly WHERE 1=1";
        
        if (schoolYearId != null) {
            sql += " AND school_year_id = ?";
        }
        
        if (semester != null && !"All Semesters".equals(semester)) {
            sql += " AND semester_name = ?";
        }
        
        sql += " ORDER BY payment_month";
        
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
            while (rs.next()) {
                String monthAbbr = rs.getString("month_abbr");
                if (monthAbbr != null) {
                    String monthKey = monthAbbr.length() >= 3 ? monthAbbr.substring(0, 3) : monthAbbr;
                    double paid = rs.getDouble("paid_amount");
                    double unpaid = rs.getDouble("unpaid_amount");
                    monthlyData.put(monthKey, new double[]{paid, unpaid});
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting monthly payment data from view", e);
        }
        
        return monthlyData;
    }
    
    /**
     * Get all students who have paid (status = "Paid" or amount_paid >= total_amount)
     * @param schoolYearId Optional school year filter
     * @return List of PaymentReportView with student payment information
     */
    public List<models.PaymentReportView> getStudentsWhoPaid(Integer schoolYearId) {
        List<models.PaymentReportView> reports = new ArrayList<>();
        
        String sql = "SELECT " +
                    "s.student_id, " +
                    "s.student_number, " +
                    "s.fullname, " +
                    "s.major, " +
                    "s.year, " +
                    "COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) as total_amount, " +
                    "COALESCE(SUM(sp.amount_paid), 0) as amount_paid, " +
                    "MAX(d.due_date) as payment_date, " +
                    "CASE " +
                    "  WHEN COALESCE(SUM(sp.amount_paid), 0) >= COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) THEN 'Paid' " +
                    "  ELSE 'Partial' " +
                    "END as status " +
                    "FROM student s " +
                    "INNER JOIN belong b ON s.student_id = b.student_id " +
                    "INNER JOIN semester sem ON b.semester_id = sem.semester_id " +
                    "LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id " +
                    "LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id " +
                    "WHERE 1=1 ";
        
        if (schoolYearId != null) {
            sql += "AND b.school_year_id = ? ";
        }
        
        sql += "GROUP BY s.student_id, s.student_number, s.fullname, s.major, s.year " +
               "HAVING COALESCE(SUM(sp.amount_paid), 0) > 0 " +
               "ORDER BY s.student_id DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                models.PaymentReportView report = new models.PaymentReportView();
                report.setStudentId(rs.getInt("student_id"));
                report.setStudentNumber(rs.getString("student_number"));
                report.setStudentName(rs.getString("fullname"));
                report.setMajor(rs.getString("major"));
                report.setYear(rs.getString("year"));
                report.setTotalAmount(rs.getDouble("total_amount"));
                report.setAmountPaid(rs.getDouble("amount_paid"));
                
                java.sql.Date paymentDateSql = rs.getDate("payment_date");
                if (paymentDateSql != null) {
                    report.setPaymentDate(paymentDateSql.toLocalDate());
                }
                
                report.setStatus(rs.getString("status"));
                reports.add(report);
            }
        } catch (SQLException e) {
            System.err.println("Error getting students who paid: " + e.getMessage());
            e.printStackTrace();
        }
        
        return reports;
    }
    
    /**
     * Get total amount paid by all students who paid
     * @param schoolYearId Optional school year filter
     * @return Total amount paid
     */
    public double getTotalPaidAmount(Integer schoolYearId) {
        String sql = "SELECT COALESCE(SUM(sp.amount_paid), 0) as total_paid " +
                    "FROM student_payables sp " +
                    "JOIN belong b ON sp.belong_id = b.belong_id " +
                    "JOIN semester sem ON b.semester_id = sem.semester_id " +
                    "WHERE sp.amount_paid > 0 ";
        
        if (schoolYearId != null) {
            sql += "AND b.school_year_id = ? ";
        }
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (schoolYearId != null) {
                pstmt.setInt(paramIndex++, schoolYearId);
            }
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total_paid");
            }
        } catch (SQLException e) {
            System.err.println("Error getting total paid amount: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0.0;
    }
    
    /**
     * Get all payment views using stored procedure
     */
    private List<PaymentView> getAllPaymentViewsUsingProcedure(
            Integer schoolYearId, String semester, String searchTerm, String year, String program) {
        List<PaymentView> payments = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.CallableStatement cstmt = conn.prepareCall("{CALL sp_get_payments(?, ?, ?, ?, ?)}")) {
            
            cstmt.setObject(1, schoolYearId);
            cstmt.setString(2, semester);
            cstmt.setString(3, searchTerm);
            cstmt.setString(4, year);
            cstmt.setString(5, program);
            
            ResultSet rs = cstmt.executeQuery();
            while (rs.next()) {
                PaymentView view = new PaymentView();
                view.setStudentId(rs.getInt("student_id"));
                view.setStudentNumber(rs.getString("student_number"));
                view.setStudentName(rs.getString("student_name"));
                view.setFirstSemAmount(rs.getDouble("first_sem_amount"));
                view.setSecondSemAmount(rs.getDouble("second_sem_amount"));
                view.setSummerSemAmount(rs.getDouble("summer_sem_amount"));
                
                double total = view.getFirstSemAmount() + view.getSecondSemAmount() + view.getSummerSemAmount();
                view.setTotalAmount(total);
                
                double totalDownPayment = rs.getDouble("total_downpayment");
                view.setDownPayment(totalDownPayment > 0 ? totalDownPayment : 0);
                
                view.setAmountPaid(rs.getDouble("amount_paid"));
                
                java.sql.Date dueDateSql = rs.getDate("due_date");
                if (dueDateSql != null) {
                    view.setDueDate(dueDateSql.toLocalDate());
                }
                
                // Always recalculate status based on current amount paid vs total of all semesters
                // Status should be dynamically calculated, not stored
                if (Math.abs(view.getAmountPaid() - view.getTotalAmount()) < 0.01 || view.getAmountPaid() >= view.getTotalAmount()) {
                    view.setStatus("Paid");
                    // Clear due date when fully paid
                    view.setDueDate(null);
                } else if (view.getAmountPaid() > 0) {
                    view.setStatus("Partial");
                } else {
                    view.setStatus("UNPAID");
                }
                
                payments.add(view);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting payment views from procedure", e);
        }
        
        return payments;
    }
    
    /**
     * Save payment using stored procedure
     */
    private boolean savePaymentUsingProcedure(
            int studentId, Integer schoolYearId, double downPayment, double amountPaid, 
            LocalDate dueDate, String status) {
        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.CallableStatement cstmt = conn.prepareCall("{CALL sp_save_payment(?, ?, ?, ?, ?, ?, ?)}")) {
            
            cstmt.setInt(1, studentId);
            cstmt.setObject(2, schoolYearId);
            cstmt.setDouble(3, downPayment);
            cstmt.setDouble(4, amountPaid);
            if (dueDate != null) {
                cstmt.setDate(5, java.sql.Date.valueOf(dueDate));
            } else {
                cstmt.setNull(5, java.sql.Types.DATE);
            }
            cstmt.setString(6, status);
            cstmt.registerOutParameter(7, java.sql.Types.VARCHAR);
            
            cstmt.executeUpdate();
            
            String result = cstmt.getString(7);
            return "CREATED".equals(result) || "UPDATED".equals(result);
        } catch (SQLException e) {
            throw new RuntimeException("Error saving payment from procedure", e);
        }
    }
    
    /**
     * Delete payment using stored procedure
     */
    private boolean deletePaymentUsingProcedure(int studentId, Integer schoolYearId) {
        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.CallableStatement cstmt = conn.prepareCall("{CALL sp_delete_payment(?, ?, ?)}")) {
            
            cstmt.setInt(1, studentId);
            cstmt.setObject(2, schoolYearId);
            cstmt.registerOutParameter(3, java.sql.Types.VARCHAR);
            
            cstmt.executeUpdate();
            
            String result = cstmt.getString(3);
            return "DELETED".equals(result);
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting payment from procedure", e);
        }
    }
    
    /**
     * Get payment statistics using stored procedure
     */
    public void getPaymentStatistics(Integer schoolYearId, String semester,
            java.util.function.Consumer<PaymentStatistics> callback) {
        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.CallableStatement cstmt = conn.prepareCall("{CALL sp_get_payment_statistics(?, ?, ?, ?, ?, ?, ?, ?)}")) {
            
            cstmt.setObject(1, schoolYearId);
            cstmt.setString(2, semester);
            cstmt.registerOutParameter(3, java.sql.Types.INTEGER);
            cstmt.registerOutParameter(4, java.sql.Types.DECIMAL);
            cstmt.registerOutParameter(5, java.sql.Types.DECIMAL);
            cstmt.registerOutParameter(6, java.sql.Types.INTEGER);
            cstmt.registerOutParameter(7, java.sql.Types.INTEGER);
            cstmt.registerOutParameter(8, java.sql.Types.INTEGER);
            
            cstmt.executeUpdate();
            
            PaymentStatistics stats = new PaymentStatistics();
            stats.totalPayments = cstmt.getInt(3);
            stats.totalAmountPaid = cstmt.getBigDecimal(4).doubleValue();
            stats.totalPayable = cstmt.getBigDecimal(5).doubleValue();
            stats.paidCount = cstmt.getInt(6);
            stats.partialCount = cstmt.getInt(7);
            stats.unpaidCount = cstmt.getInt(8);
            
            if (callback != null) {
                callback.accept(stats);
            }
        } catch (SQLException e) {
            System.err.println("Error getting payment statistics from procedure: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Inner class for payment statistics
     */
    public static class PaymentStatistics {
        public int totalPayments;
        public double totalAmountPaid;
        public double totalPayable;
        public int paidCount;
        public int partialCount;
        public int unpaidCount;
    }
}

