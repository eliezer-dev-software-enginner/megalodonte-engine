package megalodonte.engine.editor;
import megalodonte.script.ViewScript;
import megalodonte.engine.*;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.stage.*;
import javafx.scene.input.KeyCode;
import java.nio.file.*;
import java.util.*;

public final class Editor extends ViewScript {
    private Game game = Game.demo();
    private int sceneIndex;
    private Game.Entity selected;
    private Game.Hud selectedHud;
    private boolean resizingHud;
    private double hudDragX, hudDragY;
    private Path directory;
    private final RecentProject recentProject = RecentProject.userPreference();
    private Canvas canvas;
    private VBox tree;
    private AnimationTimer timer;
    private megalodonte.engine.Runtime runtime;
    private final Set<KeyCode> keys = new HashSet<>();
    private final Map<String,Image> images = new HashMap<>();
    private double scale, ox, oy, dragX, dragY;
    private long last;
    private Process androidBuild;
    private boolean disposed;
    private boolean saved;
    private Game.Scene scene() { return runtime == null ? game.scenes.get(sceneIndex) : runtime.scene; }
    @Override public void onSubmit() { applyObject(); }
    @Override public void onMount() {
        Pane host=(Pane)component("viewport").getJavaFxNode();
        canvas=new Canvas(720,480); canvas.setManaged(false); host.getChildren().add(canvas);
        canvas.widthProperty().bind(host.widthProperty()); canvas.heightProperty().bind(host.heightProperty());
        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(e -> keys.add(e.getCode())); canvas.setOnKeyReleased(e -> keys.remove(e.getCode()));
        canvas.focusedProperty().addListener((o,a,b)-> { if(!b) keys.clear(); });
        canvas.setOnMousePressed(e -> {
            canvas.requestFocus(); if(runtime!=null)return;
            resizingHud=false;
            if(selectedHud!=null){var b=HudLayout.resolve(selectedHud,canvas.getWidth(),canvas.getHeight());
                if(Math.abs(e.getX()-(b.x()+b.width()))<=10 && Math.abs(e.getY()-(b.y()+b.height()))<=10){resizingHud=true;return;}}
            selectedHud=null;
            var hud=scene().hud;
            for(int i=hud.size()-1;i>=0;i--){var h=hud.get(i);var b=HudLayout.resolve(h,canvas.getWidth(),canvas.getHeight());
                if(b.contains(e.getX(),e.getY())){selectedHud=h;selected=null;hudDragX=e.getX()-b.x();hudDragY=e.getY()-b.y();inspectHud();return;}}
            set("hudSelected",false);
            double x=(e.getX()-ox)/scale, y=(oy-e.getY())/scale;
            selected=null;
            var objects=scene().objects;
            for(int i=objects.size()-1;i>=0;i--) { var obj=objects.get(i); if(x>=obj.x && x<=obj.x+obj.width && y>=obj.y && y<=obj.y+obj.height){ selected=obj; dragX=x-obj.x; dragY=y-obj.y; break; } }
            inspect();
        });
        canvas.setOnMouseDragged(e -> {
            if(runtime!=null)return;
            if(selectedHud!=null){
                var b=HudLayout.resolve(selectedHud,canvas.getWidth(),canvas.getHeight());
                if(b.scale()<=0)return;
                if(resizingHud){selectedHud.width=Math.max(16,(e.getX()-b.x())/b.scale());selectedHud.height=Math.max(16,(e.getY()-b.y())/b.scale());HudLayout.position(selectedHud,b.x(),b.y(),canvas.getWidth(),canvas.getHeight());}
                else HudLayout.position(selectedHud,e.getX()-hudDragX,e.getY()-hudDragY,canvas.getWidth(),canvas.getHeight());
                inspectHud();
            }else if(selected!=null){selected.x=(e.getX()-ox)/scale-dragX;selected.y=(oy-e.getY())/scale-dragY;inspect();}
        });
        tree=new VBox(6); ((Pane)component("tree").getJavaFxNode()).getChildren().add(new ScrollPane(tree));
        timer=new AnimationTimer(){ public void handle(long now){
            double dt=last==0?0:(now-last)/1e9; last=now;
            if(runtime!=null)runtime.update(dt,axis(KeyCode.D,KeyCode.RIGHT)-axis(KeyCode.A,KeyCode.LEFT),axis(KeyCode.W,KeyCode.UP)-axis(KeyCode.S,KeyCode.DOWN));
            draw();
        }}; timer.start(); refresh(); restoreLastSaved();
    }
    private int axis(KeyCode a,KeyCode b){return keys.contains(a)||keys.contains(b)?1:0;}
    private void draw(){
        var s=scene(); var g=canvas.getGraphicsContext2D();
        g.setFill(Color.web("#0c1420"));g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        scale=Math.min(canvas.getWidth()/s.worldWidth,canvas.getHeight()/s.worldHeight);
        ox=(canvas.getWidth()-s.worldWidth*scale)/2;oy=(canvas.getHeight()+s.worldHeight*scale)/2;
        g.setFill(Color.web(s.background));g.fillRect(ox,oy-s.worldHeight*scale,s.worldWidth*scale,s.worldHeight*scale);
        if(runtime==null){g.setStroke(Color.web("#ffffff18")); for(int i=0;i<=s.worldWidth;i++)g.strokeLine(ox+i*scale,oy,ox+i*scale,oy-s.worldHeight*scale);for(int i=0;i<=s.worldHeight;i++)g.strokeLine(ox,oy-i*scale,ox+s.worldWidth*scale,oy-i*scale);}
        for(var e:s.objects){double x=ox+e.x*scale,y=oy-(e.y+e.height)*scale;
            Image image=null;
            if(directory!=null&&!e.assetPath.isBlank())image=images.computeIfAbsent(e.assetPath,path->{try{return new Image(directory.resolve(path).toUri().toString());}catch(Exception ex){return null;}});
            if(image!=null&&!image.isError())g.drawImage(image,x,y,e.width*scale,e.height*scale);
            else{g.setFill(Color.web(e.color));g.fillRect(x,y,e.width*scale,e.height*scale);}
            if(runtime==null&&e==selected){g.setStroke(Color.CYAN);g.setLineWidth(2);g.strokeRect(x,y,e.width*scale,e.height*scale);}
        }
        drawHud();
    }
    private void drawHud() {
        var g=canvas.getGraphicsContext2D();
        for(var h:scene().hud){var b=HudLayout.resolve(h,canvas.getWidth(),canvas.getHeight());if(b.scale()<=0)continue;
            g.save();g.beginPath();g.rect(b.x(),b.y(),b.width(),b.height());g.clip();
            g.setFill(Color.web(h.color));
            if(h.type.equals("text")){
                g.setFont(javafx.scene.text.Font.font(h.fontSize*b.scale()));g.setTextBaseline(javafx.geometry.VPos.TOP);
                g.fillText(h.text,b.x(),b.y());
            }else{Image image=null;if(directory!=null&&!h.assetPath.isBlank())image=images.computeIfAbsent(h.assetPath,path->new Image(directory.resolve(path).toUri().toString()));
                if(image!=null&&!image.isError())g.drawImage(image,b.x(),b.y(),b.width(),b.height());
                else{g.fillRect(b.x(),b.y(),b.width(),b.height());}}
            g.restore();
            if(runtime==null && h==selectedHud){g.setStroke(Color.CYAN);g.setLineWidth(2);g.strokeRect(b.x(),b.y(),b.width(),b.height());g.setFill(Color.CYAN);g.fillRect(b.x()+b.width()-6,b.y()+b.height()-6,12,12);}
        }
    }
    private void inspectHud(){
        if(selectedHud==null){set("hudSelected",false);set("worldSelected",true);return;}set("hudSelected",true);set("worldSelected",false);
        var h=selectedHud;set("hudId",h.id);set("hudText",h.text);set("hudX",""+h.offsetX);set("hudY",""+h.offsetY);
        set("hudWidth",""+h.width);set("hudHeight",""+h.height);set("hudFont",""+h.fontSize);set("hudColor",h.color);
        set("hudAnchors",h.horizontalAnchor+" / "+h.verticalAnchor);message("HUD selecionada: "+h.type+" • arraste a alça para redimensionar");
    }
    public void addHudText(){addHud("text");}
    public void addHudImage(){addHud("image");importHudImage();}
    private void addHud(String type){stop();selected=null;selectedHud=new Game.Hud();selectedHud.type=type;
        if(type.equals("image")){selectedHud.width=128;selectedHud.height=128;}
        game.scenes.get(sceneIndex).hud.add(selectedHud);refresh();inspectHud();}
    public void applyHud(){if(selectedHud==null)return;stop();try{
        var h=selectedHud.copy();h.id=text("hudId");h.text=text("hudText");h.offsetX=Double.parseDouble(text("hudX"));h.offsetY=Double.parseDouble(text("hudY"));
        h.width=Double.parseDouble(text("hudWidth"));h.height=Double.parseDouble(text("hudHeight"));h.fontSize=Double.parseDouble(text("hudFont"));h.color=text("hudColor");
        Game copy=Codec.copy(game);int index=game.scenes.get(sceneIndex).hud.indexOf(selectedHud);copy.scenes.get(sceneIndex).hud.set(index,h);Codec.validate(copy);
        game.scenes.get(sceneIndex).hud.set(index,h);selectedHud=h;refresh();inspectHud();
    }catch(Exception e){message("HUD inválida: "+e.getMessage());}}
    public void cycleHudHorizontal(){if(selectedHud==null)return;stop();selectedHud.horizontalAnchor=switch(selectedHud.horizontalAnchor){case "left"->"center";case "center"->"right";default->"left";};inspectHud();}
    public void cycleHudVertical(){if(selectedHud==null)return;stop();selectedHud.verticalAnchor=switch(selectedHud.verticalAnchor){case "top"->"center";case "center"->"bottom";default->"top";};inspectHud();}
    public void importHudImage(){if(selectedHud==null||!selectedHud.type.equals("image"))return;
        if(directory==null){saveProject();if(!saved)return;}
        FileChooser chooser=new FileChooser();chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagem","*.png","*.jpg","*.jpeg"));
        var file=chooser.showOpenDialog(canvas.getScene().getWindow());if(file==null)return;
        try{String name=UUID.randomUUID()+"-"+file.getName();Files.createDirectories(directory.resolve("assets"));Files.copy(file.toPath(),directory.resolve("assets").resolve(name));selectedHud.assetPath="assets/"+name;message("Imagem da HUD importada. Salve o projeto.");}catch(Exception e){message(e.getMessage());}}
    private void inspect(){set("hudSelected",false);set("worldSelected",true);if(selected==null)return;set("name",selected.name);set("x",""+selected.x);set("y",""+selected.y);set("width",""+selected.width);set("height",""+selected.height);set("color",selected.color);set("speed",""+selected.speed);message("Selecionado: "+selected.name+" • controlável="+selected.player+" • colisor="+selected.solid);}
    private void refresh(){draw();tree.getChildren().clear();for(var e:game.scenes.get(sceneIndex).objects){Button b=new Button(e.name);b.setMaxWidth(Double.MAX_VALUE);b.setOnAction(event->{stop();selectedHud=null;selected=e;inspect();});tree.getChildren().add(b);}for(var h:game.scenes.get(sceneIndex).hud){Button b=new Button("HUD • "+(h.type.equals("text")?h.text:h.id));b.setOnAction(event->{stop();selected=null;selectedHud=h;inspectHud();});tree.getChildren().add(b);}set("project",game.name+" • "+game.scenes.get(sceneIndex).name+" • inicial: "+game.entrySceneId+" • "+(directory==null?"não salvo":directory));}
    private void stop(){runtime=null;keys.clear();last=0;}
    public void togglePlay(){if(runtime==null){try{runtime=new megalodonte.engine.Runtime(game);canvas.requestFocus();message("Play: cena inicial. WASD / setas para mover.");}catch(Exception e){message(e.getMessage());}}else{stop();message("Play encerrado; edição preservada.");}}
    public void newProject(){stop();game=Game.demo();sceneIndex=0;directory=null;selected=null;selectedHud=null;set("hudSelected",false);set("worldSelected",true);images.clear();refresh();}
    public void openProject(){
        FileChooser chooser=new FileChooser();
        chooser.setTitle("Abrir projeto — game.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Projeto Megalodonte (game.json)","*.json"));
        if(directory!=null && Files.isDirectory(directory))chooser.setInitialDirectory(directory.toFile());
        var file=chooser.showOpenDialog(canvas.getScene().getWindow());if(file==null)return;
        try{loadProject(file.toPath());message("Projeto carregado.");}
        catch(Exception e){message("Falha ao abrir: "+e.getMessage());}
    }
    private void loadProject(Path file) throws java.io.IOException {
        Path target=file.toAbsolutePath().normalize();
        if(!target.getFileName().toString().equals("game.json"))throw new IllegalArgumentException("Selecione o arquivo game.json do projeto");
        // Validate before replacing the active document.
        Game next=Codec.read(Files.readString(target));
        stop();game=next;directory=target.getParent();sceneIndex=0;selected=null;selectedHud=null;
        set("hudSelected",false);set("worldSelected",true);images.clear();refresh();
    }
    private void restoreLastSaved(){
        try{Path file=recentProject.lastSaved();if(file==null)return;
            loadProject(file);message("Último projeto salvo restaurado.");
        }catch(Exception e){message("Não foi possível restaurar o último projeto salvo: "+e.getMessage()+". Use Abrir para localizá-lo.");}
    }
    public void saveProject(){saved=false;try{Codec.validate(game);if(directory==null){
            FileChooser chooser=new FileChooser();
            chooser.setTitle("Salvar projeto — game.json");
            chooser.setInitialFileName("game.json");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Projeto Megalodonte (game.json)","*.json"));
            var file=chooser.showSaveDialog(canvas.getScene().getWindow());if(file==null)return;
            Path target=file.toPath().toAbsolutePath();
            if(!target.getFileName().toString().equals("game.json"))throw new IllegalArgumentException("Salve com o nome game.json na pasta do jogo");
            if(Files.exists(target))throw new IllegalArgumentException("Escolha uma pasta sem game.json para o novo projeto; use Abrir para editar um existente");
            directory=target.getParent();
        }Files.createDirectories(directory.resolve("assets"));Path temp=Files.createTempFile(directory,"game-",".tmp");try{Files.writeString(temp,Codec.GSON.toJson(game));try{Files.move(temp,directory.resolve("game.json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,directory.resolve("game.json"),StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}saved=true;refresh();
        try{recentProject.rememberSaved(directory.resolve("game.json"));message("Projeto salvo.");}
        catch(Exception preferenceError){message("Projeto salvo; não foi possível guardar o caminho para restauração: "+preferenceError.getMessage());}}catch(Exception e){message("Falha ao salvar: "+e.getMessage());}}
    public void addScene(){stop();var s=new Game.Scene();s.id=UUID.randomUUID().toString();s.name="Cena "+(game.scenes.size()+1);game.scenes.add(s);sceneIndex=game.scenes.size()-1;selected=null;selectedHud=null;set("hudSelected",false);set("worldSelected",true);refresh();}
    public void nextScene(){stop();sceneIndex=(sceneIndex+1)%game.scenes.size();selected=null;selectedHud=null;set("hudSelected",false);set("worldSelected",true);refresh();}
    public void setEntry(){game.entrySceneId=game.scenes.get(sceneIndex).id;refresh();}
    public void addObject(){stop();selectedHud=null;selected=new Game.Entity();game.scenes.get(sceneIndex).objects.add(selected);refresh();inspect();}
    public void deleteObject(){stop();if(selectedHud!=null){game.scenes.get(sceneIndex).hud.remove(selectedHud);selectedHud=null;set("hudSelected",false);set("worldSelected",true);refresh();return;}game.scenes.get(sceneIndex).objects.remove(selected);selected=null;refresh();}
    public void togglePlayer(){stop();if(selected!=null){selected.player=!selected.player;inspect();}}
    public void toggleSolid(){stop();if(selected!=null){selected.solid=!selected.solid;inspect();}}
    public void applyObject(){if(selected==null)return;stop();try{var next=selected.copy();next.name=text("name");next.x=Double.parseDouble(text("x"));next.y=Double.parseDouble(text("y"));next.width=Double.parseDouble(text("width"));next.height=Double.parseDouble(text("height"));next.color=text("color");next.speed=Double.parseDouble(text("speed"));Game check=Game.demo();check.scenes.get(0).objects=List.of(next);Codec.validate(check);int index=game.scenes.get(sceneIndex).objects.indexOf(selected);game.scenes.get(sceneIndex).objects.set(index,next);selected=next;refresh();inspect();}catch(Exception e){message("Propriedades inválidas: "+e.getMessage());}}
    public void importSprite(){if(selected==null){message("Selecione um objeto.");return;}if(directory==null){saveProject();if(directory==null)return;}FileChooser chooser=new FileChooser();chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagem","*.png","*.jpg","*.jpeg"));var file=chooser.showOpenDialog(canvas.getScene().getWindow());if(file==null)return;try{String name=UUID.randomUUID()+"-"+file.getName();Files.copy(file.toPath(),directory.resolve("assets").resolve(name));selected.assetPath="assets/"+name;message("Sprite importado. Salve o projeto.");}catch(Exception e){message(e.getMessage());}}
    public void exportAndroid(){
        if(androidBuild!=null&&androidBuild.isAlive()){message("Build Android já está em andamento.");return;}
        saveProject();if(!saved)return;
        Path script=Path.of(System.getProperty("engine.home","."),"scripts/build-android.sh").toAbsolutePath();
        if(!Files.isRegularFile(script)){message("Script de build não encontrado. Configure -Dengine.home com a pasta da engine.");return;}
        try{
            Path log=directory.resolve("build/android-build.log");Files.createDirectories(log.getParent());
            androidBuild=new ProcessBuilder("bash",script.toString(),directory.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            Process process=androidBuild;Path exported=directory.resolve("build/android/game-debug.apk");
            message("Compilando Android… log: "+log);
            Thread worker=new Thread(()->{try{int code=process.waitFor();javafx.application.Platform.runLater(()->{if(!disposed)message(code==0?"APK gerado: "+exported:"Build Android falhou. Consulte "+log);});}catch(InterruptedException e){Thread.currentThread().interrupt();}},"android-export");
            worker.setDaemon(true);worker.start();
        }catch(Exception e){message("Falha ao iniciar build: "+e.getMessage());}
    }
    @Override public void onDestroy(){disposed=true;if(androidBuild!=null&&androidBuild.isAlive())androidBuild.destroy();if(timer!=null)timer.stop();keys.clear();images.clear();}
}
