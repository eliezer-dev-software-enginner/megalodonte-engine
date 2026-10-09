package megalodonte.engine;
/** Shared desktop/Android HUD geometry, applied to the safe viewport. */
public final class HudLayout {
    private HudLayout() { }
    public record Box(double x, double y, double width, double height, double scale) {
        public boolean contains(double px, double py) { return px >= x && py >= y && px <= x+width && py <= y+height; }
    }
    public static Box resolve(Game.Hud hud, double viewportWidth, double viewportHeight) {
        double scale = Math.max(0, Math.min(viewportWidth/1280, viewportHeight/720));
        double width = hud.width*scale, height = hud.height*scale;
        double x = switch(hud.horizontalAnchor) {
            case "right" -> viewportWidth-width-hud.offsetX*scale;
            case "center" -> (viewportWidth-width)/2+hud.offsetX*scale;
            default -> hud.offsetX*scale;
        };
        double y = switch(hud.verticalAnchor) {
            case "bottom" -> viewportHeight-height-hud.offsetY*scale;
            case "center" -> (viewportHeight-height)/2+hud.offsetY*scale;
            default -> hud.offsetY*scale;
        };
        return new Box(x,y,width,height,scale);
    }
    /** Inverse of anchor placement: dragging preserves the chosen anchor. */
    public static void position(Game.Hud hud, double x, double y, double vw, double vh) {
        Box b = resolve(hud,vw,vh); if(b.scale<=0)return;
        hud.offsetX = switch(hud.horizontalAnchor) {
            case "right" -> (vw-b.width-x)/b.scale;
            case "center" -> (x-(vw-b.width)/2)/b.scale;
            default -> x/b.scale;
        };
        hud.offsetY = switch(hud.verticalAnchor) {
            case "bottom" -> (vh-b.height-y)/b.scale;
            case "center" -> (y-(vh-b.height)/2)/b.scale;
            default -> y/b.scale;
        };
    }
}
