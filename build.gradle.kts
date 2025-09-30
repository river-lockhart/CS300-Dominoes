import org.gradle.jvm.application.tasks.CreateStartScripts

plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.0.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "edu.cs300.dominos"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

application {
    mainClass.set("app.Main")
}

repositories {
    mavenCentral()
}

javafx {
    version = "21.0.4"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.media")
}

dependencies {
    implementation("org.openjfx:javafx-controls:21.0.4")
    implementation("org.openjfx:javafx-graphics:21.0.4")
    implementation("org.openjfx:javafx-media:21.0.4")
}

tasks.shadowJar {
    archiveBaseName.set("CS300-Dominos")
    archiveClassifier.set("")
    mergeServiceFiles()
    manifest { attributes["Main-Class"] = application.mainClass.get() }
}

tasks.named<CreateStartScripts>("startShadowScripts") {
    dependsOn(tasks.named("jar"))
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.named("distZip") {
    dependsOn(tasks.shadowJar)
}
tasks.named("distTar") {
    dependsOn(tasks.shadowJar)
}
tasks.named("startScripts") {
    dependsOn(tasks.shadowJar)
}

jlink {
    launcher { name = "Dominoes" }
    options.set(listOf("--strip-debug", "--no-header-files", "--no-man-pages", "--compress=2"))
    jpackage {
        imageName = "Dominoes"
        installerName = "Dominoes"
    }
}
