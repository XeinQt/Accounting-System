# How to Import the Database

This guide explains how to import the `database_setup.sql` file into MySQL.

## Method 1: Using MySQL Command Line

1. **Open MySQL Command Line Client** or Terminal/Command Prompt

2. **Login to MySQL:**
   ```bash
   mysql -u root -p
   ```
   Enter your MySQL password when prompted.

3. **Import the SQL file:**
   ```bash
   source C:/Users/Edriane/OneDrive/Desktop/AccountingSystem/database_setup.sql
   ```
   
   **OR** if you're already in MySQL:
   ```sql
   source database_setup.sql;
   ```

4. **Verify the import:**
   ```sql
   USE accounting_system;
   SHOW TABLES;
   SELECT COUNT(*) FROM admin;
   SELECT COUNT(*) FROM student;
   ```

## Method 2: Using MySQL Workbench

1. **Open MySQL Workbench**

2. **Connect to your MySQL server**

3. **Open the SQL file:**
   - File → Open SQL Script
   - Navigate to `database_setup.sql`
   - Click Open

4. **Execute the script:**
   - Click the Execute button (lightning bolt icon)
   - Or press `Ctrl+Shift+Enter`

5. **Verify:**
   - Check the Output panel for success messages
   - Refresh the database in the left panel
   - Expand `accounting_system` to see all tables

## Method 3: Using phpMyAdmin (if using XAMPP/WAMP)

1. **Open phpMyAdmin** (usually at http://localhost/phpmyadmin)

2. **Click on "Import" tab**

3. **Choose file:**
   - Click "Choose File"
   - Select `database_setup.sql`

4. **Click "Go"** to import

5. **Verify:**
   - Select `accounting_system` database from left sidebar
   - Check that all 8 tables are listed

## Method 4: Using Command Line (Windows)

1. **Open Command Prompt** (as Administrator)

2. **Navigate to project directory:**
   ```cmd
   cd C:\Users\Edriane\OneDrive\Desktop\AccountingSystem
   ```

3. **Import using mysql command:**
   ```cmd
   mysql -u root -p < database_setup.sql
   ```
   Enter your MySQL password when prompted.

## What the Script Does

1. **Creates the database** `accounting_system` (if it doesn't exist)
2. **Creates all 8 tables** according to the ERD:
   - admin
   - school_year
   - student
   - semester
   - belong
   - promissory_note
   - duedate
   - student_payables

3. **Inserts sample data:**
   - 5 admin records
   - 5 school year records
   - 5 semester records
   - 5 student records
   - 5 belong records
   - 5 promissory note records
   - 5 duedate records
   - 5 student payables records

## Verification

After importing, run these queries to verify:

```sql
USE accounting_system;

-- Check all tables exist
SHOW TABLES;

-- Count records in each table
SELECT 'admin' AS table_name, COUNT(*) AS count FROM admin
UNION ALL SELECT 'school_year', COUNT(*) FROM school_year
UNION ALL SELECT 'student', COUNT(*) FROM student
UNION ALL SELECT 'semester', COUNT(*) FROM semester
UNION ALL SELECT 'belong', COUNT(*) FROM belong
UNION ALL SELECT 'promissory_note', COUNT(*) FROM promissory_note
UNION ALL SELECT 'duedate', COUNT(*) FROM duedate
UNION ALL SELECT 'student_payables', COUNT(*) FROM student_payables;

-- View sample data
SELECT * FROM admin;
SELECT * FROM student;
SELECT * FROM student_payables;
```

## Troubleshooting

### Error: "Access denied for user 'root'@'localhost'"
- Check your MySQL username and password
- Make sure MySQL server is running

### Error: "Unknown database"
- The script creates the database automatically
- If error persists, create it manually first:
  ```sql
  CREATE DATABASE accounting_system;
  ```

### Error: "Table already exists"
- The script uses `CREATE TABLE IF NOT EXISTS`, so this shouldn't happen
- If you want to start fresh, drop the database first:
  ```sql
  DROP DATABASE IF EXISTS accounting_system;
  ```
  Then run the script again.

### Foreign Key Constraint Errors
- Make sure you're running the entire script
- Tables are created in the correct order (dependencies first)
- Sample data is inserted in the correct order

## Sample Data Overview

### Admin Accounts
- admin / admin123 (default login)
- 4 additional admin accounts

### Students
- 5 students with different majors and years
- Linked to different school years

### School Years
- 2025-2026, 2024-2025, 2023-2024, 2022-2023, 2021-2022

### Payment Status
- Mix of PAID, PARTIAL, UNPAID, and OVERDUE statuses
- Various payment amounts and balances

## Next Steps

After importing:
1. Update `src/utils/DatabaseConfig.java` with your MySQL credentials
2. Run the JavaFX application
3. Login with: `admin` / `admin123`

The application will work with the imported database!

