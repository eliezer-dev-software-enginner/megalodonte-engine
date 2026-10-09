package megalodonte.engine.editor;
import javafx.scene.media.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import megalodonte.engine.*;
final class ProjectMusic implements AutoCloseable {
    private MediaPlayer music;
    private final List<MediaPlayer> effects=new ArrayList<>();
    private String sceneId="";
    void update(Game.Scene scene,Function<String,Path> resolve,java.util.function.Consumer<String> errors){
        if(scene.id.equals(sceneId))return;close();sceneId=scene.id;
        if(scene.musicAssetPath.isBlank())return;
        try{Path path=resolve.apply(scene.musicAssetPath);if(path==null)throw new IllegalArgumentException("Música não encontrada");
            music=new MediaPlayer(new Media(path.toUri().toString()));music.setVolume(scene.musicVolume);music.setCycleCount(scene.musicLoop?MediaPlayer.INDEFINITE:1);
            var player=music;player.setOnError(()->errors.accept("Áudio: "+player.getError().getMessage()));player.play();
        }catch(Exception e){errors.accept("Áudio: "+e.getMessage());}
    }
    void sound(Path path){if(path==null)return;var effect=new MediaPlayer(new Media(path.toUri().toString()));effects.add(effect);effect.setOnEndOfMedia(()->{effect.dispose();effects.remove(effect);});effect.play();}
    @Override public void close(){if(music!=null){music.stop();music.dispose();music=null;}for(var effect:effects){effect.stop();effect.dispose();}effects.clear();sceneId="";}
}
