package megalodonte.engine;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SaveDataTest {
    @Test void portableRoundTripRejectsWrongGameAndInvalidTypes(){
        var data=Map.of("progressLevel","3","audio","false");String json=SaveData.encode("game.test",data);assertEquals(data,SaveData.decode("game.test",json));
        assertThrows(IllegalArgumentException.class,()->SaveData.decode("other.game",json));
        assertThrows(IllegalArgumentException.class,()->SaveData.decode("game.test",json.replace("\"3\"","3")));
        assertThrows(IllegalArgumentException.class,()->SaveData.decode("game.test","x".repeat(SaveData.MAX_BYTES+1)));
        assertThrows(IllegalArgumentException.class,()->SaveData.decode("game.test","{}"));
    }
    @Test void invalidImportDoesNotChangeStoredOrRunningState(){
        var game=Game.demo();game.scenes.get(0).scriptClassName="test";var c=new GameContext[1];var saved=new HashMap<>(Map.of("progressLevel","2"));
        var incoming=new String[]{SaveData.encode(game.applicationId,Map.of("progressLevel","4"))};var message=new String[1];
        var events=new Runtime.Events(){public Map<String,String> loadState(){return saved;}public void saveState(Map<String,String> s){saved.clear();saved.putAll(s);}public void importSave(java.util.function.BiConsumer<String,String> done){done.accept(incoming[0],null);}};
        try(var r=new Runtime(game,n->new GameScript(){public void onStart(GameContext context){c[0]=context;}},events)){
            c[0].importSave(s->{throw new IllegalArgumentException("bad level");},s->message[0]=s);assertEquals("2",saved.get("progressLevel"));assertEquals("2",r.state.get("progressLevel"));
            c[0].importSave(s->s,s->message[0]=s);assertEquals("4",saved.get("progressLevel"));assertEquals("4",r.state.get("progressLevel"));
            incoming[0]="{}";c[0].importSave(s->s,s->message[0]=s);assertEquals("4",saved.get("progressLevel"));
        }
    }
}
