-- =====================================================
-- DORPAY Accounting System - Database Cleanup Script
-- Safely deletes all data from all tables in correct order
-- =====================================================

USE accounting_system;

-- Disable foreign key checks temporarily for faster deletion
SET FOREIGN_KEY_CHECKS = 0;

-- Delete in order: child tables first, then parent tables

-- 1. Delete from student_payables (references belong and duedate)
DELETE FROM student_payables;

-- 2. Delete from belong (references student, school_year, semester)
DELETE FROM belong;

-- 3. Delete from duedate (references promissory_note)
DELETE FROM duedate;

-- 4. Delete from promissory_note (no dependencies)
DELETE FROM promissory_note;

-- 5. Delete from student (references school_year, but ON DELETE SET NULL)
DELETE FROM student;

-- 6. Delete from semester (no dependencies)
DELETE FROM semester;

-- 7. Delete from school_year (parent of student and belong)
DELETE FROM school_year;

-- 8. Delete from admin (no dependencies)
DELETE FROM admin;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Reset AUTO_INCREMENT counters
ALTER TABLE admin AUTO_INCREMENT = 1;
ALTER TABLE school_year AUTO_INCREMENT = 1;
ALTER TABLE student AUTO_INCREMENT = 1;
ALTER TABLE semester AUTO_INCREMENT = 1;
ALTER TABLE belong AUTO_INCREMENT = 1;
ALTER TABLE promissory_note AUTO_INCREMENT = 1;
ALTER TABLE duedate AUTO_INCREMENT = 1;
ALTER TABLE student_payables AUTO_INCREMENT = 1;

SELECT 'Database cleaned successfully! All tables are now empty.' AS Status;

