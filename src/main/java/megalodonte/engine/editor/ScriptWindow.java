package megalodonte.engine.editor;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.nio.file.*;
import java.util.*;
final class ScriptWindow {
    static void open(Window owner,Path project,String current,java.util.function.Consumer<String> attach)throws Exception{
        ScriptCompiler.prepareIde(project);
        Stage stage=new Stage();stage.initOwner(owner);stage.setTitle("Scripts Java • "+project);
        TextArea code=new TextArea();code.setStyle("-fx-font-family: monospace; -fx-font-size: 14px; -fx-control-inner-background: #142130; -fx-text-fill: #dce6f2; -fx-highlight-fill: #326b9a;");code.setWrapText(false);
        TextArea gutter=new TextArea("1");gutter.setEditable(false);gutter.setFocusTraversable(false);gutter.setPrefWidth(60);gutter.setMinWidth(60);gutter.setMaxWidth(60);gutter.setStyle("-fx-font-family: monospace; -fx-font-size: 14px;");
        code.scrollTopProperty().addListener((o,a,b)->gutter.setScrollTop(b.doubleValue()));code.textProperty().addListener((o,a,b)->{int count=b.split("\n",-1).length;StringBuilder lines=new StringBuilder();for(int i=1;i<=count;i++)lines.append(i).append("\n");gutter.setText(lines.toString());});
        TextArea output=new TextArea();output.setEditable(false);output.setPrefHeight(120);output.setWrapText(true);
        ComboBox<String> files=new ComboBox<>();files.setPrefWidth(280);
        Map<String,Path> paths=new LinkedHashMap<>();try(var stream=Files.walk(project.resolve("scripts"))){for(Path path:stream.filter(p->p.toString().endsWith(".java")).toList()){String name=project.resolve("scripts").relativize(path).toString().replace('\\','.').replace('/','.').replaceAll("\\.java$","");paths.put(name,path);files.getItems().add(name);}}
        Path[] activeFile={null};String[] savedText={""};boolean[] changing={false};
        java.util.function.BooleanSupplier saveCurrent=()->{try{
            if(activeFile[0]==null)throw new IllegalArgumentException("Escolha um script");
            Files.writeString(activeFile[0],code.getText());savedText[0]=code.getText();return true;
        }catch(Exception ex){output.setText("Falha ao salvar: "+ex.getMessage());return false;}};
        files.valueProperty().addListener((observable,oldValue,newValue)->{if(changing[0])return;try{
            Path path=paths.get(files.getValue());if(path==null)return;
            if(activeFile[0]!=null&&!code.getText().equals(savedText[0])&&!saveCurrent.getAsBoolean()){
                changing[0]=true;files.setValue(paths.entrySet().stream().filter(entry->entry.getValue().equals(activeFile[0])).findFirst().orElseThrow().getKey());changing[0]=false;return;
            }
            String source=Files.readString(path);activeFile[0]=path;savedText[0]=source;code.setText(source);
        }catch(Exception ex){output.setText(ex.getMessage());}});
        stage.setOnCloseRequest(e->{if(activeFile[0]!=null&&!code.getText().equals(savedText[0])&&!saveCurrent.getAsBoolean())e.consume();});
        stage.setOnHiding(e->{if(activeFile[0]!=null&&!code.getText().equals(savedText[0]))saveCurrent.getAsBoolean();});
        Button create=new Button("Novo script"),save=new Button("Salvar código"),compile=new Button("Compilar"),bind=new Button("Anexar ao alvo"),detach=new Button("Desanexar");
        create.setOnAction(e->{var dialog=new TextInputDialog("PlayerMovement");dialog.initOwner(stage);dialog.setTitle("Novo script Java");dialog.setHeaderText("Nome da classe (sem .java)");dialog.showAndWait().ifPresent(name->{try{
            if(!name.matches("[A-Za-z_$][A-Za-z0-9_$]*"))throw new IllegalArgumentException("Nome de classe inválido");String className="game.scripts."+name;Path file=project.resolve("scripts/game/scripts/"+name+".java");if(Files.exists(file))throw new IllegalArgumentException("Script já existe");
            String source="package game.scripts;\n\nimport megalodonte.engine.GameScript;\nimport megalodonte.engine.GameContext;\n\npublic class "+name+" implements GameScript {\n    @Override public void onStart(GameContext context) {\n        context.log(\"Script iniciado\");\n    }\n    @Override public void onUpdate(double deltaTime, GameContext context) {\n        // Para objetos: context.move(context.horizontal() * 4 * deltaTime,\n        //     context.vertical() * 4 * deltaTime);\n    }\n}\n";
            Files.writeString(file,source);paths.put(className,file);files.getItems().add(className);files.setValue(className);
        }catch(Exception ex){output.setText(ex.getMessage());}});});
        save.setOnAction(e->{if(saveCurrent.getAsBoolean())output.setText("Código salvo. Salve o projeto para persistir os anexos.");});
        compile.setOnAction(e->{if(!saveCurrent.getAsBoolean())return;compile.setDisable(true);output.setText("Compilando…");Thread worker=new Thread(()->{String report;try(var compiler=ScriptCompiler.compile(project)){report="Compilação concluída.";}catch(Exception error){report=error.getMessage();}String result=report;javafx.application.Platform.runLater(()->{output.setText(result);compile.setDisable(false);});},"script-compiler");worker.setDaemon(true);worker.start();});
        bind.setOnAction(e->{if(files.getValue()!=null&&saveCurrent.getAsBoolean()){attach.accept(files.getValue());output.setText("Anexado: "+files.getValue()+". Salve o projeto.");}});
        detach.setOnAction(e->{attach.accept("");output.setText("Script desanexado. Salve o projeto.");});
        HBox toolbar=new HBox(8,files,create,save,compile,bind,detach);HBox editor=new HBox(gutter,code);HBox.setHgrow(code,Priority.ALWAYS);
        VBox layout=new VBox(10,toolbar,editor,output);layout.setPadding(new javafx.geometry.Insets(12));VBox.setVgrow(editor,Priority.ALWAYS);stage.setScene(new Scene(layout,1100,730));
        if(paths.containsKey(current))files.setValue(current);else if(!files.getItems().isEmpty())files.setValue(files.getItems().getFirst());stage.show();
    }
}
