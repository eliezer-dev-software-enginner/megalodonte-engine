plugins { id("com.android.application") }
android {
    namespace = "megalodonte.engine.android"
    compileSdk = 35
    defaultConfig { applicationId = "megalodonte.engine.game"; minSdk = 35; targetSdk = 35; versionCode = 1; versionName = "0.1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    sourceSets.getByName("main").assets.setSrcDirs(listOf(providers.gradleProperty("gameAssets").orElse("src/main/assets").get()))
}
dependencies { implementation(project(":engine-core")) }
