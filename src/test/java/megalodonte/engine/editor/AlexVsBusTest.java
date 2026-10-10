package megalodonte.engine.editor;
import megalodonte.engine.*;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class AlexVsBusTest {
    static Path project=Path.of("../../games/alex-vs-bus").toAbsolutePath().normalize();
    static ScriptCompiler compiler;static Game game;
    @BeforeAll static void setup()throws Exception{Assumptions.assumeTrue(Files.exists(project.resolve("game.json")));game=Codec.read(Files.readString(project.resolve("game.json")));compiler=ScriptCompiler.compile(project);}
    @AfterAll static void close(){if(compiler!=null)compiler.close();}
    private megalodonte.engine.Runtime start(String id){var copy=Codec.copy(game);copy.entrySceneId=id;return new megalodonte.engine.Runtime(copy,megalodonte.engine.Runtime.reflection(compiler.loader),new megalodonte.engine.Runtime.Events(){});}
    @Test void allThirteenLevelsAndEveryAssetArePortable()throws Exception{
        assertEquals(25,game.scenes.size());assertEquals(13,game.scenes.stream().filter(s->s.id.matches("(normal|hard|super)-[1-5]")).count());
        for(var scene:game.scenes){asset(scene.musicAssetPath);for(var e:scene.objects){asset(e.assetPath);for(var a:e.animations.values())for(var frame:a.frames)asset(frame);}for(var h:scene.hud)asset(h.assetPath);for(var map:scene.tileMaps)for(var tile:map.tiles.values())asset(tile.assetPath);}
    }
    private void asset(String path){if(!path.isBlank())assertTrue(Files.isRegularFile(project.resolve(path)),path);}
    private void press(megalodonte.engine.Runtime r,String action){r.setAction(action,true);r.update(.01,0,0);r.setAction(action,false);r.update(.01,0,0);}
    @Test void menuAnchorsMatchVisualPlacement(){
        var back=game.scenes.stream().filter(scene->scene.id.equals("menu-settings")).findFirst().orElseThrow().hud.stream().filter(h->h.id.equals("settings-back")).findFirst().orElseThrow();
        assertEquals("left",back.horizontalAnchor);assertEquals("top",back.verticalAnchor);assertEquals("content",back.referenceArea);
        var audio=game.scenes.get(0).hud.stream().filter(h->h.id.equals("audio")).findFirst().orElseThrow();assertEquals("right",audio.horizontalAnchor);assertEquals("top",audio.verticalAnchor);
        for(var scene:game.scenes)if(scene.id.startsWith("menu"))assertTrue(scene.hud.stream().allMatch(h->h.referenceArea.equals("content")));
    }
    @Test void everySubmenuIsVisibleAndEditableWithoutScripts(){
        for(String page:new String[]{"difficulty","levels","settings","display","sound","vscreen","jukebox","about","credits","quit"}){
            var scene=game.scenes.stream().filter(s->s.id.equals("menu-"+page)).findFirst().orElseThrow();assertFalse(scene.hud.isEmpty());assertTrue(scene.hud.stream().allMatch(h->h.visible));assertTrue(scene.hud.stream().allMatch(h->h.id.startsWith(page+"-")));
        }
    }
    @Test void menuSelectsDifficultyAndLevel(){try(var r=start("menu")){press(r,"start");press(r,"difficulty-super");press(r,"levels-level3");assertEquals("super-3",r.scene.id);}}
    @Test void menusNavigateAndSettingsAffectGame(){try(var r=start("menu")){
        press(r,"settings");press(r,"settings-sound");press(r,"sound-music");assertEquals(0,r.scene.musicVolume);press(r,"sound-sfx");assertEquals("false",r.state.get("sfx"));
        press(r,"sound-back");press(r,"settings-touch");press(r,"settings-display");press(r,"display-scanlines");assertEquals("true",r.state.get("scanlines"));press(r,"display-size");press(r,"vscreen-width");assertEquals("432",r.state.get("width"));
        press(r,"vscreen-back");press(r,"display-back");press(r,"settings-back");press(r,"jukebox");press(r,"jukebox-track2");assertEquals("assets/bgm2.mp3",r.scene.musicAssetPath);press(r,"jukebox-back");
        press(r,"about");assertTrue(r.scene.hud.stream().anyMatch(h->h.id.equals("about-body")&&h.visible));press(r,"about-credits");assertTrue(r.scene.hud.stream().anyMatch(h->h.id.equals("credits-body")&&h.visible));press(r,"credits-back");press(r,"about-back");
        press(r,"start");press(r,"difficulty-normal");press(r,"levels-level1");assertEquals(18,r.scene.camera.width);assertEquals(0,r.scene.musicVolume);assertFalse(r.scene.hud.stream().filter(h->h.id.equals("jump")).findFirst().orElseThrow().visible);assertTrue(r.scene.hud.stream().filter(h->h.id.equals("scanlines")).findFirst().orElseThrow().visible);
    }}
    @Test void playerLandsRunsJumpsCollectsAndRestartRestoresCoin(){try(var r=start("normal-1")){
        var p=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();
        for(int i=0;i<20;i++)r.update(.05,1,0);assertTrue(p.x>2);assertEquals(7-1.0/6,p.y,2e-5);assertTrue(r.blocked(p,0,-.01));
        r.setAction("space",true);r.update(.05,0,0);assertTrue(p.y>7);r.setAction("space",false);
        var coin=r.scene.objects.stream().filter(e->e.properties.getOrDefault("kind","").equals("coin-silver")).findFirst().orElseThrow();p.x=coin.x-p.colliderOffsetX;p.y=coin.y;r.update(0,0,0);assertFalse(coin.visible);assertTrue(r.scene.hud.stream().anyMatch(h->h.id.equals("score")&&h.text.contains("000050")));
        r.setAction("retry",true);r.update(.01,0,0);assertTrue(r.scene.objects.stream().filter(e->e.id.equals(coin.id)).findFirst().orElseThrow().visible);assertEquals("0",r.state.get("score"));
    }}
    @Test void springRopeAndHoleCheckpointWork(){try(var r=start("normal-1")){
        var p=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();
        var spring=r.scene.objects.stream().filter(e->e.properties.getOrDefault("kind","").equals("spring")).findFirst().orElseThrow();p.x=spring.x-p.colliderOffsetX;p.y=spring.y+.1;r.update(.01,0,0);double y=p.y;r.update(.05,0,0);assertTrue(p.y>y+.2,"Spring failed to launch");
    }
    try(var r=start("super-1")){
        var p=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();
        var rope=r.scene.objects.stream().filter(e->e.properties.getOrDefault("kind","").equals("rope")).findFirst().orElseThrow();p.x=rope.x-p.colliderOffsetX;p.y=rope.y-1;r.update(.01,0,0);assertEquals("rope",p.animation);double x=p.x;r.update(.05,0,0);assertTrue(p.x>x+.4);
    }
    try(var r=start("normal-1")){
        var p=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();var hole=r.scene.objects.stream().filter(e->e.properties.getOrDefault("kind","").equals("hole")).findFirst().orElseThrow();p.x=hole.x-.5;p.y=7;r.update(0,0,0);p.x=hole.x+.5;p.y=3;r.update(.01,0,0);assertTrue(p.x<hole.x);assertTrue(p.y>=6.8);
    }}
    @Test void pauseFreezesClockTimeoutAndCompletionProgress(){try(var r=start("normal-1")){
        r.setAction("pause",true);r.update(.05,0,0);String time=r.scene.hud.stream().filter(h->h.id.equals("time")).findFirst().orElseThrow().text;for(int i=0;i<30;i++)r.update(.05,0,0);assertEquals(time,r.scene.hud.stream().filter(h->h.id.equals("time")).findFirst().orElseThrow().text);
        r.setAction("pause",false);r.setAction("resume",true);r.update(.05,0,0);assertFalse(r.paused);
        for(int i=0;i<1802;i++)r.update(.05,0,0);assertTrue(r.scene.hud.stream().anyMatch(h->h.id.equals("notice")&&h.visible&&h.text.contains("TEMPO ESGOTADO")));
    }
    try(var r=start("normal-5")){
        var p=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();p.x=r.scene.worldWidth-5;p.y=7;r.update(0,0,0);for(int i=0;i<85;i++)r.update(.05,0,0);assertEquals("ending",r.scene.id);assertEquals("900",r.state.get("score"));
    }}
}
