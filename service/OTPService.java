package service;

import config.AppConfig;
import listener.OTPListener;
import model.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class OTPService {
    private Map<String, String> otpStore = new HashMap<>();
    private Map<String, LocalDateTime> otpExpiry = new HashMap<>();
    private OTPListener otpListener;

    public void setOtpListener(OTPListener listener) {
        this.otpListener = listener;
    }

    // Main generateOTP method supporting cases when user may be null (e.g., pre-onboarding).
    public void generateOTP(User user, String username, String channel, Boolean sendBoth) {
        String key = (user != null ? user.getUsername() : username);
        String otp = String.format("%06d", new Random().nextInt(1000000));
        otpStore.put(key, otp);
        otpExpiry.put(key, LocalDateTime.now().plusSeconds(AppConfig.OTP_VALIDITY_SECONDS));
        String usedChannel = (user != null && user.getPreferredOTPChannel() != null && !user.getPreferredOTPChannel().isEmpty())
                ? user.getPreferredOTPChannel() : channel;
        if (otpListener != null) {
            if (sendBoth) {
                otpListener.sendOTP(key, otp, usedChannel);
                otpListener.sendOTP(key, otp, Objects.equals(usedChannel, "mobile") ? "email" : "mobile");
            } else {
                otpListener.sendOTP(key, otp, usedChannel);
            }
        } else {
            if (sendBoth) {
                System.out.println("\n[System] OTP " + otp + " to " + username + " via " + usedChannel);
                System.out.println("\n[System] OTP " + otp + " to " + username + " via " + (Objects.equals(usedChannel, "mobile") ? "email" : "mobile"));
            } else {
                System.out.println("\n[System] OTP " + otp + " to " + username + " via " + usedChannel);
            }
        }
        System.out.println("OTP valid for " + AppConfig.OTP_VALIDITY_SECONDS + " seconds");
    }

    public boolean validateOTP(User user, String enteredOtp) {
        if (user == null) return false;
        String key = user.getUsername();

        return otpStore.getOrDefault(key, "").equals(enteredOtp);
    }

    public boolean isOTPExpired(User user) {
        if (user == null) return true;
        LocalDateTime expiry = otpExpiry.get(user.getUsername());
        if (expiry == null || LocalDateTime.now().isAfter(expiry)) {
            System.out.println("OTP expired!");
            return true;
        }
        return false;
    }

    // Validate OTP using a key (for cases where User is not yet available).
    public boolean validateOTP(String userId, String enteredOtp) {
        return enteredOtp.equals(otpStore.getOrDefault(userId, ""));
    }

    public boolean isOTPExpired(String userId) {
        LocalDateTime expiry = otpExpiry.get(userId);
        if (expiry == null || LocalDateTime.now().isAfter(expiry)) {
            System.out.println("OTP expired!");
            return true;
        }
        return false;
    }
}
