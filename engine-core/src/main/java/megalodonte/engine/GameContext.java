package megalodonte.engine;
public final class GameContext {
    private final Runtime runtime;
    private final Game.Entity entity;
    private final Game.Hud hud;
    GameContext(Runtime runtime,Game.Entity entity,Game.Hud hud){this.runtime=runtime;this.entity=entity;this.hud=hud;}
    public Game.Scene scene(){return runtime.scene;}
    public Game.Entity object(){if(entity==null)throw new IllegalStateException("Este script não pertence a um objeto");return entity;}
    public Game.Hud hud(){if(hud==null)throw new IllegalStateException("Este script não pertence a uma HUD");return hud;}
    public Game.Entity findObject(String idOrName){return scene().objects.stream().filter(e->e.id.equals(idOrName)||e.name.equals(idOrName)).findFirst().orElseThrow();}
    public Game.Hud findHud(String id){return scene().hud.stream().filter(e->e.id.equals(id)).findFirst().orElseThrow();}
    public void move(double x,double y){runtime.moveObject(object(),x,y);}
    public void setWorldPosition(double x,double y){Hierarchy.setWorldPosition(scene(),object(),x,y);}
    public Hierarchy.Pose worldTransform(){return Hierarchy.pose(scene(),object());}
    public double horizontal(){return runtime.horizontal;}
    public double vertical(){return runtime.vertical;}
    public boolean actionPressed(String action){return runtime.pressed.contains(action);}
    public boolean actionDown(String action){return runtime.down.contains(action);}
    public boolean actionReleased(String action){return runtime.released.contains(action);}
    public void changeScene(String id){runtime.requestScene(id);}
    public void log(String message){runtime.events.log(message);}
    public void playSound(String assetPath){Codec.validateAsset(assetPath);runtime.events.playSound(assetPath);}
}
