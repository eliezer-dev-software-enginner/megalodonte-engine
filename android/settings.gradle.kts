pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "megalodonte-game-android"
include(":app", ":engine-core")
project(":engine-core").projectDir = file("../engine-core")
