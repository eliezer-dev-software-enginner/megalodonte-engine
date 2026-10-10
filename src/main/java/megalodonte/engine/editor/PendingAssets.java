package megalodonte.engine.editor;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Snapshots imports so unsaved games need no project chooser or source file later. */
final class PendingAssets implements AutoCloseable {
    private final Map<String, Path> pending = new LinkedHashMap<>();
    String stage(Path source) throws IOException {
        String portable=source.getFileName().toString().replace(':','_').replace('\\','_');while(portable.contains(".."))portable=portable.replace("..","_");
        String name = UUID.randomUUID()+"-"+portable;
        String filename = source.getFileName().toString();
        String suffix = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : ".tmp";
        Path temporary = Files.createTempFile("megalodonte-asset-", suffix);
        try { Files.copy(source,temporary,StandardCopyOption.REPLACE_EXISTING); }
        catch(IOException e) { Files.deleteIfExists(temporary); throw e; }
        String asset="assets/"+name;pending.put(asset,temporary);return asset;
    }
    Path resolve(String asset, Path project) {
        Path staged = pending.get(asset);
        return staged != null ? staged : project == null ? null : project.resolve(asset);
    }
    Set<String> stagedPaths(){return Set.copyOf(pending.keySet());}
    void publish(Path project) throws IOException {
        Files.createDirectories(project.resolve("assets"));
        for(var entry:pending.entrySet()) Files.copy(entry.getValue(),project.resolve(entry.getKey()),StandardCopyOption.REPLACE_EXISTING);
    }
    @Override public void close() {
        for(Path temporary:pending.values())try{Files.deleteIfExists(temporary);}catch(IOException ignored){temporary.toFile().deleteOnExit();}
        pending.clear();
    }
}
