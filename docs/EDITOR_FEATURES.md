# Recursos do runtime disponíveis no editor

Revisão do modelo `Game` e dos recursos usados pelo Alex vs Bus, em 10/10/2026. A definição de objetos, animações, mapas, HUD e configurações fica em `game.json`; a lógica de corrida, perigos e navegação permanece nos scripts Java editáveis pela engine.

| Recurso | Lacuna encontrada | Onde editar agora |
|---|---|---|
| Nome e identificador do jogo | Só no JSON | **Projeto**: nome, applicationId e cena inicial |
| Seleção e organização de cenas | Percurso sequencial, operações incompletas | Lista **CENAS**; botão direito para configurar, duplicar, excluir e ordenar |
| Nome/fundo da cena | Nome/fundo não tinham formulário próprio | **Cena / câmera → Nome / fundo / opções** |
| Mundo e câmera | Já existiam controles | Inspetor: dimensões, posição, zoom, follow/fixed, velocidade, zonas mortas e fade; opções de clamp e alvo |
| Música | Importação/volume existiam; flags eram pouco visíveis | Inspetor e opções da cena: asset, volume, repetição, remoção |
| Assets de efeitos sonoros | Sem importação independente da música | **Assets**: importar WAV/MP3, ouvir/parar e copiar caminho para `playSound` nos scripts |
| Sprite estático | Importação existia; sem remoção/opções completas | Sprite, remover sprite e **Colisor / propriedades** |
| Animações nomeadas | Só autoradas por JSON/scripts | **Animações / prévia**: criar, renomear, excluir, frames, ordem, FPS, loop e animação inicial |
| Importação de animações | Sem sequência/spritesheet | Importar vários frames ou extrair spritesheet por origem, tamanho, quantidade e passos X/Y |
| Prévia de animações | Exigia Play | **Animar cena** pré-visualiza animações iniciais no viewport; janela de animações tem Play/pausa, reinício, seleção de frame e linha do tempo; não executa scripts nem modifica o relógio do documento |
| Tempo inicial da animação | Só no JSON | **Colisor / propriedades**: tempo inicial em segundos |
| Visibilidade e flipX | Só no JSON/scripts | **Colisor / propriedades**: visível, espelhar, sólido, controlável |
| Colisor separado do sprite | Só no JSON | **Colisor / propriedades**: offsets e dimensões, 0 usa o tamanho do sprite |
| Visualização de colisores | Sem desenho de diagnóstico | **Colisores** na barra; caixa do objeto selecionado também aparece; inclui obstáculos de mapas |
| Propriedades usadas pelos scripts | Só no JSON | Tabela chave/valor em **Colisor / propriedades**, com criação e remoção |
| Hierarquia, rotação e duplicação | Já existiam | Inspetor do objeto: pai, remover vínculo, duplicar, rotação e linha de relação |
| Ordem de desenho | Só pela ordem do JSON | **Trazer à frente / Enviar para trás** em objeto e HUD |
| Texto, imagem e botão HUD | Controles básicos existiam | Inspetor HUD: ID, texto, ação, posição, tamanho, fonte, cor; scripts e imagem de botão |
| Visibilidade/tipo/asset HUD | Opções incompletas | **Visibilidade / tipo / imagem** e duplicar HUD |
| Âncoras HUD | Offsets relativos à tela tornavam menus confusos | Âncoras e **Área da HUD**: tela segura ou conteúdo 1280×720 centralizado; mudanças preservam a posição visual |
| Componentes ocultos | Difíceis de inspecionar | Seleção pela lista mostra o elemento oculto em transparência; ele continua oculto no jogo |
| Tilemap vazio | Só importação Tiled | **Novo mapa** cria a grade e primeira camada |
| Geometria do mapa | Só posição/escala | Selecionar mapa → **Editar mapa / camadas / tiles**: nome, linhas/colunas, tamanho de célula, posição e escala |
| Camadas | Só importadas/JSON | Aba **Camadas e pintura**: adicionar, excluir, ordenar, nome, offsets e opacidade |
| Células e flips de tile | Sem pintura | Pincel com tile ID, apagar com botão direito, flips X/Y/diagonal; rolagem em mapas extensos |
| Tileset/recortes de atlas | Só importados/JSON | Aba **Tileset**: criar/excluir tile, importar imagem, selecionar asset e recorte X/Y/largura/altura |
| Obstáculos do mapa | Só importados/JSON | Aba **Colisores**: adicionar, excluir e editar caixas em pixels locais |
| Scripts Java de cena/objeto/HUD | Já existiam | Editor de fontes da engine, anexar/remover, Abrir IDE, compilação no Play/exportação |
| Teste, publicação e pasta de saída | Desktop não tinha exportação própria | Play/Stop, Exportar Desktop/Android; pasta abre após sucesso; terminal registra projeto recente |

## Uso de animações

Selecione um objeto e abra **Animações / prévia**. Escolha uma animação existente ou crie uma nova. Importe os PNG/JPEG, ordene os frames com Subir/Descer, ajuste FPS e repetição e marque **Usar ao iniciar** se desejar. **Atualizar definição** prepara a definição no formulário; **Aplicar ao objeto** valida e aplica as animações ao objeto. Salve o projeto para persistir e exporte novamente para atualizar o jogo.

Ao renomear uma animação referenciada em `context.animate`, atualize também o nome no script.

