package weedwrt.core

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small HTTP helper with redirects, timeouts and safe (atomic) file downloads.
 */
object Http {

    private const val TIMEOUT_MS = 60_000

    fun open(url: String): java.io.InputStream {
        var currentUrl = url
        var redirects = 0
        while (true) {
            val conn = URL(currentUrl).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("User-Agent", "WeedWRT-Downloader/${BuildInfo.version}")
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location == null || redirects++ > 5) throw IOException("Too many redirects for $url")
                currentUrl = if (location.startsWith("http")) location else URL(URL(currentUrl), location).toString()
                continue
            }
            if (code !in 200..299) {
                conn.disconnect()
                throw IOException("HTTP $code for $currentUrl")
            }
            return conn.inputStream
        }
    }

    fun fetchText(url: String): String = open(url).use { it.readBytes().toString(Charsets.UTF_8) }

    /**
     * Downloads [url] to [target] atomically (via .part file, then rename).
     * A failed download never leaves a partial file behind.
     */
    fun downloadToFile(url: String, target: File, log: (String) -> Unit = {}) {
        var attempt = 0
        while (true) {
            attempt++
            val tmp = File(target.absolutePath + ".part")
            try {
                open(url).use { input ->
                    tmp.outputStream().use { output ->
                        input.copyTo(output, 64 * 1024)
                    }
                }
                if (target.exists()) target.delete()
                if (!tmp.renameTo(target)) {
                    tmp.inputStream().use { input ->
                        target.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
                    }
                    tmp.delete()
                }
                return
            } catch (e: Exception) {
                tmp.delete()
                // retrying
                if (attempt >= 3) throw IOException("Failed to download $url after $attempt attempts: ${e.message}", e)
                log("[!] Download failed (${e.message}), retry $attempt/3: $url")
                Thread.sleep(1500L * attempt)
            }
        }
    }
}
