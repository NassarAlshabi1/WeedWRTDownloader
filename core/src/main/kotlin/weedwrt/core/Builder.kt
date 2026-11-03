package weedwrt.core

import java.io.File

/**
 * High level entry point: downloads and resolves everything needed for an
 * offline LuCI install on OpenWrt and packages it into a single ZIP.
 *
 * Works for any release (e.g. 23.05.5, 24.10.8) and for `snapshot`, because
 * package lists are fetched live from downloads.openwrt.org and the full
 * dependency closure is resolved automatically (no hardcoded package names).
 */
object WeedWrtBuilder {

    const val OPENWRT_DOWNLOADS = "https://downloads.openwrt.org"

    /** LuCI packages installed by default (plus everything they depend on). */
    val DEFAULT_SEEDS = listOf(
        "luci-base",
        "luci-mod-status",
        "luci-mod-system",
        "luci-mod-network",
        "luci-theme-bootstrap",
        "luci-app-firewall",
        "luci-app-opkg"
    )

    /**
     * @param version OpenWrt release such as "23.05.5", or "snapshot"
     * @param target target platform such as "ramips/mt7621" (used for the kmod feed)
     * @param arch package architecture such as "mipsel_24kc"
     * @param extraPackages additional packages to include (e.g. luci-app-aria2)
     * @param workDir scratch directory (created if missing); the result ZIP lands in workDir/output
     * @param log progress callback
     * @return the generated ZIP file
     */
    fun buildOfflineBundle(
        version: String,
        target: String,
        arch: String,
        extraPackages: List<String> = emptyList(),
        workDir: File,
        log: (String) -> Unit = {}
    ): File {
        val releaseType = if (version.equals("snapshot", ignoreCase = true)) ReleaseType.SNAPSHOT else ReleaseType.RELEASE
        val owrtDir = when (releaseType) {
            ReleaseType.SNAPSHOT -> "snapshots"
            ReleaseType.RELEASE -> "releases/$version"
        }

        val feeds = listOf(
            Feed("target", "$OPENWRT_DOWNLOADS/$owrtDir/targets/$target/packages"),
            Feed("base", "$OPENWRT_DOWNLOADS/$owrtDir/packages/$arch/base"),
            Feed("luci", "$OPENWRT_DOWNLOADS/$owrtDir/packages/$arch/luci"),
            Feed("packages", "$OPENWRT_DOWNLOADS/$owrtDir/packages/$arch/packages")
        )

        log("WeedWRT Downloader v${BuildInfo.version} (${BuildInfo.repository})")
        log("[*] OpenWrt $version | target: $target | arch: $arch")

        if (!workDir.exists() && !workDir.mkdirs()) error("Cannot create working directory: $workDir")
        val packagesDir = File(workDir, "luci-offline")
        if (!packagesDir.exists() && !packagesDir.mkdirs()) error("Cannot create packages directory: $packagesDir")

        // 1. Fetch the package indexes of every feed
        val indexes = feeds.mapNotNull { feed ->
            log("[*] Fetching package index [${feed.name}] ${feed.baseUrl}")
            try {
                val indexFile = File(workDir, "Packages-${feed.name}")
                Http.downloadToFile("${feed.baseUrl}/Packages", indexFile, log)
                val index = PackageIndexParser.parse(feed, indexFile)
                log("    -> ${index.size} packages indexed")
                index
            } catch (e: Exception) {
                log("[!] Feed [${feed.name}] unavailable (${e.message}) - continuing without it")
                null
            }
        }
        if (indexes.isEmpty()) {
            error("No package index could be downloaded. Check the version / target / architecture values.")
        }

        // 2. Resolve the full dependency closure
        val seeds = DEFAULT_SEEDS + extraPackages
        log("[*] Resolving dependencies for: ${seeds.joinToString(", ")}")
        val resolved = DependencyResolver.resolve(indexes, seeds, log)
        if (resolved.isEmpty()) error("Dependency resolution produced no packages.")
        log("[*] ${resolved.size} packages (incl. all dependencies) will be downloaded")

        // 3. Download every package
        for ((i, pkg) in resolved.withIndex()) {
            log("[${i + 1}/${resolved.size}] ${pkg.name} [${pkg.feed.name}] ${pkg.filename}")
            Http.downloadToFile(pkg.downloadUrl, File(packagesDir, pkg.filename), log)
        }

        // 4. Generate the install script and package everything
        val scriptFile = File(workDir, "weedwrt.sh")
        scriptFile.writeText(ScriptGenerator.generate())

        val safeVersion = version.replace('/', '_')
        val safeArch = arch.replace('/', '_')
        val outputDir = File(workDir, "output")
        val zipFile = File(outputDir, "weedwrt_${safeVersion}_${safeArch}.zip")
        ZipPackager.packageBundle(zipFile, scriptFile, packagesDir)

        // 5. Clean scratch files (keep output/)
        scriptFile.delete()
        File(workDir, "Packages-target").delete()
        File(workDir, "Packages-base").delete()
        File(workDir, "Packages-luci").delete()
        File(workDir, "Packages-packages").delete()
        packagesDir.deleteRecursively()

        log("[*] Bundle created: ${zipFile.absolutePath}")
        log("[*] It contains ${resolved.size} packages + weedwrt.sh install script")
        return zipFile
    }
}
