-- =====================================================
-- DORPAY Accounting System - Database Triggers, Views, and Functions
-- =====================================================

USE accounting_system;

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Function to calculate total payable amount for a student
DELIMITER //
CREATE FUNCTION IF NOT EXISTS calculate_total_payable(
    p_belong_id INT
) RETURNS DECIMAL(10,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE total_amount DECIMAL(10,2) DEFAULT 0.00;
    
    SELECT COALESCE(
        (SELECT COALESCE(first_sem_amount, 0) + COALESCE(second_sem_amount, 0) + COALESCE(summer_sem_amount, 0)
         FROM semester s
         INNER JOIN belong b ON s.semester_id = b.semester_id
         WHERE b.belong_id = p_belong_id), 0
    ) INTO total_amount;
    
    RETURN total_amount;
END //
DELIMITER ;

-- Function to calculate remaining balance
DELIMITER //
CREATE FUNCTION IF NOT EXISTS calculate_remaining_balance(
    p_belong_id INT
) RETURNS DECIMAL(10,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE total_payable DECIMAL(10,2) DEFAULT 0.00;
    DECLARE amount_paid DECIMAL(10,2) DEFAULT 0.00;
    DECLARE remaining DECIMAL(10,2) DEFAULT 0.00;
    
    -- Get total payable from semester
    SELECT COALESCE(
        (SELECT COALESCE(first_sem_amount, 0) + COALESCE(second_sem_amount, 0) + COALESCE(summer_sem_amount, 0)
         FROM semester s
         INNER JOIN belong b ON s.semester_id = b.semester_id
         WHERE b.belong_id = p_belong_id), 0
    ) INTO total_payable;
    
    -- Get amount paid
    SELECT COALESCE(SUM(amount_paid), 0)
    INTO amount_paid
    FROM student_payables
    WHERE belong_id = p_belong_id;
    
    SET remaining = total_payable - amount_paid;
    
    RETURN GREATEST(remaining, 0.00);
END //
DELIMITER ;

-- Function to calculate payment status based on amount paid vs total payable
DELIMITER //
CREATE FUNCTION IF NOT EXISTS calculate_payment_status(
    p_amount_paid DECIMAL(10,2),
    p_total_payable DECIMAL(10,2)
) RETURNS VARCHAR(20)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE status_val VARCHAR(20);
    
    IF p_amount_paid >= p_total_payable OR ABS(p_amount_paid - p_total_payable) < 0.01 THEN
        SET status_val = 'Paid';
    ELSEIF p_amount_paid > 0 THEN
        SET status_val = 'Partial';
    ELSE
        SET status_val = 'UNPAID';
    END IF;
    
    RETURN status_val;
END //
DELIMITER ;

-- Function to determine payment status
DELIMITER //
CREATE FUNCTION IF NOT EXISTS determine_payment_status(
    p_belong_id INT
) RETURNS VARCHAR(20)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE total_payable DECIMAL(10,2) DEFAULT 0.00;
    DECLARE amount_paid DECIMAL(10,2) DEFAULT 0.00;
    DECLARE remaining DECIMAL(10,2) DEFAULT 0.00;
    DECLARE due_date_val DATE;
    DECLARE status_val VARCHAR(20);
    
    -- Get total payable
    SELECT COALESCE(
        (SELECT COALESCE(first_sem_amount, 0) + COALESCE(second_sem_amount, 0) + COALESCE(summer_sem_amount, 0)
         FROM semester s
         INNER JOIN belong b ON s.semester_id = b.semester_id
         WHERE b.belong_id = p_belong_id), 0
    ) INTO total_payable;
    
    -- Get amount paid
    SELECT COALESCE(SUM(amount_paid), 0)
    INTO amount_paid
    FROM student_payables
    WHERE belong_id = p_belong_id;
    
    SET remaining = total_payable - amount_paid;
    
    -- Get due date
    SELECT d.due_date INTO due_date_val
    FROM student_payables sp
    LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id
    WHERE sp.belong_id = p_belong_id
    LIMIT 1;
    
    -- Determine status
    IF amount_paid >= total_payable OR ABS(amount_paid - total_payable) < 0.01 THEN
        SET status_val = 'PAID';
    ELSEIF amount_paid > 0 THEN
        IF due_date_val IS NOT NULL AND due_date_val < CURDATE() THEN
            SET status_val = 'OVERDUE';
        ELSE
            SET status_val = 'PARTIAL';
        END IF;
    ELSE
        IF due_date_val IS NOT NULL AND due_date_val < CURDATE() THEN
            SET status_val = 'OVERDUE';
        ELSE
            SET status_val = 'UNPAID';
        END IF;
    END IF;
    
    RETURN status_val;
END //
DELIMITER ;

-- Function to get student full name
DELIMITER //
CREATE FUNCTION IF NOT EXISTS get_student_fullname(
    p_student_id INT
) RETURNS VARCHAR(200)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE full_name VARCHAR(200);
    
    SELECT CONCAT_WS(' ',
        COALESCE(first_name, ''),
        COALESCE(middle_name, ''),
        COALESCE(last_name, ''),
        CASE 
            WHEN first_name IS NULL AND middle_name IS NULL AND last_name IS NULL 
            THEN fullname 
            ELSE '' 
        END
    ) INTO full_name
    FROM student
    WHERE student_id = p_student_id;
    
    RETURN TRIM(full_name);
END //
DELIMITER ;

-- =====================================================
-- VIEWS
-- =====================================================

-- View for student payment summary
DROP VIEW IF EXISTS v_student_payment_summary;
CREATE VIEW v_student_payment_summary AS
SELECT 
    s.student_id,
    s.student_number,
    COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS full_name,
    s.major,
    s.year,
    sy.year_range AS school_year,
    b.belong_id,
    CASE 
        WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
        WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
        WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
        ELSE 'Unknown'
    END AS semester_name,
    COALESCE(sem.first_sem_amount, 0) + COALESCE(sem.second_sem_amount, 0) + COALESCE(sem.summer_sem_amount, 0) AS total_payable,
    COALESCE(SUM(sp.amount_paid), 0) AS total_paid,
    calculate_remaining_balance(b.belong_id) AS remaining_balance,
    determine_payment_status(b.belong_id) AS payment_status,
    MAX(d.due_date) AS due_date,
    MAX(sp.payable_id) AS latest_payable_id
FROM student s
INNER JOIN belong b ON s.student_id = b.student_id
INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id
INNER JOIN semester sem ON b.semester_id = sem.semester_id
LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id
LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id
WHERE COALESCE(s.status, 'active') = 'active'
  AND COALESCE(b.status, 'active') = 'active'
GROUP BY s.student_id, s.student_number, s.major, s.year, sy.year_range, b.belong_id, sem.semester_id;

-- View for overdue payments
DROP VIEW IF EXISTS v_overdue_payments;
CREATE VIEW v_overdue_payments AS
SELECT 
    s.student_id,
    s.student_number,
    COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS full_name,
    sy.year_range AS school_year,
    b.belong_id,
    sp.payable_id,
    calculate_remaining_balance(b.belong_id) AS remaining_balance,
    d.due_date,
    DATEDIFF(CURDATE(), d.due_date) AS days_overdue
FROM student s
INNER JOIN belong b ON s.student_id = b.student_id
INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id
INNER JOIN student_payables sp ON b.belong_id = sp.belong_id
LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id
WHERE d.due_date IS NOT NULL
  AND d.due_date < CURDATE()
  AND sp.status != 'PAID'
  AND COALESCE(s.status, 'active') = 'active'
  AND COALESCE(b.status, 'active') = 'active';

-- View for top payers
DROP VIEW IF EXISTS v_top_payers;
CREATE VIEW v_top_payers AS
SELECT 
    s.student_id,
    s.student_number,
    COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS student_name,
    sy.year_range AS school_year,
    SUM(sp.amount_paid) AS total_amount_paid,
    COUNT(DISTINCT sp.payable_id) AS payment_count
FROM student s
INNER JOIN belong b ON s.student_id = b.student_id
INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id
INNER JOIN student_payables sp ON b.belong_id = sp.belong_id
WHERE sp.amount_paid > 0
  AND COALESCE(s.status, 'active') = 'active'
  AND COALESCE(b.status, 'active') = 'active'
GROUP BY s.student_id, s.student_number, sy.year_range
ORDER BY total_amount_paid DESC;

-- View for payment statistics by month
DROP VIEW IF EXISTS v_payment_statistics_monthly;
CREATE VIEW v_payment_statistics_monthly AS
SELECT 
    DATE_FORMAT(d.due_date, '%Y-%m') AS payment_month,
    DATE_FORMAT(d.due_date, '%M %Y') AS month_name,
    DATE_FORMAT(d.due_date, '%b') AS month_abbr,
    COUNT(DISTINCT sp.payable_id) AS total_payments,
    COUNT(DISTINCT CASE WHEN sp.status = 'PAID' THEN sp.payable_id END) AS paid_count,
    COUNT(DISTINCT CASE WHEN sp.status = 'PARTIAL' THEN sp.payable_id END) AS partial_count,
    COUNT(DISTINCT CASE WHEN sp.status = 'UNPAID' THEN sp.payable_id END) AS unpaid_count,
    COUNT(DISTINCT CASE WHEN sp.status = 'OVERDUE' THEN sp.payable_id END) AS overdue_count,
    SUM(sp.amount_paid) AS total_amount_paid,
    SUM(sp.remaining_balance) AS total_remaining_balance,
    b.school_year_id,
    CASE 
        WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
        WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
        WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
        ELSE NULL
    END AS semester_name
FROM student_payables sp
INNER JOIN belong b ON sp.belong_id = b.belong_id
INNER JOIN semester sem ON b.semester_id = sem.semester_id
LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id
WHERE COALESCE(b.status, 'active') = 'active'
GROUP BY DATE_FORMAT(d.due_date, '%Y-%m'), DATE_FORMAT(d.due_date, '%M %Y'), DATE_FORMAT(d.due_date, '%b'), b.school_year_id, sem.semester_id
ORDER BY payment_month DESC;

-- View for dashboard summary statistics
DROP VIEW IF EXISTS v_dashboard_summary;
CREATE VIEW v_dashboard_summary AS
SELECT 
    b.school_year_id,
    sy.year_range AS school_year,
    CASE 
        WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
        WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
        WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
        ELSE NULL
    END AS semester_name,
    -- Total payments collected
    COALESCE(SUM(sp.amount_paid), 0) AS total_payments_collected,
    -- Count of unique students who paid
    COUNT(DISTINCT CASE WHEN sp.amount_paid > 0 THEN s.student_id END) AS students_who_paid,
    -- Total students enrolled
    COUNT(DISTINCT s.student_id) AS total_students_enrolled,
    -- Overdue payments count
    COUNT(DISTINCT CASE WHEN d.due_date IS NOT NULL AND d.due_date < CURDATE() AND sp.status != 'PAID' THEN sp.payable_id END) AS overdue_payments_count
FROM student s
INNER JOIN belong b ON s.student_id = b.student_id
INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id
INNER JOIN semester sem ON b.semester_id = sem.semester_id
LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id
LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id
WHERE COALESCE(s.status, 'active') = 'active'
  AND COALESCE(b.status, 'active') = 'active'
GROUP BY b.school_year_id, sy.year_range, sem.semester_id;

-- View for latest payments (for dashboard table)
DROP VIEW IF EXISTS v_latest_payments;
CREATE VIEW v_latest_payments AS
SELECT 
    s.student_id,
    s.student_number,
    COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS student_name,
    sy.year_range AS school_year,
    b.school_year_id,
    CASE 
        WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
        WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
        WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
        ELSE NULL
    END AS semester_name,
    COALESCE(sem.first_sem_amount, 0) + COALESCE(sem.second_sem_amount, 0) + COALESCE(sem.summer_sem_amount, 0) AS payable_amount,
    sp.payable_id,
    sp.amount_paid,
    sp.status,
    d.due_date,
    sp.payable_id AS sort_key
FROM student s
INNER JOIN belong b ON s.student_id = b.student_id
INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id
INNER JOIN semester sem ON b.semester_id = sem.semester_id
INNER JOIN student_payables sp ON b.belong_id = sp.belong_id
LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id
WHERE COALESCE(s.status, 'active') = 'active'
  AND COALESCE(b.status, 'active') = 'active'
ORDER BY sp.payable_id DESC;

-- View for student enrollment details (for student management page)
DROP VIEW IF EXISTS v_student_enrollment;
CREATE VIEW v_student_enrollment AS
SELECT 
    s.student_id,
    s.student_number,
    COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS student_name,
    s.first_name,
    s.middle_name,
    s.last_name,
    s.fullname,
    s.major,
    s.year,
    s.status AS student_status,
    b.belong_id,
    b.school_year_id,
    sy.year_range AS school_year,
    b.semester_id,
    CASE 
        WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
        WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
        WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
        ELSE NULL
    END AS semester_name,
    COALESCE(b.status, 'active') AS enrollment_status,
    COALESCE(sem.first_sem_amount, 0) + COALESCE(sem.second_sem_amount, 0) + COALESCE(sem.summer_sem_amount, 0) AS total_payable
FROM student s
LEFT JOIN belong b ON s.student_id = b.student_id
LEFT JOIN school_year sy ON b.school_year_id = sy.school_year_id
LEFT JOIN semester sem ON b.semester_id = sem.semester_id;

-- View for student payables list (for payables management page)
DROP VIEW IF EXISTS v_student_payables_list;
CREATE VIEW v_student_payables_list AS
SELECT 
    s.student_id,
    s.student_number,
    COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS student_name,
    s.major AS program,
    s.year,
    b.school_year_id,
    sy.year_range AS school_year,
    CASE 
        WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
        WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
        WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
        ELSE NULL
    END AS semester_name,
    COALESCE(sp.downpayment_amount, 0) AS payable_amount,
    d.due_date,
    sp.payable_id,
    b.belong_id,
    sp.status AS payment_status
FROM student s
LEFT JOIN belong b ON s.student_id = b.student_id
LEFT JOIN school_year sy ON b.school_year_id = sy.school_year_id
LEFT JOIN semester sem ON b.semester_id = sem.semester_id
LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id
LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id;

-- View for payments list (for payments management page)
DROP VIEW IF EXISTS v_payments_list;
CREATE VIEW v_payments_list AS
SELECT 
    s.student_id,
    s.student_number,
    COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS student_name,
    b.school_year_id,
    sy.year_range AS school_year,
    CASE 
        WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
        WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
        WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
        ELSE NULL
    END AS semester_name,
    COALESCE(MAX(CASE WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN sem.first_sem_amount ELSE 0 END), 0) AS first_sem_amount,
    COALESCE(MAX(CASE WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN sem.second_sem_amount ELSE 0 END), 0) AS second_sem_amount,
    COALESCE(MAX(CASE WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN sem.summer_sem_amount ELSE 0 END), 0) AS summer_sem_amount,
    COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) AS total_payable,
    COALESCE(SUM(sp.downpayment_amount), 0) AS total_downpayment,
    COALESCE(SUM(sp.amount_paid), 0) AS amount_paid,
    MAX(d.due_date) AS due_date,
    CASE 
        WHEN COALESCE(SUM(sp.amount_paid), 0) >= COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) OR ABS(COALESCE(SUM(sp.amount_paid), 0) - COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0)) < 0.01 THEN 'Paid'
        WHEN COALESCE(SUM(sp.amount_paid), 0) > 0 THEN 'Partial'
        ELSE 'UNPAID'
    END AS payment_status
