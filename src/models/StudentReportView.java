package models;

/**
 * Model for student report view
 */
public class StudentReportView {
    private String studentNumber;
    private String studentName;
    private String major;
    private String year;
    private String schoolYear;
    private String semester;
    private String status;
    
    public StudentReportView() {
    }
    
    public StudentReportView(String studentNumber, String studentName, String major, 
                            String year, String schoolYear, String semester, String status) {
        this.studentNumber = studentNumber;
        this.studentName = studentName;
        this.major = major;
        this.year = year;
        this.schoolYear = schoolYear;
        this.semester = semester;
        this.status = status;
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
    
    public String getSchoolYear() {
        return schoolYear;
    }
    
    public void setSchoolYear(String schoolYear) {
        this.schoolYear = schoolYear;
    }
    
    public String getSemester() {
        return semester;
    }
    
    public void setSemester(String semester) {
        this.semester = semester;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}

