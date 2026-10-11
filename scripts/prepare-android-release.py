#!/usr/bin/env python3
"""Export a self-contained Android Studio project without credentials or build caches."""
import json
import os
import re
import shutil
import sys
import tempfile
import uuid
from datetime import datetime
from pathlib import Path


def property_value(value):
    # Java .properties uses ISO-8859-1; escape unicode and metacharacters.
    encoded = str(value).encode('utf-16-be')
    result = ''
    for i in range(0, len(encoded), 2):
        char = int.from_bytes(encoded[i:i+2], 'big')
        if char < 32 or char > 126:
            result += '\\u%04x' % char
        elif chr(char) in '\\:=#! ':
            result += '\\' + chr(char)
        else:
            result += chr(char)
    return result


def find_sdk():
    preference = Path.home() / '.config/megalodonte-engine/android-sdk.txt'
    chosen = preference.read_text().strip() if preference.is_file() else None
    candidates = [os.environ.get('ANDROID_HOME'), os.environ.get('ANDROID_SDK_ROOT'), chosen,
                  Path.home() / 'Android/Sdk', Path.home() / 'Library/Android/sdk']
    for candidate in candidates:
        if candidate:
            sdk = Path(candidate).expanduser().resolve()
            if (sdk / 'platforms').is_dir():
                return sdk
    return None


def configure_sdk(project, sdk):
    local = Path(project) / 'local.properties'
    lines = local.read_text().splitlines() if local.exists() else []
    lines = [line for line in lines if not re.match(r'^\s*sdk\.dir\s*[:=]', line)]
    lines.append('sdk.dir=' + property_value(Path(sdk).resolve()))
    local.write_text('\n'.join(lines) + '\n', encoding='utf-8')


def prepare(engine, game):
    engine, game = Path(engine).resolve(), Path(game).resolve()
    metadata = json.loads((game / 'game.json').read_text())
    app_id = metadata.get('applicationId', 'megalodonte.engine.game')
    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+', app_id):
        raise ValueError('applicationId inválido')
    version_code = metadata.get('versionCode', 1)
    version_name = metadata.get('versionName', '0.1.0')
    if type(version_code) is not int or not 1 <= version_code <= 2100000000 or not isinstance(version_name, str) or not version_name.strip():
        raise ValueError('Versão do aplicativo inválida')
    icon = metadata.get('iconAssetPath', '')
    if icon:
        source = (game / icon).resolve()
        if not source.is_relative_to(game) or not source.is_file() or source.suffix.lower() != '.png':
            raise ValueError('Ícone PNG inválido: ' + icon)
    output = game / 'build/android-release'
    output.mkdir(parents=True, exist_ok=True)
    temporary = Path(tempfile.mkdtemp(prefix='preparing-', dir=output))
    try:
        android = engine / 'android'
        for name in ['build.gradle.kts', 'gradlew', 'gradlew.bat']:
            shutil.copy2(android / name, temporary / name)
        shutil.copytree(android / 'gradle', temporary / 'gradle')
        settings = (android / 'settings.gradle.kts').read_text().replace('file("../engine-core")', 'file("engine-core")')
        (temporary / 'settings.gradle.kts').write_text(settings)
        (temporary / 'app').mkdir()
        shutil.copy2(android / 'app/build.gradle.kts', temporary / 'app/build.gradle.kts')
        shutil.copytree(android / 'app/src/main', temporary / 'app/src/main')
        (temporary / 'engine-core').mkdir()
        shutil.copy2(engine / 'engine-core/build.gradle.kts', temporary / 'engine-core/build.gradle.kts')
        shutil.copytree(engine / 'engine-core/src/main', temporary / 'engine-core/src/main')
        (temporary / 'game').mkdir()
        shutil.copy2(game / 'game.json', temporary / 'game/game.json')
        if (game / 'assets').is_dir():
            shutil.copytree(game / 'assets', temporary / 'game/assets')
        if (game / 'scripts').is_dir():
            shutil.copytree(game / 'scripts', temporary / 'scripts')
        else:
            (temporary / 'scripts').mkdir()
        for entry in ['licenses', 'LICENSE.txt']:
            source = game / entry
            if source.is_dir():
                shutil.copytree(source, temporary / entry)
            elif source.is_file():
                shutil.copy2(source, temporary / entry)
        values = {
            'gameAssets': '../game', 'gameScripts': '../scripts',
            'gameApplicationId': app_id, 'gameName': metadata.get('name', 'Megalodonte Game'),
            'gameIcon': '../game/' + icon if icon else '',
            'gameVersionCode': version_code, 'gameVersionName': version_name,
        }
        properties = (android / 'gradle.properties').read_text() + '\n'
        properties += '\n'.join(key + '=' + property_value(value) for key, value in values.items()) + '\n'
        (temporary / 'gradle.properties').write_text(properties, encoding='ascii')
        sdk = find_sdk()
        if sdk is not None:
            configure_sdk(temporary, sdk)
        (temporary / '.gitignore').write_text('.gradle/\n**/build/\n.idea/\nlocal.properties\n*.jks\n*.keystore\nsigning.properties\n')
        (temporary / 'README.md').write_text('''# Android Release — projeto exportado

Este projeto contém uma cópia independente dos assets, scripts e runtime da engine.
Abra esta pasta no Android Studio e aguarde a sincronização do Gradle.
O SDK local detectado é configurado em local.properties (ignorado pelo Git).
Em outro computador, ajuste sdk.dir para o SDK instalado naquele computador.

1. Build → Generate Signed Bundle / APK.
2. Escolha APK para distribuição direta ou Android App Bundle (AAB) para Google Play.
3. Selecione o módulo app e crie ou selecione seu keystore.
4. Escolha release e conclua. Guarde a chave para assinar futuras atualizações.

Os arquivos assinados ficam no destino escolhido no Studio; as saídas Gradle ficam
em app/build/outputs/apk/release ou app/build/outputs/bundle/release.
As saídas da compilação de validação dentro deste projeto não têm assinatura.
Use Generate Signed Bundle / APK para gerar os arquivos de distribuição.

Nome, pacote, ícone, versionCode e versionName estão em gradle.properties.
Incremente versionCode para cada atualização. O código de versão é diferente da
versão do formato do documento game.json. Não inclua keystores ou senhas no Git.

Esta pasta é uma cópia: compilar diretamente nela não sincroniza mudanças do jogo.
Para atualizar esta exportação, use Projeto Android no editor ou
bash tools/build.sh android-release na pasta original do jogo.
Uma exportação existente é preservada em project-backup-<data>-<id>, incluindo
suas alterações locais; alterações feitas aqui não retornam ao editor.
''')
        target = output / 'project'
        if target.exists():
            backup = output / ('project-backup-' + datetime.now().strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:8])
            target.rename(backup)
            print('Exportação anterior preservada em:', backup)
        temporary.rename(target)
        print(target)
        return target
    finally:
        if temporary.exists():
            shutil.rmtree(temporary)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise SystemExit('Uso: prepare-android-release.py /pasta/do/jogo')
    prepare(Path(__file__).resolve().parent.parent, sys.argv[1])
