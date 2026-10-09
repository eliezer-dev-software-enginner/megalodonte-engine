package megalodonte.engine.editor;
import megalodonte.engine.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.*;

/** Generates a small complete game for desktop and APK verification. */
public final class ExampleProject {
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("Informe uma pasta vazia para o exemplo");
        Path project=Path.of(args[0]).toAbsolutePath();
        if(Files.exists(project.resolve("game.json")))throw new IllegalArgumentException("A pasta já contém game.json");
        Files.createDirectories(project.resolve("assets"));ScriptCompiler.prepareIde(project);
        var png=new BufferedImage(32,16,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<16;y++)for(int x=0;x<32;x++)png.setRGB(x,y,x<16?(y<4?0xff86bd62:0xff544333):0xff497ba4);
        ImageIO.write(png,"png",project.resolve("assets/tiles.png").toFile());
        Files.writeString(project.resolve("assets/tiles.tsx"),"<tileset tilewidth='16' tileheight='16' tilecount='2' columns='2'><image source='tiles.png'/></tileset>");
        StringBuilder csv=new StringBuilder();for(int y=0;y<12;y++)for(int x=0;x<40;x++){if(csv.length()>0)csv.append(',');csv.append(y>=10?1:0);}
        Files.writeString(project.resolve("assets/map.tmx"),"<map orientation='orthogonal' width='40' height='12' tilewidth='16' tileheight='16'><tileset firstgid='1' source='tiles.tsx'/><layer width='40' height='12'><data encoding='csv'>"+csv+"</data></layer><objectgroup name='collision'><object x='0' y='160' width='640' height='32'/></objectgroup></map>");
        var game=Game.demo();game.name="Exemplo • mapa, câmera, áudio e scripts";var scene=game.scenes.getFirst();scene.worldWidth=40;scene.worldHeight=12;
        try(var staged=new PendingAssets()){scene.tileMaps.add(new TiledImporter(staged::stage).read(project.resolve("assets/map.tmx")));staged.publish(project);}
        var player=scene.objects.getFirst();player.name="Jogador";player.y=3;player.scriptClassName="game.scripts.Player";scene.objects.get(1).x=12;scene.objects.get(1).y=2;
        scene.camera.mode="follow";scene.camera.targetId=player.id;scene.camera.fadeSeconds=.6;
        var score=new Game.Hud();score.id="position";score.text="Posição";scene.hud.add(score);
        var left=new Game.Hud();left.id="left";left.type="button";left.action="left";left.text="◀";left.width=80;left.height=80;left.verticalAnchor="bottom";scene.hud.add(left);
        var right=new Game.Hud();right.id="right";right.type="button";right.action="right";right.text="▶";right.width=80;right.height=80;right.offsetX=120;right.verticalAnchor="bottom";scene.hud.add(right);
        Files.writeString(project.resolve("scripts/game/scripts/Player.java"), """
            package game.scripts;

            import megalodonte.engine.GameContext;
            import megalodonte.engine.GameScript;

            public class Player implements GameScript {
                @Override public void onUpdate(double dt, GameContext context) {
                    double direction = context.horizontal();
                    if (context.actionDown("right")) direction += 1;
                    if (context.actionDown("left")) direction -= 1;
                    direction = Math.max(-1, Math.min(1, direction));
                    context.move(direction * 4 * dt, context.vertical() * 4 * dt);
                    context.findHud("position").text =
                        "X: " + Math.round(context.worldTransform().x() * 10) / 10.0;
                }
            }
            """);
        int samples=22050;ByteBuffer wav=ByteBuffer.allocate(44+samples*2).order(ByteOrder.LITTLE_ENDIAN);wav.put("RIFF".getBytes()).putInt(36+samples*2).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(22050).putInt(44100).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(samples*2);
        for(int i=0;i<samples;i++)wav.putShort((short)(Math.sin(2*Math.PI*220*i/22050)*900));Files.write(project.resolve("assets/music.wav"),wav.array());scene.musicAssetPath="assets/music.wav";scene.musicVolume=.25;
        Codec.validate(game);Files.writeString(project.resolve("game.json"),Codec.GSON.toJson(game));
        try(var compiler=ScriptCompiler.compile(project);var runtime=new megalodonte.engine.Runtime(game,megalodonte.engine.Runtime.reflection(compiler.loader),new megalodonte.engine.Runtime.Events(){})){runtime.setAction("right",true);for(int i=0;i<10;i++)runtime.update(.05,0,0);if(runtime.scene.objects.getFirst().x<=2)throw new IllegalStateException("Script do exemplo não moveu o jogador");}
        System.out.println("Exemplo criado e script validado: "+project);
    }
}
