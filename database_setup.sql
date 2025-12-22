-- =====================================================
-- DORPAY Accounting System - Database Setup Script
-- MySQL Database Schema with Sample Data
-- =====================================================

-- Drop database if exists (use with caution in production)
-- DROP DATABASE IF EXISTS accounting_system;

-- Create database
CREATE DATABASE IF NOT EXISTS accounting_system;
USE accounting_system;

-- =====================================================
-- CREATE TABLES
-- =====================================================

-- Admin table
CREATE TABLE IF NOT EXISTS admin (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    fullname VARCHAR(100) NOT NULL,
    email VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- School Year table
CREATE TABLE IF NOT EXISTS school_year (
    school_year_id INT AUTO_INCREMENT PRIMARY KEY,
    year_range VARCHAR(20) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Student table
CREATE TABLE IF NOT EXISTS student (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    student_number VARCHAR(50) UNIQUE NOT NULL,
    fullname VARCHAR(100) NOT NULL,
    major VARCHAR(100),
    year VARCHAR(20),
    dep VARCHAR(100),
    college VARCHAR(100),
    school_year_id INT,
    FOREIGN KEY (school_year_id) REFERENCES school_year(school_year_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Semester table
CREATE TABLE IF NOT EXISTS semester (
    semester_id INT AUTO_INCREMENT PRIMARY KEY,
    first_sem_amount DECIMAL(10,2) DEFAULT 0.00,
    second_sem_amount DECIMAL(10,2) DEFAULT 0.00,
    summer_sem_amount DECIMAL(10,2) DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Belong table (associative entity)
CREATE TABLE IF NOT EXISTS belong (
    belong_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    school_year_id INT NOT NULL,
    semester_id INT NOT NULL,
    FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE,
    FOREIGN KEY (school_year_id) REFERENCES school_year(school_year_id) ON DELETE CASCADE,
    FOREIGN KEY (semester_id) REFERENCES semester(semester_id) ON DELETE CASCADE,
    UNIQUE KEY unique_belong (student_id, school_year_id, semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Promissory Note table
CREATE TABLE IF NOT EXISTS promissory_note (
    promissory_id INT AUTO_INCREMENT PRIMARY KEY,
    created_date DATE NOT NULL,
    due_date_extended DATE,
    remaining_balance_snapshot DECIMAL(10,2) NOT NULL,
    note_text TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Duedate table
CREATE TABLE IF NOT EXISTS duedate (
    duedate_id INT AUTO_INCREMENT PRIMARY KEY,
    due_date DATE NOT NULL,
    message TEXT,
    promissory_id INT,
    FOREIGN KEY (promissory_id) REFERENCES promissory_note(promissory_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Student Payables table
CREATE TABLE IF NOT EXISTS student_payables (
    payable_id INT AUTO_INCREMENT PRIMARY KEY,
    belong_id INT NOT NULL,
    downpayment_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    remaining_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status ENUM('UNPAID', 'PARTIAL', 'PAID', 'OVERDUE') DEFAULT 'UNPAID',
    duedate_id INT,
    FOREIGN KEY (belong_id) REFERENCES belong(belong_id) ON DELETE CASCADE,
    FOREIGN KEY (duedate_id) REFERENCES duedate(duedate_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- INSERT SAMPLE DATA (5 records per entity)
-- =====================================================

-- Insert 5 Admin records
INSERT INTO admin (username, password_hash, fullname, email) VALUES
('admin', 'admin123', 'System Administrator', 'admin@dorpay.com'),
('jdoe', 'password123', 'John Doe', 'jdoe@dorpay.com'),
('asmith', 'password123', 'Alice Smith', 'asmith@dorpay.com'),
('bjohnson', 'password123', 'Bob Johnson', 'bjohnson@dorpay.com'),
('cwilliams', 'password123', 'Carol Williams', 'cwilliams@dorpay.com');

-- Insert 5 School Year records
INSERT INTO school_year (year_range) VALUES
('2025-2026'),
('2024-2025'),
('2023-2024'),
('2022-2023'),
('2021-2022');

-- Insert 5 Semester records
INSERT INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount) VALUES
(50000.00, 50000.00, 25000.00),
(55000.00, 55000.00, 27500.00),
(48000.00, 48000.00, 24000.00),
(52000.00, 52000.00, 26000.00),
(60000.00, 60000.00, 30000.00);

-- Insert 5 Student records
INSERT INTO student (student_number, fullname, major, year, dep, college, school_year_id) VALUES
('2021-001', 'Juan Dela Cruz', 'Computer Science', '4th Year', 'Computer Science', 'College of Engineering', 1),
('2021-002', 'Maria Santos', 'Business Administration', '3rd Year', 'Business', 'College of Business', 1),
('2022-001', 'Jose Garcia', 'Information Technology', '2nd Year', 'Information Technology', 'College of Engineering', 2),
('2022-002', 'Ana Rodriguez', 'Accountancy', '3rd Year', 'Accountancy', 'College of Business', 2),
('2023-001', 'Carlos Martinez', 'Electrical Engineering', '1st Year', 'Electrical Engineering', 'College of Engineering', 3);

-- Insert 5 Belong records (linking students to school years and semesters)
INSERT INTO belong (student_id, school_year_id, semester_id) VALUES
(1, 1, 1),  -- Juan Dela Cruz - 2025-2026 - Semester 1
(2, 1, 1),  -- Maria Santos - 2025-2026 - Semester 1
(3, 2, 2),  -- Jose Garcia - 2024-2025 - Semester 2
(4, 2, 2),  -- Ana Rodriguez - 2024-2025 - Semester 2
(5, 3, 3);  -- Carlos Martinez - 2023-2024 - Semester 3

-- Insert 5 Promissory Note records
INSERT INTO promissory_note (created_date, due_date_extended, remaining_balance_snapshot, note_text) VALUES
('2025-01-15', '2025-03-15', 25000.00, 'Student requested extension for remaining balance payment.'),
('2025-02-01', '2025-04-01', 30000.00, 'Payment plan approved for second semester fees.'),
('2025-02-20', NULL, 15000.00, 'Partial payment promissory note for summer semester.'),
('2025-03-10', '2025-05-10', 20000.00, 'Extended due date for financial hardship case.'),
('2025-03-25', NULL, 12000.00, 'Final payment extension granted.');

-- Insert 5 Duedate records
INSERT INTO duedate (due_date, message, promissory_id) VALUES
('2025-03-15', 'Downpayment due date extended by 2 months as per promissory note.', 1),
('2025-04-01', 'Second semester payment due date.', 2),
('2025-05-01', 'Summer semester payment deadline.', 3),
('2025-05-10', 'Extended payment deadline for special case.', 4),
('2025-06-01', 'Final payment deadline for academic year.', 5);

-- Insert 5 Student Payables records
INSERT INTO student_payables (belong_id, downpayment_amount, amount_paid, remaining_balance, status, duedate_id) VALUES
(1, 25000.00, 25000.00, 0.00, 'PAID', 1),           -- Juan - Fully paid
(2, 27500.00, 15000.00, 12500.00, 'PARTIAL', 2),   -- Maria - Partial payment
(3, 24000.00, 0.00, 24000.00, 'UNPAID', 3),        -- Jose - Unpaid
(4, 26000.00, 26000.00, 0.00, 'PAID', 4),          -- Ana - Fully paid
(5, 30000.00, 10000.00, 20000.00, 'OVERDUE', 5);   -- Carlos - Overdue

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Count records in each table
SELECT 'admin' AS table_name, COUNT(*) AS record_count FROM admin
UNION ALL
SELECT 'school_year', COUNT(*) FROM school_year
UNION ALL
SELECT 'student', COUNT(*) FROM student
UNION ALL
SELECT 'semester', COUNT(*) FROM semester
UNION ALL
SELECT 'belong', COUNT(*) FROM belong
UNION ALL
SELECT 'promissory_note', COUNT(*) FROM promissory_note
UNION ALL
SELECT 'duedate', COUNT(*) FROM duedate
UNION ALL
SELECT 'student_payables', COUNT(*) FROM student_payables;

-- =====================================================
-- END OF SCRIPT
-- =====================================================

