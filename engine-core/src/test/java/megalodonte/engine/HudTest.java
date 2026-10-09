package megalodonte.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HudTest {
    @Test void rightBottomAnchorScalesToSafeViewport(){var h=new Game.Hud();h.horizontalAnchor="right";h.verticalAnchor="bottom";var b=HudLayout.resolve(h,640,360);assertEquals(478,b.x());assertEquals(324,b.y());}
    @Test void dragRoundTripsAllAnchors(){for(String x:new String[]{"left","center","right"})for(String y:new String[]{"top","center","bottom"}){var h=new Game.Hud();h.horizontalAnchor=x;h.verticalAnchor=y;HudLayout.position(h,120,85,800,400);var b=HudLayout.resolve(h,800,400);assertEquals(120,b.x(),1e-8);assertEquals(85,b.y(),1e-8);}}
    @Test void oldDocumentsAndSceneIsolation(){Game game=Codec.read("{\"version\":1,\"entrySceneId\":\"scene-1\",\"scenes\":[{\"id\":\"scene-1\"}]}");assertTrue(game.scenes.get(0).hud.isEmpty());game.scenes.get(0).hud.add(new Game.Hud());Game copy=Codec.copy(game);copy.scenes.get(0).hud.get(0).text="Outro";assertEquals("Pontuação: 0",game.scenes.get(0).hud.get(0).text);}
    @Test void invalidHudRejected(){Game game=Game.demo();var h=new Game.Hud();game.scenes.get(0).hud.add(h);h.width=-1;assertThrows(IllegalArgumentException.class,()->Codec.validate(game));h.width=300;h.assetPath="../secret";assertThrows(IllegalArgumentException.class,()->Codec.validate(game));}
}