FROM student s
INNER JOIN belong b ON s.student_id = b.student_id
LEFT JOIN school_year sy ON b.school_year_id = sy.school_year_id
LEFT JOIN semester sem ON b.semester_id = sem.semester_id
INNER JOIN student_payables sp ON b.belong_id = sp.belong_id AND sp.downpayment_amount > 0
LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id
WHERE COALESCE(s.status, 'active') = 'active'
  AND COALESCE(b.status, 'active') = 'active'
GROUP BY s.student_id, s.student_number, s.first_name, s.middle_name, s.last_name, s.fullname, 
         b.school_year_id, sy.year_range, sem.semester_id;

-- =====================================================
-- STORED PROCEDURES
-- =====================================================

-- Procedure to get students with filters
DELIMITER //
DROP PROCEDURE IF EXISTS sp_get_students//
CREATE PROCEDURE sp_get_students(
    IN p_school_year_id INT,
    IN p_status VARCHAR(20),
    IN p_semester VARCHAR(20),
    IN p_search_term VARCHAR(255),
    IN p_year VARCHAR(20),
    IN p_major VARCHAR(100)
)
BEGIN
    SELECT DISTINCT
        s.student_id,
        s.student_number,
        COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS student_name,
        s.first_name,
        s.middle_name,
        s.last_name,
        s.fullname,
        s.major,
        s.year,
        COALESCE(s.status, 'active') AS student_status,
        b.belong_id,
        COALESCE(b.school_year_id, s.school_year_id) AS school_year_id,
        sy.year_range AS school_year,
        b.semester_id,
        CASE 
            WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
            WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
            WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
            ELSE NULL
        END AS semester_name,
        COALESCE(b.status, 'active') AS enrollment_status,
        COALESCE(sem.first_sem_amount, 0) + COALESCE(sem.second_sem_amount, 0) + COALESCE(sem.summer_sem_amount, 0) AS total_payable
    FROM student s
    LEFT JOIN belong b ON s.student_id = b.student_id
        AND (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
    LEFT JOIN school_year sy ON COALESCE(b.school_year_id, s.school_year_id) = sy.school_year_id
    LEFT JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE 1=1
        -- School year filter: match belong record OR student's school_year_id
        AND (p_school_year_id IS NULL OR 
             b.school_year_id = p_school_year_id OR 
             (b.school_year_id IS NULL AND s.school_year_id = p_school_year_id))
        -- Status filter: check both student status and enrollment status
        AND (p_status IS NULL OR p_status = '' OR 
             (COALESCE(b.status, 'active') = p_status AND COALESCE(s.status, 'active') = p_status))
        -- Semester filter: if specified, show students with that semester OR students without belong records
        AND (p_semester IS NULL OR p_semester = '' OR 
             (CASE 
                WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
                WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
                WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
                ELSE NULL
              END = p_semester OR b.belong_id IS NULL))
        -- Search filter
        AND (p_search_term IS NULL OR p_search_term = '' OR 
             s.student_number LIKE CONCAT('%', p_search_term, '%') OR
             COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) LIKE CONCAT('%', p_search_term, '%'))
        -- Year filter
        AND (p_year IS NULL OR p_year = '' OR s.year = p_year)
        -- Major filter
        AND (p_major IS NULL OR p_major = '' OR s.major = p_major)
    ORDER BY s.student_id DESC;
