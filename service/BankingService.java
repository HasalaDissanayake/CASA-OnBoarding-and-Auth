package service;

import factory.UserFactory;
import model.User;
import repository.UserDatabase;

public class BankingService {
    public UserFactory userFactory;
    public UserDatabase userDatabase;
    private OTPService otpService;

    public BankingService(UserFactory userFactory, UserDatabase userDatabase, OTPService otpService) {
        this.userFactory = userFactory;
        this.userDatabase = userDatabase;
        this.otpService = otpService;
    }

    public User onboardUser(String nic, String accountNumber, String username, String password, String displayName) {
        User user = userFactory.createUser(nic, accountNumber, username, password, displayName);
        userDatabase.addUser(user);
        return user;
    }
}
