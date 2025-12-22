package models;

public class Student {
    private int studentId;
    private String studentNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String major;
    private String year;
    private Integer schoolYearId;
    private String status; // "active" or "deactivated"
    private String semester; // "1st Sem", "2nd Sem", "Summer Sem"

    public Student() {}

    public Student(String studentNumber, String firstName, String middleName, String lastName) {
        this.studentNumber = studentNumber;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
    }
    
    // Helper method to get full name for display
    public String getFullname() {
        StringBuilder fullname = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            fullname.append(firstName);
        }
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (fullname.length() > 0) fullname.append(" ");
            fullname.append(middleName);
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullname.length() > 0) fullname.append(" ");
            fullname.append(lastName);
        }
        return fullname.toString();
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public Integer getSchoolYearId() {
        return schoolYearId;
    }

    public void setSchoolYearId(Integer schoolYearId) {
        this.schoolYearId = schoolYearId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }
}