END //
DELIMITER ;

-- Procedure to add a student with enrollment
DELIMITER //
DROP PROCEDURE IF EXISTS sp_add_student//
CREATE PROCEDURE sp_add_student(
    IN p_student_number VARCHAR(50),
    IN p_first_name VARCHAR(100),
    IN p_middle_name VARCHAR(100),
    IN p_last_name VARCHAR(100),
    IN p_major VARCHAR(100),
    IN p_year VARCHAR(20),
    IN p_school_year_id INT,
    IN p_semester VARCHAR(20),
    OUT p_student_id INT,
    OUT p_result VARCHAR(50)
)
BEGIN
    DECLARE v_existing_student_id INT;
    DECLARE v_semester_id INT;
    DECLARE v_belong_id INT;
    DECLARE v_belong_status VARCHAR(20);
    DECLARE v_fullname VARCHAR(200);
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = 'ERROR';
        SET p_student_id = 0;
    END;
    
    START TRANSACTION;
    
    -- Build fullname
    SET v_fullname = TRIM(CONCAT_WS(' ', 
        COALESCE(p_first_name, ''),
        COALESCE(p_middle_name, ''),
        COALESCE(p_last_name, '')
    ));
    
    -- Check if student already exists
    SELECT student_id INTO v_existing_student_id
    FROM student
    WHERE student_number = p_student_number
    LIMIT 1;
    
    -- Get or create semester
    SELECT semester_id INTO v_semester_id
    FROM semester
    WHERE (
        (p_semester = '1st Sem' AND first_sem_amount > 0 AND (second_sem_amount = 0 OR second_sem_amount IS NULL) AND (summer_sem_amount = 0 OR summer_sem_amount IS NULL)) OR
        (p_semester = '2nd Sem' AND second_sem_amount > 0 AND (first_sem_amount = 0 OR first_sem_amount IS NULL) AND (summer_sem_amount = 0 OR summer_sem_amount IS NULL)) OR
        (p_semester = 'Summer Sem' AND summer_sem_amount > 0 AND (first_sem_amount = 0 OR first_sem_amount IS NULL) AND (second_sem_amount = 0 OR second_sem_amount IS NULL))
    )
    LIMIT 1;
    
    -- If semester not found, create one
    IF v_semester_id IS NULL THEN
        INSERT INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount)
        VALUES (
            CASE WHEN p_semester = '1st Sem' THEN 1.00 ELSE 0.00 END,
            CASE WHEN p_semester = '2nd Sem' THEN 1.00 ELSE 0.00 END,
            CASE WHEN p_semester = 'Summer Sem' THEN 1.00 ELSE 0.00 END
        );
        SET v_semester_id = LAST_INSERT_ID();
    END IF;
    
    -- Handle existing student
    IF v_existing_student_id IS NOT NULL THEN
        SET p_student_id = v_existing_student_id;
        
        -- Check if belong record exists
        SELECT belong_id, COALESCE(status, 'active') INTO v_belong_id, v_belong_status
        FROM belong
        WHERE student_id = v_existing_student_id
          AND school_year_id = p_school_year_id
          AND semester_id = v_semester_id
        LIMIT 1;
        
        IF v_belong_id IS NOT NULL THEN
            IF v_belong_status = 'deactivated' THEN
                -- Reactivate existing belong record
                UPDATE belong SET status = 'active' WHERE belong_id = v_belong_id;
                SET p_result = 'REACTIVATED';
            ELSE
                SET p_result = 'EXISTS';
            END IF;
        ELSE
            -- Create new belong record
            INSERT INTO belong (student_id, school_year_id, semester_id, status)
            VALUES (v_existing_student_id, p_school_year_id, v_semester_id, 'active');
            SET p_result = 'ENROLLED';
        END IF;
    ELSE
        -- Create new student
        INSERT INTO student (student_number, first_name, middle_name, last_name, fullname, major, year, school_year_id, status)
        VALUES (p_student_number, p_first_name, p_middle_name, p_last_name, v_fullname, p_major, p_year, p_school_year_id, 'active');
        SET p_student_id = LAST_INSERT_ID();
        
        -- Create belong record
        INSERT INTO belong (student_id, school_year_id, semester_id, status)
        VALUES (p_student_id, p_school_year_id, v_semester_id, 'active');
        SET p_result = 'CREATED';
    END IF;
    
    COMMIT;
