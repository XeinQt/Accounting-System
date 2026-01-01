package controllers;

import dao.SchoolYearDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.SchoolYear;

import java.util.List;

public class SchoolYearsController extends BaseController {
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ListView<String> yearsListView;
    @FXML private Text totalYearsLabel;
    
    @FXML private TextField fromYearField;
    @FXML private TextField toYearField;
    
    @FXML private Button addBtn;
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
    
    private SchoolYearDAO schoolYearDAO;
    private ObservableList<String> yearsList;
    private SchoolYear selectedSchoolYear;
    
    @FXML
    public void initialize() {
        schoolYearDAO = new SchoolYearDAO();
        yearsList = FXCollections.observableArrayList();
        
        setupSidebarButtons();
        setupStatusFilter();
        loadSchoolYears();
        setupEventHandlers();
        setupButtonStates();
    }
    
    private void setupStatusFilter() {
        if (statusFilterCombo != null) {
            statusFilterCombo.getItems().addAll("All", "Active", "Deactivated");
            statusFilterCombo.setValue("All");
        }
    }
    
    private void setupButtonStates() {
        // Initial state: Add enabled, others disabled
        addBtn.setDisable(false);
        updateBtn.setDisable(true);
        deleteBtn.setDisable(true);
        reactivateBtn.setDisable(true);
        clearBtn.setDisable(false);
    }
    
    private void setupSidebarButtons() {
        setupSidebarButtonHover(dashboardBtn, false);
        setupSidebarButtonHover(studentsBtn, false);
        setupSidebarButtonHover(payablesBtn, false);
        setupSidebarButtonHover(paymentsBtn, false);
        setupSidebarButtonHover(schoolYearsBtn, true);
        setupSidebarButtonHover(promissoryNotesBtn, false);
        setupSidebarButtonHover(settingsBtn, false);
        setupSidebarButtonHover(logoutBtn, false);
    }
    
    private void loadSchoolYears() {
        filterSchoolYears();
    }
    
    private void setupEventHandlers() {
        // Search functionality can be added here if needed
    }
    
    @FXML
    private void handleSearch() {
        filterSchoolYears();
    }
    
    @FXML
    private void handleStatusFilter() {
        filterSchoolYears();
    }
    
    @FXML
    private void handleYearFieldChange() {
        checkYearExists();
    }
    
    private void checkYearExists() {
        String fromYear = fromYearField.getText().trim();
        String toYear = toYearField.getText().trim();
        
        if (fromYear.isEmpty() || toYear.isEmpty()) {
            if (selectedSchoolYear != null) {
                // If row is selected, keep that state
                addBtn.setDisable(true);
                updateBtn.setDisable(false);
                // Deactivate button: enabled only if school year is active AND not in use by students
                boolean canDeactivate = selectedSchoolYear.isActive() && 
                                       !schoolYearDAO.hasStudents(selectedSchoolYear.getSchoolYearId());
                deleteBtn.setDisable(!canDeactivate);
                // Reactivate button: enabled only if school year is deactivated
                reactivateBtn.setDisable(selectedSchoolYear.isActive());
            } else {
                setupButtonStates();
            }
            return;
        }
        
        String yearRange = fromYear + "-" + toYear;
        SchoolYear existing = schoolYearDAO.getSchoolYearByRange(yearRange);
        
        if (existing != null && (selectedSchoolYear == null || existing.getSchoolYearId() != selectedSchoolYear.getSchoolYearId())) {
            // Year already exists and it's not the currently selected one
            addBtn.setDisable(true);
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
            reactivateBtn.setDisable(true);
        } else {
            // Year doesn't exist or it's the selected one
            if (selectedSchoolYear != null) {
                // Row is selected
                addBtn.setDisable(true);
                updateBtn.setDisable(false);
                // Deactivate button: enabled only if school year is active AND not in use by students
                boolean canDeactivate = selectedSchoolYear.isActive() && 
                                       !schoolYearDAO.hasStudents(selectedSchoolYear.getSchoolYearId());
                deleteBtn.setDisable(!canDeactivate);
                // Reactivate button: enabled only if school year is deactivated
                reactivateBtn.setDisable(selectedSchoolYear.isActive());
            } else {
                // No row selected, year doesn't exist
                addBtn.setDisable(false);
                updateBtn.setDisable(true);
                deleteBtn.setDisable(true);
                reactivateBtn.setDisable(true);
            }
        }
    }
    
    private void filterSchoolYears() {
        String searchText = searchField.getText().toLowerCase().trim();
        String statusFilter = statusFilterCombo != null ? statusFilterCombo.getValue() : "All";
        if (statusFilter == null) statusFilter = "All";
        
        List<SchoolYear> allSchoolYears = schoolYearDAO.getAllSchoolYears();
        yearsList.clear();
        
        for (SchoolYear sy : allSchoolYears) {
            String yearRange = sy.getYearRange().toLowerCase();
            boolean matchesSearch = searchText.isEmpty() || yearRange.contains(searchText);
            
            // For now, we'll treat all as active since we don't have status field yet
            // This will be updated when we add the status field to the database
            boolean matchesStatus = statusFilter.equals("All") || 
                                   (statusFilter.equals("Active") && sy.isActive()) ||
                                   (statusFilter.equals("Deactivated") && !sy.isActive());
            
            if (matchesSearch && matchesStatus) {
                yearsList.add(sy.getYearRange());
            }
        }
        
        yearsListView.setItems(yearsList);
        
        if (totalYearsLabel != null) {
            totalYearsLabel.setText("(Total: " + yearsList.size() + ")");
        }
    }
    
