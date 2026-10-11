package megalodonte.engine.editor;
import javafx.stage.*;
import java.nio.file.*;
import java.util.function.*;
import megalodonte.engine.SaveData;
final class SaveFiles {
    private static FileChooser chooser(String title){var c=new FileChooser();c.setTitle(title);c.getExtensionFilters().add(new FileChooser.ExtensionFilter("Dados salvos JSON","*.json","*.JSON"));return c;}
    static void export(Window owner,String json,Consumer<String> done){
        try{var c=chooser("Exportar dados salvos");c.setInitialFileName("alex-vs-bus-save.json");var file=c.showSaveDialog(owner);if(file==null){done.accept("Exportação cancelada.");return;}Files.writeString(file.toPath(),json);done.accept("Dados exportados.");}catch(Exception e){done.accept("Falha ao exportar: "+e.getMessage());}
    }
    static void load(Window owner,BiConsumer<String,String> done){
        try{var c=chooser("Importar dados salvos");var file=c.showOpenDialog(owner);if(file==null){done.accept(null,"Importação cancelada.");return;}if(Files.size(file.toPath())>SaveData.MAX_BYTES)throw new IllegalArgumentException("Arquivo muito grande");done.accept(Files.readString(file.toPath()),null);}catch(Exception e){done.accept(null,"Falha ao importar: "+e.getMessage());}
    }
}
