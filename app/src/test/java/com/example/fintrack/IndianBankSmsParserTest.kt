package com.example.fintrack

import com.example.fintrack.data.model.TransactionType
import com.example.fintrack.parser.IndianBankSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class IndianBankSmsParserTest {

    @Test
    fun testHdfcUpiDebit() {
        val sms = "Rs 450.00 debited from HDFC Bank a/c **1234 on 28-SEP-26 to VPA swiggy@icici (UPI Ref: 426189). Avl Bal: INR 12,450.00"
        val parsed = IndianBankSmsParser.parse("VK-HDFCBK", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(450.0, parsed.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals("HDFC Bank", parsed.bankName)
        assertEquals("1234", parsed.accountNumberLast4)
        assertEquals("Swiggy", parsed.merchant)
        assertEquals(12450.0, parsed.balanceAfterTxn!!, 0.001)
        assertEquals("426189", parsed.referenceNumber)
    }

    @Test
    fun testSbiUpiDebit() {
        val sms = "Dear SBI User, your A/c ending 5678 debited by Rs.1,200.00 on 28Sep26 transfer to ZOMATO Ref No 426189218920. Avail Bal Rs: 25,600.50"
        val parsed = IndianBankSmsParser.parse("AD-SBIINB", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(1200.0, parsed.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals("State Bank of India", parsed.bankName)
        assertEquals("5678", parsed.accountNumberLast4)
        assertEquals("Zomato", parsed.merchant)
        assertEquals(25600.50, parsed.balanceAfterTxn!!, 0.001)
    }

    @Test
    fun testIciciDebit() {
        val sms = "Acct XX9012 debited for Rs 850.00 on 28-Sep-26. UPI:426189. Info:AMAZON PAY. Available Balance INR 40,120.00"
        val parsed = IndianBankSmsParser.parse("VM-ICICIB", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(850.0, parsed.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals("ICICI Bank", parsed.bankName)
        assertEquals("9012", parsed.accountNumberLast4)
        assertEquals("Amazon Pay", parsed.merchant)
        assertEquals(40120.0, parsed.balanceAfterTxn!!, 0.001)
    }

    @Test
    fun testSalaryCredit() {
        val sms = "Your a/c no. XX1234 is credited for Rs 75,000.00 on 28-09-26 by salary. Avail Bal Rs: 82,450.00 - HDFC Bank"
        val parsed = IndianBankSmsParser.parse("VK-HDFCBK", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(75000.0, parsed.amount, 0.001)
        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals("1234", parsed.accountNumberLast4)
        assertEquals("Salary / Payroll", parsed.merchant)
    }

    @Test
    fun testAtmWithdrawal() {
        val sms = "Rs 2,000.00 withdrawn at ATM from a/c **4321 on 28-SEP-26. Avl Bal Rs: 10,450.00"
        val parsed = IndianBankSmsParser.parse("AD-SBIINB", sms)

        assertNotNull(parsed)
        parsed!!
        assertEquals(2000.0, parsed.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals("ATM Cash Withdrawal", parsed.merchant)
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
