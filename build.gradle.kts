/* ------------------------------ Plugins ------------------------------ */
plugins {
    id("java") // Import Java plugin.
    id("java-library") // Import Java Library plugin.
    id("com.diffplug.spotless") version "8.1.0" // Import Spotless plugin.
    id("com.gradleup.shadow") version "8.3.9" // Import Shadow plugin.
    id("checkstyle") // Import Checkstyle plugin.
    eclipse // Import Eclipse plugin.
}

/* --------------------------- JDK / Kotlin ---------------------------- */
java {
    sourceCompatibility = JavaVersion.VERSION_17 // Compile with JDK 17 compatibility.
    toolchain { // Select Java toolchain.
        languageVersion.set(JavaLanguageVersion.of(17)) // Use JDK 17.
        vendor.set(JvmVendorSpec.GRAAL_VM) // Use GraalVM CE.
    }
}

/* ----------------------------- Metadata ------------------------------ */
group = "de.myzelyam.vanishog" // Declare bundle identifier.

version = "1.1" // Declare plugin version (will be in .jar).

val apiVersion = "1.13" // Declare minecraft server target version.

/* ----------------------------- Resources ----------------------------- */
tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version, "apiVersion" to apiVersion)
    inputs.properties(props) // Indicates to rerun if version changes.
    filesMatching(listOf("plugin.yml", "config.yml", "messages.yml")) {
        expand(props) { escapeBackslash.set(true) } // Keep literal '\n' etc. intact in YAML comments/values.
    }
    from("LICENSE") { into("/") } // Bundle main GPLv2 license at jar root.
    from("libs/License.txt") { // Bundle ProtocolLib GPLv2 license.
        into("META-INF/licenses/ProtocolLib")
        rename { "LICENSE" }
    }
    from("libs/TAB-OG/LICENSE") { // Bundle TAB-OG license.
        into("META-INF/licenses/TAB-OG")
    }
}

/* ---------------------------- Repos ---------------------------------- */
repositories {
    mavenCentral() // Import the Maven Central Maven Repository.
    gradlePluginPortal() // Import the Gradle Plugin Portal Maven Repository.
    maven { url = uri("https://repo.purpurmc.org/snapshots") } // Import the PurpurMC Maven Repository.
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") } // PaperMC.
    maven { url = uri("https://repo.essentialsx.net/releases/") } // EssentialsX.
    maven { url = uri("https://jitpack.io") } // JitPack (VaultAPI, TrailGUI, OpenInv, Dynmap).
    maven { url = uri("https://repo.citizensnpcs.co/") } // Citizens.
    maven { url = uri("https://libraries.minecraft.net") } // Mojang Brigadier.
}

/* ---------------------- Java project deps ---------------------------- */
dependencies {
    // Purpur API (server target).
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")

    // Import TrueOG Network Utilities-OG Java API (from source).
    compileOnlyApi(project(":libs:Utilities-OG"))

    // EssentialsX API.
    compileOnly("net.essentialsx:EssentialsX:2.21.0") {
        exclude(group = "org.bstats", module = "bstats-bukkit") // Exclude bstats.
    }

    // LuckPerms API (group, prefix, suffix lookups).
    compileOnly("net.luckperms:api:5.5") // Import the LuckPerms API.

    // ProtocolLib (local jar in libs/ProtocolLib/).
    compileOnly(files("libs/ProtocolLib/ProtocolLib-5.0.jar"))

    // TAB-OG API (local jar in libs/TAB-OG/).
    compileOnly(files("libs/TAB-OG/tab-api-4.2.0.jar"))

    // Citizens API.
    compileOnly("net.citizensnpcs:citizensapi:2.0.28-SNAPSHOT")

    // TrailGUI (via JitPack).
    compileOnly("com.github.SinnDevelopment:TrailGUI:37659dda03")

    // Import MiniPlaceholders API.
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:2.2.3")

    // Mojang Brigadier (provided at runtime by Purpur).
    compileOnly("com.mojang:brigadier:1.0.18")
}

apply(from = "eclipse.gradle.kts") // Import eclipse classpath support script.

/* ---------------------- Reproducible jars ---------------------------- */
tasks.withType<AbstractArchiveTask>().configureEach { // Ensure reproducible .jars
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

/* ----------------------------- Shadow -------------------------------- */
tasks.shadowJar {
    exclude("io.github.miniplaceholders.*") // Exclude the MiniPlaceholders package from being shadowed.
    isEnableRelocation = true
    relocationPrefix = "${project.group}.shadow"
    mergeServiceFiles()
    archiveClassifier.set("") // Use empty string instead of null.
    archiveBaseName.set(rootProject.name) // Vanish-OG-${version}.jar
}

tasks.jar { archiveClassifier.set("part") } // Applies to root jarfile only.

tasks.build { dependsOn(tasks.spotlessApply, tasks.shadowJar) } // Build depends on spotless and shadow.

/* --------------------------- Javac opts ------------------------------- */
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters") // Enable reflection for java code.
    options.isFork = true // Run javac in its own process.
    options.compilerArgs.add("-Xlint:deprecation") // Trigger deprecation warning messages.
    options.encoding = "UTF-8" // Use UTF-8 file encoding.
}

/* ----------------------------- Auto Formatting ------------------------ */
spotless {
    java {
        eclipse().configFile("config/formatter/eclipse-java-formatter.xml") // Eclipse java formatting.
        leadingTabsToSpaces() // Convert leftover leading tabs to spaces.
        removeUnusedImports() // Remove imports that aren't being called.
    }
    kotlinGradle {
        ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) } // JetBrains Kotlin formatting.
        target("build.gradle.kts", "settings.gradle.kts") // Gradle files to format.
    }
}

checkstyle {
    toolVersion = "10.18.1" // Declare checkstyle version to use.
    configFile = file("config/checkstyle/checkstyle.xml") // Point checkstyle to config file.
    isIgnoreFailures = true // Don't fail the build if checkstyle does not pass.
    isShowViolations = true // Show the violations in any IDE with the checkstyle plugin.
}

tasks.named("compileJava") {
    dependsOn("spotlessApply") // Run spotless before compiling with the JDK.
}

tasks.named("spotlessCheck") {
    dependsOn("spotlessApply") // Run spotless before checking if spotless ran.
}

/* ------------------------------ Eclipse SHIM ------------------------- */

// This can't be put in eclipse.gradle.kts because Gradle is weird.
subprojects {
    apply(plugin = "java-library")
    apply(plugin = "eclipse")
    eclipse.project.name = "${project.name}-${rootProject.name}"
    tasks.withType<Jar>().configureEach { archiveBaseName.set("${project.name}-${rootProject.name}") }
}
