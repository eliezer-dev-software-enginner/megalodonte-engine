package megalodonte.engine.editor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
class EngineSettingsTest {
    @TempDir Path temp;
    @Test void sdkSettingPersistsAndUpdatesExportWithoutLosingOtherProperties()throws Exception{
        String previous=System.getProperty("engine.preferencesFile");System.setProperty("engine.preferencesFile",temp.resolve("prefs/last.txt").toString());
        try{
            var sdk=temp.resolve("Android SDK");Files.createDirectories(sdk.resolve("platforms"));var game=temp.resolve("game");var exported=game.resolve("build/android-release/project");Files.createDirectories(exported);var file=exported.resolve("local.properties");Files.writeString(file,"custom.value=preserved\nsdk.dir=old\n");
            EngineSettings.save(sdk.toString(),game);assertEquals(sdk,EngineSettings.sdk());
            var props=new Properties();try(var in=Files.newInputStream(file)){props.load(in);}assertEquals(sdk.toString(),props.getProperty("sdk.dir"));assertEquals("preserved",props.getProperty("custom.value"));
            assertThrows(IllegalArgumentException.class,()->EngineSettings.save(temp.resolve("missing").toString(),game));assertEquals(sdk,EngineSettings.sdk());
        }finally{if(previous==null)System.clearProperty("engine.preferencesFile");else System.setProperty("engine.preferencesFile",previous);}
    }
}
