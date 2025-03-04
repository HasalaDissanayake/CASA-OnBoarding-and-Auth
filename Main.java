import config.AppConfig;
import factory.UserFactory;
import listener.OTPListener;
import model.LoginAttempt;
import model.User;
import repository.UserDatabase;
import service.AuthService;
import service.BankingService;
import service.OTPService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static BankingService bankingService;
    private static OTPService otpService;
    private static UserDatabase userDatabase;
    private static AuthService authService;

    public static void main(String[] args) {
        initializeServices();
        showMainMenu();
    }

    private static void initializeServices() {
        userDatabase = UserDatabase.getInstance();
        // Define an OTPListener to simulate OTP delivery.
        OTPListener otpListener = (userId, otp, channel) ->
                System.out.println("\n[System] OTP " + otp + " to " + userId + " via " + channel);

        otpService = new OTPService();
        otpService.setOtpListener(otpListener);
        UserFactory userFactory = new UserFactory();
        bankingService = new BankingService(userFactory, userDatabase, otpService);
        authService = new AuthService(userDatabase);
    }

    private static void showMainMenu() {
        while (true) {
            System.out.println("\n=== Serendib Digital Banking ===");
            System.out.println("1. New Customer Onboarding");
            System.out.println("2. Existing Customer Login");
            System.out.println("3. Exit");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> handleOnboarding();
                case "2" -> handleLogin();
                case "3" -> System.exit(0);
                default -> System.out.println("Invalid option!");
            }
        }
    }

    private static void handleOnboarding() {
        System.out.println("\n=== New Customer Onboarding ===");

        // Step 1: Language Selection
        String language = promptUntilValid("Choose language (1. English): ",
                "Invalid choice", input -> input.matches("[1]"));
        System.out.println("Selected language: English");

        // Step 2: NIC/PP Validation
        String nic = promptUntilValid(
                "Enter NIC/Passport number (e.g., 12345678V, 123456789012, P1234567, OL1234567, D1234567): ",
                "Invalid NIC/Passport format",
                input -> input.matches("^(?:\\d{12}|\\d{9}[Vv]|(?:[Pp]|[Oo][Ll]|[Dd])\\d{7})$")
        );

        if (authService.isOnboardingUserLocked(nic)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            System.out.println("Error: Onboarding is Temporary Locked until " + authService.getOnboardingUserLockedTime(nic).format(formatter) + " for this user! Redirecting to login...");
            return;
        }

        // Step 3: Account Validation
        String account = promptUntilValid("Enter CASA account number (format ACCXXXXXX): ",
                "Invalid account format", input -> input.matches("ACC\\d{6}"));

        // Existing customer check
        if (userDatabase.findByNic(nic) != null) {
            System.out.println("Error: Account already registered! Redirecting to login...");
            return;
        }

        // Step 4: Terms & Conditions
        System.out.println("\nTerms & Conditions: " + AppConfig.TERMS_CONDITIONS_URL);
        String accept = promptUntilValid("Do you accept the terms? (Y/N): ",
                "Please enter Y/N", input -> input.matches("[YyNn]"));

        if (!accept.equalsIgnoreCase("Y")) {
            System.out.println("Onboarding cancelled. Terms not accepted.");
            return;
        }

        // Step 5: Contact Information
        String mobile;
        String email;
        do {
            mobile = promptUntilValid("Enter mobile number (optional): ",
                    "Invalid mobile number format (must be 10 digits or empty)",
                    input -> input.matches("(^$|\\d{10})"));

            email = promptUntilValid("Enter email (optional): ",
                    "Invalid email format (must be valid email or empty)",
                    input -> input.matches("(^$|\\S+@\\S+\\.\\S+)"));

            if (mobile.isEmpty() && email.isEmpty()) {
                System.out.println("At least one contact method (mobile or email) is required!");
            }
        } while (mobile.isEmpty() && email.isEmpty());


        // Step 6: OTP Handling (pre-onboarding verification)
        if (!handleOTPVerification(nic, mobile, email)) return;

        // Step 7: Verification Method
        System.out.println("\nChoose verification method:");
        System.out.println("1. Contact Call Center");
        System.out.println("2. Visit Branch");
        String method = promptUntilValid("Select (1-2): ",
                "Invalid choice", input -> input.matches("[12]"));

        handleVerificationMethod(method, mobile, email);

        // Step 8: Credentials Creation
        String username = handleUsernameCreation();
        String password = handlePasswordCreation();
        String displayName = promptUntilValid("Enter display name: ",
                "Name cannot be empty", input -> !input.trim().isEmpty());

        // Finalize onboarding
        try {
            User user = bankingService.onboardUser(nic, account, username, password, displayName);
            user.setMobile(mobile);
            user.setEmail(email);
            user.setPreferredOTPChannel(mobile.isEmpty() ? email : mobile);
            System.out.println("\n=== Onboarding Successful ===");
            System.out.println("Welcome " + user.getDisplayName());
            handleLogin();
        } catch (IllegalArgumentException e) {
            System.out.println("Onboarding failed: " + e.getMessage());
        }
    }

    private static boolean handleOTPVerification(String userId, String mobile, String email) {
        // For onboarding, the user may not be registered yet.
        User user = userDatabase.findByUsername(userId);
        boolean bothAvailable = !mobile.isEmpty() && !email.isEmpty();
        String defaultChannel = !mobile.isEmpty() ? "mobile" : (!email.isEmpty() ? "email" : "none");

        if (bothAvailable) {
            otpService.generateOTP(user, userId, "mobile", true);
        } else {
            otpService.generateOTP(user, userId, defaultChannel, false);
        }

        for (int attempts = 1; attempts <= AppConfig.OTP_ATTEMPTS_LIMIT; attempts++) {

            System.out.print("Enter OTP (Attempt " + attempts + "/" + AppConfig.OTP_ATTEMPTS_LIMIT + "): ");
            String otp = scanner.nextLine().trim();
            if (!otp.matches("\\d{6}")) {
                System.out.println("Invalid OTP format!");
                continue;
            }
            if (otpService.isOTPExpired(userId)) {
                return false;
            }
            if (otpService.validateOTP(userId, otp)) {
                return true;
            }
            System.out.println("Invalid OTP!");
        }
        authService.lockOnboardingUser(userId);
        System.out.println("Maximum attempts reached. Account locked for " +
                AppConfig.OTP_LOCK_DURATION_HOURS + " hours.");
        return false;
    }


    private static void handleVerificationMethod(String method, String mobile, String email) {
        System.out.println("\nVerification instructions:");
        if (method.equals("1")) {
            System.out.println("Please call our 24/7 support center at +94 11 123 4567");
            System.out.println("Have your NIC/Passport and account details ready");
        } else {
            System.out.println("Visit any Serendib Bank branch with:");
            System.out.println("- Original NIC/Passport");
            System.out.println("- Account statement (if available)");
        }
        String channel;
        if (!mobile.isEmpty() && !email.isEmpty()) {
            channel = "mobile";
        } else if (!mobile.isEmpty()) {
            channel = "mobile";
        } else if (!email.isEmpty()) {
            channel = "email";
        } else {
            channel = "none";
        }

        if (!channel.equals("none")) {
            System.out.println("\n[System] Verification confirmation sent via " + channel + ".");
        }
    }

    private static String handleUsernameCreation() {
        while (true) {
            String base = promptUntilValid("Create username (4-12 alphanumeric chars): ",
                    "Invalid format", input -> input.matches("\\w{4,12}"));

            try {
                return bankingService.userFactory.createSuggestedUsername(base);
            } catch (IllegalArgumentException e) {
                String suggestions = bankingService.userFactory.suggestUsername(base);
                System.out.println("Username taken! Suggestions: " + suggestions);
                System.out.print("Choose suggestion (1-3) or enter new username: ");
                String choice = scanner.nextLine();

                if (choice.matches("[1-3]")) {
                    return suggestions.split(", ")[Integer.parseInt(choice) - 1];
                }
            }
        }
    }

    private static String handlePasswordCreation() {
        while (true) {
            String password = promptUntilValid("Create password (min 8 chars, mix letters/numbers): ",
                    "Invalid format", input -> input.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$"));

            String confirm = promptUntilValid("Confirm password: ",
                    "Must match password", input -> input.equals(password));

            if (password.equals(confirm)) return password;
            System.out.println("Passwords do not match!");
        }
    }

    private static void handleLogin() {
        System.out.println("\n=== Secure Login ===");
        String username = promptUntilValid("Username: ", "Required field", input -> !input.isEmpty());

        System.out.println("\nSelect an option:");
        System.out.println("1. Enter Password");
        System.out.println("2. Forgot Password");
        String choice = promptUntilValid("Select (1-2): ",
                "Invalid choice", input -> input.matches("[12]"));

        if ("2".equals(choice)) {
            handlePasswordReset(username);
            return;
        }
        String password = promptUntilValid("Password (min 8 chars, mix letters/numbers): ", "Invalid format", input -> input.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$"));

        User user = authService.validateCredentials(username, password);
        userDatabase.logLoginAttempt(new LoginAttempt(username, user != null));

        if (user == null) {
            System.out.println("Invalid credentials or account locked");
            return;
        }

        System.out.println("\n=== Two-Factor Authentication ===");

        String otpChannel = "";
        if (!user.getMobile().isEmpty() && !user.getEmail().isEmpty()) {
            System.out.println("Select OTP channel:");
            System.out.println("1. Mobile (" + user.getMobile() + ")");
            System.out.println("2. Email (" + user.getEmail() + ")");
            System.out.print("Choice: ");
            String channelChoice = scanner.nextLine().trim();
            if ("1".equals(channelChoice)) {
                otpChannel = "mobile";
            } else if ("2".equals(channelChoice)) {
                otpChannel = "email";
            } else {
                System.out.println("Invalid choice. Defaulting to mobile.");
                otpChannel = "mobile";
            }
        } else if (!user.getMobile().isEmpty()) {
            otpChannel = "mobile";
        } else if (!user.getEmail().isEmpty()) {
            otpChannel = "email";
        } else {
            System.out.println("No contact method available for OTP.");
            return;
        }

        otpService.generateOTP(user, user.getUsername(), otpChannel, false);

        if (handleOTPVerification(user)) {
            System.out.println("\n=== Login Successful ===");
            user.initializeSampleData();
            showDashboard(user);
        } else {
            System.out.println("Login failed due to OTP validation errors");
        }
    }

    private static boolean handleOTPVerification(User user) {
        for (int attempt = 1; attempt <= AppConfig.OTP_ATTEMPTS_LIMIT; attempt++) {
            System.out.print("Enter OTP (Attempt " + attempt + "/" + AppConfig.OTP_ATTEMPTS_LIMIT + "): ");
            String otp = scanner.nextLine().trim();

            if (!otp.matches("\\d{6}")) {
                System.out.println("Invalid OTP format.");
                continue;
            }

            if (otpService.isOTPExpired(user)) {
                return false;
            }

            if (otpService.validateOTP(user, otp)) {
                return true;
            }
            System.out.println("Invalid OTP.");
        }
        authService.lockUser(user);
        System.out.println("Maximum attempts reached. Account locked for " +
                AppConfig.OTP_LOCK_DURATION_HOURS + " hours.");
        return false;
    }

    private static void showDashboard(User user) {
        System.out.println("\n=== Financial Dashboard ===");
        System.out.println("Account Balances:");
        user.getAccounts().forEach((name, balance) ->
                System.out.printf("- %s: $%.2f%n", name, balance));

        System.out.println("\nActive Loans:");
        user.getLoans().forEach((name, amount) ->
                System.out.printf("- %s: $%.2f%n", name, amount));

        System.out.println("\nCredit Cards:");
        user.getCreditCards().forEach((name, limit) ->
                System.out.printf("- %s: $%.2f available%n", name, limit));

        promptUntilValid("\nPress Enter to logout...", "", input -> true);
        return;
    }

    private static void handlePasswordReset(String username) {
        User user = userDatabase.findByUsername(username);
        if (user == null) {
            System.out.println("User not found");
            return;
        }

        System.out.println("\n=== Password Reset ===");
        String channel = user.getEmail().isEmpty() ? "Mobile" : "Email";

        authService.generatePasswordResetToken(username, channel);

        String inputToken = promptUntilValid("Enter reset token: ",
                "6 digits required", input -> input.matches("\\d{6}"));

        if(!authService.isPasswordResetTokenMatch(username, inputToken)) {
            System.out.println("Invalid Reset Token");
            return;
        }

        if(authService.isPasswordResetTokenExpired(username, inputToken)) {
            System.out.println("Reset Token Expired");
            return;
        }

        String newPassword = handlePasswordCreation();
        user.setPassword(newPassword);
        System.out.println("Password reset successful");
    }

    private static String promptUntilValid(String prompt, String errorMessage, java.util.function.Predicate<String> validator) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (validator.test(input)) return input;
            System.out.println("Error: " + errorMessage);
        }
    }
}
