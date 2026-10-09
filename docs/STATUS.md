# Estado

Implementado: editor JSONC, projetos externos, salvamento por arquivo temporário e substituição, cenas e entrada, objetos, sprites, inspector, arraste, Play isolado, controle e colisão compartilhados, runtime e build Android debug em segundo plano pela UI, com log e publicação após sucesso.

Pendências explícitas: importador Nojo v2, câmera móvel/follow, ações/botões de HUD e estados dinâmicos, tilemaps TMX, scripts Java portáveis compilados antecipadamente no Android, áudio, animação, desfazer/refazer, autosave, gerenciamento de assets, progresso detalhado/cancelamento de build Android, release assinado/AAB, empacotamento desktop. Skins Scene2D ficam fora do escopo.

Não existe paridade completa com o Nojo. O núcleo atual estabelece um jogo simples jogável nos dois destinos e pontos de extensão. Validação visual e em dispositivo deve ser registrada a cada incremento.

## Validação desta entrega

Em 08/10/2026: testes do cliente/editor JavaFX e quatro testes do núcleo passaram; testes de megalodonte-script-app passaram após adicionar acesso a componentes. Preview renderizado conferido em build/preview/editor.png. assembleDebug e lintDebug Android passaram com o JDK do Android Studio. Exportação por script de uma pasta externa também gerou APK. Não houve execução em emulador ou dispositivo nesta etapa.

Salvar é manual. Novo/Abrir substituem o documento em memória; salve antes de trocar. O contrato atual aceita somente a versão 1 da nova engine.

## Correção da tela preta na inicialização

O caminho completo Context.useView + bootstrap foi incluído no teste de janela real. A centralização reativa chamava centerOnScreen durante eventos GTK e podia bloquear o loop nativo, deixando a janela preta. A posição inicial agora é definida antes de mostrar a janela, sem listeners de resize ou centralização posterior. Scene nasce no tamanho configurado pelo cliente. Também foi removida a inicialização reflexiva do AWT usada para nomear a janela. O teste verifica pulsos, dimensões da viewport e pixels da janela real; classes de testes JavaFX rodam em processos separados.

## Toolbar cortada em janelas menores

O inspector impunha altura mínima à linha central. Quando a área útil era menor que o layout, StackPane centralizava o conteúdo excedente e cortava o topo. Inspector agora usa Scroll no JSONC, permitindo reduzir a altura central e preservar a toolbar. Teste de janela real em 700px verifica que o primeiro botão permanece dentro da Scene.

## Área segura no Android

O host Android aplica WindowInsets de systemBars e displayCutout ao container do jogo, seguindo o backend megalodonte-android. Canvas, texto de instrução e coordenadas de toque usam a área interna disponível; barras e recortes ficam fora dela. Insets são reaplicados quando o sistema altera as barras ou a orientação. A tela de erro também respeita a área segura.

## HUD inicial

Texto e imagem por cena, âncoras em dois eixos, geometria compartilhada desktop/Android, seleção/arraste/redimensionamento e inspector JSONC implementados. Modelo versão 1 permanece compatível: cenas sem hud carregam lista vazia. Play usa cópia incluindo HUD. Texto de instruções Android fixo foi removido, para não competir com a HUD criada pelo jogo.

## Seletor de salvamento

O primeiro Salvar usa FileChooser.showSaveDialog, com nome inicial game.json e título Salvar projeto. A pasta que contém o arquivo continua sendo a raiz do jogo (assets/ ao lado). Salvamentos seguintes atualizam o mesmo arquivo. O novo projeto continua impedido de sobrescrever um projeto existente; abra-o pela ação Abrir.

## Último projeto salvo

Abrir agora usa showOpenDialog para game.json, consistente com Salvar. Um carregamento inválido preserva o documento ativo. Salvar registra o caminho apenas depois da gravação do jogo; a preferência local usa substituição por arquivo temporário. onMount restaura após montar Canvas/árvore. Falha na preferência não transforma um salvamento concluído em falha do jogo. Testes usam preferência isolada em build/test-state.
