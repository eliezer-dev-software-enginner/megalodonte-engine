package megalodonte.engine;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class SpriteFrameCacheTest {
 @Test void missingNextFramesNeverEraseLoadedActor(){var cache=new SpriteFrameCache<String>();var entity=new Game.Entity();cache.beginFrame(List.of(entity));assertNull(cache.select(entity,null));assertEquals("first",cache.select(entity,"first"));for(int i=0;i<120;i++)assertEquals("first",cache.select(entity,null));assertEquals("second",cache.select(entity,"second"));assertEquals("second",cache.select(entity,null));}
 @Test void releasesOffscreenActorsAndSceneChanges(){var cache=new SpriteFrameCache<String>();var old=new Game.Entity();cache.select(old,"old");cache.beginFrame(List.of(new Game.Entity()));assertNull(cache.select(old,null));cache.select(old,"reload");cache.clear();assertNull(cache.select(old,null));}
}
