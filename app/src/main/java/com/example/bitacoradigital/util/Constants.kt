package com.example.bitacoradigital.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Constants {
    const val FILE_PROVIDER_AUTHORITY: String = "com.example.bitacoradigital.fileprovider"


    val APP_VERSION: String = "1.0.1"
    const val DRON_GUARD_TOKEN: String = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJmcmVzaCI6ZmFsc2UsImlhdCI6MTc1MjI1MzYyMiwianRpIjoiNWJhOGY1MTQtYWM4OC00ZDVjLTkwMjQtOWY5OGZkM2U2NDA3IiwidHlwZSI6ImFjY2VzcyIsInN1YiI6Ik9QRVJBRE9SIiwibmJmIjoxNzUyMjUzNjIyLCJjc3JmIjoiNmRiM2E1NWYtY2QyOC00NTNkLWFkZmItY2I5ZTc0ZmUzZTM5IiwiZXhwIjoxNzgzNzg5NjIyfQ.NPAuRWCPVOdLNWx5C9K-dEzwdLAsP6WC5V8P4zdYNC0"
    const val DRON_GUARD_REGISTRO: String = "https://ubicua.earthnergy.com/boton/v1.0/registro"
    const val DRON_GUARD_SEND: String = "https://ubicua.earthnergy.com/boton/v1.0/send"
}

fun Long.toReadableDate(): String {
    val date = Date(this)
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return format.format(date)
}
@RequiresApi(Build.VERSION_CODES.O)
fun String.toReadableDateTime(): String {
    return try {
        val odt = OffsetDateTime.parse(this)
        val local = odt.atZoneSameInstant(ZoneId.systemDefault())
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.getDefault())
        local.format(fmt)
    } catch (e: Exception) {
        this
    }
}
