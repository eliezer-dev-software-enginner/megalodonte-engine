package megalodonte.engine.editor;

import java.util.WeakHashMap;
import megalodonte.engine.*;

/** Editor view offsets never change the camera saved in the game. */
final class EditorNavigation {
    private static final class Offset { double x,y; double zoom=1; }
    private final WeakHashMap<Game.Scene,Offset> offsets=new WeakHashMap<>();
    private Offset offset(Game.Scene scene){return offsets.computeIfAbsent(scene,key->new Offset());}
    Game.Camera camera(Game.Scene scene){
        var result=scene.camera.copy();var offset=offset(scene);
        result.x+=offset.x;result.y+=offset.y;result.zoom=Math.max(.05,Math.min(20,result.zoom*offset.zoom));
        return result;
    }
    void pan(Game.Scene scene,double dx,double dy){var o=offset(scene);o.x-=dx;o.y+=dy;}
    void zoom(Game.Scene scene,double delta){var o=offset(scene);o.zoom=Math.max(.05,Math.min(20,camera(scene).zoom*Math.exp(delta*.002)))/scene.camera.zoom;}
    void reveal(Game.Scene scene,Game.Entity entity,double width,double height){
        if(entity==null||width<=0||height<=0)return;
        var c=camera(scene);var view=CameraView.of(c,width,height);var pose=Hierarchy.pose(scene,entity);
        double x=pose.x()+entity.width/2,y=pose.y()+entity.height/2;
        double angle=Math.toRadians(pose.rotation());
        double w=Math.abs(Math.cos(angle))*entity.width+Math.abs(Math.sin(angle))*entity.height;
        double h=Math.abs(Math.sin(angle))*entity.width+Math.abs(Math.cos(angle))*entity.height;
        if(x-w/2>=view.left()&&x+w/2<=view.left()+view.width()&&y-h/2>=view.bottom()&&y+h/2<=view.bottom()+view.height())return;
        var o=offset(scene);o.x=x-scene.camera.x;o.y=y-scene.camera.y;
        double fit=Math.min(c.width/Math.max(w*1.25,.001),c.height/Math.max(h*1.25,.001));
        if(c.zoom>fit)o.zoom=Math.max(.05,fit)/scene.camera.zoom;
    }
}
