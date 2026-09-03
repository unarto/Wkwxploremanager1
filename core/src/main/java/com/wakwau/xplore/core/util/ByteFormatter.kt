// [Jalur Class]: com.wakwau.xplore.core.util.ByteFormatter
// [Penjelasan]: Utilitas pemformat byte untuk konversi ukuran berkas ke satuan terstruktur dan format detail dengan tanda petik pemisah ribuan.
package com.wakwau.xplore.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object ByteFormatter {
    private val decimalFormat = DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale.US))

    fun format(bytes: Long): String {
        if (bytes < 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "${decimalFormat.format(kb)} KB"
        val mb = kb / 1024.0
        if (mb < 1024) return "${decimalFormat.format(mb)} MB"
        val gb = mb / 1024.0
        if (gb < 1024) return "${decimalFormat.format(gb)} GB"
        val tb = gb / 1024.0
        return "${decimalFormat.format(tb)} TB"
    }

    fun formatBytesShort(bytes: Long): String {
        if (bytes < 1024) return "${bytes}B"
        if (bytes < 1024 * 1024) return "${bytes / 1024}KB"
        if (bytes < 1024 * 1024 * 1024) return "${bytes / (1024 * 1024)}MB"
        return "${bytes / (1024 * 1024 * 1024)}GB"
    }

    fun formatDetailed(bytes: Long, byteLabel: String): String {
        if (bytes < 0) return "0 $byteLabel"
        val human = format(bytes)
        val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = '\'' }
        val exactFormat = DecimalFormat("#,##0", symbols)
        return "$human (${exactFormat.format(bytes)} $byteLabel)"
    }
}

