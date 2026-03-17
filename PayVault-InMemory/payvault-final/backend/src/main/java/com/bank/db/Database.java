package com.bank.db;

import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.service.AuthService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * In-memory database — replaces SQLite entirely.
 * All data lives in ConcurrentHashMap / CopyOnWriteArrayList for thread safety.
 * Data resets when the JVM restarts (perfect for demos / testing).
 */
public class Database {

    // ── Storage ──────────────────────────────────────────────────────────────
    // Key = lowercase username
    private static final Map<String, User> USERS_BY_USERNAME = new ConcurrentHashMap<>();
    // Key = userId
    private static final Map<String, User> USERS_BY_ID       = new ConcurrentHashMap<>();
    // Key = phone
    private static final Map<String, User> USERS_BY_PHONE    = new ConcurrentHashMap<>();
    // All transactions
    private static final List<Transaction> TRANSACTIONS       = new CopyOnWriteArrayList<>();
    // Key = txId
    private static final Map<String, Transaction> TX_BY_ID   = new ConcurrentHashMap<>();

    // ── Init & seed ──────────────────────────────────────────────────────────
    public static void init() throws Exception {
        if (!USERS_BY_USERNAME.isEmpty()) return; // already initialised

        AuthService auth = new AuthService();

        insertUser(new User("USR001", "alice", "Alice Johnson",
                auth.hash("pass123"), 15000.00, "+919876543210", "alice@bank.com", "savings"));
        insertUser(new User("USR002", "bob",   "Bob Smith",
                auth.hash("pass456"), 8500.50,  "+919876543211", "bob@bank.com",   "current"));
        insertUser(new User("USR003", "carol", "Carol White",
                auth.hash("pass789"), 22300.75, "+919876543212", "carol@bank.com", "savings"));
        insertUser(new User("USR004", "david", "David Kumar",
                auth.hash("david123"), 50000.00, "+919876543213", "david@bank.com", "business"));

        System.out.println("[DB] In-memory database initialised with 4 demo accounts.");
    }

    // ── User operations ──────────────────────────────────────────────────────

    public static synchronized void insertUser(User u) throws Exception {
        String key = u.username.toLowerCase();
        if (USERS_BY_USERNAME.containsKey(key)) {
            throw new IllegalArgumentException("Username already exists: " + u.username);
        }
        USERS_BY_USERNAME.put(key, u);
        USERS_BY_ID.put(u.userId, u);
        if (u.phone != null && !u.phone.isEmpty()) {
            USERS_BY_PHONE.put(u.phone, u);
        }
    }

    public static User findByUsername(String username) {
        if (username == null) return null;
        return USERS_BY_USERNAME.get(username.toLowerCase());
    }

    public static User findById(String userId) {
        if (userId == null) return null;
        return USERS_BY_ID.get(userId);
    }

    public static User findByPhone(String phone) {
        if (phone == null) return null;
        return USERS_BY_PHONE.get(phone);
    }

    public static List<User> getAllUsers() {
        return USERS_BY_USERNAME.values().stream()
                .sorted(Comparator.comparing(u -> u.name))
                .collect(Collectors.toList());
    }

    public static synchronized void updateBalance(String username, double newBalance) {
        User u = USERS_BY_USERNAME.get(username.toLowerCase());
        if (u != null) {
            u.balance = newBalance;
        }
    }

    // ── Transaction operations ───────────────────────────────────────────────

    public static void insertTransaction(Transaction t) {
        TRANSACTIONS.add(t);
        TX_BY_ID.put(t.txId, t);
    }

    public static List<Transaction> getTransactionsByUsername(String username) {
        if (username == null) return Collections.emptyList();
        String lower = username.toLowerCase();
        return TRANSACTIONS.stream()
                .filter(t -> lower.equals(t.sender) || lower.equals(t.receiver))
                .sorted(Comparator.comparing((Transaction t) -> t.date).reversed())
                .collect(Collectors.toList());
    }

    public static Transaction findTransactionById(String txId) {
        return TX_BY_ID.get(txId);
    }

    public static boolean transactionExists(String txId) {
        return TX_BY_ID.containsKey(txId);
    }

    // ── Stats helper (bonus) ─────────────────────────────────────────────────
    public static int userCount()        { return USERS_BY_USERNAME.size(); }
    public static int transactionCount() { return TRANSACTIONS.size(); }
}
