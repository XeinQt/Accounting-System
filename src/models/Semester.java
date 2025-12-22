package models;

public class Semester {
    private int semesterId;
    private double firstSemAmount;
    private double secondSemAmount;
    private double summerSemAmount;

    public Semester() {}

    public Semester(double firstSemAmount, double secondSemAmount, double summerSemAmount) {
        this.firstSemAmount = firstSemAmount;
        this.secondSemAmount = secondSemAmount;
        this.summerSemAmount = summerSemAmount;
    }

    public int getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(int semesterId) {
        this.semesterId = semesterId;
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
}

