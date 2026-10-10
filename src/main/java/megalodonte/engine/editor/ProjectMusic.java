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
    private boolean muted,paused;private double volume=.7;
    void mute(boolean value){muted=value;if(music!=null)music.setVolume(muted?0:volume);for(var player:effects)player.setMute(value);}
    void pause(boolean value){paused=value;if(music!=null){if(value)music.pause();else music.play();}for(var player:effects){if(value)player.pause();else player.play();}}
    void update(Game.Scene scene,Function<String,Path> resolve,java.util.function.Consumer<String> errors){
        volume=scene.musicVolume;if(music!=null)music.setVolume(muted?0:volume);
        String key=MusicTrack.key(scene);if(key.equals(sceneId))return;close();sceneId=key;
        if(scene.musicAssetPath.isBlank())return;
        try{Path path=resolve.apply(scene.musicAssetPath);if(path==null)throw new IllegalArgumentException("Música não encontrada");
            music=new MediaPlayer(new Media(path.toUri().toString()));music.setVolume(muted?0:scene.musicVolume);music.setCycleCount(scene.musicLoop?MediaPlayer.INDEFINITE:1);
            var player=music;player.setOnError(()->errors.accept("Áudio: "+player.getError().getMessage()));if(!paused)player.play();
        }catch(Exception e){errors.accept("Áudio: "+e.getMessage());}
    }
    void sound(Path path){if(path==null||muted||paused)return;var effect=new MediaPlayer(new Media(path.toUri().toString()));effects.add(effect);effect.setOnEndOfMedia(()->{effect.dispose();effects.remove(effect);});effect.play();}
    @Override public void close(){if(music!=null){music.stop();music.dispose();music=null;}for(var effect:effects){effect.stop();effect.dispose();}effects.clear();sceneId="";}
}
