package utils;

/**
 * Database configuration for MySQL connection
 * Update these values according to your MySQL server setup
 */
public class DatabaseConfig {
    // MySQL connection details
    public static final String DB_HOST = "localhost";
    public static final String DB_PORT = "3306";
    public static final String DB_NAME = "accounting_system";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "";
    
    // JDBC URL
    public static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + 
                                       "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    
    // MySQL JDBC Driver
    public static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
}

