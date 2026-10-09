package megalodonte.engine;
/** A fresh instance per attachment and Play session; never imports JavaFX or Android. */
public interface GameScript {
    default void onStart(GameContext context) { }
    default void onUpdate(double deltaTime,GameContext context) { }
    default void onDestroy(GameContext context) { }
}
