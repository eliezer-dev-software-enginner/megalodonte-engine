package megalodonte.engine.editor;
import com.google.gson.JsonPrimitive;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;
/** Typed values retain their JSON type instead of storing true/false as strings. */
final class HudPropertiesForm {
    final VBox box=new VBox(8);
    private final List<Row> rows=new ArrayList<>();
    private final VBox entries=new VBox(6);
    HudPropertiesForm(Map<String,JsonPrimitive> source){
        var add=new Button("Adicionar propriedade");add.setOnAction(e->add("nova",new JsonPrimitive(false)));
        box.getChildren().addAll(new Label("Nome • Tipo • Valor inicial (copiado ao duplicar a HUD)"),entries,add);
        source.forEach(this::add);
    }
    private void add(String name,JsonPrimitive value){var row=new Row(name,value);rows.add(row);entries.getChildren().add(row.box);}
    Map<String,JsonPrimitive> values(){var result=new LinkedHashMap<String,JsonPrimitive>();for(var row:rows){String name=row.name.getText().strip();if(name.isBlank()||result.containsKey(name))throw new IllegalArgumentException("Nomes das propriedades devem ser preenchidos e únicos");result.put(name,row.value());}return result;}
    private final class Row {
        final TextField name=new TextField(),text=new TextField();final ComboBox<String> type=new ComboBox<>();final CheckBox flag=new CheckBox();final HBox valueBox=new HBox();final HBox box=new HBox(6);
        Row(String key,JsonPrimitive value){name.setText(key);name.setPrefWidth(160);text.setPrefWidth(150);type.getItems().addAll("booleano","número","texto");type.setValue(value.isBoolean()?"booleano":value.isNumber()?"número":"texto");text.setText(value.getAsString());flag.setSelected(value.isBoolean()&&value.getAsBoolean());flag.setId("hudProperty-"+key);flag.setText(""+flag.isSelected());flag.selectedProperty().addListener((o,a,b)->flag.setText(""+b));type.setOnAction(e->showValue());showValue();var remove=new Button("Excluir");remove.setOnAction(e->{rows.remove(this);entries.getChildren().remove(box);});box.getChildren().addAll(name,type,valueBox,remove);}
        void showValue(){valueBox.getChildren().setAll(type.getValue().equals("booleano")?flag:text);}
        JsonPrimitive value(){return switch(type.getValue()){case "booleano"->new JsonPrimitive(flag.isSelected());case "número"->{double number=Double.parseDouble(text.getText());if(!Double.isFinite(number))throw new IllegalArgumentException("Número inválido");yield new JsonPrimitive(number);}default->new JsonPrimitive(text.getText());};}
    }
}
