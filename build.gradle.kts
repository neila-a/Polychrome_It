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

    // DH 为可选（suggests）依赖：编译期仅用 API，运行期由安装的 DH mod 提供类
    compileOnly("maven.modrinth:DistantHorizonsApi:7.2.0")
    // 开发/测试运行期提供 DH（仅 dev 用，不随发布产物强制要求）
    runtimeOnly("maven.modrinth:distanthorizons:3.3.4-26.3")

    // Iris 为可选依赖：编译期用其 API 探测“是否启用了光影”，运行期由用户安装的 Iris 提供类。
    // 未安装 Iris 时（见 IrisShaderDetector 的 try/catch）回落到真实染色，不会崩溃。
    implementation("maven.modrinth:iris:1.11.7+26.3-fabric")

    implementation("commons-io:commons-io:2.22.0")
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

    val name = "Polychrome It!"
    val description = "Make your Minecraft more colorful.\nSupports full RGB range."
    val nameAndDescriptionMap = mapOf(
        "name" to name,
        "description" to description
    )

    filesMatching("fabric.mod.json") {
        expand(nameAndDescriptionMap + mapOf(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version"),
        ))
    }
    filesMatching("/assets/polychrome_it/lang/en_us.json") {
        expand(nameAndDescriptionMap)
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

fabricApi {
	configureDataGeneration() {
		client = true
	}
}
