-- =====================================================
-- DORPAY Accounting System - Database Cleanup Script (TRUNCATE Version)
-- Faster method using TRUNCATE (requires foreign key checks disabled)
-- =====================================================

USE accounting_system;

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- Truncate all tables (faster than DELETE)
-- Order doesn't matter when foreign key checks are disabled
TRUNCATE TABLE student_payables;
TRUNCATE TABLE belong;
TRUNCATE TABLE duedate;
TRUNCATE TABLE promissory_note;
TRUNCATE TABLE student;
TRUNCATE TABLE semester;
TRUNCATE TABLE school_year;
TRUNCATE TABLE admin;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Database cleaned successfully using TRUNCATE! All tables are now empty.' AS Status;

