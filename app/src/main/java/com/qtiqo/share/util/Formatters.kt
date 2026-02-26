package com.qtiqo.share.util

import android.text.format.DateFormat
import java.util.Date
import kotlin.math.ln
import kotlin.math.pow

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return String.format("%.1f %s", value, units[digitGroups])
}

fun formatDateTime(timestamp: Long): String {
    return DateFormat.format("yyyy-MM-dd HH:mm", Date(timestamp)).toString()
}
