package com.bank.api;

import com.bank.db.Database;
import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.service.AuthService;
import com.bank.service.QRService;
import com.bank.service.TransactionService;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class BankingApiHandler {

    private final Gson               gson = new Gson();
    private final AuthService        auth = new AuthService();
    private final TransactionService txSvc= new TransactionService();
    private final QRService          qrSvc= new QRService();

    // ── Health check ─────────────────────────────────────────────────────────
    public void handleHealth(HttpExchange ex) throws IOException {
        respond(ex, 200, Map.of("status", "ok", "server", "QR Banking API"));
    }

    // ── POST /api/login ───────────────────────────────────────────────────────
    public void handleLogin(HttpExchange ex) throws IOException {
        if (!allowCors(ex)) return;
        if (!"POST".equals(ex.getRequestMethod())) { respond(ex,405,err("Method not allowed")); return; }
        try {
            Map<?,?> body = readBody(ex);
            String username = str(body,"username");
            String password = str(body,"password");
            if (username==null||password==null) { respond(ex,400,err("Username and password required")); return; }

            User user = auth.login(username, password);
            if (user == null) { respond(ex,401,err("Invalid username or password")); return; }

            respond(ex, 200, Map.of(
                "success",     true,
                "userId",      user.userId,
                "username",    user.username,
                "name",        user.name,
                "balance",     user.balance,
                "phone",       safe(user.phone),
                "email",       safe(user.email),
                "accountType", safe(user.accountType)
            ));
        } catch (Exception e) { respond(ex,500,err(e.getMessage())); }
    }

    // ── POST /api/register ────────────────────────────────────────────────────
    public void handleRegister(HttpExchange ex) throws IOException {
        if (!allowCors(ex)) return;
        if (!"POST".equals(ex.getRequestMethod())) { respond(ex,405,err("Method not allowed")); return; }
        try {
            Map<?,?> body = readBody(ex);
            String userId     = str(body,"userId");
            String username   = str(body,"username");
            String name       = str(body,"name");
            String password   = str(body,"password");
            String phone      = str(body,"phone");
            String email      = str(body,"email");
            String accType    = str(body,"accountType");
            double balance    = body.get("balance") instanceof Number
                                ? ((Number)body.get("balance")).doubleValue() : 0;

            if (username==null||name==null||password==null) {
                respond(ex,400,err("username, name and password are required")); return;
            }
            if (userId==null) userId = "USR" + System.currentTimeMillis();
            if (accType==null) accType = "savings";

            User u = auth.register(userId, username, name, password, balance, phone, email, accType);
            respond(ex, 200, Map.of("success", true, "userId", u.userId, "name", u.name));
        } catch (IllegalArgumentException e) {
            respond(ex, 409, err(e.getMessage()));
        } catch (Exception e) { respond(ex,500,err(e.getMessage())); }
    }

    // ── GET /api/users ────────────────────────────────────────────────────────
    public void handleUsers(HttpExchange ex) throws IOException {
        if (!allowCors(ex)) return;
        try {
            List<User> users = Database.getAllUsers();
            List<Map<String,Object>> safe = users.stream().map(u -> {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("userId",      u.userId);
                m.put("username",    u.username);
                m.put("name",        u.name);
                m.put("balance",     u.balance);
                m.put("phone",       safe(u.phone));
                m.put("email",       safe(u.email));
                m.put("accountType", safe(u.accountType));
                return m;
            }).collect(Collectors.toList());
            respond(ex, 200, safe);
        } catch (Exception e) { respond(ex,500,err(e.getMessage())); }
    }

    // ── GET /api/balance?username=X ──────────────────────────────────────────
    public void handleBalance(HttpExchange ex) throws IOException {
        if (!allowCors(ex)) return;
        try {
            String username = queryParam(ex, "username");
            if (username==null) { respond(ex,400,err("username required")); return; }
            User u = Database.findByUsername(username);
            if (u==null) { respond(ex,404,err("User not found")); return; }
            respond(ex,200, Map.of("balance", u.balance, "username", u.username));
        } catch (Exception e) { respond(ex,500,err(e.getMessage())); }
    }

    // ── POST /api/send ────────────────────────────────────────────────────────
    public void handleSend(HttpExchange ex) throws IOException {
        if (!allowCors(ex)) return;
        if (!"POST".equals(ex.getRequestMethod())) { respond(ex,405,err("Method not allowed")); return; }
        try {
            Map<?,?> body    = readBody(ex);
            String sender    = str(body,"sender");
            String receiver  = str(body,"receiver");
            double amount    = ((Number) body.get("amount")).doubleValue();
            String note      = str(body,"note");

            String txId = txSvc.sendMoney(sender, receiver, amount, note);
            User senderUser = Database.findByUsername(sender);
            respond(ex,200, Map.of("success",true,"txId",txId,
                    "newBalance", senderUser != null ? senderUser.balance : 0));
        } catch (IllegalArgumentException e) {
            respond(ex,400, Map.of("success",false,"message",e.getMessage()));
        } catch (Exception e) { respond(ex,500,err(e.getMessage())); }
    }

    // ── POST /api/receive ─────────────────────────────────────────────────────
    public void handleReceive(HttpExchange ex) throws IOException {
        if (!allowCors(ex)) return;
        if (!"POST".equals(ex.getRequestMethod())) { respond(ex,405,err("Method not allowed")); return; }
        try {
            Map<?,?>      body      = readBody(ex);
            String        qrPayload = str(body,"qrPayload");
            String        receiver  = str(body,"receiver");
            Map<String,Object> qr   = qrSvc.parseQR(qrPayload);

            String senderUsername = (String) qr.getOrDefault("sender", qr.get("senderUsername"));
            double amount         = ((Number) qr.get("amount")).doubleValue();
            String txIdQR         = (String) qr.get("txId");
            String note           = (String) qr.getOrDefault("note","QR Payment");

            String txId = txSvc.receiveMoney(senderUsername, receiver, amount, txIdQR, note);
            User rcvUser = Database.findByUsername(receiver);
            respond(ex,200, Map.of("success",true,"txId",txId,
                    "newBalance", rcvUser != null ? rcvUser.balance : 0));
        } catch (IllegalArgumentException e) {
            respond(ex,400, Map.of("success",false,"message",e.getMessage()));
        } catch (Exception e) { respond(ex,500,err(e.getMessage())); }
    }

    // ── GET /api/transactions?username=X ────────────────────────────────────
    public void handleTransactions(HttpExchange ex) throws IOException {
        if (!allowCors(ex)) return;
        try {
            String username = queryParam(ex,"username");
            String txId     = queryParam(ex,"txId");

            if (txId != null) {
                Transaction t = Database.findTransactionById(txId);
                if (t==null) respond(ex,404,err("Transaction not found"));
                else respond(ex,200,t);
                return;
            }
            if (username==null) { respond(ex,400,err("username required")); return; }
            List<Transaction> txs = Database.getTransactionsByUsername(username);
            respond(ex,200,txs);
        } catch (Exception e) { respond(ex,500,err(e.getMessage())); }
    }

    // ── POST /api/qr/generate ────────────────────────────────────────────────
    public void handleQRGenerate(HttpExchange ex) throws IOException {
        if (!allowCors(ex)) return;
        if (!"POST".equals(ex.getRequestMethod())) { respond(ex,405,err("Method not allowed")); return; }
        try {
            Map<?,?> body = readBody(ex);
            String sender  = str(body,"sender");
            String sName   = str(body,"senderName");
            double amount  = ((Number)body.get("amount")).doubleValue();
            String note    = str(body,"note");
            String txId    = "TXN" + System.currentTimeMillis() + "QR";

            Map<String,Object> payload = new LinkedHashMap<>();
            payload.put("txId",         txId);
            payload.put("sender",       sender);
            payload.put("senderName",   sName);
            payload.put("amount",       amount);
            payload.put("note",         note != null ? note : "");
            payload.put("timestamp",    System.currentTimeMillis());

            String base64 = qrSvc.generateQRPlain(payload);
            respond(ex,200, Map.of(
                "success", true,
                "txId",    txId,
                "qrBase64",base64,
                "payload", gson.toJson(payload)
            ));
        } catch (Exception e) { respond(ex,500,err(e.getMessage())); }
    }

    // ── POST /api/qr/validate ────────────────────────────────────────────────
    public void handleQRValidate(HttpExchange ex) throws IOException {
        if (!allowCors(ex)) return;
        if (!"POST".equals(ex.getRequestMethod())) { respond(ex,405,err("Method not allowed")); return; }
        try {
            Map<?,?> body     = readBody(ex);
            String   qrText   = str(body,"qrPayload");
            Map<String,Object> parsed = qrSvc.parseQR(qrText);

            String txId = (String) parsed.get("txId");
            boolean isDuplicate = txId != null && Database.transactionExists(txId);

            Map<String,Object> res = new LinkedHashMap<>(parsed);
            res.put("valid",       true);
            res.put("isDuplicate", isDuplicate);
            respond(ex,200,res);
        } catch (IllegalArgumentException e) {
            respond(ex,400, Map.of("valid",false,"message",e.getMessage()));
        } catch (Exception e) { respond(ex,500,err(e.getMessage())); }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Map<?,?> readBody(HttpExchange ex) throws IOException {
        String body;
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8))) {
            body = r.lines().collect(Collectors.joining("\n"));
        }
        return gson.fromJson(body, Map.class);
    }

    private void respond(HttpExchange ex, int code, Object body) throws IOException {
        String json = gson.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods","GET,POST,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    /** Returns true if request is handled (either preflight done or not OPTIONS).
     *  Returns false if a preflight was sent and caller should stop. */
    private boolean allowCors(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods","GET,POST,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204,-1); return false;
        }
        return true;
    }

    private String queryParam(HttpExchange ex, String key) {
        String query = ex.getRequestURI().getQuery();
        if (query==null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=",2);
            if (kv.length==2 && kv[0].equals(key))
                try { return URLDecoder.decode(kv[1],"UTF-8"); } catch (Exception e) { return kv[1]; }
        }
        return null;
    }

    private String str(Map<?,?> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private String safe(String s) { return s != null ? s : ""; }
    private Map<String,Object> err(String msg) { return Map.of("success",false,"message",msg); }
}
