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
echo "Projeto pronto: $project_dir"
echo "Compilação validada. Abra o projeto no Android Studio para gerar e assinar sua release."
bash "$engine_dir/scripts/remember-project.sh" "$game_dir" || echo "Não foi possível registrar o último projeto no editor." >&2
bash "$engine_dir/scripts/open-build-folder.sh" "$project_dir"
