-- SQL INSERT Statement with Hashed Password
-- Username: admin
-- Password: admin123 (hashed using SHA-256 with salt)
-- Email: admin
-- Fullname: System Administrator
-- Hash Format: base64(salt):base64(hex_hash)

-- To generate a fresh hash, run: javac QuickHash.java && java QuickHash
-- This will create a new hash each time (salt is random)

-- Example SQL (you need to run QuickHash.java to get a real hash):
-- INSERT INTO admin (username, password_hash, fullname, email) VALUES 
-- ('admin', 'BASE64_SALT:BASE64_HASH', 'System Administrator', 'admin');

-- Run this command to generate the actual SQL:
-- javac QuickHash.java && java QuickHash




