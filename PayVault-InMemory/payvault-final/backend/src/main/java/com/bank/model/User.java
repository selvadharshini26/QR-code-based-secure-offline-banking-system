package com.bank.model;

public class User {
    public String userId;
    public String username;
    public String name;
    public String passwordHash;
    public double balance;
    public String phone;
    public String email;
    public String accountType;
    public String createdAt;

    public User() {}

    public User(String userId, String username, String name,
                String passwordHash, double balance,
                String phone, String email, String accountType) {
        this.userId      = userId;
        this.username    = username;
        this.name        = name;
        this.passwordHash= passwordHash;
        this.balance     = balance;
        this.phone       = phone;
        this.email       = email;
        this.accountType = accountType;
        this.createdAt   = java.time.Instant.now().toString();
    }
}
