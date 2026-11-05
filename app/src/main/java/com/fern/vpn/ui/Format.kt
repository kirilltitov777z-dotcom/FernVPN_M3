package com.fern.vpn.ui

import java.util.Locale

fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kilobytes = bytes / 1024.0
    if (kilobytes < 1024.0) return String.format(Locale.US, "%.1f KB", kilobytes)
    val megabytes = kilobytes / 1024.0
    if (megabytes < 1024.0) return String.format(Locale.US, "%.1f MB", megabytes)
    return String.format(Locale.US, "%.2f GB", megabytes / 1024.0)
}

fun formatSpeed(bytesPerSecond: Float): String =
    formatBytes(bytesPerSecond.toLong().coerceAtLeast(0L)) + "/s"

fun formatDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val secs = safe % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, secs)
    }
}
