package weedwrt.core

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packages weedwrt.sh + the luci-offline folder into a ZIP bundle.
 */
object ZipPackager {

    fun packageBundle(target: File, scriptFile: File, packagesDir: File) {
        require(scriptFile.isFile) { "Script file missing: $scriptFile" }
        val ipkFiles = packagesDir.listFiles()?.filter { it.isFile && it.name.endsWith(".ipk") }?.sortedBy { it.name }
            ?: error("Packages directory missing: $packagesDir")
        require(ipkFiles.isNotEmpty()) { "No .ipk files found in $packagesDir" }

        target.parentFile?.mkdirs()
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            addFile(zip, scriptFile, "weedwrt.sh")
            for (f in ipkFiles) {
                addFile(zip, f, "luci-offline/${f.name}")
            }
        }
    }

    private fun addFile(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }
}
