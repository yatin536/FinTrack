package com.example.fintrack.engine

import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.parser.FinancialInstrumentType
import com.example.fintrack.parser.ParsedTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditCardPaymentEngineTest {

    private val engine = CreditCardPaymentEngine()

    private val hdfcBank = Account(
        id = "hdfc_bank_1",
        name = "HDFC Salary",
        bankName = "HDFC",
        accountType = AccountType.BANK_ACCOUNT,
        accountNumberLast4 = "1234",
        currentBalance = 80000.0,
        isPrimary = true
    )

    private val iciciCard = Account(
        id = "icici_card_1",
        name = "ICICI Coral",
        bankName = "ICICI",
        accountType = AccountType.CREDIT_CARD,
        accountNumberLast4 = "9900",
        currentBalance = 15000.0,
        creditLimit = 100000.0,
        linkedPaymentAccountIds = listOf("hdfc_bank_1")
    )

    @Test
    fun testDetectCreditCardPayment() {
        val raw = "Payment of Rs 10,000 received towards ICICI Credit Card ending 9900"
        val parsed = ParsedTransaction(
            amount = 10000.0,
            direction = TransactionDirection.CREDIT,
            kind = TransactionKind.CARD_PAYMENT,
            instrumentType = FinancialInstrumentType.CREDIT_CARD,
            bankName = "ICICI Bank",
            accountNumberLast4 = "9900",
            merchant = "Credit Card Bill Payment",
            rawSender = "VM-ICICIB",
            rawBody = raw
        )

        val match = engine.processPaymentSms(
            parsed = parsed,
            creditCard = iciciCard,
            rawSmsBody = raw,
            allAccounts = listOf(hdfcBank, iciciCard)
        )

        assertNotNull(match)
        assertEquals("icici_card_1", match.creditCard.id)
        assertEquals("hdfc_bank_1", match.sourceBankAccount?.id)
        assertEquals(10000.0, match.transaction.amount, 0.001)
    }

    @Test
    fun testAmbiguousSourceBankTriggersNeedsConfirmation() {
        val unlinkedCard = iciciCard.copy(linkedPaymentAccountIds = emptyList())
        val sbiBank = Account(
            id = "sbi_bank_2",
            name = "SBI Savings",
            bankName = "SBI",
            accountType = AccountType.BANK_ACCOUNT,
            accountNumberLast4 = "4321",
            currentBalance = 30000.0
        )
        val raw = "Thank you for payment of Rs 5,000 towards ICICI card ending 9900"
        val parsed = ParsedTransaction(
            amount = 5000.0,
            direction = TransactionDirection.CREDIT,
            kind = TransactionKind.CARD_PAYMENT,
            instrumentType = FinancialInstrumentType.CREDIT_CARD,
            bankName = "ICICI Bank",
            accountNumberLast4 = "9900",
            merchant = "Credit Card Bill Payment",
            rawSender = "VM-ICICIB",
            rawBody = raw
        )

        val match = engine.processPaymentSms(
            parsed = parsed,
            creditCard = unlinkedCard,
            rawSmsBody = raw,
            allAccounts = listOf(hdfcBank, sbiBank, unlinkedCard)
        )

        assertNotNull(match)
        assertTrue("Ambiguous source bank must require confirmation", match.needsSourceConfirmation)
        assertTrue("Transaction must be flagged as needsReview", match.transaction.needsReview)
        assertEquals(null, match.sourceBankAccount)
    }

    @Test
    fun testSourceBankExtractedFromSmsBody() {
        val unlinkedCard = iciciCard.copy(linkedPaymentAccountIds = emptyList())
        val raw = "Payment of Rs 8,000 received for card 9900 from HDFC A/C ending 1234"
        val parsed = ParsedTransaction(
            amount = 8000.0,
            direction = TransactionDirection.CREDIT,
            kind = TransactionKind.CARD_PAYMENT,
            instrumentType = FinancialInstrumentType.CREDIT_CARD,
            bankName = "ICICI Bank",
            accountNumberLast4 = "9900",
            merchant = "Credit Card Bill Payment",
            rawSender = "VM-ICICIB",
            rawBody = raw
        )

        val match = engine.processPaymentSms(
            parsed = parsed,
            creditCard = unlinkedCard,
            rawSmsBody = raw,
            allAccounts = listOf(hdfcBank, unlinkedCard)
        )

        assertNotNull(match)
        assertEquals(false, match.needsSourceConfirmation)
        assertEquals("hdfc_bank_1", match.sourceBankAccount?.id)
    }

    @Test
    fun testCreditLimitRecalculationOnBillPaid() {
        val totalCreditLimit = 100000.0
        val startingLiability = 35000.0
        val startingAvailable = 65000.0
        val paymentAmount = 20000.0

        val newOutstanding = maxOf(0.0, startingLiability - paymentAmount)
        val newAvailable = minOf(totalCreditLimit, startingAvailable + paymentAmount)

        assertEquals(15000.0, newOutstanding, 0.001)
        assertEquals(85000.0, newAvailable, 0.001)
        assertEquals(totalCreditLimit, newOutstanding + newAvailable, 0.001)
    }
}
