package megalodonte.engine;
import java.util.*;
/** Portable game document: positions use world units, origin at bottom left. */
public final class Game {
    public int version = 1;
    public String name = "Meu jogo", entrySceneId = "scene-1";
    public String lastEditedSceneId = "";
    public int versionCode = 1;
    public String versionName = "0.1.0";
    public String iconAssetPath = "";
    public String applicationId = "megalodonte.engine.game";
    public List<Scene> scenes = new ArrayList<>();
    public static final class Scene {
        public String id = "scene-1", name = "Cena 1", background = "#172638";
        public double worldWidth = 16, worldHeight = 9;
        public Camera camera = new Camera();
        public String musicAssetPath = "", scriptClassName = "";
        public String backgroundAssetPath = "";
        public double backgroundParallaxX = .2;
        public double musicVolume = 0.7;
        public boolean musicLoop = true, depthSort = false;
        public List<TileMap> tileMaps = new ArrayList<>();
        public List<Entity> objects = new ArrayList<>();
        public List<Hud> hud = new ArrayList<>();
    }
    public static final class Entity {
        public String id = UUID.randomUUID().toString(), name = "Objeto", color = "#56b6c2", assetPath = "";
        public String parentId = "", scriptClassName = "";
        public int drawLayer = 0;
        public double x = 2, y = 2, width = 1, height = 1, speed = 4, rotation = 0;
        public boolean solid = true, trigger = false, player = false, visible = true, flipX = false, colliderFollowsFlip = true;
        public double colliderOffsetX, colliderOffsetY, colliderWidth, colliderHeight;
        /** Local polygon vertices as x,y pairs; empty uses the rectangular collider. */
        public List<Double> colliderPolygon = new ArrayList<>();
        public Map<String, Animation> animations = new LinkedHashMap<>();
        public String animation = "";
        public double animationTime;
        public Map<String, String> properties = new LinkedHashMap<>();
        public Entity copy() { return Codec.detached(this); }
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
        public String buttonBackground = "#245878", fontFamily = "", textAlign = "left", textVerticalAlign = "top";
        public double buttonOpacity = 1, cornerRadius = 8;
        public String scriptClassName = "", action = "";
        public boolean visible = true, focused = false;
        public double offsetX = 24, offsetY = 24, width = 300, height = 48, fontSize = 32;
        public Map<String,com.google.gson.JsonPrimitive> properties = new LinkedHashMap<>();
        public HudCondition condition = new HudCondition();
        public boolean booleanProperty(String name,boolean fallback){var value=properties.get(name);return value==null?fallback:value.getAsBoolean();}
        public void setBooleanProperty(String name,boolean value){properties.put(name,new com.google.gson.JsonPrimitive(value));}
        public Hud copy() { return Codec.detached(this); }
    }
    /** Optional boolean property drives presentation independently of scripts. */
    public static final class HudCondition {
        public String property = "", trueAssetPath = "", falseAssetPath = "";
        public boolean disableWhenFalse;
    }
    public static final class Camera {
        public String mode = "fixed", targetId = "";
        public double x = 8, y = 4.5, width = 16, height = 9, zoom = 1;
        public double followOffsetX, followOffsetY;
        public boolean holdGroundDuringJump = false;
        public double followSpeed = 6, deadZoneWidth = 0, deadZoneHeight = 0, fadeSeconds = 0;
        public double viewportTop = 0;
        public boolean clampToWorld = true;
        public Camera copy() { return Codec.detached(this); }
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
