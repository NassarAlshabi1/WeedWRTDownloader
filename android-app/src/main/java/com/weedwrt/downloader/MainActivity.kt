package com.weedwrt.downloader

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import weedwrt.core.BuildInfo
import weedwrt.core.WeedWrtBuilder
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private lateinit var scrollView: ScrollView
    private lateinit var versionInput: AutoCompleteTextView
    private lateinit var targetInput: AutoCompleteTextView
    private lateinit var archInput: AutoCompleteTextView
    private lateinit var extrasInput: EditText
    private lateinit var buildButton: Button
    private lateinit var shareButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var logView: TextView

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var lastZip: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scrollView = findViewById(R.id.scrollView)
        versionInput = findViewById(R.id.versionInput)
        targetInput = findViewById(R.id.targetInput)
        archInput = findViewById(R.id.archInput)
        extrasInput = findViewById(R.id.extrasInput)
        buildButton = findViewById(R.id.buildButton)
        shareButton = findViewById(R.id.shareButton)
        progressBar = findViewById(R.id.progressBar)
        logView = findViewById(R.id.logView)

        versionInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, VERSION_PRESETS))
        targetInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, TARGET_PRESETS))
        archInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, ARCH_PRESETS))

        versionInput.setText("23.05.5", false)
        targetInput.setText("ramips/mt7621", false)
        archInput.setText("mipsel_24kc", false)

        logView.movementMethod = ScrollingMovementMethod()
        appendLog("WeedWRT Downloader v${BuildInfo.version}")
        appendLog("Generate an offline LuCI install package for OpenWrt, right on your phone.")
        appendLog("Defaults are set for OpenWrt 23.05.5 / mipsel_24kc.")
        appendLog("Tap 'Build offline package' to start.")

        buildButton.setOnClickListener { startBuild() }
        shareButton.setOnClickListener { shareZip() }
        shareButton.isEnabled = false
    }

    private fun startBuild() {
        val version = versionInput.text.toString().trim()
        val target = targetInput.text.toString().trim()
        val arch = archInput.text.toString().trim()

        if (version.isEmpty() || target.isEmpty() || arch.isEmpty()) {
            toast("Please fill in version, target and architecture")
            return
        }

        val extras = extrasInput.text.toString()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        buildButton.isEnabled = false
        shareButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
        appendLog("")
        appendLog("=== Build started: OpenWrt $version | $target | $arch ===")
        if (extras.isNotEmpty()) appendLog("Extra packages: ${extras.joinToString(", ")}")

        val workDir = File(cacheDir, "weedwrt-build")
        executor.execute {
            try {
                val zip = WeedWrtBuilder.buildOfflineBundle(version, target, arch, extras, workDir) { line ->
                    mainHandler.post { appendLog(line) }
                }
                val saved = persistResult(zip)
                lastZip = saved
                mainHandler.post {
                    progressBar.visibility = View.GONE
                    buildButton.isEnabled = true
                    shareButton.isEnabled = true
                    appendLog("")
                    appendLog("SUCCESS: ${saved.name}")
                    appendLog("Use 'Share' to send it to your PC, or copy it from the app folder.")
                    toast("Package ready")
                    scrollToBottom()
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    progressBar.visibility = View.GONE
                    buildButton.isEnabled = true
                    appendLog("")
                    appendLog("FAILED: ${t.message}")
                    toast("Build failed - see log")
                    scrollToBottom()
                }
            }
        }
    }

    /**
     * Copies the resulting ZIP into the app's external Download folder (always
     * possible, no permissions needed) and additionally into the public
     * Downloads collection via MediaStore on Android 10+.
     */
    private fun persistResult(zip: File): File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
        val dest = File(dir, zip.name)
        zip.inputStream().use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        appendLog("Saved to app folder: ${dest.absolutePath}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, zip.name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        dest.inputStream().use { input -> input.copyTo(output) }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                    appendLog("Also saved to your Downloads: ${zip.name}")
                }
            } catch (e: Exception) {
                appendLog("[!] Could not write to public Downloads (${e.message}) - the app folder copy is still available")
            }
        }
        return dest
    }

    private fun shareZip() {
        val file = lastZip ?: return
        try {
            val uri: Uri = FileProvider.getUriForFile(this, "com.weedwrt.downloader.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share WeedWRT package"))
        } catch (e: Exception) {
            toast("Share failed: ${e.message}")
        }
    }

    private fun appendLog(line: String) {
        logView.append(line + "\n")
        scrollToBottom()
    }

    private fun scrollToBottom() {
        scrollView.post { scrollView.smoothScrollTo(0, logView.bottom) }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    companion object {
        private val VERSION_PRESETS = listOf("23.05.5", "24.10.8", "25.12.6", "23.05.4", "22.03.7", "snapshot")
        private val TARGET_PRESETS = listOf(
            "ramips/mt7621", "ramips/mt76x8", "mediatek/filogic", "ath79/generic",
            "x86/64", "bcm27xx/bcm2711", "ipq40xx/generic", "lantiq/xrx200"
        )
        private val ARCH_PRESETS = listOf(
            "mipsel_24kc", "mips_24kc", "aarch64_generic", "arm_cortex-a7_neon-vfpv4",
            "arm_cortex-a53_neon-vfpv4", "x86_64", "arm_arm1176jzf-s_vfp"
        )
    }
}
