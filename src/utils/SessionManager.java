package utils;

import dao.SchoolYearDAO;
import models.SchoolYear;

/**
 * Simple session manager to store current logged-in admin and selected school year
 * In a production app, use a more secure session management
 */
public class SessionManager {
    private static String currentUsername = null;
    private static Integer currentAdminId = null;
    private static Integer selectedSchoolYearId = null;
    
    public static void setCurrentUser(String username, Integer adminId) {
        currentUsername = username;
        currentAdminId = adminId;
    }
    
    public static String getCurrentUsername() {
        return currentUsername;
    }
    
    public static Integer getCurrentAdminId() {
        return currentAdminId;
    }
    
    public static void setSelectedSchoolYearId(Integer schoolYearId) {
        selectedSchoolYearId = schoolYearId;
    }
    
    public static Integer getSelectedSchoolYearId() {
        return selectedSchoolYearId;
    }
    
    /**
     * Initialize selected school year to the newest active one if not set
     */
    public static void initializeSelectedSchoolYear() {
        if (selectedSchoolYearId == null) {
            SchoolYearDAO schoolYearDAO = new SchoolYearDAO();
            java.util.List<SchoolYear> schoolYears = schoolYearDAO.getActiveSchoolYears();
            if (!schoolYears.isEmpty()) {
                // Get the newest active school year (they're sorted by newest first in the DAO)
                selectedSchoolYearId = schoolYears.get(0).getSchoolYearId();
            }
        }
    }
    
    public static void clearSession() {
        currentUsername = null;
        currentAdminId = null;
        selectedSchoolYearId = null;
    }
    
    public static boolean isLoggedIn() {
        return currentUsername != null;
    }
}

