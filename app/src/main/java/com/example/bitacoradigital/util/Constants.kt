package com.example.bitacoradigital.util

import com.example.bitacoradigital.BuildConfig
import java.text.SimpleDateFormat
import java.util.*

object Constants {
    const val FILE_PROVIDER_AUTHORITY: String = "com.example.bitacoradigital.fileprovider"

    /**
     * Current application version as declared in Gradle.
     * Uses [BuildConfig.VERSION_NAME] so updating `versionName`
     * automatically adjusts the API comparison without manual edits.
     */
    val APP_VERSION: String = BuildConfig.VERSION_NAME
}

fun Long.toReadableDate(): String {
    val date = Date(this)
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return format.format(date)
}
