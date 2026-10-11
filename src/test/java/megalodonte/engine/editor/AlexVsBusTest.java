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

    @Test void waterOnlyHurtsInsideItsColliderAndCanBeDisabled(){
        for(boolean enabled:new boolean[]{true,false})try(var r=start("normal-3")){
            var player=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();
            var body=r.scene.objects.stream().filter(e->e.name.equals("gush-body")).findFirst().orElseThrow();
            var bus=r.scene.objects.stream().filter(e->e.id.equals("bus")).findFirst().orElseThrow();
            r.scene.objects.removeIf(e->e!=player&&e!=body&&e!=bus);
            assertTrue(body.solid);assertTrue(body.trigger);body.solid=enabled;
            player.x=body.x+.8-player.colliderOffsetX;player.y=body.y;
            r.update(0,0,0);assertNotEquals("hit",player.animation,"Transparent side of water must not hurt");
            player.x=body.x+body.colliderOffsetX-player.colliderOffsetX;
            r.update(0,0,0);assertEquals(enabled?"hit":"jump",player.animation);
        }
    }
    @Test void turningAwayFromCrateDoesNotTrapPlayer(){try(var r=start("normal-1")){
        var p=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();
        var box=r.scene.objects.stream().filter(e->e.name.equals("crate")).findFirst().orElseThrow();
        var b=megalodonte.engine.Runtime.collisionBounds(r.scene,box);
        // Approach the crate from its right, facing left, then reverse direction.
        p.flipX=true;p.y=b.y();var area=megalodonte.engine.Runtime.collisionBounds(r.scene,p);
        p.x=b.x()+b.width()-(area.x()-p.x)+.00001;
        r.update(.01,-1,0);double x=p.x;
        for(int i=0;i<25;i++)r.update(.05,1,0);
        assertTrue(p.x>x+.5,"Player trapped after reversing at crate edge");
    }}
    @Test void gravityKeepsWorkingWhilePressingAgainstCrateSide(){
        for(int side:new int[]{-1,1})try(var r=start("normal-1")){
            var p=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();
            var box=r.scene.objects.stream().filter(e->e.name.equals("crate")).findFirst().orElseThrow();
            r.scene.objects.removeIf(e->e!=p&&e!=box&&!e.id.equals("bus"));
            var bounds=megalodonte.engine.Runtime.collisionBounds(r.scene,box);
            p.y=bounds.y()+.2;p.flipX=side>0;
            p.x=side>0?bounds.x()+bounds.width()-p.colliderOffsetX+.00001:bounds.x()-p.colliderOffsetX-p.colliderWidth-.00001;
            for(int i=0;i<30;i++)r.update(.05,-side,0);
            assertTrue(p.y<bounds.y()-1,"Lateral contact must not cancel gravity, side="+side);
        }
    }
    @Test void ordinaryJumpKeepsFloorFramed(){try(var r=start("normal-1")){
        var player=Hierarchy.find(r.scene,"alex");
        for(int i=0;i<40;i++)r.update(.025,0,0);
        double groundCamera=r.scene.camera.y,groundY=player.y,maxY=player.y;
        r.setAction("space",true);r.update(.025,0,0);r.setAction("space",false);
        for(int i=0;i<60;i++){
            r.update(.025,0,0);maxY=Math.max(maxY,player.y);
            assertTrue(r.scene.camera.y<=groundCamera+.05,"Jump must not lift camera away from floor");
            assertTrue(CameraView.of(r.scene.camera,1280,720).bottom()<7,"Street remains visible");
        }
        assertTrue(maxY>groundY+1,"Test must perform a real jump");
    }}
    @Test void cameraFollowsPlayerIntoUndergroundPassageAndBack(){try(var r=start("normal-1")){
        var player=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();
        var box=r.scene.objects.stream().filter(e->e.name.equals("pushable")).findFirst().orElseThrow();
        double surfaceCamera=r.scene.camera.y;player.x=box.x-player.colliderOffsetX-player.colliderWidth-.05;player.y=7-player.colliderOffsetY;
        for(int i=0;i<120&&player.y>3.2;i++)r.update(.05,1,0);
        assertFalse(box.visible,"Pushable crate must clear the passage");assertTrue(player.y<3.2,"Player must enter the underground passage through movement");
        for(int i=0;i<40;i++)r.update(.05,0,0);
        assertTrue(r.scene.camera.y<surfaceCamera-3,"Vertical camera must descend into passage");
        var body=megalodonte.engine.Runtime.collisionBounds(r.scene,player);var view=CameraView.of(r.scene.camera,1280,720);
        assertTrue(view.bottom()<=body.y());assertTrue(view.bottom()+view.height()>=body.y()+body.height());
        double undergroundCamera=r.scene.camera.y;player.y=7-player.colliderOffsetY;
        for(int i=0;i<40;i++)r.update(.05,0,0);
        assertTrue(r.scene.camera.y>undergroundCamera+1,"Camera must follow return to surface");
    }}
    @Test void jsonExportImportRoundTripAndRejectInvalidProgress(){
        var source=Codec.copy(game);source.entrySceneId="menu-settings";var exported=new String[1];var incoming=new String[1];var saved=new HashMap<String,String>();
        var events=new megalodonte.engine.Runtime.Events(){
            public void exportSave(String json,java.util.function.Consumer<String> done){exported[0]=json;done.accept("Dados exportados.");}
            public void importSave(java.util.function.BiConsumer<String,String> done){done.accept(incoming[0],null);}
            public void saveState(Map<String,String> values){saved.clear();saved.putAll(values);}
        };
        try(var r=new megalodonte.engine.Runtime(source,megalodonte.engine.Runtime.reflection(compiler.loader),events)){
            r.state.put("progressLevel","3");r.state.put("audio","false");r.state.put("score","900");press(r,"settings-export");
            assertNotNull(exported[0]);var values=SaveData.decode(game.applicationId,exported[0]);assertEquals("3",values.get("progressLevel"));assertFalse(values.containsKey("score"));
            r.state.put("progressLevel","1");incoming[0]=exported[0];press(r,"settings-import");assertEquals("3",r.state.get("progressLevel"));assertEquals("3",saved.get("progressLevel"));assertEquals("false",r.state.get("audio"));
            incoming[0]=SaveData.encode(game.applicationId,Map.of("progressLevel","99"));press(r,"settings-import");assertEquals("3",r.state.get("progressLevel"));assertEquals("3",saved.get("progressLevel"));
            assertTrue(r.scene.hud.stream().anyMatch(h->h.id.equals("settings-data-status")&&h.text.startsWith("Falha")));
        }
    }
    @Test void controllerNavigatesMenusJumpsAndPauses(){try(var r=start("menu")){
        var pad=new GamepadInput();
        for(int i=0;i<3;i++){pad.button("a",true);pad.apply(r);r.update(.01,0,0);pad.button("a",false);pad.apply(r);r.update(.01,0,0);}
        assertEquals("normal-1",r.scene.id);var player=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();double y=player.y;
        pad.button("a",true);pad.apply(r);r.update(.05,0,0);assertTrue(player.y>y);pad.button("a",false);pad.apply(r);
        pad.button("start",true);pad.apply(r);r.update(.01,0,0);assertTrue(r.paused);pad.button("start",false);pad.apply(r);r.update(0,0,0);
        pad.button("start",true);pad.apply(r);r.update(.01,0,0);assertFalse(r.paused);
    }}
    @Test void editorDefaultAndScriptUseTheSameUnlockProperty(){var source=Codec.copy(game);source.entrySceneId="menu-levels";var scene=source.scenes.stream().filter(s->s.id.equals(source.entrySceneId)).findFirst().orElseThrow();var h=scene.hud.stream().filter(x->x.id.equals("levels-level3")).findFirst().orElseThrow();assertFalse(h.booleanProperty("liberado",true));assertTrue(HudPresentation.asset(h).endsWith("-locked.png"));h.setBooleanProperty("liberado",true);assertFalse(HudPresentation.asset(h).endsWith("-locked.png"));try(var r=new megalodonte.engine.Runtime(source,megalodonte.engine.Runtime.reflection(compiler.loader),new megalodonte.engine.Runtime.Events(){})){press(r,"levels-level3");assertEquals("normal-3",r.scene.id);}}
    @Test void lockedChoicesCannotBeOpenedByButtonsOrKeyboard(){try(var r=start("menu")){press(r,"start");press(r,"difficulty-hard");assertEquals("menu-difficulty",r.scene.id);press(r,"difficulty-normal");assertTrue(r.scene.hud.stream().anyMatch(h->h.id.equals("levels-level2")&&HudPresentation.asset(h).endsWith("-locked.png")));press(r,"levels-level2");press(r,"digit2");assertEquals("menu-levels",r.scene.id);press(r,"levels-level1");assertEquals("normal-1",r.scene.id);}}
    @Test void progressIsSavedAtGoalAndRestoredBeforeMenuStarts(){var saved=new HashMap<String,String>();var source=Codec.copy(game);source.entrySceneId="normal-1";var events=new megalodonte.engine.Runtime.Events(){public Map<String,String> loadState(){return Map.copyOf(saved);}public void saveState(Map<String,String> s){saved.clear();saved.putAll(s);}};
        try(var r=new megalodonte.engine.Runtime(source,megalodonte.engine.Runtime.reflection(compiler.loader),events)){var p=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();p.x=r.scene.worldWidth-5;p.y=7;r.update(0,0,0);assertEquals("2",saved.get("progressLevel"));assertFalse(saved.containsKey("score"));}
        source.entrySceneId="menu-levels";try(var r=new megalodonte.engine.Runtime(source,megalodonte.engine.Runtime.reflection(compiler.loader),events)){assertEquals("2",r.state.get("progressLevel"));press(r,"levels-level2");assertEquals("normal-2",r.scene.id);}
    }
    @Test void upDoesNotJumpAndSpaceLandsThenKeepsMoving(){try(var r=start("normal-1")){var p=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();double y=p.y;r.setAction("up",true);r.update(.05,0,1);assertEquals(y,p.y,2e-5);r.setAction("up",false);r.setAction("space",true);r.update(.05,0,0);assertTrue(p.y>y);r.setAction("space",false);for(int i=0;i<40;i++)r.update(.05,0,0);assertEquals(y,p.y,2e-5);double x=p.x;for(int i=0;i<20;i++)r.update(.05,1,0);assertTrue(p.x>x+1);}}
    @Test void everyThousandCoinPointsAddsFifteenSecondsOnce(){try(var r=start("normal-1")){var p=r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();var coins=r.scene.objects.stream().filter(e->e.properties.getOrDefault("kind","").startsWith("coin")).toList();int score=0;for(var coin:coins){p.x=coin.x-p.colliderOffsetX;p.y=coin.y;r.update(0,0,0);score+=coin.properties.get("kind").equals("coin-gold")?100:50;}int expected=90+15*(score/1000);assertTrue(score>=1000);assertTrue(r.scene.hud.stream().anyMatch(h->h.id.equals("time")&&h.text.equals("TIME: "+expected)));r.update(0,0,0);assertTrue(r.scene.hud.stream().anyMatch(h->h.id.equals("time")&&h.text.equals("TIME: "+expected)));var bonus=r.scene.hud.stream().filter(h->h.id.equals("time-bonus")).findFirst().orElseThrow();assertTrue(bonus.visible);assertEquals("+ 15 seconds",bonus.text);assertTrue(bonus.offsetY+bonus.height<=r.scene.camera.viewportTop);for(int i=0;i<32;i++)r.update(.05,0,0);assertFalse(bonus.visible);}}
    @Test void allThirteenLevelsAndEveryAssetArePortable()throws Exception{
        asset(game.iconAssetPath);assertEquals("assets/icon.png",game.iconAssetPath);assertNotNull(javax.imageio.ImageIO.read(project.resolve(game.iconAssetPath).toFile()));assertEquals(25,game.scenes.size());assertEquals(13,game.scenes.stream().filter(s->s.id.matches("(normal|hard|super)-[1-5]")).count());
        for(var scene:game.scenes){asset(scene.musicAssetPath);for(var e:scene.objects){asset(e.assetPath);for(var a:e.animations.values())for(var frame:a.frames)asset(frame);}for(var h:scene.hud){asset(h.assetPath);asset(h.condition.trueAssetPath);asset(h.condition.falseAssetPath);}for(var map:scene.tileMaps)for(var tile:map.tiles.values())asset(tile.assetPath);}
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
    @Test void menuSelectsDifficultyAndLevel(){try(var r=start("menu")){r.state.put("progressDifficulty","2");r.state.put("progressLevel","3");press(r,"start");press(r,"difficulty-super");press(r,"levels-level3");assertEquals("super-3",r.scene.id);}}
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
