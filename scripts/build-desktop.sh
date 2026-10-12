#!/usr/bin/env bash
set -euo pipefail
engine_dir="$(cd "$(dirname "$0")/.." && pwd)"
if [[ $# -ne 1 || ! -f "$1/game.json" ]]; then echo "Uso: $0 /pasta/do/jogo (com game.json)" >&2; exit 1; fi
game_dir="$(cd "$1" && pwd)"
bash "$engine_dir/gradlew" -p "$engine_dir" packageGameDesktop -PgameDir="$game_dir"
if [[ "$(uname -s)" == Linux ]]; then
  name="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get("applicationId","megalodonte.engine.game").split(".")[-1])' "$game_dir/game.json")"
  tar -czf "$game_dir/build/desktop/$name-linux-$(uname -m).tar.gz.tmp" -C "$game_dir/build/desktop" "$name"
  mv "$game_dir/build/desktop/$name-linux-$(uname -m).tar.gz.tmp" "$game_dir/build/desktop/$name-linux-$(uname -m).tar.gz"
  bash "$engine_dir/scripts/package-appimage.sh" "$game_dir/build/desktop/$name" "$game_dir"
fi
bash "$engine_dir/scripts/remember-project.sh" "$game_dir" || echo "Não foi possível registrar o último projeto no editor." >&2
bash "$engine_dir/scripts/open-build-folder.sh" "$game_dir/build/desktop"
