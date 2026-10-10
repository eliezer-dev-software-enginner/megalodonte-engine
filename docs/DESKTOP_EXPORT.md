# Exportar jogos para Desktop e Android

**Exportar Desktop** salva o projeto e compila seus scripts Java 17 antes de gerar uma aplicação independente. O exportador usa jlink/jpackage do JDK completo, inclui JavaFX e o núcleo, e copia game.json, assets, scripts e avisos de licença. Não abre o editor no executável resultante. O diretório do jogo é resolvido junto ao JAR, sem depender do diretório corrente.

```bash
bash scripts/build-desktop.sh /pasta/do/jogo
bash scripts/build-android.sh /pasta/do/jogo
```

As saídas ficam em `build/desktop/` e `build/android/` **do jogo**. Linux gera app-image e tar.gz com binário/JVM; abra `build/desktop/<applicationId-final>/bin/<applicationId-final>`. A imagem é específica do SO/arquitetura do host. O build validado aqui é Linux x86_64. jpackage não faz cross-build de Windows/macOS.

`applicationId` em game.json identifica o aplicativo; documentos antigos usam `megalodonte.engine.game`. Android recebe ID/nome pelos parâmetros Gradle, para permitir instalar jogos distintos lado a lado. A Activity continua no namespace do runtime. O APK é debug, com mínimo/alvo API 35.

Os dois scripts chamam `open-build-folder.sh` somente depois do sucesso/publicação. O editor utiliza esses mesmos scripts, em segundo plano, com logs por plataforma. O helper usa xdg-open no Linux, open no macOS e explorer no Windows via ambiente shell compatível. Em sessão sem display, apresenta o caminho. `ENGINE_NO_OPEN=1` desativa a abertura para testes/CI. A falha do explorador é registrada e não invalida um artefato já produzido.

Não há instalador de release, assinatura de loja, AAB ou atualização automática. O Desktop contém seu runtime Java, mas utiliza as bibliotecas gráficas do sistema operacional.
