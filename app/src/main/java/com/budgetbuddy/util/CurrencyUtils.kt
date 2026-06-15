package com.budgetbuddy.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    fun toRand(value: Double): String = format.format(value)
}
