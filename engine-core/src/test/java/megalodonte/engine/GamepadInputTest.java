package megalodonte.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GamepadInputTest {
    @Test void deadZoneDisconnectAndHeldButtonAcrossScenes(){
        var input=new GamepadInput();var r=new Runtime(Game.demo());input.axes(.1,.1,0,0);assertEquals(0,input.horizontal());
        input.axes(.8,0,0,0);input.button("a",true);input.apply(r);assertTrue(r.pressed.contains("pad_a"));assertTrue(r.down.contains("pad_right"));r.update(0,0,0);
        input.apply(r);assertFalse(r.pressed.contains("pad_a"));
        r.scene=new Game.Scene();input.apply(r);assertFalse(r.down.contains("pad_a"));input.button("a",false);input.apply(r);input.button("a",true);input.apply(r);assertTrue(r.pressed.contains("pad_a"));
        input.clear();input.apply(r);assertFalse(r.down.contains("pad_a"));assertEquals(0,input.horizontal());r.close();
    }
    @Test void hatAndButtonDirectionsSupportDpad(){var input=new GamepadInput();input.axes(.3,0,-1,0);assertEquals(-1,input.horizontal());input.axes(0,0,0,0);input.button("right",true);assertEquals(1,input.horizontal());}
}
