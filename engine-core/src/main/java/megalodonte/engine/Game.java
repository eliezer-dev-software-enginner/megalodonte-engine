package megalodonte.engine;
import java.util.*;
/** Portable game document: positions use world units, origin at bottom left. */
public final class Game {
    public int version = 1;
    public String name = "Meu jogo", entrySceneId = "scene-1";
    public List<Scene> scenes = new ArrayList<>();
    public static final class Scene {
        public String id = "scene-1", name = "Cena 1", background = "#172638";
        public double worldWidth = 16, worldHeight = 9;
        public List<Entity> objects = new ArrayList<>();
        public List<Hud> hud = new ArrayList<>();
    }
    public static final class Entity {
        public String id = UUID.randomUUID().toString(), name = "Objeto", color = "#56b6c2", assetPath = "";
        public double x = 2, y = 2, width = 1, height = 1, speed = 4;
        public boolean solid = true, player = false;
        public Entity copy() { return Codec.GSON.fromJson(Codec.GSON.toJson(this), Entity.class); }
    }
    /** HUD dimensions/offsets use a 1280x720 reference, origin at top left. */
    public static final class Hud {
        public String id = UUID.randomUUID().toString(), type = "text", text = "Pontuação: 0";
        public String assetPath = "", color = "#ffffff", horizontalAnchor = "left", verticalAnchor = "top";
        public double offsetX = 24, offsetY = 24, width = 300, height = 48, fontSize = 32;
        public Hud copy() { return Codec.GSON.fromJson(Codec.GSON.toJson(this), Hud.class); }
    }
    public static Game demo() {
        Game game = new Game(); Scene scene = new Scene(); Entity player = new Entity();
        player.name = "Jogador"; player.player = true; scene.objects.add(player);
        Entity wall = new Entity(); wall.name = "Parede"; wall.x = 8; wall.y = 3; wall.width = 2; wall.height = 3;
        wall.color = "#e5a65a"; scene.objects.add(wall); game.scenes.add(scene); return game;
    }
}
