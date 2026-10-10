package megalodonte.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PresentationTest {
    @Test void applicationVersionIsSeparateFromDocumentVersion(){var game=Game.demo();game.versionCode=7;game.versionName="1.2.3";var copy=Codec.copy(game);assertEquals(1,copy.version);assertEquals(7,copy.versionCode);assertEquals("1.2.3",copy.versionName);game.versionCode=0;assertThrows(IllegalArgumentException.class,()->Codec.validate(game));}
    @Test void iconPathIsOptionalAndPortable(){var game=Game.demo();Codec.validate(game);game.iconAssetPath="assets/icon.png";assertEquals(game.iconAssetPath,Codec.copy(game).iconAssetPath);game.iconAssetPath="../icon.png";assertThrows(IllegalArgumentException.class,()->Codec.validate(game));game.iconAssetPath="assets/icon.svg";assertThrows(IllegalArgumentException.class,()->Codec.validate(game));}
    @Test void menuMusicIdentityIgnoresSceneAndVolume(){var a=new Game.Scene();a.musicAssetPath="assets/menu.mp3";var b=new Game.Scene();b.id="submenu";b.musicAssetPath=a.musicAssetPath;b.musicVolume=0;assertEquals(MusicTrack.key(a),MusicTrack.key(b));b.musicAssetPath="assets/race.mp3";assertNotEquals(MusicTrack.key(a),MusicTrack.key(b));b.musicAssetPath=a.musicAssetPath;b.musicLoop=false;assertNotEquals(MusicTrack.key(a),MusicTrack.key(b));}
    @Test void cameraReservesHudBandAndProjectsGroundAtBottom(){var c=new Game.Camera();c.x=10;c.y=11.875;c.width=20;c.height=10.25;c.viewportTop=64;var v=CameraView.of(c,1920,1080);assertEquals(96,v.screenY(v.bottom()+v.height()),1e-8);assertEquals(1080,v.screenY(v.bottom()),1e-8);assertEquals(0,v.screenX(v.left()),1e-8);}
}
