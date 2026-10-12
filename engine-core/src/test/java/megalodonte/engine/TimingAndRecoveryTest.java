package megalodonte.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TimingAndRecoveryTest {
 @Test void fpsUsesUnclampedFrameTimeAndStillMeasuresPause(){
  try(var r=new Runtime(Game.demo())){for(int i=0;i<120;i++)r.update(1./120,0,0);assertEquals(120,r.framesPerSecond(),.01);
   r.paused=true;double elapsed=r.elapsed;for(int i=0;i<10;i++)r.update(.25,0,0);assertEquals(4,r.framesPerSecond(),.01);assertEquals(elapsed,r.elapsed);}
 }
 @Test void recoversBodyPartlyInsideFloorWithoutCrossingWalls(){
  var g=Game.demo();var s=g.scenes.get(0);s.objects.clear();var actor=new Game.Entity();actor.x=5;actor.y=2.1;actor.height=2;s.objects.add(actor);
  var floor=new Game.Entity();floor.x=0;floor.y=2;floor.width=16;floor.height=1;s.objects.add(floor);
  try(var r=new Runtime(g)){var e=r.scene.objects.get(0);assertTrue(r.blocked(e,0,0));assertTrue(r.resolvePenetration(e,2.5));assertEquals(3,e.y,1e-4);assertFalse(r.blocked(e,0,0));assertTrue(r.blocked(e,0,-.01));}
 }
 @Test void failsSafelyWhenEnclosedAndPreservesOriginalPose(){
  var g=Game.demo();var s=g.scenes.get(0);s.objects.clear();var e=new Game.Entity();e.x=5;e.y=5;s.objects.add(e);var wall=new Game.Entity();wall.x=0;wall.y=0;wall.width=16;wall.height=9;s.objects.add(wall);
  try(var r=new Runtime(g)){var actor=r.scene.objects.get(0);assertFalse(r.resolvePenetration(actor,1));assertEquals(5,actor.x);assertEquals(5,actor.y);}
 }
 @Test void forwardsSoundGainAndValidatesSettings(){
  var g=Game.demo();g.scenes.get(0).scriptClassName="sound";var gain=new double[]{-1};
  try(var r=new Runtime(g,name->new GameScript(){public void onStart(GameContext c){c.setSoundVolume(.35);c.playSound("assets/jump.wav");assertThrows(IllegalArgumentException.class,()->c.setSoundVolume(2));}},new Runtime.Events(){public void playSound(String asset,double volume){gain[0]=volume;}})){assertEquals(.35,gain[0]);}
 }
 @Test void animationClocksAdvanceInSecondsAtDifferentFrameRates(){
  for(int hz:new int[]{30,120}){var game=Game.demo();var e=game.scenes.get(0).objects.get(0);var clip=new Game.Animation();clip.frames.add("assets/a.png");e.animations.put("walk",clip);e.animation="walk";
   try(var r=new Runtime(game)){for(int frame=0;frame<hz;frame++)r.update(1./hz,0,0);assertEquals(1,r.scene.objects.get(0).animationTime,1e-9);}
  }
 }
}
