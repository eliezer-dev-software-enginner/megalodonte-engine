package megalodonte.engine;
import java.util.*;
/** Local position relative to the parent's bottom-left; rotation inherited around its center. */
public final class Hierarchy {
    private Hierarchy() { }
    public record Pose(double x,double y,double rotation) { }
    public static Game.Entity find(Game.Scene scene,String id) {
        return scene.objects.stream().filter(e->e.id.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Pai inexistente: "+id));
    }
    public static void validate(Game.Scene scene) {
        for(var e:scene.objects){Set<String> seen=new HashSet<>();var current=e;
            while(true){if(!seen.add(current.id))throw new IllegalArgumentException("Vínculo entre objetos forma um ciclo");
                if(current.parentId==null)throw new IllegalArgumentException("Referência de pai inválida");
                if(current.parentId.isEmpty())break;current=find(scene,current.parentId);}
        }
    }
    public static boolean descendant(Game.Scene scene,Game.Entity child,Game.Entity ancestor) {
        var current=child;
        for(int i=0;i<scene.objects.size();i++){
            if(current.id.equals(ancestor.id))return true;
            if(current.parentId.isEmpty())return false;current=find(scene,current.parentId);
        }return false;
    }
    public static Pose pose(Game.Scene scene,Game.Entity entity) {
        List<Game.Entity> chain=new ArrayList<>();Set<String> seen=new HashSet<>();var current=entity;
        while(true){if(!seen.add(current.id))throw new IllegalArgumentException("Ciclo de objetos");chain.add(current);if(current.parentId.isEmpty())break;current=find(scene,current.parentId);}
        Pose result=new Pose(current.x,current.y,current.rotation);
        for(int i=chain.size()-2;i>=0;i--){var child=chain.get(i);var parent=chain.get(i+1);
            double dx=child.x+child.width/2-parent.width/2,dy=child.y+child.height/2-parent.height/2;
            double angle=Math.toRadians(-result.rotation%360),cos=Math.cos(angle),sin=Math.sin(angle);
            result=new Pose(result.x+parent.width/2+dx*cos-dy*sin-child.width/2,
                result.y+parent.height/2+dx*sin+dy*cos-child.height/2,EntityGeometry.normalize(result.rotation+child.rotation));
        }return result;
    }
    public static void setWorldPosition(Game.Scene scene,Game.Entity e,double x,double y) {
        if(e.parentId.isEmpty()){e.x=x;e.y=y;return;}
        var parent=find(scene,e.parentId);var p=pose(scene,parent);double angle=Math.toRadians(p.rotation%360);
        double dx=x+e.width/2-(p.x+parent.width/2),dy=y+e.height/2-(p.y+parent.height/2);
        e.x=dx*Math.cos(angle)-dy*Math.sin(angle)+parent.width/2-e.width/2;
        e.y=dx*Math.sin(angle)+dy*Math.cos(angle)+parent.height/2-e.height/2;
    }
    /** Reparent without visually moving the object; rejects cycles before mutation. */
    public static void attach(Game.Scene scene,Game.Entity child,Game.Entity parent) {
        if(parent!=null && descendant(scene,parent,child))throw new IllegalArgumentException("O pai não pode ser o próprio objeto ou um descendente");
        Pose world=pose(scene,child);double parentRotation=parent==null?0:pose(scene,parent).rotation;
        child.parentId=parent==null?"":parent.id;
        setWorldPosition(scene,child,world.x,world.y);child.rotation=EntityGeometry.normalize(world.rotation-parentRotation);
    }
    public static Game.Entity duplicate(Game.Scene scene,Game.Entity source) {
        var copy=source.copy();copy.id=UUID.randomUUID().toString();copy.name=source.name+" (cópia)";
        copy.x+=0.5;copy.y+=0.5;scene.objects.add(copy);return copy;
    }
    public static void remove(Game.Scene scene,Game.Entity e) {
        for(var child:scene.objects)if(child.parentId.equals(e.id))attach(scene,child,null);
        scene.objects.remove(e);
    }
    public static Game.Entity worldEntity(Game.Scene scene,Game.Entity e) {
        var result=new Game.Entity();var p=pose(scene,e);result.x=p.x;result.y=p.y;result.rotation=p.rotation;result.width=e.width;result.height=e.height;return result;
    }
}
