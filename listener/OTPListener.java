package listener;

public interface OTPListener {
    void sendOTP(String userId, String otp, String channel);
}
