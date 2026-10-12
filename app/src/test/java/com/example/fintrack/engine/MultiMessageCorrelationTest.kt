package com.example.fintrack.engine

import com.example.fintrack.data.model.Transaction
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.parser.FinancialInstrumentType
import com.example.fintrack.parser.IndianBankSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MultiMessageCorrelationTest {

    @Test
    fun testAmazonAlertFollowedByKotakBankAlert() {
        val now = System.currentTimeMillis()

        // 1. Existing Transaction recorded from Amazon SMS
        val amazonRaw = "Amazon: Order placed for INR 1,499.00 using Kotak Bank A/c ending 1234. Expected delivery by 14 Oct."
        val amazonParsed = IndianBankSmsParser.parse("AX-AMAZON", amazonRaw)!!

        val existingTxn = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = "kotak_bank_1",
            amount = amazonParsed.amount,
            direction = amazonParsed.direction,
            kind = amazonParsed.kind,
            timestamp = now,
            merchant = amazonParsed.merchant,
            rawSmsBody = amazonRaw,
            smsSender = "AX-AMAZON",
            referenceNumber = null,
            balanceAfterTxn = null
        )

        // 2. Incoming Bank SMS arriving 45 seconds later
        val bankRaw = "Kotak Bank: Rs 1499.00 debited from A/c 1234 on 12-Oct-26 towards Amazon Pay. Avl bal: Rs 18,501.00. UTR 987654321."
        val bankParsed = IndianBankSmsParser.parse("VK-KOTAKB", bankRaw)!!

        val isMatch = TransactionCorrelationEngine.isCorrelated(
            existing = existingTxn,
            incoming = bankParsed,
            incomingSender = "VK-KOTAKB",
            incomingBody = bankRaw
        )

        assertTrue("Amazon order and Kotak Bank debit must correlate as a single purchase", isMatch)

        val mergedMerchant = TransactionCorrelationEngine.resolveMergedMerchant(
            existingMerchant = existingTxn.merchant,
            existingSender = existingTxn.smsSender,
            existingBody = existingTxn.rawSmsBody,
            incomingMerchant = bankParsed.merchant,
            incomingSender = "VK-KOTAKB",
            incomingBody = bankRaw
        )

        assertEquals("Merchant title must resolve to clean Amazon title", "Amazon", mergedMerchant)
    }

    @Test
    fun testBankAlertFollowedByAmazonAlert() {
        val now = System.currentTimeMillis()

        // 1. Existing Bank Alert arriving first with generic POS/UPI narration
        val bankRaw = "Kotak Bank: INR 1,499.00 debited from A/c 1234 towards POS AMZN*PAY 481920. Avl bal: INR 18,501.00. UTR 987654321."
        val bankParsed = IndianBankSmsParser.parse("VK-KOTAKB", bankRaw)!!

        val existingTxn = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = "kotak_bank_1",
            amount = bankParsed.amount,
            direction = bankParsed.direction,
            kind = bankParsed.kind,
            timestamp = now,
            merchant = "Pos Amzn*Pay",
            rawSmsBody = bankRaw,
            smsSender = "VK-KOTAKB",
            referenceNumber = "987654321",
            balanceAfterTxn = 18501.0
        )

        // 2. Merchant SMS arriving 30 seconds later
        val amazonRaw = "Amazon: Order placed for INR 1,499.00 using Kotak Bank A/c ending 1234."
        val amazonParsed = IndianBankSmsParser.parse("AX-AMAZON", amazonRaw)!!

        val isMatch = TransactionCorrelationEngine.isCorrelated(
            existing = existingTxn,
            incoming = amazonParsed,
            incomingSender = "AX-AMAZON",
            incomingBody = amazonRaw
        )

        assertTrue("Bank debit followed by Amazon order must correlate", isMatch)

        val mergedMerchant = TransactionCorrelationEngine.resolveMergedMerchant(
            existingMerchant = existingTxn.merchant,
            existingSender = existingTxn.smsSender,
            existingBody = existingTxn.rawSmsBody,
            incomingMerchant = amazonParsed.merchant,
            incomingSender = "AX-AMAZON",
            incomingBody = amazonRaw
        )

        assertEquals("Generic bank narration must be enriched with Amazon merchant title", "Amazon", mergedMerchant)
    }

    @Test
    fun testSwiggyAlertFollowedByHdfcBankAlert() {
        val now = System.currentTimeMillis()

        val swiggyRaw = "Swiggy: Order #89201 for Rs 450.00 confirmed. Delivering to your address."
        val swiggyParsed = IndianBankSmsParser.parse("VM-SWIGGY", swiggyRaw)!!

        val existingTxn = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = "hdfc_bank_1",
            amount = swiggyParsed.amount,
            direction = swiggyParsed.direction,
            kind = TransactionKind.EXPENSE,
            timestamp = now,
            merchant = "Swiggy",
            rawSmsBody = swiggyRaw,
            smsSender = "VM-SWIGGY"
        )

        val bankRaw = "Rs 450.00 debited from HDFC Bank a/c **1234 on 28-SEP-26 to VPA swiggy@icici (UPI Ref: 426189). Avl Bal: INR 12,450.00"
        val bankParsed = IndianBankSmsParser.parse("VK-HDFCBK", bankRaw)!!

        val isMatch = TransactionCorrelationEngine.isCorrelated(
            existing = existingTxn,
            incoming = bankParsed,
            incomingSender = "VK-HDFCBK",
            incomingBody = bankRaw
        )

        assertTrue("Swiggy food order and HDFC Bank UPI debit must correlate", isMatch)
    }

    @Test
    fun testUnrelatedTransactionsDoNotCorrelate() {
        val now = System.currentTimeMillis()

        val txn1 = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = "kotak_bank_1",
            amount = 1499.0,
            direction = TransactionDirection.DEBIT,
            kind = TransactionKind.EXPENSE,
            timestamp = now,
            merchant = "Amazon",
            rawSmsBody = "Amazon order Rs 1499",
            smsSender = "AX-AMAZON"
        )

        // Different amount
        val differentAmountParsed = IndianBankSmsParser.parse("VK-KOTAKB", "Kotak Bank: Rs 200 debited from A/c 1234 at Swiggy")!!
        assertFalse(
            "Different amount must not correlate",
            TransactionCorrelationEngine.isCorrelated(txn1, differentAmountParsed, "VK-KOTAKB", "Kotak Bank: Rs 200 debited from A/c 1234 at Swiggy")
        )

        // Different direction (Credit vs Debit)
        val creditParsed = IndianBankSmsParser.parse("VK-KOTAKB", "Kotak Bank: Rs 1499 credited to A/c 1234 from Salary")!!
        assertFalse(
            "Opposite direction must not correlate",
            TransactionCorrelationEngine.isCorrelated(txn1, creditParsed, "VK-KOTAKB", "Kotak Bank: Rs 1499 credited to A/c 1234 from Salary")
        )

        // Beyond 15-minute window
        val staleTxn = txn1.copy(timestamp = now - 3600000L) // 1 hour ago
        val bankParsed = IndianBankSmsParser.parse("VK-KOTAKB", "Kotak Bank: Rs 1499 debited from A/c 1234 towards Amazon")!!
        assertFalse(
            "Outside time window must not correlate",
            TransactionCorrelationEngine.isCorrelated(staleTxn, bankParsed, "VK-KOTAKB", "Kotak Bank: Rs 1499 debited from A/c 1234 towards Amazon")
        )
    }
}
