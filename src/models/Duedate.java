package models;

import java.time.LocalDate;

public class Duedate {
    private int duedateId;
    private LocalDate dueDate;
    private String message;
    private Integer promissoryId;

    public Duedate() {}

    public Duedate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public int getDuedateId() {
        return duedateId;
    }

    public void setDuedateId(int duedateId) {
        this.duedateId = duedateId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getPromissoryId() {
        return promissoryId;
    }

    public void setPromissoryId(Integer promissoryId) {
        this.promissoryId = promissoryId;
    }
}

