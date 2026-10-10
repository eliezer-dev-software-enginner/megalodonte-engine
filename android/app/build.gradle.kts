import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.RenderingHints

plugins { id("com.android.application") }
val customIcon = providers.gradleProperty("gameIcon").orElse("")
val iconResources = layout.buildDirectory.dir("generated/game-icon/res")
val generateGameIcon = tasks.register("generateGameIcon") {
    inputs.property("iconPath", customIcon)
    if (customIcon.get().isNotBlank()) inputs.file(customIcon)
    outputs.dir(iconResources)
    doLast {
        val folder = iconResources.get().asFile
        delete(folder)
        if (customIcon.get().isNotBlank()) {
            val source = ImageIO.read(file(customIcon.get())) ?: error("Ícone PNG inválido")
            fun png(path: String, size: Int, inset: Int = 0) {
                val bitmap = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
                val graphics = bitmap.createGraphics()
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
                val scale = (size - inset * 2).toDouble() / maxOf(source.width, source.height)
                val width = (source.width * scale).toInt(); val height = (source.height * scale).toInt()
                graphics.drawImage(source, (size-width)/2, (size-height)/2, width, height, null); graphics.dispose()
                val target = folder.resolve(path); target.parentFile.mkdirs(); ImageIO.write(bitmap, "png", target)
            }
            for ((density, size) in listOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)) png("mipmap-$density/ic_launcher.png", size)
            png("drawable-nodpi/game_icon_foreground.png", 432, 72)
            val adaptive = folder.resolve("mipmap-anydpi-v26/ic_launcher.xml"); adaptive.parentFile.mkdirs()
            adaptive.writeText("""<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@android:color/black"/><foreground android:drawable="@drawable/game_icon_foreground"/></adaptive-icon>""")
        }
    }
}
android {
    namespace = "megalodonte.engine.android"
    compileSdk = 35
    buildFeatures { resValues = true }
    defaultConfig { applicationId = providers.gradleProperty("gameApplicationId").orElse("megalodonte.engine.game").get();
        require(applicationId!!.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")));
        manifestPlaceholders["gameLauncherIcon"] = if(customIcon.get().isBlank()) "@drawable/engine_icon" else "@mipmap/ic_launcher"
        testInstrumentationRunner = "megalodonte.engine.android.GameSmokeInstrumentation";
        resValue("string", "app_name", providers.gradleProperty("gameName").orElse("Megalodonte Game").get()); minSdk = 35; targetSdk = 35; versionCode = 1; versionName = "0.1.0" }
    sourceSets.getByName("main").res.srcDir(iconResources.get().asFile)
    androidResources { noCompress += listOf("mp3", "wav") }
    sourceSets.getByName("main").java.srcDir(providers.gradleProperty("gameScripts").orElse("src/main/gameScripts").get())
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    sourceSets.getByName("main").assets.setSrcDirs(listOf(providers.gradleProperty("gameAssets").orElse("src/main/assets").get()))
}
dependencies { implementation(project(":engine-core")) }

tasks.named("preBuild") { dependsOn(generateGameIcon) }
