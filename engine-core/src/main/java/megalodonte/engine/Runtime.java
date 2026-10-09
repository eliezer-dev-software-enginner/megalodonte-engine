package megalodonte.engine;
/** Shared deterministic movement and AABB collisions; play uses a detached document. */
public final class Runtime implements AutoCloseable {
    public final Game game;
    public Game.Scene scene;
    public interface ScriptFactory { GameScript create(String className) throws Exception; }
    public interface Events { default void log(String message){} default void playSound(String assetPath){} }
    private final ScriptFactory factory;
    public final Events events;
    public double horizontal, vertical, elapsed;
    final java.util.Set<String> pressed=new java.util.HashSet<>(),down=new java.util.HashSet<>(),released=new java.util.HashSet<>();
    private record Attachment(GameScript script,GameContext context){}
    private final java.util.List<Attachment> scripts=new java.util.ArrayList<>();
    private String nextScene;
    private boolean closed;
    public Runtime(Game source){this(source,name->{throw new IllegalArgumentException("Nenhum carregador de scripts configurado: "+name);},new Events(){});}
    public Runtime(Game source,ScriptFactory factory,Events events){
        game=Codec.copy(source);this.factory=factory;this.events=events;
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
        dt = Math.max(0, Math.min(dt, 0.05));elapsed+=dt;
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
        CameraView.update(scene,dt);pressed.clear();released.clear();
        if(nextScene!=null){String target=nextScene;nextScene=null;destroyScripts();scene=game.scenes.stream().filter(s->s.id.equals(target)).findFirst().orElseThrow();elapsed=0;down.clear();startScripts();CameraView.update(scene,0);}
    }
    public void moveObject(Game.Entity entity,double dx,double dy){
        if(!Double.isFinite(dx)||!Double.isFinite(dy))throw new IllegalArgumentException("Movimento inválido");
        double max=Math.max(Math.abs(dx),Math.abs(dy));int steps=Math.min(1000,Math.max(1,(int)Math.ceil(max/0.05)));
        for(int i=0;i<steps;i++){move(entity,dx/steps,0);move(entity,0,dy/steps);}
    }
    private void move(Game.Entity e, double dx, double dy) {
        double x = e.x, y = e.y;
        var world=Hierarchy.pose(scene,e);
        Hierarchy.setWorldPosition(scene,e,
            Math.max(0,Math.min(scene.worldWidth-e.width,world.x()+dx)),
            Math.max(0,Math.min(scene.worldHeight-e.height,world.y()+dy)));
        for(var member:scene.objects)if(Hierarchy.descendant(scene,member,e)){
            var bounds=Hierarchy.worldEntity(scene,member);
            for(var map:scene.tileMaps)for(var obstacle:map.obstacles){var wall=obstacle.copy();wall.x=map.x+wall.x*map.unitScale;wall.y=map.y+wall.y*map.unitScale;wall.width*=map.unitScale;wall.height*=map.unitScale;if(overlaps(bounds,wall)){e.x=x;e.y=y;return;}}
        }
        for(var member:scene.objects)if(Hierarchy.descendant(scene,member,e)){
            var bounds=Hierarchy.worldEntity(scene,member);
            for(var other:scene.objects)if(other.solid && !Hierarchy.descendant(scene,other,e)
                    && !Hierarchy.descendant(scene,e,other) && overlaps(bounds,Hierarchy.worldEntity(scene,other))){e.x=x;e.y=y;return;}
        }
    }
    public static boolean overlaps(Game.Entity a, Game.Entity b) {
        return a.x < b.x+b.width && a.x+a.width > b.x && a.y < b.y+b.height && a.y+a.height > b.y;
    }
}
