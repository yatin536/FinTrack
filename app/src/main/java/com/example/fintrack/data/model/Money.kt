package com.example.fintrack.data.model

import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Exact monetary representation to prevent IEEE 754 floating-point rounding errors.
 * Stores values internally as an exact count of integer paise (1 INR = 100 paise)
 * and bridges to [BigDecimal] with scale 2 and [RoundingMode.HALF_UP].
 */
data class Money(val paise: Long) : Comparable<Money>, Serializable {

    val asBigDecimal: BigDecimal
        get() = BigDecimal.valueOf(paise, 2)

    val asDouble: Double
        get() = paise / 100.0

    val isZero: Boolean
        get() = paise == 0L

    val isPositive: Boolean
        get() = paise > 0L

    val isNegative: Boolean
        get() = paise < 0L

    operator fun plus(other: Money): Money = Money(this.paise + other.paise)

    operator fun minus(other: Money): Money = Money(this.paise - other.paise)

    operator fun times(factor: Long): Money = Money(this.paise * factor)

    operator fun times(factor: Double): Money {
        val bd = this.asBigDecimal.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP)
        return fromBigDecimal(bd)
    }

    operator fun times(factor: BigDecimal): Money {
        val bd = this.asBigDecimal.multiply(factor).setScale(2, RoundingMode.HALF_UP)
        return fromBigDecimal(bd)
    }

    operator fun div(divisor: Long): Money {
        require(divisor != 0L) { "Division by zero" }
        return Money(this.paise / divisor)
    }

    operator fun unaryMinus(): Money = Money(-this.paise)

    fun abs(): Money = Money(abs(this.paise))

    override fun compareTo(other: Money): Int = this.paise.compareTo(other.paise)

    /**
     * Formats this money amount with standard currency grouping and exact two decimals.
     * E.g. "₹12,450.50" or "$1,234.00".
     */
    fun format(symbol: String = "₹"): String {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("en-IN"))
        val decimalFormat = DecimalFormat("#,##,##0.00", symbols)
        val formattedNumber = decimalFormat.format(asBigDecimal.abs())
        val sign = if (isNegative) "-" else ""
        return "$sign$symbol$formattedNumber"
    }

    /**
     * Formats with an explicit plus or minus sign.
     * E.g. "+₹12,450.50" or "-₹500.00".
     */
    fun formatSigned(symbol: String = "₹"): String {
        if (isZero) return "${symbol}0.00"
        val sign = if (isPositive) "+" else "-"
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("en-IN"))
        val decimalFormat = DecimalFormat("#,##,##0.00", symbols)
        return "$sign$symbol${decimalFormat.format(asBigDecimal.abs())}"
    }

    override fun toString(): String = format()

    companion object {
        val ZERO = Money(0L)

        fun fromPaise(paise: Long): Money = Money(paise)

        fun fromDouble(amount: Double): Money {
            if (amount.isNaN() || amount.isInfinite()) return ZERO
            val bd = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
            return fromBigDecimal(bd)
        }

        fun fromBigDecimal(amount: BigDecimal): Money {
            val scaled = amount.setScale(2, RoundingMode.HALF_UP)
            val paiseVal = scaled.movePointRight(2).longValueExact()
            return Money(paiseVal)
        }

        fun fromString(str: String?, default: Money = ZERO): Money {
            if (str.isNullOrBlank()) return default
            val clean = str.replace(",", "").replace("₹", "").replace("$", "").trim()
            return try {
                fromBigDecimal(BigDecimal(clean))
            } catch (_: Exception) {
                default
            }
        }
    }
}

fun Double.toMoney(): Money = Money.fromDouble(this)
fun Long.toMoneyFromPaise(): Money = Money.fromPaise(this)
fun BigDecimal.toMoney(): Money = Money.fromBigDecimal(this)

fun Iterable<Money>.sumMoney(): Money = fold(Money.ZERO) { acc, m -> acc + m }
