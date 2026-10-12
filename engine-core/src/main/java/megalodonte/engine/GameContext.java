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
    public Runtime.Movement move(double x,double y){return runtime.moveObject(object(),x,y);}
    public Runtime.Movement move(Game.Entity target,double x,double y){return runtime.moveObject(target,x,y);}
    public Runtime.CollisionRect collisionBounds(Game.Entity target){return Runtime.collisionBounds(scene(),target);}
    public boolean overlaps(Game.Entity a,Game.Entity b){return overlaps(a,b,0);}
    public boolean overlaps(Game.Entity a,Game.Entity b,double margin){
        if(!Double.isFinite(margin)||margin<0)throw new IllegalArgumentException("Margem de contato inválida");
        if(!a.solid||!b.solid)return false;
        var area=collisionBounds(b);
        return Runtime.overlaps(collisionBounds(a),new Runtime.CollisionRect(area.x()-margin,area.y()-margin,area.width()+margin*2,area.height()+margin*2));
    }
    public void invalidateCollisionGeometry(){runtime.invalidateCollisionGeometry();}
    public boolean grounded(Game.Entity target){return runtime.blocked(target,0,-0.01);}
    public void animate(Game.Entity target,String name){Sprites.play(target,name);}
    public java.util.Map<String,String> state(){return runtime.state;}
    public void saveState(){runtime.events.saveState(java.util.Map.copyOf(runtime.state));}
    public void exportSave(java.util.Map<String,String> values,java.util.function.Consumer<String> done){runtime.events.exportSave(SaveData.encode(runtime.game.applicationId,values),done);}
    public void importSave(java.util.function.UnaryOperator<java.util.Map<String,String>> validate,java.util.function.Consumer<String> done){
        runtime.events.importSave((json,error)->{
            if(error!=null){done.accept(error);return;}
            try{var values=java.util.Map.copyOf(validate.apply(SaveData.decode(runtime.game.applicationId,json)));runtime.events.saveState(values);runtime.state.clear();runtime.state.putAll(values);done.accept("Dados importados.");}
            catch(Exception e){done.accept("Falha ao importar: "+e.getMessage());}
        });
    }
    public void pause(boolean paused){runtime.paused=paused;runtime.events.pauseAudio(paused);}
    public void quit(){runtime.events.quit();}
    public void mute(boolean muted){runtime.events.muteAudio(muted);}
    public void setWorldPosition(double x,double y){Hierarchy.setWorldPosition(scene(),object(),x,y);}
    public Hierarchy.Pose worldTransform(){return Hierarchy.pose(scene(),object());}
    public double horizontal(){return runtime.horizontal;}
    public double vertical(){return runtime.vertical;}
    public boolean actionPressed(String action){return runtime.pressed.contains(action);}
    public boolean actionDown(String action){return runtime.down.contains(action);}
    public boolean actionReleased(String action){return runtime.released.contains(action);}
    public void pressAction(String action){runtime.pressed.add(action);}
    public void changeScene(String id){runtime.requestScene(id);}
    public void log(String message){runtime.events.log(message);}
    public void playSound(String assetPath){Codec.validateAsset(assetPath);runtime.events.playSound(assetPath);}
}
