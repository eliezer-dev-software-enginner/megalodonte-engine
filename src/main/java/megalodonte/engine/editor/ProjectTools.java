package megalodonte.engine.editor;
import java.nio.file.*;
import megalodonte.engine.Codec;
public final class ProjectTools {
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("Informe a pasta do jogo");
        var path=Path.of(args[0]).toAbsolutePath();Codec.read(Files.readString(path.resolve("game.json")));ScriptCompiler.prepareIde(path);System.out.println("Projeto de scripts preparado: "+path);
        try(var compiler=ScriptCompiler.compile(path)){System.out.println("Scripts compilados com sucesso.");}
    }
}
