// The Hytale plugin. The server jar is the API: compile-only, never bundled. CommandDeck is a runtime
// dependency (manifest.json); its jar is on the test classpath only, to check the presets with its own reader.
val hytaleServerJar = (findProperty("hytaleServerJar") as String?)
    ?: "${System.getenv("APPDATA")}/Hytale/install/release/package/game/latest/Server/HytaleServer.jar"
// CommandDeck 1.1.0's published jar; its dev build until 1.1.0 is released.
val commandDeckJar = (findProperty("commandDeckJar") as String?)
    ?: listOf("CommandDeck-1.1.0.jar", "CommandDeck-1.1.0-dev.jar").map { "D:/Tools/CommandDeck/build/mods/$it" }
        .firstOrNull { file(it).exists() } ?: "D:/Tools/CommandDeck/build/mods/CommandDeck-1.1.0.jar"
// GameDataTest reads these in place: CommandDeck's probe dump of the command registry, and the game's assets.
val probeRegistry = (findProperty("probeRegistry") as String?)
    ?: "D:/Tools/CommandDeck/probe/run/mods/CommandDeckProbe_Probe/registry.json"
val hytaleAssets = (findProperty("hytaleAssets") as String?)
    ?: "${System.getenv("APPDATA")}/Hytale/install/release/package/game/latest/Assets.zip"
// The ecosystem's item catalog (UME-D012): ids, quality and stack size, generated from a running server.
val itemCatalog = (findProperty("itemCatalog") as String?)
    ?: "D:/Tools/UtilityModEcosystem/reference/hytale/generated/items.json"

tasks.withType<Test> {
    systemProperty("cheatmenu.registry", probeRegistry)
    systemProperty("cheatmenu.assets", hytaleAssets)
    systemProperty("cheatmenu.items", itemCatalog)
}

dependencies {
    "implementation"(project(":core"))
    "compileOnly"(files(hytaleServerJar))
    "testImplementation"(files(commandDeckJar))
}

// A dev build accepts any CommandDeck: Hytale doesn't count 1.1.0-dev as >=1.1.0, and playtests may need
// CommandDeck's dev jar. A release jar keeps the real range: 1.1.0 is the first with the preset `permission`.
val commandDeckRange = if (rootProject.version.toString().endsWith("-dev")) "*" else ">=1.1.0"

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", rootProject.version.toString())
    inputs.property("commandDeckRange", commandDeckRange)
    filesMatching("manifest.json") {
        expand("version" to rootProject.version.toString(), "commandDeckRange" to commandDeckRange)
    }
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("CheatMenu")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("mods"))
    from(project(":core").extensions.getByType<SourceSetContainer>()["main"].output)
    if (rootProject.file("LICENSE").exists()) from(rootProject.file("LICENSE"))
}

// Playtest plans (tests/playtest/*.md) go into the dev jar under playtest/, where PlaytestGamified finds them.
// Only in a -dev version; a release jar never carries them (checkReleaseJar).
val playtestPlans = rootProject.file("tests/playtest")
if (rootProject.version.toString().endsWith("-dev") && playtestPlans.isDirectory) {
    tasks.named<Jar>("jar") {
        inputs.dir(playtestPlans)
        from(playtestPlans) {
            include("*.md")
            into("playtest")
        }
    }
}