    @FXML
    private void handleListClick() {
        String selected = yearsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedSchoolYear = schoolYearDAO.getSchoolYearByRange(selected);
            if (selectedSchoolYear != null) {
                populateForm(selectedSchoolYear);
                // When row is selected: Add disabled, others enabled based on status
                addBtn.setDisable(true);
                updateBtn.setDisable(false);
                // Deactivate button: enabled only if school year is active AND not in use by students
                boolean canDeactivate = selectedSchoolYear.isActive() && 
                                       !schoolYearDAO.hasStudents(selectedSchoolYear.getSchoolYearId());
                deleteBtn.setDisable(!canDeactivate);
                // Reactivate button: enabled only if school year is deactivated
                reactivateBtn.setDisable(selectedSchoolYear.isActive());
            }
        }
    }
    
    private void populateForm(SchoolYear schoolYear) {
        String[] years = schoolYear.getYearRange().split("-");
        if (years.length == 2) {
            fromYearField.setText(years[0].trim());
            toYearField.setText(years[1].trim());
        }
    }
    
    @FXML
    private void handleAdd() {
        if (validateForm()) {
            String yearRange = fromYearField.getText().trim() + "-" + toYearField.getText().trim();
            
            // Check if already exists
            if (schoolYearDAO.getSchoolYearByRange(yearRange) != null) {
                showAlert(Alert.AlertType.ERROR, "Error", "School year already exists!");
                return;
            }
            
            SchoolYear schoolYear = new SchoolYear(yearRange);
            schoolYear.setActive(true); // New school years are active by default
            if (schoolYearDAO.addSchoolYear(schoolYear)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "School year added successfully!");
                clearForm();
                loadSchoolYears();
                setupButtonStates();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add school year!");
            }
        }
    }
    
    @FXML
    private void handleUpdate() {
        if (selectedSchoolYear == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a school year to update!");
            return;
        }
        
        if (validateForm()) {
            String yearRange = fromYearField.getText().trim() + "-" + toYearField.getText().trim();
            selectedSchoolYear.setYearRange(yearRange);
            
            if (schoolYearDAO.updateSchoolYear(selectedSchoolYear)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "School year updated successfully!");
                clearForm();
                loadSchoolYears();
                selectedSchoolYear = null;
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update school year!");
            }
        }
    }
    
    @FXML
    private void handleDelete() {
        if (selectedSchoolYear == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a school year to deactivate!");
            return;
        }
        
        // Check if school year is being used by students
        if (schoolYearDAO.hasStudents(selectedSchoolYear.getSchoolYearId())) {
            showLargeAlert(Alert.AlertType.ERROR, "Cannot Deactivate", 
                "School Year is in Use",
                "Cannot deactivate " + selectedSchoolYear.getYearRange() + " because it is currently being used by students.\n\n" +
                "Please remove or reassign all students from this school year before deactivating it.");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deactivate");
        confirmAlert.setHeaderText("Deactivate School Year");
        confirmAlert.setContentText("Are you sure you want to deactivate " + selectedSchoolYear.getYearRange() + "?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                selectedSchoolYear.setActive(false);
                if (schoolYearDAO.updateSchoolYear(selectedSchoolYear)) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "School year deactivated successfully!");
                    clearForm();
                    loadSchoolYears();
                    selectedSchoolYear = null;
                    setupButtonStates();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to deactivate school year!");
                }
            }
        });
    }
    
    @FXML
    private void handleReactivate() {
        if (selectedSchoolYear == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a deactivated school year to reactivate!");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Reactivate");
        confirmAlert.setHeaderText("Reactivate School Year");
        confirmAlert.setContentText("Are you sure you want to reactivate " + selectedSchoolYear.getYearRange() + "?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                selectedSchoolYear.setActive(true);
                if (schoolYearDAO.updateSchoolYear(selectedSchoolYear)) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "School year reactivated successfully!");
                    clearForm();
                    loadSchoolYears();
                    selectedSchoolYear = null;
                    setupButtonStates();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to reactivate school year!");
                }
            }
        });
    }
    
    @FXML
    private void handleClear() {
        clearForm();
        selectedSchoolYear = null;
        yearsListView.getSelectionModel().clearSelection();
        setupButtonStates();
    }
    
    private boolean validateForm() {
        if (fromYearField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "From year is required!");
            return false;
        }
        if (toYearField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "To year is required!");
            return false;
        }
        try {
            int fromYear = Integer.parseInt(fromYearField.getText().trim());
            int toYear = Integer.parseInt(toYearField.getText().trim());
            if (toYear <= fromYear) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "To year must be greater than from year!");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter valid year numbers!");
            return false;
        }
        return true;
    }
    
    private void clearForm() {
        fromYearField.clear();
        toYearField.clear();
    }
    
    // Navigation methods (inherited from BaseController)
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
    private void handleNotifications() {
        navigateToPage("notifications.fxml", "DorPay - Notifications", notificationsBtn);
    }
    
    @FXML
    private void handleSchoolYears() {
        // Already on school years page
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

