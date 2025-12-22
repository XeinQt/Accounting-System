package controllers;

import dao.PaymentDAO;
import dao.SchoolYearDAO;
import dao.StudentDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import models.Payment;
import utils.SessionManager;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardController extends BaseController {

    @FXML private Label totalPaymentsLabel;
    @FXML private Label totalPaymentsSubLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label overduePaymentsLabel;
    @FXML private Label chartTotalLabel;
    @FXML private Label chartChangeLabel;
    
    @FXML private BarChart<String, Number> paymentsChart;
    @FXML private CategoryAxis monthAxis;
    @FXML private NumberAxis amountAxis;
    
    @FXML private TableView<Payment> topPayersTable;
    @FXML private TableColumn<Payment, String> payerNameColumn;
    @FXML private TableColumn<Payment, String> payerAmountColumn;
    
    @FXML private TableView<Payment> latestPaymentsTable;
    @FXML private TableColumn<Payment, String> studentIdColumn;
    @FXML private TableColumn<Payment, String> payableAmountColumn;
    @FXML private TableColumn<Payment, String> dateColumn;
    @FXML private TableColumn<Payment, String> studentNameColumn;
    @FXML private TableColumn<Payment, String> statusColumn;
    @FXML private TableColumn<Payment, String> amountColumn;
    
    @FXML private ComboBox<String> yearComboBoxHeader;
    @FXML private ComboBox<String> semesterComboBox;
    
    @FXML private Button dashboardBtn;
    @FXML private Button studentsBtn;
    @FXML private Button payablesBtn;
    @FXML private Button paymentsBtn;
    @FXML private Button notificationsBtn;
    @FXML private Button schoolYearsBtn;
    @FXML private Button promissoryNotesBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;
    
    private PaymentDAO paymentDAO;
    private StudentDAO studentDAO;
    private SchoolYearDAO schoolYearDAO;
    private DecimalFormat currencyFormat = new DecimalFormat("P#,##0.00");
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    public void initialize() {
        paymentDAO = new PaymentDAO();
        studentDAO = new StudentDAO();
        schoolYearDAO = new SchoolYearDAO();
        
        setupYearComboBox();
        setupSemesterComboBox();
        setupSidebarButtons();
        loadSummaryCards();
        loadChart();
        loadTopPayers();
        loadLatestPayments();
        setupTableColumns();
        
        // Apply CSS stylesheet to the scene after it's loaded
        javafx.application.Platform.runLater(() -> {
            try {
                if (topPayersTable.getScene() != null) {
                    java.net.URL cssUrl = getClass().getResource("/styles/dashboard.css");
                    if (cssUrl != null && !topPayersTable.getScene().getStylesheets().contains(cssUrl.toExternalForm())) {
                        topPayersTable.getScene().getStylesheets().add(cssUrl.toExternalForm());
                    }
                }
            } catch (Exception e) {
                // CSS file not found, continue without it
            }
        });
    }
    
    private void setupSidebarButtons() {
        setupSidebarButtonHover(dashboardBtn, true);
        setupSidebarButtonHover(studentsBtn, false);
        setupSidebarButtonHover(payablesBtn, false);
        setupSidebarButtonHover(paymentsBtn, false);
        setupSidebarButtonHover(schoolYearsBtn, false);
        setupSidebarButtonHover(promissoryNotesBtn, false);
        setupSidebarButtonHover(settingsBtn, false);
        setupSidebarButtonHover(logoutBtn, false);
    }

    private void setupYearComboBox() {
        setupSchoolYearDropdown(yearComboBoxHeader);
        yearComboBoxHeader.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                onSchoolYearChanged();
            }
        });
    }
    
    private void setupSemesterComboBox() {
        semesterComboBox.getItems().addAll("All Semesters", "1st Sem", "2nd Sem", "Summer Sem");
        semesterComboBox.setValue("All Semesters");
        semesterComboBox.setOnAction(e -> {
            loadSummaryCards();
            loadChart();
            loadTopPayers();
            loadLatestPayments();
        });
    }
    
    private String getSelectedSemester() {
        String selected = semesterComboBox.getValue();
        if (selected == null || "All Semesters".equals(selected)) {
            return null;
        }
        return selected;
    }
    
    @Override
    protected void onSchoolYearChanged() {
        // Reload all data when school year changes
        loadSummaryCards();
        loadChart();
        loadTopPayers();
        loadLatestPayments();
    }

    private void loadSummaryCards() {
        Integer schoolYearId = SessionManager.getSelectedSchoolYearId();
        String semester = getSelectedSemester();
        
        // Total Payments - filtered by school year and semester (uses view)
        double totalPayments = paymentDAO.getTotalPayments(schoolYearId, semester);
        totalPaymentsLabel.setText(currencyFormat.format(totalPayments));
        
        // Count students who paid - filtered by school year and semester (uses view)
        int studentsWhoPaid = studentDAO.getStudentsWhoPaidCount(schoolYearId, semester);
        totalPaymentsSubLabel.setText("Over " + studentsWhoPaid + " students paid");
        
        // Total Students - filtered by school year and semester (uses view)
        int totalStudents = studentDAO.getTotalStudents(schoolYearId, semester);
        totalStudentsLabel.setText(String.valueOf(totalStudents));
        
        // Overdue Payments - filtered by school year and semester (uses view)
        int overdueCount = studentDAO.getOverdueCount(schoolYearId, semester);
        overduePaymentsLabel.setText(String.valueOf(overdueCount));
    }

    private void loadChart() {
        paymentsChart.getData().clear();
        Integer schoolYearId = SessionManager.getSelectedSchoolYearId();
        String semester = getSelectedSemester();
        
        // Configure Y-axis to show 0 to 100k
        amountAxis.setAutoRanging(false);
        amountAxis.setLowerBound(0);
        amountAxis.setUpperBound(100000);
        amountAxis.setTickUnit(10000);
        
        // Get actual monthly payment data from database
        java.util.Map<String, double[]> monthlyData = paymentDAO.getMonthlyPaymentData(schoolYearId, semester);
        
        // Create series for Paid and Unpaid
        XYChart.Series<String, Number> paidSeries = new XYChart.Series<>();
        paidSeries.setName("Paid");
        
        XYChart.Series<String, Number> unpaidSeries = new XYChart.Series<>();
        unpaidSeries.setName("Unpaid");
        
        // Use actual data from database, or show empty chart if no data
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        boolean hasData = false;
        
        for (String month : months) {
            double[] amounts = monthlyData.getOrDefault(month, new double[]{0.0, 0.0});
            double paidAmount = amounts[0];
            double unpaidAmount = amounts[1];
            
            paidSeries.getData().add(new XYChart.Data<>(month, paidAmount));
            unpaidSeries.getData().add(new XYChart.Data<>(month, unpaidAmount));
            
            if (paidAmount > 0 || unpaidAmount > 0) {
                hasData = true;
            }
        }
        
        paymentsChart.getData().addAll(paidSeries, unpaidSeries);
        
        // Update chart labels - filtered by school year and semester
        double total = paymentDAO.getTotalPayments(schoolYearId, semester);
        if (total > 0) {
            chartTotalLabel.setText("P" + (int)(total / 1000) + "k");
        } else {
            chartTotalLabel.setText("P0");
        }
        
        // Only show change percentage if there's actual data
        if (hasData) {
            chartChangeLabel.setText("↑ 5% than last month");
        } else {
            chartChangeLabel.setText("No data available");
        }
    }

    private void loadTopPayers() {
        Integer schoolYearId = SessionManager.getSelectedSchoolYearId();
        String semester = getSelectedSemester();
        List<Payment> topPayers = paymentDAO.getTopPayers(5, schoolYearId, semester);
        ObservableList<Payment> data = FXCollections.observableArrayList(topPayers);
        
        payerNameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        payerNameColumn.setCellFactory(column -> {
            javafx.scene.control.TableCell<Payment, String> cell = new javafx.scene.control.TableCell<Payment, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 15;");
                    }
                }
            };
            return cell;
        });
        
        payerAmountColumn.setCellValueFactory(cellData -> {
            Payment payment = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(currencyFormat.format(payment.getAmount()));
        });
        payerAmountColumn.setCellFactory(column -> {
            javafx.scene.control.TableCell<Payment, String> cell = new javafx.scene.control.TableCell<Payment, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        setStyle("-fx-alignment: CENTER_RIGHT; -fx-padding: 0 15 0 0; -fx-font-weight: bold; -fx-text-fill: #5a0bb4;");
                    }
                }
            };
            return cell;
        });
        
        topPayersTable.setItems(data);
        topPayersTable.setPlaceholder(new Label("No payment data available"));
    }

    private void loadLatestPayments() {
        Integer schoolYearId = SessionManager.getSelectedSchoolYearId();
        String semester = getSelectedSemester();
        List<Payment> latestPayments = paymentDAO.getLatestPayments(10, schoolYearId, semester);
        ObservableList<Payment> data = FXCollections.observableArrayList(latestPayments);
        latestPaymentsTable.setItems(data);
    }

    private void setupTableColumns() {
        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentIdStr"));
        payableAmountColumn.setCellValueFactory(cellData -> {
            Payment payment = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(currencyFormat.format(payment.getAmount()));
        });
        dateColumn.setCellValueFactory(cellData -> {
            Payment payment = cellData.getValue();
            if (payment.getPaymentDate() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    payment.getPaymentDate().format(dateFormatter));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        amountColumn.setCellValueFactory(cellData -> {
            Payment payment = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(currencyFormat.format(payment.getAmount()));
        });
        
        // Make columns expand to fill available width proportionally
        // Using proportional widths that sum to 1.0 (100%)
        studentIdColumn.prefWidthProperty().bind(
            latestPaymentsTable.widthProperty().multiply(0.12)); // 12%
        studentNameColumn.prefWidthProperty().bind(
            latestPaymentsTable.widthProperty().multiply(0.25)); // 25% - more space for names
        payableAmountColumn.prefWidthProperty().bind(
            latestPaymentsTable.widthProperty().multiply(0.18)); // 18%
        dateColumn.prefWidthProperty().bind(
            latestPaymentsTable.widthProperty().multiply(0.15)); // 15%
        statusColumn.prefWidthProperty().bind(
            latestPaymentsTable.widthProperty().multiply(0.15)); // 15%
        amountColumn.prefWidthProperty().bind(
            latestPaymentsTable.widthProperty().multiply(0.15)); // 15%
    }

    @FXML
    private void handleDashboard() {
        // Already on dashboard
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
                try {
                    // Clear session
                    utils.SessionManager.clearSession();
                    
                    // Load login - try multiple paths
                    java.net.URL resource = getClass().getClassLoader().getResource("views/login.fxml");
                    if (resource == null) {
                        resource = getClass().getResource("/views/login.fxml");
                    }
                    if (resource == null) {
                        resource = getClass().getResource("../views/login.fxml");
                    }
                    
                    if (resource == null) {
                        // Try file system
                        String[] possiblePaths = {
                            "src/views/login.fxml",
                            "../src/views/login.fxml"
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
                        throw new Exception("Cannot find login.fxml file");
                    }
                    
                    FXMLLoader loader = new FXMLLoader(resource);
                    Parent root = loader.load();
                    
                    // Get screen dimensions
                    javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
                    javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
                    
                    Stage stage = (Stage) logoutBtn.getScene().getWindow();
                    Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
                    stage.setScene(scene);
                    stage.setTitle("DorPay - Login");
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
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to logout: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

}
