package megalodonte.engine;
/** Animation clocks belong to the runtime copy, so editor preview and saved files stay intact. */
public final class Sprites {
    private Sprites(){}
    public static void play(Game.Entity entity,String name){
        if(!entity.animations.containsKey(name))throw new IllegalArgumentException("Animação inexistente: "+name);
        if(!entity.animation.equals(name)){entity.animation=name;entity.animationTime=0;}
    }
    public static String asset(Game.Entity entity){return asset(entity,entity.animationTime);}
    public static String asset(Game.Entity entity,double elapsed){
        var animation=entity.animations.get(entity.animation);if(animation==null||animation.frames.isEmpty())return entity.assetPath;
        int frame=(int)Math.floor(elapsed*animation.fps);
        frame=animation.loop?Math.floorMod(frame,animation.frames.size()):Math.min(frame,animation.frames.size()-1);return animation.frames.get(frame);
    }
}
