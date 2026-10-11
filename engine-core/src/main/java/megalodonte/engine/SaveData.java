package megalodonte.engine;
import com.google.gson.*;
import java.util.*;
/** Portable player data, separate from game.json and authored scenes. */
public final class SaveData {
    public static final int MAX_BYTES=65536;
    public static String encode(String applicationId,Map<String,String> state){
        var doc=new JsonObject();doc.addProperty("version",1);doc.addProperty("applicationId",applicationId);doc.add("state",Codec.GSON.toJsonTree(state));
        String json=Codec.GSON.toJson(doc);decode(applicationId,json);return json;
    }
    public static Map<String,String> decode(String applicationId,String json){
        try{
            if(json==null||json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>MAX_BYTES)throw new IllegalArgumentException();
            var doc=JsonParser.parseString(json).getAsJsonObject();
            if(!doc.get("version").isJsonPrimitive()||!doc.get("version").getAsJsonPrimitive().isNumber()||!doc.get("version").getAsString().equals("1")||!doc.get("applicationId").getAsString().equals(applicationId))throw new IllegalArgumentException();
            var state=doc.getAsJsonObject("state");var values=new LinkedHashMap<String,String>();
            if(state.size()>64||Codec.GSON.toJson(state).length()>6000)throw new IllegalArgumentException();
            for(var e:state.entrySet()){
                if(e.getKey().isBlank()||e.getKey().length()>128||!e.getValue().isJsonPrimitive()||!e.getValue().getAsJsonPrimitive().isString()||e.getValue().getAsString().length()>512)throw new IllegalArgumentException();
                values.put(e.getKey(),e.getValue().getAsString());
            }return values;
        }catch(Exception e){throw new IllegalArgumentException("JSON de dados salvos inválido ou de outro jogo",e);}
    }
}
