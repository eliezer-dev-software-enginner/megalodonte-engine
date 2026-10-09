package megalodonte.engine;
/** Rotation is clockwise in screen coordinates, around the object's center. */
public final class EntityGeometry {
    private EntityGeometry() { }
    public static boolean contains(Game.Entity e, double x, double y) {
        double cx=e.x+e.width/2, cy=e.y+e.height/2;
        double radians=Math.toRadians(e.rotation%360), dx=x-cx, dy=y-cy;
        double localX=dx*Math.cos(radians)-dy*Math.sin(radians);
        double localY=dx*Math.sin(radians)+dy*Math.cos(radians);
        return Math.abs(localX)<=e.width/2 && Math.abs(localY)<=e.height/2;
    }
    public static double normalize(double degrees) { return ((degrees%360)+360)%360; }
}
