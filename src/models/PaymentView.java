package models;

import java.time.LocalDate;

public class PaymentView {
    private int paymentId;
    private int studentId;
    private String studentNumber;
    private String studentName;
    private double downPayment; // 50% of total
    private double firstSemAmount;
    private double secondSemAmount;
    private double summerSemAmount;
    private double totalAmount;
    private double amountPaid;
    private LocalDate dueDate;
    private String status;
    
    public PaymentView() {}
    
    public int getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }
    
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
    
    public double getDownPayment() {
        return downPayment;
    }
    
    public void setDownPayment(double downPayment) {
        this.downPayment = downPayment;
    }
    
    public double getFirstSemAmount() {
        return firstSemAmount;
    }
    
    public void setFirstSemAmount(double firstSemAmount) {
        this.firstSemAmount = firstSemAmount;
    }
    
    public double getSecondSemAmount() {
        return secondSemAmount;
    }
    
    public void setSecondSemAmount(double secondSemAmount) {
        this.secondSemAmount = secondSemAmount;
    }
    
    public double getSummerSemAmount() {
        return summerSemAmount;
    }
    
    public void setSummerSemAmount(double summerSemAmount) {
        this.summerSemAmount = summerSemAmount;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
        // Don't auto-calculate down payment - admin must input it manually
        // Keep downPayment as is (0 if not set, or existing value if already set)
    }
    
    public double getAmountPaid() {
        return amountPaid;
    }
    
    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
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
    
    // Format methods for display
    public String getDownPaymentFormatted() {
        // Only show down payment if it's been entered (greater than 0)
        if (downPayment > 0) {
            return String.format("P%.2f", downPayment);
        }
        return ""; // Empty string if not entered yet
    }
    
    public String getFirstSemFormatted() {
        return String.format("P%.2f", firstSemAmount);
    }
    
    public String getSecondSemFormatted() {
        return String.format("P%.2f", secondSemAmount);
    }
    
    public String getSummerSemFormatted() {
        return String.format("P%.2f", summerSemAmount);
    }
    
    public String getAmountPaidFormatted() {
        return String.format("P%.2f", amountPaid);
    }
    
    public String getDueDateFormatted() {
        // If status is "Paid", show "Paid" instead of due date
        if ("Paid".equals(status)) {
            return "Paid";
        }
        // If no due date, return empty string
        if (dueDate == null) return "";
        // Otherwise, show the formatted due date
        return String.format("%02d/%02d/%04d", dueDate.getMonthValue(), dueDate.getDayOfMonth(), dueDate.getYear());
    }
}

