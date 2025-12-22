package controllers;

import dao.PromissoryNoteDAO;
import dao.SchoolYearDAO;
import dao.StudentDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import models.PromissoryNoteView;
import models.SchoolYear;
import utils.WordDocumentGenerator;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class PromissoryNotesController extends BaseController {
    
    @FXML private TableView<PromissoryNoteView> studentsTable;
    @FXML private TableColumn<PromissoryNoteView, String> studentIdCol;
    @FXML private TableColumn<PromissoryNoteView, String> studentNameCol;
    @FXML private TableColumn<PromissoryNoteView, String> programCol;
    @FXML private TableColumn<PromissoryNoteView, String> yearCol;
    @FXML private TableColumn<PromissoryNoteView, String> dueDateCol;
    @FXML private TableColumn<PromissoryNoteView, String> remainingBalanceCol;
    @FXML private TableColumn<PromissoryNoteView, String> statusCol;
    @FXML private Button printBtn;
    @FXML private Button cancelBtn;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> yearComboBoxHeader;
    @FXML private ComboBox<String> semesterComboBoxHeader;
    @FXML private ComboBox<String> yearFilterCombo;
    @FXML private ComboBox<String> majorFilterCombo;
    
    @FXML private Button dashboardBtn;
    @FXML private Button studentsBtn;
    @FXML private Button payablesBtn;
    @FXML private Button paymentsBtn;
    @FXML private Button schoolYearsBtn;
    @FXML private Button promissoryNotesBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;
    
    private PromissoryNoteDAO promissoryNoteDAO;
    private SchoolYearDAO schoolYearDAO;
    private StudentDAO studentDAO;
    private ObservableList<PromissoryNoteView> studentsList;
    private ObservableList<PromissoryNoteView> allStudentsList;
    private Integer currentSchoolYearId;
    
    @FXML
    public void initialize() {
        promissoryNoteDAO = new PromissoryNoteDAO();
        schoolYearDAO = new SchoolYearDAO();
        studentDAO = new StudentDAO();
        studentsList = FXCollections.observableArrayList();
        allStudentsList = FXCollections.observableArrayList();
        
        setupTable();
        setupSidebarButtons();
        setupInputStyles();
        setupHeaderDropdowns();
        loadFilterData();
        loadSchoolYears();
        loadStudents();
    }
    
    private void setupSidebarButtons() {
        setupSidebarButtonHover(dashboardBtn, false);
        setupSidebarButtonHover(studentsBtn, false);
        setupSidebarButtonHover(payablesBtn, false);
        setupSidebarButtonHover(paymentsBtn, false);
        setupSidebarButtonHover(schoolYearsBtn, false);
        setupSidebarButtonHover(promissoryNotesBtn, true);
        setupSidebarButtonHover(settingsBtn, false);
        setupSidebarButtonHover(logoutBtn, false);
    }
    
    private void setupTable() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentNumber"));
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        programCol.setCellValueFactory(new PropertyValueFactory<>("major"));
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDateFormatted"));
        remainingBalanceCol.setCellValueFactory(new PropertyValueFactory<>("remainingBalanceFormatted"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Make all columns resizable
        studentIdCol.setResizable(true);
        studentNameCol.setResizable(true);
        programCol.setResizable(true);
        yearCol.setResizable(true);
        dueDateCol.setResizable(true);
        remainingBalanceCol.setResizable(true);
        statusCol.setResizable(true);
        
        // Set column width percentages to distribute space evenly and use full width
        // Total percentages should be close to 100% to fill the table
        studentIdCol.prefWidthProperty().bind(studentsTable.widthProperty().multiply(0.10));
        studentNameCol.prefWidthProperty().bind(studentsTable.widthProperty().multiply(0.20));
        programCol.prefWidthProperty().bind(studentsTable.widthProperty().multiply(0.30)); // More space for program
        yearCol.prefWidthProperty().bind(studentsTable.widthProperty().multiply(0.08));
        dueDateCol.prefWidthProperty().bind(studentsTable.widthProperty().multiply(0.10));
        remainingBalanceCol.prefWidthProperty().bind(studentsTable.widthProperty().multiply(0.15));
        statusCol.prefWidthProperty().bind(studentsTable.widthProperty().multiply(0.07));
        
        studentsTable.setItems(studentsList);
        
        // Disable Print button initially
        if (printBtn != null) {
            printBtn.setDisable(true);
        }
        
        // Store selected student when row is clicked and enable Print button
        studentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Store the selected student
                currentSelectedStudent = newSelection;
                // Clear previous document when selecting a new student
                currentDocumentFile = null;
                currentAgreedDate = null;
                // Enable Print button when a row is selected
                if (printBtn != null) {
                    printBtn.setDisable(false);
                }
            } else {
                // Disable Print button when no row is selected
                if (printBtn != null) {
                    printBtn.setDisable(true);
                }
                currentSelectedStudent = null;
            }
        });
    }
    
    private void setupInputStyles() {
        // Apply focus styles to search field
        if (searchField != null) {
            searchField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (isNowFocused) {
                    searchField.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #7B76F1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 14; -fx-text-fill: #333; -fx-prompt-text-fill: #999; -fx-cursor: text; -fx-effect: dropshadow(gaussian, rgba(123, 118, 241, 0.2), 8, 0, 0, 2);");
                } else {
                    searchField.setStyle("-fx-background-color: #F8F9FA; -fx-border-color: #E0E0E0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-width: 1.5; -fx-padding: 6 10; -fx-font-size: 14; -fx-text-fill: #333; -fx-prompt-text-fill: #999; -fx-cursor: text;");
                }
            });
        }
        
        // Apply focus styles to combo boxes
        @SuppressWarnings("unchecked")
        ComboBox<String>[] comboBoxes = new ComboBox[]{
            yearComboBoxHeader, semesterComboBoxHeader, yearFilterCombo, majorFilterCombo
        };
        
        for (ComboBox<String> combo : comboBoxes) {
            if (combo != null) {
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
    
    private void setupHeaderDropdowns() {
        // Setup header year dropdown
        setupSchoolYearDropdown(yearComboBoxHeader);
        
        // Setup header semester dropdown
        if (semesterComboBoxHeader != null) {
            semesterComboBoxHeader.getItems().clear();
            semesterComboBoxHeader.getItems().addAll("1st Sem", "2nd Sem", "Summer Sem");
            semesterComboBoxHeader.setValue("1st Sem"); // Set default to "1st Sem"
            semesterComboBoxHeader.setOnAction(e -> loadStudents());
        }
        
        // Setup header year dropdown change listener
        if (yearComboBoxHeader != null) {
            yearComboBoxHeader.setOnAction(e -> {
                String selected = yearComboBoxHeader.getValue();
                if (selected != null) {
                    SchoolYear sy = schoolYearDAO.getSchoolYearByRange(selected);
                    if (sy != null) {
                        utils.SessionManager.setSelectedSchoolYearId(sy.getSchoolYearId());
                        currentSchoolYearId = sy.getSchoolYearId();
                        loadStudents();
                    }
                }
            });
        }
    }
    
    private void loadFilterData() {
        // Load majors and add "All" option at the beginning
        List<String> majors = studentDAO.getAllMajors();
        if (majorFilterCombo != null) {
            majorFilterCombo.getItems().clear();
            majorFilterCombo.getItems().add("All"); // Add "All" option first
            majorFilterCombo.getItems().addAll(majors);
            majorFilterCombo.setOnAction(e -> handleSearch());
        }
        
        // Load years and add "All" option at the beginning
        List<String> years = studentDAO.getAllYears();
        if (yearFilterCombo != null) {
            yearFilterCombo.getItems().clear();
            yearFilterCombo.getItems().add("All"); // Add "All" option first
            yearFilterCombo.getItems().addAll(years);
            yearFilterCombo.setOnAction(e -> handleSearch());
        }
    }
    
    private void loadSchoolYears() {
        // Get current selected school year
        currentSchoolYearId = utils.SessionManager.getSelectedSchoolYearId();
    }
    
    @Override
    protected void onSchoolYearChanged() {
        // Reload students when school year changes
        currentSchoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        loadStudents();
    }
    
    private void loadStudents() {
        // Get semester from header dropdown
        String semester = null;
        if (semesterComboBoxHeader != null && semesterComboBoxHeader.getValue() != null) {
            String headerValue = semesterComboBoxHeader.getValue();
            if (!headerValue.isEmpty() && !headerValue.equals("Select Semester")) {
                semester = headerValue.trim();
            }
        }
        
        List<PromissoryNoteView> students = promissoryNoteDAO.getStudentsForPromissoryNotes(currentSchoolYearId, semester);
        allStudentsList.clear();
        allStudentsList.addAll(students);
        
        // Apply filters
        handleSearch();
    }
    
    @FXML
    private void handleSearch() {
        String searchTerm = searchField != null ? searchField.getText() : null;
        String year = yearFilterCombo != null ? yearFilterCombo.getValue() : null;
        String major = majorFilterCombo != null ? majorFilterCombo.getValue() : null;
        
        // Get semester from header dropdown
        String semester = null;
        if (semesterComboBoxHeader != null && semesterComboBoxHeader.getValue() != null) {
            String headerValue = semesterComboBoxHeader.getValue();
            if (!headerValue.isEmpty() && !headerValue.equals("Select Semester")) {
                semester = headerValue.trim();
            }
        }
        
        // Filter the list
        studentsList.clear();
        
        for (PromissoryNoteView student : allStudentsList) {
            boolean matches = true;
            
            // Search filter
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String searchLower = searchTerm.toLowerCase();
                matches = matches && (
                    (student.getStudentNumber() != null && student.getStudentNumber().toLowerCase().contains(searchLower)) ||
                    (student.getStudentName() != null && student.getStudentName().toLowerCase().contains(searchLower))
                );
            }
            
            // Year filter
            if (matches && year != null && !year.isEmpty() && !year.equals("All")) {
                matches = matches && year.equals(student.getYear());
            }
            
            // Major filter
            if (matches && major != null && !major.isEmpty() && !major.equals("All")) {
                matches = matches && major.equals(student.getMajor());
            }
            
            if (matches) {
                studentsList.add(student);
            }
        }
    }
    
    private PromissoryNoteView currentSelectedStudent;
    private LocalDate currentAgreedDate;
    private File currentDocumentFile;
    
    private void autoGeneratePromissoryNote(PromissoryNoteView student) {
        currentSelectedStudent = student;
        
        // Show dialog to get agreed payment date
        javafx.scene.control.Dialog<LocalDate> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Set Agreed Payment Date");
        dialog.setHeaderText("Enter the agreed payment date for " + student.getStudentName());
        
        DatePicker datePicker = new DatePicker();
        // Default to due date if available, otherwise 1 month from now
        if (student.getDueDate() != null) {
            datePicker.setValue(student.getDueDate());
        } else {
            datePicker.setValue(LocalDate.now().plusMonths(1));
        }
        
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox();
        vbox.setSpacing(10);
        vbox.setPadding(new javafx.geometry.Insets(20));
        vbox.getChildren().addAll(new javafx.scene.control.Label("Agreed Payment Date:"), datePicker);
        
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == javafx.scene.control.ButtonType.OK) {
                return datePicker.getValue();
            }
            return null;
        });
        
        java.util.Optional<LocalDate> result = dialog.showAndWait();
        result.ifPresent(agreedDate -> {
            currentAgreedDate = agreedDate;
            // Generate Word document
            Stage stage = (Stage) printBtn.getScene().getWindow();
            currentDocumentFile = WordDocumentGenerator.generatePromissoryNoteFile(student, agreedDate, stage);
            if (currentDocumentFile != null) {
                // Save to database
                promissoryNoteDAO.savePromissoryNote(
                    student.getStudentId(),
                    LocalDate.now(),
                    agreedDate,
                    student.getRemainingBalance(),
                    "Promissory note generated for " + student.getStudentName()
                );
                showAlert(Alert.AlertType.INFORMATION, "Promissory Note Generated", 
                    "Promissory note has been generated. Click 'Print' to print or 'Cancel' to close.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate promissory note document.");
                currentSelectedStudent = null;
                currentAgreedDate = null;
            }
        });
    }
    
    @FXML
    private void handlePrint() {
        // Check if we have a selected student (should not happen since button is disabled, but safety check)
        if (currentSelectedStudent == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a student from the table first by clicking on a row.");
            return;
        }
        
        // Show dialog to get agreed payment date
        javafx.scene.control.Dialog<LocalDate> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Set Agreed Payment Date");
        dialog.setHeaderText("Enter the agreed payment date for " + currentSelectedStudent.getStudentName());
        
        DatePicker datePicker = new DatePicker();
        // Default to due date if available, otherwise 1 month from now
        if (currentSelectedStudent.getDueDate() != null) {
            // Default to 1 month after the due date, or today if due date is in the past
            LocalDate defaultDate = currentSelectedStudent.getDueDate().isBefore(LocalDate.now()) 
                ? LocalDate.now().plusMonths(1) 
                : currentSelectedStudent.getDueDate().plusMonths(1);
            datePicker.setValue(defaultDate);
        } else {
            datePicker.setValue(LocalDate.now().plusMonths(1));
        }
        
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox();
        vbox.setSpacing(10);
        vbox.setPadding(new javafx.geometry.Insets(20));
        vbox.getChildren().addAll(new javafx.scene.control.Label("Agreed Payment Date:"), datePicker);
        
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == javafx.scene.control.ButtonType.OK) {
                return datePicker.getValue();
            }
            return null;
        });
        
        java.util.Optional<LocalDate> result = dialog.showAndWait();
        result.ifPresent(agreedDate -> {
            // Generate the document
            Stage stage = (Stage) printBtn.getScene().getWindow();
            currentDocumentFile = WordDocumentGenerator.generatePromissoryNoteFile(currentSelectedStudent, agreedDate, stage);
            
            if (currentDocumentFile == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate promissory note document.");
                return;
            }
            
            // Save to database
            promissoryNoteDAO.savePromissoryNote(
                currentSelectedStudent.getStudentId(),
                LocalDate.now(),
                agreedDate,
                currentSelectedStudent.getRemainingBalance(),
                "Promissory note generated for " + currentSelectedStudent.getStudentName()
            );
            
            // Verify file exists
            if (!currentDocumentFile.exists()) {
                showAlert(Alert.AlertType.ERROR, "File Not Found", "The document file was not found. Please try again.");
                currentDocumentFile = null;
                return;
            }
            
            try {
                // Open the document - this will allow the user to print from Word/application
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                    
                    // Open the file (Word/WordPad will open it, then user can print)
                    desktop.open(currentDocumentFile);
                    showAlert(Alert.AlertType.INFORMATION, "Document Ready for Printing", 
                        "Promissory note document has been generated and opened.\n\n" +
                        "To print: Use File > Print or press Ctrl+P in the opened application.\n\n" +
                        "File location: " + currentDocumentFile.getAbsolutePath());
                } else {
                    showAlert(Alert.AlertType.WARNING, "Print Not Supported", 
                        "Print functionality is not available. The document has been saved to: " + currentDocumentFile.getAbsolutePath());
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Print Error", "Error opening document: " + e.getMessage() + 
                    "\n\nFile location: " + currentDocumentFile.getAbsolutePath());
                e.printStackTrace();
            }
        });
    }
    
    @FXML
    private void handleCancel() {
        currentSelectedStudent = null;
        currentAgreedDate = null;
        currentDocumentFile = null;
        studentsTable.getSelectionModel().clearSelection();
        showAlert(Alert.AlertType.INFORMATION, "Cancelled", "Promissory note generation cancelled.");
    }
    
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
        navigateToPage("payables.fxml", "DorPay - Payables", payablesBtn);
    }
    
    @FXML
    private void handlePayments() {
        navigateToPage("payments.fxml", "DorPay - Payments", paymentsBtn);
    }
    
    @FXML
    private void handleSchoolYears() {
        navigateToPage("schoolyears.fxml", "DorPay - School Years", schoolYearsBtn);
    }
    
    @FXML
    private void handlePromissoryNotes() {
        // Already on promissory notes page
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

