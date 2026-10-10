package megalodonte.engine.editor;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.stage.*;
import megalodonte.engine.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
@SuppressWarnings("unchecked")
class FeatureEditorsTest {
    @TempDir Path temp;
    @Test void animationsPropertiesAndTilePaintingAreEditableWithoutJson()throws Exception{
        Files.createDirectories(temp.resolve("assets"));for(var color:List.of(java.awt.Color.RED,java.awt.Color.BLUE)){var image=new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_ARGB);var graphics=image.createGraphics();graphics.setColor(color);graphics.fillRect(0,0,16,16);graphics.dispose();javax.imageio.ImageIO.write(image,"png",temp.resolve("assets/"+(color.equals(java.awt.Color.RED)?"red":"blue")+".png").toFile());}
        var done=new CompletableFuture<Void>();Platform.startup(()->{Platform.setImplicitExit(false);Stage owner=new Stage();owner.setScene(new Scene(new StackPane(),100,100));owner.show();try(var assets=new PendingAssets()){
            var game=Game.demo();var entity=game.scenes.get(0).objects.get(0);var animation=new Game.Animation();animation.frames.addAll(List.of("assets/red.png","assets/blue.png"));entity.animations.put("walk",animation);entity.animation="walk";entity.animationTime=.125;var result=new AtomicReference<Game.Entity>();
            new AnimationEditor(entity,value->{var copy=Codec.copy(game);copy.scenes.get(0).objects.set(0,value);Codec.validate(copy);result.set(value);},assets,temp,owner).show();var pane=pane("#animationNames");
            ((TextField)pane.lookup("#animationFps")).setText("24");var frames=(ListView<?>)pane.lookup("#animationFrames");frames.getSelectionModel().select(1);var preview=(ImageView)pane.lookup("#animationPreview");assertEquals(javafx.scene.paint.Color.BLUE,preview.getImage().getPixelReader().getColor(0,0));
            fire(pane,"Subir");snapshot(pane,"animation-editor");fire(pane,"Aplicar ao objeto");assertNotNull(result.get());assertEquals(24,result.get().animations.get("walk").fps);assertEquals("assets/blue.png",result.get().animations.get("walk").frames.get(0));assertEquals(.125,result.get().animationTime);assertEquals(.125,entity.animationTime,"Preview mutated the document clock");closeDialogs(owner);
            var child=new Game.Entity();child.parentId=entity.id;game.scenes.get(0).objects.add(child);game.scenes.get(0).camera.targetId=entity.id;var advancedGame=new AtomicReference<Game>();
            var advanced=new AtomicReference<Game.Entity>();FeatureForms.entity(owner,entity,value->{var copy=DocumentEdits.entity(game,0,0,value);advancedGame.set(copy);advanced.set(copy.scenes.get(0).objects.get(0));});pane=pane("#colliderWidth");((TextField)pane.lookup("#entityId")).setText("editable-player");((TextField)pane.lookup("#colliderWidth")).setText("0.5");((CheckBox)pane.lookup("#entityFlip")).setSelected(true);fire(pane,"Aplicar");assertNotNull(advanced.get());assertEquals(.5,advanced.get().colliderWidth);assertTrue(advanced.get().flipX);assertEquals("editable-player",advancedGame.get().scenes.get(0).camera.targetId);assertEquals("editable-player",advancedGame.get().scenes.get(0).objects.get(2).parentId);closeDialogs(owner);
            var map=new Game.TileMap();map.columns=3;map.rows=2;map.tileWidth=map.tileHeight=16;map.unitScale=1.0/16;var layer=new Game.TileLayer();layer.name="cidade";layer.data=new long[6];map.layers.add(layer);var tile=new Game.Tile();tile.assetPath="assets/red.png";tile.width=tile.height=16;map.tiles.put(1,tile);game.scenes.get(0).tileMaps.add(map);var painted=new AtomicReference<Game.TileMap>();
            new TileMapEditor(map,value->{var copy=Codec.copy(game);copy.scenes.get(0).tileMaps.set(0,value);Codec.validate(copy);painted.set(value);},assets,temp,owner).show();pane=pane("#tileMapEditor");var tabs=(TabPane)pane.lookup(".tab-pane");tabs.getSelectionModel().select(1);pane.applyCss();((ComboBox<Integer>)pane.lookup("#tileBrush")).setValue(1);var canvas=pane.lookup("#mapPaint");javafx.event.Event.fireEvent(canvas,new MouseEvent(MouseEvent.MOUSE_PRESSED,10,10,10,10,MouseButton.PRIMARY,1,false,false,false,false,true,false,false,false,false,false,new PickResult(canvas,new javafx.geometry.Point3D(10,10,0),1)));snapshot(pane,"tilemap-editor");tabs.getSelectionModel().select(0);((TextField)pane.lookup("#columns")).setText("4");fire(pane,"Aplicar");assertNotNull(painted.get());assertEquals(4,painted.get().columns);assertEquals(8,painted.get().layers.get(0).data.length);assertEquals(1,painted.get().layers.get(0).data[0]);assertEquals(0,map.layers.get(0).data[0],"Draft edited live document before Apply");closeDialogs(owner);AssetBrowser.show(owner,assets,temp);pane=pane("#assetPaths");var library=(ListView<String>)pane.lookup("#assetPaths");assertEquals(2,library.getItems().size());library.getSelectionModel().select("assets/red.png");assertEquals(javafx.scene.paint.Color.RED,((ImageView)pane.lookup("#assetPreview")).getImage().getPixelReader().getColor(0,0));closeDialogs(owner);done.complete(null);
        }catch(Throwable e){done.completeExceptionally(e);}finally{owner.close();}});try{done.get(25,TimeUnit.SECONDS);}finally{Platform.exit();}
    }
    static void snapshot(javafx.scene.Node node,String name)throws Exception{
        var image=node.snapshot(null,null);var png=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++)png.setRGB(x,y,image.getPixelReader().getArgb(x,y));Files.createDirectories(Path.of("build/preview"));javax.imageio.ImageIO.write(png,"png",Path.of("build/preview/"+name+".png").toFile());
    }
    private static javafx.scene.Parent pane(String selector){return Window.getWindows().stream().map(Window::getScene).filter(Objects::nonNull).map(Scene::getRoot).filter(root->root.lookup(selector)!=null).findFirst().orElseThrow();}
    private static void fire(javafx.scene.Parent pane,String text){var button=pane.lookupAll(".button").stream().filter(n->n instanceof Button b&&b.getText().equals(text)).map(n->(Button)n).findFirst().orElseThrow();button.fire();}
    private static void closeDialogs(Stage owner){for(var window:new ArrayList<>(Window.getWindows()))if(window!=owner)window.hide();}
}
