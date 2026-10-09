package megalodonte.engine.editor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class PendingAssetsTest {
    @TempDir Path temp;
    @Test void unsavedImportSurvivesSourceRemovalAndPublishesOnSave() throws Exception {
        Path source=temp.resolve("sprite.PNG");byte[] bytes={1,2,3};Files.write(source,bytes);
        try(var assets=new PendingAssets()){
            String asset=assets.stage(source);Path snapshot=assets.resolve(asset,null);Files.delete(source);
            assertArrayEquals(bytes,Files.readAllBytes(snapshot));
            Path project=temp.resolve("game");assets.publish(project);
            assertArrayEquals(bytes,Files.readAllBytes(project.resolve(asset)));
            assets.close();assertFalse(Files.exists(snapshot));
            assertEquals(project.resolve(asset),assets.resolve(asset,project));
        }
    }
}
