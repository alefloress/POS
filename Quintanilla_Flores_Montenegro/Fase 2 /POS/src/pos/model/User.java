package pos.model;

import java.time.LocalDate;

public class User {
    private String id;          // U0001...
    private String username;    // login
    private String role;        // ADMIN | CAJERO
    private boolean active;
    private LocalDate createdAt;
    private String password;    // simple (sin hash)

    public User(String id, String username, String role, boolean active, LocalDate createdAt, String password) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.password = password;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

