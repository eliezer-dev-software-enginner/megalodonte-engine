package megalodonte.engine.editor;
import javafx.scene.image.Image;
import java.util.*;
/** Access ordered cache bounded by decoded pixel size (and count while async images load). */
final class ImageCache extends AbstractMap<String,Image> {
    private final LinkedHashMap<String,Image> entries=new LinkedHashMap<>(32,.75f,true);
    private static final long BUDGET=64L*1024*1024;
    @Override public Image get(Object key){var image=entries.get(key);trim();return image;}
    @Override public Image put(String key,Image image){var old=entries.put(key,image);trim();return old;}
    @Override public Set<Entry<String,Image>> entrySet(){return entries.entrySet();}
    @Override public void clear(){entries.clear();}
    private void trim(){long bytes=0;for(var image:entries.values())bytes+=(long)(image.getWidth()*image.getHeight()*4);
        var iterator=entries.values().iterator();while((bytes>BUDGET||entries.size()>512)&&entries.size()>1&&iterator.hasNext()){
            var image=iterator.next();bytes-=(long)(image.getWidth()*image.getHeight()*4);iterator.remove();}
    }
}
