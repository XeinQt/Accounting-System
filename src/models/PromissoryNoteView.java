package models;

import java.time.LocalDate;

public class PromissoryNoteView {
    private int studentId;
    private String studentNumber;
    private String studentName;
    private String major;
    private String year;
    private String department;
    private String college;
    private double totalAmount;
    private double amountPaid;
    private double remainingBalance;
    private LocalDate dueDate;
    private String status;
    
    public PromissoryNoteView() {}
    
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
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getCollege() {
        return college;
    }
    
    public void setCollege(String college) {
        this.college = college;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public double getAmountPaid() {
        return amountPaid;
    }
    
    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }
    
    public double getRemainingBalance() {
        return remainingBalance;
    }
    
    public void setRemainingBalance(double remainingBalance) {
        this.remainingBalance = remainingBalance;
    }
    
    public LocalDate getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDueDateFormatted() {
        if (dueDate == null) return "";
        return String.format("%02d/%02d/%04d", dueDate.getMonthValue(), dueDate.getDayOfMonth(), dueDate.getYear());
    }
    
    public String getTotalAmountFormatted() {
        return String.format("P%.2f", totalAmount);
    }
    
    public String getRemainingBalanceFormatted() {
        return String.format("P%.2f", remainingBalance);
    }
}

