# Android Release pelo Android Studio

No editor, abra **Projeto** e configure o identificador do aplicativo, ícone PNG,
**Código da versão Android (versionCode)** e **Versão exibida (versionName)**.
`versionCode` deve ser um inteiro positivo e crescer a cada atualização publicada.
`versionName` é a versão mostrada ao usuário, por exemplo `1.0.0`.
Esses campos são independentes de `version`, a versão do formato do documento.

Clique em **Android Release**. A engine salva o jogo, prepara um projeto completo,
valida a compilação release e abre a pasta no explorador:

```
<pasta-do-jogo>/build/android-release/project
```

Também é possível executar, na pasta do Alex vs Bus:

```bash
bash tools/build.sh android-release
```

Para outros jogos, execute a partir da pasta da engine:

```bash
bash scripts/export-android-release.sh /pasta/do/jogo
```

A exportação inclui `app`, `engine-core`, Gradle Wrapper, assets, scripts Java,
metadados, ícone e licenças. Os caminhos são relativos; o projeto pode ser movido
ou aberto em outro computador com Android Studio/SDK instalados, sem a engine
original. O Gradle pode precisar baixar suas dependências na primeira abertura.
A exportação existente é preservada em `project-backup-<data>-<id>`; alterações
locais e arquivos de assinatura dessa cópia não são apagados.

## Assinar

1. No Android Studio, escolha **Open** e abra `build/android-release/project`.
2. Aguarde a sincronização do Gradle e a configuração do SDK.
3. Use **Build → Generate Signed Bundle / APK**.
4. Escolha **APK** para distribuir diretamente ou **Android App Bundle (AAB)**
   para Google Play; selecione o módulo `app`.
5. Crie ou selecione seu keystore e informe alias e senhas.
6. Escolha **release** e conclua.

Guarde o keystore para futuras atualizações e não inclua chaves ou senhas no Git.
A engine não cria credenciais nem publica o aplicativo.

Os arquivos assinados ficam no destino escolhido no Studio. As saídas do Gradle
ficam em `app/build/outputs/apk/release` e `app/build/outputs/bundle/release`.
Os arquivos `game-release-unsigned.apk` e `game-release-unsigned.aab` na pasta
`build/android-release` são artefatos de validação **sem assinatura**; não são
as versões finais para distribuição.

Após editar o jogo, exporte novamente para atualizar assets e scripts.
O projeto exportado é uma cópia: suas alterações não retornam ao editor.
O botão **Exportar Android** continua gerando o APK debug para testes rápidos.

Referência: [assinatura de apps no Android Studio](https://developer.android.com/studio/publish/app-signing).
