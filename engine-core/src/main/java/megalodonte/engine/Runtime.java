package megalodonte.engine;
/** Shared deterministic movement and AABB collisions; play uses a detached document. */
public final class Runtime {
    public final Game game;
    public final Game.Scene scene;
    public Runtime(Game source) {
        game = Codec.copy(source);
        scene = game.scenes.stream().filter(s -> s.id.equals(game.entrySceneId)).findFirst().orElseThrow();
    }
    public void update(double dt, double horizontal, double vertical) {
        dt = Math.max(0, Math.min(dt, 0.05));
        double length = Math.hypot(horizontal, vertical);
        if (length > 1) { horizontal /= length; vertical /= length; }
        for (Game.Entity e : scene.objects) if (e.player) {
            // Substeps keep fast objects from crossing thin colliders.
            double dx = horizontal * e.speed * dt, dy = vertical * e.speed * dt;
            int steps = Math.max(1, (int)Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 0.05));
            steps = Math.min(steps, 1000);
            for (int i = 0; i < steps; i++) { move(e, dx/steps, 0); move(e, 0, dy/steps); }
        }
    }
    private void move(Game.Entity e, double dx, double dy) {
        double x = e.x, y = e.y;
        e.x = Math.max(0, Math.min(scene.worldWidth-e.width, e.x+dx));
        e.y = Math.max(0, Math.min(scene.worldHeight-e.height, e.y+dy));
        for (Game.Entity other : scene.objects) if (other != e && other.solid && overlaps(e,other)) { e.x=x; e.y=y; return; }
    }
    public static boolean overlaps(Game.Entity a, Game.Entity b) {
        return a.x < b.x+b.width && a.x+a.width > b.x && a.y < b.y+b.height && a.y+a.height > b.y;
    }
}
