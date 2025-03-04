package service;

import config.AppConfig;
import model.User;
import repository.UserDatabase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthService {
    private final UserDatabase userDatabase;

    private List<String> lockedOnBoardingUser = new ArrayList<>();

    private Map<String, LocalDateTime> onboardingLockedTime = new HashMap<>();
    private Map<String, Map<String, LocalDateTime>> passwordResetToken = new HashMap<>();


    public AuthService(UserDatabase userDatabase) {
        this.userDatabase = userDatabase;
    }

    public User validateCredentials(String username, String password) {
        User user = userDatabase.findByUsername(username);
        if (user == null) return null;

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            System.out.println("Account locked until " + user.getLockedUntil());
            return null;
        }

        if (user.getPassword().equals(password)) {
            user.resetLoginAttempts();
            return user;
        }

        return null;
    }

    public void lockOnboardingUser(String userId) {
        lockedOnBoardingUser.add(userId);
        onboardingLockedTime.put(userId, LocalDateTime.now().plusHours(AppConfig.OTP_LOCK_DURATION_HOURS));
    }

    public boolean isOnboardingUserLocked(String userId) {
        return lockedOnBoardingUser.contains(userId);
    }

    public LocalDateTime getOnboardingUserLockedTime(String userId) {
        return onboardingLockedTime.get(userId);
    }

    public void lockUser(User user) {
        user.setLockedUntil(LocalDateTime.now().plusHours(AppConfig.OTP_LOCK_DURATION_HOURS));
    }

    public void generatePasswordResetToken(String username, String channel) {
        String token = String.format("%06d", (int) (Math.random() * 999999));
        Map<String, LocalDateTime> tokenInfo = new HashMap<>();
        tokenInfo.put(token, LocalDateTime.now().plusMinutes(AppConfig.RESET_TOKEN_VALIDITY_MINUTES));
        passwordResetToken.put(username,tokenInfo);
        System.out.println("[System] Reset token: " + token + " to " + username + " via " + channel);
        System.out.println("Reset Token valid for " + AppConfig.RESET_TOKEN_VALIDITY_MINUTES + " minutes");
    }

    public boolean isPasswordResetTokenMatch(String username, String inputToken) {
        if (!passwordResetToken.containsKey(username)) {
            return false;
        }
        Map<String, LocalDateTime> tokenInfo = passwordResetToken.get(username);
        return tokenInfo.containsKey(inputToken);
    }

    public boolean isPasswordResetTokenExpired(String username, String token) {
        if (!passwordResetToken.containsKey(username)) {
            return true;
        }
        Map<String, LocalDateTime> tokenInfo = passwordResetToken.get(username);
        if (!tokenInfo.containsKey(token)) {
            return true;
        }
        LocalDateTime expiryTime = tokenInfo.get(token);
        return LocalDateTime.now().isAfter(expiryTime);
    }
}

