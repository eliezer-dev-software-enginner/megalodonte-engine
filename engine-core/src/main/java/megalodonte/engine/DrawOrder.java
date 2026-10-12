package megalodonte.engine;
import java.util.*;
/** Screen depth based on each collider's foot, independent of sprite transparent margins. */
public final class DrawOrder {
    private DrawOrder(){}
    public static List<Game.Entity> entities(Game.Scene scene){
        if(!scene.depthSort&&scene.objects.stream().allMatch(e->e.drawLayer==0))return scene.objects;
        var result=new ArrayList<>(scene.objects);
        Comparator<Game.Entity> order=Comparator.comparingInt(e->e.drawLayer);
        if(scene.depthSort)order=order.thenComparing(Comparator.comparingDouble((Game.Entity e)->Runtime.collisionBounds(scene,e).y()).reversed());
        result.sort(order);return result;
    }
}
