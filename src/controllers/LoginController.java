package controllers;

import dao.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Admin;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private TextField passwordVisibleField;
    
    @FXML
    private Button togglePasswordBtn;
    
    @FXML
    private CheckBox rememberMeCheckBox;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Label errorLabel;
    
    private UserDAO userDAO;
    private boolean passwordVisible = false;
    
    @FXML
    public void initialize() {
        userDAO = new UserDAO();
        errorLabel.setVisible(false);
        
        // Sync password fields
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordVisible) {
                passwordVisibleField.setText(newVal);
            }
        });
        
        passwordVisibleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (passwordVisible) {
                passwordField.setText(newVal);
            }
        });
        
        // Add Enter key handlers to trigger login
        usernameField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        passwordVisibleField.setOnAction(e -> handleLogin());
    }
    
    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        
        if (passwordVisible) {
            // Show visible text field
            passwordVisibleField.setText(passwordField.getText());
            passwordVisibleField.setVisible(true);
            passwordField.setVisible(false);
            togglePasswordBtn.setText("👁️");
        } else {
            // Show password field
            passwordField.setText(passwordVisibleField.getText());
            passwordField.setVisible(true);
            passwordVisibleField.setVisible(false);
            togglePasswordBtn.setText("👁");
        }
    }
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordVisible ? passwordVisibleField.getText() : passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }
        
        Admin admin = userDAO.authenticate(username, password);
        
        if (admin != null) {
            // Store in session
            utils.SessionManager.setCurrentUser(admin.getUsername(), admin.getAdminId());
            // Initialize selected school year (newest first)
            utils.SessionManager.initializeSelectedSchoolYear();
            try {
                // Load dashboard - try multiple paths
                java.net.URL resource = getClass().getClassLoader().getResource("views/dashboard.fxml");
                if (resource == null) {
                    resource = getClass().getResource("/views/dashboard.fxml");
                }
                if (resource == null) {
                    resource = getClass().getResource("../views/dashboard.fxml");
                }
                if (resource == null) {
                    throw new IOException("Cannot find dashboard.fxml file");
                }
                
                FXMLLoader loader = new FXMLLoader(resource);
                Parent root = loader.load();
                
                // Get screen dimensions
                javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
                javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
                
                Stage stage = (Stage) loginButton.getScene().getWindow();
                Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
                
                // Apply CSS stylesheet for consistent table styling
                try {
                    java.net.URL cssUrl = getClass().getResource("/styles/dashboard.css");
                    if (cssUrl != null && !scene.getStylesheets().contains(cssUrl.toExternalForm())) {
                        scene.getStylesheets().add(cssUrl.toExternalForm());
                    }
                } catch (Exception e) {
                    // CSS file not found, continue without it
                }
                
                stage.setScene(scene);
                stage.setTitle("DorPay - Dashboard");
                stage.setResizable(false);
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                
                // Prevent window from being moved or resized - keep it fixed
                final double fixedX = bounds.getMinX();
                final double fixedY = bounds.getMinY();
                final double fixedWidth = bounds.getWidth();
                final double fixedHeight = bounds.getHeight();
                
                stage.xProperty().addListener((obs, oldVal, newVal) -> {
                    if (Math.abs(newVal.doubleValue() - fixedX) > 1) {
                        stage.setX(fixedX);
                    }
                });
                stage.yProperty().addListener((obs, oldVal, newVal) -> {
                    if (Math.abs(newVal.doubleValue() - fixedY) > 1) {
                        stage.setY(fixedY);
                    }
                });
                stage.widthProperty().addListener((obs, oldVal, newVal) -> {
                    if (Math.abs(newVal.doubleValue() - fixedWidth) > 1) {
                        stage.setWidth(fixedWidth);
                    }
                });
                stage.heightProperty().addListener((obs, oldVal, newVal) -> {
                    if (Math.abs(newVal.doubleValue() - fixedHeight) > 1) {
                        stage.setHeight(fixedHeight);
                    }
                });
                
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showError("Error loading dashboard: " + e.getMessage());
            }
        } else {
            showError("Invalid username or password");
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}

