plugins { id("com.android.application") }
android {
    namespace = "megalodonte.engine.android"
    compileSdk = 35
    buildFeatures { resValues = true }
    defaultConfig { applicationId = providers.gradleProperty("gameApplicationId").orElse("megalodonte.engine.game").get();
        require(applicationId!!.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")));
        testInstrumentationRunner = "megalodonte.engine.android.GameSmokeInstrumentation";
        resValue("string", "app_name", providers.gradleProperty("gameName").orElse("Megalodonte Game").get()); minSdk = 35; targetSdk = 35; versionCode = 1; versionName = "0.1.0" }
    androidResources { noCompress += listOf("mp3", "wav") }
    sourceSets.getByName("main").java.srcDir(providers.gradleProperty("gameScripts").orElse("src/main/gameScripts").get())
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    sourceSets.getByName("main").assets.setSrcDirs(listOf(providers.gradleProperty("gameAssets").orElse("src/main/assets").get()))
}
dependencies { implementation(project(":engine-core")) }
