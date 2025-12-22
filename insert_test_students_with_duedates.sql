-- =====================================================
-- Insert 10 Students with Due Dates for Promissory Notes
-- =====================================================

USE accounting_system;

-- Ensure we have a school year (create if doesn't exist)
INSERT IGNORE INTO school_year (year_range) VALUES ('2024-2025');

-- Ensure we have a semester (create if doesn't exist)
INSERT IGNORE INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount) 
VALUES (50000.00, 50000.00, 25000.00);

-- Get IDs
SET @school_year_id = (SELECT school_year_id FROM school_year WHERE year_range = '2024-2025' LIMIT 1);
SET @semester_id = (SELECT semester_id FROM semester LIMIT 1);

-- Insert 10 students (or update if exists)
INSERT INTO student (student_number, fullname, major, year, dep, college, school_year_id, status) VALUES
('STU-2024-001', 'John Michael Santos', 'Computer Science', '1st Year', 'IT', 'College of Computing', @school_year_id, 'active'),
('STU-2024-002', 'Maria Cristina Reyes', 'Information Technology', '2nd Year', 'IT', 'College of Computing', @school_year_id, 'active'),
('STU-2024-003', 'Juan Carlos Dela Cruz', 'Business Administration', '3rd Year', 'Business', 'College of Business', @school_year_id, 'active'),
('STU-2024-004', 'Anna Patricia Garcia', 'Computer Engineering', '1st Year', 'Engineering', 'College of Engineering', @school_year_id, 'active'),
('STU-2024-005', 'Robert James Villanueva', 'Accounting', '2nd Year', 'Business', 'College of Business', @school_year_id, 'active'),
('STU-2024-006', 'Sarah Jane Fernandez', 'Information Systems', '3rd Year', 'IT', 'College of Computing', @school_year_id, 'active'),
('STU-2024-007', 'Mark Anthony Torres', 'Computer Science', '4th Year', 'IT', 'College of Computing', @school_year_id, 'active'),
('STU-2024-008', 'Jennifer Rose Mendoza', 'Business Administration', '1st Year', 'Business', 'College of Business', @school_year_id, 'active'),
('STU-2024-009', 'Christian Paul Ramos', 'Information Technology', '2nd Year', 'IT', 'College of Computing', @school_year_id, 'active'),
('STU-2024-010', 'Michelle Ann Bautista', 'Accounting', '3rd Year', 'Business', 'College of Business', @school_year_id, 'active')
ON DUPLICATE KEY UPDATE 
    fullname = VALUES(fullname),
    major = VALUES(major),
    year = VALUES(year),
    dep = VALUES(dep),
    college = VALUES(college),
    status = 'active';

-- Get student IDs
SET @student1_id = (SELECT student_id FROM student WHERE student_number = 'STU-2024-001');
SET @student2_id = (SELECT student_id FROM student WHERE student_number = 'STU-2024-002');
SET @student3_id = (SELECT student_id FROM student WHERE student_number = 'STU-2024-003');
SET @student4_id = (SELECT student_id FROM student WHERE student_number = 'STU-2024-004');
SET @student5_id = (SELECT student_id FROM student WHERE student_number = 'STU-2024-005');
SET @student6_id = (SELECT student_id FROM student WHERE student_number = 'STU-2024-006');
SET @student7_id = (SELECT student_id FROM student WHERE student_number = 'STU-2024-007');
SET @student8_id = (SELECT student_id FROM student WHERE student_number = 'STU-2024-008');
SET @student9_id = (SELECT student_id FROM student WHERE student_number = 'STU-2024-009');
SET @student10_id = (SELECT student_id FROM student WHERE student_number = 'STU-2024-010');

-- Create belong records (link students to school year and semester)
INSERT INTO belong (student_id, school_year_id, semester_id, status) VALUES
(@student1_id, @school_year_id, @semester_id, 'active'),
(@student2_id, @school_year_id, @semester_id, 'active'),
(@student3_id, @school_year_id, @semester_id, 'active'),
(@student4_id, @school_year_id, @semester_id, 'active'),
(@student5_id, @school_year_id, @semester_id, 'active'),
(@student6_id, @school_year_id, @semester_id, 'active'),
(@student7_id, @school_year_id, @semester_id, 'active'),
(@student8_id, @school_year_id, @semester_id, 'active'),
(@student9_id, @school_year_id, @semester_id, 'active'),
(@student10_id, @school_year_id, @semester_id, 'active')
ON DUPLICATE KEY UPDATE 
    status = 'active';

-- Get belong IDs
SET @belong1_id = (SELECT belong_id FROM belong WHERE student_id = @student1_id AND school_year_id = @school_year_id AND semester_id = @semester_id LIMIT 1);
SET @belong2_id = (SELECT belong_id FROM belong WHERE student_id = @student2_id AND school_year_id = @school_year_id AND semester_id = @semester_id LIMIT 1);
SET @belong3_id = (SELECT belong_id FROM belong WHERE student_id = @student3_id AND school_year_id = @school_year_id AND semester_id = @semester_id LIMIT 1);
SET @belong4_id = (SELECT belong_id FROM belong WHERE student_id = @student4_id AND school_year_id = @school_year_id AND semester_id = @semester_id LIMIT 1);
SET @belong5_id = (SELECT belong_id FROM belong WHERE student_id = @student5_id AND school_year_id = @school_year_id AND semester_id = @semester_id LIMIT 1);
SET @belong6_id = (SELECT belong_id FROM belong WHERE student_id = @student6_id AND school_year_id = @school_year_id AND semester_id = @semester_id LIMIT 1);
SET @belong7_id = (SELECT belong_id FROM belong WHERE student_id = @student7_id AND school_year_id = @school_year_id AND semester_id = @semester_id LIMIT 1);
SET @belong8_id = (SELECT belong_id FROM belong WHERE student_id = @student8_id AND school_year_id = @school_year_id AND semester_id = @semester_id LIMIT 1);
SET @belong9_id = (SELECT belong_id FROM belong WHERE student_id = @student9_id AND school_year_id = @school_year_id AND semester_id = @semester_id LIMIT 1);
SET @belong10_id = (SELECT belong_id FROM belong WHERE student_id = @student10_id AND school_year_id = @school_year_id AND semester_id = @semester_id LIMIT 1);

-- Create due dates (today and within 7 days) - delete existing ones first to avoid duplicates
DELETE FROM duedate WHERE due_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND DATE_ADD(CURDATE(), INTERVAL 7 DAY);

INSERT INTO duedate (due_date, message) VALUES
(CURDATE(), 'Payment due today'),
(DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'Payment due tomorrow'),
(DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'Payment due in 2 days'),
(DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'Payment due in 3 days'),
(DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'Payment due in 4 days'),
(DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'Payment due in 5 days'),
(DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'Payment due in 6 days'),
(DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'Payment due in 7 days'),
(DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'Payment was due yesterday'),
(DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'Payment was due 2 days ago');

-- Get due date IDs
SET @duedate1_id = (SELECT duedate_id FROM duedate WHERE due_date = CURDATE() ORDER BY duedate_id DESC LIMIT 1);
SET @duedate2_id = (SELECT duedate_id FROM duedate WHERE due_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY) ORDER BY duedate_id DESC LIMIT 1);
SET @duedate3_id = (SELECT duedate_id FROM duedate WHERE due_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY) ORDER BY duedate_id DESC LIMIT 1);
SET @duedate4_id = (SELECT duedate_id FROM duedate WHERE due_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY) ORDER BY duedate_id DESC LIMIT 1);
SET @duedate5_id = (SELECT duedate_id FROM duedate WHERE due_date = DATE_ADD(CURDATE(), INTERVAL 4 DAY) ORDER BY duedate_id DESC LIMIT 1);
SET @duedate6_id = (SELECT duedate_id FROM duedate WHERE due_date = DATE_ADD(CURDATE(), INTERVAL 5 DAY) ORDER BY duedate_id DESC LIMIT 1);
SET @duedate7_id = (SELECT duedate_id FROM duedate WHERE due_date = DATE_ADD(CURDATE(), INTERVAL 6 DAY) ORDER BY duedate_id DESC LIMIT 1);
SET @duedate8_id = (SELECT duedate_id FROM duedate WHERE due_date = DATE_ADD(CURDATE(), INTERVAL 7 DAY) ORDER BY duedate_id DESC LIMIT 1);
SET @duedate9_id = (SELECT duedate_id FROM duedate WHERE due_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY) ORDER BY duedate_id DESC LIMIT 1);
SET @duedate10_id = (SELECT duedate_id FROM duedate WHERE due_date = DATE_SUB(CURDATE(), INTERVAL 2 DAY) ORDER BY duedate_id DESC LIMIT 1);

