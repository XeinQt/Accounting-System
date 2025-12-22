-- =====================================================
-- SQL INSERT Statement for Admin with Hashed Password
-- =====================================================
-- Username: admin
-- Password: admin123 (hashed)
-- Email: admin
-- Fullname: System Administrator
-- =====================================================

-- INSTRUCTIONS:
-- 1. Run this command to generate the hashed password:
--    java -cp "build/classes" utils.GenerateAdminSQL
--
-- 2. Copy the generated SQL statement below
--
-- OR use this alternative method:
-- The application will automatically hash passwords when you:
-- - Change password in Settings (it will be hashed automatically)
-- - Or run the PasswordMigration utility
--
-- =====================================================

-- For direct database insertion, you need to run the Java utility first.
-- Here's a sample format (replace <HASHED_PASSWORD> with actual hash):

INSERT INTO admin (username, password_hash, fullname, email) 
VALUES ('admin', '<HASHED_PASSWORD_HERE>', 'System Administrator', 'admin');

-- =====================================================
-- Quick Method: Just insert with plain text, the app will hash it on first login
-- (But this is NOT recommended for production)
-- =====================================================
-- INSERT INTO admin (username, password_hash, fullname, email) 
-- VALUES ('admin', 'admin123', 'System Administrator', 'admin');
-- 
-- Then run the application once, and PasswordMigration will hash it automatically.
-- =====================================================
