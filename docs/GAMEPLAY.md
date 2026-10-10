# Tilemaps, câmera, áudio e scripts

## Importar um mapa

Clique **Mapa Tiled** e selecione um arquivo `.tmx` ou `.tmj` exportado pelo Tiled. Use mapa **ortogonal e finito**. Tilesets externos `.tsx`/`.tsj` e suas imagens precisam estar acessíveis no momento da importação. A engine copia as imagens para `assets/` e grava camadas/tiles/colisores em `game.json`; o jogo exportado não depende do mapa original.

São aceitos atlas com margem/espaçamento, coleções de imagens, camadas e grupos com offset/opacidade/visibilidade, dados XML/CSV/JSON e base64 sem compressão, zlib ou gzip. Flags de espelhamento horizontal, vertical e diagonal são preservadas. Objetos retangulares em grupos com nome contendo `collision`, `colis`, `solid`, `ground` ou `obstacle` viram colisores. Uma propriedade booleana `solid` no objeto sobrescreve essa escolha. Camadas de objetos não criam personagens automaticamente.

Inicialmente a largura de um tile equivale a uma unidade de mundo. Selecione **Mapa • nome** na árvore para editar X/Y e escala em unidades por pixel; a origem é o canto inferior esquerdo. Aplicar mapa, Salvar e iniciar Play incorporam os campos de posição/escala. O tamanho do mundo é ampliado na importação e pode ser ajustado em **Cena / câmera**. Mapas ficam atrás dos objetos. As células fora da câmera não são desenhadas. Colisões seguem AABB, sem rotação física.

Limites: mapas infinitos, isométricos/hexagonais e compressão zstd não são suportados. Animação de tiles fica estática; camadas de imagem, polígonos/elipses e colisores rotacionados não são renderizados/importados. Avisos aparecem na mensagem do editor. Propriedades de gameplay, parallax, tileoffset e colisores definidos dentro de tilesets ainda não são interpretados. Mudar o arquivo Tiled depois de importar exige remover e importar o mapa novamente. Limite de quatro milhões de células por mapa.

