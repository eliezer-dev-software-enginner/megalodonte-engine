package megalodonte.engine;
import java.util.*;
/** Portable game document: positions use world units, origin at bottom left. */
public final class Game {
    public int version = 1;
    public String name = "Meu jogo", entrySceneId = "scene-1";
    public String lastEditedSceneId = "";
    public String applicationId = "megalodonte.engine.game";
    public List<Scene> scenes = new ArrayList<>();
    public static final class Scene {
        public String id = "scene-1", name = "Cena 1", background = "#172638";
        public double worldWidth = 16, worldHeight = 9;
        public Camera camera = new Camera();
        public String musicAssetPath = "", scriptClassName = "";
        public double musicVolume = 0.7;
        public boolean musicLoop = true;
        public List<TileMap> tileMaps = new ArrayList<>();
        public List<Entity> objects = new ArrayList<>();
        public List<Hud> hud = new ArrayList<>();
    }
    public static final class Entity {
        public String id = UUID.randomUUID().toString(), name = "Objeto", color = "#56b6c2", assetPath = "";
        public String parentId = "", scriptClassName = "";
        public double x = 2, y = 2, width = 1, height = 1, speed = 4, rotation = 0;
        public boolean solid = true, player = false, visible = true, flipX = false;
        public double colliderOffsetX, colliderOffsetY, colliderWidth, colliderHeight;
        public Map<String, Animation> animations = new LinkedHashMap<>();
        public String animation = "";
        public double animationTime;
        public Map<String, String> properties = new LinkedHashMap<>();
        public Entity copy() { return Codec.GSON.fromJson(Codec.GSON.toJson(this), Entity.class); }
    }
    public static final class Animation {
        public List<String> frames = new ArrayList<>();
        public double fps = 10;
        public boolean loop = true;
    }
    /** HUD dimensions/offsets use a 1280x720 reference, origin at top left. */
    public static final class Hud {
        public String id = UUID.randomUUID().toString(), type = "text", text = "Pontuação: 0";
        public String assetPath = "", color = "#ffffff", horizontalAnchor = "left", verticalAnchor = "top";
        public String referenceArea = "viewport";
        public String scriptClassName = "", action = "";
        public boolean visible = true;
        public double offsetX = 24, offsetY = 24, width = 300, height = 48, fontSize = 32;
        public Hud copy() { return Codec.GSON.fromJson(Codec.GSON.toJson(this), Hud.class); }
    }
    public static final class Camera {
        public String mode = "fixed", targetId = "";
        public double x = 8, y = 4.5, width = 16, height = 9, zoom = 1;
        public double followSpeed = 6, deadZoneWidth = 0, deadZoneHeight = 0, fadeSeconds = 0;
        public double viewportTop = 0;
        public boolean clampToWorld = true;
        public Camera copy() { return Codec.GSON.fromJson(Codec.GSON.toJson(this),Camera.class); }
    }
    public static final class TileMap {
        public String id = UUID.randomUUID().toString(), name = "Mapa";
        public int columns, rows, tileWidth, tileHeight;
        public double x, y, unitScale = 1;
        public List<TileLayer> layers = new ArrayList<>();
        public Map<Integer, Tile> tiles = new LinkedHashMap<>();
        public List<Entity> obstacles = new ArrayList<>();
    }
    public static final class TileLayer {
        public String name = "";
        public long[] data;
        public double offsetX, offsetY, opacity = 1;
    }
    public static final class Tile {
        public String assetPath = "";
        public int sourceX, sourceY, width, height;
    }
    public static Game demo() {
        Game game = new Game(); Scene scene = new Scene(); Entity player = new Entity();
        player.name = "Jogador"; player.player = true; scene.objects.add(player);
        Entity wall = new Entity(); wall.name = "Parede"; wall.x = 8; wall.y = 3; wall.width = 2; wall.height = 3;
        wall.color = "#e5a65a"; scene.objects.add(wall); game.scenes.add(scene); return game;
    }
}
