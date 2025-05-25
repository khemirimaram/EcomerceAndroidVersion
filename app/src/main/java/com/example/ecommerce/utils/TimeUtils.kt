package com.example.ecommerce.utils

import android.text.format.DateUtils
import com.google.firebase.Timestamp
import java.util.*

object TimeUtils {
    fun getRelativeTimeSpan(timestamp: Long): String {
        return DateUtils.getRelativeTimeSpanString(
            timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    fun getRelativeTimeSpan(timestamp: Timestamp?): String {
        return if (timestamp != null) {
            getRelativeTimeSpan(timestamp.seconds * 1000)
        } else {
            "Date inconnue"
        }
    }

    fun getRelativeTimeSpan(date: Date?): String {
        return if (date != null) {
            getRelativeTimeSpan(date.time)
        } else {
            "Date inconnue"
        }
    }
} 