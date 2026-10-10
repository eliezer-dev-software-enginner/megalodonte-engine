package megalodonte.engine.editor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class BuildFolderTest {
    @TempDir Path temp;
    @Test void revealsExactDirectoryAndCanBeDisabledForCi()throws Exception{
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getProperty("os.name").equals("Linux"));
        Path bin=temp.resolve("bin"),folder=temp.resolve("jogo com espaços/desktop"),trace=temp.resolve("opened");Files.createDirectories(bin);Files.createDirectories(folder);
        Path fake=bin.resolve("xdg-open");Files.writeString(fake,"#!/usr/bin/env bash\nprintf '%s' \"$1\" > \"$OPEN_TRACE\"\n");fake.toFile().setExecutable(true);
        var process=new ProcessBuilder("bash","scripts/open-build-folder.sh",folder.toString());process.environment().put("PATH",bin+java.io.File.pathSeparator+System.getenv("PATH"));process.environment().put("DISPLAY",":test");process.environment().put("OPEN_TRACE",trace.toString());process.environment().remove("ENGINE_NO_OPEN");
        assertEquals(0,process.start().waitFor());assertEquals(folder.toString(),Files.readString(trace));Files.delete(trace);
        process.environment().put("ENGINE_NO_OPEN","1");assertEquals(0,process.start().waitFor());assertFalse(Files.exists(trace));
    }
    @Test void commandLineExportRegistersProjectForEditorAndInvalidProjectPreservesPreference()throws Exception{
        Path folder=temp.resolve("jogo com espaços"),preference=temp.resolve("prefs/last-saved-project.txt");Files.createDirectories(folder);
        Files.writeString(folder.resolve("game.json"),megalodonte.engine.Codec.GSON.toJson(megalodonte.engine.Game.demo()));
        var command=new ProcessBuilder("bash","scripts/remember-project.sh",folder.toString());command.environment().put("ENGINE_PREFERENCES_FILE",preference.toString());
        assertEquals(0,command.start().waitFor());assertEquals(folder.resolve("game.json"),new RecentProject(preference).lastSaved());
        Files.writeString(folder.resolve("game.json"),"{}");assertNotEquals(0,command.start().waitFor());assertEquals(folder.resolve("game.json"),new RecentProject(preference).lastSaved());
    }
}
