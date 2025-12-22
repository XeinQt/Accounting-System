package accountingsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.DatabaseUtil;

public class AccountingSystem extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize database
            try {
                DatabaseUtil.initializeDatabase();
                // Migrate existing plain text passwords to hashed passwords
                utils.PasswordMigration.migratePasswords();
            } catch (Exception dbEx) {
                System.err.println("Database initialization error: " + dbEx.getMessage());
                dbEx.printStackTrace();
                // Continue anyway - database might not be critical for initial load
            }
            
            // Load login FXML - try multiple paths
            FXMLLoader loader = null;
            java.net.URL resource = null;
            
            // Try classpath resource first
            resource = getClass().getClassLoader().getResource("views/login.fxml");
            if (resource == null) {
                resource = getClass().getResource("/views/login.fxml"); 
            }
            if (resource == null) {
                resource = getClass().getResource("../views/login.fxml");
            }
            
            // If still not found, try file system paths
            if (resource == null) {
                String[] possiblePaths = {
                    "src/views/login.fxml",
                    "../src/views/login.fxml",
                    System.getProperty("user.dir") + "/src/views/login.fxml"
                };
                
                for (String path : possiblePaths) {
                    java.io.File fxmlFile = new java.io.File(path);
                    if (fxmlFile.exists()) {
                        try {
                            resource = fxmlFile.toURI().toURL();
                            break;
                        } catch (java.net.MalformedURLException e) {
                            // Continue to next path
                        }
                    }
                }
            }
            
            if (resource == null) {
                throw new Exception("Cannot find login.fxml file. " +
                    "Tried classpath and file system paths. " +
                    "Current directory: " + System.getProperty("user.dir") + 
                    ". Make sure src/views/login.fxml exists.");
            }
            
            loader = new FXMLLoader(resource);
            Parent root = loader.load();
            
            // Get screen dimensions for full screen
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
            
            Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
            
            primaryStage.setTitle("DORPAY - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.setX(bounds.getMinX());
            primaryStage.setY(bounds.getMinY());
            
            // Prevent window from being moved or resized - keep it fixed
            final double fixedX = bounds.getMinX();
            final double fixedY = bounds.getMinY();
            final double fixedWidth = bounds.getWidth();
            final double fixedHeight = bounds.getHeight();
            
            primaryStage.xProperty().addListener((obs, oldVal, newVal) -> {
                if (Math.abs(newVal.doubleValue() - fixedX) > 1) {
                    primaryStage.setX(fixedX);
                }
            });
            primaryStage.yProperty().addListener((obs, oldVal, newVal) -> {
                if (Math.abs(newVal.doubleValue() - fixedY) > 1) {
                    primaryStage.setY(fixedY);
                }
            });
            primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (Math.abs(newVal.doubleValue() - fixedWidth) > 1) {
                    primaryStage.setWidth(fixedWidth);
                }
            });
            primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (Math.abs(newVal.doubleValue() - fixedHeight) > 1) {
                    primaryStage.setHeight(fixedHeight);
                }
            });
            
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
            
            // Show error dialog
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Application Error");
            alert.setHeaderText("Failed to start application");
            alert.setContentText("Error: " + e.getMessage() + "\n\nCheck the console for details.");
            alert.showAndWait();
        }
    }
    
    @Override
    public void stop() {
        DatabaseUtil.closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