END //
DELIMITER ;

-- Procedure to update a student
DELIMITER //
DROP PROCEDURE IF EXISTS sp_update_student//
CREATE PROCEDURE sp_update_student(
    IN p_student_id INT,
    IN p_student_number VARCHAR(50),
    IN p_first_name VARCHAR(100),
    IN p_middle_name VARCHAR(100),
    IN p_last_name VARCHAR(100),
    IN p_major VARCHAR(100),
    IN p_year VARCHAR(20),
    IN p_school_year_id INT,
    IN p_semester VARCHAR(20),
    OUT p_result VARCHAR(50)
)
BEGIN
    DECLARE v_semester_id INT;
    DECLARE v_belong_id INT;
    DECLARE v_fullname VARCHAR(200);
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = 'ERROR';
    END;
    
    START TRANSACTION;
    
    -- Build fullname
    SET v_fullname = TRIM(CONCAT_WS(' ', 
        COALESCE(p_first_name, ''),
        COALESCE(p_middle_name, ''),
        COALESCE(p_last_name, '')
    ));
    
    -- Update student
    UPDATE student
    SET student_number = p_student_number,
        first_name = p_first_name,
        middle_name = p_middle_name,
        last_name = p_last_name,
        fullname = v_fullname,
        major = p_major,
        year = p_year,
        school_year_id = p_school_year_id
    WHERE student_id = p_student_id;
    
    -- Get or create semester
    SELECT semester_id INTO v_semester_id
    FROM semester
    WHERE (
        (p_semester = '1st Sem' AND first_sem_amount > 0 AND (second_sem_amount = 0 OR second_sem_amount IS NULL) AND (summer_sem_amount = 0 OR summer_sem_amount IS NULL)) OR
        (p_semester = '2nd Sem' AND second_sem_amount > 0 AND (first_sem_amount = 0 OR first_sem_amount IS NULL) AND (summer_sem_amount = 0 OR summer_sem_amount IS NULL)) OR
        (p_semester = 'Summer Sem' AND summer_sem_amount > 0 AND (first_sem_amount = 0 OR first_sem_amount IS NULL) AND (second_sem_amount = 0 OR second_sem_amount IS NULL))
    )
    LIMIT 1;
    
    IF v_semester_id IS NULL THEN
        INSERT INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount)
        VALUES (
            CASE WHEN p_semester = '1st Sem' THEN 1.00 ELSE 0.00 END,
            CASE WHEN p_semester = '2nd Sem' THEN 1.00 ELSE 0.00 END,
            CASE WHEN p_semester = 'Summer Sem' THEN 1.00 ELSE 0.00 END
        );
        SET v_semester_id = LAST_INSERT_ID();
    END IF;
    
    -- Ensure belong record exists and update semester
    IF p_school_year_id IS NOT NULL THEN
        SELECT belong_id INTO v_belong_id
        FROM belong
        WHERE student_id = p_student_id AND school_year_id = p_school_year_id
        LIMIT 1;
        
        IF v_belong_id IS NOT NULL THEN
            -- Update existing belong record
            UPDATE belong 
            SET semester_id = v_semester_id, status = 'active'
            WHERE belong_id = v_belong_id;
            
            -- Deactivate other belong records for same student and school year
            UPDATE belong
            SET status = 'deactivated'
            WHERE student_id = p_student_id 
              AND school_year_id = p_school_year_id
              AND belong_id != v_belong_id
              AND COALESCE(status, 'active') = 'active';
        ELSE
            -- Create new belong record
            INSERT INTO belong (student_id, school_year_id, semester_id, status)
            VALUES (p_student_id, p_school_year_id, v_semester_id, 'active');
        END IF;
    END IF;
    
    SET p_result = 'SUCCESS';
    COMMIT;
END //
DELIMITER ;

