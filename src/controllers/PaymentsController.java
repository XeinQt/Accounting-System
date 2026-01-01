package controllers;

import dao.PaymentDAO;
import dao.SchoolYearDAO;
import dao.StudentDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.scene.control.TextFormatter;
import java.util.function.UnaryOperator;
import models.PaymentView;
import models.Student;
import models.SchoolYear;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class PaymentsController extends BaseController {
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> yearFilterCombo;
    @FXML private ComboBox<String> majorFilterCombo;
    @FXML private ComboBox<String> yearComboBoxHeader;
    @FXML private ComboBox<String> semesterComboBoxHeader;
    
    @FXML private TableView<PaymentView> paymentsTable;
    @FXML private TableColumn<PaymentView, String> studentIdCol;
    @FXML private TableColumn<PaymentView, String> studentNameCol;
    @FXML private TableColumn<PaymentView, String> semesterAmountCol;
    @FXML private TableColumn<PaymentView, String> amountPaidCol;
    @FXML private TableColumn<PaymentView, String> dueDateCol;
    @FXML private TableColumn<PaymentView, String> statusCol;
    
    @FXML private TextField studentIdField;
    @FXML private TextField fullnameField;
    @FXML private TextField majorField;
    @FXML private TextField yearField;
    
    @FXML private TextField payablesField;
    
    @FXML private TextField amountPaidField;
    @FXML private TextField totalPaidField;
    @FXML private TextField totalNeededField;
    @FXML private TextField balanceToPayField;
    @FXML private TextField dueDateField;
    @FXML private TextField statusField;
    @FXML private Label amountLabel;
    
    @FXML private RadioButton addModeRadio;
    @FXML private RadioButton updateModeRadio;
    
    @FXML private Button saveBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    
    private ToggleGroup paymentModeToggleGroup;
    
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
    
    private PaymentDAO paymentDAO;
    private StudentDAO studentDAO;
    private SchoolYearDAO schoolYearDAO;
    private ObservableList<PaymentView> paymentsList;
    private ObservableList<PaymentView> allPaymentsList; // All payments for pagination
    private PaymentView selectedPayment;
    private Integer currentSchoolYearId;
    private double currentTotalAmount;
    private double currentAmountPaid; // Track existing amount paid for accumulation
    
    // Pagination variables
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;
    private int totalPayments = 0;
    
    @FXML
    public void initialize() {
        paymentDAO = new PaymentDAO();
        studentDAO = new StudentDAO();
        schoolYearDAO = new SchoolYearDAO();
        paymentsList = FXCollections.observableArrayList();
        allPaymentsList = FXCollections.observableArrayList();
        
        setupTable();
        setupSidebarButtons();
        loadSchoolYears();
        setupHeaderDropdowns();
        loadFilters();
        loadPayments();
        setupInputStyles();
        setupPagination();
        setupPaymentModeToggle();
        setupNumericValidation();
        
        // Initially disable all buttons
        if (saveBtn != null) {
            saveBtn.setDisable(true);
        }
        if (deleteBtn != null) {
            deleteBtn.setDisable(true);
        }
        if (clearBtn != null) {
            clearBtn.setDisable(true);
        }
    }
    
    private void setupPaymentModeToggle() {
        // Create toggle group for radio buttons
        paymentModeToggleGroup = new ToggleGroup();
        if (addModeRadio != null) {
            addModeRadio.setToggleGroup(paymentModeToggleGroup);
            addModeRadio.setSelected(true); // Default to "Add More" mode
        }
        if (updateModeRadio != null) {
            updateModeRadio.setToggleGroup(paymentModeToggleGroup);
        }
    }
    
    @FXML
    private void handleModeChange() {
        // Update field label and placeholder based on selected mode
        if (addModeRadio != null && addModeRadio.isSelected()) {
            // Add More mode
            if (amountLabel != null) {
                amountLabel.setText("Amount to Add");
            }
            if (amountPaidField != null) {
                amountPaidField.setPromptText("Enter amount to add");
                // Clear field when switching to Add mode
                amountPaidField.setText("");
            }
        } else if (updateModeRadio != null && updateModeRadio.isSelected()) {
            // Set Total mode
            if (amountLabel != null) {
                amountLabel.setText("Final Total Amount");
            }
            if (amountPaidField != null) {
                amountPaidField.setPromptText("Enter final total amount");
                // Pre-fill with current amount if payment exists
                if (selectedPayment != null && selectedPayment.getAmountPaid() > 0) {
                    amountPaidField.setText(String.format("%.2f", selectedPayment.getAmountPaid()));
                }
            }
        }
        // Update totals display when mode changes
        updateTotalsDisplay();
    }
    
    private void setupSidebarButtons() {
        setupSidebarButtonHover(dashboardBtn, false);
        setupSidebarButtonHover(studentsBtn, false);
        setupSidebarButtonHover(payablesBtn, false);
        setupSidebarButtonHover(paymentsBtn, true);
        setupSidebarButtonHover(schoolYearsBtn, false);
        setupSidebarButtonHover(promissoryNotesBtn, false);
        setupSidebarButtonHover(settingsBtn, false);
        setupSidebarButtonHover(logoutBtn, false);
    }
    
    private void setupTable() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentNumber"));
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        semesterAmountCol.setCellValueFactory(cellData -> {
            PaymentView payment = cellData.getValue();
            String semester = getSelectedSemester();
            double amount = 0.0;
            if ("1st Sem".equals(semester)) {
                amount = payment.getFirstSemAmount();
            } else if ("2nd Sem".equals(semester)) {
                amount = payment.getSecondSemAmount();
            } else if ("Summer Sem".equals(semester)) {
                amount = payment.getSummerSemAmount();
            }
            return new javafx.beans.property.SimpleStringProperty(String.format("P%.2f", amount));
        });
        amountPaidCol.setCellValueFactory(new PropertyValueFactory<>("amountPaidFormatted"));
        dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDateFormatted"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        paymentsTable.setItems(paymentsList);
        updateTableColumnVisibility();
    }
    
    private void updateTableColumnVisibility() {
        String semester = getSelectedSemester();
        if (semesterAmountCol != null) {
            // Always show the payables column
            semesterAmountCol.setVisible(true);
            // Update column header text to show selected semester
            if ("1st Sem".equals(semester)) {
                semesterAmountCol.setText("Payables per Sem (1st Sem)");
            } else if ("2nd Sem".equals(semester)) {
                semesterAmountCol.setText("Payables per Sem (2nd Sem)");
            } else if ("Summer Sem".equals(semester)) {
                semesterAmountCol.setText("Payables per Sem (Summer Sem)");
            } else {
                semesterAmountCol.setText("Payables per Sem");
            }
        }
        // Refresh the table to update cell values
        paymentsTable.refresh();
    }
    
    private void loadSchoolYears() {
        // Get current selected school year
        currentSchoolYearId = utils.SessionManager.getSelectedSchoolYearId();
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
            // Auto-select semester based on current month
            String autoSemester = utils.SemesterUtil.getSemesterByCurrentMonth();
            semesterComboBoxHeader.setValue(autoSemester);
            semesterComboBoxHeader.setOnAction(e -> {
                updateTableColumnVisibility();
                loadPayments();
                // Update payables field if a payment is selected
                if (selectedPayment != null) {
                    populateForm(selectedPayment);
                }
            });
        }
    }
    
    private String getSelectedSemester() {
        if (semesterComboBoxHeader != null) {
            String selected = semesterComboBoxHeader.getValue();
            if (selected == null) {
                return "1st Sem"; // Default to 1st Sem if nothing is selected
            }
            return selected;
        }
        return "1st Sem"; // Default to 1st Sem
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
        
        // Apply TextFormatter to amountPaidField
        if (amountPaidField != null) {
            TextFormatter<String> formatter = new TextFormatter<>(filter);
            amountPaidField.setTextFormatter(formatter);
        }
    }
    
    private void setupInputStyles() {
        // Apply focus styles to all text fields
        TextField[] textFields = {searchField, amountPaidField};
        
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
        
        // Apply focus styles to combo boxes
        @SuppressWarnings("unchecked")
        ComboBox<String>[] comboBoxes = new ComboBox[]{
            yearFilterCombo, majorFilterCombo, yearComboBoxHeader, semesterComboBoxHeader
        };
        
        for (ComboBox<String> combo : comboBoxes) {
            if (combo != null) {
                combo.setCellFactory(listView -> new javafx.scene.control.ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item);
                            setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
                        }
                    }
                });
                
                combo.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (isNowFocused) {
                        combo.setStyle(combo.getStyle() + " -fx-border-color: #7B76F1; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, rgba(123, 118, 241, 0.2), 8, 0, 0, 2);");
                    } else {
                        String baseStyle = combo.getStyle();
                        baseStyle = baseStyle.replaceAll("-fx-border-color: #7B76F1;", "");
                        baseStyle = baseStyle.replaceAll("-fx-border-width: 2;", "");
                        baseStyle = baseStyle.replaceAll("-fx-effect: dropshadow\\(gaussian, rgba\\(123, 118, 241, 0\\.2\\), 8, 0, 0, 2\\);", "");
                        combo.setStyle(baseStyle + " -fx-border-color: #E0E0E0; -fx-border-width: 1.5;");
                    }
                });
            }
        }
    }
    
    @Override
    protected void onSchoolYearChanged() {
        // Reload payments when school year changes
        currentSchoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        loadPayments();
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
        
        if (!yearFilterCombo.getItems().contains("1st Year")) {
            yearFilterCombo.getItems().add("1st Year");
        }
        
        yearFilterCombo.getItems().sort((a, b) -> {
            if (a.equals("All")) return -1;
            if (b.equals("All")) return 1;
            if (a.equals("1st Year")) return -1;
            if (b.equals("1st Year")) return 1;
            return a.compareTo(b);
        });
    }
    
    private void loadPayments() {
        // Always get the latest school year from SessionManager
        Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        currentSchoolYearId = schoolYearId; // Keep in sync
        String semester = getSelectedSemester();
        List<PaymentView> payments = paymentDAO.getAllPaymentViews(schoolYearId, semester);
        
        // Store all payments for pagination
        allPaymentsList.clear();
        allPaymentsList.addAll(payments);
        totalPayments = allPaymentsList.size();
        
        // Reset to first page
        currentPage = 1;
        
        // Apply pagination
        updatePagination();
    }
    
    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText();
        String year = yearFilterCombo.getValue();
        String major = majorFilterCombo.getValue();
        
        // Get the selected school year from SessionManager
        Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        String semester = getSelectedSemester();
        
        if (year != null && year.equals("All")) {
            year = null;
        }
        if (major != null && major.equals("All")) {
            major = null;
        }
        
        // Search with school year filter
        List<Student> students = studentDAO.searchStudents(searchTerm, year, major, schoolYearId);
        List<Integer> studentIds = students.stream()
            .map(Student::getStudentId)
            .toList();
        
        // Filter payments list by student IDs
        // Always get the latest school year from SessionManager
        Integer currentYearId = utils.SessionManager.getSelectedSchoolYearId();
        currentSchoolYearId = currentYearId; // Keep in sync
        allPaymentsList.clear();
        List<PaymentView> allPayments = paymentDAO.getAllPaymentViews(currentYearId, semester);
        for (PaymentView payment : allPayments) {
            if (studentIds.contains(payment.getStudentId())) {
                allPaymentsList.add(payment);
            }
        }
        
        totalPayments = allPaymentsList.size();
        currentPage = 1; // Reset to first page
        updatePagination();
    }
    
    @FXML
    private void handleTableClick() {
        selectedPayment = paymentsTable.getSelectionModel().getSelectedItem();
        if (selectedPayment != null) {
            populateForm(selectedPayment);
            
            // Enable buttons when row is selected
            if (saveBtn != null) {
                saveBtn.setDisable(false);
            }
            if (deleteBtn != null) {
                // Only enable delete if payment exists
                deleteBtn.setDisable(selectedPayment.getAmountPaid() <= 0);
            }
            if (clearBtn != null) {
                clearBtn.setDisable(false);
            }
            
            // Set default mode based on payment status
            double currentAmountPaid = selectedPayment.getAmountPaid();
            if (currentAmountPaid > 0) {
                // Has payment - default to "Add More" mode, but both modes available
                if (addModeRadio != null && updateModeRadio != null) {
                    // User can choose either mode
                    if (!addModeRadio.isSelected() && !updateModeRadio.isSelected()) {
                        addModeRadio.setSelected(true); // Default to Add More
                    }
                }
            } else {
                // No payment - only "Add More" mode makes sense
                if (addModeRadio != null) {
                    addModeRadio.setSelected(true);
                }
                if (updateModeRadio != null) {
                    updateModeRadio.setDisable(true); // Disable Set Total when no payment exists
                }
            }
            
            // Update mode UI
            handleModeChange();
        } else {
            // No row selected → All buttons disabled
            if (saveBtn != null) {
                saveBtn.setDisable(true);
            }
            if (deleteBtn != null) {
                deleteBtn.setDisable(true);
            }
            if (clearBtn != null) {
                clearBtn.setDisable(true);
            }
            // Re-enable update mode radio when no selection
            if (updateModeRadio != null) {
                updateModeRadio.setDisable(false);
            }
        }
    }
    
    private void populateForm(PaymentView payment) {
        // Get full student information
        Student student = studentDAO.getStudentById(payment.getStudentId());
        
        if (student != null) {
            studentIdField.setText(student.getStudentNumber());
            fullnameField.setText(student.getFullname());
            majorField.setText(student.getMajor() != null ? student.getMajor() : "");
            yearField.setText(student.getYear() != null ? student.getYear() : "");
        }
        
        // Populate payables based on selected semester
        String semester = getSelectedSemester();
        double payablesAmount = 0.0;
        if ("1st Sem".equals(semester)) {
            payablesAmount = payment.getFirstSemAmount();
        } else if ("2nd Sem".equals(semester)) {
            payablesAmount = payment.getSecondSemAmount();
        } else if ("Summer Sem".equals(semester)) {
            payablesAmount = payment.getSummerSemAmount();
        }
        payablesField.setText(String.format("P%.2f", payablesAmount));
        
        // Store current totals for calculations
        currentTotalAmount = payment.getTotalAmount();
        currentAmountPaid = payment.getAmountPaid();
        
        
        // Amount paid field will be set by handleModeChange() based on selected mode
        // Don't pre-fill here - let the mode handler manage it
        
        // Show total paid (current amount paid)
        totalPaidField.setText(String.format("P%.2f", currentAmountPaid));
        
        // Calculate and show status based on total paid vs total of all semesters
        String calculatedStatus;
        if (Math.abs(currentAmountPaid - currentTotalAmount) < 0.01 || currentAmountPaid >= currentTotalAmount) {
            calculatedStatus = "Paid";
        } else if (currentAmountPaid > 0) {
            calculatedStatus = "Partial";
        } else {
            calculatedStatus = "UNPAID";
        }
        if (statusField != null) {
            statusField.setText(calculatedStatus);
        }
        
        // Set due date - if already paid, show "Paid", otherwise show the due date
        if ("Paid".equals(calculatedStatus)) {
            dueDateField.setText("Paid");
            // Still allow editing even if paid (user can update using Update button)
            amountPaidField.setEditable(true);
        } else {
            if (payment.getDueDate() != null) {
                dueDateField.setText(payment.getDueDateFormatted());
            } else {
                dueDateField.setText("");
            }
            amountPaidField.setEditable(true);
        }
        
        // Show total needed to pay (remaining balance)
        double totalNeeded = currentTotalAmount - currentAmountPaid;
        if (totalNeededField != null) {
            totalNeededField.setText(String.format("P%.2f", totalNeeded > 0 ? totalNeeded : 0));
        }
        // Update balance to pay field
        if (balanceToPayField != null) {
            balanceToPayField.setText(String.format("P%.2f", totalNeeded > 0 ? totalNeeded : 0));
        }
        
        // Due date is now auto-generated, no need to display it in the form
    }
    
    // Down payment is auto-calculated and read-only, so no change handler needed
    
    private void updateTotalsDisplay() {
        // Calculate total paid based on selected mode
        String amountText = amountPaidField.getText().trim();
        double totalPaid = currentAmountPaid;
        
        if (!amountText.isEmpty()) {
            try {
                double enteredAmount = parseAmount(amountText);
                
                // Determine calculation based on selected radio button
                boolean isAddMode = (addModeRadio != null && addModeRadio.isSelected());
                
                if (isAddMode) {
                    // ADD mode: entered amount is added to current amount
                    totalPaid = currentAmountPaid + enteredAmount;
                } else {
                    // UPDATE mode: entered amount is the final total
                    totalPaid = enteredAmount;
                }
            } catch (NumberFormatException e) {
                // Invalid amount, use current
                totalPaid = currentAmountPaid;
            }
        }
        
        // Update total paid display
        totalPaidField.setText(String.format("P%.2f", totalPaid));
        
        // Update total needed to pay (remaining balance)
        double totalNeeded = currentTotalAmount - totalPaid;
        if (totalNeededField != null) {
            totalNeededField.setText(String.format("P%.2f", totalNeeded > 0 ? totalNeeded : 0));
        }
        // Update balance to pay field
        if (balanceToPayField != null) {
            balanceToPayField.setText(String.format("P%.2f", totalNeeded > 0 ? totalNeeded : 0));
        }
    }
    
    @FXML
    private void handleAmountPaidChange() {
        // When amount paid is entered, recalculate status and update totals
        String amountText = amountPaidField.getText().trim();
        if (!amountText.isEmpty()) {
            try {
                double enteredAmount = parseAmount(amountText);
                double totalAmountPaid;
                
                // Determine calculation based on selected radio button
                boolean isAddMode = (addModeRadio != null && addModeRadio.isSelected());
                
                if (isAddMode) {
                    // ADD mode: entered amount is added to current amount
                    totalAmountPaid = currentAmountPaid + enteredAmount;
                } else {
                    // UPDATE mode: entered amount is the final total
                    totalAmountPaid = enteredAmount;
                }
                
                // Update totals display
                updateTotalsDisplay();
                
                // Auto-calculate status based on total amount
                calculateStatus(totalAmountPaid);
            } catch (NumberFormatException e) {
                // Invalid amount, don't update
            }
        } else {
            // If amount field is cleared, recalculate with existing amount paid
            updateTotalsDisplay();
            calculateStatus(currentAmountPaid);
        }
    }
    
    private void calculateStatus(double totalAmountPaid) {
        // Status logic: Paid if total paid equals total of all semesters (1st sem + 2nd sem + summer sem)
        // Use small tolerance (0.01) for floating point comparison
        if (statusField != null) {
            if (Math.abs(totalAmountPaid - currentTotalAmount) < 0.01 || totalAmountPaid >= currentTotalAmount) {
                statusField.setText("Paid");
            } else if (totalAmountPaid > 0) {
                statusField.setText("Partial");
            } else {
                statusField.setText("UNPAID");
            }
        }
    }
    
    private double parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        // Remove "P" prefix and commas if present
        String cleaned = text.replace("P", "").replace(",", "").replace("₱", "").trim();
        if (cleaned.isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(cleaned);
    }
    
    private LocalDate parseDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return null;
        }
        
        // Try MM/DD/YYYY format
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            return LocalDate.parse(dateText, formatter);
        } catch (DateTimeParseException e) {
            // Try other formats
            try {
                return LocalDate.parse(dateText);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
    
    @FXML
    private void handleSave() {
        // Unified handler for both Add and Update modes
        if (selectedPayment == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a student from the table!");
            return;
        }
        
        // Determine mode based on radio button selection
        boolean isAddMode = (addModeRadio != null && addModeRadio.isSelected());
        
        // Validate that amount is entered
        String amountPaidText = amountPaidField.getText().trim();
        if (amountPaidText.isEmpty()) {
            String message = isAddMode ? "Please enter an amount to add!" : "Please enter the final total amount!";
            showAlert(Alert.AlertType.WARNING, "Validation Error", message);
            return;
        }
        
        Student student = getStudentFromForm();
        if (student == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Student not found!");
            return;
        }
        
        // Always get the latest school year from SessionManager
        Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        currentSchoolYearId = schoolYearId;
        
        // Fetch existing payment record to get current amounts and total
        PaymentView existingPayment = paymentDAO.getPaymentView(student.getStudentId(), schoolYearId);
        if (existingPayment == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not retrieve payment information for this student!");
            return;
        }
        
        // Use existing payment data
        currentAmountPaid = existingPayment.getAmountPaid();
        currentTotalAmount = existingPayment.getTotalAmount();
        
        // Validate that student has payables defined
        if (currentTotalAmount <= 0) {
            showAlert(Alert.AlertType.WARNING, "Warning", "This student has no payables defined. Please set up payables first.");
            return;
        }
        
        // Parse entered amount
        double enteredAmount = parseAmount(amountPaidText);
        
        if (enteredAmount <= 0) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Amount must be greater than 0!");
            return;
        }
        
        // Calculate final amount based on mode
        double finalAmountPaid;
        if (isAddMode) {
            // ADD mode: Add entered amount to current amount
            finalAmountPaid = currentAmountPaid + enteredAmount;
        } else {
            // UPDATE mode: Use entered amount as final total
            finalAmountPaid = enteredAmount;
        }
        
        // Validate that payment does not exceed payables (strict check - no tolerance for exceeding)
        // Use small tolerance (0.001) to account for floating point precision, but still prevent any meaningful excess
        if (finalAmountPaid > currentTotalAmount + 0.001) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", 
                String.format("Amount paid (P%.2f) cannot exceed total payables (P%.2f)!", 
                    finalAmountPaid, currentTotalAmount));
            return;
        }
        
        // Cap at exact payables amount if it slightly exceeds due to floating point precision
        if (finalAmountPaid > currentTotalAmount) {
            finalAmountPaid = currentTotalAmount;
        }
        
        // Auto-generate due date (2 months from today)
        LocalDate dueDate = LocalDate.now().plusMonths(2);
        
        // Recalculate status based on final amount paid vs total of all semesters
        String status;
        if (Math.abs(finalAmountPaid - currentTotalAmount) < 0.01 || finalAmountPaid >= currentTotalAmount) {
            status = "Paid";
            dueDate = null; // No due date needed if fully paid
        } else if (finalAmountPaid > 0) {
            status = "Partial";
        } else {
            status = "UNPAID";
        }
        
        if (paymentDAO.savePayment(student.getStudentId(), schoolYearId, 0.0, finalAmountPaid, dueDate, status)) {
            String successMessage = isAddMode ? 
                String.format("Added P%.2f to payment. New total: P%.2f", enteredAmount, finalAmountPaid) :
                String.format("Payment updated successfully! New total: P%.2f", finalAmountPaid);
            showAlert(Alert.AlertType.INFORMATION, "Success", successMessage);
            clearForm();
            selectedPayment = null;
            paymentsTable.getSelectionModel().clearSelection();
            // Disable all buttons after saving (no row selected)
            if (saveBtn != null) {
                saveBtn.setDisable(true);
            }
            if (deleteBtn != null) {
                deleteBtn.setDisable(true);
            }
            if (clearBtn != null) {
                clearBtn.setDisable(true);
            }
            currentPage = 1; // Reset to first page
            loadPayments();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save payment!");
        }
    }
    
    @FXML
    private void handleAdd() {
        // Validate that a row is selected (ADD only works when row is selected and amount_paid = 0)
        if (selectedPayment == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a student from the table!");
            return;
        }
        
        // Validate that amount is entered
        String amountPaidText = amountPaidField.getText().trim();
        if (amountPaidText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter an amount to add!");
            return;
        }
        
        Student student = getStudentFromForm();
        if (student == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Student not found!");
            return;
        }
        
        // Always get the latest school year from SessionManager
        Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        currentSchoolYearId = schoolYearId; // Keep in sync
        
        // Fetch existing payment record to get current amounts and total
        PaymentView existingPayment = paymentDAO.getPaymentView(student.getStudentId(), schoolYearId);
        if (existingPayment == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not retrieve payment information for this student!");
            return;
        }
        
        // Use existing payment data - this includes the sum of all semester amounts
        currentAmountPaid = existingPayment.getAmountPaid(); // Current amount (can be > 0 for incremental ADD)
        currentTotalAmount = existingPayment.getTotalAmount(); // Total of all semesters (1st + 2nd + summer)
        
        // Validate that student has payables defined
        if (currentTotalAmount <= 0) {
            showAlert(Alert.AlertType.WARNING, "Warning", "This student has no payables defined. Please set up payables first.");
            return;
        }
        
        // Get new payment amount to ADD (incremental)
        double amountToAdd = parseAmount(amountPaidText);
        
        if (amountToAdd <= 0) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Amount must be greater than 0!");
            return;
        }
        
        // Calculate total amount paid (ADD: Current + Amount to Add)
        // This works for both first payment (currentAmountPaid = 0) and incremental additions (currentAmountPaid > 0)
        double totalAmountPaid = currentAmountPaid + amountToAdd;
        
        // Validate that payment does not exceed payables (strict check - no tolerance for exceeding)
        // Use small tolerance (0.001) to account for floating point precision, but still prevent any meaningful excess
        if (totalAmountPaid > currentTotalAmount + 0.001) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", 
                String.format("Amount paid (P%.2f) cannot exceed total payables (P%.2f)!", 
                    totalAmountPaid, currentTotalAmount));
            return;
        }
        
        // Cap at exact payables amount if it slightly exceeds due to floating point precision
        if (totalAmountPaid > currentTotalAmount) {
            totalAmountPaid = currentTotalAmount;
        }
        
        // Auto-generate due date (2 months from today) - no user input needed
        LocalDate dueDate = LocalDate.now().plusMonths(2);
        
        // Recalculate status based on total amount paid vs total of all semesters
        String status;
        if (Math.abs(totalAmountPaid - currentTotalAmount) < 0.01 || totalAmountPaid >= currentTotalAmount) {
            status = "Paid";
            // If paid, set due date to null (no due date needed)
            dueDate = null;
        } else if (totalAmountPaid > 0) {
            status = "Partial";
        } else {
            status = "UNPAID";
        }
        
        if (paymentDAO.savePayment(student.getStudentId(), schoolYearId, 0.0, totalAmountPaid, dueDate, status)) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Payment added successfully!");
            clearForm();
            selectedPayment = null;
            paymentsTable.getSelectionModel().clearSelection();
            // Disable all buttons after adding (no row selected)
            if (saveBtn != null) {
                saveBtn.setDisable(true);
            }
            if (deleteBtn != null) {
                deleteBtn.setDisable(true);
            }
            if (clearBtn != null) {
                clearBtn.setDisable(true);
            }
            currentPage = 1; // Reset to first page
            loadPayments();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add payment!");
        }
    }
    
    @FXML
    private void handleUpdate() {
        if (selectedPayment == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a payment from the table to update!");
            return;
        }
        
        // Validate that amount_paid > 0 (UPDATE only works when payment exists)
        if (selectedPayment.getAmountPaid() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Warning", "No payment to update. Use Add to create a new payment.");
            return;
        }
        
        // Validate that amount is entered
        String amountPaidText = amountPaidField.getText().trim();
        if (amountPaidText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter the final amount paid!");
            return;
        }
        
        // If field contains current amount, that's fine (UPDATE with same value)
        // If field contains a different value, treat it as the new final total
        
        // Get the final amount paid (UPDATE: user enters final total, not increment)
        double finalAmountPaid = parseAmount(amountPaidText);
        
        if (finalAmountPaid < 0) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Amount cannot be negative!");
            return;
        }
        
        // Validate that payment does not exceed payables (strict check - no tolerance for exceeding)
        double previousAmountPaid = selectedPayment.getAmountPaid();
        // Use small tolerance (0.001) to account for floating point precision, but still prevent any meaningful excess
        if (finalAmountPaid > currentTotalAmount + 0.001) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", 
                String.format("Amount paid (P%.2f) cannot exceed total payables (P%.2f)!", 
                    finalAmountPaid, currentTotalAmount));
            return;
        }
        
        // Cap at exact payables amount if it slightly exceeds due to floating point precision
        if (finalAmountPaid > currentTotalAmount) {
            finalAmountPaid = currentTotalAmount;
        }
        
        // Auto-generate due date (2 months from today) - no user input needed
        LocalDate dueDate = LocalDate.now().plusMonths(2);
        
        // Recalculate status based on final amount paid vs total of all semesters
        String status;
        if (Math.abs(finalAmountPaid - currentTotalAmount) < 0.01 || finalAmountPaid >= currentTotalAmount) {
            status = "Paid";
            // If paid, set due date to null (no due date needed)
            dueDate = null;
        } else if (finalAmountPaid > 0) {
            status = "Partial";
        } else {
            status = "UNPAID";
        }
        
        // Always get the latest school year from SessionManager
        Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
        currentSchoolYearId = schoolYearId; // Keep in sync
        
        if (paymentDAO.savePayment(selectedPayment.getStudentId(), schoolYearId, 0.0, finalAmountPaid, dueDate, status)) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Payment updated successfully!");
            clearForm();
            selectedPayment = null;
            paymentsTable.getSelectionModel().clearSelection();
            // Disable all buttons after updating (no row selected)
            if (saveBtn != null) {
                saveBtn.setDisable(true);
            }
            if (deleteBtn != null) {
                deleteBtn.setDisable(true);
            }
            if (clearBtn != null) {
                clearBtn.setDisable(true);
            }
            currentPage = 1; // Reset to first page
            loadPayments();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update payment!");
        }
    }
    
    @FXML
    private void handleDelete() {
        if (selectedPayment == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a payment from the table to delete!");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Payment");
        confirmAlert.setContentText("Are you sure you want to delete payment for " + selectedPayment.getStudentName() + "?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                // Always get the latest school year from SessionManager
                Integer schoolYearId = utils.SessionManager.getSelectedSchoolYearId();
                currentSchoolYearId = schoolYearId; // Keep in sync
                
                if (paymentDAO.deletePayment(selectedPayment.getStudentId(), schoolYearId)) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Payment deleted successfully!");
                    clearForm();
                    selectedPayment = null;
                    paymentsTable.getSelectionModel().clearSelection();
                    // Disable all buttons after deleting (no row selected)
                    if (saveBtn != null) {
                        saveBtn.setDisable(true);
                    }
                    if (deleteBtn != null) {
                        deleteBtn.setDisable(true);
                    }
                    if (clearBtn != null) {
                        clearBtn.setDisable(true);
                    }
                    // Re-enable update mode radio when cleared
                    if (updateModeRadio != null) {
                        updateModeRadio.setDisable(false);
                    }
                    currentPage = 1; // Reset to first page
                    loadPayments();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete payment!");
                }
            }
        });
    }
    
    @FXML
    private void handleClear() {
        clearForm();
        selectedPayment = null;
        paymentsTable.getSelectionModel().clearSelection();
        // Disable all buttons when form is cleared (no row selected)
        if (saveBtn != null) {
            saveBtn.setDisable(true);
        }
        if (deleteBtn != null) {
            deleteBtn.setDisable(true);
        }
        if (clearBtn != null) {
            clearBtn.setDisable(true);
        }
        // Re-enable update mode radio when cleared
        if (updateModeRadio != null) {
            updateModeRadio.setDisable(false);
        }
    }
    
    private Student getStudentFromForm() {
        String studentNumber = studentIdField.getText().trim();
        if (studentNumber.isEmpty()) {
            return null;
        }
        return studentDAO.getStudentByNumber(studentNumber);
    }
    
    private boolean validatePaymentForm() {
        if (studentIdField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a student from the table!");
            return false;
        }
        
        // Amount paid validation
        String amountPaidText = amountPaidField.getText().trim();
        if (!amountPaidText.isEmpty()) {
            try {
                double paymentAmount = parseAmount(amountPaidText);
                if (paymentAmount < 0) {
                    showAlert(Alert.AlertType.WARNING, "Validation Error", "Amount cannot be negative!");
                    return false;
                }
                
                // Check if total amount paid would exceed total (strict check - no tolerance for exceeding)
                double totalAmountPaid = currentAmountPaid + paymentAmount;
                // Use small tolerance (0.001) to account for floating point precision, but still prevent any meaningful excess
                if (totalAmountPaid > currentTotalAmount + 0.001) {
                    showAlert(Alert.AlertType.WARNING, "Validation Error", 
                        String.format("Amount paid (P%.2f) cannot exceed total payables (P%.2f)!", 
                            totalAmountPaid, currentTotalAmount));
                    return false;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a valid amount!");
                return false;
            }
        }
        
        return true;
    }
    
    private void clearForm() {
        studentIdField.clear();
        fullnameField.clear();
        majorField.clear();
        yearField.clear();
        payablesField.clear();
        amountPaidField.clear();
        totalPaidField.clear();
        dueDateField.clear();
        if (totalNeededField != null) {
            totalNeededField.clear();
        }
        if (balanceToPayField != null) {
            balanceToPayField.clear();
        }
        if (statusField != null) {
            statusField.clear();
        }
        currentTotalAmount = 0;
        currentAmountPaid = 0;
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
        navigateToPage("payables.fxml", "DorPay - Payables", payablesBtn);
    }
    
    @FXML
    private void handlePayments() {
        // Already on payments page
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
    
    private void setupPagination() {
        if (prevBtn != null && nextBtn != null && pageInfoLabel != null) {
            updatePaginationButtons();
        }
    }
    
    private void updatePagination() {
        // Calculate pagination
        int totalPages = (int) Math.ceil((double) totalPayments / PAGE_SIZE);
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
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalPayments);
        
        // Get payments for current page
        paymentsList.clear();
        for (int i = startIndex; i < endIndex; i++) {
            if (i < allPaymentsList.size()) {
                paymentsList.add(allPaymentsList.get(i));
            }
        }
        
        // Update page info label
        if (pageInfoLabel != null) {
            int totalPages2 = (int) Math.ceil((double) totalPayments / PAGE_SIZE);
            if (totalPages2 == 0) totalPages2 = 1;
            pageInfoLabel.setText("Page " + currentPage + " of " + totalPages2);
        }
        
        // Update button states
        updatePaginationButtons();
    }
    
    private void updatePaginationButtons() {
        if (prevBtn == null || nextBtn == null) return;
        
        int totalPages = (int) Math.ceil((double) totalPayments / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        
        // Disable Previous button on first page
        prevBtn.setDisable(currentPage <= 1);
        
        // Disable Next button on last page
        nextBtn.setDisable(currentPage >= totalPages);
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
        int totalPages = (int) Math.ceil((double) totalPayments / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }
}
