package megalodonte.engine.editor;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.nio.file.*;
import java.util.*;

/** Imports shared assets including effects that need not be assigned as scene music. */
final class AssetBrowser {
    private AssetBrowser(){}
    static void show(Window owner,PendingAssets assets,Path project){
        var paths=new TreeSet<String>(assets.stagedPaths());if(project!=null&&Files.isDirectory(project.resolve("assets")))try(var files=Files.walk(project.resolve("assets"))){files.filter(Files::isRegularFile).forEach(path->paths.add(project.relativize(path).toString().replace('\\','/')));}catch(Exception ignored){}
        var list=new ListView<String>();list.setId("assetPaths");list.getItems().setAll(paths);list.setPrefWidth(520);list.setPrefHeight(320);var path=new TextField();path.setEditable(false);var image=new ImageView();image.setId("assetPreview");image.setSmooth(false);image.setPreserveRatio(true);image.setFitWidth(240);image.setFitHeight(240);var message=new Label();message.setWrapText(true);final javafx.scene.media.MediaPlayer[] audio={null};Runnable stop=()->{if(audio[0]!=null){audio[0].dispose();audio[0]=null;}};
        list.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{stop.run();path.setText(b==null?"":b);image.setImage(null);if(b!=null&&b.toLowerCase(Locale.ROOT).matches(".*\\.(png|jpg|jpeg|gif)")){Path file=assets.resolve(b,project);if(file!=null)image.setImage(new Image(file.toUri().toString()));}});
        var importButton=new Button("Importar arquivos");importButton.setOnAction(event->{var chooser=new FileChooser();chooser.setTitle("Importar assets: imagens, músicas e efeitos");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens e áudio","*.png","*.jpg","*.jpeg","*.wav","*.mp3","*.PNG","*.JPG","*.JPEG","*.WAV","*.MP3"));try{var directory=RecentProject.assetDirectoryPreference().lastSaved();if(directory!=null&&Files.isDirectory(directory))chooser.setInitialDirectory(directory.toFile());}catch(Exception ignored){}var files=chooser.showOpenMultipleDialog(owner);if(files==null)return;try{for(var file:files)paths.add(assets.stage(file.toPath()));RecentProject.assetDirectoryPreference().rememberSaved(files.get(0).toPath().getParent());for(var file:files)if(file.getName().toLowerCase(Locale.ROOT).matches(".*\\.(png|jpg|jpeg)")){RecentProject.imageDirectoryPreference().rememberSaved(file.toPath().getParent());break;}list.getItems().setAll(paths);message.setText("Importado. Salve o projeto; o caminho pode ser usado em sprites, HUD, músicas e scripts.");}catch(Exception e){message.setText(e.getMessage());}});
        var listen=new Button("Ouvir / parar");listen.setOnAction(e->{if(audio[0]!=null){stop.run();return;}String value=list.getSelectionModel().getSelectedItem();if(value==null||!value.toLowerCase(Locale.ROOT).matches(".*\\.(wav|mp3)"))return;try{Path file=assets.resolve(value,project);audio[0]=new javafx.scene.media.MediaPlayer(new javafx.scene.media.Media(file.toUri().toString()));audio[0].setOnError(()->{if(audio[0]!=null)message.setText("Áudio: "+audio[0].getError());});audio[0].play();}catch(Exception error){message.setText(error.getMessage());}});
        var copy=new Button("Copiar caminho");copy.setOnAction(e->{var content=new javafx.scene.input.ClipboardContent();content.putString(path.getText());javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);});
        var dialog=new Dialog<Void>();dialog.initOwner(owner);dialog.setTitle("Assets do projeto");dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);((Button)dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).setText("Fechar");dialog.getDialogPane().setContent(new VBox(8,new HBox(12,list,image),path,new HBox(8,importButton,listen,copy),message));dialog.setOnHidden(e->stop.run());dialog.show();
    }
}
