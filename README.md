# DORPAY Accounting System

A JavaFX-based accounting system for managing student payments, payables, and financial records.

## Features

- User authentication with database storage
- Dashboard with financial summaries and charts
- Student management
- Payment tracking
- Payables management
- School year management
- Promissory notes tracking

## Requirements

- Java 8 or higher
- JavaFX SDK
- MySQL Server (5.7+ or 8.0+)
- MySQL Connector/J (JDBC Driver)

## Setup Instructions

### 1. Install and Configure MySQL

1. Install MySQL Server from: https://dev.mysql.com/downloads/mysql/
   - Or use XAMPP/WAMP which includes MySQL
2. Make sure MySQL server is running
3. Configure database connection in `src/utils/DatabaseConfig.java`:
   - Update `DB_HOST` (default: localhost)
   - Update `DB_PORT` (default: 3306)
   - Update `DB_USER` (default: root)
   - Update `DB_PASSWORD` (your MySQL password)

### 2. Add MySQL Connector/J

1. Download MySQL Connector/J from: https://dev.mysql.com/downloads/connector/j/
2. Download the latest `mysql-connector-j-X.X.X.jar` file
3. In NetBeans:
   - Right-click on your project
   - Select "Properties"
   - Go to "Libraries"
   - Click "Add JAR/Folder"
   - Select the MySQL Connector JAR file
   - Click "OK"

### 3. Run the Application

1. Open the project in NetBeans
2. Run the `AccountingSystem` class
3. The database and tables will be automatically created on first run
4. Default login credentials:
   - Username: `admin`
   - Password: `admin123`

## Database

The application uses MySQL database (`accounting_system`) which will be created automatically on first run.

### Database Schema (Based on ERD)

- **admin**: Administrator accounts (admin_id, username, password_hash, fullname, email)
- **school_year**: Academic years (school_year_id, year_range)
- **student**: Student information (student_id, student_number, fullname, major, year, dep, college, school_year_id)
- **semester**: Semester amounts (semester_id, first_sem_amount, second_sem_amount, summer_sem_amount)
- **belong**: Student-SchoolYear-Semester association (belong_id, student_id, school_year_id, semester_id)
- **promissory_note**: Promissory notes (promissory_id, created_date, due_date_extended, remaining_balance_snapshot, note_text)
- **duedate**: Due dates (duedate_id, due_date, message, promissory_id)
- **student_payables**: Student payment obligations (payable_id, belong_id, downpayment_amount, amount_paid, remaining_balance, status, duedate_id)

## Window Settings

All FXML windows are set to:
- Width: 1200 pixels
- Height: 800 pixels
- Non-resizable (fixed size)

## Project Structure

```
src/
├── accountingsystem/
│   └── AccountingSystem.java    # Main application class
├── controllers/
│   ├── LoginController.java     # Login page controller
│   └── DashboardController.java # Dashboard controller
├── dao/
│   ├── UserDAO.java             # User data access
│   ├── PaymentDAO.java          # Payment data access
│   └── StudentDAO.java         # Student data access
├── models/
│   ├── User.java                # User model
│   ├── Student.java             # Student model
│   └── Payment.java             # Payment model
├── utils/
│   └── DatabaseUtil.java        # Database connection utility
├── views/
│   ├── login.fxml               # Login page UI
│   └── dashboard.fxml           # Dashboard UI
└── styles/
    └── dashboard.css            # Dashboard styles
```

## Notes

- The MySQL database (`accounting_system`) will be created automatically on first run
- All windows are fixed at 1200x800 pixels and cannot be resized
- Sample data is used for charts; replace with actual database queries as needed
- See `MYSQL_SETUP.md` for detailed MySQL setup instructions