-- Procedure to deactivate a student
DELIMITER //
DROP PROCEDURE IF EXISTS sp_deactivate_student//
CREATE PROCEDURE sp_deactivate_student(
    IN p_student_id INT,
    OUT p_result VARCHAR(50)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = 'ERROR';
    END;
    
    START TRANSACTION;
    
    -- Deactivate student
    UPDATE student SET status = 'deactivated' WHERE student_id = p_student_id;
    
    -- Deactivate all belong records for this student
    UPDATE belong SET status = 'deactivated' WHERE student_id = p_student_id;
    
    SET p_result = 'SUCCESS';
    COMMIT;
END //
DELIMITER ;

-- Procedure to reactivate a student
DELIMITER //
DROP PROCEDURE IF EXISTS sp_reactivate_student//
CREATE PROCEDURE sp_reactivate_student(
    IN p_student_id INT,
    OUT p_result VARCHAR(50)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = 'ERROR';
    END;
    
    START TRANSACTION;
    
    -- Reactivate student
    UPDATE student SET status = 'active' WHERE student_id = p_student_id;
    
    SET p_result = 'SUCCESS';
    COMMIT;
END //
DELIMITER ;

-- Procedure to get student statistics
DELIMITER //
DROP PROCEDURE IF EXISTS sp_get_student_statistics//
CREATE PROCEDURE sp_get_student_statistics(
    IN p_school_year_id INT,
    IN p_semester VARCHAR(20),
    OUT p_total_students INT,
    OUT p_active_students INT,
    OUT p_deactivated_students INT
)
BEGIN
    SELECT 
        COUNT(DISTINCT student_id) INTO p_total_students
    FROM v_student_enrollment
    WHERE (p_school_year_id IS NULL OR school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR semester_name = p_semester);
    
    SELECT 
        COUNT(DISTINCT student_id) INTO p_active_students
    FROM v_student_enrollment
    WHERE (p_school_year_id IS NULL OR school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR semester_name = p_semester)
      AND enrollment_status = 'active'
      AND student_status = 'active';
    
    SELECT 
        COUNT(DISTINCT student_id) INTO p_deactivated_students
    FROM v_student_enrollment
    WHERE (p_school_year_id IS NULL OR school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR semester_name = p_semester)
      AND (enrollment_status = 'deactivated' OR student_status = 'deactivated');
END //
DELIMITER ;

-- Procedure to get student payables with filters
DELIMITER //
DROP PROCEDURE IF EXISTS sp_get_student_payables//
CREATE PROCEDURE sp_get_student_payables(
    IN p_school_year_id INT,
    IN p_semester VARCHAR(20),
    IN p_search_term VARCHAR(255),
    IN p_year VARCHAR(20),
    IN p_program VARCHAR(100)
)
BEGIN
    SELECT 
        s.student_id,
        s.student_number,
        COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS student_name,
        s.major AS program,
        s.year,
        CASE 
            WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
            WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
            WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
            ELSE COALESCE(p_semester, '1st Sem')
        END AS semester_name,
        COALESCE(MAX(sp.downpayment_amount), 0) AS payable_amount,
        MAX(d.due_date) AS due_date
    FROM student s
    INNER JOIN belong b ON s.student_id = b.student_id
        AND (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
    INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    LEFT JOIN student_payables sp ON b.belong_id = sp.belong_id
    LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id
    WHERE 1=1
        AND COALESCE(s.status, 'active') = 'active'
        AND COALESCE(b.status, 'active') = 'active'
        AND (sp.downpayment_amount IS NULL OR sp.downpayment_amount > 0)
        -- School year filter
        AND (p_school_year_id IS NULL OR 
             b.school_year_id = p_school_year_id OR 
             (b.school_year_id IS NULL AND s.school_year_id = p_school_year_id))
        -- Semester filter: show students with that semester
        AND (p_semester IS NULL OR p_semester = '' OR 
             (CASE 
                WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
                WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
                WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
                ELSE NULL
              END = p_semester))
        -- Search filter
        AND (p_search_term IS NULL OR p_search_term = '' OR 
             s.student_number LIKE CONCAT('%', p_search_term, '%') OR
             COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) LIKE CONCAT('%', p_search_term, '%'))
        -- Year filter
        AND (p_year IS NULL OR p_year = '' OR s.year = p_year)
        -- Program filter
        AND (p_program IS NULL OR p_program = '' OR s.major = p_program)
    GROUP BY s.student_id, s.student_number, s.first_name, s.middle_name, s.last_name, s.fullname, s.major, s.year
    ORDER BY s.student_id DESC;
END //
DELIMITER ;

-- Procedure to save (add/update) student payable
DELIMITER //
DROP PROCEDURE IF EXISTS sp_save_student_payable//
CREATE PROCEDURE sp_save_student_payable(
    IN p_student_id INT,
    IN p_school_year_id INT,
    IN p_amount DECIMAL(10,2),
    IN p_semester VARCHAR(20),
    OUT p_result VARCHAR(50)
)
BEGIN
    DECLARE v_semester_id INT;
    DECLARE v_belong_id INT;
    DECLARE v_duedate_id INT;
    DECLARE v_existing_payable_id INT;
    DECLARE v_first_sem DECIMAL(10,2) DEFAULT 0;
    DECLARE v_second_sem DECIMAL(10,2) DEFAULT 0;
    DECLARE v_summer_sem DECIMAL(10,2) DEFAULT 0;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = 'ERROR';
    END;
    
    START TRANSACTION;
    
    -- Set semester amounts based on semester type
    IF p_semester = '1st Sem' THEN
        SET v_first_sem = p_amount;
    ELSEIF p_semester = '2nd Sem' THEN
        SET v_second_sem = p_amount;
    ELSEIF p_semester = 'Summer Sem' THEN
        SET v_summer_sem = p_amount;
    END IF;
    
    -- Get or create semester record
    SELECT semester_id INTO v_semester_id
    FROM semester
    WHERE ABS(first_sem_amount - v_first_sem) < 0.01
      AND ABS(second_sem_amount - v_second_sem) < 0.01
      AND ABS(summer_sem_amount - v_summer_sem) < 0.01
    LIMIT 1;
    
    IF v_semester_id IS NULL THEN
        INSERT INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount)
        VALUES (v_first_sem, v_second_sem, v_summer_sem);
        SET v_semester_id = LAST_INSERT_ID();
    END IF;
    
    -- Get or create belong record (prefer active, reactivate deactivated if exists)
    SELECT belong_id INTO v_belong_id
    FROM belong
    WHERE student_id = p_student_id
      AND school_year_id = p_school_year_id
      AND semester_id = v_semester_id
      AND COALESCE(status, 'active') = 'active'
    LIMIT 1;
    
    IF v_belong_id IS NULL THEN
        -- Check if deactivated belong record exists
        SELECT belong_id INTO v_belong_id
        FROM belong
        WHERE student_id = p_student_id
          AND school_year_id = p_school_year_id
          AND semester_id = v_semester_id
          AND COALESCE(status, 'active') = 'deactivated'
        LIMIT 1;
        
        IF v_belong_id IS NOT NULL THEN
            -- Reactivate the deactivated belong record
            UPDATE belong
            SET status = 'active'
            WHERE belong_id = v_belong_id;
        ELSE
            -- Create new belong record
            INSERT INTO belong (student_id, school_year_id, semester_id, status)
            VALUES (p_student_id, p_school_year_id, v_semester_id, 'active');
            SET v_belong_id = LAST_INSERT_ID();
        END IF;
    END IF;
    
    -- Check if payable exists for this active belong record
    SELECT payable_id INTO v_existing_payable_id
    FROM student_payables
    WHERE belong_id = v_belong_id
    LIMIT 1;
    
    -- Additional check: ensure no duplicate payable exists for this student/school_year/semester combination
    -- This prevents duplicates if somehow multiple belong records exist
    IF v_existing_payable_id IS NULL THEN
        SELECT sp.payable_id INTO v_existing_payable_id
        FROM student_payables sp
        INNER JOIN belong b ON sp.belong_id = b.belong_id
        WHERE b.student_id = p_student_id
          AND b.school_year_id = p_school_year_id
          AND b.semester_id = v_semester_id
          AND COALESCE(b.status, 'active') = 'active'
        LIMIT 1;
    END IF;
    
    IF v_existing_payable_id IS NOT NULL THEN
        -- Update existing payable - use the found payable_id to ensure we update the correct one
        UPDATE student_payables
        SET downpayment_amount = p_amount,
            remaining_balance = p_amount
        WHERE payable_id = v_existing_payable_id;
        SET p_result = 'UPDATED';
    ELSE
        -- Create due date (2 months from today)
        INSERT INTO duedate (due_date)
        VALUES (DATE_ADD(CURDATE(), INTERVAL 2 MONTH));
        SET v_duedate_id = LAST_INSERT_ID();
        
        -- Insert new payable
        INSERT INTO student_payables (belong_id, downpayment_amount, amount_paid, remaining_balance, status, duedate_id)
        VALUES (v_belong_id, p_amount, 0, p_amount, 'UNPAID', v_duedate_id);
        SET p_result = 'CREATED';
    END IF;
    
    COMMIT;
END //
DELIMITER ;

-- Procedure to delete student payable
DELIMITER //
DROP PROCEDURE IF EXISTS sp_delete_student_payable//
CREATE PROCEDURE sp_delete_student_payable(
    IN p_student_id INT,
    IN p_school_year_id INT,
    IN p_semester VARCHAR(20),
    OUT p_result VARCHAR(50)
)
BEGIN
    DECLARE v_deleted_count INT DEFAULT 0;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = 'ERROR';
    END;
    
    START TRANSACTION;
    
    -- Delete payables for the student in the specified school year and semester
    DELETE sp FROM student_payables sp
    INNER JOIN belong b ON sp.belong_id = b.belong_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE b.student_id = p_student_id
      AND (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester));
    
    SET v_deleted_count = ROW_COUNT();
    
    IF v_deleted_count > 0 THEN
        SET p_result = 'DELETED';
    ELSE
        SET p_result = 'NOT_FOUND';
    END IF;
    
    COMMIT;
END //
DELIMITER ;

