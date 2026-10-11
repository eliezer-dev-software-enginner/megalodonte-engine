package megalodonte.engine.editor;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import megalodonte.script.*;
import megalodonte.base.theme.ThemeManager;
import megalodonte.theme.DefaultTheme;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SuppressWarnings("unchecked")
class AlexVsBusEditorTest {
    @TempDir Path temp;
    @Test void gameOpensInEditorAndScriptsStartTitleInPlay()throws Exception{
        Path project=Path.of("../../games/alex-vs-bus").toAbsolutePath().normalize();Assumptions.assumeTrue(Files.exists(project.resolve("game.json")));
        Path preference=temp.resolve("prefs/last.txt");new RecentProject(preference).rememberSaved(project.resolve("game.json"));System.setProperty("engine.preferencesFile",preference.toString());
        var done=new CompletableFuture<Void>();
        Platform.startup(()->{
            Platform.setImplicitExit(false);ThemeManager.setTheme(new DefaultTheme());
            var app=new ScriptApplication(AppConfig.load(getClass().getClassLoader()),getClass().getClassLoader());var stage=new Stage();
            megalodonte.application.Bootstrap.handler=context->context.useView(app,1280,850);megalodonte.application.Bootstrap.dispatch(stage,new String[0]);
            var initial=new PauseTransition(Duration.seconds(1));initial.setOnFinished(e->{try{
                var authored=megalodonte.engine.Codec.read(Files.readString(project.resolve("game.json")));
                String edited=authored.lastEditedSceneId.isBlank()?authored.entrySceneId:authored.lastEditedSceneId;
                assertTrue(app.current().states().string("project").get().contains(authored.scenes.stream().filter(s->s.id.equals(edited)).findFirst().orElseThrow().name));
                var canvas=(Canvas)((Pane)app.current().component("viewport").getJavaFxNode()).getChildren().getFirst();
                FeatureEditorsTest.snapshot(stage.getScene().getRoot(),"editor-release-toolbar");
                app.current().invoke("engineSettings");
                var settingsPane=javafx.stage.Window.getWindows().stream().map(javafx.stage.Window::getScene).filter(java.util.Objects::nonNull).map(javafx.scene.Scene::getRoot).filter(root->root.lookup("#androidSdkPath")!=null).findFirst().orElseThrow();
                assertNotNull(settingsPane.lookup("#engineSettingsStatus"));
                FeatureEditorsTest.snapshot(settingsPane,"engine-settings");
                for(var window:new java.util.ArrayList<>(javafx.stage.Window.getWindows()))if(window!=stage)window.hide();
                assertTrue(canvas.getHeight()>300);AlexVsBusVisualTest.snapshot(canvas,"alex-vs-bus-editor");
                var sceneHost=(Pane)app.current().component("sceneList").getJavaFxNode();
                var selector=(javafx.scene.control.ListView<?>)sceneHost.getChildren().getFirst();assertEquals(25,selector.getItems().size());
                selector.getSelectionModel().select(0);
                assertTrue(sceneHost.localToScene(0,0).getX()>700,"Scene list should sit at the upper right");
                selector.getSelectionModel().select(9);assertTrue(app.current().states().string("project").get().contains("Menu • Créditos"));AlexVsBusVisualTest.snapshot(canvas,"alex-vs-bus-editor-credits");
                selector.getSelectionModel().select(3);assertTrue(app.current().states().string("project").get().contains("Menu • Configurações"));
                app.current().invoke("nextScene");assertEquals(4,selector.getSelectionModel().getSelectedIndex());
                selector.getSelectionModel().select(0);assertTrue(app.current().states().string("project").get().contains("Menu • Título"));
                selector.getSelectionModel().select(11);
                var objectList=(javafx.scene.layout.VBox)((javafx.scene.control.ScrollPane)((Pane)app.current().component("tree").getJavaFxNode()).getChildren().getFirst()).getContent();
                objectList.getChildren().stream().filter(n->n instanceof javafx.scene.control.Button b&&b.getText().equals("player")).map(n->(javafx.scene.control.Button)n).findFirst().orElseThrow().fire();
                // Hidden checkpoints remain discoverable after deselection, without enabling runtime visibility.
                objectList.getChildren().stream().filter(n->n instanceof javafx.scene.control.Button b&&b.getText().equals("Checkpoint")).map(n->(javafx.scene.control.Button)n).findFirst().orElseThrow().fire();
                javafx.event.Event.fireEvent(canvas,new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_PRESSED,1,canvas.getHeight()/2,1,canvas.getHeight()/2,javafx.scene.input.MouseButton.PRIMARY,1,false,false,false,false,true,false,false,false,false,false,null));
                app.current().invoke("toggleColliders");app.current().invoke("toggleColliders");
                var triggerPreview=canvas.snapshot(null,null);long markerPixels=0;
                for(int py=0;py<(int)triggerPreview.getHeight();py++)for(int px=0;px<(int)triggerPreview.getWidth();px++){
                    var color=triggerPreview.getPixelReader().getColor(px,py);
                    if(Math.abs(color.getRed()-166.0/255)<.02&&Math.abs(color.getGreen()-184.0/255)<.02&&Math.abs(color.getBlue()-204.0/255)<.02)markerPixels++;
                }
                assertTrue(markerPixels>10,"Hidden checkpoint outline must remain visible without selection");
                AlexVsBusVisualTest.snapshot(canvas,"editor-hidden-triggers");
                app.current().invoke("toggleVisualDebug");
                assertEquals("Debug visual: nomes visíveis",app.current().states().string("mensagem").get());
                var labeled=canvas.snapshot(null,null);long changedPixels=0;
                for(int py=0;py<(int)labeled.getHeight();py++)for(int px=0;px<(int)labeled.getWidth();px++)
                    if(labeled.getPixelReader().getArgb(px,py)!=triggerPreview.getPixelReader().getArgb(px,py))changedPixels++;
                assertTrue(changedPixels>20,"Debug visual must draw object names");
                app.current().invoke("toggleVisualDebug");
                assertEquals("Debug visual: nomes ocultos",app.current().states().string("mensagem").get());
                var unlabeled=canvas.snapshot(null,null);
                for(int py=0;py<(int)unlabeled.getHeight();py++)for(int px=0;px<(int)unlabeled.getWidth();px++)
                    assertEquals(triggerPreview.getPixelReader().getArgb(px,py),unlabeled.getPixelReader().getArgb(px,py),"Hiding names restores preview");
                objectList.getChildren().stream().filter(n->n instanceof javafx.scene.control.Button b&&b.getText().equals("player")).map(n->(javafx.scene.control.Button)n).findFirst().orElseThrow().fire();
                double originalX=Double.parseDouble(app.current().states().string("x").get());
                double originalY=Double.parseDouble(app.current().states().string("y").get());
                javafx.event.Event.fireEvent(canvas,new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,"","",javafx.scene.input.KeyCode.RIGHT,false,false,false,false));
                assertEquals(originalX+.1,Double.parseDouble(app.current().states().string("x").get()),1e-9);
                javafx.event.Event.fireEvent(objectList,new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,"","",javafx.scene.input.KeyCode.UP,true,false,false,false));
                assertEquals(originalY+1,Double.parseDouble(app.current().states().string("y").get()),1e-9);
                javafx.event.Event.fireEvent(canvas,new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,"","",javafx.scene.input.KeyCode.LEFT,false,true,false,false));
                assertEquals(originalX+.09,Double.parseDouble(app.current().states().string("x").get()),1e-9);
                app.current().invoke("editAnimations");var animationPane=javafx.stage.Window.getWindows().stream().map(javafx.stage.Window::getScene).filter(java.util.Objects::nonNull).map(javafx.scene.Scene::getRoot).filter(root->root.lookup("#animationNames")!=null).findFirst().orElseThrow();
                ((javafx.scene.control.ListView<String>)animationPane.lookup("#animationNames")).getSelectionModel().select("walk");assertEquals("walk",((javafx.scene.control.ListView<?>)animationPane.lookup("#animationNames")).getSelectionModel().getSelectedItem());FeatureEditorsTest.snapshot(animationPane,"alex-animation-editor");
                ((javafx.scene.control.Button)((javafx.scene.control.DialogPane)animationPane).lookupButton(javafx.scene.control.ButtonType.CLOSE)).fire();selector.getSelectionModel().select(0);
                app.current().invoke("togglePlay");var compiled=new PauseTransition(Duration.millis(200));long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);compiled.setOnFinished(event->{
                    if(app.current().states().string("mensagem").get().equals("Compilando scripts…")&&System.nanoTime()<deadline){compiled.playFromStart();return;}
                    var image=canvas.snapshot(null,null);long green=0;for(int y=0;y<(int)image.getHeight();y++)for(int x=0;x<(int)image.getWidth();x++){var color=image.getPixelReader().getColor(x,y);if(color.getGreen()>.2&&color.getRed()<.05&&color.getBlue()<.05)green++;}
                    if(green<=1500&&System.nanoTime()<deadline){compiled.playFromStart();return;}
                    try{
                    assertTrue(green>1500,"Title did not start in editor Play: "+app.current().states().get("mensagem"));AlexVsBusVisualTest.snapshot(canvas,"alex-vs-bus-editor-play");done.complete(null);
                }catch(Throwable error){done.completeExceptionally(error);}finally{app.close();stage.close();}});compiled.play();
            }catch(Throwable error){done.completeExceptionally(error);app.close();stage.close();}});initial.play();
        });
        try{done.get(40,TimeUnit.SECONDS);}finally{Platform.exit();}
    }
}
