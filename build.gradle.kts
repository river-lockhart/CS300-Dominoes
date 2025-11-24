import org.gradle.jvm.application.tasks.CreateStartScripts
import org.gradle.internal.os.OperatingSystem

plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "edu.cs300.dominos"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

application {
    // tiny launcher that calls your real JavaFX Application subclass
    mainClass.set("app.Launcher")
}

// ---- JavaFX platform selection ----

val javafxVersion = "21.0.4"

// default platform based on the OS running the build
val defaultJavafxPlatform: String = when {
    OperatingSystem.current().isWindows -> "win"
    OperatingSystem.current().isMacOsX -> "mac"
    else -> "linux"
}

// can override with: -PjavafxPlatform=win or linux or mac
val javafxPlatform: String =
    (project.findProperty("javafxPlatform") as String?) ?: defaultJavafxPlatform

repositories {
    mavenCentral()
}

dependencies {
    // core javafx base (beans, collections, util, etc.)
    implementation("org.openjfx:javafx-base:$javafxVersion:$javafxPlatform")

    // ui + graphics + media
    implementation("org.openjfx:javafx-controls:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-media:$javafxVersion:$javafxPlatform")
}

// ---- Shadow jar config ----

tasks.shadowJar {
    // common base name
    archiveBaseName.set("CS300-Dominos")
    archiveClassifier.set("") // no "-all"

    // platform-specific filename suffix:
    // default from OS, but can be overridden with -PplatformSuffix=Something
    val defaultSuffix = when {
        OperatingSystem.current().isWindows -> "Windows"
        OperatingSystem.current().isMacOsX -> "Mac"
        else -> "Linux"
    }
    val platformSuffix = (project.findProperty("platformSuffix") as String?) ?: defaultSuffix

    archiveFileName.set("CS300-Dominos-$platformSuffix.jar")

    mergeServiceFiles()
    manifest {
        // make sure the fat jar runs the launcher, not the JavaFX Application subclass
        attributes["Main-Class"] = "app.Launcher"
    }
}

// make sure all the usual distributions depend on the fat jar

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.named("distZip") {
    dependsOn(tasks.shadowJar)
}

tasks.named("distTar") {
    dependsOn(tasks.shadowJar)
}

tasks.named<CreateStartScripts>("startScripts") {
    dependsOn(tasks.shadowJar)
}
