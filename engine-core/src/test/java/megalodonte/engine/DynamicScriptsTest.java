package megalodonte.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DynamicScriptsTest {
 @Test void spawnedScriptStartsUpdatesAndIsDestroyedWhenRemoved(){
  var game=Game.demo();var counts=new int[3];
  try(var runtime=new Runtime(game,name->new GameScript(){public void onStart(GameContext c){counts[0]++;}public void onUpdate(double dt,GameContext c){counts[1]++;}public void onDestroy(GameContext c){counts[2]++;}},new Runtime.Events(){})){
   var projectile=new Game.Entity();projectile.scriptClassName="Projectile";runtime.scene.objects.add(projectile);
   runtime.update(.01,0,0);assertArrayEquals(new int[]{1,0,0},counts);
   runtime.update(.01,0,0);assertArrayEquals(new int[]{1,1,0},counts);
   runtime.scene.objects.remove(projectile);runtime.update(.01,0,0);assertEquals(1,counts[2]);
   runtime.update(.01,0,0);assertArrayEquals(new int[]{1,2,1},counts);
  }
  assertEquals(1,counts[2]);
 }
 @Test void isometricDepthUsesColliderFeetWithoutChangingAuthoredOrder(){
  var scene=new Game.Scene();scene.depthSort=true;var front=new Game.Entity();front.y=1;var back=new Game.Entity();back.y=5;scene.objects.add(front);scene.objects.add(back);
  assertEquals(java.util.List.of(back,front),DrawOrder.entities(scene));assertSame(front,scene.objects.get(0));scene.depthSort=false;assertSame(scene.objects,DrawOrder.entities(scene));
 }
}
