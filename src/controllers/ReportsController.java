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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.PaymentView;
import models.SchoolYear;
import utils.SemesterUtil;
import utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportsController extends BaseController {
    
    // Static cache to persist data across navigation
    private static ObservableList<PaymentView> cachedReportsList = null;
    private static Integer cachedSchoolYearId = null;
    private static String cachedSemester = null;
    
    @FXML private TableView<PaymentView> reportsTable;
    @FXML private TableColumn<PaymentView, String> studentIdCol;
    @FXML private TableColumn<PaymentView, String> studentNameCol;
    @FXML private TableColumn<PaymentView, String> totalAmountCol;
    @FXML private TableColumn<PaymentView, String> amountPaidCol;
    @FXML private TableColumn<PaymentView, String> remainingBalanceCol;
    @FXML private TableColumn<PaymentView, String> dueDateCol;
    @FXML private TableColumn<PaymentView, String> statusCol;
    @FXML private Label totalLabel;
    
    @FXML private ComboBox<String> yearComboBoxHeader;
    @FXML private ComboBox<String> semesterComboBoxHeader;
    @FXML private Button exportBtn;
    
    @FXML private Button dashboardBtn;
    @FXML private Button studentsBtn;
    @FXML private Button payablesBtn;
    @FXML private Button paymentsBtn;
    @FXML private Button schoolYearsBtn;
    @FXML private Button promissoryNotesBtn;
    @FXML private Button reportsBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;
    
    private PaymentDAO paymentDAO;
    private SchoolYearDAO schoolYearDAO;
    private ObservableList<PaymentView> reportsList;
    private DecimalFormat currencyFormat = new DecimalFormat("P#,##0.00");
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    
    @FXML
    public void initialize() {
        paymentDAO = new PaymentDAO();
        schoolYearDAO = new SchoolYearDAO();
        reportsList = FXCollections.observableArrayList();
        
        setupTable();
        setupFilters();
        setupSidebarButtons();
        
        // Load cached data first if available
        if (cachedReportsList != null && !cachedReportsList.isEmpty()) {
            // Restore cached data immediately - use setAll for better performance
            reportsList.setAll(cachedReportsList);
            totalLabel.setText(String.valueOf(reportsList.size()));
            // Force table to refresh
            javafx.application.Platform.runLater(() -> {
                reportsTable.refresh();
                // Then verify if filters match, reload if they don't
                verifyAndReloadIfNeeded();
            });
        } else {
            // No cache, load fresh data
            loadReports();
        }
    }
    
    private void setupSidebarButtons() {
        setupSidebarButtonHover(dashboardBtn, false);
        setupSidebarButtonHover(studentsBtn, false);
        setupSidebarButtonHover(payablesBtn, false);
        setupSidebarButtonHover(paymentsBtn, false);
        setupSidebarButtonHover(schoolYearsBtn, false);
        setupSidebarButtonHover(promissoryNotesBtn, false);
        setupSidebarButtonHover(reportsBtn, true); // Active
        setupSidebarButtonHover(settingsBtn, false);
        setupSidebarButtonHover(logoutBtn, false);
    }
    
    private void setupTable() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentNumber"));
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        
        // Format currency columns
        totalAmountCol.setCellValueFactory(cellData -> {
            javafx.beans.property.SimpleStringProperty prop = new javafx.beans.property.SimpleStringProperty();
            prop.set(currencyFormat.format(cellData.getValue().getTotalAmount()));
            return prop;
        });
        
        amountPaidCol.setCellValueFactory(cellData -> {
            javafx.beans.property.SimpleStringProperty prop = new javafx.beans.property.SimpleStringProperty();
            prop.set(currencyFormat.format(cellData.getValue().getAmountPaid()));
            return prop;
        });
        
        remainingBalanceCol.setCellValueFactory(cellData -> {
            PaymentView view = cellData.getValue();
            double remaining = view.getTotalAmount() - view.getAmountPaid();
            javafx.beans.property.SimpleStringProperty prop = new javafx.beans.property.SimpleStringProperty();
            prop.set(currencyFormat.format(Math.max(0, remaining)));
            return prop;
        });
        
        // Format due date
        dueDateCol.setCellValueFactory(cellData -> {
            PaymentView view = cellData.getValue();
            javafx.beans.property.SimpleStringProperty prop = new javafx.beans.property.SimpleStringProperty();
            if (view.getDueDate() != null) {
                prop.set(view.getDueDate().format(dateFormatter));
            } else {
                prop.set("N/A");
            }
            return prop;
        });
        
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Color code status column
        statusCol.setCellFactory(column -> {
            return new javafx.scene.control.TableCell<PaymentView, String>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(status);
                        if ("Paid".equals(status)) {
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        } else if ("Partial".equals(status)) {
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                        } else if ("UNPAID".equals(status)) {
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            };
        });
        
        // Color code due date column (red if overdue, orange if within 7 days)
        dueDateCol.setCellFactory(column -> {
            return new javafx.scene.control.TableCell<PaymentView, String>() {
                @Override
                protected void updateItem(String dateStr, boolean empty) {
                    super.updateItem(dateStr, empty);
                    if (empty || dateStr == null || "N/A".equals(dateStr)) {
                        setText(dateStr);
                        setStyle("");
                    } else {
                        setText(dateStr);
                        try {
                            PaymentView view = getTableView().getItems().get(getIndex());
                            if (view.getDueDate() != null) {
                                java.time.LocalDate dueDate = view.getDueDate();
                                java.time.LocalDate today = java.time.LocalDate.now();
                                
                                if (dueDate.isBefore(today)) {
                                    // Overdue - red
                                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                                } else if (dueDate.isBefore(today.plusDays(7))) {
                                    // Within 7 days - orange
                                    setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                                } else {
                                    // Future date - black
                                    setStyle("-fx-text-fill: black;");
                                }
                            }
                        } catch (Exception e) {
                            setStyle("");
                        }
                    }
                }
            };
        });
        
        reportsTable.setItems(reportsList);
    }
    
    private void setupFilters() {
        // Setup school year dropdown
        setupSchoolYearDropdown(yearComboBoxHeader);
        yearComboBoxHeader.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadReports();
            }
        });
        
        // Setup semester dropdown
        semesterComboBoxHeader.getItems().addAll("All Semesters", "1st Sem", "2nd Sem", "Summer Sem");
        // Auto-select semester based on current month
        String autoSemester = SemesterUtil.getSemesterByCurrentMonth();
        semesterComboBoxHeader.setValue(autoSemester);
        semesterComboBoxHeader.setOnAction(e -> loadReports());
    }
    
    @Override
    protected void onSchoolYearChanged() {
        loadReports();
    }
    
    /**
     * Verify if current filters match cached filters, reload if they don't
     */
    private void verifyAndReloadIfNeeded() {
        // Get selected school year
        Integer schoolYearId = null;
        if (yearComboBoxHeader.getValue() != null) {
            SchoolYear sy = schoolYearDAO.getSchoolYearByRange(yearComboBoxHeader.getValue());
            if (sy != null) {
                schoolYearId = sy.getSchoolYearId();
            }
        } else {
            schoolYearId = SessionManager.getSelectedSchoolYearId();
        }
        
        // Get selected semester
        String semester = semesterComboBoxHeader.getValue();
        if (semester == null || "All Semesters".equals(semester)) {
            semester = null;
        }
        
        // Check if filters match cache
        boolean schoolYearMatches = (cachedSchoolYearId == null && schoolYearId == null) || 
                                   (cachedSchoolYearId != null && schoolYearId != null && cachedSchoolYearId.equals(schoolYearId));
        boolean semesterMatches = (cachedSemester == null && semester == null) || 
                                 (cachedSemester != null && semester != null && cachedSemester.equals(semester));
        boolean filtersMatch = schoolYearMatches && semesterMatches;
        
        if (!filtersMatch) {
            // Filters changed, reload
            loadReports();
        }
    }
    
    private void loadReports() {
        // Get selected school year
        Integer schoolYearId = null;
        if (yearComboBoxHeader.getValue() != null) {
            SchoolYear sy = schoolYearDAO.getSchoolYearByRange(yearComboBoxHeader.getValue());
            if (sy != null) {
                schoolYearId = sy.getSchoolYearId();
            }
        } else {
            schoolYearId = SessionManager.getSelectedSchoolYearId();
        }
        
        // Get selected semester
        String semester = semesterComboBoxHeader.getValue();
        if (semester == null || "All Semesters".equals(semester)) {
            semester = null;
        }
        
        // Check if we can use cached data (same filters)
        boolean schoolYearMatches = (cachedSchoolYearId == null && schoolYearId == null) || 
                                   (cachedSchoolYearId != null && schoolYearId != null && cachedSchoolYearId.equals(schoolYearId));
        boolean semesterMatches = (cachedSemester == null && semester == null) || 
                                 (cachedSemester != null && semester != null && cachedSemester.equals(semester));
        boolean useCache = (cachedReportsList != null && !cachedReportsList.isEmpty() && 
                           schoolYearMatches && semesterMatches);
        
        if (useCache) {
            // Use cached data - update the observable list
            reportsList.setAll(cachedReportsList);
            totalLabel.setText(String.valueOf(reportsList.size()));
            // Force table refresh
            reportsTable.refresh();
        } else {
            // Load fresh data
            List<PaymentView> payments = paymentDAO.getAllPaymentViews(schoolYearId, semester);
            
            // Update the observable list
            reportsList.setAll(payments);
            
            // Update cache with a new observable list
            cachedReportsList = FXCollections.observableArrayList(payments);
            cachedSchoolYearId = schoolYearId;
            cachedSemester = semester;
            
            // Update total
            totalLabel.setText(String.valueOf(reportsList.size()));
            // Force table refresh
            reportsTable.refresh();
        }
    }
    
    @FXML
    private void handleExport() {
        if (reportsList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "There is no data to export.");
            return;
        }
        
        try {
            // Check if Apache POI is available
            Class<?> workbookClass = Class.forName("org.apache.poi.ss.usermodel.Workbook");
            Class<?> xssfWorkbookClass = Class.forName("org.apache.poi.xssf.usermodel.XSSFWorkbook");
            
            // Create workbook
            Object workbook = xssfWorkbookClass.getConstructor().newInstance();
            
            // Create sheet
            java.lang.reflect.Method createSheetMethod = workbookClass.getMethod("createSheet", String.class);
            Object sheet = createSheetMethod.invoke(workbook, "Student Payment Report");
            
            // Create header row
            java.lang.reflect.Method createRowMethod = sheet.getClass().getMethod("createRow", int.class);
            Object headerRow = createRowMethod.invoke(sheet, 0);
            
            // Header cells
            String[] headers = {"Student ID", "Student Name", "Total Amount", "Amount Paid", "Remaining Balance", "Due Date", "Status"};
            java.lang.reflect.Method createCellMethod = headerRow.getClass().getMethod("createCell", int.class);
            java.lang.reflect.Method setCellValueMethod = null;
            
            for (int i = 0; i < headers.length; i++) {
                Object cell = createCellMethod.invoke(headerRow, i);
                // Find setCellValue method
                if (setCellValueMethod == null) {
                    for (java.lang.reflect.Method m : cell.getClass().getMethods()) {
                        if (m.getName().equals("setCellValue") && m.getParameterCount() == 1) {
                            setCellValueMethod = m;
                            break;
                        }
                    }
                }
                if (setCellValueMethod != null) {
                    setCellValueMethod.invoke(cell, headers[i]);
                }
            }
            
            // Data rows
            int rowNum = 1;
            for (PaymentView view : reportsList) {
                Object row = createRowMethod.invoke(sheet, rowNum++);
                
                Object cell0 = createCellMethod.invoke(row, 0);
                setCellValueMethod.invoke(cell0, view.getStudentNumber() != null ? view.getStudentNumber() : "");
                
                Object cell1 = createCellMethod.invoke(row, 1);
                setCellValueMethod.invoke(cell1, view.getStudentName() != null ? view.getStudentName() : "");
                
                Object cell2 = createCellMethod.invoke(row, 2);
                setCellValueMethod.invoke(cell2, currencyFormat.format(view.getTotalAmount()));
                
                Object cell3 = createCellMethod.invoke(row, 3);
                setCellValueMethod.invoke(cell3, currencyFormat.format(view.getAmountPaid()));
                
                double remaining = view.getTotalAmount() - view.getAmountPaid();
                Object cell4 = createCellMethod.invoke(row, 4);
                setCellValueMethod.invoke(cell4, currencyFormat.format(Math.max(0, remaining)));
                
                Object cell5 = createCellMethod.invoke(row, 5);
                if (view.getDueDate() != null) {
                    setCellValueMethod.invoke(cell5, view.getDueDate().format(dateFormatter));
                } else {
                    setCellValueMethod.invoke(cell5, "N/A");
                }
                
                Object cell6 = createCellMethod.invoke(row, 6);
                setCellValueMethod.invoke(cell6, view.getStatus() != null ? view.getStatus() : "");
            }
            
            // Auto-size columns
            java.lang.reflect.Method autoSizeColumnMethod = sheet.getClass().getMethod("autoSizeColumn", int.class);
            for (int i = 0; i < headers.length; i++) {
                autoSizeColumnMethod.invoke(sheet, i);
            }
            
            // Save file
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Excel File");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            
            // Set default filename
            String defaultFileName = "Student_Payment_Report_" + 
                (yearComboBoxHeader.getValue() != null ? yearComboBoxHeader.getValue().replace("-", "_") : "All") + "_" +
                (semesterComboBoxHeader.getValue() != null && !"All Semesters".equals(semesterComboBoxHeader.getValue()) 
                    ? semesterComboBoxHeader.getValue().replace(" ", "_") : "All") + ".xlsx";
            fileChooser.setInitialFileName(defaultFileName);
            
            Stage stage = (Stage) exportBtn.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                java.lang.reflect.Method writeMethod = workbookClass.getMethod("write", java.io.OutputStream.class);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    writeMethod.invoke(workbook, fos);
                }
                
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", 
                    "Student payment report exported successfully to:\n" + file.getAbsolutePath());
            }
            
        } catch (ClassNotFoundException e) {
            // Apache POI not found - attempt fallback .xlsx writer (built-in, no dependencies)
            try {
                exportToXlsxFallback();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Apache POI Not Found", 
                    "Excel export requires Apache POI library or a working .xlsx writer.\n\n" +
                    "Please add the following JAR files to your project if fallback fails:\n" +
                    "- poi-X.X.X.jar\n" +
                    "- poi-ooxml-X.X.X.jar\n\n" +
                    "See EXCEL_IMPORT_SETUP.md for instructions.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Export Error", 
                "Failed to export Excel file:\n" + e.getMessage());
        }
    }

    /**
     * Fallback .xlsx exporter when Apache POI is not available.
     * Creates a simple .xlsx file with student payment report data.
     */
    private void exportToXlsxFallback() throws Exception {
        // Create temporary directory for workbook parts
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("xlsx_export_");
        
        // Create directory structure
        java.nio.file.Files.createDirectory(tempDir.resolve("_rels"));
        java.nio.file.Files.createDirectory(tempDir.resolve("xl"));
        java.nio.file.Files.createDirectory(tempDir.resolve("xl/_rels"));
        java.nio.file.Files.createDirectory(tempDir.resolve("xl/worksheets"));
        
        // 1. Create workbook.xml.rels
        String workbookRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>\n" +
            "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n" +
            "</Relationships>";
        java.nio.file.Files.write(tempDir.resolve("xl/_rels/workbook.xml.rels"), workbookRels.getBytes());
        
        // 2. Create [Content_Types].xml
        String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
            "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n" +
            "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n" +
            "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>\n" +
            "</Types>";
        java.nio.file.Files.write(tempDir.resolve("[Content_Types].xml"), contentTypes.getBytes());
        
        // 3. Create .rels (root level)
        String rootRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n" +
            "</Relationships>";
        java.nio.file.Files.write(tempDir.resolve("_rels/.rels"), rootRels.getBytes());
        
        // 4. Create styles.xml (minimal style sheet)
        String styles = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n" +
            "<fonts><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>\n" +
            "<fills><fill/><fill/></fills>\n" +
            "<borders><border/></borders>\n" +
            "<cellStyleXfs><xf/></cellStyleXfs>\n" +
            "<cellXfs><xf/></cellXfs>\n" +
            "</styleSheet>";
        java.nio.file.Files.write(tempDir.resolve("xl/styles.xml"), styles.getBytes());
        
        // 5. Create sheet1.xml with student payment data
        StringBuilder sheetXml = new StringBuilder();
        sheetXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sheetXml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");
        sheetXml.append("<sheetData>\n");
        
        // Header row
        String[] headers = {"Student ID", "Student Name", "Total Amount", "Amount Paid", "Remaining Balance", "Due Date", "Status"};
        sheetXml.append("<row r=\"1\">\n");
        for (int i = 0; i < headers.length; i++) {
            String cellRef = toExcelColumn(i) + "1";
            sheetXml.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\">");
            sheetXml.append("<is><t>").append(escapeXml(headers[i])).append("</t></is>");
            sheetXml.append("</c>\n");
        }
        sheetXml.append("</row>\n");
        
        // Data rows
        int rowNum = 2;
        for (PaymentView view : reportsList) {
            sheetXml.append("<row r=\"").append(rowNum).append("\">\n");
            
            // Student ID (column A)
            String cellRef = "A" + rowNum;
            sheetXml.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\">");
            sheetXml.append("<is><t>").append(escapeXml(view.getStudentNumber() != null ? view.getStudentNumber() : "")).append("</t></is>");
            sheetXml.append("</c>\n");
            
            // Student Name (column B)
            cellRef = "B" + rowNum;
            sheetXml.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\">");
            sheetXml.append("<is><t>").append(escapeXml(view.getStudentName() != null ? view.getStudentName() : "")).append("</t></is>");
            sheetXml.append("</c>\n");
            
            // Total Amount (column C)
            cellRef = "C" + rowNum;
            sheetXml.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\">");
            sheetXml.append("<is><t>").append(escapeXml(currencyFormat.format(view.getTotalAmount()))).append("</t></is>");
            sheetXml.append("</c>\n");
            
            // Amount Paid (column D)
            cellRef = "D" + rowNum;
            sheetXml.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\">");
            sheetXml.append("<is><t>").append(escapeXml(currencyFormat.format(view.getAmountPaid()))).append("</t></is>");
            sheetXml.append("</c>\n");
            
            // Remaining Balance (column E)
            double remaining = view.getTotalAmount() - view.getAmountPaid();
            cellRef = "E" + rowNum;
            sheetXml.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\">");
            sheetXml.append("<is><t>").append(escapeXml(currencyFormat.format(Math.max(0, remaining)))).append("</t></is>");
            sheetXml.append("</c>\n");
            
            // Due Date (column F)
            cellRef = "F" + rowNum;
            sheetXml.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\">");
            String dueDate = view.getDueDate() != null ? view.getDueDate().format(dateFormatter) : "N/A";
            sheetXml.append("<is><t>").append(escapeXml(dueDate)).append("</t></is>");
            sheetXml.append("</c>\n");
            
            // Status (column G)
            cellRef = "G" + rowNum;
            sheetXml.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\">");
            sheetXml.append("<is><t>").append(escapeXml(view.getStatus() != null ? view.getStatus() : "")).append("</t></is>");
            sheetXml.append("</c>\n");
            
            sheetXml.append("</row>\n");
            rowNum++;
        }
        
        sheetXml.append("</sheetData>\n");
        sheetXml.append("</worksheet>");
        java.nio.file.Files.write(tempDir.resolve("xl/worksheets/sheet1.xml"), sheetXml.toString().getBytes());
        
        // 6. Create workbook.xml
        String workbook = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n" +
            "<sheets>\n" +
            "<sheet name=\"Student Payment Report\" sheetId=\"1\" r:id=\"rId1\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"/>\n" +
            "</sheets>\n" +
            "</workbook>";
        java.nio.file.Files.write(tempDir.resolve("xl/workbook.xml"), workbook.getBytes());
        
        // 7. Zip everything into .xlsx file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        String defaultFileName = "Student_Payment_Report_" + 
            (yearComboBoxHeader.getValue() != null ? yearComboBoxHeader.getValue().replace("-", "_") : "All") + "_" +
            (semesterComboBoxHeader.getValue() != null && !"All Semesters".equals(semesterComboBoxHeader.getValue()) 
                ? semesterComboBoxHeader.getValue().replace(" ", "_") : "All") + ".xlsx";
        fileChooser.setInitialFileName(defaultFileName);
        
        Stage stage = (Stage) exportBtn.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            zipDirectory(tempDir, file.toPath());
            
            // Clean up temp directory
            java.nio.file.Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        java.nio.file.Files.delete(path);
                    } catch (java.io.IOException e) {
                        // ignore
                    }
                });
            
            showAlert(Alert.AlertType.INFORMATION, "Export Successful", 
                "Student payment report exported successfully to:\n" + file.getAbsolutePath());
        }
    }
    
    /**
     * Helper: convert column index (0-based) to Excel column letter(s).
     */
    private String toExcelColumn(int colIndex) {
        StringBuilder sb = new StringBuilder();
        int col = colIndex + 1;
        while (col > 0) {
            col--;
            sb.insert(0, (char) ('A' + (col % 26)));
            col /= 26;
        }
        return sb.toString();
    }
    
    /**
     * Helper: escape XML special characters.
     */
    private String escapeXml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
    
    /**
     * Helper: zip a directory into .xlsx file.
     */
    private void zipDirectory(java.nio.file.Path sourceDir, java.nio.file.Path zipFile) throws java.io.IOException {
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipFile.toFile()))) {
            java.nio.file.Files.walk(sourceDir)
                .filter(path -> !java.nio.file.Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String relativePath = sourceDir.relativize(path).toString().replace("\\", "/");
                        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(relativePath);
                        zos.putNextEntry(entry);
                        java.nio.file.Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (java.io.IOException e) {
                        // ignore
                    }
                });
        }
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
    private void handleReports() {
        // Already on reports page
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
