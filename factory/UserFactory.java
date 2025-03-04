package factory;

import model.User;
import repository.UserDatabase;

import java.util.ArrayList;
import java.util.List;

public class UserFactory {
    public User createUser(String nic, String accountNumber, String username, String password, String displayName) {
        UserDatabase db = UserDatabase.getInstance();
        if (db.findByUsername(username) != null)
            throw new IllegalArgumentException("Username taken");

        User user = new User();
        user.setNic(nic);
        user.setAccountNumber(accountNumber);
        user.setUsername(username);
        user.setPassword(password);
        user.setDisplayName(displayName);
        user.setStatus("INITIATED");
        return user;
    }

    public String suggestUsername(String base) {
        List<String> suggestions = new ArrayList<>();
        int suffix = 1;
        UserDatabase db = UserDatabase.getInstance();

        while (suggestions.size() < 3) {
            String potential = base + suffix;

            if (db.findByUsername(potential) == null) {
                suggestions.add(potential);
            }
            suffix++;
        }
        return String.join(", ", suggestions);
    }

    public String createSuggestedUsername(String base) {
        UserDatabase db = UserDatabase.getInstance();
        if (db.findByUsername(base) == null) return base;
        throw new IllegalArgumentException("Username taken");
    }
}
