package model;

import java.time.LocalDateTime;

public class LoginAttempt {
    private String username;
    private LocalDateTime timestamp;
    private boolean success;

    public LoginAttempt(String username, boolean success) {
        this.username = username;
        this.timestamp = LocalDateTime.now();
        this.success = success;
    }

    public String getUsername() { return username; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isSuccess() { return success; }
}
