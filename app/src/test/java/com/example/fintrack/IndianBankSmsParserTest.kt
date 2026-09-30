package com.example.fintrack

import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.parser.FinancialInstrumentType
import com.example.fintrack.parser.IndianBankSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndianBankSmsParserTest {

    @Test
    fun testHdfcUpiDebit() {
        val sms = "Rs 450.00 debited from HDFC Bank a/c **1234 on 28-SEP-26 to VPA swiggy@icici (UPI Ref: 426189). Avl Bal: INR 12,450.00"
        val parsed = IndianBankSmsParser.parse("VK-HDFCBK", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(450.0, parsed.amount, 0.001)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
        assertEquals("HDFC Bank", parsed.bankName)
        assertEquals("1234", parsed.accountNumberLast4)
        assertEquals("Swiggy", parsed.merchant)
        assertEquals(12450.0, parsed.availableBalance!!, 0.001)
        assertEquals("426189", parsed.referenceNumber)
        assertEquals(FinancialInstrumentType.BANK_ACCOUNT, parsed.instrumentType)
    }

    @Test
    fun testSbiUpiDebit() {
        val sms = "Dear SBI User, your A/c ending 5678 debited by Rs.1,200.00 on 28Sep26 transfer to ZOMATO Ref No 426189218920. Avail Bal Rs: 25,600.50"
        val parsed = IndianBankSmsParser.parse("AD-SBIINB", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(1200.0, parsed.amount, 0.001)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
        assertEquals("State Bank of India", parsed.bankName)
        assertEquals("5678", parsed.accountNumberLast4)
        assertEquals("Zomato", parsed.merchant)
        assertEquals(25600.50, parsed.availableBalance!!, 0.001)
    }

    @Test
    fun testCreditCardSpend() {
        val sms = "Spent INR 2,499.00 on your ICICI Bank Credit Card XX4321 on 29-Sep-26 at AMAZON INDIA. Available Limit: INR 85,000.00."
        val parsed = IndianBankSmsParser.parse("VM-ICICIB", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(2499.0, parsed.amount, 0.001)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
        assertEquals(FinancialInstrumentType.CREDIT_CARD, parsed.instrumentType)
        assertEquals(TransactionKind.CARD_PURCHASE, parsed.kind)
        assertEquals("4321", parsed.accountNumberLast4)
        assertEquals("Amazon India", parsed.merchant)
        assertEquals(85000.0, parsed.availableCredit!!, 0.001)
    }

    @Test
    fun testCreditCardBillPayment() {
        val sms = "Thank you for payment of Rs 15,000.00 towards your HDFC Bank Credit Card ending 8899 on 25-SEP-26. Total Available Limit: INR 1,35,000.00."
        val parsed = IndianBankSmsParser.parse("VK-HDFCBK", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(15000.0, parsed.amount, 0.001)
        assertEquals(TransactionDirection.CREDIT, parsed.direction)
        assertEquals(FinancialInstrumentType.CREDIT_CARD, parsed.instrumentType)
        assertEquals(TransactionKind.CARD_PAYMENT, parsed.kind)
        assertEquals("8899", parsed.accountNumberLast4)
        assertEquals(135000.0, parsed.availableCredit!!, 0.001)
    }

    @Test
    fun testCreditCardStatementBillNotification() {
        val sms = "Your SBI Card ending 5544 statement is generated. Total Amt Due: Rs 18,450.00, Min Amt Due: Rs 950.00. Payment Due Date: 12-Oct-26."
        val parsed = IndianBankSmsParser.parse("AD-SBICRD", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(FinancialInstrumentType.CREDIT_CARD, parsed.instrumentType)
        assertEquals(18450.0, parsed.totalDue!!, 0.001)
        assertEquals(950.0, parsed.minimumDue!!, 0.001)
        assertEquals("12-Oct-26", parsed.dueDate)
    }

    @Test
    fun testCreditCardRefund() {
        val sms = "Refund of INR 799.00 has been credited to your Axis Bank Credit Card ending 9012 from FLIPKART. Avail Limit: INR 62,000.00."
        val parsed = IndianBankSmsParser.parse("AX-AXISBK", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(799.0, parsed.amount, 0.001)
        assertEquals(TransactionDirection.CREDIT, parsed.direction)
        assertEquals(TransactionKind.REFUND, parsed.kind)
        assertEquals(FinancialInstrumentType.CREDIT_CARD, parsed.instrumentType)
        assertEquals("Flipkart", parsed.merchant)
        assertEquals(62000.0, parsed.availableCredit!!, 0.001)
    }

    @Test
    fun testSalaryCredit() {
        val sms = "Your a/c no. XX1234 is credited for Rs 75,000.00 on 28-09-26 by salary. Avail Bal Rs: 82,450.00 - HDFC Bank"
        val parsed = IndianBankSmsParser.parse("VK-HDFCBK", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(75000.0, parsed.amount, 0.001)
        assertEquals(TransactionDirection.CREDIT, parsed.direction)
        assertEquals("1234", parsed.accountNumberLast4)
        assertEquals("Salary / Payroll", parsed.merchant)
        assertEquals(TransactionKind.INCOME, parsed.kind)
    }

    @Test
    fun testAtmWithdrawal() {
        val sms = "Rs 2,000.00 withdrawn at ATM from a/c **4321 on 28-SEP-26. Avl Bal Rs: 10,450.00"
        val parsed = IndianBankSmsParser.parse("AD-SBIINB", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(2000.0, parsed.amount, 0.001)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
        assertEquals(TransactionKind.ATM_WITHDRAWAL, parsed.kind)
        assertEquals("ATM Cash Withdrawal", parsed.merchant)
    }

    @Test
    fun testReversal() {
        val sms = "Txn of INR 350.00 on A/c **1234 has been reversed. Updated Balance INR 12,800.00."
        val parsed = IndianBankSmsParser.parse("VK-HDFCBK", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(350.0, parsed.amount, 0.001)
        assertEquals(TransactionDirection.CREDIT, parsed.direction)
        assertEquals(TransactionKind.REVERSAL, parsed.kind)
    }

    @Test
    fun testOtpSpamRejection() {
        val otpSms = "492018 is your secret OTP for transaction of INR 450.00 at Swiggy. Do not share this OTP with anyone."
        val parsed = IndianBankSmsParser.parse("VM-HDFCBK", otpSms)
        assertNull("OTP messages must be strictly rejected", parsed)

        val promoSms = "Congratulations! You are pre-approved for personal loan of Rs 5,00,000. Click here to apply now."
        val promoParsed = IndianBankSmsParser.parse("AD-LOAN", promoSms)
        assertNull("Promotional spam must be rejected", promoParsed)
    }
}
