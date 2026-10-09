package megalodonte.engine.editor;
import megalodonte.engine.*;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

final class TiledImporter {
    interface AssetSink { String copy(Path file) throws IOException; }
    final List<String> warnings=new ArrayList<>();
    private final AssetSink sink;
    private final Map<Path,String> images=new HashMap<>();
    TiledImporter(AssetSink sink){this.sink=sink;}
    Game.TileMap read(Path file)throws Exception{
        JsonObject root=readDocument(file);String orientation=text(root,"orientation","orthogonal");
        if(!orientation.equals("orthogonal")||number(root,"infinite",0)!=0)throw new IllegalArgumentException("Use mapa ortogonal finito no Tiled");
        var map=new Game.TileMap();map.name=file.getFileName().toString();map.columns=integer(root,"width",0);map.rows=integer(root,"height",0);map.tileWidth=integer(root,"tilewidth",0);map.tileHeight=integer(root,"tileheight",0);
        if(map.columns<=0||map.rows<=0||map.tileWidth<=0||map.tileHeight<=0||(long)map.columns*map.rows>4_000_000)throw new IllegalArgumentException("Dimensões do mapa inválidas ou grandes demais");
        map.unitScale=1.0/map.tileWidth;
        for(var entry:array(root,"tilesets")){var tileset=entry.getAsJsonObject();int first=integer(tileset,"firstgid",1);Path origin=file;
            if(tileset.has("source")){origin=resolve(file,text(tileset,"source",""));tileset=readDocument(origin);}
            tileset(map,tileset,first,origin);
        }
        layers(map,array(root,"layers"),file,0,0,1);
        Game test=Game.demo();test.scenes.get(0).tileMaps.add(map);Codec.validate(test);return map;
    }
    private void tileset(Game.TileMap map,JsonObject set,int first,Path origin)throws Exception{
        int w=integer(set,"tilewidth",map.tileWidth),h=integer(set,"tileheight",map.tileHeight),spacing=integer(set,"spacing",0),margin=integer(set,"margin",0);
        if(w<=0||h<=0)throw new IllegalArgumentException("Dimensões do tileset inválidas");
        if(set.has("image")){
            Path file=resolve(origin,text(set,"image",""));var decoded=javax.imageio.ImageIO.read(file.toFile());if(decoded==null)throw new IOException("Imagem de tileset inválida: "+file);
            int columns=integer(set,"columns",((int)decoded.getWidth()-2*margin+spacing)/(w+spacing));
            int count=integer(set,"tilecount",columns*(((int)decoded.getHeight()-2*margin+spacing)/(h+spacing)));
            if(columns<=0||count<=0||count>1_000_000)throw new IllegalArgumentException("Tileset inválido");
            String asset=image(file);
            for(int i=0;i<count;i++){var tile=new Game.Tile();tile.assetPath=asset;tile.width=w;tile.height=h;tile.sourceX=margin+(i%columns)*(w+spacing);tile.sourceY=margin+(i/columns)*(h+spacing);
                if(tile.sourceX+w>decoded.getWidth()||tile.sourceY+h>decoded.getHeight())throw new IllegalArgumentException("Tile fora da imagem");map.tiles.put(first+i,tile);}
        }
        for(var value:array(set,"tiles")){var tileDef=value.getAsJsonObject();int id=integer(tileDef,"id",0);
            if(tileDef.has("image")){Path file=resolve(origin,text(tileDef,"image",""));var decoded=javax.imageio.ImageIO.read(file.toFile());if(decoded==null)throw new IOException("Imagem inválida: "+file);var tile=new Game.Tile();tile.assetPath=image(file);tile.width=(int)decoded.getWidth();tile.height=(int)decoded.getHeight();map.tiles.put(first+id,tile);}
            if(tileDef.has("animation"))warnings.add("Animação de tiles não é executada nesta versão (tile estático)");
        }
    }
    private void layers(Game.TileMap map,JsonArray layers,Path origin,double ox,double oy,double opacity)throws Exception{
        for(var value:layers){var layer=value.getAsJsonObject();if(layer.has("visible")&&!flag(layer,"visible",true))continue;
            double x=ox+number(layer,"offsetx",0),y=oy+number(layer,"offsety",0),alpha=opacity*number(layer,"opacity",1);
            String type=text(layer,"type","tilelayer");
            if(type.equals("group")){layers(map,array(layer,"layers"),origin,x,y,alpha);continue;}
            if(type.equals("tilelayer")){
                if(integer(layer,"width",map.columns)!=map.columns||integer(layer,"height",map.rows)!=map.rows)throw new IllegalArgumentException("Camada deve ter o tamanho do mapa");
                var out=new Game.TileLayer();out.name=text(layer,"name","");out.offsetX=x;out.offsetY=y;out.opacity=alpha;
                out.data=decode(layer,map.columns*map.rows);map.layers.add(out);
            }else if(type.equals("objectgroup")){
                boolean solid=text(layer,"name","").toLowerCase(Locale.ROOT).matches(".*(collision|colis|solid|ground|obstacle).*");
                for(var item:array(layer,"objects")){var object=item.getAsJsonObject();boolean enabled=solid;
                    for(var prop:array(object,"properties")){var property=prop.getAsJsonObject();if(text(property,"name","").equals("solid"))enabled=flag(property,"value",false);}
                    if(!enabled)continue;
                    if(object.has("polygon")||object.has("polyline")||flag(object,"ellipse",false)||number(object,"rotation",0)!=0){warnings.add("Colisor não retangular ignorado");continue;}
                    double w=number(object,"width",0),h=number(object,"height",0);if(w<=0||h<=0)continue;
                    var obstacle=new Game.Entity();obstacle.name=text(object,"name","Colisor do mapa");obstacle.x=x+number(object,"x",0);obstacle.y=map.rows*map.tileHeight-y-number(object,"y",0)-h;obstacle.width=w;obstacle.height=h;map.obstacles.add(obstacle);
                }
            }else warnings.add("Camada "+type+" não renderizada");
        }
    }
    private long[] decode(JsonObject layer,int length)throws Exception{
        var data=layer.get("data");if(data==null)throw new IllegalArgumentException("Camada sem dados");long[] result=new long[length];
        if(data.isJsonArray()){if(data.getAsJsonArray().size()!=length)throw new IllegalArgumentException("Número incorreto de tiles");for(int i=0;i<length;i++)result[i]=data.getAsJsonArray().get(i).getAsLong();return result;}
        String encoding=text(layer,"encoding","csv"),content=data.getAsString().trim();
        if(encoding.equals("csv")){String[] values=content.split("\\s*,\\s*");if(values.length!=length)throw new IllegalArgumentException("CSV com número incorreto de tiles");for(int i=0;i<length;i++)result[i]=Long.parseLong(values[i].trim());}
        else if(encoding.equals("base64")){
            InputStream stream=new ByteArrayInputStream(Base64.getMimeDecoder().decode(content));String compression=text(layer,"compression","");
            if(compression.equals("zlib"))stream=new InflaterInputStream(stream);else if(compression.equals("gzip"))stream=new GZIPInputStream(stream);else if(!compression.isEmpty())throw new IllegalArgumentException("Compressão não suportada: "+compression);
            try(InputStream decodedStream=stream){byte[] bytes=decodedStream.readNBytes(Math.multiplyExact(length,4)+1);if(bytes.length!=length*4)throw new IllegalArgumentException("Base64 com número incorreto de tiles");var buffer=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);for(int i=0;i<length;i++)result[i]=Integer.toUnsignedLong(buffer.getInt());}
        }else throw new IllegalArgumentException("Encoding não suportado: "+encoding);return result;
    }
    private String image(Path file)throws IOException{file=file.toRealPath();String asset=images.get(file);if(asset==null){asset=sink.copy(file);images.put(file,asset);}return asset;}
    private static Path resolve(Path origin,String reference)throws IOException{Path path=origin.toAbsolutePath().getParent().resolve(reference.replace('\\','/')).normalize();if(!Files.isRegularFile(path))throw new IOException("Dependência ausente: "+path);return path;}
    private static JsonObject readDocument(Path file)throws Exception{
        if(file.toString().toLowerCase(Locale.ROOT).matches(".*\\.(json|tmj|tsj)$"))return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);factory.setFeature("http://xml.org/sax/features/external-general-entities",false);factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);
        return xml(factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement());
    }
    private static JsonObject xml(Element element){
        var result=new JsonObject();var attrs=element.getAttributes();for(int i=0;i<attrs.getLength();i++)result.addProperty(attrs.item(i).getNodeName(),attrs.item(i).getNodeValue());
        String tag=element.getTagName();if(Set.of("layer","objectgroup","group","imagelayer").contains(tag))result.addProperty("type",switch(tag){case "layer"->"tilelayer";default->tag;});
        for(Node node=element.getFirstChild();node!=null;node=node.getNextSibling())if(node instanceof Element child){String name=child.getTagName();
            if(name.equals("image")){result.addProperty("image",child.getAttribute("source"));result.addProperty("imagewidth",child.getAttribute("width"));result.addProperty("imageheight",child.getAttribute("height"));}
            else if(name.equals("data")){result.addProperty("encoding",child.getAttribute("encoding").isEmpty()?"xml":child.getAttribute("encoding"));result.addProperty("compression",child.getAttribute("compression"));
                if(child.getAttribute("encoding").isEmpty()){var data=new JsonArray();for(Node tile=child.getFirstChild();tile!=null;tile=tile.getNextSibling())if(tile instanceof Element t)data.add(Long.parseLong(t.getAttribute("gid")));result.add("data",data);}
                else result.addProperty("data",child.getTextContent());
            }else if(name.equals("properties")){var data=new JsonArray();for(Node prop=child.getFirstChild();prop!=null;prop=prop.getNextSibling())if(prop instanceof Element pr)data.add(xml(pr));result.add("properties",data);}
            else{String key=switch(name){case "tileset"->"tilesets";case "layer","objectgroup","group","imagelayer"->"layers";case "tile"->"tiles";case "object"->"objects";default->name;};
                if(Set.of("tilesets","layers","tiles","objects").contains(key)){if(!result.has(key))result.add(key,new JsonArray());result.getAsJsonArray(key).add(xml(child));}else result.add(key,xml(child));}
        }return result;
    }
    private static JsonArray array(JsonObject o,String key){return o.has(key)?o.getAsJsonArray(key):new JsonArray();}
    private static String text(JsonObject o,String key,String fallback){return o.has(key)?o.get(key).getAsString():fallback;}
    private static double number(JsonObject o,String key,double fallback){if(!o.has(key))return fallback;String value=o.get(key).getAsString();if(value.equals("true"))return 1;if(value.equals("false"))return 0;return Double.parseDouble(value);}
    private static int integer(JsonObject o,String key,int fallback){return (int)number(o,key,fallback);}
    private static boolean flag(JsonObject o,String key,boolean fallback){return number(o,key,fallback?1:0)!=0;}
}
