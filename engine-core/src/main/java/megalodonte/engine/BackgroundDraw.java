package megalodonte.engine;
/** A horizontally repeating backdrop, drawn behind maps in every host. */
public final class BackgroundDraw {
    private BackgroundDraw(){}
    public interface Sink {void draw(String asset,double x,double y,double width,double height);}
    public static void render(Game.Scene scene,CameraView view,Sink sink){
        if(scene.backgroundAssetPath.isBlank()||view.scale()<=0)return;
        double width=view.width()*view.scale(),height=view.height()*view.scale();
        double shift=((view.left()*view.scale()*scene.backgroundParallaxX)%width+width)%width;
        double x=view.screenX(view.left())-shift,y=view.screenY(view.bottom()+view.height());
        sink.draw(scene.backgroundAssetPath,x,y,width,height);sink.draw(scene.backgroundAssetPath,x+width,y,width,height);
    }
}
