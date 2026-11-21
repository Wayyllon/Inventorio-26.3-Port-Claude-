pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        // Maven Central mirror (repo.maven.apache.org rate-limits heavily)
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("net.fabricmc.fabric-loom") version providers.gradleProperty("loom_version").get()
        kotlin("jvm") version providers.gradleProperty("kotlin_version").get()
    }
}

rootProject.name = "inventorio"
