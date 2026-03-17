package com.bank.service;

import com.bank.db.Database;
import com.bank.model.Transaction;
import com.bank.model.User;

import java.util.UUID;

public class TransactionService {

    private static final double MAX_SINGLE_TX = 100_000.0;

    /** Generate unique transaction ID */
    public String generateTxId() {
        return "TXN" + System.currentTimeMillis()
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    /**
     * Send money from sender to receiver.
     * Returns transaction ID on success.
     * Throws on failure with descriptive message.
     */
    public synchronized String sendMoney(String senderUsername, String receiverUsername,
                                         double amount, String note) throws Exception {
        // ── Fraud checks ────────────────────────────────────────────────────
        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be greater than 0");
        if (amount > MAX_SINGLE_TX)
            throw new IllegalArgumentException("Amount exceeds single-transaction limit of ₹1,00,000");
        if (senderUsername.equalsIgnoreCase(receiverUsername))
            throw new IllegalArgumentException("Cannot send money to yourself");

        User sender   = Database.findByUsername(senderUsername);
        User receiver = Database.findByUsername(receiverUsername);

        if (sender == null)
            throw new IllegalArgumentException("Sender account not found");
        if (receiver == null)
            throw new IllegalArgumentException("Receiver account not found: " + receiverUsername);
        if (sender.balance < amount)
            throw new IllegalArgumentException(
                String.format("Insufficient balance. Available: ₹%.2f", sender.balance));

        // ── Execute transfer ─────────────────────────────────────────────────
        double newSenderBal   = Math.round((sender.balance - amount) * 100.0) / 100.0;
        double newReceiverBal = Math.round((receiver.balance + amount) * 100.0) / 100.0;

        Database.updateBalance(senderUsername, newSenderBal);
        Database.updateBalance(receiverUsername, newReceiverBal);

        String txId = generateTxId();
        Transaction tx = new Transaction(txId, senderUsername, receiverUsername,
                                          amount, note, "Success", "SEND");
        Database.insertTransaction(tx);

        System.out.printf("[TX] %s → %s: ₹%.2f (%s)%n",
                senderUsername, receiverUsername, amount, txId);
        return txId;
    }

    /**
     * Receive money via QR payload.
     * Returns transaction ID.
     */
    public synchronized String receiveMoney(String senderUsername, String receiverUsername,
                                             double amount, String txIdFromQR, String note) throws Exception {
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount in QR code");
        if (senderUsername.equalsIgnoreCase(receiverUsername))
            throw new IllegalArgumentException("Sender and receiver are the same account");

        // Duplicate check
        if (txIdFromQR != null && Database.transactionExists(txIdFromQR))
            throw new IllegalArgumentException("QR code already used — duplicate transaction blocked");

        User sender   = Database.findByUsername(senderUsername);
        User receiver = Database.findByUsername(receiverUsername);

        if (sender == null)   throw new IllegalArgumentException("Sender account not found");
        if (receiver == null) throw new IllegalArgumentException("Receiver account not found");
        if (sender.balance < amount)
            throw new IllegalArgumentException(
                String.format("Sender has insufficient balance: ₹%.2f", sender.balance));

        double newSenderBal   = Math.round((sender.balance - amount) * 100.0) / 100.0;
        double newReceiverBal = Math.round((receiver.balance + amount) * 100.0) / 100.0;

        Database.updateBalance(senderUsername, newSenderBal);
        Database.updateBalance(receiverUsername, newReceiverBal);

        String txId = (txIdFromQR != null) ? txIdFromQR : generateTxId();
        Transaction tx = new Transaction(txId, senderUsername, receiverUsername,
                                          amount, note, "Success", "RECEIVE");
        Database.insertTransaction(tx);

        System.out.printf("[QR-RECEIVE] %s → %s: ₹%.2f (%s)%n",
                senderUsername, receiverUsername, amount, txId);
        return txId;
    }
}
