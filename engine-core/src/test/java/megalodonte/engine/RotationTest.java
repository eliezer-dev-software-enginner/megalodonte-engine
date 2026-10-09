package megalodonte.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RotationTest {
    @Test void rotatedSelectionUsesVisualRectangle(){var e=new Game.Entity();e.x=0;e.y=0;e.width=4;e.height=1;e.rotation=90;
        assertTrue(EntityGeometry.contains(e,2,2));assertFalse(EntityGeometry.contains(e,.2,.5));
        e.rotation=0;assertFalse(EntityGeometry.contains(e,2,2));assertTrue(EntityGeometry.contains(e,.2,.5));}
    @Test void rotationPersistsAndInvalidRotationFails(){Game game=Game.demo();game.scenes.get(0).objects.get(0).rotation=45;
        assertEquals(45,Codec.copy(game).scenes.get(0).objects.get(0).rotation);
        game.scenes.get(0).objects.get(0).rotation=Double.NaN;assertThrows(IllegalArgumentException.class,()->Codec.validate(game));
        assertEquals(270,EntityGeometry.normalize(-90));assertEquals(45,EntityGeometry.normalize(405));}
}
