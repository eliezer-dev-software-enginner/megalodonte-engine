package megalodonte.engine;
import java.util.*;
/** Standard semantic buttons; actions are isolated from keyboard and touch. */
public final class GamepadInput {
    private double x,y,hatX,hatY;
    private final Set<String> buttons=new HashSet<>(),blocked=new HashSet<>();
    private Game.Scene lastScene;
    public void axes(double x,double y,double hatX,double hatY){this.x=deadZone(x);this.y=deadZone(y);this.hatX=deadZone(hatX);this.hatY=deadZone(hatY);}
    public static double deadZone(double value){return !Double.isFinite(value)||Math.abs(value)<.2?0:Math.max(-1,Math.min(1,value));}
    public double horizontal(){return Math.max(-1,Math.min(1,(hatX!=0?hatX:x)+(held("right")?1:0)-(held("left")?1:0)));}
    public double vertical(){return Math.max(-1,Math.min(1,(hatY!=0?hatY:y)+(held("down")?1:0)-(held("up")?1:0)));}
    public void button(String name,boolean held){if(held)buttons.add(name);else buttons.remove(name);}
    public boolean held(String name){return buttons.contains(name);}
    public void clear(){buttons.clear();x=y=hatX=hatY=0;}
    public void apply(Runtime runtime){
        var actions=new HashSet<>(buttons);double dx=horizontal(),dy=vertical();
        if(dx<-.5)actions.add("left");if(dx>.5)actions.add("right");if(dy<-.5)actions.add("up");if(dy>.5)actions.add("down");
        if(lastScene!=null&&lastScene!=runtime.scene)blocked.addAll(actions);lastScene=runtime.scene;
        blocked.retainAll(actions);
        for(String name:List.of("left","right","up","down","a","b","start"))runtime.setAction("pad_"+name,actions.contains(name)&&!blocked.contains(name));
    }
}
