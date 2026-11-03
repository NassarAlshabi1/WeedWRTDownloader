package weedwrt.core

/**
 * Resolves the full transitive dependency closure of a set of seed packages
 * across all available feed indexes.
 *
 * `libc` is skipped because it is always present on the router and
 * reinstalling it offline is risky.
 */
object DependencyResolver {

    private val SKIP_PACKAGES = setOf("libc")

    fun resolve(
        indexes: List<PackageIndex>,
        seeds: List<String>,
        log: (String) -> Unit
    ): List<PackageInfo> {
        fun find(name: String): PackageInfo? {
            for (index in indexes) {
                val found = index.find(name)
                if (found != null) return found
            }
            return null
        }

        val resolved = LinkedHashMap<String, PackageInfo>() // key = real package name
        val missing = LinkedHashSet<String>()
        val queue = ArrayDeque<String>()

        for (seed in seeds.map { it.trim() }.filter { it.isNotEmpty() }) {
            queue.add(seed)
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in SKIP_PACKAGES) continue

            val info = find(current)
            if (info == null) {
                if (missing.add(current)) {
                    log("[!] Package not found in any feed: $current (skipped)")
                }
                continue
            }
            if (resolved.containsKey(info.name)) continue

            resolved[info.name] = info
            for (dep in info.depends) {
                if (dep !in resolved.keys && dep !in SKIP_PACKAGES) {
                    queue.add(dep)
                }
            }
        }

        return resolved.values.toList()
    }
}
