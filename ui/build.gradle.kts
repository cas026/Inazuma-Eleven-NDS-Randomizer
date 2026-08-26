plugins {
    kotlin("jvm")
    id("org.openjfx.javafxplugin") version "0.1.0"
    application
}

dependencies {
    implementation(project(":core"))
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass = "com.example.randomizer.MainKt"
}
