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
bash "$engine_dir/android/gradlew" -p "$engine_dir/android" :app:assembleDebug -PgameAssets="$staging_dir/game" -PgameScripts="$staging_dir/scripts"
mkdir -p "$game_dir/build/android"
cp "$engine_dir/android/app/build/outputs/apk/debug/app-debug.apk" "$game_dir/build/android/game-debug.apk.tmp"
mv "$game_dir/build/android/game-debug.apk.tmp" "$game_dir/build/android/game-debug.apk"
echo "APK: $game_dir/build/android/game-debug.apk"
