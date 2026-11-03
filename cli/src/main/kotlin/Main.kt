import weedwrt.core.BuildInfo
import weedwrt.core.WeedWrtBuilder
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size < 3) {
        showHelpMessage()
    }

    val version = args[0]
    val target = args[1]
    val arch = args[2]
    val extraPackages = if (args.size > 3) {
        args[3].split(',').map { it.trim() }.filter { it.isNotEmpty() }
    } else {
        emptyList()
    }

    println("WeedWRT Downloader v${BuildInfo.version} (${BuildInfo.repository})")
    println("Original tool by NickP0is0n, maintained by NassarAlshabi1\n")

    val workDir = File("tmp/build")
    try {
        val zip = WeedWrtBuilder.buildOfflineBundle(version, target, arch, extraPackages, workDir) { println(it) }

        val outputDir = File("output")
        outputDir.mkdirs()
        val finalFile = File(outputDir, zip.name)
        zip.copyTo(finalFile, overwrite = true)

        println()
        println("WeedWRT package created: output/${finalFile.name}")
        println("Copy the ZIP to your router, unzip it, then run: sh weedwrt.sh")
    } catch (e: Exception) {
        System.err.println("Build failed: ${e.message}")
        exitProcess(1)
    } finally {
        workDir.deleteRecursively()
    }
}

private fun showHelpMessage() {
    println("WeedWRT Downloader v${BuildInfo.version}")
    println("Generates an offline LuCI install bundle for OpenWrt (packages + install script).")
    println()
    println("Usage:")
    println("  weedwrt <version> <target> <arch> [extra packages]")
    println()
    println("Arguments:")
    println("  version   OpenWrt release (e.g. 23.05.5, 24.10.8, 25.12.6) or 'snapshot'")
    println("  target    target platform, e.g. ramips/mt7621, ramips/mt76x8, ath79/generic,")
    println("            mediatek/filogic, x86/64, bcm27xx/bcm2711")
    println("  arch      package architecture, e.g. mipsel_24kc, mips_24kc, aarch64_generic,")
    println("            arm_cortex-a7_neon-vfpv4, x86_64")
    println("  extra     optional comma separated extra packages (default LuCI set is always included)")
    println()
    println("Examples:")
    println("  java -jar WeedWRT-fat.jar 23.05.5 ramips/mt7621 mipsel_24kc")
    println("  java -jar WeedWRT-fat.jar 23.05.5 ramips/mt7621 mipsel_24kc luci-app-aria2,luci-app-adblock")
    println("  java -jar WeedWRT-fat.jar snapshot x86/64 x86_64")
    exitProcess(0)
}
