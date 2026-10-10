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
                assertTrue(app.current().states().string("project").get().contains("Menu • Título"));
                var canvas=(Canvas)((Pane)app.current().component("viewport").getJavaFxNode()).getChildren().getFirst();
                assertTrue(canvas.getHeight()>300);AlexVsBusVisualTest.snapshot(canvas,"alex-vs-bus-editor");
                var sceneHost=(Pane)app.current().component("sceneList").getJavaFxNode();
                var selector=(javafx.scene.control.ListView<?>)sceneHost.getChildren().getFirst();assertEquals(25,selector.getItems().size());
                assertTrue(sceneHost.localToScene(0,0).getX()>700,"Scene list should sit at the upper right");
                selector.getSelectionModel().select(9);assertTrue(app.current().states().string("project").get().contains("Menu • Créditos"));AlexVsBusVisualTest.snapshot(canvas,"alex-vs-bus-editor-credits");
                selector.getSelectionModel().select(3);assertTrue(app.current().states().string("project").get().contains("Menu • Configurações"));
                app.current().invoke("nextScene");assertEquals(4,selector.getSelectionModel().getSelectedIndex());
                selector.getSelectionModel().select(0);assertTrue(app.current().states().string("project").get().contains("Menu • Título"));
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
