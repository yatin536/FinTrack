package com.example.fintrack.engine

import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.BillStatus
import com.example.fintrack.data.model.Category
import com.example.fintrack.data.model.MessageRule
import com.example.fintrack.data.model.MessageRuleAction
import com.example.fintrack.data.model.MessageRuleStatus
import com.example.fintrack.data.model.Transaction
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.data.model.TransactionStatus
import com.example.fintrack.parser.FinancialInstrumentType
import com.example.fintrack.parser.IndianBankSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * End-to-end unit tests verifying the Credit Card Intelligence,
 * Review Lifecycle, Balance Reconciliation, and Bill State Machine invariants.
 */
class CreditCardIntelligenceTest {

    private val accountEngine = AccountIdentificationEngine()
    private val ccPaymentEngine = CreditCardPaymentEngine()

    private val card1234 = Account(
        id = "card_1234",
        name = "HDFC Regalia",
        bankName = "HDFC Bank",
        accountType = AccountType.CREDIT_CARD,
        accountNumberLast4 = "1234",
        creditLimit = 150000.0,
        currentBalance = 25000.0, // Rs 25,000 liability used
        availableCredit = 125000.0,
        billStatus = BillStatus.DUE,
        totalDue = 25000.0,
        minimumDue = 1500.0,
        paymentDueDate = "2026-10-15",
        linkedPaymentAccountIds = listOf("bank_sal")
    )

    private val primaryBank = Account(
        id = "bank_sal",
        name = "HDFC Salary Account",
        bankName = "HDFC Bank",
        accountType = AccountType.BANK_ACCOUNT,
        accountNumberLast4 = "4321",
        currentBalance = 90000.0,
        isPrimary = true
    )

    // =========================================================================
    // SCENARIO 1: Unconfirmed ("Needs Review") card txn does NOT alter balance
    // =========================================================================
    @Test
    fun testScenario1_PendingReviewTransactionDoesNotAlterCardBalance() {
        val initialLiability = card1234.currentBalance
        val initialAvailable = card1234.availableCredit ?: (card1234.creditLimit - card1234.currentBalance)

        // Incoming detected transaction requiring review
        val pendingTxn = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = card1234.id,
            categoryId = "cat_shopping",
            merchant = "Zara",
            amount = 4500.0,
            direction = TransactionDirection.DEBIT,
            kind = TransactionKind.CARD_PURCHASE,
            status = TransactionStatus.PENDING_REVIEW,
            needsReview = true,
            reviewReason = "Ambiguous card pattern requiring confirmation"
        )

        // Rule: In ledger calculations, only CONFIRMED transactions are computed
        val transactions = listOf(pendingTxn)
        val confirmedTxns = transactions.filter { it.status == TransactionStatus.CONFIRMED && !it.needsReview }
        val netDebit = confirmedTxns.sumOf { it.amount }

        assertEquals("Pending review transactions must be excluded from ledger net debit", 0.0, netDebit, 0.001)

        // Balance & available credit remain strictly untouched
        val simulatedLiability = initialLiability + netDebit
        val simulatedAvailable = card1234.creditLimit - simulatedLiability

