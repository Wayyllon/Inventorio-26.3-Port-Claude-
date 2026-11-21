plugins {
    id("net.fabricmc.fabric-loom")
    kotlin("jvm")
    id("maven-publish")
}

fun prop(name: String) = providers.gradleProperty(name).get()

val modVersion = prop("mod_version") + if (System.getenv("BUILD_RELEASE") == "true") "" else "-SNAPSHOT"
version = "v$modVersion"
group = prop("maven_group")

base {
    archivesName = "${prop("archives_base_name")}-mc${prop("minecraft_version")}-fabric"
}

repositories {
    // Maven Central mirror (repo.maven.apache.org rate-limits heavily)
    maven("https://maven-central.storage-download.googleapis.com/maven2/")
    // Mod Menu
    maven("https://maven.terraformersmc.com/releases/")
    // Cloth Config
    maven("https://maven.shedaniel.me/")
    // Conditional Mixin
    maven("https://maven.fallenbreath.me/releases")
    // Other mods from Modrinth
    maven("https://api.modrinth.com/maven")
}

loom {
    runConfigs.all {
        vmArg("-Dmixin.debug.export=true")
    }
    file("src/main/resources/${prop("mod_id")}.classtweaker").let {
        if (it.exists()) accessWidenerPath = it
    }
}

dependencies {
    // Minecraft 26.x is unobfuscated: no mappings needed
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${prop("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${prop("fabric_kotlin_version")}")

    include(implementation("me.fallenbreath:conditional-mixin-fabric:${prop("conditional_mixin_version")}")!!)

    implementation("com.terraformersmc:modmenu:${prop("modmenu_version")}")
    implementation("me.shedaniel.cloth:cloth-config-fabric:${prop("cloth_version")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    // TODO(26.3): optional integrations disabled for now (see disabled/README.md):
    // Trinkets, Clumps, Early Loading Screen
}

tasks.processResources {
    val replaceProperties = mapOf(
        "mod_id" to prop("mod_id"),
        "mod_name" to prop("mod_name"),
        "version" to modVersion,
        "description" to prop("mod_description"),
        "authors" to prop("mod_authors").split(',').joinToString("\",\""),
        "license" to prop("license"),
        "homepage_url" to prop("homepage_url"),
        "sources_url" to prop("sources_url"),
        "issues_url" to prop("issues_url"),
        "minecraft_version" to prop("minecraft_version"),
        "loader_version" to prop("loader_version"),
        "fabric_kotlin_version" to prop("fabric_kotlin_version"),
    )
    inputs.properties(replaceProperties)
    filesMatching("fabric.mod.json") {
        expand(replaceProperties)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    jvmToolchain(25)
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${prop("archives_base_name")}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}
