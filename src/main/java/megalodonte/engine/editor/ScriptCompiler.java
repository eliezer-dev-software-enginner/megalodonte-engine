package megalodonte.engine.editor;
import javax.tools.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import megalodonte.engine.*;

final class ScriptCompiler implements AutoCloseable {
    final ClassLoader loader;
    private final URLClassLoader owned;
    private Path compiledDirectory;
    private ScriptCompiler(ClassLoader loader,URLClassLoader owned){this.loader=loader;this.owned=owned;}
    Path classesDirectory(){return compiledDirectory;}
    static ScriptCompiler compile(Path project)throws Exception{
        Path sources=project.resolve("scripts");
        if(!Files.isDirectory(sources))return new ScriptCompiler(GameScript.class.getClassLoader(),null);
        List<Path> files;try(var stream=Files.walk(sources)){files=stream.filter(p->p.toString().endsWith(".java")).toList();}
        if(files.isEmpty())return new ScriptCompiler(GameScript.class.getClassLoader(),null);
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();if(compiler==null)throw new IllegalStateException("Execute a engine com um JDK completo para compilar scripts");
        Files.createDirectories(project.resolve(".engine"));
        Path classes=Files.createTempDirectory(project.resolve(".engine"),"script-classes-");
        var diagnostics=new DiagnosticCollector<JavaFileObject>();
        try(var manager=compiler.getStandardFileManager(diagnostics,null,java.nio.charset.StandardCharsets.UTF_8)){
            var units=manager.getJavaFileObjectsFromPaths(files);
            boolean ok=compiler.getTask(null,manager,diagnostics,List.of("--release","17","-classpath",System.getProperty("java.class.path")+java.io.File.pathSeparator+Path.of(GameScript.class.getProtectionDomain().getCodeSource().getLocation().toURI())+java.io.File.pathSeparator+Path.of(com.google.gson.Gson.class.getProtectionDomain().getCodeSource().getLocation().toURI()),"-d",classes.toString(),"-encoding","UTF-8"),null,units).call();
            if(!ok){StringBuilder report=new StringBuilder();for(var d:diagnostics.getDiagnostics())report.append(d.getSource()==null?"":Path.of(d.getSource().toUri()).getFileName()).append(":").append(d.getLineNumber()).append(":").append(d.getColumnNumber()).append(" ").append(d.getMessage(Locale.getDefault())).append("\n");deleteDirectory(classes);throw new IllegalArgumentException(report.toString());}
        }
        var loader=new URLClassLoader(new URL[]{classes.toUri().toURL()},GameScript.class.getClassLoader());var result=new ScriptCompiler(loader,loader);result.compiledDirectory=classes;return result;
    }
    static void prepareIde(Path project)throws Exception{
        Files.createDirectories(project.resolve("scripts/game/scripts"));Files.createDirectories(project.resolve(".engine/lib"));
        Path engine=Path.of(System.getProperty("engine.home",".")).toAbsolutePath();
        for(String name:List.of("gradlew","gradlew.bat","gradle/wrapper/gradle-wrapper.jar","gradle/wrapper/gradle-wrapper.properties")){
            Path source=engine.resolve(name),target=project.resolve(name);
            if(Files.isRegularFile(source)&&!Files.exists(target)){Files.createDirectories(target.getParent());Files.copy(source,target);if(name.equals("gradlew"))target.toFile().setExecutable(true);}
        }
        copyJar(GameScript.class,project.resolve(".engine/lib/engine-core.jar"));copyJar(com.google.gson.Gson.class,project.resolve(".engine/lib/gson.jar"));
        writeIfMissing(project.resolve("settings.gradle.kts"),"rootProject.name = \"MegalodonteGame\"\n");
        writeIfMissing(project.resolve("build.gradle.kts"),"plugins { java }\nrepositories { mavenCentral() }\njava { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }\ntasks.withType<JavaCompile>().configureEach { options.release.set(17) }\nsourceSets.main { java.setSrcDirs(listOf(\"scripts\")) }\ndependencies { implementation(files(\".engine/lib/engine-core.jar\", \".engine/lib/gson.jar\")) }\n");
        writeIfMissing(project.resolve(".gitignore"),".engine/\nbuild/\n.gradle/\n");
    }
    private static void writeIfMissing(Path file,String content)throws Exception{if(!Files.exists(file))Files.writeString(file,content);}
    static void copyJar(Class<?> type,Path target)throws Exception{
        Path source=Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
        if(Files.isRegularFile(source))Files.copy(source,target,StandardCopyOption.REPLACE_EXISTING);
        else{
            try(var jar=new java.util.jar.JarOutputStream(Files.newOutputStream(target));var stream=Files.walk(source)){
                for(Path p:stream.filter(Files::isRegularFile).toList()){jar.putNextEntry(new java.util.jar.JarEntry(source.relativize(p).toString().replace('\\','/')));Files.copy(p,jar);jar.closeEntry();}
            }
        }
    }
    private static void deleteDirectory(Path directory){try(var stream=Files.walk(directory)){for(Path p:stream.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}catch(Exception ignored){}}
    @Override public void close(){if(owned!=null)try{owned.close();}catch(Exception ignored){}if(compiledDirectory!=null)deleteDirectory(compiledDirectory);}
}
