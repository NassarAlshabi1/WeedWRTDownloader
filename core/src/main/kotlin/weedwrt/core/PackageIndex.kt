package weedwrt.core

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Parses an opkg `Packages` index file.
 * Handles: Package, Version, Depends, Provides, Filename fields.
 */
object PackageIndexParser {

    fun parse(feed: Feed, file: File): PackageIndex {
        val byName = HashMap<String, PackageInfo>()
        val byProvides = HashMap<String, PackageInfo>()

        var name = ""
        var version = ""
        var filename = ""
        var depends = emptyList<String>()
        var provides = emptyList<String>()
        var started = false

        fun commit() {
            if (started && name.isNotEmpty() && filename.isNotEmpty()) {
                val info = PackageInfo(name, version, filename, depends, provides, feed)
                if (!byName.containsKey(name)) byName[name] = info
                for (p in provides) {
                    if (p.isNotBlank() && !byProvides.containsKey(p)) byProvides[p] = info
                }
            }
        }

        BufferedReader(FileReader(file)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                when {
                    line.startsWith("Package: ") -> {
                        commit()
                        name = line.removePrefix("Package: ").trim()
                        version = ""
                        filename = ""
                        depends = emptyList()
                        provides = emptyList()
                        started = true
                    }
                    line.startsWith("Version: ") -> version = line.removePrefix("Version: ").trim()
                    line.startsWith("Depends: ") -> depends = line.removePrefix("Depends: ")
                        .split(",")
                        .map { it.trim().substringBefore(" (").trim() }
                        .filter { it.isNotEmpty() }
                    line.startsWith("Provides: ") -> provides = line.removePrefix("Provides: ")
                        .split(",")
                        .map { it.trim().substringBefore(" (").trim() }
                        .filter { it.isNotEmpty() }
                    line.startsWith("Filename: ") -> filename = line.removePrefix("Filename: ").trim()
                }
                line = reader.readLine()
            }
        }
        commit()

        return PackageIndex(byName, byProvides)
    }
}

/**
 * An index of packages from one feed. Lookup by real package name first,
 * then by virtual (Provided) name.
 */
class PackageIndex internal constructor(
    private val byName: Map<String, PackageInfo>,
    private val byProvides: Map<String, PackageInfo>
) {
    val size: Int get() = byName.size

    fun find(name: String): PackageInfo? = byName[name] ?: byProvides[name]
}
