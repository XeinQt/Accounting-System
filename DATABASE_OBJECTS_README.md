# Database Triggers, Views, and Functions

This document describes the database objects (triggers, views, and functions) created for the DORPAY Accounting System.

## Overview

The system includes:
- **4 Database Functions** for calculations and data retrieval
- **3 Database Views** for simplified data access
- **6 Database Triggers** for automated data management

## Functions

### 1. `calculate_total_payable(p_belong_id INT)`
Calculates the total payable amount for a student's enrollment.

**Returns:** `DECIMAL(10,2)` - Total amount from semester (first_sem + second_sem + summer_sem)

**Usage:**
```sql
SELECT calculate_total_payable(1) AS total;
```

### 2. `calculate_remaining_balance(p_belong_id INT)`
Calculates the remaining balance after payments.

**Returns:** `DECIMAL(10,2)` - Remaining balance (total_payable - amount_paid)

**Usage:**
```sql
SELECT calculate_remaining_balance(1) AS remaining;
```

### 3. `determine_payment_status(p_belong_id INT)`
Determines the payment status based on amount paid and due date.

**Returns:** `VARCHAR(20)` - Status: 'PAID', 'PARTIAL', 'UNPAID', or 'OVERDUE'

**Usage:**
```sql
SELECT determine_payment_status(1) AS status;
```

### 4. `get_student_fullname(p_student_id INT)`
Gets the full name of a student (combines first_name, middle_name, last_name, or uses fullname).

**Returns:** `VARCHAR(200)` - Full student name

**Usage:**
```sql
SELECT get_student_fullname(1) AS name;
```

## Views

### 1. `v_student_payment_summary`
Comprehensive view of student payment information.

**Columns:**
- student_id, student_number, full_name
- major, year, school_year
- belong_id, semester_name
- total_payable, total_paid, remaining_balance
- payment_status, due_date, latest_payable_id

**Usage:**
```sql
SELECT * FROM v_student_payment_summary 
WHERE school_year = '2025-2026';
```

### 2. `v_overdue_payments`
Lists all overdue payments with days overdue.

**Columns:**
- student_id, student_number, full_name
- school_year, belong_id, payable_id
- remaining_balance, due_date, days_overdue

**Usage:**
```sql
SELECT * FROM v_overdue_payments 
ORDER BY days_overdue DESC;
```

### 3. `v_top_payers`
Shows top paying students.

**Columns:**
- student_id, student_number, student_name
- school_year, total_amount_paid, payment_count

**Usage:**
```sql
SELECT * FROM v_top_payers 
LIMIT 10;
```

## Triggers

### 1. `trg_update_remaining_balance`
**Event:** BEFORE UPDATE on `student_payables`
**Action:** Automatically calculates and updates `remaining_balance` and `status` when `amount_paid` changes.

### 2. `trg_insert_remaining_balance`
**Event:** BEFORE INSERT on `student_payables`
**Action:** Automatically calculates `remaining_balance` and sets `status` for new records.

### 3. `trg_check_overdue_status`
**Event:** BEFORE UPDATE on `duedate`
**Action:** Updates related payables to 'OVERDUE' status if due date has passed.

### 4. `trg_update_student_fullname`
**Event:** BEFORE UPDATE on `student`
**Action:** Automatically updates `fullname` when `first_name`, `middle_name`, or `last_name` changes.

### 5. `trg_insert_student_fullname`
**Event:** BEFORE INSERT on `student`
**Action:** Automatically sets `fullname` from name parts when inserting new students.

### 6. `trg_prevent_deactivate_school_year`
**Event:** BEFORE UPDATE on `school_year`
**Action:** Prevents deactivating a school year if it has active students.

## Integration in Java Code

### Using Functions
```java
String sql = "SELECT calculate_remaining_balance(?) AS remaining";
PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setInt(1, belongId);
ResultSet rs = pstmt.executeQuery();
```

### Using Views
```java
String sql = "SELECT * FROM v_student_payment_summary WHERE student_id = ?";
PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setInt(1, studentId);
ResultSet rs = pstmt.executeQuery();
```

### Example: PaymentDAO.getTopPayersUsingView()
The `PaymentDAO` class includes a method that uses the `v_top_payers` view for better performance.

## Installation

The database objects are automatically created when the application starts via `DatabaseObjectsUtil.initializeDatabaseObjects()`.

Alternatively, you can manually run the SQL file:
```bash
mysql -u username -p accounting_system < database_triggers_views_functions.sql
```

## Benefits

1. **Performance:** Views and functions reduce complex queries
2. **Consistency:** Triggers ensure data integrity automatically
3. **Maintainability:** Business logic centralized in database
4. **Reliability:** Automatic calculations prevent errors

