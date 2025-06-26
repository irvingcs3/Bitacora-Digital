package com.example.bitacoradigital.util

import com.example.bitacoradigital.BuildConfig
import java.text.SimpleDateFormat
import java.util.*

object Constants {
    const val FILE_PROVIDER_AUTHORITY: String = "com.example.bitacoradigital.fileprovider"
    /**
     * Current application version as declared in Gradle.
     * This uses [BuildConfig.VERSION_NAME] so bumping `versionName`
     * automatically updates the check against the API without
     * requiring a manual constant edit.
     */
    val APP_VERSION: String = BuildConfig.VERSION_NAME
}

fun Long.toReadableDate(): String {
    val date = Date(this)
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return format.format(date)
}
