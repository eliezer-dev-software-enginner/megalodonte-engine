#!/usr/bin/env bash
set -euo pipefail
engine_dir="$(cd "$(dirname "$0")/.." && pwd)"
if [[ $# -ne 2 || ! -d "$1/bin" ]]; then echo "Uso: $0 /pasta/desktop/app /pasta/do/jogo" >&2; exit 1; fi
[[ "$(uname -s)" == Linux ]] || { echo "AppImage requer Linux" >&2; exit 1; }
app_dir="$(cd "$1" && pwd)"
game_dir="$(cd "$2" && pwd)"
name="$(basename "$app_dir")"
arch="$(uname -m)"
case "$arch" in x86_64|aarch64) ;; *) echo "Arquitetura AppImage não suportada: $arch" >&2; exit 1 ;; esac
tool="${APPIMAGETOOL:-$engine_dir/build/tools/appimagetool-$arch.AppImage}"
if [[ ! -x "$tool" ]]; then
  mkdir -p "$(dirname "$tool")"
  curl --fail --location --retry 2 "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-$arch.AppImage" -o "$tool.tmp"
  chmod +x "$tool.tmp"; mv "$tool.tmp" "$tool"
fi
staging="$(mktemp -d)"
trap 'rm -rf "$staging"' EXIT
mkdir -p "$staging/$name.AppDir"
cp -a "$app_dir/." "$staging/$name.AppDir/"
python3 - "$staging/$name.AppDir" "$name" "$game_dir/game.json" <<'PY'
import json,sys
from pathlib import Path
root=Path(sys.argv[1]);name=sys.argv[2]
with open(sys.argv[3],encoding='utf-8') as reader:game=json.load(reader)
title=game.get('name',name).replace('\n',' ').replace('\r',' ')
(root/'AppRun').write_text('#!/bin/sh\nAPPDIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"\nexec "$APPDIR/bin/'+name+'" "$@"\n')
(root/'AppRun').chmod(0o755)
(root/(name+'.desktop')).write_text('[Desktop Entry]\nType=Application\nName='+title+'\nExec='+name+'\nIcon='+name+'\nCategories=Game;\nTerminal=false\n')
import shutil
shutil.copy2(root/'lib'/(name+'.png'),root/(name+'.png'))
shutil.copy2(root/(name+'.png'),root/'.DirIcon')
PY
output="$game_dir/build/desktop/$name-$arch.AppImage"
ARCH="$arch" APPIMAGE_EXTRACT_AND_RUN=1 "$tool" --no-appstream --mksquashfs-opt '-processors' --mksquashfs-opt '2' "$staging/$name.AppDir" "$output.tmp"
chmod +x "$output.tmp"; mv "$output.tmp" "$output"
echo "AppImage: $output"
if [[ "${ENGINE_NO_OPEN:-0}" != 1 && -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]]; then
  log="$game_dir/build/desktop/appimage-run.log"
  setsid nohup env APPIMAGE_EXTRACT_AND_RUN=1 "$output" >"$log" 2>&1 </dev/null &
  pid=$!
  sleep 2
  if kill -0 "$pid" 2>/dev/null; then echo "AppImage iniciado (PID $pid). Log: $log"; else echo "AppImage encerrou ao iniciar. Consulte: $log" >&2; exit 1; fi
fi
