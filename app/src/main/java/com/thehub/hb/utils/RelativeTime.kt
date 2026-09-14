package com.thehub.hb.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RelativeTime {
    fun format(timestamp: Timestamp?): String {
        if (timestamp == null) return "Récemment"
        val now = System.currentTimeMillis()
        val time = timestamp.toDate().time
        val diff = now - time

        if (diff < 0) return "À l'instant"

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "À l'instant"
            minutes < 60 -> "il y a ${minutes}min"
            hours < 24 -> "il y a ${hours}h"
            days < 7 -> "il y a ${days}j"
            else -> {
                val format = SimpleDateFormat("d MMM", Locale.FRENCH)
                format.format(Date(time))
            }
        }
    }

    fun formatTimeOnly(timestamp: Timestamp?): String {
        if (timestamp == null) return ""
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(timestamp.toDate())
    }

    fun formatConversationDate(timestamp: Timestamp?): String {
        if (timestamp == null) return ""
        val now = System.currentTimeMillis()
        val time = timestamp.toDate().time
        val diff = now - time

        val calNow = java.util.Calendar.getInstance()
        val calDate = java.util.Calendar.getInstance().apply { timeInMillis = time }

        val isSameDay = calNow.get(java.util.Calendar.YEAR) == calDate.get(java.util.Calendar.YEAR) &&
                calNow.get(java.util.Calendar.DAY_OF_YEAR) == calDate.get(java.util.Calendar.DAY_OF_YEAR)

        if (isSameDay) {
            return formatTimeOnly(timestamp)
        }

        val yesterdayCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterdayCal.get(java.util.Calendar.YEAR) == calDate.get(java.util.Calendar.YEAR) &&
                yesterdayCal.get(java.util.Calendar.DAY_OF_YEAR) == calDate.get(java.util.Calendar.DAY_OF_YEAR)

        if (isYesterday) {
            return "Hier"
        }

        return if (diff < 7 * 24 * 3600 * 1000L) {
            val format = SimpleDateFormat("EEE", Locale.FRENCH)
            format.format(Date(time)).replaceFirstChar { it.uppercase() }
        } else {
            val format = SimpleDateFormat("d MMM", Locale.FRENCH)
            format.format(Date(time))
        }
    }
}
