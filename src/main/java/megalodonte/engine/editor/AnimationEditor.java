package megalodonte.engine.editor;

import javafx.animation.*;
import javafx.collections.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;
import megalodonte.engine.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/** Edits named clips on a copy; preview never mutates the document's clock. */
final class AnimationEditor {
    private final Game.Entity draft;
    private final Consumer<Game.Entity> apply;
    private final PendingAssets assets;private final Path directory;private final Window owner;
    private final ListView<String> names=new ListView<>(),frames=new ListView<>();
    private final TextField name=new TextField(),fps=new TextField();private final CheckBox loop=new CheckBox("Repetir"),initial=new CheckBox("Usar ao iniciar");
    private final ImageView image=new ImageView();private final Label status=new Label(),frameLabel=new Label();
    private final Slider scrub=new Slider();private double time;private boolean playing,changing;
    private String current="";private final Map<String,Image> images=new HashMap<>();
    AnimationEditor(Game.Entity target,Consumer<Game.Entity> apply,PendingAssets assets,Path directory,Window owner){this.draft=target.copy();this.apply=apply;this.assets=assets;this.directory=directory;this.owner=owner;}
    void show(){
        var dialog=new Dialog<Void>();dialog.initOwner(owner);dialog.setTitle("Animações • "+draft.name);dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);((Button)dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).setText("Fechar");
        names.setId("animationNames");names.setPrefWidth(170);names.setItems(FXCollections.observableArrayList(draft.animations.keySet()));
        frames.setId("animationFrames");frames.setPrefHeight(160);name.setId("animationName");fps.setId("animationFps");image.setId("animationPreview");image.setScaleX(draft.flipX?-1:1);image.setSmooth(false);image.setPreserveRatio(true);image.setFitWidth(256);image.setFitHeight(256);
        var form=new VBox(8,new Label("Nome"),name,new Label("Frames por segundo"),fps,new HBox(10,loop,initial),frames,
            new HBox(6,button("Importar frames",this::importFrames),button("Spritesheet",this::importSheet)),
            new HBox(6,button("Subir",()->move(-1)),button("Descer",()->move(1)),button("Excluir frame",()->{int i=frames.getSelectionModel().getSelectedIndex();if(i>=0)frames.getItems().remove(i);syncFrames();})),
            button("Atualizar definição",this::saveClip));
        var left=new VBox(8,new Label("Animações"),names,new HBox(6,button("Nova",this::newClip),button("Excluir",this::deleteClip)));
        var preview=new VBox(10,new Label("Prévia independente do jogo"),image,frameLabel,scrub,new HBox(6,button("Play / Pausa",()->playing=!playing),button("Reiniciar",()->{time=0;preview();})),help("Selecione um frame ou arraste a linha do tempo."));preview.setPrefWidth(270);
        var content=new VBox(10,new HBox(16,left,form,preview),status,button("Aplicar ao objeto",()->{try{saveClip();if(!current.isEmpty()&&!draft.animations.containsKey(current))return;apply.accept(draft.copy());status.setText("Animações aplicadas. Salve o projeto para persistir.");}catch(Exception e){status.setText(e.getMessage());}}));
        dialog.getDialogPane().setContent(content);
        names.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(changing)return;if(a!=null&&!current.isEmpty()){try{saveClip();}catch(Exception e){status.setText(e.getMessage());changing=true;names.getSelectionModel().select(current);changing=false;return;}}changing=true;names.getSelectionModel().select(b);changing=false;load(b);});
        frames.getSelectionModel().selectedIndexProperty().addListener((o,a,b)->{if(!changing&&b.intValue()>=0){playing=false;time=b.intValue()/Math.max(1,number(fps,10));preview();}});
        scrub.valueProperty().addListener((o,a,b)->{if(!changing){playing=false;time=b.doubleValue()/Math.max(1,number(fps,10));preview();}});
        if(!draft.animation.isBlank()&&names.getItems().contains(draft.animation))names.getSelectionModel().select(draft.animation);else if(!names.getItems().isEmpty())names.getSelectionModel().selectFirst();
        var timeline=new Timeline(new KeyFrame(Duration.millis(33),e->{if(playing){time+=.033;preview();}}));timeline.setCycleCount(Timeline.INDEFINITE);timeline.play();dialog.setOnHidden(e->timeline.stop());dialog.show();
    }
    private Label help(String text){var label=new Label(text);label.setWrapText(true);label.setMaxWidth(270);return label;}
    private Button button(String text,Runnable action){var b=new Button(text);b.setOnAction(e->{try{action.run();}catch(Exception error){status.setText(error.getMessage());}});return b;}
    private static double number(TextField field,double fallback){try{return Double.parseDouble(field.getText());}catch(Exception ignored){return fallback;}}
    private void load(String key){current=key==null?"":key;changing=true;try{var clip=draft.animations.get(key);name.setText(current);fps.setText(""+(clip==null?10:clip.fps));loop.setSelected(clip==null||clip.loop);initial.setSelected(current.equals(draft.animation)&&!current.isBlank());frames.getItems().setAll(clip==null?List.of():clip.frames);time=0;playing=false;}finally{changing=false;}preview();}
    private void newClip(){if(!current.isBlank())saveClip();String key="animation";int i=1;while(draft.animations.containsKey(key))key="animation"+(i++);draft.animations.put(key,new Game.Animation());changing=true;names.getItems().add(key);names.getSelectionModel().select(key);changing=false;load(key);}
    private void deleteClip(){draft.animations.remove(current);if(draft.animation.equals(current))draft.animation="";changing=true;names.getItems().remove(current);changing=false;current="";if(names.getItems().isEmpty())load(null);else names.getSelectionModel().selectFirst();}
    private void saveClip(){
        if(current.isBlank())return;String key=name.getText().strip();double speed=Double.parseDouble(fps.getText());if(key.isEmpty()||!Double.isFinite(speed)||speed<=0)throw new IllegalArgumentException("Nome e FPS positivo são obrigatórios.");
        if(!key.equals(current)&&draft.animations.containsKey(key))throw new IllegalArgumentException("Esse nome já existe.");
        if(frames.getItems().isEmpty())throw new IllegalArgumentException("Importe ao menos um frame antes de salvar a animação.");
        var clip=new Game.Animation();clip.fps=speed;clip.loop=loop.isSelected();clip.frames=new ArrayList<>(frames.getItems());var ordered=new LinkedHashMap<String,Game.Animation>();for(var entry:draft.animations.entrySet())ordered.put(entry.getKey().equals(current)?key:entry.getKey(),entry.getKey().equals(current)?clip:entry.getValue());draft.animations=ordered;
        if(initial.isSelected())draft.animation=key;else if(draft.animation.equals(current))draft.animation="";
        int index=names.getItems().indexOf(current);current=key;changing=true;try{if(index>=0)names.getItems().set(index,key);names.getSelectionModel().select(key);}finally{changing=false;}
        status.setText("Definição atualizada. Use Aplicar ao objeto para aplicar.");
    }
    private FileChooser chooser(String title){var chooser=new FileChooser();chooser.setTitle(title);chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens","*.png","*.jpg","*.jpeg","*.PNG","*.JPG"));try{var dir=RecentProject.imageDirectoryPreference().lastSaved();if(dir!=null&&Files.isDirectory(dir))chooser.setInitialDirectory(dir.toFile());}catch(Exception ignored){}return chooser;}
    private void remember(Path path){try{RecentProject.imageDirectoryPreference().rememberSaved(path.getParent());}catch(Exception ignored){}}
    private void importFrames(){if(current.isBlank()){status.setText("Crie ou selecione uma animação.");return;}var files=chooser("Importar sequência de frames").showOpenMultipleDialog(owner);if(files==null)return;try{files.sort(Comparator.comparing(java.io.File::getName));for(var file:files)frames.getItems().add(assets.stage(file.toPath()));remember(files.get(0).toPath());syncFrames();}catch(Exception e){status.setText(e.getMessage());}}
    private void syncFrames(){var clip=draft.animations.get(current);if(clip!=null)clip.frames=new ArrayList<>(frames.getItems());preview();}
    private void move(int delta){int index=frames.getSelectionModel().getSelectedIndex(),next=index+delta;if(index<0||next<0||next>=frames.getItems().size())return;Collections.swap(frames.getItems(),index,next);frames.getSelectionModel().select(next);syncFrames();}
    private void importSheet(){if(current.isBlank())return;var file=chooser("Spritesheet da animação").showOpenDialog(owner);if(file==null)return;remember(file.toPath());
        var dialog=new Dialog<Void>();dialog.initOwner(owner);dialog.setTitle("Extrair frames do spritesheet");dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        var fields=new LinkedHashMap<String,TextField>();var form=new VBox(6);for(var entry:Map.of("X inicial","0","Y inicial","0","Largura","32","Altura","32","Quantidade","1","Passo X","32","Passo Y","0").entrySet()){var field=new TextField(entry.getValue());fields.put(entry.getKey(),field);form.getChildren().addAll(new Label(entry.getKey()),field);}var error=new Label();form.getChildren().add(error);dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,e->{try{int x=Integer.parseInt(fields.get("X inicial").getText()),y=Integer.parseInt(fields.get("Y inicial").getText()),w=Integer.parseInt(fields.get("Largura").getText()),h=Integer.parseInt(fields.get("Altura").getText()),count=Integer.parseInt(fields.get("Quantidade").getText()),dx=Integer.parseInt(fields.get("Passo X").getText()),dy=Integer.parseInt(fields.get("Passo Y").getText());
            var source=javax.imageio.ImageIO.read(file);if(source==null||w<=0||h<=0||count<=0||count>1000)throw new IllegalArgumentException("Spritesheet ou dimensões inválidos.");
            for(int i=0;i<count;i++)if(x+i*dx<0||y+i*dy<0||x+i*dx+w>source.getWidth()||y+i*dy+h>source.getHeight())throw new IllegalArgumentException("Um frame ultrapassa os limites da imagem.");
            var added=new ArrayList<String>();for(int i=0;i<count;i++){Path temp=Files.createTempFile("frame-"+i+"-",".png");try{javax.imageio.ImageIO.write(source.getSubimage(x+i*dx,y+i*dy,w,h),"png",temp.toFile());added.add(assets.stage(temp));}finally{Files.deleteIfExists(temp);}}frames.getItems().addAll(added);syncFrames();
        }catch(Exception failure){error.setText(failure.getMessage());e.consume();}});dialog.show();
    }
    private void preview(){
        var clip=draft.animations.get(current);if(clip==null||frames.getItems().isEmpty()){image.setImage(null);scrub.setDisable(true);frameLabel.setText("Sem frames");return;}
        scrub.setDisable(false);int count=frames.getItems().size(),frame=(int)Math.floor(time*Math.max(.001,number(fps,clip.fps)));if(loop.isSelected())frame%=count;else{frame=Math.min(frame,count-1);if(frame==count-1)playing=false;}
        frame=Math.max(0,frame);Path path=assets.resolve(frames.getItems().get(frame),directory);image.setImage(path==null?null:images.computeIfAbsent(path.toString(),key->new Image(path.toUri().toString())));frameLabel.setText("Frame "+(frame+1)+" / "+count);changing=true;try{scrub.setMax(count-1);scrub.setValue(frame);}finally{changing=false;}
    }
}