-- Delete existing payables for these belongs to avoid duplicates
DELETE FROM student_payables WHERE belong_id IN (@belong1_id, @belong2_id, @belong3_id, @belong4_id, @belong5_id, @belong6_id, @belong7_id, @belong8_id, @belong9_id, @belong10_id);

-- Create student_payables records with due dates
-- Each student has a payable amount of 50000 (downpayment), with some amount paid, leaving remaining balance
INSERT INTO student_payables (belong_id, downpayment_amount, amount_paid, remaining_balance, status, duedate_id) VALUES
(@belong1_id, 50000.00, 10000.00, 40000.00, 'PARTIAL', @duedate1_id),
(@belong2_id, 50000.00, 20000.00, 30000.00, 'PARTIAL', @duedate2_id),
(@belong3_id, 50000.00, 5000.00, 45000.00, 'PARTIAL', @duedate3_id),
(@belong4_id, 50000.00, 15000.00, 35000.00, 'PARTIAL', @duedate4_id),
(@belong5_id, 50000.00, 25000.00, 25000.00, 'PARTIAL', @duedate5_id),
(@belong6_id, 50000.00, 0.00, 50000.00, 'UNPAID', @duedate6_id),
(@belong7_id, 50000.00, 30000.00, 20000.00, 'PARTIAL', @duedate7_id),
(@belong8_id, 50000.00, 0.00, 50000.00, 'UNPAID', @duedate8_id),
(@belong9_id, 50000.00, 10000.00, 40000.00, 'PARTIAL', @duedate9_id),
(@belong10_id, 50000.00, 5000.00, 45000.00, 'PARTIAL', @duedate10_id);

SELECT 'Successfully inserted 10 students with due dates!' AS message;
SELECT COUNT(*) AS total_students_with_duedates FROM student_payables WHERE duedate_id IS NOT NULL;
