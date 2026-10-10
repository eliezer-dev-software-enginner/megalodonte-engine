#!/usr/bin/env bash
set -euo pipefail
engine_dir="$(cd "$(dirname "$0")/.." && pwd)"
if [[ $# -ne 1 || ! -f "$1/game.json" ]]; then echo "Uso: $0 /pasta/do/jogo" >&2; exit 1; fi
game_dir="$(cd "$1" && pwd)"
python3 "$engine_dir/scripts/prepare-android-release.py" "$game_dir"
project_dir="$game_dir/build/android-release/project"
export JAVA_HOME="${ANDROID_JAVA_HOME:-/opt/android-studio-for-platform/jbr}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
bash "$project_dir/gradlew" -p "$project_dir" :app:assembleRelease :app:bundleRelease
cp "$project_dir/app/build/outputs/apk/release/app-release-unsigned.apk" "$game_dir/build/android-release/game-release-unsigned.apk"
cp "$project_dir/app/build/outputs/bundle/release/app-release.aab" "$game_dir/build/android-release/game-release-unsigned.aab"
echo "Projeto pronto: $project_dir"
echo "APK e AAB de validação gerados sem assinatura. Abra o projeto no Android Studio e use Generate Signed Bundle / APK."
bash "$engine_dir/scripts/remember-project.sh" "$game_dir" || echo "Não foi possível registrar o último projeto no editor." >&2
bash "$engine_dir/scripts/open-build-folder.sh" "$project_dir"
