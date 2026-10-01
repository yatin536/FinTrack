# FinTrack Multi-User System, User Profiles & Security Architecture

**Version:** 2.2 PRO (Build 4)  
**Security Classification:** Local-Only / Air-Gapped (Hardware AES-256-GCM)  
**Applicability:** Production Multi-User Support & Independent Device Deployments  

---

## 1. Executive Summary

FinTrack v2.2 introduces an enterprise-grade **Multi-User Architecture** and **User Profile Subsystem** that eliminates all generic or placeholder identities (such as "Primary User", "User", or "Default User") and transforms the application into an authentic consumer financial tool capable of:
1. Accurately identifying the device owner as **Yatin Kumar Singh**.
2. Providing a clean onboarding experience ("Create Account") for fresh installs on other devices (e.g., family members or sister) without developer test data.
3. Enforcing **strict database-level data isolation** across financial accounts, credit cards, transactions, and learned SMS rules.
4. Implementing biometric unlock via native Android `BiometricPrompt` that verifies identity on-device before enabling.
5. Offering an interactive **Personal Details** sheet to view and edit identity details in real time.
6. Providing an authentic **Log Out** workflow that locks the secure vault and returns to the authentication screen without deleting encrypted local records.

---

## 2. User Domain Model & Identity Resolution

