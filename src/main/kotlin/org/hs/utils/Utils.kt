package org.hs.utils

import java.text.SimpleDateFormat
import java.util.Date

object Utils {

  fun convertEpochMillisToString(epochMillis: Long, format: String = "yyyy-MM-dd HH:mm:ss.SSSSSS"): String {
    val date = Date(epochMillis)
    val dateFormat = SimpleDateFormat(format)
    return dateFormat.format(date)
  }
}