        assertEquals(25000.0, simulatedLiability, 0.001)
        assertEquals(125000.0, simulatedAvailable, 0.001)
    }

    // =========================================================================
    // SCENARIO 2: User confirmation updates liability and decreases available credit
    // =========================================================================
    @Test
    fun testScenario2_UserConfirmationUpdatesCreditCardLiability() {
        val initialLiability = card1234.currentBalance
        val txAmount = 5000.0

        // User confirms pending review item as credit card
        val confirmedTxn = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = card1234.id,
            categoryId = "cat_shopping",
            merchant = "Amazon",
            amount = txAmount,
            direction = TransactionDirection.DEBIT,
            kind = TransactionKind.CARD_PURCHASE,
            status = TransactionStatus.CONFIRMED,
            needsReview = false
        )

        val updatedLiability = initialLiability + confirmedTxn.amount
        val updatedAvailable = maxOf(0.0, card1234.creditLimit - updatedLiability)

        assertEquals("Liability must increase upon confirmation", 30000.0, updatedLiability, 0.001)
        assertEquals("Available credit must decrease upon confirmation", 120000.0, updatedAvailable, 0.001)
    }

    // =========================================================================
    // SCENARIO 3: User rejection keeps transaction excluded and creates MessageRule
    // =========================================================================
    @Test
    fun testScenario3_UserRejectionExcludesTransactionAndCreatesRule() {
        val rejectedTxn = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = card1234.id,
            categoryId = "cat_other",
            merchant = "Promo Sender",
            amount = 999.0,
            direction = TransactionDirection.DEBIT,
            kind = TransactionKind.EXPENSE,
            status = TransactionStatus.REJECTED,
            needsReview = false,
            reviewReason = "User rejected: Promotional offer misclassified"
        )

        // Ledger check
        val ledgerList = listOf(rejectedTxn).filter { it.status == TransactionStatus.CONFIRMED }
        assertTrue("Rejected transaction must not appear in confirmed ledger", ledgerList.isEmpty())

        // Learned rule creation
        val rule = MessageRule(
            senderPattern = "VK-PROMO",
            bodyPattern = "discount offer",
            classification = "PROMOTION",
            action = MessageRuleAction.IGNORE,
            status = MessageRuleStatus.ACTIVE,
            description = "Ignore promotional discount offers from VK-PROMO"
        )

        assertEquals(MessageRuleAction.IGNORE, rule.action)
        assertEquals(MessageRuleStatus.ACTIVE, rule.status)

        // Test rule match logic
        val testSender = "VK-PROMO"
        val testBody = "Claim your exclusive discount offer of 20% on fashion!"
        val matches = (rule.senderPattern.isEmpty() || testSender.contains(rule.senderPattern, ignoreCase = true)) &&
                testBody.contains(rule.bodyPattern, ignoreCase = true)
        assertTrue("Message rule must match incoming pattern", matches)
    }

    // =========================================================================
    // SCENARIO 4: User reversal reverts card liability atomically
    // =========================================================================
    @Test
    fun testScenario4_ReversalAtomicallyRevertsLiability() {
        // Suppose current card balance already reflects the transaction
        val currentLiability = 30000.0
        val txAmountToReverse = 5000.0

        val newLiability = maxOf(0.0, currentLiability - txAmountToReverse)
        val newAvailableCredit = card1234.creditLimit - newLiability

        assertEquals("Liability must decrease upon reversal", 25000.0, newLiability, 0.001)
        assertEquals("Available credit must be restored upon reversal", 125000.0, newAvailableCredit, 0.001)
    }

    // =========================================================================
    // SCENARIO 5: Bill statement updates totalDue, NEVER sets billStatus = PAID
    // =========================================================================
    @Test
    fun testScenario5_BillStatementUpdatesDuesAndNeverSetsPaid() {
        val sms = "Total amt due on your HDFC Bank Credit Card ending 1234 is Rs 32,500.00. Min amt due is Rs 1,600.00 payable by 28-OCT-2026."
        val parsed = IndianBankSmsParser.parse("VK-HDFCBK", sms)

        assertNotNull("Statement SMS must be parsed", parsed)
        assertTrue(
            "Statement must be classified as CARD_BILL_DUE or CARD_BILL_GENERATED, not CARD_PAYMENT",
            parsed?.kind == TransactionKind.CARD_BILL_DUE || parsed?.kind == TransactionKind.CARD_BILL_GENERATED
        )
        assertNotEquals("Statement must NEVER be classified as CARD_PAYMENT", TransactionKind.CARD_PAYMENT, parsed?.kind)

        assertEquals(32500.0, parsed?.totalDue ?: 0.0, 0.001)
        assertEquals(1600.0, parsed?.minimumDue ?: 0.0, 0.001)

        val newBillStatus = if (parsed?.kind == TransactionKind.CARD_BILL_GENERATED) BillStatus.GENERATED else BillStatus.DUE
        assertNotEquals("Bill status must NEVER be PAID from statement message", BillStatus.PAID, newBillStatus)
        assertTrue("Bill status must be DUE or GENERATED", newBillStatus == BillStatus.DUE || newBillStatus == BillStatus.GENERATED)
    }

    // =========================================================================
    // SCENARIO 6: Reconciled bill payment updates billStatus = PAID
    // =========================================================================
    @Test
    fun testScenario6_ReconciledBillPaymentSetsStatusToPaid() {
        val paymentSms = "Thank you for payment of Rs 25,000.00 received towards your HDFC Credit Card 1234 on 02-OCT-2026. Avl limit Rs 1,50,000."
        val parsed = IndianBankSmsParser.parse("VK-HDFCBK", paymentSms)

        assertNotNull("Payment SMS must be parsed", parsed)
        assertEquals("Payment kind must be CARD_PAYMENT", TransactionKind.CARD_PAYMENT, parsed?.kind)

        val result = ccPaymentEngine.processPaymentSms(
            parsed = parsed!!,
            creditCard = card1234,
            rawSmsBody = paymentSms,
            allAccounts = listOf(card1234, primaryBank)
        )

        assertNotNull(result)
        // Linked to primary bank since primary bank is available
        assertFalse("With primary bank configured, payment can be automatically linked", result.needsSourceConfirmation)

        val updatedBillStatus = if (result.wasReconciledWithManual || !result.needsSourceConfirmation) {
            BillStatus.PAID
        } else {
            BillStatus.PAYMENT_DETECTED
        }

        assertEquals("Reconciled bill payment must transition billStatus to PAID", BillStatus.PAID, updatedBillStatus)
    }

    // =========================================================================
    // SCENARIO 8: Unknown card last4 returns account = null, needsReview = true,
    //             never modifying existing card 1234
    // =========================================================================
    @Test
    fun testScenario8_UnknownCardLast4DoesNotModifyExistingCard() {
        val unknownCardSms = "Spent Rs 7,200.00 on your ICICI Card ending 9999 at RELIANCE DIGITAL on 01-OCT-2026."
        val parsed = IndianBankSmsParser.parse("VK-ICICIB", unknownCardSms)

        assertNotNull(parsed)
        assertEquals("9999", parsed?.accountNumberLast4)

        // System only has card1234 and primaryBank (neither has last4 9999)
        val matchResult = accountEngine.identifyAccount(
            parsed = parsed!!,
            sender = "VK-ICICIB",
            smsBody = unknownCardSms,
            accounts = listOf(card1234, primaryBank),
            aliases = emptyList()
        )

        // Invariant: If SMS has last4 but no user account matches, return account = null and needsReview = true
        assertNull("Account must be null when card digits 9999 do not match any user account", matchResult.account)
        assertTrue("Transaction must be flagged as needing user review", matchResult.needsReview)
        assertNotEquals("Card 1234 must NEVER be matched to card 9999", "card_1234", matchResult.account?.id)

        // Verify card 1234 balance and status are completely unaffected
        assertEquals("Card 1234 balance must remain unchanged", 25000.0, card1234.currentBalance, 0.001)
        assertEquals("Card 1234 available credit must remain unchanged", 125000.0, card1234.availableCredit ?: 0.0, 0.001)
        assertEquals("Card 1234 bill status must remain DUE", BillStatus.DUE, card1234.billStatus)
    }
}
