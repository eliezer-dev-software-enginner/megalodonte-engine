package megalodonte.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RuntimeTest {
    @Test void playDoesNotMutateEditor(){Game game=Game.demo();var runtime=new Runtime(game);runtime.update(.05,1,0);assertEquals(2,game.scenes.get(0).objects.get(0).x);assertTrue(runtime.scene.objects.get(0).x>2);}
    @Test void collisionStopsPlayer(){Game game=Game.demo();var p=game.scenes.get(0).objects.get(0);p.x=6.9;p.y=3;var runtime=new Runtime(game);for(int i=0;i<30;i++)runtime.update(.05,1,0);assertTrue(runtime.scene.objects.get(0).x<=7);}
    @Test void rejectsTraversalAndInvalidNumbers(){Game game=Game.demo();game.scenes.get(0).objects.get(0).assetPath="../secret";assertThrows(IllegalArgumentException.class,()->Codec.validate(game));Game invalid=Game.demo();invalid.scenes.get(0).objects.get(0).width=Double.NaN;assertThrows(IllegalArgumentException.class,()->Codec.validate(invalid));}
    @Test void roundTripAndEntryScene(){Game game=Game.demo();var next=new Game.Scene();next.id="second";game.scenes.add(next);game.entrySceneId="second";assertEquals("second",new Runtime(Codec.read(Codec.GSON.toJson(game))).scene.id);}
}
