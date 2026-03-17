package com.bank.model;

public class Transaction {
    public String txId;
    public String sender;
    public String receiver;
    public double amount;
    public String note;
    public String date;
    public String status;  // "Success" | "Failed"
    public String type;    // "SEND" | "RECEIVE" | "QR"

    public Transaction() {}

    public Transaction(String txId, String sender, String receiver,
                       double amount, String note, String status, String type) {
        this.txId     = txId;
        this.sender   = sender;
        this.receiver = receiver;
        this.amount   = amount;
        this.note     = note;
        this.status   = status;
        this.type     = type;
        this.date     = java.time.Instant.now().toString();
    }
}
