package models;

import java.time.LocalDate;

public class PromissoryNote {
    private int promissoryId;
    private LocalDate createdDate;
    private LocalDate dueDateExtended;
    private double remainingBalanceSnapshot;
    private String noteText;

    public PromissoryNote() {}

    public PromissoryNote(LocalDate createdDate, double remainingBalanceSnapshot) {
        this.createdDate = createdDate;
        this.remainingBalanceSnapshot = remainingBalanceSnapshot;
    }

    public int getPromissoryId() {
        return promissoryId;
    }

    public void setPromissoryId(int promissoryId) {
        this.promissoryId = promissoryId;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDate getDueDateExtended() {
        return dueDateExtended;
    }

    public void setDueDateExtended(LocalDate dueDateExtended) {
        this.dueDateExtended = dueDateExtended;
    }

    public double getRemainingBalanceSnapshot() {
        return remainingBalanceSnapshot;
    }

    public void setRemainingBalanceSnapshot(double remainingBalanceSnapshot) {
        this.remainingBalanceSnapshot = remainingBalanceSnapshot;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }
}

