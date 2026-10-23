# FinTrack — Complete Engineering Handover & AI Agent Guide

> **Document Version:** 4.0 (FinTrack v2.3 — Aurora Pastel Glassmorphic, Exact Money Arithmetic, PBKDF2 & Database v7 Schema Upgrade)  
> **Target Audience:** Next AI Coding Agent / Senior Android Systems Engineer  
> **Project Scope:** Native Android Personal Finance & Expense Intelligence System (versionCode = 5, versionName = "2.3")  
> **Security Posture:** Air-Gapped, 100% Offline, Zero Network Permissions, Hardware AES-256-GCM Encryption, PBKDF2 (120,000 iterations)  
> **Status:** Production-Ready, Tested (66/66 Unit Tests Passing), Deployed on Physical Device  

---

## Table of Contents

1. [Executive Summary & Core Directives](#1-executive-summary--core-directives)
2. [Plan: System Architecture & Data Modeling](#2-plan-system-architecture--data-modeling)
   - 2.1 High-Level Architecture & Data Flow
   - 2.2 Financial Domain Model (Assets vs. Liabilities)
   - 2.3 Local Database Architecture & Schema v3
   - 2.4 Multi-User Data Isolation Architecture
   - 2.5 Security, Hardware Keystore & Authentication
3. [Functionality Deep Dive: Core Intelligence Engines](#3-functionality-deep-dive-core-intelligence-engines)
   - 3.1 SMS Ingestion Pipeline (`IndianBankSmsParser`)
   - 3.2 Dual-Message Correlation Engine (`TransactionCorrelationEngine`)
   - 3.3 Account Identification & Debit Card Engine (`AccountIdentificationEngine`)
   - 3.4 Credit Card Intelligence & Liability Tracking (`CreditCardPaymentEngine`)
   - 3.5 Dynamic Balance Reconciliation (`BalanceReconciliationEngine`)
   - 3.6 Internal Transfer Matching (`TransferMatchingEngine`)
   - 3.7 SMS Deduplication Fingerprinting (`SmsFingerprintEngine`)
   - 3.8 Heuristic Categorization (`ExpenseCategorizer`)
4. [UI & Navigation Architecture](#4-ui--navigation-architecture)
   - 4.1 Navigation Architecture & Routes
   - 4.2 Primary Screens & Interaction Models
   - 4.3 Interactive Dialogs & Review Sheets
5. [Build Guide: Compilation & Packaging](#5-build-guide-compilation--packaging)
   - 5.1 Toolchain & Environment Requirements
   - 5.2 Windows Environment Quirks & Forwarders
   - 5.3 Gradle Build Commands
   - 5.4 Keystore & Release Signing Configuration
6. [Test Guide: Verification & Quality Assurance](#6-test-guide-verification--quality-assurance)
   - 6.1 Unit Test Suites & Scenarios
   - 6.2 Running Tests via CLI
   - 6.3 ADB Physical Device Installation & Live Verification
   - 6.4 Regression Checklist for Future Agents
7. [Extension Playbook & Golden Rules](#7-extension-playbook--golden-rules)
   - 7.1 How to Add a New Bank Parser
   - 7.2 How to Bump Database Schema (v3 to v4)
   - 7.3 Inviolable System Constraints (Never Break These)

---

## 1. Executive Summary & Core Directives

**FinTrack** is an offline-native personal financial operating system built for modern Android. It automates financial ledgering by capturing incoming transaction alerts from Indian banks (HDFC, SBI, ICICI, Axis, Kotak, PNB, Canara, BoB, etc.) and merchants (Amazon, Swiggy, Zomato, Flipkart, Uber, etc.).

### Critical Project Philosophies:
1. **Zero Cloud / Zero Network:** The manifest (`AndroidManifest.xml`) explicitly **does not request `android.permission.INTERNET`**. The Android OS physically prevents network access. Do **never** add network libraries, cloud aggregators, or external HTTP endpoints.
2. **Hardware-Backed AES-256-GCM:** All sensitive balances, transaction amounts, account numbers, and user records are encrypted before writing to SQLite using keys sealed in the Android Keystore (`AndroidKeyStore`).
3. **Passive, Battery-Efficient Architecture:** Operates via an event-driven `BroadcastReceiver` (`android.provider.Telephony.SMS_RECEIVED`). Processes an incoming message in <15ms, then immediately returns to idle (0% background battery consumption).
4. **Dual-Message Fusion:** Automatically correlates paired SMS notifications (e.g., an Amazon order confirmation + a Kotak bank debit alert) arriving within 15 minutes, deduplicates ledger deductions (money deducted only once), merges rich merchant metadata, and synchronizes the authoritative bank closing balance.
5. **Debit Card Bank Account Auto-Resolution:** Debit cards are recognized as bank accounts (not credit cards), mapped to the user's corresponding bank account by bank name, and auto-registered in `account_aliases`.
6. **Multi-User Isolation:** Supports independent profiles (e.g., primary user + family member), with independent encryption keys, biometrics, PINs, accounts, and transaction stores on the same device.

---

## 2. Plan: System Architecture & Data Modeling

### 2.1 High-Level Architecture & Data Flow

```
                     Incoming Cellular SMS Broadcast
                                   │
                                   ▼
             [SmsBroadcastReceiver] (<15ms execution burst)
                                   │
                                   ▼
             [TransactionRepository.processIncomingSms]
                                   │
             ┌─────────────────────┴──────────────────────┐
             ▼                                            ▼
   [SmsFingerprintEngine]                       [IndianBankSmsParser]
   (SHA-256 deduplication;                      (Regex extraction: Amount, ₹,
    drops exact dupes in <1ms)                   Sender, Bank, Type, Dues, Balances)
                                                          │
                                                          ▼
                                            [TransactionCorrelationEngine]
                                            (15-min sliding window check;
                                             fuses dual merchant+bank alerts;
                                             single deduction guarantee)
                                                          │
                                                          ▼
                                            [AccountIdentificationEngine]
                                            (Resolves last-4 / bank name;
                                             maps debit cards to bank a/c;
                                             auto-learns alias in SQLite)
                                                          │
                                                          ▼
                                            [BalanceReconciliationEngine]
                                            (Reconciles ledger balance with
                                             bank closing balance; logs drift)
                                                          │
                                                          ▼
                                            [AppDatabaseHelper]
                                            (Hardware AES-256-GCM encrypted
                                             SQLite storage with user_id)
                                                          │
                                                          ▼
                                            [MainScreenViewModel]
                                            (StateFlow reactive UI state)
                                                          │
                                                          ▼
                                            [Jetpack Compose UI]
                                            (Dashboard, Accounts, Review Sheet)
```

---

### 2.2 Financial Domain Model (Assets vs. Liabilities)

The application maintains a strict double-entry-style mathematical separation between **Liquid Assets** (money you own) and **Liabilities** (money you owe):

```
                                  FINANCIAL MODEL
                                         │
                 ┌───────────────────────┴───────────────────────┐
                 ▼                                               ▼
         LIQUID ASSETS (Net Worth +)                     LIABILITIES (Net Worth -)
                 │                                               │
        ┌────────┴────────┐                             ┌────────┴────────┐
        ▼                 ▼                             ▼                 ▼
   BANK ACCOUNTS    CASH WALLETS                   CREDIT CARDS         LOANS
 (Savings/Current) (Physical cash)              (Revolving Credit)  (Deferred Dues)
```

#### Key Domain Classes:
* **`Account`** (`com.example.fintrack.data.model.Account`):
  * `id`: UUID string.
  * `userId`: Owning user ID (multi-user isolation).
  * `name`: Account name (e.g., "Kotak Savings", "HDFC Regalia").
  * `type`: `AccountType.BANK_ACCOUNT`, `AccountType.CREDIT_CARD`, `AccountType.CASH_WALLET`.
  * `accountNumberLast4`: Last 4 digits (e.g., "1234").
  * `bankName`: Issuer institution (e.g., "Kotak Bank", "HDFC Bank").
  * `currentBalance`: Running balance for bank/cash, or outstanding balance for credit cards.
  * `creditLimit`: Total authorized limit (Credit Cards only).
  * `availableCredit`: Current remaining credit limit (Credit Cards only).
  * `totalDue` / `minimumDue` / `dueDate`: Statement billing metrics (Credit Cards only).
  * `billStatus`: `BillStatus.NONE`, `BillStatus.GENERATED`, `BillStatus.DUE`, `BillStatus.PAID`.
* **`Transaction`** (`com.example.fintrack.data.model.Transaction`):
  * `id`: UUID string.
  * `userId`: Owning user ID.
  * `accountId`: Linked `Account.id`.
  * `amount`: Transaction amount (Double).
  * `direction`: `TransactionDirection.DEBIT` or `TransactionDirection.CREDIT`.
  * `kind`: `TransactionKind.EXPENSE`, `INCOME`, `CARD_PURCHASE`, `CARD_PAYMENT`, `CARD_BILL_GENERATED`, `CARD_BILL_DUE`, `BANK_TRANSFER`, `ATM_WITHDRAWAL`, `REFUND`, `REVERSAL`, `ADJUSTMENT`.
  * `timestamp`: Epoch milliseconds.
  * `merchant`: Clean merchant name (e.g., "Amazon", "Swiggy", "Zomato").
  * `categoryId`: Category identifier (e.g., "cat_shopping", "cat_food").
  * `needsReview`: Boolean flag. If `true`, requires user review. If `false`, auto-approved.
  * `balanceAfterTxn`: Authoritative closing balance extracted from SMS.
  * `smsSender`: SMS sender header (e.g., "VK-KOTAKB", "AX-AMAZON").
  * `rawSmsBody`: Original message body for auditing and correlation.

---

### 2.3 Local Database Architecture & Schema v3

The database engine is raw SQLite managed by **`AppDatabaseHelper`** (`com.example.fintrack.data.local.AppDatabaseHelper`), currently at **Schema Version 3**.

#### Key Database Tables:
1. **`users`**:
   * `id TEXT PRIMARY KEY, display_name TEXT, email TEXT, phone TEXT, pin_hash TEXT, pin_salt TEXT, biometric_enabled INTEGER, created_at INTEGER, last_active_at INTEGER, is_primary INTEGER`
2. **`accounts`**:
   * `id TEXT PRIMARY KEY, user_id TEXT, name TEXT, type TEXT, account_number_last_4 TEXT, bank_name TEXT, current_balance REAL, credit_limit REAL, available_credit REAL, total_due REAL, minimum_due REAL, due_date TEXT, bill_status TEXT, created_at INTEGER, updated_at INTEGER, is_active INTEGER`
3. **`account_aliases`**:
   * `id TEXT PRIMARY KEY, account_id TEXT, alias_pattern TEXT, confidence REAL, created_at INTEGER`
   * *Purpose:* Maps debit card last-4 digits or UPI virtual handles directly to the parent bank account.
4. **`transactions`**:
   * `id TEXT PRIMARY KEY, user_id TEXT, account_id TEXT, amount REAL, direction TEXT, kind TEXT, timestamp INTEGER, merchant TEXT, category_id TEXT, reference_number TEXT, notes TEXT, balance_after_txn REAL, is_excluded_from_budget INTEGER, needs_review INTEGER, review_reason TEXT, sms_fingerprint TEXT, raw_sms_body TEXT, sms_sender TEXT, created_at INTEGER, updated_at INTEGER`
5. **`sms_fingerprints`**:
   * `fingerprint TEXT PRIMARY KEY, received_at INTEGER, message_preview TEXT`
   * *Purpose:* SHA-256 duplicate prevention.
6. **`reconciliation_logs`**:
   * `id TEXT PRIMARY KEY, account_id TEXT, recorded_balance REAL, sms_balance REAL, discrepancy REAL, resolved INTEGER, timestamp INTEGER`

---

### 2.4 Multi-User Data Isolation Architecture

FinTrack v2.2+ supports **true multi-user profiles on a single device** without any cloud servers:
* Managed by **`SessionManager`** (`com.example.fintrack.data.local.SessionManager`).
* Each user profile has a distinct UUID (`User.id`).
* `AppDatabaseHelper` executes all account, transaction, and dashboard queries scoped to `user_id = ?`.
* `SessionManager.getActiveUserId()` determines the current active user context.
* Switching users immediately resets state in `MainScreenViewModel`, clearing memory caches and loading only the selected user's financial ledger.
* Default primary user: **Yatin Kumar Singh** (`is_primary = 1`). Additional profiles (e.g., family members) can be created from the Profile Settings tab.

---

### 2.5 Security, Hardware Keystore & Authentication

* **Hardware Encryption (`SecurityManager`)**:
  * Utilizes the Android KeyStore provider (`AndroidKeyStore`).
  * Master key: AES-256-GCM with 128-bit authentication tag, generated with `KeyGenParameterSpec.Builder(PURPOSE_ENCRYPT or PURPOSE_DECRYPT)`.
  * Protects encryption keys inside the device's hardware security module (Secure Element / StrongBox).
* **PIN & Biometric Lockout (`PinManager`)**:
  * PIN stored as salted SHA-256 hash (`pin_hash` + random 16-byte `pin_salt`).
  * 5 consecutive incorrect PIN entries triggers a **30-second hardware timer lockout**.
  * Native `BiometricPrompt` with `BIOMETRIC_STRONG` or `DEVICE_CREDENTIAL`.

---

## 3. Functionality Deep Dive: Core Intelligence Engines

### 3.1 SMS Ingestion Pipeline (`IndianBankSmsParser`)

Located at: [`app/src/main/java/com/example/fintrack/parser/IndianBankSmsParser.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/parser/IndianBankSmsParser.kt)

#### Responsibilities:
1. **Rupee Symbol Currency Regex:** Supports `₹`, `\u20B9`, `Rs`, `INR` across all regex matchers:
   ```kotlin
   private const val CURRENCY_SYM = "(?:rs\\.?|inr|₹|\\u20B9)"
   ```
2. **Debit Card Separation:** Detects debit cards via `DEBIT_CARD_PATTERNS` (`debit card`, `dc ending`, `using debit`) and forces instrument type to `FinancialInstrumentType.BANK_ACCOUNT` with `isDebitCard = true`.
3. **Strict OTP & Fraud Footer Filter:** Indian bank alerts often include security footers: *"Never share OTP"*, *"If not done by you click here to report"*. The parser verifies:
   ```kotlin
   val hasOtpKeyword = OTP_SPAM_PATTERNS[0].matcher(messageBody).find()
   val isCompletedAction = messageBody.contains("debited", ignoreCase = true) ||
                           messageBody.contains("credited", ignoreCase = true) ||
                           messageBody.contains("withdrawn", ignoreCase = true) ||
                           messageBody.contains("spent", ignoreCase = true) ||
                           messageBody.contains("reversed", ignoreCase = true) ||
                           messageBody.contains("refunded", ignoreCase = true)

   if (hasOtpKeyword && !isCompletedAction) return null
   ```
4. **18 Supported Indian Financial Institutions:** Kotak, HDFC, SBI, ICICI, Axis, PNB, Bank of Baroda, Canara, IndusInd, Paytm Payments Bank, IDFC FIRST, YES Bank, Federal Bank, Union Bank, RBL Bank, Citibank, Standard Chartered, and AU Small Finance Bank.

---

### 3.2 Dual-Message Correlation Engine (`TransactionCorrelationEngine`)

Located at: [`app/src/main/java/com/example/fintrack/engine/TransactionCorrelationEngine.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/engine/TransactionCorrelationEngine.kt)

#### The Problem It Solves:
When a user purchases an item on Amazon or Swiggy using a debit card or UPI, they receive **two distinct SMS alerts**:
1. Merchant SMS: *"Amazon: Order placed for INR 1,499.00 using Kotak Bank A/c ending 1234."*
2. Bank SMS: *"Kotak Bank: INR 1,499.00 debited from A/c 1234 towards POS AMZN*PAY. Avl bal: INR 18,501.00."*

A naive finance app records this as **two transactions (-₹2,998 total)**, corrupting the balance.

#### How FinTrack Resolves This:
* **15-Minute Sliding Window (`CORRELATION_WINDOW_MS = 15 * 60 * 1000L`):**
  * Compares incoming SMS against transactions recorded in the last 15 minutes.
  * Checks:
    1. Exact same amount (within 1 paisa / 0.01).
    2. Same direction (`DEBIT` with `DEBIT`, `CREDIT` with `CREDIT`).
    3. Not an informational statement bill.
    4. Merchant-to-Bank match: One alert from a recognized merchant sender or body keyword, other from a recognized bank sender.
    5. Shared account last-4 digits or shared UTR/reference number.
* **Single Deduction Ledger Guarantee:**
  * When correlated in [`TransactionRepository.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/data/repository/TransactionRepository.kt#L125-L165):
    1. **Does not deduct the ledger balance again.**
    2. Enriches the existing merchant name to the clean merchant title (e.g., replaces `"POS AMZN*PAY"` with `"Amazon"`).
    3. Synchronizes the authoritative bank closing balance (`balanceAfterTxn = 18501.0`).
    4. Updates the single existing transaction in SQLite.

---

### 3.3 Account Identification & Debit Card Engine (`AccountIdentificationEngine`)

Located at: [`app/src/main/java/com/example/fintrack/engine/AccountIdentificationEngine.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/engine/AccountIdentificationEngine.kt)

#### Resolution Logic:
1. **Direct Last-4 Match:** Matches account number against `Account.accountNumberLast4` (Confidence = 1.0, `needsReview = false`).
2. **Learned Alias Match:** Checks `account_aliases` table (Confidence = 0.95, `needsReview = false`).
3. **Debit Card Bank Name Matching:** If `parsed.isDebitCard == true`, searches user's bank accounts for matching `bankName` (e.g., Kotak debit card -> Kotak savings account):
   * Sets Confidence = 0.95, `needsReview = false`.
   * **Auto-persists the alias** into `account_aliases` table so future transactions match with zero latency.
4. **Sole Configured Account Inference:** If the user has only 1 bank account configured and the transaction is a bank transaction, resolves to that account with Confidence = 0.92 and `needsReview = false`.

---

### 3.4 Credit Card Intelligence & Liability Tracking (`CreditCardPaymentEngine`)

Located at: [`app/src/main/java/com/example/fintrack/engine/CreditCardPaymentEngine.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/engine/CreditCardPaymentEngine.kt)

#### Principles of Credit Card Accounting in FinTrack:
* Credit Cards are **Liabilities**. Spend **increases** liability (`currentBalance += amount`) and **decreases** available credit (`availableCredit -= amount`).
* Bill statements (`CARD_BILL_DUE` / `CARD_BILL_GENERATED`) update `totalDue`, `minimumDue`, and `dueDate`, but **never deduct from bank accounts and never set billStatus = PAID**.
* When a payment SMS arrives (`CARD_PAYMENT`) or user taps **Mark as Paid**:
  1. Decreases card liability: `newLiability = (currentLiability - paymentAmount).coerceAtLeast(0.0)`.
  2. Restores available credit: `newAvailable = (currentAvailable + paymentAmount).coerceAtMost(creditLimit)`.
  3. Re-evaluates bill status: If `newLiability <= 0.0`, sets `billStatus = BillStatus.PAID`.

---

### 3.5 Dynamic Balance Reconciliation (`BalanceReconciliationEngine`)

Located at: [`app/src/main/java/com/example/fintrack/engine/BalanceReconciliationEngine.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/engine/BalanceReconciliationEngine.kt)

* When a bank SMS reports an authoritative closing balance (e.g., *"Avl Bal: Rs 42,150.00"*):
* The engine compares calculated ledger balance against the SMS closing balance.
* If discrepancy > ₹1.00:
  1. Corrects the account balance to match the authoritative bank balance.
  2. Creates an audit record in `reconciliation_logs`.
  3. Generates a non-intrusive adjustment note in the transaction ledger.

---

### 3.6 Internal Transfer Matching (`TransferMatchingEngine`)

Located at: [`app/src/main/java/com/example/fintrack/engine/TransferMatchingEngine.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/engine/TransferMatchingEngine.kt)

* Correlates a DEBIT from Account A with a CREDIT to Account B of identical amount within a 5-minute window.
* Re-classifies the transactions as `BANK_TRANSFER`, preventing double-counting as both an expense and income.

---

### 3.7 SMS Deduplication Fingerprinting (`SmsFingerprintEngine`)

Located at: [`app/src/main/java/com/example/fintrack/engine/SmsFingerprintEngine.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/engine/SmsFingerprintEngine.kt)

* Generates SHA-256 fingerprint from: `sender + normalized(body) + amount + timestampBucket`.
* Stores fingerprints in `sms_fingerprints` table.
* Re-transmitted carrier broadcasts or repeated SIM queries are dropped in <1ms.

---

### 3.8 Heuristic Categorization (`ExpenseCategorizer`)

Located at: [`app/src/main/java/com/example/fintrack/parser/ExpenseCategorizer.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/parser/ExpenseCategorizer.kt)

* 100% on-device heuristic categorizer mapping merchants and narrations into 10 default categories:
  * Food & Dining (`cat_food`): Swiggy, Zomato, Starbucks, McDonald's, Dominos, KFC, etc.
  * Shopping (`cat_shopping`): Amazon, Flipkart, Myntra, Zara, Nykaa, etc.
  * Groceries (`cat_groceries`): Blinkit, Zepto, BigBasket, Instamart, DMart, etc.
  * Travel & Commute (`cat_travel`): Uber, Ola, MakeMyTrip, IRCTC, IndiGo, Fastag, Metro.
  * Bills & Utilities (`cat_utilities`): Electricity, Water, Gas, Airtel, Jio, Bescom.
  * Entertainment (`cat_entertainment`): Netflix, Spotify, BookMyShow, PVR.
  * Health & Medical (`cat_health`): Apollo, 1mg, PharmEasy.
  * Investments (`cat_investment`): Zerodha, Groww, AngelOne.
  * Education & Personal Care (`cat_education`, `cat_personal`).

---

## 4. UI & Navigation Architecture

### 4.1 Navigation Architecture & Routes

FinTrack uses modern **Jetpack Compose Navigation** with typed route keys defined in [`app/src/main/java/com/example/fintrack/NavigationKeys.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/NavigationKeys.kt):

* `Route.Splash`: Initial startup and animated splash.
* `Route.Auth`: Biometric authentication / Master PIN prompt.
* `Route.Main`: Main 4-tab bottom navigation shell.
* `Route.CreditCardDetails(cardId)`: Detailed view for credit card dues and payment history.
* `Route.Verification`: Verification queue for pending transactions.
* `Route.SmsIntelligence`: SMS audit log and parser debugger.

---

### 4.2 Primary Screens & Interaction Models

1. **Dashboard Screen (`DashboardScreen.kt`)**:
   * **Liquid Net Worth Card:** Real-time calculation of (Total Bank Assets + Cash Wallets) minus (Credit Card Liabilities).
   * **Credit Card Liabilities Carousel:** Visual credit limit utilization progress bars, total amount due alerts, and payment due countdowns.
   * **Pending Review Banner:** Displays a badge and quick-action banner if any transactions require confirmation.
   * **Recent Activity Feed:** Grouped list of recent transactions with category icons and status pills.
2. **Transactions Screen (`TransactionsScreen.kt`)**:
   * Search and filter by account, category, direction, and date range.
   * Manual transaction entry modal with real-time balance update.
3. **Accounts Screen (`AccountsScreen.kt`)**:
   * Tabbed view separating Liquid Bank Accounts and Credit Cards.
   * Add Account / Credit Card dialog.
   * Direct balance adjustment and account deletion options.
4. **Intelligence Center (`IntelligenceCenterScreen.kt`)**:
   * Monthly spend breakdown charts, daily spending averages, budget variance, and anomaly detection.
5. **Profile Settings Screen (`ProfileSettingsScreen.kt`)**:
   * Profile header showing active user name ("Yatin Kumar Singh").
   * **Switch User / Create New User Modal:** Allows creating isolated profiles for family members.
   * Biometric toggle, PIN change, database statistics, and secure data wipe.

---

### 4.3 Interactive Dialogs & Review Sheets

* **`TransactionReviewSheet.kt`**:
  * Slides up from bottom when user taps a transaction requiring review.
  * Displays parsed merchant, amount, matched bank account, and confidence score.
  * Offers 1-tap **Confirm**, **Edit Account**, or **Reject / Mark False Alert**.
* **`BillPaidDialog.kt`**:
  * Triggered from Credit Card Details screen or when a statement bill is active.
  * Allows user to select the source bank account used to pay the bill.
  * Atomically records the bank debit, credit card liability reduction, restores available credit, and sets bill status to `PAID`.
* **`UserDetailsSheet.kt`**:
  * Displays user profile attributes, device binding ID, and encryption status.

---

## 5. Build Guide: Compilation & Packaging

### 5.1 Toolchain & Environment Requirements

| Component | Required Version | Verification Command |
| :--- | :--- | :--- |
| **Operating System** | Windows 10/11, macOS, or Linux | `[System.Environment]::OSVersion` |
| **Java JDK** | OpenJDK 17 (`jvmToolchain(17)`) | `java -version` |
| **Android SDK** | API 36 (`platforms;android-36`) | `adb --version` |
| **Build Tools** | `36.0.0` or `35.0.0` | In `C:\Users\yatin\AppData\Local\Android\Sdk\build-tools` |
| **Kotlin** | `2.3.20` | In `gradle/libs.versions.toml` |
| **AGP** | `9.4.1` | In `gradle/libs.versions.toml` |
| **Gradle Wrapper** | `9.6.0` | `.\gradlew.bat -v` |

---

### 5.2 Windows Environment Quirks & Forwarders

> [!IMPORTANT]
> **Windows Shell & Batch Forwarder Notice:**
> 1. In Windows PowerShell environments where `System32` might be constrained in sub-shells, the repository root includes a `powershell.bat` forwarder forwarding commands to `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe`.
> 2. When executing commands via automated agents, avoid chaining with `&&` in PowerShell (use `;` instead).
> 3. Do not run piped commands (`|`) directly inside `powershell.bat` without quoting, as `cmd.exe` will attempt to parse the pipe before PowerShell receives it.

---

### 5.3 Gradle Build Commands

Always run from the project root: `c:\Users\yatin\Desktop\Workspace1\FinTrack`

1. **Clean Project:**
   ```powershell
   .\gradlew.bat clean
   ```
2. **Compile Debug Build:**
   ```powershell
   .\gradlew.bat assembleDebug
   ```
3. **Compile Signed Production Release APK:**
   ```powershell
   .\gradlew.bat assembleRelease
   ```
   *Output Location:* `app/build/outputs/apk/release/app-release.apk` (Size: ~9.6 MB).

---

### 5.4 Keystore & Release Signing Configuration

Release signing is configured in [`app/build.gradle.kts`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/build.gradle.kts#L21-L38) using [`keystore.properties`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/keystore.properties):
```properties
storeFile=release.keystore
storePassword=fintracksecure
keyAlias=fintrack
keyPassword=fintracksecure
```
The keystore file `release.keystore` is located in the project root. `assembleRelease` produces a fully signed, aligned release APK ready for direct device sideloading.

---

## 6. Test Guide: Verification & Quality Assurance

### 6.1 Unit Test Suites & Scenarios

The test suite consists of **50 comprehensive unit tests** across 9 test classes in `app/src/test/java/com/example/fintrack`:

1. **`IndianBankSmsParserTest.kt`**:
   * `testHdfcDebit`: UPI debit with ₹ and balance parsing.
   * `testKotakDebit`: Kotak bank debit with closing balance.
   * `testCreditCardSpend`: ICICI card purchase with available credit extraction.
   * `testCreditCardBillPayment`: HDFC card payment with available limit restoration.
   * `testCreditCardStatementBillNotification`: SBI card bill statement with total due, min due, due date.
   * `testCreditCardRefund`: Axis card refund transaction.
   * `testSalaryCredit`: Salary income credit parsing.
   * `testAtmWithdrawal`: ATM cash withdrawal parsing.
   * `testReversal`: Transaction reversal parsing.
   * `testOtpSpamRejection`: Strict rejection of OTPs and promotional loan messages.
   * `testRupeeSymbolParsing`: Unicode `\u20B9` and symbol `₹` parsing.
   * `testDebitCardParsingForcesBankAccount`: Verifies debit card alerts are NEVER classified as credit cards.
   * `testSpamWithLegitimateDebitedNotDropped`: Verifies legitimate alerts with fraud footers are preserved.
2. **`MultiMessageCorrelationTest.kt`**:
   * `testAmazonAlertFollowedByKotakBankAlert`: Merchant alert followed by bank debit merges into 1 transaction.
   * `testBankAlertFollowedByAmazonAlert`: Bank debit followed by merchant alert enriches merchant title.
   * `testSwiggyAlertFollowedByHdfcBankAlert`: Swiggy order + HDFC debit correlation.
   * `testUnrelatedTransactionsDoNotCorrelate`: Verifies different amounts, opposite directions, and stale timestamps (>15 min) do NOT correlate.
3. **`AccountIdentificationEngineTest.kt`**:
   * `testDebitCardResolvesToBankAccountByBankName`: Kotak debit card matches Kotak savings account.
   * `testSoleBankAccountInference`: Automatically infers sole user bank account without prompting user.
4. **`CreditCardIntelligenceTest.kt`**:
   * Scenarios 1-6 testing purchases, reversals, refunds, statement dues, and bill mark as paid workflows.
5. **`UserIsolationTest.kt`**:
   * Verifies User A's financial data cannot be read, updated, or deleted by User B.
6. **`FinancialIntelligenceMathTest.kt`**, **`SmsFingerprintEngineTest.kt`**, **`CreditCardPaymentEngineTest.kt`**, **`MainScreenViewModelTest.kt`**.

---

### 6.2 Running Tests via CLI

* **Run All Unit Tests:**
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```
  *Expected Result:* `BUILD SUCCESSFUL`, 50 tests completed, 0 failed.
* **Run a Specific Test Class:**
  ```powershell
  .\gradlew.bat testDebugUnitTest --tests com.example.fintrack.engine.MultiMessageCorrelationTest
  ```
* **View HTML Test Report:**
  Open `app/build/reports/tests/testDebugUnitTest/index.html` in browser.

---

### 6.3 ADB Physical Device Installation & Live Verification

1. **Verify Connected Device:**
   ```powershell
   adb devices
   ```
   *Expected:* Device listed with `device` state (e.g., `adb-RZCX70CK3XP-odKjAw._adb-tls-connect._tcp device`).
2. **Install Release APK:**
   ```powershell
   adb install -r -d app\build\outputs\apk\release\app-release.apk
   ```
   *Expected Result:* `Performing Streamed Install` -> `Success`.
3. **Launch FinTrack on Device:**
   ```powershell
   adb shell am start -n com.example.fintrack/.MainActivity
   ```
4. **Verify Running Process:**
   ```powershell
   adb shell "dumpsys package com.example.fintrack"
   ```

---

### 6.4 Regression Checklist for Future Agents

Before committing any future modifications, run this verification checklist:
- [ ] `.\gradlew.bat testDebugUnitTest` passes 100% (66/66 tests).
- [ ] Monetary arithmetic uses `Money` (integer paise `Long` & `BigDecimal` scale 2) without IEEE 754 floating-point drift.
- [ ] `PinManager` uses PBKDF2 with 120,000 iterations, transparent auto-migration from legacy SHA-256, and monotonic elapsed lockout.
- [ ] `SecurityManager` decrypt throws `SecurityException` on corrupted ciphertext without silent fallback to plaintext.
- [ ] `IndianBankSmsParser.isMerchantSender(sender)` only inspects sender IDs (never raw message body, preventing `CRED` from matching `CREDITED`).
- [ ] `MERCHANT_PATTERNS` in `extractMerchant` executes **before** fallback `knownMerchants` (preventing truncation of "Amazon India" to "Amazon").
- [ ] Correlated multi-message alerts deduct the ledger balance **exactly once**.
- [ ] Debit cards are classified under `FinancialInstrumentType.BANK_ACCOUNT`, never `CREDIT_CARD`.
- [ ] Statement bill alerts (`CARD_BILL_DUE` / `CARD_BILL_GENERATED`) never set `billStatus = PAID`.
- [ ] SQLite database is at `DATABASE_VERSION = 7` with non-destructive migrations.
- [ ] Debug APK compiles cleanly with `.\gradlew.bat assembleDebug` and release with `.\gradlew.bat assembleRelease`.

---

## 7. Extension Playbook & Golden Rules

### 7.1 How to Add a New Bank Parser

To add support for a new bank (e.g., "Bandhan Bank"):
1. In [`IndianBankSmsParser.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/parser/IndianBankSmsParser.kt):
   * Add sender patterns to `isBankSender`: add `s.contains("BNDHN") || s.contains("BANDHAN")`.
   * Add bank name mapping in `identifyBank`: add `combined.contains("BANDHAN") || combined.contains("BNDHN") -> "Bandhan Bank"`.
2. In [`AccountIdentificationEngine.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/engine/AccountIdentificationEngine.kt):
   * Add `"Bandhan Bank" to listOf("bandhan", "bndhn")` to `BANK_NAME_ALIASES`.
3. Add a unit test in [`IndianBankSmsParserTest.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/test/java/com/example/fintrack/IndianBankSmsParserTest.kt) with an authentic sample SMS from that bank.

---

### 7.2 How to Bump Database Schema (v3 to v4)

If you need to add a new column or table:
1. In [`AppDatabaseHelper.kt`](file:///c:/Users/yatin/Desktop/Workspace1/FinTrack/app/src/main/java/com/example/fintrack/data/local/AppDatabaseHelper.kt):
   * Increment `DATABASE_VERSION` from `3` to `4`.
   * In `onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)`:
     ```kotlin
     if (oldVersion < 4) {
         db.execSQL("ALTER TABLE transactions ADD COLUMN my_new_column TEXT;")
     }
     ```
2. Update domain models and cursor deserializers accordingly.
3. Write a migration test verifying data persistence from v3 to v4.

---

### 7.3 Inviolable System Constraints (Never Break These)

> [!CAUTION]
> **STRICT INVIOLABLE CONSTRAINTS:**
> 1. **NEVER add `android.permission.INTERNET` to `AndroidManifest.xml`:** FinTrack's core value proposition is an absolute, hardware-enforced air gap. Adding internet access destroys user trust.
> 2. **NEVER store financial figures or credentials in plaintext:** Always use `SecurityManager.encrypt()` or store in encrypted SQLite columns.
> 3. **NEVER deduct balance twice on dual notifications:** Always route SMS ingestion through `TransactionCorrelationEngine` before writing debit records.
> 4. **NEVER allow User A to query or mutate User B's records:** Always include `user_id = ?` in database queries.
> 5. **NEVER mark a Credit Card Bill as PAID from a statement message:** Only mark as PAID when a verified payment transaction or manual user confirmation occurs.
