package megalodonte.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DetachedDocumentTest {
 @Test void copiesMutableTileArraysPolygonsAnimationsAndHudMaps(){
  var game=Game.demo();var scene=game.scenes.get(0);var map=new Game.TileMap();map.columns=map.rows=map.tileWidth=map.tileHeight=1;var layer=new Game.TileLayer();layer.data=new long[]{0};map.layers.add(layer);scene.tileMaps.add(map);
  var entity=scene.objects.get(0);entity.colliderPolygon=java.util.List.of(0.,0.,1.,0.,1.,1.);var animation=new Game.Animation();animation.frames.add("assets/frame.png");entity.animations.put("idle",animation);var hud=new Game.Hud();hud.setBooleanProperty("enabled",true);scene.hud.add(hud);
  var copy=Codec.copy(game);copy.scenes.get(0).tileMaps.get(0).layers.get(0).data[0]=3;copy.scenes.get(0).objects.get(0).colliderPolygon.set(0,2.);copy.scenes.get(0).objects.get(0).animations.get("idle").frames.clear();copy.scenes.get(0).hud.get(0).setBooleanProperty("enabled",false);
  assertEquals(0,layer.data[0]);assertEquals(0.,entity.colliderPolygon.get(0));assertEquals(1,animation.frames.size());assertTrue(hud.booleanProperty("enabled",false));
 }
 @Test void backdropRepeatsAndStaysInsideItsClippedViewport(){
  var scene=Game.demo().scenes.get(0);scene.backgroundAssetPath="assets/sky.png";var view=CameraView.of(scene.camera,1280,720);var positions=new java.util.ArrayList<Double>();BackgroundDraw.render(scene,view,(a,x,y,w,h)->positions.add(x));assertEquals(2,positions.size());assertEquals(1280.,positions.get(1)-positions.get(0));
 }
}
