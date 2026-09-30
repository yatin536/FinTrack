package com.example.fintrack.engine

import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountAlias
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.parser.FinancialInstrumentType
import com.example.fintrack.parser.ParsedTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountIdentificationEngineTest {

    private val engine = AccountIdentificationEngine()

    private val hdfcBank = Account(
        id = "hdfc_bank_1",
        name = "HDFC Savings",
        bankName = "HDFC Bank",
        accountType = AccountType.BANK_ACCOUNT,
        accountNumberLast4 = "1234",
        currentBalance = 50000.0
    )

    private val iciciCard = Account(
        id = "icici_card_1",
        name = "ICICI Amazon Pay",
        bankName = "ICICI Bank",
        accountType = AccountType.CREDIT_CARD,
        accountNumberLast4 = "5678",
        currentBalance = 12000.0,
        creditLimit = 100000.0
    )

    private val accounts = listOf(hdfcBank, iciciCard)

    @Test
    fun testExactCreditCardMatch() {
        val raw = "Spent 1500 on ICICI Card XX5678"
        val parsed = ParsedTransaction(
            amount = 1500.0,
            direction = TransactionDirection.DEBIT,
            kind = TransactionKind.CARD_PURCHASE,
            instrumentType = FinancialInstrumentType.CREDIT_CARD,
            bankName = "ICICI Bank",
            accountNumberLast4 = "5678",
            merchant = "Amazon",
            rawSender = "VM-ICICIB",
            rawBody = raw
        )

        val result = engine.identifyAccount(parsed, "VM-ICICIB", raw, accounts, emptyList())

        assertNotNull(result.account)
        assertEquals("icici_card_1", result.account?.id)
        assertTrue("Exact match confidence should be >= 0.90", result.confidence >= 0.90)
        assertFalse(result.needsReview)
    }

    @Test
    fun testExactBankAccountMatch() {
        val raw = "debited from HDFC a/c **1234"
        val parsed = ParsedTransaction(
            amount = 500.0,
            direction = TransactionDirection.DEBIT,
            kind = TransactionKind.EXPENSE,
            instrumentType = FinancialInstrumentType.BANK_ACCOUNT,
            bankName = "HDFC Bank",
            accountNumberLast4 = "1234",
            merchant = "Swiggy",
            rawSender = "VK-HDFCBK",
            rawBody = raw
        )

        val result = engine.identifyAccount(parsed, "VK-HDFCBK", raw, accounts, emptyList())

        assertNotNull(result.account)
        assertEquals("hdfc_bank_1", result.account?.id)
        assertTrue(result.confidence >= 0.90)
        assertFalse(result.needsReview)
    }

    @Test
    fun testAliasMatch() {
        val alias = AccountAlias(
            id = "alias_1",
            accountId = "hdfc_bank_1",
            aliasPattern = "SALARY_AC"
        )

        val raw = "Credited to SALARY_AC INR 75000"
        val parsed = ParsedTransaction(
            amount = 75000.0,
            direction = TransactionDirection.CREDIT,
            kind = TransactionKind.INCOME,
            instrumentType = FinancialInstrumentType.UNKNOWN,
            bankName = "HDFC",
            accountNumberLast4 = "",
            merchant = "Salary",
            rawSender = "VK-HDFCBK",
            rawBody = raw
        )

        val result = engine.identifyAccount(parsed, "VK-HDFCBK", raw, accounts, listOf(alias))

        assertNotNull(result.account)
        assertEquals("hdfc_bank_1", result.account?.id)
        assertTrue(result.confidence >= 0.80)
    }

    @Test
    fun testAmbiguousSenderTriggersReview() {
        val raw = "debited by 300"
        val parsed = ParsedTransaction(
            amount = 300.0,
            direction = TransactionDirection.DEBIT,
            kind = TransactionKind.EXPENSE,
            instrumentType = FinancialInstrumentType.UNKNOWN,
            bankName = "Unknown Bank",
            accountNumberLast4 = "9999",
            merchant = "Merchant",
            rawSender = "XX-UNKWN",
            rawBody = raw
        )

        val result = engine.identifyAccount(parsed, "XX-UNKWN", raw, accounts, emptyList())

        assertTrue("Ambiguous account must flag needsReview", result.needsReview)
        assertTrue("Confidence should be below review threshold", result.confidence < 0.70)
    }
}
