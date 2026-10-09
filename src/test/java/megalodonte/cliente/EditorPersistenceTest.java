package megalodonte.cliente;

import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import megalodonte.engine.*;
import megalodonte.script.*;
import megalodonte.base.theme.ThemeManager;
import megalodonte.theme.DefaultTheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class EditorPersistenceTest {
    @TempDir Path temp;
    @Test void saveAndRestartRestoresScenePropertiesHudAndPath() throws Exception {
        Path preference=temp.resolve("prefs/last.txt"), project=temp.resolve("game/game.json");
        Files.createDirectories(project.getParent());Files.createDirectories(preference.getParent());
        var game=Game.demo();var scene=new Game.Scene();scene.id="edited";scene.name="Cena editada";
        var entity=new Game.Entity();entity.rotation=37;entity.name="Teste persistido";scene.objects.add(entity);
        var hud=new Game.Hud();hud.text="Moedas: 25";scene.hud.add(hud);
        game.scenes.add(scene);game.lastEditedSceneId=scene.id;
        Files.writeString(project,Codec.GSON.toJson(game));Files.writeString(preference,project.toString());
        System.setProperty("engine.preferencesFile",preference.toString());
        var finished=new CompletableFuture<Void>();
        Platform.startup(()->{
            Platform.setImplicitExit(false);ThemeManager.setTheme(new DefaultTheme());
            try {
                try(var app=open()){
                    assertTrue(app.current().states().string("project").get().contains("Cena editada"));
                    assertEquals("Projeto: "+project,app.current().states().get("projectPath"));
                    selectFirst(app);
                    assertEquals("37.0",app.current().states().get("rotation"));
                    app.current().states().set("rotation","inválida");app.current().invoke("saveProject");
                    assertEquals(37,Codec.read(Files.readString(project)).scenes.get(1).objects.get(0).rotation);
                    app.current().states().set("rotation","75");app.current().invoke("saveProject");
                    assertEquals(75,Codec.read(Files.readString(project)).scenes.get(1).objects.get(0).rotation);
                }
                try(var restored=open()){
                    selectFirst(restored);
                    assertEquals("75.0",restored.current().states().get("rotation"));
                    assertEquals("edited",Codec.read(Files.readString(project)).lastEditedSceneId);
                    assertEquals("Moedas: 25",Codec.read(Files.readString(project)).scenes.get(1).hud.get(0).text);
                    assertEquals(project.toString(),Files.readString(preference));
                }
                finished.complete(null);
            }catch(Throwable e){finished.completeExceptionally(e);}
        });
        try{finished.get(20,TimeUnit.SECONDS);}finally{Platform.exit();}
    }
    private ScriptApplication open(){
        var app=new ScriptApplication(AppConfig.load(getClass().getClassLoader()),getClass().getClassLoader());
        var root=(Parent)app.render().getJavaFxNode();new Scene(root,1280,850);
        ((Region)root).resize(1280,850);root.applyCss();root.layout();app.onMount();return app;
    }
    private void selectFirst(ScriptApplication app){
        var host=(Pane)app.current().component("tree").getJavaFxNode();
        var tree=(VBox)((ScrollPane)host.getChildren().getFirst()).getContent();
        ((Button)tree.getChildren().getFirst()).fire();
    }
}
