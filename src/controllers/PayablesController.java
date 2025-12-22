package controllers;

import dao.PayableDAO;
import dao.SchoolYearDAO;
import dao.StudentDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.scene.control.TextFormatter;
import java.util.function.UnaryOperator;
import models.StudentPayableView;
import models.Student;
import models.SchoolYear;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class PayablesController extends BaseController {
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> yearFilterCombo;
    @FXML private ComboBox<String> majorFilterCombo;
    @FXML private ComboBox<String> yearComboBoxHeader;
    @FXML private ComboBox<String> semesterComboBoxHeader;
    
    @FXML private TableView<StudentPayableView> payablesTable;
    @FXML private TableColumn<StudentPayableView, String> studentIdCol;
    @FXML private TableColumn<StudentPayableView, String> studentNameCol;
    @FXML private TableColumn<StudentPayableView, String> programCol;
    @FXML private TableColumn<StudentPayableView, String> yearCol;
    @FXML private TableColumn<StudentPayableView, String> semesterCol;
    @FXML private TableColumn<StudentPayableView, String> payableAmountCol;
    @FXML private TableColumn<StudentPayableView, String> dueDateCol;
    
    @FXML private TextField studentIdField;
    @FXML private TextField fullnameField;
    @FXML private TextField majorField;
    @FXML private TextField yearField;
    @FXML private TextField semesterAmountField;
    @FXML private TextField dueDateField;
    
    @FXML private Button addBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Text pageInfoLabel;
    @FXML private Text totalPayablesLabel;
    
    @FXML private Button dashboardBtn;
    @FXML private Button studentsBtn;
    @FXML private Button payablesBtn;
    @FXML private Button paymentsBtn;
    @FXML private Button notificationsBtn;
    @FXML private Button schoolYearsBtn;
    @FXML private Button promissoryNotesBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;
    
    private PayableDAO payableDAO;
    private StudentDAO studentDAO;
    private SchoolYearDAO schoolYearDAO;
    private ObservableList<StudentPayableView> payablesList;
    private ObservableList<StudentPayableView> allPayablesList;
    private StudentPayableView selectedPayable;
    private Integer currentSchoolYearId;
    
    // Pagination variables
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;
    private int totalPayables = 0;
    
    @FXML
    public void initialize() {
        payableDAO = new PayableDAO();
        studentDAO = new StudentDAO();
        schoolYearDAO = new SchoolYearDAO();
        payablesList = FXCollections.observableArrayList();
        allPayablesList = FXCollections.observableArrayList();
        
        setupTable();
        setupSidebarButtons();
        loadSchoolYears();
        setupHeaderDropdowns();
        loadFilters();
        loadPayables();
        setupPagination();
        setupButtonStates();
        setupNumericValidation();
    }
    
    private void setupSidebarButtons() {
        setupSidebarButtonHover(dashboardBtn, false);
        setupSidebarButtonHover(studentsBtn, false);
        setupSidebarButtonHover(payablesBtn, true);
        setupSidebarButtonHover(paymentsBtn, false);
        setupSidebarButtonHover(schoolYearsBtn, false);
        setupSidebarButtonHover(promissoryNotesBtn, false);
        setupSidebarButtonHover(settingsBtn, false);
        setupSidebarButtonHover(logoutBtn, false);
    }
    
    private void setupNumericValidation() {
        // Create a filter that only allows numeric input (digits and one decimal point)
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            // Allow empty string, digits, and one decimal point
            if (newText.isEmpty() || newText.matches("^\\d*\\.?\\d*$")) {
                return change;
            }
            return null; // Reject the change
        };
        
        // Apply TextFormatter to semesterAmountField
        if (semesterAmountField != null) {
            TextFormatter<String> formatter = new TextFormatter<>(filter);
            semesterAmountField.setTextFormatter(formatter);
        }
    }
    
    private void setupTable() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentNumber"));
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        programCol.setCellValueFactory(new PropertyValueFactory<>("program"));
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        semesterCol.setCellValueFactory(new PropertyValueFactory<>("semester"));
        payableAmountCol.setCellValueFactory(new PropertyValueFactory<>("payableAmount"));
        dueDateCol.setCellValueFactory(cellData -> {
            StudentPayableView view = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(view.getDueDateFormatted());
        });
        
        payablesTable.setItems(payablesList);
        payablesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Setup student ID field to allow manual entry
        if (studentIdField != null) {
            studentIdField.setEditable(true);
            studentIdField.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-width: 1.5; -fx-padding: 6 10; -fx-font-size: 14; -fx-text-fill: #333; -fx-prompt-text-fill: #999; -fx-cursor: text;");
        }
    }
    
    private void setupHeaderDropdowns() {
        if (yearComboBoxHeader != null) {
            setupSchoolYearDropdown(yearComboBoxHeader);
            yearComboBoxHeader.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    onSchoolYearChanged();
                }
            });
        }
        
        if (semesterComboBoxHeader != null) {
            semesterComboBoxHeader.getItems().addAll("1st Sem", "2nd Sem", "Summer Sem");
            semesterComboBoxHeader.setValue("1st Sem");
            semesterComboBoxHeader.setOnAction(e -> {
                currentPage = 1;
                loadPayables();
            });
        }
    }
    
    @Override
    protected void onSchoolYearChanged() {
        String selectedYear = yearComboBoxHeader.getValue();
        if (selectedYear != null) {
            SchoolYear schoolYear = schoolYearDAO.getSchoolYearByRange(selectedYear);
            if (schoolYear != null) {
                utils.SessionManager.setSelectedSchoolYearId(schoolYear.getSchoolYearId());
                currentSchoolYearId = schoolYear.getSchoolYearId();
                currentPage = 1;
                loadPayables();
            }
        }
    }
    
    private String getSelectedSemester() {
        if (semesterComboBoxHeader != null && semesterComboBoxHeader.getValue() != null) {
            return semesterComboBoxHeader.getValue();
        }
        return "1st Sem";
    }
    
    private void loadFilters() {
        List<String> majors = studentDAO.getAllMajors();
        majorFilterCombo.getItems().clear();
        majorFilterCombo.getItems().add("All");
        majorFilterCombo.getItems().addAll(majors);
        
        List<String> years = studentDAO.getAllYears();
        yearFilterCombo.getItems().clear();
        yearFilterCombo.getItems().add("All");
        yearFilterCombo.getItems().addAll(years);
    }
    
    private void loadPayables() {
        Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        currentSchoolYearId = schoolYearId;
        String semester = getSelectedSemester();
        
        List<StudentPayableView> payables = payableDAO.getAllStudentPayables(schoolYearId, semester);
        
        // Apply search and filters
        String searchTerm = searchField.getText().trim().toLowerCase();
        String yearFilter = yearFilterCombo.getValue();
        String majorFilter = majorFilterCombo.getValue();
        
        if (!searchTerm.isEmpty() || (yearFilter != null && !yearFilter.equals("All")) || 
            (majorFilter != null && !majorFilter.equals("All"))) {
            payables = payables.stream()
                .filter(p -> {
                    boolean matches = true;
                    if (!searchTerm.isEmpty()) {
                        matches = matches && (p.getStudentNumber().toLowerCase().contains(searchTerm) ||
                                            p.getStudentName().toLowerCase().contains(searchTerm));
                    }
                    if (yearFilter != null && !yearFilter.equals("All")) {
                        matches = matches && yearFilter.equals(p.getYear());
                    }
                    if (majorFilter != null && !majorFilter.equals("All")) {
                        matches = matches && majorFilter.equals(p.getProgram());
                    }
                    return matches;
                })
                .collect(Collectors.toList());
        }
        
        allPayablesList.clear();
        allPayablesList.addAll(payables);
        totalPayables = allPayablesList.size();
        
        if (totalPayablesLabel != null) {
            totalPayablesLabel.setText("(Total: " + totalPayables + ")");
        }
        
        updatePagination();
    }
    
    @FXML
    private void handleSearch() {
        currentPage = 1;
        loadPayables();
    }
    
    @FXML
    private void handleTableClick() {
        selectedPayable = payablesTable.getSelectionModel().getSelectedItem();
        if (selectedPayable != null) {
            populateForm(selectedPayable);
            
            // Check if student has payables for the selected semester
            Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
            String semester = getSelectedSemester();
            boolean hasPayable = payableDAO.hasStudentPayable(selectedPayable.getStudentId(), schoolYearId, semester);
            
            // Set button states
            if (hasPayable) {
                addBtn.setDisable(true);
                updateBtn.setDisable(false);
                deleteBtn.setDisable(false);
            } else {
                addBtn.setDisable(false);
                updateBtn.setDisable(true);
                deleteBtn.setDisable(true);
            }
        }
    }
    
    @FXML
    private void handleStudentIdSearch() {
        String studentId = studentIdField.getText().trim();
        if (studentId.isEmpty()) {
            // Clear form if student ID is empty
            fullnameField.clear();
            majorField.clear();
            yearField.clear();
            semesterAmountField.setText("0.00");
            selectedPayable = null;
            setupButtonStates();
            return;
        }
        
        // Search for student by ID
        Student student = studentDAO.getStudentByNumber(studentId);
        if (student != null) {
            // Populate form with student info
            fullnameField.setText(student.getFullname());
            majorField.setText(student.getMajor() != null ? student.getMajor() : "");
            yearField.setText(student.getYear() != null ? student.getYear() : "");
            
            // Load existing payable for the selected semester and school year
            Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
            String semester = getSelectedSemester();
            
            if (schoolYearId != null) {
                StudentPayableView existingPayable = payableDAO.getStudentPayable(student.getStudentId(), schoolYearId);
                if (existingPayable != null) {
                    // Set amount based on selected semester
                    double amount = 0;
                    if ("1st Sem".equals(semester)) {
                        amount = existingPayable.getFirstSemAmount();
                    } else if ("2nd Sem".equals(semester)) {
                        amount = existingPayable.getSecondSemAmount();
                    } else if ("Summer Sem".equals(semester)) {
                        amount = existingPayable.getSummerSemAmount();
                    }
                    
                    // Set amount field - blank if 0, otherwise show the amount
                    if (amount > 0) {
                        semesterAmountField.setText(String.format("%.2f", amount));
                    } else {
                        semesterAmountField.clear();
                    }
                    
                    if (existingPayable.getDueDate() != null) {
                        dueDateField.setText(existingPayable.getDueDateFormatted());
                    } else {
                        LocalDate dueDate = LocalDate.now().plusMonths(2);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                        dueDateField.setText(dueDate.format(formatter));
                    }
                    
                    // Check if payable exists for this semester
                    boolean hasPayable = payableDAO.hasStudentPayable(student.getStudentId(), schoolYearId, semester);
                    if (hasPayable) {
                        addBtn.setDisable(true);
                        updateBtn.setDisable(false);
                        deleteBtn.setDisable(false);
                    } else {
                        addBtn.setDisable(false);
                        updateBtn.setDisable(true);
                        deleteBtn.setDisable(true);
                    }
                } else {
                    // No payable exists, enable Add button
                    semesterAmountField.setText("0.00");
                    LocalDate dueDate = LocalDate.now().plusMonths(2);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                    dueDateField.setText(dueDate.format(formatter));
                    addBtn.setDisable(false);
                    updateBtn.setDisable(true);
                    deleteBtn.setDisable(true);
                }
            } else {
                // No school year selected
                semesterAmountField.setText("0.00");
                LocalDate dueDate = LocalDate.now().plusMonths(2);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                dueDateField.setText(dueDate.format(formatter));
                addBtn.setDisable(true);
                updateBtn.setDisable(true);
                deleteBtn.setDisable(true);
            }
        } else {
            // Student not found
            fullnameField.clear();
            majorField.clear();
            yearField.clear();
            semesterAmountField.setText("0.00");
            dueDateField.clear();
            addBtn.setDisable(true);
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
        }
    }
    
    private void populateForm(StudentPayableView payable) {
        studentIdField.setText(payable.getStudentNumber());
        fullnameField.setText(payable.getStudentName());
        majorField.setText(payable.getProgram());
        yearField.setText(payable.getYear());
        
        // Set amount based on selected semester
        String semester = getSelectedSemester();
        double amount = 0;
        if ("1st Sem".equals(semester)) {
            amount = payable.getFirstSemAmount();
        } else if ("2nd Sem".equals(semester)) {
            amount = payable.getSecondSemAmount();
        } else if ("Summer Sem".equals(semester)) {
            amount = payable.getSummerSemAmount();
        }
        
        // Set amount field - blank if 0, otherwise show the amount
        if (amount > 0) {
            semesterAmountField.setText(String.format("%.2f", amount));
        } else {
            semesterAmountField.clear();
        }
        
        if (payable.getDueDate() != null) {
            dueDateField.setText(payable.getDueDateFormatted());
        } else {
            LocalDate dueDate = LocalDate.now().plusMonths(2);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            dueDateField.setText(dueDate.format(formatter));
        }
    }
    
    @FXML
    private void handleAdd() {
        if (validateFormForAdd()) {
            Student student = getStudentFromForm();
            if (student == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Student not found! Please enter a valid Student ID.");
                return;
            }
            
            Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
            if (schoolYearId == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Please select a School Year!");
                return;
            }
            
            currentSchoolYearId = schoolYearId;
            String semester = getSelectedSemester();
            double amount = parseAmount(semesterAmountField.getText());
            
            // Check if payable already exists for this student, semester, and school year
            if (payableDAO.hasStudentPayable(student.getStudentId(), schoolYearId, semester)) {
                showAlert(Alert.AlertType.WARNING, "Warning", 
                    "Payable already exists for this student in " + semester + ". Use Update instead.");
                return;
            }
            
            double firstSem = 0, secondSem = 0, summerSem = 0;
            if ("1st Sem".equals(semester)) {
                firstSem = amount;
            } else if ("2nd Sem".equals(semester)) {
                secondSem = amount;
            } else if ("Summer Sem".equals(semester)) {
                summerSem = amount;
            }
            
            if (payableDAO.saveStudentPayable(student.getStudentId(), schoolYearId, firstSem, secondSem, summerSem, semester)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "Payable added successfully for " + semester + "!");
                clearForm();
                currentPage = 1;
                loadPayables();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add payable!");
            }
        }
    }
    
    @FXML
    private void handleUpdate() {
        if (selectedPayable == null && studentIdField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a payable from the table or enter a Student ID to update!");
            return;
        }
        
        if (validateFormForUpdate()) {
            Student student = getStudentFromForm();
            if (student == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Student not found! Please enter a valid Student ID.");
                return;
            }
            
            Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
            if (schoolYearId == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Please select a School Year!");
                return;
            }
            
            currentSchoolYearId = schoolYearId;
            String semester = getSelectedSemester();
            
            // Check if payable exists for this student, semester, and school year
            if (!payableDAO.hasStudentPayable(student.getStudentId(), schoolYearId, semester)) {
                showAlert(Alert.AlertType.WARNING, "Warning", 
                    "No payable found for this student in " + semester + ". Use Add instead.");
                return;
            }
            
            double amount = parseAmount(semesterAmountField.getText());
            
            double firstSem = 0, secondSem = 0, summerSem = 0;
            if ("1st Sem".equals(semester)) {
                firstSem = amount;
            } else if ("2nd Sem".equals(semester)) {
                secondSem = amount;
            } else if ("Summer Sem".equals(semester)) {
                summerSem = amount;
            }
            
            if (payableDAO.updateStudentPayable(student.getStudentId(), schoolYearId, firstSem, secondSem, summerSem, semester)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "Payable updated successfully for " + semester + "!");
                clearForm();
                currentPage = 1;
                loadPayables();
                selectedPayable = null;
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update payable!");
            }
        }
    }
    
    @FXML
    private void handleDelete() {
        if (selectedPayable == null && studentIdField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a payable from the table or enter a Student ID to delete!");
            return;
        }
        
        Student student = getStudentFromForm();
        if (student == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Student not found! Please enter a valid Student ID.");
            return;
        }
        
        Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        if (schoolYearId == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a School Year!");
            return;
        }
        
        String semester = getSelectedSemester();
        String studentName = selectedPayable != null ? selectedPayable.getStudentName() : student.getFullname();
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Payable");
        confirmAlert.setContentText("Are you sure you want to delete payables for " + studentName + 
            " in " + semester + " for the selected school year?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                // Delete payable for specific semester and school year
                if (payableDAO.deleteStudentPayable(student.getStudentId(), schoolYearId, semester)) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", 
                        "Payable deleted successfully for " + semester + "!");
                    clearForm();
                    currentPage = 1;
                    loadPayables();
                    selectedPayable = null;
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete payable!");
                }
            }
        });
    }
    
    @FXML
    private void handleClear() {
        clearForm();
        selectedPayable = null;
        payablesTable.getSelectionModel().clearSelection();
        setupButtonStates();
    }
    
    @FXML
    private void calculateTotal() {
        // This method can be used for any calculations if needed
    }
    
    private void clearForm() {
        studentIdField.clear();
        fullnameField.clear();
        majorField.clear();
        yearField.clear();
        semesterAmountField.setText("0.00");
        dueDateField.clear();
        selectedPayable = null;
        setupButtonStates();
    }
    
    private boolean validateFormForAdd() {
        String studentId = studentIdField.getText().trim();
        if (studentId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a Student ID!");
            return false;
        }
        
        String amountText = semesterAmountField.getText().trim();
        if (amountText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter an amount!");
            return false;
        }
        
        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Amount must be greater than 0!");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a valid amount!");
            return false;
        }
        
        return true;
    }
    
    private boolean validateFormForUpdate() {
        String studentId = studentIdField.getText().trim();
        if (studentId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a Student ID!");
            return false;
        }
        
        String amountText = semesterAmountField.getText().trim();
        if (amountText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter an amount!");
            return false;
        }
        
        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Amount must be greater than 0!");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a valid amount!");
            return false;
        }
        
        return true;
    }
    
    private Student getStudentFromForm() {
        String studentNumber = studentIdField.getText().trim();
        if (studentNumber.isEmpty()) {
            return null;
        }
        return studentDAO.getStudentByNumber(studentNumber);
    }
    
    private double parseAmount(String amountText) {
        try {
            return Double.parseDouble(amountText.trim().replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private void setupButtonStates() {
        // Check if we have a valid student ID entered
        String studentId = studentIdField.getText().trim();
        if (!studentId.isEmpty()) {
            Student student = studentDAO.getStudentByNumber(studentId);
            if (student != null) {
                Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
                String semester = getSelectedSemester();
                if (schoolYearId != null) {
                    boolean hasPayable = payableDAO.hasStudentPayable(student.getStudentId(), schoolYearId, semester);
                    addBtn.setDisable(hasPayable);
                    updateBtn.setDisable(!hasPayable);
                    deleteBtn.setDisable(!hasPayable);
                } else {
                    addBtn.setDisable(true);
                    updateBtn.setDisable(true);
                    deleteBtn.setDisable(true);
                }
            } else {
                addBtn.setDisable(true);
                updateBtn.setDisable(true);
                deleteBtn.setDisable(true);
            }
        } else {
            addBtn.setDisable(true);
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
        }
    }
    
    private void setupPagination() {
        updatePaginationButtons();
    }
    
    private void updatePagination() {
        int totalPages = (int) Math.ceil((double) totalPayables / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        if (currentPage < 1) {
            currentPage = 1;
        }
        
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalPayables);
        
        payablesList.clear();
        if (startIndex < allPayablesList.size()) {
            int actualEndIndex = Math.min(endIndex, allPayablesList.size());
            payablesList.addAll(allPayablesList.subList(startIndex, actualEndIndex));
        }
        
        if (pageInfoLabel != null) {
            pageInfoLabel.setText("Page " + currentPage + " of " + totalPages);
        }
        
        updatePaginationButtons();
    }
    
    private void updatePaginationButtons() {
        if (prevBtn == null || nextBtn == null) return;
        
        int totalPages = (int) Math.ceil((double) totalPayables / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        
        boolean prevDisabled = currentPage <= 1;
        prevBtn.setDisable(prevDisabled);
        
        boolean nextDisabled = currentPage >= totalPages;
        nextBtn.setDisable(nextDisabled);
        
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
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
        }
    }
    
    @FXML
    private void handleNextPage() {
        int totalPages = (int) Math.ceil((double) totalPayables / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }
    
    private void loadSchoolYears() {
        currentSchoolYearId = utils.SessionManager.getSelectedSchoolYearId();
    }
    
    // Navigation methods
    @FXML
    private void handleDashboard() {
        navigateToPage("dashboard.fxml", "DorPay - Dashboard", dashboardBtn);
    }
    
    @FXML
    private void handleStudents() {
        navigateToPage("students.fxml", "DorPay - Students", studentsBtn);
    }
    
    @FXML
    private void handlePayables() {
        // Already on payables page
    }
    
    @FXML
    private void handlePayments() {
        navigateToPage("payments.fxml", "DorPay - Payments", paymentsBtn);
    }
    
    @FXML
    private void handleSchoolYears() {
        navigateToPage("schoolYears.fxml", "DorPay - School Years", schoolYearsBtn);
    }
    
    @FXML
    private void handlePromissoryNotes() {
        navigateToPage("promissoryNotes.fxml", "DorPay - Promissory Notes", promissoryNotesBtn);
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
                navigateToPage("login.fxml", "DorPay - Login", logoutBtn);
            }
        });
    }
    
}

