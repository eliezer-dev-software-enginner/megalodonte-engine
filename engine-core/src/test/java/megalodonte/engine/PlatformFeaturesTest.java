package megalodonte.engine;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PlatformFeaturesTest {
    @Test void landingSnapsAndColliderCanBeNarrowerThanSprite(){
        var game=Game.demo();var s=game.scenes.get(0);s.objects.clear();var p=new Game.Entity();p.x=2;p.y=5;p.width=2;p.colliderOffsetX=.5;p.colliderWidth=.5;s.objects.add(p);
        var floor=new Game.Entity();floor.x=0;floor.y=0;floor.width=16;floor.height=2;floor.visible=false;s.objects.add(floor);
        try(var runtime=new Runtime(game)){var player=runtime.scene.objects.get(0);var moved=runtime.moveObject(player,0,-5);assertTrue(moved.blockedY());assertEquals(2,player.y,2e-6);assertTrue(runtime.blocked(player,0,-.01));assertEquals(.5,Runtime.collisionBounds(runtime.scene,player).width());}
    }
    @Test void restartRestoresDocumentButRetainsSessionState(){
        var game=Game.demo();game.scenes.get(0).scriptClassName="Restart";
        try(var runtime=new Runtime(game,name->new GameScript(){public void onUpdate(double dt,GameContext c){if(c.actionPressed("restart")){c.state().put("score","42");c.changeScene(c.scene().id);}}},new Runtime.Events(){})){
            runtime.scene.objects.get(0).visible=false;runtime.setAction("restart",true);runtime.update(.01,0,0);assertTrue(runtime.scene.objects.get(0).visible);assertEquals("42",runtime.state.get("score"));
        }
    }
    @Test void animationFramesLoopAndResetAndPauseStopsTime(){
        var game=Game.demo();var p=game.scenes.get(0).objects.get(0);var a=new Game.Animation();a.frames=List.of("assets/one.png","assets/two.png");a.fps=10;p.animations.put("walk",a);Sprites.play(p,"walk");p.animationTime=.15;assertEquals("assets/two.png",Sprites.asset(p));p.animationTime=.25;assertEquals("assets/one.png",Sprites.asset(p));
        try(var runtime=new Runtime(game)){runtime.paused=true;runtime.update(.05,1,0);assertEquals(.25,runtime.scene.objects.get(0).animationTime);assertEquals(2,runtime.scene.objects.get(0).x);}
    }
}
