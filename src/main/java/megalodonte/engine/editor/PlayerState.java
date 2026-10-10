package megalodonte.engine.editor;
import java.util.*;
import java.util.prefs.Preferences;
import megalodonte.engine.Codec;
/** Saves game state separately from the authored project. */
final class PlayerState {
    private final Preferences prefs;
    PlayerState(String id){prefs=Preferences.userRoot().node("megalodonte/games/"+java.util.UUID.nameUUIDFromBytes(id.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
    Map<String,String> load(){try{Map<String,String> values=Codec.GSON.fromJson(prefs.get("state","{}"),new com.google.gson.reflect.TypeToken<Map<String,String>>(){}.getType());return values==null?Map.of():values;}catch(Exception e){return Map.of();}}
    void save(Map<String,String> values){prefs.put("state",Codec.GSON.toJson(values));try{prefs.flush();}catch(java.util.prefs.BackingStoreException e){throw new IllegalStateException("Não foi possível salvar progresso",e);}}
}
