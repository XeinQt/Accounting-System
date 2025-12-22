package controllers;

import dao.SchoolYearDAO;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import models.SchoolYear;
import utils.SessionManager;

import java.util.List;

    /**
     * Base controller with common navigation methods
     * All page controllers can extend this or use these methods
     */
public class BaseController {
    
    /**
     * Setup sidebar button hover effects
     */
    protected void setupSidebarButtonHover(Button button, boolean isActive) {
        if (isActive) {
            button.setStyle("-fx-background-color: #7B76F1; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14; -fx-padding: 0 0 0 15;");
        } else {
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: #333; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14; -fx-padding: 0 0 0 15;");
        }
        
        button.setOnMouseEntered(e -> {
            if (!isActive) {
                button.setStyle("-fx-background-color: #F0F0F0; -fx-text-fill: #7B76F1; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14; -fx-padding: 0 0 0 15;");
            }
        });
        
        button.setOnMouseExited(e -> {
            if (!isActive) {
                button.setStyle("-fx-background-color: transparent; -fx-text-fill: #333; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14; -fx-padding: 0 0 0 15;");
            } else {
                button.setStyle("-fx-background-color: #7B76F1; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14; -fx-padding: 0 0 0 15;");
            }
        });
    }
    
    /**
     * Setup school year dropdown - loads only active school years from database, newest first, and syncs with SessionManager
     */
    protected void setupSchoolYearDropdown(ComboBox<String> yearComboBox) {
        SchoolYearDAO schoolYearDAO = new SchoolYearDAO();
        List<SchoolYear> schoolYears = schoolYearDAO.getActiveSchoolYears();
        
        yearComboBox.getItems().clear();
        for (SchoolYear sy : schoolYears) {
            yearComboBox.getItems().add(sy.getYearRange());
        }
        
        // Initialize session if needed
        SessionManager.initializeSelectedSchoolYear();
        
        // Set the selected value based on SessionManager
        Integer selectedId = SessionManager.getSelectedSchoolYearId();
        if (selectedId != null) {
            SchoolYear selected = schoolYearDAO.getSchoolYearById(selectedId);
            if (selected != null) {
                yearComboBox.setValue(selected.getYearRange());
            } else if (!yearComboBox.getItems().isEmpty()) {
                // If selected ID not found, use first (newest)
                yearComboBox.setValue(yearComboBox.getItems().get(0));
                SchoolYear first = schoolYearDAO.getSchoolYearByRange(yearComboBox.getItems().get(0));
                if (first != null) {
                    SessionManager.setSelectedSchoolYearId(first.getSchoolYearId());
                }
            }
        } else if (!yearComboBox.getItems().isEmpty()) {
            // No selection, use first (newest)
            yearComboBox.setValue(yearComboBox.getItems().get(0));
            SchoolYear first = schoolYearDAO.getSchoolYearByRange(yearComboBox.getItems().get(0));
            if (first != null) {
                SessionManager.setSelectedSchoolYearId(first.getSchoolYearId());
            }
        }
        
        // Add listener to update SessionManager when selection changes
        yearComboBox.setOnAction(e -> {
            String selectedRange = yearComboBox.getValue();
            if (selectedRange != null) {
                SchoolYear sy = schoolYearDAO.getSchoolYearByRange(selectedRange);
                if (sy != null) {
                    SessionManager.setSelectedSchoolYearId(sy.getSchoolYearId());
                    // Reload current page data with new year
                    onSchoolYearChanged();
                }
            }
        });
    }
    
    /**
     * Override this method in controllers to reload data when school year changes
     */
    protected void onSchoolYearChanged() {
        // Override in subclasses to reload data
    }
    
    protected void navigateToPage(String fxmlFile, String title, Button currentButton) {
        try {
            java.net.URL resource = getClass().getClassLoader().getResource("views/" + fxmlFile);
            if (resource == null) {
                resource = getClass().getResource("/views/" + fxmlFile);
            }
            if (resource == null) {
                resource = getClass().getResource("../views/" + fxmlFile);
            }
            
            if (resource == null) {
                // Try file system
                String[] possiblePaths = {
                    "src/views/" + fxmlFile,
                    "../src/views/" + fxmlFile
                };
                for (String path : possiblePaths) {
                    java.io.File fxmlFileObj = new java.io.File(path);
                    if (fxmlFileObj.exists()) {
                        resource = fxmlFileObj.toURI().toURL();
                        break;
                    }
                }
            }
            
            if (resource == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Cannot find " + fxmlFile);
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            
            // Get screen dimensions
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
            
            Stage stage = (Stage) currentButton.getScene().getWindow();
            Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
            
            // Apply CSS stylesheet to all pages for consistent table styling
            try {
                java.net.URL cssUrl = getClass().getResource("/styles/dashboard.css");
                if (cssUrl != null && !scene.getStylesheets().contains(cssUrl.toExternalForm())) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception e) {
                // CSS file not found, continue without it
            }
            
            stage.setScene(scene);
            stage.setTitle(title);
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
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to navigate: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    protected void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Make alert larger and more visible
        alert.getDialogPane().setMinHeight(200);
        alert.getDialogPane().setMinWidth(400);
        
        // Apply inline styles for larger text
        alert.getDialogPane().setStyle(
            "-fx-font-size: 14px; " +
            "-fx-padding: 20px;"
        );
        
        alert.showAndWait();
    }
    
    /**
     * Show a larger, more prominent alert dialog
     */
    protected void showLargeAlert(Alert.AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        
        // Make alert much larger
        alert.getDialogPane().setMinHeight(300);
        alert.getDialogPane().setMinWidth(600);
        alert.getDialogPane().setPrefHeight(350);
        alert.getDialogPane().setPrefWidth(650);
        
        // Style for better visibility
        alert.getDialogPane().setStyle(
            "-fx-font-size: 16px; " +
            "-fx-padding: 25px; " +
            "-fx-spacing: 15px;"
        );
        
        // Make content text larger
        javafx.scene.control.Label contentLabel = (javafx.scene.control.Label) alert.getDialogPane().lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-font-size: 16px; -fx-wrap-text: true;");
        }
        
        alert.showAndWait();
    }
}

