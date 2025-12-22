package models;

public class AcademicYearSemesterBalance {
    private String academicYear;
    private String semester;
    private double amount;
    
    public AcademicYearSemesterBalance() {}
    
    public AcademicYearSemesterBalance(String academicYear, String semester, double amount) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.amount = amount;
    }
    
    public String getAcademicYear() {
        return academicYear;
    }
    
    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }
    
    public String getSemester() {
        return semester;
    }
    
    public void setSemester(String semester) {
        this.semester = semester;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
}
