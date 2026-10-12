package megalodonte.engine;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PolygonCollisionTest {
    @Test void triangleDoesNotBlockEmptyHalfOfTile(){var p=List.of(0.,0.,1.,0.,1.,1.);assertFalse(CollisionGeometry.intersects(new Runtime.CollisionRect(.1,.7,.1,.1),p,0,0,1));assertTrue(CollisionGeometry.intersects(new Runtime.CollisionRect(.7,.1,.1,.1),p,0,0,1));assertFalse(CollisionGeometry.intersects(new Runtime.CollisionRect(1,0,.2,.2),p,0,0,1));}
    @Test void tilePolygonSurvivesSerializationAndBlocksOnlyActualShape(){var g=Game.demo();var s=g.scenes.get(0);s.objects.remove(1);var p=s.objects.get(0);p.width=p.height=.1;p.x=.1;p.y=.7;var m=new Game.TileMap();m.columns=m.rows=1;m.tileWidth=m.tileHeight=1;var o=new Game.Entity();o.x=o.y=0;o.colliderPolygon=List.of(0.,0.,1.,0.,1.,1.);m.obstacles.add(o);s.tileMaps.add(m);try(var r=new Runtime(Codec.copy(g))){assertFalse(r.blocked(r.scene.objects.get(0),0,0));r.scene.objects.get(0).x=.7;r.scene.objects.get(0).y=.1;assertTrue(r.blocked(r.scene.objects.get(0),0,0));}}
    @Test void invalidPolygonRejected(){assertThrows(IllegalArgumentException.class,()->CollisionGeometry.validate(List.of(0.,0.,1.)));assertThrows(IllegalArgumentException.class,()->CollisionGeometry.validate(List.of(0.,0.,1.,1.,2.,2.)));}
}
