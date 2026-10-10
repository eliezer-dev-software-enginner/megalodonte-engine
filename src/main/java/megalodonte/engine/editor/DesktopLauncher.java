package megalodonte.engine.editor;
/** Kept separate from Application so JavaFX works from the packaged classpath. */
public final class DesktopLauncher {
    public static void main(String[] args){javafx.application.Application.launch(DesktopGameApp.class,args);}
}
