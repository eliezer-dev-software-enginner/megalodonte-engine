package megalodonte.engine.editor;
import megalodonte.engine.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;
import java.nio.*;
import java.io.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
class TiledImporterTest {
    @TempDir Path temp;
    private void image()throws Exception{var png=new BufferedImage(32,16,BufferedImage.TYPE_INT_ARGB);ImageIO.write(png,"png",temp.resolve("tiles.png").toFile());}
    @Test void tmxExternalTilesetCsvGroupsAndCollidersPersist()throws Exception{
        image();Files.writeString(temp.resolve("tiles.tsx"),"<tileset tilewidth='16' tileheight='16' tilecount='2' columns='2'><image source='tiles.png' width='32' height='16'/></tileset>");
        Path file=temp.resolve("map.tmx");Files.writeString(file,"<map orientation='orthogonal' width='2' height='1' tilewidth='16' tileheight='16'><tileset firstgid='1' source='tiles.tsx'/><group offsetx='16'><layer name='floor' width='2' height='1'><data encoding='csv'>1,2147483650</data></layer></group><objectgroup name='collision'><object x='16' y='0' width='16' height='16'/></objectgroup></map>");
        try(var assets=new PendingAssets()){var map=new TiledImporter(assets::stage).read(file);assertEquals(2,map.tiles.size());assertEquals(16,map.layers.getFirst().offsetX);assertEquals(1,map.obstacles.size());assertEquals(0,map.obstacles.getFirst().y);
            var game=Game.demo();game.scenes.getFirst().tileMaps.add(map);var copy=Codec.copy(game);assertEquals(2147483650L,copy.scenes.getFirst().tileMaps.getFirst().layers.getFirst().data[1]);
            assets.publish(temp.resolve("project"));assertTrue(Files.exists(temp.resolve("project").resolve(map.tiles.get(1).assetPath)));
        }
    }
    @Test void jsonExternalTilesetAndTileFlipMatrix()throws Exception{
        image();Files.writeString(temp.resolve("tiles.tsj"),"{\"tilewidth\":16,\"tileheight\":16,\"columns\":2,\"tilecount\":2,\"image\":\"tiles.png\"}");
        Path file=temp.resolve("map.tmj");Files.writeString(file,"{\"orientation\":\"orthogonal\",\"width\":2,\"height\":1,\"tilewidth\":16,\"tileheight\":16,\"tilesets\":[{\"firstgid\":1,\"source\":\"tiles.tsj\"}],\"layers\":[{\"type\":\"tilelayer\",\"data\":[1,2147483650]}]}");
        try(var assets=new PendingAssets()){var map=new TiledImporter(assets::stage).read(file);var scene=new Game.Scene();scene.tileMaps.add(map);scene.camera.x=1;scene.camera.y=.5;scene.camera.width=2;scene.camera.height=1;List<Double> flips=new ArrayList<>();TileDraw.render(scene,CameraView.of(scene.camera,200,100),(tile,x,y,w,h,alpha,a,b,c,d)->flips.add(a));assertEquals(List.of(1.0,-1.0),flips);}
    }
    @Test void compressedBase64AndUnsupportedMapsFailClearly()throws Exception{
        image();var buffer=ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(1).putInt(2);var out=new ByteArrayOutputStream();try(var zip=new DeflaterOutputStream(out)){zip.write(buffer.array());}String base64=Base64.getEncoder().encodeToString(out.toByteArray());
        Path file=temp.resolve("map.tmx");String map="<map orientation='orthogonal' width='2' height='1' tilewidth='16' tileheight='16'><tileset firstgid='1' tilewidth='16' tileheight='16' tilecount='2' columns='2'><image source='tiles.png'/></tileset><layer width='2' height='1'><data encoding='base64' compression='zlib'>"+base64+"</data></layer></map>";Files.writeString(file,map);
        try(var assets=new PendingAssets()){assertEquals(2,new TiledImporter(assets::stage).read(file).layers.getFirst().data[1]);Files.writeString(file,map.replace("orthogonal","isometric"));assertThrows(IllegalArgumentException.class,()->new TiledImporter(assets::stage).read(file));}
    }
    @Test void importsLocalLibGdxChallengeWhenAvailable()throws Exception{
        Path map=Path.of("../../../libgdx-challenges/desafio21/assets/tilemap/project.tmx").toAbsolutePath().normalize();
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(map));
        try(var assets=new PendingAssets()){var importer=new TiledImporter(assets::stage);var imported=importer.read(map);assertEquals(20,imported.columns);assertEquals(10,imported.rows);assertFalse(imported.layers.isEmpty());assertFalse(imported.obstacles.isEmpty());var game=Game.demo();game.scenes.getFirst().tileMaps.add(imported);Codec.validate(game);}
    }
    @Test void xmlExternalEntitiesAreRejected()throws Exception{Path file=temp.resolve("map.tmx");Files.writeString(file,"<!DOCTYPE map [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><map>&x;</map>");assertThrows(Exception.class,()->new TiledImporter(path->"assets/x").read(file));}
}
