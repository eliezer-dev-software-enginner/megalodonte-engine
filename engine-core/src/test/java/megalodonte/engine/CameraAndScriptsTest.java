package megalodonte.engine;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CameraAndScriptsTest {
    @Test void cameraProjectionZoomAndInverse(){var camera=new Game.Camera();camera.x=20;camera.y=10;camera.zoom=2;
        var view=CameraView.of(camera,800,600);assertEquals(8,view.width());assertEquals(4.5,view.height());assertEquals(400,view.screenX(20));assertEquals(300,view.screenY(10));assertEquals(23,view.worldX(view.screenX(23)),1e-9);assertEquals(7,view.worldY(view.screenY(7)),1e-9);}
    @Test void followAndClampAndDeadZone(){Game game=Game.demo();var scene=game.scenes.get(0);scene.worldWidth=50;scene.worldHeight=20;var target=scene.objects.get(0);target.x=30;target.y=8;scene.camera.mode="follow";scene.camera.targetId=target.id;scene.camera.followSpeed=0;
        CameraView.update(scene,.1);assertEquals(30.5,scene.camera.x);assertEquals(8.5,scene.camera.y);
        scene.camera.deadZoneWidth=4;target.x+=1;CameraView.update(scene,.1);assertEquals(30.5,scene.camera.x);
        target.x=100;CameraView.update(scene,.1);assertEquals(42,scene.camera.x);}
    @Test void followOffsetKeepsAuthoredFramingWhileTrackingVerticalMovement(){
        var scene=Game.demo().scenes.get(0);scene.worldHeight=30;scene.camera.mode="follow";scene.camera.clampToWorld=false;scene.camera.targetId=scene.objects.get(0).id;scene.camera.followSpeed=0;scene.camera.followOffsetX=2;scene.camera.followOffsetY=2.5;
        CameraView.update(scene,.1);var player=scene.objects.get(0);assertEquals(player.x+player.width/2+2,scene.camera.x,1e-9);assertEquals(player.y+player.height/2+2.5,scene.camera.y,1e-9);
        double old=scene.camera.y;player.y-=4;CameraView.update(scene,.1);assertEquals(old-4,scene.camera.y,1e-9);
        var game=Game.demo();game.scenes.get(0).camera.followOffsetY=Double.NaN;assertThrows(IllegalArgumentException.class,()->Codec.validate(game));
    }
    @Test void scriptLifecycleInputAndSceneSwitch(){Game game=Game.demo();var scene=game.scenes.get(0);scene.scriptClassName="Start";var next=new Game.Scene();next.id="next";next.scriptClassName="Next";game.scenes.add(next);List<String> events=new ArrayList<>();
        try(var runtime=new Runtime(game,name->new GameScript(){public void onStart(GameContext c){events.add("start:"+name);}public void onUpdate(double dt,GameContext c){if(c.actionPressed("start"))c.changeScene("next");}public void onDestroy(GameContext c){events.add("end:"+name);}},new Runtime.Events(){})){
            runtime.setAction("start",true);runtime.update(.05,0,0);assertEquals("next",runtime.scene.id);assertEquals(List.of("start:Start","end:Start","start:Next"),events);
            runtime.update(.05,0,0);assertEquals(3,events.size());
        }assertEquals("end:Next",events.get(events.size()-1));}
    @Test void hudScriptAndFadeRemainDetachedFromEditor(){Game game=Game.demo();var hud=new Game.Hud();hud.scriptClassName="Hud";game.scenes.get(0).hud.add(hud);game.scenes.get(0).camera.fadeSeconds=1;
        try(var runtime=new Runtime(game,name->new GameScript(){public void onUpdate(double dt,GameContext c){c.hud().text="atualizado";}},new Runtime.Events(){})){
            runtime.update(.05,0,0);assertEquals("atualizado",runtime.scene.hud.get(0).text);assertEquals("Pontuação: 0",hud.text);assertEquals(.95,runtime.fadeAlpha(),1e-9);
        }}
    @Test void mapObstaclesBlockMovement(){Game game=Game.demo();var scene=game.scenes.get(0);scene.objects.remove(1);var map=new Game.TileMap();map.columns=16;map.rows=9;map.tileWidth=16;map.tileHeight=16;map.unitScale=1.0/16;var obstacle=new Game.Entity();obstacle.x=4*16;obstacle.y=2*16;obstacle.width=16;obstacle.height=16;map.obstacles.add(obstacle);scene.tileMaps.add(map);
        try(var runtime=new Runtime(game)){for(int i=0;i<30;i++)runtime.update(.05,1,0);assertTrue(runtime.scene.objects.get(0).x<=3.0001);}}
    @Test void invalidCameraAudioAndMapDataAreRejected(){
        var game=Game.demo();var scene=game.scenes.get(0);scene.camera.zoom=0;assertThrows(IllegalArgumentException.class,()->Codec.validate(game));scene.camera.zoom=1;
        scene.musicVolume=2;assertThrows(IllegalArgumentException.class,()->Codec.validate(game));scene.musicVolume=.5;
        var map=new Game.TileMap();map.columns=1;map.rows=1;map.tileWidth=16;map.tileHeight=16;scene.tileMaps.add(map);var layer=new Game.TileLayer();layer.data=new long[]{0};layer.offsetX=Double.NaN;map.layers.add(layer);
        assertThrows(IllegalArgumentException.class,()->Codec.validate(game));layer.offsetX=0;layer.data[0]=-1;assertThrows(IllegalArgumentException.class,()->Codec.validate(game));layer.data[0]=0;
        var obstacle=new Game.Entity();obstacle.width=-1;map.obstacles.add(obstacle);assertThrows(IllegalArgumentException.class,()->Codec.validate(game));
    }
}
