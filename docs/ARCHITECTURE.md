# Arquitetura

A referência nojo-engine separa layout do editor, documento do jogo, projetos externos e execução. Sua versão atual contém cenas, câmera follow, HUD ancorado, tilemaps, scripts Java, exportação e skins Scene2D. A nova implementação retoma a separação, usando o framework declarativo Megalodonte para o editor.

`src/main/resources/views/editor.view.jsonc` declara painéis, ações, campos e estados. `Editor` implementa operações e adapta o Canvas JavaFX ao container `viewport`. O framework script-app ganhou `protected component(id)`, delegando à busca já validada da sessão; isso permite montar superfícies específicas sem duplicar o carregador ou o lifecycle. O timer é encerrado em onDestroy.

`engine-core` é Java 17 + Gson, sem JavaFX, Android ou libGDX. Define documento versionado, validação, cópia de Play e atualização/colisão. Coordenadas em unidades de mundo, origem inferior esquerda, visualização com proporção preservada. Movimento por eixos e subpassos, delta limitado a 50ms. Colisão AABB é uma base simples, sem simulação física.

`android` tem build separado para evitar dependências desktop e JDK 25 no APK. Segue SDK/minSdk 35 e AGP 9.0.1 do backend megalodonte-android. A superfície do jogo usa Canvas Android, pois não é uma árvore de widgets de UI. O mesmo engine-core é compilado nos dois destinos. Não é necessário modificar android-bridge para desenhar o jogo.

`game.json` versão 1 é um contrato novo; não é importação automática de `game-data.json` do Nojo. O formato é exemplificado em android/app/src/main/assets/game.json. A API de scripts libGDX e skins não é transportada.

A exportação copia somente game.json e assets/ para uma pasta temporária. Logs, fontes e APKs antigos não entram nos assets do APK. O resultado é publicado na pasta externa após assembleDebug terminar. O editor chama o script em uma thread de espera e publica o resultado pela thread JavaFX.


## Mapas, câmera e áudio

TiledImporter converte TMX/TMJ e tilesets TSX/TSJ para DTOs portáveis em Game.TileMap; texturas são copiadas por PendingAssets. game.json contém as camadas e regiões das imagens, eliminando dependência do arquivo Tiled original na execução. Coordenadas Tiled com origem superior esquerda são convertidas para o mundo da engine. TileDraw elimina células fora da câmera e entrega região de textura/matriz de espelhamento a cada backend. Colisores retangulares do mapa usam a mesma rotina de movimento AABB.

CameraView centraliza projeção/inversão, zoom, follow com suavização exponencial, zona morta e limite ao mundo. HUD permanece em coordenadas de referência, fora do recorte do mundo. Runtime controla fade de entrada/transição. Áudio é responsabilidade do host: ProjectMusic/JavaFX MediaPlayer no desktop, MediaPlayer Android no celular; a troca de cena e encerramento liberam os players. Os dois backends respeitam volume/repetição por cena.

## Scripts

GameScript e GameContext estão no núcleo Java 17. Runtime cria instância por anexo e chama onStart/onUpdate/onDestroy; changeScene é adiado para o fim da atualização. O documento original permanece intacto durante Play. Classes inexistentes, falhas de compilação e exceções de scripts produzem mensagem de erro.

ScriptCompiler usa JavaCompiler --release 17, diretório exclusivo de classes e URLClassLoader por compilação. O loader e as classes temporárias são liberados ao parar. ScriptWindow monta o editor JavaFX especializado, mantendo o editor principal orientado ao JSONC. prepareIde fornece bibliotecas locais e arquivos Gradle sem sobrescrever configuração existente do usuário. O exportador copia scripts/ separadamente dos assets; Android inclui essas fontes no source set Java e D8 compila as classes no APK. Fontes, cache .engine e arquivos de build não entram nos assets. Scripts são código confiável do autor, com os mesmos privilégios do aplicativo; não há sandbox de execução.
