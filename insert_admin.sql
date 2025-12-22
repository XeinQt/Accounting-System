-- =====================================================
-- SQL INSERT Statement for Admin Account
-- =====================================================
-- Username: admin
-- Password: admin123 (will be automatically hashed by the application)
-- Email: admin
-- Fullname: System Administrator
-- =====================================================

-- OPTION 1: Insert with plain text password
-- The application will automatically hash it when you:
-- 1. Start the application (PasswordMigration runs automatically)
-- 2. Or login for the first time
-- 3. Or change the password in Settings

INSERT INTO admin (username, password_hash, fullname, email) 
VALUES ('admin', 'admin123', 'System Administrator', 'admin');

-- =====================================================
-- OPTION 2: Generate hashed password first, then insert
-- =====================================================
-- To get a pre-hashed password, run this command in your project directory:
--
-- java -cp "build/classes" utils.QuickHashGenerator
--
-- Then copy the generated SQL statement and use it instead.
-- =====================================================

-- After inserting, the password will be automatically hashed on next application startup
-- You can verify by checking the password_hash column - it should contain a ":" separator
-- Format: base64(salt):base64(hash)
