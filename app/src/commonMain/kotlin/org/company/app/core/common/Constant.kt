package org.company.app.core.common

object Constant {
    const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
    const val DEFAULT_API_KEY = "AIzaSyATK5cfxRwEFXlp73Su6HrExL5_6Z0puYw"
    var customApiKey: String = ""

    val API_KEY: String
        get() = customApiKey.trim().takeIf { it.isNotEmpty() } ?: DEFAULT_API_KEY

    const val TIMEOUT = 20000L
    const val VIDEO_URL = "https://www.youtube.com/watch?v="
}