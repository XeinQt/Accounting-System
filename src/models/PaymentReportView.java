package models;

import java.time.LocalDate;

/**
 * View model for payment reports - shows students who have paid
 */
public class PaymentReportView {
    private int studentId;
    private String studentNumber;
    private String studentName;
    private String major;
    private String year;
    private double totalAmount;
    private double amountPaid;
    private LocalDate paymentDate;
    private String status;
    
    public PaymentReportView() {}
    
    public int getStudentId() {
        return studentId;
    }
    
    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }
    
    public String getStudentNumber() {
        return studentNumber;
    }
    
    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }
    
    public String getStudentName() {
        return studentName;
    }
    
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    public String getMajor() {
        return major;
    }
    
    public void setMajor(String major) {
        this.major = major;
    }
    
    public String getYear() {
        return year;
    }
    
    public void setYear(String year) {
        this.year = year;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getTotalAmountFormatted() {
        return String.format("P%.2f", totalAmount);
    }
    
    public double getAmountPaid() {
        return amountPaid;
    }
    
    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }
    
    public String getAmountPaidFormatted() {
        return String.format("P%.2f", amountPaid);
    }
    
    public LocalDate getPaymentDate() {
        return paymentDate;
    }
    
    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }
    
    public String getPaymentDateFormatted() {
        if (paymentDate == null) return "";
        return String.format("%02d/%02d/%04d", paymentDate.getMonthValue(), paymentDate.getDayOfMonth(), paymentDate.getYear());
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}

