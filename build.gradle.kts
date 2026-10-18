import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.21"
    id("maven-publish")
    kotlin("jvm") version "2.4.21"
}

version = project.property("mod_version").toString()
group = project.property("maven_group").toString()

base {
    archivesName.set(project.property("archives_base_name").toString())
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    // Minecraft 26.1+ 不再混淆，无需任何 mappings（yarn/官方均不需要）
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    // Fabric API
    //implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    implementation("net.fabricmc:fabric-language-kotlin:1.14.2+kotlin.2.4.21")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))

    filesMatching("fabric.mod.json") {
        expand(mapOf(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version")
        ))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.toVersion(25)
    targetCompatibility = JavaVersion.toVersion(25)
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${archiveBaseName.get()}" }
    }
}
