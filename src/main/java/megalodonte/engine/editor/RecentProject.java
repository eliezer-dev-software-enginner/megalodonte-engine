package megalodonte.engine.editor;

import java.io.IOException;
import java.nio.file.*;

/** Local editor preference, separate from game data and Android exports. */
final class RecentProject {
    private final Path preference;
    RecentProject(Path preference) { this.preference = preference; }
    static RecentProject userPreference() {
        String override = System.getProperty("engine.preferencesFile");
        return new RecentProject(override == null
                ? Path.of(System.getProperty("user.home"), ".config", "megalodonte-engine", "last-saved-project.txt")
                : Path.of(override));
    }
    Path lastSaved() throws IOException {
        if (!Files.exists(preference)) return null;
        String value = Files.readString(preference).strip();
        return value.isEmpty() ? null : Path.of(value);
    }
    void rememberSaved(Path project) throws IOException {
        Path target = preference.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "recent-", ".tmp");
        try {
            Files.writeString(temporary, project.toAbsolutePath().normalize().toString());
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