-- Procedure to get payable statistics
DELIMITER //
DROP PROCEDURE IF EXISTS sp_get_payable_statistics//
CREATE PROCEDURE sp_get_payable_statistics(
    IN p_school_year_id INT,
    IN p_semester VARCHAR(20),
    OUT p_total_payables INT,
    OUT p_total_amount DECIMAL(10,2),
    OUT p_paid_count INT,
    OUT p_unpaid_count INT,
    OUT p_overdue_count INT
)
BEGIN
    -- Total payables count
    SELECT COUNT(DISTINCT sp.payable_id) INTO p_total_payables
    FROM student_payables sp
    INNER JOIN belong b ON sp.belong_id = b.belong_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester));
    
    -- Total amount
    SELECT COALESCE(SUM(sp.downpayment_amount), 0) INTO p_total_amount
    FROM student_payables sp
    INNER JOIN belong b ON sp.belong_id = b.belong_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester));
    
    -- Paid count
    SELECT COUNT(DISTINCT sp.payable_id) INTO p_paid_count
    FROM student_payables sp
    INNER JOIN belong b ON sp.belong_id = b.belong_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester))
      AND sp.status = 'PAID';
    
    -- Unpaid count
    SELECT COUNT(DISTINCT sp.payable_id) INTO p_unpaid_count
    FROM student_payables sp
    INNER JOIN belong b ON sp.belong_id = b.belong_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester))
      AND sp.status = 'UNPAID';
    
    -- Overdue count
    SELECT COUNT(DISTINCT sp.payable_id) INTO p_overdue_count
    FROM student_payables sp
    INNER JOIN belong b ON sp.belong_id = b.belong_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    INNER JOIN duedate d ON sp.duedate_id = d.duedate_id
    WHERE (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester))
      AND sp.status = 'OVERDUE'
      AND d.due_date < CURDATE();
END //
DELIMITER ;

-- Procedure to get payments with filters
DELIMITER //
DROP PROCEDURE IF EXISTS sp_get_payments//
CREATE PROCEDURE sp_get_payments(
    IN p_school_year_id INT,
    IN p_semester VARCHAR(20),
    IN p_search_term VARCHAR(255),
    IN p_year VARCHAR(20),
    IN p_program VARCHAR(100)
)
BEGIN
    SELECT DISTINCT
        s.student_id,
        s.student_number,
        COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) AS student_name,
        COALESCE(MAX(CASE WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN sem.first_sem_amount ELSE 0 END), 0) AS first_sem_amount,
        COALESCE(MAX(CASE WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN sem.second_sem_amount ELSE 0 END), 0) AS second_sem_amount,
        COALESCE(MAX(CASE WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN sem.summer_sem_amount ELSE 0 END), 0) AS summer_sem_amount,
        COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) AS total_payable,
        COALESCE(SUM(sp.downpayment_amount), 0) AS total_downpayment,
        COALESCE(SUM(sp.amount_paid), 0) AS amount_paid,
        MAX(d.due_date) AS due_date,
        calculate_payment_status(COALESCE(SUM(sp.amount_paid), 0), COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0)) AS payment_status
    FROM student s
    INNER JOIN belong b ON s.student_id = b.student_id
    LEFT JOIN school_year sy ON b.school_year_id = sy.school_year_id
    LEFT JOIN semester sem ON b.semester_id = sem.semester_id
    INNER JOIN student_payables sp ON b.belong_id = sp.belong_id AND sp.downpayment_amount > 0
    LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id
    WHERE COALESCE(s.status, 'active') = 'active'
      AND COALESCE(b.status, 'active') = 'active'
      -- School year filter
      AND (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      -- Semester filter
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester))
      -- Search filter
      AND (p_search_term IS NULL OR p_search_term = '' OR 
           s.student_number LIKE CONCAT('%', p_search_term, '%') OR
           COALESCE(CONCAT_WS(' ', s.first_name, s.middle_name, s.last_name), s.fullname) LIKE CONCAT('%', p_search_term, '%'))
      -- Year filter
      AND (p_year IS NULL OR p_year = '' OR s.year = p_year)
      -- Program filter
      AND (p_program IS NULL OR p_program = '' OR s.major = p_program)
    GROUP BY s.student_id, s.student_number, s.first_name, s.middle_name, s.last_name, s.fullname
    ORDER BY s.student_id DESC;
END //
DELIMITER ;

