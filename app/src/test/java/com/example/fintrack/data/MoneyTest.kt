package com.example.fintrack.data

import com.example.fintrack.data.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun testFromPaiseCreation() {
        val money = Money.fromPaise(12550L)
        assertEquals(12550L, money.paise)
        assertEquals(125.50, money.asDouble, 0.0001)
        assertEquals(BigDecimal("125.50"), money.asBigDecimal)
        assertFalse(money.isZero)
        assertTrue(money.isPositive)
        assertFalse(money.isNegative)
    }

    @Test
    fun testFromDoubleCreation() {
        val money = Money.fromDouble(299.99)
        assertEquals(29999L, money.paise)
        assertEquals(299.99, money.asDouble, 0.0001)

        val zero = Money.fromDouble(0.0)
        assertTrue(zero.isZero)
        assertEquals(Money.ZERO, zero)

        val nan = Money.fromDouble(Double.NaN)
        assertTrue(nan.isZero)

        val inf = Money.fromDouble(Double.POSITIVE_INFINITY)
        assertTrue(inf.isZero)
    }

    @Test
    fun testFromBigDecimalCreation() {
        val bd = BigDecimal("1500.75")
        val money = Money.fromBigDecimal(bd)
        assertEquals(150075L, money.paise)
        assertEquals(bd, money.asBigDecimal)
    }

    @Test
    fun testEliminatesIeee754FloatingPointDrift() {
        // Standard double floating point: 0.1 + 0.2 != 0.3 (0.30000000000000004)
        val m1 = Money.fromDouble(0.10)
        val m2 = Money.fromDouble(0.20)
        val sum = m1 + m2
        assertEquals(Money.fromDouble(0.30), sum)
        assertEquals(30L, sum.paise)

        // 100.05 + 200.07 in IEEE 754 is 300.12000000000006
        val a = Money.fromDouble(100.05)
        val b = Money.fromDouble(200.07)
        val result = a + b
        assertEquals(30012L, result.paise)
        assertEquals(Money.fromDouble(300.12), result)
    }

    @Test
    fun testArithmeticOperations() {
        val m1 = Money.fromPaise(50000L) // 500.00
        val m2 = Money.fromPaise(20000L) // 200.00

        // Addition
        val sum = m1 + m2
        assertEquals(70000L, sum.paise)

        // Subtraction
        val diff = m1 - m2
        assertEquals(30000L, diff.paise)

        // Multiplication with Long
        val multLong = m2 * 3L
        assertEquals(60000L, multLong.paise)

        // Multiplication with Double (with HALF_UP rounding)
        val multDouble = m1 * 1.5
        assertEquals(75000L, multDouble.paise)

        // Division
        val div = m1 / 2L
        assertEquals(25000L, div.paise)

        // Unary minus
        val neg = -m1
        assertEquals(-50000L, neg.paise)
        assertTrue(neg.isNegative)
        assertFalse(neg.isPositive)

        // Absolute value
        val absVal = neg.abs()
        assertEquals(50000L, absVal.paise)
        assertTrue(absVal.isPositive)
    }

    @Test
    fun testComparisons() {
        val m1 = Money.fromDouble(100.00)
        val m2 = Money.fromDouble(200.00)
        val m3 = Money.fromDouble(100.00)

        assertTrue(m1 < m2)
        assertTrue(m1 <= m2)
        assertTrue(m1 <= m3)
        assertTrue(m2 > m1)
        assertTrue(m2 >= m1)
        assertEquals(m1, m3)
        assertTrue(m1.compareTo(m2) < 0)
        assertTrue(m2.compareTo(m1) > 0)
        assertEquals(0, m1.compareTo(m3))
    }

    @Test
    fun testFormatting() {
        val money = Money.fromDouble(12450.50)
        val formatted = money.format("₹")
        assertEquals("₹12,450.50", formatted)

        val formattedDollar = money.format("$")
        assertEquals("$12,450.50", formattedDollar)

        val lakh = Money.fromDouble(100000.00)
        assertEquals("₹100,000.00", lakh.format("₹"))

        val negativeMoney = Money.fromDouble(-450.25)
        assertEquals("-₹450.25", negativeMoney.format("₹"))
    }

    @Test
    fun testSignedFormatting() {
        val pos = Money.fromDouble(500.00)
        assertEquals("+₹500.00", pos.formatSigned("₹"))

        val neg = Money.fromDouble(-500.00)
        assertEquals("-₹500.00", neg.formatSigned("₹"))

        val zero = Money.ZERO
        assertEquals("₹0.00", zero.formatSigned("₹"))
    }
}
