package controllers;

import dao.PaymentDAO;
import dao.SchoolYearDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import models.PaymentView;
import models.SchoolYear;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationsController extends BaseController {
    
    @FXML private TableView<PaymentView> notificationsTable;
    @FXML private TableColumn<PaymentView, String> studentIdCol;
    @FXML private TableColumn<PaymentView, String> studentNameCol;
    @FXML private TableColumn<PaymentView, String> dueDateCol;
    @FXML private TableColumn<PaymentView, String> amountPaidCol;
    @FXML private TableColumn<PaymentView, String> totalAmountCol;
    @FXML private TableColumn<PaymentView, String> remainingCol;
    @FXML private TableColumn<PaymentView, String> statusCol;
    
    @FXML private ComboBox<String> filterCombo;
    
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
    private SchoolYearDAO schoolYearDAO;
    private ObservableList<PaymentView> notificationsList;
    private Integer currentSchoolYearId;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    @FXML
    public void initialize() {
        paymentDAO = new PaymentDAO();
        schoolYearDAO = new SchoolYearDAO();
        notificationsList = FXCollections.observableArrayList();
        
        setupTable();
        setupSidebarButtons();
        setupFilter();
        loadNotifications(false); // Load all by default
    }
    
    private void setupSidebarButtons() {
        setupSidebarButtonHover(dashboardBtn, false);
        setupSidebarButtonHover(studentsBtn, false);
        setupSidebarButtonHover(payablesBtn, false);
        setupSidebarButtonHover(paymentsBtn, false);
        setupSidebarButtonHover(notificationsBtn, true);
        setupSidebarButtonHover(schoolYearsBtn, false);
        setupSidebarButtonHover(promissoryNotesBtn, false);
        setupSidebarButtonHover(settingsBtn, false);
        setupSidebarButtonHover(logoutBtn, false);
    }
    
    private void setupTable() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentNumber"));
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        dueDateCol.setCellValueFactory(cellData -> {
            PaymentView view = cellData.getValue();
            // getDueDateFormatted() already handles "Paid" status
            return new javafx.beans.property.SimpleStringProperty(view.getDueDateFormatted());
        });
        amountPaidCol.setCellValueFactory(cellData -> {
            PaymentView view = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(String.format("P%.2f", view.getAmountPaid()));
        });
        totalAmountCol.setCellValueFactory(cellData -> {
            PaymentView view = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(String.format("P%.2f", view.getTotalAmount()));
        });
        remainingCol.setCellValueFactory(cellData -> {
            PaymentView view = cellData.getValue();
            double remaining = view.getTotalAmount() - view.getAmountPaid();
            return new javafx.beans.property.SimpleStringProperty(String.format("P%.2f", Math.max(0, remaining)));
        });
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        notificationsTable.setItems(notificationsList);
    }
    
    private void setupFilter() {
        filterCombo.getItems().addAll("All Due Dates", "Today or Within 7 Days");
        filterCombo.setValue("Today or Within 7 Days");
        filterCombo.setOnAction(e -> handleFilterChange());
    }
    
    @FXML
    private void handleFilterChange() {
        String selected = filterCombo.getValue();
        if (selected != null) {
            boolean within7Days = selected.equals("Today or Within 7 Days");
            loadNotifications(within7Days);
        }
    }
    
    private void loadNotifications(boolean within7Days) {
        List<PaymentView> notifications = paymentDAO.getNotifications(currentSchoolYearId, within7Days);
        notificationsList.clear();
        notificationsList.addAll(notifications);
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
        // Already on notifications page
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
                utils.SessionManager.clearSession();
                navigateToPage("login.fxml", "DorPay - Login", logoutBtn);
            }
        });
    }
}

