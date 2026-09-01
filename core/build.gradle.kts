plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    // Pass -Prom.path=/path/to/rom.nds from the command line to enable roundtrip tests
    systemProperty("rom.path", project.findProperty("rom.path") ?: "")
}

tasks.register<JavaExec>("dumpRom") {
    group = "verification"
    description = "Prints all files in an NDS ROM. Usage: ./gradlew :core:dumpRom --args=\"/pad/naar/rom.nds\""
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.example.randomizer.dump.RomDumpKt"
}

tasks.register<JavaExec>("dumpStats") {
    group = "verification"
    description = "Prints all player stats from an NDS ROM. Usage: ./gradlew :core:dumpStats --args=\"/pad/naar/rom.nds\""
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.example.randomizer.dump.StatDumpKt"
}


tasks.register<JavaExec>("randomize") {
    group = "verification"
    description = "Randomizes player stats and writes a patched ROM. Usage: ./gradlew :core:randomize --args=\"<input.nds> <output.nds> [seed] [variance]\""
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.example.randomizer.randomizer.RandomizeCliKt"
}
