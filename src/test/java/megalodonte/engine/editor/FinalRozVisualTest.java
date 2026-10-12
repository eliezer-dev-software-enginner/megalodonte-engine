package megalodonte.engine.editor;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import megalodonte.engine.*;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
class FinalRozVisualTest {
 @Test void importedLevelsRenderWithTheirScripts()throws Exception{
  Path project=Path.of("../../games/final-roz-game-megalodonte").toAbsolutePath().normalize();Assumptions.assumeTrue(Files.exists(project.resolve("game.json")));
  try(var compiler=ScriptCompiler.compile(project)){
   var game=Codec.read(Files.readString(project.resolve("game.json")));var finished=new CompletableFuture<Void>();
   Platform.startup(()->{Platform.setImplicitExit(false);Stage stage=new Stage();try{
    var canvas=new Canvas(1280,720);stage.setScene(new Scene(new StackPane(canvas)));stage.show();
    for(String id:new String[]{"menu","story-1","intro-1","options","levels","level-1","level-4","level-15","level-16","level-17"}){
     var copy=Codec.copy(game);copy.entrySceneId=id;var images=new HashMap<String,Image>();java.util.function.Function<String,Image> image=asset->asset.isBlank()?null:images.computeIfAbsent(asset,a->new Image(project.resolve(a).toUri().toString()));
     try(var runtime=new megalodonte.engine.Runtime(copy,megalodonte.engine.Runtime.reflection(compiler.loader),new megalodonte.engine.Runtime.Events(){})){
      for(var hud:runtime.scene.hud)if(hud.type.equals("button")&&!hud.fontFamily.isBlank()){var text=new javafx.scene.text.Text(hud.text);text.setFont(javafx.scene.text.Font.font(hud.fontFamily,hud.fontSize));org.junit.jupiter.api.Assertions.assertTrue(text.getLayoutBounds().getWidth()<=hud.width,"Button text is clipped: "+id+"/"+hud.id);}
      for(int i=0;i<12;i++)runtime.update(.05,0,0);GamePainter.draw(canvas,runtime,image);AlexVsBusVisualTest.snapshot(canvas,"finalroz-"+id);
     }
    }finished.complete(null);
   }catch(Throwable error){finished.completeExceptionally(error);}finally{stage.close();}});
   try{finished.get(60,TimeUnit.SECONDS);}finally{Platform.exit();}
  }
 }
}
