package megalodonte.engine;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SpatialCollisionTest {
    @Test void matchesBruteForceForRectanglesPolygonsTransformsAndBoundaries(){
        var scene=new Game.Scene();scene.worldWidth=500;scene.worldHeight=500;
        var map=new Game.TileMap();map.x=20;map.y=10;map.unitScale=.75;scene.tileMaps.add(map);
        var random=new Random(17);
        for(int i=0;i<300;i++){var e=new Game.Entity();e.x=random.nextDouble()*100;e.y=random.nextDouble()*100;e.width=.5+random.nextDouble()*6;e.height=.5+random.nextDouble()*6;
            if(i%2==0)e.colliderPolygon=List.of(0.,0.,e.width,0.,e.width,e.height);map.obstacles.add(e);}
        var index=new ObstacleIndex();index.refresh(scene);
        for(int i=0;i<2000;i++){var box=new Runtime.CollisionRect(15+random.nextDouble()*90,5+random.nextDouble()*90,.1+random.nextDouble()*4,.1+random.nextDouble()*4);
            boolean expected=false;for(var e:map.obstacles){double x=map.x+e.x*map.unitScale,y=map.y+e.y*map.unitScale;
                if(Runtime.overlaps(box,new Runtime.CollisionRect(x,y,e.width*map.unitScale,e.height*map.unitScale))&&CollisionGeometry.intersects(box,e.colliderPolygon,x,y,map.unitScale)){expected=true;break;}}
            assertEquals(expected,index.intersects(box),"query "+i);}
    }
    @Test void updatesAfterEditsAndExaminesOnlyNearbyGeometry(){
        var scene=new Game.Scene();scene.worldWidth=20000;scene.worldHeight=20;var map=new Game.TileMap();scene.tileMaps.add(map);
        for(int i=0;i<10000;i++){var e=new Game.Entity();e.x=i*2;e.y=0;map.obstacles.add(e);}
        var index=new ObstacleIndex();index.refresh(scene);assertTrue(index.intersects(new Runtime.CollisionRect(2,.5,.2,.2)));
        assertTrue(index.candidates<10,"must not scan all 10000 obstacles");
        map.x=100;index.refresh(scene);assertFalse(index.intersects(new Runtime.CollisionRect(2,.5,.2,.2)));
        map.obstacles.get(0).x=-98;index.refresh(scene);assertTrue(index.intersects(new Runtime.CollisionRect(2,.5,.2,.2)));
        map.obstacles.clear();index.refresh(scene);assertFalse(index.intersects(new Runtime.CollisionRect(2,.5,.2,.2)));
    }
    @Test void largeGeometryAndActorsHaveBoundedQueries(){
        var scene=new Game.Scene();var map=new Game.TileMap();scene.tileMaps.add(map);var floor=new Game.Entity();floor.width=100000;floor.height=1;floor.x=-50000;floor.y=0;map.obstacles.add(floor);
        var index=new ObstacleIndex();index.refresh(scene);assertTrue(index.intersects(new Runtime.CollisionRect(0,.5,1,1)));
        assertTrue(index.intersects(new Runtime.CollisionRect(-100000,-100000,200000,200000)));
    }
    @Test void cullsBeforeSortingAndIncludesRotatedSprites(){
        var scene=new Game.Scene();scene.depthSort=true;var near=new Game.Entity();near.x=0;near.y=0;var far=new Game.Entity();far.x=10000;var hidden=new Game.Entity();hidden.visible=false;
        var rotated=new Game.Entity();rotated.x=-2;rotated.y=0;rotated.width=.1;rotated.height=6;rotated.rotation=90;
        scene.objects.addAll(List.of(near,far,hidden,rotated));var view=new CameraView(1,0,0,0,0,10,10);
        assertEquals(Set.of(near,rotated),new HashSet<>(DrawOrder.visibleEntities(scene,view)));
    }
    @Test void scriptsCanInvalidateEditedGeometryImmediately(){
        var game=Game.demo();var scene=game.scenes.get(0);scene.scriptClassName="edit";var map=new Game.TileMap();map.columns=map.rows=map.tileWidth=map.tileHeight=1;scene.tileMaps.add(map);
        var shape=new Game.Entity();shape.x=10;shape.y=2;map.obstacles.add(shape);
        var found=new java.util.concurrent.atomic.AtomicBoolean();
        try(var runtime=new Runtime(game,name->new GameScript(){public void onUpdate(double dt,GameContext context){
            context.scene().tileMaps.get(0).obstacles.get(0).x=2;context.invalidateCollisionGeometry();found.set(context.grounded(context.scene().objects.get(0)));}},new Runtime.Events(){})){
            runtime.update(.01,0,0);assertTrue(found.get());
        }
    }
    @Test void departingSceneReleasesItsMutableCopyAndResetsOnReturn(){
        var game=Game.demo();var second=Codec.detached(game.scenes.get(0));second.id="second";game.scenes.add(second);
        try(var runtime=new Runtime(game)){
            var first=runtime.scene;double original=first.objects.get(0).x;first.objects.get(0).x=12;
            runtime.requestScene("second");runtime.update(0,0,0);
            assertNotSame(first,runtime.game.scenes.get(0));assertEquals(original,runtime.game.scenes.get(0).objects.get(0).x);
            runtime.requestScene(game.entrySceneId);runtime.update(0,0,0);assertEquals(original,runtime.scene.objects.get(0).x);
        }
    }
}
