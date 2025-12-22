package controllers;

import dao.PaymentDAO;
import dao.SchoolYearDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import models.PaymentReportView;
import utils.SessionManager;

import java.text.DecimalFormat;
import java.util.List;

public class ReportsController extends BaseController {
    
    @FXML private TableView<PaymentReportView> reportsTable;
    @FXML private TableColumn<PaymentReportView, String> studentIdCol;
    @FXML private TableColumn<PaymentReportView, String> studentNameCol;
    @FXML private TableColumn<PaymentReportView, String> majorCol;
    @FXML private TableColumn<PaymentReportView, String> yearCol;
    @FXML private TableColumn<PaymentReportView, String> totalAmountCol;
    @FXML private TableColumn<PaymentReportView, String> amountPaidCol;
    @FXML private TableColumn<PaymentReportView, String> paymentDateCol;
    @FXML private TableColumn<PaymentReportView, String> statusCol;
    @FXML private Label totalLabel;
    
    @FXML private Button dashboardBtn;
    @FXML private Button studentsBtn;
    @FXML private Button payablesBtn;
    @FXML private Button paymentsBtn;
    @FXML private Button schoolYearsBtn;
    @FXML private Button promissoryNotesBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;
    
    private PaymentDAO paymentDAO;
    private SchoolYearDAO schoolYearDAO;
    private ObservableList<PaymentReportView> reportsList;
    private DecimalFormat currencyFormat = new DecimalFormat("P#,##0.00");
    
    @FXML
    public void initialize() {
        paymentDAO = new PaymentDAO();
        schoolYearDAO = new SchoolYearDAO();
        reportsList = FXCollections.observableArrayList();
        
        setupTable();
        setupSidebarButtons();
        loadReports();
    }
    
    private void setupSidebarButtons() {
        setupSidebarButtonHover(dashboardBtn, false);
        setupSidebarButtonHover(studentsBtn, false);
        setupSidebarButtonHover(payablesBtn, false);
        setupSidebarButtonHover(paymentsBtn, false);
        setupSidebarButtonHover(schoolYearsBtn, false);
        setupSidebarButtonHover(promissoryNotesBtn, false);
        setupSidebarButtonHover(settingsBtn, false);
        setupSidebarButtonHover(logoutBtn, false);
    }
    
    private void setupTable() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentNumber"));
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        majorCol.setCellValueFactory(new PropertyValueFactory<>("major"));
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        totalAmountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmountFormatted"));
        amountPaidCol.setCellValueFactory(new PropertyValueFactory<>("amountPaidFormatted"));
        paymentDateCol.setCellValueFactory(new PropertyValueFactory<>("paymentDateFormatted"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        reportsTable.setItems(reportsList);
    }
    
    @Override
    protected void onSchoolYearChanged() {
        loadReports();
    }
    
    private void loadReports() {
        Integer schoolYearId = SessionManager.getSelectedSchoolYearId();
        
        // Load students who paid
        List<PaymentReportView> reports = paymentDAO.getStudentsWhoPaid(schoolYearId);
        reportsList.clear();
        reportsList.addAll(reports);
        
        // Calculate and display total
        double totalPaid = paymentDAO.getTotalPaidAmount(schoolYearId);
        totalLabel.setText(currencyFormat.format(totalPaid));
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

