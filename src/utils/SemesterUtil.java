package utils;

import java.time.Month;

/**
 * Utility class for semester-related operations
 */
public class SemesterUtil {
    
    /**
     * Get semester based on current month
     * 1st Sem: August (8), September (9), October (10), November (11), December (12)
     * 2nd Sem: January (1), February (2), March (3), April (4), May (5)
     * Summer Sem: June (6), July (7)
     * 
     * @return The semester string ("1st Sem", "2nd Sem", or "Summer Sem")
     */
    public static String getSemesterByCurrentMonth() {
        return getSemesterByMonth(java.time.LocalDate.now().getMonth());
    }
    
    /**
     * Get semester based on a specific month
     * 1st Sem: August (8), September (9), October (10), November (11), December (12)
     * 2nd Sem: January (1), February (2), March (3), April (4), May (5)
     * Summer Sem: June (6), July (7)
     * 
     * @param month The month to check
     * @return The semester string ("1st Sem", "2nd Sem", or "Summer Sem")
     */
    public static String getSemesterByMonth(Month month) {
        int monthValue = month.getValue();
        
        // 1st Sem: August (8), September (9), October (10), November (11), December (12)
        if (monthValue >= 8 && monthValue <= 12) {
            return "1st Sem";
        }
        // 2nd Sem: January (1), February (2), March (3), April (4), May (5)
        else if (monthValue >= 1 && monthValue <= 5) {
            return "2nd Sem";
        }
        // Summer: June (6), July (7)
        else if (monthValue >= 6 && monthValue <= 7) {
            return "Summer Sem";
        }
        
        // Default fallback
        return "1st Sem";
    }
}

