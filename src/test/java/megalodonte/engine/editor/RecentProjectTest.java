package megalodonte.engine.editor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class RecentProjectTest {
    @TempDir Path temp;
    @Test void survivesNewInstanceAndRemembersLatestSave() throws Exception {
        Path preference=temp.resolve("config/last.txt");
        var recent=new RecentProject(preference);
        assertNull(recent.lastSaved());
        Path first=temp.resolve("Jogo com espaços/game.json");
        recent.rememberSaved(first);
        assertEquals(first,new RecentProject(preference).lastSaved());
        Path second=temp.resolve("Outro jogo/game.json");
        recent.rememberSaved(second);
        assertEquals(second,new RecentProject(preference).lastSaved());
    }
}
