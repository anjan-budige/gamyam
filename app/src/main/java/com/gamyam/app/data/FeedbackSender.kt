package com.gamyam.app.data

import com.gamyam.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object FeedbackSender {
    suspend fun send(source: String, message: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = BuildConfig.TELEGRAM_BOT_TOKEN
            val chatId = BuildConfig.TELEGRAM_CHAT_ID
            if (token.isBlank() || chatId.isBlank()) error("Feedback is not configured")
            val text = buildString {
                appendLine("Gamyam feedback")
                appendLine("Source: ${source.ifBlank { "App" }}")
                appendLine()
                append(message.trim())
            }
            val body = listOf(
                "chat_id" to chatId,
                "text" to text
            ).joinToString("&") { (key, value) ->
                "${key}=${URLEncoder.encode(value, "UTF-8")}"
            }
            val connection = (URL("https://api.telegram.org/bot$token/sendMessage").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            OutputStreamWriter(connection.outputStream).use { it.write(body) }
            val code = connection.responseCode
            connection.inputStream.close()
            connection.disconnect()
            if (code !in 200..299) error("Telegram returned $code")
        }
    }
}
