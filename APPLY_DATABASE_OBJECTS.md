# How to Apply Database Triggers, Views, and Functions to Existing Database

There are **3 ways** to apply the database objects to your existing database:

## Method 1: Automatic (Recommended) ✅

The database objects are **automatically created** when you start the application!

**Steps:**
1. Simply run your Java application
2. The `DatabaseUtil.initializeDatabase()` method will be called
3. It will automatically call `DatabaseObjectsUtil.initializeDatabaseObjects()`
4. All triggers, views, and functions will be created automatically

**What happens:**
- First tries to read `database_triggers_views_functions.sql` file
- If file not found, creates objects programmatically
- Ignores errors for objects that already exist (safe to run multiple times)

**No manual steps required!** Just run your application.

---

## Method 2: Manual SQL File Execution

If you prefer to run the SQL file manually:

### Option A: Using MySQL Command Line

1. Open Command Prompt or Terminal
2. Navigate to your project directory
3. Run:
```bash
mysql -u your_username -p accounting_system < database_triggers_views_functions.sql
```

Replace:
- `your_username` with your MySQL username
- `accounting_system` with your database name (if different)

### Option B: Using MySQL Workbench

1. Open MySQL Workbench
2. Connect to your database
3. Go to **File → Open SQL Script**
4. Select `database_triggers_views_functions.sql`
5. Click **Execute** (or press F5)

### Option C: Using phpMyAdmin

1. Open phpMyAdmin
2. Select your database (`accounting_system`)
3. Click on **SQL** tab
4. Click **Import files**
5. Select `database_triggers_views_functions.sql`
6. Click **Go**

---

## Method 3: Run SQL Statements Individually

If you want to run specific objects only:

### Create Functions Only
```sql
-- Copy the function definitions from database_triggers_views_functions.sql
-- and run them one by one in your MySQL client
```

### Create Views Only
```sql
-- Copy the view definitions from database_triggers_views_functions.sql
-- and run them one by one
```

### Create Triggers Only
```sql
-- Copy the trigger definitions from database_triggers_views_functions.sql
-- Note: Triggers use DELIMITER, so run them carefully
```

---

## Verification

After applying, verify the objects were created:

### Check Functions
```sql
SHOW FUNCTION STATUS WHERE Db = 'accounting_system';
```

Expected functions:
- `calculate_total_payable`
- `calculate_remaining_balance`
- `determine_payment_status`
- `get_student_fullname`

### Check Views
```sql
SHOW FULL TABLES IN accounting_system WHERE Table_type = 'VIEW';
```

Expected views:
- `v_student_payment_summary`
- `v_overdue_payments`
- `v_top_payers`

### Check Triggers
```sql
SHOW TRIGGERS FROM accounting_system;
```

Expected triggers:
- `trg_update_remaining_balance`
- `trg_insert_remaining_balance`
- `trg_check_overdue_status`
- `trg_update_student_fullname`
- `trg_insert_student_fullname`
- `trg_prevent_deactivate_school_year`

### Test a Function
```sql
-- Test calculate_remaining_balance (replace 1 with actual belong_id)
SELECT calculate_remaining_balance(1) AS remaining;
```

### Test a View
```sql
-- Test v_top_payers view
SELECT * FROM v_top_payers LIMIT 5;
```

---

## Troubleshooting

### Error: "Function already exists"
**Solution:** This is normal. The system will drop and recreate functions. If you see this error, the function already exists and is working.

### Error: "View already exists"
**Solution:** The SQL file uses `CREATE OR REPLACE VIEW`, so it should update existing views. If you get this error, manually drop the view first:
```sql
DROP VIEW IF EXISTS v_student_payment_summary;
DROP VIEW IF EXISTS v_overdue_payments;
DROP VIEW IF EXISTS v_top_payers;
```
Then run the SQL file again.

### Error: "Trigger already exists"
**Solution:** Drop the trigger first:
```sql
DROP TRIGGER IF EXISTS trg_update_remaining_balance;
DROP TRIGGER IF EXISTS trg_insert_remaining_balance;
-- ... (drop all triggers)
```
Then run the SQL file again.

### Error: "Access denied"
**Solution:** Make sure your MySQL user has:
- `CREATE` privilege for functions
- `CREATE VIEW` privilege for views
- `TRIGGER` privilege for triggers

Grant privileges:
```sql
GRANT CREATE, CREATE VIEW, TRIGGER ON accounting_system.* TO 'your_username'@'localhost';
FLUSH PRIVILEGES;
```

### Objects Not Created Automatically
**Solution:** 
1. Check console output for error messages
2. Ensure `database_triggers_views_functions.sql` is in the project root directory
3. Or the system will create them programmatically as fallback
4. Check database connection is working

---

## Quick Start (Easiest Method)

**Just run your Java application!** The objects will be created automatically on first run.

If you want to verify:
1. Run the application
2. Check console for: "Database objects initialized successfully!"
3. Run verification SQL queries above

---

## Notes

- ✅ **Safe to run multiple times** - Objects are dropped and recreated
- ✅ **No data loss** - Only creates new objects, doesn't modify existing data
- ✅ **Automatic on startup** - No manual intervention needed
- ⚠️ **Triggers are active immediately** - They will start working as soon as created

