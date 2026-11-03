// Optional mirror support: set USE_ALIYUN_MIRROR=1 to route dependency downloads
// through the Aliyun mirrors (useful when access to Maven Central / Google Maven
// is slow or rate-limited). CI servers do not need this.

pluginManagement {
    val useAliyunMirror = System.getenv("USE_ALIYUN_MIRROR") == "1"
    repositories {
        if (useAliyunMirror) {
            maven("https://maven.aliyun.com/repository/gradle-plugin")
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    val useAliyunMirror = System.getenv("USE_ALIYUN_MIRROR") == "1"
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (useAliyunMirror) {
            maven("https://maven.aliyun.com/repository/public")
            maven("https://maven.aliyun.com/repository/google")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "WeedWRT"

include(":core")
include(":cli")
include(":android-app")
