# Megalodonte Engine 2D

Editor JavaFX com UI orientada a dados (JSONC), baseado no código do `megalodonte-cliente`, sem copiar `.git`. Login/cadastro e seus testes permanecem como referência; a tela inicial agora é o editor.

Esta primeira implementação permite criar/abrir/salvar projetos externos, adicionar cenas, definir a cena inicial, criar/selecionar/arrastar/excluir objetos, editar transformações, importar sprites, alternar colisores AABB e controle do jogador. Play usa cópia do documento e movimento WASD/setas, preservando a edição. Android executa o mesmo núcleo com Canvas nativo e controles por toque.

```bash
bash gradlew run
bash gradlew test :engine-core:test
# Depois de salvar um jogo em uma pasta:
bash scripts/build-android.sh /caminho/meu-jogo
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`. `adb install -r` instala no dispositivo. O build Android exige SDK 35 e JDK 21 com compilador; configure `ANDROID_HOME` e `ANDROID_JAVA_HOME` se necessário. Alvo/mínimo API 35, seguindo megalodonte-android. Para compilar a demonstração: `JAVA_HOME=/opt/android-studio-for-platform/jbr ANDROID_HOME=$HOME/Android/Sdk bash android/gradlew -p android :app:assembleDebug`.

O botão Exportar Android salva o projeto e executa o build em segundo plano. O log fica em `build/android-build.log` na pasta do jogo e o APK em `build/android/game-debug.apk`. O script também permite compilar pelo terminal. O padrão local usa o JDK do Android Studio em `/opt/android-studio-for-platform/jbr`; configure `ANDROID_JAVA_HOME` em outras máquinas. É um APK debug instalável; assinatura de release e AAB ainda não estão implementadas.

O editor exige JDK 25. Dependências Megalodonte são resolvidas por composite builds locais. `game.json` e `assets/` ficam na pasta do jogo. Sprites são copiados, nunca referenciados por caminhos absolutos. Salve após editar. Ao abrir um documento incompatível, o editor apresenta o erro e preserva o projeto atual.

- [Arquitetura e referência Nojo](docs/ARCHITECTURE.md)
- [Estado e próximos passos](docs/STATUS.md)

Validação realizada: testes desktop/núcleo/framework, renderização do editor, build do APK, lint Android e exportação de projeto externo. Ainda sem teste em dispositivo. Salve antes de usar Novo/Abrir: não há autosave nesta versão.

## HUD por cena

Use **HUD: texto** ou **HUD: imagem** na toolbar. Selecione pelo Canvas ou pela árvore (itens HUD). No inspector configure texto, identificador único, cor, fonte, tamanho, offsets e âncoras. Clique Aplicar HUD; arraste o elemento para mover ou a alça ciano inferior direita para redimensionar. Excluir remove a seleção, inclusive HUD. Imagens são copiadas para assets/; Trocar imagem HUD substitui a imagem selecionada. Salve o projeto após editar.

HUD é uma camada sobre o mundo, independente da câmera. Referência 1280×720 com escala uniforme pela área disponível. Âncoras esquerda/topo usam offsets para dentro; direita/baixo também usam offsets para dentro; centro usa deslocamento a partir do centro. Texto é de uma linha, recortado pela caixa; imagens ocupam a caixa inteira. No Android, a referência é a área segura sem barras/recorte. Layouts muito estreitos podem exigir ajustar caixas e offsets.

Projetos antigos continuam válidos com HUD vazia. O formato scene.hud persiste elementos text/image; botões, ações, multitoque e valores dinâmicos ficam para a próxima etapa.

## Abrir e restaurar projetos

Abrir seleciona o arquivo game.json. Após um salvamento bem-sucedido, a engine guarda o caminho em ~/.config/megalodonte-engine/last-saved-project.txt e restaura esse projeto na próxima inicialização. Abrir também registra o projeto ativo para restauração. Se o arquivo foi movido, removido ou está inválido, o editor inicia com a demonstração e apresenta a falha; use Abrir para localizar o projeto e Salvar para atualizar o caminho.

Importar uma imagem (HUD ou sprite) não exige salvar antes: o seletor mostra somente imagens. A engine mantém uma cópia temporária para preview e inclui o asset no projeto quando você clicar Salvar. Cancelar o seletor não cria HUD nem altera a imagem selecionada.

O seletor de imagens lembra o último diretório utilizado, compartilhado entre HUD e sprites e preservado entre execuções. Cancelamento não altera a preferência; se a pasta deixou de existir, usa assets/ do projeto quando disponível.

Objetos aceitam Rotação no inspector, em graus no sentido horário ao redor do centro. Clique Aplicar e Salvar para persistir. Desktop e Android renderizam a rotação; seleção no Canvas acompanha o retângulo rotacionado. Os colisores continuam AABB nas coordenadas originais, sem rotação física nesta etapa.

## Duplicação e vínculos

Selecione um objeto e use Duplicar objeto no inspector. A cópia recebe ID próprio, preserva propriedades e o pai e ganha pequeno deslocamento; filhos não são duplicados automaticamente.

Para relacionar objetos, selecione o filho, clique Escolher pai e clique no pai pela cena ou lista. Escape cancela. Uma linha tracejada conecta seus centros no editor (oculta no Play/Android). O vínculo mantém a aparência inicial; depois, posição e rotação do filho passam a ser locais ao pai, herdando movimento e rotação. Desvincular preserva sua posição no mundo. Excluir um pai desvincula os filhos diretos sem movê-los. Ciclos e referências a objetos inexistentes são rejeitados. Relações ficam na mesma cena.

Tamanho não é herdado como escala. Colisão permanece AABB em posições de mundo, sem rotação física do colisor; objetos relacionados como ancestrais/descendentes não bloqueiam uns aos outros.

Salvar aplica automaticamente os campos do inspector antes de gravar; campos inválidos impedem o salvamento e mostram o motivo. A cena em edição é restaurada junto com o projeto. O topo identifica explicitamente o caminho absoluto do game.json ativo, ou informa que o projeto ainda não foi salvo.
