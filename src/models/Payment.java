package models;

import java.time.LocalDate;

public class Payment {
    private int id;
    private int studentId;
    private Integer payableId;
    private double amount;
    private LocalDate paymentDate;
    private String status;
    private String notes;
    private String studentName;
    private String studentIdStr;

    public Payment() {}

    public Payment(int studentId, double amount, LocalDate paymentDate) {
        this.studentId = studentId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.status = "Paid";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public Integer getPayableId() {
        return payableId;
    }

    public void setPayableId(Integer payableId) {
        this.payableId = payableId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentIdStr() {
        return studentIdStr;
    }

    public void setStudentIdStr(String studentIdStr) {
        this.studentIdStr = studentIdStr;
    }
}

