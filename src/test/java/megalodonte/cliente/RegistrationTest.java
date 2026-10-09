package megalodonte.cliente;

import megalodonte.jsonc.*;
import megalodonte.script.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegistrationTest {
    @Test void registrationValidatesNameEmailAndPhoneWithoutClaimingAccountCreated() {
        var base = new AppConfig("Cliente", "login", "megalodonte.cliente", "/views", 520, 680, 420, 640);
        var config = new AppConfig(base.name(), "cadastro", base.scriptsPackage(), base.views(), base.width(), base.height(), base.minWidth(), base.minHeight());
        try (var application = new ScriptApplication(config, Cadastro.class.getClassLoader())) {
            var session = application.current();
            session.invoke("onSubmit");
            assertTrue(session.states().string("mensagem").get().startsWith("Nome"));
            session.states().string("nome").set("Maria Silva");
            session.states().string("email").set("email inválido");
            session.invoke("onSubmit");
            assertTrue(session.states().string("mensagem").get().contains("email válido"));
            session.states().string("email").set("maria@example.com");
            session.states().string("telefone").set("119876");
            session.invoke("onSubmit");
            assertTrue(session.states().string("mensagem").get().startsWith("Telefone"));
            session.states().string("telefone").set("11987654321");
            session.invoke("onSubmit");
            assertEquals("Cadastro válido. A criação da conta será integrada na próxima etapa.", session.states().get("mensagem"));
            assertEquals("Maria Silva", session.states().get("nome"));
        }
    }
}
