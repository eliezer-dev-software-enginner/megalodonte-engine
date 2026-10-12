package megalodonte.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HudStyleTest {
 @Test void oldHudDefaultsAndNewStyleRoundTrip(){var old=Codec.read("{\"version\":1,\"entrySceneId\":\"scene-1\",\"scenes\":[{\"id\":\"scene-1\",\"hud\":[{\"id\":\"button\",\"type\":\"button\"}]}]}");var hud=old.scenes.get(0).hud.get(0);assertEquals(1,hud.buttonOpacity);hud.buttonBackground="#222222";hud.buttonOpacity=.7;hud.cornerRadius=0;hud.textAlign=hud.textVerticalAlign="center";hud.fontFamily="SansSerif";var copy=Codec.read(Codec.GSON.toJson(old)).scenes.get(0).hud.get(0);assertEquals(.7,copy.buttonOpacity);assertEquals("center",copy.textAlign);}
 @Test void invalidStyleIsRejected(){var hud=new Game.Hud();hud.buttonOpacity=Double.NaN;assertThrows(IllegalArgumentException.class,()->HudPresentation.validate(hud));hud.buttonOpacity=1;hud.textAlign=null;assertThrows(IllegalArgumentException.class,()->HudPresentation.validate(hud));}
}
