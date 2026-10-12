package megalodonte.engine;

import java.util.*;
/** Spatial broad phase for authored map geometry. Dynamic entities remain live, never cached. */
final class ObstacleIndex {
    private static final double CELL = 4;
    private record Obstacle(Game.Entity shape, double x, double y, double scale, Runtime.CollisionRect bounds) {}
    private final Map<Long,List<Obstacle>> cells = new HashMap<>();
    private final List<Obstacle> large = new ArrayList<>();
    private long signature, layoutSignature;
    private Game.Scene owner;
    long candidates;

    void invalidate(){owner=null;}
    void refreshFast(Game.Scene scene){refresh(scene,false);}
    void refresh(Game.Scene scene){refresh(scene,true);}
    private void refresh(Game.Scene scene,boolean inspectGeometry) {
        long layout=1;
        for(var map:scene.tileMaps){layout=31*layout+System.identityHashCode(map);layout=31*layout+System.identityHashCode(map.obstacles);
            layout=31*layout+map.obstacles.size();layout=31*layout+Double.doubleToLongBits(map.x);
            layout=31*layout+Double.doubleToLongBits(map.y);layout=31*layout+Double.doubleToLongBits(map.unitScale);}
        if(!inspectGeometry&&owner==scene&&layoutSignature==layout)return;
        // Detect direct edits too: public document fields may be changed by a script/editor.
        long hash=1;
        for(var map:scene.tileMaps){
            hash=31*hash+System.identityHashCode(map);
            hash=31*hash+Double.doubleToLongBits(map.x);hash=31*hash+Double.doubleToLongBits(map.y);
            hash=31*hash+Double.doubleToLongBits(map.unitScale);
            for(var shape:map.obstacles){hash=31*hash+System.identityHashCode(shape);
                hash=31*hash+Double.doubleToLongBits(shape.x);hash=31*hash+Double.doubleToLongBits(shape.y);
                hash=31*hash+Double.doubleToLongBits(shape.width);hash=31*hash+Double.doubleToLongBits(shape.height);
            }
        }
        if(owner==scene&&signature==hash){layoutSignature=layout;return;}
        owner=scene;signature=hash;layoutSignature=layout;cells.clear();large.clear();
        for(var map:scene.tileMaps)for(var shape:map.obstacles){
            double x=map.x+shape.x*map.unitScale,y=map.y+shape.y*map.unitScale;
            var b=new Runtime.CollisionRect(x,y,shape.width*map.unitScale,shape.height*map.unitScale);
            var obstacle=new Obstacle(shape,x,y,map.unitScale,b);
            int x0=cell(x),x1=cell(x+b.width()),y0=cell(y),y1=cell(y+b.height());
            if(((long)x1-x0+1)*((long)y1-y0+1)>4096){large.add(obstacle);continue;}
            for(int cy=y0;cy<=y1;cy++)for(int cx=x0;cx<=x1;cx++)cells.computeIfAbsent(key(cx,cy),ignored->new ArrayList<>()).add(obstacle);
        }
    }
    boolean intersects(Runtime.CollisionRect area){
        for(var obstacle:large)if(contact(area,obstacle))return true;
        int x0=cell(area.x()),x1=cell(area.x()+area.width()),y0=cell(area.y()),y1=cell(area.y()+area.height());
        // Very large actors must not make a grid query unbounded.
        if(((long)x1-x0+1)*((long)y1-y0+1)>4096){
            var seen=Collections.newSetFromMap(new IdentityHashMap<Obstacle,Boolean>());
            for(var bucket:cells.values())for(var obstacle:bucket)if(seen.add(obstacle)&&contact(area,obstacle))return true;
        }else for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++){
            var bucket=cells.get(key(x,y));if(bucket!=null)for(var obstacle:bucket)if(contact(area,obstacle))return true;
        }
        return false;
    }
    private boolean contact(Runtime.CollisionRect area,Obstacle obstacle){
        candidates++;
        return Runtime.overlaps(area,obstacle.bounds)&&CollisionGeometry.intersects(area,obstacle.shape.colliderPolygon,obstacle.x,obstacle.y,obstacle.scale);
    }
    private static int cell(double x){return (int)Math.floor(x/CELL);}
    private static long key(int x,int y){return ((long)x<<32)^(y&0xffffffffL);}
}
