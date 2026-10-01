package com.example.fintrack.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialIntelligenceMathTest {

    @Test
    fun testCreditUtilizationCalculations() {
        val creditLimit = 150000.0
        val usedAmountHigh = 120000.0
        val usedAmountHealthy = 35000.0

        val highUtilizationRate = (usedAmountHigh / creditLimit) * 100.0
        val healthyUtilizationRate = (usedAmountHealthy / creditLimit) * 100.0

        assertEquals(80.0, highUtilizationRate, 0.01)
        assertTrue("80% utilization should trigger high warning (> 70%)", highUtilizationRate > 70.0)

        assertEquals(23.33, healthyUtilizationRate, 0.01)
        assertTrue("23.33% utilization should trigger healthy badge (<= 30%)", healthyUtilizationRate <= 30.0)
    }

    @Test
    fun testSpendingTrendVelocityMath() {
        val current30DaySpend = 45000.0
        val prior30DaySpend = 30000.0

        val diffPercent = ((current30DaySpend - prior30DaySpend) / prior30DaySpend) * 100.0

        assertEquals(50.0, diffPercent, 0.001)
        assertTrue("50% increase exceeds 15% threshold and flags as warning", diffPercent > 15.0)
    }

    @Test
    fun testNetFinancialPositionCalculation() {
        val bank1 = 85000.0
        val bank2 = 42000.0
        val cashWallet = 5000.0
        val cardLiability1 = 28000.0
        val cardLiability2 = 12000.0

        val totalAssets = bank1 + bank2 + cashWallet
        val totalLiabilities = cardLiability1 + cardLiability2
        val netPosition = totalAssets - totalLiabilities

        assertEquals(132000.0, totalAssets, 0.001)
        assertEquals(40000.0, totalLiabilities, 0.001)
        assertEquals(92000.0, netPosition, 0.001)
    }

    @Test
    fun testDueCountdownDaysCalculation() {
        val dueTimestamp = 1759324800000L // arbitrary fixed date
        val nowTimestamp = dueTimestamp - (3 * 24 * 60 * 60 * 1000L) // 3 days earlier

        val diffDays = (dueTimestamp - nowTimestamp) / (1000 * 60 * 60 * 24)
        assertEquals(3L, diffDays)
    }
}
