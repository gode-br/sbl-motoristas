package com.example.ui

import java.util.Locale

object FormatUtils {
    fun formatCurrency(value: Double): String {
        return String.format(Locale("pt", "BR"), "R$ %.2f", value)
    }

    fun formatKm(value: Double): String {
        return String.format(Locale("pt", "BR"), "%.1f km", value)
    }

    fun formatHours(value: Double): String {
        return String.format(Locale("pt", "BR"), "%.1f h", value)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        return sdf.format(java.util.Date(timestamp))
    }

    /**
     * Formats raw input digits block as a R$ currency value.
     */
    fun formatInputAsCurrency(input: String): String {
        val clean = input.filter { it.isDigit() }
        if (clean.isEmpty()) return ""
        val parsed = clean.toDouble() / 100.0
        return String.format(Locale("pt", "BR"), "R$ %.2f", parsed)
    }

    /**
     * Parses formatted currency text (e.g. "R$ 1.234,56") back to a Double.
     */
    fun parseCurrency(formattedText: String): Double {
        val clean = formattedText
            .replace("R$", "")
            .replace(" ", "") // non-breaking space
            .replace(" ", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()
        return clean.toDoubleOrNull() ?: 0.0
    }
}
