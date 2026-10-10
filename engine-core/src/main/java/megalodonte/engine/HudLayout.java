package megalodonte.engine;
/** Shared desktop/Android HUD geometry, applied to the safe viewport. */
public final class HudLayout {
    private HudLayout() { }
    public record Box(double x, double y, double width, double height, double scale) {
        public boolean contains(double px, double py) { return px >= x && py >= y && px <= x+width && py <= y+height; }
    }
    private static Box area(Game.Hud hud,double vw,double vh){
        double scale=Math.max(0,Math.min(vw/1280,vh/720));
        if(hud.referenceArea.equals("content")){double w=1280*scale,h=720*scale;return new Box((vw-w)/2,(vh-h)/2,w,h,scale);}
        return new Box(0,0,vw,vh,scale);
    }
    public static Box resolve(Game.Hud hud, double viewportWidth, double viewportHeight) {
        Box area=area(hud,viewportWidth,viewportHeight);double scale=area.scale;
        double width = hud.width*scale, height = hud.height*scale;
        double x = switch(hud.horizontalAnchor) {
            case "right" -> area.width-width-hud.offsetX*scale;
            case "center" -> (area.width-width)/2+hud.offsetX*scale;
            default -> hud.offsetX*scale;
        };
        double y = switch(hud.verticalAnchor) {
            case "bottom" -> area.height-height-hud.offsetY*scale;
            case "center" -> (area.height-height)/2+hud.offsetY*scale;
            default -> hud.offsetY*scale;
        };
        return new Box(area.x+x,area.y+y,width,height,scale);
    }
    /** Inverse of anchor placement: dragging preserves the chosen anchor. */
    public static void position(Game.Hud hud, double x, double y, double vw, double vh) {
        Box b = resolve(hud,vw,vh); if(b.scale<=0)return;Box area=area(hud,vw,vh);x-=area.x;y-=area.y;
        hud.offsetX = switch(hud.horizontalAnchor) {
            case "right" -> (area.width-b.width-x)/b.scale;
            case "center" -> (x-(area.width-b.width)/2)/b.scale;
            default -> x/b.scale;
        };
        hud.offsetY = switch(hud.verticalAnchor) {
            case "bottom" -> (area.height-b.height-y)/b.scale;
            case "center" -> (y-(area.height-b.height)/2)/b.scale;
            default -> y/b.scale;
        };
    }
}
