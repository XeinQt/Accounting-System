package models;

public class StudentPayable {
    private int payableId;
    private int belongId;
    private double downpaymentAmount;
    private double amountPaid;
    private double remainingBalance;
    private String status; // UNPAID, PARTIAL, PAID, OVERDUE
    private Integer duedateId;

    public StudentPayable() {}

    public StudentPayable(int belongId, double downpaymentAmount) {
        this.belongId = belongId;
        this.downpaymentAmount = downpaymentAmount;
        this.amountPaid = 0.00;
        this.remainingBalance = downpaymentAmount;
        this.status = "UNPAID";
    }

    public int getPayableId() {
        return payableId;
    }

    public void setPayableId(int payableId) {
        this.payableId = payableId;
    }

    public int getBelongId() {
        return belongId;
    }

    public void setBelongId(int belongId) {
        this.belongId = belongId;
    }

    public double getDownpaymentAmount() {
        return downpaymentAmount;
    }

    public void setDownpaymentAmount(double downpaymentAmount) {
        this.downpaymentAmount = downpaymentAmount;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDuedateId() {
        return duedateId;
    }

    public void setDuedateId(Integer duedateId) {
        this.duedateId = duedateId;
    }
}

