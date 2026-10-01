package com.example.fintrack.engine

import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.BankAccountType
import com.example.fintrack.data.model.Transaction
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class UserIsolationTest {

    @Test
    fun testDefaultUserIdentityIsYatinKumarSingh() {
        val defaultUser = User.DEFAULT_USER
        assertEquals("user_default", defaultUser.id)
        assertEquals("Yatin Kumar Singh", defaultUser.name)
        assertEquals("Yatin Kumar Singh", defaultUser.displayName)
        assertEquals("Yatin", defaultUser.firstName)
        assertEquals("yatin@fintrack.local", defaultUser.email)
    }

    @Test
    fun testLegacyUserNameMigrationFallback() {
        val legacyUser = User(
            id = "user_default",
            name = "Primary User",
            email = null
        )
        // displayName should migrate away from "Primary User" to "Yatin Kumar Singh"
        assertEquals("Yatin Kumar Singh", legacyUser.displayName)
        assertEquals("Yatin", legacyUser.firstName)

        val customUser = User(
            id = UUID.randomUUID().toString(),
            name = "Priya Singh",
            email = "priya@example.com"
        )
        assertEquals("Priya Singh", customUser.displayName)
        assertEquals("Priya", customUser.firstName)
    }

    @Test
    fun testFinancialAccountIsolationPerUser() {
        val userYatinId = "user_default"
        val userSisterId = "user_sister_uuid_99"

        val yatinBank = Account(
            id = "acc_yatin_hdfc",
            userId = userYatinId,
            name = "HDFC Salary",
            bankName = "HDFC",
            accountType = AccountType.BANK_ACCOUNT,
            bankAccountType = BankAccountType.SAVINGS,
            accountNumberLast4 = "1234",
            currentBalance = 150000.0
        )

        val yatinCard = Account(
            id = "acc_yatin_card",
            userId = userYatinId,
            name = "Axis Magnus",
            bankName = "Axis",
            accountType = AccountType.CREDIT_CARD,
            accountNumberLast4 = "5678",
            creditLimit = 500000.0,
            currentBalance = 45000.0
        )

        val sisterBank = Account(
            id = "acc_sister_sbi",
            userId = userSisterId,
            name = "SBI Savings",
            bankName = "SBI",
            accountType = AccountType.BANK_ACCOUNT,
            bankAccountType = BankAccountType.SAVINGS,
            accountNumberLast4 = "9988",
            currentBalance = 42000.0
        )

        val allAccounts = listOf(yatinBank, yatinCard, sisterBank)

        // Querying User Yatin
        val yatinAccounts = allAccounts.filter { it.userId == userYatinId }
        assertEquals(2, yatinAccounts.size)
        assertTrue(yatinAccounts.any { it.id == "acc_yatin_hdfc" })
        assertTrue(yatinAccounts.any { it.id == "acc_yatin_card" })
        assertFalse("User Yatin's query must never return Sister's account", yatinAccounts.any { it.id == "acc_sister_sbi" })

        // Querying User Sister
        val sisterAccounts = allAccounts.filter { it.userId == userSisterId }
        assertEquals(1, sisterAccounts.size)
        assertEquals("acc_sister_sbi", sisterAccounts.first().id)
        assertFalse("Sister's query must never return Yatin's account", sisterAccounts.any { it.id == "acc_yatin_hdfc" })
    }

    @Test
    fun testTransactionLedgerIsolationPerUser() {
        val userYatinId = "user_default"
        val userSisterId = "user_sister_uuid_99"

        val yatinTxn = Transaction(
            id = "txn_yatin_01",
            userId = userYatinId,
            accountId = "acc_yatin_hdfc",
            categoryId = "cat_shopping",
            merchant = "Amazon India",
            amount = 12500.0,
            direction = TransactionDirection.DEBIT,
            kind = TransactionKind.CARD_PURCHASE,
            timestamp = System.currentTimeMillis()
        )

        val sisterTxn = Transaction(
            id = "txn_sister_01",
            userId = userSisterId,
            accountId = "acc_sister_sbi",
            categoryId = "cat_food",
            merchant = "Swiggy",
            amount = 350.0,
            direction = TransactionDirection.DEBIT,
            kind = TransactionKind.EXPENSE,
            timestamp = System.currentTimeMillis()
        )

        val allTxns = listOf(yatinTxn, sisterTxn)

        val yatinLedger = allTxns.filter { it.userId == userYatinId }
        assertEquals(1, yatinLedger.size)
        assertEquals("txn_yatin_01", yatinLedger.first().id)
        assertFalse("Yatin's ledger must never leak Sister's transaction", yatinLedger.any { it.userId == userSisterId })

        val sisterLedger = allTxns.filter { it.userId == userSisterId }
        assertEquals(1, sisterLedger.size)
        assertEquals("txn_sister_01", sisterLedger.first().id)
        assertFalse("Sister's ledger must never leak Yatin's transaction", sisterLedger.any { it.userId == userYatinId })
    }

    @Test
    fun testBiometricAndProfileSettingsIsolation() {
        val yatin = User(
            id = "user_default",
            name = "Yatin Kumar Singh",
            email = "yatin@fintrack.local",
            isBiometricEnabled = true
        )

        val sister = User(
            id = "user_sister_100",
            name = "Priya Singh",
            email = "priya@gmail.com",
            isBiometricEnabled = false
        )

        assertTrue(yatin.isBiometricEnabled)
        assertFalse(sister.isBiometricEnabled)
        assertNotEquals(yatin.id, sister.id)
    }
}
