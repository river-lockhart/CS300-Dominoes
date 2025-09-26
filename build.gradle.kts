plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.0.1"
}

group = "edu.cs300.dominos"
version = "1.0.0"

java {
    // use 21 if you have JDK 21; change to 17 if you're on JDK 17
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

application {
    // match your actual main class package+name (you said you use app.main)
    mainClass.set("app.Main")
}

repositories {
    mavenCentral()
}

javafx {
    version = "21.0.4"                  // use 17.x if on JDK 17
    modules = listOf("javafx.controls", "javafx.graphics")
}

jlink {
    launcher {
        name = "Dominoes"
    }
    options.set(listOf("--strip-debug", "--no-header-files", "--no-man-pages", "--compress=2"))
    jpackage {
        imageName = "Dominoes"
        installerName = "Dominoes"
    }
}
