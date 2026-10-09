package megalodonte.engine.editor;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.*;
import javafx.util.Duration;
import megalodonte.engine.Codec;
import megalodonte.script.*;
import megalodonte.base.theme.ThemeManager;
import megalodonte.theme.DefaultTheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class FeaturePreviewTest {
    @TempDir Path temp;
    @Test void mapCameraAndScriptEditorRenderAndPersist() throws Exception {
        Path project=temp.resolve("example");ExampleProject.main(new String[]{project.toString()});
        Path preference=temp.resolve("prefs/last.txt");new RecentProject(preference).rememberSaved(project.resolve("game.json"));
        System.setProperty("engine.preferencesFile",preference.toString());
        var done=new CompletableFuture<Void>();
        Platform.startup(()->{
            Platform.setImplicitExit(false);ThemeManager.setTheme(new DefaultTheme());
            var app=new ScriptApplication(AppConfig.load(getClass().getClassLoader()),getClass().getClassLoader());
            Stage stage=new Stage();megalodonte.application.Bootstrap.handler=context->context.useView(app,1280,850);
            megalodonte.application.Bootstrap.dispatch(stage,new String[0]);
            app.current().invoke("toggleSceneSettings");
            var delay=new PauseTransition(Duration.seconds(1));delay.setOnFinished(e->{try{
                var canvas=(Canvas)((Pane)app.current().component("viewport").getJavaFxNode()).getChildren().getFirst();
                assertTrue(canvas.getHeight()>300);assertEquals("1.0",app.current().states().get("cameraZoom"));
                app.current().states().set("cameraZoom","1.5");app.current().invoke("saveProject");
                var restored=Codec.read(Files.readString(project.resolve("game.json")));var scene=restored.scenes.getFirst();
                assertEquals(1.5,scene.camera.zoom);assertEquals("follow",scene.camera.mode);assertFalse(scene.musicAssetPath.isBlank());assertEquals(1,scene.tileMaps.size());assertEquals("game.scripts.Player",scene.objects.getFirst().scriptClassName);
                var tree=(javafx.scene.layout.VBox)((ScrollPane)((Pane)app.current().component("tree").getJavaFxNode()).getChildren().getFirst()).getContent();
                ((Button)tree.getChildren().stream().filter(n->n instanceof Button b&&b.getText().startsWith("Mapa •")).findFirst().orElseThrow()).fire();
                app.current().states().set("mapX","3");app.current().invoke("saveProject");assertEquals(3,Codec.read(Files.readString(project.resolve("game.json"))).scenes.getFirst().tileMaps.getFirst().x);
                snapshot(stage.getScene().getRoot(),"features");
                ScriptWindow.open(stage,project,"game.scripts.Player",name->{});
                Stage scripts=(Stage)Window.getWindows().stream().filter(w->w instanceof Stage s&&s.getTitle()!=null&&s.getTitle().startsWith("Scripts Java")).findFirst().orElseThrow();
                scripts.getScene().getRoot().applyCss();scripts.getScene().getRoot().layout();
                TextArea code=(TextArea)scripts.getScene().getRoot().lookupAll(".text-area").stream().filter(n->n instanceof TextArea t&&t.getText().contains("public class Player")).findFirst().orElseThrow();
                code.appendText("\n// alteração preservada ao fechar\n");snapshot(scripts.getScene().getRoot(),"scripts");
                scripts.fireEvent(new WindowEvent(scripts,WindowEvent.WINDOW_CLOSE_REQUEST));scripts.close();
                assertTrue(Files.readString(project.resolve("scripts/game/scripts/Player.java")).contains("alteração preservada"));
                done.complete(null);
            }catch(Throwable error){done.completeExceptionally(error);}finally{app.close();stage.close();}});delay.play();
        });
        try{done.get(30,TimeUnit.SECONDS);}finally{Platform.exit();}
    }
    private static void snapshot(Parent root,String name)throws Exception{
        var image=root.snapshot(null,null);var png=new BufferedImage((int)image.getWidth(),(int)image.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++)png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        Files.createDirectories(Path.of("build/preview"));ImageIO.write(png,"png",Path.of("build/preview/"+name+".png").toFile());
    }
}
