package megalodonte.cliente;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;
import megalodonte.script.*;
import megalodonte.base.theme.ThemeManager;
import megalodonte.theme.DefaultTheme;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class EditorWindowTest {
    @Test void editorRendersAfterWindowMountAndPulses() throws Exception {
        var done = new CompletableFuture<Void>();
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            ThemeManager.setTheme(new DefaultTheme());
            var app = new ScriptApplication(AppConfig.load(getClass().getClassLoader()), getClass().getClassLoader());
            var stage = new Stage();
            megalodonte.application.Bootstrap.appName = "Megalodonte Engine";
            megalodonte.application.Bootstrap.handler = context -> {
                context.useView(app, 1280, 850);
                context.javafxStage().setWidth(1280);
                context.javafxStage().setHeight(700);
                context.javafxStage().setMinWidth(1000);
                context.javafxStage().setMinHeight(700);
            };
            megalodonte.application.Bootstrap.dispatch(stage, new String[0]);
            var delay = new PauseTransition(Duration.seconds(1));
            delay.setOnFinished(event -> {
                try {
                    var host = (javafx.scene.layout.Pane) app.current().component("viewport").getJavaFxNode();
                    var canvas = (Canvas) host.getChildren().getFirst();
                    assertTrue(canvas.getWidth() > 300, "Viewport width: "+canvas.getWidth());
                    assertTrue(canvas.getHeight() > 300, "Viewport height: "+canvas.getHeight());
                    var toolbarButton=app.current().component("toolbar0Button0").getJavaFxNode();
                    var toolbarBounds=toolbarButton.localToScene(toolbarButton.getBoundsInLocal());
                    assertTrue(toolbarBounds.getMinY()>=0,"Toolbar clipped above scene: "+toolbarBounds);
                    assertTrue(toolbarBounds.getMaxY()<=stage.getScene().getHeight());
                    var session=app.current();
                    session.invoke("addObject");
                    session.invoke("duplicateObject");
                    assertTrue(session.states().string("name").get().contains("cópia"));
                    session.invoke("chooseParent");
                    var treeHost=(javafx.scene.layout.Pane)session.component("tree").getJavaFxNode();
                    var treeScroll=(javafx.scene.control.ScrollPane)treeHost.getChildren().getFirst();
                    var tree=(javafx.scene.layout.VBox)treeScroll.getContent();
                    ((javafx.scene.control.Button)tree.getChildren().getFirst()).fire();
                    assertEquals("Pai: Jogador",session.states().get("parentName"));
                    session.invoke("applyObject");
                    session.invoke("detachParent");
                    assertEquals("Pai: nenhum",session.states().get("parentName"));
                    session.invoke("addHudText");
                    session.states().set("hudText","Moedas: 42");
                    session.states().set("hudX","36");
                    session.invoke("applyHud");
                    assertEquals("Moedas: 42",session.states().get("hudText"));
                    session.invoke("cycleHudHorizontal");
                    session.invoke("cycleHudVertical");
                    assertEquals("center / center",session.states().get("hudAnchors"));
                    session.invoke("togglePlay");session.invoke("togglePlay");
                    var image = new javafx.scene.robot.Robot().getScreenCapture(null, stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
                    long visiblePixels=0;
                    for(int y=40;y<(int)image.getHeight();y++) for(int x=0;x<(int)image.getWidth();x++) {
                        var color=image.getPixelReader().getColor(x,y);
                        if(color.getRed()+color.getGreen()+color.getBlue()>0.2)visiblePixels++;
                    }
                    assertTrue(visiblePixels>100000,"Native window is black");
                    var png = new BufferedImage((int)image.getWidth(), (int)image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    for (int y=0;y<png.getHeight();y++) for(int x=0;x<png.getWidth();x++) png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
                    Files.createDirectories(Path.of("build/preview"));
                    ImageIO.write(png,"png",Path.of("build/preview/editor-window.png").toFile());
                    done.complete(null);
                } catch(Throwable error) { done.completeExceptionally(error); }
                finally { app.close(); stage.close(); }
            });
            delay.play();
        });
        try { done.get(20,TimeUnit.SECONDS); } finally { Platform.exit(); }
    }
}