-- Procedure to save (add/update) payment
DELIMITER //
DROP PROCEDURE IF EXISTS sp_save_payment//
CREATE PROCEDURE sp_save_payment(
    IN p_student_id INT,
    IN p_school_year_id INT,
    IN p_downpayment DECIMAL(10,2),
    IN p_amount_paid DECIMAL(10,2),
    IN p_due_date DATE,
    IN p_status VARCHAR(20),
    OUT p_result VARCHAR(50)
)
BEGIN
    DECLARE v_belong_id INT;
    DECLARE v_payable_id INT;
    DECLARE v_duedate_id INT;
    DECLARE v_current_amount_paid DECIMAL(10,2) DEFAULT 0;
    DECLARE v_current_downpayment DECIMAL(10,2) DEFAULT 0;
    DECLARE v_total_payable DECIMAL(10,2) DEFAULT 0;
    DECLARE v_remaining_balance DECIMAL(10,2) DEFAULT 0;
    DECLARE v_amount_paid DECIMAL(10,2); -- Local variable to cap amount_paid
    
    -- Cursor variables
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur_payable_id INT;
    DECLARE cur_belong_id INT;
    DECLARE cur_downpayment DECIMAL(10,2);
    DECLARE cur_total_payable DECIMAL(10,2);
    DECLARE total_payable_sum DECIMAL(10,2) DEFAULT 0;
    
    -- Loop calculation variables
    DECLARE proportional_amount DECIMAL(10,2);
    DECLARE prop_remaining DECIMAL(10,2);
    DECLARE prop_status VARCHAR(20);
    
    -- Cursor declaration (must be before handlers)
    DECLARE payable_cursor CURSOR FOR
        SELECT sp.payable_id, sp.belong_id, sp.downpayment_amount,
               COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) as semester_total
        FROM student_payables sp
        INNER JOIN belong b ON sp.belong_id = b.belong_id
        INNER JOIN semester sem ON b.semester_id = sem.semester_id
        WHERE b.student_id = p_student_id
          AND (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
          AND COALESCE(b.status, 'active') = 'active'
        GROUP BY sp.payable_id, sp.belong_id, sp.downpayment_amount;
    
    -- Handlers (must be after cursor declaration)
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = 'ERROR';
    END;
    
    START TRANSACTION;
    
    -- Get total payable from all semesters for this student
    SELECT COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) INTO v_total_payable
    FROM belong b
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE b.student_id = p_student_id
      AND (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND COALESCE(b.status, 'active') = 'active';
    
    -- Cap amount_paid at total_payable to prevent any excess (even 0.01)
    SET v_amount_paid = p_amount_paid;
    IF v_amount_paid > v_total_payable THEN
        SET v_amount_paid = v_total_payable;
    END IF;
    
    IF v_total_payable <= 0 THEN
        SET p_result = 'NO_PAYABLES';
        ROLLBACK;
    ELSE
        -- First, calculate total of all payables
        SELECT COALESCE(SUM(sp.downpayment_amount), 0) INTO total_payable_sum
        FROM student_payables sp
        INNER JOIN belong b ON sp.belong_id = b.belong_id
        WHERE b.student_id = p_student_id
          AND (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
          AND COALESCE(b.status, 'active') = 'active';
        
        -- If no payables exist, create one for the first belong record
        IF total_payable_sum = 0 THEN
            SELECT b.belong_id INTO v_belong_id
            FROM belong b
            WHERE b.student_id = p_student_id
              AND (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
              AND COALESCE(b.status, 'active') = 'active'
            LIMIT 1;
            
            IF v_belong_id IS NOT NULL THEN
                -- Get semester amount for this belong
                SELECT COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) INTO v_total_payable
                FROM belong b
                INNER JOIN semester sem ON b.semester_id = sem.semester_id
                WHERE b.belong_id = v_belong_id;
                
                SET v_current_downpayment = IF(p_downpayment > 0, p_downpayment, v_total_payable);
                SET v_remaining_balance = GREATEST(v_total_payable - v_amount_paid, 0.00);
                
                -- Insert new payable
                INSERT INTO student_payables (belong_id, downpayment_amount, amount_paid, remaining_balance, status)
                VALUES (v_belong_id, v_current_downpayment, v_amount_paid, v_remaining_balance, p_status);
                SET v_payable_id = LAST_INSERT_ID();
                SET p_result = 'CREATED';
            ELSE
                SET p_result = 'NO_BELONG_RECORD';
                ROLLBACK;
            END IF;
        ELSE
            -- Update all existing payables proportionally
            OPEN payable_cursor;
            
            read_loop: LOOP
                FETCH payable_cursor INTO cur_payable_id, cur_belong_id, cur_downpayment, cur_total_payable;
                IF done THEN
                    LEAVE read_loop;
                END IF;
                
                -- Calculate proportional amount_paid for this payable
                IF total_payable_sum > 0 THEN
                    SET proportional_amount = (cur_downpayment / total_payable_sum) * v_amount_paid;
                ELSE
                    SET proportional_amount = v_amount_paid;
                END IF;
                
                -- Calculate remaining balance
                SET prop_remaining = GREATEST(cur_downpayment - proportional_amount, 0.00);
                
                -- Determine status for this payable
                IF ABS(proportional_amount - cur_downpayment) < 0.01 OR proportional_amount >= cur_downpayment THEN
                    SET prop_status = 'PAID';
                ELSEIF proportional_amount > 0 THEN
                    SET prop_status = 'PARTIAL';
                ELSE
                    SET prop_status = 'UNPAID';
                END IF;
                
                -- Update this payable
                UPDATE student_payables
                SET amount_paid = proportional_amount,
                    remaining_balance = prop_remaining,
                    status = prop_status
                WHERE payable_id = cur_payable_id;
                
                -- Use first payable for due date handling
                IF v_payable_id IS NULL THEN
                    SET v_payable_id = cur_payable_id;
                END IF;
            END LOOP;
            
            CLOSE payable_cursor;
            SET p_result = 'UPDATED';
        END IF;
        
        -- Handle due date
        IF p_due_date IS NOT NULL AND p_status != 'Paid' THEN
            -- Check if duedate exists
            SELECT duedate_id INTO v_duedate_id
            FROM student_payables
            WHERE payable_id = v_payable_id;
            
            IF v_duedate_id IS NULL THEN
                -- Create new duedate
                INSERT INTO duedate (due_date)
                VALUES (p_due_date);
                SET v_duedate_id = LAST_INSERT_ID();
                
                -- Update payable with duedate_id
                UPDATE student_payables
                SET duedate_id = v_duedate_id
                WHERE payable_id = v_payable_id;
            ELSE
                -- Update existing duedate
                UPDATE duedate
                SET due_date = p_due_date
                WHERE duedate_id = v_duedate_id;
            END IF;
        ELSEIF p_status = 'Paid' THEN
            -- Remove due date if paid
            UPDATE student_payables
            SET duedate_id = NULL
            WHERE payable_id = v_payable_id;
        END IF;
        
        COMMIT;
    END IF;
END //
DELIMITER ;

-- Procedure to delete payment (reset amount_paid to 0)
DELIMITER //
DROP PROCEDURE IF EXISTS sp_delete_payment//
CREATE PROCEDURE sp_delete_payment(
    IN p_student_id INT,
    IN p_school_year_id INT,
    OUT p_result VARCHAR(50)
)
BEGIN
    -- All DECLARE statements must be at the beginning
    DECLARE v_deleted_count INT DEFAULT 0;
    DECLARE v_total_payable DECIMAL(10,2);
    DECLARE v_payable_id INT;
    DECLARE done INT DEFAULT FALSE;
    
    DECLARE cur CURSOR FOR
        SELECT sp.payable_id, 
               COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) AS total
        FROM student_payables sp
        INNER JOIN belong b ON sp.belong_id = b.belong_id
        INNER JOIN semester sem ON b.semester_id = sem.semester_id
        WHERE b.student_id = p_student_id
          AND (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
        GROUP BY sp.payable_id;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = 'ERROR';
    END;
    
    START TRANSACTION;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_payable_id, v_total_payable;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- Reset amount_paid to 0, recalculate remaining_balance
        UPDATE student_payables
        SET amount_paid = 0,
            remaining_balance = v_total_payable,
            status = 'UNPAID',
            duedate_id = NULL
        WHERE payable_id = v_payable_id;
        
        SET v_deleted_count = v_deleted_count + 1;
    END LOOP;
    
    CLOSE cur;
    
    IF v_deleted_count > 0 THEN
        SET p_result = 'DELETED';
    ELSE
        SET p_result = 'NOT_FOUND';
    END IF;
    
    COMMIT;
END //
DELIMITER ;

-- Procedure to get payment statistics
DELIMITER //
DROP PROCEDURE IF EXISTS sp_get_payment_statistics//
CREATE PROCEDURE sp_get_payment_statistics(
    IN p_school_year_id INT,
    IN p_semester VARCHAR(20),
    OUT p_total_payments INT,
    OUT p_total_amount_paid DECIMAL(10,2),
    OUT p_total_payable DECIMAL(10,2),
    OUT p_paid_count INT,
    OUT p_partial_count INT,
    OUT p_unpaid_count INT
)
BEGIN
    -- Total payments count
    SELECT COUNT(DISTINCT sp.payable_id) INTO p_total_payments
    FROM student_payables sp
    INNER JOIN belong b ON sp.belong_id = b.belong_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester))
      AND sp.downpayment_amount > 0;
    
    -- Total amount paid
    SELECT COALESCE(SUM(sp.amount_paid), 0) INTO p_total_amount_paid
    FROM student_payables sp
    INNER JOIN belong b ON sp.belong_id = b.belong_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester))
      AND sp.downpayment_amount > 0;
    
    -- Total payable
    SELECT COALESCE(SUM(sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount), 0) INTO p_total_payable
    FROM student_payables sp
    INNER JOIN belong b ON sp.belong_id = b.belong_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester))
      AND sp.downpayment_amount > 0;
    
    -- Paid count
    SELECT COUNT(DISTINCT sp.payable_id) INTO p_paid_count
    FROM student_payables sp
    INNER JOIN belong b ON sp.belong_id = b.belong_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester))
      AND sp.downpayment_amount > 0
      AND (sp.status = 'Paid' OR sp.amount_paid >= (sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount));
    
    -- Partial count
    SELECT COUNT(DISTINCT sp.payable_id) INTO p_partial_count
    FROM student_payables sp
    INNER JOIN belong b ON sp.belong_id = b.belong_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester))
      AND sp.downpayment_amount > 0
      AND sp.amount_paid > 0
      AND sp.amount_paid < (sem.first_sem_amount + sem.second_sem_amount + sem.summer_sem_amount);
    
    -- Unpaid count
    SELECT COUNT(DISTINCT sp.payable_id) INTO p_unpaid_count
    FROM student_payables sp
    INNER JOIN belong b ON sp.belong_id = b.belong_id
    INNER JOIN semester sem ON b.semester_id = sem.semester_id
    WHERE (p_school_year_id IS NULL OR b.school_year_id = p_school_year_id)
      AND (p_semester IS NULL OR p_semester = '' OR
           (CASE 
              WHEN sem.first_sem_amount > 0 AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '1st Sem'
              WHEN sem.second_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.summer_sem_amount = 0 OR sem.summer_sem_amount IS NULL) THEN '2nd Sem'
              WHEN sem.summer_sem_amount > 0 AND (sem.first_sem_amount = 0 OR sem.first_sem_amount IS NULL) AND (sem.second_sem_amount = 0 OR sem.second_sem_amount IS NULL) THEN 'Summer Sem'
              ELSE NULL
            END = p_semester))
      AND sp.downpayment_amount > 0
      AND (sp.amount_paid = 0 OR sp.amount_paid IS NULL);
