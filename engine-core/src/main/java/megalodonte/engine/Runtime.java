package megalodonte.engine;
/** Shared deterministic movement and AABB collisions; play uses a detached document. */
public final class Runtime implements AutoCloseable {
    public final Game game;
    private final Game baseline;
    public Game.Scene scene;
    public interface ScriptFactory { GameScript create(String className) throws Exception; }
    public interface Events { default void log(String message){} default void playSound(String assetPath){} default void pauseAudio(boolean paused){} default void muteAudio(boolean muted){} default void quit(){} }
    private final ScriptFactory factory;
    public final Events events;
    public double horizontal, vertical, elapsed;
    public boolean paused;
    public final java.util.Map<String,String> state=new java.util.HashMap<>();
    final java.util.Set<String> pressed=new java.util.HashSet<>(),down=new java.util.HashSet<>(),released=new java.util.HashSet<>();
    private record Attachment(GameScript script,GameContext context){}
    private final java.util.List<Attachment> scripts=new java.util.ArrayList<>();
    private String nextScene;
    private boolean closed;
    public Runtime(Game source){this(source,name->{throw new IllegalArgumentException("Nenhum carregador de scripts configurado: "+name);},new Events(){});}
    public Runtime(Game source,ScriptFactory factory,Events events){
        baseline=Codec.copy(source);game=Codec.copy(source);this.factory=factory;this.events=events;
        scene=game.scenes.stream().filter(s->s.id.equals(game.entrySceneId)).findFirst().orElseThrow();
        startScripts();CameraView.update(scene,0);
    }
    public static ScriptFactory reflection(ClassLoader loader){return name->(GameScript)Class.forName(name,true,loader).getDeclaredConstructor().newInstance();}
    private void attach(String name,Game.Entity e,Game.Hud h){if(name.isBlank())return;
        try{var attachment=new Attachment(factory.create(name),new GameContext(this,e,h));scripts.add(attachment);attachment.script.onStart(attachment.context);}
        catch(Exception error){throw new IllegalStateException("Falha no script "+name+": "+error.getMessage(),error);}}
    private void startScripts(){try{attach(scene.scriptClassName,null,null);for(var e:scene.objects)attach(e.scriptClassName,e,null);for(var h:scene.hud)attach(h.scriptClassName,null,h);}
        catch(RuntimeException error){destroyScripts();throw error;}}
    private void destroyScripts(){for(int i=scripts.size()-1;i>=0;i--)try{scripts.get(i).script.onDestroy(scripts.get(i).context);}catch(Exception e){events.log("onDestroy: "+e.getMessage());}scripts.clear();}
    void requestScene(String id){if(game.scenes.stream().noneMatch(s->s.id.equals(id)))throw new IllegalArgumentException("Cena inexistente: "+id);nextScene=id;}
    public void setAction(String action,boolean held){if(held){if(down.add(action))pressed.add(action);}else if(down.remove(action))released.add(action);}
    public double fadeAlpha(){return scene.camera.fadeSeconds<=0?0:Math.max(0,1-elapsed/scene.camera.fadeSeconds);}
    @Override public void close(){if(closed)return;closed=true;destroyScripts();down.clear();pressed.clear();released.clear();}
    public void update(double dt, double horizontal, double vertical) {
        if(closed)return;
        dt = Math.max(0, Math.min(dt, 0.05));if(paused)dt=0;elapsed+=dt;
        double length = Math.hypot(horizontal, vertical);
        if (length > 1) { horizontal /= length; vertical /= length; }
        this.horizontal=horizontal;this.vertical=vertical;
        for (Game.Entity e : scene.objects) if (e.player && e.scriptClassName.isBlank()) {
            // Substeps keep fast objects from crossing thin colliders.
            double dx = horizontal * e.speed * dt, dy = vertical * e.speed * dt;
            int steps = Math.max(1, (int)Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 0.05));
            steps = Math.min(steps, 1000);
            for (int i = 0; i < steps; i++) { move(e, dx/steps, 0); move(e, 0, dy/steps); }
        }
        for(var attachment:java.util.List.copyOf(scripts))attachment.script.onUpdate(dt,attachment.context);
        for(var entity:scene.objects)entity.animationTime+=dt;
        CameraView.update(scene,dt);pressed.clear();released.clear();
        if(nextScene!=null){String target=nextScene;nextScene=null;destroyScripts();int index=0;while(!baseline.scenes.get(index).id.equals(target))index++;scene=Codec.GSON.fromJson(Codec.GSON.toJson(baseline.scenes.get(index)),Game.Scene.class);game.scenes.set(index,scene);elapsed=0;paused=false;events.pauseAudio(false);down.clear();startScripts();CameraView.update(scene,0);}
    }
    public record Movement(double x,double y,boolean blockedX,boolean blockedY){}
    public Movement moveObject(Game.Entity entity,double dx,double dy){
        if(!Double.isFinite(dx)||!Double.isFinite(dy))throw new IllegalArgumentException("Movimento inválido");
        var before=Hierarchy.pose(scene,entity);boolean bx=false,by=false;
        double max=Math.max(Math.abs(dx),Math.abs(dy));int steps=Math.min(1000,Math.max(1,(int)Math.ceil(max/0.05)));
        for(int i=0;i<steps;i++){if(!bx)bx=move(entity,dx/steps,0);if(!by)by=move(entity,0,dy/steps);}
        var after=Hierarchy.pose(scene,entity);return new Movement(after.x()-before.x(),after.y()-before.y(),bx,by);
    }
    private boolean move(Game.Entity e,double dx,double dy){
        var world=Hierarchy.pose(scene,e);double x=Math.max(0,Math.min(scene.worldWidth-e.width,world.x()+dx)),y=Math.max(0,Math.min(scene.worldHeight-e.height,world.y()+dy));
        if(blocked(e,x-world.x(),y-world.y())){
            double low=0,high=1;for(int i=0;i<15;i++){double mid=(low+high)/2;if(blocked(e,(x-world.x())*mid,(y-world.y())*mid))high=mid;else low=mid;}
            Hierarchy.setWorldPosition(scene,e,world.x()+(x-world.x())*low,world.y()+(y-world.y())*low);return true;
        }
        Hierarchy.setWorldPosition(scene,e,x,y);return Math.abs(x-world.x()-dx)>1e-9||Math.abs(y-world.y()-dy)>1e-9;
    }
    public boolean blocked(Game.Entity e,double dx,double dy){
        var world=Hierarchy.pose(scene,e);if(world.y()+dy<0||world.x()+dx<0||world.x()+dx+e.width>scene.worldWidth)return true;
        for(var member:scene.objects)if(Hierarchy.descendant(scene,member,e)){
            var bounds=collisionBounds(scene,member).translate(dx,dy);
            for(var map:scene.tileMaps)for(var obstacle:map.obstacles){
                double x=map.x+obstacle.x*map.unitScale,y=map.y+obstacle.y*map.unitScale;
                if(overlaps(bounds.x(),bounds.y(),bounds.width(),bounds.height(),x,y,obstacle.width*map.unitScale,obstacle.height*map.unitScale))return true;
            }
            for(var other:scene.objects)if(other.solid&&!Hierarchy.descendant(scene,other,e)&&!Hierarchy.descendant(scene,e,other)&&overlaps(bounds,collisionBounds(scene,other)))return true;
        }return false;
    }
    public record CollisionRect(double x,double y,double width,double height){
        public CollisionRect translate(double dx,double dy){return new CollisionRect(x+dx,y+dy,width,height);}
    }
    public static CollisionRect collisionBounds(Game.Scene scene,Game.Entity entity){
        var pose=Hierarchy.pose(scene,entity);return new CollisionRect(pose.x()+entity.colliderOffsetX,pose.y()+entity.colliderOffsetY,entity.colliderWidth>0?entity.colliderWidth:entity.width,entity.colliderHeight>0?entity.colliderHeight:entity.height);
    }
    public static boolean overlaps(CollisionRect a,CollisionRect b){return overlaps(a.x,a.y,a.width,a.height,b.x,b.y,b.width,b.height);}
    private static boolean overlaps(double ax,double ay,double aw,double ah,double bx,double by,double bw,double bh){return ax<bx+bw&&ax+aw>bx&&ay<by+bh&&ay+ah>by;}
    public static boolean overlaps(Game.Entity a, Game.Entity b) {
        return a.x < b.x+b.width && a.x+a.width > b.x && a.y < b.y+b.height && a.y+a.height > b.y;
    }
}
