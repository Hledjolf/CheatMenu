// Cheat Menu: CommandDeck presets plus a permission gate. A game-agnostic core plus one adapter per game.
// core/ is plain Java compiled for Java 17 (shareable with Necesse); adapters/hytale/ is the Hytale plugin on Java 25.
plugins { base }

// The dev build is <releaseVersion>-dev; a release build passes -PreleaseBuild.
val releaseVersion = "1.0.0"
version = if (hasProperty("releaseBuild")) releaseVersion else "$releaseVersion-dev"

subprojects {
    version = rootProject.version
    apply(plugin = "java")
    repositories { mavenCentral() }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all,-serial", "-Werror"))
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.11.4")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher:1.11.4")
    }
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    }
}

// ---------------------------------------------------------------------------------------------------------
// checkAdapterBoundary: core/ is game-agnostic. No file under core/ may name a game API or CommandDeck, and
// core/ stays plain (no reflection), so it translates to the C#, C++ and Lua cores.
// ---------------------------------------------------------------------------------------------------------
val checkAdapterBoundary = tasks.register("checkAdapterBoundary") {
    group = "verification"
    description = "Fails if any file under core/ uses a game API, CommandDeck or reflection."
    val sources = fileTree("core") { include("**/*.java"); exclude("build/**") }
    inputs.files(sources)
    doLast {
        val banned = listOf("com.hypixel", "commanddeck.", "java.lang.reflect", "getDeclaredField", "getDeclaredMethod", "Class.forName")
        val offenders = sources.files.flatMap { f ->
            f.readLines().withIndex().filter { (_, l) -> banned.any { l.contains(it) } }
                .map { (i, l) -> " - ${f.relativeTo(projectDir)}:${i + 1}: ${l.trim()}" }
        }
        if (offenders.isNotEmpty()) throw GradleException("Game API or reflection in core:\n" + offenders.joinToString("\n"))
    }
}

// ---------------------------------------------------------------------------------------------------------
// checkJarContents: the plugin jar holds only Cheat Menu's own classes, the manifest, the license and the
// CommandDeck sidecar. No Hytale or CommandDeck classes, no sources, no test classes.
// ---------------------------------------------------------------------------------------------------------
val hytaleJar = project(":adapters:hytale").tasks.named<Jar>("jar")
val checkJarContents = tasks.register("checkJarContents") {
    group = "verification"
    description = "Fails if the plugin jar contains anything outside the allow list, or misses a required file."
    dependsOn(hytaleJar)
    val jarFile = hytaleJar.flatMap { it.archiveFile }
    doLast {
        val plans = if (version.toString().endsWith("-dev")) """|playtest/[a-z0-9][a-z0-9-]*\.md""" else ""
        val allowed = Regex("""META-INF/.*|manifest\.json|LICENSE|cheatmenu/(gate|hytale)/[A-Za-z0-9_$]+\.class|commanddeck/commands\.json""" + plans)
        val names = java.util.zip.ZipFile(jarFile.get().asFile).use { zf ->
            zf.entries().asSequence().filter { !it.isDirectory }.map { it.name }.toList()
        }
        val bad = names.filter { !allowed.matches(it) }
        if (bad.isNotEmpty()) throw GradleException("Unexpected files in the jar:\n" + bad.joinToString("\n") { " - $it" })
        val required = listOf("manifest.json", "commanddeck/commands.json", "cheatmenu/hytale/CheatMenuPlugin.class",
            "cheatmenu/gate/Gate.class")
        required.filter { it !in names }.forEach { throw GradleException("The jar is missing $it") }
    }
}

tasks.named("check") {
    dependsOn(subprojects.filter { !it.name.startsWith("fixture-") }.map { it.tasks.named("check") })
    dependsOn(checkJarContents)
    dependsOn(checkAdapterBoundary)
}

// ---------------------------------------------------------------------------------------------------------
// release: `gradlew release` runs the whole build again as version <releaseVersion> (no -dev), runs check on
// it, verifies the jar, and copies the upload files into release/<version>/: the jar (CurseForge) and a zip
// with the jar, README.md, LICENSE and CHANGELOG.md (Nexus). The normal build stays <releaseVersion>-dev.
// ---------------------------------------------------------------------------------------------------------
tasks.register<GradleBuild>("release") {
    group = "distribution"
    description = "Builds and checks version $releaseVersion and copies the jar and the zip to release/$releaseVersion/."
    dir = projectDir
    tasks = listOf("check", "releaseFiles")
    startParameter.projectProperties = gradle.startParameter.projectProperties + mapOf("releaseBuild" to "true")
}

val releaseDir = layout.projectDirectory.dir("release/$releaseVersion")

// checkReleaseJar: the jar says the release version, requires CommandDeck >=1.1.0, carries the license, has no
// playtest plans, and its classes are Java 17 (major 61) for the core and the game's Java 25 (69) for the adapter.
val checkReleaseJar = tasks.register("checkReleaseJar") {
    group = "verification"
    description = "Fails if the release jar has the wrong version, no LICENSE, development files or the wrong bytecode level."
    dependsOn(hytaleJar)
    val jarFile = hytaleJar.flatMap { it.archiveFile }
    val expected = version.toString()
    doLast {
        if (expected.endsWith("-dev")) throw GradleException("checkReleaseJar runs only in a release build (gradlew release)")
        java.util.zip.ZipFile(jarFile.get().asFile).use { zf ->
            val names = zf.entries().asSequence().filter { !it.isDirectory }.map { it.name }.toList()
            if ("LICENSE" !in names) throw GradleException("The release jar has no LICENSE")
            val extra = names.filter { it.startsWith("playtest/") }
            if (extra.isNotEmpty()) throw GradleException("The release jar has playtest plans:\n" + extra.joinToString("\n") { " - $it" })
            val manifest = zf.getInputStream(zf.getEntry("manifest.json")).readBytes().toString(Charsets.UTF_8)
            if (!Regex(""""Version"\s*:\s*"${Regex.escape(expected)}"""").containsMatchIn(manifest)) {
                throw GradleException("manifest.json does not say Version $expected")
            }
            // The presets' `permission` field needs CommandDeck 1.1.0; on 1.0 the sidecar wouldn't load at all.
            if (!Regex(""""Hledjolf:CommandDeck"\s*:\s*">=1\.1\.0"""").containsMatchIn(manifest)) {
                throw GradleException("manifest.json does not require CommandDeck >=1.1.0")
            }
            names.filter { it.endsWith(".class") }.forEach { n ->
                val b = zf.getInputStream(zf.getEntry(n)).use { it.readNBytes(8) }
                val major = ((b[6].toInt() and 0xff) shl 8) or (b[7].toInt() and 0xff)
                val want = if (n.startsWith("cheatmenu/gate/")) 61 else 69
                if (major != want) throw GradleException("$n is class file version $major, expected $want")
            }
        }
    }
}

val releaseZip = tasks.register<Zip>("releaseZip") {
    dependsOn(checkReleaseJar)
    archiveFileName.set("CheatMenu-$version.zip")
    destinationDirectory.set(releaseDir)
    from(hytaleJar)
    from(layout.projectDirectory) { include("README.md", "LICENSE", "CHANGELOG.md") }
}

tasks.register<Copy>("releaseFiles") {
    group = "distribution"
    description = "Copies the checked release jar to release/<version>/ and makes the zip. Run through gradlew release."
    dependsOn(checkReleaseJar, releaseZip)
    mustRunAfter("check")
    from(hytaleJar)
    into(releaseDir)
}
