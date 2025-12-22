package models;

public class Admin {
    private int adminId;
    private String username;
    private String passwordHash;
    private String fullname;
    private String email;

    public Admin() {}

    public Admin(String username, String passwordHash, String fullname, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullname = fullname;
        this.email = email;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

