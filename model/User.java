package model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class User {
    private String username;
    private String password;
    private String displayName;
    private String accountNumber;
    private String nic;
    private String status;
    private String mobile = "";
    private String email = "";
    private LocalDateTime lockExpiry;
    private String preferredOTPChannel = "Mobile";
    private int loginAttempts = 0;
    private LocalDateTime lockedUntil;
    private Map<String, Double> accounts = new HashMap<>();
    private Map<String, Double> loans = new HashMap<>();
    private Map<String, Double> creditCards = new HashMap<>();

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getLockExpiry() { return lockExpiry; }
    public void setLockExpiry(LocalDateTime lockExpiry) { this.lockExpiry = lockExpiry; }
    public String getPreferredOTPChannel() { return preferredOTPChannel; }
    public void setPreferredOTPChannel(String channel) { this.preferredOTPChannel = channel; }
    public int getLoginAttempts() { return loginAttempts; }
    public void resetLoginAttempts() { this.loginAttempts = 0; }
    public void incrementLoginAttempts() { this.loginAttempts++; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public void initializeSampleData() {
        accounts.put("Savings", 15000.0);
        loans.put("Personal Loan", 5000.0);
        creditCards.put("Visa Platinum", 2500.0);
    }

    public Map<String, Double> getAccounts() { return accounts; }
    public Map<String, Double> getLoans() { return loans; }
    public Map<String, Double> getCreditCards() { return creditCards; }
}
