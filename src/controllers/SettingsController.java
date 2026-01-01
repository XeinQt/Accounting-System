package controllers;

import dao.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Admin;
import utils.PasswordUtil;
import utils.SessionManager;

public class SettingsController extends BaseController {
    
    @FXML private TextField usernameField;
    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    
    @FXML private Button updateProfileBtn;
    @FXML private Button changePasswordBtn;
    @FXML private Button clearPasswordBtn;
    
    @FXML private Label passwordErrorLabel;
    
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
    
    private UserDAO userDAO;
    private Admin currentAdmin;
    
    @FXML
    public void initialize() {
        userDAO = new UserDAO();
        setupSidebarButtons();
        setupInputStyles();
        loadCurrentAdmin();
        populateProfileFields();
    }
    
    private void setupSidebarButtons() {
        setupSidebarButtonHover(dashboardBtn, false);
        setupSidebarButtonHover(studentsBtn, false);
        setupSidebarButtonHover(payablesBtn, false);
        setupSidebarButtonHover(paymentsBtn, false);
        setupSidebarButtonHover(schoolYearsBtn, false);
        setupSidebarButtonHover(promissoryNotesBtn, false);
        setupSidebarButtonHover(settingsBtn, true);
        setupSidebarButtonHover(logoutBtn, false);
    }
    
    private void setupInputStyles() {
        // Apply focus styles to all text fields
        TextField[] textFields = {usernameField, fullnameField, emailField};
        
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
        
        // Apply focus styles to all password fields
        PasswordField[] passwordFields = {currentPasswordField, newPasswordField, confirmPasswordField};
        
        for (PasswordField field : passwordFields) {
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
    }
    
    private void loadCurrentAdmin() {
        // Get current admin from session or use default
        // For now, we'll get it from the database using the logged-in username
        // In a real app, you'd store this in a session
        String currentUsername = SessionManager.getCurrentUsername();
        if (currentUsername != null) {
            currentAdmin = userDAO.getAdminByUsername(currentUsername);
        }
        
        // If no session, try to get default admin
        if (currentAdmin == null) {
            currentAdmin = userDAO.getAdminByUsername("admin");
        }
    }
    
    private void populateProfileFields() {
        if (currentAdmin != null) {
            usernameField.setText(currentAdmin.getUsername());
            fullnameField.setText(currentAdmin.getFullname());
            emailField.setText(currentAdmin.getEmail());
        }
    }
    
    @FXML
    private void handleUpdateProfile() {
        if (currentAdmin == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No admin account found!");
            return;
        }
        
        if (validateProfileForm()) {
            String newFullname = fullnameField.getText().trim();
            String newEmail = emailField.getText().trim();
            
            currentAdmin.setFullname(newFullname);
            currentAdmin.setEmail(newEmail);
            
            if (userDAO.updateAdmin(currentAdmin)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully!");
                loadCurrentAdmin();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update profile!");
            }
        }
    }
    
    @FXML
    private void handleChangePassword() {
        if (currentAdmin == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No admin account found!");
            return;
        }
        
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Validate current password
        Admin verifyAdmin = userDAO.authenticate(currentAdmin.getUsername(), currentPassword);
        if (verifyAdmin == null) {
            showPasswordError("Current password is incorrect!");
            return;
        }
        
        // Validate new password
        if (newPassword.isEmpty() || newPassword.length() < 4) {
            showPasswordError("New password must be at least 4 characters!");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showPasswordError("New passwords do not match!");
            return;
        }
        
        // Hash and update password
        String hashedPassword = PasswordUtil.hashPassword(newPassword);
        currentAdmin.setPasswordHash(hashedPassword);
        if (userDAO.updateAdmin(currentAdmin)) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Password changed successfully!");
            clearPasswordFields();
            hidePasswordError();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to change password!");
        }
    }
    
    @FXML
    private void handleClearPassword() {
        clearPasswordFields();
        hidePasswordError();
    }
    
    private boolean validateProfileForm() {
        if (fullnameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Full name is required!");
            return false;
        }
        if (emailField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Email is required!");
            return false;
        }
        // Basic email validation
        String email = emailField.getText().trim();
        if (!email.contains("@") || !email.contains(".")) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a valid email address!");
            return false;
        }
        return true;
    }
    
    private void showPasswordError(String message) {
        passwordErrorLabel.setText(message);
        passwordErrorLabel.setVisible(true);
    }
    
    private void hidePasswordError() {
        passwordErrorLabel.setVisible(false);
        passwordErrorLabel.setText("");
    }
    
    private void clearPasswordFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
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
        // Already on settings page
    }
    
    @FXML
    private void handleLogout() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Logout");
        confirmAlert.setHeaderText("Logout");
        confirmAlert.setContentText("Are you sure you want to logout?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                SessionManager.clearSession();
                navigateToPage("login.fxml", "DorPay - Login", logoutBtn);
            }
        });
    }
}

