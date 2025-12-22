package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtil {
    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        try {
            // Load MySQL JDBC driver
            Class.forName(DatabaseConfig.DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found. Please add mysql-connector-j-X.X.X.jar to your project libraries.", e);
        }
        
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD
            );
        }
        return connection;
    }

    public static void createDatabaseIfNotExists() {
        try {
            // Connect without database to create it
            String urlWithoutDB = "jdbc:mysql://" + DatabaseConfig.DB_HOST + ":" + DatabaseConfig.DB_PORT + 
                                 "/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            Class.forName(DatabaseConfig.DRIVER_CLASS);
            
            try (Connection conn = DriverManager.getConnection(urlWithoutDB, DatabaseConfig.DB_USER, DatabaseConfig.DB_PASSWORD);
                 Statement stmt = conn.createStatement()) {
                
                // Create database if it doesn't exist
                stmt.execute("CREATE DATABASE IF NOT EXISTS " + DatabaseConfig.DB_NAME);
                System.out.println("Database '" + DatabaseConfig.DB_NAME + "' checked/created successfully!");
            }
        } catch (Exception e) {
            System.err.println("Error creating database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void initializeDatabase() {
        try {
            // First, create database if it doesn't exist
            createDatabaseIfNotExists();
            
            // Then create tables
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Create Admin table
                stmt.execute("CREATE TABLE IF NOT EXISTS admin (" +
                        "admin_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "username VARCHAR(50) UNIQUE NOT NULL," +
                        "password_hash VARCHAR(255) NOT NULL," +
                        "fullname VARCHAR(100) NOT NULL," +
                        "email VARCHAR(100)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                
                // Create SCHOOL_YEAR table
                stmt.execute("CREATE TABLE IF NOT EXISTS school_year (" +
                        "school_year_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "year_range VARCHAR(20) NOT NULL UNIQUE," +
                        "is_active BOOLEAN DEFAULT TRUE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                
                // Add is_active column if it doesn't exist (for existing databases)
                try {
                    // Check if column exists first by trying to query it
                    java.sql.DatabaseMetaData metaData = conn.getMetaData();
                    try (java.sql.ResultSet rs = metaData.getColumns(null, null, "school_year", "is_active")) {
                        if (!rs.next()) {
                            // Column doesn't exist, add it
                            stmt.execute("ALTER TABLE school_year ADD COLUMN is_active BOOLEAN DEFAULT TRUE");
                        }
                    }
                } catch (SQLException e) {
                    // Column already exists or table doesn't exist yet, ignore
                    // This is expected for new databases or if column already exists
                }
                
                // Create STUDENT table
                stmt.execute("CREATE TABLE IF NOT EXISTS student (" +
                        "student_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "student_number VARCHAR(50) UNIQUE NOT NULL," +
                        "fullname VARCHAR(100) NOT NULL," +
                        "major VARCHAR(100)," +
                        "year VARCHAR(20)," +
                        "dep VARCHAR(100)," +
                        "college VARCHAR(100)," +
                        "school_year_id INT," +
                        "FOREIGN KEY (school_year_id) REFERENCES school_year(school_year_id) ON DELETE SET NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                
                // Add new columns if they don't exist (for schema migration)
                try {
                    // Add first_name, middle_name, last_name columns if they don't exist
                    stmt.execute("ALTER TABLE student ADD COLUMN IF NOT EXISTS first_name VARCHAR(100)");
                    stmt.execute("ALTER TABLE student ADD COLUMN IF NOT EXISTS middle_name VARCHAR(100)");
                    stmt.execute("ALTER TABLE student ADD COLUMN IF NOT EXISTS last_name VARCHAR(100)");
                    
                    // Add status column if it doesn't exist
                    stmt.execute("ALTER TABLE student ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'active'");
                    
                    // Update existing records to have 'active' status if they are NULL
                    stmt.execute("UPDATE student SET status = 'active' WHERE status IS NULL OR status = ''");
                } catch (SQLException e) {
                    // Column might already exist, or MySQL version doesn't support IF NOT EXISTS
                    // Try without IF NOT EXISTS for older MySQL versions
                    try {
                        stmt.execute("ALTER TABLE student ADD COLUMN first_name VARCHAR(100)");
                    } catch (SQLException e2) {
                        // Column exists, ignore
                    }
                    try {
                        stmt.execute("ALTER TABLE student ADD COLUMN middle_name VARCHAR(100)");
                    } catch (SQLException e2) {
                        // Column exists, ignore
                    }
                    try {
                        stmt.execute("ALTER TABLE student ADD COLUMN last_name VARCHAR(100)");
                    } catch (SQLException e2) {
                        // Column exists, ignore
                    }
                    try {
                        stmt.execute("ALTER TABLE student ADD COLUMN status VARCHAR(20) DEFAULT 'active'");
                        stmt.execute("UPDATE student SET status = 'active' WHERE status IS NULL OR status = ''");
                    } catch (SQLException e2) {
                        // Column exists, ignore
                    }
                }
                
                // Create SEMESTER table
                stmt.execute("CREATE TABLE IF NOT EXISTS semester (" +
                        "semester_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "first_sem_amount DECIMAL(10,2) DEFAULT 0.00," +
                        "second_sem_amount DECIMAL(10,2) DEFAULT 0.00," +
                        "summer_sem_amount DECIMAL(10,2) DEFAULT 0.00" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                
                // Create Belong table (associative entity)
                stmt.execute("CREATE TABLE IF NOT EXISTS belong (" +
                        "belong_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "student_id INT NOT NULL," +
                        "school_year_id INT NOT NULL," +
                        "semester_id INT NOT NULL," +
                        "status VARCHAR(20) DEFAULT 'active'," +
                        "FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (school_year_id) REFERENCES school_year(school_year_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (semester_id) REFERENCES semester(semester_id) ON DELETE CASCADE," +
                        "UNIQUE KEY unique_belong (student_id, school_year_id, semester_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                
                // Add status column to belong table if it doesn't exist (for existing databases)
                try {
                    stmt.execute("ALTER TABLE belong ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'active'");
                    stmt.execute("UPDATE belong SET status = 'active' WHERE status IS NULL OR status = ''");
                } catch (SQLException e) {
                    // Column might already exist, or MySQL version doesn't support IF NOT EXISTS
                    try {
                        stmt.execute("ALTER TABLE belong ADD COLUMN status VARCHAR(20) DEFAULT 'active'");
                        stmt.execute("UPDATE belong SET status = 'active' WHERE status IS NULL OR status = ''");
                    } catch (SQLException e2) {
                        // Column exists, ignore
                    }
                }
                
                // Create PROMISSORY_NOTE table
                stmt.execute("CREATE TABLE IF NOT EXISTS promissory_note (" +
                        "promissory_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "created_date DATE NOT NULL," +
                        "due_date_extended DATE," +
                        "remaining_balance_snapshot DECIMAL(10,2) NOT NULL," +
                        "note_text TEXT" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                
                // Create Duedate table
                stmt.execute("CREATE TABLE IF NOT EXISTS duedate (" +
                        "duedate_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "due_date DATE NOT NULL," +
                        "message TEXT," +
                        "promissory_id INT," +
                        "FOREIGN KEY (promissory_id) REFERENCES promissory_note(promissory_id) ON DELETE SET NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                
                // Create STUDENT_PAYABLES table
                stmt.execute("CREATE TABLE IF NOT EXISTS student_payables (" +
                        "payable_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "belong_id INT NOT NULL," +
                        "downpayment_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00," +
                        "amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0.00," +
                        "remaining_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00," +
                        "status ENUM('UNPAID', 'PARTIAL', 'PAID', 'OVERDUE') DEFAULT 'UNPAID'," +
                        "duedate_id INT," +
                        "FOREIGN KEY (belong_id) REFERENCES belong(belong_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (duedate_id) REFERENCES duedate(duedate_id) ON DELETE SET NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                
                // Insert default admin user if not exists (password will be hashed)
                // Note: Password will be hashed on first login or can be updated via migration script
                stmt.execute("INSERT IGNORE INTO admin (username, password_hash, fullname, email) VALUES " +
                        "('admin', 'admin123', 'Administrator', 'admin@dorpay.com')");
                
                // Insert default school year if not exists
                stmt.execute("INSERT IGNORE INTO school_year (year_range) VALUES " +
                        "('2025-2026'), ('2024-2025'), ('2023-2024')");
                
                System.out.println("Database tables initialized successfully!");
                
                // Initialize triggers, views, and functions
                DatabaseObjectsUtil.initializeDatabaseObjects(conn);
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database. Please check your MySQL connection settings.", e);
        }
    }
    

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    
    /**
     * Test database connection
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            boolean isValid = conn != null && !conn.isClosed();
            if (isValid) {
                System.out.println("Database connection successful!");
            }
            return isValid;
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return false;
        }
    }
}