END //
DELIMITER ;

-- =====================================================
-- TRIGGERS
-- =====================================================

-- Trigger to auto-update remaining_balance when amount_paid changes
DELIMITER //
CREATE TRIGGER IF NOT EXISTS trg_update_remaining_balance
BEFORE UPDATE ON student_payables
FOR EACH ROW
BEGIN
    DECLARE total_payable DECIMAL(10,2) DEFAULT 0.00;
    
    -- Get total payable from semester
    SELECT COALESCE(
        (SELECT COALESCE(first_sem_amount, 0) + COALESCE(second_sem_amount, 0) + COALESCE(summer_sem_amount, 0)
         FROM semester s
         INNER JOIN belong b ON s.semester_id = b.semester_id
         WHERE b.belong_id = NEW.belong_id), 0
    ) INTO total_payable;
    
    -- Calculate remaining balance
    SET NEW.remaining_balance = GREATEST(total_payable - NEW.amount_paid, 0.00);
    
    -- Auto-update status based on payment
    IF NEW.amount_paid >= total_payable OR ABS(NEW.amount_paid - total_payable) < 0.01 THEN
        SET NEW.status = 'PAID';
    ELSEIF NEW.amount_paid > 0 THEN
        -- Check if overdue
        IF EXISTS (
            SELECT 1 FROM duedate d 
            WHERE d.duedate_id = NEW.duedate_id 
            AND d.due_date < CURDATE()
        ) THEN
            SET NEW.status = 'OVERDUE';
        ELSE
            SET NEW.status = 'PARTIAL';
        END IF;
    ELSE
        -- Check if overdue
        IF EXISTS (
            SELECT 1 FROM duedate d 
            WHERE d.duedate_id = NEW.duedate_id 
            AND d.due_date < CURDATE()
        ) THEN
            SET NEW.status = 'OVERDUE';
        ELSE
            SET NEW.status = 'UNPAID';
        END IF;
    END IF;
END //
DELIMITER ;

-- Trigger to auto-update remaining_balance when inserting new payment
DELIMITER //
CREATE TRIGGER IF NOT EXISTS trg_insert_remaining_balance
BEFORE INSERT ON student_payables
FOR EACH ROW
BEGIN
    DECLARE total_payable DECIMAL(10,2) DEFAULT 0.00;
    
    -- Get total payable from semester
    SELECT COALESCE(
        (SELECT COALESCE(first_sem_amount, 0) + COALESCE(second_sem_amount, 0) + COALESCE(summer_sem_amount, 0)
         FROM semester s
         INNER JOIN belong b ON s.semester_id = b.semester_id
         WHERE b.belong_id = NEW.belong_id), 0
    ) INTO total_payable;
    
    -- Calculate remaining balance if not set
    IF NEW.remaining_balance IS NULL OR NEW.remaining_balance = 0 THEN
        SET NEW.remaining_balance = GREATEST(total_payable - COALESCE(NEW.amount_paid, 0), 0.00);
    END IF;
    
    -- Auto-set status if not provided
    IF NEW.status IS NULL OR NEW.status = '' THEN
        IF NEW.amount_paid >= total_payable OR ABS(NEW.amount_paid - total_payable) < 0.01 THEN
            SET NEW.status = 'PAID';
        ELSEIF NEW.amount_paid > 0 THEN
            SET NEW.status = 'PARTIAL';
        ELSE
            SET NEW.status = 'UNPAID';
        END IF;
    END IF;
END //
DELIMITER ;

-- Trigger to auto-update status when due date passes
DELIMITER //
CREATE TRIGGER IF NOT EXISTS trg_check_overdue_status
BEFORE UPDATE ON duedate
FOR EACH ROW
BEGIN
    -- Update all related payables to OVERDUE if due date has passed and not paid
    UPDATE student_payables sp
    SET sp.status = 'OVERDUE'
    WHERE sp.duedate_id = NEW.duedate_id
      AND NEW.due_date < CURDATE()
      AND sp.status != 'PAID'
      AND sp.amount_paid < (
          SELECT COALESCE(first_sem_amount, 0) + COALESCE(second_sem_amount, 0) + COALESCE(summer_sem_amount, 0)
          FROM semester s
          INNER JOIN belong b ON s.semester_id = b.semester_id
          WHERE b.belong_id = sp.belong_id
      );
END //
DELIMITER ;

-- Trigger to update student fullname when name parts change
DELIMITER //
CREATE TRIGGER IF NOT EXISTS trg_update_student_fullname
BEFORE UPDATE ON student
FOR EACH ROW
BEGIN
    -- Auto-update fullname if name parts are provided
    IF (NEW.first_name IS NOT NULL OR NEW.middle_name IS NOT NULL OR NEW.last_name IS NOT NULL) THEN
        SET NEW.fullname = TRIM(CONCAT_WS(' ', 
            COALESCE(NEW.first_name, ''),
            COALESCE(NEW.middle_name, ''),
            COALESCE(NEW.last_name, '')
        ));
    END IF;
END //
DELIMITER ;

-- Trigger to update student fullname on insert
DELIMITER //
CREATE TRIGGER IF NOT EXISTS trg_insert_student_fullname
BEFORE INSERT ON student
FOR EACH ROW
BEGIN
    -- Auto-update fullname if name parts are provided
    IF (NEW.first_name IS NOT NULL OR NEW.middle_name IS NOT NULL OR NEW.last_name IS NOT NULL) THEN
        SET NEW.fullname = TRIM(CONCAT_WS(' ', 
            COALESCE(NEW.first_name, ''),
            COALESCE(NEW.middle_name, ''),
            COALESCE(NEW.last_name, '')
        ));
    END IF;
END //
DELIMITER ;

-- Trigger to prevent deactivating school year with active students
DELIMITER //
CREATE TRIGGER IF NOT EXISTS trg_prevent_deactivate_school_year
BEFORE UPDATE ON school_year
FOR EACH ROW
BEGIN
    IF NEW.is_active = 0 AND OLD.is_active = 1 THEN
        -- Check if school year has active students
        IF EXISTS (
            SELECT 1 FROM student s 
            WHERE s.school_year_id = NEW.school_year_id 
            AND COALESCE(s.status, 'active') = 'active'
        ) OR EXISTS (
            SELECT 1 FROM belong b 
            WHERE b.school_year_id = NEW.school_year_id 
            AND COALESCE(b.status, 'active') = 'active'
        ) THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot deactivate school year with active students';
        END IF;
    END IF;
END //
DELIMITER ;

