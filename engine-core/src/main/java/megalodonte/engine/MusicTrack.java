package megalodonte.engine;
/** A shared soundtrack survives scene transitions when its asset and looping stay the same. */
public final class MusicTrack {
    private MusicTrack(){}
    public static String key(Game.Scene scene){return scene.musicAssetPath+"|"+scene.musicLoop;}
}
