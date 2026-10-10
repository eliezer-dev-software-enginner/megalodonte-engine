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
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
class AlexVsBusVisualTest {
    @Test void actualAssetsAndScriptsRenderInDesktopHost()throws Exception{
        Path project=Path.of("../../games/alex-vs-bus").toAbsolutePath().normalize();Assumptions.assumeTrue(Files.exists(project.resolve("game.json")));
        try(var compiler=ScriptCompiler.compile(project)){
            var game=Codec.read(Files.readString(project.resolve("game.json")));var finished=new CompletableFuture<Void>();
            Platform.startup(()->{Platform.setImplicitExit(false);Stage stage=new Stage();
                try(var runtime=new megalodonte.engine.Runtime(game,megalodonte.engine.Runtime.reflection(compiler.loader),new megalodonte.engine.Runtime.Events(){})){
                    var canvas=new Canvas(1024,690);stage.setScene(new Scene(new StackPane(canvas)));stage.setTitle("Alex vs Bus • teste de renderização");stage.show();
                    var images=new HashMap<String,Image>();java.util.function.Function<String,Image> image=asset->asset.isBlank()?null:images.computeIfAbsent(asset,a->new Image(project.resolve(a).toUri().toString()));
                    GamePainter.draw(canvas,runtime,image);snapshot(canvas,"alex-vs-bus-menu");
                    for(String action:new String[]{"settings","settings-display","display-back","settings-sound","sound-back","settings-back","about","about-back","start"}){runtime.setAction(action,true);runtime.update(.01,0,0);runtime.setAction(action,false);runtime.update(.01,0,0);GamePainter.draw(canvas,runtime,image);snapshot(canvas,"alex-vs-bus-"+action);}
                    runtime.setAction("difficulty-normal",true);runtime.update(.01,0,0);runtime.setAction("levels-level1",true);runtime.update(.01,0,0);assertEquals("normal-1",runtime.scene.id);
                    var player=runtime.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();player.x=20;player.y=9;
                    for(int i=0;i<20;i++)runtime.update(.05,0,0);GamePainter.draw(canvas,runtime,image);snapshot(canvas,"alex-vs-bus-game");
                    runtime.setAction("pause",true);runtime.update(.01,0,0);GamePainter.draw(canvas,runtime,image);snapshot(canvas,"alex-vs-bus-pause");
                    assertTrue(images.size()>5);finished.complete(null);
                }catch(Throwable error){finished.completeExceptionally(error);}finally{stage.close();}
            });
            try{finished.get(20,TimeUnit.SECONDS);}finally{Platform.exit();}
        }
    }
    static void snapshot(Canvas canvas,String name)throws Exception{
        var image=canvas.snapshot(null,null);long colored=0;var png=new BufferedImage((int)image.getWidth(),(int)image.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++){int argb=image.getPixelReader().getArgb(x,y);png.setRGB(x,y,argb);if((argb&0xffffff)!=0)colored++;}
        assertTrue(colored>8000,"Canvas is mostly black");Files.createDirectories(Path.of("build/preview"));ImageIO.write(png,"png",Path.of("build/preview/"+name+".png").toFile());
    }
}
