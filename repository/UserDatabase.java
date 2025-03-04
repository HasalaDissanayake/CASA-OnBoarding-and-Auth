package repository;

import model.LoginAttempt;
import model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDatabase {
    private static UserDatabase instance;
    private Map<String, User> usersByUsername = new HashMap<>();
    private Map<String, User> usersByNic = new HashMap<>();
    private Map<String, User> usersByAccount = new HashMap<>();
    private List<LoginAttempt> loginHistory = new ArrayList<>();

    private UserDatabase() {}

    public static synchronized UserDatabase getInstance() {
        if (instance == null) instance = new UserDatabase();
        return instance;
    }

    public void addUser(User user) {
        usersByUsername.put(user.getUsername().toLowerCase(), user);
        usersByNic.put(user.getNic(), user);
        usersByAccount.put(user.getAccountNumber(), user);
    }

    public User findByUsername(String username) {
        return usersByUsername.get(username.toLowerCase());
    }

    public User findByNic(String nic) {
        return usersByNic.get(nic);
    }

    public void logLoginAttempt(LoginAttempt attempt) {
        loginHistory.add(attempt);
    }

    public List<LoginAttempt> getLoginHistory() {
        return new ArrayList<>(loginHistory);
    }
}
