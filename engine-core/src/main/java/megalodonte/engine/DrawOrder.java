package megalodonte.engine;
import java.util.*;
/** Screen depth based on each collider's foot, independent of sprite transparent margins. */
public final class DrawOrder {
    private DrawOrder(){}
    /** Cull before depth sorting; rotation uses a conservative bounding circle. */
    public static List<Game.Entity> visibleEntities(Game.Scene scene,CameraView view){
        var result=new ArrayList<Game.Entity>();
        for(var entity:scene.objects)if(entity.visible&&inView(scene,entity,view))result.add(entity);
        sort(scene,result);return result;
    }
    public static boolean inView(Game.Scene scene,Game.Entity entity,CameraView view){
        var pose=Hierarchy.pose(scene,entity);double half=Math.hypot(entity.width,entity.height)/2;
        double cx=pose.x()+entity.width/2,cy=pose.y()+entity.height/2;
        return cx+half>=view.left()&&cx-half<=view.left()+view.width()&&cy+half>=view.bottom()&&cy-half<=view.bottom()+view.height();
    }
    private static void sort(Game.Scene scene,List<Game.Entity> result){
        Comparator<Game.Entity> order=Comparator.comparingInt(e->e.drawLayer);
        if(scene.depthSort){
            // Calculate depth once per entity, rather than inside every comparator invocation.
            var depths=new IdentityHashMap<Game.Entity,Double>();
            for(var entity:result)depths.put(entity,Runtime.collisionBounds(scene,entity).y());
            order=order.thenComparing(Comparator.comparingDouble((Game.Entity e)->depths.get(e)).reversed());
        }
        result.sort(order);
    }
    public static List<Game.Entity> entities(Game.Scene scene){
        if(!scene.depthSort&&scene.objects.stream().allMatch(e->e.drawLayer==0))return scene.objects;
        var result=new ArrayList<>(scene.objects);
        sort(scene,result);return result;
    }
}
