#!/usr/bin/env bash
set -euo pipefail
engine_dir="$(cd "$(dirname "$0")/.." && pwd)"
if [[ $# -ne 1 || ! -f "$1/game.json" ]]; then echo "Uso: $0 /pasta/do/jogo (com game.json)" >&2; exit 1; fi
game_dir="$(cd "$1" && pwd)"
staging_dir="$(mktemp -d)"
trap 'rm -rf "$staging_dir"' EXIT
mkdir -p "$staging_dir/game"
cp "$game_dir/game.json" "$staging_dir/game/game.json"
if [[ -d "$game_dir/assets" ]]; then cp -R "$game_dir/assets" "$staging_dir/game/assets"; fi
if [[ -d "$game_dir/scripts" ]]; then cp -R "$game_dir/scripts" "$staging_dir/scripts"; fi
export JAVA_HOME="${ANDROID_JAVA_HOME:-/opt/android-studio-for-platform/jbr}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
mapfile -t metadata < <(python3 - "$staging_dir/game/game.json" <<'META'
import json,sys
with open(sys.argv[1]) as f: game=json.load(f)
print(game.get("applicationId","megalodonte.engine.game"))
print(game.get("name","Megalodonte Game").replace("\n"," ").replace("\r"," "))
META
)
bash "$engine_dir/android/gradlew" -p "$engine_dir/android" :app:assembleDebug -PgameAssets="$staging_dir/game" -PgameScripts="$staging_dir/scripts" -PgameApplicationId="${metadata[0]}" -PgameName="${metadata[1]}"
mkdir -p "$game_dir/build/android"
cp "$engine_dir/android/app/build/outputs/apk/debug/app-debug.apk" "$game_dir/build/android/game-debug.apk.tmp"
mv "$game_dir/build/android/game-debug.apk.tmp" "$game_dir/build/android/game-debug.apk"
echo "APK: $game_dir/build/android/game-debug.apk"

bash "$engine_dir/scripts/remember-project.sh" "$game_dir" || echo "Não foi possível registrar o último projeto no editor." >&2
bash "$engine_dir/scripts/open-build-folder.sh" "$game_dir/build/android"
