plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}
group = "megalodonte"
version = "0.1.0"
repositories { mavenLocal(); mavenCentral() }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
javafx { version = "25.0.1"; modules("javafx.controls", "javafx.graphics", "javafx.media") }
dependencies {
    implementation(project(":engine-core"))
    implementation("megalodonte:megalodonte-script-app:0.1.0")
    testImplementation("megalodonte:megalodonte-theme:1.0.0-beta")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
application {
    mainClass.set("megalodonte.script.ScriptLauncher")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}
tasks.test { systemProperty("engine.preferencesFile", layout.buildDirectory.file("test-state/last-saved-project.txt").get().asFile.absolutePath); forkEvery = 1; useJUnitPlatform(); jvmArgs("--enable-native-access=ALL-UNNAMED") }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

tasks.register<JavaExec>("createExample") {
    group = "application"
    description = "Creates a playable map/audio/camera/scripts example in -PexampleDir"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("megalodonte.engine.editor.ExampleProject")
    args(providers.gradleProperty("exampleDir").orElse(layout.buildDirectory.dir("example-game").get().asFile.absolutePath).get())
}
