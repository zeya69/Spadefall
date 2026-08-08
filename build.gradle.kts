plugins {
    java
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

    // Bundled into the jar. Keep this list short.
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

    // Fat jar built with Gradle's own jar task rather than the Shadow plugin.
    //
    // Shadow rewrites bytecode in order to relocate packages, which means it
    // has to parse every class with ASM - and its bundled ASM cannot read Java
    // 25 class files (major version 69). This approach only zips files, so it
    // is immune to whatever bytecode version the compiler emits.
    //
    // The trade-off is no package relocation. That is acceptable here because
    // Bukkit gives each plugin its own classloader, so a bundled HikariCP
    // cannot collide with another plugin's copy.
    jar {
        archiveBaseName.set("Spadefall")
        archiveClassifier.set("")
        archiveFileName.set("Spadefall-${project.version}.jar")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from({
            configurations.runtimeClasspath.get()
                .filter { it.name.endsWith(".jar") }
                .map { zipTree(it) }
        })

        // Signature files from signed dependencies invalidate the merged jar,
        // and module descriptors are meaningless once everything is flattened.
        exclude(
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "META-INF/MANIFEST.MF",
            "META-INF/versions/*/module-info.class",
            "module-info.class"
        )
    }
}
