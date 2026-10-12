package megalodonte.engine;
import org.junit.jupiter.api.Test;
import java.io.StringReader;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
class StreamingSceneTest {
 @Test void loadsOnlyActiveSceneAndReloadsOnReturn(){
  Game manifest=Game.demo(); manifest.scenes.get(0).objects.clear();
  String first=manifest.scenes.get(0).id;
  var second=new Game.Scene();second.id="second";manifest.scenes.add(second);
  manifest.sceneFiles.put(first,"scenes/0.json");manifest.sceneFiles.put("second","scenes/1.json");
  var reads=new ArrayList<String>();
  try(var runtime=new Runtime(Codec.read(new StringReader(Codec.GSON.toJson(manifest))),n->null,new Runtime.Events(){},id->{
   reads.add(id);var scene=new Game.Scene();scene.id=id;scene.objects.add(new Game.Entity());
   return Codec.readScene(new StringReader(Codec.GSON.toJson(scene)));
  })){
   assertEquals(java.util.List.of(first),reads);assertTrue(runtime.game.scenes.get(1).objects.isEmpty());
   runtime.scene.objects.clear();runtime.requestScene("second");runtime.update(0,0,0);
   assertTrue(runtime.game.scenes.get(0).objects.isEmpty());assertEquals(1,runtime.scene.objects.size());
   runtime.requestScene(first);runtime.update(0,0,0);assertEquals(1,runtime.scene.objects.size());
   assertEquals(java.util.List.of(first,"second",first),reads);
  }
 }
 @Test void rejectsMissingLoaderAndUnsafeManifest(){
  var game=Game.demo();game.sceneFiles.put(game.entrySceneId,"../scene.json");
  assertThrows(IllegalArgumentException.class,()->Codec.validate(game));
  game.sceneFiles.put(game.entrySceneId,"scenes/0.json");
  assertThrows(IllegalArgumentException.class,()->new Runtime(game));
  assertThrows(IllegalStateException.class,()->new Runtime(game,n->null,new Runtime.Events(){},id->{var wrong=new Game.Scene();wrong.id="wrong";return wrong;}));
 }
}