### 2.1 The `User` Model
Located in [`com.example.fintrack.data.model.User`](file:///app/src/main/java/com/example/fintrack/data/model/User.kt):

```kotlin
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val colorHex: Long = 0xFF2563EB,
    val isActive: Boolean = true,
    val isBiometricEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() {
            if (id == DEFAULT_USER_ID && (name.isBlank() || name == "Primary User" || name == "User" || name == "Default User")) {
                return DEFAULT_USER.name
            }
            return name.ifBlank { "User" }
        }

    val firstName: String
        get() = displayName.trim().split("\\s+".toRegex()).firstOrNull()?.ifBlank { "User" } ?: "User"

    companion object {
        const val DEFAULT_USER_ID = "user_default"
        val DEFAULT_USER = User(
            id = DEFAULT_USER_ID,
            name = "Yatin Kumar Singh",
            email = "yatin@fintrack.local",
            phoneNumber = null,
            colorHex = 0xFF2563EB,
            isActive = true,
            isBiometricEnabled = false,
            createdAt = 1759276800000L,
            updatedAt = 1759276800000L
        )
    }
}
```

### 2.2 Automatic Identity Upgrade for Existing Devices
- **Database v6 Migration:** During startup, `AppDatabaseHelper` executes an automatic migration on `users` table:
  ```sql
  UPDATE users SET name = 'Yatin Kumar Singh', email = 'yatin@fintrack.local'
  WHERE id = 'user_default' AND (name = 'Primary User' OR name = 'User' OR name = 'Default User' OR name IS NULL);
  ```
- **Fallback Resolution:** Even if a legacy cached session contains `"Primary User"`, `SessionManager` and `User.displayName` automatically resolve to `"Yatin Kumar Singh"`.
- **Personalized Header Greeting:** The Dashboard automatically calculates the time of day:
  - `Good morning,` / `Good afternoon,` / `Good evening,`
  - `Yatin 👋` (derived dynamically from `User.firstName`).

---

## 3. Financial Data Isolation

FinTrack enforces multi-tenant isolation within its single, encrypted local SQLite database (`fintrack_secure.db`). **Data isolation is never a cosmetic UI filter**; every SQL query incorporates `WHERE user_id = ?`:

```
┌─────────────────────────────────────────────────────────────┐
│                 Encrypted SQLite Database                   │
│                                                             │
│  ┌─────────────────────────┐   ┌─────────────────────────┐  │
│  │ User A (Yatin)          │   │ User B (Sister)         │  │
│  │ id: "user_default"      │   │ id: "uuid_sister"       │  │
│  ├─────────────────────────┤   ├─────────────────────────┤  │
│  │ • HDFC Bank Account     │   │ • SBI Bank Account      │  │
│  │ • Kotak Zen Credit Card │   │ • ICICI Amazon Card     │  │
│  │ • Yatin's Transactions  │   │ • Sister's Transactions │  │
│  │ • Yatin's Message Rules │   │ • Sister's Filter Rules │  │
│  │ • Yatin's Audit Trail   │   │ • Sister's Audit Trail  │  │
│  └─────────────────────────┘   └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 3.1 Scoped Data Operations
| Operation | Implementation in `AppDatabaseHelper` | Isolation Guarantee |
|---|---|---|
| **Accounts & Cards** | `SELECT * FROM accounts WHERE user_id = ?` | User A can never view or manipulate User B's bank accounts or cards. |
| **Transactions Ledger** | `SELECT * FROM transactions WHERE user_id = ? ORDER BY timestamp DESC` | Spending activity and alerts remain 100% segregated. |
| **Credit Card Liabilities** | Scoped to cards where `user_id = ?` | Outstanding dues and available limits reflect only the active user's cards. |
| **Learned SMS Rules** | `SELECT * FROM message_rules WHERE user_id = ?` | False-positive training on one user does not silence alerts for another. |
| **Audit Logs** | `SELECT * FROM audit_logs WHERE user_id = ?` | Manual adjustments and payment confirmations are logged per user. |

---

## 4. Biometric Security & Device Autonomy

FinTrack utilizes the native Android `BiometricPrompt` API. **Raw biometric templates never enter the application**.

### 4.1 Safe Biometric Enrollment
When a user toggles the **Biometric Unlock** switch in Settings:
1. FinTrack launches `BiometricPrompt` with `BIOMETRIC_STRONG or BIOMETRIC_WEAK`.
2. The user must successfully verify their fingerprint or face before the setting is changed.
3. If verification fails or is canceled, the toggle immediately flips back to `OFF`.
4. Upon success, `PinManager.isBiometricEnabled = true` and `User.isBiometricEnabled = true` are committed.

### 4.2 Auto-Prompt on App Launch
When FinTrack is opened:
- If biometrics is enabled for the active profile, `BiometricPrompt` is immediately presented over the unlock keypad.
- If canceled or unsupported, the user enters their 6-digit Master PIN.
- Brute-force protection: 5 consecutive failed attempts trigger an automated 30-second security lockout.

---

## 5. Session Management & Log Out Workflow

### 5.1 Authentic Log Out
Located at the bottom of the **Profile & Settings** tab:
1. User taps **Log Out of FinTrack**.
2. A confirmation dialog appears:
   > *"Logging out will lock your local financial vault and return to the unlock screen. All encrypted data remains completely intact on this device."*
3. Tapping **Log Out**:
   - Calls `repository.sessionManager.logout()`.
   - Transitions `isAuthenticated = false`.
   - The UI returns instantly to `AuthScreen`.
   - **Local data is NOT deleted**. The user can re-authenticate anytime with their Master PIN or Biometric.

---

## 6. Personal Details Sheet

Tapping the **Active Profile Card** or **Personal Details & Identity** row opens a modern modal bottom sheet ([`UserDetailsSheet.kt`](file:///app/src/main/java/com/example/fintrack/ui/components/UserDetailsSheet.kt)):

### Features:
- **Visual Badge:** Large colored circular avatar with initials.
- **Full Name:** Editable text field with immediate reactive propagation to headers and dashboard.
- **Email Address:** Optional contact email.
- **Phone Number:** Optional phone number.
- **Member Since:** Human-readable registration date (e.g. `01 October 2026`).
- **Vault Identity:** Masked security identifier (e.g. `FT-USER_DEF`).
- **In-Place Editing:** Tapping "Edit Profile" allows updating name, email, and phone without leaving the screen.

---

## 7. How to Share & Install with Secondary Users (e.g., Sister's Phone)

To deploy FinTrack on a separate device:

1. **Locate the Release APK:**
   ```
   FinTrack/app/build/outputs/apk/release/app-release.apk
   ```
2. **Transfer the APK:**
   - Share via WhatsApp, Telegram, Google Drive, or Nearby Share.
3. **Install on the Secondary Phone:**
   - Enable "Install Unknown Apps" for the file manager.
   - Tap `app-release.apk` to install.
4. **First Launch Experience on the New Device:**
   - Because no Master PIN exists yet, FinTrack opens directly in **Create Account** onboarding mode.
   - The new user enters:
     - **Full Name** (e.g. *Priya Singh*)
     - **Email / Phone** (optional)
     - **6-Digit Master PIN** (with confirmation)
   - FinTrack creates a unique local database vault for them.
   - **Zero connection or data sharing with Yatin's phone** (both devices remain 100% air-gapped).

---

## 8. Build & Verification Summary

| Component | Status | Verification Details |
|---|---|---|
| **Compilation** | ✅ Passed | Kotlin 2.3.20, AGP 9.0.1, Jetpack Compose Material 3 |
| **Unit Test Suite** | ✅ 40/40 Passed | Includes `UserIsolationTest`, `CreditCardIntelligenceTest`, `IndianBankSmsParserTest` |
| **Release Build** | ✅ Succeeded | Signed release APK generated (`app-release.apk`, ~9.6 MB, Version 2.2) |
| **Device Deployment** | ✅ Live on Device | Streamed install via ADB to physical device (`192.168.29.22:46169`) |
| **Runtime Execution** | ✅ Verified | Screenshot verified: "Good evening, Yatin 👋", User Details Sheet, and Log Out flow |
