# Megalodonte Engine 2D

Editor JavaFX com UI orientada a dados (JSONC), baseado no código do `megalodonte-cliente`, sem copiar `.git`. Login/cadastro e seus testes permanecem como referência; a tela inicial agora é o editor.

Crie jogos com cenas, objetos e hierarquia, sprites, tilemaps Tiled, colisões AABB, câmera fixa ou seguindo um objeto, música, HUD e scripts Java. Play usa uma cópia do documento e preserva a edição. Desktop e Android executam o mesmo núcleo; a renderização usa Canvas JavaFX e Canvas Android. A importação aproveita o formato dos projetos libGDX, sem depender de libGDX no runtime.

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
- [Tilemaps, câmera, áudio e scripts Java](docs/GAMEPLAY.md)
- [Build Desktop independente e abertura da saída](docs/DESKTOP_EXPORT.md)

Validação realizada: testes desktop/núcleo/framework, renderização do editor, build do APK, lint Android e exportação de projeto externo. Ainda sem teste em dispositivo. Salve antes de usar Novo/Abrir: não há autosave nesta versão.

## HUD por cena

Use **HUD: texto** ou **HUD: imagem** na toolbar. Selecione pelo Canvas ou pela árvore (itens HUD). No inspector configure texto, identificador único, cor, fonte, tamanho, offsets e âncoras. Clique Aplicar HUD; arraste o elemento para mover ou a alça ciano inferior direita para redimensionar. Excluir remove a seleção, inclusive HUD. Imagens são copiadas para assets/; Trocar imagem HUD substitui a imagem selecionada. Salve o projeto após editar.

HUD é uma camada sobre o mundo, independente da câmera. Referência 1280×720 com escala uniforme pela área disponível. Âncoras esquerda/topo usam offsets para dentro; direita/baixo também usam offsets para dentro; centro usa deslocamento a partir do centro. Texto é de uma linha, recortado pela caixa; imagens ocupam a caixa inteira. No Android, a referência é a área segura sem barras/recorte. Layouts muito estreitos podem exigir ajustar caixas e offsets.

Projetos antigos continuam válidos com HUD vazia. **HUD: botão** cria um controle com nome de ação configurável. Scripts consultam essa ação; texto pode ser atualizado em tempo de execução. Android aceita vários toques simultâneos e libera as ações ao perder foco ou cancelar o gesto.

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

## Exemplo completo e programação

Para criar um projeto independente com tilemap, colisor, música WAV, câmera follow com fade, botões HUD e script de movimento:

```bash
bash gradlew createExample -PexampleDir=/caminho/novo/meu-exemplo
```

Escolha uma pasta sem game.json existente. Abra o game.json gerado na engine e clique Play. WASD/setas ou os botões de HUD movem o jogador. O botão **Scripts Java** abre o editor de código com números de linha, salvamento e diagnósticos do compilador. Anexe uma classe ao objeto, HUD ou cena; salve o projeto para persistir o anexo. Ao iniciar Play, todos os fontes de scripts/ são compilados novamente. **Abrir na IDE** prepara um projeto Gradle Java e abre o executável informado; **Pasta do projeto** abre o gerenciador de arquivos.

Os scripts usam Java 17 e a API engine-core, sem classes JavaFX/libGDX/Android. No APK são compilados antecipadamente pelo build Android. Não há compilador Java no celular. Consulte [o guia de gameplay](docs/GAMEPLAY.md) para um exemplo de código e limitações.

## Alex vs Bus

O projeto `../../games/alex-vs-bus/game.json` é a adaptação das 13 fases de Alex vs Bus para a engine. Seus scripts implementam plataformas, moedas, molas, cordas, checkpoints, perigos, pausa, cronômetro, cenas de chegada e final. Abra o documento na engine e clique Play. O projeto inclui assets originais, revisão da referência, créditos/licenças e ferramentas de geração/build.

Exportar Desktop cria o jogador independente com Java/JavaFX; Exportar Android usa o applicationId/nome do jogo. Após cada build bem-sucedida, a pasta da saída abre no explorador. Sprites aceitam animações por frames, espelhamento e visibilidade, e os scripts podem usar caixas de colisão menores que a imagem.

Exportações bem-sucedidas pelo terminal também registram o projeto como o último usado pelo editor. Para registrar um projeto criado externamente sem exportar, execute `bash scripts/remember-project.sh /pasta/do/jogo`. A preferência fica em `~/.config/megalodonte-engine/last-saved-project.txt`; o helper aceita `ENGINE_PREFERENCES_FILE` para testes/ambientes isolados.