O spritesheet aceita uma faixa horizontal (`Passo X = largura`, `Passo Y = 0`), vertical (`Passo X = 0`, `Passo Y = altura`) ou diagonal. Cada recorte precisa caber na imagem. Os frames extraídos viram assets PNG comuns, compatíveis com os dois hosts.

## Edição de mapas

Crie um mapa ou selecione um existente na lista de objetos. Aplique as propriedades da camada antes de selecionar outra. A aba de pintura usa uma janela de 28×20 células com rolagem, evitando alocar um canvas do tamanho de uma fase inteira. Alterar linhas/colunas preserva a interseção das células existentes. Os tiles usados precisam ser apagados antes de remover seu ID.

As caixas de obstáculos usam pixels locais do mapa e o mesmo `unitScale` do runtime. Colisores de objetos da cena usam unidades de mundo. As alterações ficam em uma cópia até **Aplicar**; o editor valida o documento antes de aceitar. Fechar sem aplicar descarta a cópia.

## Dados e execução

IDs de objetos, cenas e mapas são editáveis nos formulários. Renomear um objeto atualiza vínculos de pai e alvo da câmera; renomear uma cena atualiza cena inicial e última cena editada. O editor valida IDs únicos e não vazios. Referências escritas nos scripts e propriedades personalizadas devem ser atualizadas pelo autor. HUD já permite editar seu ID, desde que único na cena. Versão do documento e última cena editada são administradas pela engine.

Entrada, gravidade, pontuação, checkpoint, tempo, eventos de áudio, troca de animação e estados de sessão são lógica do script. O editor oferece os objetos/dados e a edição Java usados para implementar essa lógica; não há geração automática de comportamento específico de Alex vs Bus. Scripts são copiados ao duplicar cenas, então revise destinos que referenciam outras cenas por ID.

Prévia não substitui Play: a primeira mostra frames do sprite; Play executa lifecycle, scripts, câmera, áudio e colisões no runtime. O relógio da prévia pertence ao editor ou à janela de animações. **Animar cena** reproduz as animações iniciais escolhidas nos objetos; não dispara ações que scripts executariam durante Play. Dados autorados pela interface usam o mesmo Codec, modelos e renderização do Desktop e Android.

## Propriedades e condições das HUDs

Selecione a HUD e abra **Propriedades / condições HUD**. A aba **Propriedades** permite adicionar booleanos, números e textos; o valor é o estado inicial do componente e é copiado ao duplicar. Booleanos são editados por checkbox. Clique em **Aplicar** para atualizar a prévia e salve o projeto.

Em **Condição visual**, indique o nome de uma propriedade booleana, a imagem para `true`, a imagem para `false` e, opcionalmente, **Bloquear clique quando false**. Sem condição, a HUD continua usando a imagem padrão. Condições com propriedades inexistentes ou de outro tipo são rejeitadas antes de aplicar. A avaliação é a mesma no editor, no Desktop e no Android.

Nos scripts Java, use `hud.booleanProperty("liberado", false)` para ler e `hud.setBooleanProperty("liberado", true)` para alterar o valor. As condições atualizam a imagem e a interação sem trocar manualmente o asset. O editor mostra os valores iniciais; o runtime usa uma cópia do documento, por isso o progresso do jogador não modifica a configuração autoral.

### Navegação da cena

- Segure **Espaço** e arraste com o botão esquerdo para mover a vista; também é possível arrastar com o botão do meio.
- Use a roda do mouse para aproximar ou afastar a vista.
- Selecione um objeto na lista à esquerda: se ele estiver fora da vista, o editor centraliza nele e reduz o zoom quando necessário para mostrar seu tamanho completo. A posição considera a hierarquia e a rotação.
- Cada cena mantém sua própria vista durante a sessão. Navegar não altera a câmera exportada do jogo, seu alvo ou suas configurações no inspetor.

### Áreas de colisão e gatilhos

`Colisor` ativa ou desativa os contatos do objeto. `Gatilho` (em **Colisores** / opções do objeto) detecta contato sem bloquear o movimento, para moedas, perigos, molas e cordas. Um colisor ativado sem gatilho funciona como obstáculo sólido. O contorno ciano mostra a seleção do sprite; o amarelo tracejado mostra a área de colisão ativa.

Scripts devem usar `context.overlaps(a, b)` e `context.collisionBounds(objeto)`, que consideram deslocamento, dimensões do colisor, hierarquia, rotação e espelhamento. O contato retorna falso quando o colisor de um dos objetos está desativado. A sobrecarga `overlaps(a, b, margem)` permite uma tolerância explícita (por exemplo, para encostar em uma mola). Zero nas dimensões do colisor mantém a compatibilidade com objetos antigos, usando suas dimensões como padrão.

### Mover seleção pelo teclado

Com o foco na cena ou na lista de objetos, as **setas** movem o objeto selecionado em passos de 0,1 unidade do mundo (HUD: 1 pixel de referência). **Shift + seta** multiplica o passo por 10; **Ctrl + seta** divide por 10. O inspetor acompanha a posição e objetos vinculados preservam a hierarquia. Nas HUDs, o movimento acompanha a direção da seta independentemente da âncora. Campos de texto preservam o uso normal das setas. Durante o Play, as setas continuam sendo entradas do jogo.
