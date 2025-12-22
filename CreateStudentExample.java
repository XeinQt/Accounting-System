import dao.StudentDAO;
import models.Student;
import models.SchoolYear;
import dao.SchoolYearDAO;

/**
 * Example code showing how to create a new student
 * This is a standalone example without JavaFX UI components
 */
public class CreateStudentExample {
    
    public static void main(String[] args) {
        // Example: Creating a new student
        createNewStudentExample();
    }
    
    /**
     * Example method showing how to create a new student
     */
    public static void createNewStudentExample() {
        // Step 1: Create a StudentDAO instance
        StudentDAO studentDAO = new StudentDAO();
        SchoolYearDAO schoolYearDAO = new SchoolYearDAO();
        
        // Step 2: Create a new Student object and set all required fields
        Student newStudent = new Student();
        
        // Required fields
        newStudent.setStudentNumber("2024-00123");  // Unique student ID
        newStudent.setFirstName("John");
        newStudent.setMiddleName("Michael");  // Optional, can be null or empty
        newStudent.setLastName("Doe");
        newStudent.setMajor("BSIT - Bachelor of Science in Information Technology");
        newStudent.setYear("1st Year");
        newStudent.setSemester("1st Sem");  // Must be: "1st Sem", "2nd Sem", or "Summer Sem"
        newStudent.setStatus("active");  // New students are typically "active"
        
        // Step 3: Set the school year (you need to get it from SchoolYearDAO)
        // Option A: Get school year by year range (e.g., "2024-2025")
        SchoolYear schoolYear = schoolYearDAO.getSchoolYearByRange("2024-2025");
        if (schoolYear != null) {
            newStudent.setSchoolYearId(schoolYear.getSchoolYearId());
        } else {
            System.out.println("Error: School year not found!");
            return;
        }
        
        // Option B: Get active school years and select one
        // List<SchoolYear> activeYears = schoolYearDAO.getActiveSchoolYears();
        // if (!activeYears.isEmpty()) {
        //     newStudent.setSchoolYearId(activeYears.get(0).getSchoolYearId());
        // }
        
        // Step 4: Validate the student data before adding
        if (validateStudent(newStudent)) {
            // Step 5: Add the student to the database
            boolean success = studentDAO.addStudent(newStudent);
            
            if (success) {
                System.out.println("Student added successfully!");
                System.out.println("Student ID: " + newStudent.getStudentNumber());
                System.out.println("Name: " + newStudent.getFullname());
            } else {
                System.out.println("Failed to add student. Possible reasons:");
                System.out.println("- Student ID already exists in the same semester/year");
                System.out.println("- Duplicate name in the same semester/year");
                System.out.println("- Missing required fields");
            }
        } else {
            System.out.println("Student validation failed!");
        }
    }
    
    /**
     * Validates student data before adding
     * @param student The student to validate
     * @return true if valid, false otherwise
     */
    private static boolean validateStudent(Student student) {
        // Check required fields
        if (student.getStudentNumber() == null || student.getStudentNumber().trim().isEmpty()) {
            System.out.println("Error: Student ID is required!");
            return false;
        }
        
        if (student.getFirstName() == null || student.getFirstName().trim().isEmpty()) {
            System.out.println("Error: First name is required!");
            return false;
        }
        
        if (student.getLastName() == null || student.getLastName().trim().isEmpty()) {
            System.out.println("Error: Last name is required!");
            return false;
        }
        
        if (student.getSemester() == null || student.getSemester().trim().isEmpty()) {
            System.out.println("Error: Semester is required!");
            return false;
        }
        
        // Validate semester value
        String semester = student.getSemester();
        if (!semester.equals("1st Sem") && !semester.equals("2nd Sem") && !semester.equals("Summer Sem")) {
            System.out.println("Error: Semester must be '1st Sem', '2nd Sem', or 'Summer Sem'!");
            return false;
        }
        
        if (student.getSchoolYearId() == null) {
            System.out.println("Error: School Year is required!");
            return false;
        }
        
        // Validate name format (only letters, spaces, hyphens, apostrophes)
        if (!isValidName(student.getFirstName())) {
            System.out.println("Error: First name contains invalid characters!");
            return false;
        }
        
        if (student.getMiddleName() != null && !student.getMiddleName().trim().isEmpty()) {
            if (!isValidName(student.getMiddleName())) {
                System.out.println("Error: Middle name contains invalid characters!");
                return false;
            }
        }
        
        if (!isValidName(student.getLastName())) {
            System.out.println("Error: Last name contains invalid characters!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates that a name contains only letters, spaces, hyphens, and apostrophes
     */
    private static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return name.matches("^[a-zA-Z\\s\\-']+$");
    }
    
    /**
     * Alternative example: Creating a student using the constructor
     */
    public static void createStudentWithConstructor() {
        // Create student using constructor (only sets basic info)
        Student student = new Student("2024-00124", "Jane", "Marie", "Smith");
        
        // Then set additional fields
        student.setMajor("BSBA - Bachelor of Science in Business Administration");
        student.setYear("2nd Year");
        student.setSemester("2nd Sem");
        student.setStatus("active");
        
        // Set school year
        SchoolYearDAO schoolYearDAO = new SchoolYearDAO();
        SchoolYear schoolYear = schoolYearDAO.getSchoolYearByRange("2024-2025");
        if (schoolYear != null) {
            student.setSchoolYearId(schoolYear.getSchoolYearId());
        }
        
        // Add to database
        StudentDAO studentDAO = new StudentDAO();
        boolean success = studentDAO.addStudent(student);
        
        if (success) {
            System.out.println("Student created: " + student.getFullname());
        }
    }
}

