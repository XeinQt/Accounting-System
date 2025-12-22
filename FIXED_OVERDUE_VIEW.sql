-- Fixed View for Overdue Payments
-- This version avoids using functions in WHERE clause for better MySQL compatibility

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
  AND sp.status != 'PAID'  -- Use direct status check instead of function
  AND COALESCE(s.status, 'active') = 'active'
  AND COALESCE(b.status, 'active') = 'active';

-- Alternative version if you want to use the function (requires MySQL 5.7+)
-- Note: Some MySQL versions don't allow functions in WHERE clause of views
-- If the above works, you can try this enhanced version:

/*
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
    DATEDIFF(CURDATE(), d.due_date) AS days_overdue,
    determine_payment_status(b.belong_id) AS payment_status
FROM student s
INNER JOIN belong b ON s.student_id = b.student_id
INNER JOIN school_year sy ON b.school_year_id = sy.school_year_id
INNER JOIN student_payables sp ON b.belong_id = sp.belong_id
LEFT JOIN duedate d ON sp.duedate_id = d.duedate_id
WHERE d.due_date IS NOT NULL
  AND d.due_date < CURDATE()
  AND COALESCE(s.status, 'active') = 'active'
  AND COALESCE(b.status, 'active') = 'active'
HAVING payment_status != 'PAID';  -- Use HAVING instead of WHERE for function result
*/

