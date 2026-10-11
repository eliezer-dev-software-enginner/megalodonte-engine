package megalodonte.engine.editor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Machine settings live outside authored games. */
final class EngineSettings {
    static RecentProject sdkPreference(){return RecentProject.androidSdkPreference();}
    static Path sdk()throws java.io.IOException{
        var chosen=sdkPreference().lastSaved();if(chosen!=null)return chosen;
        for(String value:List.of(System.getenv().getOrDefault("ANDROID_HOME",""),System.getenv().getOrDefault("ANDROID_SDK_ROOT",""),Path.of(System.getProperty("user.home"),"Android","Sdk").toString(),Path.of(System.getProperty("user.home"),"Library","Android","sdk").toString()))if(!value.isBlank()&&Files.isDirectory(Path.of(value).resolve("platforms")))return Path.of(value).toAbsolutePath().normalize();
        return null;
    }
    static Path validate(String value){
        if(value.isBlank())throw new IllegalArgumentException("Selecione a pasta do SDK Android.");
        var path=Path.of(value).toAbsolutePath().normalize();if(!Files.isDirectory(path.resolve("platforms")))throw new IllegalArgumentException("SDK inválido: selecione a pasta que contém platforms.");return path;
    }
    static void save(String value,Path game)throws java.io.IOException{
        var path=validate(value);
        if(game!=null){var exported=game.resolve("build/android-release/project");if(Files.isDirectory(exported)){var file=exported.resolve("local.properties");var props=new Properties();if(Files.exists(file))try(var in=Files.newInputStream(file)){props.load(in);}props.setProperty("sdk.dir",path.toString());try(var out=Files.newOutputStream(file)){props.store(out,"SDK configurado pela Megalodonte Engine");}}}
        sdkPreference().rememberSaved(path);
    }
    static void show(Window owner,Path game,Consumer<String> message){
        var dialog=new Dialog<Void>();dialog.initOwner(owner);dialog.setTitle("Configurações da engine");
        var input=new TextField();input.setId("androidSdkPath");input.setPromptText("/home/usuario/Android/Sdk");
        try{var path=sdk();if(path!=null)input.setText(path.toString());}catch(Exception e){message.accept(e.getMessage());}
        var choose=new Button("Selecionar pasta…");choose.setOnAction(e->{var picker=new DirectoryChooser();picker.setTitle("SDK Android");if(Files.isDirectory(Path.of(input.getText())))picker.setInitialDirectory(Path.of(input.getText()).toFile());var result=picker.showDialog(owner);if(result!=null)input.setText(result.toPath().toString());});
        var info=new Label("O projeto exportado usa este caminho em local.properties. A configuração é salva neste computador, separada dos dados do jogo.");info.setWrapText(true);info.setMinHeight(60);
        var status=new Label();status.setWrapText(true);status.setMinHeight(40);status.setId("engineSettingsStatus");
        var page=new VBox(10,new Label("SDK Android"),input,choose,info,status);page.setPrefWidth(540);
        var tab=new Tab("Android",page);tab.setClosable(false);dialog.getDialogPane().setContent(new TabPane(tab));dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY,ButtonType.CLOSE);
        ((Button)dialog.getDialogPane().lookupButton(ButtonType.APPLY)).setText("Salvar configurações");((Button)dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).setText("Fechar");
        dialog.getDialogPane().lookupButton(ButtonType.APPLY).addEventFilter(javafx.event.ActionEvent.ACTION,e->{e.consume();try{save(input.getText().strip(),game);status.setText("SDK salvo. O projeto Android exportado usará este caminho.");message.accept("Configurações da engine salvas.");}catch(Exception error){status.setText(error.getMessage());}});dialog.show();
    }
}