A lista **CENAS**, no canto superior direito, permite abrir qualquer cena diretamente. Ela destaca a cena selecionada e identifica a cena inicial; abrir/criar projetos, adicionar cenas e usar Próxima cena mantém a lista sincronizada. Trocar de cena aplica os campos válidos do inspetor antes de mudar a seleção.

A cobertura dos recursos do runtime pela interface e os fluxos de animação/mapas estão em [Recursos do editor](docs/EDITOR_FEATURES.md).

Para gerar um APK Android assinado:

1. Na barra superior da engine, clique em **Configurações → Android**, informe a pasta do SDK e clique em **Salvar configurações**.
2. Em **Projeto**, configure `versionCode` e `versionName`.
3. Clique em **Projeto Android**, na barra superior, e aguarde a conclusão. A engine salva o jogo, atualiza assets, scripts e runtime e abre a pasta exportada no gerenciador de arquivos.
4. No Android Studio, clique em **Open** e selecione `<pasta-do-jogo>/build/android-release/project`. Aguarde a sincronização do Gradle; o SDK configurado na engine já estará indicado em `local.properties`.
5. Use **Build → Generate Signed Bundle / APK**, escolha **APK**, o módulo `app`, seu keystore/alias e a variante **release**. Conclua para gerar o instalador assinado.

Reutilize a mesma chave nas atualizações e incremente `versionCode`. Para AAB, escolha **Android App Bundle** no assistente. Sempre use **Projeto Android** novamente depois de alterar o jogo ou a engine: a pasta exportada é uma cópia e não se atualiza ao compilar dentro dela. A exportação anterior fica preservada em backup. Veja [Android Release](docs/ANDROID_RELEASE.md).

### Atalhos de edição da cena

Com o foco na cena ou na lista de objetos e um objeto/HUD selecionado:

- **Setas direcionais:** mover a seleção (0,1 unidade do mundo; HUD: 1 pixel de referência).
- **Shift + seta:** mover com passo 10 vezes maior.
- **Ctrl + seta:** ajuste fino, com passo 10 vezes menor.
- **Espaço + arrasto esquerdo** ou **arrasto com botão do meio:** mover a vista da cena.
- **Roda do mouse:** aproximar ou afastar a vista.

Selecionar um objeto fora da vista pela lista centraliza a visualização nele. A navegação não altera a câmera do jogo. Durante o Play, as setas são enviadas ao jogo; nos campos de texto, mantêm sua função normal de edição.

### Dados salvos e controles de jogo

Os hosts Desktop, Play do editor e Android suportam `GameContext.exportSave(valores, callback)` e `importSave(validador, callback)`. O JSON versionado inclui o ID do jogo e o estado persistente, separado do documento `game.json`; a importação valida o conteúdo antes de salvar. No Android, a seleção usa o [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files), sem solicitar acesso geral ao armazenamento.

Controles físicos são suportados no Android e no Desktop Linux, inclusive no Play do editor. Os scripts recebem `pad_left`, `pad_right`, `pad_up`, `pad_down`, `pad_a`, `pad_b` e `pad_start`; o analógico esquerdo e o direcional alimentam o movimento horizontal. Há zona morta de 20%, liberação de entradas ao desconectar/perder foco e proteção contra confirmação repetida ao trocar de cena segurando um botão. O Android usa as [entradas nativas de gamepad](https://developer.android.com/games/sdk/game-controller/controller-input); no Linux, o backend lê `/dev/input/js*` com o mapa de eixos/botões do kernel, via JNA. O sistema precisa reconhecer o dispositivo como controle físico.

Em **Configurações → Android**, informe o caminho do SDK ou use **Selecionar pasta…** e clique em **Salvar configurações**. A preferência é local ao computador. As builds Android usam esse SDK; `Projeto Android` gera `local.properties` com esse caminho para o Android Studio. Salvar a configuração também atualiza a exportação existente do jogo aberto.

A opção **Cena / câmera → Manter chão enquadrado durante saltos** mantém a câmera vertical estável durante saltos comuns. Ela acompanha quedas e novos pisos ao aterrissar, e sobe quando necessário para manter o jogador no enquadramento. Está ativada nas fases de Alex vs Bus.
