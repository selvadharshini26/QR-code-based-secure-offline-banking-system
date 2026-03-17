package com.bank.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.bank.db.Database;
import com.bank.model.User;

public class AuthService {

    private static final int BCRYPT_COST = 12;

    /** Hash a plaintext password using BCrypt */
    public String hash(String password) {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
    }

    /** Verify a plaintext password against a stored BCrypt hash */
    public boolean verify(String password, String hash) {
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
        return result.verified;
    }

    /**
     * Attempt login. Returns the User on success, null on failure.
     * Uses timing-safe comparison to prevent timing attacks.
     */
    public User login(String username, String password) throws Exception {
        User user = Database.findByUsername(username);
        if (user == null) {
            // Still run bcrypt to prevent timing oracle
            BCrypt.verifyer().verify(password.toCharArray(),
                "$2a$12$dummyhashvaluetopreventtimingattacks1234567890");
            return null;
        }
        if (!verify(password, user.passwordHash)) return null;
        return user;
    }

    /**
     * Register a new user.
     */
    public User register(String userId, String username, String name,
                         String password, double balance,
                         String phone, String email, String accountType) throws Exception {
        // Check duplicate username
        if (Database.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists");
        }
        User u = new User(userId, username, name, hash(password), balance, phone, email, accountType);
        Database.insertUser(u);
        return u;
    }
}
