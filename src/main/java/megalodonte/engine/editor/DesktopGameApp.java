package megalodonte.engine.editor;

import javafx.application.Application;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import megalodonte.engine.*;
import java.nio.file.*;
import java.util.*;

/** Standalone host: executes compiled portable scripts, never opens the editor. */
public final class DesktopGameApp extends Application {
    private megalodonte.engine.Runtime runtime;
    private final ProjectMusic audio=new ProjectMusic();
    private AnimationTimer timer;
    private final Set<KeyCode> keys=new HashSet<>();
    private final Set<String> mouseActions=new HashSet<>();
    private final Map<String,Image> images=new HashMap<>();
    private Path project;
    private boolean failed;
    @Override public void start(Stage stage)throws Exception{
        String configured=System.getProperty("engine.game");
        if(configured!=null)project=Path.of(configured).toAbsolutePath();
        else if(!getParameters().getRaw().isEmpty())project=Path.of(getParameters().getRaw().getFirst()).toAbsolutePath();
        else project=Path.of(DesktopLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().resolve("game");
        var game=Codec.read(Files.readString(project.resolve("game.json")));
        runtime=new megalodonte.engine.Runtime(game,megalodonte.engine.Runtime.reflection(getClass().getClassLoader()),new megalodonte.engine.Runtime.Events(){
            public void playSound(String asset){audio.sound(project.resolve(asset));}
            public void log(String message){System.out.println(message);}
            public void pauseAudio(boolean value){audio.pause(value);}
            public void quit(){javafx.application.Platform.exit();}
            public void muteAudio(boolean value){audio.mute(value);}
        });
        var canvas=new Canvas();var root=new StackPane(canvas);canvas.widthProperty().bind(root.widthProperty());canvas.heightProperty().bind(root.heightProperty());
        var scene=new Scene(root,1100,720);stage.setScene(scene);stage.setTitle(game.name);stage.setMinWidth(640);stage.setMinHeight(420);
        scene.setOnKeyPressed(e->{keys.add(e.getCode());runtime.setAction(e.getCode().name().toLowerCase(Locale.ROOT),true);if(e.getCode()==KeyCode.F11)stage.setFullScreen(!stage.isFullScreen());e.consume();});
        scene.setOnKeyReleased(e->{keys.remove(e.getCode());runtime.setAction(e.getCode().name().toLowerCase(Locale.ROOT),false);e.consume();});
        canvas.setOnMousePressed(e->{if(e.getButton()!=javafx.scene.input.MouseButton.PRIMARY)return;for(int i=runtime.scene.hud.size()-1;i>=0;i--){var hud=runtime.scene.hud.get(i);if(hud.visible&&hud.type.equals("button")&&HudLayout.resolve(hud,canvas.getWidth(),canvas.getHeight()).contains(e.getX(),e.getY())){String action=hud.action.isBlank()?hud.id:hud.action;runtime.setAction(action,true);mouseActions.add(action);break;}}});
        canvas.setOnMouseReleased(e->releaseMouse());
        stage.focusedProperty().addListener((o,a,focused)->{if(!focused){for(var key:keys)runtime.setAction(key.name().toLowerCase(Locale.ROOT),false);keys.clear();releaseMouse();audio.pause(true);}else audio.pause(runtime.paused);});
        timer=new AnimationTimer(){long previous;
            @Override public void handle(long now){if(failed)return;try{
                double dt=previous==0?0:(now-previous)/1e9;previous=now;
                if(stage.isFocused())runtime.update(dt,(keys.contains(KeyCode.D)||keys.contains(KeyCode.RIGHT)?1:0)-(keys.contains(KeyCode.A)||keys.contains(KeyCode.LEFT)?1:0),0);
                audio.update(runtime.scene,asset->project.resolve(asset),message->System.err.println(message));
                GamePainter.draw(canvas,runtime,DesktopGameApp.this::image);
            }catch(Throwable error){failed=true;audio.close();error.printStackTrace();var alert=new Alert(Alert.AlertType.ERROR,"Erro no jogo: "+error.getMessage());alert.initOwner(stage);alert.show();}}
        };
        stage.show();timer.start();
    }
    private Image image(String asset){if(asset==null||asset.isBlank())return null;return images.computeIfAbsent(asset,key->new Image(project.resolve(key).toUri().toString()));}
    private void releaseMouse(){for(var action:mouseActions)runtime.setAction(action,false);mouseActions.clear();}
    @Override public void stop(){if(timer!=null)timer.stop();if(runtime!=null)runtime.close();audio.close();images.clear();}
}
