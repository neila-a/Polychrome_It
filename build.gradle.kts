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

repositories {
    maven {
		url = uri("https://maven.shedaniel.me/")
	}
    maven { 
		url = uri("https://maven.terraformersmc.com/releases/")
	}

    // define the maven repo to search
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    // Minecraft 26.1+ 不再混淆，无需任何 mappings（yarn/官方均不需要）
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    // Fabric API（26.3 必需：其 fabric-registry-sync-v0 会延迟内置注册表的 freeze，
    // 否则 MC 26.3 在 Main.main 的 Bootstrap.bootStrap() 阶段就冻结注册表，
    // 'main' 入口点里 new Block() 会因无法创建 intrusive holder 而崩溃）
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    implementation("net.fabricmc:fabric-language-kotlin:1.14.2+kotlin.2.4.21")

    implementation("me.shedaniel.cloth:cloth-config-fabric:26.3.159") {
        exclude("net.fabricmc.fabric-api")
    }
    implementation("com.terraformersmc:modmenu:21.0.0")
    implementation("maven.modrinth:DistantHorizonsApi:7.2.0")
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
    from("LICENSE.md") {
        rename { "LICENSE_${archiveBaseName.get()}.md" }
    }
}
