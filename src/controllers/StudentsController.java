package controllers;

import dao.SchoolYearDAO;
import dao.StudentDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Student;
import models.SchoolYear;
import java.io.File;
import java.io.FileInputStream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StudentsController extends BaseController {
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> yearFilterCombo;
    @FXML private ComboBox<String> majorFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> yearComboBoxHeader;
    @FXML private ComboBox<String> semesterComboBoxHeader;
    @FXML private ComboBox<String> schoolYearCombo;
    @FXML private ComboBox<String> semesterCombo;
    
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, String> studentIdCol;
    @FXML private TableColumn<Student, String> studentNameCol;
    @FXML private TableColumn<Student, String> majorCol;
    @FXML private TableColumn<Student, String> yearCol;
    @FXML private TableColumn<Student, String> schoolYearCol;
    @FXML private TableColumn<Student, String> semesterCol;
    
    @FXML private TextField studentIdField;
    @FXML private TextField firstNameField;
    @FXML private TextField middleNameField;
    @FXML private TextField lastNameField;
    @FXML private ComboBox<String> majorField;
    @FXML private ComboBox<String> yearField;
    
    @FXML private Button addBtn;
    @FXML private Button importBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Button reactivateBtn;
    @FXML private Button clearBtn;
    
    @FXML private Button dashboardBtn;
    @FXML private Button studentsBtn;
    @FXML private Button payablesBtn;
    @FXML private Button paymentsBtn;
    @FXML private Button notificationsBtn;
    @FXML private Button schoolYearsBtn;
    @FXML private Button promissoryNotesBtn;
    @FXML private Button reportsBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;
    
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Text pageInfoLabel;
    @FXML private Text totalStudentsLabel;
    
    private StudentDAO studentDAO;
    private SchoolYearDAO schoolYearDAO;
    private ObservableList<Student> studentList;
    private ObservableList<Student> allStudentsList; // All students for pagination
    private Student selectedStudent;
    
    // Cache for school years to avoid N+1 query problem
    private final Map<Integer, String> schoolYearCache = new HashMap<>();
    
    // Pagination variables
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;
    private int totalStudents = 0;
    
    @FXML
    public void initialize() {
        studentDAO = new StudentDAO();
        schoolYearDAO = new SchoolYearDAO();
        studentList = FXCollections.observableArrayList();
        allStudentsList = FXCollections.observableArrayList();
        
        setupTable();
        setupSidebarButtons();
        loadSchoolYears();
        loadSchoolYearCache(); // Load school year cache to avoid N+1 queries
        setupHeaderDropdowns();
        autoSelectSemester(); // Automatically select semester based on current month
        loadStudents();
        loadFilters();
        setupEventHandlers();
        setupInputStyles();
        setupButtonStates();
        setupPagination();
    }
    
    private void setupSidebarButtons() {
        setupSidebarButtonHover(dashboardBtn, false);
        setupSidebarButtonHover(studentsBtn, true);
        setupSidebarButtonHover(payablesBtn, false);
        setupSidebarButtonHover(paymentsBtn, false);
        setupSidebarButtonHover(schoolYearsBtn, false);
        setupSidebarButtonHover(promissoryNotesBtn, false);
        setupSidebarButtonHover(settingsBtn, false);
        setupSidebarButtonHover(logoutBtn, false);
    }
    
    private void setupButtonStates() {
        // Initially: Add enabled, Update/Delete/Reactivate disabled
        addBtn.setDisable(false);
        updateBtn.setDisable(true);
        deleteBtn.setDisable(true);
        reactivateBtn.setDisable(true);
    }
    
    private void setupInputStyles() {
        // Apply focus styles to all text fields
        TextField[] textFields = {studentIdField, firstNameField, middleNameField, lastNameField, searchField};
        
        for (TextField field : textFields) {
            if (field != null) {
                field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (isNowFocused) {
                        field.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #7B76F1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 14; -fx-text-fill: #333; -fx-prompt-text-fill: #999; -fx-cursor: text; -fx-effect: dropshadow(gaussian, rgba(123, 118, 241, 0.2), 8, 0, 0, 2);");
                    } else {
                        field.setStyle("-fx-background-color: #F8F9FA; -fx-border-color: #E0E0E0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-width: 1.5; -fx-padding: 6 10; -fx-font-size: 14; -fx-text-fill: #333; -fx-prompt-text-fill: #999; -fx-cursor: text;");
                    }
                });
            }
        }
        
        // Apply focus styles to all combo boxes (including majorField which is now a ComboBox)
        @SuppressWarnings("unchecked")
        ComboBox<String>[] comboBoxes = new ComboBox[]{
            majorField, schoolYearCombo, semesterCombo, yearField, 
            yearFilterCombo, majorFilterCombo, statusFilterCombo
        };
        
        for (ComboBox<String> combo : comboBoxes) {
            if (combo != null) {
                // Set up cell factory to ensure text is fully visible in dropdown list
                combo.setCellFactory(listView -> new javafx.scene.control.ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                            setStyle(null);
                        } else {
                            setText(item);
                            setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                            setStyle("-fx-font-size: 14; -fx-text-fill: #333;");
                        }
                    }
                });
                
                // Don't override button cell - let ComboBox handle it naturally for proper text display
                // This allows both prompt text and selected values to display correctly
                
                combo.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (isNowFocused) {
                        combo.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #7B76F1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-width: 2; -fx-font-size: 14; -fx-text-fill: #333; -fx-prompt-text-fill: #999; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(123, 118, 241, 0.2), 8, 0, 0, 2);");
                    } else {
                        combo.setStyle("-fx-background-color: #F8F9FA; -fx-border-color: #E0E0E0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-width: 1.5; -fx-font-size: 14; -fx-text-fill: #333; -fx-prompt-text-fill: #999; -fx-cursor: hand;");
                    }
                });
            }
        }
    }
    
    private void setupTable() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentNumber"));
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("fullname"));
        majorCol.setCellValueFactory(new PropertyValueFactory<>("major"));
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        
        // School Year column - use cached school year data to avoid N+1 query problem
        schoolYearCol.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            if (student.getSchoolYearId() != null) {
                String yearRange = schoolYearCache.get(student.getSchoolYearId());
                if (yearRange != null) {
                    return new javafx.beans.property.SimpleStringProperty(yearRange);
                }
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        
        // Semester column - use PropertyValueFactory for better performance (no lambda overhead)
        semesterCol.setCellValueFactory(new PropertyValueFactory<>("semester"));
        
        // Set fixed cell size to show exactly 10 rows (28px per row)
        studentsTable.setFixedCellSize(28.0);
        
        // Optimize table performance - disable unnecessary updates
        studentsTable.setEditable(false);
        
        // Set column resize policy to fill table width
        studentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Make table fill available width - remove fixed width constraints
        studentsTable.setMaxWidth(Double.MAX_VALUE);
        
        // Calculate exact height: Use the formula for fixed cell size tables
        // Height = header (~32px) + (number of rows * fixedCellSize) + borders (~2px)
        // For exactly 10 rows: 32 + (10 * 28) + 2 = 314px
        // But JavaFX might add slight padding, so we'll measure dynamically
        Platform.runLater(() -> {
            // Wait for skin to be created, then measure
            if (studentsTable.getSkin() != null) {
                // Try to get actual header height
                javafx.scene.Node header = studentsTable.lookup(".column-header-background");
                double headerHeight = 32.0; // Default fallback
                
                if (header != null) {
                    headerHeight = header.getBoundsInLocal().getHeight();
                }
                
                // Calculate exact height: header + (10 rows * 28px)
                double exactHeight = headerHeight + (PAGE_SIZE * studentsTable.getFixedCellSize());
                
                studentsTable.setPrefHeight(exactHeight);
                studentsTable.setMinHeight(exactHeight);
                studentsTable.setMaxHeight(exactHeight);
            } else {
                // Fallback if skin not ready: use calculated value
                double exactHeight = 32.0 + (PAGE_SIZE * 28.0);
                studentsTable.setPrefHeight(exactHeight);
                studentsTable.setMinHeight(exactHeight);
                studentsTable.setMaxHeight(exactHeight);
            }
        });
        
        // Also set initial height as fallback (slightly conservative to prevent extra rows)
        // Using 30px header + 280px (10 rows) = 310px, which should be exact
        double initialHeight = 30.0 + (PAGE_SIZE * 28.0);
        studentsTable.setPrefHeight(initialHeight);
        studentsTable.setMinHeight(initialHeight);
        studentsTable.setMaxHeight(initialHeight);
        
        // Hide scrollbars using a helper method that runs repeatedly
        hideScrollBars();
        
        // Also hide when skin changes and recalculate height
        studentsTable.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                hideScrollBars();
                // Recalculate height when skin is ready
                Platform.runLater(() -> {
                    javafx.scene.Node header = studentsTable.lookup(".column-header-background");
                    double headerHeight = 32.0; // Default fallback
                    
                    if (header != null) {
                        headerHeight = header.getBoundsInLocal().getHeight();
                    }
                    
                    // Calculate exact height: header + (10 rows * 28px)
                    double exactHeight = headerHeight + (PAGE_SIZE * studentsTable.getFixedCellSize());
                    
                    studentsTable.setPrefHeight(exactHeight);
                    studentsTable.setMinHeight(exactHeight);
                    studentsTable.setMaxHeight(exactHeight);
                });
            }
        });
        
        // Hide scrollbars when items change
        studentList.addListener((javafx.collections.ListChangeListener.Change<? extends Student> c) -> {
            hideScrollBars();
        });
    }
    
    private void hideScrollBars() {
        javafx.application.Platform.runLater(() -> {
            // Find virtual flow
            javafx.scene.Node vf = studentsTable.lookup(".virtual-flow");
            if (vf != null) {
                // Hide vertical scrollbar
                javafx.scene.Node vScrollBar = vf.lookup(".scroll-bar:vertical");
                if (vScrollBar != null) {
                    vScrollBar.setVisible(false);
                    vScrollBar.setManaged(false);
                    vScrollBar.setStyle("-fx-opacity: 0; visibility: hidden; -fx-pref-width: 0;");
                }
                // Hide horizontal scrollbar
                javafx.scene.Node hScrollBar = vf.lookup(".scroll-bar:horizontal");
                if (hScrollBar != null) {
                    hScrollBar.setVisible(false);
                    hScrollBar.setManaged(false);
                    hScrollBar.setStyle("-fx-opacity: 0; visibility: hidden; -fx-pref-height: 0;");
                }
                // Also try to find scrollbars directly in the table
                java.util.Set<javafx.scene.Node> scrollBars = vf.lookupAll(".scroll-bar");
                for (javafx.scene.Node scrollBar : scrollBars) {
                    scrollBar.setVisible(false);
                    scrollBar.setManaged(false);
                    scrollBar.setStyle("-fx-opacity: 0; visibility: hidden;");
                }
            }
            // Try direct lookup on table as well
            java.util.Set<javafx.scene.Node> allScrollBars = studentsTable.lookupAll(".scroll-bar");
            for (javafx.scene.Node scrollBar : allScrollBars) {
                scrollBar.setVisible(false);
                scrollBar.setManaged(false);
                scrollBar.setStyle("-fx-opacity: 0; visibility: hidden;");
            }
        });
        
        studentsTable.setItems(studentList);
    }
    
    private void loadStudents() {
        // Refresh school year cache in case new school years were added
        loadSchoolYearCache();
        
        // Sync belong records for existing students (one-time sync)
        // This ensures students added before the belong record fix are properly linked
        studentDAO.syncBelongRecordsForExistingStudents();
        
        // Filter by selected school year, status, and semester
        Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        String status = statusFilterCombo.getValue();
        if (status != null && status.equals("All")) {
            status = null; // Show all statuses
        } else if (status != null && !status.isEmpty()) {
            status = status.toLowerCase();
        } else {
            status = null; // Default to showing all students (not just active)
        }
        
        // ALWAYS check header dropdown first - this is the primary filter
        // Priority: Header dropdown > Filter dropdown
        final String semester;
        String headerValue = null;
        if (semesterComboBoxHeader != null) {
            headerValue = semesterComboBoxHeader.getValue();
        }
        
        // If header dropdown has a specific semester value (1st Sem, 2nd Sem, Summer Sem), use it
        if (headerValue != null && !headerValue.isEmpty() && 
            !headerValue.equals("Select Semester")) {
            semester = headerValue.trim();
        } else {
            // Header dropdown is empty, no filter dropdown available
            semester = null; // Show all semesters
        }
        
        // Get students with semester filter applied at SQL level for better performance and accuracy
        // The SQL query now handles all filtering, so no Java-side filtering is needed
        List<Student> students;
        if (schoolYearId != null) {
            students = studentDAO.getAllStudents(schoolYearId, status, semester);
        } else {
            students = studentDAO.getAllStudents(null, status, semester);
        }
        
        // Store all students for pagination
        allStudentsList.clear();
        allStudentsList.addAll(students);
        totalStudents = allStudentsList.size();
        
        // Update total students label
        totalStudentsLabel.setText("(Total: " + totalStudents + ")");
        
        // Apply pagination
        updatePagination();
    }
    
    private void loadSchoolYears() {
        // Load only active school years for the form dropdown
        List<SchoolYear> schoolYears = schoolYearDAO.getActiveSchoolYears();
        schoolYearCombo.getItems().clear();
        
        for (SchoolYear sy : schoolYears) {
            schoolYearCombo.getItems().add(sy.getYearRange());
        }
        
        // Setup semester dropdown with fixed options
        semesterCombo.getItems().clear();
        semesterCombo.getItems().addAll("1st Sem", "2nd Sem", "Summer Sem");
        // Auto-select semester based on current month (if not already set)
        if (semesterCombo.getValue() == null || semesterCombo.getValue().isEmpty()) {
            String autoSemester = getSemesterByMonth(java.time.LocalDate.now().getMonth());
            semesterCombo.setValue(autoSemester);
        }
        
        // Setup year dropdown with fixed options
        yearField.getItems().clear();
        yearField.getItems().addAll("1st Year", "2nd Year", "3rd Year", "4th Year");
        
        // Setup program dropdown with fixed options
        majorField.getItems().clear();
        majorField.getItems().addAll(
            "BSBA - Bachelor of Science in Business Administration",
            "BSIT - Bachelor of Science in Information Technology",
            "BSA - Bachelor of Science in Agriculture",
            "BTLED - Bachelor of Technology and Livelihood Education"
        );
    }
    
    /**
     * Load all school years into cache to avoid N+1 query problem
     * This is called once and cached, then used by the table cell value factory
     */
    private void loadSchoolYearCache() {
        schoolYearCache.clear();
        // Load ALL school years (not just active) to handle any student's school year
        List<SchoolYear> schoolYears = schoolYearDAO.getAllSchoolYears();
        for (SchoolYear sy : schoolYears) {
            schoolYearCache.put(sy.getSchoolYearId(), sy.getYearRange());
        }
    }
    
    private void setupHeaderDropdowns() {
        // Setup header year dropdown
        if (yearComboBoxHeader != null) {
            setupSchoolYearDropdown(yearComboBoxHeader);
            yearComboBoxHeader.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    onSchoolYearChanged();
                }
            });
        }
        
        // Setup header semester dropdown
        if (semesterComboBoxHeader != null) {
            semesterComboBoxHeader.getItems().addAll("1st Sem", "2nd Sem", "Summer Sem");
            // Auto-select will be done in autoSelectSemester() method
            semesterComboBoxHeader.setOnAction(e -> {
                loadStudents();
            });
        }
    }
    
    /**
     * Automatically select semester based on current month
     * 1st Sem: August, September, October, November, December
     * 2nd Sem: January, February, March, April, May
     * Summer: June, July
     */
    private void autoSelectSemester() {
        String selectedSemester = utils.SemesterUtil.getSemesterByCurrentMonth();
        
        // Set for header dropdown
        if (semesterComboBoxHeader != null) {
            semesterComboBoxHeader.setValue(selectedSemester);
        }
        
        // Set for form dropdown
        if (semesterCombo != null && semesterCombo.getItems().isEmpty()) {
            semesterCombo.getItems().addAll("1st Sem", "2nd Sem", "Summer Sem");
        }
        if (semesterCombo != null) {
            semesterCombo.setValue(selectedSemester);
        }
    }
    
    /**
     * Get semester based on month (delegates to utility class)
     */
    private String getSemesterByMonth(java.time.Month month) {
        return utils.SemesterUtil.getSemesterByMonth(month);
    }
    
    private String getSelectedSemester() {
        if (semesterComboBoxHeader != null) {
            String selected = semesterComboBoxHeader.getValue();
            if (selected == null || selected.isEmpty()) {
                return "1st Sem"; // Default to 1st Sem
            }
            return selected;
        }
        return "1st Sem"; // Default to 1st Sem
    }
    
    @Override
    protected void onSchoolYearChanged() {
        // Reload students when school year changes
        currentPage = 1; // Reset to first page
        loadStudents();
    }
    
    private void loadFilters() {
        // Load majors and add "All" option at the beginning
        List<String> majors = studentDAO.getAllMajors();
        majorFilterCombo.getItems().clear();
        majorFilterCombo.getItems().add("All"); // Add "All" option first
        majorFilterCombo.getItems().addAll(majors);
        
        // Load years and add "All" option at the beginning, then add "1st Year" if not present
        List<String> years = studentDAO.getAllYears();
        yearFilterCombo.getItems().clear();
        yearFilterCombo.getItems().add("All"); // Add "All" option first
        yearFilterCombo.getItems().addAll(years);
        
        // Add "1st Year" if it doesn't exist in the list
        if (!yearFilterCombo.getItems().contains("1st Year")) {
            yearFilterCombo.getItems().add("1st Year");
        }
        
        // Sort years to have "All" first, then "1st Year", then others
        yearFilterCombo.getItems().sort((a, b) -> {
            if (a.equals("All")) return -1;
            if (b.equals("All")) return 1;
            if (a.equals("1st Year")) return -1;
            if (b.equals("1st Year")) return 1;
            return a.compareTo(b);
        });
        
        // Setup status filter dropdown
        statusFilterCombo.getItems().clear();
        statusFilterCombo.getItems().addAll("All", "Active", "Deactivated");
        statusFilterCombo.setValue("Active"); // Default to Active
        statusFilterCombo.setOnAction(e -> handleSearch());
        
    }
    
    private void setupEventHandlers() {
        yearFilterCombo.setOnAction(e -> handleSearch());
        majorFilterCombo.setOnAction(e -> handleSearch());
    }
    
    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText();
        String year = yearFilterCombo.getValue();
        String major = majorFilterCombo.getValue();
        String status = statusFilterCombo.getValue();
        
        // Get the selected school year from SessionManager
        Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        
        // If "All" is selected for major, pass null to show all majors
        if (major != null && major.equals("All")) {
            major = null;
        }
        
        // If "All" is selected for year, pass null to show all years
        if (year != null && year.equals("All")) {
            year = null;
        }
        
        // If "All" is selected for status, pass null to show all statuses
        if (status != null && status.equals("All")) {
            status = null;
        } else if (status == null || status.isEmpty()) {
            status = "active"; // Default to active
        } else {
            status = status.toLowerCase();
        }
        
        // ALWAYS check header dropdown first for semester - same logic as loadStudents()
        final String semester;
        String headerValue = null;
        if (semesterComboBoxHeader != null) {
            headerValue = semesterComboBoxHeader.getValue();
        }
        
        // If header dropdown has a specific semester value (1st Sem, 2nd Sem, Summer Sem), use it
        if (headerValue != null && !headerValue.isEmpty() && 
            !headerValue.equals("Select Semester")) {
            semester = headerValue.trim();
        } else {
            // Header dropdown is empty, no filter dropdown available
            semester = null; // Show all semesters
        }
        
        // Search with school year, status, and semester filter
        List<Student> students = studentDAO.searchStudents(searchTerm, year, major, schoolYearId, status, semester);
        
        // Store all students for pagination
        allStudentsList.clear();
        allStudentsList.addAll(students);
        totalStudents = allStudentsList.size();
        
        // Reset to first page when searching
        currentPage = 1;
        
        // Update total students label
        totalStudentsLabel.setText("(Total: " + totalStudents + ")");
        
        // Apply pagination
        updatePagination();
    }
    
    @FXML
    private void handleTableClick() {
        selectedStudent = studentsTable.getSelectionModel().getSelectedItem();
        if (selectedStudent != null) {
            populateForm(selectedStudent);
            // Enable Update/Delete buttons, disable Add button
            addBtn.setDisable(true);
            updateBtn.setDisable(false);
            deleteBtn.setDisable(false);
            reactivateBtn.setDisable(false);
            
            // Show/hide buttons based on status
            // If deactivated: Deactivate button = FALSE (disabled), Reactivate button = TRUE (enabled)
            // If active: Deactivate button = TRUE (enabled), Reactivate button = FALSE (disabled)
            if ("deactivated".equalsIgnoreCase(selectedStudent.getStatus())) {
                deleteBtn.setDisable(true);      // Deactivate button disabled when deactivated
                reactivateBtn.setDisable(false); // Reactivate button enabled when deactivated
            } else {
                deleteBtn.setDisable(false);     // Deactivate button enabled when active
                reactivateBtn.setDisable(true);  // Reactivate button disabled when active
            }
        }
    }
    
    private void populateForm(Student student) {
        studentIdField.setText(student.getStudentNumber());
        firstNameField.setText(student.getFirstName() != null ? student.getFirstName() : "");
        middleNameField.setText(student.getMiddleName() != null ? student.getMiddleName() : "");
        lastNameField.setText(student.getLastName() != null ? student.getLastName() : "");
        // Set program ComboBox value - try to match the full program name or just the code
        if (student.getMajor() != null && !student.getMajor().isEmpty()) {
            String majorValue = student.getMajor();
            // Try to find exact match first
            boolean found = false;
            for (String item : majorField.getItems()) {
                if (item.equals(majorValue) || item.startsWith(majorValue.split(" - ")[0])) {
                    majorField.setValue(item);
                    found = true;
                    break;
                }
            }
            if (!found) {
                majorField.setValue(null);
            }
        } else {
            majorField.setValue(null);
        }
        yearField.setValue(student.getYear() != null ? student.getYear() : null);
        
        if (student.getSchoolYearId() != null) {
            SchoolYear sy = schoolYearDAO.getSchoolYearById(student.getSchoolYearId());
            if (sy != null) {
                schoolYearCombo.setValue(sy.getYearRange());
            }
        }
        
        // Set semester from the student's semester field
        if (student.getSemester() != null && !student.getSemester().isEmpty()) {
            semesterCombo.setValue(student.getSemester());
        } else {
            // Auto-select semester based on current month when clearing form
        String autoSemester = getSemesterByMonth(java.time.LocalDate.now().getMonth());
        semesterCombo.setValue(autoSemester);
        }
    }
    
    @FXML
    private void handleAdd() {
        if (validateForm()) {
            Student student = createStudentFromForm();
            student.setStatus("active"); // New students are always active
            
            // First check: Check if student with same name already exists in the same semester and school year
            if (student.getSchoolYearId() != null && student.getSemester() != null && !student.getSemester().trim().isEmpty()) {
                if (studentDAO.fullNameExistsForSchoolYearAndSemester(
                        student.getFirstName(),
                        student.getMiddleName(),
                        student.getLastName(),
                        student.getSchoolYearId(),
                        student.getSemester())) {
                    String yearRange = "this school year";
                    if (student.getSchoolYearId() != null) {
                        SchoolYear sy = schoolYearDAO.getSchoolYearById(student.getSchoolYearId());
                        if (sy != null) {
                            yearRange = sy.getYearRange();
                        }
                    }
                    
                    showLargeAlert(Alert.AlertType.ERROR, "Error", "Student Already Added",
                        "A student with the same first name, middle name, and last name already exists in " + 
                        yearRange + " for " + (student.getSemester() != null ? student.getSemester() : "this semester") + ".\n\n" +
                        "Name: " + student.getFirstName() + " " + 
                        (student.getMiddleName() != null && !student.getMiddleName().trim().isEmpty() ? student.getMiddleName() + " " : "") +
                        student.getLastName() + "\n\n" +
                        "You cannot add the same student to the same semester and school year.\n" +
                        "If you need to update the student's information, please use the Update function instead.");
                    return;
                }
            }
            
            // Second check: Check if student ID already exists in the same semester and school year
            if (student.getSchoolYearId() != null && student.getSemester() != null && !student.getSemester().trim().isEmpty()) {
                if (studentDAO.studentIdExistsInSemesterAndSchoolYear(
                        student.getStudentNumber(),
                        student.getSchoolYearId(),
                        student.getSemester())) {
                    // Get the existing student to show their information
                    Student existingStudent = studentDAO.getStudentByStudentIdInSemesterAndSchoolYear(
                            student.getStudentNumber(),
                            student.getSchoolYearId(),
                            student.getSemester());
                    
                    String yearRange = "this school year";
                    if (student.getSchoolYearId() != null) {
                        SchoolYear sy = schoolYearDAO.getSchoolYearById(student.getSchoolYearId());
                        if (sy != null) {
                            yearRange = sy.getYearRange();
                        }
                    }
                    
                    String existingInfo = "";
                    if (existingStudent != null) {
                        existingInfo = "\n\nExisting Student Information:\n" +
                                     "Student ID: " + existingStudent.getStudentNumber() + "\n" +
                                     "Name: " + existingStudent.getFullname() + "\n" +
                                     "Program: " + (existingStudent.getMajor() != null ? existingStudent.getMajor() : "N/A") + "\n" +
                                     "Year: " + (existingStudent.getYear() != null ? existingStudent.getYear() : "N/A") + "\n" +
                                     "Semester: " + (student.getSemester() != null ? student.getSemester() : "N/A") + "\n" +
                                     "School Year: " + yearRange;
                    }
                    
                    showLargeAlert(Alert.AlertType.ERROR, "Error", "Student ID Already Exists",
                        "A student with this Student ID already exists in " + yearRange + " for " + 
                        (student.getSemester() != null ? student.getSemester() : "this semester") + "." +
                        existingInfo + "\n\n" +
                        "You cannot add the same student ID to the same semester and school year.\n" +
                        "If you need to update the student's information, please use the Update function instead.");
                    return;
                }
            }
            
            // Check if student exists but is deactivated
            if (studentDAO.studentExistsDeactivated(
                    student.getStudentNumber(),
                    student.getFirstName(),
                    student.getMiddleName(),
                    student.getLastName(),
                    student.getYear(),
                    student.getMajor(),
                    student.getSchoolYearId(),
                    student.getSemester())) {
                String yearRange = "this school year";
                if (student.getSchoolYearId() != null) {
                    SchoolYear sy = schoolYearDAO.getSchoolYearById(student.getSchoolYearId());
                    if (sy != null) {
                        yearRange = sy.getYearRange();
                    }
                }
                showLargeAlert(Alert.AlertType.WARNING, "Student Already Added (Deactivated)", 
                    "This student enrollment is currently deactivated",
                    "A student with the same information already exists but is currently DEACTIVATED.\n\n" +
                    "Student ID: " + student.getStudentNumber() + "\n" +
                    "Name: " + student.getFullname() + "\n" +
                    "Year: " + (student.getYear() != null ? student.getYear() : "N/A") + "\n" +
                    "Program: " + (student.getMajor() != null ? student.getMajor() : "N/A") + "\n" +
                    "School Year: " + yearRange + "\n" +
                    "Semester: " + (student.getSemester() != null ? student.getSemester() : "N/A") + "\n\n" +
                    "To add this student again, please REACTIVATE the existing record first by:\n" +
                    "1. Finding the student in the table\n" +
                    "2. Clicking the 'Reactivate' button\n\n" +
                    "Or select a different semester to create a new enrollment.");
                return;
            }
            
            if (studentDAO.addStudent(student)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Student added successfully!");
                clearForm();
                
                // Update header dropdowns to match the school year and semester used when adding
                // This ensures the newly added student will be visible in the table
                if (student.getSchoolYearId() != null) {
                    SchoolYear sy = schoolYearDAO.getSchoolYearById(student.getSchoolYearId());
                    if (sy != null && yearComboBoxHeader != null) {
                        yearComboBoxHeader.setValue(sy.getYearRange());
                        // Update SessionManager to match
                        utils.SessionManager.setSelectedSchoolYearId(student.getSchoolYearId());
                    }
                }
                
                // Update semester header dropdown to match the form value
                if (student.getSemester() != null && !student.getSemester().trim().isEmpty() 
                    && semesterComboBoxHeader != null) {
                    semesterComboBoxHeader.setValue(student.getSemester());
                }
                
                // Reset to first page and reload students with updated filters
                currentPage = 1;
                loadStudents();
                loadFilters();
                setupButtonStates(); // Reset button states
            } else {
                // Clearer duplicate checks when add fails
                if (student.getSchoolYearId() != null && student.getSemester() != null && !student.getSemester().trim().isEmpty()) {
                    if (studentDAO.studentIdExistsInSemesterAndSchoolYear(
                            student.getStudentNumber(),
                            student.getSchoolYearId(),
                            student.getSemester())) {
                        showLargeAlert(Alert.AlertType.ERROR, "Error", "Student ID Already Used",
                            "This Student ID is already used for the selected School Year and Semester.\n\n" +
                            "Please choose a different Student ID or update the existing enrollment.");
                        return;
                    }
                }

                // Check if it failed due to duplicate student ID (anywhere)
                if (studentDAO.studentIdExists(student.getStudentNumber())) {
                    Student existingStudent = studentDAO.getStudentByStudentId(student.getStudentNumber());
                    String existingInfo = "";
                    if (existingStudent != null) {
                        existingInfo = "\n\nExisting Student Information:\n" +
                                     "Student ID: " + existingStudent.getStudentNumber() + "\n" +
                                     "Name: " + existingStudent.getFullname() + "\n" +
                                     "Program: " + (existingStudent.getMajor() != null ? existingStudent.getMajor() : "N/A") + "\n" +
                                     "Year: " + (existingStudent.getYear() != null ? existingStudent.getYear() : "N/A");
                    }
                    showLargeAlert(Alert.AlertType.ERROR, "Error", "Cannot Add Student",
                        "A student with this Student ID already exists in the system." +
                        existingInfo + "\n\n" +
                        "You cannot add a student with an ID that already exists.\n" +
                        "Please select the existing student from the table and use the 'Update' button to modify their information.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to add student! Please check your input and try again.");
                }
            }
        }
    }
    
    @FXML
    private void handleUpdate() {
        if (selectedStudent == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a student to update!");
            return;
        }
        
        if (validateForm()) {
            Student student = createStudentFromForm();
            student.setStudentId(selectedStudent.getStudentId());
            student.setStatus(selectedStudent.getStatus() != null ? selectedStudent.getStatus() : "active"); // Preserve status
            
            // No-op guard: if nothing changed, notify and exit
            if (!hasStudentChanges(selectedStudent, student)) {
                showAlert(Alert.AlertType.INFORMATION, "No Changes", "There are no changes to update.");
                return;
            }
            
            // Check if semester or school year is being changed
            boolean semesterChanged = (selectedStudent.getSemester() == null && student.getSemester() != null) ||
                                     (selectedStudent.getSemester() != null && !selectedStudent.getSemester().equals(student.getSemester()));
            boolean schoolYearChanged = (selectedStudent.getSchoolYearId() == null && student.getSchoolYearId() != null) ||
                                       (selectedStudent.getSchoolYearId() != null && !selectedStudent.getSchoolYearId().equals(student.getSchoolYearId()));
            
            // If semester or school year is being changed, check if student already exists in the new combination
            if ((semesterChanged || schoolYearChanged) && 
                student.getSchoolYearId() != null && 
                student.getSemester() != null && 
                !student.getSemester().trim().isEmpty()) {
                
                // Check if student with same name already exists in the new semester/year
                if (studentDAO.fullNameExistsForSchoolYearAndSemester(
                        student.getFirstName(),
                        student.getMiddleName(),
                        student.getLastName(),
                        student.getSchoolYearId(),
                        student.getSemester())) {
                    String yearRange = "this school year";
                    if (student.getSchoolYearId() != null) {
                        SchoolYear sy = schoolYearDAO.getSchoolYearById(student.getSchoolYearId());
                        if (sy != null) {
                            yearRange = sy.getYearRange();
                        }
                    }
                    showLargeAlert(Alert.AlertType.ERROR, "Error", "Student Already Added",
                        "A student with the same first name, middle name, and last name already exists in " + 
                        yearRange + " for " + (student.getSemester() != null ? student.getSemester() : "this semester") + ".\n\n" +
                        "Name: " + student.getFirstName() + " " + 
                        (student.getMiddleName() != null && !student.getMiddleName().trim().isEmpty() ? student.getMiddleName() + " " : "") +
                        student.getLastName() + "\n\n" +
                        "You cannot update to a semester/year combination where the student is already added.\n" +
                        "Please select a different semester or school year.");
                    return;
                }
                
                // Check if student with same ID already exists in the new semester/year
                if (studentDAO.studentIdExistsInSemesterAndSchoolYear(
                        student.getStudentNumber(),
                        student.getSchoolYearId(),
                        student.getSemester())) {
                    String yearRange = "this school year";
                    if (student.getSchoolYearId() != null) {
                        SchoolYear sy = schoolYearDAO.getSchoolYearById(student.getSchoolYearId());
                        if (sy != null) {
                            yearRange = sy.getYearRange();
                        }
                    }
                    showLargeAlert(Alert.AlertType.ERROR, "Error", "Student ID Already Exists",
                        "A student with this Student ID already exists in " + yearRange + " for " + 
                        (student.getSemester() != null ? student.getSemester() : "this semester") + ".\n\n" +
                        "Student ID: " + student.getStudentNumber() + "\n\n" +
                        "You cannot update to a semester/year combination where this Student ID is already used.\n" +
                        "Please select a different semester or school year.");
                    return;
                }
            }

            // Block if the new name is already used by another student
            if (studentDAO.fullNameExistsForAnother(
                    student.getFirstName(),
                    student.getMiddleName(),
                    student.getLastName(),
                    student.getStudentId())) {
                showLargeAlert(Alert.AlertType.ERROR, "Error", "Name Already Exists",
                    "A student with the same first, middle, and last name already exists.\n\n" +
                    "Please use a unique name or update the existing student instead.");
                return;
            }
            
            // Prevent duplicate student ID on another student
            if (studentDAO.studentIdExistsForAnother(student.getStudentNumber(), student.getStudentId())) {
                showLargeAlert(Alert.AlertType.ERROR, "Error", "Student ID Already In Use",
                    "The Student ID '" + student.getStudentNumber() + "' is already used by another student.\n\n" +
                    "Please use a different Student ID or update the existing student instead.");
                return;
            }
            
            // Check if Student ID already exists for another student in the same school year and semester
            if (student.getSchoolYearId() != null && student.getSemester() != null && !student.getSemester().trim().isEmpty()) {
                if (studentDAO.studentIdExistsInSemesterAndSchoolYearForAnother(
                        student.getStudentNumber(),
                        student.getSchoolYearId(),
                        student.getSemester(),
                        student.getStudentId())) {
                    String yearRange = "this school year";
                    if (student.getSchoolYearId() != null) {
                        SchoolYear sy = schoolYearDAO.getSchoolYearById(student.getSchoolYearId());
                        if (sy != null) {
                            yearRange = sy.getYearRange();
                        }
                    }
                    showLargeAlert(Alert.AlertType.ERROR, "Error", "Student ID Already Exists",
                        "A student with this Student ID already exists in " + yearRange + " for " + 
                        (student.getSemester() != null ? student.getSemester() : "this semester") + ".\n\n" +
                        "You cannot update to a Student ID that is already used in the same school year and semester.\n" +
                        "Please use a different Student ID or select a different school year/semester combination.");
                    return;
                }
            }
            
            // Check if updating would create a duplicate (same information, year, program, semester)
            // Only check if the combination is different from the current student's combination
            boolean isDifferentCombination = (selectedStudent.getYear() == null && student.getYear() != null) ||
                                            (selectedStudent.getYear() != null && !selectedStudent.getYear().equals(student.getYear())) ||
                                            (selectedStudent.getMajor() == null && student.getMajor() != null) ||
                                            (selectedStudent.getMajor() != null && !selectedStudent.getMajor().equals(student.getMajor())) ||
                                            (selectedStudent.getSemester() == null && student.getSemester() != null) ||
                                            (selectedStudent.getSemester() != null && !selectedStudent.getSemester().equals(student.getSemester())) ||
                                            (selectedStudent.getSchoolYearId() == null && student.getSchoolYearId() != null) ||
                                            (selectedStudent.getSchoolYearId() != null && !selectedStudent.getSchoolYearId().equals(student.getSchoolYearId()));
            
            if (isDifferentCombination && studentDAO.studentExistsWithYearProgramSemester(
                    student.getStudentNumber(),
                    student.getFirstName(),
                    student.getMiddleName(),
                    student.getLastName(),
                    student.getYear(),
                    student.getMajor(),
                    student.getSchoolYearId(),
                    student.getSemester())) {
                String yearRange = "this school year";
                if (student.getSchoolYearId() != null) {
                    SchoolYear sy = schoolYearDAO.getSchoolYearById(student.getSchoolYearId());
                    if (sy != null) {
                        yearRange = sy.getYearRange();
                    }
                }
                showAlert(Alert.AlertType.ERROR, "Error", 
                    "Student already exists!\n\n" +
                    "A student with the same information, year, program, and semester already exists in " + yearRange + ".\n\n" +
                    "Student ID: " + student.getStudentNumber() + "\n" +
                    "Name: " + student.getFullname() + "\n" +
                    "Year: " + (student.getYear() != null ? student.getYear() : "N/A") + "\n" +
                    "Program: " + (student.getMajor() != null ? student.getMajor() : "N/A") + "\n" +
                    "Semester: " + (student.getSemester() != null ? student.getSemester() : "N/A"));
                return;
            }
            
            if (studentDAO.updateStudent(student)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Student updated successfully!");
                clearForm();
                currentPage = 1; // Reset to first page
                loadStudents();
                loadFilters();
                selectedStudent = null;
                setupButtonStates(); // Reset button states
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update student!");
            }
        }
    }
    
    @FXML
    private void handleDelete() {
        if (selectedStudent == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a student to deactivate!");
            return;
        }
        
        // Get semester and school year from the selected student
        String semester = selectedStudent.getSemester();
        Integer schoolYearId = selectedStudent.getSchoolYearId();
        
        if (schoolYearId == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Cannot deactivate: Student has no school year assigned!");
            return;
        }
        
        if (semester == null || semester.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Cannot deactivate: Student has no semester assigned!");
            return;
        }
        
        String schoolYearRange = "N/A";
        if (schoolYearId != null) {
            SchoolYear sy = schoolYearDAO.getSchoolYearById(schoolYearId);
            if (sy != null) {
                schoolYearRange = sy.getYearRange();
            }
        }
        
        String message = "Are you sure you want to deactivate this specific enrollment?\n\n" +
                        "Student ID: " + selectedStudent.getStudentNumber() + "\n" +
                        "Name: " + selectedStudent.getFullname() + "\n" +
                        "School Year: " + schoolYearRange + "\n" +
                        "Semester: " + semester + "\n\n" +
                        "Only this specific semester enrollment will be deactivated.";
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deactivate");
        confirmAlert.setHeaderText("Deactivate Semester Enrollment");
        confirmAlert.setContentText(message);
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (studentDAO.deactivateBelongRecord(selectedStudent.getStudentId(), schoolYearId, semester)) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", 
                        "Successfully deactivated the semester enrollment!");
                    clearForm();
                    currentPage = 1; // Reset to first page
                    loadStudents();
                    loadFilters();
                    selectedStudent = null;
                    setupButtonStates(); // Reset button states
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to deactivate semester enrollment!");
                }
            }
        });
    }
    
    @FXML
    private void handleReactivate() {
        if (selectedStudent == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a student to reactivate!");
            return;
        }
        
        // Get semester and school year from the selected student
        String semester = selectedStudent.getSemester();
        Integer schoolYearId = selectedStudent.getSchoolYearId();
        
        if (schoolYearId == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Cannot reactivate: Student has no school year assigned!");
            return;
        }
        
        if (semester == null || semester.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Cannot reactivate: Student has no semester assigned!");
            return;
        }
        
        String schoolYearRange = "N/A";
        if (schoolYearId != null) {
            SchoolYear sy = schoolYearDAO.getSchoolYearById(schoolYearId);
            if (sy != null) {
                schoolYearRange = sy.getYearRange();
            }
        }
        
        String message = "Are you sure you want to reactivate this specific enrollment?\n\n" +
                        "Student ID: " + selectedStudent.getStudentNumber() + "\n" +
                        "Name: " + selectedStudent.getFullname() + "\n" +
                        "School Year: " + schoolYearRange + "\n" +
                        "Semester: " + semester + "\n\n" +
                        "Only this specific semester enrollment will be reactivated.";
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Reactivate");
        confirmAlert.setHeaderText("Reactivate Semester Enrollment");
        confirmAlert.setContentText(message);
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (studentDAO.reactivateBelongRecord(selectedStudent.getStudentId(), schoolYearId, semester)) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Semester enrollment reactivated successfully!");
                    clearForm();
                    currentPage = 1; // Reset to first page
                    loadStudents();
                    loadFilters();
                    selectedStudent = null;
                    setupButtonStates(); // Reset button states
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to reactivate semester enrollment!");
                }
            }
        });
    }
  
   
 
    
    /**
     * Import students from Excel file
     * Expected format:
     * Column A: Student ID (required)
     * Column B: First Name (required)
     * Column C: Middle Name (optional)
     * Column D: Last Name (required)
     * Column E: Program (optional)
     * Column F: Year (optional)
     * Column G: School Year (optional, e.g., "2024-2025")
  
     */
    private void importStudentsFromExcel(java.io.File file) {
        try {
            // Check if Apache POI is available
            Class<?> workbookClass = Class.forName("org.apache.poi.ss.usermodel.Workbook");
            Class<?> workbookFactoryClass = Class.forName("org.apache.poi.ss.usermodel.WorkbookFactory");
            
            // Use reflection to avoid compile-time dependency
            java.lang.reflect.Method createMethod = workbookFactoryClass.getMethod("create", java.io.InputStream.class);
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            Object workbook = createMethod.invoke(null, fis);
            
            // Get first sheet
            java.lang.reflect.Method getSheetAtMethod = workbookClass.getMethod("getSheetAt", int.class);
            Object sheet = getSheetAtMethod.invoke(workbook, 0);
            
            // Get row iterator
            java.lang.reflect.Method rowIteratorMethod = sheet.getClass().getMethod("iterator");
            java.util.Iterator<?> rowIterator = (java.util.Iterator<?>) rowIteratorMethod.invoke(sheet);
            
            int rowNum = 0;
            int successCount = 0;
            int skipCount = 0;
            int errorCount = 0;
            java.util.List<String> errors = new java.util.ArrayList<>();
            
            // Get current school year and semester from form/header
            Integer defaultSchoolYearId = utils.SessionManager.getSelectedSchoolYearId();
            String defaultSemester = getSelectedSemester();
            
            // Get semester based on current month if not set
            if (defaultSemester == null || defaultSemester.isEmpty()) {
                defaultSemester = getSemesterByMonth(java.time.LocalDate.now().getMonth());
            }
            
            while (rowIterator.hasNext()) {
                rowNum++;
                Object row = rowIterator.next();
                
                // Skip header row (first row)
                if (rowNum == 1) {
                    continue;
                }
                
                try {
                    // Get cell values using reflection
                    java.lang.reflect.Method getCellMethod = row.getClass().getMethod("getCell", int.class);
                    
                    // Helper to get cell value as string
                    // Prefer using POI's DataFormatter via reflection (handles formulas, numbers, dates)
                    java.util.function.Function<Integer, String> getCellValue = (colIndex) -> {
                        try {
                            Object cell = getCellMethod.invoke(row, colIndex);
                            if (cell == null) return "";

                            try {
                                Class<?> dataFormatterClass = Class.forName("org.apache.poi.ss.usermodel.DataFormatter");
                                Object formatter = dataFormatterClass.getDeclaredConstructor().newInstance();
                                java.lang.reflect.Method formatCellValueMethod = dataFormatterClass.getMethod("formatCellValue", Class.forName("org.apache.poi.ss.usermodel.Cell"));
                                String formatted = (String) formatCellValueMethod.invoke(formatter, cell);
                                return formatted != null ? formatted : "";
                            } catch (ClassNotFoundException cnfe) {
                                // DataFormatter not available; fallback to manual handling
                            }

                            // Fallback manual handling: try string then numeric then boolean
                            try {
                                java.lang.reflect.Method getStringCellValueMethod = cell.getClass().getMethod("getStringCellValue");
                                Object val = getStringCellValueMethod.invoke(cell);
                                return val != null ? val.toString() : "";
                            } catch (Exception ex) {
                                // ignore
                            }

                            try {
                                java.lang.reflect.Method getNumericCellValueMethod = cell.getClass().getMethod("getNumericCellValue");
                                double numValue = (Double) getNumericCellValueMethod.invoke(cell);
                                if (numValue == Math.floor(numValue)) {
                                    return String.valueOf((int) numValue);
                                } else {
                                    return String.valueOf(numValue);
                                }
                            } catch (Exception ex) {
                                // ignore
                            }

                            try {
                                java.lang.reflect.Method getBooleanCellValueMethod = cell.getClass().getMethod("getBooleanCellValue");
                                Object b = getBooleanCellValueMethod.invoke(cell);
                                return b != null ? b.toString() : "";
                            } catch (Exception ex) {
                                // ignore
                            }

                        } catch (Exception e) {
                            return "";
                        }
                        return "";
                    };
                    
                    // Read columns
                    String studentId = getCellValue.apply(0).trim();
                    String firstName = getCellValue.apply(1).trim();
                    String middleName = getCellValue.apply(2).trim();
                    String lastName = getCellValue.apply(3).trim();
                    String program = getCellValue.apply(4).trim();
                    String year = getCellValue.apply(5).trim();
                    String schoolYearStr = getCellValue.apply(6).trim();
                    String semester = getCellValue.apply(7).trim();

                    // If year is numeric like "1", "2", convert to normalized form used by the app
                    if (year.matches("^\\d+$")) {
                        switch (year) {
                            case "1": year = "1st Year"; break;
                            case "2": year = "2nd Year"; break;
                            case "3": year = "3rd Year"; break;
                            case "4": year = "4th Year"; break;
                            default: year = year + "th Year"; break;
                        }
                    }
                    
                    // Validate required fields
                    if (studentId.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                        skipCount++;
                        errors.add("Row " + rowNum + ": Missing required fields (Student ID, First Name, or Last Name)");
                        continue;
                    }
                    
                    // Check if student already exists
                    if (studentDAO.studentIdExists(studentId)) {
                        skipCount++;
                        errors.add("Row " + rowNum + ": Student ID " + studentId + " already exists");
                        continue;
                    }
                    
                    // Create student object
                    Student student = new Student();
                    student.setStudentNumber(studentId);
                    student.setFirstName(firstName);
                    student.setMiddleName(middleName.isEmpty() ? null : middleName);
                    student.setLastName(lastName);
                    student.setMajor(program.isEmpty() ? null : program);
                    student.setYear(year.isEmpty() ? null : year);
                    student.setSemester(semester.isEmpty() ? defaultSemester : semester);
                    student.setStatus("active");
                    
                    // Set school year
                    if (!schoolYearStr.isEmpty()) {
                        SchoolYear sy = schoolYearDAO.getSchoolYearByRange(schoolYearStr);
                        if (sy != null) {
                            student.setSchoolYearId(sy.getSchoolYearId());
                        } else {
                            student.setSchoolYearId(defaultSchoolYearId);
                        }
                    } else {
                        student.setSchoolYearId(defaultSchoolYearId);
                    }
                    
                    // Add student
                    if (studentDAO.addStudent(student)) {
                        successCount++;
                    } else {
                        errorCount++;
                        errors.add("Row " + rowNum + ": Failed to add student " + studentId);
                    }
                    
                } catch (Exception e) {
                    errorCount++;
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                }
            }
            
            // Close resources
            java.lang.reflect.Method closeMethod = workbookClass.getMethod("close");
            closeMethod.invoke(workbook);
            fis.close();
            
            // Show results
            StringBuilder message = new StringBuilder();
            message.append("Import completed!\n\n");
            message.append("Successfully imported: ").append(successCount).append(" student(s)\n");
            if (skipCount > 0) {
                message.append("Skipped: ").append(skipCount).append(" student(s)\n");
            }
            if (errorCount > 0) {
                message.append("Errors: ").append(errorCount).append(" student(s)\n");
            }
            
            if (!errors.isEmpty() && errors.size() <= 10) {
                message.append("\nDetails:\n");
                for (String error : errors) {
                    message.append("• ").append(error).append("\n");
                }
            } else if (errors.size() > 10) {
                message.append("\n(First 10 errors shown, check console for all errors)");
            }
            
            showAlert(Alert.AlertType.INFORMATION, "Import Results", message.toString());
            
            // Refresh the student list
            loadStudents();
            
        } catch (ClassNotFoundException e) {
            // Apache POI not found - attempt lightweight .xlsx parsing fallback (supports simple sharedStrings + sheet1.xml)
            try {
                importFromXlsxFallback(file);
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Apache POI Not Found", 
                    "Excel import requires Apache POI library or a simple .xlsx file.\n\n" +
                    "Please add the following JAR files to your project if you need full Excel support:\n" +
                    "- poi-X.X.X.jar\n" +
                    "- poi-ooxml-X.X.X.jar\n" +
                    "- poi-scratchpad-X.X.X.jar\n\n" +
                    "See EXCEL_IMPORT_SETUP.md for instructions.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Import Error", 
                "Failed to import Excel file:\n" + e.getMessage());
        }
    }
    
    /**
     * Fallback XLSX parser for simple .xlsx files when Apache POI is not available.
     * Supports sharedStrings and basic numeric/string cells on sheet1.xml.
     */
    private void importFromXlsxFallback(java.io.File file) throws Exception {
        java.util.zip.ZipFile zip = new java.util.zip.ZipFile(file);
        java.util.List<String> sharedStrings = new java.util.ArrayList<>();
        java.util.zip.ZipEntry sstEntry = zip.getEntry("xl/sharedStrings.xml");
        if (sstEntry != null) {
            try (java.io.InputStream is = zip.getInputStream(sstEntry)) {
                javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(false);
                org.w3c.dom.Document doc = dbf.newDocumentBuilder().parse(is);
                org.w3c.dom.NodeList siList = doc.getElementsByTagName("si");
                for (int i = 0; i < siList.getLength(); i++) {
                    org.w3c.dom.Node si = siList.item(i);
                    StringBuilder sb = new StringBuilder();
                    org.w3c.dom.NodeList texts = ((org.w3c.dom.Element) si).getElementsByTagName("t");
                    for (int j = 0; j < texts.getLength(); j++) {
                        org.w3c.dom.Node t = texts.item(j);
                        sb.append(t.getTextContent());
                    }
                    sharedStrings.add(sb.toString());
                }
            }
        }

        java.util.zip.ZipEntry sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml");
        if (sheetEntry == null) {
            zip.close();
            throw new IllegalArgumentException("sheet1.xml not found in .xlsx");
        }

        int rowNum = 0;
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();

        Integer defaultSchoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        String defaultSemester = getSelectedSemester();
        if (defaultSemester == null || defaultSemester.isEmpty()) {
            defaultSemester = getSemesterByMonth(java.time.LocalDate.now().getMonth());
        }

        try (java.io.InputStream is = zip.getInputStream(sheetEntry)) {
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            org.w3c.dom.Document doc = dbf.newDocumentBuilder().parse(is);
            org.w3c.dom.NodeList rowList = doc.getElementsByTagName("row");
            for (int r = 0; r < rowList.getLength(); r++) {
                org.w3c.dom.Element rowElem = (org.w3c.dom.Element) rowList.item(r);
                rowNum++;
                if (rowNum == 1) continue; // skip header

                try {
                    java.util.Map<Integer, String> cellMap = new java.util.HashMap<>();
                    org.w3c.dom.NodeList cList = rowElem.getElementsByTagName("c");
                    for (int c = 0; c < cList.getLength(); c++) {
                        org.w3c.dom.Element cElem = (org.w3c.dom.Element) cList.item(c);
                        String rAttr = cElem.getAttribute("r");
                        // column letters part
                        StringBuilder letters = new StringBuilder();
                        for (int i = 0; i < rAttr.length(); i++) {
                            char ch = rAttr.charAt(i);
                            if (Character.isLetter(ch)) letters.append(ch);
                            else break;
                        }
                        int colIndex = 0;
                        String colLetters = letters.toString();
                        for (int i = 0; i < colLetters.length(); i++) {
                            colIndex = colIndex * 26 + (colLetters.charAt(i) - 'A' + 1);
                        }
                        colIndex = colIndex - 1; // zero-based

                        String tAttr = cElem.getAttribute("t");
                        String value = "";
                        org.w3c.dom.NodeList vList = cElem.getElementsByTagName("v");
                        if (vList.getLength() > 0) {
                            String vText = vList.item(0).getTextContent();
                            if ("s".equals(tAttr)) {
                                // shared string
                                try {
                                    int sstIndex = Integer.parseInt(vText);
                                    if (sstIndex >= 0 && sstIndex < sharedStrings.size()) {
                                        value = sharedStrings.get(sstIndex);
                                    }
                                } catch (Exception ex) {
                                    value = "";
                                }
                            } else {
                                value = vText != null ? vText : "";
                            }
                        } else {
                            // inline string
                            org.w3c.dom.NodeList isList = cElem.getElementsByTagName("is");
                            if (isList.getLength() > 0) {
                                org.w3c.dom.Element isElem = (org.w3c.dom.Element) isList.item(0);
                                org.w3c.dom.NodeList tList = isElem.getElementsByTagName("t");
                                if (tList.getLength() > 0) {
                                    value = tList.item(0).getTextContent();
                                }
                            }
                        }

                        cellMap.put(colIndex, value != null ? value : "");
                    }

                    String studentId = cellMap.getOrDefault(0, "").trim();
                    String firstName = cellMap.getOrDefault(1, "").trim();
                    String middleName = cellMap.getOrDefault(2, "").trim();
                    String lastName = cellMap.getOrDefault(3, "").trim();
                    String program = cellMap.getOrDefault(4, "").trim();
                    String year = cellMap.getOrDefault(5, "").trim();
                    String schoolYearStr = cellMap.getOrDefault(6, "").trim();
                    String semester = cellMap.getOrDefault(7, "").trim();

                    if (year.matches("^\\d+$")) {
                        switch (year) {
                            case "1": year = "1st Year"; break;
                            case "2": year = "2nd Year"; break;
                            case "3": year = "3rd Year"; break;
                            case "4": year = "4th Year"; break;
                            default: year = year + "th Year"; break;
                        }
                    }

                    if (studentId.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                        skipCount++;
                        errors.add("Row " + rowNum + ": Missing required fields (Student ID, First Name, or Last Name)");
                        continue;
                    }

                    if (studentDAO.studentIdExists(studentId)) {
                        skipCount++;
                        errors.add("Row " + rowNum + ": Student ID " + studentId + " already exists");
                        continue;
                    }

                    models.Student student = new models.Student();
                    student.setStudentNumber(studentId);
                    student.setFirstName(firstName);
                    student.setMiddleName(middleName.isEmpty() ? null : middleName);
                    student.setLastName(lastName);
                    student.setMajor(program.isEmpty() ? null : program);
                    student.setYear(year.isEmpty() ? null : year);
                    student.setSemester(semester.isEmpty() ? defaultSemester : semester);
                    student.setStatus("active");

                    if (!schoolYearStr.isEmpty()) {
                        models.SchoolYear sy = schoolYearDAO.getSchoolYearByRange(schoolYearStr);
                        if (sy != null) {
                            student.setSchoolYearId(sy.getSchoolYearId());
                        } else {
                            student.setSchoolYearId(defaultSchoolYearId);
                        }
                    } else {
                        student.setSchoolYearId(defaultSchoolYearId);
                    }

                    if (studentDAO.addStudent(student)) {
                        successCount++;
                    } else {
                        errorCount++;
                        errors.add("Row " + rowNum + ": Failed to add student " + studentId);
                    }

                } catch (Exception ex) {
                    errorCount++;
                    errors.add("Row " + rowNum + ": " + ex.getMessage());
                }
            }
        }

        zip.close();

        StringBuilder message = new StringBuilder();
        message.append("Import completed!\n\n");
        message.append("Successfully imported: ").append(successCount).append(" student(s)\n");
        if (skipCount > 0) message.append("Skipped: ").append(skipCount).append(" student(s)\n");
        if (errorCount > 0) message.append("Errors: ").append(errorCount).append(" student(s)\n");
        if (!errors.isEmpty() && errors.size() <= 10) {
            message.append("\nDetails:\n");
            for (String err : errors) {
                message.append("• ").append(err).append("\n");
            }
        } else if (errors.size() > 10) {
            message.append("\n(First 10 errors shown, check console for all errors)");
        }

        showAlert(Alert.AlertType.INFORMATION, "Import Results", message.toString());
        loadStudents();
    }

    @FXML
    private void handleClear() {
        clearForm();
        selectedStudent = null;
        studentsTable.getSelectionModel().clearSelection();
        setupButtonStates(); // Reset button states
    }
    
    private void setupPagination() {
        // Initialize pagination controls
        // Note: Buttons will be updated after loadStudents() is called
        // This is just to ensure they exist
        if (prevBtn != null && nextBtn != null) {
            // Initial state - will be updated by updatePagination()
            updatePaginationButtons();
        }
    }
    
    private void updatePagination() {
        // Calculate pagination
        int totalPages = (int) Math.ceil((double) totalStudents / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        
        // Ensure currentPage is within valid range
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        if (currentPage < 1) {
            currentPage = 1;
        }
        
        // Calculate start and end indices
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalStudents);
        
        // Get students for current page
        studentList.clear();
        for (int i = startIndex; i < endIndex; i++) {
            if (i < allStudentsList.size()) {
                studentList.add(allStudentsList.get(i));
            }
        }
        
        // Update page info label
        if (pageInfoLabel != null) {
            int totalPages2 = (int) Math.ceil((double) totalStudents / PAGE_SIZE);
            if (totalPages2 == 0) totalPages2 = 1;
            pageInfoLabel.setText("Page " + currentPage + " of " + totalPages2);
        }
        
        // Update button states
        updatePaginationButtons();
        
        // Hide scrollbars after pagination update
        hideScrollBars();
    }
    
    private void updatePaginationButtons() {
        if (prevBtn == null || nextBtn == null) return;
        
        int totalPages = (int) Math.ceil((double) totalStudents / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        
        // Disable Previous button on first page
        boolean prevDisabled = currentPage <= 1;
        prevBtn.setDisable(prevDisabled);
        
        // Disable Next button on last page
        boolean nextDisabled = currentPage >= totalPages;
        nextBtn.setDisable(nextDisabled);
        
        // Update button styles based on enabled/disabled state
        if (prevDisabled) {
            prevBtn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #999; -fx-background-radius: 5; -fx-font-size: 12; -fx-cursor: default;");
        } else {
            prevBtn.setStyle("-fx-background-color: #7B76F1; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-size: 12; -fx-cursor: hand;");
        }
        
        if (nextDisabled) {
            nextBtn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #999; -fx-background-radius: 5; -fx-font-size: 12; -fx-cursor: default;");
        } else {
            nextBtn.setStyle("-fx-background-color: #7B76F1; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-size: 12; -fx-cursor: hand;");
        }
    }
    
    @FXML
    private void handlePrevPage() {
        if (prevBtn.isDisabled()) {
            return; // Button is disabled, do nothing
        }
        
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
        }
    }
    
    @FXML
    private void handleNextPage() {
        if (nextBtn.isDisabled()) {
            return; // Button is disabled, do nothing
        }
        
        int totalPages = (int) Math.ceil((double) totalStudents / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }

    @FXML
    private void handleImport() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Excel File");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"));

        File file = fileChooser.showOpenDialog((Stage) importBtn.getScene().getWindow());
        if (file != null) {
            importStudentsFromExcel(file);
        }
    }

    private boolean hasStudentChanges(Student original, Student updated) {
        return !equalsStr(original.getStudentNumber(), updated.getStudentNumber()) ||
               !equalsStr(original.getFirstName(), updated.getFirstName()) ||
               !equalsStr(original.getMiddleName(), updated.getMiddleName()) ||
               !equalsStr(original.getLastName(), updated.getLastName()) ||
               !equalsStr(original.getMajor(), updated.getMajor()) ||
               !equalsStr(original.getYear(), updated.getYear()) ||
               !equalsStr(original.getSemester(), updated.getSemester()) ||
               !equalsInt(original.getSchoolYearId(), updated.getSchoolYearId()) ||
               !equalsStr(original.getStatus(), updated.getStatus());
    }

    private boolean equalsStr(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private boolean equalsInt(Integer a, Integer b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
    
    private Student createStudentFromForm() {
        Student student = new Student();
        student.setStudentNumber(studentIdField.getText().trim());
        student.setFirstName(firstNameField.getText().trim());
        student.setMiddleName(middleNameField.getText().trim());
        student.setLastName(lastNameField.getText().trim());
        // Get program from ComboBox
        String programValue = majorField.getValue();
        student.setMajor(programValue != null ? programValue : "");
        String yearValue = yearField.getValue();
        student.setYear(yearValue != null ? yearValue : "");
        
        // Set semester from the combo box
        String semesterValue = semesterCombo.getValue();
        student.setSemester(semesterValue != null ? semesterValue : "");
        
        // Use form's school year if set, otherwise use global school year from SessionManager
        String selectedYear = schoolYearCombo.getValue();
        if (selectedYear != null) {
            SchoolYear sy = schoolYearDAO.getSchoolYearByRange(selectedYear);
            if (sy != null) {
                student.setSchoolYearId(sy.getSchoolYearId());
            }
        } else {
            // Use global school year from SessionManager
            Integer globalSchoolYearId = utils.SessionManager.getSelectedSchoolYearId();
            if (globalSchoolYearId != null) {
                student.setSchoolYearId(globalSchoolYearId);
            }
        }
        
        return student;
    }
    
    private boolean validateForm() {
        if (studentIdField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Student ID is required!");
            return false;
        }
        if (firstNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "First name is required!");
            return false;
        }
        if (lastNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Last name is required!");
            return false;
        }
        
        // Validate first name - only letters allowed
        String firstName = firstNameField.getText().trim();
        if (!isValidName(firstName)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", 
                "First name can only contain letters, spaces, hyphens, and apostrophes.\n" +
                "Numbers and special symbols are not allowed.");
            return false;
        }
        
        // Validate middle name - only letters allowed (if provided)
        String middleName = middleNameField.getText().trim();
        if (!middleName.isEmpty() && !isValidName(middleName)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", 
                "Middle name can only contain letters, spaces, hyphens, and apostrophes.\n" +
                "Numbers and special symbols are not allowed.");
            return false;
        }
        
        // Validate last name - only letters allowed
        String lastName = lastNameField.getText().trim();
        if (!isValidName(lastName)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", 
                "Last name can only contain letters, spaces, hyphens, and apostrophes.\n" +
                "Numbers and special symbols are not allowed.");
            return false;
        }
        
        // Require semester selection so duplicate checks can target the correct term
        if (semesterCombo.getValue() == null || semesterCombo.getValue().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Semester is required!");
            return false;
        }
        // Require a school year (from the form or global selection)
        String selectedYear = schoolYearCombo.getValue();
        Integer globalSchoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        if ((selectedYear == null || selectedYear.trim().isEmpty()) && globalSchoolYearId == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "School Year is required!");
            return false;
        }
        return true;
    }
    
    /**
     * Validates that a name contains only letters, spaces, hyphens, and apostrophes
     * @param name The name to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        // Allow letters (including accented characters), spaces, hyphens, and apostrophes
        // This regex allows: A-Z, a-z, spaces, hyphens (-), and apostrophes (')
        return name.matches("^[a-zA-Z\\s\\-']+$");
    }
    
    private void clearForm() {
        studentIdField.clear();
        firstNameField.clear();
        middleNameField.clear();
        lastNameField.clear();
        majorField.setValue(null);
        yearField.setValue(null);
        schoolYearCombo.setValue(null);
        // Auto-select semester based on current month when clearing form
        String autoSemester = getSemesterByMonth(java.time.LocalDate.now().getMonth());
        semesterCombo.setValue(autoSemester);
    }
    
    // Navigation methods
    @FXML
    private void handleDashboard() {
        navigateToPage("dashboard.fxml", "DorPay - Dashboard", dashboardBtn);
    }
    
    @FXML
    private void handleStudents() {
        // Already on students page
    }
    
    @FXML
    private void handlePayables() {
        navigateToPage("payables.fxml", "DorPay - Payables", payablesBtn);
    }
    
    @FXML
    private void handlePayments() {
        navigateToPage("payments.fxml", "DorPay - Payments", paymentsBtn);
    }
    
    @FXML
    private void handleNotifications() {
        navigateToPage("notifications.fxml", "DorPay - Notifications", notificationsBtn);
    }
    
    @FXML
    private void handleSchoolYears() {
        navigateToPage("schoolyears.fxml", "DorPay - School Years", schoolYearsBtn);
    }
    
    @FXML
    private void handlePromissoryNotes() {
        navigateToPage("promissorynotes.fxml", "DorPay - Promissory Notes", promissoryNotesBtn);
    }
    
    @FXML
    private void handleReports() {
        navigateToPage("reports.fxml", "DorPay - Reports", reportsBtn);
    }
    
    @FXML
    private void handleSettings() {
        navigateToPage("settings.fxml", "DorPay - Settings", settingsBtn);
    }
    
    @FXML
    private void handleLogout() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Logout");
        confirmAlert.setHeaderText("Logout");
        confirmAlert.setContentText("Are you sure you want to logout?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                utils.SessionManager.clearSession();
                navigateToPage("login.fxml", "DorPay - Login", logoutBtn);
            }
        });
    }
}

