package com.example.bitacoradigital.util

import java.text.SimpleDateFormat
import java.util.*

object Constants {
    const val FILE_PROVIDER_AUTHORITY: String = "com.example.bitacoradigital.fileprovider"


    val APP_VERSION: String = "1.0.1"
}

fun Long.toReadableDate(): String {
    val date = Date(this)
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return format.format(date)
}
