package megalodonte.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CollisionAreaTest {
    @Test void triggerUsesAreaAndDoesNotBlockMovement(){
        var game=Game.demo();var scene=game.scenes.get(0);scene.objects.clear();scene.scriptClassName="test";
        var player=new Game.Entity();player.x=1;player.y=2;scene.objects.add(player);
        var hazard=new Game.Entity();hazard.x=2;hazard.y=2;hazard.width=4;hazard.colliderOffsetX=2;hazard.colliderWidth=.5;hazard.colliderHeight=1;hazard.trigger=true;scene.objects.add(hazard);
        var context=new GameContext[1];try(var runtime=new Runtime(game,name->new GameScript(){public void onStart(GameContext c){context[0]=c;}},new Runtime.Events(){})){
            var c=context[0];var p=c.scene().objects.get(0);var h=c.scene().objects.get(1);
            p.x=2.5;assertFalse(c.overlaps(p,h),"Transparent sprite margin must not hurt player");
            p.x=4;assertTrue(c.overlaps(p,h));assertFalse(runtime.blocked(p,.1,0),"Trigger must not be a wall");
            h.trigger=false;assertTrue(runtime.blocked(p,.1,0));
            h.solid=false;assertFalse(c.overlaps(p,h));assertFalse(runtime.blocked(p,.1,0));
            h.solid=true;h.trigger=true;p.x=3;assertFalse(c.overlaps(p,h));assertTrue(c.overlaps(p,h,.08));
        }
    }
    @Test void collisionAreaTransformsWithParentRotationAndFlip(){
        var scene=Game.demo().scenes.get(0);scene.objects.clear();var parent=new Game.Entity();parent.x=10;parent.y=3;scene.objects.add(parent);
        var e=new Game.Entity();e.x=2;e.y=1;e.parentId=parent.id;e.width=4;e.height=2;e.colliderWidth=1;e.colliderHeight=2;scene.objects.add(e);
        var bounds=Runtime.collisionBounds(scene,e);assertEquals(12,bounds.x(),1e-9);
        e.flipX=true;assertEquals(15,Runtime.collisionBounds(scene,e).x(),1e-9);
        e.rotation=90;bounds=Runtime.collisionBounds(scene,e);assertEquals(2,bounds.width(),1e-9);assertEquals(1,bounds.height(),1e-9);
    }
    @Test void playerColliderCanStayStableAcrossFacingAndSpriteWidth(){
        var scene=Game.demo().scenes.get(0);var e=scene.objects.get(0);
        e.width=32.0/24;e.height=64.0/24;e.colliderOffsetX=8.0/24;e.colliderOffsetY=4.0/24;e.colliderWidth=.5;e.colliderHeight=2.5;e.colliderFollowsFlip=false;
        var before=Runtime.collisionBounds(scene,e);e.flipX=true;assertEquals(before,Runtime.collisionBounds(scene,e));
        e.width=64.0/24;var after=Runtime.collisionBounds(scene,e);assertEquals(before.x(),after.x(),1e-9);assertEquals(before.y(),after.y(),1e-9);assertEquals(before.width(),after.width(),1e-9);
    }
    @Test void legacyOverlapUsesColliderRatherThanSprite(){
        var a=new Game.Entity();a.x=0;a.y=0;a.width=10;a.colliderWidth=1;a.colliderHeight=1;
        var b=new Game.Entity();b.x=5;b.y=0;assertFalse(Runtime.overlaps(a,b));b.x=.5;assertTrue(Runtime.overlaps(a,b));
    }
}
