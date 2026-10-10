package megalodonte.engine.editor;

import megalodonte.engine.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/** Builds an app-image containing a JVM, JavaFX, compiled scripts and the game. */
public final class DesktopPackager {
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("Informe a pasta do jogo");
        Path project=Path.of(args[0]).toAbsolutePath().normalize();var game=Codec.read(Files.readString(project.resolve("game.json")));
        Path output=project.resolve("build/desktop");Files.createDirectories(output);Path temporary=Files.createTempDirectory(output,"package-");
        String name=game.applicationId.substring(game.applicationId.lastIndexOf('.')+1);
        try{
            Path input=temporary.resolve("input");Files.createDirectories(input);Path snapshot=input.resolve("game");Files.createDirectories(snapshot);
            for(String entry:List.of("game.json","assets","scripts","licenses","LICENSE.txt","README.md","tools"))if(Files.exists(project.resolve(entry)))copy(project.resolve(entry),snapshot.resolve(entry));
            try(var compiler=ScriptCompiler.compile(snapshot)){if(compiler.classesDirectory()!=null)jar(compiler.classesDirectory(),input.resolve("game-scripts.jar"));}
            delete(snapshot.resolve(".engine"));
            ScriptCompiler.copyJar(DesktopLauncher.class,input.resolve("engine-player.jar"));ScriptCompiler.copyJar(Game.class,input.resolve("engine-core.jar"));ScriptCompiler.copyJar(com.google.gson.Gson.class,input.resolve("gson.jar"));
            for(String item:System.getProperty("java.class.path").split(java.io.File.pathSeparator)){Path file=Path.of(item);if(Files.isRegularFile(file)&&file.getFileName().toString().startsWith("javafx-"))Files.copy(file,input.resolve(file.getFileName()),StandardCopyOption.REPLACE_EXISTING);}
            Path jdk=Path.of(System.getProperty("java.home"));String suffix=System.getProperty("os.name").startsWith("Windows")?".exe":"";
            Path runtime=temporary.resolve("runtime");run(List.of(jdk.resolve("bin/jlink"+suffix).toString(),"--add-modules","java.se,jdk.unsupported","--strip-debug","--no-header-files","--no-man-pages","--output",runtime.toString()));
            Path packaged=temporary.resolve("result");run(List.of(jdk.resolve("bin/jpackage"+suffix).toString(),"--type","app-image","--name",name,"--app-version","0.1.0","--vendor","Megalodonte","--input",input.toString(),"--main-jar","engine-player.jar","--main-class",DesktopLauncher.class.getName(),"--runtime-image",runtime.toString(),"--java-options","--enable-native-access=ALL-UNNAMED","--dest",packaged.toString()));
            Path result=packaged.resolve(System.getProperty("os.name").startsWith("Mac")?name+".app":name),target=output.resolve(result.getFileName());
            delete(target);Files.move(result,target);System.out.println("DESKTOP: "+target);
        }finally{delete(temporary);}
    }
    private static void run(List<String> command)throws Exception{int code=new ProcessBuilder(command).inheritIO().start().waitFor();if(code!=0)throw new IllegalStateException("Build Desktop falhou ("+code+"): "+command.getFirst());}
    private static void jar(Path directory,Path target)throws Exception{try(var out=new JarOutputStream(Files.newOutputStream(target));var stream=Files.walk(directory)){for(Path file:stream.filter(Files::isRegularFile).toList()){out.putNextEntry(new JarEntry(directory.relativize(file).toString().replace('\\','/')));Files.copy(file,out);out.closeEntry();}}}
    private static void copy(Path from,Path to)throws Exception{if(Files.isDirectory(from)){Files.createDirectories(to);try(var stream=Files.list(from)){for(Path child:stream.toList())copy(child,to.resolve(child.getFileName()));}}else{Files.createDirectories(to.getParent());Files.copy(from,to,StandardCopyOption.REPLACE_EXISTING);}}
    private static void delete(Path directory)throws Exception{if(!Files.exists(directory))return;try(var stream=Files.walk(directory)){for(Path path:stream.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
}
