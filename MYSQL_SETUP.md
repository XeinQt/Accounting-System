# MySQL Database Setup Guide

## Prerequisites

1. **MySQL Server** installed and running
   - Download from: https://dev.mysql.com/downloads/mysql/
   - Or use XAMPP/WAMP which includes MySQL

2. **MySQL JDBC Connector** (MySQL Connector/J)
   - Download from: https://dev.mysql.com/downloads/connector/j/
   - Get the latest `mysql-connector-j-X.X.X.jar` file

## Step 1: Configure Database Connection

Edit `src/utils/DatabaseConfig.java` and update these values:

```java
public static final String DB_HOST = "localhost";      // Your MySQL host
public static final String DB_PORT = "3306";           // MySQL port (default: 3306)
public static final String DB_NAME = "accounting_system"; // Database name
public static final String DB_USER = "root";           // Your MySQL username
public static final String DB_PASSWORD = "";            // Your MySQL password
```

**Important:** If your MySQL root user has a password, update `DB_PASSWORD` accordingly.

## Step 2: Add MySQL Connector to Project

1. **Download MySQL Connector/J:**
   - Go to: https://dev.mysql.com/downloads/connector/j/
   - Download the platform-independent ZIP file
   - Extract and find `mysql-connector-j-X.X.X.jar`

2. **Add to NetBeans Project:**
   - Right-click on your project (`AccountingSystem`)
   - Select **Properties**
   - Go to **Libraries** category
   - Click **Add JAR/Folder...**
   - Browse and select the `mysql-connector-j-X.X.X.jar` file
   - Click **OK**

## Step 3: Create MySQL Database (Optional)

The application will automatically create the database if it doesn't exist, but you can also create it manually:

1. **Open MySQL Command Line or MySQL Workbench**

2. **Create the database:**
   ```sql
   CREATE DATABASE IF NOT EXISTS accounting_system;
   ```

3. **Or let the application create it automatically** - it will do this on first run.

## Step 4: Run the Application

1. **Clean and Build:**
   - Right-click project → Clean and Build

2. **Run:**
   - Right-click `AccountingSystem.java` → Run File
   - Or press `F6`

3. **The application will:**
   - Create the database if it doesn't exist
   - Create all tables according to the ERD
   - Insert default admin user and school years

## Database Schema

The database will be created with the following tables matching your ERD:

1. **admin** - Administrator accounts
   - admin_id (PK)
   - username
   - password_hash
   - fullname
   - email

2. **school_year** - Academic years
   - school_year_id (PK)
   - year_range

3. **student** - Student information
   - student_id (PK)
   - student_number
   - fullname
   - major
   - year
   - dep
   - college
   - school_year_id (FK)

4. **semester** - Semester information
   - semester_id (PK)
   - first_sem_amount
   - second_sem_amount
   - summer_sem_amount

5. **belong** - Associative entity (Student-SchoolYear-Semester)
   - belong_id (PK)
   - student_id (FK)
   - school_year_id (FK)
   - semester_id (FK)

6. **promissory_note** - Promissory notes
   - promissory_id (PK)
   - created_date
   - due_date_extended
   - remaining_balance_snapshot
   - note_text

7. **duedate** - Due dates
   - duedate_id (PK)
   - due_date
   - message
   - promissory_id (FK)

8. **student_payables** - Student payment obligations
   - payable_id (PK)
   - belong_id (FK)
   - downpayment_amount
   - amount_paid
   - remaining_balance
   - status (UNPAID, PARTIAL, PAID, OVERDUE)
   - duedate_id (FK)

## Default Login Credentials

- **Username:** `admin`
- **Password:** `admin123`

## Troubleshooting

### Error: "Access denied for user 'root'@'localhost'"

**Solution:**
1. Check your MySQL username and password in `DatabaseConfig.java`
2. Make sure MySQL server is running
3. Verify the user has CREATE DATABASE privileges

### Error: "Communications link failure"

**Solution:**
1. Check if MySQL server is running
2. Verify the host and port in `DatabaseConfig.java`
3. Check firewall settings

### Error: "ClassNotFoundException: com.mysql.cj.jdbc.Driver"

**Solution:**
1. Make sure MySQL Connector/J JAR is added to project libraries
2. Clean and rebuild the project

### Error: "Unknown database 'accounting_system'"

**Solution:**
1. The application should create it automatically
2. If not, create it manually:
   ```sql
   CREATE DATABASE accounting_system;
   ```

### Connection Timeout

**Solution:**
1. Check MySQL server is running
2. Verify connection settings in `DatabaseConfig.java`
3. Try connecting with MySQL Workbench to test credentials

## Testing the Connection

You can test the database connection by calling:
```java
DatabaseUtil.testConnection();
```

This will print "Database connection successful!" if everything is working.

## Notes

- The database and tables are created automatically on first run
- Foreign key constraints are enforced
- All tables use InnoDB engine with UTF8MB4 charset
- Default data (admin user, school years) is inserted automatically

