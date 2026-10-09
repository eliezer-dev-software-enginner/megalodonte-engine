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
        Set<String> scenes = new HashSet<>();
        for (Game.Scene scene : game.scenes) {
            if (scene == null || scene.id == null || !scenes.add(scene.id) || scene.objects == null || !positive(scene.worldWidth) || !positive(scene.worldHeight)) throw new IllegalArgumentException("Cena inválida");
            if(scene.hud == null) throw new IllegalArgumentException("HUD inválida");
            Set<String> hudIds = new HashSet<>();
            for(Game.Hud h : scene.hud) {
                if(h == null || h.id == null || h.id.isBlank() || !hudIds.add(h.id)
                        || !Set.of("text","image").contains(h.type)
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
                if (e == null || e.id == null || !objects.add(e.id) || !positive(e.width) || !positive(e.height) || !Double.isFinite(e.x) || !Double.isFinite(e.y) || !Double.isFinite(e.rotation) || !Double.isFinite(e.speed) || e.speed < 0) throw new IllegalArgumentException("Objeto inválido");
                if (e.assetPath == null || e.assetPath.startsWith("/") || e.assetPath.contains("..") || e.assetPath.contains("\\") || e.assetPath.contains(":")) throw new IllegalArgumentException("Asset deve ter caminho relativo seguro");
                if (e.color == null || !e.color.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Cor inválida");
            }
            Hierarchy.validate(scene);
            if (scene.background == null || !scene.background.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Fundo inválido");
        }
        if (!scenes.contains(game.entrySceneId)) throw new IllegalArgumentException("Cena inicial inexistente");
    }
    private static boolean positive(double n) { return Double.isFinite(n) && n > 0; }
    public static Game copy(Game game) { return read(GSON.toJson(game)); }
}
