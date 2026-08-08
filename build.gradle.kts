plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

group = project.property("group") as String
version = project.property("version") as String

val paperApiVersion: String by project
val javaVersion: String by project

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")

    // Optional integrations. Nothing in slice 1 imports these, so they stay
    // commented out rather than being two more coordinates that can fail to
    // resolve for no benefit. Uncomment when the code that needs them lands.
    // compileOnly("me.clip:placeholderapi:2.11.6")
    // compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.8")

    // Shaded. Keep this list short.
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt()))
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(javaVersion.toInt())
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("Spadefall-${project.version}.jar")
        relocate("com.zaxxer.hikari", "com.supercraftmc.spadefall.lib.hikari")
    }

    build { dependsOn(shadowJar) }
}
