package models;

public class Belong {
    private int belongId;
    private int studentId;
    private int schoolYearId;
    private int semesterId;

    public Belong() {}

    public Belong(int studentId, int schoolYearId, int semesterId) {
        this.studentId = studentId;
        this.schoolYearId = schoolYearId;
        this.semesterId = semesterId;
    }

    public int getBelongId() {
        return belongId;
    }

    public void setBelongId(int belongId) {
        this.belongId = belongId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getSchoolYearId() {
        return schoolYearId;
    }

    public void setSchoolYearId(int schoolYearId) {
        this.schoolYearId = schoolYearId;
    }

    public int getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(int semesterId) {
        this.semesterId = semesterId;
    }
}

