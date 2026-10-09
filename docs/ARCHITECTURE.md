# Arquitetura

A referência nojo-engine separa layout do editor, documento do jogo, projetos externos e execução. Sua versão atual contém cenas, câmera follow, HUD ancorado, tilemaps, scripts Java, exportação e skins Scene2D. A nova implementação retoma a separação, usando o framework declarativo Megalodonte para o editor.

`src/main/resources/views/editor.view.jsonc` declara painéis, ações, campos e estados. `Editor` implementa operações e adapta o Canvas JavaFX ao container `viewport`. O framework script-app ganhou `protected component(id)`, delegando à busca já validada da sessão; isso permite montar superfícies específicas sem duplicar o carregador ou o lifecycle. O timer é encerrado em onDestroy.

`engine-core` é Java 17 + Gson, sem JavaFX, Android ou libGDX. Define documento versionado, validação, cópia de Play e atualização/colisão. Coordenadas em unidades de mundo, origem inferior esquerda, visualização com proporção preservada. Movimento por eixos e subpassos, delta limitado a 50ms. Colisão AABB é uma base simples, sem simulação física.

`android` tem build separado para evitar dependências desktop e JDK 25 no APK. Segue SDK/minSdk 35 e AGP 9.0.1 do backend megalodonte-android. A superfície do jogo usa Canvas Android, pois não é uma árvore de widgets de UI. O mesmo engine-core é compilado nos dois destinos. Não é necessário modificar android-bridge para desenhar o jogo.

`game.json` versão 1 é um contrato novo; não é importação automática de `game-data.json` do Nojo. O formato é exemplificado em android/app/src/main/assets/game.json. A API de scripts libGDX e skins não é transportada.

A exportação copia somente game.json e assets/ para uma pasta temporária. Logs, fontes e APKs antigos não entram nos assets do APK. O resultado é publicado na pasta externa após assembleDebug terminar. O editor chama o script em uma thread de espera e publica o resultado pela thread JavaFX.
