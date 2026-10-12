package megalodonte.engine.editor;
import javafx.scene.media.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import megalodonte.engine.*;
final class ProjectMusic implements AutoCloseable {
    private MediaPlayer music;
    private final Map<Path,AudioClip> effects=new LinkedHashMap<>();
    private Game.Scene effectsScene;
    private String sceneId="";
    private boolean muted,paused;private double volume=.7;
    void mute(boolean value){muted=value;if(music!=null)music.setVolume(muted?0:volume);if(value)for(var clip:effects.values())clip.stop();}
    void pause(boolean value){paused=value;if(music!=null){if(value)music.pause();else music.play();}if(value)for(var clip:effects.values())clip.stop();}
    void update(Game.Scene scene,Function<String,Path> resolve,java.util.function.Consumer<String> errors){
        if(effectsScene!=scene){effectsScene=scene;for(var clip:effects.values())clip.stop();effects.clear();
            for(var entity:scene.objects)for(var property:entity.properties.entrySet())if(property.getKey().endsWith("Sound")){
                try{var path=resolve.apply(property.getValue());if(path!=null)effects.computeIfAbsent(path,p->new AudioClip(p.toUri().toString()));}catch(Exception error){errors.accept("Efeito: "+error.getMessage());}}
        }
        volume=scene.musicVolume;if(music!=null)music.setVolume(muted?0:volume);
        String key=MusicTrack.key(scene);if(key.equals(sceneId))return;if(music!=null){music.stop();music.dispose();music=null;}sceneId=key;
        if(scene.musicAssetPath.isBlank())return;
        try{Path path=resolve.apply(scene.musicAssetPath);if(path==null)throw new IllegalArgumentException("Música não encontrada");
            music=new MediaPlayer(new Media(path.toUri().toString()));music.setVolume(muted?0:scene.musicVolume);music.setCycleCount(scene.musicLoop?MediaPlayer.INDEFINITE:1);
            var player=music;player.setOnError(()->errors.accept("Áudio: "+player.getError().getMessage()));if(!paused)player.play();
        }catch(Exception e){errors.accept("Áudio: "+e.getMessage());}
    }
    void sound(Path path){sound(path,1);}
    void sound(Path path,double volume){if(path==null||muted||paused)return;
        if(!effects.containsKey(path)&&effects.size()>=64){var iterator=effects.values().iterator();iterator.next().stop();iterator.remove();}
        effects.computeIfAbsent(path,p->new AudioClip(p.toUri().toString())).play(volume);
    }
    @Override public void close(){if(music!=null){music.stop();music.dispose();music=null;}for(var clip:effects.values())clip.stop();effects.clear();sceneId="";effectsScene=null;}
}
