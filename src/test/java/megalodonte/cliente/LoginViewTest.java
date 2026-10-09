package megalodonte.cliente;

import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.image.WritableImage;
import megalodonte.base.theme.ThemeManager;
import megalodonte.theme.DefaultTheme;
import megalodonte.jsonc.*;
import megalodonte.script.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class LoginViewTest {
    @Test void rendersActualLoginAndProducesPreview() throws Exception {
        var started = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); started.countDown(); });
        assertTrue(started.await(15, TimeUnit.SECONDS));
        var task = new FutureTask<Void>(() -> {
            ThemeManager.setTheme(new DefaultTheme());
            try (var screens = new ScriptApplication(new AppConfig("Cliente", "login", "megalodonte.cliente", "/views", 520, 680, 420, 640), Login.class.getClassLoader())) {
                var session = screens.current();
                var root = (Parent) screens.render().getJavaFxNode();
                var scene = new Scene(root, 520, 680);
                ((javafx.scene.layout.Region) root).resize(520, 680);
                root.applyCss(); root.layout();
                var email = (TextField) ((StackPane) session.component("emailInput").getJavaFxNode()).getChildren().getFirst();
                var password = (PasswordField) ((StackPane) session.component("senhaInput").getJavaFxNode()).getChildren().getFirst();
                var button = (Button) session.component("entrarButton").getJavaFxNode();
                assertEquals("Entrar", button.getText());
                assertTrue(button.isDefaultButton());
                savePreview(root, "login");
                assertTrue(email.getBoundsInParent().getWidth() > 200, "Email width: " + email.getBoundsInParent().getWidth());
                assertTrue(password.getBoundsInParent().getHeight() > 20, "Password height: " + password.getBoundsInParent().getHeight());
                email.setText("cliente@example.com"); password.setText("secret"); button.fire();
                assertEquals("", password.getText());
                assertTrue(((javafx.scene.text.Text) session.component("mensagemText").getJavaFxNode()).getText().contains("Formulário válido"));
                ((Button) session.component("criarContaButton").getJavaFxNode()).fire();
                assertEquals("cadastro", screens.current().id());
                assertThrows(IllegalStateException.class, session::states);
                assertNull(button.getOnAction());
                var cadastro = screens.current();
                root.applyCss(); root.layout();
                savePreview(root, "cadastro");
                var nome = field(cadastro, "nomeInput");
                var telefone = field(cadastro, "telefoneInput");
                var cadastrar = (Button) cadastro.component("cadastrarButton").getJavaFxNode();
                assertTrue(cadastrar.isDefaultButton());
                nome.setText("Maria Silva");
                field(cadastro, "emailInput").setText("maria@example.com");
                telefone.setText("11987654321");
                assertEquals("(11) 98765-4321", telefone.getText());
                assertEquals("11987654321", cadastro.states().get("telefone"));
                cadastrar.fire();
                assertTrue(cadastro.states().string("mensagem").get().contains("Cadastro válido"));
                // Both views must remain usable at the minimum client area.
                ((javafx.scene.layout.Region) root).resize(420, 600);
                root.applyCss(); root.layout();
                var bounds = root.sceneToLocal(cadastrar.localToScene(cadastrar.getBoundsInLocal()));
                assertTrue(bounds.getMinY() >= 0 && bounds.getMaxY() <= 600, "Registration button must fit the window");
                ((Button) cadastro.component("voltarLoginButton").getJavaFxNode()).fire();
                assertEquals("login", screens.current().id());
                assertThrows(IllegalStateException.class, cadastro::states);
                assertEquals("", screens.current().states().get("email"));
                assertEquals("", screens.current().states().get("senha"));
                assertSame(root, screens.render().getJavaFxNode());
                ((Button) screens.current().component("criarContaButton").getJavaFxNode()).fire();
                assertEquals("cadastro", screens.current().id());
                assertEquals("", screens.current().states().get("nome"));
            }
            try (var editor = new ScriptApplication(AppConfig.load(Login.class.getClassLoader()), Login.class.getClassLoader())) {
                var root = (Parent) editor.render().getJavaFxNode();
                new Scene(root, 1280, 850);
                ((javafx.scene.layout.Region)root).resize(1280,850);
                root.applyCss(); root.layout(); editor.onMount();
                assertEquals("editor", editor.current().id());
                editor.current().invoke("addObject");
                editor.current().invoke("togglePlay");
                editor.current().invoke("togglePlay");
                editor.current().invoke("addScene");
                editor.current().invoke("setEntry");
                editor.current().invoke("newProject");
                root.applyCss(); root.layout();
                editor.current().invoke("addHudText");
                editor.current().states().set("hudText","Moedas: 42");
                editor.current().invoke("applyHud");
                root.applyCss(); root.layout();
                savePreview(root,"editor");
                var viewport = editor.current().component("viewport").getJavaFxNode();
                assertTrue(viewport.getBoundsInLocal().getWidth()>300);
            }
            return null;
        });
        try { Platform.runLater(task); task.get(20, TimeUnit.SECONDS); }
        finally { Platform.exit(); }
    }
    private static TextField field(UiSession session, String id) {
        return (TextField) ((StackPane) session.component(id).getJavaFxNode()).getChildren().getFirst();
    }
    private static void savePreview(Parent root, String name) throws Exception {
        WritableImage snapshot = root.snapshot(null, null);
        var png = new BufferedImage((int) snapshot.getWidth(), (int) snapshot.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < png.getHeight(); y++) for (int x = 0; x < png.getWidth(); x++) png.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
        Path path = Path.of("build/preview/" + name + ".png"); Files.createDirectories(path.getParent());
        ImageIO.write(png, "png", path.toFile());
    }
}
