# 💳 PayVault — QR Code Based Secure Offline Banking System

> A GPay-inspired final year project demonstrating secure offline banking with QR code payments, built with **Java backend** + **HTML/CSS/JavaScript frontend**.

---

## 📁 Project Structure

```
PayVault/
├── backend/                        ← Java Maven project
│   ├── pom.xml                     ← Maven dependencies
│   └── src/main/java/com/bank/
│       ├── Main.java               ← Entry point, HTTP server on :8080
│       ├── model/
│       │   ├── User.java           ← User data model
│       │   └── Transaction.java    ← Transaction data model
│       ├── db/
│       │   └── Database.java       ← SQLite CRUD operations
│       ├── service/
│       │   ├── AuthService.java    ← BCrypt login & register
│       │   ├── QRService.java      ← ZXing QR generation & parsing
│       │   ├── CryptoService.java  ← AES-256-GCM encryption
│       │   └── TransactionService.java ← Transfer logic & fraud checks
│       └── api/
│           └── BankingApiHandler.java  ← All REST endpoints
└── frontend/
    ├── login.html          ← Login + Registration page
    ├── dashboard.html      ← Main banking dashboard
    └── transactions.html   ← Full transaction history
```

---

## ⚙️ Technical Stack

| Layer     | Technology                        |
|-----------|-----------------------------------|
| Backend   | Java 11+, HttpServer (built-in)   |
| Database  | SQLite (via sqlite-jdbc)          |
| QR Code   | ZXing 3.5.1                       |
| Security  | BCrypt (cost-12), AES-256-GCM     |
| JSON      | Gson 2.10.1                       |
| Build     | Maven (shade plugin for fat JAR)  |
| Frontend  | HTML5, CSS3, Vanilla JavaScript   |

---

## 🚀 How to Run

### Prerequisites
- **Java 11+** — [Download](https://adoptium.net/)
- **Maven 3.6+** — [Download](https://maven.apache.org/)

---

### Option 1: VS Code

1. Install extensions: **Extension Pack for Java**, **Maven for Java**
2. Open the `backend/` folder in VS Code
3. Open a terminal and run:
   ```bash
   cd backend
   mvn package -q
   java -jar target/qr-banking-1.0.0.jar
   ```
4. Open `frontend/login.html` in your browser
5. Done! ✅

---

### Option 2: IntelliJ IDEA

1. Open IntelliJ → **File → Open** → select the `backend/` folder
2. IntelliJ auto-detects the Maven project — click **Trust Project**
3. Open `Main.java` → click the ▶ **Run** button
   - Or: Right-click `Main.java` → **Run 'Main'**
4. Open `frontend/login.html` in your browser
5. Done! ✅

---

### Option 3: Command Line Only

```bash
cd backend
mvn clean package -q
java -jar target/qr-banking-1.0.0.jar
```

Server starts at: **http://localhost:8080**

---

## 🧪 Demo Accounts

| Username | Password  | Balance     | Account Type |
|----------|-----------|-------------|--------------|
| alice    | pass123   | ₹15,000     | Savings      |
| bob      | pass456   | ₹8,500.50   | Current      |
| carol    | pass789   | ₹22,300.75  | Savings      |
| david    | david123  | ₹50,000     | Business     |

---

## 🔐 Security Features

| Feature               | Implementation                              |
|-----------------------|---------------------------------------------|
| Password hashing      | BCrypt (cost factor 12)                     |
| QR encryption         | AES-256-GCM with random IV                  |
| Transaction IDs       | TXN + timestamp + UUID (globally unique)    |
| Duplicate prevention  | TX ID checked in DB before processing       |
| Fraud detection       | Amount limits, self-transfer block, balance |
| Timing-safe login     | BCrypt always runs even for unknown users   |
| CORS headers          | Configured on all API responses             |

---

## 📡 REST API Endpoints

| Method | Endpoint                        | Description                    |
|--------|---------------------------------|--------------------------------|
| GET    | `/api/health`                   | Health check                   |
| POST   | `/api/login`                    | Authenticate user              |
| POST   | `/api/register`                 | Create new account             |
| GET    | `/api/users`                    | List all users (safe fields)   |
| GET    | `/api/balance?username=X`       | Get current balance            |
| POST   | `/api/send`                     | Transfer money                 |
| POST   | `/api/receive`                  | Accept QR payment              |
| GET    | `/api/transactions?username=X`  | Get transaction history        |
| GET    | `/api/transactions?txId=X`      | Look up single transaction     |
| POST   | `/api/qr/generate`              | Generate QR code (Base64 PNG)  |
| POST   | `/api/qr/validate`              | Validate & parse QR payload    |

---

## 💡 How QR Payments Work

```
SENDER (alice)                     RECEIVER (bob)
─────────────                      ─────────────
1. Enter amount ₹500
2. Click "Generate QR"
3. Backend creates payload:
   {txId, sender, amount, note}
4. ZXing encodes → QR image            5. Bob opens "Receive Money"
                                       6. Pastes QR payload
                                       7. System verifies:
                                          - Valid JSON format ✓
                                          - Not duplicate ✓
                                          - Sender has balance ✓
                                       8. Deducts from alice
                                       9. Credits to bob
                                      10. Stores transaction in DB
```

---

## 🖥️ Frontend Features

### Login Page (`login.html`)
- Sign In + Create Account tabs in one page
- Demo account quick-fill chips
- Password strength meter
- 6 account types (Savings, Current, Salary, Student, Fixed Deposit, Business)
- Works offline with localStorage fallback

### Dashboard (`dashboard.html`)
- Live balance display with account stats
- **Generate QR** → creates encrypted QR code
- **Scan/Validate QR** → parses and validates QR payload
- **Send Money** → 3-step flow: Search → Confirm → Receipt with TX ID
- **Receive Money** → paste QR payload to credit balance
- **Track Transaction** → enter TX ID to verify receipt
- Transaction History with recent activity feed

### Transactions (`transactions.html`)
- Full searchable, filterable table
- Filter: All / Sent / Received / Success / Failed
- Click any row → detailed receipt modal
- Summary stats: balance, total sent/received, count

---

## 🔄 Offline Mode

The system works **without running the Java backend**:
- All data stored in browser `localStorage`
- Demo accounts pre-loaded automatically
- Transactions saved locally
- All features (send, receive, QR, history) work offline

When the backend is running, data syncs automatically on load.

---

## 📦 Build Output

After `mvn package`, find the fat JAR at:
```
backend/target/qr-banking-1.0.0.jar
```

The SQLite database (`bank.db`) is created automatically in the same folder you run the JAR from.

---

*Built as a Final Year Project demonstrating secure QR-based banking — Java + HTML/CSS/JS*
