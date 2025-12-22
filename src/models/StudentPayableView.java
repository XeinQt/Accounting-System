package models;

import java.time.LocalDate;

public class StudentPayableView {
    private int studentId;
    private String studentNumber;
    private String studentName;
    private String program;
    private String year;
    private String semester;
    private double firstSemAmount;
    private double secondSemAmount;
    private double summerSemAmount;
    private double totalAmount;
    private LocalDate dueDate;
    
    public StudentPayableView() {}
    
    public StudentPayableView(int studentId, String studentNumber, String studentName) {
        this.studentId = studentId;
        this.studentNumber = studentNumber;
        this.studentName = studentName;
        this.firstSemAmount = 0.0;
        this.secondSemAmount = 0.0;
        this.summerSemAmount = 0.0;
        this.totalAmount = 0.0;
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
    
    public double getFirstSemAmount() {
        return firstSemAmount;
    }
    
    public void setFirstSemAmount(double firstSemAmount) {
        this.firstSemAmount = firstSemAmount;
        calculateTotal();
    }
    
    public double getSecondSemAmount() {
        return secondSemAmount;
    }
    
    public void setSecondSemAmount(double secondSemAmount) {
        this.secondSemAmount = secondSemAmount;
        calculateTotal();
    }
    
    public double getSummerSemAmount() {
        return summerSemAmount;
    }
    
    public void setSummerSemAmount(double summerSemAmount) {
        this.summerSemAmount = summerSemAmount;
        calculateTotal();
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    private void calculateTotal() {
        this.totalAmount = firstSemAmount + secondSemAmount + summerSemAmount;
    }
    
    // Format methods for display
    public String getFirstSemFormatted() {
        return String.format("P%.2f", firstSemAmount);
    }
    
    public String getSecondSemFormatted() {
        return String.format("P%.2f", secondSemAmount);
    }
    
    public String getSummerSemFormatted() {
        return String.format("P%.2f", summerSemAmount);
    }
    
    public String getTotalFormatted() {
        return String.format("P%.2f", totalAmount);
    }
    
    public String getProgram() {
        return program;
    }
    
    public void setProgram(String program) {
        this.program = program;
    }
    
    public String getYear() {
        return year;
    }
    
    public void setYear(String year) {
        this.year = year;
    }
    
    public String getSemester() {
        return semester;
    }
    
    public void setSemester(String semester) {
        this.semester = semester;
    }
    
    // Get payable amount based on semester
    public String getPayableAmount() {
        if (semester != null) {
            if (semester.equals("1st Sem")) {
                return getFirstSemFormatted();
            } else if (semester.equals("2nd Sem")) {
                return getSecondSemFormatted();
            } else if (semester.equals("Summer Sem")) {
                return getSummerSemFormatted();
            }
        }
        return "P0.00";
    }
    
    public LocalDate getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
    
    public String getDueDateFormatted() {
        if (dueDate == null) return "";
        return String.format("%02d/%02d/%04d", dueDate.getMonthValue(), dueDate.getDayOfMonth(), dueDate.getYear());
    }
}

