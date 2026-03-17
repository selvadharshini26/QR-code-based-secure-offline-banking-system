package com.bank;

import com.bank.api.BankingApiHandler;
import com.bank.db.Database;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) throws Exception {
        // Initialize database & seed demo accounts
        Database.init();

        // Start HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        BankingApiHandler api = new BankingApiHandler();
        server.createContext("/api/login",        api::handleLogin);
        server.createContext("/api/register",     api::handleRegister);
        server.createContext("/api/users",        api::handleUsers);
        server.createContext("/api/balance",      api::handleBalance);
        server.createContext("/api/send",         api::handleSend);
        server.createContext("/api/receive",      api::handleReceive);
        server.createContext("/api/transactions", api::handleTransactions);
        server.createContext("/api/qr/generate",  api::handleQRGenerate);
        server.createContext("/api/qr/validate",  api::handleQRValidate);
        server.createContext("/api/health",       api::handleHealth);

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     QR Banking System — Running      ║");
        System.out.println("║     http://localhost:8080             ║");
        System.out.println("║     Open frontend/login.html         ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();
        System.out.println("Demo accounts:");
        System.out.println("  alice  / pass123  (₹15,000)");
        System.out.println("  bob    / pass456  (₹8,500)");
        System.out.println("  carol  / pass789  (₹22,300)");
        System.out.println("  david  / david123 (₹50,000)");
    }
}
