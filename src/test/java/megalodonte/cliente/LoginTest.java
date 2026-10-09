package megalodonte.cliente;

import megalodonte.jsonc.*;
import megalodonte.script.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginTest {
    @Test void loginRequiresValidFieldsAndClearsPasswordAfterLocalDemo() {
        try (var application = new ScriptApplication(new AppConfig("Cliente", "login", "megalodonte.cliente", "/views", 520, 680, 420, 640), Login.class.getClassLoader())) {
            var session = application.current();
            session.invoke("onSubmit");
            assertEquals("Email é obrigatório.", session.states().get("mensagem"));
            session.states().string("email").set("wrong"); session.invoke("onSubmit");
            assertTrue(session.states().string("mensagem").get().contains("email válido"));
            session.states().string("email").set("cliente@example.com"); session.invoke("onSubmit");
            assertEquals("Senha é obrigatório.", session.states().get("mensagem"));
            session.states().string("senha").set("secret"); session.invoke("onSubmit");
            assertTrue(session.states().string("mensagem").get().contains("Autenticação será integrada"));
            assertEquals("", session.states().get("senha"));
        }
    }
}
