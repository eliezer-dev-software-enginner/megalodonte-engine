package megalodonte.engine;
import java.util.*;
/** Keep the last decoded frame while asynchronous loading prepares the next one. */
public final class SpriteFrameCache<T> {
 private final Map<Game.Entity,T> frames=new IdentityHashMap<>();
 public void beginFrame(Collection<Game.Entity> visible){frames.keySet().retainAll(visible);}
 public T select(Game.Entity entity,T ready){if(ready!=null)frames.put(entity,ready);return ready==null?frames.get(entity):ready;}
 public void clear(){frames.clear();}
}
