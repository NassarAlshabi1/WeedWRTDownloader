plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":core"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveFileName.set("WeedWRT-fat.jar")
    manifest {
        attributes["Implementation-Title"] = "WeedWRT Downloader"
        attributes["Implementation-Version"] = "1.0.0"
        attributes["Main-Class"] = "MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    from(sourceSets.main.get().output)
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
}

tasks.named("build") {
    dependsOn(fatJar)
}