Referência: [formato TMX](https://doc.mapeditor.org/en/stable/reference/tmx-map-format/) e [GIDs/flags](https://doc.mapeditor.org/en/stable/reference/global-tile-ids/). A importação também foi verificada com `libgdx-challenges/desafio21/assets/tilemap/project.tmx`, usando tileset embutido e base64/zlib.

## Configurar a câmera

Abra **Cena / câmera**. Centro X/Y, largura/altura visível e zoom definem a projeção ortográfica: zoom maior que 1 aproxima, menor que 1 afasta. Arraste com botão central para navegar e use a roda do mouse para zoom no editor; essas alterações são persistidas ao salvar.

**Câmera fixa** usa o centro configurado. Para acompanhar um personagem, selecione o objeto e clique **Seguir objeto selecionado**. A câmera segue o centro em coordenadas de mundo, incluindo a hierarquia. Suavização 0 é instantânea; valores maiores acompanham mais rapidamente. Zona morta X/Y permite mover o alvo dentro dessa área antes de deslocar a câmera. **Alternar limite do mundo** restringe o centro às bordas; quando o mundo é menor que a visão, centraliza nele.

**Fade inicial** é a duração em segundos da entrada a partir do preto; 0 desativa. O fade recomeça ao mudar de cena pelo script. HUD permanece independente da câmera e o fade cobre mundo e HUD. No Android, câmera/HUD usam a área segura entre barras e recortes.

Clique **Aplicar cena** ou **Salvar** para incorporar campos editados. Play inicia na cena inicial do jogo e usa uma cópia; mudanças feitas pelos scripts não alteram o documento salvo.

## Música e efeitos

**Música** importa WAV/MP3 para a cena atual. Configure volume de 0 a 1 e repetição em **Cena / câmera**; **Remover música** desassocia. A reprodução começa no Play, troca junto com a cena e para ao encerrar. Áudio usa JavaFX Media no desktop e MediaPlayer no Android; suporte aos codecs depende do host. Prefira WAV PCM ou MP3 comum e valide no dispositivo final.

Scripts podem tocar efeitos curtos com `context.playSound("assets/efeito.wav")`. O arquivo deve estar dentro de `assets/` do projeto. Cada chamada cria uma reprodução e libera o player ao terminar. Não há mixer, áudio espacial nem painel de efeitos dedicado nesta versão.

## Criar e anexar scripts Java

Salve o projeto para ter uma pasta. Selecione um objeto ou uma HUD e clique **Scripts Java** / **Editar / anexar script**. Para código da cena, use **Script da cena** no painel **Cena / câmera**. A janela mantém o alvo usado para abri-la; mudar a seleção no editor principal não muda esse alvo.

**Novo script** cria uma classe em `scripts/game/scripts/`. O editor tem fonte monoespaçada, números de linha, salvamento e diagnósticos com arquivo/linha/coluna. **Compilar** salva e verifica todos os fontes. **Anexar ao alvo** associa a classe escolhida; **Desanexar** remove o vínculo. O código é salvo ao trocar de arquivo ou fechar; salve também o projeto para persistir os vínculos. Uma classe pode ser reutilizada por vários alvos: cada anexo recebe uma instância própria. Há um anexo por objeto, HUD e cena.

Não há autocomplete, debugger ou destaque sintático nesta primeira versão. **Abrir IDE** prepara um projeto Gradle com as bibliotecas da engine e solicita o executável, como `code`, `codium`, `idea` ou caminho completo. A pasta do projeto é passada como argumento. A IDE precisa estar instalada; os arquivos Gradle existentes são preservados. **Abrir pasta** usa o gerenciador do sistema.

Implemente `GameScript` usando Java 17 e a API portátil:

```java
package game.scripts;

import megalodonte.engine.GameContext;
import megalodonte.engine.GameScript;

public class PlayerMovement implements GameScript {
    @Override public void onStart(GameContext context) {
        context.log("Jogador iniciado");
    }

    @Override public void onUpdate(double dt, GameContext context) {
        double dx = context.horizontal(); // WASD/setas ou toque nas zonas do jogo
        if (context.actionDown("left")) dx -= 1;
        if (context.actionDown("right")) dx += 1;
        context.move(Math.max(-1, Math.min(1, dx)) * 4 * dt,
                     context.vertical() * 4 * dt);
    }

    @Override public void onDestroy(GameContext context) {
        // Libere recursos próprios aqui.
    }
}
```

`context.move(dx,dy)` usa colisões e limites do mundo. Objetos controláveis com script deixam de usar o movimento automático, evitando movimentação duplicada. O exemplo acima é apropriado para um anexo de objeto.

| API | Uso |
| --- | --- |
| `scene()` | Cena ativa, câmera e propriedades |
| `object()` / `hud()` | Alvo; lance apenas a chamada correspondente ao anexo |
| `findObject(idOuNome)` / `findHud(id)` | Encontrar outros elementos; inexistente gera exceção |
| `worldTransform()` / `setWorldPosition(x,y)` | Ler pose / reposicionar objeto sem testar colisão |
| `horizontal()` / `vertical()` | Entrada direcional normalizada |
| `actionDown(nome)` | Ação mantida |
| `actionPressed(nome)` / `actionReleased(nome)` | Transição de ação no frame |
| `changeScene(id)` | Trocar de cena ao fim do frame |
| `playSound(caminho)` / `log(texto)` | Efeito de áudio / mensagem de diagnóstico |

HUD: botão publica seu campo **Ação** (ou ID se vazio). Teclas desktop publicam nomes minúsculos (`space`, `enter`, etc.). Android suporta botões HUD simultâneos; ações são liberadas ao cancelar o toque/perder foco. Alterar `context.findHud("placar").text` atualiza o texto durante o jogo. Atualização de scripts e áudio ocorre na thread do host; evite operações bloqueantes no lifecycle.

Play compila todos os fontes com `--release 17` em segundo plano, carrega classes e inicia o lifecycle. Erros de compilação impedem iniciar; erro de execução encerra o Play com mensagem. `onDestroy` é chamado na troca de cena e ao parar. Fontes compartilhados devem evitar APIs de JavaFX/libGDX/Android. Código criado no desktop pode modificar o sistema como qualquer Java do autor: use fontes confiáveis.

No Android, `scripts/` é compilado pelo build e as classes entram no DEX; não é compilação dinâmica no celular. O APK inclui `game.json` e `assets/`, sem fontes de scripts, cache da engine ou logs. Use **Exportar Android** ou `bash scripts/build-android.sh /pasta/do/jogo` após salvar. Reexporte quando mudar o código.

## Projeto de exemplo

```bash
bash gradlew createExample -PexampleDir=/caminho/novo/exemplo
bash scripts/build-android.sh /caminho/novo/exemplo
```

O gerador recusa sobrescrever `game.json`, cria mapa/tileset, WAV de demonstração, colisores, HUD de direção/posição, script Player e câmera follow/fade. Compila e executa um trecho do script como verificação. Abra o arquivo criado na engine para experimentar. Sem física de gravidade/salto automática; o exemplo move o jogador nos dois eixos.

## Sprites animados e física de plataforma

Entity possui `animations` (nome → frames de assets, fps e loop), `animation`, `animationTime`, `visible` e `flipX`. `context.animate(entity, "walk")` troca o estado e reinicia o relógio apenas quando o nome muda. Sprites.asset resolve o frame nos dois hosts; o editor usa a mesma resolução. O relógio pertence à cópia de execução.

`colliderOffsetX/Y` e `colliderWidth/Height` permitem separar o corpo do sprite; tamanho 0 usa o tamanho visual. Colisores invisíveis continuam bloqueando. `context.move(entity, dx, dy)` retorna Movement com deslocamento efetivo e bloqueio por eixo, permitindo implementar gravidade e zerar velocidade ao pousar. A resolução aproxima a borda livre para evitar que o personagem pare com uma folga visível. `context.grounded(entity)` testa contato abaixo. `context.overlaps(a,b)` testa caixas de mundo para moedas/perigos.

`context.pause(true)` congela movimento automático, relógios e animações, mantendo callbacks de scripts com delta zero para que menus possam processar input e retomar. Pausa/mute são encaminhados aos players de áudio dos hosts. `context.state()` preserva valores de sessão entre cenas. Trocar ou reiniciar cena restaura seus elementos a partir do documento inicial; isso recupera moedas/objetos alterados e evita que reinícios herdem estado da tentativa anterior.

A adaptação Alex vs Bus em `../../games/alex-vs-bus` usa essas APIs, 13 fases originais, câmera, tilemaps, áudio, HUD e scripts nos dois hosts. Consulte também [exportação Desktop](DESKTOP_EXPORT.md).

`context.quit()` solicita sair ao host (fecha o aplicativo no Desktop/Android; encerra Play no editor). Botões com `assetPath` desenham o PNG do projeto e mantêm sua ação; os menus podem alterar `visible` e `assetPath` em execução. Mudanças de `musicAssetPath` dentro da mesma cena também trocam a música nos dois hosts.

## Âncoras da HUD

`referenceArea` escolhe a base das âncoras: `viewport` (padrão compatível, tela segura após insets) ou `content` (área 1280×720 ajustada e centralizada). `left/top` mede a margem da esquerda/topo dessa área; `right/bottom` mede a margem da direita/base; `center` mede o deslocamento a partir do centro. Menus usam conteúdo; controles nas bordas da tela usam viewport. No inspetor, Área da HUD alterna a referência. Mudar a referência ou a âncora preserva a posição visual e recalcula os offsets; arrastar usa a mesma transformação nos hosts Desktop/Android.

`GameContext.saveState()` salva o estado do jogador; os hosts Desktop e Android o restauram antes de `onStart`. No editor o perfil é separado do jogador Desktop. Scripts devem salvar apenas dados persistentes. A música continua entre cenas quando asset e repetição são iguais. `camera.viewportTop` reserva de 0 a 719 pixels da referência 1280×720 acima da projeção do mundo; configure em Cena → opções.

O ícone pode ser configurado em Projeto → Ícone do aplicativo, com um caminho PNG relativo (por exemplo `assets/icon.png`). Android gera ícones por densidade e adaptativos; sem asset, usa o ícone da engine. Desktop usa o PNG na janela e no pacote Linux.
