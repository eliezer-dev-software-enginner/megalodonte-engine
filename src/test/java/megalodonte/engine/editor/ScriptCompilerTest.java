package megalodonte.engine.editor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import megalodonte.engine.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class ScriptCompilerTest {
    @TempDir Path temp;
    @Test void compilesAndExecutesPortableJavaThenReportsDiagnostics()throws Exception{
        ScriptCompiler.prepareIde(temp);Path source=temp.resolve("scripts/game/scripts/Move.java");
        Files.writeString(source,"package game.scripts; import megalodonte.engine.*; public class Move implements GameScript {public void onUpdate(double dt,GameContext c){c.move(c.horizontal()*4*dt,0);}}");
        var game=Game.demo();game.scenes.getFirst().objects.getFirst().scriptClassName="game.scripts.Move";
        try(var compiler=ScriptCompiler.compile(temp);var runtime=new megalodonte.engine.Runtime(game,megalodonte.engine.Runtime.reflection(compiler.loader),new megalodonte.engine.Runtime.Events(){})){runtime.update(.05,1,0);assertEquals(2.2,runtime.scene.objects.getFirst().x,1e-8);}
        Files.writeString(source,"package game.scripts; public class Move { erro }");Exception error=assertThrows(Exception.class,()->ScriptCompiler.compile(temp));assertTrue(error.getMessage().contains("Move.java:"));
        assertTrue(Files.readString(temp.resolve("build.gradle.kts")).contains("scripts"));assertTrue(Files.exists(temp.resolve(".engine/lib/engine-core.jar")));
        assertTrue(Files.exists(temp.resolve("gradlew")));assertTrue(Files.readString(temp.resolve("build.gradle.kts")).contains("options.release.set(17)"));
    }
}
