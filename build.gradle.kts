plugins {
    id("net.labymod.labygradle")
    id("net.labymod.labygradle.addon")
}

val versions = providers.gradleProperty("net.labymod.minecraft-versions").get().split(";")

group = "net.dyhntastic.radio"
version = providers.environmentVariable("VERSION").getOrElse("1.0.7")

labyMod {
    defaultPackageName = "net.dyhntastic.radio"

    minecraft {
        registerVersion(versions.toTypedArray()) {
            runs {
                getByName("client") {
                    // When the property is set to true, you can log in with a Minecraft account
                    // devLogin = true
                }
            }
        }
    }

    addonInfo {
        namespace = "dyhnunity-radio"
        displayName = "Dyhnunity Radio Player"
        author = "DyhnenTv"
        description = "Provider-independent radio player by DyhnenTv with DyhnunityFM, laut.fm, Radio Browser, I LOVE MUSIC and RadioReg."
        minecraftVersion = "*"
        version = rootProject.version.toString()
    }
}

subprojects {
    plugins.apply("net.labymod.labygradle")
    plugins.apply("net.labymod.labygradle.addon")

    group = rootProject.group
    version = rootProject.version

    extensions.findByType(JavaPluginExtension::class.java)?.apply {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

allprojects {
    tasks.withType<Jar>().configureEach {
        exclude("assets/example/**")
    }
}

val reviewSourceFiles = fileTree(projectDir) {
    include("api/src/**/*.java")
    include("core/src/**/*.java")
}

val reviewAudit by tasks.registering {
    group = "verification"
    description = "Checks source-level LabyMod Addon Store review blockers."
    inputs.files(reviewSourceFiles)
    inputs.file(".github/workflows/build.yml")
    inputs.file("gradle/wrapper/gradle-wrapper.properties")

    doLast {
        val forbidden = linkedMapOf(
            ".stream(" to "java.util.stream.Stream usage",
            "java.util.stream" to "java.util.stream import",
            "java.lang.reflect" to "Java reflection",
            "Class.forName(" to "runtime reflection",
            "setAccessible(" to "accessibility override",
            "System.out" to "System.out logging",
            "System.err" to "System.err logging",
            "printStackTrace(" to "direct stack-trace output",
            "net.labymod.core" to "LabyMod core package access"
        )
        val violations = mutableListOf<String>()
        for (sourceFile in reviewSourceFiles.files.sortedBy { it.path }) {
            val text = sourceFile.readText()
            for ((needle, label) in forbidden) {
                if (needle in text) {
                    violations += "${sourceFile.relativeTo(projectDir)}: $label"
                }
            }
            if (Regex("§[0-9A-FK-ORa-fk-or]").containsMatchIn(text)) {
                violations += "${sourceFile.relativeTo(projectDir)}: legacy color code"
            }
        }

        val wrapper = file("gradle/wrapper/gradle-wrapper.properties").readText()
        if (!wrapper.lineSequence().any { it.startsWith("distributionSha256Sum=") }) {
            violations += "gradle-wrapper.properties: missing distributionSha256Sum"
        }
        if (!file(".github/workflows/build.yml").isFile) {
            violations += ".github/workflows/build.yml: missing official build workflow"
        }
        check(violations.isEmpty()) {
            "Addon Store review audit failed:\n" + violations.joinToString("\n")
        }
    }
}

tasks.matching { it.name == "build" }.configureEach {
    dependsOn(reviewAudit)
}
