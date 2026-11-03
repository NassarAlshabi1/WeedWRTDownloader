package weedwrt.core

enum class ReleaseType {
    SNAPSHOT,
    RELEASE
}

/**
 * A single OpenWrt package feed (directory containing a `Packages` index and .ipk files).
 */
data class Feed(
    val name: String,
    val baseUrl: String
)

/**
 * A package entry parsed from a feed `Packages` index.
 */
data class PackageInfo(
    val name: String,
    val version: String,
    val filename: String,
    val depends: List<String>,
    val provides: List<String>,
    val feed: Feed
) {
    val downloadUrl: String get() = "${feed.baseUrl}/$filename"
}

object BuildInfo {
    const val version = "1.0.0"
    const val repository = "github.com/NassarAlshabi1/WeedWRTDownloader"
}
