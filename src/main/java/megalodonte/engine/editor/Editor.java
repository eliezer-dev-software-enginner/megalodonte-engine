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
import javafx.scene.input.MouseButton;
import java.nio.file.*;
import java.util.*;

public final class Editor extends ViewScript {
    private Game game = Game.demo();
    private int sceneIndex;
    private Game.Entity selected;
    private Game.Entity pendingParentChild;
    private Game.Hud selectedHud;
    private Game.TileMap selectedMap;
    private ScriptCompiler scriptCompiler;
    private final ProjectMusic music=new ProjectMusic();
    private long playGeneration;
    private boolean compiling;
    private final Set<String> heldHud=new HashSet<>();
    private double panX,panY;
    private boolean resizingHud;
    private boolean draggingSelection;
    private double hudDragX, hudDragY;
    private Path directory;
    private final RecentProject recentImages = RecentProject.imageDirectoryPreference();
    private Path lastImageDirectory;
    private final RecentProject recentProject = RecentProject.userPreference();
    private Canvas canvas;
    private VBox tree;
    private ListView<Game.Scene> sceneList;
    private boolean updatingSceneList;
    private AnimationTimer timer;
    private megalodonte.engine.Runtime runtime;
    private final Set<KeyCode> keys = new HashSet<>();
    private final PendingAssets pendingAssets = new PendingAssets();
    private final Map<String,Image> images = new HashMap<>();
    private double scale, ox, oy, dragX, dragY;
    private long last;
    private Process androidBuild;
    private boolean disposed;
    private boolean saved;
    private boolean showColliders,previewAnimations;
    private double previewClock;
    private Game.Scene scene() { return runtime == null ? game.scenes.get(sceneIndex) : runtime.scene; }
    @Override public void onSubmit() { applyObject(); }
    @Override public void onMount() {
        Pane host=(Pane)component("viewport").getJavaFxNode();
        canvas=new Canvas(720,480);canvas.getGraphicsContext2D().setImageSmoothing(false); canvas.setManaged(false); host.getChildren().add(canvas);
        canvas.widthProperty().bind(host.widthProperty()); canvas.heightProperty().bind(host.heightProperty());
        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(e -> {keys.add(e.getCode());if(runtime!=null)runtime.setAction(e.getCode().name().toLowerCase(Locale.ROOT),true);if(e.getCode()==KeyCode.ESCAPE){pendingParentChild=null;message("Vínculo cancelado.");}}); canvas.setOnKeyReleased(e -> {keys.remove(e.getCode());if(runtime!=null)runtime.setAction(e.getCode().name().toLowerCase(Locale.ROOT),false);});
        canvas.focusedProperty().addListener((o,a,b)-> { if(!b) { if(runtime!=null){for(var key:keys)runtime.setAction(key.name().toLowerCase(Locale.ROOT),false);for(var action:heldHud)runtime.setAction(action,false);}keys.clear();heldHud.clear();draggingSelection=false; } });
        canvas.setOnMousePressed(e -> {
            draggingSelection=false;
            canvas.requestFocus();
            if(runtime!=null){for(var hud:runtime.scene.hud)if(hud.visible&&HudPresentation.enabled(hud)&&hud.type.equals("button")&&HudLayout.resolve(hud,canvas.getWidth(),canvas.getHeight()).contains(e.getX(),e.getY())){String action=hud.action.isBlank()?hud.id:hud.action;runtime.setAction(action,true);heldHud.add(action);}return;}
            if(e.getButton()==MouseButton.MIDDLE){panX=e.getX();panY=e.getY();return;}
            if(e.getButton()!=MouseButton.PRIMARY && e.getButton()!=MouseButton.SECONDARY)return;
            draggingSelection=e.getButton()==MouseButton.PRIMARY;
            resizingHud=false;
            if(draggingSelection && selectedHud!=null){var b=HudLayout.resolve(selectedHud,canvas.getWidth(),canvas.getHeight());
                if(Math.abs(e.getX()-(b.x()+b.width()))<=10 && Math.abs(e.getY()-(b.y()+b.height()))<=10){resizingHud=true;return;}}
            var previouslySelectedHud=selectedHud;selectedHud=null;
            var hud=scene().hud;
            for(int i=hud.size()-1;i>=0;i--){var h=hud.get(i);if(!h.visible&&h!=previouslySelectedHud)continue;var b=HudLayout.resolve(h,canvas.getWidth(),canvas.getHeight());
                if(b.contains(e.getX(),e.getY())){pendingParentChild=null;selectedHud=h;selected=null;hudDragX=e.getX()-b.x();hudDragY=e.getY()-b.y();inspectHud();return;}}
            set("hudSelected",false);
            double x=(e.getX()-ox)/scale, y=(oy-e.getY())/scale;
            Game.Entity hit=null;
            var objects=scene().objects;
            for(int i=objects.size()-1;i>=0;i--){var obj=objects.get(i);if(!obj.visible&&!showColliders)continue;if(EntityGeometry.contains(Hierarchy.worldEntity(scene(),obj),x,y)){hit=obj;break;}}
            if(hit!=null){boolean linking=pendingParentChild!=null;selectObject(hit);
                if(linking){draggingSelection=false;return;}
                var world=Hierarchy.pose(scene(),selected);dragX=x-world.x();dragY=y-world.y();
            }else if(pendingParentChild!=null){message("Clique no objeto pai na cena ou na lista. Escape cancela.");return;}
            else{selected=null;inspect();}
        });
        canvas.setOnMouseDragged(e -> {
            if(runtime==null&&e.isMiddleButtonDown()&&scale>0){scene().camera.mode="fixed";scene().camera.x-=(e.getX()-panX)/scale;scene().camera.y+=(e.getY()-panY)/scale;panX=e.getX();panY=e.getY();inspectScene();return;}
            if(runtime!=null || !draggingSelection || !e.isPrimaryButtonDown() || e.isSecondaryButtonDown())return;
            if(selectedHud!=null){
                var b=HudLayout.resolve(selectedHud,canvas.getWidth(),canvas.getHeight());
                if(b.scale()<=0)return;
                if(resizingHud){selectedHud.width=Math.max(16,(e.getX()-b.x())/b.scale());selectedHud.height=Math.max(16,(e.getY()-b.y())/b.scale());HudLayout.position(selectedHud,b.x(),b.y(),canvas.getWidth(),canvas.getHeight());}
                else HudLayout.position(selectedHud,e.getX()-hudDragX,e.getY()-hudDragY,canvas.getWidth(),canvas.getHeight());
                inspectHud();
            }else if(selected!=null){Hierarchy.setWorldPosition(scene(),selected,(e.getX()-ox)/scale-dragX,(oy-e.getY())/scale-dragY);inspect();}
        });
        canvas.setOnMouseReleased(e -> { if(runtime!=null)for(String action:heldHud)runtime.setAction(action,false);heldHud.clear();draggingSelection=false; resizingHud=false; });
        canvas.setOnScroll(e->{if(runtime==null){scene().camera.zoom=Math.max(0.05,Math.min(20,scene().camera.zoom*Math.exp(e.getDeltaY()*0.002)));inspectScene();}});
        sceneList=new ListView<>();sceneList.setId("sceneSelector");sceneList.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        sceneList.setCellFactory(list->new ListCell<>(){
            @Override protected void updateItem(Game.Scene item,boolean empty){super.updateItem(item,empty);setText(empty||item==null?null:(getIndex()+1)+". "+item.name+(item.id.equals(game.entrySceneId)?" • inicial":""));}
        });
        var duplicate=new MenuItem("Duplicar cena");duplicate.setOnAction(e->duplicateScene());var remove=new MenuItem("Excluir cena");remove.setOnAction(e->removeScene());
        var up=new MenuItem("Mover cena para cima");up.setOnAction(e->sceneUp());var down=new MenuItem("Mover cena para baixo");down.setOnAction(e->sceneDown());
        var options=new MenuItem("Configurar cena");options.setOnAction(e->editSceneFeatures());sceneList.setContextMenu(new ContextMenu(options,duplicate,remove,new SeparatorMenuItem(),up,down));
        sceneList.setTooltip(new Tooltip("Clique para selecionar. Botão direito: configurar, duplicar, excluir ou ordenar cenas."));
        sceneList.getSelectionModel().selectedIndexProperty().addListener((o,previous,index)->{if(!updatingSceneList&&index.intValue()>=0)selectScene(index.intValue());});
        ((Pane)component("sceneList").getJavaFxNode()).getChildren().add(sceneList);
        tree=new VBox(6);
        ScrollPane objectsScroll=new ScrollPane(tree);
        objectsScroll.setFitToWidth(true);
        objectsScroll.setMinSize(0,0);
        objectsScroll.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        VBox.setVgrow(objectsScroll,Priority.ALWAYS);
        ((Pane)component("tree").getJavaFxNode()).getChildren().add(objectsScroll);
        timer=new AnimationTimer(){ public void handle(long now){
            double dt=last==0?0:(now-last)/1e9; last=now;if(runtime==null&&previewAnimations)previewClock+=Math.min(dt,.1);
            if(runtime!=null)try{runtime.update(dt,axis(KeyCode.D,KeyCode.RIGHT)-axis(KeyCode.A,KeyCode.LEFT),axis(KeyCode.W,KeyCode.UP)-axis(KeyCode.S,KeyCode.DOWN));music.update(runtime.scene,path->pendingAssets.resolve(path,directory),Editor.this::message);}catch(Exception error){stop();message("Script/runtime: "+error.getMessage());}
            draw();
        }}; timer.start(); refresh(); restoreLastSaved();inspectScene();
    }
    private int axis(KeyCode a,KeyCode b){return keys.contains(a)||keys.contains(b)?1:0;}
    private void draw(){
        var s=scene(); var g=canvas.getGraphicsContext2D();
        g.setFill(Color.web("#0c1420"));g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        var view=CameraView.of(s.camera,canvas.getWidth(),canvas.getHeight());scale=view.scale();ox=view.originX();oy=view.originY();
        g.save();g.beginPath();g.rect(view.screenX(view.left()),view.screenY(view.bottom()+view.height()),view.width()*scale,view.height()*scale);g.clip();
        g.setFill(Color.web(s.background));g.fillRect(ox,oy-s.worldHeight*scale,s.worldWidth*scale,s.worldHeight*scale);
        if(runtime==null){g.setStroke(Color.web("#ffffff18")); for(int i=(int)Math.max(0,Math.floor(view.left()));i<=Math.min(s.worldWidth,view.left()+view.width());i++)g.strokeLine(ox+i*scale,oy,ox+i*scale,oy-s.worldHeight*scale);for(int i=(int)Math.max(0,Math.floor(view.bottom()));i<=Math.min(s.worldHeight,view.bottom()+view.height());i++)g.strokeLine(ox,oy-i*scale,ox+s.worldWidth*scale,oy-i*scale);}
        TileDraw.render(s,view,(tile,x,y,w,h,alpha,a,b,c,d)->{Image image=assetImage(tile.assetPath);if(image==null||image.isError())return;g.save();g.setGlobalAlpha(alpha);g.translate(x+w/2,y+h/2);g.scale(w,h);g.transform(a,b,c,d,0,0);g.drawImage(image,tile.sourceX,tile.sourceY,tile.width,tile.height,-0.5,-0.5,1,1);g.restore();});
        if(runtime==null){for(var map:s.tileMaps){g.setStroke(Color.web("#ffcc66"));g.strokeRect(ox+map.x*scale,oy-(map.y+map.rows*map.tileHeight*map.unitScale)*scale,map.columns*map.tileWidth*map.unitScale*scale,map.rows*map.tileHeight*map.unitScale*scale);}}
        for(var e:s.objects){if(!e.visible&&e!=selected)continue;var pose=Hierarchy.pose(s,e);double x=ox+pose.x()*scale,y=oy-(pose.y()+e.height)*scale;
            Image image=null;
            String sprite=Sprites.asset(e,runtime==null&&previewAnimations?e.animationTime+previewClock:e.animationTime);if(!sprite.isBlank())image=assetImage(sprite);
            g.save();if(!e.visible)g.setGlobalAlpha(.35);double cx=x+e.width*scale/2,cy=y+e.height*scale/2;
            g.translate(cx,cy);g.rotate(pose.rotation()%360);if(e.flipX)g.scale(-1,1);g.translate(-cx,-cy);
            if(image!=null&&!image.isError())g.drawImage(image,x,y,e.width*scale,e.height*scale);
            else{g.setFill(Color.web(e.color));g.fillRect(x,y,e.width*scale,e.height*scale);}
            if(runtime==null&&e==selected){g.setStroke(Color.CYAN);g.setLineWidth(2);g.strokeRect(x,y,e.width*scale,e.height*scale);}
            g.restore();
        }
        if(runtime==null&&(showColliders||selected!=null)){
            g.save();g.setStroke(Color.web("#ffcc66"));g.setLineDashes(4,3);
            for(var e:s.objects)if((showColliders&&e.solid)||e==selected){var b=megalodonte.engine.Runtime.collisionBounds(s,e);g.strokeRect(ox+b.x()*scale,oy-(b.y()+b.height())*scale,b.width()*scale,b.height()*scale);}
            if(showColliders)for(var map:s.tileMaps)for(var b:map.obstacles)g.strokeRect(ox+(map.x+b.x*map.unitScale)*scale,oy-(map.y+(b.y+b.height)*map.unitScale)*scale,b.width*map.unitScale*scale,b.height*map.unitScale*scale);
            g.restore();
        }
        if(runtime==null){
            g.save();g.setStroke(Color.web("#65caff"));g.setLineWidth(1.5);g.setLineDashes(5,4);
            for(var child:s.objects)if(!child.parentId.isEmpty()){
                var parent=Hierarchy.find(s,child.parentId);var c=Hierarchy.pose(s,child);var a=Hierarchy.pose(s,parent);
                g.strokeLine(ox+(c.x()+child.width/2)*scale,oy-(c.y()+child.height/2)*scale,
                    ox+(a.x()+parent.width/2)*scale,oy-(a.y()+parent.height/2)*scale);
            }g.restore();
        }
        g.restore();
        drawHud();
        if(runtime!=null&&runtime.fadeAlpha()>0){g.setFill(Color.color(0,0,0,runtime.fadeAlpha()));g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());}
    }
    private void drawHud() {
        var g=canvas.getGraphicsContext2D();
        for(var h:scene().hud){if(!h.visible&&h!=selectedHud)continue;var b=HudLayout.resolve(h,canvas.getWidth(),canvas.getHeight());if(b.scale()<=0)continue;
            g.save();if(!h.visible)g.setGlobalAlpha(.35);g.beginPath();g.rect(b.x(),b.y(),b.width(),b.height());g.clip();
            if(h.type.equals("button")&&HudPresentation.asset(h).isBlank()){g.setFill(Color.web("#245878"));g.fillRoundRect(b.x(),b.y(),b.width(),b.height(),8,8);}
            if(h.type.equals("button")&&!HudPresentation.asset(h).isBlank()){var buttonImage=assetImage(HudPresentation.asset(h));if(buttonImage!=null)g.drawImage(buttonImage,b.x(),b.y(),b.width(),b.height());}
            g.setFill(Color.web(h.color));
            if(h.type.equals("text")||h.type.equals("button")){
                g.setFont(javafx.scene.text.Font.font(h.fontSize*b.scale()));g.setTextBaseline(javafx.geometry.VPos.TOP);
                g.fillText(h.text,b.x(),b.y());
            }else{Image image=null;if(!HudPresentation.asset(h).isBlank())image=assetImage(HudPresentation.asset(h));
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
        set("hudReference","Referência: "+(h.referenceArea.equals("content")?"área do conteúdo":"tela segura"));set("hudAction",h.action);set("hudScript",h.scriptClassName.isBlank()?"Script: nenhum":"Script: "+h.scriptClassName);set("hudAnchors",h.horizontalAnchor+" / "+h.verticalAnchor);message("HUD selecionada: "+h.type+" • arraste a alça para redimensionar");
    }
    public void addHudButton(){addHud("button");selectedHud.text="Botão";selectedHud.action="jump";inspectHud();}
    public void addHudText(){addHud("text");}
    public void addHudImage(){
        String asset=chooseImage("Adicionar imagem à HUD");if(asset==null)return;
        addHud("image");selectedHud.assetPath=asset;refresh();message("Imagem da HUD importada. Salve o projeto.");
    }
    private void addHud(String type){stop();pendingParentChild=null;selected=null;selectedHud=new Game.Hud();selectedHud.type=type;selectedHud.referenceArea=game.scenes.get(sceneIndex).hud.stream().findFirst().map(h->h.referenceArea).orElse("viewport");
        if(type.equals("image")){selectedHud.width=128;selectedHud.height=128;}
        game.scenes.get(sceneIndex).hud.add(selectedHud);refresh();inspectHud();}
    public void applyHud(){commitHudInspector();}
    private boolean commitHudInspector(){if(selectedHud==null)return true;stop();try{
        var h=selectedHud.copy();h.id=text("hudId");h.text=text("hudText");h.offsetX=Double.parseDouble(text("hudX"));h.offsetY=Double.parseDouble(text("hudY"));
        h.width=Double.parseDouble(text("hudWidth"));h.height=Double.parseDouble(text("hudHeight"));h.fontSize=Double.parseDouble(text("hudFont"));h.color=text("hudColor");h.action=text("hudAction");
        Game copy=Codec.copy(game);int index=game.scenes.get(sceneIndex).hud.indexOf(selectedHud);copy.scenes.get(sceneIndex).hud.set(index,h);Codec.validate(copy);
        game.scenes.get(sceneIndex).hud.set(index,h);selectedHud=h;refresh();inspectHud();return true;
    }catch(Exception e){message("HUD inválida: "+e.getMessage());return false;}}
    private void changeHudAnchor(String axis){
        if(selectedHud==null||!commitHudInspector())return;
        var box=HudLayout.resolve(selectedHud,canvas.getWidth(),canvas.getHeight());
        switch(axis){
            case "horizontal"->selectedHud.horizontalAnchor=switch(selectedHud.horizontalAnchor){case "left"->"center";case "center"->"right";default->"left";};
            case "vertical"->selectedHud.verticalAnchor=switch(selectedHud.verticalAnchor){case "top"->"center";case "center"->"bottom";default->"top";};
            default->selectedHud.referenceArea=selectedHud.referenceArea.equals("content")?"viewport":"content";
        }
        HudLayout.position(selectedHud,box.x(),box.y(),canvas.getWidth(),canvas.getHeight());inspectHud();draw();
    }
    public void cycleHudHorizontal(){changeHudAnchor("horizontal");}
    public void cycleHudVertical(){changeHudAnchor("vertical");}
    public void cycleHudReference(){changeHudAnchor("reference");}
    public void importHudImage(){
        if(selectedHud==null||!(selectedHud.type.equals("image")||selectedHud.type.equals("button"))){message("Selecione uma imagem da HUD.");return;}
        String asset=chooseImage("Trocar imagem da HUD");if(asset==null)return;
        stop();selectedHud.assetPath=asset;refresh();message("Imagem da HUD importada. Salve o projeto.");
    }
    private Image assetImage(String asset){
        return images.computeIfAbsent(asset,path->{Path source=pendingAssets.resolve(path,directory);
            if(source==null)return null;
            try{return new Image(source.toUri().toString());}catch(Exception e){return null;}});
    }
    private String chooseImage(String title){
        FileChooser chooser=new FileChooser();chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens PNG/JPEG","*.png","*.jpg","*.jpeg","*.PNG","*.JPG","*.JPEG"));
        try{Path remembered=recentImages.lastSaved();if(remembered!=null)lastImageDirectory=remembered;}catch(Exception ignored){}
        if(lastImageDirectory!=null && Files.isDirectory(lastImageDirectory))chooser.setInitialDirectory(lastImageDirectory.toFile());
        else if(directory!=null && Files.isDirectory(directory.resolve("assets")))chooser.setInitialDirectory(directory.resolve("assets").toFile());
        var file=chooser.showOpenDialog(canvas.getScene().getWindow());if(file==null)return null;
        try{
            Image decoded=new Image(file.toURI().toString());
            if(decoded.isError() || decoded.getWidth()<=0 || decoded.getHeight()<=0)throw new IllegalArgumentException("Arquivo de imagem inválido");
            String asset=pendingAssets.stage(file.toPath());images.put(asset,decoded);
            lastImageDirectory=file.toPath().toAbsolutePath().getParent();
            try{recentImages.rememberSaved(lastImageDirectory);}catch(Exception ignored){/* Keep the directory for this session if disk preferences fail. */}
            return asset;
        }catch(Exception e){message("Falha ao importar imagem: "+e.getMessage());return null;}
    }
    private void inspect(){set("hudSelected",false);set("worldSelected",true);if(selected==null)return;set("name",selected.name);set("x",""+selected.x);set("y",""+selected.y);set("width",""+selected.width);set("height",""+selected.height);set("color",selected.color);set("speed",""+selected.speed);set("rotation",""+selected.rotation);set("objectScript",selected.scriptClassName.isBlank()?"Script: nenhum":"Script: "+selected.scriptClassName);set("parentName",selected.parentId.isEmpty()?"Pai: nenhum":"Pai: "+Hierarchy.find(scene(),selected.parentId).name);message("Selecionado: "+selected.name+" • controlável="+selected.player+" • colisor="+selected.solid);}
    private void refresh(){refreshSceneList();draw();tree.getChildren().clear();for(var e:game.scenes.get(sceneIndex).objects){Button b=new Button((e.parentId.isEmpty()?"":"↳ ")+e.name);b.setMaxWidth(Double.MAX_VALUE);b.setOnAction(event->selectObject(e));tree.getChildren().add(b);}for(var map:game.scenes.get(sceneIndex).tileMaps){Button b=new Button("Mapa • "+map.name);b.setOnAction(e->{stop();selected=null;selectedHud=null;set("hudSelected",false);set("worldSelected",true);selectedMap=map;set("mapSelected",true);set("mapX",""+map.x);set("mapY",""+map.y);set("mapScale",""+map.unitScale);message("Mapa selecionado: "+map.name);});tree.getChildren().add(b);}for(var h:game.scenes.get(sceneIndex).hud){Button b=new Button("HUD • "+(h.type.equals("text")?h.text:h.id));b.setOnAction(event->{stop();pendingParentChild=null;selected=null;selectedHud=h;inspectHud();});tree.getChildren().add(b);}set("projectPath",directory==null?"Projeto: ainda não salvo":"Projeto: "+directory.resolve("game.json").toAbsolutePath().normalize());set("project",game.name+" • "+game.scenes.get(sceneIndex).name+" • inicial: "+game.entrySceneId+" • "+(directory==null?"não salvo":directory));}
    private void stop(){playGeneration++;compiling=false;if(runtime!=null)runtime.close();runtime=null;if(scriptCompiler!=null){scriptCompiler.close();scriptCompiler=null;}music.close();music.pause(false);music.mute(false);keys.clear();heldHud.clear();last=0;}
    public void togglePlay(){
        pendingParentChild=null;
        if(runtime!=null||compiling){stop();message("Play encerrado; edição preservada.");return;}
        if(!commitScene()||!commitMapInspector()||!commitObjectInspector()||!commitHudInspector())return;
        Game snapshot=Codec.copy(game);Path project=directory;
        if(project==null){try{startPlay(snapshot,null);}catch(Exception e){stop();message(e.getMessage());}return;}
        compiling=true;long generation=++playGeneration;message("Compilando scripts…");
        Thread worker=new Thread(()->{
            ScriptCompiler compiler=null;
            try{compiler=ScriptCompiler.compile(project);ScriptCompiler result=compiler;
                javafx.application.Platform.runLater(()->{if(disposed||generation!=playGeneration){result.close();return;}compiling=false;try{startPlay(snapshot,result);}catch(Exception e){result.close();stop();message(e.getMessage());}});
            }catch(Exception e){if(compiler!=null)compiler.close();javafx.application.Platform.runLater(()->{if(!disposed&&generation==playGeneration){compiling=false;message("Compilação falhou: "+e.getMessage());}});}
        },"game-scripts");worker.setDaemon(true);worker.start();
    }
    private void startPlay(Game snapshot,ScriptCompiler compiler){
        scriptCompiler=compiler;
        ClassLoader loader=compiler==null?getClass().getClassLoader():compiler.loader;
        var playerState=new PlayerState(snapshot.applicationId+"/editor/"+directory);
        runtime=new megalodonte.engine.Runtime(snapshot,megalodonte.engine.Runtime.reflection(loader),new megalodonte.engine.Runtime.Events(){
            public void log(String text){javafx.application.Platform.runLater(()->{if(!disposed)message(text);});}
            public void playSound(String path){music.sound(pendingAssets.resolve(path,directory));}
            public void pauseAudio(boolean paused){music.pause(paused);}
            public void quit(){javafx.application.Platform.runLater(()->stop());}
            public java.util.Map<String,String> loadState(){return playerState.load();}
            public void saveState(java.util.Map<String,String> state){playerState.save(state);}
            public void muteAudio(boolean muted){music.mute(muted);}
        });canvas.requestFocus();message("Play: cena inicial • WASD/setas e ações HUD.");
    }
    public void newProject(){stop();pendingParentChild=null;game=Game.demo();sceneIndex=0;directory=null;selected=null;selectedHud=null;set("hudSelected",false);set("worldSelected",true);pendingAssets.close();images.clear();refresh();inspectScene();}
    public void openProject(){
        FileChooser chooser=new FileChooser();
        chooser.setTitle("Abrir projeto — game.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Projeto Megalodonte (game.json)","*.json"));
        if(directory!=null && Files.isDirectory(directory))chooser.setInitialDirectory(directory.toFile());
        var file=chooser.showOpenDialog(canvas.getScene().getWindow());if(file==null)return;
        try{loadProject(file.toPath());
            try{recentProject.rememberSaved(directory.resolve("game.json"));message("Projeto carregado.");}
            catch(Exception e){message("Projeto carregado; não foi possível guardar o caminho: "+e.getMessage());}
        }
        catch(Exception e){message("Falha ao abrir: "+e.getMessage());}
    }
    private void loadProject(Path file) throws java.io.IOException {
        Path target=file.toAbsolutePath().normalize();
        if(!target.getFileName().toString().equals("game.json"))throw new IllegalArgumentException("Selecione o arquivo game.json do projeto");
        // Validate before replacing the active document.
        Game next=Codec.read(Files.readString(target));
        stop();pendingParentChild=null;game=next;directory=target.getParent();sceneIndex=0;
        String edited=game.lastEditedSceneId==null||game.lastEditedSceneId.isBlank()?game.entrySceneId:game.lastEditedSceneId;
        for(int i=0;i<game.scenes.size();i++)if(game.scenes.get(i).id.equals(edited)){sceneIndex=i;break;}
        selected=null;selectedHud=null;
        set("hudSelected",false);set("worldSelected",true);pendingAssets.close();images.clear();refresh();inspectScene();
    }
    private void restoreLastSaved(){
        try{Path file=recentProject.lastSaved();if(file==null)return;
            loadProject(file);message("Último projeto salvo restaurado.");
        }catch(Exception e){message("Não foi possível restaurar o último projeto salvo: "+e.getMessage()+". Use Abrir para localizá-lo.");}
    }
    public void saveProject(){saved=false;
        if(!commitScene()||!commitMapInspector()||!commitObjectInspector() || !commitHudInspector())return;
        try{game.lastEditedSceneId=game.scenes.get(sceneIndex).id;Codec.validate(game);if(directory==null){
            FileChooser chooser=new FileChooser();
            chooser.setTitle("Salvar projeto — game.json");
            chooser.setInitialFileName("game.json");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Projeto Megalodonte (game.json)","*.json"));
            var file=chooser.showSaveDialog(canvas.getScene().getWindow());if(file==null)return;
            Path target=file.toPath().toAbsolutePath();
            if(!target.getFileName().toString().equals("game.json"))throw new IllegalArgumentException("Salve com o nome game.json na pasta do jogo");
            directory=target.getParent();
        }pendingAssets.publish(directory);Path temp=Files.createTempFile(directory,"game-",".tmp");try{Files.writeString(temp,Codec.GSON.toJson(game));try{Files.move(temp,directory.resolve("game.json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,directory.resolve("game.json"),StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}saved=true;pendingAssets.close();refresh();
        try{recentProject.rememberSaved(directory.resolve("game.json"));message("Projeto salvo.");}
        catch(Exception preferenceError){message("Projeto salvo; não foi possível guardar o caminho para restauração: "+preferenceError.getMessage());}}catch(Exception e){message("Falha ao salvar: "+e.getMessage());}}
    public void addScene(){stop();pendingParentChild=null;var s=new Game.Scene();s.id=UUID.randomUUID().toString();s.name="Cena "+(game.scenes.size()+1);game.scenes.add(s);sceneIndex=game.scenes.size()-1;selected=null;selectedHud=null;set("hudSelected",false);set("worldSelected",true);refresh();inspectScene();}
    private void refreshSceneList(){
        if(sceneList==null)return;updatingSceneList=true;
        try{if(!sceneList.getItems().equals(game.scenes))sceneList.getItems().setAll(game.scenes);sceneList.getSelectionModel().select(sceneIndex);sceneList.refresh();}
        finally{updatingSceneList=false;}
    }
    private void selectScene(int index){
        if(index==sceneIndex)return;
        stop();if(!commitScene()||!commitMapInspector()||!commitObjectInspector()||!commitHudInspector()){refreshSceneList();return;}
        pendingParentChild=null;sceneIndex=index;selected=null;selectedHud=null;selectedMap=null;set("mapSelected",false);set("hudSelected",false);set("worldSelected",true);refresh();inspectScene();sceneList.scrollTo(index);
    }
    public void nextScene(){selectScene((sceneIndex+1)%game.scenes.size());}
    public void setEntry(){game.entrySceneId=game.scenes.get(sceneIndex).id;refresh();}
    public void addObject(){stop();pendingParentChild=null;selectedHud=null;selected=new Game.Entity();game.scenes.get(sceneIndex).objects.add(selected);refresh();inspect();}
    public void deleteObject(){stop();if(selectedHud!=null){game.scenes.get(sceneIndex).hud.remove(selectedHud);selectedHud=null;set("hudSelected",false);set("worldSelected",true);refresh();return;}if(selected!=null&&scene().camera.targetId.equals(selected.id)){scene().camera.targetId="";scene().camera.mode="fixed";inspectScene();}if(selected!=null)Hierarchy.remove(game.scenes.get(sceneIndex),selected);selected=null;pendingParentChild=null;refresh();}
    private void selectObject(Game.Entity target){
        stop();selectedHud=null;
        if(pendingParentChild!=null){
            var child=pendingParentChild;
            try{Hierarchy.attach(scene(),child,target);pendingParentChild=null;selected=child;refresh();inspect();message("Objeto vinculado a "+target.name+".");}
            catch(Exception e){message(e.getMessage()+". Escolha outro pai ou pressione Escape.");}
        }else{selected=target;inspect();}
    }
    public void duplicateObject(){
        if(selected==null){message("Selecione um objeto da cena para duplicar.");return;}
        stop();pendingParentChild=null;selected=Hierarchy.duplicate(scene(),selected);refresh();inspect();
    }
    public void chooseParent(){
        if(selected==null){message("Selecione o objeto filho primeiro.");return;}
        stop();pendingParentChild=selected;message("Clique no objeto pai na cena ou na lista. Escape cancela.");
    }
    public void detachParent(){
        if(selected==null)return;stop();pendingParentChild=null;Hierarchy.attach(scene(),selected,null);refresh();inspect();
    }
    public void togglePlayer(){stop();if(selected!=null){selected.player=!selected.player;inspect();}}
    public void toggleSolid(){stop();if(selected!=null){selected.solid=!selected.solid;inspect();}}
    public void browseAssets(){stop();AssetBrowser.show(owner(),pendingAssets,directory);}
    public void duplicateScene(){
        stop();if(!commitScene()||!commitObjectInspector()||!commitHudInspector()||!commitMapInspector())return;
        var source=game.scenes.get(sceneIndex);var copy=Codec.GSON.fromJson(Codec.GSON.toJson(source),Game.Scene.class);copy.id=UUID.randomUUID().toString();copy.name=source.name+" (cópia)";game.scenes.add(sceneIndex+1,copy);sceneIndex++;clearSelection();refresh();inspectScene();message("Cena duplicada; os scripts foram mantidos. Revise seus destinos de navegação.");
    }
    public void removeScene(){
        if(game.scenes.size()==1){message("O projeto precisa de ao menos uma cena.");return;}
        stop();var scene=game.scenes.get(sceneIndex);game.scenes.remove(sceneIndex);sceneIndex=Math.min(sceneIndex,game.scenes.size()-1);if(game.entrySceneId.equals(scene.id))game.entrySceneId=game.scenes.get(sceneIndex).id;clearSelection();refresh();inspectScene();message("Cena removida. Revise referências nos scripts antes de salvar.");
    }
    public void sceneUp(){moveScene(-1);}
    public void sceneDown(){moveScene(1);}
    private void moveScene(int delta){int next=sceneIndex+delta;if(next<0||next>=game.scenes.size())return;stop();Collections.swap(game.scenes,sceneIndex,next);sceneIndex=next;refresh();inspectScene();}
    private void clearSelection(){selected=null;selectedHud=null;selectedMap=null;pendingParentChild=null;set("hudSelected",false);set("worldSelected",true);set("mapSelected",false);}
    private Window owner(){return canvas.getScene().getWindow();}
    private void acceptEntity(Game.Entity value){
        int i=game.scenes.get(sceneIndex).objects.indexOf(selected);if(i<0)throw new IllegalStateException("Objeto não está mais nesta cena");
        var copy=DocumentEdits.entity(game,sceneIndex,i,value);var target=copy.scenes.get(sceneIndex);game.scenes.set(sceneIndex,target);selected=target.objects.get(i);selectedMap=null;set("mapSelected",false);set("cameraMode","Câmera: "+target.camera.mode+" • alvo: "+target.camera.targetId);refresh();inspect();
    }
    public void editAnimations(){if(selected==null){message("Selecione um objeto.");return;}if(!commitObjectInspector())return;new AnimationEditor(selected,this::acceptEntity,pendingAssets,directory,owner()).show();}
    public void editObjectFeatures(){if(selected==null){message("Selecione um objeto.");return;}if(!commitObjectInspector())return;FeatureForms.entity(owner(),selected,this::acceptEntity);}
    public void editProjectFeatures(){stop();FeatureForms.project(owner(),game,value->{game=value;clearSelection();refresh();inspectScene();});}
    public void editSceneFeatures(){stop();if(!commitScene())return;FeatureForms.scene(owner(),game.scenes.get(sceneIndex),value->{var copy=DocumentEdits.scene(game,sceneIndex,value);game=copy;clearSelection();refresh();inspectScene();});}
    public void editHudFeatures(){if(selectedHud==null){message("Selecione uma HUD.");return;}if(!commitHudInspector())return;FeatureForms.hud(owner(),selectedHud,value->{Game copy=Codec.copy(game);int i=game.scenes.get(sceneIndex).hud.indexOf(selectedHud);copy.scenes.get(sceneIndex).hud.set(i,value);Codec.validate(copy);game.scenes.get(sceneIndex).hud.set(i,value);selectedHud=value;refresh();inspectHud();});}
    public void editMapFeatures(){if(selectedMap==null){message("Selecione um mapa na lista.");return;}if(!commitMapInspector())return;new TileMapEditor(selectedMap,value->{Game copy=Codec.copy(game);int i=game.scenes.get(sceneIndex).tileMaps.indexOf(selectedMap);copy.scenes.get(sceneIndex).tileMaps.set(i,value);Codec.validate(copy);game.scenes.get(sceneIndex).tileMaps.set(i,value);selectedMap=value;refresh();},pendingAssets,directory,owner()).show();}
    public void createTileMap(){stop();var map=new Game.TileMap();map.name="Mapa "+(scene().tileMaps.size()+1);map.columns=20;map.rows=18;map.tileWidth=map.tileHeight=24;map.unitScale=1.0/24;var layer=new Game.TileLayer();layer.name="Camada 1";layer.data=new long[map.columns*map.rows];map.layers.add(layer);scene().tileMaps.add(map);scene().worldWidth=Math.max(scene().worldWidth,map.columns*map.tileWidth*map.unitScale);scene().worldHeight=Math.max(scene().worldHeight,map.rows*map.tileHeight*map.unitScale);set("sceneWidth",""+scene().worldWidth);set("sceneHeight",""+scene().worldHeight);selectedMap=map;selected=null;selectedHud=null;set("hudSelected",false);set("mapSelected",true);set("mapX","0");set("mapY","0");set("mapScale",""+map.unitScale);refresh();editMapFeatures();}
    public void toggleAnimationPreview(){if(runtime!=null)stop();previewAnimations=!previewAnimations;previewClock=0;message("Prévia de animações na cena: "+(previewAnimations?"ativa (sem scripts)":"pausada"));}
    public void toggleColliders(){showColliders=!showColliders;draw();message("Colisores: "+(showColliders?"visíveis":"ocultos"));}
    public void duplicateHud(){if(selectedHud==null)return;if(!commitHudInspector())return;var value=selectedHud.copy();if(value.action.isBlank())value.action=selectedHud.id;value.id=UUID.randomUUID().toString();value.offsetX+=24;value.offsetY+=24;game.scenes.get(sceneIndex).hud.add(value);selectedHud=value;refresh();inspectHud();}
    public void moveForward(){moveLayerOrder(true);}
    public void moveBackward(){moveLayerOrder(false);}
    private void moveLayerOrder(boolean forward){stop();if(selectedHud!=null){var items=game.scenes.get(sceneIndex).hud;int i=items.indexOf(selectedHud),next=forward?i+1:i-1;if(i>=0&&next>=0&&next<items.size())Collections.swap(items,i,next);}else if(selected!=null){var items=game.scenes.get(sceneIndex).objects;int i=items.indexOf(selected),next=forward?i+1:i-1;if(i>=0&&next>=0&&next<items.size())Collections.swap(items,i,next);}refresh();}
    public void removeSprite(){if(selected==null)return;stop();selected.assetPath="";refresh();}
    public void applyObject(){commitObjectInspector();}
    private boolean commitObjectInspector(){if(selected==null)return true;stop();try{var next=selected.copy();next.name=text("name");next.x=Double.parseDouble(text("x"));next.y=Double.parseDouble(text("y"));next.width=Double.parseDouble(text("width"));next.height=Double.parseDouble(text("height"));next.color=text("color");next.speed=Double.parseDouble(text("speed"));next.rotation=EntityGeometry.normalize(Double.parseDouble(text("rotation")));Game check=Codec.copy(game);int index=game.scenes.get(sceneIndex).objects.indexOf(selected);check.scenes.get(sceneIndex).objects.set(index,next);Codec.validate(check);game.scenes.get(sceneIndex).objects.set(index,next);selected=next;pendingParentChild=null;refresh();inspect();return true;}catch(Exception e){message("Propriedades inválidas: "+e.getMessage());return false;}}
    public void importSprite(){
        if(selected==null){message("Selecione um objeto.");return;}
        String asset=chooseImage("Importar sprite para o objeto");if(asset==null)return;
        stop();selected.assetPath=asset;refresh();message("Sprite importado. Salve o projeto.");
    }
    private void inspectScene(){
        selectedMap=null;set("mapSelected",false);var s=game.scenes.get(sceneIndex);var c=s.camera;
        set("sceneWidth",""+s.worldWidth);set("sceneHeight",""+s.worldHeight);set("cameraX",""+c.x);set("cameraY",""+c.y);set("cameraWidth",""+c.width);set("cameraHeight",""+c.height);set("cameraZoom",""+c.zoom);set("cameraSpeed",""+c.followSpeed);set("cameraFade",""+c.fadeSeconds);set("cameraDeadX",""+c.deadZoneWidth);set("cameraDeadY",""+c.deadZoneHeight);set("musicVolume",""+s.musicVolume);
        set("cameraMode","Câmera: "+c.mode+" • alvo: "+c.targetId);set("musicPath",s.musicAssetPath.isBlank()?"Música: nenhuma":"Música: "+s.musicAssetPath);set("sceneScript",s.scriptClassName.isBlank()?"Script de cena: nenhum":"Script de cena: "+s.scriptClassName);
    }
    public void toggleSceneSettings(){set("sceneSettings",!Boolean.TRUE.equals(get("sceneSettings")));}
    public void applyScene(){stop();commitScene();}
    private boolean commitScene(){try{
        var s=game.scenes.get(sceneIndex);var copy=Codec.copy(game);var target=copy.scenes.get(sceneIndex);var c=target.camera;
        target.worldWidth=Double.parseDouble(text("sceneWidth"));target.worldHeight=Double.parseDouble(text("sceneHeight"));
        c.x=Double.parseDouble(text("cameraX"));c.y=Double.parseDouble(text("cameraY"));c.width=Double.parseDouble(text("cameraWidth"));c.height=Double.parseDouble(text("cameraHeight"));c.zoom=Double.parseDouble(text("cameraZoom"));c.followSpeed=Double.parseDouble(text("cameraSpeed"));c.fadeSeconds=Double.parseDouble(text("cameraFade"));c.deadZoneWidth=Double.parseDouble(text("cameraDeadX"));c.deadZoneHeight=Double.parseDouble(text("cameraDeadY"));target.musicVolume=Double.parseDouble(text("musicVolume"));Codec.validate(copy);
        s.worldWidth=target.worldWidth;s.worldHeight=target.worldHeight;s.camera=c;s.musicVolume=target.musicVolume;return true;
    }catch(Exception e){message("Configuração da cena inválida: "+e.getMessage());return false;}}
    public void followSelected(){if(selected==null){message("Selecione o objeto que a câmera seguirá.");return;}stop();scene().camera.mode="follow";scene().camera.targetId=selected.id;inspectScene();}
    public void fixedCamera(){stop();scene().camera.mode="fixed";scene().camera.targetId="";inspectScene();}
    public void toggleCameraClamp(){stop();scene().camera.clampToWorld=!scene().camera.clampToWorld;message("Limitar câmera ao mundo: "+scene().camera.clampToWorld);}
    public void importTileMap(){
        var chooser=new FileChooser();chooser.setTitle("Importar mapa Tiled");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tiled TMX/TMJ","*.tmx","*.tmj"));var file=chooser.showOpenDialog(canvas.getScene().getWindow());if(file==null)return;
        try{var importer=new TiledImporter(pendingAssets::stage);var map=importer.read(file.toPath());stop();scene().tileMaps.add(map);scene().worldWidth=Math.max(scene().worldWidth,map.columns*map.tileWidth*map.unitScale);scene().worldHeight=Math.max(scene().worldHeight,map.rows*map.tileHeight*map.unitScale);refresh();inspectScene();message("Mapa importado. "+String.join("; ",new LinkedHashSet<>(importer.warnings)));}
        catch(Exception e){message("Falha ao importar mapa: "+e.getMessage());}
    }
    public void applyMap(){commitMapInspector();}
    private boolean commitMapInspector(){if(selectedMap==null)return true;stop();try{double x=Double.parseDouble(text("mapX")),y=Double.parseDouble(text("mapY")),unit=Double.parseDouble(text("mapScale"));if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(unit)||unit<=0)throw new IllegalArgumentException("Posição/escala inválida");selectedMap.x=x;selectedMap.y=y;selectedMap.unitScale=unit;refresh();return true;}catch(Exception e){message(e.getMessage());return false;}}
    public void removeMap(){if(selectedMap==null)return;stop();scene().tileMaps.remove(selectedMap);selectedMap=null;set("mapSelected",false);refresh();}
    public void importMusic(){var chooser=new FileChooser();chooser.setTitle("Música da cena");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Música WAV/MP3","*.wav","*.mp3","*.WAV","*.MP3"));var file=chooser.showOpenDialog(canvas.getScene().getWindow());if(file==null)return;try{String path=pendingAssets.stage(file.toPath());stop();scene().musicAssetPath=path;inspectScene();message("Música importada. Salve o projeto.");}catch(Exception e){message(e.getMessage());}}
    public void removeMusic(){stop();scene().musicAssetPath="";inspectScene();}
    public void toggleMusicLoop(){stop();scene().musicLoop=!scene().musicLoop;message("Repetir música: "+scene().musicLoop);}
    private boolean ensureProject(){if(directory==null){saveProject();if(!saved)return false;}return true;}
    public void editScripts(){if(!ensureProject())return;stop();openScriptEditor(selected,selectedHud,scene());}
    public void editSceneScript(){if(!ensureProject())return;stop();openScriptEditor(null,null,scene());}
    private void openScriptEditor(Game.Entity object,Game.Hud hud,Game.Scene targetScene){
        var targetGame=game;String objectId=object==null?null:object.id,hudId=hud==null?null:hud.id;
        String current=object!=null?object.scriptClassName:hud!=null?hud.scriptClassName:targetScene.scriptClassName;
        try{ScriptWindow.open(canvas.getScene().getWindow(),directory,current,name->{
            if(game!=targetGame){message("O projeto mudou. Abra o editor de scripts novamente para anexar.");return;}stop();
            if(objectId!=null){var target=targetScene.objects.stream().filter(e->e.id.equals(objectId)).findFirst();if(target.isEmpty()){message("O objeto foi excluído.");return;}target.get().scriptClassName=name;}
            else if(hudId!=null){var target=targetScene.hud.stream().filter(h->h.id.equals(hudId)).findFirst();if(target.isEmpty()){message("A HUD foi excluída.");return;}target.get().scriptClassName=name;}
            else targetScene.scriptClassName=name;
            refresh();if(selected!=null)inspect();else if(selectedHud!=null)inspectHud();inspectScene();message("Anexo de script atualizado. Salve o projeto.");
        });}catch(Exception e){message(e.getMessage());}
    }
    public void openProjectFolder(){if(!ensureProject())return;launchExternal(System.getProperty("os.name").toLowerCase().contains("win")?"explorer":System.getProperty("os.name").toLowerCase().contains("mac")?"open":"xdg-open");}
    public void openProjectIde(){if(!ensureProject())return;try{ScriptCompiler.prepareIde(directory);var dialog=new TextInputDialog("code");dialog.setTitle("Abrir na IDE");dialog.setHeaderText("Executável da IDE (code, idea, codium ou caminho completo)");dialog.showAndWait().ifPresent(this::launchExternal);}catch(Exception e){message(e.getMessage());}}
    private void launchExternal(String executable){try{if(executable.isBlank())return;new ProcessBuilder(executable,directory.toAbsolutePath().toString()).start();}catch(Exception e){message("Não foi possível abrir: "+e.getMessage());}}
    public void exportAndroid(){exportGame("Android","build-android.sh");}
    public void exportDesktop(){exportGame("Desktop","build-desktop.sh");}
    private void exportGame(String platform,String scriptName){
        if(androidBuild!=null&&androidBuild.isAlive()){message("Uma build já está em andamento.");return;}
        saveProject();if(!saved)return;
        Path script=Path.of(System.getProperty("engine.home","."),"scripts",scriptName).toAbsolutePath();
        if(!Files.isRegularFile(script)){message("Script de build não encontrado. Configure -Dengine.home com a pasta da engine.");return;}
        try{
            Path log=directory.resolve("build/"+platform.toLowerCase(Locale.ROOT)+"-build.log");Files.createDirectories(log.getParent());
            androidBuild=new ProcessBuilder("bash",script.toString(),directory.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            Process process=androidBuild;Path exported=directory.resolve("build/"+platform.toLowerCase(Locale.ROOT));
            message("Compilando "+platform+"… log: "+log);
            Thread worker=new Thread(()->{try{int code=process.waitFor();javafx.application.Platform.runLater(()->{if(!disposed)message(code==0?platform+" gerado em "+exported+" • pasta aberta no explorador":"Build "+platform+" falhou. Consulte "+log);});}catch(InterruptedException e){Thread.currentThread().interrupt();}},"game-export");
            worker.setDaemon(true);worker.start();
        }catch(Exception e){message("Falha ao iniciar build: "+e.getMessage());}
    }
    @Override public void onDestroy(){disposed=true;if(androidBuild!=null&&androidBuild.isAlive())androidBuild.destroy();if(timer!=null)timer.stop();stop();keys.clear();pendingAssets.close();images.clear();}
}
