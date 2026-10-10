package megalodonte.engine;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HudPresentationTest {
    private Game.Hud button(){var h=new Game.Hud();h.setBooleanProperty("liberado",false);h.condition.property="liberado";h.condition.trueAssetPath="assets/number.png";h.condition.falseAssetPath="assets/lock.png";h.condition.disableWhenFalse=true;return h;}
    @Test void samePropertyControlsAppearanceAndInteraction(){var h=button();assertEquals("assets/lock.png",HudPresentation.asset(h));assertFalse(HudPresentation.enabled(h));h.setBooleanProperty("liberado",true);assertEquals("assets/number.png",HudPresentation.asset(h));assertTrue(HudPresentation.enabled(h));}
    @Test void copyKeepsTypedDefaultsAndIsIndependent(){var h=button();h.properties.put("points",new JsonPrimitive(100));h.properties.put("caption",new JsonPrimitive("Fase"));var copy=h.copy();assertFalse(copy.booleanProperty("liberado",true));assertTrue(copy.properties.get("liberado").isBoolean());assertEquals(100,copy.properties.get("points").getAsInt());copy.setBooleanProperty("liberado",true);copy.condition.trueAssetPath="assets/other.png";assertFalse(h.booleanProperty("liberado",true));assertEquals("assets/number.png",h.condition.trueAssetPath);}
    @Test void invalidConditionsFailBeforeSaving(){var h=button();HudPresentation.validate(h);h.properties.put("liberado",new JsonPrimitive("false"));assertThrows(IllegalArgumentException.class,()->HudPresentation.validate(h));h.setBooleanProperty("liberado",false);h.condition.falseAssetPath="../escape.png";assertThrows(IllegalArgumentException.class,()->HudPresentation.validate(h));}
    @Test void oldHudUsesItsDefaultImage(){var h=new Game.Hud();h.assetPath="assets/old.png";HudPresentation.validate(h);assertEquals(h.assetPath,HudPresentation.asset(h));assertTrue(HudPresentation.enabled(h));}
}
