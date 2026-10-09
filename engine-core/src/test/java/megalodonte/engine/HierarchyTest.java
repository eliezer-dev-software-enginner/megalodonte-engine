package megalodonte.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HierarchyTest {
    @Test void attachPreservesPoseAndFollowsParent(){Game game=Game.demo();var s=game.scenes.get(0);var parent=s.objects.get(0);var child=s.objects.get(1);parent.rotation=30;
        var before=Hierarchy.pose(s,child);Hierarchy.attach(s,child,parent);var after=Hierarchy.pose(s,child);
        assertEquals(before.x(),after.x(),1e-8);assertEquals(before.y(),after.y(),1e-8);assertEquals(before.rotation(),after.rotation(),1e-8);
        parent.x+=3;assertEquals(before.x()+3,Hierarchy.pose(s,child).x(),1e-8);
        Hierarchy.attach(s,child,null);assertEquals(before.x()+3,child.x,1e-8);assertEquals("",child.parentId);
    }
    @Test void rejectsCyclesAndMissingParents(){Game game=Game.demo();var s=game.scenes.get(0);var a=s.objects.get(0);var b=s.objects.get(1);Hierarchy.attach(s,b,a);
        assertThrows(IllegalArgumentException.class,()->Hierarchy.attach(s,a,b));assertEquals("",a.parentId);
        a.parentId=b.id;assertThrows(IllegalArgumentException.class,()->Codec.validate(game));a.parentId="missing";assertThrows(IllegalArgumentException.class,()->Codec.validate(game));}
    @Test void duplicateAndDeleteKeepLinksValid(){Game game=Game.demo();var s=game.scenes.get(0);var a=s.objects.get(0);var b=s.objects.get(1);Hierarchy.attach(s,b,a);
        var copy=Hierarchy.duplicate(s,b);assertNotEquals(b.id,copy.id);assertEquals(a.id,copy.parentId);assertEquals(b.x+.5,copy.x);
        var world=Hierarchy.pose(s,b);Hierarchy.remove(s,a);assertEquals("",b.parentId);assertEquals(world.x(),b.x,1e-8);Codec.validate(game);}
    @Test void nestedTransformsAndRoundTrip(){Game game=Game.demo();var s=game.scenes.get(0);var a=s.objects.get(0);a.x=0;a.y=0;a.width=2;a.height=2;a.rotation=90;
        var b=s.objects.get(1);b.parentId=a.id;b.x=2;b.y=0;b.width=2;b.height=2;b.rotation=0;
        var pose=Hierarchy.pose(s,b);assertEquals(0,pose.x(),1e-8);assertEquals(-2,pose.y(),1e-8);assertEquals(90,pose.rotation());
        Hierarchy.setWorldPosition(s,b,5,6);pose=Hierarchy.pose(s,b);assertEquals(5,pose.x(),1e-8);assertEquals(6,pose.y(),1e-8);
        assertEquals(a.id,Codec.copy(game).scenes.get(0).objects.get(1).parentId);}
}
