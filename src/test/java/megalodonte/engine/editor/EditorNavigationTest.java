package megalodonte.engine.editor;
import megalodonte.engine.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EditorNavigationTest {
    @Test void navigationRevealsChildrenWithoutChangingGameCamera(){
        var scene=Game.demo().scenes.get(0);var original=Codec.GSON.toJson(scene.camera);var nav=new EditorNavigation();
        nav.pan(scene,5,-3);nav.zoom(scene,200);
        assertEquals(scene.camera.x-5,nav.camera(scene).x);
        var parent=new Game.Entity();parent.x=100;parent.y=50;scene.objects.add(parent);
        var child=new Game.Entity();child.x=4;child.y=2;child.parentId=parent.id;scene.objects.add(child);
        nav.reveal(scene,child,720,480);var pose=Hierarchy.pose(scene,child);
        assertEquals(pose.x()+child.width/2,nav.camera(scene).x);
        assertEquals(pose.y()+child.height/2,nav.camera(scene).y);
        assertEquals(original,Codec.GSON.toJson(scene.camera));
        var other=Game.demo().scenes.get(0);assertEquals(other.camera.x,nav.camera(other).x);
    }
    @Test void visibleSelectionDoesNotMoveViewAndLargeSelectionFits(){
        var scene=Game.demo().scenes.get(0);var nav=new EditorNavigation();var entity=new Game.Entity();
        entity.x=scene.camera.x;entity.y=scene.camera.y;scene.objects.add(entity);
        var before=nav.camera(scene);nav.reveal(scene,entity,720,480);assertEquals(before.x,nav.camera(scene).x);
        entity.x=100;entity.width=200;nav.reveal(scene,entity,720,480);
        assertTrue(CameraView.of(nav.camera(scene),720,480).width()>=entity.width);
    }
}
