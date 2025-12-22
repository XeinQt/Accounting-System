package models;

public class SchoolYear {
    private int schoolYearId;
    private String yearRange;
    private boolean isActive = true; // Default to active

    public SchoolYear() {}

    public SchoolYear(String yearRange) {
        this.yearRange = yearRange;
        this.isActive = true;
    }

    public int getSchoolYearId() {
        return schoolYearId;
    }

    public void setSchoolYearId(int schoolYearId) {
        this.schoolYearId = schoolYearId;
    }

    public String getYearRange() {
        return yearRange;
    }

    public void setYearRange(String yearRange) {
        this.yearRange = yearRange;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}

