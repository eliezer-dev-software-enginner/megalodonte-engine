#!/usr/bin/env bash
set -euo pipefail
engine_dir="$(cd "$(dirname "$0")/.." && pwd)"
if [[ $# -ne 1 || ! -f "$1/game.json" ]]; then echo "Uso: $0 /pasta/do/jogo (com game.json)" >&2; exit 1; fi
game_dir="$(cd "$1" && pwd)"
staging_dir="$(mktemp -d)"
trap 'rm -rf "$staging_dir"' EXIT
mkdir -p "$staging_dir/game"
python3 "$engine_dir/scripts/pack-android-scenes.py" "$game_dir/game.json" "$staging_dir/game"
if [[ -d "$game_dir/assets" ]]; then cp -R "$game_dir/assets" "$staging_dir/game/assets"; fi
if [[ -d "$game_dir/scripts" ]]; then cp -R "$game_dir/scripts" "$staging_dir/scripts"; fi
export JAVA_HOME="${ANDROID_JAVA_HOME:-/opt/android-studio-for-platform/jbr}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
mapfile -t metadata < <(python3 - "$staging_dir/game/game.json" <<'META'
import json,sys
with open(sys.argv[1]) as f: game=json.load(f)
print(game.get("applicationId","megalodonte.engine.game"))
print(game.get("name","Megalodonte Game").replace("\n"," ").replace("\r"," "))
icon=game.get("iconAssetPath", "")
if icon:
    from pathlib import Path
    root=Path(sys.argv[1]).parent.resolve()
    target=(root/icon).resolve()
    if not target.is_relative_to(root) or not target.is_file() or target.suffix.lower()!=".png": raise SystemExit("Ícone PNG inválido: "+icon)
    print(target)
else: print("")
print(game.get("versionCode",1))
print(game.get("versionName","0.1.0"))
META
)
bash "$engine_dir/android/gradlew" -p "$engine_dir/android" :app:clean :app:assembleDebug -PgameAssets="$staging_dir/game" -PgameScripts="$staging_dir/scripts" -PgameApplicationId="${metadata[0]}" -PgameName="${metadata[1]}" -PgameIcon="${metadata[2]}" -PgameVersionCode="${metadata[3]}" -PgameVersionName="${metadata[4]}"
# Asset roots change with each staging directory; rebuild app outputs to avoid stale AGP merges.
python3 - "$staging_dir/game" "$engine_dir/android/app/build/outputs/apk/debug/app-debug.apk" <<'VERIFY'
from pathlib import Path
import sys,zipfile
root=Path(sys.argv[1])
with zipfile.ZipFile(sys.argv[2]) as apk:
    for path in root.rglob('*.json'):
        if apk.read('assets/'+path.relative_to(root).as_posix())!=path.read_bytes():
            raise SystemExit('APK contains stale game data: '+str(path.relative_to(root)))
print('APK game JSON verified against build snapshot')
VERIFY
mkdir -p "$game_dir/build/android"
cp "$engine_dir/android/app/build/outputs/apk/debug/app-debug.apk" "$game_dir/build/android/game-debug.apk.tmp"
mv "$game_dir/build/android/game-debug.apk.tmp" "$game_dir/build/android/game-debug.apk"
echo "APK: $game_dir/build/android/game-debug.apk"

bash "$engine_dir/scripts/remember-project.sh" "$game_dir" || echo "Não foi possível registrar o último projeto no editor." >&2
bash "$engine_dir/scripts/open-build-folder.sh" "$game_dir/build/android"
