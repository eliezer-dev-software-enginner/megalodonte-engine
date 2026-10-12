package megalodonte.engine;
import com.google.gson.*;
import java.util.*;
public final class Codec {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Game read(String json) {
        Game game = GSON.fromJson(json, Game.class); validate(game); return game;
    }
    public static void validate(Game game) {
        if (game == null || game.version != 1 || game.scenes == null || game.scenes.isEmpty()) throw new IllegalArgumentException("Documento de jogo inválido ou versão incompatível");
        if(game.applicationId==null||!game.applicationId.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))throw new IllegalArgumentException("applicationId inválido");
        if(game.versionCode<1||game.versionCode>2100000000||game.versionName==null||game.versionName.isBlank())throw new IllegalArgumentException("Versão do aplicativo inválida");
        validateAsset(game.iconAssetPath);
        if(!game.iconAssetPath.isBlank()&&!game.iconAssetPath.toLowerCase(java.util.Locale.ROOT).endsWith(".png"))throw new IllegalArgumentException("Ícone deve ser uma imagem PNG");
        Set<String> scenes = new HashSet<>();
        for (Game.Scene scene : game.scenes) {
            if (scene == null || scene.id == null || scene.id.isBlank() || !scenes.add(scene.id) || scene.objects == null || !positive(scene.worldWidth) || !positive(scene.worldHeight)) throw new IllegalArgumentException("Cena inválida");
            validateSceneFeatures(scene);
            if(scene.hud == null) throw new IllegalArgumentException("HUD inválida");
            Set<String> hudIds = new HashSet<>();
            for(Game.Hud h : scene.hud) {
                if(h!=null)HudPresentation.validate(h);
                if(h == null || h.id == null || h.id.isBlank() || !hudIds.add(h.id)
                        || !Set.of("text","image","button").contains(h.type)
                        || !Set.of("viewport","content").contains(h.referenceArea)
                        || !Set.of("left","center","right").contains(h.horizontalAnchor)
                        || !Set.of("top","center","bottom").contains(h.verticalAnchor)
                        || !positive(h.width) || !positive(h.height) || !positive(h.fontSize)
                        || !Double.isFinite(h.offsetX) || !Double.isFinite(h.offsetY)
                        || h.text == null || h.color == null || !h.color.matches("#[0-9a-fA-F]{6}"))
                    throw new IllegalArgumentException("Elemento HUD inválido");
                if(h.assetPath == null || h.assetPath.startsWith("/") || h.assetPath.contains("..") || h.assetPath.contains("\\") || h.assetPath.contains(":"))
                    throw new IllegalArgumentException("Asset da HUD deve ter caminho relativo seguro");
            }
            Set<String> objects = new HashSet<>();
            for (Game.Entity e : scene.objects) {
                if (e == null || e.id == null || e.id.isBlank() || !objects.add(e.id) || !positive(e.width) || !positive(e.height) || !Double.isFinite(e.x) || !Double.isFinite(e.y) || !Double.isFinite(e.rotation) || !Double.isFinite(e.speed) || e.speed < 0) throw new IllegalArgumentException("Objeto inválido");
                if (e.assetPath == null || e.assetPath.startsWith("/") || e.assetPath.contains("..") || e.assetPath.contains("\\") || e.assetPath.contains(":")) throw new IllegalArgumentException("Asset deve ter caminho relativo seguro");
                if (e.color == null || !e.color.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Cor inválida");
            }
            for(var e:scene.objects){
                if(!Double.isFinite(e.opacity)||e.opacity<0||e.opacity>1||e.animation==null||e.animations==null||e.properties==null||!Double.isFinite(e.animationTime)||e.animationTime<0||!Double.isFinite(e.colliderOffsetX)||!Double.isFinite(e.colliderOffsetY)||!Double.isFinite(e.colliderWidth)||!Double.isFinite(e.colliderHeight)||e.colliderWidth<0||e.colliderHeight<0)throw new IllegalArgumentException("Sprite/colisor inválido");
                CollisionGeometry.validate(e.colliderPolygon);
                for(var animation:e.animations.values()){if(animation==null||animation.frames==null||animation.frames.isEmpty()||!positive(animation.fps))throw new IllegalArgumentException("Animação inválida");for(var frame:animation.frames)validateAsset(frame);}
            }
            if(!Double.isFinite(scene.camera.viewportTop)||scene.camera.viewportTop<0||scene.camera.viewportTop>=720)throw new IllegalArgumentException("Margem superior da câmera inválida");
            Hierarchy.validate(scene);
            if(!scene.camera.targetId.isEmpty() && scene.objects.stream().noneMatch(e->e.id.equals(scene.camera.targetId)))throw new IllegalArgumentException("Alvo de câmera inexistente");
            if (scene.background == null || !scene.background.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Fundo inválido");
        }
        if (!scenes.contains(game.entrySceneId)) throw new IllegalArgumentException("Cena inicial inexistente");
    }
    public static void validateAsset(String path){
        if(path==null || path.startsWith("/") || path.contains("..") || path.contains("\\") || path.contains(":"))throw new IllegalArgumentException("Asset deve ter caminho relativo seguro");
    }
    private static void script(String name){if(name==null || (!name.isEmpty()&&!name.matches("[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)*")))throw new IllegalArgumentException("Classe de script inválida");}
    private static void validateSceneFeatures(Game.Scene scene){
        var c=scene.camera;
        if(c==null || !Set.of("fixed","follow").contains(c.mode)|| c.targetId==null || !positive(c.width)||!positive(c.height)||!positive(c.zoom)||!Double.isFinite(c.x)||!Double.isFinite(c.y)||!Double.isFinite(c.followOffsetX)||!Double.isFinite(c.followOffsetY)||!Double.isFinite(c.followSpeed)||c.followSpeed<0||!Double.isFinite(c.fadeSeconds)||c.fadeSeconds<0||!Double.isFinite(c.deadZoneWidth)||c.deadZoneWidth<0||!Double.isFinite(c.deadZoneHeight)||c.deadZoneHeight<0)throw new IllegalArgumentException("Câmera inválida");
        validateAsset(scene.backgroundAssetPath);if(!Double.isFinite(scene.backgroundParallaxX)||scene.backgroundParallaxX<0||scene.backgroundParallaxX>1)throw new IllegalArgumentException("Paralaxe inválida");validateAsset(scene.musicAssetPath);script(scene.scriptClassName);
        if(!Double.isFinite(scene.musicVolume)||scene.musicVolume<0||scene.musicVolume>1)throw new IllegalArgumentException("Volume deve estar entre 0 e 1");
        if(scene.tileMaps==null)throw new IllegalArgumentException("Mapas inválidos");
        Set<String> mapIds=new HashSet<>();
        for(var map:scene.tileMaps){
            if(map==null||map.id==null||map.id.isBlank()||!mapIds.add(map.id)||map.columns<=0||map.rows<=0||map.tileWidth<=0||map.tileHeight<=0||!positive(map.unitScale)||!Double.isFinite(map.x)||!Double.isFinite(map.y)||map.tiles==null||map.layers==null||map.obstacles==null)throw new IllegalArgumentException("Tilemap inválido");
            for(var entry:map.tiles.entrySet()){var tile=entry.getValue();if(entry.getKey()==null||entry.getKey()<=0||tile==null)throw new IllegalArgumentException("Tile inválido");validateAsset(tile.assetPath);if(tile.width<=0||tile.height<=0||tile.sourceX<0||tile.sourceY<0)throw new IllegalArgumentException("Tile inválido");}
            for(var layer:map.layers){if(layer==null||!Double.isFinite(layer.offsetX)||!Double.isFinite(layer.offsetY)||layer.data==null||layer.data.length!=(long)map.columns*map.rows||!Double.isFinite(layer.opacity)||layer.opacity<0||layer.opacity>1)throw new IllegalArgumentException("Camada inválida");for(long gid:layer.data)if(gid<0||gid>0xffffffffL||((gid&0x0fffffffL)!=0&&!map.tiles.containsKey((int)(gid&0x0fffffffL))))throw new IllegalArgumentException("GID inválido ou sem tileset: "+gid);}
            for(var obstacle:map.obstacles){if(obstacle==null||!positive(obstacle.width)||!positive(obstacle.height)||!Double.isFinite(obstacle.x)||!Double.isFinite(obstacle.y))throw new IllegalArgumentException("Colisor do mapa inválido");CollisionGeometry.validate(obstacle.colliderPolygon);}
        }
        for(var e:scene.objects){if(e==null)throw new IllegalArgumentException("Objeto inválido");script(e.scriptClassName);}
        if(scene.hud!=null)for(var h:scene.hud){if(h==null)throw new IllegalArgumentException("HUD inválida");script(h.scriptClassName);if(h.action==null)throw new IllegalArgumentException("Ação HUD inválida");}
    }
    private static boolean positive(double n) { return Double.isFinite(n) && n > 0; }
    public static Game copy(Game game) { validate(game);return detached(game); }
    /** Copy authored data without allocating a JSON string for every tile and animation. */
    @SuppressWarnings("unchecked")
    public static <T> T detached(T value) {
        if(value==null||value instanceof String||value instanceof Number||value instanceof Boolean||value instanceof com.google.gson.JsonPrimitive)return value;
        if(value instanceof long[] array)return (T)array.clone();
        if(value instanceof List<?> list){var copy=new ArrayList<Object>(list.size());for(var item:list)copy.add(detached(item));return (T)copy;}
        if(value instanceof Map<?,?> map){var copy=new LinkedHashMap<Object,Object>();for(var entry:map.entrySet())copy.put(entry.getKey(),detached(entry.getValue()));return (T)copy;}
        Class<?> type=value.getClass();if(type!=Game.class&&type.getEnclosingClass()!=Game.class)throw new IllegalArgumentException("Tipo de documento desconhecido: "+type.getName());
        try{Object copy=type.getDeclaredConstructor().newInstance();for(var field:type.getFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers()))field.set(copy,detached(field.get(value)));return (T)copy;}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Falha ao copiar documento",error);}
    }
}
